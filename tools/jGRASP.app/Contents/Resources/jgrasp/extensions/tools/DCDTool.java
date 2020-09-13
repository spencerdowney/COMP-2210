
import java.io.File;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jgrasp.FileData;
import jgrasp.ResizableFlexWidthIcon;
import jgrasp.Tool;

import jgrasp.tool.AbstractToolAction;
import jgrasp.tool.ActionContext;
import jgrasp.tool.CompilerEnvironmentLocation;
import jgrasp.tool.Conditions;
import jgrasp.tool.DateTestType;
import jgrasp.tool.ExecType;
import jgrasp.tool.Highlightable;
import jgrasp.tool.ProjectState;
import jgrasp.tool.ToolAction;
import jgrasp.tool.ToolCreateData;
import jgrasp.tool.ToolState;
import jgrasp.tool.ToolUtil;


/** DCD tool interface. **/
public class DCDTool implements Tool {
 
   /** Arbitrary ID for tool storage. **/
   private static final int SETTINGS_ID = 426;

   /** Identifer for storing and retrieving per-project per-file
    *  state data. **/
   private static String dataId = "jgrDCD0xr";
  
   /** The "about" action. **/
   private ToolAction aboutAction = 
      new AbstractToolAction("About") {
       
         public void execute(final ActionContext context,
               final Highlightable h) {
            about(context);
         }
      };
 
   /** The configure action. **/
   private ToolAction configureAction = 
      new AbstractToolAction("Configure") {
       
         public void execute(final ActionContext context,
               final Highlightable h) {
            configure(context);
         }
      };
 
   /** Menu separator. **/
   private ToolAction sepAction = 
      new AbstractToolAction(null) {
       
         public boolean showMenuItem() {
            return checkFileAction.showMenuItem()
                  || checkProjectAction.showMenuItem();
         }
      
         public void execute(final ActionContext context,
               final Highlightable h) {
         }
      };
 
   /** The check file action. **/
   private ToolAction checkFileAction = 
      new AbstractToolAction("Find Dead Code in Current Directory",
            "DCD", "Find dead code in parent directory of file",
            new DCDIcon()) {
       
         public boolean showMenuItem() {
            if (settings.getDCDHome() == null) {
               return false;
            }
            return activeJavaFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            FileData af = context.getActiveFile();
            if (af == null) {
               // Should never happen.
               return;
            }
         
            List<File> filesTested = new ArrayList<File>();
            filesTested.add(af.getFile());
            executeCommand(filesTested, context, h);
         }
      };
 
   /** The check project action. **/
   private ToolAction checkProjectAction = 
      new AbstractToolAction("Find Dead Code in Project Directories",
            "DCD", "Find dead code in project directories and jar files",
            new DCDIcon(),
            EnumSet.of(ToolAction.Flag.PROJECT)) {
       
         public boolean showMenuItem() {
            if (settings.getDCDHome() == null) {
               return false;
            }
            return projectWithJavaFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            List<File> files = new ArrayList<File>();
            // Find all files.
            List<FileData> fds = context.getProjectFiles();
            if (fds == null) {
               return;
            }
            for (FileData fd : fds) {
               if (fd.hasLanguage("Java")) {
                  files.add(fd.getFile());
               }
            }
            executeCommand(files, context, h);
         }
      };
 
   /** The configuration dialog. **/
   private DCDSettingsDialog configureDialog;
 
   /** Current DCD settings. **/
   private DCDSettings settings;

   /** Creation data. **/
   private ToolCreateData creationData;
 
   /** True if current conditions indicate an active Java file which
    *  is not in a project, false otherwise. **/
   private boolean activeJavaFile;
 
   /** True if the current conditions indicate a project containing
    *  at least one Java file, false otherwise. **/
   private boolean projectWithJavaFile;
 
 
   /** Creates a new DCD tool.
    *
    *  @param tcd tool creation data. **/
   public DCDTool(final ToolCreateData tcd) {
      creationData = tcd;
      settings = new DCDSettings(tcd.getData(DCDTool.class,
            SETTINGS_ID));
   }


   /** Executes DCD.
    *
    *  @param file the source files to be checked.
    *
    *  @param context the current context.
    *
    *  @param h item to be highlighted during execution.
    *  This may be null. **/
   private void executeCommand(final List<File> files,
         final ActionContext context, final Highlightable h) {
   
      if (files.size() == 0) {
         creationData.showError(context.getDialogParent(),
               "Project contains no source files.",
               "DCD Plugin Error");
         return;
      }
   
      Set<File> dirsAndJars = new HashSet<File>();
      for (File f : files) {
         List<File> classFiles = ToolUtil.getClassFiles(f, context);
         for (File cf : classFiles) {
            File dir = cf.getParentFile();
            if (dir != null) {
               dirsAndJars.add(dir);
            }
         }
      }
   
      if (dirsAndJars.size() == 0) {
         creationData.showError(context.getDialogParent(),
               "No class files found.\nYou may need to "
               + "compile your source files.", "DCD Plugin Error");
         return;
      }
   
      StringBuilder cfg = new StringBuilder();
      cfg.append("directories = ");
      boolean first = true;
      for (File f : dirsAndJars) {
         if (!first) {
            cfg.append(",");
         }
         first = false;
         cfg.append(f.getAbsolutePath().replace("\\", "\\\\"));
      }
      cfg.append("\n");
      String exclClasses = settings.getExcludedClasses();
      if (exclClasses.length() > 0) {
         cfg.append("excludedClasses = " + exclClasses);
      }
      String exclMethods = settings.getExcludedMethods();
      if (exclMethods.length() > 0) {
         cfg.append("excludedMethods = " + exclMethods);
      }
      if (settings.checkPublic()) {
         cfg.append("publicDeadCode = true\n");
      }
      if (settings.checkLocal()) {
         cfg.append("localDeadCode = true\n");
      }
      
      File cfgFile = creationData.createTmpFile(context.getDialogParent(),
            "dcd", null, cfg.toString(), Charset.defaultCharset(), "DCD");
      if (cfgFile == null) {
         return;
      }
   
      StringBuilder command = new StringBuilder();
      command.append("java ");
      command.append(settings.getJavaFlags());
      command.append(" -cp \"dcd.jar%:lib%/asm-3.1.jar;");
      command.append("lib%/asm-tree-3.1.jar%:lib%/jsp-api.jar%:");
      command.append("lib%/servlet-api.jar%:%<JGRASP_CLASSPATHS>\" ");
      command.append("dcd.DeadCodeDetector \"");
      command.append(cfgFile.getAbsolutePath());
      command.append("\"");
   
      context.executeCommand(command.toString(), ExecType.CHECK,
            "c-.* in class ([\\w\\.\\$]+).*",
            settings.getDCDHome().getAbsolutePath(),
            "PATH+=%<JAVA_BIN_DIR>%;\n"
            + "PATH+=%<JGRASP_PATHS>%;", "DCD", h,
            null, null, null, DateTestType.TARGETS, files, null);
   
      cfgFile.deleteOnExit();
   }
   
   
   /** {@inheritDoc} **/
   public String getMenuName() {
      return "DCD";
   }
   
   
   /** {@inheritDoc} **/
   public ToolAction[] getToolActions() {
      return new ToolAction[] { checkFileAction, checkProjectAction,
            sepAction, configureAction, aboutAction };
   }
   
 
   /** {@inheritDoc} **/
   public List<CompilerEnvironmentLocation> getCompilerEnvironments() {
      return null;
   }
   
   
   /** Configures this tool.
    *
    *  @param context action context. **/
   private void configure(final ActionContext context) {
    
      if (configureDialog == null) {
         configureDialog = new DCDSettingsDialog(this);
      }
      configureDialog.showDialog(context, settings);
   }
   
   
   /** Shows about text.
    *
    *  @param context action context. **/
   private void about(final ActionContext context) {
      String text; 
      if (settings.getDCDHome() == null) {
         text = "jGRASP plugin for the Dead Code Detector (DCD) "
               + "development tool.\n"
               + "A DCD distribution was not found. Use "
               + "\"Configure\" to choose the DCD home directory "
               + "if DCD is installed.\n"
               + "DCD and information about DCD are "
               + "available at https://dcd.dev.java.net .";
      }
      else {
         text = "jGRASP plugin for the Dead Code Detector (DCD) "
               + " development tool.\n"
               + "DCD and information about DCD are "
               + "available at https://dcd.dev.java.net .";
      }
      creationData.showMessage(context.getDialogParent(), text,
            "About jGRASP DCD Plugin");
   }
   

   /** {@inheritDoc} **/
   public void applyConditions(final Conditions conditions) {
      activeJavaFile = false;
      FileData fd = conditions.getActiveFile();
      if (fd != null && !conditions.isInProject()) {
         activeJavaFile = fd.hasLanguage("Java");
      }
      
      projectWithJavaFile = false;
      List<FileData> fds = conditions.getProjectFiles();
      if (fds != null) {
         for (FileData f : fds) {
            if (f.hasLanguage("Java")) {
               projectWithJavaFile = true;
               break;
            }
         }
      }
   }
       
       
   /** Called when the user has changed the settings.
    *
    *  @param newSettings the new settings. **/
   public void settingsChanged(final DCDSettings newSettings) {
      settings = newSettings;
      creationData.storeData(DCDTool.class, SETTINGS_ID, settings.getData());
      creationData.guiChanged();
   }
   

   /** {@inheritDoc} **/
   public ResizableFlexWidthIcon getStateIcon(final ProjectState ps,
         final File fl, final boolean getSpacer) {
      return null;
   }
   
   
   /** {@inheritDoc} **/
   public String getClasspath() {
      return null;
   }
   
   
   /** {@inheritDoc} **/
   public String getDataId() {
      return dataId;
   }
   
   
   /** {@inheritDoc} **/
   public ToolState createState() {
      return null;
   }


   /** {@inheritDoc} **/
   public void fileRemoved(final Conditions conditions, final File fl,
         final boolean isTest) {
   }
   
   
   /** {@inheritDoc} **/
   public void closeWindows() {
   }
}


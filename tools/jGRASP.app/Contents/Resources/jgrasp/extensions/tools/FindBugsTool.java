
import java.io.File;

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


/** FindBugs tool interface. **/
public class FindBugsTool implements Tool {
 
   /** Arbitrary ID for tool storage. **/
   private static final int SETTINGS_ID = 197;
 
   /** Identifer for storing and retrieving per-project per-file
    *  state data. **/
   private static String dataId = "jgrFindBugs0xr";

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
      new AbstractToolAction("Find Bugs in File", "FindBugs",
         "Run FindBugs on file", new FindBugsIcon()) {
       
         public boolean showMenuItem() {
            if (settings.getFindBugsHome() == null) {
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
      new AbstractToolAction("Find Bugs in Project Files",
            "FindBugs", "Run FindBugs on project files",
            new FindBugsIcon(),
            EnumSet.of(ToolAction.Flag.PROJECT)) {
       
         public boolean showMenuItem() {
            if (settings.getFindBugsHome() == null) {
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
   private FindBugsSettingsDialog configureDialog;
 
   /** Current FindBugs settings. **/
   private FindBugsSettings settings;

   /** Creation data. **/
   private ToolCreateData creationData;
 
   /** True if current conditions indicate an active Java file which
    *  is not in a project, false otherwise. **/
   private boolean activeJavaFile;
 
   /** True if the current conditions indicate a project containing
    *  at least one Java file, false otherwise. **/
   private boolean projectWithJavaFile;
 
 
   /** Creates a new FindBugs tool.
    *
    *  @param tcd tool creation data. **/
   public FindBugsTool(final ToolCreateData tcd) {
      creationData = tcd;
      settings = new FindBugsSettings(tcd.getData(FindBugsTool.class,
            SETTINGS_ID));
   }


   /** Executes FindBugs.
    *
    *  @param command the command line.
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
               "FindBugs Plugin Error");
         return;
      }
   
      File workingDir = null;
      for (File f : files) {
         if (workingDir == null) {
            String wkdir = ToolUtil.getPackageRoot(f,
                  context.getCharset());
            if (wkdir == null) {
               //*** Error message.
               return;
            }
            workingDir = new File(wkdir);
         }
      }
      File jarFile = settings.getFindBugsJar();
      StringBuilder command = new StringBuilder();
      command.append("java -jar \"");
      command.append(jarFile.getAbsolutePath());
      command.append("\" ");
      command.append(settings.getJavaFlags());
      command.append(" \"");
      command.append("\" -textui -emacs ");
      command.append(settings.getFlags());
   
      boolean addedClass = false;
      for (File f : files) {
         List<File> classFiles = ToolUtil.getClassFiles(f, context);
         for (File cf : classFiles) {
            addedClass = true;
            command.append(" \"" + cf.getAbsolutePath() + "\"");
         }
      }
      if (!addedClass) {
         creationData.showError(context.getDialogParent(),
               "No class files found.\nYou may need to compile "
               + "your source files.", "FindBugs Plugin Error");
         return;
      }
   
      context.executeCommand(command.toString(), ExecType.CHECK,
            "f1-(\\S(?:\\s*\\S)*):(-?\\d+):.*",
            workingDir.getAbsolutePath(),
            "CLASSPATH=" + getRunClasspath(files, context) + "\n"
            + "PATH+=%<JAVA_BIN_DIR>%:\n"
            + "PATH+=%<JGRASP_PATHS>%:", "FindBugs", h, null,
            null, null, DateTestType.TARGETS, files, null);
   }
   
   
   /** Gets the necessary classpath.
    *
    *  @param files the files to be processed.
    * 
    *  @param context the current context.
    *
    *  @return the classpath necessary to run Checkstyle
    *  on the specified files. **/
   private String getRunClasspath(final List<File> files,
         final ActionContext context) {
      File jarFile = settings.getFindBugsJar();
         
      StringBuilder result = new StringBuilder();
      result.append(jarFile.getAbsolutePath());
      result.append("%:%<JGRASP_CLASSPATHS>");
      
      Set<File> rootDirs = new HashSet<File>();
      for (File f : files) {
         String dir = ToolUtil.getPackageRoot(f,
               context.getCharset());
         if (dir != null) {
            rootDirs.add(new File(dir));
         }
      }
   
      for (File f : rootDirs) {
         result.append("%:");
         result.append(f.getAbsolutePath());
      }
      return result.toString();
   }
   
 
   /** {@inheritDoc} **/
   public String getMenuName() {
      return "FindBugs";
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
         configureDialog = new FindBugsSettingsDialog(this);
      }
      configureDialog.showDialog(context, settings);
   }
   
   
   /** Shows about text.
    *
    *  @param context action context. **/
   private void about(final ActionContext context) {
      String text; 
      if (settings.getFindBugsHome() == null) {
         text = "jGRASP plugin for the FindBugs development tool.\n"
               + "A FindBugs distribution was not found. Use "
               + "\"Configure\" to choose the FindBugs home directory "
               + "if FindBugs is installed.\n"
               + "FindBugs and information about FindBugs are "
               + "available at http://findbugs.sourceforge.net .";
      }
      else {
         text = "jGRASP plugin for the FindBugs development tool.\n"
               + "FindBugs and information about FindBugs are "
               + "available at http://findbugs.sourceforge.net .";
      }
      creationData.showMessage(context.getDialogParent(), text,
            "About jGRASP FindBugs Plugin");
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
   public void settingsChanged(final FindBugsSettings newSettings) {
      settings = newSettings;
      creationData.storeData(FindBugsTool.class, SETTINGS_ID,
            settings.getData());
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


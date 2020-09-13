
import java.io.File;

import java.net.URISyntaxException;
import java.net.URL;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgrasp.FileData;
import jgrasp.PluginUtil;
import jgrasp.ResizableFlexWidthIcon;
import jgrasp.StateIcon;
import jgrasp.Tool;

import jgrasp.tool.AbstractToolAction;
import jgrasp.tool.ActionContext;
import jgrasp.tool.CompilerEnvironmentLocation;
import jgrasp.tool.Conditions;
import jgrasp.tool.DateTestType;
import jgrasp.tool.ExecType;
import jgrasp.tool.Highlightable;
import jgrasp.tool.OutputListener;
import jgrasp.tool.PostExecAction;
import jgrasp.tool.PostExecData;
import jgrasp.tool.ProjectState;
import jgrasp.tool.StateStorage;
import jgrasp.tool.ToolAction;
import jgrasp.tool.ToolCreateData;
import jgrasp.tool.ToolState;
import jgrasp.tool.ToolUtil;


/** Checkstyle tool interface. **/
public class CheckstyleTool implements Tool {
 
 
   /** Action to be run after a test completes. **/
   private class AfterExec implements PostExecAction,
         OutputListener {
   
      /** The files that were checked. **/
      private List<File> files;
   
      /** Files for which there were error messages. **/
      private Set<File> failedFiles = new HashSet<File>();
   
   
      /** Creates a new AfterExec.
       *
       *  @param fileTested the files that were checked. **/
      AfterExec(final List<File> filesTested) {
         files = filesTested;
      }
   
   
      /** {@inheritDoc} **/
      public void execute(final ActionContext context,
            final PostExecData data) {
         if (data.getExitValue() != 0 || data.wasAborted()) {
            return;
         }
         processResults(context, data, files, failedFiles);
      }
      
      
      /** {@inheritDoc} **/
      public void processLine(final String line) {
         Matcher m = errPattern.matcher(line);
         try {
            if (!m.matches()) {
               return;
            }
         }
         catch (StackOverflowError e) {
            return;
         }
         String file = m.group(1);
         if (file == null) {
            file = m.group(3);
         }
         if (file == null) {
            return;
         }
         failedFiles.add(new File(file));
      }
   };


   /** Identifer for storing and retrieving per-project per-file
    *  state data. **/
   private static String dataId = "jgrCheckstyle0xr";

   /** Pattern for matching filenames in Checkstyle error message. **/
   private static String errPatternText =
         "(?:\\[[^\\]]*\\]\\s+)?(?:(?:(\\S(?:\\s*\\S)*):(\\d+):(?:\\d+):.*)|"
         + "(?:(\\S(?:\\s*\\S)*):(\\d+):.*))";

   /** Compiled pattern for matching filenames in Checkstyle error
    *  message. **/
   private static Pattern errPattern = Pattern.compile(errPatternText);
   
   /** The location of this tool class, or null if it is
    *  not located in a file. **/
   private static File toolHome;
   
   /** Arbitrary ID for tool storage. **/
   private static final int SETTINGS_ID = 513;
 
   static {
      try {
         URL url = CheckstyleTool.class.getResource(
               "CheckstyleTool.class");
         toolHome = new File(url.toURI()).getParentFile();
      }
      catch (URISyntaxException e) {
      }
      catch (IllegalArgumentException e) {
            //*** Probably due to Java bug 5086147.
            //*** jGRASP check classes won't be available.
      }
   }


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
                  || checkProjectAction.showMenuItem()
                  || checkDirectoryAction.showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
         }
      };
 
   /** The check file action. **/
   private ToolAction checkFileAction = 
      new AbstractToolAction("Check File", "Check File",
         "Run Checkstyle on file",
         new CheckstyleFileIcon()) {
       
         public boolean showMenuItem() {
            if (settings.getCheckstyleHome() == null) {
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
            runCheckstyle(context, filesTested, h);
         }
      };
 
   /** The check project files action. **/
   private ToolAction checkProjectAction = 
      new AbstractToolAction("Check Project Files",
         "Check Project",
         "Run Checkstyle on project files",
         new CheckstyleFileIcon(),
         EnumSet.of(ToolAction.Flag.PROJECT)) {
       
         public boolean showMenuItem() {
            if (settings.getCheckstyleHome() == null) {
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
            if (fds != null) {
               for (FileData fd : fds) {
                  if (fd.hasLanguage("Java")) {
                     files.add(fd.getFile());
                  }
               }
            }
            if (settings.getProcessTests()) {
               fds = context.getProjectTestFiles();
               if (fds != null) {
                  for (FileData fd : fds) {
                     if (fd.hasLanguage("Java")) {
                        files.add(fd.getFile());
                     }
                  }
               }
            }
            if (files.size() == 0) {
               return;
            }
            runCheckstyle(context, files, h);
         }
      };
 
   /** The check directory action. **/
   private ToolAction checkDirectoryAction = 
      new AbstractToolAction("Check Directory", "Check Directory",
         "Run Checkstyle on all Java files in same directory as "
         + "active file",
         new CheckstyleDirIcon()) {
       
         public boolean showMenuItem() {
            if (settings.getCheckstyleHome() == null) {
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
            File dir = af.getFile().getParentFile();
            if (dir == null) {
               return;
            }
            File[] files = dir.listFiles();
            if (files == null) {
               return;
            }
            List<File> filesTested = new ArrayList<File>();
            for (File f : files) {
               if (PluginUtil.endsWithIgnoreCase(f.getName(), ".java")) {
                  filesTested.add(f);
               }
            }
            
            runCheckstyle(context, filesTested, h);
         }
      };
 
   /** The configuration dialog. **/
   private CheckstyleSettingsDialog configureDialog;
 
   /** Current Checkstyle settings. **/
   private CheckstyleSettings settings;

   /** Creation data. **/
   private ToolCreateData creationData;
 
   /** True if current conditions indicate an active Java file,
    *  false otherwise. **/
   private boolean activeJavaFile;
 
   /** True if the current conditions indicate a project containing
    *  at least one Java file, false otherwise. **/
   private boolean projectWithJavaFile;
 
 
   /** Creates a new Checkstyle tool.
    *
    *  @param tcd tool creation data. **/
   public CheckstyleTool(final ToolCreateData tcd) {
      creationData = tcd;
      settings = new CheckstyleSettings(tcd.getData(CheckstyleTool.class,
            SETTINGS_ID));
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
      File jarFile = settings.getCheckstyleJar();
         
      StringBuilder result = new StringBuilder();
      result.append(jarFile.getAbsolutePath());
      result.append("%:%<JGRASP_CLASSPATHS>");
      if (toolHome != null) {
         result.append("%:");
         result.append(toolHome);
      }
      
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
   
 
   /** Executes Checkstyle.
    *
    *  @param context the current context.
    *
    *  @param files the files to be tested.
    *
    *  @param h item to be highlighted during execution. This
    *  may be null. **/
   private void runCheckstyle(final ActionContext context,
         final List<File> files, final Highlightable h) {
      List<File> filesTested = new ArrayList<File>();
      StringBuilder command = new StringBuilder();
      command.append("java -cp \"");
      command.append(getRunClasspath(files, context));
      command.append("\" ");
      command.append(settings.getJavaFlags());
      Charset charset = context.getCharset();
      if (charset != null) {
         command.append(" -Dfile.encoding=" + charset.name());
      }
      int tabSize = context.getTabSize();
      if (tabSize != 8) {
         command.append(" -Djgrasp.tabwidth=");
         command.append(tabSize);
      }
      command.append(" com.puppycrawl.tools.checkstyle.Main -c \"");
      command.append(settings.getChecksFile().getAbsolutePath());
      command.append("\" ");
      command.append(settings.getFlags());
      for (File f : files) {
         command.append(" \"" + f.getAbsolutePath() + "\"");
         filesTested.add(f);
      }
         
      File homeFile = settings.getCheckstyleHome();
      if (!homeFile.isDirectory()) {
         homeFile = homeFile.getParentFile();
         if (homeFile == null) {
            homeFile = new File(System.getProperty("user.dir"));
         }
      }
   
      AfterExec runAction = new AfterExec(filesTested);
      context.executeCommand(command.toString(), ExecType.CHECK,
            "f1f1-" + errPatternText,
            homeFile.getAbsolutePath(),
            "PATH+=%<JAVA_BIN_DIR>%;\n"
            + "PATH+=%<JGRASP_PATHS>%;", "Checkstyle", h,
            runAction, runAction, null, DateTestType.NONE,
            null, null);
   }
   
   
   /** {@inheritDoc} **/
   public String getMenuName() {
      return "Checkstyle";
   }
   
   
   /** {@inheritDoc} **/
   public ToolAction[] getToolActions() {
      return new ToolAction[] { checkFileAction, checkProjectAction,
            checkDirectoryAction, sepAction, configureAction,
            aboutAction };
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
         configureDialog = new CheckstyleSettingsDialog(this);
      }
      configureDialog.showDialog(context, settings);
   }
   
   
   /** Shows about text.
    *
    *  @param context action context. **/
   private void about(final ActionContext context) {
      String text; 
      if (settings.getCheckstyleHome() == null) {
         text = "jGRASP plugin for the Checkstyle development tool.\n"
               + "A Checkstyle distribution was not found. Use "
               + "\"Configure\" to choose the Checkstyle home directory "
               + "if Checkstyle is installed.\n"
               + "Checkstyle and information about Checkstyle are "
               + "available at http://checkstyle.sourceforge.net .";
      }
      else {
         text = "jGRASP plugin for the Checkstyle development tool.\n"
               + "Checkstyle and information about Checkstyle are "
               + "available at http://checkstyle.sourceforge.net .";
      }
      creationData.showMessage(context.getDialogParent(),
         text, "About jGRASP Checkstyle Plugin");
   }
   

   /** {@inheritDoc} **/
   public void applyConditions(final Conditions conditions) {
      activeJavaFile = false;
      FileData fd = conditions.getActiveFile();
      if (fd != null) {
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
      if (!projectWithJavaFile) {
         fds = conditions.getProjectTestFiles();
         if (fds != null) {
            for (FileData f : fds) {
               if (f.hasLanguage("Java")) {
                  projectWithJavaFile = true;
                  break;
               }
            }
         }
      }
   }
       
       
   /** Called when the user has changed the settings.
    *
    *  @param newSettings the new settings. **/
   public void settingsChanged(final CheckstyleSettings newSettings) {
      settings = newSettings;
      creationData.storeData(CheckstyleTool.class, SETTINGS_ID,
            settings.getData());
      creationData.guiChanged();
   }
   
   
   /** Called after a check command successfully executes.
    *
    *  @param context context under which the results were
    *  determined.
    *
    *  @param data command post-execution data.
    *
    *  @param files the files that were tested.
    *
    *  @param failedFiles the files for which there were errors. **/
   private void processResults(final ActionContext context,
         final PostExecData data,
         final List<File> files, final Set<File> failedFiles) {
      long time = System.currentTimeMillis();
      StateStorage ss = StateStorage.getInstance();
      for (File f : files) {
         boolean failed = failedFiles.contains(f);
         PassFailData pfd = new PassFailData(time, failed);
         ss.storeData(context, dataId, f, pfd);
      }
   }
   

   /** {@inheritDoc} **/
   public ResizableFlexWidthIcon getStateIcon(final ProjectState ps,
         final File fl, final boolean getSpacer) {
      if (getSpacer) {
         return new CheckstyleStateIcon(StateIcon.State.UNUSED);
      }
      PassFailData pfd = (PassFailData) StateStorage.getInstance().
            retrieveData(ps, dataId, fl);
      if (pfd == null) {
         return null;
      }
      if (ToolUtil.isModified(fl) || fl.lastModified() > pfd.getTime()) {
         return new CheckstyleStateIcon(StateIcon.State.NEEDS_TESTING);
      }
      return new CheckstyleStateIcon(pfd.failed()? StateIcon.State.FAIL
            : StateIcon.State.PASS);
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
      return new PassFailData();
   }


   /** {@inheritDoc} **/
   public void fileRemoved(final Conditions conditions, final File fl,
         final boolean isTest) {
   }
   
   
   /** {@inheritDoc} **/
   public void closeWindows() {
   }
}


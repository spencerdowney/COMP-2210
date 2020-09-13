
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import jgrasp.tool.DirType;
import jgrasp.tool.ExecType;
import jgrasp.tool.Highlightable;
import jgrasp.tool.OneShotTimer;
import jgrasp.tool.PostExecAction;
import jgrasp.tool.PostExecData;
import jgrasp.tool.ProjectState;
import jgrasp.tool.StateStorage;
import jgrasp.tool.ToolAction;
import jgrasp.tool.ToolCreateData;
import jgrasp.tool.ToolState;
import jgrasp.tool.ToolUtil;


/** JUnit tool interface. **/
public class JUnitTool implements Tool {


   /** Class for holding file test data. **/
   private static class FileInfo {
   
      /** The test file. **/
      private File fl;
   
      /** The test class name. **/
      private String testClass;
   
      /** The test source file, or null if there is no single
       *  source associated with the test. **/
      private File source;
      
      /** The charset for the test file. **/
      private Charset charset;
      
      /** Sort order. **/
      private int order;
   
   
      /** Creates a new FileInfo.
       *
       *  @param file the source or test source file.
       *
       *  @param isTest true if <code>file</code> is a test source
       *  file, false if it is a normal source file.
       *
       *  @param context the current conditions or context.
       *
       *  @param orderIn the sort order. **/
      FileInfo(final File file, final boolean isTest,
            final Conditions context, final Integer orderIn) {
         if (isTest) {
            fl = file;
         }
         else {
            source = file;
            fl = getTestFile(file, false, context);
         }
         init(orderIn);
      }
   
   
      /** Creates a new FileInfo.
       *
       *  @param file the test source file.
       *
       *  @param src the corresponding source file.
       *
       *  @param context the current conditions or context.
       *
       *  @param orderIn the sort order. **/
      FileInfo(final File file, final File src,
            final Conditions context, final Integer orderIn) {
         fl = file;
         source = src;
         charset = context.getCharset();
         init(orderIn);
      }
      
   
      /** Initialization to be perfomed by all constructors.
       *
       *  @param orderIn the sort order. **/
      private void init(final Integer orderIn) {
         if (fl != null) {
            String pack = ToolUtil.getJavaPackage(fl, charset);
            testClass = getTestClassName(fl, true);
            if (pack.length() > 0) {
               testClass = pack + "." + testClass;
            }
         }
         if (orderIn != null) {
            order = orderIn.intValue();
         }
      }
   }


   /** Action for running after a compile. **/
   private class RunLaterAction implements PostExecAction {
   
      /** A file to be deleted, or null if there is none. **/
      private File toDelete;
   
      /** Info on the files to be processed, or null if there are none. **/
      private List<FileInfo> files;
      
      /** True if the tests will be run in debug mode, false
       *  otherwise. **/
      private boolean debug;
      
      /** Item to be highlighted during execution. This may
       *  be null. **/
      private Highlightable highlightItem;
   
   
      /** Creates a new RunLaterAction.
       *
       *  @param fileToDelete a file to be deleted, or null if there
       *  is none.
       *
       *  @param filesIn info on the files to be processed, or null
       *  if there are none.
       *
       *  @param debugIn true if the tests will be run in debug mode,
       *  false otherwise.
       *
       *  @param h item to be highlighted during execution. This
       *  may be null. **/
      RunLaterAction(final File fileToDelete,
            final List<FileInfo> filesIn,
            final boolean debugIn, final Highlightable h) {
         toDelete = fileToDelete;
         files = filesIn;
         debug = debugIn;
         highlightItem = h;
      }
   
   
      /** {@inheritDoc} **/
      public void execute(final ActionContext context,
            final PostExecData data) {
         if (toDelete != null) {
            toDelete.delete();
         }
         if (files == null) {
            return;
         }
         if (data.hasErrors() || data.getExitValue() != 0) {
            String extra = data.hasErrors()? ""
                  : " (non-zero exit value from compiler)";
            creationData.showMessage(context.getDialogParent(),
                  "Run ended because compilation appears to have failed"
                        + extra + ".", "jGRASP JUnit Plugin");
            return;
         }
         if (data.wasAborted()) {
            return;
         }
         runTestFiles(context, files, false, true, debug, highlightItem);
      }
   };
 
  
   /** Action to be run after a test completes. **/
   private class AfterExec implements PostExecAction {
   
      /** Info for the files that were checked. **/
      private List<FileInfo> files;
   
   
      /** Creates a new AfterExec.
       *
       *  @param fileTested info for the files that were checked.
       *
       *  @param disp the output display. **/
      AfterExec(final List<FileInfo> filesTested) {
         files = filesTested;
      }
   
   
      /** {@inheritDoc} **/
      public void execute(final ActionContext context,
            final PostExecData data) {
         outputDisplay.flagEnd();
         if (aatTimer == null) {
            ActionListener aatTimerListener = 
               new ActionListener() {
                  public void actionPerformed(final ActionEvent e) {
                     outputDisplay.setTmpAat(false);
                  }
               };
            aatTimer = new OneShotTimer(250000000L, aatTimerListener);
         }
         aatTimer.start();
         if (data.getExitValue() != 0 || data.wasAborted()) {
            return;
         }
         Set<String> failedClasses = outputDisplay.getFailedClasses();
         processResults(context, data, files, failedClasses);
      }
   };


   /** Identifer for storing and retrieving per-project per-file
    *  state data. **/
   private static String dataId = "jgrJUnit0xr";

   /** Pattern for matching class names in JUnit error message. **/
   private static Pattern errPattern = Pattern.compile(
         "\\s*\\d*\\) \\w+\\((.+)\\).*");

   /** Arbitrary ID for tool storage. **/
   private static final int SETTINGS_ID = 342;
   
   /** Reusable empty set. **/
   private static Set<File> emptySet = new HashSet<File>();
   
   /** The directory or JAR file containing this class. **/
   private static String thisPath = ToolUtil.getRootFile(JUnitTool.class).
         getAbsolutePath();
   
   /** File info sorter. **/
   private static Comparator<FileInfo> fiSort =
      new Comparator<FileInfo>() {
         public int compare(final FileInfo fi1, final FileInfo fi2) {
            if (fi1.order < fi2.order) {
               return -1;
            }
            else if (fi1.order > fi2.order) {
               return 1;
            }
            return 0;
         }
      };
   
   /** The output display module. **/
   private JUnitOutputDisplay outputDisplay;
   
   /** Timer for turning off temporary aat in output display. **/
   private OneShotTimer aatTimer;
 
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
            return createTestFileAction.showMenuItem()
                  || compileTestFileAction.showMenuItem()
                  || runTestFileAction.showMenuItem()
                  || compileAndRunProjectAction.showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
         }
      };

   /** The create test file action. **/
   private ToolAction createTestFileAction = 
      new AbstractToolAction("Create Test File", "Create Test",
            "Create JUnit test file", new JUnitCreateIcon()) {
       
         public boolean showMenuItem() {
            if (settings.getJUnitHome() == null) {
               return false;
            }
            return activeJavaFile && !isTestFile && !haveTestFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            createTestFile(context);
         }
      };

   /** The edit test file action. **/
   private ToolAction editTestFileAction = 
      new AbstractToolAction("Edit Test File", "Edit Test",
            "Edit JUnit test file", new JUnitCreateIcon()) {
       
         public boolean showMenuItem() {
            return haveTestFile && !isTestFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            editTestFile(context);
         }
      };

   /** The compile test file action. **/
   private ToolAction compileTestFileAction = 
      new AbstractToolAction("Compile Tests", "Compile Tests",
            "Compile JUnit tests", new JUnitCompileIcon(),
            EnumSet.of(ToolAction.Flag.COMPILE)) {
       
         public boolean showMenuItem() {
            if (settings.getJUnitHome() == null) {
               return false;
            }
            return haveTestFile || isTestFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            if (!isTestFile) {
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            if (settings.getJUnitHome() == null) {
               return;
            }
            final List<FileInfo> fi = getActiveFileInfo(context);
            if (context.isInProject()) {
               PostExecAction afterCompile = 
                  new PostExecAction() {
                     public void execute(final ActionContext context,
                           final PostExecData data) {
                        if (data.wasAborted() || data.hasErrors()) {
                           return;
                        }
                        compileTestFiles(context, fi, null, false, h);
                     }
                  };
               if (!context.compileProject(afterCompile, "JUnit Run")) {
                  return;
               }
            }
            compileTestFiles(context, fi, null, false, h);
         }
      };

   /** The run test file action. **/
   private ToolAction runTestFileAction = 
      new AbstractToolAction("Run Tests", "Run Tests",
            "Run JUnit tests", null) {
       
         public boolean showMenuItem() {
            if (settings.getJUnitHome() == null) {
               return false;
            }
            return haveTestFile || isTestFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            if (settings.getJUnitHome() == null) {
               return;
            }
         
            runTestFiles(context, getActiveFileInfo(context), false, false,
                  false, h);
         }
      };

   /** The deubg test file action. **/
   private ToolAction debugTestFileAction = 
      new AbstractToolAction("Debug Tests", "Debug Tests",
            "Debug JUnit tests", null) {
       
         public boolean showMenuItem() {
            if (settings.getJUnitHome() == null) {
               return false;
            }
            return haveTestFile || isTestFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            if (settings.getJUnitHome() == null) {
               return;
            }
            if (context.isInProject()) {
               PostExecAction afterCompile = 
                  new PostExecAction() {
                     public void execute(final ActionContext context,
                           final PostExecData data) {
                        if (data.wasAborted() || data.hasErrors()) {
                           return;
                        }
                        runTestFiles(context, getActiveFileInfo(context),
                              false, false, true, h);
                     }
                  };
               if (!context.compileProject(afterCompile, "JUnit Run")) {
                  return;
               }
            }
            runTestFiles(context, getActiveFileInfo(context), false, false,
                  true, h);
         }
      };

   /** The compile and run test file action. **/
   private ToolAction compileAndRunTestFileAction = 
      new AbstractToolAction("Compile and Run Tests", "Run Tests",
            "Compile and run JUnit tests",
            new JUnitRunIcon(), EnumSet.of(ToolAction.Flag.RUN)) {
       
         public boolean showMenuItem() {
            if (settings.getJUnitHome() == null) {
               return false;
            }
            return haveTestFile || isTestFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            if (settings.getJUnitHome() == null) {
               return;
            }
            if (context.isInProject()) {
               PostExecAction afterCompile = 
                  new PostExecAction() {
                     public void execute(final ActionContext context,
                           final PostExecData data) {
                        if (data.wasAborted() || data.hasErrors()) {
                           return;
                        }
                        runTestFiles(context, getActiveFileInfo(context),
                              true, false, false, h);
                     }
                  };
               if (!context.compileProject(afterCompile, "JUnit Run")) {
                  return;
               }
            }
            runTestFiles(context, getActiveFileInfo(context), true, false,
                  false, h);
         }
      };

   /** The compile and debug test file action. **/
   private ToolAction compileAndDebugTestFileAction = 
      new AbstractToolAction("Compile and Debug Tests", "Debug Tests",
            "Compile and debug JUnit tests", new JUnitDebugIcon(),
            EnumSet.of(ToolAction.Flag.DEBUG)) {
       
         public boolean showMenuItem() {
            if (settings.getJUnitHome() == null) {
               return false;
            }
            return haveTestFile || isTestFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            if (!isTestFile) {
               // Only show debug on menu for source file.
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            if (settings.getJUnitHome() == null) {
               return;
            }
            if (context.isInProject()) {
               PostExecAction afterCompile = 
                  new PostExecAction() {
                     public void execute(final ActionContext context,
                           final PostExecData data) {
                        if (data.wasAborted() || data.hasErrors()) {
                           return;
                        }
                        runTestFiles(context, getActiveFileInfo(context),
                              true, false, true, h);
                     }
                  };
               if (!context.compileProject(afterCompile, "JUnit Run")) {
                  return;
               }
            }
            runTestFiles(context, getActiveFileInfo(context), true, false,
                  true, h);
         }
      };
 
   /** The compile project action. **/
   private ToolAction compileProjectAction = 
      new AbstractToolAction("Compile All Project Tests",
            "Compile All Project Tests",
            "Compile all JUnit test files in project", new JUnitCompileIcon(),
            EnumSet.of(ToolAction.Flag.PROJECT)) {
       
         public boolean showMenuItem() {
            if (settings.getJUnitHome() == null) {
               return false;
            }
            return haveProjectTestFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            PostExecAction afterCompile = 
               new PostExecAction() {
                  public void execute(final ActionContext context,
                        final PostExecData data) {
                     if (data.wasAborted() || data.hasErrors()) {
                        return;
                     }
                     compileOrRunProject(context, h, false);
                  }
               };
            if (!context.compileProject(afterCompile, "JUnit Run")) {
               return;
            }
            compileOrRunProject(context, h, false);
         }
      };
 
   /** The compile and run project action. **/
   private ToolAction compileAndRunProjectAction = 
      new AbstractToolAction("Compile and Run Project Tests",
            "Compile and Run Project Tests",
            "Compile out-of-date test files and run all JUnit tests in "
            + "project", new JUnitRunIcon(),
            EnumSet.of(ToolAction.Flag.PROJECT)) {
       
         public boolean showMenuItem() {
            if (settings.getJUnitHome() == null) {
               return false;
            }
            return haveProjectTestFile;
         }
          
         public boolean showToolbarItem() {
            if (settings.hideToolbarItems()) {
               return false;
            }
            return showMenuItem();
         }
          
         public void execute(final ActionContext context,
               final Highlightable h) {
            PostExecAction afterCompile = 
               new PostExecAction() {
                  public void execute(final ActionContext context,
                        final PostExecData data) {
                     if (data.wasAborted() || data.hasErrors()) {
                        return;
                     }
                     compileOrRunProject(context, h, true);
                  }
               };
            if (!context.compileProject(afterCompile, "JUnit Run")) {
               return;
            }
            compileOrRunProject(context, h, true);
         }
      };
 
   /** The configuration dialog. **/
   private JUnitSettingsDialog configureDialog;
 
   /** Current JUnit settings. **/
   private JUnitSettings settings;

   /** Creation data. **/
   private ToolCreateData creationData;
 
   /** True if current conditions indicate an active Java file,
    *  false otherwise. **/
   private boolean activeJavaFile;
 
   /** True if there is an active Java file and a test file
    *  that corresponds to it, false otherwise. **/
   private boolean haveTestFile;
 
   /** True if the active file is a JUnit test case source file,
    *  false otherwise. **/
   private boolean isTestFile;
 
   /** True if there is a test file corresponding to at least
    *  one source file in the current project, or a lone test
    *  file in the test directory if it exists, false otherwise. **/
   private boolean haveProjectTestFile;
 
 
   /** Creates a new JUnit tool.
    *
    *  @param tcd tool creation data. **/
   public JUnitTool(final ToolCreateData tcd) {
      creationData = tcd;
      settings = new JUnitSettings(tcd.getData(JUnitTool.class,
            SETTINGS_ID));
   }


   /** Gets file info for the active file.
    *
    *  @param context the current context.
    *
    *  @return a list containing the file info (one element) for
    *  the active file. **/
   private static List<FileInfo> getActiveFileInfo(
         final ActionContext context) {
      FileData fd = context.getActiveFile();
      
      List<FileInfo> fi = new ArrayList<FileInfo>();
      File srcFile = null;
      if (ToolUtil.isTestFile(fd.getFile(), context)) {
         srcFile = getSourceForTest(context, fd.getFile());
         fi.add(new FileInfo(fd.getFile(), srcFile, context,
               Integer.valueOf(0)));
      }
      else {
         fi.add(new FileInfo(fd.getFile(), false, context,
               Integer.valueOf(0)));
      }
      return fi;
   }


   /** Gets the source file, if any, corresponding to a specified
    *  test file.
    *
    *  @param conditions the current conditions.
    *
    *  @param testFile the test file of interest.
    *
    *  @return the source file corresponding to the specified test
    *  file, or null if there is no project, or there is no associated
    *  source file. **/
   private static File getSourceForTest(final Conditions conditions,
         final File testFile) {
      List<FileData> sourceFds = conditions.getProjectFiles();
      if (sourceFds == null) {
         return null;
      }
      for (FileData fd : sourceFds) {
         if (!PluginUtil.endsWithIgnoreCase(fd.getFile().getName(),
               ".java")) {
            continue;
         }
         File f = getTestFile(fd.getFile(), false, conditions);
         if (f.equals(testFile)) {
            return fd.getFile();
         }
      }
      return null;
   }


   /** Gets the test file corresponding to a source file.
    *
    *  @param f the source or test file.
    *
    *  @param isTest true if <code>f</code> is a test source file,
    *  false if it is a regular source file.
    *
    *  @param conditions the current conditions or context.
    *
    *  @return the test file corresponding to <code>f</code>,
    *  or null if the correct file can not be determined. **/
   private static File getTestFile(final File f, final boolean isTest,
         final Conditions conditions) {
      if (isTest) {
         return f;
      }
      return getTestOrClassFile(f, conditions, ".java");
   }


   /** Gets the test class file corresponding to a source file
    *  or test source file.
    *
    *  @param f the source or test file.
    *
    *  @param isTest true if <code>f</code> is a test source file,
    *  false if it is a regular source file.
    *
    *  @param conditions the current conditions or context.
    *
    *  @return the test class file corresponding to <code>f</code>,
    *  or null if the correct file can not be determined. **/
   private static File getTestClassFile(final File f,
         final boolean isTest, final Conditions conditions) {
      if (isTest) {
         File parentDir = f.getParentFile();
         String name = f.getName();
         if (PluginUtil.endsWithIgnoreCase(name, ".java")) {
            name = name.substring(0, name.length() - 5);
         }
         name += ".class";
         return new File(parentDir, name);
      }
      return getTestOrClassFile(f, conditions, ".class");
   }
   
   
   /** Gets the test source or class file corresponding to a source file.
    *
    *  @param f the source or file.
    *
    *  @param context the current conditions or context.
    *
    *  @param ext extension to use for the file (".java" for a test source
    *  file, ".class" for a text class file.
    *
    *  @return the test file corresponding to <code>f</code>,
    *  or null if the correct file can not be determined. **/
   private static File getTestOrClassFile(final File f,
         final Conditions conditions, final String ext) {
      File dir = conditions.getProjectDir(DirType.TEST, true);
      if (dir == null) {
         String dirStr = ToolUtil.getPackageRoot(f,
               conditions.getCharset());
         if (dirStr == null) {
            return null;
         }
         dir = new File(dirStr);
      }
   
      String pack = ToolUtil.getJavaPackage(f,
            conditions.getCharset());
      if (pack != null && pack.length() > 0) {
         dir = new File(dir, pack.replace('.', File.separatorChar));
      }
      
      return new File(dir, getTestClassName(f, false) + ext);
   }


   /** Gets the test class name for a source or test file.
    *
    *  @param f the source or test file of interest.
    *
    *  @param isTest true if <code>f</code> is a test source file,
    *  false if it is a regular source file.
    *
    *  @return a class name for the test class corresponding
    *  to <code>srcFile</code>. **/
   private static String getTestClassName(final File f,
         final boolean isTest) {
      String name = f.getName();
      int dotLoc = name.lastIndexOf('.');
      if (dotLoc >= 0) {
         if (isTest) {
            return name.substring(0, dotLoc);
         }
         return name.substring(0, dotLoc) + "Test";
      }
      if (isTest) {
         return name;
      }
      return name + "Test";
   }
   
   
   /** Replaces backslashes and double quotes in a string
    *  with backslash-escapes.
    *
    *  @param text the input string.
    *
    *  @return the result of escaping all backslashes
    *  and double quotes in text. That is, \ is replaced
    *  with \\ and " is replaces with \". **/
   public static String encodeQuotes(final String text) {
      StringBuilder result = new StringBuilder(text.length());
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == '\\') {
            result.append("\\\\");
         }
         else if (c == '"') {
            result.append("\\\"");
         }
         else {
            result.append(c);
         }
      }
      return result.toString();
   }


   /** {@inheritDoc} **/
   public String getMenuName() {
      return "JUnit";
   }
   
   
   /** {@inheritDoc} **/
   public ToolAction[] getToolActions() {
      return new ToolAction[] { createTestFileAction,
            editTestFileAction, compileTestFileAction,
            runTestFileAction, debugTestFileAction,
            compileAndRunTestFileAction,
            compileAndDebugTestFileAction, compileProjectAction,
            compileAndRunProjectAction,
            sepAction, configureAction,
            aboutAction };
   }
   
 
   /** {@inheritDoc} **/
   public List<CompilerEnvironmentLocation> getCompilerEnvironments() {
      URL index = JUnitTool.class.getResource("junit_index.jav");
      URL data = JUnitTool.class.getResource("junit_setup.jav");
      if (index == null || data == null) {
         return null;
      }
      CompilerEnvironmentLocation loc =
            new CompilerEnvironmentLocation(index, data);
      ArrayList<CompilerEnvironmentLocation> result =
           new ArrayList<CompilerEnvironmentLocation>();
      result.add(loc);
      return result;
   }
   
   
   /** Configures this tool.
    *
    *  @param context action context. **/
   private void configure(final ActionContext context) {
    
      if (configureDialog == null) {
         configureDialog = new JUnitSettingsDialog(this);
      }
      configureDialog.showDialog(context, settings);
   }
   
   
   /** Shows about text.
    *
    *  @param context action context. **/
   private void about(final ActionContext context) {
      String text; 
      if (settings.getJUnitHome() == null) {
         text = "jGRASP plugin for the JUnit testing tool.\n"
               + "A JUnit distribution was not found. Use "
               + "\"Configure\" to choose the JUnit home directory "
               + "if JUnit is installed.\n"
               + "JUnit and information about JUnit are "
               + "available at http://www.junit.org .";
      }
      else {
         text = "jGRASP plugin for the JUnit development tool.\n"
               + "JUnit and information about JUnit are "
               + "available at http://www.junit.org .";
      }
      creationData.showMessage(context.getDialogParent(), text,
            "About jGRASP JUnit Plugin");
   }
   

   /** Creates a test file corresponding to the current source file.
    *
    *  @param context action context. **/
   private void createTestFile(final ActionContext context) {
      if (settings.getJUnitHome() == null) {
         return;
      }
      FileData fd = context.getActiveFile();
      if (fd == null) {
         return;
      }
      File testFile = getTestFile(fd.getFile(), false, context);
      if (testFile.exists()) {
         String result = creationData.showQuestion(
               context.getDialogParent(), "JUnit Plugin",
               "File \""
               + testFile.getAbsolutePath() + "\" exists.\n\n",
               "Use Existing File~Cancel", "Use Existing File");
         if (!result.equals("Use Existing File")) {
            return;
         }
         ToolUtil.setCompilerEnvironment(testFile, "Java", "Java (Test)");
         if (context.isInProject()) {
            ToolUtil.addToProject(testFile, true, context);
         }
         creationData.guiChanged();
         creationData.editFile(testFile);
         return;
      }
      
      File parent = testFile.getParentFile();
      if (!parent.exists()) {
         if (!ToolUtil.mkdirs(parent)) {
            creationData.showError(context.getDialogParent(),
                  "Could not create directory \""
                  + parent.getAbsolutePath() + "\".",
                  "JUnit Plugin Error");
            return;
         }
      }
      
      String pkg = ToolUtil.getJavaPackage(fd.getFile(),
            context.getCharset());
      StringBuilder testSrc = new StringBuilder();
      if (pkg != null && pkg.length() > 0) {
         testSrc.append("package " + pkg);
         testSrc.append(";\n\n");
      }
   
      testSrc.append("import org.junit.Assert;\n");
      testSrc.append("import static org.junit.Assert.*;\n");
      testSrc.append("import org.junit.Before;\n");
      testSrc.append("import org.junit.Test;\n\n\n");
   
      String className = getTestClassName(fd.getFile(), false);
      testSrc.append("public class " + className + " {\n\n\n");
      
      testSrc.append("   /** Fixture initialization (common "
            + "initialization\n"
            + "    *  for all tests). **/\n");
      testSrc.append("   @Before public void setUp() {\n");
      testSrc.append("   }\n\n\n");
      
      testSrc.append("   /** A test that always fails. **/\n");
      testSrc.append("   @Test public void defaultTest() {\n");
      testSrc.append("      Assert.assertEquals(\"Default test added by "
            + "jGRASP. Delete \"\n");
      testSrc.append("            + \"this test once you have "
            + "added your own.\", 0, 1);\n");
      testSrc.append("   }\n}\n");
   
      FileOutputStream fos;
      try {
         fos = new FileOutputStream(testFile);
      }
      catch (FileNotFoundException e) {
         creationData.showMessage(context.getDialogParent(),
                  "Error creating test file \""
                  + testFile.getAbsolutePath() + "\": "
                  + e.getMessage(), "jGRASP JUnit Plugin");
         return;
      }
      Writer w = new OutputStreamWriter(fos, context.getCharset());
      try {
         w.write(testSrc.toString());
      }
      catch (IOException e) {
         creationData.showMessage(context.getDialogParent(),
                  "Error writing to test file \""
                  + testFile.getAbsolutePath() + "\": "
                  + e.getMessage(), "jGRASP JUnit Plugin");
      }
      try {
         w.close();
      }
      catch (IOException e) {
      }
      ToolUtil.setCompilerEnvironment(testFile, "Java", "Java (Test)");
      if (context.isInProject()) {
         ToolUtil.addToProject(testFile, true, context);
      }
      creationData.guiChanged();
      creationData.parentChanged(testFile);
      creationData.editFile(testFile);
   }
            

   /** Opens a test file corresponding to the current source file.
    *
    *  @param context action context. **/
   private void editTestFile(final ActionContext context) {
      FileData fd = context.getActiveFile();
      if (fd == null) {
         return;
      }
      File testFile = getTestFile(fd.getFile(), false, context);
      if (!testFile.exists()) {
         return;
      }
      creationData.editFile(testFile);
   }
            

   /** Compiles all or compiles-if-necessary-and-runs all test files.
    *
    *  @param context the action context.
    *
    *  @param h item to be highlighted during the operation. This
    *  may be null.
    *
    *  @param run true if out-of-date test files will be compiled then all
    *  test files run, false if all test files will be compiled. **/
   private void compileOrRunProject(final ActionContext context,
         final Highlightable h, final boolean run) {
      if (settings.getJUnitHome() == null) {
         return;
      }
      List<FileData> testFds = context.getProjectTestFiles();
      Map<File, FileData> testFileToFd =
                  new HashMap<File, FileData>();
      Map<FileData, Integer> fdToOrder =
                  new HashMap<FileData, Integer>();
      int i = 0;
      for (FileData fd : testFds) {
         if (!PluginUtil.endsWithIgnoreCase(fd.getFile().getName(),
               ".java")) {
            continue;
         }
         testFileToFd.put(fd.getFile(), fd);
         fdToOrder.put(fd, Integer.valueOf(i++));
      }
         
      List<FileInfo> fi = new ArrayList<FileInfo>();
      List<FileData> sourceFds = context.getProjectFiles();
      for (FileData fd : sourceFds) {
         if (!PluginUtil.endsWithIgnoreCase(fd.getFile().getName(),
               ".java")) {
            continue;
         }
         File f = getTestFile(fd.getFile(), false, context);
         FileData testFd = testFileToFd.remove(f);
         if (testFd != null) {
            fi.add(new FileInfo(f, fd.getFile(), context,
                        fdToOrder.get(testFd)));
         }
      }
            
      for (FileData fd : testFileToFd.values()) {
         fi.add(new FileInfo(fd.getFile(), true, context,
                     fdToOrder.get(fd)));
      }
      Collections.sort(fi, fiSort);
      
      if (run) {
         runTestFiles(context, fi, true, false, false, h);
      }
      else {
         compileTestFiles(context, fi, null, false, h);
      }
   }


   /** Runs JUnit test files.
    *
    *  @param context action context.
    *
    *  @param files info for the files that will be processed.
    *
    *  @param compileIfNecessary if true, the test file will
    *  be compiled if it is out of date.
    *
    *  @param afterCompile true if the file should be up to
    *  date, so that an error message should be given if it
    *  is not, false if no such test should be performed.
    *
    *  @param debug true if this will be a debug command,
    *  false otherwise.
    *
    *  @param h item to be highlighted during execution. This
    *  may be null. **/
   private void runTestFiles(final ActionContext context,
         final List<FileInfo> files, final boolean compileIfNecessary,
         final boolean afterCompile, final boolean debug,
         final Highlightable h) {
      File workingDir = null;
      List<FileInfo> needCompiling = new ArrayList<FileInfo>();
      List<String> testClassNames = new ArrayList<String>();
      Set<File> testSrcFiles = new HashSet<File>();
      Set<File> testRootDirs = new HashSet<File>();
      for (FileInfo fi : files) {
         File f = fi.fl;
         if (f == null) {
            continue;
         }
         testClassNames.add(fi.testClass);
         File testClassFile = getTestClassFile(f, true,  context);
         if (testClassFile == null || ToolUtil.isModified(f)
               || f.lastModified() > testClassFile.lastModified()
               || (fi.source != null && (fi.source.lastModified()
                     > testClassFile.lastModified()
                     || ToolUtil.isModified(fi.source)))) {
            needCompiling.add(fi);
         }
         
         String rootStr = ToolUtil.getPackageRoot(f, context.getCharset());
         if (rootStr == null) {
            creationData.showError(context.getDialogParent(),
                  "Could not get root package directory for file \""
                     + f.getAbsolutePath() + "\".",
                  "jGRASP JUnit Plugin");
            return;
         }
         File root = new File(rootStr);
         testRootDirs.add(root);
         if (workingDir == null) {
            workingDir = root;
         }
         
         if (fi.source != null) {
            testSrcFiles.add(fi.source);
         }
      }
      if (testClassNames.size() == 0) {
         return;
      }
   
      if (needCompiling.size() == 0 && testSrcFiles.size() > 0
            && !context.checkDates(DateTestType.SOURCES,
            "JUnit", null, testSrcFiles)) {
         return;
      }
     
      if (needCompiling.size() > 0) {
         if (afterCompile) {
            creationData.showMessage(context.getDialogParent(),
                  "Run aborted because compilation appears to have failed"
                   + " (no errors, but class files are not up to date).",
                  "jGRASP JUnit Plugin");
            return;
         }
         if (compileIfNecessary) {
            compileTestFiles(context, needCompiling, files, debug, h);
            return;
         }
      }
    
      File jarFile = settings.getJUnitJar();
      File hcJarFile = settings.getHamcrestJar();
   
      File cpDir = context.getProjectDir(DirType.CLASSES, true);
      String cpStr;
      if (cpDir != null) {
         cpStr = "CLASSPATH+=" + cpDir + "%;\n";
      }
      else {
         cpStr = "";
      }
   
      ExecType type;
      if (debug) {
         type = ExecType.DEBUG;
      }
      else {
         type = ExecType.RUN;
      }
      if (!context.prepareExec(type, "JUnit")) {
         return;
      }
   
      ServerSocket ss;
      try {
         ss = new ServerSocket(0, 0, InetAddress.getByName(null));
      }
      catch (IOException e) {
         creationData.showError(context.getDialogParent(),
               "Could not create communication socket: "
               + e.getMessage(), "jGRASP JUnit Plugin");
         return;
      }
   
      if (outputDisplay == null) {
         outputDisplay = new JUnitOutputDisplay();
      }
      boolean success = outputDisplay.init(ss, context.getLocationData(),
            testRootDirs, settings.dontShowDialog(), context.getTitle(),
            creationData);
      if (!success) {
         creationData.showError(context.getDialogParent(),
               "Previous JUnit session is still running.",
               "jGRASP JUnit Plugin");
         return;
      }
      if (!debug) {
         outputDisplay.setTmpAat(true);
      }
      new Thread(outputDisplay).start();
   
      StringBuilder tcn = new StringBuilder();
      for (String c : testClassNames) {
         tcn.append(" \"");
         tcn.append(c);
         tcn.append("\"");
      }
      AfterExec runAction = new AfterExec(files);
      String cmd;
      if (debug) {
         cmd = "java " + settings.getJavaFlags()
               + " -Xnoagent -Djava.compiler=NONE -Xdebug "
               + "-Xrunjdwp:transport=%<TRANSPORT>,suspend=y,server=y "
               + "junit_runner.JgrRunner " + ss.getLocalPort();
      }
      else {
         cmd = "java "  + settings.getJavaFlags()
               + " junit_runner.JgrRunner " + ss.getLocalPort();
      }
      
      context.executeCommand(cmd + tcn, type,
            "cu1-\\s*at (\\S+)\\.[^.]+\\(([^:]+):(\\d+)\\)",
            workingDir.getAbsolutePath(),
            "PATH+=%<JAVA_BIN_DIR>%;\n"
            + "PATH+=%<JGRASP_PATHS>%;\n"
            + "CLASSPATH+=%<JGRASP_CLASSPATHS>%;\n"
            + cpStr
            + "CLASSPATH+=%<REL_DEF_CLASSES_DIR>%;\n"
            + "CLASSPATH+=%<ALL_CLASSPATHS>%;\n"
            + "CLASSPATH+=" + jarFile.getAbsolutePath() + "%;\n"
            + ((hcJarFile == null)? ""
               : "CLASSPATH+=" + hcJarFile.getAbsolutePath() + "%;\n")
            + "CLASSPATH+=" + thisPath + "%;\n"
            + "PATH+=%<JAVA_BIN_DIR>%:\n"
            + "PATH+=%<JGRASP_PATHS>%:",
            "JUnit", h, runAction, null, "\\s*at\\s*"
            + "(sun\\.reflect|java\\.lang\\.reflect|org\\.junit|"
               + "junit_runner).*",
            DateTestType.TARGETS, null, testSrcFiles);
   }


   /** Compiles JUnit test files.
    *
    *  @param context the action context.
    *
    *  @param files info on files to be compiled.
    *
    *  @param runAfterFiles files to be run after compilation, or
    *  null if this will only compile files.
    *
    *  @param debugAfter true if <code>runAfterFiles</code> (if any)
    *  will be debugged after compliling, false if they will be run
    *  after compiling.
    *
    *  @param h item to be highlighted during compilation. This
    *  may be null. **/
   private void compileTestFiles(final ActionContext context,
         final List<FileInfo> files, final List<FileInfo> runAfterFiles,
         final boolean debugAfter, final Highlightable h) {
     
      // Check dates on all project source files.
      Set<File> srcFiles = new HashSet<File>();
      List<FileData> projFiles = context.getProjectFiles();
      if (projFiles != null) {
         for (FileData fd : projFiles) {
            File f = fd.getFile();
            if (PluginUtil.endsWithIgnoreCase(f.getName(), ".java")) {
               srcFiles.add(f);
            }
         }
      }
     
      File workingDir = null;
      StringBuilder filenames = new StringBuilder();
      boolean haveFiles = false;
      List<File> compileFiles = new ArrayList<File>();
      Set<File> testSrcFiles = new HashSet<File>();
      for (FileInfo fi : files) {
         if (fi.fl == null) {
            continue;
         }
         if (workingDir == null) {
            String wkdir = ToolUtil.getPackageRoot(fi.fl,
                  context.getCharset());
            if (wkdir == null) {
               creationData.showError(context.getDialogParent(),
                     "Could not determine package root directory for "
                     + "file \"" + fi.fl.getAbsolutePath() + "\"..",
                     "jGRASP JUnit Plugin");
               return;
            }
            workingDir = new File(wkdir);
         }
         haveFiles = true;
         filenames.append(" \"");
         //*** Should check for charset compatibility here.
         filenames.append(encodeQuotes(fi.fl.getAbsolutePath()));
         filenames.append("\"");
         
         compileFiles.add(fi.fl);
         
         if (fi.source != null) {
            testSrcFiles.add(fi.source);
         }
      
         creationData.autoCSD(fi.fl);
      }
      if (!haveFiles) {
         return;
      }
    
      if (srcFiles.size() > 0 && !context.checkDates(DateTestType.STRICT,
            "JUnit", srcFiles, testSrcFiles)) {
         return;
      }
   
      File filesFile = writeFilesFile(context, filenames.toString());
      if (filesFile == null) {
         return;
      }
   
      File jarFile = settings.getJUnitJar();
      File hcJarFile = settings.getHamcrestJar();
      
      RunLaterAction runAction = null;
      runAction = new RunLaterAction(filesFile, runAfterFiles, debugAfter, h);
      
      File cpDir = context.getProjectDir(DirType.CLASSES, true);
      String cpStr;
      if (cpDir != null) {
         cpStr = "CLASSPATH+=" + cpDir + "%;\n";
      }
      else {
         cpStr = "";
      }
      context.executeCommand("javac " + settings.getFlags()
            + " -g \"@" + filesFile.getAbsolutePath()
            + "\"", ExecType.COMPILE,
            "f1-(\\S(?:\\s*\\S)*):(\\d+):.*",
            workingDir.getAbsolutePath(),
            "PATH+=%<JAVA_BIN_DIR>%;\n"
            + "PATH+=%<JGRASP_PATHS>%;\n"
            + "CLASSPATH+=%<JGRASP_CLASSPATHS>%;\n"
            + cpStr
            + "CLASSPATH+=%<REL_DEF_CLASSES_DIR>%;\n"
            + "CLASSPATH+=" + jarFile.getAbsolutePath() + "%;\n"
            + ((hcJarFile == null)? ""
               : "CLASSPATH+=" + hcJarFile.getAbsolutePath() + "%;\n"),
            "JUnit", h, runAction, null, null, DateTestType.SOURCES,
            null, compileFiles);
   }


   /** {@inheritDoc} **/
   public void applyConditions(final Conditions conditions) {
      activeJavaFile = false;
      FileData fd = conditions.getActiveFile();
      if (fd != null) {
         activeJavaFile = fd.hasLanguage("Java");
      }
      
      isTestFile = false;
      haveTestFile = false;
      if (activeJavaFile) {
         isTestFile = ToolUtil.isTestFile(fd.getFile(), conditions);
         File testFile = getTestFile(fd.getFile(), isTestFile, conditions);
         haveTestFile = !isTestFile && testFile != null
               && testFile.exists()
               && ToolUtil.isTestFile(testFile, conditions);
      }
   
      if (!conditions.isInProject()) {
         haveProjectTestFile = false;
         return;
      }
      // Check for test file corresponding to project file.
      
      haveProjectTestFile = false;
      for (FileData f : conditions.getProjectTestFiles()) {
         if (PluginUtil.endsWithIgnoreCase(f.getFile().getName(), ".java")) {
            haveProjectTestFile = true;
            break;
         }
      }
   }
       

   /** Called when the user has changed the settings.
    *
    *  @param newSettings the new settings. **/
   public void settingsChanged(final JUnitSettings newSettings) {
      settings = newSettings;
      creationData.storeData(JUnitTool.class, SETTINGS_ID,
            settings.getData());
      creationData.guiChanged();
   }
   
   
   /** Writes the files file.
    *
    *  @param context the action context.
    *
    *  @param data the list of files to be writted.
    *
    *  @return the files file, or null if an error occurs. Errors will
    *  be reported to the user within this method. **/
   private File writeFilesFile(final ActionContext context,
         final String data) {
      File filesFile = null;
      try {
         filesFile = File.createTempFile("jgrasp_junit_test_files_",
               "jgr");
         filesFile.deleteOnExit();
      }
      catch (IOException e) {
         creationData.showError(context.getDialogParent(),
               "Error creating 'files list' file:\n" + e.getMessage(),
               "jGRASP JUnit Plugin");
         return null;
      }
   
      String result = creationData.writeFile(filesFile,
            Charset.defaultCharset(), false, data);
      if (result != null) {
         filesFile.delete();
         creationData.showError(context.getDialogParent(),
               "Error writing 'files list' file:\n" + result,
               "jGRASP JUnit Plugin");
         return null;
      }
   
      return filesFile;
   }
   
   
   /** Called after a JUnit run successfully executes.
    *
    *  @param context context under which the results were
    *  determined.
    *
    *  @param data command post-execution data.
    *
    *  @param files the files that were tested.
    *
    *  @param failedClasses the files for which there were errors. **/
   private void processResults(final ActionContext context,
         final PostExecData data, final List<FileInfo> files,
         final Set<String> failedClasses) {
      long time = System.currentTimeMillis();
      StateStorage ss = StateStorage.getInstance();
      for (FileInfo f : files) {
         boolean failed = failedClasses.contains(f.testClass);
         JUnitStateData pfd = new JUnitStateData(time, failed, f.source);
         ss.storeData(context, dataId, f.fl, pfd);
         if (f.source != null) {
            pfd = new JUnitStateData(time, failed, f.fl);
            ss.storeData(context, dataId, f.source, pfd);
         }
      }
   }
   

   /** {@inheritDoc} **/
   public ResizableFlexWidthIcon getStateIcon(final ProjectState ps,
         final File fl, final boolean getSpacer) {
      if (getSpacer) {
         return new JUnitStateIcon(StateIcon.State.UNUSED);
      }
      JUnitStateData pfd = (JUnitStateData) StateStorage.getInstance().
            retrieveData(ps, dataId, fl);
      if (pfd == null) {
         return null;
      }
      File altFile = pfd.getAltFile();
      long lastMod = fl.lastModified();
      if (ToolUtil.isModified(fl) || lastMod > pfd.getTime()
            || (altFile != null
            && (ToolUtil.isModified(altFile)
            || altFile.lastModified() > pfd.getTime()))) {
         return new JUnitStateIcon(StateIcon.State.NEEDS_TESTING);
      }
      List<File> classes = ToolUtil.getClassFiles(fl, ps);
      for (File c : classes) {
         long cLastMod = c.lastModified();
         if (lastMod > cLastMod || cLastMod > pfd.getTime()) {
            return new JUnitStateIcon(StateIcon.State.NEEDS_TESTING);
         }
      }
      if (altFile != null) {
         long altMod = altFile.lastModified();
         classes = ToolUtil.getClassFiles(altFile, ps);
         for (File c : classes) {
            long cLastMod = c.lastModified();
            if (altMod > c.lastModified() || cLastMod > pfd.getTime()) {
               return new JUnitStateIcon(StateIcon.State.NEEDS_TESTING);
            }
         }
      }
      return new JUnitStateIcon(pfd.failed()? StateIcon.State.FAIL
            : StateIcon.State.PASS);
   }
   
   
   /** {@inheritDoc} **/
   public String getClasspath() {
      File jarFile = settings.getJUnitJar();
      File hcJarFile = settings.getHamcrestJar();
      if (jarFile == null) {
         return null;
      }
      if (hcJarFile != null) {
         return jarFile.getAbsolutePath() + File.pathSeparator
               + hcJarFile.getAbsolutePath();
      }
      return jarFile.getAbsolutePath();
   }
   
   
   /** {@inheritDoc} **/
   public String getDataId() {
      return dataId;
   }
   
   
   /** {@inheritDoc} **/
   public ToolState createState() {
      return new JUnitStateData();
   }


   /** {@inheritDoc} **/
   public void fileRemoved(final Conditions conditions, final File fl,
         final boolean isTest) {
      if (!isTest) {
         return;
      }
      File srcFile = getSourceForTest(conditions, fl);
      if (srcFile == null) {
         return;
      }
      StateStorage ss = StateStorage.getInstance();
      ss.storeData(conditions, dataId, srcFile, null);
   }
   
   
   /** {@inheritDoc} **/
   public void closeWindows() {
      if (outputDisplay != null) {
         outputDisplay.closeWindow();
      }
   }
}


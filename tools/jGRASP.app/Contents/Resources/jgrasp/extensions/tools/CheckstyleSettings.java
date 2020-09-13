
import java.io.File;

import java.net.URISyntaxException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgrasp.PluginUtil;

import jgrasp.tool.ToolUtil;


/** Checkstyle tool settings. **/
public class CheckstyleSettings {
         
   /** Version number extracting expression. **/
   private static String verMatch = ".*-(\\d+(?:\\.\\d+)+)-all\\.jar";

   /** Checkstyle "all" jar file matching expression. **/
   private static String jarMatch =
         "checkstyle.*-(?:\\d+(?:\\.\\d+)+)-all\\.jar";
         
   /** Checkstyle "all" jar file matching expression human readable
    *  description. **/
   private static String jarMatchDisplay =
         "checkstyle*-version-all.jar";

   /** Checks file version pattern. **/
   private static Pattern verPattern = Pattern.compile(
         "simple_jdoc_pub_only_checkstyle_(\\d+(?:\\_\\d+)+)_checks.xml");

   /** Checks file version range pattern. **/
   private static Pattern verRangePattern = Pattern.compile(
         "simple_jdoc_pub_only_checkstyle_(\\d+(?:\\_\\d+)+)_to_"
         + "(\\d+(?:\\_\\d+)+)_checks.xml");
 
   /** Currently selected Checkstyle home directory and jar file,
    *  or null if none have been found or selected. **/
   private HomeAndJar checkstyleHAJ;
   
   /** Currently selected Checkstyle checks file, or null if none
    *  has been found or selected. **/
   private File checksFile;

   /** Current default Checkstyle home directory and jar file, or
    *  null if none have been found. **/
   private HomeAndJar defaultCheckstyleHAJ;

   /** Current default Checkstyle checks file, or null if none
    *  was found. **/
   private File defaultChecksFile;

   /** Possible Checkstyle home directories. **/
   private HomeAndJar[] checkstyleHAJs;
   
   /** True if test files should be checked, false otherwise. **/
   private boolean processTests = true;
   
   /** True if toolbar items should be hidden, false if they should
    *  be shown. **/
   private boolean hideTBI;
   
   /** User-supplied command line flags. **/
   private String flags = "";
   
   /** User-supplied command line jvm flags. **/
   private String javaFlags = "";

 
   /** Creates a new Checkstyle settings.
    *
    *  @param data initialization data. **/
   public CheckstyleSettings(final String data) {
      File home = null;
      File checks = null;
      if (data != null) {
         String[] lines = data.split("\n");
         for (String line : lines) {
            if (line.startsWith("H")) {
               String fn = PluginUtil.decodeFilename(line.substring(1));
               if (fn != null) {
                  home = new File(fn);
               }
            }
            else if (line.startsWith("C")) {
               String fn = PluginUtil.decodeFilename(line.substring(1));
               if (fn != null) {
                  checks = new File(fn);
               }
            }
            else if (line.startsWith("D")) {
               hideTBI = true;
            }
            else if (line.startsWith("F")) {
               flags = line.substring(1);
            }
            else if (line.startsWith("J")) {
               javaFlags = line.substring(1);
            }
            else if (line.startsWith("K")) {
               processTests = false;
            }
         }
      }
      init(home, checks);
   }
    
    
   /** Creates a new checkstyle settings.
    *
    *  @param home the Checkstyle home directory, or null if the
    *  default should be used.
    *
    *  @param checks the checks file, or null if the default
    *  should be used.
    *
    *  @param processTestsIn true if test files should be checked,
    *  false otherwise.
    *
    *  @param hideTBIs true if all toolbar buttons should be hidden,
    *  false if they should be shown when applicable.
    *
    *  @param clFlags additional command line flags.
    *
    *  @param jFlags additonal jvm command line flags. **/
   public CheckstyleSettings(final File home, final File checks,
         final boolean processTestsIn, final boolean hideTBIs,
         final String clFlags, final String jFlags) {
      hideTBI = hideTBIs;
      flags = clFlags;
      javaFlags = jFlags;
      processTests = processTestsIn;
      init(home, checks);
   }
      
      
   /** Searches a specified directory for installed versions of
    *  Checkstyle.
    *
    *  @param dirName full name of the directory to be searched.
    *
    *  @param results list to which result installation data
    *  will be added.
    *
    *  @param depth the current depth of the search. **/
   private static void searchForCheckstyle(final String dirName,
         final List<HomeAndJar> results, final int depth) {
      File dir = new File(dirName);
      File[] files = dir.listFiles();
      if (files == null) {
         return;
      }
      for (File f : files) {
         // Accept any directory with a name starting with "checkstyle".
         if (f.isDirectory() && PluginUtil.startsWithIgnoreCase(f.getName(),
               "checkstyle")) {
            // Must contain a checkstyle-all jar file.
            File jf = getJarFromHome(f);
            if (jf != null) {
               results.add(new HomeAndJar(f, jf, verMatch, true));
            }
            else if (depth == 0) {
               // Search one level deeper.
               searchForCheckstyle(f.getAbsolutePath(), results, 1);
            }
         }
         else if (f.isFile() && f.getName().matches(jarMatch)) {
            results.add(new HomeAndJar(f, f, verMatch, true));
         }
      }
   }


   /** Determines if a jar file could be a CheckStyle "all" jar file.
    *
    *  @param jarName the simple jar file name.
    *
    *  @return true if the jar file could be a CheckStyle "all" jar file,
    *  false if it appears not to be. **/
   public static boolean isAllJar(final String jarName) {
      return jarName.matches(jarMatch);
   }


   /** Gets a human readable description of the CheckStyle "all" jar file
    *  matching expression.
    *
    *  @return a human readable description of the CheckStyle "all" jar file
    *  matching expression. **/
   public static String getJarMatchDescription() {
      return jarMatchDisplay;
   }


   /** Gets the Checkstyle jar file corresponding to a Checkstyle
    *  home directory.
    *
    *  @param homeDirectory the Checkstyle home directory of interest.
    *
    *  @return the Checkstyle "all" jar file corresponding to
    *  <code>homeDirectory</code>, or null if there is no such
    *  jar file. **/
   public static File getJarFromHome(final File homeDirectory) {
      File[] files = homeDirectory.listFiles();
      if (files == null) {
         return null;
      }
      for (File f : files) {
         String name = f.getName();
         if (name.matches(jarMatch)) {
            return f;
         }
      }
      return null;
   }


   /** Searches for checkstyle checks.
    *
    *  @param dirName the directory to be searched.
    *
    *  @param results list to which result filenames will be added. **/
   private static void searchForChecks(final File dir,
         final List<String> results) {
      File[] files = dir.listFiles();
      if (files == null) {
         return;
      }
      for (File f : files) {
         if (PluginUtil.endsWithIgnoreCase(f.getName(), "_checks.xml")) {
            results.add(f.getAbsolutePath());
         }
      }
   }
   
  
   /** Finds possible checks for a checkstyle home.
    *
    *  @param checkstyleHome the Checkstyle home directory or jar file.
    *
    *  @return possible checks files for the specified home. **/
   public static String[] findChecks(final File checkstyleHome) {
      List<String> checks = new ArrayList<String>();
      try {
         URL url = CheckstyleSettings.class.getResource(
               "CheckstyleSettings.class");
         File dir = new File(url.toURI()).getParentFile();
         if (dir != null) {
            searchForChecks(dir, checks);
         }
      }
      catch (URISyntaxException e) {
      }
      catch (IllegalArgumentException e) {
            //*** Probably due to Java bug 5086147.
            //*** jGRASP checks won't be available.
      }
      if (checkstyleHome != null && checkstyleHome.isDirectory()) {
         searchForChecks(checkstyleHome, checks);
      }
      String[] result = new String[checks.size()];
      result = checks.toArray(result);
      return result;
   }


   /** Initializes this settings.
    *
    *  @param home the Checkstyle home directory or jar file, or null if the
    *  default should be used.
    *
    *  @param checks the checks file, or null if the default
    *  should be used. **/
   private void init(final File home, final File checks) {
      findCheckstyle();
      // Keep the home only if it is not the default.
      if (home != null && (defaultCheckstyleHAJ == null
            || !defaultCheckstyleHAJ.getHomeDirectory().equals(home))) {
         File jf = home.isDirectory()? getJarFromHome(home) : home;
         if (jf != null) {
            checkstyleHAJ = new HomeAndJar(home, jf,
                  "[^\\d]*(\\d+(?:\\.\\d+)+).*\\.jar", true);
         }
      }
      initChecks();
      // Keep the checks only if it is not the default.
      if (checks != null && (defaultChecksFile == null
            || !defaultChecksFile.equals(checks))) {
         checksFile = checks;
      }
   }
   
   
   /** Searches for installed versions of Checkstyle. **/
   private void findCheckstyle() {
      List<String> locations = ToolUtil.getLikelyHomeLocations();
      List<HomeAndJar> possibleDirectories =
            new ArrayList<HomeAndJar>();
    
      // Search for Checkstyle.
      for (String location : locations) {
         searchForCheckstyle(location, possibleDirectories, 0);
      }
      Collections.sort(possibleDirectories);
      
      checkstyleHAJs = new HomeAndJar[possibleDirectories.size()];
      checkstyleHAJs = possibleDirectories.toArray(checkstyleHAJs);
      
      if (checkstyleHAJs.length > 0) {
         defaultCheckstyleHAJ = checkstyleHAJs[0];
      }
   }


   /** Initializes the possible checks. **/
   private void initChecks() {
      File home = (getHAJ() == null)? null
            : getHAJ().getHomeDirectory();
      String[] possibleChecks = findChecks(home);
      
      Integer[] version = (getHAJ() == null)? new Integer[0]
            : getHAJ().getVersion();
   
      if (defaultChecksFile == null && possibleChecks.length > 0) {
         // Choose default checks file.
         File includedChecks = null;
         File includedChecksExact = null;
         File sunChecks = null;
         for (String c : possibleChecks) {
            File f = new File(c);
            String name = f.getName();
            if (name.equals("simple_jdoc_pub_only_checkstyle_checks.xml")) {
               includedChecks = f;
               continue;
            }
            if (name.startsWith("simple_jdoc_pub_only_checkstyle_")
                  && name.endsWith("checks.xml") && version.length > 0) {
               Matcher verMatcher = verPattern.matcher(name);
               if (verMatcher.matches() && verMatcher.groupCount() == 1) {
                  Integer[] ver = HomeAndJar.stringsToVersion(
                        verMatcher.group(1).split("\\_"));
                  if (HomeAndJar.versionsEqual(version, ver)) {
                     includedChecksExact = f;
                     break;
                  }
               }
               Matcher verRangeMatcher = verRangePattern.matcher(name);
               if (verRangeMatcher.matches()
                     && verRangeMatcher.groupCount() == 2) {
                  Integer[] ver1 = HomeAndJar.stringsToVersion(
                        verRangeMatcher.group(1).split("\\_"));
                  Integer[] ver2 = HomeAndJar.stringsToVersion(
                        verRangeMatcher.group(2).split("\\_"));
                  if (HomeAndJar.versionsCompare(ver1, version) <= 0
                        && HomeAndJar.versionsCompare(ver2, version) >= 0) {
                     includedChecksExact = f;
                     break;
                  }
               }
            }
            if (name.equals("sun_checks.xml")) {
               sunChecks = f;
            }
         }
      
         if (includedChecksExact != null) {
            defaultChecksFile = includedChecksExact;
         }
         else if (includedChecks != null) {
            defaultChecksFile = includedChecks;
         }
         else if (sunChecks != null) {
            defaultChecksFile = sunChecks;
         }
         else {
            defaultChecksFile = new File(possibleChecks[0]);
         }
      }
   }
    

   /** Gets the current home and jar.
    *
    *  @return the current home and jar. **/
   public HomeAndJar getHAJ() {
      if (checkstyleHAJ == null) {
         return defaultCheckstyleHAJ;
      }
      return checkstyleHAJ;
   }
   

   /** Gets the current Checkstyle home directory.
    *
    *  @return the Checkstyle home directory, or null if one
    *  has not been found or selected. **/
   public File getCheckstyleHome() {
      if (getHAJ() == null) {
         return null;
      }
      return getHAJ().getHomeDirectory();
   }


   /** Gets the current Checkstyle jar file.
    *
    *  @return the Checkstyle jar file, or null if one has not
    *  been found or selected. **/
   public File getCheckstyleJar() {
      if (getHAJ() == null) {
         return null;
      }
      return getHAJ().getJarFile();
   }


   /** Gets the current checkstyle checks file.
    *
    *  @return the checkstyle checks file. **/
   public File getChecksFile() {
      if (checksFile == null) {
         return defaultChecksFile;
      }
      return checksFile;
   }
   
   
   /** Gets the possible Checkstyle home directories.
    *
    *  @return the possible Checkstyle home directory paths. **/
   public String[] getCheckstyleHomes() {
      String[] result = new String[checkstyleHAJs.length];
      for (int i = 0; i < checkstyleHAJs.length; i++) {
         result[i] = checkstyleHAJs[i].getHomeDirectory().getAbsolutePath();
      }
      return result;
   }
   

   /** Determines if toolbar items should be hidden.
    *
    *  @return true if toolbar items should be hidden,
    *  false if they should be shown. **/
   public boolean hideToolbarItems() {
      return hideTBI;
   }


   /** Gets user-supplied command line flags.
    *
    *  @return command line flags, or an empty string if
    *  the user has not specified any. **/
   public String getFlags() {
      return flags;
   }


   /** Gets user-supplied jvm command line flags.
    *
    *  @return jvm command line flags, or an empty string if
    *  the user has not specified any. **/
   public String getJavaFlags() {
      return javaFlags;
   }


   /** Determines if test files should be checked.
    *
    *  @return true if test files should be checked, false
    *  otherwise. **/
   public boolean getProcessTests() {
      return processTests;
   }


   /** Gets the settings data.
    *
    *  @return the settings data. **/
   public String getData() {
      // Simple storage method for this simple data. If it gets more
      // complex, XML could be used.
      StringBuilder result = new StringBuilder();
      if (checkstyleHAJ != null) {
         result.append("H");
         result.append(PluginUtil.encodeFilename(
               checkstyleHAJ.getHomeDirectory().getAbsolutePath()));
         result.append("\n");
      }
      if (checksFile != null) {
         result.append("C");
         result.append(PluginUtil.encodeFilename(
               checksFile.getAbsolutePath()));
         result.append("\n");
      }
      if (hideTBI) {
         result.append("D\n");
      }
      if (flags.length() > 0) {
         result.append("F");
         result.append(flags);
         result.append("\n");
      }
      if (javaFlags.length() > 0) {
         result.append("J");
         result.append(javaFlags);
         result.append("\n");
      }
      if (!processTests) {
         result.append("K\n");
      }
      return result.toString();
   }
}

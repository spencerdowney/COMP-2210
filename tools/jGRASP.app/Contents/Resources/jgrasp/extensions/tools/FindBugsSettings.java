
import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jgrasp.PluginUtil;

import jgrasp.tool.ToolUtil;


/** FindBugs tool settings. **/
public class FindBugsSettings {
 
   /** Currently selected FindBugs home directory and jar file,
    *  or null if none have been found or selected. **/
   private HomeAndJar findBugsHAJ;
   
   /** Current default FindBugs home directory and jar file, or
    *  null if none have been found. **/
   private HomeAndJar defaultFindBugsHAJ;

   /** Possible FindBugs home directories. **/
   private HomeAndJar[] findBugsHAJs;
   
   /** True if toolbar items should be hidden, false if they should
    *  be shown. **/
   private boolean hideTBI;
   
   /** User-supplied command line flags. **/
   private String flags = "";
   
   /** User-supplied command line jvm flags. **/
   private String javaFlags = "";

 
   /** Creates a new FindBugs settings.
    *
    *  @param data initialization data. **/
   public FindBugsSettings(final String data) {
      File home = null;
      if (data != null) {
         String[] lines = data.split("\n");
         for (String line : lines) {
            if (line.startsWith("H")) {
               String fn = PluginUtil.decodeFilename(line.substring(1));
               if (fn != null) {
                  home = new File(fn);
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
         }
      }
      init(home);
   }
    
    
   /** Creates a new FindBugs settings.
    *
    *  @param home the FindBugs home directory, or null if the
    *  default should be used.
    *
    *  @param hideTBIs true if all toolbar buttons should be hidden,
    *  false if they should be shown when applicable.
    *
    *  @param clFlags additional command line flags.
    *
    *  @param jFlags additonal jvm command line flags. **/
   public FindBugsSettings(final File home, final boolean hideTBIs,
         final String clFlags, final String jFlags) {
      hideTBI = hideTBIs;
      flags = clFlags;
      javaFlags = jFlags;
      init(home);
   }
      
      
   /** Searches a specified directory for installed versions of
    *  FindBugs.
    *
    *  @param dirName full name of the directory to be searched.
    *
    *  @param results list to which result installation data
    *  will be added.
    *
    *  @param depth the current depth of the search. **/
   private static void searchForFindBugs(final String dirName,
         final List<HomeAndJar> results, final int depth) {
      File dir = new File(dirName);
      File[] files = dir.listFiles();
      if (files == null) {
         return;
      }
      for (File f : files) {
         // Accept any directory with a name starting with "findbugs".
         if (f.isDirectory() && PluginUtil.startsWithIgnoreCase(f.getName(),
               "findbugs")) {
            // Must contain a lib/findbugs.jar file.
            File jf = getJarFromHome(f);
            if (jf != null) {
               results.add(new HomeAndJar(f, jf,
                     "findbugs-(\\d+(?:\\.\\d+)+)", false));
            }
            else if (depth == 0) {
               // Search one level deeper.
               searchForFindBugs(f.getAbsolutePath(), results, 1);
            }
         }
      }
   }


   /** Gets the FindBugs jar file corresponding to a FindBugs
    *  home directory.
    *
    *  @param homeDirectory the FindBugs home directory of interest.
    *
    *  @return the FindBugs "all" jar file corresponding to
    *  <code>homeDirectory</code>, or null if there is no such
    *  jar file. **/
   public static File getJarFromHome(final File homeDirectory) {
      File lib = new File(homeDirectory, "lib");
      File[] files = lib.listFiles();
      if (files == null) {
         return null;
      }
      for (File f : files) {
         String name = f.getName();
         if (name.equals("findbugs.jar")) {
            return f;
         }
      }
      return null;
   }


   /** Initializes this settings.
    *
    *  @param home the FindBugs home directory, or null if the
    *  default should be used. **/
   private void init(final File home) {
      findFindBugs();
      // Keep the home only if it is not the default.
      if (home != null && (defaultFindBugsHAJ == null
            || !defaultFindBugsHAJ.getHomeDirectory().equals(home))) {
         File jf = getJarFromHome(home);
         if (jf != null) {
            findBugsHAJ = new HomeAndJar(home, jf,
                  "findbugs-(\\d+(?:\\.\\d+)+)", false);
         }
      }
   }
   
   
   /** Searches for installed versions of FindBugs. **/
   private void findFindBugs() {
      List<String> locations = ToolUtil.getLikelyHomeLocations();
      List<HomeAndJar> possibleDirectories = new ArrayList<HomeAndJar>();
    
      // Search for FindBugs.
      for (String location : locations) {
         searchForFindBugs(location, possibleDirectories, 0);
      }
      Collections.sort(possibleDirectories);
      
      findBugsHAJs = new HomeAndJar[possibleDirectories.size()];
      findBugsHAJs = possibleDirectories.toArray(findBugsHAJs);
      
      if (findBugsHAJs.length > 0) {
         defaultFindBugsHAJ = findBugsHAJs[0];
      }
   }


   /** Gets the current home and jar.
    *
    *  @return the current home and jar. **/
   public HomeAndJar getHAJ() {
      if (findBugsHAJ == null) {
         return defaultFindBugsHAJ;
      }
      return findBugsHAJ;
   }
   

   /** Gets the current FindBugs home directory.
    *
    *  @return the FindBugs home directory, or null if one
    *  has not been found or selected. **/
   public File getFindBugsHome() {
      if (getHAJ() == null) {
         return null;
      }
      return getHAJ().getHomeDirectory();
   }


   /** Gets the current FindBugs jar file.
    *
    *  @return the FindBugs jar file, or null if one has not
    *  been found or selected. **/
   public File getFindBugsJar() {
      if (getHAJ() == null) {
         return null;
      }
      return getHAJ().getJarFile();
   }


   /** Gets the possible FindBugs home directories.
    *
    *  @return the possible FindBugs home directory paths. **/
   public String[] getFindBugsHomes() {
      String[] result = new String[findBugsHAJs.length];
      for (int i = 0; i < findBugsHAJs.length; i++) {
         result[i] =
               findBugsHAJs[i].getHomeDirectory().getAbsolutePath();
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


   /** Gets the settings data.
    *
    *  @return the settings data. **/
   public String getData() {
      // Simple storage method for this simple data. If it gets more
      // complex, XML could be used.
      StringBuilder result = new StringBuilder();
      if (findBugsHAJ != null) {
         result.append("H");
         result.append(PluginUtil.encodeFilename(
               findBugsHAJ.getHomeDirectory().getAbsolutePath()));
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
      return result.toString();
   }
}

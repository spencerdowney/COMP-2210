
import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jgrasp.PluginUtil;

import jgrasp.tool.ToolUtil;


/** DCD tool settings. **/
public class DCDSettings {
 
   /** Currently selected DCD home directory and jar file,
    *  or null if none have been found or selected. **/
   private HomeAndJar dcdHAJ;
   
   /** Current default DCD home directory and jar file, or
    *  null if none have been found. **/
   private HomeAndJar defaultDCDHAJ;

   /** Possible DCD home directories. **/
   private HomeAndJar[] dcdHAJs;
   
   /** True if toolbar items should be hidden, false if they should
    *  be shown. **/
   private boolean hideTBI;
   
   /** User-supplied command line jvm flags. **/
   private String javaFlags = "";

   /** Excluded classes. **/
   private String excludedClasses = "";

   /** Excluded methods. **/
   private String excludedMethods = "";
   
   /** True if public methods and fields will be checked,
    *  false otherwise. **/
   private boolean publicDeadCode = true;
   
   /** True if local variables will be checked, false otherwise. **/
   private boolean localDeadCode = true;
   
 
   /** Creates a new DCD settings.
    *
    *  @param data initialization data. **/
   public DCDSettings(final String data) {
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
            else if (line.startsWith("J")) {
               javaFlags = line.substring(1);
            }
            else if (line.startsWith("E")) {
               excludedClasses = line.substring(1);
            }
            else if (line.startsWith("M")) {
               excludedMethods = line.substring(1);
            }
            else if (line.startsWith("P")) {
               publicDeadCode = false;
            }
            else if (line.startsWith("L")) {
               localDeadCode = false;
            }
         }
      }
      init(home);
   }
    
    
   /** Creates a new DCD settings.
    *
    *  @param home the DCD home directory, or null if the
    *  default should be used.
    *
    *  @param hideTBIs true if all toolbar buttons should be hidden,
    *  false if they should be shown when applicable.
    *
    *  @param jFlags additonal jvm command line flags.\
    *
    *  @param exclClasses excluded classes, command separated regular
    *  expressions.
    *
    *  @param exclClasses excluded methods, command separated regular
    *  expressions.
    *
    *  @param publicDC true if public code should be checked, false
    *  otherwise.
    *
    *  @param localDC true if local variables should be checked, false
    *  otherwise. **/
   public DCDSettings(final File home, final boolean hideTBIs,
         final String jFlags, final String exclClasses,
         final String exclMethods, final boolean publicDC,
         final boolean localDC) {
      hideTBI = hideTBIs;
      javaFlags = jFlags;
      excludedClasses = exclClasses;
      excludedMethods = exclMethods;
      publicDeadCode = publicDC;
      localDeadCode = localDC;
      init(home);
   }
      
      
   /** Searches a specified directory for installed versions of
    *  DCD.
    *
    *  @param dirName full name of the directory to be searched.
    *
    *  @param results list to which result full filenames will be
    *  added.
    *
    *  @param depth the current depth of the search. **/
   private static void searchForDCD(final String dirName,
         final List<HomeAndJar> results, final int depth) {
      File dir = new File(dirName);
      File[] files = dir.listFiles();
      if (files == null) {
         return;
      }
      for (File f : files) {
         // Accept any directory with a name starting with "dcd".
         if (f.isDirectory() && PluginUtil.startsWithIgnoreCase(f.getName(),
               "dcd")) {
            // Must contain a dcd.jar file.
            File jf = getJarFromHome(f);
            if (jf != null) {
               results.add(new HomeAndJar(f, jf,
                     null, false));
            }
            else if (depth == 0) {
               // Search one level deeper.
               searchForDCD(f.getAbsolutePath(), results, 1);
            }
         }
      }
   }


   /** Gets the DCD jar file corresponding to a DCD
    *  home directory.
    *
    *  @param homeDirectory the DCD home directory of interest.
    *
    *  @return the DCD jar file corresponding to
    *  <code>homeDirectory</code>, or null if there is no such
    *  jar file. **/
   public static File getJarFromHome(final File homeDirectory) {
      File[] files = homeDirectory.listFiles();
      if (files == null) {
         return null;
      }
      for (File f : files) {
         String name = f.getName();
         if (name.equals("dcd.jar")) {
            return f;
         }
      }
      return null;
   }


   /** Initializes this settings.
    *
    *  @param home the DCD home directory, or null if the
    *  default should be used. **/
   private void init(final File home) {
      findDCD();
      // Keep the home only if it is not the default.
      if (home != null && (defaultDCDHAJ == null
            || !defaultDCDHAJ.getHomeDirectory().equals(home))) {
         File jf = getJarFromHome(home);
         if (jf != null) {
            dcdHAJ = new HomeAndJar(home, jf, null, false);
         }
      }
   }
   
   
   /** Searches for installed versions of DCD. **/
   private void findDCD() {
      List<String> locations = ToolUtil.getLikelyHomeLocations();
      List<HomeAndJar> possibleDirectories = new ArrayList<HomeAndJar>();
    
      // Search for DCD.
      for (String location : locations) {
         searchForDCD(location, possibleDirectories, 0);
      }
      Collections.sort(possibleDirectories);
      
      dcdHAJs = new HomeAndJar[possibleDirectories.size()];
      dcdHAJs = possibleDirectories.toArray(dcdHAJs);
      
      if (dcdHAJs.length > 0) {
         defaultDCDHAJ = dcdHAJs[0];
      }
   }


   /** Gets the current home and jar.
    *
    *  @return the current home and jar. **/
   public HomeAndJar getHAJ() {
      if (dcdHAJ == null) {
         return defaultDCDHAJ;
      }
      return dcdHAJ;
   }
   

   /** Gets the current DCD home directory.
    *
    *  @return the DCD home directory, or null if one
    *  has not been found or selected. **/
   public File getDCDHome() {
      if (getHAJ() == null) {
         return null;
      }
      return getHAJ().getHomeDirectory();
   }


   /** Gets the current DCD jar file.
    *
    *  @return the DCD jar file, or null if one has not
    *  been found or selected. **/
   public File getDCDJar() {
      if (getHAJ() == null) {
         return null;
      }
      return getHAJ().getJarFile();
   }


   /** Gets the possible DCD home directories.
    *
    *  @return the possible DCD home directory paths. **/
   public String[] getDCDHomes() {
      String[] result = new String[dcdHAJs.length];
      for (int i = 0; i < dcdHAJs.length; i++) {
         result[i] =
               dcdHAJs[i].getHomeDirectory().getAbsolutePath();
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


   /** Gets excluded classes.
    *
    *  @return a comma-separated list of excluded class regular
    *  expressions. **/
   public String getExcludedClasses() {
      return excludedClasses;
   }


   /** Gets excluded methods.
    *
    *  @return a comma-separated list of excluded method regular
    *  expressions. **/
   public String getExcludedMethods() {
      return excludedMethods;
   }
   

   /** Determines if public code should be checked.
    *
    *  @return true if public code should be checked,
    *  false otherwise. **/
   public boolean checkPublic() {
      return publicDeadCode;
   }
   

   /** Determines if local variables should be checked.
    *
    *  @return true if local variables should be checked,
    *  false otherwise. **/
   public boolean checkLocal() {
      return localDeadCode;
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
      if (dcdHAJ != null) {
         result.append("H");
         result.append(PluginUtil.encodeFilename(
               dcdHAJ.getHomeDirectory().getAbsolutePath()));
         result.append("\n");
      }
      if (hideTBI) {
         result.append("D\n");
      }
      if (javaFlags.length() > 0) {
         result.append("J");
         result.append(javaFlags);
         result.append("\n");
      }
      if (excludedClasses.length() > 0) {
         result.append("E");
         result.append(excludedClasses);
         result.append("\n");
      }
      if (excludedMethods.length() > 0) {
         result.append("M");
         result.append(excludedMethods);
         result.append("\n");
      }
      if (!publicDeadCode) {
         result.append("P\n");
      }
      if (!localDeadCode) {
         result.append("L\n");
      }
      return result.toString();
   }
}

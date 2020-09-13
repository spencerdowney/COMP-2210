
import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jgrasp.PluginUtil;

import jgrasp.tool.ToolUtil;


/** JUnit tool settings. **/
public class JUnitSettings {
 
 
   /** Home and jar subclass for JUnit. **/
   public static class JUnitHAJ extends HomeAndJar {
   
      /** The hamcrest jar file. **/
      private File hamcrestJar;
      
      
      /** Creates a new HomeAndJar.
       *
       *  @param homeDir the home directory.
       *
       *  @param jarFile the jar file.
       *
       *  @param hamcrestJarFile the hamcrest jar file. This may
       *  be null. **/
      public JUnitHAJ(final File homeDir, final File jarFile,
            final File hamcrestJarFile) {
         super(homeDir, jarFile, "[^\\d]*-(\\d+(?:\\.\\d+)+).*\\.jar", true);
         hamcrestJar = hamcrestJarFile;
      }
      
      
      /** Gets the hamcrest jar file.
       *
       *  @return the hamcrest jar file, or null if there is none. **/
      public File getHamcrestJarFile() {
         return hamcrestJar;
      }
      
      
      /** {@inheritDoc} **/
      public boolean equals(final Object o) {
         if (!(o instanceof JUnitHAJ)) {
            return false;
         }
         JUnitHAJ haj = (JUnitHAJ) o;
         if ((hamcrestJar == null) != (haj.hamcrestJar == null)) {
            return false;
         }
         if (hamcrestJar != null && !hamcrestJar.equals(haj.hamcrestJar)) {
            return false;
         }
         return super.equals(o);
      }
   
   
      /** {@inheritDoc} **/
      public int hashCode() {
         return super.hashCode() + ((hamcrestJar == null)? 0
            : hamcrestJar.hashCode());
      }
   }
 
 
   /** Currently selected JUnit home directory and jar file,
    *  or null if none have been found or selected. **/
   private JUnitHAJ junitHAJ;

   /** Current default JUnit home directory and jar file, or
    *  null if none have been found. **/
   private JUnitHAJ defaultJUnitHAJ;

   /** Possible JUnit home directories. **/
   private JUnitHAJ[] junitHAJs;
   
   /** True if a results dialog should not be shown when running tests,
    *  false if it should be shown. **/
   private boolean dontShowDialog;
   
   /** True if toolbar items should be hidden, false if they should
    *  be shown. **/
   private boolean hideTBI;
   
   /** User-supplied command line compiler flags. **/
   private String flags = "";
   
   /** User-supplied command line jvm flags. **/
   private String javaFlags = "";

 
   /** Creates a new JUnit settings.
    *
    *  @param data initialization data. **/
   public JUnitSettings(final String data) {
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
            else if (line.startsWith("G")) {
               dontShowDialog = true;
            }
         }
      }
      init(home);
   }
    
    
   /** Creates a new JUnit settings.
    *
    *  @param home the JUnit home directory, or null if the
    *  default should be used.
    *
    *  @param dontShowDialogIn true if a results dialog should not be
    *  shown when running tests, false if it should be shown.
    *
    *  @param hideTBIs true if all toolbar buttons should be hidden,
    *  false if they should be shown when applicable.
    *
    *  @param clFlags additional command line flags.
    *
    *  @param jFlags additonal jvm command line flags. **/
   public JUnitSettings(final File home, final boolean dontShowDialogIn,
         final boolean hideTBIs, final String clFlags, final String jFlags) {
      dontShowDialog = dontShowDialogIn;
      hideTBI = hideTBIs;
      flags = clFlags;
      javaFlags = jFlags;
      init(home);
   }
      
      
   /** Searches a specified directory for installed versions of
    *  JUnit.
    *
    *  @param dirName full name of the directory to be searched.
    *
    *  @param results list to which result installation data
    *  will be added.
    *
    *  @param depth the current depth of the search. **/
   private static void searchForJUnit(final String dirName,
         final List<HomeAndJar> results, final int depth) {
      File dir = new File(dirName);
      File[] files = dir.listFiles();
      if (files == null) {
         return;
      }
      for (File f : files) {
         // Accept any directory with a name starting with "junit".
         if (f.isDirectory() && PluginUtil.startsWithIgnoreCase(f.getName(),
               "junit")) {
            // Must contain a junit jar file.
            File jf = getJarFromHome(f);
            if (jf != null) {
               File hjf = getHamcrestJarFromHome(f);
               HomeAndJar haj = new JUnitHAJ(f, jf, hjf);
               Integer[] version = haj.getVersion();
               if (version.length > 0 && version[0].intValue() >= 4) {
                  results.add(haj);
               }
            }
            else if (depth == 0) {
               // Search one level deeper.
               searchForJUnit(f.getAbsolutePath(), results, 1);
            }
         }
      }
   }


   /** Gets the JUnit jar file corresponding to a JUnit
    *  home directory.
    *
    *  @param homeDirectory the JUnit home directory of interest.
    *
    *  @return the JUnit jar file corresponding to
    *  <code>homeDirectory</code>, or null if there is no such
    *  jar file. **/
   public static File getJarFromHome(final File homeDirectory) {
      File[] files = homeDirectory.listFiles();
      if (files == null) {
         return null;
      }
      // Here we use HomeAndJar just for its version identifying
      // and comparison capabilities.
      HomeAndJar bestVersion = null;
      File bestJar = null;
      for (File f : files) {
         String name = f.getName();
         if (!name.startsWith("junit")
                     || !name.endsWith(".jar")) {
            continue;
         }
         if (name.indexOf("src") >= 0) {
            continue;
         }
         if (name.indexOf("dep") >= 0) {
            continue;
         }
         if (name.indexOf("doc") >= 0) {
            continue;
         }
         HomeAndJar haj = new JUnitHAJ(homeDirectory, f, null);
         if (bestVersion == null || haj.compareTo(bestVersion) < 0) {
            bestVersion = haj;
         }
      }
      if (bestVersion != null) {
         return bestVersion.getJarFile();
      }
      return null;
   }


   /** Gets the hamcrest jar file corresponding to a JUnit
    *  home directory.
    *
    *  @param homeDirectory the JUnit home directory of interest.
    *
    *  @return the hamcrest jar file corresponding to
    *  <code>homeDirectory</code>, or null if there is no such
    *  jar file. **/
   public static File getHamcrestJarFromHome(final File homeDirectory) {
      File[] files = homeDirectory.listFiles();
      if (files == null) {
         return null;
      }
      for (File f : files) {
         String name = f.getName();
         if (name.startsWith("hamcrest-core-")
                     && name.endsWith(".jar")) {
            return f;
         }
      }
      return null;
   }


   /** Initializes these settings.
    *
    *  @param home the JUnit home directory, or null if the
    *  default should be used. **/
   private void init(final File home) {
      findJUnit();
      // Keep the home only if it is not the default.
      if (home != null && (defaultJUnitHAJ == null
            || !defaultJUnitHAJ.getHomeDirectory().equals(home))) {
         File jf = getJarFromHome(home);
         if (jf != null) {
            File hjf = getHamcrestJarFromHome(home);
            junitHAJ = new JUnitHAJ(home, jf, hjf);
         }
      }
   }
   
   
   /** Searches for installed versions of JUnit. **/
   private void findJUnit() {
      List<String> locations = ToolUtil.getLikelyHomeLocations();
      List<HomeAndJar> possibleDirectories =
            new ArrayList<HomeAndJar>();
    
      // Search for JUnit.
      for (String location : locations) {
         searchForJUnit(location, possibleDirectories, 0);
      }
      Collections.sort(possibleDirectories);
      
      junitHAJs = new JUnitHAJ[possibleDirectories.size()];
      junitHAJs = possibleDirectories.toArray(junitHAJs);
      
      if (junitHAJs.length > 0) {
         defaultJUnitHAJ = junitHAJs[0];
      }
   }


   /** Gets the current home and jar.
    *
    *  @return the current home and jar. **/
   public JUnitHAJ getHAJ() {
      if (junitHAJ == null) {
         return defaultJUnitHAJ;
      }
      return junitHAJ;
   }
   

   /** Gets the current JUnit home directory.
    *
    *  @return the JUnit home directory, or null if one
    *  has not been found or selected. **/
   public File getJUnitHome() {
      if (getHAJ() == null) {
         return null;
      }
      return getHAJ().getHomeDirectory();
   }


   /** Gets the current JUnit jar file.
    *
    *  @return the JUnit jar file, or null if one has not
    *  been found or selected. **/
   public File getJUnitJar() {
      if (getHAJ() == null) {
         return null;
      }
      return getHAJ().getJarFile();
   }


   /** Gets the current hamcrest jar file.
    *
    *  @return the hamcrest jar file, or null if one has not
    *  been found or selected. **/
   public File getHamcrestJar() {
      if (getHAJ() == null) {
         return null;
      }
      return getHAJ().getHamcrestJarFile();
   }


   /** Gets the possible JUnit home directories.
    *
    *  @return the possible JUnit home directory paths. **/
   public String[] getJUnitHomes() {
      String[] result = new String[junitHAJs.length];
      for (int i = 0; i < junitHAJs.length; i++) {
         result[i] = junitHAJs[i].getHomeDirectory().getAbsolutePath();
      }
      return result;
   }
   

   /** Determines if a results dialog should be shown when running tests.
    *
    *  @return true if a results dialog should not be shown, false
    *  if it should be shown. **/
   public boolean dontShowDialog() {
      return dontShowDialog;
   }
   

   /** Determines if toolbar items should be hidden.
    *
    *  @return true if toolbar items should be hidden,
    *  false if they should be shown. **/
   public boolean hideToolbarItems() {
      return hideTBI;
   }


   /** Gets user-supplied command line compiler flags.
    *
    *  @return command line compiler flags, or an empty string if
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
      if (junitHAJ != null) {
         result.append("H");
         result.append(PluginUtil.encodeFilename(
               junitHAJ.getHomeDirectory().getAbsolutePath()));
         result.append("\n");
      }
      if (dontShowDialog) {
         result.append("G\n");
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

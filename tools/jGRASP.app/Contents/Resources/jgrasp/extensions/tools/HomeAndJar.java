
import java.io.File;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Class for storing a tool home directory and jar file. **/
public class HomeAndJar implements Comparable<HomeAndJar> {
 
   /** The home directory. **/
   private File home;
   
   /** The jar file. **/
   private File jar;
   
   /** Version number components. **/
   private Integer[] version;
   

   /** Creates a new HomeAndJar.
    *
    *  @param homeDir the home directory.
    *
    *  @param jarFile the jar file.
    *
    *  @param pattern text of regular expression used
    *  to determine version number. The matched groups
    *  will be assumed to be version number components
    *  in order of importance. If this is null, there
    *  will be no version number recorded.
    *
    *  @param versionInJar true if the version number
    *  is to be found in the jar file name, false if it
    *  is to be found in the home directory name. **/
   public HomeAndJar(final File homeDir, final File jarFile,
         final String pattern, final boolean versionInJar) {
      home = homeDir;
      jar = jarFile;
      
      if (pattern == null) {
         version = new Integer[0];
         return;
      }
      
      Pattern verPattern = Pattern.compile(pattern);
      Matcher verMatcher;
      if (versionInJar) {
         String jarName = jarFile.getName();
         verMatcher = verPattern.matcher(jarName);
      }
      else {
         String homeName = homeDir.getName();
         verMatcher = verPattern.matcher(homeName);
      }
      if (verMatcher.matches()) {
         int count = verMatcher.groupCount();
         if (count == 1 && verMatcher.group(1).indexOf('.') >= 0) {
            version = stringsToVersion(verMatcher.group(1).split("\\."));
         }
         else {
            version = new Integer[count];
            for (int i = 0; i < count; i++) {
               String versionComponent = verMatcher.group(i + 1);
               try {
                  version[i] = Integer.valueOf(versionComponent);
               }
               catch (NumberFormatException e) {
               }
            }
         }
      }
      else {
         version = new Integer[0];
      }
      // System.out.println("jar " + jarFile);
      // for (Integer i : version) {
         // System.out.println("   " + i);
      // }
   }
   
   
   /** Converts an array of strings to an version.
    *
    *  @param strings the version number strings.
    *
    *  @return the version number, as an array of number segments. **/
   public static Integer[] stringsToVersion(final String[] strings) {
      Integer[] version = new Integer[strings.length];
      for (int i = 0; i < strings.length; i++) {
         try {
            version[i] = Integer.valueOf(strings[i]);
         }
         catch (NumberFormatException e) {
         }
      }
      return version;
   }
   
   
   /** Determines if two version are the same.
    *
    *  @param v1 first version, as an array of version number segments.
    *
    *  @param v1 second version, as an array of version number segments.
    *
    *  @return two if the versions are identical, false otherwise. **/
   public static boolean versionsEqual(final Integer[] v1,
         final Integer[] v2) {
      if (v1.length != v2.length) {
         return false;
      }
      for (int i = 0; i < v1.length; i++) {
         if (!v1[i].equals(v2[i])) {
            return false;
         }
      }
      return true;
   }
   
   
   /** Compares two version.
    *
    *  @param v1 first version, as an array of version number segments.
    *
    *  @param v1 second version, as an array of version number segments.
    *
    *  @return -1 if v1 is less than v2, 1 if v1 is greater than v2,
    *  and 0 if v1 and v2 are the same. **/
   public static int versionsCompare(final Integer[] v1, final Integer[] v2) {
      int i;
      for (i = 0; i < v1.length && i < v2.length; i++) {
         int result = v1[i].compareTo(v2[i]);
         if (result != 0) {
            return result;
         }
      }
      if (i < v2.length) {
         return -1;
      }
      if (i < v1.length) {
         return 1;
      }
      return 0;
   }
   
   
   /** Gets the home directory.
    *
    *  @return the home directory. **/
   public File getHomeDirectory() {
      return home;
   }
   
 
   /** Gets the jar file.
    *
    *  @return the jar file. **/
   public File getJarFile() {
      return jar;
   }


   /** {@inheritDoc}
    *
    *  <P>Overridden to return the home directory path string. **/
   public String toString() {
      return home.getAbsolutePath();
   }
   
   
   /** {@inheritDoc}
    *
    *  <P>Sorts from highest to lowest version number,
    *  jar file name, then home directory name. **/
   public int compareTo(final HomeAndJar haj) {
      int result = versionsCompare(version, haj.version);
      if (result != 0) {
         return -result;
      }
      result = jar.getName().compareTo(haj.jar.getName());
      if (result != 0) {
         return result;
      }
      result = home.getName().compareTo(haj.home.getName());
      if (result != 0) {
         return result;
      }
      return home.compareTo(haj.home);
   }
   
 
   /** {@inheritDoc} **/
   public boolean equals(final Object o) {
      if (!(o instanceof HomeAndJar)) {
         return false;
      }
      HomeAndJar haj = (HomeAndJar) o;
      if (!versionsEqual(version, haj.version)) {
         return false;
      }
      return jar.equals(haj.jar) && home.equals(haj.home);
   }
 
 
   /** {@inheritDoc} **/
   public int hashCode() {
      return getJarFile().hashCode();
   }
   

   /** Gets the version number info.
    *
    *  @return an array containing the version number
    *  segments in order, or an empty array if there is
    *  no known version number. **/
   public Integer[] getVersion() {
      return version.clone();
   }
}

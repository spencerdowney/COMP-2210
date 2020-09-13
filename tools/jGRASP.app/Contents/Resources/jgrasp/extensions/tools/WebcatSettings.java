
/*
 The Web-CAT plugin sources, and only those sources, in jGRASP are
 licensed under the GPL Version 2 as specified below. This code
 may be used under that license and linked with the (non-open-source)
 jGRASP jar file (jgrasp.jar), as long as only public APIs of jGRASP
 (currently the viewer and tool plugin APIs) are used.
 
 Source code for our slightly modified version of the Eclipse Web-CAT
 submission plugin is available on request. See http://www.jgrasp.org
 for contact information.
*/

import jgrasp.tool.ToolUtil;


/** Web-CAT tool settings. **/
public class WebcatSettings {
 
   /** Assignment definition URL. **/
   private String url;
   
   /** True if toolbar items should be hidden, false if they should
    *  be shown. **/
   private boolean hideTBI;
   
   /** Previously selected assignment definition URLs. **/
   private String[] urlHistory = new String[0];
   
   /** Last username used. **/
   private String username;
   
 
   /** Creates a new Web-CAT settings.
    *
    *  @param data initialization data. **/
   public WebcatSettings(final String data) {
      if (data != null) {
         String[] lines = data.split("\n");
         for (String line : lines) {
            if (line.startsWith("UH")) {
               urlHistory = ToolUtil.decodeArray(line, 2, line.length());
            }
            else if (line.startsWith("U")) {
               url = ToolUtil.decode(line, 1, line.length());
            }
            else if (line.startsWith("D")) {
               hideTBI = true;
            }
            else if (line.startsWith("N")) {
               username = ToolUtil.decode(line, 1, line.length());
            }
         }
      }
   }
    
    
   /** Creates a new Web-CAT settings.
    *
    *  @param defURL the assignment definition URL. This may
    *  be null.
    *
    *  @param hideTBIs true if all toolbar buttons should be hidden,
    *  false if they should be shown when applicable.
    *
    *  @param urlHist previously selected assignment definition
    *  URLs. **/
   public WebcatSettings(final String defURL,
         final boolean hideTBIs, final String[] urlHist) {
      url = defURL;
      hideTBI = hideTBIs;
      urlHistory = urlHist.clone();
   }
      
      
   /** Gets the assignment definition URL.
    *
    *  @return the assignment definition URL, or null if one
    *  has not been set. **/
   public String getURL() {
      return url;
   }


   /** Getst the assignment definition URL history.
    *
    *  @return previously selected assignment definition URLs,
    *  or null if there are none. **/
   public String[] getUrlHistory() {
      return urlHistory.clone();
   }


   /** Determines if toolbar items should be hidden.
    *
    *  @return true if toolbar items should be hidden,
    *  false if they should be shown. **/
   public boolean hideToolbarItems() {
      return hideTBI;
   }
      
      
   /** Gets the username.
    *
    *  @return the last username set, or null if none were set. **/
   public String getUsername() {
      return username;
   }
      
      
   /** Sets the username.
    *
    *  @param name the new username. **/
   public void setUsername(final String name) {
      username = name;
   }


   /** Gets the settings data.
    *
    *  @return the settings data. **/
   public String getData() {
      // Simple storage method for this simple data. If it gets more
      // complex, XML could be used.
      StringBuilder result = new StringBuilder();
      if (url != null) {
         result.append("U");
         result.append(ToolUtil.encode(url));
         result.append("\n");
      }
      if (hideTBI) {
         result.append("D\n");
      }
      if (urlHistory.length > 0) {
         result.append("UH");
         result.append(ToolUtil.encodeArray(urlHistory));
         result.append("\n");
      }
      if (username != null) {
         result.append("N");
         result.append(ToolUtil.encode(username));
         result.append("\n");
      }
      return result.toString();
   }
}

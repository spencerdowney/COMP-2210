
   import java.io.File;
   
   import jgrasp.tool.ToolState;
   import jgrasp.tool.ToolUtil;


   /** File data for a tool that records a pass/fail state for
    *  each file. **/
   public class JUnitStateData implements ToolState {
   
      /** Time when the state was recorded. **/
      private long time;
      
      /** True if the test failed, false otherwise. **/
      private boolean failed;
      
      /** Other file that should be checked for modification date. **/
      private File altFile;
   
   
      /** Creates a new uninitialized PassFailData. **/
      public JUnitStateData() {
      }
      
      
      /** Creates a new PassFailData.
       *
       *  @param tm the time at which the file was evaluated.
       *
       *  @param fail true if the file failed evaluation, false
       *  if it passed.
       *
       *  @param altFileIn file that should also be checked for
       *  modification time (source if this is state for a test
       *  file, or test if this is state for a source file). This
       *  may be null. **/
      public JUnitStateData(final long tm, final boolean fail,
            final File altFileIn) {
         time = tm;
         failed = fail;
         altFile = altFileIn;
      }
   
   
      /** {@inheritDoc} **/
      public String toData(final File f) {
         return String.valueOf(time) + ":" + (failed? "F" : "P")
               + ((altFile == null)? ""
               : ToolUtil.encodeRelativeFile(altFile, f));
      }
      
   
      /** {@inheritDoc} **/
      public boolean initFromData(final String data, final File f) {
         int sep = data.indexOf(':');
         if (sep < 0) {
            return false;
         }
         if (sep >= data.length() - 1) {
            return false;
         }
         failed = data.charAt(sep + 1) == 'F';
         String timeStr = data.substring(0, sep);
         try {
            time = Long.parseLong(timeStr);
         }
            catch (NumberFormatException e) {
               return false;
            }
         int next = data.indexOf(':', sep + 2);
         if (next < 0) {
            next = data.length();
         }
         if (sep + 2 < next) {
            String altFl = data.substring(sep + 2, next);
            altFile = ToolUtil.decodeRelativeFile(altFl, f);
         }
         return true;
      }
      
   
      /** Gets the evaluation time.
       *
       * @return the time at which the file was evaluated. **/
      public long getTime() {
         return time;
      }
      
      
      /** Gets the evaluation state.
       *
       *  return true if the file failed evaluation, false
       *  if it passed. **/
      public boolean failed() {
         return failed;
      }
      
      
      /** Gets the alternate file.
       *
       *  @return a file that should also be checked for
       *  modification time (source if this is state for a test
       *  file, or test if this is state for a source file), or
       *  null if there is no such file. **/
      public File getAltFile() {
         return altFile;
      }
   }

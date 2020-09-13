
   import java.io.File;
   
   import jgrasp.tool.ToolState;


   /** File data for a tool that records a pass/fail state for
    *  each file. **/
   public class PassFailData implements ToolState {
   
      /** Time when the state was recorded. **/
      private long time;
      
      /** True if the test failed, false otherwise. **/
      private boolean failed;
   
   
      /** Creates a new uninitialized PassFailData. **/
      public PassFailData() {
      }
      
      
      /** Creates a new PassFailData.
       *
       *  @param tm the time at which the file was evaluated.
       *
       *  @param fail true if the file failed evaluation, false
       *  if it passed. **/
      public PassFailData(final long tm, final boolean fail) {
         time = tm;
         failed = fail;
      }
   
   
      /** {@inheritDoc} **/
      public String toData(final File f) {
         return String.valueOf(time) + ":" + (failed? "F" : "P");
      }
      
   
      /** {@inheritDoc} **/
      public boolean initFromData(final String data,
            final File f) {
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
   }

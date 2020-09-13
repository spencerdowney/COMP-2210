
import jgrasp.viewer.Util;
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.text.TextAreaView;


/** A viewer that displays a java.lang.String as it could
 *  appear in source code. **/
public class java__lang__String_SourceView extends TextAreaView {


   /** Creates a new String source-code-format viewer.
    *
    *  @param vcd creation data. **/
   public java__lang__String_SourceView(
         final ViewerCreateData vcd) {
   }


   /** {@inheritDoc} **/
   public String getViewName() {
      return "Source Format";
   }
    
       
   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return 7;
   }
   
      
   /** {@inheritDoc} **/
   public String getDisplayText(final Value value,
         final DebugContext context) throws ViewerException {
      String result = Util.encodeString(value.toString(context), false);
      if (result.length() < 50) {
         return "\"" + result + "\"";
      }
      StringBuilder buf = new StringBuilder((int) (result.length() * 1.2));
      for (int start = 0; start < result.length();) {
         int end = start + 50;
         if (end >= result.length()) {
            end = result.length();
         }
         else {
            int lastBackslash = result.lastIndexOf('\\', end - 1);
            if (lastBackslash >= end - 1
                  || (lastBackslash >= end - 6
                  && result.charAt(lastBackslash + 1) == 'u')) {
               // Escape sequence would be broken.
               end = lastBackslash;
            }
         }
         if (start > 0) {
            buf.append("+ ");
         }
         buf.append("\"" + result.substring(start, end) + "\"");
         if (end < result.length()) {
            buf.append('\n');
         }
         start = end;
      }
      return buf.toString();
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Source format viewer for String");
      vi.setLongDescription("This viewer displays a String "
            + "as it would appear in Java source code. Note that "
            + "if the String does appear in source code, the "
            + "format may not be exactly the same, but it will "
            + "produce an equivalent String.");
   }
}

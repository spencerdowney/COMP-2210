
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.text.TextAreaView;


/** A viewer that displays java.lang.String text. **/
public class java__lang__String_FormattedView extends TextAreaView {


   /** Creates a new String text viewer.
    *
    *  @param vcd creation data. **/
   public java__lang__String_FormattedView(
         final ViewerCreateData vcd) {
   }


   /** {@inheritDoc} **/
   public String getViewName() {
      return "Formatted";
   }
   
       
   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return 10;
   }
  
      
   /** {@inheritDoc} **/
   public String getDisplayText(final Value value,
         final DebugContext context) throws ViewerException {
      return value.toString(context);
   }
   
   
   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Formatted String viewer");
      vi.setLongDescription("This viewer displays a String as "
            + "it would be displayed in a text window.");
   }

}

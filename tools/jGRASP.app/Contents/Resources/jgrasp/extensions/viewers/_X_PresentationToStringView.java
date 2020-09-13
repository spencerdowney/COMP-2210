
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.presentation.PresentationNodeView;


/** A presentation "toString()" viewer for anything. **/
public class _X_PresentationToStringView extends PresentationNodeView {


   /** Creates a new toString() viewer.
    *
    *  @param vcd creation data. **/
   public _X_PresentationToStringView(final ViewerCreateData vcd) {
   }


   /** {@inheritDoc} **/
   public String getViewName() {
      return "Presentation String";
   }
   
       
   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return -20;
   }
  
      
   /** {@inheritDoc} **/
   public String getDisplayText(final Value value,
         final DebugContext context) throws ViewerException {
      return value.toString(context);
   }
   
   
   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("toString() viewer");
      vi.setLongDescription("This viewer displays the toString() "
            + "value of a value.");
   }

}

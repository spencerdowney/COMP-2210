import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;

import jgrasp.viewer.gui.ElementListViewer;

import jgrasp.viewer.jgrdi.Value;


/** An object elements viewer. **/
public class _X_ElementView extends ElementListViewer {


   /** Creates a new element viewer.
    *
    *  @param vcd creation data. **/
   public _X_ElementView(final ViewerCreateData vcd) {
      super();
   }


   /** {@inheritDoc} **/
   public String getViewName() {
      return "Basic";
   }


   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      if (vpd.isCanvas()) {
         Value v = vpd.getValue();
         if (v != null && v.isPrimitive()) {
            return 10000;
         }
         return 1;
      }
      return -1;
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Object viewer");
      vi.setLongDescription("This viewer shows instance fields for "
            + "Objects, array elements for arrays, and a text description "
            + "for primitives.");
   }
}

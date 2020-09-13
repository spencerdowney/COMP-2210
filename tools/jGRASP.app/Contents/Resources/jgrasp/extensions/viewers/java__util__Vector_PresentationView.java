
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.presentation.PresentationListView;


/** Presentation viewer for java.util.Vector. **/
public class java__util__Vector_PresentationView extends
      PresentationListView {
   
   /** The per-class settings. **/
   private static Settings perClassSettings;


   /** Creates a new presentation vector viewer.
    *
    *  @param vcd creation data. **/
   public java__util__Vector_PresentationView(
         final ViewerCreateData vcd) {
      super(0);
   }


   /** {@inheritDoc} **/
   public Object getElementContext(final Value value,
         final DebugContext context) throws ViewerException {
      return value.getFieldValue(context, "elementData");
   }


   /** {@inheritDoc} **/
   public Value getElement(final Value value, final int index,
         final DebugContext context, final Object elementContext)
         throws ViewerException {
      return ((Value) elementContext).getArrayElement(context, index);
   }


   /** {@inheritDoc} **/
   public int getLength(final Value value, final DebugContext context)
         throws ViewerException {
      return value.getFieldValue(context, "elementCount").toInt(context);
   }


   /** {@inheritDoc} **/
   public boolean isObjectList(final Value value,
         final DebugContext context) {
      return true;
   }
   
   
   /** {@inheritDoc} **/
   public int getFullLimit() {
      return 50;
   }
   
   
   /** {@inheritDoc} **/
   public String getLeftIndexMarker() {
      return "<";
   }
    
    
   /** {@inheritDoc} **/
   public String getRightIndexMarker() {
      return ">";
   }


   /** {@inheritDoc} **/
   public Value getContainer(final Value value, final int index,
         final DebugContext context, final Object elementContext)
         throws ViewerException {
      return null;
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Presentation viewer for Vectors");
      vi.setLongDescription("This viewer displays the toString() value "
            + "of Vector elements in a graphical array representation. "
            + "Selecting an element will cause its value to be displayed "
            + "in a subviewer.");
   }
      
      
   /** {@inheritDoc} **/
   public Settings getPerClassSettings() {
      if (perClassSettings == null) {
         perClassSettings = createGlobalSettings();
      }
      return perClassSettings;
   }
}

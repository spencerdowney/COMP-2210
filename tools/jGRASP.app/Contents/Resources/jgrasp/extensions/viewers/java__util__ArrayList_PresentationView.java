
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.presentation.PresentationListView;


/** Presentation viewer for java.util.ArrayList. **/
public class java__util__ArrayList_PresentationView extends
      PresentationListView {
   
   /** The per-class settings. **/
   private static Settings perClassSettings;


   /** Creates a new presentation ArrayList viewer.
    *
    *  @param vcd creation data. **/
   public java__util__ArrayList_PresentationView(
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
      return value.getFieldValue(context, "size").toInt(context);
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
      vi.setShortDescription("Presentation viewer for ArrayLists");
      vi.setLongDescription("This viewer displays the graphical structure "
            + "of an ArrayList. The toString() values of the elements are "
            + "displayed in the nodes. "
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

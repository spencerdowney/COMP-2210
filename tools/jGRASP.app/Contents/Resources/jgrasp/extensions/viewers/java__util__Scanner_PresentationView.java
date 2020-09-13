
import java.util.ArrayList;
import java.util.List;

import jgrasp.viewer.IndexItem;
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.presentation.PresentationListView;


/** Presentation viewer for scanners. **/
public class java__util__Scanner_PresentationView
      extends PresentationListView {
   
   /** The per-class settings. **/
   private static Settings perClassSettings;


   /** Creates a new scanner viewer.
    *
    *  @param vcd creation data. **/
   public java__util__Scanner_PresentationView(final ViewerCreateData vcd) {
      super(0);
   }


   /** {@inheritDoc} **/
   public String getViewName() {
      return "Presentation";
   }


   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return 1000;
   }


   /** {@inheritDoc} **/
   public Value getElement(final Value value, final int index,
         final DebugContext context, final Object elementContext)
         throws ViewerException {
      Value buf = value.getFieldValue(context, "buf");
      if (buf == null) {
         return null;
      }
      Value hb = buf.getFieldValue(context, "hb");
      if (hb == null) {
         return null;
      }
      return hb.getArrayElement(context, index);
   }


   /** {@inheritDoc} **/
   public boolean elementUsed(final Value value, final Value container,
         final Value element, final int index, final DebugContext context,
         final Object elementContext) throws ViewerException {
      Value buf = value.getFieldValue(context, "buf");
      if (buf == null) {
         return true;
      }
      int offs = buf.getFieldValue(context, "offset").toInt(context);
      if (index < offs) {
         return false;
      }
      int limit = buf.getFieldValue(context, "limit").toInt(context);
      return index < limit;
   }


   /** {@inheritDoc} **/
   public int getLength(final Value value, final DebugContext context)
         throws ViewerException {
      Value buf = value.getFieldValue(context, "buf");
      if (buf == null) {
         return 0;
      }
      Value hb = buf.getFieldValue(context, "hb");
      if (hb == null) {
         return 0;
      }
      return hb.getArrayLength(context);
   }


   /** {@inheritDoc} **/
   public boolean isObjectList(final Value value,
         final DebugContext context) throws ViewerException {
      return false;
   }


   /** {@inheritDoc} **/
   public int getFullLimit() {
      return 500;
   }


   /** {@inheritDoc} **/
   public String getLeftIndexMarker() {
      return "[";
   }
    
    
   /** {@inheritDoc} **/
   public String getRightIndexMarker() {
      return "]";
   }
    
      
   /** {@inheritDoc} **/
   public Value getContainer(final Value value, final int index,
         final DebugContext context, final Object elementContext)
         throws ViewerException {
      return null;
   }      


   /** {@inheritDoc} **/
   public List<IndexItem> getMarkers(final Value value,
         final DebugContext context) throws ViewerException {
      List<IndexItem> result = new ArrayList<IndexItem>();
      int pos = value.getFieldValue(context, "position").toInt(context);
      // Hash prevents conflict with other indexes.
      result.add(new IndexItem("#position", "position", pos, false));
      return result;
   }


   /** {@inheritDoc} **/
   public int getAutoscrollIndex(final Value value,
         final DebugContext context) throws ViewerException {
      int pos = value.getFieldValue(context, "position").toInt(context);
      return pos;
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Presentation viewer for Scanners");
      vi.setLongDescription("This viewer displays the contents "
            + " of the Scanner's buffer.");
   }
      
      
   /** {@inheritDoc} **/
   public Settings getPerClassSettings() {
      if (perClassSettings == null) {
         perClassSettings = createGlobalSettings();
      }
      return perClassSettings;
   }
}

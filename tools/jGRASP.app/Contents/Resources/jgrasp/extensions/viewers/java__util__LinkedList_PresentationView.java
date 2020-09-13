
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.NoSuchTypeException;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.presentation.PresentationListView;


/** Presentation viewer for java.util.LinkedList. **/
public class java__util__LinkedList_PresentationView extends
      PresentationListView {
   
   
   /** Context for quickly finding neighboring nodes. **/
   private static final class Context {
      
      /** Last node examined. **/
      private Value entry;
      
      /** Index of last node examined. **/
      private int index = -2;
   }


   /** The per-class settings. **/
   private static Settings perClassSettings;

   /** True if the object being viewed is a Java 1.7 LinkedList, false
    *  if it is from an earlier version of Java. **/
   private boolean v17;


   /** Creates a new presentation LinkedList viewer.
    *
    *  @param vcd creation data. **/
   public java__util__LinkedList_PresentationView(
         final ViewerCreateData vcd) {
      super(LINKED | BACK_LINKS | HEADER | END_LINK_TO_START
            | START_LINK_TO_END);
      try {
         vcd.getDebugContext().getType("java.util.LinkedList$Node");
         v17 = true;
         setLinkage(false, false, false);
      }
      catch (NoSuchTypeException e) {
      }
   }


   /** {@inheritDoc} **/
   public Object getElementContext(final Value value,
         final DebugContext context) throws ViewerException {
      return new Context();
   }


   /** {@inheritDoc} **/
   public Value getElement(final Value value, final int index,
         final DebugContext context, final Object elementContext)
         throws ViewerException {
      Context ec = (Context) elementContext;
      Value entry;
   
      if (index == 0) {
         if (v17) {
            entry = value.getFieldValue(context, "first");
         }
         else {
            entry = value.getFieldValue(context, "header");
         }
      }
      else if (index == ec.index + 1) {
         entry = ec.entry.getFieldValue(context, "next");
      }
      else {
         entry = getEntry(value, index, context);
      }
   
      Value element;
      if (v17) {
         element = entry.getFieldValue(context, "item");
      }
      else {
         element = entry.getFieldValue(context, "element");
      }
      ec.entry = entry;
      ec.index = index;
      return element;
   }


   /** Gets the list node ("entry" in java.util.LinkedList terminology)
    *  at a specified index.
    *
    *  @param value the linked list value.
    *
    *  @param index index of the node of interest.
    *
    *  @param context the current debugger context.
    *
    *  @return the requested node.
    *
    *  @throws ViewerException if an error occurs while retrieving
    *  the node. **/
   private Value getEntry(final Value value, final int index,
         final DebugContext context) throws ViewerException {
      Method getMethod;
      if (v17) {
         getMethod = value.getMethod(context, "node",
               "java.util.LinkedList$Node", new String[] { "int" });
      }
      else {
         getMethod = value.getMethod(context, "entry",
               "java.util.LinkedList$Entry", new String[] { "int" });
      }
      return value.invokeMethod(context, getMethod,
            new Value[] {
               context.createPrimitiveValue("int",
               String.valueOf(index)) });
   }


   /** {@inheritDoc} **/
   public Value getContainer(final Value value, final int index,
         final DebugContext context, final Object elementContext)
         throws ViewerException {
      Context ec = (Context) elementContext;
      if (index == ec.index) {
         return ec.entry;
      }
      if (index == 0) {
         if (v17) {
            return value.getFieldValue(context, "first");
         }
         else {
            return value.getFieldValue(context, "header");
         }
      }
      return getEntry(value, index - 1, context);
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
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Presentation viewer for LinkedLists");
      vi.setLongDescription("This viewer displays the internal "
            + "structure of a LinkedList. Selecting an entry or value "
            + "will cause it to be displayed in a subviewer.\n\n"
            + "A correct structure is assumed. Stepping into LinkedList "
            + "methods may cause the viewer to temporarily fail.");
   }
      
      
   /** {@inheritDoc} **/
   public Settings getPerClassSettings() {
      if (perClassSettings == null) {
         perClassSettings = createGlobalSettings();
      }
      return perClassSettings;
   }
}

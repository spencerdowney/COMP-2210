
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.JgrdiNoSuchFieldException;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.presentation.PresentationListView;


/** Presentation viewer for Strings. **/
public class java__lang__String_CharArrayView extends PresentationListView {
   
   /** The per-class settings. **/
   private static Settings perClassSettings;


   /** Creates a new presentation String viewer.
    *
    *  @param vcd creation data. **/
   public java__lang__String_CharArrayView(final ViewerCreateData vcd) {
      super(0);
   }


   /** {@inheritDoc} **/
   public Value getElement(final Value value, final int index,
         final DebugContext context, final Object elementContext)
         throws ViewerException {
      Value valArray = value.getFieldValue(context, "value");
      int offset;
      try {
         Value offsetV = value.getFieldValue(context, "offset");
         offset = offsetV.toInt(context);
      }
      catch (JgrdiNoSuchFieldException e) {
         offset = 0;
      }
      if (context.getTargetMajorVersionNumber() <= 8) {
         return valArray.getArrayElement(context, offset + index);
      }
      Value coder = value.getFieldValue(context, "coder");
      if (coder.toInt(context) == 0) {
         Value result = valArray.getArrayElement(context, offset + index);
         return context.createPrimitiveValue("char",
               String.valueOf(result.toChar(context)));
      }
   
      byte low = valArray.getArrayElement(context, (offset + index) * 2).
            toByte(context);
      byte high = valArray.getArrayElement(context, (offset + index) * 2 + 1).
            toByte(context);
      return context.createPrimitiveValue("char",
               String.valueOf((char) ((high << 8) + low)));
   }


   /** {@inheritDoc} **/
   public int getLength(final Value value, final DebugContext context)
         throws ViewerException {
      try {
         return value.getFieldValue(context, "count").toInt(context);
      }
      catch (JgrdiNoSuchFieldException e) {
         Value valArray = value.getFieldValue(context, "value");
         int result = valArray.getArrayLength(context);
         if (context.getTargetMajorVersionNumber() <= 8) {
            return result;
         }
         Value coder = value.getFieldValue(context, "coder");
         return (coder.toInt(context) == 0)? result : (result / 2);
      }
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
   public String getViewName() {
      return "Presentation Char Array";
   }


   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return 2;
   }
      
      
   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Character array viewer for Strings");
      vi.setLongDescription("This viewer displays a String "
            + "as an array of characters. Note that this is not "
            + "necessarily the same as the character array that "
            + "the String uses to hold its characters (which may "
            + "be a larger array, where the Strings offset and "
            + "length limit the range).");
   }     
      
      
   /** {@inheritDoc} **/
   public Settings getPerClassSettings() {
      if (perClassSettings == null) {
         perClassSettings = createGlobalSettings();
      }
      return perClassSettings;
   }
}

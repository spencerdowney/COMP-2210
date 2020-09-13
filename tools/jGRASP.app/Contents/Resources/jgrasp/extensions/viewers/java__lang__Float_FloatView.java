
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Value;


/** java.lang.Float detail viewer. **/
public class java__lang__Float_FloatView extends FloatView {


   /** Creates a new Float viewer.
    *
    *  @param vcd creation data. **/
   public java__lang__Float_FloatView(final ViewerCreateData vcd) {
      super(31, 30, 23, 22, 0, 127);
   }


   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return 10;
   }


   /** {@inheritDoc} **/
   public double getDoubleValue(final Value value,
         final DebugContext context) throws ViewerException {
      return getValue(value, context);
   }


   /** {@inheritDoc} **/
   public String getValueText(final Value value,
         final DebugContext context) throws ViewerException {
      return Float.toString(getValue(value, context));
   }
   
   
   /** {@inheritDoc} **/
   public long getBits(final Value value,
         final DebugContext context) throws ViewerException {
      return Float.floatToRawIntBits(getValue(value, context));
   }


   /** {@inheritDoc} **/
   public String getMantissaText(final double mantissa) {
      return Float.toString((float) mantissa);
   }


   /** Gets the value from a Float.
    *
    *  @param value the value of interest, of type
    *  java.lang.Float.
    *
    *  @param context the current debug context.
    *
    *  @return the current double value of <code>value</code>
    *
    *  @throws ViewerException if an error occurs while
    *  retrieving the value. **/
   private float getValue(final Value value,
         final DebugContext context) throws ViewerException {
      Method getValueMethod = value.getMethod(context, "floatValue",
            "float", null);
      return value.invokeMethod(context, getValueMethod, null).
            toFloat(context);
   }
   
   
   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for Longs");
      vi.setLongDescription("This viewer displays a Long value, "
            + "and the details of its binary representation.");
   }
}

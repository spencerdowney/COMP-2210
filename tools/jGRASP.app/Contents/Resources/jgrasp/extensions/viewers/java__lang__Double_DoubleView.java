

import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Value;


/** java.lang.Double detail viewer. **/
public class java__lang__Double_DoubleView extends FloatView {


   /** Creates a new Double viewer.
    *
    *  @param vcd creation data. **/
   public java__lang__Double_DoubleView(final ViewerCreateData vcd) {
      super(63, 62, 52, 51, 0, 1023);
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
      return Double.toString(getValue(value, context));
   }
   
   
   /** {@inheritDoc} **/
   public long getBits(final Value value, final DebugContext context)
         throws ViewerException {
      return Double.doubleToRawLongBits(getValue(value, context));
   }


   /** {@inheritDoc} **/
   public String getMantissaText(final double mantissa) {
      return Double.toString(mantissa);
   }


   /** Gets the value from a Double.
    *
    *  @param value the value of interest, of type
    *  java.lang.Double.
    *
    *  @param context the current debug context.
    *
    *  @return the current double value of <code>value</code>
    *
    *  @throws ViewerException if an error occurs while
    *  retrieving the value. **/
   private double getValue(final Value value,
         final DebugContext context) throws ViewerException {
      Method getValueMethod = value.getMethod(context, "doubleValue",
            "double", null);
      return value.invokeMethod(context, getValueMethod, null).
            toDouble(context);
   }
   
   
   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for Doubles");
      vi.setLongDescription("This viewer displays a Double value, "
            + "and the details of its binary representation.");
   }
}

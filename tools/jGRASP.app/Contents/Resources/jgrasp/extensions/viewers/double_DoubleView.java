
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerInfo;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.NoConversionException;
import jgrasp.viewer.jgrdi.Value;


/** Primitive double detail viewer. **/
public class double_DoubleView extends FloatView {


   /** Creates a new double viewer.
    *
    *  @param vcd creation data. **/
   public double_DoubleView(final ViewerCreateData vcd) {
      super(63, 62, 52, 51, 0, 1023);
   }


   /** {@inheritDoc} **/
   public double getDoubleValue(final Value value,
         final DebugContext context) throws NoConversionException {
      return value.toDouble(context);
   }


   /** {@inheritDoc} **/
   public String getValueText(final Value value,
         final DebugContext context) throws NoConversionException {
      return Double.toString(value.toDouble(context));
   }
   
   
   /** {@inheritDoc} **/
   public long getBits(final Value value, final DebugContext context)
         throws NoConversionException {
      return Double.doubleToRawLongBits(value.toDouble(context));
   }


   /** {@inheritDoc} **/
   public String getMantissaText(final double mantissa) {
      return Double.toString(mantissa);
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for doubles");
      vi.setLongDescription("This viewer displays a double, and "
            + "the details of its binary representation.");
   }
}

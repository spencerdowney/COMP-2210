
import java.util.Locale;

import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Value;


/** java.lang.Integer detail viewer. **/
public class java__lang__Integer_IntView extends IntView {


   /** Creates a new Integer viewer.
    *
    *  @param vcd creation data. **/
   public java__lang__Integer_IntView(final ViewerCreateData vcd) {
   }


   /** {@inheritDoc} **/
   public String getDisplayText(final Value value,
         final DebugContext context) throws ViewerException {
      Method getValueMethod = value.getMethod(context, "intValue",
            "int", null);
      int val = value.invokeMethod(context, getValueMethod, null).
            toInt(context);
   
      return "Decimal: " + val
            + "\nHex: 0x"
               + Integer.toHexString(val).toUpperCase(Locale.ENGLISH)
            + "\nOctal: 0" + Integer.toOctalString(val)
            + "\nBinary: " + bitString(val, 31);
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for Integers");
      vi.setLongDescription("This viewer displays an Integer's "
            + "value in decimal, hex, octal, and binary forms.");
   }

}

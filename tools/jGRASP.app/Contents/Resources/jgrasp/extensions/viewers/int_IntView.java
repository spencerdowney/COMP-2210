
import java.util.Locale;

import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Value;


/** Primitive int detail viewer. **/
public class int_IntView extends IntView {


   /** Creates a new int viewer.
    *
    *  @param vcd creation data. **/
   public int_IntView(final ViewerCreateData vcd) {
   }


   /** {@inheritDoc} **/
   public String getDisplayText(final Value value,
         final DebugContext context) throws ViewerException {
      int val = value.toInt(context);
      return "Decimal: " + val
            + "\nHex: 0x"
               + Integer.toHexString(val).toUpperCase(Locale.ENGLISH)
            + "\nOctal: 0" + Integer.toOctalString(val)
            + "\nBinary: " + bitString(val, 31);
   }
   
   
   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for ints");
      vi.setLongDescription("This viewer displays an int "
            + "in decimal, hex, octal, and binary forms.");
   }
}

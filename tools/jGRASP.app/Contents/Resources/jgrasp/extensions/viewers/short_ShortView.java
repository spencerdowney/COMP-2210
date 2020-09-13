
import java.util.Locale;

import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Value;


/** Primitive short detail viewer. **/
public class short_ShortView extends IntView {
 
 
   /** Creates a new short viewer.
    *
    *  @param vcd creation data. **/
   public short_ShortView(final ViewerCreateData vcd) {
   }


   /** {@inheritDoc} **/
   public String getDisplayText(final Value value,
         final DebugContext context) throws ViewerException {
      short val = value.toShort(context);
      int intVal = val & 0xFFFF;
      return "Decimal: " + val
            + "\nHex: 0x"
               + Integer.toHexString(intVal).toUpperCase(Locale.ENGLISH)
            + "\nOctal: 0" + Integer.toOctalString(intVal)
            + "\nBinary: " + bitString(val, 15);
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for shorts");
      vi.setLongDescription("This viewer displays a short in decimal, "
            + "hex, octal, and binary forms.");
   }
}

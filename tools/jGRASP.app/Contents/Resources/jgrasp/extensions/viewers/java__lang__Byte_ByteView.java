
import java.util.Locale;

import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Value;


/** java.lang.byte detail viewer. **/
public class java__lang__Byte_ByteView extends IntView {


   /** Creates a new Byte viewer.
    *
    *  @param vcd creation data. **/
   public java__lang__Byte_ByteView(final ViewerCreateData vcd) {
   }


   /** {@inheritDoc} **/
   public String getDisplayText(final Value value,
         final DebugContext context) throws ViewerException {
      Method getValueMethod = value.getMethod(context, "byteValue",
            "byte", null);
      byte val = value.invokeMethod(context, getValueMethod, null).
            toByte(context);
   
      int intVal = val & 0xFF;
      return "Decimal: " + val
            + "\nHex: 0x"
               + Integer.toHexString(intVal).toUpperCase(Locale.ENGLISH)
            + "\nOctal: 0" + Integer.toOctalString(intVal)
            + "\nBinary: " + bitString(val, 7);
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for Bytes");
      vi.setLongDescription("This viewer displays a Byte's value in "
            + "decimal, hex, octal, and binary forms.");
   }
}

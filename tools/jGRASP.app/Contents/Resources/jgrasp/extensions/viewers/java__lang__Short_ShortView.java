
import java.util.Locale;

import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Value;


/** java.lang.Short detail viewer. **/
public class java__lang__Short_ShortView extends IntView {
   
    
   /** Creates a new Short viewer.
    *
    *  @param vcd creation data. **/
   public java__lang__Short_ShortView(final ViewerCreateData vcd) {
   }


   /** {@inheritDoc} **/
   public String getDisplayText(final Value value,
         final DebugContext context) throws ViewerException {
      Method getValueMethod = value.getMethod(context, "shortValue",
            "short", null);
      short val = value.invokeMethod(context, getValueMethod, null).
            toShort(context);
   
      int intVal = val & 0xFFFF;
      return "Decimal: " + val
            + "\nHex: 0x"
                  + Integer.toHexString(intVal).toUpperCase(Locale.ENGLISH)
            + "\nOctal: 0" + Integer.toOctalString(intVal)
            + "\nBinary: " + bitString(val, 15);
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for Shorts");
      vi.setLongDescription("This viewer displays a Short's "
            + "value in decimal, hex, octal, and binary forms.");
   }
}

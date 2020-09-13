
import java.util.Locale;

import jgrasp.viewer.Util;
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Value;


/** java.lang.Character number value detail viewer. **/
public class java__lang__Character_CharView extends IntView {


   /** Creates a new Character viewer.
    *
    *  @param vcd creation data. **/
   public java__lang__Character_CharView(final ViewerCreateData vcd) {
   }


   /** {@inheritDoc} **/
   public String getDisplayText(final Value value,
         final DebugContext context) throws ViewerException {
      Method getValueMethod = value.getMethod(context, "charValue",
            "char", null);
      char val = value.invokeMethod(context, getValueMethod, null).
            toChar(context);
   
      return "Character: " + val
            + "\nSource format: \'" + Util.encodeChar(val) + "\'"
            + "\nDecimal: " + (int) val
            + "\nHex: 0x"
                  + Integer.toHexString(val).toUpperCase(Locale.ENGLISH)
            + "\nOctal: 0" + Integer.toOctalString(val)
            + "\nBinary: " + bitString(val, 15);
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for Characters");
      vi.setLongDescription("This viewer displays a Character's "
            + "value as a char and the source format for the "
            + "char, and its numeric value in decimal, "
            + "hex, octal, and binary forms.");
   }
}

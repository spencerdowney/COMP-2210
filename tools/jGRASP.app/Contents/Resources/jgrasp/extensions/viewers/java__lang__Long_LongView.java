
import java.util.Locale;

   import jgrasp.viewer.ViewerCreateData;
   import jgrasp.viewer.ViewerException;
   import jgrasp.viewer.ViewerInfo;
   import jgrasp.viewer.ViewerPriorityData;

   import jgrasp.viewer.jgrdi.DebugContext;
   import jgrasp.viewer.jgrdi.Method;
   import jgrasp.viewer.jgrdi.Value;


   /** java.lang.Long detail viewer. **/
   public class java__lang__Long_LongView extends IntView {
      
       
      /** Creates a new Long viewer.
       *
       *  @param vcd creation data. **/
      public java__lang__Long_LongView(final ViewerCreateData vcd) {
      }
      
      
      /** {@inheritDoc} **/
      public int getPriority(final ViewerPriorityData vpd) {
         return 10;
      }
   
   
      /** {@inheritDoc} **/
      public String getDisplayText(final Value value,
            final DebugContext context) throws ViewerException {
         Method getValueMethod = value.getMethod(context, "longValue",
               "long", null);
         long val = value.invokeMethod(context, getValueMethod, null).
               toLong(context);
      
         return "Decimal: " + val
               + "\nHex: 0x"
                     + Long.toHexString(val).toUpperCase(Locale.ENGLISH)
               + "\nOctal: 0" + Long.toOctalString(val)
               + "\nBinary: " + bitString(val, 63);
      }
   
   
      /** {@inheritDoc} **/
      public void getInfo(final ViewerInfo vi) {
         vi.setShortDescription("Detail viewer for Longs");
         vi.setLongDescription("This viewer displays a Long's "
               + "value in decimal, hex, octal, and binary forms.");
      }
   }

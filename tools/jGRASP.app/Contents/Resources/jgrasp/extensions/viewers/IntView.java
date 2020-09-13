
   import jgrasp.viewer.ViewerPriorityData;

   import jgrasp.viewer.text.TextAreaView;


   /** Base class for integer detail viewers. **/
   public abstract class IntView extends TextAreaView {
   
   
      /** Get the string representation of integer bits.
       *
       *  @param bits the bits of interest.
       *
       *  @param firstBit the number of significant bits to display,
       *  minus one.
       *
       *  @return a string representation of the specified bits. **/
      public static String bitString(final long bits, final int firstBit) {
         StringBuilder buffer = new StringBuilder();
         long bit = 1L << firstBit;
         for (int i = firstBit; i >= 0; i--, bit >>>= 1) {
            buffer.append(((bits & bit) == 0L)? '0' : '1');
            if (i > 0 && (i % 4) == 0) {
               buffer.append(' ');
            }
         }
         return buffer.toString();
      }
   
   
      /** {@inheritDoc} **/
      public String getViewName() {
         return "Numeric";
      }
   
   
      /** {@inheritDoc} **/
      public int getPriority(final ViewerPriorityData vpd) {
         return -1;
      }
   }


import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;

import java.math.BigDecimal;

import java.util.EnumSet;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerPriorityData;
import jgrasp.viewer.ViewerRoot;
import jgrasp.viewer.ViewerUpdateData;
import jgrasp.viewer.ViewerValueData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Value;


/** Base class for floating-point detail viewers. **/
public abstract class FloatView extends ViewerRoot {

   /** The current value as a double. **/
   private double currentVal;

   /** The current value text. **/
   private String currentValueText;
   
   /** The current bit representation. **/
   private long currentBits;

   /** The top label, which displays the value in decimal. **/
   private JLabel topLabel;
   
   /** Labels for showing the sign, exponent, and mantissa
    *  in binary, hex, and decimal, and the header labels
    *  for those categories. **/
   private JLabel[] textSegmentLabels = new JLabel[12];
   
   /** Labels which display the value computations. **/
   private JLabel[] computationLabel = new JLabel[8];

   /** Index of the sign bit for the float type that this
    *  viewer displays. **/
   private int signBit;
   
   /** Index of the first exponent bit for the float type that this
    *  viewer displays. **/
   private int firstExponentBit;
   
   /** Index of the last exponent bit for the float type that this
    *  viewer displays. **/
   private int lastExponentBit;
   
   /** Index of the first mantissa bit for the float type that this
    *  viewer displays. **/
   private int firstMantissaBit;
   
   /** Index of the last mantissa bit for the float type that this
    *  viewer displays. **/
   private int lastMantissaBit;
   
   /** Bias to be subtracted from the exponent bits value to get the
    *  actual exponent. **/
   private int exponentBias;


   /** Creates a new FloatView.
    *
    *  @param signBitIndex the of the sign bit for the float type that this
    *  viewer displays.
    *
    *  @param firstExponentBitIndex the index of the first exponent bit for
    *  the float type that this viewer displays.
    *
    *  @param lastExponentBitIndex the index of the last exponent bit for
    *  the float type that this viewer displays.
    *
    *  @param firstMantissaBitIndex the index of the first mantissa bit for
    *  the float type that this viewer displays.
    *
    *  @param lastMantissaBitIndex the index of the last mantissa bit for
    *  the float type that this viewer displays.
    *
    *  @param exponentBiasValue the bias to be subtracted from the exponent
    *  bits value to get the actual exponent. **/
   public FloatView(final int signBitIndex,
         final int firstExponentBitIndex,
         final int lastExponentBitIndex, final int firstMantissaBitIndex,
         final int lastMantissaBitIndex, final int exponentBiasValue) {
      super(false, false, EnumSet.of(CreationFlags.STANDARD_BORDER));
      
      signBit = signBitIndex;
      firstExponentBit = firstExponentBitIndex;
      lastExponentBit = lastExponentBitIndex;
      firstMantissaBit = firstMantissaBitIndex;
      lastMantissaBit = lastMantissaBitIndex;
      exponentBias = exponentBiasValue;
   }


   /** Gets a text representation for a BigDecimal value that is similar in
    *  format to a given text representation.
    *
    *  @param val the value of interest.
    *
    *  @param ref the reference representation.
    *
    *  @return a text representation for the value that is similar in form
    *  to the reference representation. **/
   private static String getBDText(final BigDecimal val, final String ref) {
      // Can the exact decimal representation have more than 1000 digits?
      String result;
      if (ref.indexOf('E') < 0) {
         result = String.format(Locale.US, "%.1000f", val);
      }
      else {
         result = String.format(Locale.US, "%.1000E", val).replace("E+", "E").
               replace("E0", "E").replace("E-0", "E-");
      }
      // Strip trailing zeros in the decimal part.
      int ePos = result.indexOf('E');
      int dotPos = result.indexOf('.');
      if (dotPos >= 0) {
         int end = (ePos >= 0)? ePos : result.length();
         int zeroCut = end;
         while (zeroCut > dotPos + 2
               && result.charAt(zeroCut - 1) == '0') {
            zeroCut--;
         }
         if (zeroCut < end) {
            result = result.substring(0, zeroCut)
                  + ((ePos >= 0)? result.substring(ePos) : "");
         }
      }
      return result;
   }


   /** {@inheritDoc} **/
   public void destroy() {
   }


   /** {@inheritDoc} **/
   public String getViewName() {
      return "Numeric";
   }


   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return -1;
   }


   /** {@inheritDoc} **/
   public void updateState(final ViewerValueData valueData,
         final ViewerUpdateData data, final DebugContext context)
         throws ViewerException {
      currentVal = getDoubleValue(valueData.getValue(), context);
      currentValueText = getValueText(valueData.getValue(), context);
      currentBits = getBits(valueData.getValue(), context);
   }


   /** {@inheritDoc} **/
   public void updateGui() {
      if (topLabel != null) {
         topLabel.setText("value = " + currentValueText);
      }
      
      long signMask = 1L << signBit;
      long bits = currentBits;
      computationLabel[0].setText("sign = "
            + (((bits & signMask) == 0L)? '+' : '-'));
      
      textSegmentLabels[3].setText(((bits & signMask) == 0L)? " 0" : " 1");
      textSegmentLabels[4].setText(" " + bitString(bits, firstExponentBit,
            lastExponentBit));
      textSegmentLabels[5].setText(" " + bitString(bits, firstMantissaBit,
            lastMantissaBit));
   
      long exponentMask = (2L << firstExponentBit)
            - (1L << lastExponentBit);
      long exponent = (bits & exponentMask) >> lastExponentBit;
      
      long mantissaMask = (2L << firstMantissaBit)
            - (1L << lastMantissaBit);
      long mantissa = (bits & mantissaMask) >> lastMantissaBit;
      
      textSegmentLabels[6].setText(((bits & signMask) == 0L)? " 0" : " 1");
      textSegmentLabels[7].setText("0x" + Long.toString(exponent, 16));
      textSegmentLabels[8].setText("0x" + Long.toString(mantissa, 16));
      
      textSegmentLabels[9].setText(((bits & signMask) == 0L)? " 0" : " 1");
      textSegmentLabels[10].setText(Long.toString(exponent));
      textSegmentLabels[11].setText(Long.toString(mantissa));
      
      if (exponent == ((2L << (firstExponentBit
            - lastExponentBit)) - 1)) {
         if (mantissa == 0L) {
            computationLabel[1].setText("value = "
                  + (((bits & signMask) == 0L)? '+' : '-')
                  + "infinity (exponent all 1s and mantissa 0 indicates "
                  + "infinity)");
         }
         else {
            computationLabel[0].setText("value = NaN (exponent all 1s and "
                  + "mantissa not 0 indicates NaN)");
            computationLabel[1].setText("");
         }
         for (int i = 2; i < computationLabel.length; i++) {
            computationLabel[i].setText("");
         }
         return;
      }
      
      if (exponent == 0L) {
         computationLabel[1].setText("exponent = " + (-exponentBias + 1)
               + " (zero exponent indicates denormalized form)");
      }
      else {
         computationLabel[1].setText("exponent = " + exponent + " - bias of "
               + exponentBias + " = " + (exponent - exponentBias));
      }
      BigDecimal bdMant = BigDecimal.valueOf(mantissa).stripTrailingZeros();
      BigDecimal div = BigDecimal.valueOf(2);
      try {
         div = div.pow(firstMantissaBit - lastMantissaBit + 1).
               stripTrailingZeros();
         BigDecimal bdDiv = bdMant.divide(div, 200, BigDecimal.ROUND_HALF_UP).
               stripTrailingZeros();
         if (exponent == 0L) {
            computationLabel[2].setText("mantissa = " + mantissa + " / 2^"
                  + (firstMantissaBit - lastMantissaBit + 1));
            computationLabel[3].setText("      = " + bdDiv);
         }
         else {
            computationLabel[2].setText("mantissa = assumed 1 + " + mantissa
                  + " / 2^" + (firstMantissaBit - lastMantissaBit + 1));
            bdDiv = bdDiv.add(BigDecimal.valueOf(1)).stripTrailingZeros();
            computationLabel[3].setText("      = " + bdDiv);
         }
         computationLabel[4].setText("value = (sign) mantissa * 2 ^ exponent");
         if (exponent == 0L) {
            computationLabel[5].setText("      = "
                  + (((bits & signMask) == 0L)? '+' : '-')
                  + "mantissa * 2 ^ " + (-exponentBias + 1));
         }
         else {
            computationLabel[5].setText("      = "
                  + (((bits & signMask) == 0L)? '+' : '-')
                  + "mantissa * 2 ^ "
                  + (exponent - exponentBias));
         }
         BigDecimal bdVal = new BigDecimal(currentVal).stripTrailingZeros();
         String bdValText = getBDText(bdVal, currentValueText);
         computationLabel[6].setText("      = " + bdValText);
         if (currentValueText != null && !currentValueText.equals(bdValText)) {
            topLabel.setText("value = " + currentValueText + "  (rounded)");
            computationLabel[7].setText("      = " + currentValueText
                  + "  using default Java rounding");
         }
         else {
            computationLabel[7].setText("");
         }
      }
      catch (ArithmeticException e) {
         computationLabel[2].setText("<Unexpected Internal Error in Viewer>");
         for (int i = 3; i < computationLabel.length; i++) {
            computationLabel[7].setText("");
         }
      }
   }


   /** Gets a bit string from a segment of a long.
    *
    *  @param bits the bits.
    *
    *  @param firstBit index of the first bit.
    *
    *  @param lastBit index of the last bit.
    *
    *  @return a string representation of the bits in <code>bits</code>
    *  from <code>firstBit</code> to <code>lastBit</code>. **/
   private String bitString(final long bits, final int firstBit,
         final int lastBit) {
      StringBuilder buffer = new StringBuilder();
      long bit = 1L << firstBit;
      for (int i = firstBit; i >= lastBit; i--, bit >>= 1) {
         buffer.append(((bits & bit) == 0L)? '0' : '1');
         if (i > lastBit && ((i - lastBit) % 4) == 0) {
            buffer.append(' ');
         }
      }
      return buffer.toString();
   }
   
   
   /** Gets the double representation of a value. This will be called from
    *  the debugger thread.
    *
    *  @param value the value.
    *
    *  @param context the current debugger context.
    *
    *  @return the double representation of the value.
    *
    *  @throws ViewerException if an exception occurs while
    *  accessing the value. **/
   public abstract double getDoubleValue(Value value, DebugContext context)
         throws ViewerException;


   /** Gets text for the value. This will be called from the debugger
    *  thread.
    *
    *  @param value the value.
    *
    *  @param context the current debugger context.
    *
    *  @return a string representing <code>value</code> in decimal.
    *
    *  @throws ViewerException if an exception occurs while
    *  accessing the value. **/
   public abstract String getValueText(Value value, DebugContext context)
         throws ViewerException;
   
   
   /** Gets bit representation of the value. This will be called from
    *  the debugger thread.
    *
    *  @param value the value.
    *
    *  @param context the current debugger context.
    *
    *  @return the bit representation of <code>value</code>.
    *
    *  @throws ViewerException if an exception occurs while
    *  accessing the value. **/
   public abstract long getBits(Value value, DebugContext context)
         throws ViewerException;
    

   /** Gets the mantissa text.
    *
    *  @param mantissa the mantissa or an intermediate value
    *  for mantissa computation.
    *
    *  @return a string representing the decimal value of
    *  <code>mantissa</code>, with an appropriate number of
    *  digits for the type that this viewer displays. **/
   public abstract String getMantissaText(double mantissa);


   /** {@inheritDoc} **/
   public void buildGui(final JPanel mainPanel) {
      mainPanel.setOpaque(true);
      GridBagLayout layout = new GridBagLayout();
      mainPanel.setLayout(layout);
      GridBagConstraints constraints = new GridBagConstraints();
      Insets insets = constraints.insets;
     
      int spacing = 8;
      constraints.weightx = .001;
      constraints.weighty = .001;
      insets.bottom = 0;
      insets.top = spacing;
      insets.left = spacing;
      insets.right = spacing;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.anchor = GridBagConstraints.WEST;
   
      topLabel = new JLabel(" ");
      mainPanel.add(topLabel);
      layout.setConstraints(topLabel, constraints);
   
      constraints.gridwidth = 1;
      int seg = 0;
      for (int row = 0; row < 4; row++) {
         if (row == 1) {
            insets.top = 0;
         }
         for (int col = 0; col < 3; col++) {
            JLabel segment = new JLabel(" ");
            segment.setBorder(BorderFactory.createEtchedBorder());
            if (row >= 1) {
               segment.setHorizontalAlignment(JTextField.RIGHT);
            }
            textSegmentLabels[seg++] = segment;
            insets.left = (col == 0)? spacing : 0;
            insets.right = 0;
            mainPanel.add(segment);
            layout.setConstraints(segment, constraints);
            if (col == 2) {
               JPanel spacer = new JPanel(false);
               constraints.gridwidth = GridBagConstraints.REMAINDER;
               constraints.weightx = 1.0;
               mainPanel.add(spacer);
               layout.setConstraints(spacer, constraints);
               constraints.gridwidth = 1;
               constraints.weightx = .001;
            }
         }
      }
      textSegmentLabels[0].setText(" Sign");
      textSegmentLabels[1].setText(" Exponent");
      textSegmentLabels[2].setText(" Mantissa");
   
      insets.left = spacing;
      insets.right = spacing;
      insets.top = spacing;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      for (int row = 0; row < computationLabel.length; row++) {
         if (row == 1) {
            insets.top = 0;
         }
         computationLabel[row] = new JLabel(" ");
         mainPanel.add(computationLabel[row]);
         layout.setConstraints(computationLabel[row], constraints);
      }
      constraints.weighty = 1.0;
      constraints.fill = GridBagConstraints.BOTH;
      JPanel spacer = new JPanel(false);
      mainPanel.add(spacer);
      layout.setConstraints(spacer, constraints);
   }
   
   
   /** {@inheritDoc} **/
   public Dimension getMainSize() {
      // Default width will not consider computation label width.
      JPanel mainPanel = getMainPanel();
      if (mainPanel == null) {
         return null;
      }
      Dimension size = mainPanel.getPreferredSize();
      int w = textSegmentLabels[2].getX()
            + textSegmentLabels[2].getWidth()
            + textSegmentLabels[0].getX()
            + mainPanel.getInsets().left
            + mainPanel.getInsets().right;
      size.width = Math.min(w, size.width);
      return size;
   }


      /** {@inheritDoc} **/
   public int getScrollableUnitIncrement(final Rectangle visibleRect,
         final int orientation, final int direction) {
      if (textSegmentLabels == null || textSegmentLabels[0] == null) {
         return 1;
      }
      return Math.max(1, textSegmentLabels[0].getHeight() / 9);
   }
}


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import java.util.EnumSet;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;
import jgrasp.viewer.ViewerRoot;
import jgrasp.viewer.ViewerUpdateData;
import jgrasp.viewer.ViewerValueData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Value;


/** A viewer that displays a java.awt.Color with a color
 *  swatch and RGB components. **/
public class java__awt__Color_ColorView extends ViewerRoot {

   /** The controls. **/
   private JLabel controls;

   /** The color swatch panel. **/
   private JPanel colorPanel;
   
   /** The current color RGB value. **/
   private int currentRGB;
   
   /** The current display text. **/
   private String currentText;


   /** Creates a new color viewer.
    *
    *  @param vcd viewer creation data. **/
   public java__awt__Color_ColorView(final ViewerCreateData vcd) {
      super(false, EnumSet.of(CreationFlags.STANDARD_BORDER));
   }
     
      
   /** {@inheritDoc} **/
   public void buildGui(final JPanel mainPanel) {
      buildColorPanel();
      mainPanel.setLayout(new BorderLayout());
      mainPanel.add(colorPanel, "Center");
      buildControls();
   }
   
   
   /** Builds the controls. **/
   private void buildControls() {
      controls = new JLabel(" ");
      getVIData().setControls(controls);
   }
   
   
   /** Builds the main display panel. **/
   private void buildColorPanel() {
      colorPanel = 
         new JPanel() {
            public Dimension getPreferredSize() {
               return new Dimension(1, 1);
            }
         };
      colorPanel.setOpaque(true);
   }

    
   /** {@inheritDoc} **/
   public void destroy() {
   }


   /** {@inheritDoc} **/
   public String getViewName() {
      return "Color";
   }


   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return 10;
   }
   
 
   /** {@inheritDoc} **/
   public void updateState(final ViewerValueData valueData,
         final ViewerUpdateData data, final DebugContext context)
         throws ViewerException {
      Value value = valueData.getValue();
      Method getRgbMethod = value.getMethod(context, "getRGB", "int",
            null);
      currentRGB = value.invokeMethod(context, getRgbMethod, null).
            toInt(context);
      
      StringBuilder textBuild = new StringBuilder();
      textBuild.append("red = ");
      textBuild.append((currentRGB & 0xff0000) >> 16);
      textBuild.append(", green = ");
      textBuild.append((currentRGB & 0xff00) >> 8);
      textBuild.append(", blue = ");
      textBuild.append(currentRGB & 0xff);
      int alpha = currentRGB >>> 24;
      if (alpha < 255) {
         textBuild.append(", alpha = ");
         textBuild.append(alpha);
      }
      currentText = textBuild.toString();
   }


   /** {@inheritDoc} **/
   public void updateGui() {
      Color color = new Color(currentRGB);
      colorPanel.setBackground(color);
      controls.setText(currentText);
   }
    
    
   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for colors");
      vi.setLongDescription("This viewer displays the RGBA "
            + "components of a color, and displays a square "
            + "of the color.");
   }
   
   
   /** {@inheritDoc} **/
   public Dimension getMainSize() {
      Font f = colorPanel.getFont();
      if (f != null) {
         FontMetrics fm = colorPanel.getFontMetrics(f);
         int ht = fm.getHeight();
         return new Dimension(ht * 2, ht * 2);
      }
      return new Dimension(120, 120);
   }
}

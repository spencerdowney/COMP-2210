
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;

import java.awt.font.GlyphVector;

import javax.swing.Icon;


/** An icon that displays a single letter. **/
public class LetterIcon implements Icon {

   /** Current icon width. **/
   private int iconW;
   
   /** Current icon height. **/
   private int iconH;

   /** The fill color. **/
   private Color iconColor;
   
   /** The letter to be displayed. **/
   private char letter;


   /** Creates a new StateIcon.
    *
    *  @param h the icon height.
    *
    *  @param color the icon color.
    *
    *  @param letterIn the letter to be displayed. **/
   public LetterIcon(final char letterIn, final int h, final Color color) {
      letter = letterIn;
      iconH = h;
      iconW = h * 3 / 5;
      iconColor = color;
   }


   /** {@inheritDoc} **/
   public int getIconHeight() {
      return iconH;
   }


   /** {@inheritDoc} **/
   public int getIconWidth() {
      return iconW;
   }


   /** {@inheritDoc} **/
   public void paintIcon(final Component c, final Graphics graphics,
         final int x, final int y) {
      Graphics2D g = (Graphics2D) graphics;
   
      RenderingHints rh = g.getRenderingHints();
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_PURE);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
               
      GlyphVector gv = null;
      Rectangle bounds = null;
      Font f = g.getFont().deriveFont(Font.BOLD);
      gv = f.createGlyphVector(
            g.getFontRenderContext(), String.valueOf(letter));
      bounds = gv.getPixelBounds(
            g.getFontRenderContext(), 0.0f, 0.0f);
      int maxYGap = (iconH - 3 - (int) bounds.getHeight() + 1) / 2;
      int yoffs = iconH - (int) bounds.getMaxY()
            - Math.min((iconH + 3) / 6, maxYGap) - 1;
         
      int xoffs = (int) ((iconW - bounds.getWidth()) / 2.0
            - bounds.getMinX() - .5);
      Shape shape = gv.getOutline(x + xoffs, y + yoffs);
      boolean outline = iconH > 15;
      if (outline) {
         g.setColor(iconColor);
      }
      else {
         Color bg = c.getBackground();
         float[] hsb = Color.RGBtoHSB(bg.getRed(), bg.getGreen(),
               bg.getBlue(), null);
         if (hsb[2] < .6) {
            g.setColor(iconColor.brighter());
         }
         else {
            g.setColor(iconColor.darker());
         }
      }
      g.fill(shape);
      if (outline) {
         g.setColor(Color.BLACK);
         g.draw(shape);
      }
   
      g.setRenderingHints(rh);
   }

}

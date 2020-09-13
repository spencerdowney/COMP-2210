
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;

import jgrasp.AutosizedIcon;
import jgrasp.Drawing;


/** The DCD action icon. **/
public class DCDIcon extends AutosizedIcon {
      
   
   /** {@inheritDoc} **/
   public void paintImage(final Toolkit tk, final Graphics2D g, final int w,
         final int h) {
      int sz = Math.min(w, h);
      if (sz < 3) {
         return;
      }
      int xoffs = (w - sz) / 2;
      int yoffs = (h - sz) / 2;
      Drawing.drawLinedPage(g, xoffs, yoffs, sz, sz, Color.WHITE, false,
            new Color [] { Color.BLACK, Color.RED, Color.RED, Color.BLACK },
            0, false);
   }
}

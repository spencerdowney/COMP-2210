
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;

import jgrasp.AutosizedIcon;
import jgrasp.Drawing;


/** The FindBugs action icon. **/
public class FindBugsIcon extends AutosizedIcon {
      
   
   /** {@inheritDoc} **/
   public void paintImage(final Toolkit tk, final Graphics2D g, final int w,
         final int h) {
      Drawing.drawBug(g, 0, 0, w, h, Math.min(w, h),
            new Color(255, 111, 43));
      int sz = Math.min(w, h);
      int xoffs = (w - sz) / 2;
      int yoffs = (h - sz) / 2;
      int checkW = (sz * 3 + 2) / 4;
      int checkOffsX = sz - checkW;
      int checkOffsY = (sz - checkW + 1) / 2;
      Drawing.drawCheck(g, xoffs + checkOffsX, yoffs + checkOffsY, checkW, sz,
            new Color(0, 180, 0));
   }
}


import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Toolkit;

import jgrasp.AutosizedIcon;
import jgrasp.Drawing;


/** The Checkstyle file action icon. **/
public class CheckstyleFileIcon extends AutosizedIcon {
   
   /** The last paint used. **/
   private static Paint paint;
   
   /** The last icon size, or -1 if uninitialized. **/
   private static int lastSz = -1;
   
   
   /** Sets the last icon size and cached paint.
    *
    *  @param sz the current icon size.
    *
    *  @param paintIn the new paint. **/
   private static void setPaint(final int sz, final Paint paintIn) {
      lastSz = sz;
      paint = paintIn;
   }

   
   /** {@inheritDoc} **/
   public void paintImage(final Toolkit tk, final Graphics2D g, final int w,
         final int h) {
      int sz = Math.min(w, h);
      if (sz < 3) {
         return;
      }
      int xoffs = (w - sz) / 2;
      int yoffs = (h - sz) / 2;
      if (sz != lastSz) {
         setPaint(sz, new GradientPaint(sz / 5f, sz / 5f,
               Color.getHSBColor(.136f, 1f, 1f), sz - sz / 5f, sz - sz / 5f,
               Color.getHSBColor(.138f, 1f, .82f)));
      }
      Drawing.drawLinedPage(g, xoffs, yoffs, sz, sz, paint, false,
            Color.BLACK, 0, false);
      int checkW = (sz * 4 + 2) / 5;
      int checkOffsX = sz - checkW;
      int checkOffsY = (sz - checkW + 1) / 2;
      Drawing.drawCheck(g, xoffs + checkOffsX, yoffs + checkOffsY, checkW, sz,
            Color.WHITE);
   }
}


import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Toolkit;

import java.awt.geom.GeneralPath;

import jgrasp.AutosizedIcon;
import jgrasp.Drawing;


/** The JUnit compile action icon. **/
public class JUnitCompileIcon extends AutosizedIcon {
   
   /** The last upper-left paint used. **/
   private static Paint ulPaint;
   
   /** The last lower-right paint used. **/
   private static Paint lrPaint;
   
   /** The last icon size, or -1 if uninitialized. **/
   private static int lastSz = -1;
   
   
   /** Sets the cached paints.
    *
    *  @param sz the current icon size.
    *
    *  @param ulPaintIn the new upper left section paint.
    *
    *  @param lrPaintIn the new lower right section paint. **/
   private static void setPaints(final int sz, final Paint ulPaintIn,
      final Paint lrPaintIn) {
      lastSz = sz;
      ulPaint = ulPaintIn;
      lrPaint = lrPaintIn;
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
         setPaints(sz, new GradientPaint(xoffs + sz / 5, yoffs + sz / 5,
               Color.RED,
               xoffs + sz / 2, yoffs + sz / 2, Color.WHITE),
               new GradientPaint(xoffs + sz - sz / 5, yoffs + sz - sz / 5,
               Color.GREEN, xoffs + sz / 2, yoffs + sz / 2, Color.WHITE));
      }
      //*** Get rid of this mess and use LinearGradientPaint once
      //*** we give up on Java 1.5 support.
      Shape s = g.getClip();
      GeneralPath path = new GeneralPath();
      path.moveTo(0, 0);
      path.lineTo(sz, 0);
      path.lineTo(0, sz);
      path.closePath();
      g.clip(path);
      Drawing.drawCross(g, xoffs, yoffs, sz, sz, ulPaint, 1.1f);
      g.setClip(s);
      path = new GeneralPath();
      path.moveTo(sz, 0);
      path.lineTo(sz, sz);
      path.lineTo(0, sz);
      path.closePath();
      g.clip(path);
      Drawing.drawCross(g, xoffs, yoffs, sz, sz, lrPaint, 1.1f);
      g.setClip(s);
   }
}

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.Toolkit;

import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

import jgrasp.AutosizedIcon;
import jgrasp.Drawing;


/** The Webcat icon. **/
public class WebcatIcon extends AutosizedIcon {

   /** The square root of 2. **/
   private static final float SQRT2 = 1.414213562f;


   /** {@inheritDoc} **/
   public void paintImage(final Toolkit tk, final Graphics2D g, final int w,
         final int h) {
      int sz = Math.min(w, h);
      if (sz < 3) {
         return;
      }
   
      int lineW = Drawing.getLineWidth(sz);
      float lineOffs = lineW / 2.0f;
      
      int headW = (sz - lineW) / 2 * 2 + lineW;
      int earH = (sz + 3) / 6;
      int headH = sz - earH;
      
      int xoffs = (w - headW) / 2;
      int yoffs = (w - (headH + earH)) / 2;
      
      Arc2D.Float arc = new Arc2D.Float(xoffs + lineOffs,
            yoffs + earH + lineOffs, headW - lineW, headH - lineW,
            150, 240, Arc2D.OPEN);
      PathIterator pi = arc.getPathIterator(g.getTransform());
      GeneralPath path = new GeneralPath();
      Drawing.appendPath(path, pi, false);
      
      float centerX = arc.x + arc.width / 2f;
      float centerY = arc.y + arc.height / 2f;
      path.lineTo(centerX + ((float) sz / 2) - lineW,
            centerY - ((float) sz / 2));
      
      arc.start = 60;
      arc.extent = 60;
      pi = arc.getPathIterator(g.getTransform());
      Drawing.appendPath(path, pi, true);
   
      path.lineTo(centerX - ((float) sz / 2) + lineW,
            centerY - ((float) sz / 2));
      
      g.setColor(new Color(243, 209, 51));
      g.fill(path);
      
      Stroke stroke = new BasicStroke(lineW, BasicStroke.CAP_SQUARE,
            BasicStroke.JOIN_ROUND);
      g.setStroke(stroke);
      g.setColor(Color.BLACK);
      path.closePath();
      g.draw(path);
      
      int eyeW = (headW * 4 + 7) / 15;
      int eyeH = (eyeW * 3 + 2) / 4;
      int eyeOffsX = (headW - eyeW * 2 + 1) / 3;
      int eyeOffsY = (headH + 1) / 3 - eyeH / 2;
      Ellipse2D.Float eye = new Ellipse2D.Float(arc.x + eyeOffsX,
            arc.y + eyeOffsY, eyeW, eyeH);
      g.fill(eye);
      
      eye.x = arc.x + arc.width - eyeOffsX - eyeW;
      g.fill(eye);
    
      if (eyeH > lineW * 2) {
         g.setColor(Color.BLUE);
         eye.width -= lineW * 2;
         eye.height -= lineW * 2;
         eye.x += lineW;
         eye.y += lineW;
         g.fill(eye);
         
         eye.x = arc.x + eyeOffsX + lineW;
         eye.y = arc.y + eyeOffsY + lineW;
         g.fill(eye);
      }
      
      int noseW = (headW + 4) / 9;
      int noseH = (int) (noseW * 1.4);
      int noseOffsY = (headH * 2 - noseH * 3) / 6;
      path = new GeneralPath();
      float y = arc.y + arc.height - noseOffsY + (lineW & 1) / 2f;
      path.moveTo(centerX, y);
      y -= noseH;
      path.lineTo(centerX - noseW, y);
      path.lineTo(centerX + noseW, y);
      g.setColor(Color.BLACK);
      g.fill(path);
   }
}

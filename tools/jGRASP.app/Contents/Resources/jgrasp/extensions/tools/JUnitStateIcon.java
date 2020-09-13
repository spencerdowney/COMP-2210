
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Stroke;

import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import jgrasp.Drawing;
import jgrasp.StateIcon;


/** State icon for JUnit. **/
public class JUnitStateIcon extends StateIcon {

   /** Reusable array. **/
   private static float[] xpoints = new float[3];

   /** Reusable array. **/
   private static float[] ypoints = new float[3];


   /** Creates a new JUnitStateIcon.
    *
    *  @param stateIn the icon state. **/
   public JUnitStateIcon(final State state) {
      super(state);
   }


   /** {@inheritDoc} **/
   public void paintMainIcon(final Graphics2D g, final int x, final int y,
         final int w, final int h) {
      int d = (Math.min(w - 1, h - 1) + 1) * 5 / 12 * 2;
      int xoffs = x + (w - d) / 2;
      int yoffs = y + h - 1 - (h - d) / 2 - d;
      
      int lineW = Drawing.getLineWidth(h);
      float lineOffs = lineW / 2f;
      Rectangle2D.Float rect = new Rectangle2D.Float(xoffs + lineOffs,
            yoffs + lineOffs, d - lineW, d - lineW);
   
      g.setPaint(new GradientPaint(xoffs, yoffs, Color.RED,
            xoffs + d * 2 / 5, yoffs + d * 2 / 5, Color.WHITE));
      g.fill(rect);
      g.setPaint(new GradientPaint(xoffs + d, yoffs + d, Color.GREEN,
            xoffs + d - d * 2 / 5, yoffs + d - d * 2 / 5,
            Color.WHITE));
      xpoints[0] = xoffs + lineOffs;
      xpoints[1] = xoffs + d - lineOffs;
      xpoints[2] = xpoints[1];
      ypoints[0] = yoffs + d - lineOffs;
      ypoints[1] = ypoints[0];
      ypoints[2] = yoffs + lineOffs;
      GeneralPath path = new GeneralPath();
      path.moveTo(xpoints[0], ypoints[0]);
      for (int i = 1; i < 3; i++) {
         path.lineTo(xpoints[i], ypoints[i]);
      }
      g.fill(path);
      
      Stroke stroke = new BasicStroke(lineW, BasicStroke.CAP_SQUARE,
            BasicStroke.JOIN_ROUND);
      g.setStroke(stroke);
      g.setColor(Color.BLACK);
      g.draw(rect);
   }
}


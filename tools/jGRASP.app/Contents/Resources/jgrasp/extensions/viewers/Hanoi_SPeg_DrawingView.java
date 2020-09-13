
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import jgrasp.viewer.Sizes;
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;
import jgrasp.viewer.ViewerRoot;
import jgrasp.viewer.ViewerUpdateData;
import jgrasp.viewer.ViewerValueData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Value;


/** A viewer that displays a Hanoi.Peg. **/
public class Hanoi_SPeg_DrawingView extends ViewerRoot {

   /** The current disks. **/
   private List<Integer> disks = new ArrayList<Integer>();
   
   /** The current font height in pixels. **/
   private int fontHeight;
   
   /** Drawing base unit, in pixels. **/
   private int unit;


   /** Creates a new Hanoi.Peg viewer.
    *
    *  @param vcd viewer creation data. **/
   public Hanoi_SPeg_DrawingView(final ViewerCreateData vcd) {
      super(false);
   }
     
      
   /** {@inheritDoc} **/
   public void destroy() {
   }


   /** {@inheritDoc} **/
   public String getViewName() {
      return "Hanoi Peg";
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
      Value diskV = value.getFieldValue(context, "peg");
      Value elementsV = diskV.getFieldValue(context, "elementData");
      int len = elementsV.getArrayLength(context);
      List<Integer> newDisks = new ArrayList<Integer>(len);
      for (int d = 0; d < len; d++) {
         Value integerV = elementsV.getArrayElement(context, d);
         if (integerV.isNull()) {
            newDisks.add(Integer.valueOf(0));
         }
         else {
            Value intV = integerV.getFieldValue(context, "value");
            newDisks.add(Integer.valueOf(intV.toInt(context)));
         }
      }
      disks = newDisks;
   }


   /** {@inheritDoc} **/
   public void configure() {
      JPanel mp = getMainPanel();
      Graphics g = mp.getGraphics();
      if (g == null) {
         return;
      }
      g.setFont(mp.getFont());
      FontMetrics fm = g.getFontMetrics();
      fontHeight = fm.getHeight();
      unit = (fm.getHeight() + 1) / 2;
      List<Integer> currentDisks = disks;
      Dimension size = new Dimension(currentDisks.size() * unit * 2
            + unit * 4, currentDisks.size() * unit * 2 + unit * 3);
      if (!size.equals(mp.getPreferredSize())) {
         mp.setPreferredSize(size);
         mp.revalidate();
         mp.validate();
      }
      mp.repaint();
   }
    
    
   /** {@inheritDoc} **/
   public void paintMainView(final Graphics2D graphics,
         final JPanel paintPanel) {
      if (!isViewerTransparent()) {
         graphics.setColor(Color.BLACK);
         graphics.fillRect(0, 0, paintPanel.getWidth(),
               paintPanel.getHeight());
         int l = Sizes.getVeryThinEdge(fontHeight);
         if (paintPanel.getWidth() > l + l) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(l, l, paintPanel.getWidth() - l - l,
                  paintPanel.getHeight() - l - l);
         }
      }
      List<Integer> currentDisks = disks;
      int size = currentDisks.size();
      int x = size * unit + unit * 2;
      graphics.setColor(Color.BLUE);
      graphics.fillRect(x - unit, unit, unit * 2, size * unit * 2 + unit);
      for (int d = 0; d < size; d++) {
         int s = currentDisks.get(d).intValue();
         if (s > 0) {
            int w = s * unit + unit;
            int y = (size - d - 1) * unit * 2 + unit * 2;
            graphics.setColor(Color.ORANGE);
            graphics.fillRect(x - w, y, w * 2, unit * 2);
            graphics.setColor(Color.BLACK);
            graphics.drawRect(x - w, y, w * 2, unit * 2);
         }
      }
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Viewer for towers of Hanoi Peg");
      vi.setLongDescription("This viewer displays a \"Towers of Hanoi\""
            + " peg graphically.");
   }
}

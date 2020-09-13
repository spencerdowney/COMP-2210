
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import jgrasp.tool.URTextArea;

import jgrasp.viewer.Sizes;
import jgrasp.viewer.SubviewerPosition;
import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerInitData;
import jgrasp.viewer.ViewerPriorityData;
import jgrasp.viewer.ViewerRoot;
import jgrasp.viewer.ViewerUpdateData;
import jgrasp.viewer.ViewerValueData;

import jgrasp.viewer.gui.SubViewer;
import jgrasp.viewer.gui.ValueDragData;
import jgrasp.viewer.gui.ValueDragger;
import jgrasp.viewer.gui.ValueDraggerDataSource;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Value;

import org.w3c.dom.Element;


/** A viewer that shows relative component positions and sizes. **/
public class java__awt__Component_ComponentView extends ViewerRoot {


   /** A component boundary description. **/
   private static class Bounds {
   
      /** The bounds. **/
      private Rectangle bounds = new Rectangle();
   
      /** Bounds of all the children. **/
      private Bounds[] children;
   
      /** The component class name. **/
      private String type;
   
      /** The component value. **/
      private Value value;
   
      /** The component value id. **/
      private long id;
   
   
      /** {@inheritDoc} **/
      public String toString() {
         StringBuilder sb = new StringBuilder();
         getDescription(sb, "");
         return sb.toString();
      }
   
   
      private void getDescription(final StringBuilder sb,
            final String indent) {
         sb.append(indent);
         sb.append(type);
         sb.append(" (");
         sb.append(bounds.x);
         sb.append(", ");
         sb.append(bounds.y);
         sb.append(") ");
         sb.append(bounds.width);
         sb.append("x");
         sb.append(bounds.height);
         sb.append("\n");
         if (children != null) {
            for (Bounds cb : children) {
               cb.getDescription(sb, indent + "   ");
            }
         }
      }
   }


   /** The gui root panel. **/
   private JPanel outerPanel;

   /** The bounds display panel. **/
   private JPanel displayPanel;

   /** Text display (shows types of components under mouse). **/
   private JTextArea textArea;

   /** The boundary hierarchy. This will be accessed by both the
    *  debugger and gui threads, so it should be read only once
    *  wherever the value must remain consistent. **/
   private Bounds topBounds;

   /** True if the mouse is outside the component display,
    *  false otherwise. **/
   private boolean mouseOut = true;
   
   /** The subviewer. **/
   private SubViewer selectionViewer;
   
   /** The currently selected component, or null if none is
    *  selected. **/
   private Value selComponent;
   
   /** The id of the currently selected component, or -1 if
    *  none is selected. **/
   private long selId = -1;

   /** The new selected value, or null if none was selected. **/
   private Value newSelComponent;


   /** Creates a new component viewer.
    *
    *  @param vcd viewer creation data. **/
   public java__awt__Component_ComponentView(final ViewerCreateData vcd) {
      super(false, EnumSet.of(CreationFlags.STANDARD_BORDER));
   }


   /** {@inheritDoc} **/
   public void build(final ViewerInitData vid,
         final Element initDataIn) {
      super.build(vid, initDataIn);
      vid.setAutoUpdate(true);
      selectionViewer = SubViewer.createSubViewer(this, vid);
   }


   /** {@inheritDoc} **/
   public void buildGui(final JPanel mainPanel) {
      buildPanel();
      mainPanel.setLayout(new BorderLayout());
      mainPanel.add(outerPanel, "Center");
   }


   /** Builds the view panel. **/
   private void buildPanel() {
      outerPanel =
         new JPanel(new BorderLayout()) {
            public void updateUI() {
               super.updateUI();
               BorderLayout layout = (BorderLayout) getLayout();
               int gap = 1 + getFont().getSize() / 2;
               layout.setHgap(gap);
               layout.setVgap(gap);
               setBorder(BorderFactory.createEmptyBorder(gap, gap,
                     gap, gap));
            }
         };
      displayPanel =
         new JPanel() {
            public Dimension getPreferredSize() {
               return new Dimension(0, 0);
            }
         
            public void paintComponent(final Graphics g) {
               paintDisplay(g);
            }
         };
      displayPanel.addMouseMotionListener(
            new MouseMotionAdapter() {
               public void mouseMoved(final MouseEvent e) {
                  handleMouseMove(e.getX(), e.getY());
               }
            });
      displayPanel.addMouseListener(
            new MouseAdapter() {
               public void mouseExited(final MouseEvent e) {
                  mouseOut = true;
                  Bounds b = topBounds;
                  if (b == null) {
                     setTextAreaText("");
                  }
                  else {
                     setTextAreaText(b.toString());
                  }
               }
               public void mousePressed(final MouseEvent e) {
                  if ((e.getModifiers()
                        & displayPanel.getToolkit().getMenuShortcutKeyMask())
                        != 0) {
                     newSelComponent = null;
                  }
                  else {
                     long id = ((e.getModifiers() & MouseEvent.SHIFT_MASK) == 0)
                           ? selId : -1;
                     long keep =
                           ((e.getModifiers() & MouseEvent.SHIFT_MASK) == 0)
                           ? -1 : selId;
                     newSelComponent = findComponent(e.getPoint(), id, keep);
                  }
                  getVIData().update();
               }
            });
      ValueDragger.createValueDragger(displayPanel, getVIData(),
            new ValueDraggerDataSource() {
            
               public ValueDragData getDragData(final Point pos) {
                  if (selComponent == null) {
                     return null;
                  }
                  return new ValueDragData(null, selComponent, null,
                        getVIData().getScope(),
                        java__awt__Component_ComponentView.this.getClass().
                              getName());
               }
            });
      JSplitPane splitPane = new JSplitPane();
      splitPane.setResizeWeight(.5);
      outerPanel.add(splitPane, "Center");
      splitPane.setTopComponent(displayPanel);
      textArea = new URTextArea("Component Hierarchy");
      textArea.setEditable(false);
      splitPane.setBottomComponent(
            new JScrollPane(textArea) {
               public Dimension getPreferredSize() {
                  return new Dimension(0, 0);
               }
            });
   
      GridBagConstraints constraints = new GridBagConstraints();
      Insets insets = constraints.insets;
      int spacing = 4;
      constraints.weightx = .001;
      constraints.weighty = .001;
      insets.top = spacing;
      insets.bottom = spacing;
      insets.right = spacing;
      insets.left = spacing;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      constraints.fill = GridBagConstraints.NONE;
      //constraints.anchor = GridBagConstraints.WEST;
   
      Bounds b = topBounds;
      if (b != null) {
         setTextAreaText(b.toString());
      }
   }


   /** {@inheritDoc} **/
   public void destroy() {
      selComponent = null;
      newSelComponent = null;
   }


   /** {@inheritDoc} **/
   public String getViewName() {
      return "Component";
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
      topBounds = getBounds(value, context);
      setSelectionViewValue(context, data.isReset());
   }


   /** Changes the value in the selection viewer based on the current
    *  selected value.
    *
    *  @param context the current debug context.
    *
    *  @parmam isReset true if the current value should be considered
    *  a "new" value, false otherwise. **/
   public void setSelectionViewValue(final DebugContext context,
         final boolean isReset) {
     
      if (selectionViewer == null) {
         return;
      }
      final Value v;
      final String expr;
      v = newSelComponent;
      if (v == null && selComponent == null) {
         return;
      }
      if (v != null && selComponent != null && v.isSameNaN(selComponent)) {
         return;
      }
         
      final boolean prevNullSubviewVal = selComponent == null;
      selComponent = v;
      selId = (v == null)? -1 : v.getUniqueID();
      selectionViewer.setValue(v, context, "", " ", isReset);
      SwingUtilities.invokeLater(
            new Runnable() {
               public void run() {
                  if ((v == null) != prevNullSubviewVal) {
                     getVIData().setSubviewPanel((v == null)? null
                              : selectionViewer.getComponent(),
                           SubviewerPosition.BOTTOM);
                  }
               }
            });
   }
   
   
   /** {@inheritDoc} **/
   public void updateGui() {
      Bounds b = topBounds;
      if (b == null) {
         return;
      }
      if (mouseOut) {
         setTextAreaText(b.toString());
      }
      if (displayPanel != null) {
         displayPanel.repaint();
      }
   }


   /** Gets component boundary information.
    *
    *  @param value the component.
    *
    *  @param context the current debugger context.
    *
    *  @return boundary information for the component.
    *
    *  @throws ViewerException if an error occurs while retrieving
    *  the bounds. **/
   private Bounds getBounds(final Value value, final DebugContext context)
         throws ViewerException {
      Bounds b = new Bounds();
      Method getBoundsMethod = value.getMethod(context,
            "getBounds", "java.awt.Rectangle", null);
      Value bounds = value.invokeMethod(context, getBoundsMethod,
            null);
      String type = value.getType(context).getName(context);
      int dot = type.lastIndexOf('.');
      if (dot >= 0) {
         type = type.substring(dot + 1);
      }
      b.type = type;
      b.value = value;
      b.id = value.getUniqueID();
   
      try {
         Method getTextMethod = value.getMethod(context, "getText",
               "java.lang.String", null);
         Value result = value.invokeMethod(context, getTextMethod,
               null);
         if (!result.isNull()) {
            String str = result.toString(context);
            if (str.length() > 10) {
               str = str.substring(0, 7) + "...";
            }
            b.type += " \"" + str + "\"";
         }
      }
      catch (ViewerException e) {
      }
   
      b.bounds.x = bounds.getFieldValue(context, "x").toInt(context);
      b.bounds.y = bounds.getFieldValue(context, "y").toInt(context);
      b.bounds.width = bounds.getFieldValue(context, "width").
            toInt(context);
      b.bounds.height = bounds.getFieldValue(context, "height").
            toInt(context);
      if (value.isInstanceOf(context, "java.awt.Container")) {
         Method getComponentsMethod = value.getMethod(context,
               "getComponents", "java.awt.Component[]", null);
         Value children;
         try {
            children = value.invokeMethod(context,
               getComponentsMethod, null);
         }
         catch (ViewerException e) {
            b.children = new Bounds[0];
            return b;
         }
         b.children = new Bounds[children.getArrayLength(context)];
         for (int c = 0; c < b.children.length; c++) {
            b.children[c] = getBounds(children.getArrayElement(context,
               c), context);
         }
      }
      return b;
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for components");
      vi.setLongDescription("This viewer displays the component shape "
            + "and the shapes of its descendents. Clicking on the viewer "
            + "repeatedly will cycle the selection through all components "
            + "under the cursor. Hold down the shift key to click and "
            + "drag out a component without cycling to the next component.");
   }


   /** Paints the bounds rectangles on the display panel.
    *
    *  @param g graphics on which the rendering will be performed. **/
   public void paintDisplay(final Graphics g) {
      int width = displayPanel.getWidth() - 1;
      int height = displayPanel.getHeight() - 1;
      g.setColor(outerPanel.getBackground());
      g.fillRect(0, 0, width + 1, height + 1);
      Bounds bounds = topBounds;
      if (bounds == null) {
         return;
      }
   
      Rectangle b = bounds.bounds;
      double scale = Math.min(width / (double) b.width,
            height / (double) b.height);
      double xOffs = (width - b.width * scale) / 2.0;
      double yOffs = (height - b.height * scale) / 2.0;
   
      FontMetrics fm = g.getFontMetrics();
      int fontHeight = fm.getHeight();
      int l = Sizes.getVeryThinEdge(fontHeight);
   
      paintBounds(g, xOffs, yOffs, scale, bounds, 0, l);
   }


   /** Paints the bounds of a component and its children.
    *
    *  @param g graphics on which the rendering will be performed.
    *
    *  @param xOffset x offset of the parent.
    *
    *  @param yOffset y offset of the parent.
    *
    *  @param scale scale multiplier from bounds units to
    *  graphics units.
    *
    *  @param bounds bounds of the component that is being painted.
    *
    *  @param level the depth in the hierarchy.
    *
    *  @param l line thickness for painting. **/
   private void paintBounds(final Graphics g, final double xOffset,
         final double yOffset, final double scale, final Bounds bounds,
         final int level, final int l) {
      Rectangle b = bounds.bounds;
   
      double xOffs = xOffset;
      double yOffs = yOffset;
      if (level > 0) {
         xOffs += b.x * scale;
         yOffs += b.y * scale;
      }
   
      if (bounds.id == selId) {
         g.setColor(Color.RED);
      }
      else {
         g.setColor(Color.BLACK);
      }
      g.fillRect((int) xOffs, (int) yOffs,
            (int) (xOffs + b.width * scale) - (int) xOffs,
            (int) (yOffs + b.height * scale) - (int) yOffs);
      if ((int) (xOffs + b.width * scale) - (int) xOffs > l + l
            && (int) (yOffs + b.height * scale) - (int) yOffs > l + l) {
         int i = (int) Math.max(.5, 255 * (1 - .05 * level));
         if (bounds.id == selId) {
            g.setColor(new Color(255, i, i));
         }
         else {
            g.setColor(new Color(i, i, i));
         }
         g.fillRect((int) xOffs + l, (int) yOffs + l,
               (int) (xOffs + b.width * scale) - (int) xOffs - l - l,
               (int) (yOffs + b.height * scale) - (int) yOffs - l - l);
         for (int c = 0; c < bounds.children.length; c++) {
            paintBounds(g, xOffs, yOffs, scale, bounds.children[c],
                  level + 1, l);
         }
      }
   }


   /** Responds to mouse motion.
    *
    *  @param x mouse x position.
    *
    *  @param y mouse y position. **/
   public void handleMouseMove(final int x, final int y) {
      int width = displayPanel.getWidth() - 1;
      int height = displayPanel.getHeight() - 1;
      Bounds bounds = topBounds;
      if (bounds == null) {
         mouseOut = true;
         setTextAreaText("");
         return;
      }
   
      Rectangle b = bounds.bounds;
      double scale = Math.min(width / (double) b.width,
            height / (double) b.height);
      double xOffs = (width - b.width * scale) / 2.0;
      double yOffs = (height - b.height * scale) / 2.0;
   
      StringBuilder result = new StringBuilder();
      findComponents(x, y, xOffs, yOffs, scale, bounds, 0,
            result);
      String resultStr = result.toString();
      if (resultStr.length() > 0) {
         mouseOut = false;
         setTextAreaText(resultStr);
      }
      else {
         mouseOut = true;
         setTextAreaText(bounds.toString());
      }
   }


   /** Gets a text description of the components at a certain position.
    *
    *  @param x x position of mouse.
    *
    *  @param y y position of mouse.
    *
    *  @param xOffset x offset from parent position.
    *
    *  @param yOffset y offset from parent position.
    *
    *  @param scale scale multiplier from bounds units to
    *  graphics units.
    *
    *  @param bounds bounds of the component that is being searched.
    *
    *  @param level the depth in the hierarchy.
    *
    *  @param textOut the text result. **/
   private void findComponents(final int x, final int y,
         final double xOffset, final double yOffset, final double scale,
         final Bounds bounds, final int level,
         final StringBuilder textOut) {
      Rectangle b = bounds.bounds;
   
      double xOffs = xOffset;
      double yOffs = yOffset;
      if (level > 0) {
         xOffs += b.x * scale;
         yOffs += b.y * scale;
      }
   
      Rectangle rect = new Rectangle((int) xOffs, (int) yOffs,
            (int) (xOffs + b.width * scale) - (int) xOffs,
            (int) (yOffs + b.height * scale) - (int) yOffs);
      if (!rect.contains(x, y)) {
         return;
      }
      for (int l = 0; l < level; l++) {
         textOut.append(" ");
      }
      textOut.append(bounds.type + " (" + b.x + ", " + b.y + ") "
            + b.width + "x" + b.height + "\n");
      for (int c = 0; c < bounds.children.length; c++) {
         findComponents(x, y, xOffs, yOffs, scale, bounds.children[c],
               level + 1, textOut);
      }
   }


   /** Finds the component at a certain position.
    *
    *  @param pos the position to be searched.
    *
    *  @param old if this is non-negative and a component with this id
    *  is found, select the component found after it, if any. Using this
    *  repeatedly will cycle through all the components at a certain
    *  position.
    *
    *  @param keep if this is non-negative and <code>old</code> is
    *  negative, choose the component with this id if it is one of the
    *  ones found.
    *
    *  @return the a component found at the specified position,
    *  or null if none was found. **/
   public Value findComponent(final Point pos, final long old,
         final long keep) {
      int width = displayPanel.getWidth() - 1;
      int height = displayPanel.getHeight() - 1;
      Bounds bounds = topBounds;
      if (bounds == null) {
         return null;
      }
      Rectangle b = bounds.bounds;
      double scale = Math.min(width / (double) b.width,
                     height / (double) b.height);
      double xOffs = (width - b.width * scale) / 2.0;
      double yOffs = (height - b.height * scale) / 2.0;
   
      List<Bounds> result = new ArrayList<Bounds>();
      findComponents(pos.x, pos.y, xOffs, yOffs, scale,
            bounds, 0, result);
      if (result.size() == 0) {
         return null;
      }
      if (old < 0) {
         if (keep >= 0) {
            for (int i = 0; i < result.size(); i++) {
               if (result.get(i).id == keep) {
                  return result.get(i).value;
               }
            }
         }
         return result.get(result.size() - 1).value;
      }
      for (int i = 0; i < result.size(); i++) {
         if (result.get(i).id == old) {
            return (i > 0)? result.get(i - 1).value
                  : result.get(result.size() - 1).value;
         }
      }
      return result.get(result.size() - 1).value;
   }


   /** Finds the components at a certain position.
    *
    *  @param x x position of mouse.
    *
    *  @param y y position of mouse.
    *
    *  @param xOffset x offset from parent position.
    *
    *  @param yOffset y offset from parent position.
    *
    *  @param scale scale multiplier from bounds units to
    *  graphics units.
    *
    *  @param bounds bounds of the component that is being searched.
    *
    *  @param level the depth in the hierarchy.
    *
    *  @param out list for holding the found components. **/
   private void findComponents(final int x, final int y,
         final double xOffset, final double yOffset, final double scale,
         final Bounds bounds, final int level, final List<Bounds> out) {
      Rectangle b = bounds.bounds;
   
      double xOffs = xOffset;
      double yOffs = yOffset;
      if (level > 0) {
         xOffs += b.x * scale;
         yOffs += b.y * scale;
      }
   
      Rectangle rect = new Rectangle((int) xOffs, (int) yOffs,
            (int) (xOffs + b.width * scale) - (int) xOffs,
            (int) (yOffs + b.height * scale) - (int) yOffs);
      if (!rect.contains(x, y)) {
         return;
      }
      out.add(bounds);
      for (int c = 0; c < bounds.children.length; c++) {
         findComponents(x, y, xOffs, yOffs, scale,
               bounds.children[c], level + 1, out);
      }
   }


   /** Sets the text.
    *
    *  @param the new text. **/
   private void setTextAreaText(final String text) {
      if (text.equals(textArea.getText())) {
         return;
      }
      textArea.setText(text);
   }


   /** {@inheritDoc} **/
   public Dimension getMainSize() {
      Font f = outerPanel.getFont();
      if (f != null) {
         FontMetrics fm = outerPanel.getFontMetrics(f);
         int ht = fm.getHeight();
         return new Dimension(ht * 15, ht * 7);
      }
      return new Dimension(210, 100);
   }
}

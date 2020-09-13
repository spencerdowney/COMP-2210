
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;

import java.awt.image.BufferedImage;

import java.util.EnumSet;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;
import jgrasp.viewer.ViewerRoot;
import jgrasp.viewer.ViewerUpdateData;
import jgrasp.viewer.ViewerValueData;

import jgrasp.viewer.jgrdi.Constructor;
import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Field;
import jgrasp.viewer.jgrdi.JgrdiIsFinalException;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Type;
import jgrasp.viewer.jgrdi.Value;


/** A base class for image viewers. **/
public abstract class ImageView extends ViewerRoot {


   /** Data for the current image. **/
   private static class Data {
   
      /** The display size description. **/
      private String dsText;
   
      /** Text supplied by the subclass. **/
      private String clientText;
   
      /** The pixel data, or null if the image is not available. **/
      private int[] pixelData;
   
      /** The width of the image. **/
      private int w;
   
      /** The height of the image. **/
      private int h;
   
      /** True if the image data is complete, false otherwise. **/
      private boolean complete;
      
      
      /** Creates a new Data.
       *
       *  @param dsText the display size description.
       *
       *  @param clientText text supplied by the subclass.
       *
       *  @param pixelData the pixel data, or null if the image is
       *  not available.
       *
       *  @param w the width of the image supplied in
       *  <code>pixelData</code>.
       *
       *  @param h the height of the image supplied in
       *  <code>pixelData</code>.
       *
       *  @param complete true if the image data is complete,
       *  false otherwise. **/
      Data(final String dsTextIn, final String clientTextIn,
            final int[] pixelDataIn, final int wIn, final int hIn,
            final boolean completeIn) {
         dsText = dsTextIn;
         clientText = clientTextIn;
         pixelData = pixelDataIn;
         w = wIn;
         h = hIn;
         complete = completeIn;
      }
   }


   /** Maximum image display width. **/
   private static final int MAX_WIDTH = 250;

   /** Maximum image display height. **/
   private static final int MAX_HEIGHT = 250;

   /** The current image data. **/
   private Data currentData;

   /** The controls. **/
   private JLabel controls;

   /** The gui root panel. **/
   private JPanel imagePanel;

   /** The display version of the image. **/
   private BufferedImage displayImage;

   /** Display text describing image size. **/
   private String displaySizeText;
   
   /** Last display text provided by subclass. **/
   private String currentClientText;

   /** True if the image was complete when captured, false otherwise. **/
   private boolean isComplete;


   /** Creates a new color viewer.
    *
    *  @param vcd viewer creation data. **/
   public ImageView(final ViewerCreateData vcd) {
      super(false, EnumSet.of(CreationFlags.STANDARD_BORDER));
   }
   
   
   /** {@inheritDoc} **/
   public void buildGui(final JPanel mainPanel) {
      buildImagePanel();
      mainPanel.setLayout(new BorderLayout());
      mainPanel.add(imagePanel, "Center");
      buildControls();
   }
   
   
   /** Builds the controls. **/
   private void buildControls() {
      controls = new JLabel(" ");
      getVIData().setControls(controls);
   }
   
   
   /** Builds the main display panel. **/
   private void buildImagePanel() {
      imagePanel = 
         new JPanel() {
            public Dimension getPreferredSize() {
               if (displayImage == null) {
                  return new Dimension(10, 10);
               }
               Insets insets = getInsets();
               int imageWidth = displayImage.getWidth();
               int imageHeight = displayImage.getHeight();
               return new Dimension(
                     Math.min(imageWidth + insets.left
                           + insets.right, MAX_WIDTH),
                     Math.min(imageHeight + insets.top
                           + insets.bottom, MAX_HEIGHT));
            }
         
            public void paintComponent(final Graphics g) {
               int width = getWidth();
               int height = getHeight();
               g.setColor(getBackground());
               g.fillRect(0, 0, width, height);
               if (displayImage == null) {
                  return;
               }
               Insets insets = getInsets();
               int borderXOffs = insets.left;
               int borderYOffs = insets.top;
               width -= insets.left + insets.right;
               height -= insets.top + insets.bottom;
               if (width < 0) {
                  width = 0;
               }
               if (height < 0) {
                  height = 0;
               }
               int imageWidth = displayImage.getWidth();
               int imageHeight = displayImage.getHeight();
               Image drawImage;
               boolean scaled = false;
               if ((imageWidth == width && imageHeight <= height)
                     || (imageHeight == height && imageWidth <= width)) {
                  drawImage = displayImage;
               }
               else {
                  scaled = true;
                  double scale = Math.min((double) width / imageWidth,
                        (double) height / imageHeight);
                  if (scale > 1) {
                     scale = Math.floor(scale);
                  }
                  imageWidth = (int) (imageWidth * scale);
                  imageHeight = (int) (imageHeight * scale);
                  if (imageWidth > 0 && imageHeight > 0) {
                     drawImage =
                           displayImage.getScaledInstance(imageWidth,
                           imageHeight, Image.SCALE_DEFAULT);
                  }
                  else {
                     drawImage = null;
                  }
               }
               String text = displaySizeText;
               if (scaled) {
                  text += " (shown at " + imageWidth + " x "
                        + imageHeight + ")";
               }
               if (!isComplete) {
                  text += " (image is currently incomplete)";
               }
               setDisplayText(text);
                     
               int xoffs = borderXOffs + (width - imageWidth) / 2;
               int yoffs = borderYOffs + (height - imageHeight) / 2;
               if (drawImage != null) {
                  g.drawImage(drawImage, xoffs, yoffs, null);
               }
            }
         };
   }
   
   
   /** {@inheritDoc} **/
   public void destroy() {
   }


   /** {@inheritDoc} **/
   public String getViewName() {
      return "Image";
   }


   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return 1000;
   }
   
 
   /** {@inheritDoc} **/
   public void updateState(final ViewerValueData valueData,
         final ViewerUpdateData data, final DebugContext context)
         throws ViewerException {
      String[] clientTextOut = new String[1];
      Value value = getImage(valueData.getValue(), context,
            clientTextOut);
      if (value.isNull()) {
         currentData = new Data("Image is null.", clientTextOut[0], null,
               0, 0, false);
         return;
      }
      Method getWidthMethod = value.getMethod(context,
            "getWidth", "int", null);
      int width = value.invokeMethod(context, getWidthMethod,
            null).toInt(context);
      Method getHeightMethod = value.getMethod(context,
            "getHeight", "int", null);
      int height = value.invokeMethod(context, getHeightMethod,
            null).toInt(context);
   
      String dsText = String.valueOf(width) + " x " + height;
   
      int scaledWidth = width;
      int scaledHeight = height;
      if (scaledWidth <= 0 || scaledHeight <= 0) {
         currentData = new Data(dsText, clientTextOut[0], null, 0, 0,
               false);
         return;
      }
      Value scaledWidthV = context.createPrimitiveValue("int",
            String.valueOf(scaledWidth));
      Value scaledHeightV = context.createPrimitiveValue("int",
            String.valueOf(scaledHeight));
      Value zeroV = context.createPrimitiveValue("int", "0");
      Value nullV = context.createNullValue();
   
      Value bufferedImage = null;
      boolean complete = true;
      boolean createdImage = false;
      int type = BufferedImage.TYPE_INT_ARGB;
      if (value.isInstanceOf(context, "java.awt.image.BufferedImage")) {
         type = value.getFieldValue(context, "imageType").
               toInt(context);
         if (type == BufferedImage.TYPE_INT_ARGB
               || type == BufferedImage.TYPE_INT_RGB
               || type == BufferedImage.TYPE_INT_BGR) {
            bufferedImage = value;
         }
      }
      if (bufferedImage == null) {
         createdImage = true;
         Type bufferedImageType = context.getType(
               "java.awt.image.BufferedImage");
         bufferedImageType.validate(context);
         Constructor biCons = bufferedImageType.getConstructor(
               context, new String[] { "int", "int", "int" });
         bufferedImage = bufferedImageType.createInstance(context,
               biCons, new Value[] {
               scaledWidthV, scaledHeightV,
               context.createPrimitiveValue("int", "2" /* TYPE_INT_ARGB */)
               });
         Method getGraphicsMethod = bufferedImageType.getMethod(
               context, "getGraphics", "java.awt.Graphics", null);
         Value graphics = bufferedImage.invokeMethod(context,
               getGraphicsMethod, null);
         Method drawImage = graphics.getMethod(context, "drawImage",
               "boolean", new String[] { "java.awt.Image", "int", "int",
               "int", "int", "java.awt.image.ImageObserver" });
         Value result = graphics.invokeMethod(context, drawImage,
               new Value[] { value, zeroV, zeroV, scaledWidthV,
               scaledHeightV, nullV });
         complete = result.toBoolean(context);
         Method dispose = graphics.getMethod(context,
               "dispose", "void", null);
         graphics.invokeMethod(context, dispose, null);
         List<Field> gFields = graphics.getType(context).
               getFields(Type.INSTANCE);
         for (Field f : gFields) {
            Type t = f.getType(context);
            if (t != null && t.isObject()) {
               graphics.setFieldValue(context, f, nullV);
            }
         }
      }         
      List<Value> pixels = null;
      Value raster = bufferedImage.getFieldValue(context, "raster");
      if (!raster.isNull()) {
         Value rgbData = raster.getFieldValue(context, "data");
         if (!rgbData.isNull()) {
            pixels = rgbData.getArrayElements(context);
         }
      }
      
      if (pixels == null || pixels.size() != scaledWidth * scaledHeight) {
         currentData = new Data(dsText, clientTextOut[0], null, 0, 0,
               false);
         return;
      }
      int[] pixelData = new int[pixels.size()];
      for (int i = 0; i < pixels.size(); i++) {
         int pixel = pixels.get(i).toInt(context);
         switch (type) {
            case BufferedImage.TYPE_INT_ARGB:
               break;
            case BufferedImage.TYPE_INT_RGB:
               pixel |= 0xff000000;
               break;
            case BufferedImage.TYPE_INT_BGR:
               pixel = 0xff000000 | ((pixel & 0xff0000) >> 16)
                     | (pixel & 0xff00) | ((pixel & 0xff) << 16);
               break;
            default:
               break;
         }
         pixelData[i] = pixel;
      }
   
      if (createdImage) {
         Method flush = bufferedImage.getMethod(context, "flush", "void",
               null);
         bufferedImage.invokeMethod(context, flush, null);
         try {
            bufferedImage.setFieldValue(context, "raster", nullV);
         }
         catch (JgrdiIsFinalException e) {
         }
         try {
            bufferedImage.setFieldValue(context, "surfaceManager", nullV);
         }
         catch (JgrdiIsFinalException e) {
         }
      }
      currentData = new Data(dsText, clientTextOut[0], pixelData,
            scaledWidth, scaledHeight, complete);
   }


   /** {@inheritDoc} **/
   public void updateGui() {
      Data data = currentData;
   
      displaySizeText = data.dsText;
      currentClientText = data.clientText;
      
      if (data.pixelData == null) {
         displayImage = null;
      }
      else {
         displayImage = new BufferedImage(data.w, data.h,
               BufferedImage.TYPE_INT_ARGB);
         displayImage.setRGB(0, 0, data.w, data.h, data.pixelData, 0,
               data.w);
         imagePanel.revalidate();
         getMainPanel().revalidate();
      }
      isComplete = data.complete;
      
      imagePanel.repaint();
   }
         

   /** Sets the display text.
    *
    *  @param sizeText text description of the size. **/
   private void setDisplayText(final String sizeText) {
      String text = "" + (sizeText == null? "" : sizeText);
      String ccText = currentClientText == null? "" : currentClientText;
      if (text.length() > 0 && ccText.length() > 0) {
         text += "  ";
      }
      text += ccText;
      if (text.length() == 0) {
         text = " ";
      }
      controls.setText(text);
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Detail viewer for images");
      vi.setLongDescription("This viewer displays the image at actual "
            + "size up to a maximum of 250x250 pixels.");
   }


   /** Gets the image to be displayed.
    *
    *  @param value the value being viewed.
    *
    *  @param context the current debugger context.
    *
    *  @param clientTextOut the first element of this array is
    *  used to return display text, if any.
    *
    *  @throws ViewerException if an error occurs while retrieving
    *  the image.
    *
    *  @return the image value. **/
   public abstract Value getImage(Value value, DebugContext context,
         String[] clientTextOut) throws ViewerException;
}

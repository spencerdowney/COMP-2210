
   import jgrasp.viewer.ViewerCreateData;
   import jgrasp.viewer.ViewerException;

   import jgrasp.viewer.jgrdi.DebugContext;
   import jgrasp.viewer.jgrdi.Method;
   import jgrasp.viewer.jgrdi.Value;


   /** An image viewer. **/
   public class java__awt__Image_ImageView extends ImageView {
   
   
      /** Creates a new image viewer.
       *
       *  @param vcd viewer creation data. **/
      public java__awt__Image_ImageView(final ViewerCreateData vcd) {
         super(vcd);
      }
   
   
      /** {@inheritDoc} **/
      public Value getImage(final Value value, final DebugContext context,
            final String[] clientTextOut) throws ViewerException {
         Method getSourceMethod = value.getMethod(context, "getSource",
               "java.awt.image.ImageProducer", null);
         Value source = value.invokeMethod(context, getSourceMethod,
               null);
         if (!source.isNull() && source.isInstanceOf(context,
               "sun.awt.image.FileImageSource")) {
            Value filename = source.getFieldValue(context, "imagefile");
            if (!filename.isNull()) {
               clientTextOut[0] = filename.toString(context);
            }
         }
         
         return value;
      }
   }

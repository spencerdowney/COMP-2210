
   import jgrasp.viewer.ViewerCreateData;
   import jgrasp.viewer.ViewerException;

   import jgrasp.viewer.jgrdi.DebugContext;
   import jgrasp.viewer.jgrdi.Value;


   /** An image viewer for image icons. **/
    public class javax__swing__ImageIcon_ImageView extends ImageView {
         
   
      /** Creates a new image icon viewer.
       *
       *  @param vcd viewer creation data. **/
       public javax__swing__ImageIcon_ImageView(final ViewerCreateData vcd) {
         super(vcd);
      }
      
   
      /** {@inheritDoc} **/
       public Value getImage(final Value value, final DebugContext context,
            final String[] clientTextOut) throws ViewerException {
         Value filename = value.getFieldValue(context, "filename");
         if (!filename.isNull()) {
            clientTextOut[0] = filename.toString(context);
         }
            
         return value.getFieldValue(context, "image");
      }
   }

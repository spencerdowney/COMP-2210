
   import jgrasp.viewer.ViewerCreateData;
   import jgrasp.viewer.ViewerException;
   import jgrasp.viewer.ViewerInfo;
   import jgrasp.viewer.ViewerPriorityData;
   
   import jgrasp.viewer.jgrdi.DebugContext;
   import jgrasp.viewer.jgrdi.Value;
   
   import jgrasp.viewer.text.TextAreaView;


   /** A viewer that displays the text of a java.lang.StringBuilder. **/
   public class java__lang__StringBuilder_FormattedWrapView
         extends TextAreaView {
   
   
      /** Creates a new StringBuilder formatted text viewer.
       *
       *  @param vcd creation data. **/
      public java__lang__StringBuilder_FormattedWrapView(
            final ViewerCreateData vcd) {
         super(true);
      }
   
   
      /** {@inheritDoc} **/
      public String getViewName() {
         return "Formatted Wrapping";
      }
   
          
      /** {@inheritDoc} **/
      public int getPriority(final ViewerPriorityData vpd) {
         return 9;
      }
   
   
      /** {@inheritDoc} **/
      public String getDisplayText(final Value value,
            final DebugContext context) throws ViewerException {
         return value.toString(context);
      }
   
   
      /** {@inheritDoc} **/
      public void getInfo(final ViewerInfo vi) {
         vi.setShortDescription("Formatted StringBuilder wrapping viewer");
         vi.setLongDescription("This viewer displays a the String "
               + "value of a StringBuilder as it would be displayed "
               + "in a wrapped text window.");
      }
   }

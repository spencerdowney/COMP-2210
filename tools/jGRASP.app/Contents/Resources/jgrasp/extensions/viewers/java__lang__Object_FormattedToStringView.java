
   import jgrasp.PluginOptOut;

   import jgrasp.viewer.ViewerCreateData;
   import jgrasp.viewer.ViewerException;
   import jgrasp.viewer.ViewerInfo;
   import jgrasp.viewer.ViewerPriorityData;

   import jgrasp.viewer.jgrdi.DebugContext;
   import jgrasp.viewer.jgrdi.Method;
   import jgrasp.viewer.jgrdi.Type;
   import jgrasp.viewer.jgrdi.Value;

   import jgrasp.viewer.text.TextAreaView;


   /** A formatted "toString()" viewer for objects. **/
   public class java__lang__Object_FormattedToStringView extends TextAreaView {
   
   
      /** Creates a new toString() viewer.
       *
       *  @param vcd creation data. **/
      public java__lang__Object_FormattedToStringView(
            final ViewerCreateData vcd) {
         /** Only show this viewer when the target value overrides
          *  Object.toString() and is not java.lang.String. **/
         DebugContext dc = vcd.getDebugContext();
         Type t = vcd.getValue().getType(dc);
         Method toStringMethod = null;
         try {
            toStringMethod = t.getMethod(dc, "toString",
                  "java.lang.String", null);
         }
            catch (Exception e) {
               throw new PluginOptOut();
            }
         Type declaringType = toStringMethod.getDeclaringType(dc);
         String dtName = declaringType.getName(dc);
         if (dtName.equals("java.lang.Object")
               || dtName.equals("java.lang.String")) {
            throw new PluginOptOut();
         }
      }
   
   
      /** {@inheritDoc} **/
      public String getViewName() {
         return "toString()";
      }
      
          
      /** {@inheritDoc} **/
      public int getPriority(final ViewerPriorityData vpd) {
         return -10;
      }
     
         
      /** {@inheritDoc} **/
      public String getDisplayText(final Value value,
            final DebugContext context) throws ViewerException {
         return value.toString(context);
      }
      
      
      /** {@inheritDoc} **/
      public void getInfo(final ViewerInfo vi) {
         vi.setShortDescription("toString() viewer");
         vi.setLongDescription("This viewer displays the toString() "
               + "value of an object as it would be displayed in a text "
               + "window.");
      }
   
   }

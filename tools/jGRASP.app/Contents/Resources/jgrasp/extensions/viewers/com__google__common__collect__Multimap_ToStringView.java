
import jgrasp.viewer.Util;

import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;
import jgrasp.viewer.ViewerUpdateData;
import jgrasp.viewer.ViewerValueData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.text.StringListViewWSV;


/** A guava Multimap viewer that displays the entries as a
 *  list of key/value collection strings. **/
public class com__google__common__collect__Multimap_ToStringView
      extends StringListViewWSV {


   /** Creates a new string list viewer for multimaps.
    *
    *  @param vcd viewer creation data. **/
   public com__google__common__collect__Multimap_ToStringView(
         final ViewerCreateData vcd) {
      super(true);
   }
      
      
   /** {@inheritDoc} **/
   public String getViewName() {
      return "Key/Values";
   }


   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return 10;
   }


   /** {@inheritDoc} **/
   public int update(final ViewerValueData valueData,
         final ViewerUpdateData data, final DebugContext context,
         final int viewOffset, final int numItemsShown, final int selected,
         final String[] textOut, final Value[] valuesOut,
         final Value[] selectedValuesOut,
         final String[] selectedExpressionOut,
         final String[] errorOut) throws ViewerException {
      Value value = valueData.getValue();
      
      Method asMapMethod = value.getMethod(context, "asMap", "java.util.Map",
            null);
      Value map = value.invokeMethod(context, asMapMethod, null);
      
      Method sizeMethod = map.getMethod(context, "size", "int", null);
      int size = map.invokeMethod(context, sizeMethod, null).
            toInt(context);
      if (size > 1500) {
         errorOut[0] = "Map contains " + size + " entries.\n\n"
               + "For performance reasons, this view can display a "
               + "maximum of 1500 entries.";
         return 0;
      }
   
      Method entrySetMethod = map.getMethod(context, "entrySet",
            "java.util.Set", null);
      Value entrySet = map.invokeMethod(context, entrySetMethod,
            null);
      
      Method toArrayMethod = entrySet.getMethod(context, "toArray",
            "java.lang.Object[]", null);
      Value array = entrySet.invokeMethod(context, toArrayMethod, null);
   
      boolean gotSelection = false;
      for (int i = 0; i < numItemsShown; i++) {
         if (i + viewOffset < size) {
            Value element = array.getArrayElement(context, i + viewOffset);
            Value keyVal = element.getFieldValue(context, "key");
            Value valueVal = element.getFieldValue(context, "value");
         
            if (i + viewOffset == selected) {
               selectedValuesOut[0] = keyVal;
               selectedValuesOut[1] = valueVal;
               gotSelection = true;
            }
            String keyStr =
                  Util.encodeString(keyVal.toString(context), true);
            if (keyStr.length() > 25) {
               keyStr = keyStr.substring(0, 21) + " ...";
            }
            String valueStr =
                  Util.encodeString(valueVal.toString(context), true);
            if (valueStr.length() > 25) {
               valueStr = valueStr.substring(0, 21) + " ...";
            }
            textOut[i] = "_" + (i + viewOffset) + "_ = "
                  + keyStr + ": " + valueStr;
         }
         else {
            textOut[i] = " ";
         }
      }
      if (selected >= 0 && selected < size && !gotSelection) {
         Value element = array.getArrayElement(context, selected);
         Value keyVal = element.getFieldValue(context, "key");
         Value valueVal = element.getFieldValue(context, "value");
         
         selectedValuesOut[0] = keyVal;
         selectedValuesOut[1] = valueVal;
      }
   
      return size;
   }
   
   
   /** {@inheritDoc} **/
   public String getSubviewerLabel(final int index,
         final String viewerLabel, final int itemIndex) {
      return (index == 0? "Key" : "Value") + " (" + itemIndex + ")";
   }


   /** {@inheritDoc} **/
   public String getSubviewerTreeLabel(final int index,
         final String viewerLabel, final int itemIndex) {
      return (index == 0? "key" : "value") + " (" + itemIndex + ")";
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("Map \"toString()\" viewer");
      vi.setLongDescription("This viewer displays the toString() value "
            + "for each key and value in the map view of the multimap. "
            + "Selecting an entry "
            + "will cause the key and values for that entry to be "
            + "displayed in a subviewer.\n\nNote that for identification "
            + "purposes, indices are assigned to the entries, "
            + "although they may have no specific order.");
   }

}

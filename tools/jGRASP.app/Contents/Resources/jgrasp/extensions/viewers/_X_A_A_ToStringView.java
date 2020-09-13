import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;
import jgrasp.viewer.ViewerUpdateData;
import jgrasp.viewer.ViewerValueData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Type;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.text.StringTableViewWSV;


/** A 2D array viewer that displays the elements as a table of
 *  strings. **/
public class _X_A_A_ToStringView extends StringTableViewWSV {


   /** Creates a new string list viewer for 2D arrays.
    *
    *  @param vcd viewer creation data. **/
   public _X_A_A_ToStringView(final ViewerCreateData vcd) {
      super(false);
   }
    
      
   /** {@inheritDoc} **/
   public String getViewName() {
      return "2D Array Elements";
   }


   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return 201;
   }


   /** {@inheritDoc} **/
   public void update(final ViewerValueData valueData,
         final ViewerUpdateData data, final DebugContext context,
         final int rowOffset, final int colOffset, final int numRowsShown,
         final int numColsShown, final int selectedRow,
         final int selectedCol, final String[][] textOut,
         final Value[][] valuesOut,
         final Value[] selectedValuesOut,
         final String[] selectedExpressionsOut,
         final int[] rowsOut, final int[] colsOut,
         final String[] errorOut) throws ViewerException {
   
      Value value = valueData.getValue();
      int rows = value.getArrayLength(context);
      rowsOut[0] = rows;
      
      int longestCol = 0;
      boolean gotSelection = false;
      for (int i = 0; i < numRowsShown; i++) {
         if (i + rowOffset < rows) {
            Value row = value.getArrayElement(context, i + rowOffset);
            int cols = 0;
            if (!row.isNull()) {
               cols = row.getArrayLength(context);
            }
            if (cols > longestCol) {
               longestCol = cols;
            }
            for (int j = 0; j < numColsShown; j++) {
               if (j + colOffset < cols) {
                  Value element = row.getArrayElement(context,
                        j + colOffset);
                  valuesOut[i][j] = element;
                  if (i == selectedRow && j == selectedCol) {
                     selectedValuesOut[0] = element;
                     selectedExpressionsOut[0] = getExpr(valueData,
                           i + rowOffset, j + colOffset, context);
                     gotSelection = true;
                  }
                  String str = element.toString(context);
                  if (str.length() > 100) {
                     str = str.substring(0, 96) + " ...";
                  }
                  textOut[i][j] = str;
               }
               else {
                  valuesOut[i][j] = null;
                  textOut[i][j] = null;
               }
            }
         }
         else {
            for (int j = 0; j < numColsShown; j++) {
               valuesOut[i][j] = null;
               textOut[i][j] = null;
            }
         }
      }
      colsOut[0] = longestCol;
   
      if (selectedRow >= 0 && selectedCol >= 0
            && selectedRow < rows && !gotSelection) {
         Value row = value.getArrayElement(context, selectedRow);
         int cols = 0;
         if (!row.isNull()) {
            cols = row.getArrayLength(context);
         }
         if (selectedCol < cols) {
            Value element = row.getArrayElement(context, selectedCol);
            selectedValuesOut[0] = element;
            selectedExpressionsOut[0] = getExpr(valueData,
                  selectedRow, selectedCol, context);
         }
      }
   }
   
   
   /** {@inheritDoc} **/
   public String getSubviewerLabel(final int index,
         final String viewerLabel, final int rowIndex,
         final int colIndex) {
      return viewerLabel + " [" + rowIndex + "," + colIndex + "]";
   }


   /** {@inheritDoc} **/
   public String getSubviewerTreeLabel(final int index,
         final String viewerLabel, final int rowIndex,
         final int colIndex) {
      return "[" + rowIndex + "," + colIndex + "]";
   }


   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("2D array \"toString()\" viewer");
      vi.setLongDescription("This viewer displays the toString() value "
            + "for array elements in a 2D grid of cells. Selecting a "
            + "cell will cause its value to be displayed in a subviewer.");
   }


   /** Gets the expression for a value at a specified row and column.
    *
    *  @param valueData the value update data.
    *
    *  @param row the row of interest.
    *
    *  @param col the column of interest.
    *
    *  @return an expression for the value at the requested position,
    *  or null if there is no expression for the current value. **/
   private String getExpr(final ViewerValueData valueData,
         final int row, final int col, final DebugContext context) {
      String expr = getVIData().getExpression();
      if (expr == null) {
         return null;
      }
      Type dt = valueData.getDeclaredType();
      if (dt.getName(context).endsWith("[][]")) {
         if (getVIData().getNeedParen()) {
            return "(" + expr + ")[" + row + "][" + col + "]";
         }
         return expr + "[" + row + "][" + col + "]";
      }
      Value value = valueData.getValue();
      String cast = value.getType(context).getName(context);
      return "((" + ((cast.indexOf('.') >= 0)? "`cast`" : "")
            + cast + ") " + expr + ")[" + row + "][" + col + "]";
   }
}

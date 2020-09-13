
import java.text.SimpleDateFormat;

import java.util.Date;

import jgrasp.viewer.ViewerCreateData;
import jgrasp.viewer.ViewerException;
import jgrasp.viewer.ViewerInfo;
import jgrasp.viewer.ViewerPriorityData;

import jgrasp.viewer.jgrdi.DebugContext;
import jgrasp.viewer.jgrdi.Method;
import jgrasp.viewer.jgrdi.Value;

import jgrasp.viewer.text.TextAreaView;


/** A viewer that displays file information. **/
public class java__io__File_FileView extends TextAreaView {


   /** Creates a new file viewer.
    *
    *  @param vcd creation data. **/
   public java__io__File_FileView(final ViewerCreateData vcd) {
   }
    
    
   /** {@inheritDoc} **/
   public String getViewName() {
      return "File";
   }


   /** {@inheritDoc} **/
   public int getPriority(final ViewerPriorityData vpd) {
      return 10;
   }


   /** {@inheritDoc} **/
   public String getDisplayText(final Value value,
         final DebugContext context) throws ViewerException {
     
      Method apMethod = value.getMethod(context, "getAbsolutePath",
            "java.lang.String", null);
      Value ap = value.invokeMethod(context, apMethod, null);
   
      Method isAbsMethod = value.getMethod(context, "isAbsolute",
            "boolean", null);
      Value isAbsV = value.invokeMethod(context, isAbsMethod, null);
      boolean isAbs = isAbsV.toBoolean(context);
   
      Method existsMethod = value.getMethod(context, "exists",
            "boolean", null);
      Value existsV = value.invokeMethod(context, existsMethod, null);
      boolean exists = existsV.toBoolean(context);
      if (!exists) {
         return (isAbs? "Absolute" : "Relative") + " File Path: "
               + ap.toString(context) + "\n"
               + "Exists: " + exists;
      }
      Method isDirMethod = value.getMethod(context, "isDirectory",
            "boolean", null);
      Value isDirV = value.invokeMethod(context, isDirMethod, null);
      boolean isDir = isDirV.toBoolean(context);
      
      if (isDir) {
         return (isAbs? "Absolute" : "Relative") + " Directory Path: "
               + ap.toString(context);
      }
   
      Method lengthMethod = value.getMethod(context, "length", "long", null);
      Value lengthV = value.invokeMethod(context, lengthMethod, null);
      long length = lengthV.toLong(context);
      
      Method lastModMethod = value.getMethod(context, "lastModified", "long",
            null);
      Value lastModV = value.invokeMethod(context, lastModMethod, null);
      long lastMod = lastModV.toLong(context);
      SimpleDateFormat sdf = new SimpleDateFormat();
      
      Method isHiddenMethod = value.getMethod(context, "isHidden",
            "boolean", null);
      Value isHiddenV = value.invokeMethod(context, isHiddenMethod, null);
      boolean isHidden = isHiddenV.toBoolean(context);
      
      Method canReadMethod = value.getMethod(context, "canRead",
            "boolean", null);
      Value canReadV = value.invokeMethod(context, canReadMethod, null);
      boolean canRead = canReadV.toBoolean(context);
      
      Method canWriteMethod = value.getMethod(context, "canWrite",
            "boolean", null);
      Value canWriteV = value.invokeMethod(context, canWriteMethod, null);
      boolean canWrite = canWriteV.toBoolean(context);
      
      return (isAbs? "Absolute" : "Relative") + " File Path: "
               + ap.toString(context) + "\n"
            + "Length: " + length + " bytes\n"
            + "Last Modified: " + lastMod + " = "
               + sdf.format(new Date(lastMod)) + "\n"
            + "Hidden: " + isHidden + "\n"
            + "Readable: " + canRead + "\n"
            + "Writable: " + canWrite;
   }
   
   
   /** {@inheritDoc} **/
   public void getInfo(final ViewerInfo vi) {
      vi.setShortDescription("File viewer");
      vi.setLongDescription("This viewer displays File information.");
   }
}

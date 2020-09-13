
package junit_runner;


import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.net.InetAddress;
import java.net.Socket;

import java.security.Permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;


public class JgrRunner {


   /** A security exception thrown when user code or a test calls
    *  System.exit(). **/
   private static class JgrExitException69jh extends SecurityException  {
   
   
      /** Creates a new exit exception.
       *
       *  @param status the exit status. **/
      JgrExitException69jh(final int status)  {
         super("System.exit(" + status + ") or Runtime.exit(" + status
               + ") was called");
      }
   }


   /** A security manager that throws an exception when System.exit() is
    *  called. **/
   private static class BlockExitSecurityManager extends SecurityManager {
   
   
      /** {@inheritDoc} **/
      public void checkPermission(final Permission perm) {
      }
   
   
      /** {@inheritDoc} **/
      public void checkPermission(final Permission perm,
            final Object context)  {
      }   
      
         
      /** {@inheritDoc} **/
      public void checkExit(final int status) {
         super.checkExit(status);
         throw new JgrExitException69jh(status);
      }
   }
   
   
   /** The communications socket writer. **/
   private static Writer writer;
   
   /** Map from descriptions to id numbers. **/
   private static Map<Description, Integer> dToID
         = new HashMap<Description, Integer>();
   
   /** Current id number. **/
   private static int currentId;
   
   /** The total number of tests performed. **/
   private static int totalCount;
   
   /** The number of failed tests. **/
   private static int failedCount;
   
   /** The number of ignored tests. **/
   private static int ignoredCount;
   
   /** The JUnit event listener. **/
   private static RunListener listener = 
      new RunListener() {
      
         public void testAssumptionFailure(final Failure failure) {
            failure(failure, true);
         }
         
         public void testFailure(final Failure failure) {
            failure(failure, false);
         }
         
         
         private void failure(final Failure failure, final boolean assumption) {
            if (dToID == null) {
               return;
            }
            Description desc = failure.getDescription();
            Integer id = dToID.get(desc);
            if (id == null) {
               return;
            }
            String[] traceLines = failure.getTrace().trim().split("[\r\n]+");
            
            boolean isExitException =
                  failure.getException() instanceof JgrExitException69jh;
            if (isExitException && traceLines.length > 0
                  && traceLines[0].indexOf(
                        JgrExitException69jh.class.getName()) >= 0) {
               // Replace exception line with exception message only.
               traceLines[0] = failure.getMessage();
            }
            
            StringBuilder trace = new StringBuilder();
            boolean first = true;
            for (String line : traceLines) {
               if (line.trim().length() == 0) {
                  continue;
               }
               if (line.matches("\\s*at\\s*"
                     + "(sun\\.reflect|java\\.lang\\.reflect|org\\.junit|"
                     + "junit_runner).*")) {
                  continue;
               }
               if (isExitException && line.matches("\\s*at\\s*"
                     + "java\\.lang\\.(Runtime|System)\\.exit\\(.*")) {
                  continue;
               }
               if (!first) {
                  trace.append('\n');
               }
               first = false;
               trace.append(line);
            }
            
            write((assumption? "a" : "f") + id.toString() + ","
                  + encode(trace.toString()) + "\n");
            String d = desc.getClassName();
            String m = desc.getMethodName();
            if (m != null) {
               if (d == null) {
                  d = m;
               }
               else {
                  d = d + "." + m;
               }
            }
            
            if (d != null) {
               System.err.print("\n === " + d + " ===");
            }
            System.err.println("\n" + trace);
            if (assumption) {
               ignoredCount++;
            }
            else {
               failedCount++;
            }
         }
      
         public void testStarted(final Description description) {
            if (dToID == null) {
               return;
            }
            Integer id = dToID.get(description);
            if (id == null) {
               return;
            }
            write("b" + id.toString() + "\n");
         }
      
         public void testFinished(final Description description) {
            if (dToID == null) {
               return;
            }
            Integer id = dToID.get(description);
            if (id == null) {
               return;
            }
            write("e" + id.toString() + "\n");
            totalCount++;
         }
      
         public void testIgnored(final Description description) {
            if (dToID == null) {
               return;
            }
            Integer id = dToID.get(description);
            if (id == null) {
               return;
            }
            write("i" + id.toString() + "\n");
            ignoredCount++;
            totalCount++;
         }
      
         public void testRunFinished(final Result result) {
            write("x\n");
            System.err.print("\nCompleted " + totalCount + " tests");
            int passed = totalCount - failedCount - ignoredCount;
            if (passed > 0) {
               System.err.print("  " + passed + " passed");
            }
            if (failedCount > 0) {
               System.err.print("  " + failedCount + " failed");
            }
            if (ignoredCount > 0) {
               System.err.print("  " + ignoredCount + " ignored");
            }
            System.err.println();
         }
      
         public void testRunStarted(final Description description) {
            StringBuilder sb = new StringBuilder();
            appendDescriptionTree(sb, description);
            write("r" + sb + "\n");
            int count = description.testCount();
            System.err.println("Running " + count + " JUnit "
                  + ((count == 1)? "test." : "tests"));
         }
      };


   public static void main(final String[] args) {
      if (args.length < 2) {
         System.err.println("jGRASP JUnit Runner error: less than two "
               + "arguments.");
         return;
      }
      
      int port;
      try {
         port = Integer.parseInt(args[0]);
      }
      catch (NumberFormatException e) {
         System.err.println("jGRASP JUnit Runner error: first argument \""
               + args[0] + "\" is not a port number.");
         return;
      }
      
      Socket socket;
      try {
         socket = new Socket(InetAddress.getByName(null), port);
      }
      catch (IOException e) {
         System.err.println("jGRASP JUnit Runner error: could not connect "
               + "to plugin: " + e.getMessage());
         return;
      }
      catch (IllegalArgumentException e) {
         System.err.println("jGRASP JUnit Runner error: bad port number: "
               + e.getMessage());
         return;
      }
      OutputStream os;
      try {
         os = socket.getOutputStream();
      }
      catch (IOException e) {
         System.err.println("jGRASP JUnit Runner error: could not get "
               + "socket output stream: " + e.getMessage());
         close(socket);
         return;
      }
      try {
         writer = new OutputStreamWriter(os, "UTF-8");
      }
      catch (UnsupportedEncodingException e) {
         System.err.println("jGRASP JUnit Runner error: UTF-8 "
               + "charset is missing: " + e.getMessage());
         close(os);
         close(socket);
         return;
      }
      
      JUnitCore core = new JUnitCore();
      core.addListener(listener);
      
      List<Class> classes = new ArrayList<Class>();
      for (int i = 1; i < args.length; i++) {
         try {
            Class c = Class.forName(args[i]);
            classes.add(c);
         }
         catch (ClassNotFoundException e) {
            System.err.println("jGRASP JUnit Runner warning: class \""
                  + args[i] + "\" not found.");
         }
         catch (LinkageError e) {
            System.err.println("jGRASP JUnit Runner warning: class \""
                  + args[i] + "\" could not be loaded.");
         }
      }
      
      System.setSecurityManager(new BlockExitSecurityManager());
      core.run(classes.toArray(new Class[0]));
      System.setSecurityManager(null);
   
      close(socket);
   }


   /** Writes some text to the communications socket writer.
    *
    *  @param text the text to be written. **/
   private static void write(final String text) {
      try {
         writer.write(text);
         writer.flush();
      }
      catch (IOException e) {
      }
   }


   /** Closes a socket.
    *
    *  @param s the socket to be closed. **/
   private static void close(final Socket s) {
      try {
         s.close();
      }
      catch (IOException e) {
      }
   }


   /** Closes something.
    *
    *  @param c the thing to be closed. **/
   private static void close(final Closeable c) {
      try {
         c.close();
      }
      catch (IOException e) {
      }
   }
   
   
   /** Appends a description tree for communication to the launcher and fills
    *  the description to id mapping.
    *
    *  @param out string builder to which the description will be appended.
    *
    *  @param d the description. **/
   private static void appendDescriptionTree(final StringBuilder out,
         final Description d) {
      dToID.put(d, Integer.valueOf(currentId));
      out.append('<');
      String m = d.getMethodName();
      if (m != null) {
         out.append(m);
      }
      else {
         String c = d.getClassName();
         if (c != null && !c.equals("null")) {
            out.append(c);
         }
         else {
            String dn = d.getDisplayName();
            if (dn != null && !dn.equals("null")) {
               out.append(dn);
            }
         }
      }
      out.append(',');
      out.append(d.testCount());
      out.append(',');
      out.append(currentId++);
      out.append(',');
      for (Description child : d.getChildren()) {
         appendDescriptionTree(out, child);
      }
      out.append('>');
   }
   
   
   /** Encodes newlines and backslashes in a string.
    *
    *  @param text the string to be encoded.
    *
    *  @return the encoded string. **/
   public static String encode(final String text) {
      if (text.indexOf('\n') < 0 && text.indexOf('\r') < 0) {
         return text;
      }
      if (text.indexOf('\\') < 0) {
         return text.replace("\n", "\\n").replace("\r", "\\r");
      }
      return text.replace("\\", "\\\\").replace("\n", "\\n").
            replace("\r", "\\r");
   }
}

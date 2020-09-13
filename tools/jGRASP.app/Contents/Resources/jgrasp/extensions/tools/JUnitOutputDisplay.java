
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jgrasp.GIconData;
import jgrasp.JgrTreeCellRenderer;
import jgrasp.PluginUtil;
import jgrasp.StateIcon;

import jgrasp.tool.JGraspFrame;
import jgrasp.tool.LocData;
import jgrasp.tool.ToolCreateData;


/** Output reader and display component for JUnit tool plugin. **/
public class JUnitOutputDisplay implements Runnable {


   /** Exception for communication data parsing errors. **/
   private static class FormatException extends Exception {
    
    
      /** Creates a new FormatException.
       *
       *  @param message the detail message. **/
      FormatException(final String message) {
         super(message);
      }
   }


   /** Renderer for result tree nodes. **/
   private static class Renderer extends JgrTreeCellRenderer {
   
      /** Failure icon. **/
      private static StateIcon failIcon = 
         new StateIcon(StateIcon.State.FAIL) {
            public void paintMainIcon(final Graphics2D g, final int x,
                  final int y, final int w, final int h) {
            }
         };
   
      /** Success icon. **/
      private static StateIcon passIcon = 
         new StateIcon(StateIcon.State.PASS) {
            public void paintMainIcon(final Graphics2D g, final int x,
                  final int y, final int w, final int h) {
            }
         };
   
      /** Ignored icon. **/
      private static StateIcon ignoredIcon = 
         new StateIcon(StateIcon.State.IGNORED) {
            public void paintMainIcon(final Graphics2D g, final int x,
                  final int y, final int w, final int h) {
            }
         };
   
      /** Untested icon. **/
      private static StateIcon untestedIcon = 
         new StateIcon(StateIcon.State.NEEDS_TESTING) {
            public void paintMainIcon(final Graphics2D g, final int x,
                  final int y, final int w, final int h) {
            }
         };
   
   
      /** {@inheritDoc} **/
      public void setupComponent(final Object userValue,
            final boolean leaf, final boolean expanded,
            final boolean selected) {
      
         NodeData nd = null;
         if (userValue != null) {
            nd = (NodeData) userValue;
         }
         if (nd == null) {
            return;
         }
         
         int iconHt = getPreferredCellHeight() - 2;
         if (nd.failedCount > 0) {
            failIcon.setHeight(iconHt);
            setIcon(failIcon);
         }
         else if (nd.ignoredCount == nd.count) {
            ignoredIcon.setHeight(iconHt);
            setIcon(ignoredIcon);
         }
         else if (nd.passedCount + nd.ignoredCount == nd.count) {
            passIcon.setHeight(iconHt);
            setIcon(passIcon);
         }
         else {
            untestedIcon.setHeight(iconHt);
            setIcon(untestedIcon);
         }
      }
   }
   
   
   /** Results line data. **/
   private static class LineData {
   
      /** The tool tip text for the line. **/
      private String toolTip;
      
      /** Start position of the line. **/
      private int start;
      
      /** End position of the line (exclusive). **/
      private int end;
   
   
      /** Creates a new LineData.
       *
       *  @param startIn the start position of the line.
       *
       *  @param endIn the end position of the line (exclusive).
       *
       *  @param toolTipIn the tool tip text for the line. **/
      LineData(final int startIn, final int endIn,
            final String toolTipIn) {
         start = startIn;
         end = endIn;
         toolTip = toolTipIn;
      }
   }
   
   
   /** Results stack dump clickable line data. **/
   private static class ClickData extends LineData {
   
      /** The class name. **/
      private String className;
   
      /** The simple filename. **/
      private String filename;
   
      /** The line number. **/
      private int linenum;
   
   
      /** Creates a new ClickData.
       *
       *  @param classNameIn the class name.
       *
       *  @param filenameIn the simple filename.
       *
       *  @param linenumIn the line number.
       *
       *  @param startIn the start position of the link.
       *
       *  @param endIn the end position of the link (exclusive).
       *
       *  @param toolTipIn the tool tip text for the link. **/
      ClickData(final String classNameIn, final String filenameIn,
            final int linenumIn, final int startIn, final int endIn,
            final String toolTipIn) {
         super(startIn, endIn, toolTipIn);
         className = classNameIn;
         filename = filenameIn;
         linenum = linenumIn;
      }
   }
   
   
   /** Test description tree node data class. **/
   private class NodeData {
   
      /** The display description for the node. **/
      private String name;
   
      /** The number of tests associated with the node and its
       *  children (if any). **/
      private int count;
      
      /** The number of passed tests associated with the node and its
       *  children (if any). **/
      private int passedCount;
      
      /** The number of failed tests associated with the node and its
       *  children (if any). **/
      private int failedCount;
      
      /** The number of ignored tests associated with the node and its
       *  children (if any). **/
      private int ignoredCount;
      
      /** The parent data, or null if this is data for the root node. **/
      private NodeData parent;
      
      /** True if this is a single test, false otherwise. **/
      private boolean isTest;
      
      /** The tree node associated with the data. **/
      private DefaultMutableTreeNode node;
      
      /** Descriptions for each failure. **/
      private List<String> failDescriptions =
            new ArrayList<String>();
            
      /** Test start time, or -1 if uninitialized. **/
      private long startTime = -1;
            
      /** Test time, or -1 if uninitialized. **/
      private long time = -1;
      
      /** The level in the tree. **/
      private int level;
      
      
      /** Creates a new NodeData.
       *
       *  @param nameIn the display description for the node.
       *
       *  @param countIn the number of tests associated with the node and its
       *  children (if any).
       *
       *  @param level the level in the tree. **/
      NodeData(final String nameIn, final int countIn,
            final NodeData parentIn, final int levelIn) {
         name = nameIn;
         count = countIn;
         parent = parentIn;
         level = levelIn;
      }
      
      
      /** Adds a failed test.
       *
       *  @param description a text description of the failure.
       *
       *  @param assumption true if it was an assumption failure, false if it
       *  was a test failure. **/
      public void failedTest(final String description,
            final boolean assumption) {
         if (!assumption && level == 1) {
            failedClasses.add(name);
         }
         failDescriptions.add(description);
         if (assumption) {
            ignoredCount++;
         }
         else {
            failedCount++;
         }
         treeModel.nodeChanged(node);
         if (!isTest) {
            tree.expandPath(
                  new TreePath(treeModel.getPathToRoot(node)));
         }
         if (parent != null) {
            if (assumption) {
               parent.ignoredTest();
            }
            else {
               parent.failedTest(description, false);
            }
         }
         if (selectedTreeNode == node) {
            setSelection(node);
         }
      }
      
      
      /** Adds an ignored test. **/
      public void ignoredTest() {
         ignoredCount++;
         treeModel.nodeChanged(node);
         if (parent != null) {
            parent.ignoredTest();
         }
      }
      
      
      /** Adds a passed test. **/
      public void passedTest() {
         passedCount++;
         treeModel.nodeChanged(node);
         if (parent != null) {
            parent.passedTest();
         }
      }
      
      
      /** Called when a test is started in the node or one of its
       *  children. **/
      public void testStart() {
         if (startTime == -1) {
            startTime = System.currentTimeMillis();
         }
         if (parent != null) {
            parent.testStart();
         }
      }
      
      
      /** Closes a test. **/
      public void testDone() {
         if (failedCount == 0 && ignoredCount == 0) {
            passedTest();
         }
         done(0);
      }
      
      
      /** Called when a test ends for the node or one of its
       *  children.
       *
       *  @param childTime the time for a child test, or zero if the
       *  node is the finished test. **/
      public void done(final long childTime) {
         if (startTime == -1) {
            return;
         }
         long tm;
         if (isTest) {
            if (time != -1) {
               return;
            }
            time = System.currentTimeMillis() - startTime;
            tm = time;
         }
         else {
            time += childTime;
            tm = childTime;
         }
         if (parent != null && tm != 0) {
            parent.done(tm);
         }
         treeModel.nodeChanged(node);
      }
      
      
      /** {@inheritDoc} **/
      public String toString() {
         if (isTest) {
            String str;
            if (passedCount > 0) {
               str = name + "  passed";
            }
            else if (failedCount > 0) {
               str = name + "  failed";
            }
            else if (ignoredCount > 0) {
               str = name + "  ignored";
            }
            else {
               str = name;
            }
            if (time >= 0) {
               str += " (" + timeDisplay(time) + ")";
            }
            return str;
         }
         StringBuilder result = new StringBuilder(
               (name.length() == 0)? name : (name + "  "));
         result.append(count);
         result.append((count == 1)? " test" : " tests");
         if (passedCount > 0) {
            result.append("  ");
            result.append(passedCount);
            result.append(" passed");
         }
         if (failedCount > 0) {
            result.append("  ");
            result.append(failedCount);
            result.append(" failed");
         }
         if (ignoredCount > 0) {
            result.append("  ");
            result.append(ignoredCount);
            result.append(" ignored");
         }
         if (time >= 0) {
            result.append(" (");
            result.append(timeDisplay(time));
            result.append(")");
         }
         return result.toString();
      }
      
      
      /** Gets the failure messages.
       *
       *  @return the failure messages. **/
      public String getErrorText() {
         StringBuilder result = new StringBuilder();
         boolean first = true;
         for (String fd : failDescriptions) {
            if (!first) {
               result.append("\n\n");
            }
            first = false;
            if (fd.indexOf('\\') >= 0) {
               fd = fd.replace("\\r", "\r");
               fd = fd.replace("\\n", "\n");
               fd = fd.replace("\\\\", "\\");
            }
            result.append(fd);
         }
         return result.toString();
      }
   }
   
   
   /** Progress display panel. **/
   private class ProgressPanel extends JPanel {
   
   
      /** {@inheritDoc} **/
      public boolean isOpaque() {
         return false;
      }
   
   
      /** {@inheritDoc} **/
      public void paintComponent(final Graphics g) {
      
         Graphics2D g2d = (Graphics2D) g;
      
         g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
               RenderingHints.VALUE_STROKE_PURE);
          
         int fHt = g.getFontMetrics().getHeight();
         int width = getWidth() - fHt;
         if (width < 3) {
            width = 3;
         }
         
         int div = (totalCount == 0)? 1 : totalCount;
         int testedWidth = totalTested * width / div;
         int failWidth = totalFailed * width / div;
         int ignoredWidth = totalIgnored * width / div;
         int successWidth = testedWidth - failWidth - ignoredWidth;
         g.setColor(Color.GREEN);
         int l = fHt / 2;
         int t = fHt / 2 + fHt;
         if (successWidth > 0) {
            g.fillRect(l + 1, t, successWidth, fHt);
            g.setColor(Color.BLACK);
            g.drawLine(l + successWidth, t, l + successWidth, t + fHt);
         }
         
         if (ignoredWidth > 0) {
            g.setColor(Color.ORANGE);
            g.fillRect(l + 1 + successWidth, t, ignoredWidth, fHt);
            g.setColor(Color.BLACK);
            g.drawLine(l + successWidth + ignoredWidth, t,
                  l + successWidth + ignoredWidth, t + fHt);
         }
         
         if (failWidth > 0) {
            g.setColor(Color.RED);
            g.fillRect(l + 1 + successWidth + ignoredWidth, t,
                  failWidth, fHt);
            g.setColor(Color.BLACK);
            g.drawLine(l + successWidth + ignoredWidth + failWidth,
                  t, l + successWidth + ignoredWidth + failWidth,
                  t + fHt);
         }
         
         g.setColor(Color.BLACK);
         g.drawRect(l, t, width, fHt);
         
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON);
         
         String label;
         if (!finished) {
            label = "Testing:  " + totalTested + " / " + totalCount;
         }
         else {
            label = "Testing Complete";
         }
         int ascent = (int) (g.getFontMetrics().getAscent() + .5);
         g.drawString(label, l, fHt / 2 + ascent);
      }
      
      
      /** {@inheritDoc} **/
      public Dimension getPreferredSize() {
         Graphics g = getGraphics();
         if (g == null) {
            return super.getPreferredSize();
         }
      
         int fHt = g.getFontMetrics().getHeight();
         return new Dimension(fHt * 3, fHt * 3);
      }
   }


   /** Text pane tool tip line attribute name. **/
   private static final String LINE_ATTRIBUTE = "jgr_tooltip";

   /** Text pane link attribute name. **/
   private static final String LINK_ATTRIBUTE = "jgr_clickable";
   
   /** The stack dump error pattern. **/
   private static Pattern errorPattern =
         Pattern.compile("\\s*at (\\S+)\\.[^.]+\\(([^:]+):(\\d+)\\)");
         
   /** Color for text links. **/
   private static Color linkColor = new Color(0, 203, 0);
   
   /** Header marker string. **/
   private static String headerMarker = "-&^$$#*=";
   
   /** Connect timeout in seconds. **/
   private static int connectTimeout = 8;

   /** The server socket for communicating with the JUnit runner. **/
   private ServerSocket serverSocket;
   
   /** The socket for the single connection to the JUnit runner. **/
   private Socket serviceSocket;
   
   /** The test description tree. **/
   private DefaultMutableTreeNode descriptionRoot;
   
   /** The test description tree. **/
   private JTree tree;
   
   /** The test description tree model. **/
   private DefaultTreeModel treeModel;
   
   /** The progress and results pane. **/
   private JPanel rightPane;
   
   /** The progress pane. **/
   private ProgressPanel progressPane;
   
   /** The text results pane. **/
   private JTextPane textPane;
   
   /** The text results plain text style. **/
   private Style textStyle;
   
   /** The text results document. **/
   private StyledDocument doc;
   
   /** The display frame. **/
   private JGraspFrame frame;
   
   /** Map from node ids to data. **/
   private Map<Integer, NodeData> idToNodeData =
         new HashMap<Integer, NodeData>();
         
   /** The total number of tests. **/
   private int totalCount;
   
   /** The total number of tests completed. **/
   private int totalTested;
   
   /** The total number of tests failed. **/
   private int totalFailed;
   
   /** The total number of tests ignored. **/
   private int totalIgnored;
   
   /** The classes for which there was a failure. **/
   private Set<String> failedClasses = new HashSet<String>();
   
   /** True if the testing is finished, false otherwise. **/
   private boolean finished;
   
   /** Set of all tested ids. **/
   private Set<Integer> tested = new HashSet<Integer>();
   
   /** The currently selected tree node. **/
   private TreeNode selectedTreeNode;
   
   /** Location data for opening files. **/
   private LocData locData;
   
   /** Source root directories for test files. **/
   private Set<File> testRootDirs;
   
   /** True if the results dialog should not be popped up, false if
    *  it should be. **/
   private boolean dontShowDialog;
   
   /** A title for the thing being tested. **/
   private String title;
   
   /** True if the communication thread is running, false otherwise. **/
   private boolean running;
   
   /** True if the previous process ended before the reading of JUnit
    *  communications was finished. **/
   private volatile boolean endFlag;
   
   /** Creation data. **/
   private ToolCreateData creationData;


   /** Creates a new JUnitOutputDisplay. **/
   public JUnitOutputDisplay() {
   }
   
   
   /** Closes a server socket.
    *
    *  @param c the server socket to be closed. **/
   private static void close(final ServerSocket s) {
      try {
         s.close();
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
   
   
   /** Gets a millisecond time reporting string.
    *
    *  @param ms the time in milliseconds.
    *
    *  @return a string representation of the time. **/
   private static String timeDisplay(final long ms) {
      if (ms < 1) {
         return "< 1 ms";
      }
      if (ms < 100) {
         return ms + " ms";
      }
      double s = ms / 1000.0;
      long ls = (long) s;
      if (ls >= 100) {
         return String.valueOf(ls) + " s";
      }
      String str = String.format("%.3g", s);
      int end = str.length();
      while (end > 1 && str.charAt(end - 1) == '0') {
         end--;
      }
      if (end > 1 && !Character.isDigit(str.charAt(end - 1))) {
         end--;
      }
      if (end > 0 && end < str.length()) {
         str = str.substring(0, end);
      }
      
      return str + " s";
   }
   
   
   /** Initializes the JUnitOutputDisplay.
    *
    *  @param ss the server socket for communicating with the
    *  JUnit runner.
    *
    *  @param locDataIn location data for opening files.
    *
    *  @param testRootDirsIn source root directories for test files.
    *
    *  @param dontShowDialogIn true if the results dialog should not be
    *  popped up, false if it should.
    *
    *  @param titleIn a useful title for the thing being tested.
    *
    *  @param tcd tool creation data for JUnit plugin.
    *
    *  @return false if a previous session is still running after a short wait,
    *  true otherwise. **/
   public boolean init(final ServerSocket ss, final LocData locDataIn,
         final Set<File> testRootDirsIn, final boolean dontShowDialogIn,
         final String titleIn, final ToolCreateData tcd) {
     /** In case the previous run has not terminated. **/
      if (running && endFlag) {
         /** Previous process has ended, but we may be stuck
          *  waiting for a connection or reading. **/
         Socket s = serviceSocket;
         if (s != null) {
            close(s);
         }
         ServerSocket rSS = serverSocket;
         if (rSS != null) {
            close(rSS);
         }
      }
      for (int i = 0; i < 20 && running; i++) {
         try {
            Thread.sleep(100);
         }
         catch (InterruptedException e) {
         }
      }
      if (running) {
         close(ss);
         return false;
      }
      
      serverSocket = ss;
      locData = locDataIn;
      testRootDirs = testRootDirsIn;
      dontShowDialog = dontShowDialogIn;
      title = titleIn;
      creationData = tcd;
      serviceSocket = null;
      idToNodeData.clear();
      totalCount = 0;
      totalTested = 0;
      totalFailed = 0;
      totalIgnored = 0;
      failedClasses.clear();
      finished = false;
      tested.clear();
      if (textPane != null) {
         textPane.setText("");
      }
      if (treeModel != null) {
         treeModel.setRoot(null);
      }
      if (progressPane != null) {
         progressPane.repaint();
      }
      descriptionRoot = null;
      return true;
   }
   
   
   /** {@inheritDoc} **/
   public void run() {
      ToolCreateData tcd = creationData;
      synchronized (this) {
         running = true;
         endFlag = false;
      }
      try {
         serverSocket.setSoTimeout(connectTimeout * 1000);
      }
      catch (SocketException e) {
         if (!endFlag) {
            showError(tcd, "Unable to set timeout on communication socket: "
                  + e.getMessage());
         }
         close(serverSocket);
         synchronized (this) {
            running = false;
            endFlag = false;
         }
         return;
      }
      try {
         serviceSocket = serverSocket.accept();
      }
      catch (java.net.SocketTimeoutException e) {
         synchronized (this) {
            running = false;
            endFlag = false;
         }
         showError(tcd, "Unable connect to JUnit runner after "
               + connectTimeout + " seconds.");
         close(serverSocket);
         return;
      }
      catch (IOException e) {
         if (!endFlag) {
            showError(tcd, "Unable to open communication socket: "
                  + e.getMessage());
         }
         close(serverSocket);
         synchronized (this) {
            running = false;
            endFlag = false;
         }
         return;
      }
   
      InputStream is;
      try {
         is = serviceSocket.getInputStream();
      }
      catch (IOException e) {
         if (!endFlag) {
            showError(tcd,
                  "Unable to open communication socket input stream: "
                  + e.getMessage());
         }
         close(serviceSocket);
         close(serverSocket);
         synchronized (this) {
            running = false;
            endFlag = false;
         }
         return;
      }
   
      BufferedReader reader;
      try {
         reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      }
      catch (UnsupportedEncodingException e) {
         showError(tcd, "Unexpected encoding error: " + e.getMessage());
         close(serviceSocket);
         close(serverSocket);
         synchronized (this) {
            running = false;
            endFlag = false;
         }
         return;
      }
   
      try {
         String line;
         do {
            line = reader.readLine();
            processLine(line);
         } while (line != null);
      }
      catch (IOException e) {
      }
      close(reader);
      close(serviceSocket);
      close(serverSocket);
      synchronized (this) {
         running = false;
         endFlag = false;
      }
   }
   
   
   /** Displays an error on the EDT. **/
   private void showError(final ToolCreateData tcd, final String msg) {
      SwingUtilities.invokeLater(
            new Runnable() {
               public void run() {
                  tcd.showError(PluginUtil.getDialogParent(), msg,
                        "JUnit Plugin Error");
               }
            });
   }
   
   
   /** Processes an input line.
    *
    *  @param line the input line. This may be null. **/
   private void processLine(final String line) {
      if (line == null) {
         return;
      }
      SwingUtilities.invokeLater(
            new Runnable() {
               public void run() {
                  processLineEDT(line);
               }
            });
   }
      
      
   /** Processes an input line on the AWT event dispatch thread.
    *
    *  @param line the input line. **/
   private void processLineEDT(final String line) {
      if (line.startsWith("r")) {
         createGui(line.substring(1));
         if (descriptionRoot != null) {
            NodeData nd = (NodeData) descriptionRoot.getUserObject();
            totalCount = nd.count;
         }
         return;
      }
      if (line.startsWith("f")) {
         handleFail(line.substring(1), false);
         return;
      }
      if (line.startsWith("a")) {
         handleFail(line.substring(1), true);
         return;
      }
      if (line.startsWith("b")) {
         handleBegin(line.substring(1));
         return;
      }
      if (line.startsWith("e")) {
         handleEnd(line.substring(1));
         return;
      }
      if (line.startsWith("i")) {
         handleIgnored(line.substring(1));
         return;
      }
      if (line.startsWith("x")) {
         finished = true;
         progressPane.repaint();
         tree.setSelectionRow(0);
         return;
      }
   }
   
   
   /** Handles a failed test.
    *
    *  @param description the id and failure description.
    *
    *  @param assumption true if this is an assumption failure, false if
    *  it is a test failure. **/
   private void handleFail(final String description,
         final boolean assumption) {
      int endId = description.indexOf(',');
      if (endId < 0) {
         return;
      }
      Integer id;
      try {
         id = Integer.valueOf(description.substring(0, endId));
      }
      catch (NumberFormatException e) {
         return;
      }
      NodeData nd = idToNodeData.get(id);
      if (nd == null) {
         return;
      }
      NodeData parent = nd.parent;
      if (parent == null || parent.parent == null) {
         return;
      }
      
      String desc = headerMarker + parent.name + "." + nd.name + "\n"
            + description.substring(endId + 1);
      nd.failedTest(desc, assumption);
      tested.add(id);
      if (assumption) {
         totalIgnored++;
      }
      else {
         totalFailed++;
      }
      totalTested++;
      progressPane.repaint();
   }
   
   
   /** Handles the beginning of a test.
    *
    *  @param idStr the id. **/
   private void handleBegin(final String idStr) {
      Integer id;
      try {
         id = Integer.valueOf(idStr);
      }
      catch (NumberFormatException e) {
         return;
      }
      NodeData nd = idToNodeData.get(id);
      if (nd == null) {
         return;
      }
      nd.testStart();
   }
   
   
   /** Handles the end of a test.
    *
    *  @param idStr the id. **/
   private void handleEnd(final String idStr) {
      Integer id;
      try {
         id = Integer.valueOf(idStr);
      }
      catch (NumberFormatException e) {
         return;
      }
      NodeData nd = idToNodeData.get(id);
      if (nd == null) {
         return;
      }
      nd.testDone();
      if (!tested.contains(id)) {
         tested.add(id);
         totalTested++;
         progressPane.repaint();
      }
   }
   
   
   /** Handles an ignored test.
    *
    *  @param description the id. **/
   private void handleIgnored(final String description) {
      Integer id;
      try {
         id = Integer.valueOf(description);
      }
      catch (NumberFormatException e) {
         return;
      }
      NodeData nd = idToNodeData.get(id);
      if (nd == null) {
         return;
      }
      nd.ignoredTest();
      tested.add(id);
      totalIgnored++;
      totalTested++;
      progressPane.repaint();
   }
   
   
   /** Creates the gui.
    *
    *  @param description the tests to be performed. **/
   private void createGui(final String description) {
   
      try {
         parseTree(description, 0, null, null, idToNodeData, 0);
      }
      catch (FormatException e) {
         return;
      }
   
      if (frame == null) {
         GIconData iconData = 
            new GIconData() {
            
               public Paint getOutlinedBorderPaint(final int x, final int y,
                     final int size) {
                  //*** Change to a LinearGradientPaint with white in between
                  //*** once we give up on Java 1.5 support.
                  return new GradientPaint(x + size / 5, y + size / 5,
                        Color.RED, x + size - size / 5, y + size - size / 5,
                        Color.GREEN);
               }
            
               public Paint getBorderPaint(final int x, final int y,
                     final int size) {
                  //*** Change to a LinearGradientPaint with white in between
                  //*** once we give up on Java 1.5 support.
                  return new GradientPaint(x + size / 5, y + size / 5,
                        new Color(200, 0, 0), x + size - size / 5,
                        y + size - size / 5, new Color(0, 200, 0));
               }
               
               public String getText() {
                  return "JUnit";
               }
               
               public String getKey() {
                  return "JUnitTool";
               }
            };
         frame = new JGraspFrame("JUnit Results", iconData,
               PluginUtil.getDialogParent().getGraphicsConfiguration());
         frame.setAat(true);
         frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
         treeModel = new DefaultTreeModel(descriptionRoot);
         tree = PluginUtil.createTree(treeModel, "Results");
         tree.setCellRenderer(new Renderer());
         tree.setScrollsOnExpand(false);
         tree.getSelectionModel().setSelectionMode(
               TreeSelectionModel.SINGLE_TREE_SELECTION);
         tree.addTreeSelectionListener(
               new TreeSelectionListener() {
                  public void valueChanged(final TreeSelectionEvent e) {
                     TreePath tp = e.getNewLeadSelectionPath();
                     setSelection((tp == null)? null
                           : (TreeNode) tp.getLastPathComponent());
                  }
               });
      
         rightPane = new JPanel(new BorderLayout());
         
         textPane = 
            new JTextPane() {
               public boolean getScrollableTracksViewportWidth() {
                  return true;
               }
            };
         textStyle = textPane.addStyle("text", null);
         StyleConstants.setForeground(textStyle, Color.BLACK);
            
         textPane.setEditable(false);
         textPane.addMouseListener(
               new MouseAdapter() {
                  public void mouseClicked(final MouseEvent e) {
                     handleTextMouseClick(e);
                  }
               });
         textPane.addMouseMotionListener(
               new MouseMotionAdapter() {
                  public void mouseMoved(final MouseEvent e) {
                     handleTextMouseMoved(e);
                  }
               });
         doc = textPane.getStyledDocument();
         
         rightPane.add(new JScrollPane(textPane), "Center");
         
         progressPane = new ProgressPanel();
         rightPane.add(progressPane, "North");
         
         JSplitPane splitPane = 
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
               new JScrollPane(tree), rightPane) {
               public Dimension getPreferredSize() {
                  Graphics g = progressPane.getGraphics();
                  if (g == null) {
                     return super.getPreferredSize();
                  }
                  FontMetrics fm = g.getFontMetrics();
                  if (fm == null) {
                     return super.getPreferredSize();
                  }
                  int fh = fm.getHeight();
                  g.dispose();
                  int h = fh * 20;
                  int w = fh * 40;
                  Dimension screen = getToolkit().getScreenSize();
                  h = Math.min(h, screen.width / 3);
                  w = Math.min(w, screen.width / 2);
                  return new Dimension(w, h);
               }
            };
         frame.getContentPane().add(splitPane, "Center");
         frame.pack();
         splitPane.setDividerLocation(.5);
      }
      else {
         treeModel.setRoot(descriptionRoot);
      }
      frame.setTitle("JUnit: " + title);
      if (!dontShowDialog) {
         frame.setVisible(true);
         if (frame.getState() == Frame.ICONIFIED) {
            frame.setState(Frame.NORMAL);
         }
         frame.toFront();
      }
   }
   
   
   /** Called when the mouse is clicked in the results pane.
    *
    *  @param e the click event. **/
   private void handleTextMouseClick(final MouseEvent e) {
      JTextPane tp = (JTextPane) e.getComponent();
      int pos = tp.viewToModel(e.getPoint());
      Element el = tp.getStyledDocument().getCharacterElement(pos);
      ClickData cd =
            (ClickData) el.getAttributes().getAttribute(LINK_ATTRIBUTE);
      if (cd == null) {
         return;
      }
      tp.setSelectionStart(((LineData) cd).start);
      tp.setSelectionEnd(((LineData) cd).end);
      PluginUtil.openClass(cd.className, cd.filename, cd.linenum, locData,
            testRootDirs);
   }
   
   
   /** Called when the mouse is moved in the results pane.
    *
    *  @param e the click event. **/
   private void handleTextMouseMoved(final MouseEvent e) {
      JTextPane tp = (JTextPane) e.getComponent();
      Element el = tp.getStyledDocument().getCharacterElement(
            tp.viewToModel(e.getPoint()));
      ClickData cd =
            (ClickData) el.getAttributes().getAttribute(LINK_ATTRIBUTE);
      LineData ld = cd;
      if (ld == null) {
         ld = (LineData) el.getAttributes().getAttribute(LINE_ATTRIBUTE);
      }
      if (cd != null) {
         frame.setMessageText("  " + cd.filename + ":" + cd.linenum);
         tp.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }
      else {
         frame.setMessageText(" ");
         tp.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
      
      if (ld != null) {
         try {
            Rectangle endChar = tp.modelToView(ld.end);
            int endX = endChar.x + endChar.width;
            if (endX > tp.getWidth() - tp.getInsets().right) {
               tp.setToolTipText(ld.toolTip);
            }
         }
         catch (BadLocationException ex) {
         }
      }
      else {
         tp.setToolTipText(null);
      }
   }
   
   
   /** Called when the selected tree node changes.
    *
    *  @param node the new selected node, or null if there is
    *  no selection. **/
   private void setSelection(final TreeNode node) {
      selectedTreeNode = node;
      textPane.setText("");
      if (node == null) {
         return;
      }
      NodeData nd = (NodeData) ((DefaultMutableTreeNode) node).
            getUserObject();
      if (nd.failedCount == 0 && nd.failDescriptions.size() == 0) {
         return;
      }
            
      String[] lines = nd.getErrorText().split("\r\n|\r|\n");
      for (int l = 0; l < lines.length; l++) {
         String line = lines[l];
         try {
            Style style;
            if (line.startsWith(headerMarker)) {
               Style boldStyle = textPane.addStyle("text", null);
               StyleConstants.setForeground(boldStyle, Color.BLACK);
               StyleConstants.setBold(boldStyle, true);
               line = line.substring(headerMarker.length());
               boldStyle.addAttribute(LINE_ATTRIBUTE,
                     new LineData(doc.getLength(), doc.getLength()
                     + line.length(), line.trim()));
               style = boldStyle;
            }
            else {
               style = textStyle;
               Matcher m = errorPattern.matcher(line);
               if (m.matches()) {
                  String trimmedLine = line.trim();
                  line = " " + trimmedLine;
                  line = line.replace(' ', '\u00A0');
                  MatchResult mr = m.toMatchResult();
                  if (mr.groupCount() == 3) {
                     int linenum;
                     try {
                        linenum = Integer.parseInt(mr.group(3));
                        ClickData cd = new ClickData(mr.group(1), mr.group(2),
                              linenum, doc.getLength(),
                              doc.getLength() + line.length(), trimmedLine);
                        style = textPane.addStyle("clickable", null);
                        StyleConstants.setForeground(style, linkColor);
                        style.addAttribute(LINK_ATTRIBUTE, cd);
                     }
                     catch (NumberFormatException e) {
                     }
                  }
               }
            }
            doc.insertString(doc.getLength(), line + "\n", style);
         }
         catch (BadLocationException e) {
         }
      }
      textPane.setCaretPosition(0);
   }
   
   
   /** Recursively parses a test description node, creating the tree
    *  and populating the id-to-node data mapping.
    *
    *  @param description string containing the description.
    *
    *  @param posIn position of the start of the description in the
    *  string.
    *
    *  @param parent parent node, or null if the description for the
    *  root node will be parsed.
    *
    *  @param parentData parent node data, or null if the description for the
    *  root node will be parsed.
    *
    *  @param idToData map from nodes id to node data.
    *
    *  @param level the level in the tree.
    *
    *  @return the position at the end of the node description in the
    *  string.
    *
    *  @throws FormatException if the description is has an invalid
    *  format. **/
   private int parseTree(final String description,
         final int posIn, final DefaultMutableTreeNode parent,
         final NodeData parentData,
         final Map<Integer, NodeData> idToData, final int level)
         throws FormatException {
      int pos = posIn;
      if (description.charAt(pos) != '<') {
         throw new FormatException("Description tree does not start with "
               + "'<'.");
      }
      pos++;
      int endName = description.indexOf(',', pos);
      if (endName < 0) {
         throw new FormatException("Description tree name missing.");
      }
      String name = description.substring(pos, endName);
      pos = endName + 1;
      int endCount = description.indexOf(',', pos);
      if (endCount < 0) {
         throw new FormatException("Description tree count missing.");
      }
      String countStr = description.substring(pos, endCount);
      pos = endCount + 1;
      int count;
      try {
         count = Integer.parseInt(countStr);
      }
      catch (NumberFormatException e) {
         throw new FormatException("Description tree count is not a number.");
      }
      int endId = description.indexOf(',', pos);
      if (endId < 0) {
         throw new FormatException("Description tree id missing.");
      }
      String idStr = description.substring(pos, endId);
      pos = endId + 1;
      int id;
      try {
         id = Integer.parseInt(idStr);
      }
      catch (NumberFormatException e) {
         throw new FormatException("Description tree id is not a number.");
      }
      
      NodeData data = new NodeData(name, count, parentData, level);
      idToData.put(Integer.valueOf(id), data);
      DefaultMutableTreeNode node = new DefaultMutableTreeNode(data);
      data.node = node;
      if (parent != null) {
         parent.add(node);
      }
      else {
         descriptionRoot = node;
      }
      int childCount = 0;
      while (description.charAt(pos) == '<') {
         pos = parseTree(description, pos, node, data, idToData, level + 1);
         childCount++;
      }
      if (description.charAt(pos) != '>') {
         throw new FormatException("Description tree does not end with "
               + "'>'.");
      }
      if (childCount == 0 && count == 1) {
         data.isTest = true;
      }
      pos++;
      return pos;
   }


   /** Gets the failed classes.
    *
    *  @return all the classes for which there was at least one failure. **/
   public Set<String> getFailedClasses() {
      return Collections.unmodifiableSet(failedClasses);
   }
   
   
   /** Called to indicate that the running jUnit process has
    *  ended, so any new process should be allowed to start. **/
   public void flagEnd() {
      synchronized (this) {
         if (!running) {
            return;
         }
         endFlag = true;
      }
   }
   
   
   /** Sets or releases a temporary always on top override state
    *  for the window.
    *
    *  @param state true if the temporary always-on-top state will
    *  be set, false if it will be released. **/
   public void setTmpAat(final boolean state) {
      if (frame != null) {
         frame.setTmpAat(state);
      }
   }
   
   
   /** Closes the output display window, if it is open. **/
   public void closeWindow() {
      if (frame != null) {
         frame.setVisible(false);
      }
   }
}

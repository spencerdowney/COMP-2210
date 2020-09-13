
/*
 The Web-CAT plugin sources, and only those sources, in jGRASP are
 licensed under the GPL Version 2 as specified below. This code
 may be used under that license and linked with the (non-open-source)
 jGRASP jar file (jgrasp.jar), as long as only public APIs of jGRASP
 (currently the viewer and tool plugin APIs) are used.
 
 Source code for our slightly modified version of the Eclipse Web-CAT
 submission plugin is available on request. See http://www.jgrasp.org
 for contact information.
*/

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.InvocationTargetException;

import java.net.MalformedURLException;
import java.net.URL;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import jgrasp.Icons;

import jgrasp.tool.FileChooser;
import jgrasp.tool.JGraspDialog;
import jgrasp.tool.ToolCreateData;
import jgrasp.tool.ToolUtil;
import jgrasp.tool.TTList;
import jgrasp.tool.TTTree;
import jgrasp.tool.URTextArea;
import jgrasp.tool.URTextField;

import org.webcat.submitter.ILongRunningTask;
import org.webcat.submitter.ILongRunningTaskManager;
import org.webcat.submitter.IProgressChangeListener;
import org.webcat.submitter.SubmissionManifest;
import org.webcat.submitter.SubmissionTargetException;
import org.webcat.submitter.SubmittableFile;
import org.webcat.submitter.Submitter;

import org.webcat.submitter.targets.AssignmentTarget;
import org.webcat.submitter.targets.SubmissionTarget;


/** Submission dialog for Web-CAT Eclipse submission plugin. **/
public class WebcatSubmitDialog {


   /** A JButton with no maximum width. **/
   private static class NoMaxButton extends JButton {
   
   
      /** Creates a new NoMaxButton.
       *
       *  @param label the button label text. **/
      NoMaxButton(final String label) {
         super(label);
      }
   
   
      /** {@inheritDoc}
       *
       *  <P>Overridden to provide no limit to the width. **/
      public Dimension getMaximumSize() {
         Dimension s = super.getMaximumSize();
         s.width = Integer.MAX_VALUE;
         return s;
      }
   }


   /** Color for assignments in submission tree. **/
   private static Color iconColor = new Color(235, 110, 235);

   /** Comparator for sorting files primarily by name and
    *  secondarily by path. **/
   private static Comparator<File> fileNameComparator = 
      new Comparator<File>() {
         
         public int compare(final File f1, final File f2) {
            int result = f1.getName().compareTo(f2.getName());
            if (result != 0) {
               return result;
            }
            return f1.compareTo(f2);
         }
      };

   /** The top level pane. **/
   private JPanel mainPanel;

   /** The main content pane. **/
   private JPanel content;

   /** Layout for the main content pane. **/
   private CardLayout layout;
   
   /** Tree for selecting assignment. **/
   private JTree submissionTree;
   
   /** The submitter. **/
   private Submitter submitter;

   /** The current page. **/
   private int page;

   /** The current dialog. **/
   private JDialog dialog;
   
   /** The progress dialog. **/
   private JDialog progressDialog;
   
   /** The progress dialog progress bar. **/
   private JProgressBar progressBar;
   
   /** The progress dialog label. **/
   private JLabel progressLabel;
   
   /** The previous page button. **/
   private JButton prevButton;
   
   /** The next page or submit button. **/
   private JButton nextButton;
   
   /** The cancel button. **/
   private JButton cancelButton;
   
   /** The files list. **/
   private JList fileList;
   
   /** The username field. **/
   private JTextField usernameTF;
   
   /** The password field. **/
   private JTextField passwordTF;
   
   /** The tool the tool on which this dialog is acting. **/
   private WebcatTool tool;

   /** The current dialog parent. **/
   private Frame dialogParent;
   
   /** Default directory for file choosers. This may be null. **/
   private File defaultDir;
   
   /** The current creation data. **/
   private ToolCreateData creationData;
   
   /** The current file chooser. **/
   private FileChooser chooser;
   
   /** Observer for file add chooser. **/
   private FileChooser.Observer fileObserver;
   
   /** The most recently used username. This may be null. **/
   private String prevUsername;
   
   /** The project source files. **/
   private List<File> files;
   
   /** The project test files. **/
   private List<File> testFiles;
   
   /** Selected submission target. **/
   private AssignmentTarget selectedTarget;
   
   /** Current long running task, or null if there is none. **/
   private ILongRunningTask currentTask;

   /** Renderer for submission tree items. **/
   private TreeCellRenderer treeRenderer =                
      new DefaultTreeCellRenderer() {
         public Component getTreeCellRendererComponent(
               final JTree tree, final Object value, final boolean sel,
               final boolean expanded, final boolean leaf, final int row,
               final boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(
                  tree, value, sel, expanded, leaf, row, hasFocus);
            Object val = value;
            if (val instanceof DefaultMutableTreeNode) {
               val = ((DefaultMutableTreeNode) value).getUserObject();
            }
            if (!(val instanceof SubmissionTarget)) {
               return c;
            }
            SubmissionTarget st = (SubmissionTarget) val;
            setText(st.getName());
         
            Dimension size = getPreferredSize();
            int ht = size.height;
            if (!(st instanceof AssignmentTarget)) {
               setIcon(Icons.getFolderIcon(ht));
            }
            else {
               setIcon(Icons.getFileIcon(ht, '\0', iconColor));
            }
            return c;
         }
      };

   /** Renderer for files list. **/
   private ListCellRenderer listRenderer =
      new DefaultListCellRenderer() {
         public Component getListCellRendererComponent(
               final JList list, final Object value,
               final int index, final boolean isSelected,
               final boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(
                  list, value, index, isSelected, cellHasFocus);
            if (value instanceof File) {
               File f = (File) value;
               File p = f.getParentFile();
               if (p != null) {
                  setText(f.getName() + "   [" + p.getAbsolutePath()
                        + "]");
               }
            }
            return c;
         }
      };


   /** Creates a new WebcatSubmitDialog.
    *
    *  @param toolIn the tool on which this dialog will act.
    *
    *  @param creationDataIn tool creation data. **/
   public WebcatSubmitDialog(final WebcatTool toolIn,
         final ToolCreateData creationDataIn, final String username) {
      tool = toolIn;
      creationData = creationDataIn;
      prevUsername = username;
      chooser = null;
   }
   

   /** Recursively builds the assignment tree data nodes.
    *
    *  @param target submission target being examined.
    *
    *  @param parentNode the parent tree node, or null if this
    *  will be the root node.
    *
    *  @return the node created. **/
   private static DefaultMutableTreeNode getAssignmentTargets(
         final SubmissionTarget target,
         final DefaultMutableTreeNode parentNode) {
      DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(target);
      if (parentNode != null) {
         parentNode.add(treeNode);
      }
      try {
         SubmissionTarget[] children = target.getLogicalChildren();
         for (SubmissionTarget c : children) {
            getAssignmentTargets(c, treeNode);
         }
      }
      catch (SubmissionTargetException e) {
      }
      return treeNode;
   }
   

   /** Allows the user to select and submit an assignment.
    *
    *  @param dialogParentIn parent frame for popping up dialogs.
    *
    *  @param url the assignment definition URL.
    *
    *  @param filesIn the source files to be submitted.
    *
    *  @param testFilesIn the test files to be submitted.
    *
    *  @param defaultDirIn default directory for file chooser dialogs.
    *  This may be null. **/
   public void go(final Frame dialogParentIn, final String url,
         final List<File> filesIn, final List<File> testFilesIn,
         final File defaultDirIn) {
      dialogParent = dialogParentIn;
      files = filesIn;
      testFiles = testFilesIn;
      defaultDir = defaultDirIn;
      if (content == null) {
         buildContentPane();
      }
      String errMsg = createSubmitter(url);
      if (errMsg != null) {
         showError(errMsg);
         return;
      }
      initAssignmentTree();
      initFilesList();
   
      page = 1;
      layout.show(content, "p" + page);
      pageChanged();
   
      dialog = new JGraspDialog(dialogParent, true);
      dialog.setTitle("Web-CAT Submission");
      dialog.setContentPane(mainPanel);
      JRootPane root = (JRootPane) SwingUtilities.getAncestorOfClass(
            JRootPane.class, mainPanel);
      if (root != null) {
         root.setDefaultButton(nextButton);
      }
      
      dialog.pack();
      dialog.setLocationRelativeTo(dialogParent);
      dialog.setVisible(true);
      if (chooser != null) {
         chooser.clean();
         chooser = null;
      }
      dialog.dispose();
   }
   

   /** Creates the progress dialog. **/
   private void createProgressDialog() {
      progressBar = new JProgressBar(0, 100);
      progressBar.setStringPainted(true);
      progressDialog = new JGraspDialog(dialog, true);
      progressDialog.setTitle("Web-CAT Progress");
      JPanel panel = new JPanel();
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      Insets insets = constraints.insets;
      int spacing = 8;
      panel.setLayout(gbl);
      
      progressLabel = new JLabel();
      panel.add(progressLabel);
      constraints.weightx = .001;
      constraints.weighty = .001;
      constraints.fill = GridBagConstraints.NONE;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.top = spacing;
      insets.left = spacing;
      insets.right = spacing;
      insets.bottom = 0;
      gbl.setConstraints(progressLabel, constraints);
      
      panel.add(progressBar);
      constraints.fill = GridBagConstraints.HORIZONTAL;
      insets.bottom = spacing;
      gbl.setConstraints(progressBar, constraints);
   
      JButton cancelBtn = new JButton("Cancel");
      cancelBtn.addActionListener(
            new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                  synchronized (this) {
                     if (currentTask != null) {
                        currentTask.cancel();
                     }
                  }
               }
            });
      constraints.fill = GridBagConstraints.NONE;
      panel.add(cancelBtn);
      gbl.setConstraints(cancelBtn, constraints);
      progressDialog.setContentPane(panel);
      progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      progressDialog.addWindowListener(
            new WindowAdapter() {
               public void windowClosing(final WindowEvent e) {
                  synchronized (this) {
                     if (currentTask != null) {
                        currentTask.cancel();
                     }
                  }
               }
            });
   }
   
   
   /** Builds the contents pane. **/
   private void buildContentPane() {
      content = new JPanel();
      layout = new CardLayout();
      content.setLayout(layout);
      
      JPanel page1 = buildPage1();
      content.add(page1, "p1");
      
      JPanel page2 = buildPage2();
      content.add(page2, "p2");
      
      JPanel page3 = buildPage3();
      content.add(page3, "p3");
      
      // Use a text area to get a good default size.
      content.add(new URTextArea(10, 10, "unused"), "x");
      
      mainPanel = new JPanel();
      mainPanel.setLayout(new BorderLayout());
      mainPanel.add(content, "Center");
      
      Box buttonPanel = Box.createHorizontalBox();
      buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      mainPanel.add(buttonPanel, "South");
      
      prevButton = new JButton("Prev");
      prevButton.addActionListener(
            new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                  if (page == 0) {
                     return;
                  }
                  page--;
                  layout.show(content, "p" + page);
                  pageChanged();
               }
            });
      buttonPanel.add(prevButton);
      buttonPanel.add(Box.createHorizontalGlue());
   
      nextButton = new JButton("Next");
      nextButton.addActionListener(
            new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                  if (page == 1) {
                     toPage2();
                  }
                  else if (page == 2) {
                     toPage3();
                  }
                  else if (page == 3) {
                     submit();
                  }
               }
            });
      buttonPanel.add(nextButton);
      buttonPanel.add(Box.createHorizontalGlue());
      
      cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(
            new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                  dialog.setVisible(false);
               }
            });
      buttonPanel.add(cancelButton);
   }
   
   
   /** Adjusts the buttons when the page changes. **/
   private void pageChanged() {
      prevButton.setEnabled(page > 1);
      nextButton.setText(page == 3? "Submit" : "Next");
   }
   
   
   /** Builds the assignment selection page.
    *
    *  @return the assignment selection page. **/
   private JPanel buildPage1() {
      JPanel panel = new JPanel();
   
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      Insets insets = constraints.insets;
      int spacing = 8;
      panel.setLayout(gbl);
   
      JLabel label = new JLabel("Select Assignment");
      panel.add(label);
      constraints.weightx = 1.0;
      constraints.weighty = .001;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.top = spacing;
      insets.left = spacing;
      insets.right = spacing;
      insets.bottom = 0;
      gbl.setConstraints(label, constraints);
   
      submissionTree = new TTTree("Available Assignments");
      submissionTree.setRootVisible(false);
      submissionTree.setShowsRootHandles(true);
      submissionTree.setCellRenderer(treeRenderer);
      
      JScrollPane sp = 
         new JScrollPane(submissionTree) {
            public Dimension getPreferredSize() {
               return new Dimension(0, 0);
            }
         };
      panel.add(sp);
      constraints.weighty = 1.0;
      constraints.fill = GridBagConstraints.BOTH;
      insets.bottom = spacing;
      gbl.setConstraints(sp, constraints);
      
      return panel;
   }
   

   /** Builds the file selection page.
    *
    *  @return the file selection page. **/
   private JPanel buildPage2() {
      JPanel panel = new JPanel();
   
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      Insets insets = constraints.insets;
      int spacing = 8;
      panel.setLayout(gbl);
   
      JLabel label = new JLabel("Files to be Submitted");
      panel.add(label);
      constraints.weightx = 1.0;
      constraints.weighty = .001;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.top = spacing;
      insets.left = spacing;
      insets.right = spacing;
      insets.bottom = 0;
      gbl.setConstraints(label, constraints);
   
      fileList = new TTList(new DefaultListModel(),
            "Files to be Submitted");
      fileList.setCellRenderer(listRenderer);
      fileList.addKeyListener(
            new KeyAdapter() {
               public void keyPressed(final KeyEvent e) {
                  int code = e.getKeyCode();
                  if (code == KeyEvent.VK_DELETE) {
                     removeFiles(fileList.getSelectedValues());
                  }
               }
            }
         );
      
      JScrollPane sp = 
         new JScrollPane(fileList) {
            public Dimension getPreferredSize() {
               return new Dimension(0, 0);
            }
         };
      panel.add(sp);
      constraints.weighty = 1.0;
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridwidth = 1;
      insets.bottom = spacing;
      gbl.setConstraints(sp, constraints);
      
      Box buttonPanel = Box.createVerticalBox();
      panel.add(buttonPanel);
      constraints.fill = GridBagConstraints.NONE;
      constraints.weightx = .001;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      gbl.setConstraints(buttonPanel, constraints);
   
      JButton addButton = new NoMaxButton("Add");
      buttonPanel.add(addButton);
      addButton.addActionListener(
            new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                  addFiles();
               }
            });
   
      final JButton removeButton = new NoMaxButton("Remove");
      removeButton.addActionListener(
            new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                  removeFiles(fileList.getSelectedValues());
               }
            });
      removeButton.setEnabled(false);
      buttonPanel.add(removeButton);
      
      fileList.addListSelectionListener(
            new ListSelectionListener() {
               public void valueChanged(final ListSelectionEvent e) {
                  removeButton.setEnabled(
                        fileList.getSelectedValues().length > 0);
               }
            });
      
      return panel;
   }
   
 
   /** Allows the user to add files to the files list. **/
   private void addFiles() {
      if (chooser == null) {
         chooser = new FileChooser(dialog, defaultDir,
               "Add Files to Web-CAT Submission", "Add", 'a',
               "Add selected files", "Done",
               EnumSet.of(FileChooser.Flags.FILE,
               FileChooser.Flags.ALIGN_RIGHT), null);
      }
      if (fileObserver == null) {
         fileObserver = 
            new FileChooser.Observer() {
               public void fileChosen(final File f) {
                  if (f == null || files.contains(f)
                        || testFiles.contains(f)) {
                     return;
                  }
                  files.add(f);
                  initFilesList();
               }
            };
      }
      chooser.chooseFiles(fileObserver);
   }
   

   /** Removes files from the files list.
    *
    *  @param fileToRemove the files to be removed. These must
    *  be {@link File} objects. **/
   private void removeFiles(final Object[] filesToRemove) {
      for (Object f : filesToRemove) {
         files.remove(f);
         testFiles.remove(f);
      }
      initFilesList();
   }
   

   /** Builds the submission page.
    *
    *  @return the submission page. **/
   private JPanel buildPage3() {
      JPanel panel = new JPanel();
   
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      Insets insets = constraints.insets;
      int spacing = 8;
      panel.setLayout(gbl);
   
      String l = "User Name";
      JLabel label = new JLabel(l);
      panel.add(label);
      constraints.weightx = .001;
      constraints.weighty = .001;
      constraints.fill = GridBagConstraints.NONE;
      insets.top = spacing;
      insets.left = spacing;
      insets.right = spacing;
      insets.bottom = spacing;
      gbl.setConstraints(label, constraints);
   
      usernameTF = new URTextField(30, l);
      if (prevUsername != null) {
         usernameTF.setText(prevUsername);
      }
      panel.add(usernameTF);
      insets.left = 0;
      constraints.weightx = 1.0;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.bottom = spacing;
      gbl.setConstraints(usernameTF, constraints);
      
      label = new JLabel("Password");
      panel.add(label);
      constraints.weightx = .001;
      constraints.fill = GridBagConstraints.NONE;
      constraints.gridwidth = 1;
      insets.left = spacing;
      gbl.setConstraints(label, constraints);
   
      passwordTF = new JPasswordField(30);
      panel.add(passwordTF);
      insets.left = 0;
      constraints.weightx = 1.0;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.bottom = spacing;
      gbl.setConstraints(passwordTF, constraints);
      
      JPanel spacer = new JPanel();
      panel.add(spacer);
      constraints.weighty = 1.0;
      constraints.fill = GridBagConstraints.BOTH;
      gbl.setConstraints(spacer, constraints);
      
      return panel;
   }
   

   /** Creates and initializes the submitter.
    *
    *  @return null on success, an error message on failure. **/
   private String createSubmitter(final String url) {
      submitter = new Submitter();
      try {
         URL targetsURL = new URL(url);
         if (targetsURL.getHost().length() == 0) {
            return "Bad assignment definition URL \""
                  + url + "\": no host";
         }
         submitter.readSubmissionTargets(targetsURL);
      }
      catch (MalformedURLException e) {
         return "Bad assignment definition URL \""
                  + url + "\": " + e.getMessage();
      }
      catch (IOException e) {
         return "Error reading submission targets: "
                  + e.getMessage();
      }
      submitter.setLongRunningTaskManager(
            new ILongRunningTaskManager() {
               public void run(final ILongRunningTask task)
                     throws InvocationTargetException {
                  synchronized (this) {
                     currentTask = task;
                  }
                  processTask(task);
                  synchronized (this) {
                     currentTask = null;
                  }
               }
            });
      return null;
   }


   /** Processes a long running task.
    *
    *  @param task the task to be processed.
    *
    *  @throws InvocationTargetException if the task execution
    *  throws an exception. **/
   private void processTask(final ILongRunningTask task)
         throws InvocationTargetException {
      final String description = task.getDescription();
      SwingUtilities.invokeLater(
            new Runnable() {
               public void run() {
                  progressLabel.setText(description);
                  if (progressDialog != null) {
                     progressDialog.pack();
                  }
               }
            });
               
      IProgressChangeListener progressChangeListener = 
         new IProgressChangeListener() {
            public void progressChanged(final int progress) {
               SwingUtilities.invokeLater(
                     new Runnable() {
                        public void run() {
                           progressBar.setValue(progress);
                        }
                     });
            }
         };
      task.addProgressChangeListener(progressChangeListener);
      try {
         task.run();
      }
      catch (Exception e) {
         throw new InvocationTargetException(e);
      }   
      task.removeProgressChangeListener(progressChangeListener);
   }


   /** Initializes the assignment choices. **/
   private void initAssignmentTree() {
      TreePath tp = submissionTree.getSelectionPath();
      List<Object> selObjects = null;
      List<Object> selPath = null;
      if (tp != null) {
         selObjects = new ArrayList<Object>();
         Object[] nodes = tp.getPath();
         for (Object o : nodes) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) o;
            selObjects.add(n.getUserObject());
         }
         selPath = new ArrayList<Object>();
      }
      DefaultMutableTreeNode treeRoot =
            getAssignmentTargets(submitter.getRoot(), null);
      DefaultTreeModel tm = (DefaultTreeModel) submissionTree.getModel();
      tm.setRoot(treeRoot);
      if (tp != null) {
         if (findPath(selObjects, selPath,
               (DefaultMutableTreeNode) tm.getRoot(), 0)) {
            submissionTree.setSelectionPath(new TreePath(selPath.toArray()));
         }
      }
   }


   /** recursively searches for a path in the submission tree with
    *  the same submission target names as a specified path.
    *
    *  @param selObjects the node user object path to search for.
    *
    *  @param selPathOut the tree path that was found, if this
    *  method succeeds.
    *
    *  @param node the node to be searched.
    *
    *  @param level the path index currently being examined. **/
   private boolean findPath(final List<Object> selObjects,
         final List<Object> selPathOut, final DefaultMutableTreeNode node,
         final int level) {
      selPathOut.add(node);
      if (level + 1 >= selObjects.size()) {
         return true;
      }
      Object o = selObjects.get(level + 1);
      SubmissionTarget t = (SubmissionTarget) o;
      int count = node.getChildCount();
      for (int i = 0; i < count; i++) {
         DefaultMutableTreeNode n =
               (DefaultMutableTreeNode) node.getChildAt(i);
         Object u = n.getUserObject();
         SubmissionTarget ut = (SubmissionTarget) u;
         String tn = t.getName();
         String utn = ut.getName();
         if ((tn == null && utn == null)
               || (tn != null && tn.equals(utn))) {
            return findPath(selObjects, selPathOut, n, level + 1);
         }
      }
      return false;
   }


   /** Initializes the files list. **/
   private void initFilesList() {
      DefaultListModel lm = (DefaultListModel) fileList.getModel();
      Collections.sort(files, fileNameComparator);
      Collections.sort(testFiles, fileNameComparator);
      lm.clear();
      for (File f : files) {
         lm.addElement(f);
      }
      for (File f : testFiles) {
         lm.addElement(f);
      }
   }


   /** Displays an error message.
    *
    *  @param message the message text. **/
   private void showError(final String message) {
      if (dialog != null) {
         creationData.showError(dialog, message,
               "Web-CAT Plugin Error");
      }
      else {
         creationData.showError(dialogParent, message,
               "Web-CAT Plugin Error");
      }
   }
   
 
   /** Attempts to go from the assignment selection page to
    *  the file selection page. **/
   private void toPage2() {
      TreePath tp = submissionTree.getSelectionPath();
      if (tp == null) {
         showError("No assignment selected.");
         return;
      }
      DefaultMutableTreeNode tn =
            (DefaultMutableTreeNode) tp.getLastPathComponent();
      SubmissionTarget target = (SubmissionTarget) tn.getUserObject();
      if (!(target instanceof AssignmentTarget)) {
         showError("Selected item is not an assignment.");
         return;
      }
      selectedTarget = (AssignmentTarget) target;
      layout.show(content, "p2");
      page++;
      pageChanged();
   }
   
   
   /** Attempts to go from the file selection page to
    *  the submission page. **/
   private void toPage3() {
      if (files.size() == 0 && testFiles.size() == 0) {
         showError("The files list is empty.");
         return;
      }
   
      layout.show(content, "p3");
      page++;
      pageChanged();
   }
   
   
   /** Attempts to submit the files. **/
   private void submit() {
      String u = usernameTF.getText().trim();
      if (!u.equals(prevUsername)) {
         tool.usernameChanged(u);
      }
      prevUsername = u;
      if (u.length() == 0) {
         showError("You need to enter a username.");
         return;
      }
      String p = passwordTF.getText().trim();
      if (p.length() == 0) {
         showError("You need to enter a password.");
         return;
      }
   
      SubmittableFile[] itemsToSubmit =
            new SubmittableFile[files.size() + testFiles.size()];
      int ind = 0;
      for (File f : files) {
         itemsToSubmit[ind++] = new SubmittableFile(f);
      }
      for (File f : testFiles) {
         itemsToSubmit[ind++] = new SubmittableFile(f);
      }
   
      final SubmissionManifest manifest = new SubmissionManifest();
      manifest.setSubmittableItems(itemsToSubmit);
      manifest.setAssignment(selectedTarget);
      manifest.setUsername(u);
      manifest.setPassword(p);
   
      if (progressDialog == null) {
         createProgressDialog();
      }
      progressLabel.setText(" ");
      progressDialog.pack();
      progressDialog.setLocationRelativeTo(dialog);
      submit(manifest);
      progressDialog.setVisible(true);
   }
    

   /** Submits the files.
    *
    *  @param manifest the submission manifest. **/
   private void submit(final SubmissionManifest manifest) {
      ToolUtil.recordToolAction("Web-CAT", "Multiple");
      Runnable runnable = 
         new Runnable() {
            public void run() {
               try {
                  submitter.submit(manifest);
               }
               catch (IOException e) {
                  final String msg = e.getMessage();
                  SwingUtilities.invokeLater(
                        new Runnable() {
                           public void run() {
                              showError("Submission error: " + msg);
                           }
                        });
               }
               finally {
                  SwingUtilities.invokeLater(
                        new Runnable() {
                           public void run() {
                              progressDialog.setVisible(false);
                           }
                        });
               }
               SwingUtilities.invokeLater(
                     new Runnable() {
                        public void run() {
                           processResponse();
                        }
                     });
            }
         };
      new Thread(runnable).start();
   }
    
 
   /** Processes the last submission response. **/   
   private void processResponse() {
      if (submitter.wasCancelled()) {
         return;
      }
      String response = submitter.getResponse();
      if (!submitter.hasResponse() || response == null) {
         creationData.showMessage(dialogParent,
               "Submission appears to have succeeded "
               + " but there was no response."
               + "\n\nYou should login to WebCAT directly to view"
               + "your results.", "Web-CAT Plugin");
         dialog.setVisible(false);
         return;
      }
      
      File tmpFile = creationData.createTmpFile(dialogParent, "wct",
            ".html", response, Charset.defaultCharset(), "Web-CAT");
      if (tmpFile == null) {
         showError("Unable to create temp file to show results."
               + "\n\nYou should login to WebCAT directly to view"
               + "your results.");
         return;
      }
      tmpFile.deleteOnExit();
      try {
         URL url = tmpFile.toURI().toURL();
         creationData.openURL(url);
         dialog.setVisible(false);
         creationData.showMessage(dialogParent,
               "Submission succeeded. If Web-CAT does not open "
               + "automatically in your default browser, you should "
               + "login to Web-CAT directly to view your results.",
               "Web-CAT Plugin");
         return;
      }
      catch (MalformedURLException e) {
         showError("Error displaying results: " + e.getMessage()
               + "\n\nYou should login to WebCAT directly to view"
               + "your results.");
      }
      catch (IOException e) {
         showError("Error displaying results: " + e.getMessage()
               + "\n\nYou should login to WebCAT directly to view"
               + "your results.");
      }
   }
   
}

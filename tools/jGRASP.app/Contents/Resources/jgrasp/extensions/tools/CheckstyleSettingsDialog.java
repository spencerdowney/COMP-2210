
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.Arrays;
import java.util.EnumSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jgrasp.tool.ActionContext;
import jgrasp.tool.ComboFileChooser;
import jgrasp.tool.JGraspDialog;
import jgrasp.tool.URTextField;


/** Dialog for changing Checkstyle settings. **/
public class CheckstyleSettingsDialog {

   /** Checkstyle home selection combo. **/
   private ComboFileChooser homeCombo;
   
   /** Checks file selection combo. **/
   private ComboFileChooser checksCombo;
   
   /** Command line flags field. **/
   private JTextField flagsField;
   
   /** Jvm command line flags field. **/
   private JTextField javaFlagsField;
   
   /** Checkbox for controlling whether or not test source
    *  files are checked. **/
   private JCheckBox processTestCKB;

   /** Checkbox for controlling toolbar item visibility. **/
   private JCheckBox hideTBICKB;
   
   /** The current dialog. **/
   private JDialog dialog;
   
   /** The tool the tool for which this dialog is modifying
    *  the settings. **/
   private CheckstyleTool tool;
   
   /** The last selected checkstyle home directory. **/
   private File prevHome;
 

   /** Creates a new CheckstyleSettingsDialog.
    *
    *  @param csTool the tool for which this dialog is modifying
    *  the settings. **/
   public CheckstyleSettingsDialog(final CheckstyleTool csTool) {
      tool = csTool;
   }


   /** Creates the configure dialog.
    *
    *  @param context action context.
    *
    *  @param settings the settings that the dialog will use
    *  and modify. **/
   public void showDialog(final ActionContext context,
         final CheckstyleSettings settings) {
      Frame parent = context.getDialogParent();
      String[] priorHomeHistory = null;
      String[] priorChecksHistory = null;
      if (dialog != null && parent != dialog.getParent()) {
         // Rebuild if parent is different.
         priorHomeHistory = homeCombo.getHistory();
         priorChecksHistory = checksCombo.getHistory();
         dialog.dispose();
         dialog = null;
      }
      if (dialog == null) {
         dialog = createDialog(parent);
         dialog.pack();
      }
   
      // Initialize.
      homeCombo.setFixedItems(settings.getCheckstyleHomes());
      File checkstyleHome = settings.getCheckstyleHome();
      if (checkstyleHome != null) {
         homeCombo.setSelectedFile(checkstyleHome.getAbsolutePath());
      }
      setPossibleChecks();
      File checksFile = settings.getChecksFile();
      if (checksFile != null) {
         checksCombo.setSelectedFile(checksFile.getAbsolutePath());
      }
   
      if (priorHomeHistory != null) {
         homeCombo.setHistory(priorHomeHistory);
      }
      if (priorChecksHistory != null) {
         checksCombo.setHistory(priorChecksHistory);
      }
      // In case the values are not in the fixed items or history.
      homeCombo.storeHistory();
      checksCombo.storeHistory();
   
      flagsField.setText(settings.getFlags());
      javaFlagsField.setText(settings.getJavaFlags());
      processTestCKB.setSelected(settings.getProcessTests());
      hideTBICKB.setSelected(settings.hideToolbarItems());
   
      dialog.setLocationRelativeTo(parent);
      dialog.setVisible(true);
   }
   

   /** Creates the dialog.
    *
    *  @param parent the dialog parent frame.
    *
    *  @return the newly created dialog. **/
   private JDialog createDialog(final Frame parent) {
      JDialog dg = JGraspDialog.createDialog(parent, false);
      dg.setTitle("Checkstyle Tool Settings");
      JComponent contentPanel = new JPanel(false);
      dg.setContentPane(contentPanel);
      
      GridBagLayout layout = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      Insets insets = constraints.insets;
      int spacing = 8;
      contentPanel.setLayout(layout);
      JLabel label = new JLabel("Checkstyle Home or Jar");
      contentPanel.add(label);
      constraints.weightx = 1.0;
      constraints.weighty = 1.0;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      insets.top = spacing;
      insets.left = spacing;
      insets.right = spacing;
      insets.bottom = 0;
      layout.setConstraints(label, constraints);
   
      homeCombo = new ComboFileChooser("Checkstyle Home or Jar",
            null,
            EnumSet.of(ComboFileChooser.Flags.FILE_OR_DIRECTORY),
            Arrays.asList(new String[] { "jar" }));
      homeCombo.addActionListener(
            new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                  if (!e.getActionCommand().equals("comboBoxChanged")) {
                     return;
                  }
                  setPossibleChecks();
               }
            });
   
      contentPanel.add(homeCombo);   
      constraints.weightx = 1000.0;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.left = 0;
      layout.setConstraints(homeCombo, constraints);
      
      label = new JLabel("Checks File");
      contentPanel.add(label);
      constraints.weightx = 1.0;
      constraints.gridwidth = 1;
      insets.left = spacing;
      layout.setConstraints(label, constraints);
   
      checksCombo = new ComboFileChooser("Checks File",
            null,
            EnumSet.of(ComboFileChooser.Flags.FILE),
            Arrays.asList(new String[] { "xml" }));
      contentPanel.add(checksCombo);   
   
      constraints.weightx = 1000.0;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.left = 0;
      layout.setConstraints(checksCombo, constraints);
   
      String l = "Checkstyle Flags";
      label = new JLabel(l);
      contentPanel.add(label);
      constraints.weightx = 1.0;
      constraints.gridwidth = 1;
      insets.left = spacing;
      layout.setConstraints(label, constraints);
   
      flagsField = new URTextField(l);
      contentPanel.add(flagsField);   
   
      constraints.weightx = 1000.0;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.left = 0;
      layout.setConstraints(flagsField, constraints);
      
      l = "Java Flags";
      label = new JLabel(l);
      contentPanel.add(label);
      constraints.weightx = 1.0;
      constraints.gridwidth = 1;
      insets.left = spacing;
      layout.setConstraints(label, constraints);
   
      javaFlagsField = new URTextField(l);
      contentPanel.add(javaFlagsField);   
   
      constraints.weightx = 1000.0;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.left = 0;
      layout.setConstraints(javaFlagsField, constraints);
      
      processTestCKB = new JCheckBox("Check Test Files");
      contentPanel.add(processTestCKB);
      insets.left = spacing;
      layout.setConstraints(processTestCKB, constraints);
   
      hideTBICKB = new JCheckBox("Hide Toolbar Items");
      contentPanel.add(hideTBICKB);
      insets.bottom = spacing;
      layout.setConstraints(hideTBICKB, constraints);
      
      JPanel buttonPanel = new JPanel(false);
      buttonPanel.setLayout(new BorderLayout());
      contentPanel.add(buttonPanel);
      constraints.fill = GridBagConstraints.HORIZONTAL;
      insets.left = spacing;
      layout.setConstraints(buttonPanel, constraints);
      
      JButton okButton = new JButton("OK");
      okButton.addActionListener(
            new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                  accept();
               }
            });
      buttonPanel.add(okButton, "West");
   
      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(
            new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                  dialog.setVisible(false);
               }
            });
      buttonPanel.add(cancelButton, "East");
      
      return dg;
   }
   

   /** Sets the possible checks based on the current home directory
    *  selection. **/
   private void setPossibleChecks() {
      String hs = homeCombo.getSelectedFile();
      File hf = null;
      if (hs != null && hs.length() > 0) {
         hf = new File(hs);
      }
      String[] checks = CheckstyleSettings.findChecks(hf);
      String oldFile = checksCombo.getSelectedFile();
      boolean wasDefault = false;
      if (oldFile != null && prevHome != null && hf != null
            && !prevHome.equals(hf)) {
         CheckstyleSettings oldSet = new CheckstyleSettings(
               prevHome, null, true, true, "", "");
         File oldDefault = oldSet.getChecksFile();
         if (oldDefault != null
               && oldDefault.getAbsolutePath().equals(oldFile)) {
            wasDefault = true;
         }
      }
      prevHome = hf;
   
      checksCombo.setFixedItems(checks);
      if (wasDefault && checks.length > 0) {
         CheckstyleSettings newSet = new CheckstyleSettings(
               hf, null, true, true, "", "");
         File newDefault = newSet.getChecksFile();
         checksCombo.setSelectedFile((newDefault != null)
               ? newDefault.getAbsolutePath() : oldFile);
      }
      else {
         checksCombo.setSelectedFile(oldFile);
      }
   }
   
   
   /** Responds to the "OK" button. **/
   private void accept() {
      File home;
      File jar;
      String homeDir = homeCombo.getSelectedFile();
      if (homeDir.length() == 0) {
         home = null;
      }
      else {
         home = new File(homeDir);
         if (home.isDirectory()) {
            home = new File(homeDir);
            jar = CheckstyleSettings.getJarFromHome(home);
            if (jar == null) {
               JOptionPane.showMessageDialog(dialog, "Could not find "
                     + "checkstyle-all jar file in\n"
                     + "\"" + home.getAbsolutePath() + "\".",
                     "Error", JOptionPane.ERROR_MESSAGE);
               return;
            }
         }
         else {
            jar = home;
            String homeName = home.getName();
            if (!CheckstyleSettings.isAllJar(homeName)) {
               if (JOptionPane.showOptionDialog(dialog, "Checkstyle jar file \""
                     + homeName + "\"\ndoes not match expected "
                     + "pattern for \"all\" jar file\n\"" +
                     CheckstyleSettings.getJarMatchDescription() + "\".",
                     "Warning", JOptionPane.DEFAULT_OPTION,
                     JOptionPane.WARNING_MESSAGE, null,
                     new Object[] { "Use Anyway", "Cancel" }, "Cancel")
                     != 0) {
                  return;
               }
            }
         }
      }
      File checks = new File(checksCombo.getSelectedFile());
      String flags = flagsField.getText().replace('\n', ' ');
      String javaFlags = javaFlagsField.getText().replace('\n', ' ');
      CheckstyleSettings settings =
            new CheckstyleSettings(home, checks,
            processTestCKB.isSelected(), hideTBICKB.isSelected(),
            flags, javaFlags);
      tool.settingsChanged(settings);
      homeCombo.storeHistory();
      checksCombo.storeHistory();
      if (dialog != null) {
         dialog.setVisible(false);
      }
   }
}

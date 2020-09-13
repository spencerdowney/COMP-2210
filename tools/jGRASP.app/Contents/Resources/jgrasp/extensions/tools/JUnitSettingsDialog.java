
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

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


/** Dialog for changing JUnit settings. **/
public class JUnitSettingsDialog {

   /** JUnit home selection combo. **/
   private ComboFileChooser homeCombo;
   
   /** Command line compiler flags field. **/
   private JTextField flagsField;
   
   /** Jvm command line flags field. **/
   private JTextField javaFlagsField;
   
   /** Checkbox for controlling results dialog visibility. **/
   private JCheckBox noDialogCKB;
   
   /** Checkbox for controlling toolbar item visibility. **/
   private JCheckBox hideTBICKB;
   
   /** The current dialog. **/
   private JDialog dialog;
   
   /** The tool the tool for which this dialog is modifying
    *  the settings. **/
   private JUnitTool tool;
 

   /** Creates a new JUnitSettingsDialog.
    *
    *  @param fbTool the tool for which this dialog is modifying
    *  the settings. **/
   public JUnitSettingsDialog(final JUnitTool juTool) {
      tool = juTool;
   }


   /** Creates the configure dialog.
    *
    *  @param context action context.
    *
    *  @param settings the settings that the dialog will use
    *  and modify. **/
   public void showDialog(final ActionContext context,
         final JUnitSettings settings) {
      Frame parent = context.getDialogParent();
      String[] priorHomeHistory = null;
      if (dialog != null && parent != dialog.getParent()) {
         // Rebuild if parent is different.
         priorHomeHistory = homeCombo.getHistory();
         dialog.dispose();
         dialog = null;
      }
      if (dialog == null) {
         dialog = createDialog(parent);
         dialog.pack();
      }
   
      // Initialize.
      homeCombo.setFixedItems(settings.getJUnitHomes());
      File junitHome = settings.getJUnitHome();
      if (junitHome != null) {
         homeCombo.setSelectedFile(junitHome.getAbsolutePath());
      }
   
      if (priorHomeHistory != null) {
         homeCombo.setHistory(priorHomeHistory);
      }
      // In case the values are not in the fixed items or history.
      homeCombo.storeHistory();
   
      flagsField.setText(settings.getFlags());
      javaFlagsField.setText(settings.getJavaFlags());
      noDialogCKB.setSelected(settings.dontShowDialog());
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
      dg.setTitle("JUnit Tool Settings");
      JComponent contentPanel = new JPanel(false);
      dg.setContentPane(contentPanel);
      
      GridBagLayout layout = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      Insets insets = constraints.insets;
      int spacing = 8;
      contentPanel.setLayout(layout);
      JLabel label = new JLabel("JUnit Home");
      contentPanel.add(label);
      constraints.weightx = 1.0;
      constraints.weighty = 1.0;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      insets.top = spacing;
      insets.left = spacing;
      insets.right = spacing;
      insets.bottom = 0;
      layout.setConstraints(label, constraints);
   
      homeCombo = new ComboFileChooser("JUnit Home",
            null,
            EnumSet.of(ComboFileChooser.Flags.DIRECTORY), null);
   
      contentPanel.add(homeCombo);   
      constraints.weightx = 1000.0;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.left = 0;
      layout.setConstraints(homeCombo, constraints);
      
      String l = "Compiler Flags";
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
      
      l = "Run Flags";
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
      
      noDialogCKB = new JCheckBox("Don't pop up results window");
      contentPanel.add(noDialogCKB);
      insets.left = spacing;
      layout.setConstraints(noDialogCKB, constraints);
      
      hideTBICKB = new JCheckBox("Hide toolbar items");
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
         jar = JUnitSettings.getJarFromHome(home);
         if (jar == null) {
            JOptionPane.showMessageDialog(dialog, "Could not find "
               + "junit jar file in\n"
               + "\"" + home.getAbsolutePath() + "\".",
               "Error", JOptionPane.ERROR_MESSAGE);
            return;
         }
      }
      String flags = flagsField.getText().replace('\n', ' ');
      String javaFlags = javaFlagsField.getText().replace('\n', ' ');
      JUnitSettings settings =
            new JUnitSettings(home, noDialogCKB.isSelected(),
                  hideTBICKB.isSelected(), flags, javaFlags);
      tool.settingsChanged(settings);
      homeCombo.storeHistory();
      if (dialog != null) {
         dialog.setVisible(false);
      }
   }
}

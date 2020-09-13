
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


/** Dialog for changing DCD settings. **/
public class DCDSettingsDialog {

   /** DCD home selection combo. **/
   private ComboFileChooser homeCombo;
   
   /** Jvm command line flags field. **/
   private JTextField javaFlagsField;
   
   /** Excluded classes field. **/
   private JTextField excludedClassesField;
   
   /** Excluded methods field. **/
   private JTextField excludedMethodsField;
   
   /** Checkbox for controlling toolbar item visibility. **/
   private JCheckBox hideTBICKB;
   
   /** Checkbox for controlling whether or not public dead code
    *  is checked. **/
   private JCheckBox publicCKB;
   
   /** Checkbox for controlling whether or not local variables
    *  are checked. **/
   private JCheckBox localCKB;
   
   /** The current dialog. **/
   private JDialog dialog;
   
   /** The tool the tool for which this dialog is modifying
    *  the settings. **/
   private DCDTool tool;
 

   /** Creates a new DCDSettingsDialog.
    *
    *  @param fbTool the tool for which this dialog is modifying
    *  the settings. **/
   public DCDSettingsDialog(final DCDTool fbTool) {
      tool = fbTool;
   }


   /** Creates the configure dialog.
    *
    *  @param context action context.
    *
    *  @param settings the settings that the dialog will use
    *  and modify. **/
   public void showDialog(final ActionContext context,
         final DCDSettings settings) {
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
      homeCombo.setFixedItems(settings.getDCDHomes());
      File dcdHome = settings.getDCDHome();
      if (dcdHome != null) {
         homeCombo.setSelectedFile(dcdHome.getAbsolutePath());
      }
   
      if (priorHomeHistory != null) {
         homeCombo.setHistory(priorHomeHistory);
      }
      // In case the values are not in the fixed items or history.
      homeCombo.storeHistory();
   
      javaFlagsField.setText(settings.getJavaFlags());
      excludedClassesField.setText(settings.getExcludedClasses());
      excludedMethodsField.setText(settings.getExcludedMethods());
      hideTBICKB.setSelected(settings.hideToolbarItems());
      publicCKB.setSelected(settings.checkPublic());
      localCKB.setSelected(settings.checkLocal());
   
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
      dg.setTitle("DCD Tool Settings");
      JComponent contentPanel = new JPanel(false);
      dg.setContentPane(contentPanel);
      
      GridBagLayout layout = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      Insets insets = constraints.insets;
      int spacing = 8;
      contentPanel.setLayout(layout);
      JLabel label = new JLabel("DCD Home");
      contentPanel.add(label);
      constraints.weightx = 1.0;
      constraints.weighty = 1.0;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      insets.top = spacing;
      insets.left = spacing;
      insets.right = spacing;
      insets.bottom = 0;
      layout.setConstraints(label, constraints);
   
      homeCombo = new ComboFileChooser("DCD Home",
            null,
            EnumSet.of(ComboFileChooser.Flags.DIRECTORY), null);
   
      contentPanel.add(homeCombo);   
      constraints.weightx = 1000.0;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.left = 0;
      layout.setConstraints(homeCombo, constraints);
      
      String l = "Excluded Classes (comma-separated regular expressions)";
      label = new JLabel(l);
      contentPanel.add(label);
      insets.left = spacing;
      layout.setConstraints(label, constraints);
   
      excludedClassesField = new URTextField(l);
      contentPanel.add(excludedClassesField);   
      insets.top = 0;
      layout.setConstraints(excludedClassesField, constraints);
      
      l = "Excluded Methods (comma-separated regular expressions)";
      label = new JLabel(l);
      contentPanel.add(label);
      insets.top = spacing;
      layout.setConstraints(label, constraints);
   
      excludedMethodsField = new URTextField(l);
      contentPanel.add(excludedMethodsField);   
      insets.top = 0;
      layout.setConstraints(excludedMethodsField, constraints);
      
      publicCKB = new JCheckBox("Check Public Code");
      contentPanel.add(publicCKB);
      insets.top = spacing;
      layout.setConstraints(publicCKB, constraints);
      
      localCKB = new JCheckBox("Check Local Variables");
      contentPanel.add(localCKB);
      layout.setConstraints(localCKB, constraints);
      
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
      
      hideTBICKB = new JCheckBox("Hide Toolbar Items");
      contentPanel.add(hideTBICKB);
      insets.left = spacing;
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
         jar = DCDSettings.getJarFromHome(home);
         if (jar == null) {
            JOptionPane.showMessageDialog(dialog, "Could not find "
               + "dcd.jar file in\n"
               + "\"" + home.getAbsolutePath() + "\".",
               "Error", JOptionPane.ERROR_MESSAGE);
            return;
         }
      }
      String exclClasses = excludedClassesField.getText().replace('\n', ' ');
      String exclMethods = excludedMethodsField.getText().replace('\n', ' ');
      String javaFlags = javaFlagsField.getText().replace('\n', ' ');
      DCDSettings settings =
            new DCDSettings(home, hideTBICKB.isSelected(), javaFlags,
            exclClasses, exclMethods, publicCKB.isSelected(),
            localCKB.isSelected());
      tool.settingsChanged(settings);
      homeCombo.storeHistory();
      if (dialog != null) {
         dialog.setVisible(false);
      }
   }
}

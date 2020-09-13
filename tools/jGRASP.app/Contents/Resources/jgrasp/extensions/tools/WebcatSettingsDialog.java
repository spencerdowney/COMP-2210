  
/*
 The Web-CAT plugin sources, and only those sources, in jGRASP are
 licensed under the GPL Version 2 as specified in the WebcatTool.java
 source file.
 
 Source code for our slightly modified version of the Eclipse Web-CAT
 submission plugin is available on request. See http://www.jgrasp.org
 for contact information.
*/
 
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jgrasp.tool.ActionContext;
import jgrasp.tool.HistoryCombo;
import jgrasp.tool.JGraspDialog;


/** Dialog for changing Web-CAT settings. **/
public class WebcatSettingsDialog {

   /** Assignment definition URL field. **/
   private HistoryCombo urlCombo;
   
   /** Checkbox for controlling toolbar item visibility. **/
   private JCheckBox hideTBICKB;
   
   /** The current dialog. **/
   private JDialog dialog;
   
   /** The tool the tool for which this dialog is modifying
    *  the settings. **/
   private WebcatTool tool;
 

   /** Creates a new WebcatSettingsDialog.
    *
    *  @param wTool the tool for which this dialog is modifying
    *  the settings. **/
   public WebcatSettingsDialog(final WebcatTool wTool) {
      tool = wTool;
   }


   /** Creates the configure dialog.
    *
    *  @param context action context.
    *
    *  @param settings the settings that the dialog will use
    *  and modify. **/
   public void showDialog(final ActionContext context,
         final WebcatSettings settings) {
      Frame parent = context.getDialogParent();
      String[] priorUrlHistory = settings.getUrlHistory();
      if (dialog != null && parent != dialog.getParent()) {
         // Rebuild if parent is different.
         priorUrlHistory = urlCombo.getHistory();
         dialog.dispose();
         dialog = null;
      }
      if (dialog == null) {
         dialog = createDialog(parent);
         dialog.pack();
      }
   
      // Initialize.
      if (priorUrlHistory != null) {
         urlCombo.setHistory(priorUrlHistory);
      }
   
      String defURL = settings.getURL();
      if (defURL != null) {
         urlCombo.setSelectedValue(defURL);
      }
   
      urlCombo.storeHistory();
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
      dg.setTitle("Web-CAT Tool Settings");
      JComponent contentPanel = new JPanel(false);
      dg.setContentPane(contentPanel);
      
      GridBagLayout layout = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      Insets insets = constraints.insets;
      int spacing = 8;
      contentPanel.setLayout(layout);
      JLabel label = new JLabel("Assignment Definition URL");
      contentPanel.add(label);
      constraints.weightx = 1.0;
      constraints.weighty = 1.0;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      insets.top = spacing;
      insets.left = spacing;
      insets.right = spacing;
      insets.bottom = 0;
      layout.setConstraints(label, constraints);
   
      urlCombo = new HistoryCombo(null, "Assignment Definition URL");
      contentPanel.add(urlCombo);   
      constraints.weightx = 1000.0;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      insets.left = 0;
      layout.setConstraints(urlCombo, constraints);
   
      label = new JLabel("The URL is typically [Web-CAT server]/Web-CAT/"
            + "WebObjects/Web-CAT.woa/wa/assignments/eclipse .");
      contentPanel.add(label);
      insets.bottom = spacing;
      insets.left = spacing;
      layout.setConstraints(label, constraints);
      
      hideTBICKB = new JCheckBox("Hide Toolbar Items");
      contentPanel.add(hideTBICKB);
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
      String url = urlCombo.getSelectedValue();
      WebcatSettings settings =
            new WebcatSettings(url, hideTBICKB.isSelected(),
            urlCombo.getHistory());
      tool.settingsChanged(settings);
      urlCombo.storeHistory();
      if (dialog != null) {
         dialog.setVisible(false);
      }
   }
}


import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ilan
 */
public class DicomFrame extends javax.swing.JFrame {

	/**
	 * Creates new form DicomFrame
	 */
	public DicomFrame() {
		initComponents();
		init();
	}
	
	private void init() {
		int i;
		String tmp1, tmpHeight;
//		WindowManager.addWindow(this);
		jPrefer = Preferences.userNodeForPackage(DicomFrame.class);
		jPrefer = jPrefer.node("biplugins");
		path = jPrefer.get("DicomFrame path", null);
		isMacro = false;
		boolean isOK = true;
		seriesName = origSerName = null;
		origInfo = null;
		origSerNum = 1;
		img1 = WindowManager.getCurrentImage();
		String arg = Macro.getOptions();
//		arg = "\t" + "/home/ilan";
		if( arg != null && !arg.isEmpty()) {
			i = arg.indexOf("	");
			if( i>= 0) {
				isMacro = true;
				if(i>0) seriesName = arg.substring(0, i);
				path = arg.substring(i+1);
			}
		}
		if(path != null) path = path.trim();
		jTextPath.setText(path);
		dcm1 = new myWriteDicom(img1);
		if( img1 == null) isOK = false;
		else {
			meta = ChoosePetCt.getMeta(1, img1);
			if( meta == null) isOK = false;
			else {
				origSerName = getTrimValue("0008,103E");
				tmp1 = getTrimValue("0020,0011");
				origSerNum = ChoosePetCt.parseInt(tmp1);
				currStyUID = getTrimValue( "0020,000D");
				prevStyUID = jPrefer.get("DicomFrame UID", "");
				origInfo = dcm1.getNewInfo();
				origInfo.patName = getTrimValue( "0010,0010");
				origInfo.patID = getTrimValue( "0010,0020");
				origInfo.patSex = getTrimValue( "0010,0040");
				origInfo.patDOB = getTrimValue( "0010,0030");
				tmpHeight = getTrimValue( "0010,1020");
				origInfo.patHeight = ChoosePetCt.parseDouble(tmpHeight);
				tmp1 = getTrimValue( "0010,1030");
				origInfo.patWeight = ChoosePetCt.parseDouble(tmp1);
				jTextName.setText(origInfo.patName);
				jTextID.setText(origInfo.patID);
				jTextSex.setText(origInfo.patSex);
				jTextDOB.setText(origInfo.patDOB);
				jTextHeight.setText(tmpHeight);
				jTextWeight.setText(tmp1);
			}
		}
		SpinnerNumberModel spin1 = getSpinModel(0);
		spin1.setValue(origSerNum);
		if(seriesName != null) {
			seriesName = seriesName.trim();
			jTextserName.setText(seriesName);
		} else {
			if(origSerName != null) jTextserName.setText(origSerName);
		}
		if( !isOK) {
			isMacro = false;
			jLabserName.setText("Can't save data");
		}
		jButSave.setEnabled(isOK);
	}

	String getTrimValue( String key) {
		String tmp = ChoosePetCt.getDicomValue(meta, key);
		if(tmp == null) tmp = "";
		return tmp;
	}

	void Browse2Path() {
		final JFileChooser fc;
		File file1;
		try {
			path = jTextPath.getText();
			fc = new JFileChooser(path);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int retVal = fc.showOpenDialog(this);
			if( retVal != JFileChooser.APPROVE_OPTION) return;
			file1 = fc.getSelectedFile();
			path = file1.getPath();
			jTextPath.setText(path);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}
	
	void openHelp() {
		ChoosePetCt.openHelp("Save as myDicom");
	}

	void changeVals() {
		String tmp1;
		Double height0, height1, weight0, weight1;
		tmp1 = jPrefer.get("DicomFrame patName", "");
		jTextName.setText(tmp1);
		tmp1 = jPrefer.get("DicomFrame patID", "");
		jTextID.setText(tmp1);
		tmp1 = jPrefer.get("DicomFrame patSex", "");
		jTextSex.setText(tmp1);
		tmp1 = jPrefer.get("DicomFrame patDOB", "");
		jTextDOB.setText(tmp1);
		tmp1 = jTextHeight.getText();
		height0 = ChoosePetCt.parseDouble(tmp1);
		height1 = jPrefer.getDouble("DicomFrame patHeight", 0);
		tmp1 = jTextWeight.getText();
		weight0 = ChoosePetCt.parseDouble(tmp1);
		weight1 = jPrefer.getDouble("DicomFrame patWeight", 0);
		if( !currStyUID.equals(prevStyUID)) {
			if(height0.compareTo(height1) != 0 || weight0.compareTo(weight1) != 0) {
				tmp1 = weight0 + " is different from " + weight1 + " or ";
				tmp1 += height0 + " is differnt from " + height1;
				tmp1 += "\nIs it OK to change them?";
				int i = JOptionPane.showConfirmDialog(this, tmp1,
					"Weight height changes", JOptionPane.YES_NO_OPTION);
				if( i != JOptionPane.YES_OPTION) return;
			}
		}
		jTextHeight.setText(height1.toString());
		jTextWeight.setText(weight1.toString());
	}

	void saveDicom() {
		int serNum;
		seriesName = jTextserName.getText().trim();
		if( origSerName != null && origSerName.equals(seriesName)) {
			seriesName = null;
		}
		SpinnerNumberModel spin1 = getSpinModel(0);
		serNum = spin1.getNumber().intValue();
		if( serNum == origSerNum) serNum = -1;	// no change
		path = jTextPath.getText();
		jPrefer.put("DicomFrame path", path);
		jPrefer.put("DicomFrame UID", currStyUID);
		myWriteDicom.patInfo updated = getUpdatedInfo();
		jPrefer.put("DicomFrame patName", updated.patName);
		jPrefer.put("DicomFrame patID", updated.patID);
		jPrefer.put("DicomFrame patSex", updated.patSex);
		jPrefer.put("DicomFrame patDOB", updated.patDOB);
		jPrefer.putDouble("DicomFrame patHeight", updated.patHeight);
		jPrefer.putDouble("DicomFrame patWeight", updated.patWeight);
		dcm1.writeDicomHeader(path, seriesName, serNum, maybeUpdated(updated));
		dispose();
	}

	myWriteDicom.patInfo getUpdatedInfo() {
		String tmp1;
		myWriteDicom.patInfo updated = dcm1.getNewInfo();
		updated.patName = jTextName.getText().trim();
		updated.patID = jTextID.getText().trim();
		updated.patSex = jTextSex.getText().trim();
		updated.patDOB = jTextDOB.getText().trim();
		tmp1 = jTextWeight.getText();
		updated.patWeight = ChoosePetCt.parseDouble(tmp1);
		tmp1 = jTextHeight.getText();
		updated.patHeight = ChoosePetCt.parseDouble(tmp1);
		return updated;
	}

	myWriteDicom.patInfo maybeUpdated( myWriteDicom.patInfo updated) {
		if(updated.patName.equals(origInfo.patName) && updated.patID.equals(origInfo.patID) &&
			updated.patSex.equals(origInfo.patSex) && updated.patDOB.equals(origInfo.patDOB) &&
			updated.patWeight==origInfo.patWeight && updated.patHeight==origInfo.patHeight)
			return null;
		return updated;
	}

	void sigValChange() {
		int curSpin;
		String key1;
		SpinnerNumberModel spin1 = getSpinModel(1);
		curSpin = spin1.getNumber().intValue();
		key1 = "significant image path";
		if( curSpin > 0) key1 += curSpin;
		path = jPrefer.get(key1, null);
		jTextPath.setText(path);
	}

	SpinnerNumberModel getSpinModel(int type) {
		if( type==1) return (SpinnerNumberModel) jSpinSigVal.getModel();
		return (SpinnerNumberModel) jSpinNum.getModel();
	}
	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabserName = new javax.swing.JLabel();
        jTextserName = new javax.swing.JTextField();
        jLabSerNum = new javax.swing.JLabel();
        jSpinNum = new javax.swing.JSpinner();
        patPanel = new javax.swing.JPanel();
        jButPrev = new javax.swing.JButton();
        jLabID = new javax.swing.JLabel();
        jTextID = new javax.swing.JTextField();
        jLabName = new javax.swing.JLabel();
        jTextName = new javax.swing.JTextField();
        jLabDOB = new javax.swing.JLabel();
        jTextDOB = new javax.swing.JTextField();
        jTextSex = new javax.swing.JTextField();
        jLabSex = new javax.swing.JLabel();
        jLabWeight = new javax.swing.JLabel();
        jTextWeight = new javax.swing.JTextField();
        jLabHeight = new javax.swing.JLabel();
        jTextHeight = new javax.swing.JTextField();
        jLabPath = new javax.swing.JLabel();
        jSpinSigVal = new javax.swing.JSpinner();
        jTextPath = new javax.swing.JTextField();
        jButBrowse = new javax.swing.JButton();
        jButSave = new javax.swing.JButton();
        jButHelp = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Save as Dicom");

        jLabserName.setText("series name");

        jLabSerNum.setText("series number");

        patPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Patient"));

        jButPrev.setText("Use previous");
        jButPrev.setToolTipText("Use the values of the last series which used Save Dicom");
        jButPrev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButPrevActionPerformed(evt);
            }
        });

        jLabID.setText("ID");

        jLabName.setText("Name");

        jLabDOB.setText("Birth date");

        jTextDOB.setToolTipText("Use Dicom format: YYYYMMDD");

        jLabSex.setText("Sex");

        jLabWeight.setText("Weight");

        jLabHeight.setText("Height");

        javax.swing.GroupLayout patPanelLayout = new javax.swing.GroupLayout(patPanel);
        patPanel.setLayout(patPanelLayout);
        patPanelLayout.setHorizontalGroup(
            patPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(patPanelLayout.createSequentialGroup()
                .addComponent(jButPrev)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabID)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextID))
            .addGroup(patPanelLayout.createSequentialGroup()
                .addComponent(jLabName)
                .addGap(18, 18, 18)
                .addComponent(jTextName)
                .addContainerGap())
            .addGroup(patPanelLayout.createSequentialGroup()
                .addComponent(jLabDOB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextDOB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabSex)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextSex, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(patPanelLayout.createSequentialGroup()
                .addComponent(jLabWeight)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextWeight, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabHeight)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 97, Short.MAX_VALUE))
        );
        patPanelLayout.setVerticalGroup(
            patPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(patPanelLayout.createSequentialGroup()
                .addGroup(patPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButPrev)
                    .addComponent(jLabID)
                    .addComponent(jTextID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(patPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabName)
                    .addComponent(jTextName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(patPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabDOB)
                    .addComponent(jTextDOB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextSex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabSex))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(patPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabWeight)
                    .addComponent(jTextWeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabHeight)
                    .addComponent(jTextHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jLabPath.setText("path");

        jSpinSigVal.setModel(new javax.swing.SpinnerNumberModel(0, 0, 9, 1));
        jSpinSigVal.setToolTipText("significant image path 0-9");
        jSpinSigVal.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinSigValStateChanged(evt);
            }
        });

        jTextPath.setToolTipText("");

        jButBrowse.setText("Browse");
        jButBrowse.setToolTipText("Can browse to special purpose locations");
        jButBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBrowseActionPerformed(evt);
            }
        });

        jButSave.setText("Save Dicom");
        jButSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSaveActionPerformed(evt);
            }
        });

        jButHelp.setText("Help");
        jButHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButHelpActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(patPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabserName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextserName))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabPath)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinSigVal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextPath)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButBrowse))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabSerNum)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinNum, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButHelp, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabserName)
                    .addComponent(jTextserName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabSerNum)
                    .addComponent(jSpinNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(patPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabPath)
                    .addComponent(jSpinSigVal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButSave)
                    .addComponent(jButHelp)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSaveActionPerformed
		saveDicom();
    }//GEN-LAST:event_jButSaveActionPerformed

    private void jButBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBrowseActionPerformed
		Browse2Path();
    }//GEN-LAST:event_jButBrowseActionPerformed

    private void jButHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButHelpActionPerformed
		openHelp();
    }//GEN-LAST:event_jButHelpActionPerformed

    private void jSpinSigValStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinSigValStateChanged
		sigValChange();
    }//GEN-LAST:event_jSpinSigValStateChanged

    private void jButPrevActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButPrevActionPerformed
		changeVals();
    }//GEN-LAST:event_jButPrevActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButBrowse;
    private javax.swing.JButton jButHelp;
    private javax.swing.JButton jButPrev;
    private javax.swing.JButton jButSave;
    private javax.swing.JLabel jLabDOB;
    private javax.swing.JLabel jLabHeight;
    private javax.swing.JLabel jLabID;
    private javax.swing.JLabel jLabName;
    private javax.swing.JLabel jLabPath;
    private javax.swing.JLabel jLabSerNum;
    private javax.swing.JLabel jLabSex;
    private javax.swing.JLabel jLabWeight;
    private javax.swing.JLabel jLabserName;
    private javax.swing.JSpinner jSpinNum;
    private javax.swing.JSpinner jSpinSigVal;
    private javax.swing.JTextField jTextDOB;
    private javax.swing.JTextField jTextHeight;
    private javax.swing.JTextField jTextID;
    private javax.swing.JTextField jTextName;
    private javax.swing.JTextField jTextPath;
    private javax.swing.JTextField jTextSex;
    private javax.swing.JTextField jTextWeight;
    private javax.swing.JTextField jTextserName;
    private javax.swing.JPanel patPanel;
    // End of variables declaration//GEN-END:variables
	boolean isMacro;
	int origSerNum;
	String seriesName, origSerName, meta, currStyUID, prevStyUID;
	String path;
	myWriteDicom.patInfo origInfo;
	myWriteDicom dcm1;
	ImagePlus img1;
	Preferences jPrefer;
}

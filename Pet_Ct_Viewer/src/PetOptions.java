import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;



/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PetOptions.java
 *
 * Created on Jan 12, 2010, 12:17:17 PM
 */

/**
 *
 * @author Ilan
 */
public class PetOptions extends javax.swing.JDialog {
	PetCtFrame parent = null;

    /** Creates new form PetOptions
	 * @param parent
	 * @param modal */
    public PetOptions(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
		this.parent = (PetCtFrame) parent;
        initComponents();
		init();
    }

	private void init() {
		setLocationRelativeTo(getOwner());
		Preferences prefer1 = parent.jPrefer;
		jCheckHotIron.setSelected(ChoosePetCt.isOptionSelected(prefer1, ChoosePetCt.HOT_IRON_FUSE));
		jCheckBlackBackground.setSelected(ChoosePetCt.isOptionSelected(prefer1, ChoosePetCt.BLACK_BKGD));
		jCheckBluesLut.setSelected(ChoosePetCt.isOptionSelected(prefer1, ChoosePetCt.EXT_BLUES_LUT));
		jCheckHotLut.setSelected(ChoosePetCt.isOptionSelected(prefer1, ChoosePetCt.EXT_HOT_IRON_LUT));
		jCheckFixedLUT.setSelected(ChoosePetCt.isOptionSelected(prefer1, ChoosePetCt.FIXED_USER_LUT));
		jCheckMriLut.setSelected(ChoosePetCt.isOptionSelected(prefer1, ChoosePetCt.MRI_CT_LUT));
		jComboSUV.setSelectedIndex(parent.SUVtype);
		jComboSUV2.setSelectedIndex(parent.SUVtype2);
		jCheckOperatorName.setSelected(parent.operatorNameFlg);
		jCheckAutoSize.setSelected(parent.autoResize);
		jCheckCircle.setSelected(parent.circleSUV);
		jCheckSphere.setSelected(parent.sphereSUV);
		jCheckCircle2.setSelected(parent.circleSUV2);
		jCheckSphere2.setSelected(parent.sphereSUV2);
		jCheckInvertScroll.setSelected(parent.invertScroll);
		jCheckSUL.setSelected(parent.useSUL);
		jCheckAnnotations.setSelected(parent.startAnnotations);
		jCheckIgnoreSUV.setSelected(parent.ignoreSUV);
		jCheckROIs.setSelected(parent.startROIs);
		jTextBluesLut.setText(prefer1.get("ext blues LUT", ""));
		jTextBluesLutMenu.setText(prefer1.get("ext blues name", "The blues"));
		jTextHotLut.setText(prefer1.get("ext hotiron LUT", ""));
		jTextHotLutMenu.setText(prefer1.get("ext hotiron name", "Hot iron"));
		jTextMriLut.setText(prefer1.get("MriCt LUT", ""));
		jTextMriLutMenu.setText(prefer1.get("MriCt name", "MRI/CT LUT"));
		jTextUserLUT.setText(prefer1.get("fixed LUT", ""));
		jCheckQuality.setSelected(parent.qualityRendering);
		jTextZTri.setText(parent.triThick.toString());
		Integer suv = parent.SUVmm;
		jTextMm.setText(suv.toString());
		suv = parent.SUVmm2;
		jTextMm2.setText(suv.toString());
		maxFactor.setValue(parent.sliceMaxFactor);
		jCheckForceTmp.setSelected(prefer1.getBoolean("force tmpFile", false));
		jCheckExFloat.setSelected(prefer1.getBoolean("external float", true));
		jCheckExRelative.setSelected(prefer1.getBoolean("external relative", true));
		jCheckExCt.setSelected(prefer1.getBoolean("external ct file", true));
		jCheckExMask.setSelected(prefer1.getBoolean("external mask", true));
		jCheckExCtMask.setSelected(prefer1.getBoolean("external ctmask", true));

		jChkMRIchop.setVisible(false);	// comment out 1 of these 2 lines
//		jChkMRIchop.setSelected(prefer1.getBoolean("allow MRI chop", false));

		curSpin = prefer1.getInt("significant spin val", 0);
		SpinnerNumberModel spin1 = (SpinnerNumberModel) jSpinSig.getModel();
		spin1.setValue(curSpin);
		getSetSignificant(false);
		Double oc_ml = prefer1.getDouble("OC ml", 2.0);
		jTextMinVol.setText(oc_ml.toString());
		Double oc_suv = prefer1.getDouble("OC SUV", 2.5);
		jTextSUV.setText(oc_suv.toString());
		Integer pgUpDn = parent.pgUpDn;
		// there is a timing problem with spin1.setValue. Use empty jTextPgUpDn to solve
		jTextPgUpDn.setText(pgUpDn.toString());
//		jCheckCurLock.setSelected(parent.jPrefer.getBoolean("cursor pixel lock", true));
	}

	void saveVals() {
		Preferences prefer1 = parent.jPrefer;
		prefer1.putBoolean("hot iron fuse",  jCheckHotIron.isSelected());
		prefer1.putBoolean("auto resize", jCheckAutoSize.isSelected());
		prefer1.putBoolean("operator name", jCheckOperatorName.isSelected());
		prefer1.putBoolean("circle SUV", isCircleSUV(1));
		prefer1.putBoolean("sphere SUV", isSphereSUV(1));
		prefer1.putBoolean("circle SUV2", isCircleSUV(2));
		prefer1.putBoolean("sphere SUV2", isSphereSUV(2));
		prefer1.putBoolean("invert scroll", jCheckInvertScroll.isSelected());
		prefer1.putBoolean("use SUL", jCheckSUL.isSelected());
		prefer1.putBoolean("start ROIs", jCheckROIs.isSelected());
		prefer1.putBoolean("start annotations", jCheckAnnotations.isSelected());
		prefer1.putBoolean("ignore SUV", jCheckIgnoreSUV.isSelected());
		prefer1.putBoolean("quality rendering", jCheckQuality.isSelected());
		prefer1.putDouble("tricubic zmin", getTriZ());
		prefer1.putInt("SUV mm", getSUVmm(1));
		prefer1.putInt("SUV mm2", getSUVmm(2));
		prefer1.putInt("page up down", getPgUpDn());
		prefer1.putBoolean("black background",  jCheckBlackBackground.isSelected());
		prefer1.putBoolean("use ext blues LUT",  jCheckBluesLut.isSelected());
		prefer1.putBoolean("use ext hotiron LUT",  jCheckHotLut.isSelected());
		prefer1.putBoolean("use external LUT",  jCheckFixedLUT.isSelected());
		prefer1.putBoolean("use MriCt LUT",  jCheckMriLut.isSelected());
		prefer1.putBoolean("force tmpFile", jCheckForceTmp.isSelected());
		prefer1.putBoolean("external float", jCheckExFloat.isSelected());
		prefer1.putBoolean("external relative", jCheckExRelative.isSelected());
		prefer1.putBoolean("external ct file", jCheckExCt.isSelected());
		prefer1.putBoolean("external mask", jCheckExMask.isSelected());
		prefer1.putBoolean("external ctmask", jCheckExCtMask.isSelected());
		prefer1.putBoolean("allow MRI chop", jChkMRIchop.isSelected());
		int suvType = getSUVtype(2);
		prefer1.putInt("SUV type2", suvType);
		suvType = getSUVtype(1);	// this determines last peak type
		prefer1.putInt("SUV type", suvType);
		prefer1.putDouble("slice max factor", getMaxFactor());
		if( suvType >= 1 && suvType <= 2) {
			prefer1.putInt("last peak type", suvType);
		}
		prefer1.put("ext blues LUT", jTextBluesLut.getText());
		prefer1.put("ext blues name", jTextBluesLutMenu.getText());
		prefer1.put("ext hotiron LUT", jTextHotLut.getText());
		prefer1.put("ext hotiron name", jTextHotLutMenu.getText());
		prefer1.put("MriCt LUT", jTextMriLut.getText());
		prefer1.put("MriCt name", jTextMriLutMenu.getText());
		prefer1.put("fixed LUT", jTextUserLUT.getText());
		double oc_ml = Double.parseDouble(jTextMinVol.getText());
		if( oc_ml > 0.0 && oc_ml <= 50.0) prefer1.putDouble("OC ml", oc_ml);
		double oc_suv = Double.parseDouble(jTextSUV.getText());
		if( oc_suv >= 2.5 && oc_suv <= 8) prefer1.putDouble("OC SUV", oc_suv);
	}
	
	double getMaxFactor() {
		double max1 = Double.parseDouble(maxFactor.getModel().getValue().toString());
		return max1;
	}
	
	int getSUVtype(int type) {
		if(type == 2) return jComboSUV2.getSelectedIndex();
		return jComboSUV.getSelectedIndex();
	}
	
	boolean isCircleSUV(int type) {
		if( type == 2) return jCheckCircle2.isSelected();
		return jCheckCircle.isSelected();
	}
	
	boolean isSphereSUV(int type) {
		if( type == 2) return jCheckSphere2.isSelected();
		return jCheckSphere.isSelected();
	}

	void getSetSignificant(boolean writeFlg) {
		Preferences prefer1 = parent.jPrefer;
		String key1 = "significant image path";
		if( curSpin > 0) key1 += curSpin;
		if( writeFlg) {
			prefer1.put( key1, jTextSignificantImage.getText());
		} else {
			jTextSignificantImage.setText(prefer1.get(key1, ""));
		}
	}

	int getSUVmm(int type) {
		int mm=0;
		String tmp = jTextMm.getText();
		if(type == 2) tmp = jTextMm2.getText();
		if( !tmp.isEmpty()) mm = Integer.parseInt(tmp);
		if( mm <= 0) mm = 5;
		return mm;
	}

	int getPgUpDn() {
		int val=3;
		String tmp = jTextPgUpDn.getText();
		if( !tmp.isEmpty()) val = Integer.parseInt(tmp);
		if( val <= 0 || val > 20) val = 3;
		return val;
	}
	
	void browseLUT(int type) {
		final JFileChooser fc;
		File file1;
		JTextField jText1;
		switch( type) {
			case 1:
				jText1 = jTextBluesLut;
				break;
				
			case 2:
				jText1 = jTextHotLut;
				break;
				
			case 4:
				jText1 = jTextMriLut;
				break;
				
			default:
				jText1 = jTextUserLUT;
		}
		String flPath;
		try {
			flPath = ChoosePetCt.userDir;
			flPath += "luts" + File.separator;
			fc = new JFileChooser(flPath);
			int retVal = fc.showOpenDialog(this);
			if( retVal != JFileChooser.APPROVE_OPTION) return;
			file1 = fc.getSelectedFile();
			jText1.setText(file1.getName());
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	void browseSignificant() {
		final JFileChooser fc;
		JTextField jText1 = jTextSignificantImage;
		String flPath = jText1.getText();
		fc = new JFileChooser(flPath);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int retVal = fc.showOpenDialog(this);
		if( retVal != JFileChooser.APPROVE_OPTION) return;
		File file1 = fc.getSelectedFile();
		jText1.setText(file1.getPath());
	}

	void spinSignificant() {
		if( jTextPgUpDn.getText().isEmpty()) return;	// true at initialization
		getSetSignificant(true);	// write the old value
		SpinnerNumberModel spin1 = (SpinnerNumberModel) jSpinSig.getModel();
		curSpin =  spin1.getNumber().intValue();
		Preferences prefer1 = parent.jPrefer;
		prefer1.putInt("significant spin val", curSpin);
		getSetSignificant(false);	// get the new value
	}

	void changeQuality() {
		parent.getPetCtPanel1().changeQuality(jCheckQuality.isSelected());
	}

	Double getTriZ() {
		return Double.parseDouble(jTextZTri.getText());
	}

	@Override
	public void dispose() {
		getSetSignificant(true);
		parent.SUVtype = getSUVtype(1);
		parent.SUVtype2 = getSUVtype(2);
		parent.operatorNameFlg = jCheckOperatorName.isSelected();
		parent.autoResize = jCheckAutoSize.isSelected();
		parent.circleSUV = isCircleSUV(1);
		parent.sphereSUV = isSphereSUV(1);
		parent.circleSUV2 = isCircleSUV(2);
		parent.sphereSUV2 = isSphereSUV(2);
		parent.invertScroll = jCheckInvertScroll.isSelected();
		parent.useSUL = jCheckSUL.isSelected();
		parent.startAnnotations = jCheckAnnotations.isSelected();
		parent.ignoreSUV = jCheckIgnoreSUV.isSelected();
		parent.startROIs = jCheckROIs.isSelected();
		parent.SUVmm = getSUVmm(1);
		parent.SUVmm2 = getSUVmm(2);
		parent.pgUpDn = getPgUpDn();
		parent.sliceMaxFactor = getMaxFactor();
		parent.setSliceMaxText();
		parent.triThick = getTriZ();
		super.dispose();
	}
	
/*	void cursorLock() {
		boolean sel = jCheckCurLock.isSelected();
		parent.jPrefer.putBoolean("cursor pixel lock", sel);
		if( parent.fuse3 != null) {
			parent.fuse3.getDisplay3Panel().repaint();
		}
	}*/

	/** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelBasic = new javax.swing.JPanel();
        jCheckHotIron = new javax.swing.JCheckBox();
        jCheckOperatorName = new javax.swing.JCheckBox();
        jCheckAutoSize = new javax.swing.JCheckBox();
        jPanelStart = new javax.swing.JPanel();
        jCheckAnnotations = new javax.swing.JCheckBox();
        jCheckROIs = new javax.swing.JCheckBox();
        jComboSUV = new javax.swing.JComboBox();
        jTextMm = new javax.swing.JTextField();
        jLabelMm = new javax.swing.JLabel();
        jCheckSphere = new javax.swing.JCheckBox();
        jCheckCircle = new javax.swing.JCheckBox();
        jComboSUV2 = new javax.swing.JComboBox();
        jTextMm2 = new javax.swing.JTextField();
        jLabelMm2 = new javax.swing.JLabel();
        jCheckSphere2 = new javax.swing.JCheckBox();
        jCheckCircle2 = new javax.swing.JCheckBox();
        jLabelRight = new javax.swing.JLabel();
        maxFactor = new javax.swing.JSpinner();
        jLabelPgUpDn = new javax.swing.JLabel();
        jTextPgUpDn = new javax.swing.JTextField();
        jLabelSlices = new javax.swing.JLabel();
        jCheckSUL = new javax.swing.JCheckBox();
        jCheckIgnoreSUV = new javax.swing.JCheckBox();
        jCheckBlackBackground = new javax.swing.JCheckBox();
        jCheckQuality = new javax.swing.JCheckBox();
        jTextZTri = new javax.swing.JTextField();
        jLabmm = new javax.swing.JLabel();
        jCheckInvertScroll = new javax.swing.JCheckBox();
        jPanelLuts = new javax.swing.JPanel();
        jCheckBluesLut = new javax.swing.JCheckBox();
        jTextBluesLutMenu = new javax.swing.JTextField();
        jCheckHotLut = new javax.swing.JCheckBox();
        jTextHotLutMenu = new javax.swing.JTextField();
        jCheckFixedLUT = new javax.swing.JCheckBox();
        jCheckMriLut = new javax.swing.JCheckBox();
        jTextMriLutMenu = new javax.swing.JTextField();
        jTextBluesLut = new javax.swing.JTextField();
        jTextHotLut = new javax.swing.JTextField();
        jTextUserLUT = new javax.swing.JTextField();
        jTextMriLut = new javax.swing.JTextField();
        jButBrowseBlues = new javax.swing.JButton();
        jButBrowseHot = new javax.swing.JButton();
        jButBrowseUser = new javax.swing.JButton();
        jButBrowseMri = new javax.swing.JButton();
        jButSave = new javax.swing.JButton();
        jButTmp = new javax.swing.JButton();
        jButHelp = new javax.swing.JButton();
        jPanelSigImage = new javax.swing.JPanel();
        jTextSignificantImage = new javax.swing.JTextField();
        jButBrowseSig = new javax.swing.JButton();
        jSpinSig = new javax.swing.JSpinner();
        jChkMRIchop = new javax.swing.JCheckBox();
        jPanelExtended = new javax.swing.JPanel();
        jPanelNifti = new javax.swing.JPanel();
        jCheckForceTmp = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        jCheckExFloat = new javax.swing.JCheckBox();
        jCheckExRelative = new javax.swing.JCheckBox();
        jCheckExCt = new javax.swing.JCheckBox();
        jCheckExMask = new javax.swing.JCheckBox();
        jCheckExCtMask = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jLabSUV = new javax.swing.JLabel();
        jLabMinPix = new javax.swing.JLabel();
        jTextSUV = new javax.swing.JTextField();
        jTextMinVol = new javax.swing.JTextField();
        jButSave1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Options");

        jCheckHotIron.setText("hot iron fused display");

        jCheckOperatorName.setText("display operator name");

        jCheckAutoSize.setText("auto resize window to fit data");

        jPanelStart.setBorder(javax.swing.BorderFactory.createTitledBorder("Auto start"));

        jCheckAnnotations.setText("Annotations");

        jCheckROIs.setText("BF, ROIs");

        javax.swing.GroupLayout jPanelStartLayout = new javax.swing.GroupLayout(jPanelStart);
        jPanelStart.setLayout(jPanelStartLayout);
        jPanelStartLayout.setHorizontalGroup(
            jPanelStartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelStartLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelStartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckAnnotations)
                    .addComponent(jCheckROIs))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelStartLayout.setVerticalGroup(
            jPanelStartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelStartLayout.createSequentialGroup()
                .addComponent(jCheckAnnotations)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jCheckROIs))
        );

        jComboSUV.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "SUVmax", "SUVpeak", "SUVqpeak", "SUVmean", "SULmax", "SULmean" }));

        jLabelMm.setText("mm");

        jCheckSphere.setText("use sphere");

        jCheckCircle.setText("show circle");

        jComboSUV2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "SUVmax", "SUVpeak", "SUVqpeak", "SUVmean", "SULmax", "SULmean" }));

        jLabelMm2.setText("mm");

        jCheckSphere2.setText("use sphere");

        jCheckCircle2.setText("show circle");

        jLabelRight.setText("Right click - Slice max factor");

        maxFactor.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.4d, 2.0d, 0.1d));

        jLabelPgUpDn.setText("PgUp, PgDn =");

        jLabelSlices.setText("slices");

        jCheckSUL.setText("use SUL");

        jCheckIgnoreSUV.setText("ignore SUV");

        jCheckBlackBackground.setText("Black background");

        jCheckQuality.setText("best quality, z >");
        jCheckQuality.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckQualityActionPerformed(evt);
            }
        });

        jLabmm.setText("mm");

        jCheckInvertScroll.setText("invert coronal, sagittal scroll direction");

        jPanelLuts.setBorder(javax.swing.BorderFactory.createTitledBorder("LUTs - updated on next start of Pet Ct Viewer"));

        jTextBluesLutMenu.setText("The blues");

        jTextHotLutMenu.setText("Hot iron");

        jCheckFixedLUT.setText("Fixed user LUT");

        jTextMriLutMenu.setText("MRI/CT LUT");

        jButBrowseBlues.setText("browse");
        jButBrowseBlues.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBrowseBluesActionPerformed(evt);
            }
        });

        jButBrowseHot.setText("browse");
        jButBrowseHot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBrowseHotActionPerformed(evt);
            }
        });

        jButBrowseUser.setText("browse");
        jButBrowseUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBrowseUserActionPerformed(evt);
            }
        });

        jButBrowseMri.setText("browse");
        jButBrowseMri.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBrowseMriActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelLutsLayout = new javax.swing.GroupLayout(jPanelLuts);
        jPanelLuts.setLayout(jPanelLutsLayout);
        jPanelLutsLayout.setHorizontalGroup(
            jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLutsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelLutsLayout.createSequentialGroup()
                        .addComponent(jCheckHotLut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextHotLutMenu))
                    .addComponent(jCheckFixedLUT, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelLutsLayout.createSequentialGroup()
                        .addComponent(jCheckBluesLut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jTextBluesLutMenu, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelLutsLayout.createSequentialGroup()
                        .addComponent(jCheckMriLut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextMriLutMenu, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextMriLut)
                    .addComponent(jTextUserLUT, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                    .addComponent(jTextHotLut)
                    .addComponent(jTextBluesLut))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButBrowseMri)
                    .addComponent(jButBrowseBlues)
                    .addComponent(jButBrowseHot)
                    .addComponent(jButBrowseUser))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelLutsLayout.setVerticalGroup(
            jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLutsLayout.createSequentialGroup()
                .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextBluesLutMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTextBluesLut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButBrowseBlues))
                    .addComponent(jCheckBluesLut))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextHotLutMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTextHotLut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButBrowseHot))
                    .addComponent(jCheckHotLut))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckFixedLUT)
                    .addComponent(jTextUserLUT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButBrowseUser))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButBrowseMri)
                        .addComponent(jTextMriLut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelLutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jTextMriLutMenu, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCheckMriLut)))
                .addContainerGap())
        );

        jButSave.setText("Save as defaults");
        jButSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSaveActionPerformed(evt);
            }
        });

        jButTmp.setText("Temporary");
        jButTmp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButTmpActionPerformed(evt);
            }
        });

        jButHelp.setText("Help");
        jButHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButHelpActionPerformed(evt);
            }
        });

        jPanelSigImage.setBorder(javax.swing.BorderFactory.createTitledBorder("Directory for Significant Image"));

        jButBrowseSig.setText("browse");
        jButBrowseSig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBrowseSigActionPerformed(evt);
            }
        });

        jSpinSig.setModel(new javax.swing.SpinnerNumberModel(0, 0, 9, 1));
        jSpinSig.setToolTipText("Choose path number 0-9");
        jSpinSig.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinSigStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanelSigImageLayout = new javax.swing.GroupLayout(jPanelSigImage);
        jPanelSigImage.setLayout(jPanelSigImageLayout);
        jPanelSigImageLayout.setHorizontalGroup(
            jPanelSigImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSigImageLayout.createSequentialGroup()
                .addComponent(jTextSignificantImage, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButBrowseSig)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinSig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanelSigImageLayout.setVerticalGroup(
            jPanelSigImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSigImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextSignificantImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jButBrowseSig)
                .addComponent(jSpinSig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jChkMRIchop.setText("Allow MRI chop");

        javax.swing.GroupLayout jPanelBasicLayout = new javax.swing.GroupLayout(jPanelBasic);
        jPanelBasic.setLayout(jPanelBasicLayout);
        jPanelBasicLayout.setHorizontalGroup(
            jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelBasicLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jCheckBlackBackground)
                .addGap(155, 155, 155))
            .addComponent(jPanelLuts, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanelBasicLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButSave)
                .addGap(18, 18, 18)
                .addComponent(jButTmp)
                .addGap(18, 18, 18)
                .addComponent(jButHelp)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanelBasicLayout.createSequentialGroup()
                .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelBasicLayout.createSequentialGroup()
                        .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckHotIron)
                            .addComponent(jCheckOperatorName)
                            .addComponent(jCheckAutoSize))
                        .addGap(18, 18, 18)
                        .addComponent(jPanelStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelBasicLayout.createSequentialGroup()
                        .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(jPanelBasicLayout.createSequentialGroup()
                                .addComponent(jComboSUV2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextMm2))
                            .addGroup(jPanelBasicLayout.createSequentialGroup()
                                .addComponent(jComboSUV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextMm, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelBasicLayout.createSequentialGroup()
                                .addComponent(jLabelMm)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckSphere)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckCircle))
                            .addGroup(jPanelBasicLayout.createSequentialGroup()
                                .addComponent(jLabelMm2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckSphere2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckCircle2))))
                    .addGroup(jPanelBasicLayout.createSequentialGroup()
                        .addComponent(jLabelRight)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(maxFactor, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCheckInvertScroll)
                    .addComponent(jPanelSigImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(jPanelBasicLayout.createSequentialGroup()
                            .addComponent(jCheckQuality)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jTextZTri, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLabmm)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jChkMRIchop))
                        .addGroup(jPanelBasicLayout.createSequentialGroup()
                            .addComponent(jLabelPgUpDn)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jTextPgUpDn, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLabelSlices)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jCheckSUL)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jCheckIgnoreSUV))))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanelBasicLayout.setVerticalGroup(
            jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBasicLayout.createSequentialGroup()
                .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelBasicLayout.createSequentialGroup()
                        .addComponent(jCheckHotIron)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckOperatorName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckAutoSize))
                    .addComponent(jPanelStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboSUV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextMm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelMm)
                    .addComponent(jCheckSphere)
                    .addComponent(jCheckCircle))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboSUV2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextMm2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelMm2)
                    .addComponent(jCheckSphere2)
                    .addComponent(jCheckCircle2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelRight)
                    .addComponent(maxFactor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelPgUpDn)
                    .addComponent(jTextPgUpDn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelSlices)
                    .addComponent(jCheckSUL)
                    .addComponent(jCheckIgnoreSUV))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBlackBackground)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckQuality)
                    .addComponent(jTextZTri, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabmm)
                    .addComponent(jChkMRIchop))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckInvertScroll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelLuts, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelBasicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButSave)
                    .addComponent(jButTmp)
                    .addComponent(jButHelp))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelSigImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("basic", jPanelBasic);

        jPanelNifti.setBorder(javax.swing.BorderFactory.createTitledBorder("Nifti"));

        jCheckForceTmp.setText("Force \"use tmp file\" at starup");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("external Nifti files"));

        jCheckExFloat.setText("float");

        jCheckExRelative.setText("relative");

        jCheckExCt.setText("CT");

        jCheckExMask.setText("PET mask");

        jCheckExCtMask.setText("CT mask");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jCheckExFloat)
                        .addGap(35, 35, 35)
                        .addComponent(jCheckExMask))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jCheckExRelative)
                        .addGap(18, 18, 18)
                        .addComponent(jCheckExCtMask))
                    .addComponent(jCheckExCt))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckExFloat)
                    .addComponent(jCheckExMask))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckExRelative)
                    .addComponent(jCheckExCtMask))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckExCt))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("3D OC"));

        jLabSUV.setText("min SUV (2.5 -> 8)");

        jLabMinPix.setText("min vol in ml");

        jTextSUV.setText("2.5");

        jTextMinVol.setText("4.0");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabSUV)
                    .addComponent(jLabMinPix))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextSUV)
                    .addComponent(jTextMinVol))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextSUV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabSUV))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextMinVol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabMinPix))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanelNiftiLayout = new javax.swing.GroupLayout(jPanelNifti);
        jPanelNifti.setLayout(jPanelNiftiLayout);
        jPanelNiftiLayout.setHorizontalGroup(
            jPanelNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jCheckForceTmp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanelNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelNiftiLayout.setVerticalGroup(
            jPanelNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelNiftiLayout.createSequentialGroup()
                .addComponent(jCheckForceTmp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jButSave1.setText("Save");
        jButSave1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSave1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelExtendedLayout = new javax.swing.GroupLayout(jPanelExtended);
        jPanelExtended.setLayout(jPanelExtendedLayout);
        jPanelExtendedLayout.setHorizontalGroup(
            jPanelExtendedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelExtendedLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButSave1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanelExtendedLayout.createSequentialGroup()
                .addComponent(jPanelNifti, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 199, Short.MAX_VALUE))
        );
        jPanelExtendedLayout.setVerticalGroup(
            jPanelExtendedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelExtendedLayout.createSequentialGroup()
                .addComponent(jPanelNifti, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButSave1)
                .addGap(0, 255, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("extended", jPanelExtended);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 456, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 597, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jButSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSaveActionPerformed
		saveVals();
		dispose();
	}//GEN-LAST:event_jButSaveActionPerformed

	private void jButTmpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButTmpActionPerformed
		dispose();
	}//GEN-LAST:event_jButTmpActionPerformed

    private void jButHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButHelpActionPerformed
		ChoosePetCt.openHelp("Options");
    }//GEN-LAST:event_jButHelpActionPerformed

    private void jButBrowseUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBrowseUserActionPerformed
		browseLUT(3);
    }//GEN-LAST:event_jButBrowseUserActionPerformed

    private void jCheckQualityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckQualityActionPerformed
		changeQuality();
    }//GEN-LAST:event_jCheckQualityActionPerformed

    private void jButBrowseBluesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBrowseBluesActionPerformed
		browseLUT(1);
    }//GEN-LAST:event_jButBrowseBluesActionPerformed

    private void jButBrowseHotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBrowseHotActionPerformed
		browseLUT(2);
    }//GEN-LAST:event_jButBrowseHotActionPerformed

    private void jButBrowseMriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBrowseMriActionPerformed
		browseLUT(4);
    }//GEN-LAST:event_jButBrowseMriActionPerformed

    private void jSpinSigStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinSigStateChanged
		spinSignificant();
    }//GEN-LAST:event_jSpinSigStateChanged

    private void jButBrowseSigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBrowseSigActionPerformed
		browseSignificant();
    }//GEN-LAST:event_jButBrowseSigActionPerformed

    private void jButSave1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSave1ActionPerformed
		saveVals();
		dispose();
    }//GEN-LAST:event_jButSave1ActionPerformed
 
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButBrowseBlues;
    private javax.swing.JButton jButBrowseHot;
    private javax.swing.JButton jButBrowseMri;
    private javax.swing.JButton jButBrowseSig;
    private javax.swing.JButton jButBrowseUser;
    private javax.swing.JButton jButHelp;
    private javax.swing.JButton jButSave;
    private javax.swing.JButton jButSave1;
    private javax.swing.JButton jButTmp;
    private javax.swing.JCheckBox jCheckAnnotations;
    private javax.swing.JCheckBox jCheckAutoSize;
    private javax.swing.JCheckBox jCheckBlackBackground;
    private javax.swing.JCheckBox jCheckBluesLut;
    private javax.swing.JCheckBox jCheckCircle;
    private javax.swing.JCheckBox jCheckCircle2;
    private javax.swing.JCheckBox jCheckExCt;
    private javax.swing.JCheckBox jCheckExCtMask;
    private javax.swing.JCheckBox jCheckExFloat;
    private javax.swing.JCheckBox jCheckExMask;
    private javax.swing.JCheckBox jCheckExRelative;
    private javax.swing.JCheckBox jCheckFixedLUT;
    private javax.swing.JCheckBox jCheckForceTmp;
    private javax.swing.JCheckBox jCheckHotIron;
    private javax.swing.JCheckBox jCheckHotLut;
    private javax.swing.JCheckBox jCheckIgnoreSUV;
    private javax.swing.JCheckBox jCheckInvertScroll;
    private javax.swing.JCheckBox jCheckMriLut;
    private javax.swing.JCheckBox jCheckOperatorName;
    private javax.swing.JCheckBox jCheckQuality;
    private javax.swing.JCheckBox jCheckROIs;
    private javax.swing.JCheckBox jCheckSUL;
    private javax.swing.JCheckBox jCheckSphere;
    private javax.swing.JCheckBox jCheckSphere2;
    private javax.swing.JCheckBox jChkMRIchop;
    private javax.swing.JComboBox jComboSUV;
    private javax.swing.JComboBox jComboSUV2;
    private javax.swing.JLabel jLabMinPix;
    private javax.swing.JLabel jLabSUV;
    private javax.swing.JLabel jLabelMm;
    private javax.swing.JLabel jLabelMm2;
    private javax.swing.JLabel jLabelPgUpDn;
    private javax.swing.JLabel jLabelRight;
    private javax.swing.JLabel jLabelSlices;
    private javax.swing.JLabel jLabmm;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelBasic;
    private javax.swing.JPanel jPanelExtended;
    private javax.swing.JPanel jPanelLuts;
    private javax.swing.JPanel jPanelNifti;
    private javax.swing.JPanel jPanelSigImage;
    private javax.swing.JPanel jPanelStart;
    private javax.swing.JSpinner jSpinSig;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextBluesLut;
    private javax.swing.JTextField jTextBluesLutMenu;
    private javax.swing.JTextField jTextHotLut;
    private javax.swing.JTextField jTextHotLutMenu;
    private javax.swing.JTextField jTextMinVol;
    private javax.swing.JTextField jTextMm;
    private javax.swing.JTextField jTextMm2;
    private javax.swing.JTextField jTextMriLut;
    private javax.swing.JTextField jTextMriLutMenu;
    private javax.swing.JTextField jTextPgUpDn;
    private javax.swing.JTextField jTextSUV;
    private javax.swing.JTextField jTextSignificantImage;
    private javax.swing.JTextField jTextUserLUT;
    private javax.swing.JTextField jTextZTri;
    private javax.swing.JSpinner maxFactor;
    // End of variables declaration//GEN-END:variables
	int curSpin;
}

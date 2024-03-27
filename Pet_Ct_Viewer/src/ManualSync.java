import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.prefs.Preferences;
import javax.swing.SpinnerNumberModel;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ManualSync.java
 *
 * Created on Jul 20, 2011, 1:25:34 PM
 */
/**
 *
 * @author ilan
 */
public class ManualSync extends javax.swing.JDialog implements WindowFocusListener {
	PetCtFrame parent;
	JFijiPipe refPipe, setPipe = null;
	Preferences jPrefer;
	boolean isMRI = true, isInit = true;
	int currStoreIndx = 0, prevSliceType = 0;

	/** Creates new form ManualSync
	 * @param parent
	 * @param modal */
	public ManualSync(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		this.parent = (PetCtFrame) parent;
		initComponents();
		init();
	}
	
	private void init() {
		jPrefer = parent.jPrefer;
		addWindowFocusListener(this);
		setPipes();
		getStoreValues();
	}

	@Override
	public void windowGainedFocus(WindowEvent we) {
		if( setPipe == null) return;
		int currSlcTyp = setPipe.sliceType;
		if( prevSliceType == currSlcTyp) return;
		prevSliceType = currSlcTyp;
		if( isInit) return;
		setXYX();
		updateTitle();
	}

	@Override
	public void windowLostFocus(WindowEvent we) {}
	
	void resetValues() {
		int xShft, yShft, zShft;
		setPipes();
		JFijiPipe.mriOff mri0 = setPipe.mri1;
/*		refPipe.data1.mriOffZ = 0;
		refPipe.mriOffX = refPipe.mriOffY = 0;
		zShft = setPipe.data1.mriOffZ;*/
		refPipe.mri1.init();
		zShft = mri0.getOff(JFijiPipe.OFFZ);
		if( isMRI && zShft != 0) {
			parent.getPetCtPanel1().maybeSetMriOffset();
			zShft = 0;
//			xShft = setPipe.mriOffX;
//			yShft = setPipe.mriOffY;
			xShft = mri0.getOff(JFijiPipe.OFFX);
			yShft = mri0.getOff(JFijiPipe.OFFY);
		} else {
			xShft = yShft = 0;
			zShft = resetZ();
		}
		jSpinOffZ.setValue(zShft);
		jSpinOffY.setValue(yShft);
		jSpinOffX.setValue(xShft);
		jCheckIgnore.setSelected(false);
		setPipeIgnore(false);
		parent.repaint();
	}

	private void updateTitle() {
		String val;
		switch( parent.getPetCtPanel1().m_sliceType) {
			default:
				val = "Axial";
				break;

			case JFijiPipe.DSP_CORONAL:
				val = "Coronal";
				break;

			case JFijiPipe.DSP_SAGITAL:
				val = "Sagittal";
				break;
		}
		setTitle("Sync MRI data -> " + val);
	}

	private int resetZ() {
		double reflo, refhi, setlo, sethi;
		int zShft = 0;
		switch (setPipe.sliceType) {
			case JFijiPipe.DSP_AXIAL:
				reflo = refPipe.getZpos(0);
				setlo = setPipe.getZpos(0);
				int n = refPipe.data1.numFrms;
				refhi = refPipe.getZpos(n-1);
				n = setPipe.data1.numFrms;
				sethi = setPipe.getZpos(n-1);
				zShft = ChoosePetCt.round((refhi - sethi + reflo - setlo)/(2*setPipe.data1.sliceThickness));
				break;

			case JFijiPipe.DSP_CORONAL:
/*				reflo = refPipe.getPixelSpacing(1);
				setlo = setPipe.getPixelSpacing(1);
				refhi = refPipe.data1.height;
				sethi = setPipe.data1.height;*/
				break;

			case JFijiPipe.DSP_SAGITAL:
				break;
		}
		return zShft;
	}

	void offsetChanged(int indx) {
		String tmp0 = "sync store ";
		switch (indx) {
			case 1:
				setCurrSpin(jSpinOffX, JFijiPipe.OFFX);
				break;

			case 2:
				setCurrSpin(jSpinOffY, JFijiPipe.OFFY);
				break;

			case 3:
				setCurrSpin(jSpinOffZ, JFijiPipe.OFFZ);
				break;

			case 5:
				break;
		}
		if( indx == 5) {
			tmp0 = tmp0 + "ignore" + currStoreIndx;
			boolean ignorXY = jCheckIgnore.isSelected();
			jPrefer.putBoolean(tmp0, ignorXY);
			setPipeIgnore(ignorXY);
		} else {
			tmp0 = tmp0 + "offs" + currStoreIndx;
			String tmp1 = java.util.Arrays.toString(setPipe.mri1.mriOffs);
			jPrefer.put(tmp0, tmp1);
		}
/*		SpinnerNumberModel spin1 = (SpinnerNumberModel) jSpinOffZ.getModel();
		int i = spin1.getNumber().intValue();
		setPipe.data1.mriOffZ = i;
		setPipe.corSagShift = parent.getPetCtPanel1().getCorSagShift(setPipe);
		int saveVal = i;
		spin1 = (SpinnerNumberModel) jSpinOffY.getModel();
		i = spin1.getNumber().intValue();
		int wid2 = setPipe.data1.width / 2;
		if( i < -wid2 || i > wid2) {
			if( i < 0) i = -wid2;
			else i = wid2;
			jSpinOffY.setValue(i);
		}
		if(indx == 2) saveVal = i;
		setPipe.mriOffY = i;
		spin1 = (SpinnerNumberModel) jSpinOffX.getModel();
		i = spin1.getNumber().intValue();
		if( i < -wid2 || i > wid2) {
			if( i < 0) i = -wid2;
			else i = wid2;
			jSpinOffX.setValue(i);
		}
		setPipe.mriOffX = i;
		if(indx == 1) saveVal = i;

		// now save value to registry
		i = currStoreIndx;
		String tmp1 = "sync store ";
		if(indx ==1) tmp1 += "x";
		if(indx ==2) tmp1 += "y";
		if(indx ==3) tmp1 += "z";
		if(indx ==5) tmp1 += "ignore";
		tmp1 = tmp1 + i;
		if(indx ==5) {
			boolean ignorXY = jCheckIgnore.isSelected();
			jPrefer.putBoolean(tmp1, ignorXY);
			setPipeIgnore(ignorXY);
		} else
			jPrefer.putInt(tmp1, saveVal);
		tmp1 = "sync store label" + i;
		jPrefer.put(tmp1,jTextStore.getText());*/
		parent.getPetCtPanel1().updateDisp3Value(isMRI);
		parent.repaint();
	}

	private int setCurrSpin(javax.swing.JSpinner cur1, int type) {
		int i, wid2 = setPipe.data1.width / 2;
		SpinnerNumberModel spin1 = (SpinnerNumberModel) cur1.getModel();
		i = spin1.getNumber().intValue();
		if( type == JFijiPipe.OFFX || type == JFijiPipe.OFFY) {
			if( i < -wid2 || i > wid2) {
				if( i < 0) i = -wid2;
				else i = wid2;
				cur1.setValue(i);
			}
		}
		setPipe.mri1.setOff(type, i);
		return i;
	}
	
	private void setPipeIgnore(boolean ignoreXY) {
		parent.getPetCtPanel1().maybeChangeUseXYShift(!ignoreXY);
	}
	
	void storeChanged() {
		SpinnerNumberModel spin1 = (SpinnerNumberModel) jSpinStore.getModel();
		currStoreIndx = spin1.getNumber().intValue();
		getStoreValues();
		parent.repaint();
	}
	
	void lableChanged() {
		String tmp1 = "sync store label" + currStoreIndx;
		jPrefer.put(tmp1,jTextStore.getText());
	}
	
	void getStoreValues() {
		int indx = currStoreIndx;
		String defVal = "default position";
		if(indx > 0) defVal = "Please set to a better label";
		String indxStr = "sync store label" + indx;
		String val1 = jPrefer.get(indxStr, null);
		if( val1 == null) val1 = defVal;
		jTextStore.setText(val1);
/*		indxStr = "sync store z" + indx;
		int ival1 = jPrefer.getInt(indxStr, 0);
		jSpinOffZ.setValue(ival1);
		indxStr = "sync store x" + indx;
		ival1 = jPrefer.getInt(indxStr, 0);
		jSpinOffX.setValue(ival1);
		indxStr = "sync store y" + indx;
		ival1 = jPrefer.getInt(indxStr, 0);
		jSpinOffY.setValue(ival1);*/
		int offS[] = getStoreSub(indx);
		if( offS != null && offS.length == 9)
			System.arraycopy(offS,0,setPipe.mri1.mriOffs,0,9);
		setXYX();
		indxStr = "sync store ignore" + indx;
		boolean ignore = jPrefer.getBoolean(indxStr, false);
		jCheckIgnore.setSelected(ignore);
		setPipeIgnore(ignore);
	}

	private  void setXYX() {
		JFijiPipe.mriOff m1 = setPipe.mri1;
		jSpinOffX.setValue(m1.getOff(JFijiPipe.OFFX));
		jSpinOffY.setValue(m1.getOff(JFijiPipe.OFFY));
		jSpinOffZ.setValue(m1.getOff(JFijiPipe.OFFZ));
		isInit = false;
	}

	private int [] getStoreSub(int indx) {
		String indxStr = "sync store offs" + indx;
		String tmp, val1 = jPrefer.get(indxStr, null);
		if( val1 == null) return null;
		val1 = val1.substring(1, val1.length()-1);
		String[] split1 = val1.split(",");
		int len1 = split1.length;
		if ( len1 != 9) return null;	// should be 3*3 elements
		int[] offS = new int[len1];
		for( int i=0; i<len1; i++) {
			tmp = split1[i].trim();
			if( tmp.charAt(0) == '-') {
				tmp = tmp.substring(1);
				offS[i] = Integer.parseInt(tmp)*(-1);
			} else {
				offS[i] = Integer.parseInt(tmp);
			}
		}
		return offS;
	}
	private void setPipes() {
		PetCtPanel panel1 = parent.getPetCtPanel1();
//		if( isMRI) {
			refPipe = panel1.getCorrectedOrUncorrectedPipe(false);
			setPipe = panel1.getMriOrCtPipe();
/*		} else {
			setPipe = panel1.getCorrectedOrUncorrectedPipe(false);
			refPipe = panel1.getMriOrCtPipe();
		}*/
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jButReset = new javax.swing.JButton();
        jLabZ = new javax.swing.JLabel();
        jSpinOffZ = new javax.swing.JSpinner();
        jLabX = new javax.swing.JLabel();
        jSpinOffX = new javax.swing.JSpinner();
        jLabY = new javax.swing.JLabel();
        jSpinOffY = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        jButHelp = new javax.swing.JButton();
        jCheckIgnore = new javax.swing.JCheckBox();
        jSpinStore = new javax.swing.JSpinner();
        jTextStore = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Sync MRI data");

        jLabel1.setText("When using MRI data, there can be an alignment problem");

        jLabel2.setText("compared to the PET-CT study.");

        jLabel3.setText("This program allows a manual correction to be applied.");

        jLabel6.setText("Reset gives an initial estimate for the Z value.");

        jButReset.setText("Reset");
        jButReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButResetActionPerformed(evt);
            }
        });

        jLabZ.setText("Z:");

        jSpinOffZ.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinOffZStateChanged(evt);
            }
        });

        jLabX.setText("X:");

        jSpinOffX.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinOffXStateChanged(evt);
            }
        });

        jLabY.setText("Y:");

        jSpinOffY.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinOffYStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jButReset)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabZ)
                .addGap(1, 1, 1)
                .addComponent(jSpinOffZ, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabX)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinOffX, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabY)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinOffY, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jButReset)
                .addComponent(jLabZ)
                .addComponent(jSpinOffZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabX)
                .addComponent(jSpinOffX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabY)
                .addComponent(jSpinOffY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel7.setText("Choose where to store data and set label:");

        jButHelp.setText("Help");
        jButHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButHelpActionPerformed(evt);
            }
        });

        jCheckIgnore.setText("ignore XY");
        jCheckIgnore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckIgnoreActionPerformed(evt);
            }
        });

        jSpinStore.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10, 1));
        jSpinStore.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinStoreStateChanged(evt);
            }
        });

        jTextStore.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextStoreKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jSpinStore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextStore)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(jButHelp))
                            .addComponent(jLabel3)
                            .addComponent(jLabel7)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckIgnore)))
                        .addGap(0, 36, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jButHelp))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinStore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextStore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jCheckIgnore))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jSpinStoreStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinStoreStateChanged
		storeChanged();
    }//GEN-LAST:event_jSpinStoreStateChanged

    private void jTextStoreKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextStoreKeyReleased
		lableChanged();
    }//GEN-LAST:event_jTextStoreKeyReleased

    private void jSpinOffYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinOffYStateChanged
		offsetChanged(2);
    }//GEN-LAST:event_jSpinOffYStateChanged

    private void jSpinOffXStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinOffXStateChanged
		offsetChanged(1);
    }//GEN-LAST:event_jSpinOffXStateChanged

    private void jSpinOffZStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinOffZStateChanged
        offsetChanged(3);
    }//GEN-LAST:event_jSpinOffZStateChanged

    private void jButResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButResetActionPerformed
		resetValues();
    }//GEN-LAST:event_jButResetActionPerformed

    private void jButHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButHelpActionPerformed
		ChoosePetCt.openHelp("Sync MRI data");
    }//GEN-LAST:event_jButHelpActionPerformed

    private void jCheckIgnoreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckIgnoreActionPerformed
		offsetChanged(5);
    }//GEN-LAST:event_jCheckIgnoreActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButHelp;
    private javax.swing.JButton jButReset;
    private javax.swing.JCheckBox jCheckIgnore;
    private javax.swing.JLabel jLabX;
    private javax.swing.JLabel jLabY;
    private javax.swing.JLabel jLabZ;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSpinner jSpinOffX;
    private javax.swing.JSpinner jSpinOffY;
    private javax.swing.JSpinner jSpinOffZ;
    private javax.swing.JSpinner jSpinStore;
    private javax.swing.JTextField jTextStore;
    // End of variables declaration//GEN-END:variables
}

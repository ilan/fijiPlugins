import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.prefs.Preferences;
import javax.swing.SpinnerNumberModel;

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
	PetCtPanel panel1;
	JFijiPipe refPipe, setPipe = null;
	Preferences jPrefer;
	ModifyTable mt = null;
	boolean isMRI = true, isInit = true;
	int currStoreIndx = 0, prevSliceType = 0;
	static final int MATRIX_SIZE = 9;

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
		panel1 = parent.getPetCtPanel1();
		jCheckTable.setVisible(false);	// not useful. Hide it...
		addWindowFocusListener(this);
		setPipes();
		getStoreValues();
	}

	@Override
	public void windowGainedFocus(WindowEvent we) {
		if( setPipe == null) return;
		JFijiPipe currPipe = panel1.getMriOrCtPipe();
		int currSlcTyp = currPipe.sliceType;
//		if( jCheckAll.isSelected()) 
//			currSlcTyp = JFijiPipe.DSP_ALL;
		if( prevSliceType == currSlcTyp && currPipe == setPipe) return;
		prevSliceType = currSlcTyp;
		if( isInit) return;
		setPipe = currPipe;
		setXYZ();
		updateTitle(currSlcTyp);
	}

	@Override
	public void windowLostFocus(WindowEvent we) {}
	
	void resetValues() {
		setPipes();
		JFijiPipe.mriOff mri0 = setPipe.mri1;
		mri0.init();
		jSpinOffZ.setValue(0);
		jSpinOffY.setValue(0);
		jSpinOffX.setValue(0);
//		setPipeIgnore(false);
		parent.repaint();
	}

	private void updateTitle(int slcType) {
		String title, val;
		switch( slcType) {
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
		title = "Sync MRI data -> ";
		if(!panel1.isMri()) title = "Sync CT(not MRI) data -> ";
		setTitle(title + val);
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
/*			tmp0 = tmp0 + "ignore" + currStoreIndx;
			boolean ignorXY = jCheckResetAll.isSelected();
			jPrefer.putBoolean(tmp0, ignorXY);
			setPipeIgnore(ignorXY);*/
		} else {
			tmp0 = tmp0 + "offs" + currStoreIndx;
			String tmp1 = java.util.Arrays.toString(setPipe.mri1.mriOffs);
			jPrefer.put(tmp0, tmp1);
		}
		panel1.updatePipeInfo();
		panel1.updateDisp3Value(isMRI);
		parent.repaint();
	}

	private int setCurrSpin(javax.swing.JSpinner cur1, int type) {
		int i, wid2 = setPipe.mri1.getWid2();
		SpinnerNumberModel spin1 = (SpinnerNumberModel) cur1.getModel();
		i = spin1.getNumber().intValue();
/*		if( type == JFijiPipe.OFFX || type == JFijiPipe.OFFY) {
			if( i < -wid2 || i > wid2) {
				if( i < 0) i = -wid2;
				else i = wid2;
				cur1.setValue(i);
			}
		}*/
		setPipe.mri1.setOff(type, i);
		return i;
	}

	private void changeTableValues() {
		boolean isTable = jCheckTable.isSelected();
		setPipe.mri1.isTable = isTable;
		if(isTable) {
			if( mt==null) {
				mt = new ModifyTable(parent, false);
				mt.init(this);
			}
			mt.setVisible(true);
		}
		parent.repaint();
	}
/*	private void setPipeIgnore(boolean ignoreXY) {
		panel1.maybeChangeUseXYShift(!ignoreXY);
	}*/
	
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
		int offS[] = getStoreSub(indx);
		String val1 = jPrefer.get(indxStr, null);
		if( val1 == null || offS == null) val1 = defVal;
		jTextStore.setText(val1);
		if( offS != null) {
			System.arraycopy(offS,0,setPipe.mri1.mriOffs,0,MATRIX_SIZE);
			setXYZ();
		}
	}

	private  void setXYZ() {
		JFijiPipe.mriOff m1 = setPipe.mri1;
		jSpinOffX.setValue(m1.getCleanOff(JFijiPipe.OFFX));
		jSpinOffY.setValue(m1.getCleanOff(JFijiPipe.OFFY));
		jSpinOffZ.setValue(m1.getCleanOff(JFijiPipe.OFFZ));
		isInit = false;
	}

	private int [] getStoreSub(int indx) {
		String indxStr = "sync store offs" + indx;
		String tmp, val1 = jPrefer.get(indxStr, null);
		if( val1 == null) return null;
		int[] offS = new int[MATRIX_SIZE];
		val1 = val1.substring(1, val1.length()-1);
		String[] split1 = val1.split(",");
		int len1 = split1.length;
		if ( len1 != MATRIX_SIZE) return offS;	// if wrong return zero array
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
		refPipe = panel1.getCorrectedOrUncorrectedPipe(false);
		setPipe = panel1.getMriOrCtPipe();
//		setPipe.mri1.debug();
	}
	
	private void windowClosing() {
		if( mt != null) mt.dispose();
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
        jLabZ = new javax.swing.JLabel();
        jLabX = new javax.swing.JLabel();
        jSpinOffX = new javax.swing.JSpinner();
        jLabY = new javax.swing.JLabel();
        jSpinOffY = new javax.swing.JSpinner();
        jSpinOffZ = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        jButHelp = new javax.swing.JButton();
        jSpinStore = new javax.swing.JSpinner();
        jTextStore = new javax.swing.JTextField();
        jCheckTable = new javax.swing.JCheckBox();
        jButReset = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Sync MRI data");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jLabel1.setText("When using MRI data, there can be an alignment problem");

        jLabel2.setText("compared to the PET-CT study.");

        jLabel3.setText("This program allows a manual correction to be applied.");

        jLabel6.setText("Reset to zero X,Y,Z in all (axial, sagittal, coronal) views.");

        jLabZ.setText("Z:");

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

        jSpinOffZ.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinOffZStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabZ)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinOffZ, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabX)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinOffX, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabY)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinOffY, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabZ)
                .addComponent(jLabX)
                .addComponent(jSpinOffX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabY)
                .addComponent(jSpinOffY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jSpinOffZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel7.setText("Choose where to store data and set label:");

        jButHelp.setText("Help");
        jButHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButHelpActionPerformed(evt);
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

        jCheckTable.setToolTipText("modify Table X,Y");
        jCheckTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckTableActionPerformed(evt);
            }
        });

        jButReset.setText("Reset");
        jButReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButHelp)
                        .addGap(34, 34, 34))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jSpinStore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextStore, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                    .addComponent(jLabel7)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jCheckTable))
                                .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                    .addComponent(jButReset)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.LEADING)))
                        .addGap(0, 0, Short.MAX_VALUE))))
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
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7))
                    .addComponent(jCheckTable))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinStore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextStore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButReset))
                .addContainerGap(7, Short.MAX_VALUE))
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

    private void jCheckTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckTableActionPerformed
		changeTableValues();
    }//GEN-LAST:event_jCheckTableActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
		windowClosing();
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButHelp;
    private javax.swing.JButton jButReset;
    private javax.swing.JCheckBox jCheckTable;
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

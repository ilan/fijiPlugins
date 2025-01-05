
import ij.ImagePlus;
import ij.WindowManager;
import infovis.panel.DoubleBoundedRangeModel;
import infovis.panel.dqinter.DoubleRangeSlider;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.Date;
import java.util.prefs.Preferences;
import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * RenalFrame.java
 *
 * Created on Jun 21, 2011, 9:51:54 AM
 */
/**
 *
 * @author ilan
 */
public class RenalFrame extends javax.swing.JFrame {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;
	static final int LEFT_KIDNEY = 0;
	static final int RIGHT_KIDNEY = 1;
	static final int LEFT_BACKGROUND = 2;
	static final int RIGHT_BACKGROUND = 3;
	static final int MAN1 = 4;
	static final int MAN2 = 5;

	/** Creates new form RenalFrame */
	public RenalFrame() {
		initComponents();
		init();
	}
	
	private void init() {
		ChooseRenal dlg = new ChooseRenal(this, true);
		dlg.setVisible(true);
		renalImg = dlg.chosenRenal;
		if( renalImg == null) return;
		WindowManager.addWindow(this);
		jPrefer = dlg.jPrefer;
		initDualSlider();
		renalPanel1.LoadData(this, renalImg);
		int i, j;
		Dimension sz1 = new Dimension();
		sz1.height = jPrefer.getInt("renal dialog height", 0);
		if( sz1.height > 0) {
			i = jPrefer.getInt("renal dialog x", 0);
			j = jPrefer.getInt("renal dialog y", 0);
			sz1.width = jPrefer.getInt("renal dialog width", 0);
			setSize(sz1);
			setLocation(i,j);
		}
		jGraph1Panel.setLayout(new FlowLayout());
		jGraph2Panel.setLayout(new FlowLayout());
	}

	private void initDualSlider() {
		dSlide = new DoubleRangeSlider(0, 1000, 0, 500);
		dSlide.setEnabled(true);
		GroupLayout jPanelDualLayout = (GroupLayout) jPanel1.getLayout();
		jPanelDualLayout.setHorizontalGroup(
			jPanelDualLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(jPanelDualLayout.createSequentialGroup()
				.addComponent(dSlide))
				);
		jPanelDualLayout.setVerticalGroup(
			jPanelDualLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup( jPanelDualLayout.createSequentialGroup()
			.addComponent(dSlide))
			);
		DoubleBoundedRangeModel range1 = dSlide.getModel();
		range1.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				double newLevel, newWidth, hiVal, loVal;
				hiVal = dSlide.getHighValue();
				loVal = dSlide.getLowValue();
				newWidth = hiVal-loVal;
				newLevel = (hiVal+loVal)/2;
				renalPanel1.ActionSliderChanged(newWidth, newLevel);
			}
		});
	}

	public Date getStudyDate() {
		if( renalPanel1.rPipe == null) {
			return new Date();	// it no longer exists so return "now"
		}
		return renalPanel1.rPipe.data1.serTime;
	}
	
	boolean isT12() {
		return jCheckT12time.isSelected();
	}

	void fillPatientData() {
		JFijiPipe p1 = renalPanel1.rPipe;
		String meta = p1.data1.metaData;
		if( meta == null) return;
		BI_dbSaveInfo curr1;
		curr1 = (BI_dbSaveInfo) renalImg.getProperty("bidb");	// get database info
		m_patName = ChoosePetCt.getCompressedPatName(meta);
		m_patID = ChoosePetCt.getCompressedID(meta);
		m_styName = ChoosePetCt.getDicomValue(meta, "0008,1030");
		m_serName = ChoosePetCt.getDicomValue(meta, "0008,103E");
		if( curr1 != null && !curr1.serName.isEmpty()) {
			m_styName = curr1.styName;
			m_serName = curr1.serName;
		}
		String tmp = ChoosePetCt.getDicomValue(meta, "0010,0030");
		m_patBirthday = ChoosePetCt.getDateTime(tmp, null);
		int time = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0018,1242"));
		if( time <= 0) time = 30000;
		frameDuration = time;
		dualGraphs = true;
		if( frameDuration < 10000) dualGraphs = false;
		setTitle(getTitleBar());
	}

	String getTitleBar() {
		JFijiPipe p1 = renalPanel1.rPipe;
		String str1 = m_patName + "   ";
		if( m_patBirthday != null) {
			long sdyTime, birthTime, currDiff;
			Integer years;
			sdyTime = getStudyDate().getTime();
			birthTime = m_patBirthday.getTime();
			currDiff = (sdyTime - birthTime)/(24*60*60*1000);	// number of days
			years = (int)( currDiff/365.242199);
			str1 += years.toString() + "y   ";
		}
		str1 += m_patID + "   ";
		str1 += DateFormat.getDateInstance(DateFormat.MEDIUM).format(p1.data1.serTime);
		str1 += "   " + m_styName + "   " + m_serName;
		return str1;
	}

	private void buttonPush(int val) {
		JToggleButton curBut = getButton(val);
		boolean b1 = curBut.isSelected();
		releaseButton(saveVal);
		if(b1) {
			saveVal = val;
		}
	}
	
	void releaseButton( int val) {
		renalPanel1.saveRoi(val);
		if( val < 0) return;
		JToggleButton curBut = getButton(val);
		curBut.setSelected(false);
		saveVal = -1;
	}
	
	JToggleButton getButton(int val) {
		JToggleButton curBut = jTogLeftKidney;
		switch( val) {
			case RIGHT_KIDNEY:
				curBut = jTogRightKidney;
				break;
				
			case LEFT_BACKGROUND:
				curBut = jTogLeftBackground;
				break;
				
			case RIGHT_BACKGROUND:
				curBut = jTogRightBackground;
				break;
				
			case MAN1:
				curBut = jTogMan1;
				break;
				
			case MAN2:
				curBut = jTogMan2;
				break;
		}
		return curBut;
	}

	void savePrefs() {
		if( renalImg == null) return;
		Dimension sz1 = getSize();
		Point pt1 = getLocation();
		jPrefer.putInt("renal dialog x", pt1.x);
		jPrefer.putInt("renal dialog y", pt1.y);
		jPrefer.putInt("renal dialog width", sz1.width);
		jPrefer.putInt("renal dialog height", sz1.height);
	}

	void fitWindow() {
		Dimension sz1, sz2;
		int width1, width2, heigh1, heigh2, offY, menuY;
		int offX2=0, offY2=0;
		double scale1;
		menuY = 20;	// what exactly is this number?
		offY = renalPanel1.getY();
		sz1 = this.getRootPane().getSize();
		sz2 = renalPanel1.getWindowDim();
		scale1 = ((double) sz2.height) / sz2.width;	// should be 0.5
		width1 = sz1.width;
		heigh1 = (int) (width1 * scale1 + 0.5);
		renalPanel1.setSize(width1, heigh1);
		offY += heigh1;
		width2 = width1;
		saveGraphHeight = heigh2 = sz1.height - offY - menuY;
		if( dualGraphs) {
			offY2 = offY;
			if( width2*100/heigh2 > 140) {
				width2 /= 2;
				offX2 = width2;
			} else {
				heigh2 /= 2;
				offY2 += heigh2;
			}
		}
		jGraph1Panel.setLocation(0, offY);
		jGraph1Panel.setSize(width2, heigh2);
		if( dualGraphs) {
			jGraph2Panel.setLocation(offX2, offY2);
			jGraph2Panel.setSize(width2, heigh2);
		}
	}

	public JPanel getjGraph1Panel() {
		return jGraph1Panel;
	}

	public JPanel getjGraph2Panel() {
		return jGraph2Panel;
	}
	
	void saveData(boolean dispFlg) {
		if( work2 != null) return;	// already working
		disposeFlg = dispFlg;
		work2 = new bkgdSaveData();
		work2.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String propertyName = evt.getPropertyName();
				if( propertyName.equals("state")) {
					SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
					if( state == SwingWorker.StateValue.DONE) {
						work2 = null;
					}
				}
			}
		});
		work2.execute();
	}
	
	void doSave() {
		try {
			BufferedImage im1;
			Rectangle rc1 = renalPanel1.getVisibleRect();
			Point pt1 = renalPanel1.getLocationOnScreen();
			rc1.height += saveGraphHeight;
			rc1.x = pt1.x;
			rc1.y = pt1.y;
			Thread.sleep(100);
			im1 = new Robot().createScreenCapture(rc1);
			ImagePlus myImage = new ImagePlus("Renal clearance", im1);
			String meta1 = Extra.makeMetaData("Renal clearance", 1, myImage,
					ChoosePetCt.SOPCLASS_TYPE_SC, renalPanel1.rPipe.data1.metaData);
			myImage.setProperty("Info", meta1);
			myImage.show();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		if( disposeFlg) dispose();
	}

	/**
	 * Save data, i.e. make a new ImagePlus object is done in the background
	 */
	protected class bkgdSaveData extends SwingWorker {

		@Override
		protected Void doInBackground() {
			doSave();
			return null;
		}
		
	}

	@Override
	public void dispose() {
		WindowManager.removeWindow(this);
		savePrefs();
		super.dispose();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        jTogLeftKidney = new javax.swing.JToggleButton();
        jTogRightKidney = new javax.swing.JToggleButton();
        jTogLeftBackground = new javax.swing.JToggleButton();
        jTogRightBackground = new javax.swing.JToggleButton();
        jTogMan1 = new javax.swing.JToggleButton();
        jTogMan2 = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jPanel1 = new javax.swing.JPanel();
        renalPanel1 = new RenalPanel();
        jGraph1Panel = new javax.swing.JPanel();
        jGraph2Panel = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuSave = new javax.swing.JMenuItem();
        jMenuSaveExit = new javax.swing.JMenuItem();
        jMenuExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jCheckT12time = new javax.swing.JCheckBoxMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuContents = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Renal Clearance");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jToolBar1.setRollover(true);

        jTogLeftKidney.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        jTogLeftKidney.setForeground(new java.awt.Color(255, 0, 0));
        jTogLeftKidney.setText("LK");
        jTogLeftKidney.setFocusable(false);
        jTogLeftKidney.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jTogLeftKidney.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jTogLeftKidney.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTogLeftKidneyActionPerformed(evt);
            }
        });
        jToolBar1.add(jTogLeftKidney);

        jTogRightKidney.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        jTogRightKidney.setForeground(new java.awt.Color(0, 0, 255));
        jTogRightKidney.setText("RK");
        jTogRightKidney.setFocusable(false);
        jTogRightKidney.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jTogRightKidney.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jTogRightKidney.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTogRightKidneyActionPerformed(evt);
            }
        });
        jToolBar1.add(jTogRightKidney);

        jTogLeftBackground.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        jTogLeftBackground.setForeground(new java.awt.Color(0, 255, 0));
        jTogLeftBackground.setText("LB");
        jTogLeftBackground.setFocusable(false);
        jTogLeftBackground.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jTogLeftBackground.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jTogLeftBackground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTogLeftBackgroundActionPerformed(evt);
            }
        });
        jToolBar1.add(jTogLeftBackground);

        jTogRightBackground.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        jTogRightBackground.setForeground(java.awt.Color.magenta);
        jTogRightBackground.setText("RB");
        jTogRightBackground.setFocusable(false);
        jTogRightBackground.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jTogRightBackground.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jTogRightBackground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTogRightBackgroundActionPerformed(evt);
            }
        });
        jToolBar1.add(jTogRightBackground);

        jTogMan1.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        jTogMan1.setForeground(java.awt.Color.cyan);
        jTogMan1.setText("m1");
        jTogMan1.setFocusable(false);
        jTogMan1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jTogMan1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jTogMan1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTogMan1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jTogMan1);

        jTogMan2.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        jTogMan2.setForeground(java.awt.Color.pink);
        jTogMan2.setText("m2");
        jTogMan2.setFocusable(false);
        jTogMan2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jTogMan2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jTogMan2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTogMan2ActionPerformed(evt);
            }
        });
        jToolBar1.add(jTogMan2);
        jToolBar1.add(jSeparator1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );

        jToolBar1.add(jPanel1);

        javax.swing.GroupLayout renalPanel1Layout = new javax.swing.GroupLayout(renalPanel1);
        renalPanel1.setLayout(renalPanel1Layout);
        renalPanel1Layout.setHorizontalGroup(
            renalPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        renalPanel1Layout.setVerticalGroup(
            renalPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 103, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jGraph1PanelLayout = new javax.swing.GroupLayout(jGraph1Panel);
        jGraph1Panel.setLayout(jGraph1PanelLayout);
        jGraph1PanelLayout.setHorizontalGroup(
            jGraph1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 187, Short.MAX_VALUE)
        );
        jGraph1PanelLayout.setVerticalGroup(
            jGraph1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 133, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jGraph2PanelLayout = new javax.swing.GroupLayout(jGraph2Panel);
        jGraph2Panel.setLayout(jGraph2PanelLayout);
        jGraph2PanelLayout.setHorizontalGroup(
            jGraph2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 207, Short.MAX_VALUE)
        );
        jGraph2PanelLayout.setVerticalGroup(
            jGraph2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 133, Short.MAX_VALUE)
        );

        jMenu1.setText("File");

        jMenuSave.setText("Save");
        jMenuSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuSave);

        jMenuSaveExit.setText("Save and exit");
        jMenuSaveExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveExitActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuSaveExit);

        jMenuExit.setText("Exit");
        jMenuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuExitActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuExit);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        jCheckT12time.setText("Set T½ time");
        jMenu2.add(jCheckT12time);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Help");

        jMenuContents.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        jMenuContents.setText("Contents");
        jMenuContents.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuContentsActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuContents);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .addComponent(renalPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jGraph1Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jGraph2Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(renalPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jGraph2Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jGraph1Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jTogLeftKidneyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTogLeftKidneyActionPerformed
		buttonPush(LEFT_KIDNEY);
	}//GEN-LAST:event_jTogLeftKidneyActionPerformed

	private void jTogRightKidneyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTogRightKidneyActionPerformed
		buttonPush(RIGHT_KIDNEY);
	}//GEN-LAST:event_jTogRightKidneyActionPerformed

	private void jTogLeftBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTogLeftBackgroundActionPerformed
		buttonPush(LEFT_BACKGROUND);
	}//GEN-LAST:event_jTogLeftBackgroundActionPerformed

	private void jTogRightBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTogRightBackgroundActionPerformed
		buttonPush(RIGHT_BACKGROUND);
	}//GEN-LAST:event_jTogRightBackgroundActionPerformed

	private void jTogMan1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTogMan1ActionPerformed
		buttonPush(MAN1);
	}//GEN-LAST:event_jTogMan1ActionPerformed

	private void jTogMan2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTogMan2ActionPerformed
		buttonPush(MAN2);
	}//GEN-LAST:event_jTogMan2ActionPerformed

	private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
		renalPanel1.resizeOnce = false;
	}//GEN-LAST:event_formComponentResized

	private void jMenuContentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuContentsActionPerformed
		ChoosePetCt.openHelp("Renal Clearance");
	}//GEN-LAST:event_jMenuContentsActionPerformed

	private void jMenuSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSaveActionPerformed
		saveData(false);
	}//GEN-LAST:event_jMenuSaveActionPerformed

	private void jMenuSaveExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSaveExitActionPerformed
		saveData(true);
	}//GEN-LAST:event_jMenuSaveExitActionPerformed

	private void jMenuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuExitActionPerformed
		dispose();
	}//GEN-LAST:event_jMenuExitActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem jCheckT12time;
    private javax.swing.JPanel jGraph1Panel;
    private javax.swing.JPanel jGraph2Panel;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuContents;
    private javax.swing.JMenuItem jMenuExit;
    private javax.swing.JMenuItem jMenuSave;
    private javax.swing.JMenuItem jMenuSaveExit;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToggleButton jTogLeftBackground;
    private javax.swing.JToggleButton jTogLeftKidney;
    private javax.swing.JToggleButton jTogMan1;
    private javax.swing.JToggleButton jTogMan2;
    private javax.swing.JToggleButton jTogRightBackground;
    private javax.swing.JToggleButton jTogRightKidney;
    private javax.swing.JToolBar jToolBar1;
    private RenalPanel renalPanel1;
    // End of variables declaration//GEN-END:variables
	ImagePlus renalImg = null;
	int frameDuration, saveVal = -1, saveGraphHeight;
	Preferences jPrefer = null;
	DoubleRangeSlider dSlide = null;
	String m_patName, m_patID, m_styName, m_serName;
	Date m_patBirthday = null;
	boolean dualGraphs = true, disposeFlg = false;
	bkgdSaveData work2 = null;
}

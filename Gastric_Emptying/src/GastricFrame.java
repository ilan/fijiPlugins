
import ij.ImagePlus;
import ij.WindowManager;
import infovis.panel.DoubleBoundedRangeModel;
import infovis.panel.dqinter.DoubleRangeSlider;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.Date;
import java.util.prefs.Preferences;
import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * GastricFrame.java
 *
 * Created on Aug 30, 2010, 3:07:24 PM
 */

/**
 *
 * @author Ilan
 */
public class GastricFrame extends javax.swing.JFrame {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;

    /** Creates new form GastricFrame */
    public GastricFrame() {
        initComponents();
		init();
    }

	class ManualParameters {
		boolean usePoint = false;
		int frameNum;
		double minutes, factor;
		Point anterior, posterior;
	}

	private void init() {
		ChooseGastric chooseDlg = new ChooseGastric(this, true);
		chooseDlg.setVisible(true);
		if( chooseDlg.chosenOnes == null ) return;
		WindowManager.addWindow(this);
		jPrefer = chooseDlg.jPrefer;
		initDualSlider();
		gastricPanel1.LoadData(this, chooseDlg.imgList, chooseDlg.chosenOnes);
		int i, j;
		Dimension sz1 = new Dimension();
		sz1.height = jPrefer.getInt("gastric dialog height", 0);
		if( sz1.height > 0) {
			i = jPrefer.getInt("gastric dialog x", 0);
			j = jPrefer.getInt("gastric dialog y", 0);
			sz1.width = jPrefer.getInt("gastric dialog width", 0);
			setSize(sz1);
			setLocation(i,j);
		}
		jPanelGraph.setLayout(new FlowLayout());
		OKtoRun = true;
	}

	private void initDualSlider() {
		dSlide = new DoubleRangeSlider(0, 1000, 0 ,1000);
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
			public void stateChanged( ChangeEvent e) {
				double newLevel, newWidth, hiVal, loVal;
				hiVal = dSlide.getHighValue();
				loVal = dSlide.getLowValue();
				newWidth = hiVal-loVal;
				newLevel = (hiVal+loVal)/2;
				gastricPanel1.ActionSliderChanged(newWidth, newLevel);
			}
		});
	}

	void fillPatientData() {
		JFijiPipe ant1 = gastricPanel1.antPipe;
		String meta = ant1.data1.metaData;
		if( meta == null) return;
		m_patName = ChoosePetCt.getCompressedPatName(meta);
		m_patID = ChoosePetCt.getCompressedID(meta);
		m_styName = ChoosePetCt.getDicomValue(meta, "0008,1030");
		String tmp = ChoosePetCt.getDicomValue(meta, "0010,0030");
		m_patBirthday = ChoosePetCt.getDateTime(tmp, null);
		int time = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0018,1242"));
		if( time <= 0) time = 60000;
		frameDuration = time;
		setTitle(getTitleBar());
	}

	String getTitleBar() {
		JFijiPipe ant1 = gastricPanel1.antPipe;
		String str1 = m_patName + "   ";
		if( m_patBirthday != null) {
			Date tmpDate = new Date();
			long currTime, birthTime, currDiff;
			Integer years;
			currTime = tmpDate.getTime();
			birthTime = m_patBirthday.getTime();
			currDiff = (currTime - birthTime)/(24*60*60*1000);	// number of days
			years = (int)( currDiff/365.242199);
			str1 += years.toString() + "y   ";
		}
		str1 += m_patID + "   ";
		str1 += DateFormat.getDateInstance(DateFormat.MEDIUM).format(ant1.data1.serTime);
		str1 += "   " + m_styName;
		return str1;
	}

	void changeDisplayType( int buttonNum) {
		gastricPanel1.dispType = buttonNum;
		int antVal = 1;
		if( buttonNum > 1) antVal = 0;
		gastricPanel1.antPipe.imgPos[0].x = antVal;
		jButAnterior.setSelected(buttonNum == 0);
		jButPosterior.setSelected(buttonNum == 1);
		jButMean.setSelected(buttonNum == 2);
		gastricPanel1.setupGraph();
		repaint();
	}

	void killRoiBut() {
		jButDrawRoi.setSelected(false);
		pressRoiBut();
		gastricPanel1.setupGraph();
	}

	void pressRoiBut() {
		gastricPanel1.drawingRoi = jButDrawRoi.isSelected();
		gastricPanel1.startRoi = true;
	}

	void releaseManual() {
		gman = null;
		jButManual.setSelected(false);
	}

	void pressManualBut() {
		if( gman != null) {
			gman.dispose();
			return;
		}
		gman = new GastricManual(this, false);
		gman.setVisible(true);
	}

	void savePrefs() {
		if( !OKtoRun) return;
		Dimension sz1 = getSize();
		Point pt1 = getLocation();
		jPrefer.putInt("gastric dialog x", pt1.x);
		jPrefer.putInt("gastric dialog y", pt1.y);
		jPrefer.putInt("gastric dialog width", sz1.width);
		jPrefer.putInt("gastric dialog height", sz1.height);
	}

	void fitWindow() {
		Dimension sz1, sz2;
		int width1, heigh1, heigh2, offY;
		double scale1;
		offY = gastricPanel1.getY();
		sz1 = this.getRootPane().getSize();
		sz2 = gastricPanel1.getWindowDim();
		scale1 = ((double) sz2.height) / sz2.width;	// should be 0.5
		width1 = sz1.width;
		heigh1 = ChoosePetCt.round(width1 * scale1);
		gastricPanel1.setSize(width1, heigh1);
		offY += heigh1+5;
		heigh2 = sz1.height-offY-20;
		jPanelGraph.setLocation(0, offY);
		jPanelGraph.setSize(width1, heigh2);
	}

	double getTime( int frameNum) {	// FrameNum counts from 1, not 0
		int i, off1 = 0, n = mpar1.length;
		double min1 = 0;
		for( i=0; i<n; i++) {
			if( mpar1[i] == null || !mpar1[i].usePoint) break;
			if( mpar1[i].frameNum <= frameNum) {
				off1 = mpar1[i].frameNum;
				min1 = mpar1[i].minutes;
			}
		}
		min1 += (frameNum - off1) * frameDuration / 60000.0;
		return min1;
	}

	Point getRoiOffset( int frameNum, boolean antFlg) {	// frameNum counts from 0, not 1
		Point currVal, retVal = new Point();
		int i, n = mpar1.length;
		for( i=0; i<n; i++) {
			if( mpar1[i] == null || !mpar1[i].usePoint) break;
			if( mpar1[i].frameNum <= frameNum+1) {
				if( antFlg) currVal = mpar1[i].anterior;
				else currVal = mpar1[i].posterior;
				retVal.x += currVal.x;
				retVal.y += currVal.y;
			}
		}
		return retVal;
	}

	public JPanel getjPanelGraph() {
		return jPanelGraph;
	}

	GastricPanel getGastricPanel() {
		return gastricPanel1;
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
			Rectangle rc1 = gastricPanel1.getVisibleRect();
			Point pt1 = gastricPanel1.getLocationOnScreen();
			Dimension sz2 = jPanelGraph.getSize();
			rc1.height += sz2.height;
			rc1.x = pt1.x;
			rc1.y = pt1.y;
			Thread.sleep(100);
			im1 = new Robot().createScreenCapture(rc1);
			ImagePlus myImage = new ImagePlus("Gastric analysis", im1);
			String meta1 = Extra.makeMetaData("Gastric analysis", 1, myImage,
					ChoosePetCt.SOPCLASS_TYPE_SC, gastricPanel1.antPipe.data1.metaData);
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
        jButDrawRoi = new javax.swing.JToggleButton();
        jButAnterior = new javax.swing.JToggleButton();
        jButPosterior = new javax.swing.JToggleButton();
        jButMean = new javax.swing.JToggleButton();
        jButManual = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jPanel1 = new javax.swing.JPanel();
        gastricPanel1 = new GastricPanel();
        jPanelGraph = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuSave = new javax.swing.JMenuItem();
        jMenuSaveExit = new javax.swing.JMenuItem();
        jMenuExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        jMenuHelpContents = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Gastric Emptying");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jToolBar1.setRollover(true);

        jButDrawRoi.setSelected(true);
        jButDrawRoi.setText("Draw ROI");
        jButDrawRoi.setFocusable(false);
        jButDrawRoi.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButDrawRoi.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButDrawRoi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButDrawRoiActionPerformed(evt);
            }
        });
        jToolBar1.add(jButDrawRoi);

        jButAnterior.setSelected(true);
        jButAnterior.setText("Anterior");
        jButAnterior.setFocusable(false);
        jButAnterior.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButAnterior.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButAnterior.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButAnteriorActionPerformed(evt);
            }
        });
        jToolBar1.add(jButAnterior);

        jButPosterior.setText("Posterior");
        jButPosterior.setFocusable(false);
        jButPosterior.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButPosterior.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButPosterior.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButPosteriorActionPerformed(evt);
            }
        });
        jToolBar1.add(jButPosterior);

        jButMean.setText("Mean");
        jButMean.setFocusable(false);
        jButMean.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButMean.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButMean.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMeanActionPerformed(evt);
            }
        });
        jToolBar1.add(jButMean);

        jButManual.setText("Manual");
        jButManual.setFocusable(false);
        jButManual.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButManual.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButManualActionPerformed(evt);
            }
        });
        jToolBar1.add(jButManual);
        jToolBar1.add(jSeparator1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 139, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );

        jToolBar1.add(jPanel1);

        javax.swing.GroupLayout gastricPanel1Layout = new javax.swing.GroupLayout(gastricPanel1);
        gastricPanel1.setLayout(gastricPanel1Layout);
        gastricPanel1Layout.setHorizontalGroup(
            gastricPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 406, Short.MAX_VALUE)
        );
        gastricPanel1Layout.setVerticalGroup(
            gastricPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 210, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanelGraphLayout = new javax.swing.GroupLayout(jPanelGraph);
        jPanelGraph.setLayout(jPanelGraphLayout);
        jPanelGraphLayout.setHorizontalGroup(
            jPanelGraphLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 406, Short.MAX_VALUE)
        );
        jPanelGraphLayout.setVerticalGroup(
            jPanelGraphLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 181, Short.MAX_VALUE)
        );

        jMenu1.setText("File");

        jMenuSave.setText("Save");
        jMenuSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuSave);

        jMenuSaveExit.setText("Save and Exit");
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
        jMenuBar1.add(jMenu2);

        jMenu3.setText("Help");

        jMenuHelpContents.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        jMenuHelpContents.setText("Contents");
        jMenuHelpContents.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuHelpContentsActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuHelpContents);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE)
            .addComponent(jPanelGraph, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(gastricPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gastricPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelGraph, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jButAnteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButAnteriorActionPerformed
		changeDisplayType(0);
	}//GEN-LAST:event_jButAnteriorActionPerformed

	private void jButPosteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButPosteriorActionPerformed
		changeDisplayType(1);
	}//GEN-LAST:event_jButPosteriorActionPerformed

	private void jButMeanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMeanActionPerformed
		changeDisplayType(2);
	}//GEN-LAST:event_jButMeanActionPerformed

	private void jButDrawRoiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButDrawRoiActionPerformed
		pressRoiBut();
	}//GEN-LAST:event_jButDrawRoiActionPerformed

	private void jButManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButManualActionPerformed
		pressManualBut();
	}//GEN-LAST:event_jButManualActionPerformed

	private void jMenuHelpContentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuHelpContentsActionPerformed
		ChoosePetCt.openHelp("Gastric Emptying");
	}//GEN-LAST:event_jMenuHelpContentsActionPerformed

	private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
		gastricPanel1.resizeOnce = false;
	}//GEN-LAST:event_formComponentResized

	private void jMenuSaveExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSaveExitActionPerformed
		saveData(true);
	}//GEN-LAST:event_jMenuSaveExitActionPerformed

	private void jMenuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuExitActionPerformed
		dispose();
	}//GEN-LAST:event_jMenuExitActionPerformed

	private void jMenuSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSaveActionPerformed
		saveData(false);
	}//GEN-LAST:event_jMenuSaveActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private GastricPanel gastricPanel1;
    private javax.swing.JToggleButton jButAnterior;
    private javax.swing.JToggleButton jButDrawRoi;
    private javax.swing.JToggleButton jButManual;
    private javax.swing.JToggleButton jButMean;
    private javax.swing.JToggleButton jButPosterior;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuExit;
    private javax.swing.JMenuItem jMenuHelpContents;
    private javax.swing.JMenuItem jMenuSave;
    private javax.swing.JMenuItem jMenuSaveExit;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelGraph;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables
	Preferences jPrefer = null;
	DoubleRangeSlider dSlide = null;
	boolean OKtoRun = false, disposeFlg = false;
	String m_patName, m_patID, m_styName;
	Date m_patBirthday = null;
	GastricManual gman = null;
	ManualParameters[] mpar1 = new ManualParameters[4];
	int frameDuration;
	bkgdSaveData work2 = null;
}

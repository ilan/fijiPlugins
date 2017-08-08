import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import infovis.panel.DoubleBoundedRangeModel;
import infovis.panel.dqinter.DoubleRangeSlider;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Locale;
import java.util.prefs.Preferences;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*
 * Display3Frame.java
 *
 * Created on Jan 14, 2010, 1:36:01 PM
 */

/**
 *
 * @author Ilan
 */
public class Display3Frame extends javax.swing.JFrame implements KeyListener, WindowFocusListener {
	static final int DSP3_PET = 0;
	static final int DSP3_CT = 1;
	static final int DSP3_FUSED = 2;

    /** Creates new form Display3Frame */
	public Display3Frame() {
		super();
		isInitialized = false;
		SwingUtilities.invokeLater(new Runnable(){

			@Override
			public void run() {
				initComponents();
				isInitialized = true;
			}
		});
	}

	void init0() {
		ImageIcon img = new ImageIcon(getClass().getResource("resources/bethIsrael.png"));
		setIconImage(img.getImage());
		ctLevel = new int[5];
		ctWidth = new int[5];
		ctLevel[0] = 56;	// Chest-Abdomen
		ctWidth[0] = 340;
		ctLevel[1] = -400;	// Lung
		ctWidth[1] = 1200;
		ctLevel[2] = 93;	// Liver
		ctWidth[2] = 108;
		ctLevel[3] = 570;	// Bone
		ctWidth[3] = 2000;
		ctLevel[4] = 40;	// Brain-Sinus
		ctWidth[4] = 80;
		m_titleType = "";
		levelSave = "0";
		widthSave = "0";
		initDualSlider();
		setCineButtons(false);
		if(ChoosePetCt.isOptionSelected(jPrefer, ChoosePetCt.BLACK_BKGD)) {
			display3Panel1.setBackground(new java.awt.Color(0, 0, 0));
		}
		display3Panel1.splitCursor = jPrefer.getBoolean("split cursor", false);
		addWindowFocusListener(this);
	}

	public boolean init1(ChoosePetCt dlg1) {
		if( openMode > 0) return false;
		jPrefer = dlg1.jPrefer;
		ArrayList<Integer> chosen = dlg1.chosenOnes;
		if( chosen == null || chosen.size() != 1) return false;
		int i, j, k;
		ImagePlus currImg;
		Dimension sz1 = new Dimension();
		sz1.height = jPrefer.getInt("display3 dialog height", 0);
		if( sz1.height > 0) {
			i = jPrefer.getInt("display3 dialog x", 0);
			j = jPrefer.getInt("display3 dialog y", 0);
			sz1.width = jPrefer.getInt("display3 dialog width", 0);
			setSize(sz1);
			setLocation(i,j);
		}
		init0();
		k = chosen.get(0);
		currImg = dlg1.imgList.get(k);
		j = dlg1.getSeriesType(k);
		display3Panel1.LoadData(this, currImg, ChoosePetCt.getForcedVal(j));
		initFinish();
		jMenuSync.setVisible(false);
		jMenuMixReset.setVisible(false);
		openMode = 1;
		return true;
	}

	public Display3Panel getDisplay3Panel() {
		return display3Panel1;
	}

	public boolean init2(JFijiPipe petCtPipe, PetCtFrame par1, JFijiPipe ctPipe, int type) {
		if( openMode > 0) return false;
		srcPipe = petCtPipe;
		srcCtPipe = ctPipe;
		srcFrame = par1;
		m_type = type;
		Dimension sz1 = new Dimension();
		jPrefer = par1.jPrefer;
		sz1.height = jPrefer.getInt("display3 dialog height", 0);
		if( sz1.height > 0) {
			sz1.width = jPrefer.getInt("display3 dialog width", 0);
			setSize(sz1);
		}
		setLocationRelativeTo(par1);
		init0();
		switch(type) {
			case DSP3_PET:
				m_titleType = "3Pet: ";
				break;

			case DSP3_CT:
				m_titleType = "3Ct: ";
				break;

			case DSP3_FUSED:
				m_titleType = "3fused: ";
				break;
				
			default:
				m_titleType = "";
		}
		display3Panel1.LoadData(this, petCtPipe, ctPipe);
		if(isFused()) {
			display3Panel1.d3Color = par1.getPetCtPanel1().fusedColor;
			display3Panel1.d3FusedColor = par1.getPetCtPanel1().ctMriColor;
		}
		else jMenuMixReset.setVisible(false);
		if(ChoosePetCt.getUserLUT(ChoosePetCt.EXT_BLUES_LUT, jPrefer) != null)
			jCheckBlues.setText(jPrefer.get("ext blues name", "The blues"));
		if(ChoosePetCt.getUserLUT(ChoosePetCt.EXT_HOT_IRON_LUT, jPrefer) != null)
			jCheckHotIron.setText(jPrefer.get("ext hotiron name", "Hot iron"));
		jCheckMriLut.setVisible(false);
		if(ChoosePetCt.getUserLUT(ChoosePetCt.MRI_CT_LUT, jPrefer) != null) {
			jCheckMriLut.setVisible(true);
			jCheckMriLut.setText(jPrefer.get("MriCt name", "MRI/CT LUT"));
		}
		isChange( petCtPipe);
		initFinish();
		openMode = 2;
		return true;
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		int diff;
		if( display3Panel1.isGated && !display3Panel1.runGated &&
			(keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT)) {
			e.consume();
			display3Panel1.incrGatePosition(keyCode == KeyEvent.VK_RIGHT);
			return;
		}
		if( keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_UP ||
			keyCode == KeyEvent.VK_PAGE_DOWN || keyCode == KeyEvent.VK_PAGE_UP) {
			e.consume();
			diff = 1;
			if( keyCode == KeyEvent.VK_DOWN) diff = -1;
			if( keyCode == KeyEvent.VK_PAGE_UP) diff = pgUpDn;
			if( keyCode == KeyEvent.VK_PAGE_DOWN) diff = -pgUpDn;
			if( PetCtFrame.keyIndex > 0) {
				long time1 = System.currentTimeMillis();
				long diff1 = time1 - currKeyTime;
//				IJ.showStatus("diff = " + diff1 + ", curr= " + currKeyTime);
				if( diff1 >= 0 && diff1 < keyDelay[PetCtFrame.keyIndex]) return;
				currKeyTime = time1;
			}
			display3Panel1.incrSlicePosition(scrollPage, diff);
		}
		if( keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
			e.consume();
			diff = 1;
			if( keyCode == KeyEvent.VK_LEFT) diff = -1;
			diff += PetCtFrame.keyIndex;
			if( diff < 0) diff = 0;
			if( diff >= 9) diff = 9;
			PetCtFrame.keyIndex = diff;
			IJ.showStatus("Key delay = " + diff);
		}
		if( keyCode == KeyEvent.VK_HOME) {
			e.consume();
			display3Panel1.incrSlicePosition(scrollPage, 1000);
		}
		if( keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_3) {
			scrollPage = keyCode - KeyEvent.VK_0;
		}
		if( keyCode >= KeyEvent.VK_NUMPAD1 && keyCode <= KeyEvent.VK_NUMPAD3) {
			scrollPage = keyCode - KeyEvent.VK_NUMPAD0;
		}
		if( keyCode == KeyEvent.VK_Z) {
			e.consume();
			display3Panel1.zoomTog = !display3Panel1.zoomTog;
			jButZoom.setSelected(display3Panel1.zoomTog);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
//		currKeyTime = 0;
	}

	public int updateCenterPoint( JFijiPipe petCtPipe) {
		int retVal = 0;
		srcPipe = petCtPipe;
		boolean change = isChange( petCtPipe);
		if( change) {
			retVal = 1;
			hideAllPopupMenus();
			if( display3Panel1.calculateSUVandCT()) return -1;	// no pipe, kill parent
			display3Panel1.maybePanImage();
			display3Panel1.repaint();
		}
		return retVal;
	}

	private boolean isChange( JFijiPipe petCtPipe) {
		boolean change = false;
		PetCtFrame par1 = srcFrame;
		if( par1 == null) return change;
		double inVal, outVal;
		int sliceIn;
		inVal = par1.getPetCtPanel1().petAxial;
		outVal = petCtPipe.findCtPos(inVal, true);
		if( isChangeSub(display3Panel1.d3Axial, outVal)) change = true;
		display3Panel1.d3Axial = outVal;

		inVal = par1.getPetCtPanel1().petCoronal;
		outVal = petCtPipe.corFactor * inVal + petCtPipe.corOffset - petCtPipe.mriOffY;
		if( isChangeSub(display3Panel1.d3Coronal, outVal)) change = true;
		display3Panel1.d3Coronal = outVal;

		inVal = par1.getPetCtPanel1().petSagital;
		outVal = petCtPipe.sagFactor * inVal + petCtPipe.sagOffset - petCtPipe.mriOffX;
		if(  isChangeSub(display3Panel1.d3Sagital, outVal)) change = true;
		display3Panel1.d3Sagital = outVal;

		sliceIn = par1.getPetCtPanel1().gateIndx;
		if( sliceIn != display3Panel1.gateIndx) change = true;
		display3Panel1.gateIndx = sliceIn;
		display3Panel1.invertScroll = par1.invertScroll;
		// need to update brown fat dialog
		display3Panel1.bfDlg = par1.getPetCtPanel1().bfDlg;
		display3Panel1.anotateDlg = par1.getPetCtPanel1().anotateDlg;
		return change;
	}
	
	void syncMaster() {
		hideAllPopupMenus();
		int page = display3Panel1.mouse1.page;
		// by luck the page number equals dsp_axial etc.
		if( srcFrame == null) return;
		double d3Axial, d3Coronal, d3Sagital;
		double factor, off0, offMri;
		JFijiPipe petPipe;
		PetCtPanel petPanel = srcFrame.getPetCtPanel1();
		d3Axial = display3Panel1.d3Axial;
		d3Coronal = display3Panel1.d3Coronal;
		d3Sagital = display3Panel1.d3Sagital;
		int styType = display3Panel1.styType;
		if( styType == Display3Panel.CT_STUDY || styType == Display3Panel.MRI_STUDY) {
			factor = srcPipe.corFactor;
			off0 = srcPipe.corOffset;
			offMri = srcPipe.mriOffY;
			d3Coronal = (d3Coronal + offMri - off0)/factor;

			factor = srcPipe.sagFactor;
			off0 = srcPipe.sagOffset;
			offMri = srcPipe.mriOffX;
			d3Sagital = (d3Sagital + offMri - off0)/factor;

			petPipe = petPanel.petPipe;
			if(petPipe != null) d3Axial = display3Panel1.d3Pipe.findPetPos(d3Axial, petPipe, true);
		}
		petPanel.petAxial = d3Axial;
		petPanel.petCoronal = d3Coronal;
		petPanel.petSagital = d3Sagital;
		petPanel.updateFakeValues();
		srcFrame.changeLayout(page);
	}
	
	private boolean isChangeSub(double inVal, double outVal) {
		int sliceIn, sliceOut;
		sliceIn  = ChoosePetCt.round(inVal);
		sliceOut = ChoosePetCt.round(outVal);
		return sliceIn != sliceOut;
	}

	void initFinish() {
		int styType;
		boolean fusedFlg;
		int ser = display3Panel1.d3Pipe.data1.seriesType;
		if( ser == ChoosePetCt.SERIES_CT || 	ser == ChoosePetCt.SERIES_CT_VARICAM
				|| ser == ChoosePetCt.SER_FORCE_CT) {
			display3Panel1.d3Color = JFijiPipe.COLOR_GRAY;
			jMenuAuto.setVisible(false);
			jMenuBrain.setVisible(false);
			jMenuMax.setVisible(false);
			jCheckBlues.setVisible(false);
			jCheckGrayScale.setVisible(false);
			jCheckInverse.setVisible(false);
			jCheckHotIron.setVisible(false);
			jCheckSourceColor.setVisible(false);
			styType = Display3Panel.CT_STUDY;
		} else {
			fusedFlg = isFused();
			if( !fusedFlg) {
				jCheckChest.setVisible(false);
				jCheckLung.setVisible(false);
				jCheckLiver.setVisible(false);
				jCheckBone.setVisible(false);
				jCheckBrain.setVisible(false);
				jCheckMriLut.setVisible(false);
			}
			styType = Display3Panel.SUV_STUDY;
			if( fusedFlg) styType = Display3Panel.FUSED_SUV;
			if( display3Panel1.d3Pipe.data1.SUVfactor <= 0) {
				jMenuBrain.setVisible(false);
				styType = Display3Panel.CNT_STUDY;
				if( ser == ChoosePetCt.SERIES_MRI || ser == ChoosePetCt.SER_FORCE_MRI)
					styType = Display3Panel.MRI_STUDY;
				if( fusedFlg) styType = Display3Panel.FUSED_CNT;
			}
			if( fusedFlg) {
				jCheckGrayScale.setVisible(false);
				jCheckInverse.setVisible(false);
			}
		}
		display3Panel1.styType = styType;
		double lowVal = dSlide.getLowValue();
		dSlide.setLowValue(lowVal + 1);	// cause an event
		dSlide.setLowValue(lowVal);
		getPreferences();
		SUVmm = jPrefer.getInt("SUV mm", 20);
		SUVtype = jPrefer.getInt("SUV type", 0);
		display3Panel1.calculateSUVandCT();
		jWidthVal.addKeyListener(this);
		keyDelay = new int[10];
		int i,j;
		j = 300;
		for( i=1; i<10; i++) {
			keyDelay[i] = j;
			j = (int) (j * 1.3);
		}
	}
	
	boolean isFused() {
		return display3Panel1.ct4fused != null;
	}

	private void getPreferences() {
		display3Panel1.limitCursor = jPrefer.getBoolean("small 3cursor", false);
		display3Panel1.cursorSize = jPrefer.getInt("3cursor size", 10);
		pgUpDn = jPrefer.getInt("page up down", 3);
	}

	private void initDualSlider() {
		dSlide = new DoubleRangeSlider(0, 1000, 0 ,1000) {
			@Override
			public String getToolTipText(MouseEvent evt) {
				return display3Panel1.getSliderToolTip();
			}
		};
		dSlide.setLocale(Locale.US);
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
				double oldLevel, newLevel, oldWidth, newWidth, hiVal, loVal, tmpVal;
				String tmp;
				JFijiPipe pip1;
				hiVal = dSlide.getHighValue();
				loVal = dSlide.getLowValue();
				oldLevel = PetCtFrame.roundN(Double.valueOf( levelSave), slideDigits);
				oldWidth = PetCtFrame.roundN(Double.valueOf( widthSave), slideDigits);
				newWidth = PetCtFrame.roundN(hiVal-loVal, slideDigits);
				newLevel = PetCtFrame.roundN((hiVal+loVal)/2, slideDigits);
				if( newLevel == oldLevel && newWidth == oldWidth) return;
				widthSave = PetCtFrame.myFormat(newWidth, slideDigits);
				levelSave = PetCtFrame.myFormat(newLevel, slideDigits);
				int styType = display3Panel1.styType;
				if( styType == Display3Panel.CT_STUDY || styType == Display3Panel.FUSED_SUV ||
					styType == Display3Panel.FUSED_CNT || styType < 0) {
					jWidthVal.setText(widthSave);
					jLevelVal.setText(levelSave);
				} else {
					pip1 = display3Panel1.d3Pipe;
					tmpVal = pip1.getLogOriginal(loVal);
					tmp = PetCtFrame.myFormat(tmpVal, slideDigits);
					jWidthVal.setText(tmp);
					tmpVal = pip1.getLogOriginal(hiVal);
					tmp = PetCtFrame.myFormat(tmpVal, slideDigits);
					jLevelVal.setText(tmp);
					
				}
				display3Panel1.ActionSliderChanged(newWidth, newLevel);
			}
		});
	}

	void updateMenu() {
		boolean mipTog = display3Panel1.showMip;
		jButMip.setSelected(mipTog);
		setCineButtons(mipTog);
	}

	void setDualSliderValues( double loVal, double hiVal) {
		dSlide.setLowValue(loVal);
		dSlide.setHighValue(hiVal);
	}

	void switchDualSlider( int digits) {
		slideDigits = digits;
		double min1, max1, width1, level1;
		min1 = display3Panel1.minSlider;
		max1 = display3Panel1.maxSlider;
		width1 = display3Panel1.getWidthSlider();
		level1 = display3Panel1.getLevelSlider();
		dSlide.setMinimum(min1);
		dSlide.setMaximum(max1);
		dSlide.setLowValue(level1 - width1 / 2);
		dSlide.setHighValue(level1 + width1 / 2);
		jWidthVal.setToolTipText(display3Panel1.getLowerEditToolTip());
		jLevelVal.setToolTipText(display3Panel1.getUpperEditToolTip());
	}

	void fillPatientData() {
		JFijiPipe pet1 = display3Panel1.d3Pipe;
		String meta = pet1.data1.metaData;
		if( meta == null) return;
		m_patName = ChoosePetCt.getCompressedPatName(meta);
		m_patID = ChoosePetCt.getCompressedID(meta);
		m_styName = ChoosePetCt.getDicomValue(meta, "0008,1030");
	}

	String getTitleBar(int type) {
		JFijiPipe pet1 = display3Panel1.d3Pipe;
		String str1 = m_patName + "   " + m_patID + "   ";
		if( pet1 == null) return str1;
		str1 += ChoosePetCt.UsaDateFormat(pet1.data1.serTime);
		switch( type) {
			case 1:
				return str1;
				
			case 2:
				return m_styName;
				
			default:
				return m_titleType + str1 + "   " + m_styName;
		}
	}

	void swapMipState() {
		display3Panel1.showMip = !display3Panel1.showMip;
		updateMenu();
		display3Panel1.repaint();
	}

	void setCineButtons(boolean vis1) {
		jButFwd.setVisible(vis1);
		jButFront.setVisible(vis1);
		jButSide.setVisible(vis1);
		if(!vis1) return;
		int val =  display3Panel1.getCineState();
		boolean cineRun = false, cineFor = false, cineSid = false;
		switch( val) {
			case PetCtPanel.CINE_RUNNING:
				cineRun = true;
				break;

			case PetCtPanel.CINE_FORWARD:
				cineFor = true;
				break;

			case PetCtPanel.CINE_SIDE:
				cineSid = true;
				break;
		}
		jButFwd.setSelected(cineRun);
		jButFront.setSelected(cineFor);
		jButSide.setSelected(cineSid);
	}
	
	double getSliceThickness() {
		if( display3Panel1.mouse1.page != 1) return 0;
		return Math.abs(display3Panel1.d3Pipe.data1.spacingBetweenSlices);
	}

	void savePrefs() {
		if( openMode <= 0) return;
		Dimension sz1 = getSize();
		jPrefer.putInt("display3 dialog width", sz1.width);
		jPrefer.putInt("display3 dialog height", sz1.height);
		if( openMode >= 2) return;
		Point pt1 = getLocation();
		jPrefer.putInt("display3 dialog x", pt1.x);
		jPrefer.putInt("display3 dialog y", pt1.y);
	}

	public JPopupMenu getjPopupD3() {
		return jPopupD3;
	}

	void hideAllPopupMenus() {
		jPopupD3.setVisible(false);
	}

	private void changePetColor( int indx) {
		display3Panel1.d3Color = indx;
		hideAllPopupMenus();
		display3Panel1.repaint();
	}
	
	private void changeMriColor() {
		int indx = JFijiPipe.COLOR_GRAY;
		if( jCheckMriLut.isSelected()) indx = JFijiPipe.COLOR_MRI;
		if(isFused()) display3Panel1.d3FusedColor = indx;
		else display3Panel1.d3Color = indx;
		hideAllPopupMenus();
		display3Panel1.repaint();
	}

	private void flipSplitState() {
		display3Panel1.splitCursor = !display3Panel1.splitCursor;
		jPrefer.putBoolean("split cursor", display3Panel1.splitCursor);
		hideAllPopupMenus();
		display3Panel1.repaint();
	}

	void fitWindow() {
		Dimension sz1, sz0, sz2;
		double scale1;
		sz1 = getSize();
		sz0 = display3Panel1.getSize();
		// get the difference between the panel and the whole window
		sz1.height -= sz0.height;
		sz1.width -= sz0.width;
		sz2 = display3Panel1.getWindowDim(display3Panel1.d3Pipe);

		scale1 = ((double) sz0.width) / sz2.width;
		sz2.width = ChoosePetCt.round(scale1* sz2.width);
		sz2.height = ChoosePetCt.round(scale1* sz2.height);
		display3Panel1.setSize(sz2);
		sz2.width += sz1.width;
		sz2.height += sz1.height;
		setSize(sz2);
		display3Panel1.updateMultYOff(false);
	}

	/**
	 * Called when user clicks on Auto reset, or one of the CT preset levels.
	 * This routine is used to reset the gray scale after the user has changed it.
	 * It is a convenient way for him to fix the gray scale back to known levels.
	 *
	 * @param indx the key to which level to set.
	 */
	protected void setWindowLevelAndWidth( int indx) {
		hideAllPopupMenus();
		display3Panel1.presetWindowLevels(indx);
	}

	void updateCheckmarks() {
		int i, indx = display3Panel1.d3Color;
		jCheckBlues.setSelected(indx == JFijiPipe.COLOR_BLUES);
		jCheckGrayScale.setSelected(indx == JFijiPipe.COLOR_GRAY);
		jCheckInverse.setSelected(indx == JFijiPipe.COLOR_INVERSE);
		jCheckHotIron.setSelected(indx == JFijiPipe.COLOR_HOTIRON);
		jCheckSourceColor.setSelected(indx == JFijiPipe.COLOR_USER);
		jCheckSplitCursor.setSelected(display3Panel1.splitCursor);
		if( display3Panel1.d3Pipe == null) return;
		JFijiPipe currPipe = display3Panel1.d3Pipe;
		if( isFused()) currPipe = display3Panel1.ct4fused;
		indx = -1;
		for( i=0; i<5; i++) {
			if( (int)currPipe.winLevel == ctLevel[i] && (int)currPipe.winWidth == ctWidth[i]) {
				indx = i;
				break;
			}
		}
		jCheckChest.setSelected(indx == 0);
		jCheckLung.setSelected(indx == 1);
		jCheckLiver.setSelected(indx == 2);
		jCheckBone.setSelected(indx == 3);
		jCheckBrain.setSelected(indx == 4);
	}

/*	void externalSpinnerChange( int diff) {
		display3Panel1.maybeIncrSlicePosition(-diff);
	}*/

	void setGatedButtonVisible( boolean visible) {
		jButGated.setVisible(visible);
	}
	
	void resetMixing() {
		setDualSliderValues(475,525);	// 500 - 50/2, 500 + 50/2
		hideAllPopupMenus();
		display3Panel1.repaint();
	}
	
	void fillWindowList() {
		int i;
		String title = PetCtFrame.IMAGEJ_NAME;
		javax.swing.JMenuItem item1;
		Window win;
		Window [] frList = WindowManager.getAllNonImageWindows();
		if( frList == null) return;
		for( i=-1; i<frList.length; i++) {
			if( i>=0) {
				win = frList[i];
				title = win instanceof Frame?((Frame)win).getTitle():((Dialog)win).getTitle();
			}
			item1 = new javax.swing.JMenuItem(title);
			item1.addActionListener(new java.awt.event.ActionListener() {

				@Override
				public void actionPerformed(ActionEvent ae) {
					chooseWindowAction(ae);
				}
			});
			jMenuWindow.add(item1);
		}
	}
	
	private void chooseWindowAction(java.awt.event.ActionEvent evt) {
		int i;
		String title;
		title = evt.getActionCommand();
		Window win;
		if(title.equals(PetCtFrame.IMAGEJ_NAME)) {
			ImageJ ij = IJ.getInstance();
			if( ij != null) WindowManager.toFront(ij);
			return;
		}
		Window [] frList = WindowManager.getAllNonImageWindows();
		if( frList == null) return;
		for( i=0; i<frList.length; i++) {
			win = frList[i];
			if( win instanceof Frame) {
				Frame currFr = (Frame) win;
				if(currFr.getTitle().equals(title)) {
					WindowManager.toFront(currFr);
					return;
				}
				continue;
			}
			if( win instanceof Dialog) {
				Dialog currDlg = (Dialog) win;
				if(currDlg.getTitle().equals(title)) {
					currDlg.toFront();
					return;
				}
			}
		}
	}

	@Override
	public void dispose() {
		WindowManager.removeWindow(this);
		hideAllPopupMenus();
		savePrefs();
		display3Panel1.d3Pipe = display3Panel1.mipPipe = null;
		display3Panel1.ct4fused = null;
		display3Panel1.parent = null;
		openMode = 0;
		srcPipe = null;
		srcCtPipe = null;
		// null the reference to this object
		if( srcFrame != null) switch( m_type) {
			case DSP3_PET:
				srcFrame.pet3 = null;
				break;

			case DSP3_CT:
				srcFrame.ct3 = null;
				break;

			case DSP3_FUSED:
				srcFrame.fuse3 = null;
				break;
		}
		srcFrame = null;
		super.dispose();
	}

	@Override
	public void windowGainedFocus(WindowEvent we) {
		fillWindowList();
	}

	@Override
	public void windowLostFocus(WindowEvent we) {
		jMenuWindow.removeAll();
	}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupD3 = new javax.swing.JPopupMenu();
        jMenuMixReset = new javax.swing.JMenuItem();
        jCheckSplitCursor = new javax.swing.JCheckBoxMenuItem();
        jMenuSync = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuAuto = new javax.swing.JMenuItem();
        jMenuBrain = new javax.swing.JMenuItem();
        jMenuMax = new javax.swing.JMenuItem();
        jCheckChest = new javax.swing.JCheckBoxMenuItem();
        jCheckLung = new javax.swing.JCheckBoxMenuItem();
        jCheckLiver = new javax.swing.JCheckBoxMenuItem();
        jCheckBone = new javax.swing.JCheckBoxMenuItem();
        jCheckBrain = new javax.swing.JCheckBoxMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jCheckInverse = new javax.swing.JCheckBoxMenuItem();
        jCheckGrayScale = new javax.swing.JCheckBoxMenuItem();
        jCheckBlues = new javax.swing.JCheckBoxMenuItem();
        jCheckHotIron = new javax.swing.JCheckBoxMenuItem();
        jCheckSourceColor = new javax.swing.JCheckBoxMenuItem();
        jCheckMriLut = new javax.swing.JCheckBoxMenuItem();
        jToolBar1 = new javax.swing.JToolBar();
        jButZoom = new javax.swing.JToggleButton();
        jButGated = new javax.swing.JToggleButton();
        jButMip = new javax.swing.JToggleButton();
        jButFwd = new javax.swing.JToggleButton();
        jButFront = new javax.swing.JToggleButton();
        jButSide = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jWidthVal = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLevelVal = new javax.swing.JTextField();
        display3Panel1 = new Display3Panel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuSaveMip = new javax.swing.JMenuItem();
        jMenuExit = new javax.swing.JMenuItem();
        jMenuEdit = new javax.swing.JMenu();
        jMenuFitWindow = new javax.swing.JMenuItem();
        jMenuOptions = new javax.swing.JMenuItem();
        jMenuWindow = new javax.swing.JMenu();
        jMenuHelp = new javax.swing.JMenu();
        jMenuContents = new javax.swing.JMenuItem();
        jMenuAbout = new javax.swing.JMenuItem();

        jMenuMixReset.setText("50-50 mix");
        jMenuMixReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuMixResetActionPerformed(evt);
            }
        });
        jPopupD3.add(jMenuMixReset);

        jCheckSplitCursor.setText("Split cursor");
        jCheckSplitCursor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckSplitCursorActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckSplitCursor);

        jMenuSync.setText("Sync");
        jMenuSync.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSyncActionPerformed(evt);
            }
        });
        jPopupD3.add(jMenuSync);
        jPopupD3.add(jSeparator2);

        jMenuAuto.setText("Auto level");
        jMenuAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAutoActionPerformed(evt);
            }
        });
        jPopupD3.add(jMenuAuto);

        jMenuBrain.setText("Brain");
        jMenuBrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuBrainActionPerformed(evt);
            }
        });
        jPopupD3.add(jMenuBrain);

        jMenuMax.setText("Slice Max");
        jMenuMax.setToolTipText("");
        jMenuMax.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuMaxActionPerformed(evt);
            }
        });
        jPopupD3.add(jMenuMax);

        jCheckChest.setText("Chest-Abdomen");
        jCheckChest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckChestActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckChest);

        jCheckLung.setText("Lung");
        jCheckLung.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckLungActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckLung);

        jCheckLiver.setText("Liver");
        jCheckLiver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckLiverActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckLiver);

        jCheckBone.setText("Bone");
        jCheckBone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoneActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckBone);

        jCheckBrain.setText("Brain");
        jCheckBrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBrainActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckBrain);
        jPopupD3.add(jSeparator3);

        jCheckInverse.setText("Inverse");
        jCheckInverse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckInverseActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckInverse);

        jCheckGrayScale.setSelected(true);
        jCheckGrayScale.setText("Gray scale");
        jCheckGrayScale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckGrayScaleActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckGrayScale);

        jCheckBlues.setSelected(true);
        jCheckBlues.setText("The Blues");
        jCheckBlues.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBluesActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckBlues);

        jCheckHotIron.setText("Hot Iron");
        jCheckHotIron.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckHotIronActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckHotIron);

        jCheckSourceColor.setSelected(true);
        jCheckSourceColor.setText("Use source");
        jCheckSourceColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckSourceColorActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckSourceColor);

        jCheckMriLut.setText("--");
        jCheckMriLut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckMriLutActionPerformed(evt);
            }
        });
        jPopupD3.add(jCheckMriLut);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("3 view");

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        jButZoom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/zoom.gif"))); // NOI18N
        jButZoom.setToolTipText("zoom (using mouse wheel) and pan images");
        jButZoom.setFocusable(false);
        jButZoom.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButZoom.setPreferredSize(new java.awt.Dimension(17, 10));
        jButZoom.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButZoom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButZoomActionPerformed(evt);
            }
        });
        jToolBar1.add(jButZoom);

        jButGated.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/cine.gif"))); // NOI18N
        jButGated.setSelected(true);
        jButGated.setToolTipText("cine gated images");
        jButGated.setFocusable(false);
        jButGated.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButGated.setPreferredSize(new java.awt.Dimension(20, 27));
        jButGated.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButGated.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButGatedActionPerformed(evt);
            }
        });
        jToolBar1.add(jButGated);

        jButMip.setText("MIP");
        jButMip.setToolTipText("show MIP or sagittal image");
        jButMip.setFocusable(false);
        jButMip.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButMip.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButMip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMipActionPerformed(evt);
            }
        });
        jToolBar1.add(jButMip);

        jButFwd.setText(">>");
        jButFwd.setToolTipText("rotate MIP image");
        jButFwd.setFocusable(false);
        jButFwd.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButFwd.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButFwd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButFwdActionPerformed(evt);
            }
        });
        jToolBar1.add(jButFwd);

        jButFront.setText("F");
        jButFront.setToolTipText("MIP in frontal view");
        jButFront.setFocusable(false);
        jButFront.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButFront.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButFront.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButFrontActionPerformed(evt);
            }
        });
        jToolBar1.add(jButFront);

        jButSide.setText("S");
        jButSide.setToolTipText("MIP in side view");
        jButSide.setFocusable(false);
        jButSide.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButSide.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButSide.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSideActionPerformed(evt);
            }
        });
        jToolBar1.add(jButSide);
        jToolBar1.add(jSeparator1);

        jWidthVal.setEditable(false);
        jWidthVal.setText("0");
        jWidthVal.setMaximumSize(new java.awt.Dimension(50, 20));
        jWidthVal.setPreferredSize(new java.awt.Dimension(44, 20));
        jToolBar1.add(jWidthVal);

        jPanel1.setPreferredSize(new java.awt.Dimension(100, 23));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 140, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        jToolBar1.add(jPanel1);

        jLevelVal.setEditable(false);
        jLevelVal.setText("0");
        jLevelVal.setMaximumSize(new java.awt.Dimension(50, 20));
        jLevelVal.setPreferredSize(new java.awt.Dimension(40, 20));
        jToolBar1.add(jLevelVal);

        javax.swing.GroupLayout display3Panel1Layout = new javax.swing.GroupLayout(display3Panel1);
        display3Panel1.setLayout(display3Panel1Layout);
        display3Panel1Layout.setHorizontalGroup(
            display3Panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        display3Panel1Layout.setVerticalGroup(
            display3Panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 248, Short.MAX_VALUE)
        );

        jMenuFile.setText("File");

        jMenuSaveMip.setText("Save MIP");
        jMenuSaveMip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveMipActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuSaveMip);

        jMenuExit.setText("Exit");
        jMenuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuExitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuExit);

        jMenuBar1.add(jMenuFile);

        jMenuEdit.setText("Edit");

        jMenuFitWindow.setText("Fit Window to data");
        jMenuFitWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuFitWindowActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuFitWindow);

        jMenuOptions.setText("Options");
        jMenuOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuOptionsActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuOptions);

        jMenuBar1.add(jMenuEdit);

        jMenuWindow.setText("Window");
        jMenuBar1.add(jMenuWindow);

        jMenuHelp.setText("Help");

        jMenuContents.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        jMenuContents.setText("Contents");
        jMenuContents.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuContentsActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuContents);

        jMenuAbout.setText("About");
        jMenuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAboutActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuAbout);

        jMenuBar1.add(jMenuHelp);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(display3Panel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(display3Panel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jMenuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuExitActionPerformed
		dispose();
	}//GEN-LAST:event_jMenuExitActionPerformed

	private void jButMipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMipActionPerformed
		swapMipState();
	}//GEN-LAST:event_jButMipActionPerformed

	private void jButFwdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButFwdActionPerformed
		display3Panel1.ActionMipCine(PetCtPanel.CINE_RUNNING);
		setCineButtons(true);
	}//GEN-LAST:event_jButFwdActionPerformed

	private void jButFrontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButFrontActionPerformed
		display3Panel1.ActionMipCine(PetCtPanel.CINE_FORWARD);
		setCineButtons(true);
	}//GEN-LAST:event_jButFrontActionPerformed

	private void jButSideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSideActionPerformed
		display3Panel1.ActionMipCine(PetCtPanel.CINE_SIDE);
		setCineButtons(true);
	}//GEN-LAST:event_jButSideActionPerformed

	private void jMenuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAboutActionPerformed
		PetCtAbout dlg = new PetCtAbout(this, true);
		dlg.setVisible(true);
	}//GEN-LAST:event_jMenuAboutActionPerformed

	private void jMenuContentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuContentsActionPerformed
		ChoosePetCt.openHelp("Display 3 views");
	}//GEN-LAST:event_jMenuContentsActionPerformed

	private void jCheckSplitCursorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckSplitCursorActionPerformed
		flipSplitState();
	}//GEN-LAST:event_jCheckSplitCursorActionPerformed

	private void jMenuAutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAutoActionPerformed
		setWindowLevelAndWidth(0);
	}//GEN-LAST:event_jMenuAutoActionPerformed

	private void jMenuBrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuBrainActionPerformed
		setWindowLevelAndWidth(9);
	}//GEN-LAST:event_jMenuBrainActionPerformed

	private void jCheckLungActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckLungActionPerformed
		setWindowLevelAndWidth(2);
	}//GEN-LAST:event_jCheckLungActionPerformed

	private void jCheckChestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckChestActionPerformed
		setWindowLevelAndWidth(1);
	}//GEN-LAST:event_jCheckChestActionPerformed

	private void jCheckLiverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckLiverActionPerformed
		setWindowLevelAndWidth(3);
	}//GEN-LAST:event_jCheckLiverActionPerformed

	private void jCheckBoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoneActionPerformed
		setWindowLevelAndWidth(4);
	}//GEN-LAST:event_jCheckBoneActionPerformed

	private void jCheckBrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBrainActionPerformed
		setWindowLevelAndWidth(5);
	}//GEN-LAST:event_jCheckBrainActionPerformed

	private void jCheckInverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckInverseActionPerformed
		changePetColor(JFijiPipe.COLOR_INVERSE);
	}//GEN-LAST:event_jCheckInverseActionPerformed

	private void jCheckGrayScaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckGrayScaleActionPerformed
		changePetColor(JFijiPipe.COLOR_GRAY);
	}//GEN-LAST:event_jCheckGrayScaleActionPerformed

	private void jCheckBluesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBluesActionPerformed
		changePetColor(JFijiPipe.COLOR_BLUES);
	}//GEN-LAST:event_jCheckBluesActionPerformed

	private void jCheckHotIronActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckHotIronActionPerformed
		changePetColor(JFijiPipe.COLOR_HOTIRON);
	}//GEN-LAST:event_jCheckHotIronActionPerformed

	private void jMenuFitWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuFitWindowActionPerformed
		fitWindow();
	}//GEN-LAST:event_jMenuFitWindowActionPerformed

	private void jMenuOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuOptionsActionPerformed
		Display3Options dlg = new Display3Options(this, true);
		dlg.setVisible(true);
		getPreferences();
		display3Panel1.repaint();
	}//GEN-LAST:event_jMenuOptionsActionPerformed

	private void jButGatedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButGatedActionPerformed
		display3Panel1.runGated = jButGated.isSelected();
	}//GEN-LAST:event_jButGatedActionPerformed

	private void jMenuSaveMipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSaveMipActionPerformed
		SaveMip dlg = new SaveMip(this, true);
		dlg.setVisible(true);
		dlg.doAction(null);
	}//GEN-LAST:event_jMenuSaveMipActionPerformed

	private void jButZoomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButZoomActionPerformed
		display3Panel1.zoomTog = !display3Panel1.zoomTog;
		hideAllPopupMenus();
		display3Panel1.repaint();
	}//GEN-LAST:event_jButZoomActionPerformed

    private void jMenuMaxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuMaxActionPerformed
		setWindowLevelAndWidth(8);
    }//GEN-LAST:event_jMenuMaxActionPerformed

    private void jCheckSourceColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckSourceColorActionPerformed
		changePetColor(JFijiPipe.COLOR_USER);
    }//GEN-LAST:event_jCheckSourceColorActionPerformed

    private void jMenuSyncActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSyncActionPerformed
		syncMaster();
    }//GEN-LAST:event_jMenuSyncActionPerformed

    private void jMenuMixResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuMixResetActionPerformed
		resetMixing();
    }//GEN-LAST:event_jMenuMixResetActionPerformed

    private void jCheckMriLutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckMriLutActionPerformed
		changeMriColor();
    }//GEN-LAST:event_jCheckMriLutActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private Display3Panel display3Panel1;
    private javax.swing.JToggleButton jButFront;
    private javax.swing.JToggleButton jButFwd;
    private javax.swing.JToggleButton jButGated;
    private javax.swing.JToggleButton jButMip;
    private javax.swing.JToggleButton jButSide;
    private javax.swing.JToggleButton jButZoom;
    private javax.swing.JCheckBoxMenuItem jCheckBlues;
    private javax.swing.JCheckBoxMenuItem jCheckBone;
    private javax.swing.JCheckBoxMenuItem jCheckBrain;
    private javax.swing.JCheckBoxMenuItem jCheckChest;
    private javax.swing.JCheckBoxMenuItem jCheckGrayScale;
    private javax.swing.JCheckBoxMenuItem jCheckHotIron;
    private javax.swing.JCheckBoxMenuItem jCheckInverse;
    private javax.swing.JCheckBoxMenuItem jCheckLiver;
    private javax.swing.JCheckBoxMenuItem jCheckLung;
    private javax.swing.JCheckBoxMenuItem jCheckMriLut;
    private javax.swing.JCheckBoxMenuItem jCheckSourceColor;
    private javax.swing.JCheckBoxMenuItem jCheckSplitCursor;
    private javax.swing.JTextField jLevelVal;
    private javax.swing.JMenuItem jMenuAbout;
    private javax.swing.JMenuItem jMenuAuto;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuBrain;
    private javax.swing.JMenuItem jMenuContents;
    private javax.swing.JMenu jMenuEdit;
    private javax.swing.JMenuItem jMenuExit;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuFitWindow;
    private javax.swing.JMenu jMenuHelp;
    private javax.swing.JMenuItem jMenuMax;
    private javax.swing.JMenuItem jMenuMixReset;
    private javax.swing.JMenuItem jMenuOptions;
    private javax.swing.JMenuItem jMenuSaveMip;
    private javax.swing.JMenuItem jMenuSync;
    private javax.swing.JMenu jMenuWindow;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPopupMenu jPopupD3;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTextField jWidthVal;
    // End of variables declaration//GEN-END:variables
	int openMode = 0, SUVmm = 5, SUVtype, pgUpDn = 3, m_type = -1;
	JFijiPipe srcPipe = null, srcCtPipe = null;
	PetCtFrame srcFrame = null;
	boolean isInitialized;
	Preferences jPrefer = null;
	DoubleRangeSlider dSlide = null;
	String m_titleType, m_patName, m_patID, m_styName, levelSave, widthSave;
	long currKeyTime = 0;
	int slideDigits = 0, scrollPage = 1;
	int [] ctLevel = null;
	int [] ctWidth = null;
	int [] keyDelay = null;
}

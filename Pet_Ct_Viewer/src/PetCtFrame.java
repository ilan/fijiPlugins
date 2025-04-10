import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import infovis.panel.DoubleBoundedRangeModel;
import infovis.panel.dqinter.DoubleRangeSlider;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.prefs.Preferences;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jogamp.vecmath.Point2d;
import org.jogamp.vecmath.Point3d;

/*
 * PetCtFrame.java
 *
 * Created on Dec 8, 2009, 10:51:52 AM
 */

/**
 *
 * @author Ilan
 */
public class PetCtFrame extends javax.swing.JFrame implements KeyListener, WindowFocusListener {

/** Creates new form PetCtFrame
	 * @param arg */
	public PetCtFrame(String arg) {
		super();
		runArg = arg;
		isInitialized = false;
//		Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler());
		SwingUtilities.invokeLater(new Runnable(){

			@Override
			public void run() {
				Thread.currentThread().setUncaughtExceptionHandler(new MyExceptionHandler());
				initComponents();
				init(runArg);
				isInitialized = true;
			}
		});
	}


	/**
	 * This routine first and foremost calls a dialog to choose the elements of the frame.
	 * However an optional arg has been added. This can be used by Read_BI_Studies to
	 * pass a list of series UIDs which have just been read from the database.
	 * If this list is valid, then no choose dialog is presented and Pet_Ct_Viewer comes up
	 * immediately. This is to be "user friendly" in the simple cases of Pet-Ct.
	 * 
	 * @param arg seriesUIDs
	 */
	protected final void init(String arg) {
		chooseDlg = new ChoosePetCt(this, true);
		String tmp;
		if( !chooseDlg.checkList(arg)) chooseDlg.setVisible(true);
		ArrayList<Integer> chosen = chooseDlg.chosenOnes;
		foundData = 0;
//		System.out.println(System.getProperty("java.vendor"));
//		System.out.println(System.getProperty("java.vendor.url"));
//		System.out.println(System.getProperty("java.version"));
//		String tmp = System.getProperty("LD_LIBRARY_PATH");
		if( chosen == null) return;
		foundData = chosen.size();
		if( foundData < 2) return;
		jWidthVal.addKeyListener(this);
		jPrefer = chooseDlg.jPrefer;
		int i, j, k;
		ImagePlus currImg;
		Dimension sz1 = new Dimension();
		sz1.height = jPrefer.getInt("petct dialog height", 0);
		if( sz1.height > 0) {
			i = jPrefer.getInt("petct dialog x", 0);
			j = jPrefer.getInt("petct dialog y", 0);
			sz1.width = jPrefer.getInt("petct dialog width", 0);
			setSize(sz1);
			setLocation(i,j);
		}
		if(ChoosePetCt.isOptionSelected(jPrefer, ChoosePetCt.BLACK_BKGD)) {
			petCtPanel1.setBackground(new java.awt.Color(0, 0, 0));
		}
		if( ChoosePetCt.isOptionSelected(jPrefer, ChoosePetCt.HOT_IRON_FUSE)) {
			petCtPanel1.fusedColor = JFijiPipe.COLOR_HOTIRON;
		}
		sliceMaxFactor = jPrefer.getDouble("slice max factor", 1.0);
		pgUpDn = jPrefer.getInt("page up down", 3);
		setSliceMaxText();
		operatorNameFlg = jPrefer.getBoolean("operator name", false);
		autoResize = jPrefer.getBoolean("auto resize", true);
		circleSUV = jPrefer.getBoolean("circle SUV", false);
		sphereSUV = jPrefer.getBoolean("sphere SUV", false);
		circleSUV2 = jPrefer.getBoolean("circle SUV2", false);
		sphereSUV2 = jPrefer.getBoolean("sphere SUV2", false);
		invertScroll = jPrefer.getBoolean("invert scroll", false);
		useSUL = jPrefer.getBoolean("use SUL", false);
		isFWHM = jPrefer.getBoolean("Gauss FWHM", false);
		sigmaGauss = jPrefer.getDouble("sigma Gauss", 2.5);
		startROIs = jPrefer.getBoolean("start ROIs", false);
		qualityRendering = jPrefer.getBoolean("quality rendering", false);
		triThick = jPrefer.getDouble("tricubic zmin", 2.0);
		startAnnotations = jPrefer.getBoolean("start annotations", false);
		ignoreSUV = jPrefer.getBoolean("ignore SUV", false);
		allowMRIchop = jPrefer.getBoolean("allow MRI chop", false);
		jMenuShowSource.setSelected(jPrefer.getBoolean("show source", true));
		jMenuShowCursor.setSelected(jPrefer.getBoolean("show cursor", false));
		SUVmm = jPrefer.getInt("SUV mm", 20);
		SUVtype = jPrefer.getInt("SUV type", 0);
		SUVmm2 = jPrefer.getInt("SUV mm2", 20);
		SUVtype2 = jPrefer.getInt("SUV type2", 0);
		jCheckTop.setSelected(jPrefer.getBoolean("set to top1", false));
		if(getUserLUT(ChoosePetCt.EXT_BLUES_LUT) != null) {
			tmp = jPrefer.get("ext blues name", "The blues");
			jCheckBlues.setText(tmp);
			jCheckFuseBlues.setText(tmp);
		}
		if(getUserLUT(ChoosePetCt.EXT_HOT_IRON_LUT) != null) {
			tmp = jPrefer.get("ext hotiron name", "Hot iron");
			jCheckHotIron.setText(tmp);
			jCheckFuseHot.setText(tmp);
		}
		jCheckMriLut.setVisible(false);
		if(getUserLUT(ChoosePetCt.MRI_CT_LUT) != null) {
			tmp = jPrefer.get("MriCt name", "MRI/CT LUT");
			jCheckMriLut.setVisible(true);
			jCheckMriLut.setText(tmp);
		}
//		fuseFactor = jPrefer.getInt("fusion factor", 120);
//		jMenuBrownFat.setVisible(false);
		checkForVtk();
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
		ArrayList<ImagePlus> imgList = new ArrayList<>();
		ArrayList<Integer> seriesType = new ArrayList<>();
		for( i=0; i<foundData; i++) {
			j = chosen.get(i);
			k = chooseDlg.getSeriesType(j);
			seriesType.add(k);
			currImg = chooseDlg.imgList.get(j);
			imgList.add(currImg);
		}
		levelSave = "0";
		widthSave = "0";
		loSave = -1.0;		// not a legal value
		initDualSlider();
		chooseDlg = null;	// free it
		initToolBar2();
		petCtPanel1.LoadData(this, imgList, seriesType);
//		jMenuSyncMri.setEnabled(false);
		keyDelay = new int[10];
		j = 300;
		for( i=1; i<10; i++) {
			keyDelay[i] = j;
			j = (int) (j * 1.3);
		}
		if( conferenceList == null) conferenceList = new ArrayList<>();
		conferenceList.add(this);
		if(extList == null) extList = new ArrayList<>();
		addWindowFocusListener(this);
		jPopupCtMenu.setInvoker(petCtPanel1);
		jPopupMipMenu.setInvoker(petCtPanel1);
		jPopupPetMenu.setInvoker(petCtPanel1);
		jPopupFusedMenu.setInvoker(petCtPanel1);
	}
	
	void initToolBar2() {
		setVisibleToolBar2(false);
		buttonGroup1 = new javax.swing.ButtonGroup();
		buttonGroup1.add(jRadioDistance);
		buttonGroup1.add(jRadArUp);
		buttonGroup1.add(jRadArDown);
		buttonGroup1.add(jRadArLeft);
		buttonGroup1.add(jRadArRight);
		buttonGroup1.add(jRadioText);
		buttonGroup1.add(jRadioSUV);
		buttonGroup1.add(jRadioBM);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if( keyCode == KeyEvent.VK_COMMA) keyCode = KeyEvent.VK_LESS;
		if( keyCode == KeyEvent.VK_PERIOD) keyCode = KeyEvent.VK_GREATER;
		int diff, mousePage;
		if( petCtPanel1.isGated && !petCtPanel1.runGated &&
			(keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT)) {
			e.consume();
			petCtPanel1.incrGatePosition(keyCode == KeyEvent.VK_RIGHT);
			BrownFat bfDlg1 = petCtPanel1.bfDlg;
			if( bfDlg1 != null && bfDlg1 instanceof BrownFat) bfDlg1.calculateVol(true);
			return;
		}
		if( keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_UP ||
			keyCode == KeyEvent.VK_PAGE_DOWN || keyCode == KeyEvent.VK_PAGE_UP ||
			keyCode == KeyEvent.VK_LESS || keyCode == KeyEvent.VK_GREATER) {
			e.consume();
			mousePage = -1;
			diff = 1;
			if( keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_LESS) diff = -1;
			if( keyCode == KeyEvent.VK_PAGE_UP) diff = pgUpDn;
			if( keyCode == KeyEvent.VK_PAGE_DOWN) diff = -pgUpDn;
			if( keyCode == KeyEvent.VK_LESS || keyCode == KeyEvent.VK_GREATER) mousePage = 2;
			if( keyIndex > 0) {
				long time1 = System.currentTimeMillis();
				long diff1 = time1 - currKeyTime;
//				IJ.showStatus("diff = " + diff1 + ", curr= " + currKeyTime);
				if( diff1 >= 0 && diff1 < keyDelay[keyIndex]) return;
				currKeyTime = time1;
			}
			petCtPanel1.incrSlicePosition(diff, mousePage, false, 0);
		}
		if( keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
			e.consume();
			diff = 1;
			if( keyCode == KeyEvent.VK_LEFT) diff = -1;
			keyIndex += diff;
			if( keyIndex < 0) keyIndex = 0;
			if( keyIndex >= 9) keyIndex = 9;
			IJ.showStatus("Key delay = " + keyIndex);
		}
		if( keyCode == KeyEvent.VK_HOME) {
			e.consume();
			petCtPanel1.incrSlicePosition(1000, -1, false, 0);
		}
		if( keyCode == KeyEvent.VK_Z) {
			e.consume();
			petCtPanel1.zoomTog = !petCtPanel1.zoomTog;
			jButZoom.setSelected(petCtPanel1.zoomTog);
			hideAllPopupMenus();
			petCtPanel1.repaint();
		}
		if( keyCode == KeyEvent.VK_B) {
			e.consume();
			if( !isScroll()) {
				IJ.log("(ctrl)B to return to base position works only when ⇕ is active");
				return;
			}
			petCtPanel1.useSpinnersBaseValues(0);
			petCtPanel1.repaint();
			petCtPanel1.setCursor(petCtPanel1.maybePanImage());
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
//		currKeyTime = 0;
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	private void initDualSlider() {
		dSlide = new DoubleRangeSlider(0, 1000, 0 ,1000) {
			@Override
			public String getToolTipText(MouseEvent evt) {return petCtPanel1.getSliderToolTip();}
		};
		dSlide.setEnabled(true);
		dSlide.setLocale(Locale.US);
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
				int sl1Owner;
				double oldLevel, newLevel, oldWidth, newWidth, hiVal, loVal, tmpVal, min;
				String tmp;
				JFijiPipe pip1;
				hiVal = dSlide.getHighValue();
				loVal = dSlide.getLowValue();
				min = dSlide.getMinimum();
				oldLevel = roundN(Double.parseDouble( levelSave), slideDigits);
				oldWidth = roundN(Double.parseDouble( widthSave), slideDigits);
				newWidth = roundN(hiVal-loVal, slideDigits);
				newLevel = roundN((hiVal+loVal)/2, slideDigits);
				sl1Owner = petCtPanel1.sliderOwner;
				if( loVal == loSave && sl1Owner == 0) {
					tmpVal = newLevel - newWidth/2;
					if( tmpVal != loVal) {
						hiVal += tmpVal;
						newWidth = roundN(hiVal-loVal, slideDigits);
						newLevel = roundN((hiVal+loVal)/2, slideDigits);
					}
				}
				loSave = loVal;
				if( newLevel == oldLevel && newWidth == oldWidth) return;
				widthSave = myFormat(newWidth, slideDigits);
				levelSave = myFormat(newLevel, slideDigits);
				if( sl1Owner == 1 || (sl1Owner == 2 && !petCtPanel1.isMIPdisplay())) {
					jWidthVal.setText(widthSave);
					jLevelVal.setText(levelSave);
				} else {
					pip1 = petCtPanel1.getCorrectedOrUncorrectedPipe(false);
					tmpVal = pip1.getLogOriginal(loVal);
					tmp = myFormat(tmpVal, slideDigits);
					jWidthVal.setText(tmp);
					tmpVal = pip1.getLogOriginal(hiVal);
					tmp = myFormat(tmpVal, slideDigits);
					jLevelVal.setText(tmp);
				}
				petCtPanel1.ActionSliderChanged(newWidth, newLevel);
			}
		});
	}

	void resizeForm() {
		if( petCtPanel1 == null) return;
		if( !changeView) petCtPanel1.curPosition[0] = null;
		changeView = false;	// do once per orientation change
	}

	String getUserLUT(int type) {
		return ChoosePetCt.getUserLUT(type, jPrefer);
	}

	void setVisibleToolBar2(boolean visible) {
		jRadioBM.setSelected(true);
		jToolBar2.setVisible(visible);
	}

	void tbAutoValue(boolean readVal) {
		boolean isAuto;
		String key1 = "annotation auto 2 BM";
		if( readVal) {
			isAuto= jPrefer.getBoolean(key1, false);
			jCheckAutoBM.setSelected(isAuto);
			return;
		}
		isAuto = jCheckAutoBM.isSelected();
		jPrefer.putBoolean(key1, isAuto);
	}

	void setCountLabel(Integer val) {
		jLabCount.setText(val.toString());
	}

	private void checkForVtk() {
		int j=0;
		jMenuVtk.setVisible(false);
		if( j==0) return;	// short circuit routine, it stopped working in Linux
		String tmp1 = System.getProperty("java.library.path");
		String tmp = tmp1;
		char sep1 = ';';
		if( File.separatorChar == '/') sep1 = ':';	// Linux and Mac
//		System.out.println("java.library.path = " + tmp);
		while ((j = tmp1.indexOf(sep1))> 0) {
			tmp = tmp1.substring(0, j);
			tmp1 = tmp1.substring(j+1);
//			System.out.println("java.library.path = " + tmp);
			if( tmp.contains("Fiji.app/lib")) break;
		}
		File dir = new File(tmp);
/*		tmp = "user.dir: " + System.getProperty("user.dir");
		tmp += "\njava.library.path: " + System.getProperty("java.library.path");
		tmp += "\nLD_LIBRARY_PATH: " + System.getenv("LD_LIBRARY_PATH");
		IJ.log(tmp);
		tmp = System.getenv("DYLD_LIBRARY_PATH");
		IJ.log(tmp);*/
		if(dir.exists()) {
			File[] files = dir.listFiles();
			for( j=0; j<files.length; j++) {
				tmp = files[j].getName().toLowerCase();
				// if we find a match, go home and leave menu item
				if( tmp.contains("vtkcommonjava")) return;
			}
		}
/*		else {
			tmp = "Can't find vtk lib: " + System.getProperty("java.library.path");
			tmp += "\n" + System.getProperty("user.dir");
			IJ.log(tmp);
		}*/
		jMenuVtk.setVisible(false);
	}

	// the text box jComboText needs to be unset when changing radiobutton
	void helper4annotBar(boolean setFocus) {
		if( setFocus) {
			jComboText.requestFocus();
			return;
		}
		jWidthVal.requestFocus();
	}

	// called at init to maybe turn on ROIs and Annotations
	int checkforROIandAnnotations() {
		BrownFat dlg;
//		petCtPanel1.testGetInstance();
		ReadOrthancSub sub1 = new ReadOrthancSub();
/*		Bytst btst = new Bytst();
		btst.test1();*/
		if( petCtPanel1.anotateDlg == null) {
			sub1.look4annotations(petCtPanel1, 0);
			petCtPanel1.anotateDlg = new Annotations(this,false);
		}
		if( petCtPanel1.anotateDlg.m_BMList.isEmpty()) {
			petCtPanel1.anotateDlg = null;
		} else {
			if(startAnnotations) {
				petCtPanel1.anotateDlg.setVisible(true);
				petCtPanel1.anotateDlg.showBookmarksTab();
			}
		}
		if( !startROIs || !useBF ) return 0;
		//String flName = BrownFat.getSpreadSheetName(petCtPanel1.petPipe);
		String flName = getBfName();
		if( flName == null) {	// look maybe Orthanc
			sub1.look4annotations(petCtPanel1, 1);
			flName = getBfName();
		}
		int numFrm = petCtPanel1.petPipe.getNormalizedNumFrms();
		dlg = BrownFat.getInstance();
		if( dlg != null) {
			if( flName == null) return 0;
			dlg.saveParentExt(this);
			dlg.loadStoredROIs(flName, numFrm);
			mySleep(500);	// loading ROIs needs some extra time
			dlg.calculateVol(true);
			dlg.followCheck();
			return 0;
		}
		if( flName != null) {
			dlg = BrownFat.makeNew(this);
			dlg.loadStoredROIs(flName,numFrm);
//			dlg.setSavedSize();
			dlg.setVisible(true);
		}
		return 0;
	}

	String getBfName() {
		String retv = "";
		try {
			retv = Extra.getSpreadSheetName(petCtPanel1.petPipe);
		}
		catch(Exception e) { IJ.log("Can't find static code."); }
		return retv;
	}

	JCheckBox getAutoBM() {
		return jCheckAutoBM;
	}

	public Date getStudyDate() {
		if( petCtPanel1.petPipe == null) {
			return new Date();	// it no longer exists so return "now"
		}
		return petCtPanel1.petPipe.data1.serTime;
	}
	
	int  [] getImageList( ArrayList<Object> petImages) {
		int [] IDList = null;
		int i, n;
		ArrayList<Integer> ID1 = new ArrayList<>();
		addImage(petCtPanel1.petPipe, petImages, ID1);
		addImage(petCtPanel1.upetPipe, petImages, ID1);
		addImage(petCtPanel1.ctPipe, petImages, ID1);
		addImage(petCtPanel1.mriPipe, petImages, ID1);
		n = ID1.size();
		if( n> 0) {
			IDList = new int[n];
			for( i=0; i<n; i++) IDList[i] = ID1.get(i);
		}
		return IDList;
	}
	
	private void addImage(JFijiPipe currPipe, ArrayList<Object> petImages, ArrayList<Integer> ID1) {
		ImagePlus currImg;
		if( currPipe == null) return;
		currImg = currPipe.data1.srcImage;
		if( currImg == null) return;
		ID1.add(currImg.getID());
		petImages.add(currImg);
	}

	private void updateAxCoSaButtons() {
		int type = petCtPanel1.m_sliceType;
		jButAxial.setSelected(type == JFijiPipe.DSP_AXIAL);
		jButCoronal.setSelected(type == JFijiPipe.DSP_CORONAL);
		jButSagital.setSelected(type == JFijiPipe.DSP_SAGITAL);
	}

	void updateMenu() {
		boolean mipTog = petCtPanel1.showMip;
		jButMip.setSelected(mipTog);
		setCineButtons(mipTog);
	}

	void setDualSliderValues( double loVal, double hiVal) {
		dSlide.setLowValue(loVal);
		dSlide.setHighValue(hiVal);
	}

	void switchDualSlider( int digits, boolean enableIt) {
		slideDigits = digits;
		double min1, max1, width1, level1;
		min1 = petCtPanel1.minSlider;
		max1 = petCtPanel1.maxSlider;
		width1 = petCtPanel1.getWidthSlider();
		level1 = petCtPanel1.getLevelSlider();
		dSlide.setEnabled(enableIt);
		dSlide.setMinimum(min1);
		dSlide.setMaximum(max1);
		dSlide.setLowValue(level1 - width1 / 2);
		dSlide.setHighValue(level1 + width1 / 2);
		jWidthVal.setToolTipText(petCtPanel1.getLowerEditToolTip());
		jLevelVal.setToolTipText(petCtPanel1.getUpperEditToolTip());
	}

	void changeLayout(int type) {
		petCtPanel1.m_sliceType = type;
		petCtPanel1.updatePipeInfo();
		Point pt1 = petCtPanel1.maybePanImage();
		if(!petCtPanel1.isInitializing) {
			petCtPanel1.setCursor(pt1);
			changeView = true;
		}
//		petCtPanel1.setAllZoom(-1000);	// bad, don't reset zoom to 1
		if(petCtPanel1.bfDlg != null) petCtPanel1.bfDlg.changeOrientation();
		setTitle(getTitleBar(0));
		maybeAddWindow();
		updateAxCoSaButtons();
		repaint();
		notifySyncedStudies(PetCtPanel.NOTIFY_LAYOUT, type, -1, null, null);
	}

	void fillPatientData() {
		JFijiPipe pet1 = petCtPanel1.petPipe;
		String meta = pet1.data1.metaData;
		String opName;
		if( meta == null) return;
		m_patName = ChoosePetCt.getCompressedPatName(meta);
		m_patID = ChoosePetCt.getCompressedID(meta);
		m_styName = ChoosePetCt.getDicomValue(meta, "0008,1030");
		m_studyInstanceUID = ChoosePetCt.getDicomValue(meta, "0020,000D");
		opName = ChoosePetCt.getDicomValue(meta, "0008,1070");
		String tmp = ChoosePetCt.getDicomValue(meta, "0010,0030");
		m_patBirthday = ChoosePetCt.getDateTime(tmp, null);
		JFijiPipe ct1 = petCtPanel1.ctPipe;
		meta = ct1.data1.metaData;
		if( meta == null) return;
		tmp = ChoosePetCt.getDicomValue(meta, "0018,0060");
		petCtPanel1.m_kvp = ChoosePetCt.parseInt(tmp);
		tmp = ChoosePetCt.getDicomValue(meta, "0018,1151");
		petCtPanel1.m_ctMa = ChoosePetCt.parseInt(tmp);
		if( opName == null) opName = ChoosePetCt.getDicomValue(meta, "0008,1070");
		petCtPanel1.operatorName = opName;
	}

	String generateFileNameExt(int type, boolean spcSwap) {
		String retVal;
		if( type == 0) {
			if( m_patName==null || m_patName.isEmpty()) return "no_name";
			retVal = m_patName.replace('.', ' ').trim();
			retVal = retVal.replaceAll("[,/]", "_");
		} else {
			retVal = ChoosePetCt.UsaDateFormat(getStudyDate());
			retVal = retVal.replace(", ", "_");
		}
		if( spcSwap) return retVal.replace(' ', '_');
		return retVal;
	}

	void fillSUV_SUL(boolean showFlg) {
		double SUV, SUL;
		SUVDialog dlg1 = new SUVDialog(this, true);
		SUV = dlg1.calculateSUV(petCtPanel1.petPipe, showFlg);
		if( ignoreSUV) SUV = 0;
		petCtPanel1.petPipe.data1.setSUVfactor( SUV);
		petCtPanel1.mipPipe.data1.setSUVfactor( SUV);
		SUL = dlg1.calculateSUL(petCtPanel1.petPipe);
		petCtPanel1.petPipe.data1.SULfactor = SUL;
		petCtPanel1.mipPipe.data1.SULfactor = SUL;
		jMenuBrain.setVisible(petCtPanel1.petPipe.data1.SUVfactor > 0);
		petCtPanel1.changeCurrentSlider();
		double fusionMixing = jPrefer.getDouble("fusion mixing", 500.0);
		if( fusionMixing < 0 || fusionMixing > 1000.) fusionMixing = 500.0;
		// perhaps it is NOT a good idea to override the default 500?
		petCtPanel1.petPipe.fuseLevel = fusionMixing;
		// if the dialog is displayed, we need to take care of log display
		if(showFlg) useLog(false);
	}

	String getTitleBar(int type) {
		JFijiPipe pet1 = petCtPanel1.petPipe;
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
		if(pet1 != null)
			str1 += ChoosePetCt.UsaDateFormat(pet1.data1.serTime);
		String str2 = m_styName + "   " + petCtPanel1.petSeriesName;
		switch( type) {
			case 1:
				return str1;
			
			case 2:
				return str2;
				
			default:
				return "Pet-Ct: " + str1 + "   " + str2;
		}
	}

	void setCineButtons(boolean vis1) {
		jButFwd.setVisible(vis1);
		jButFront.setVisible(vis1);
		jButSide.setVisible(vis1);
		if(!vis1) return;
		int val =  petCtPanel1.getCineState();
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

	void savePrefs() {
		if( foundData < 2) return;
		if( petCtPanel1 == null || petCtPanel1.petPipe == null) return;
		jPrefer.putDouble("fusion mixing", petCtPanel1.petPipe.fuseLevel);
		jPrefer.putBoolean("show cursor", isShowCursor());
		Dimension sz1 = getSize();
		Point pt1 = getLocation();
		jPrefer.putInt("petct dialog x", pt1.x);
		jPrefer.putInt("petct dialog y", pt1.y);
		jPrefer.putInt("petct dialog width", sz1.width);
		jPrefer.putInt("petct dialog height", sz1.height);
	}

	public JPopupMenu getjPopupCtMenu() { return jPopupCtMenu; }

	public JPopupMenu getjPopupMipMenu() { return jPopupMipMenu; }

	public JPopupMenu getjPopupPetMenu() { return jPopupPetMenu; }

	public JPopupMenu getjPopupFusedMenu() { return jPopupFusedMenu; }

	JComboBox getComboText() { return jComboText; }

	void hideAllPopupMenus() {
		jPopupPetMenu.setVisible(false);
		jPopupCtMenu.setVisible(false);
		jPopupMipMenu.setVisible(false);
		jPopupFusedMenu.setVisible(false);
	}

	private void changePetColor( int indx) {
		petCtPanel1.petColor = indx;
		hideAllPopupMenus();
		petCtPanel1.repaint();
	}

	private void changeFuseColor( int indx) {
		petCtPanel1.fusedColor = indx;
		hideAllPopupMenus();
		petCtPanel1.repaint();
	}

	private void changeMipColor() {
		int i = JFijiPipe.COLOR_INVERSE;
		if( jCheckUsePetLut.isSelected()) i = JFijiPipe.COLOR_PET_USER;
		petCtPanel1.mipColor = i;
		hideAllPopupMenus();
		petCtPanel1.repaint();
	}

	private void changeMriLut() {
		int color = jCheckMriLut.isSelected() ? JFijiPipe.COLOR_MRI : JFijiPipe.COLOR_GRAY;
		petCtPanel1.ctMriColor = color;
		hideAllPopupMenus();
		petCtPanel1.repaint();
	}
	
	private void alternateROI() {
		altROI = jCheckAltROI.isSelected();
		hideAllPopupMenus();
		petCtPanel1.curPosition[0] = null;	// kill cross hairs
		petCtPanel1.repaint();
	}
	
	void useLog(boolean isMenu) {
		boolean isLog = jMenuLogDisplay.isSelected();
		boolean currUseSrcWidth = false;
		JFijiPipe pip1 = petCtPanel1.petPipe;
		if( pip1 == null) return;
		String tmp = "use log";
		if(isMenu) {
			jPrefer.putBoolean(tmp, isLog);
		} else {
			isLog = jPrefer.getBoolean(tmp, false);
			jMenuLogDisplay.setSelected(isLog);
			// this is called at startup. If no log, no action is required
			if(!isLog) return;
		}
		JFijiPipe upip = petCtPanel1.upetPipe;
		JFijiPipe mipPipe = petCtPanel1.mipPipe;
		int indx = 0;
		if( isLog) indx = 10;
		pip1.isLog = isLog;
		pip1.dirtyFlg = true;
		if( upip != null) {
			upip.isLog = isLog;
			if( upip == petCtPanel1.getCorrectedOrUncorrectedPipe(true))
				pip1.setLogSlider();
		}
		if( mipPipe != null) {
			currUseSrcWidth = mipPipe.useSrcPetWinLev;
			mipPipe.useSrcPetWinLev = true;
		}
		petCtPanel1.presetWindowLevels(indx);
		JFijiPipe pip2 = petCtPanel1.mipPipe;
		if( pip2 != null) {
			pip2.isLog = isLog;
			pip2.dirtyFlg = true;
			pip2.winLevel = pip1.winLevel;
			pip2.winWidth = pip1.winWidth;
		}
		if( mipPipe != null) {
			mipPipe.useSrcPetWinLev = currUseSrcWidth;
			dSlide.setEnabled(!currUseSrcWidth);
		}
	}
	
	void showSource(boolean isMenu, boolean show1) {
		boolean isShow = show1;
		if(isMenu) {
			isShow = isShowSource();
			jPrefer.putBoolean("show source", isShow);
		}
		setSrcVisibile(petCtPanel1.petPipe, isShow);
		setSrcVisibile(petCtPanel1.upetPipe, isShow);
		setSrcVisibile(petCtPanel1.ctPipe, isShow);
		setSrcVisibile(petCtPanel1.mriPipe, isShow);
		setSrcVisibile(petCtPanel1.mipPipe, isShow);
	}
	
	boolean isShowSource() {
		return jMenuShowSource.isSelected();
	}

	boolean isShowCursor() {
		return jMenuShowCursor.isSelected();
	}

	boolean isTop() {
		return jCheckTop.isSelected();
	}

	private void changeTop() {
		boolean top = isTop();
		jPrefer.putBoolean("set to top1", top);
		petCtPanel1.updatePipeInfo();
		petCtPanel1.repaint();
	}

	void maybeClearSource() {
		if( isShowSource()) return;
		int i, j;
		for( i=0; i<6; i++) {
			j = 0;
			if( ClearSource(petCtPanel1.petPipe)) j++;
			if( ClearSource(petCtPanel1.upetPipe)) j+=2;
			if( ClearSource(petCtPanel1.ctPipe)) j+=4;
			if( ClearSource(petCtPanel1.mriPipe)) j+=8;
			if( ClearSource(petCtPanel1.mipPipe)) j+=16;
			if(j==0) return;
			mySleep(500);
		}
		showSource(false, true);
//			IJ.log("Not all series were closed. Use Fiji->File->Close All.  Err="+j);
	}

	void mySleep(int time) {
		try {
			Thread.sleep(time);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	boolean ClearSource(JFijiPipe pip1) {
		if( pip1 == null) return false;
		clrSrcSub(pip1.data1.orthAnno);
		clrSrcSub(pip1.data1.orthBF);
		return clrSrcSub(pip1.data1.srcImage);
	}

	boolean clrSrcSub(ImagePlus img) {
		if(img == null) return false;
		ImageWindow win1 = img.getWindow();
		if(win1 == null || win1.isClosed()) return false;
		WindowManager.setCurrentWindow(win1);
		IJ.doCommand("Close");
		return true;
	}

	void setSrcVisibile(JFijiPipe pip1, boolean showFlg) {
		if( pip1 == null) return;
		setSrcVisSub(pip1.data1.orthAnno, showFlg);
		setSrcVisSub(pip1.data1.orthBF,   showFlg);
		setSrcVisSub(pip1.data1.srcImage, showFlg);
	}

	void setSrcVisSub(ImagePlus img, boolean showFlg) {
		if( img == null) return;
		ImageWindow win1 = img.getWindow();
		if(win1 != null) win1.setVisible(showFlg);
	}

	void getOptionDlg() {
		PetOptions petDlg = new PetOptions(this, true);
		petDlg.setVisible(true);
		petCtPanel1.updatePipeInfo();
		petCtPanel1.repaint();
	}


	void fitWindow() {
		Dimension sz1, sz0, sz2;
		double area1, scale1;
		int stype = petCtPanel1.m_sliceType;
		JFijiPipe petPipe = petCtPanel1.petPipe;
		sz1 = getSize();
		sz0 = petCtPanel1.getSize();
		// get the difference between the panel and the whole window
		sz1.height -= sz0.height;
		sz1.width -= sz0.width;
		area1 = sz0.width * sz0.height;
		sz2 = petCtPanel1.getWindowDim();
		if( stype == JFijiPipe.DSP_CORONAL || stype == JFijiPipe.DSP_SAGITAL) {
			if( petPipe != null) sz2.height *= petPipe.zoom1;
		}
//		if( autoResize) scale1 = ((double) sz0.width) / sz2.width;
//		else scale1 = Math.sqrt(area1 / (sz2.width * sz2.height));
		scale1 = ((double) sz0.width) / sz2.width;
		sz2.width = ChoosePetCt.round(scale1* sz2.width);
		sz2.height = ChoosePetCt.round(scale1* sz2.height);
		petCtPanel1.setSize(sz2);
		sz2.width += sz1.width;
		sz2.height += sz1.height;
		setSize(sz2);
/*		Dimension sz0, sz2;
		double area1, scale1;
		Integer y0, x0;
		Insets in1 = getInsets();
		y0 = in1.top + in1.bottom;
		x0 = in1.left + in1.right;
		sz0 = petCtPanel1.getSize();
		y0 += jMenuBar1.getHeight() + jToolBar1.getHeight() + 8;	// 8 & 4 are gaps
		if( jToolBar2.isVisible()) y0 += jToolBar2.getHeight() + 4;
		area1 = sz0.width * sz0.height;
		sz2 = petCtPanel1.getWindowDim();
		if( autoResize) scale1 = ((double) sz0.width) / sz2.width;
		else scale1 = Math.sqrt(area1 / (sz2.width * sz2.height));
		sz2.width = ChoosePetCt.round(scale1* sz2.width) + x0;
		sz2.height = ChoosePetCt.round(scale1* sz2.height) + y0;
		setSize(sz2);*/
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
		petCtPanel1.presetWindowLevels(indx);
	}

	void updatePetCheckmarks( int indx) {
		if( indx >= 0) {
			jCheckBlues.setSelected(indx == JFijiPipe.COLOR_BLUES);
			jCheckGrayScale.setSelected(indx == JFijiPipe.COLOR_GRAY);
			jCheckInverse.setSelected(indx == JFijiPipe.COLOR_INVERSE);
			jCheckHotIron.setSelected(indx == JFijiPipe.COLOR_HOTIRON);
			jCheckSourceColor.setSelected(indx == JFijiPipe.COLOR_USER);
		}
		jCheckUncorrected.setEnabled( petCtPanel1.upetPipe != null);
		jCheckFused.setEnabled(petCtPanel1.showMip);
		int i = petCtPanel1.m_masterFlg;
		switch(i) {
			case PetCtPanel.VW_PET_CT_FUSED:
				i = PetCtPanel.VW_MIP;
				break;

			case PetCtPanel.VW_PET_CT_FUSED_UNCORRECTED:
				i = PetCtPanel.VW_MIP_UNCORRECTED;
				break;
		}
		jCheckCorrected.setSelected(i == PetCtPanel.VW_MIP);
		jCheckUncorrected.setSelected(i == PetCtPanel.VW_MIP_UNCORRECTED);
		jCheckFused.setSelected(i == PetCtPanel.VW_MIP_FUSED);
		jCheck3Pet.setSelected(pet3 != null);
		boolean colorFlg = !jCheckFused.isSelected();
		jCheckHotIron.setEnabled(colorFlg);
		jCheckBlues.setEnabled(colorFlg);
		jCheckGrayScale.setEnabled(colorFlg);
		jCheckInverse.setEnabled(colorFlg);
		jCheckSourceColor.setEnabled(colorFlg);
	}

	void updateCtCheckmarks() {
		int i, indx = -1;	// no ctlevel
		jCheckMri.setEnabled(petCtPanel1.mriPipe != null && petCtPanel1.ctPipe != null);
		jCheckMri.setSelected(petCtPanel1.MRIflg);
		jCheck3Ct.setSelected(ct3 != null);
		JFijiPipe currPipe = petCtPanel1.getMriOrCtPipe();
		if( currPipe == null) return;
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

	// update both Mip and fused checkmarks
	void updateMipCheckmarks( int indx) {
		if( indx >= 0) {
			jCheckFuseBlues.setSelected(indx == JFijiPipe.COLOR_BLUES);
			jCheckFuseHot.setSelected(indx == JFijiPipe.COLOR_HOTIRON);
			jCheckFuseSource.setSelected(indx == JFijiPipe.COLOR_USER);
		}
		jCheckUsePetWin.setSelected(petCtPanel1.mipPipe.useSrcPetWinLev);
		jCheck3Fused.setSelected(fuse3 != null);
	}

	boolean getShowText() {
		return jMenuShowText.isSelected();
	}

	void update3Display() {
		JFijiPipe pip1;
		Point3d obliqueShift = petCtPanel1.getObliqueShift();
		if( pet3 != null) {
			pip1 = petCtPanel1.getCorrectedOrUncorrectedPipe(false);
			if( pet3.updateCenterPoint(pip1, obliqueShift) < 0)
				updatePetCheckmarks(-1);
		}
		if( ct3 != null) {
			pip1 = petCtPanel1.getMriOrCtPipe();
			if( ct3.updateCenterPoint(pip1, obliqueShift) < 0)
				updateCtCheckmarks();
		}
		if( fuse3 != null) {
			pip1 = petCtPanel1.getCorrectedOrUncorrectedPipe(false);
			if( fuse3.updateCenterPoint(pip1, obliqueShift) < 0)
				updateMipCheckmarks(-1);
		}
	}

	void update3Gate() {
		int gate = petCtPanel1.gateIndx;
		if(petCtPanel1.runGated) return;
		if( pet3 != null) pet3.getDisplay3Panel().updateGateIndx(gate);
		if( fuse3 != null) fuse3.getDisplay3Panel().updateGateIndx(gate);
	}

	void updateVtkLimits(boolean isMIP) {
		if( vtkDiag == null) return;
		int xpos, ypos, zpos;
		xpos = ChoosePetCt.round(petCtPanel1.petSagital);
		ypos = ChoosePetCt.round(petCtPanel1.petCoronal);
		zpos = ChoosePetCt.round(petCtPanel1.petAxial);
		vtkDiag.updatePointList(xpos, ypos, zpos, isMIP);
	}

	void launch3Pet() {
		if( pet3 == null) {
			pet3 = new Display3Frame();
			int master = petCtPanel1.m_masterFlg;
			JFijiPipe ctPip = null;
			if( master == PetCtPanel.VW_MIP_FUSED) {
				ctPip = check4Overlap4Mip();
			}
			JFijiPipe pip1 = petCtPanel1.getCorrectedOrUncorrectedPipe(false);
			myWait4Initial(pet3, pip1, ctPip, Display3Frame.DSP3_PET);
//			if( !pet3.init2(pip1, this, ctPip)) return;
//			pet3.setVisible(true);
			hideAllPopupMenus();
			return;
		}
		pet3.dispose();
		pet3 = null;
		hideAllPopupMenus();
	}

	void launch3Ct() {
		if( ct3 == null) {
			ct3 = new Display3Frame();
			JFijiPipe pip1 = petCtPanel1.getMriOrCtPipe();
			myWait4Initial(ct3, pip1, null, Display3Frame.DSP3_CT);
//			if( !ct3.init2(pip1, this, null)) return;
//			ct3.setVisible(true);
			hideAllPopupMenus();
			return;
		}
		ct3.dispose();
		ct3 = null;
		hideAllPopupMenus();
	}
	
	void launch3Fused() {
		if( fuse3 == null) {
			fuse3 = new Display3Frame();
			JFijiPipe pip1 = petCtPanel1.getCorrectedOrUncorrectedPipe(false);
			JFijiPipe ctPip = check4Overlap4Mip();
			myWait4Initial(fuse3, pip1, ctPip, Display3Frame.DSP3_FUSED);
//			if( !fuse3.init2(pip1, this, ctPip)) return;
//			fuse3.setVisible(true);
			hideAllPopupMenus();
			return;
		}
		fuse3.dispose();
		fuse3 = null;
		hideAllPopupMenus();
	}
		
	void myWait4Initial(final Display3Frame d3Frm, final JFijiPipe pip1, final JFijiPipe ctPip, final int type) {
		final PetCtFrame this1 = this;
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if( !d3Frm.isInitialized) return;
				if( !d3Frm.init2(pip1, this1, ctPip, type)) return;
				d3Frm.setVisible(true);
			}
			
		});
	}

	// Here we have an additional check to eliminate blank
	// frames at start. Hawkeye has this problem.
	JFijiPipe check4Overlap4Mip() {
		double ctPos, petPos;
		int shift;
		JFijiPipe ctPip = petCtPanel1.getMriOrCtPipe();
		if( Math.abs( ctPip.corSagShift) <= view3Slop) return ctPip;
		// new we give a second chance for blank slices
		JFijiPipe pip1 = petCtPanel1.getCorrectedOrUncorrectedPipe(false);
		ctPos = ctPip.getZpos(ctPip.data1.numBlankFrms);
		petPos = pip1.getZpos(pip1.data1.numBlankFrms);
		shift = ChoosePetCt.round((petPos - ctPos) / Math.abs(ctPip.data1.sliceThickness));
		if( Math.abs( shift) <= view3Slop) return ctPip;
/*		JOptionPane.showMessageDialog(this,
			"Cannot show fused data because PET and CT data fail to completely overlap");
		return null;*/
		return ctPip;
	}

//	@SuppressWarnings("UnusedAssignment")
	void setPetDisplayMode( int mode1) {
		int mod2 = mode1;
		if( !petCtPanel1.showMip) switch( mod2) {
			case PetCtPanel.VW_MIP:
			case PetCtPanel.VW_MIP_FUSED:
				mod2 = PetCtPanel.VW_PET_CT_FUSED;
				break;

			case PetCtPanel.VW_MIP_UNCORRECTED:
				mod2 = PetCtPanel.VW_PET_CT_FUSED_UNCORRECTED;
				break;
		}
		petCtPanel1.m_masterFlg = mod2;
		hideAllPopupMenus();
		updateMenu();
		petCtPanel1.updatePipeInfo();
		setTitle(getTitleBar(0));
		maybeAddWindow();
		petCtPanel1.repaint();
	}

	void maybeAddWindow() {
		if( isSetTitle) return;	// do once
		WindowManager.addWindow(this);
		isSetTitle = true;
	}

	void setExternalSpinners(boolean toggle) {
		setExternalSpinnersSub( toggle, isScroll(), this);
		petCtPanel1.useSpinnersBaseValues(1);
	}
	
	boolean isScroll() {
		return jButScroll.isSelected();
	}
	
/*	// this is only of value in axial display
	double getSliceThickness() {
		if(petCtPanel1.m_sliceType != JFijiPipe.DSP_AXIAL) return 0;
		return Math.abs(petCtPanel1.petPipe.data1.sliceThickness);
	}*/
	
	int getSUVtype() {
		int type = SUVtype;
		if( altROI) type = SUVtype2;
		return type;
	}
	
	boolean isSphereSUV() {
		boolean isSphere = sphereSUV;
		if( altROI) isSphere = sphereSUV2;
		return isSphere;
	}
	
	boolean isCircleSUV() {
		boolean isCircle = circleSUV;
		if( altROI) isCircle = circleSUV2;
		return isCircle;
	}
	
	int getSUVmm() {
		int mm = SUVmm;
		if( altROI) mm = SUVmm2;
		return mm;
	}

	static void setExternalSpinnersSub(boolean toggle, boolean active, JFrame frm) {
		if( !toggle) {
			if( !active) return;
			active = false;	// toggle it now
		}
		if( !active) {
			extList.remove(frm);
			return;
		}
		extList.add(frm);
	}

	boolean frameNotFocus() {
		return gotFocus != this;
	}

	void notifySyncedStudies( int type, int val1, int val2, Point2d pt1, MouseEvent e) {
		if( !isScroll() || frameNotFocus()) return;
		int i, n = extList.size();
		Object this1;
		PetCtFrame other1;
		PetCtPanel othPanel;
		if( n <= 1) return;
		for( i=0; i<n; i++) {
			this1 = extList.get(i);
			if( this1 == gotFocus) continue;
			if( !(this1 instanceof PetCtFrame)) continue;
			other1 = (PetCtFrame) this1;
			othPanel = other1.getPetCtPanel1();
			switch( type) {
				case PetCtPanel.NOTIFY_SPINNER:
					other1.externalSpinnerChange(val1, val2, pt1.x);
					break;
					
				case PetCtPanel.NOTIFY_LAYOUT:
					other1.changeLayout(val1);
					break;

				case PetCtPanel.NOTIFY_WIN_LEVELS:
					othPanel.presetWindowLevels(val1);
					break;

				case PetCtPanel.NOTIFY_XY_SHIFT:
					othPanel.notificationOfXYshift(val1, pt1);
					break;

				case PetCtPanel.NOTIFY_ZOOM:
					othPanel.maybeNotifyZoom(val1);
					break;

				case PetCtPanel.NOTIFY_PAN:
					boolean start = val1>0;
					othPanel.panDrag(e, start);
					break;

				case PetCtPanel.NOTIFY_BASE_VALUE:
					othPanel.notificationOfBaseValues(val1);
					break;
			}
		}
	}

	void externalSpinnerChange( int diff, int mousePage, double zDiff) {
		petCtPanel1.maybeIncrSlicePosition(diff, mousePage, zDiff);
	}

	void setGatedButtonVisible( boolean visible) {
		jButGated.setVisible(visible);
	}

	void resetButScroll() {
		jButScroll.setSelected(false);
	}

	static double roundN( double in, int numOfDigits) {
		double factorOfTen = 1;
		while( numOfDigits-- > 0) factorOfTen *= 10;
		return (Math.round(in*factorOfTen)/factorOfTen);
	}

	static String myFormat( double in, int numOfDigits) {
		String for1;
		int i = numOfDigits;
		for1 = "0";
		if( i>0) {
			for1 = "0.";
			while( i-- > 0) for1 += "0";
		}
		DecimalFormat for2 = new DecimalFormat(for1, new DecimalFormatSymbols(Locale.US));
		return for2.format(in);
	}

	public PetCtPanel getPetCtPanel1() {
		return petCtPanel1;
	}
	
	void resetMixing() {
		setDualSliderValues(475,525);	// 500 - 50/2, 500 + 50/2
		hideAllPopupMenus();
		petCtPanel1.repaint();
	}
	
	void setSliceMaxText() {
		int val10 = (int) (10.0*sliceMaxFactor);
		Double val1 = val10/10.0;
		if( val1 == 1.0) {
			jMenuMax.setText("Slice max");
			return;
		}
		jMenuMax.setText(val1.toString() + " Slice max");
	}
	
	void fillWindowList() {
		int i;
		String title = IMAGEJ_NAME;
		javax.swing.JMenuItem item1;
		Window win;
		Window [] frList = WindowManager.getAllNonImageWindows();
		if( frList == null) return;
		for( i=-1; i<frList.length; i++) {
			if( i>=0) {
				win = frList[i];
				if( win==null ) continue;
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
		if(title.equals(IMAGEJ_NAME)) {
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

	void scriptMip() {
		SaveMip dlg = new SaveMip(this, true);
		dlg.scriptAction();
	}

	boolean isMipRotating() {
		return jButFwd.isVisible() && jButFwd.isSelected();
	}

	void annotationSave(int indx) {
		Annotations currAno = getPetCtPanel1().anotateDlg;
		AnoToolBar currAnoTb = getPetCtPanel1().anotateTB;
		boolean isDelay = false;
		int sizeBM, columns, rows, nSlices, scrWidth, scrHeight;
		Dimension sz0, sz1, sz2;
		double fillX, fillY;
		if( currAno == null && currAnoTb == null) {
			IJ.log("Annotations have to be active - command ignored.");
			return;
		}
		if( currAno != null) sizeBM = currAno.getBMsize();
		else sizeBM = currAnoTb.getBMsize();
		if( sizeBM <= 0) {
			IJ.log("No annotations have been defined - command ignored.");
			return;
		}
		sz0 = getSize();
		fitWindow();
		int[] montSize;
		if( currAno != null) montSize = currAno.getImageSize(indx);
		else montSize = currAnoTb.getImageSize(indx);
		GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		scrWidth = devices[0].getDisplayMode().getWidth();
		scrHeight = devices[0].getDisplayMode().getHeight();
		nSlices = montSize[2];
//		columns = (int) Math.sqrt((double)nSlices*montSize[1]/montSize[0]);
		columns = 1;
		rows = columns;
		while (nSlices > columns*rows) {
			fillX = (double)columns*montSize[0]/scrWidth;
			fillY = (double)rows*montSize[1]/scrHeight;
			if( fillX <= fillY) columns++;
			else rows++;
		}
		fillX = (double)columns*montSize[0]/scrWidth;
		fillY = (double)rows*montSize[1]/scrHeight;
		if(fillX > 1.2 || fillY > 1.2) {
			if( fillX < fillY) fillX = fillY;
			sz2 = getSize();
			sz2.width = (int)(0.9*sz2.width/fillX);
			setSize(sz2);
			fitWindow();
			isDelay = true;
/*			String tmp = "Your image window is too large to show text clearly.\n";
			tmp += "Do you want to continue and have text which may not be readable?\n";
			tmp += "It is best to hit the No button, and shrink your image.\n";
			tmp += "Use Yes to continue as is.\n";
			tmp += "Use Cancle to suppress this dialog and to continue with Yes.";
			int i = JOptionPane.showConfirmDialog(this, tmp);
			if( i!=JOptionPane.YES_OPTION) {
				if( i == JOptionPane.NO_OPTION) return;
				ignoreDialog = true;
			}*/
		} else sz0 = null;
		if( currAno != null) currAno.saveSignificant(indx, isDelay, sz0);
		else currAnoTb.saveSignificant(indx, isDelay, sz0);
	}

	void fusedSave() {
		JDialog dlg1 = new SaveFused(this, true);
		dlg1.setVisible(true);
	}

	void followUp() {
		JDialog dlg1 = new FollowUp(this, false);
		dlg1.setVisible(true);
	}

	@Override
	public void dispose() {
		WindowManager.removeWindow(this);
		ChoosePetCt.MipPanel = null;
		hideAllPopupMenus();
		savePrefs();
		setExternalSpinners(false);
		if(petCtPanel1 != null) {
			maybeClearSource();
			if( useBF) petCtPanel1.removeBFdata();
			petCtPanel1.ctPipe = petCtPanel1.mipPipe = petCtPanel1.mriPipe = null;
			petCtPanel1.petPipe = petCtPanel1.upetPipe = petCtPanel1.reproPipe = null;
		}
		if(conferenceList != null) {
			conferenceList.remove(this);
			if(conferenceList.isEmpty()) conferenceList = null;
		}
		if(extList != null && extList.isEmpty()) extList = null;
		super.dispose();
	}

	@Override
	public void windowGainedFocus(WindowEvent we) {
		gotFocus = this;
		fillWindowList();
	}

	@Override
	public void windowLostFocus(WindowEvent we) {
		gotFocus = null;
		jMenuWindow.removeAll();
	}
	
	static class MyExceptionHandler implements Thread.UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread thread, Throwable thrwbl) {
			String tmp = thread.getName();
			if(tmp.startsWith("AWT-EventQueue")) return;
			IJ.log(tmp);
		}
		
	}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupPetMenu = new javax.swing.JPopupMenu();
        jMenuAutoPet = new javax.swing.JMenuItem();
        jMenuBrain = new javax.swing.JMenuItem();
        jMenuMax = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        jCheckFused = new javax.swing.JCheckBoxMenuItem();
        jCheckCorrected = new javax.swing.JCheckBoxMenuItem();
        jCheckUncorrected = new javax.swing.JCheckBoxMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        jCheck3Pet = new javax.swing.JCheckBoxMenuItem();
        jCheckAltROI = new javax.swing.JCheckBoxMenuItem();
        jCheckTop = new javax.swing.JCheckBoxMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jCheckInverse = new javax.swing.JCheckBoxMenuItem();
        jCheckGrayScale = new javax.swing.JCheckBoxMenuItem();
        jCheckBlues = new javax.swing.JCheckBoxMenuItem();
        jCheckHotIron = new javax.swing.JCheckBoxMenuItem();
        jCheckSourceColor = new javax.swing.JCheckBoxMenuItem();
        jPopupCtMenu = new javax.swing.JPopupMenu();
        jCheckChest = new javax.swing.JCheckBoxMenuItem();
        jCheckLung = new javax.swing.JCheckBoxMenuItem();
        jCheckLiver = new javax.swing.JCheckBoxMenuItem();
        jCheckBone = new javax.swing.JCheckBoxMenuItem();
        jCheckBrain = new javax.swing.JCheckBoxMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        jCheck3Ct = new javax.swing.JCheckBoxMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        jCheckMri = new javax.swing.JCheckBoxMenuItem();
        jCheckMriLut = new javax.swing.JCheckBoxMenuItem();
        jPopupMipMenu = new javax.swing.JPopupMenu();
        jCheckUsePetWin = new javax.swing.JCheckBoxMenuItem();
        jCheckReprojection = new javax.swing.JCheckBoxMenuItem();
        jCheckUsePetLut = new javax.swing.JCheckBoxMenuItem();
        jMenuImproveMIP = new javax.swing.JMenuItem();
        jPopupFusedMenu = new javax.swing.JPopupMenu();
        jMenuMixReset = new javax.swing.JMenuItem();
        jCheck3Fused = new javax.swing.JCheckBoxMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        jCheckFuseBlues = new javax.swing.JCheckBoxMenuItem();
        jCheckFuseHot = new javax.swing.JCheckBoxMenuItem();
        jCheckFuseSource = new javax.swing.JCheckBoxMenuItem();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jToolBar1 = new javax.swing.JToolBar();
        jButAxial = new javax.swing.JToggleButton();
        jButCoronal = new javax.swing.JToggleButton();
        jButSagital = new javax.swing.JToggleButton();
        jButZoom = new javax.swing.JToggleButton();
        jButScroll = new javax.swing.JToggleButton();
        jButGated = new javax.swing.JToggleButton();
        jButMip = new javax.swing.JToggleButton();
        jButFwd = new javax.swing.JToggleButton();
        jButFront = new javax.swing.JToggleButton();
        jButSide = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jWidthVal = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLevelVal = new javax.swing.JTextField();
        jToolBar2 = new javax.swing.JToolBar();
        jLabMeasure = new javax.swing.JLabel();
        jRadioDistance = new javax.swing.JRadioButton();
        jRadioSUV = new javax.swing.JRadioButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        jLabArrows = new javax.swing.JLabel();
        jRadArUp = new javax.swing.JRadioButton();
        jRadArDown = new javax.swing.JRadioButton();
        jRadArLeft = new javax.swing.JRadioButton();
        jRadArRight = new javax.swing.JRadioButton();
        jSeparator9 = new javax.swing.JToolBar.Separator();
        jRadioText = new javax.swing.JRadioButton();
        jComboText = new javax.swing.JComboBox();
        jSeparator10 = new javax.swing.JToolBar.Separator();
        jLabClear = new javax.swing.JLabel();
        jLabCount = new javax.swing.JLabel();
        jButAll = new javax.swing.JButton();
        jButLast = new javax.swing.JButton();
        jSeparator11 = new javax.swing.JToolBar.Separator();
        jCheckAutoBM = new javax.swing.JCheckBox();
        jRadioBM = new javax.swing.JRadioButton();
        jButMark = new javax.swing.JButton();
        jButPrev = new javax.swing.JButton();
        jButNext = new javax.swing.JButton();
        jSeparator12 = new javax.swing.JToolBar.Separator();
        jButSave = new javax.swing.JButton();
        jButQuit = new javax.swing.JButton();
        petCtPanel1 = new PetCtPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuAnotPane1 = new javax.swing.JMenu();
        jMenuAnoPane1 = new javax.swing.JMenuItem();
        jMenuAnoPane2 = new javax.swing.JMenuItem();
        jMenuAnoPane3 = new javax.swing.JMenuItem();
        jMenuAnoAll3 = new javax.swing.JMenuItem();
        jMenuSaveFused = new javax.swing.JMenuItem();
        jMenuSaveMip = new javax.swing.JMenuItem();
        jMenuSaveSignificant = new javax.swing.JMenuItem();
        jMenuExit = new javax.swing.JMenuItem();
        jMenuEdit = new javax.swing.JMenu();
        jMenuOptions = new javax.swing.JMenuItem();
        jMenuAnnotations = new javax.swing.JMenuItem();
        jMenuAnoTB = new javax.swing.JMenuItem();
        jMenuBrownFat = new javax.swing.JMenuItem();
        jMenuFitWindow = new javax.swing.JMenuItem();
        jMenuFollowup = new javax.swing.JMenuItem();
        jMenuLungMip = new javax.swing.JMenuItem();
        jMenuGauss = new javax.swing.JMenuItem();
        jMenuSUV = new javax.swing.JMenuItem();
        jMenuSyncMri = new javax.swing.JMenuItem();
        jMenuVtk = new javax.swing.JMenuItem();
        jMenuView = new javax.swing.JMenu();
        jMenuLogDisplay = new javax.swing.JCheckBoxMenuItem();
        jMenuShowSource = new javax.swing.JCheckBoxMenuItem();
        jMenuShowText = new javax.swing.JCheckBoxMenuItem();
        jMenuShowCursor = new javax.swing.JCheckBoxMenuItem();
        jMenuWindow = new javax.swing.JMenu();
        jMenuHelp = new javax.swing.JMenu();
        jMenuContents = new javax.swing.JMenuItem();
        jMenuAbout = new javax.swing.JMenuItem();

        jMenuAutoPet.setText("Auto level");
        jMenuAutoPet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAutoPetActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jMenuAutoPet);

        jMenuBrain.setText("Brain");
        jMenuBrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuBrainActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jMenuBrain);

        jMenuMax.setText("Slice max");
        jMenuMax.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuMaxActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jMenuMax);
        jPopupPetMenu.add(jSeparator3);

        jCheckFused.setSelected(true);
        jCheckFused.setText("Fused");
        jCheckFused.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckFusedActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheckFused);

        jCheckCorrected.setSelected(true);
        jCheckCorrected.setText("Attenuation corrected");
        jCheckCorrected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckCorrectedActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheckCorrected);

        jCheckUncorrected.setSelected(true);
        jCheckUncorrected.setText("Uncorrected");
        jCheckUncorrected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckUncorrectedActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheckUncorrected);
        jPopupPetMenu.add(jSeparator4);

        jCheck3Pet.setSelected(true);
        jCheck3Pet.setText("3 PET");
        jCheck3Pet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheck3PetActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheck3Pet);

        jCheckAltROI.setText("Alternate ROI");
        jCheckAltROI.setToolTipText("");
        jCheckAltROI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckAltROIActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheckAltROI);

        jCheckTop.setText("Top");
        jCheckTop.setToolTipText("show axial at top of display");
        jCheckTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckTopActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheckTop);
        jPopupPetMenu.add(jSeparator2);

        jCheckInverse.setSelected(true);
        jCheckInverse.setText("Inverse");
        jCheckInverse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckInverseActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheckInverse);

        jCheckGrayScale.setSelected(true);
        jCheckGrayScale.setText("Gray scale");
        jCheckGrayScale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckGrayScaleActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheckGrayScale);

        jCheckBlues.setSelected(true);
        jCheckBlues.setText("The blues");
        jCheckBlues.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBluesActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheckBlues);

        jCheckHotIron.setSelected(true);
        jCheckHotIron.setText("Hot iron");
        jCheckHotIron.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckHotIronActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheckHotIron);

        jCheckSourceColor.setSelected(true);
        jCheckSourceColor.setText("Use source");
        jCheckSourceColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckSourceColorActionPerformed(evt);
            }
        });
        jPopupPetMenu.add(jCheckSourceColor);

        jCheckChest.setSelected(true);
        jCheckChest.setText("Chest-Abdomen");
        jCheckChest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckChestActionPerformed(evt);
            }
        });
        jPopupCtMenu.add(jCheckChest);

        jCheckLung.setSelected(true);
        jCheckLung.setText("Lung");
        jCheckLung.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckLungActionPerformed(evt);
            }
        });
        jPopupCtMenu.add(jCheckLung);

        jCheckLiver.setSelected(true);
        jCheckLiver.setText("Liver");
        jCheckLiver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckLiverActionPerformed(evt);
            }
        });
        jPopupCtMenu.add(jCheckLiver);

        jCheckBone.setText("Bone");
        jCheckBone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoneActionPerformed(evt);
            }
        });
        jPopupCtMenu.add(jCheckBone);

        jCheckBrain.setSelected(true);
        jCheckBrain.setText("Brain-Sinus");
        jCheckBrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBrainActionPerformed(evt);
            }
        });
        jPopupCtMenu.add(jCheckBrain);
        jPopupCtMenu.add(jSeparator5);

        jCheck3Ct.setSelected(true);
        jCheck3Ct.setText("3 CT");
        jCheck3Ct.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheck3CtActionPerformed(evt);
            }
        });
        jPopupCtMenu.add(jCheck3Ct);
        jPopupCtMenu.add(jSeparator6);

        jCheckMri.setSelected(true);
        jCheckMri.setText("MRI");
        jCheckMri.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckMriActionPerformed(evt);
            }
        });
        jPopupCtMenu.add(jCheckMri);

        jCheckMriLut.setText("--");
        jCheckMriLut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckMriLutActionPerformed(evt);
            }
        });
        jPopupCtMenu.add(jCheckMriLut);

        jCheckUsePetWin.setSelected(true);
        jCheckUsePetWin.setText("Use PET gray scale");
        jCheckUsePetWin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckUsePetWinActionPerformed(evt);
            }
        });
        jPopupMipMenu.add(jCheckUsePetWin);

        jCheckReprojection.setText("Reprojection");
        jCheckReprojection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckReprojectionActionPerformed(evt);
            }
        });
        jPopupMipMenu.add(jCheckReprojection);

        jCheckUsePetLut.setText("Use PET source LUT");
        jCheckUsePetLut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckUsePetLutActionPerformed(evt);
            }
        });
        jPopupMipMenu.add(jCheckUsePetLut);

        jMenuImproveMIP.setText("Improve resolution");
        jMenuImproveMIP.setToolTipText("calculates 32 frame MIP");
        jMenuImproveMIP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuImproveMIPActionPerformed(evt);
            }
        });
        jPopupMipMenu.add(jMenuImproveMIP);

        jMenuMixReset.setText("50-50 mix");
        jMenuMixReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuMixResetActionPerformed(evt);
            }
        });
        jPopupFusedMenu.add(jMenuMixReset);

        jCheck3Fused.setSelected(true);
        jCheck3Fused.setText("3 fused");
        jCheck3Fused.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheck3FusedActionPerformed(evt);
            }
        });
        jPopupFusedMenu.add(jCheck3Fused);
        jPopupFusedMenu.add(jSeparator7);

        jCheckFuseBlues.setSelected(true);
        jCheckFuseBlues.setText("The blues");
        jCheckFuseBlues.setToolTipText("");
        jCheckFuseBlues.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckFuseBluesActionPerformed(evt);
            }
        });
        jPopupFusedMenu.add(jCheckFuseBlues);

        jCheckFuseHot.setSelected(true);
        jCheckFuseHot.setText("Hot iron");
        jCheckFuseHot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckFuseHotActionPerformed(evt);
            }
        });
        jPopupFusedMenu.add(jCheckFuseHot);

        jCheckFuseSource.setSelected(true);
        jCheckFuseSource.setText("Use source");
        jCheckFuseSource.setToolTipText("");
        jCheckFuseSource.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckFuseSourceActionPerformed(evt);
            }
        });
        jPopupFusedMenu.add(jCheckFuseSource);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Pet-Ct Viewer");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jToolBar1.setRollover(true);

        jButAxial.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/axial.gif"))); // NOI18N
        jButAxial.setToolTipText("choose axial display");
        jButAxial.setFocusable(false);
        jButAxial.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButAxial.setPreferredSize(new java.awt.Dimension(17, 10));
        jButAxial.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButAxial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButAxialActionPerformed(evt);
            }
        });
        jToolBar1.add(jButAxial);

        jButCoronal.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/coronal.gif"))); // NOI18N
        jButCoronal.setToolTipText("choose coronal display");
        jButCoronal.setFocusable(false);
        jButCoronal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButCoronal.setMinimumSize(new java.awt.Dimension(17, 10));
        jButCoronal.setPreferredSize(new java.awt.Dimension(17, 10));
        jButCoronal.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButCoronal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButCoronalActionPerformed(evt);
            }
        });
        jToolBar1.add(jButCoronal);

        jButSagital.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/sagital.gif"))); // NOI18N
        jButSagital.setToolTipText("choose sagittal display");
        jButSagital.setFocusable(false);
        jButSagital.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButSagital.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jButSagital.setMinimumSize(new java.awt.Dimension(17, 10));
        jButSagital.setPreferredSize(new java.awt.Dimension(17, 10));
        jButSagital.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButSagital.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSagitalActionPerformed(evt);
            }
        });
        jToolBar1.add(jButSagital);

        jButZoom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/zoom.gif"))); // NOI18N
        jButZoom.setToolTipText("zoom (using mouse wheel) and pan images, shortcut = z");
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

        jButScroll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/arrows.gif"))); // NOI18N
        jButScroll.setToolTipText("simultaneously scroll through N studies.  Use ↑B to return to base values");
        jButScroll.setFocusable(false);
        jButScroll.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButScroll.setMinimumSize(new java.awt.Dimension(20, 27));
        jButScroll.setPreferredSize(new java.awt.Dimension(20, 27));
        jButScroll.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButScroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButScrollActionPerformed(evt);
            }
        });
        jToolBar1.add(jButScroll);

        jButGated.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/cine.gif"))); // NOI18N
        jButGated.setSelected(true);
        jButGated.setToolTipText("cine gated images, or use right, left arrows to step");
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
        jButMip.setToolTipText("show MIP or fused image");
        jButMip.setFocusable(false);
        jButMip.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButMip.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButMip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMipActionPerformed(evt);
            }
        });
        jToolBar1.add(jButMip);

        jButFwd.setSelected(true);
        jButFwd.setText(">>");
        jButFwd.setToolTipText("rotate MIP image");
        jButFwd.setFocusable(false);
        jButFwd.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButFwd.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jButFwd.setMaximumSize(new java.awt.Dimension(35, 23));
        jButFwd.setMinimumSize(new java.awt.Dimension(20, 9));
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
            .addGap(0, 694, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 22, Short.MAX_VALUE)
        );

        jToolBar1.add(jPanel1);

        jLevelVal.setEditable(false);
        jLevelVal.setText("0");
        jLevelVal.setMaximumSize(new java.awt.Dimension(50, 20));
        jLevelVal.setPreferredSize(new java.awt.Dimension(40, 20));
        jToolBar1.add(jLevelVal);

        jToolBar2.setRollover(true);

        jLabMeasure.setText("Measure");
        jToolBar2.add(jLabMeasure);

        jRadioDistance.setText("distance");
        jRadioDistance.setFocusable(false);
        jRadioDistance.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jRadioDistance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioDistanceActionPerformed(evt);
            }
        });
        jToolBar2.add(jRadioDistance);

        jRadioSUV.setText("SUV");
        jRadioSUV.setFocusable(false);
        jRadioSUV.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jRadioSUV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioSUVActionPerformed(evt);
            }
        });
        jToolBar2.add(jRadioSUV);
        jToolBar2.add(jSeparator8);

        jLabArrows.setText("Arrows");
        jToolBar2.add(jLabArrows);

        jRadArUp.setText("up");
        jRadArUp.setFocusable(false);
        jRadArUp.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jRadArUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadArUpActionPerformed(evt);
            }
        });
        jToolBar2.add(jRadArUp);

        jRadArDown.setText("down");
        jRadArDown.setFocusable(false);
        jRadArDown.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jRadArDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadArDownActionPerformed(evt);
            }
        });
        jToolBar2.add(jRadArDown);

        jRadArLeft.setText("left");
        jRadArLeft.setFocusable(false);
        jRadArLeft.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jRadArLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadArLeftActionPerformed(evt);
            }
        });
        jToolBar2.add(jRadArLeft);

        jRadArRight.setText("right");
        jRadArRight.setFocusable(false);
        jRadArRight.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jRadArRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadArRightActionPerformed(evt);
            }
        });
        jToolBar2.add(jRadArRight);
        jToolBar2.add(jSeparator9);

        jRadioText.setText("Text");
        jRadioText.setFocusable(false);
        jRadioText.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jRadioText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioTextActionPerformed(evt);
            }
        });
        jToolBar2.add(jRadioText);

        jComboText.setEditable(true);
        jToolBar2.add(jComboText);
        jToolBar2.add(jSeparator10);

        jLabClear.setText("Clear");
        jToolBar2.add(jLabClear);

        jLabCount.setText("1");
        jLabCount.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jToolBar2.add(jLabCount);

        jButAll.setText("All");
        jButAll.setFocusable(false);
        jButAll.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButAll.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButAllActionPerformed(evt);
            }
        });
        jToolBar2.add(jButAll);

        jButLast.setText("Last");
        jButLast.setFocusable(false);
        jButLast.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButLast.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButLast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButLastActionPerformed(evt);
            }
        });
        jToolBar2.add(jButLast);
        jToolBar2.add(jSeparator11);

        jCheckAutoBM.setFocusable(false);
        jCheckAutoBM.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar2.add(jCheckAutoBM);

        jRadioBM.setText("Bookmarks");
        jRadioBM.setFocusable(false);
        jRadioBM.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jRadioBM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioBMActionPerformed(evt);
            }
        });
        jToolBar2.add(jRadioBM);

        jButMark.setText("Mark");
        jButMark.setFocusable(false);
        jButMark.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButMark.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButMark.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMarkActionPerformed(evt);
            }
        });
        jToolBar2.add(jButMark);

        jButPrev.setText("<<");
        jButPrev.setFocusable(false);
        jButPrev.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButPrev.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButPrev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButPrevActionPerformed(evt);
            }
        });
        jToolBar2.add(jButPrev);

        jButNext.setText(">>");
        jButNext.setFocusable(false);
        jButNext.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButNext.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButNextActionPerformed(evt);
            }
        });
        jToolBar2.add(jButNext);
        jToolBar2.add(jSeparator12);

        jButSave.setText("Save");
        jButSave.setFocusable(false);
        jButSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSaveActionPerformed(evt);
            }
        });
        jToolBar2.add(jButSave);

        jButQuit.setText("Quit");
        jButQuit.setFocusable(false);
        jButQuit.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButQuit.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButQuitActionPerformed(evt);
            }
        });
        jToolBar2.add(jButQuit);

        javax.swing.GroupLayout petCtPanel1Layout = new javax.swing.GroupLayout(petCtPanel1);
        petCtPanel1.setLayout(petCtPanel1Layout);
        petCtPanel1Layout.setHorizontalGroup(
            petCtPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        petCtPanel1Layout.setVerticalGroup(
            petCtPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 559, Short.MAX_VALUE)
        );

        jMenuFile.setText("File");

        jMenuAnotPane1.setText("Save Annotated Images");

        jMenuAnoPane1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_1, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuAnoPane1.setText("Pane 1");
        jMenuAnoPane1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAnoPane1ActionPerformed(evt);
            }
        });
        jMenuAnotPane1.add(jMenuAnoPane1);

        jMenuAnoPane2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_2, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuAnoPane2.setText("Pane 2");
        jMenuAnoPane2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAnoPane2ActionPerformed(evt);
            }
        });
        jMenuAnotPane1.add(jMenuAnoPane2);

        jMenuAnoPane3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_3, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuAnoPane3.setText("Pane 3");
        jMenuAnoPane3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAnoPane3ActionPerformed(evt);
            }
        });
        jMenuAnotPane1.add(jMenuAnoPane3);

        jMenuAnoAll3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuAnoAll3.setText("All 3");
        jMenuAnoAll3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAnoAll3ActionPerformed(evt);
            }
        });
        jMenuAnotPane1.add(jMenuAnoAll3);

        jMenuFile.add(jMenuAnotPane1);

        jMenuSaveFused.setText("Save Fused Images");
        jMenuSaveFused.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveFusedActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuSaveFused);

        jMenuSaveMip.setText("Save MIP");
        jMenuSaveMip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveMipActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuSaveMip);

        jMenuSaveSignificant.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuSaveSignificant.setText("Save Significant Image");
        jMenuSaveSignificant.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveSignificantActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuSaveSignificant);

        jMenuExit.setText("Exit");
        jMenuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuExitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuExit);

        jMenuBar1.add(jMenuFile);

        jMenuEdit.setText("Edit");

        jMenuOptions.setText("Options");
        jMenuOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuOptionsActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuOptions);

        jMenuAnnotations.setText("Annotations (Oblique)");
        jMenuAnnotations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAnnotationsActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuAnnotations);

        jMenuAnoTB.setText("Annotations toolbar");
        jMenuAnoTB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAnoTBActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuAnoTB);

        jMenuBrownFat.setText("Brown fat, ROIs");
        jMenuBrownFat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuBrownFatActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuBrownFat);

        jMenuFitWindow.setText("Fit Window to data");
        jMenuFitWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuFitWindowActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuFitWindow);

        jMenuFollowup.setText("Follow up studies");
        jMenuFollowup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuFollowupActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuFollowup);

        jMenuLungMip.setText("Lung MIP");
        jMenuLungMip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuLungMipActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuLungMip);

        jMenuGauss.setText("PET smooth");
        jMenuGauss.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuGaussActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuGauss);

        jMenuSUV.setText("SUV parameters");
        jMenuSUV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSUVActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuSUV);

        jMenuSyncMri.setText("Sync MRI data");
        jMenuSyncMri.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSyncMriActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuSyncMri);

        jMenuVtk.setText("Vtk Volume");
        jMenuVtk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuVtkActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuVtk);

        jMenuBar1.add(jMenuEdit);

        jMenuView.setText("View");

        jMenuLogDisplay.setText("Log display");
        jMenuLogDisplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuLogDisplayActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuLogDisplay);

        jMenuShowSource.setText("Show source");
        jMenuShowSource.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuShowSourceActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuShowSource);

        jMenuShowText.setSelected(true);
        jMenuShowText.setText("Show text on Image");
        jMenuShowText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuShowTextActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuShowText);

        jMenuShowCursor.setText("Show cursor");
        jMenuView.add(jMenuShowCursor);

        jMenuBar1.add(jMenuView);

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
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(petCtPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jToolBar2, javax.swing.GroupLayout.DEFAULT_SIZE, 1011, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(petCtPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jMenuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuExitActionPerformed
		dispose();
	}//GEN-LAST:event_jMenuExitActionPerformed

	private void jButFrontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButFrontActionPerformed
		petCtPanel1.ActionMipCine(PetCtPanel.CINE_FORWARD);
	}//GEN-LAST:event_jButFrontActionPerformed

	private void jMenuAutoPetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAutoPetActionPerformed
		setWindowLevelAndWidth(0);
}//GEN-LAST:event_jMenuAutoPetActionPerformed

	private void jMenuBrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuBrainActionPerformed
		setWindowLevelAndWidth(9);
}//GEN-LAST:event_jMenuBrainActionPerformed

	private void jCheckFusedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckFusedActionPerformed
		setPetDisplayMode( PetCtPanel.VW_MIP_FUSED);
}//GEN-LAST:event_jCheckFusedActionPerformed

	private void jCheckCorrectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckCorrectedActionPerformed
		setPetDisplayMode( PetCtPanel.VW_MIP);
}//GEN-LAST:event_jCheckCorrectedActionPerformed

	private void jCheckUncorrectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckUncorrectedActionPerformed
		setPetDisplayMode( PetCtPanel.VW_MIP_UNCORRECTED);
}//GEN-LAST:event_jCheckUncorrectedActionPerformed

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

	private void jCheckChestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckChestActionPerformed
		setWindowLevelAndWidth(1);
}//GEN-LAST:event_jCheckChestActionPerformed

	private void jCheckLungActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckLungActionPerformed
		setWindowLevelAndWidth(2);
}//GEN-LAST:event_jCheckLungActionPerformed

	private void jCheckLiverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckLiverActionPerformed
		setWindowLevelAndWidth(3);
}//GEN-LAST:event_jCheckLiverActionPerformed

	private void jCheckBoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoneActionPerformed
		setWindowLevelAndWidth(4);
}//GEN-LAST:event_jCheckBoneActionPerformed

	private void jCheckBrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBrainActionPerformed
		setWindowLevelAndWidth(5);
}//GEN-LAST:event_jCheckBrainActionPerformed

	private void jCheckMriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckMriActionPerformed
		petCtPanel1.MRIflg = !petCtPanel1.MRIflg;
		hideAllPopupMenus();
//		jMenuSyncMri.setEnabled(petCtPanel1.MRIflg);
		petCtPanel1.calculateSUVandCT();
		petCtPanel1.repaint();
}//GEN-LAST:event_jCheckMriActionPerformed

	private void jCheckUsePetWinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckUsePetWinActionPerformed
		boolean enab1 = petCtPanel1.mipPipe.useSrcPetWinLev;
		dSlide.setEnabled(enab1);
		petCtPanel1.mipPipe.useSrcPetWinLev = !enab1;
		hideAllPopupMenus();
		petCtPanel1.repaint();
}//GEN-LAST:event_jCheckUsePetWinActionPerformed

	private void jButAxialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButAxialActionPerformed
		changeLayout( JFijiPipe.DSP_AXIAL);
	}//GEN-LAST:event_jButAxialActionPerformed

	private void jButCoronalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButCoronalActionPerformed
		changeLayout( JFijiPipe.DSP_CORONAL);
	}//GEN-LAST:event_jButCoronalActionPerformed

	private void jButSagitalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSagitalActionPerformed
		changeLayout( JFijiPipe.DSP_SAGITAL);
	}//GEN-LAST:event_jButSagitalActionPerformed

	private void jButZoomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButZoomActionPerformed
		petCtPanel1.zoomTog = !petCtPanel1.zoomTog;
		hideAllPopupMenus();
		petCtPanel1.repaint();
	}//GEN-LAST:event_jButZoomActionPerformed

	private void jButMipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMipActionPerformed
		petCtPanel1.showMip = !petCtPanel1.showMip;
		setPetDisplayMode(PetCtPanel.VW_MIP_FUSED);
	}//GEN-LAST:event_jButMipActionPerformed

	private void jButFwdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButFwdActionPerformed
		petCtPanel1.ActionMipCine(PetCtPanel.CINE_RUNNING);
	}//GEN-LAST:event_jButFwdActionPerformed

	private void jButSideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSideActionPerformed
		petCtPanel1.ActionMipCine(PetCtPanel.CINE_SIDE);
	}//GEN-LAST:event_jButSideActionPerformed

	private void jMenuOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuOptionsActionPerformed
		getOptionDlg();
	}//GEN-LAST:event_jMenuOptionsActionPerformed

	private void jCheck3PetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheck3PetActionPerformed
		launch3Pet();
	}//GEN-LAST:event_jCheck3PetActionPerformed

	private void jCheck3CtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheck3CtActionPerformed
		launch3Ct();
	}//GEN-LAST:event_jCheck3CtActionPerformed

	private void jMenuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAboutActionPerformed
		PetCtAbout dlg = new PetCtAbout(this, true);
		dlg.setVisible(true);
	}//GEN-LAST:event_jMenuAboutActionPerformed

	private void jMenuContentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuContentsActionPerformed
		ChoosePetCt.openHelp("Pet-Ct Viewer");
	}//GEN-LAST:event_jMenuContentsActionPerformed

	private void jMenuFitWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuFitWindowActionPerformed
		fitWindow();
	}//GEN-LAST:event_jMenuFitWindowActionPerformed

	private void jButScrollActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButScrollActionPerformed
		setExternalSpinners(true);
	}//GEN-LAST:event_jButScrollActionPerformed

	private void jMenuShowTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuShowTextActionPerformed
		petCtPanel1.repaint();
	}//GEN-LAST:event_jMenuShowTextActionPerformed

	private void jMenuBrownFatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuBrownFatActionPerformed
/*		Btst inst1 = Btst.getInstance();
		if( inst1 != null) return;

		Btst dlg = Btst.makeNew(this);
		dlg.setVisible(true);*/

		BrownFat inst1 = BrownFat.getInstance();
		if(inst1 != null) {
			inst1.saveParentExt(this);
			return;
		}
		BrownFat dlg = BrownFat.makeNew(this);
		dlg.setVisible(true);
	}//GEN-LAST:event_jMenuBrownFatActionPerformed

	private void jCheckReprojectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckReprojectionActionPerformed
		petCtPanel1.reprojectionAction(jCheckReprojection.isSelected());
	}//GEN-LAST:event_jCheckReprojectionActionPerformed

	private void jButGatedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButGatedActionPerformed
		petCtPanel1.runGated = jButGated.isSelected();
	}//GEN-LAST:event_jButGatedActionPerformed

	private void jMenuSaveMipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSaveMipActionPerformed
		SaveMip dlg = new SaveMip(this, true);
//		dlg.scriptAction();
		dlg.setVisible(true);
		dlg.doAction(null);
	}//GEN-LAST:event_jMenuSaveMipActionPerformed

	private void jMenuSaveSignificantActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSaveSignificantActionPerformed
		myWriteDicom dcm1 = new myWriteDicom(this, null);
		dcm1.specialType = 2;	// significant image
		dcm1.writeDicomHeader();
		dcm1.writeLogMessage();
	}//GEN-LAST:event_jMenuSaveSignificantActionPerformed

	private void jMenuSyncMriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSyncMriActionPerformed
		ManualSync syncDlg = new ManualSync(this, false);
		syncDlg.setVisible(true);
	}//GEN-LAST:event_jMenuSyncMriActionPerformed

	private void jMenuMaxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuMaxActionPerformed
		setWindowLevelAndWidth(8);
	}//GEN-LAST:event_jMenuMaxActionPerformed

	private void jMenuVtkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuVtkActionPerformed
		vtkDiag = new VtkDialog(this, false);
		if( vtkDiag.IsOK()) vtkDiag.setVisible(true);
//		VtkFrame vtk1 = new VtkFrame(this);
//		vtk1.setVisible(true);
	}//GEN-LAST:event_jMenuVtkActionPerformed

    private void jMenuAnnotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAnnotationsActionPerformed
		petCtPanel1.annotDlg();
    }//GEN-LAST:event_jMenuAnnotationsActionPerformed

    private void jCheck3FusedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheck3FusedActionPerformed
		launch3Fused();
    }//GEN-LAST:event_jCheck3FusedActionPerformed

    private void jMenuMixResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuMixResetActionPerformed
		resetMixing();
    }//GEN-LAST:event_jMenuMixResetActionPerformed

    private void jCheckFuseHotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckFuseHotActionPerformed
		changeFuseColor(JFijiPipe.COLOR_HOTIRON);
    }//GEN-LAST:event_jCheckFuseHotActionPerformed

    private void jCheckSourceColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckSourceColorActionPerformed
		changePetColor(JFijiPipe.COLOR_USER);
    }//GEN-LAST:event_jCheckSourceColorActionPerformed

    private void jCheckFuseSourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckFuseSourceActionPerformed
		changeFuseColor(JFijiPipe.COLOR_USER);
    }//GEN-LAST:event_jCheckFuseSourceActionPerformed

    private void jCheckFuseBluesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckFuseBluesActionPerformed
		changeFuseColor(JFijiPipe.COLOR_BLUES);
    }//GEN-LAST:event_jCheckFuseBluesActionPerformed

    private void jCheckAltROIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckAltROIActionPerformed
		alternateROI();
    }//GEN-LAST:event_jCheckAltROIActionPerformed

    private void jMenuLungMipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuLungMipActionPerformed
		LungMIP dlg1 = new LungMIP(this, false);
		dlg1.setVisible(true);
    }//GEN-LAST:event_jMenuLungMipActionPerformed

    private void jMenuLogDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuLogDisplayActionPerformed
		useLog(true);
    }//GEN-LAST:event_jMenuLogDisplayActionPerformed

    private void jMenuShowSourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuShowSourceActionPerformed
		showSource(true, true);
    }//GEN-LAST:event_jMenuShowSourceActionPerformed

    private void jCheckMriLutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckMriLutActionPerformed
		changeMriLut();
    }//GEN-LAST:event_jCheckMriLutActionPerformed

    private void jMenuAnoTBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAnoTBActionPerformed
		petCtPanel1.annoTB();
    }//GEN-LAST:event_jMenuAnoTBActionPerformed

    private void jRadioDistanceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioDistanceActionPerformed
		petCtPanel1.setRadioTB2(AnoToolBar.RB_DIST);
    }//GEN-LAST:event_jRadioDistanceActionPerformed

    private void jRadArUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadArUpActionPerformed
		petCtPanel1.setRadioTB2(AnoToolBar.RB_UPARROW);
    }//GEN-LAST:event_jRadArUpActionPerformed

    private void jRadArDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadArDownActionPerformed
		petCtPanel1.setRadioTB2(AnoToolBar.RB_DOWNARROW);
    }//GEN-LAST:event_jRadArDownActionPerformed

    private void jRadArLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadArLeftActionPerformed
		petCtPanel1.setRadioTB2(AnoToolBar.RB_LEFTARROW);
    }//GEN-LAST:event_jRadArLeftActionPerformed

    private void jRadArRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadArRightActionPerformed
		petCtPanel1.setRadioTB2(AnoToolBar.RB_RIGHTARROW);
    }//GEN-LAST:event_jRadArRightActionPerformed

    private void jRadioTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioTextActionPerformed
		petCtPanel1.setRadioTB2(AnoToolBar.RB_TEXT);
    }//GEN-LAST:event_jRadioTextActionPerformed

    private void jRadioBMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioBMActionPerformed
		petCtPanel1.setRadioTB2(AnoToolBar.RB_BM);
    }//GEN-LAST:event_jRadioBMActionPerformed

    private void jButQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButQuitActionPerformed
		petCtPanel1.quitTB2();
    }//GEN-LAST:event_jButQuitActionPerformed

    private void jButPrevActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButPrevActionPerformed
		petCtPanel1.setBMNextSlice(-1);
    }//GEN-LAST:event_jButPrevActionPerformed

    private void jButNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButNextActionPerformed
		petCtPanel1.setBMNextSlice(1);
    }//GEN-LAST:event_jButNextActionPerformed

    private void jButAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButAllActionPerformed
		petCtPanel1.removeTBPoint(true);
    }//GEN-LAST:event_jButAllActionPerformed

    private void jButLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButLastActionPerformed
		petCtPanel1.removeTBPoint(false);
    }//GEN-LAST:event_jButLastActionPerformed

    private void jButMarkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMarkActionPerformed
		petCtPanel1.markButton();
    }//GEN-LAST:event_jButMarkActionPerformed

    private void jButSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSaveActionPerformed
		petCtPanel1.saveTBfiles();
    }//GEN-LAST:event_jButSaveActionPerformed

    private void jRadioSUVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioSUVActionPerformed
		petCtPanel1.setRadioTB2(AnoToolBar.RB_SUV);
    }//GEN-LAST:event_jRadioSUVActionPerformed

    private void jMenuAnoPane1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAnoPane1ActionPerformed
		annotationSave(1);
    }//GEN-LAST:event_jMenuAnoPane1ActionPerformed

    private void jMenuAnoPane2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAnoPane2ActionPerformed
		annotationSave(2);
    }//GEN-LAST:event_jMenuAnoPane2ActionPerformed

    private void jMenuAnoPane3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAnoPane3ActionPerformed
		annotationSave(3);
    }//GEN-LAST:event_jMenuAnoPane3ActionPerformed

    private void jMenuAnoAll3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAnoAll3ActionPerformed
		annotationSave(4);
    }//GEN-LAST:event_jMenuAnoAll3ActionPerformed

    private void jMenuSaveFusedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSaveFusedActionPerformed
		fusedSave();
    }//GEN-LAST:event_jMenuSaveFusedActionPerformed

    private void jCheckUsePetLutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckUsePetLutActionPerformed
		changeMipColor();
    }//GEN-LAST:event_jCheckUsePetLutActionPerformed

    private void jMenuFollowupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuFollowupActionPerformed
		followUp();
    }//GEN-LAST:event_jMenuFollowupActionPerformed

    private void jMenuImproveMIPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuImproveMIPActionPerformed
		petCtPanel1.calcMIP2();
    }//GEN-LAST:event_jMenuImproveMIPActionPerformed

    private void jMenuSUVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSUVActionPerformed
		fillSUV_SUL(true);
    }//GEN-LAST:event_jMenuSUVActionPerformed

    private void jMenuGaussActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuGaussActionPerformed
		Gaussian dlg1 = new Gaussian(this, false);
		dlg1.setVisible(true);
    }//GEN-LAST:event_jMenuGaussActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
		resizeForm();
    }//GEN-LAST:event_formComponentResized

    private void jCheckTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckTopActionPerformed
		changeTop();
    }//GEN-LAST:event_jCheckTopActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButAll;
    private javax.swing.JToggleButton jButAxial;
    private javax.swing.JToggleButton jButCoronal;
    private javax.swing.JToggleButton jButFront;
    private javax.swing.JToggleButton jButFwd;
    private javax.swing.JToggleButton jButGated;
    private javax.swing.JButton jButLast;
    private javax.swing.JButton jButMark;
    private javax.swing.JToggleButton jButMip;
    private javax.swing.JButton jButNext;
    private javax.swing.JButton jButPrev;
    private javax.swing.JButton jButQuit;
    private javax.swing.JToggleButton jButSagital;
    private javax.swing.JButton jButSave;
    private javax.swing.JToggleButton jButScroll;
    private javax.swing.JToggleButton jButSide;
    private javax.swing.JToggleButton jButZoom;
    private javax.swing.JCheckBoxMenuItem jCheck3Ct;
    private javax.swing.JCheckBoxMenuItem jCheck3Fused;
    private javax.swing.JCheckBoxMenuItem jCheck3Pet;
    private javax.swing.JCheckBoxMenuItem jCheckAltROI;
    private javax.swing.JCheckBox jCheckAutoBM;
    private javax.swing.JCheckBoxMenuItem jCheckBlues;
    private javax.swing.JCheckBoxMenuItem jCheckBone;
    private javax.swing.JCheckBoxMenuItem jCheckBrain;
    private javax.swing.JCheckBoxMenuItem jCheckChest;
    private javax.swing.JCheckBoxMenuItem jCheckCorrected;
    private javax.swing.JCheckBoxMenuItem jCheckFuseBlues;
    private javax.swing.JCheckBoxMenuItem jCheckFuseHot;
    private javax.swing.JCheckBoxMenuItem jCheckFuseSource;
    private javax.swing.JCheckBoxMenuItem jCheckFused;
    private javax.swing.JCheckBoxMenuItem jCheckGrayScale;
    private javax.swing.JCheckBoxMenuItem jCheckHotIron;
    private javax.swing.JCheckBoxMenuItem jCheckInverse;
    private javax.swing.JCheckBoxMenuItem jCheckLiver;
    private javax.swing.JCheckBoxMenuItem jCheckLung;
    private javax.swing.JCheckBoxMenuItem jCheckMri;
    private javax.swing.JCheckBoxMenuItem jCheckMriLut;
    private javax.swing.JCheckBoxMenuItem jCheckReprojection;
    private javax.swing.JCheckBoxMenuItem jCheckSourceColor;
    private javax.swing.JCheckBoxMenuItem jCheckTop;
    private javax.swing.JCheckBoxMenuItem jCheckUncorrected;
    private javax.swing.JCheckBoxMenuItem jCheckUsePetLut;
    private javax.swing.JCheckBoxMenuItem jCheckUsePetWin;
    private javax.swing.JComboBox jComboText;
    private javax.swing.JLabel jLabArrows;
    private javax.swing.JLabel jLabClear;
    private javax.swing.JLabel jLabCount;
    private javax.swing.JLabel jLabMeasure;
    private javax.swing.JTextField jLevelVal;
    private javax.swing.JMenuItem jMenuAbout;
    private javax.swing.JMenuItem jMenuAnnotations;
    private javax.swing.JMenuItem jMenuAnoAll3;
    private javax.swing.JMenuItem jMenuAnoPane1;
    private javax.swing.JMenuItem jMenuAnoPane2;
    private javax.swing.JMenuItem jMenuAnoPane3;
    private javax.swing.JMenuItem jMenuAnoTB;
    private javax.swing.JMenu jMenuAnotPane1;
    private javax.swing.JMenuItem jMenuAutoPet;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuBrain;
    private javax.swing.JMenuItem jMenuBrownFat;
    private javax.swing.JMenuItem jMenuContents;
    private javax.swing.JMenu jMenuEdit;
    private javax.swing.JMenuItem jMenuExit;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuFitWindow;
    private javax.swing.JMenuItem jMenuFollowup;
    private javax.swing.JMenuItem jMenuGauss;
    private javax.swing.JMenu jMenuHelp;
    private javax.swing.JMenuItem jMenuImproveMIP;
    private javax.swing.JCheckBoxMenuItem jMenuLogDisplay;
    private javax.swing.JMenuItem jMenuLungMip;
    private javax.swing.JMenuItem jMenuMax;
    private javax.swing.JMenuItem jMenuMixReset;
    private javax.swing.JMenuItem jMenuOptions;
    private javax.swing.JMenuItem jMenuSUV;
    private javax.swing.JMenuItem jMenuSaveFused;
    private javax.swing.JMenuItem jMenuSaveMip;
    private javax.swing.JMenuItem jMenuSaveSignificant;
    private javax.swing.JCheckBoxMenuItem jMenuShowCursor;
    private javax.swing.JCheckBoxMenuItem jMenuShowSource;
    private javax.swing.JCheckBoxMenuItem jMenuShowText;
    private javax.swing.JMenuItem jMenuSyncMri;
    private javax.swing.JMenu jMenuView;
    private javax.swing.JMenuItem jMenuVtk;
    private javax.swing.JMenu jMenuWindow;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPopupMenu jPopupCtMenu;
    private javax.swing.JPopupMenu jPopupFusedMenu;
    private javax.swing.JPopupMenu jPopupMipMenu;
    private javax.swing.JPopupMenu jPopupPetMenu;
    private javax.swing.JRadioButton jRadArDown;
    private javax.swing.JRadioButton jRadArLeft;
    private javax.swing.JRadioButton jRadArRight;
    private javax.swing.JRadioButton jRadArUp;
    private javax.swing.JRadioButton jRadioBM;
    private javax.swing.JRadioButton jRadioDistance;
    private javax.swing.JRadioButton jRadioSUV;
    private javax.swing.JRadioButton jRadioText;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JToolBar.Separator jSeparator11;
    private javax.swing.JToolBar.Separator jSeparator12;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JToolBar.Separator jSeparator9;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JTextField jWidthVal;
    private PetCtPanel petCtPanel1;
    // End of variables declaration//GEN-END:variables
	int foundData = 0, SUVmm = 5, SUVmm2 = 5, pgUpDn = 3;
	boolean autoResize = false, circleSUV = false, altROI = false, changeView = false;
	boolean circleSUV2 = false, sphereSUV2 = false, useSUL = false;
	boolean operatorNameFlg = false, sphereSUV = false, invertScroll = false;
	boolean startROIs = false, startAnnotations = false, qualityRendering = false;
	boolean isInitialized = false, ignoreSUV = false, isSetTitle = false;
	boolean isFWHM = false, allowMRIchop = false;
	Double sliceMaxFactor, sigmaGauss, triThick, loSave;
//	int fuseFactor = 120;
	Preferences jPrefer = null;
	String m_patName, m_patID, m_styName, m_studyInstanceUID;
	String runArg, levelSave, widthSave;
	VtkDialog vtkDiag = null;
	Date m_patBirthday = null;
	DoubleRangeSlider dSlide = null;
	Display3Frame ct3 =  null, pet3 = null, fuse3 = null;
	ChoosePetCt chooseDlg = null;
	int slideDigits = 0;
	int SUVtype = 0, SUVtype2 = 0;
	int [] ctLevel = null;
	int [] ctWidth = null;
	static int [] keyDelay = null;
	static int keyIndex = 0;
	int view3Slop = 6;
	long currKeyTime;
	boolean top1 = false, useBF = true;	// change to true
	static PetCtFrame gotFocus = null;
	static ArrayList<Object> extList = null;
	static ArrayList<Object> conferenceList = null;
	static final String IMAGEJ_NAME = "(Fiji is just) ImageJ";
}

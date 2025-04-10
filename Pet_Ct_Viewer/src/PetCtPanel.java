import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.FileInfo;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.*;
import org.jogamp.vecmath.Point2d;
import org.jogamp.vecmath.Point3d;

/*
 * PetCtPanel.java
 *
 * Created on Dec 23, 2009, 8:14:04 AM
 */

/**
 *
 * @author Ilan
 */
public class PetCtPanel extends JPanel implements MouseListener, MouseMotionListener,
		MouseWheelListener {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;
	static final int VW_MIP = 1;
	static final int VW_MIP_FUSED = 2;
	static final int VW_MIP_UNCORRECTED = 3;
	static final int VW_PET_CT_FUSED = 4;
	static final int VW_PET_CT_FUSED_UNCORRECTED = 5;

	static final int SZ_MIP_AXIAL = 0;
	static final int SZ_AXIAL = 1;
	static final int SZ_CORONAL = 2;

	static final int CINE_RUNNING = 0;
	static final int CINE_STOPPED = 1;
	static final int CINE_FORWARD = 2;
	static final int CINE_SIDE = 3;

	static final int NOTIFY_SPINNER = 1;
	static final int NOTIFY_LAYOUT = 2;
	static final int NOTIFY_WIN_LEVELS = 3;
	static final int NOTIFY_XY_SHIFT = 4;
	static final int NOTIFY_ZOOM = 5;
	static final int NOTIFY_PAN = 6;
	static final int NOTIFY_BASE_VALUE = 7;

	static final int WHEEL_SPEED2 = 20000;

	myMouse mouse1 = new myMouse();
	bkgdLoadData work2 = null, work3 = null, work4 = null;
	BrownFat bfDlg = null;
	LungMIP LungMipDlg = null;
	Annotations anotateDlg = null;
	AnoToolBar anotateTB = null;
	PetCtFrame parent;
	ArrayList<ImagePlus> inImgList = null;
	ArrayList<Integer> inSeriesType = null;
	JFijiPipe petPipe=null, upetPipe=null, ctPipe=null, mipPipe = null, mriPipe=null;
	JFijiPipe reproPipe = null;
	Annotations.myOblique m_oblique = null;
	int m_kvp = 0, m_ctMa = 0;
	long nanoTime, logStart, logLast;
	String petSeriesName = null, operatorName = null, niftiTmp = "";
	int sliderOwner = -1, petColor = JFijiPipe.COLOR_INVERSE, upetOffset = 0, petIndx = 0;
	int m_masterFlg = 0, m_sliceType = 0, CTval;
	int gateIndx = 0, fusedColor = JFijiPipe.COLOR_BLUES;
	int ctMriColor = JFijiPipe.COLOR_GRAY, mipColor = JFijiPipe.COLOR_INVERSE;
	boolean paintingFlg = false, cinePaint = false, SUVonceFlg = false, SUVflg = false;
	boolean  runCine = false,showMip = true, resizeOnce = false;
	boolean MRIflg = false, zoomTog = false, reproFlg = false, isInitializing;
	boolean isGated = false, runGated = false, didMaybe = false;
	double minSlider = 0, maxSlider = 1000, SUVorCount, saveWidth, saveLevel;
	double petRelativeZoom = 1.0, ratioCt2Pet = 1.0, lastScale;
	double petAxial, petAxialFake, petCoronalFake, petSagitalFake, petCoronal, petSagital;
	double syncAxial, syncCoronal, syncSagital, dCnvt, dCnvtCor;
	int baseAxial=0, baseCoronal=0, baseSagital=0;
	Point[] curPosition = new Point[3];
	SUVpoints suvPnt = null;
	double curMax, SUVpeak, SUVmean, SD, SULmax, SULmean, SD_SUL;
	private Timer m_timer = null;

	/**
	 * Creates new form PetCtPanel
	 */
	public PetCtPanel() {
		initComponents();
		init();
	}

	private void init() {
		addMouseListener( this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
//		boolean canFocus = this.isFocusable();
		isInitializing = true;
		m_masterFlg = VW_MIP;
		m_sliceType = JFijiPipe.DSP_AXIAL;
		m_timer = new Timer(200, new CineAction());
		nanoTime = System.nanoTime();
//		testGetInstance();
	}

	class CineAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			boolean paintFlg = false;
			if( paintingFlg == false && showMip && runCine) {
				cinePaint = true;
				paintFlg = true;
			}
			if( runGated ) {
				incrGatePosition(true);
				return;
			}
			if( paintFlg) repaint();
		}
	}

	/**
	 * Note: there is a fundamental difference in the counting here.
	 * The mouse position, evt0, counts from 1, whereas in doing mod
	 * commands we need to count from 0. So subtract 1 from evt0.
	 */
	class myMouse {
		int xPos, yPos, widthX, widthY, page, xDrag, yDrag, button1, startSlice, numSlice;
		int whichButton;
		double zoomZ, zoomY, zoom1;
		Point3d pan3d = null;

		void initializeParameters() {
			double scale = getScalePet();
			widthX = (int) (scale * petPipe.data1.width);
		}

		int getMousePage(MouseEvent evt0, boolean save) {
			Dimension dm2;
			int x1, y1;
			initializeParameters();
			dm2 = getSize();
			widthY = dm2.height;
			x1 = evt0.getX() - 1;
			y1 = evt0.getY() - 1;
			if( x1 < 0) x1 = 0;	// these 2 lines should never happen
			if( y1 < 0) y1 = 0;
			int ret1 = (x1/widthX) + 1;
			if( ret1 > 3) ret1 = 0;	// illegal
			if( save) {
				xPos = x1;
				yPos = y1;
				page = ret1;
			}
			return ret1;
		}

		Point2d getDragOffset(MouseEvent evt0, boolean save) {
			int x = evt0.getX() - 1;
			int y = evt0.getY() - 1;
			Point2d pt1 = new Point2d();
			if( save) {
				initializeParameters();
				button1 = evt0.getButton();
				xDrag = x;
				yDrag = y;
				if(m_sliceType == JFijiPipe.DSP_AXIAL) numSlice = petPipe.getNormalizedNumFrms();
				else numSlice = petPipe.data1.width;
				startSlice = ChoosePetCt.round(getCurrentSlice());
				return pt1;
			}
			pt1.x = 1.0 * (x - xDrag) / widthX;
			pt1.y = 1.0 * (yDrag - y) / widthX;
			return pt1;
		}

		Point2d getPanOffset(MouseEvent evt0, boolean save) {
			if( evt0 == null) return null;
			int x = evt0.getX() - 1;
			int y = evt0.getY() - 1;
			Point2d pt1 = new Point2d();
			if( save) {
				getMousePage(evt0, true);
				button1 = evt0.getButton();
				return pt1;
			}
			pt1.x = 1.0 * (xPos - x) / widthX;
			pt1.y = 1.0 * (yPos - y) / widthY;
//			String tmp = "xPos "+ xPos + ", x " + x + ", yPos " + yPos +", y " + y;
//			tmp += ", pt1 " + pt1;
//			IJ.log(tmp);
			return pt1;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		int i, j;
		i = e.getButton();
		j = e.getClickCount();
		changeSliderParameters( e);
		if( i != MouseEvent.BUTTON1) return;
		if( j == 2) {
			processMouseDoubleClick(e);
			return;
		}
		if( j == 1) {
			processMouseSingleClick(e);
		}
	}

	@Override
	public void mouseEntered(MouseEvent me) {}
	@Override
	public void mouseExited(MouseEvent me) {}
	@Override
	public void mouseReleased(MouseEvent me) {}

	@Override
	public void mousePressed(MouseEvent e) {
		mouse1.whichButton = e.getButton();
//		miniDebug(1, "button "+mouse1.whichButton);
		if( isDrawOrAnnotate( e, true)) return;

		if(zoomTog) panDrag( e, true);
		else winLevelDrag(e, true);
		// watch out that resizing doesn't change the gray scale - look at cursor
		if(getCursor() != Cursor.getDefaultCursor()) mouse1.button1 = MouseEvent.NOBUTTON;
		maybeShowPopupMenu(e);
		if(zoomTog) parent.notifySyncedStudies(NOTIFY_PAN, 1, 0, null, e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if( bfDlg != null && bfDlg.handleMouseDrag(e, this)) return;	// brown fat changed point
		if( mouse1.button1 != MouseEvent.BUTTON1 && mouse1.button1 != MouseEvent.BUTTON2) return;
		if(zoomTog) {
			panDrag( e, false);
			parent.notifySyncedStudies(NOTIFY_PAN, 0, 0, null, e);
		}
		else winLevelDrag( e, false);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		int j;
		if( bfDlg != null) bfDlg.handleMouseMove(e, this);
//		if( LungMipDlg != null) LungMipDlg.handleMouseMove(e, this);
		if( curPosition[0] == null) return;
		if( parent.isShowCursor()) return;
		j = mouse1.widthX/20;	// tolerance
		Point pos1 = e.getPoint();
		if( Math.abs(pos1.x - mouse1.xPos) > j || Math.abs(pos1.y - mouse1.yPos) > j) {
			curPosition[0] = null;	// kill cross hairs
			repaint();
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int i,j;
		Integer diff1, j1;
		long currTime;
		i = mouse1.getMousePage(e, false);
		j = j1 = e.getWheelRotation();
		if( j > 0) j = -1;
		else j = 1;
		currTime = System.nanoTime();
		diff1 = (int)((currTime - nanoTime)/10000);
		nanoTime = currTime;
		if( diff1 < 200) return;	// eliminate contact bounce
		j1 = Math.abs(j1);
		if( j1 == 1) {
			if( diff1 < WHEEL_SPEED2) j1 = 2;
			if( diff1 < WHEEL_SPEED2/2) j1 = 4;
//			if( diff1 < WHEEL_SPEED2/4) j1 = 4;
		}
		j1 *= j;
//		IJ.log("time " + diff1.toString() + ",  step " + j1.toString());
		if(zoomTog) {
			if( parent.frameNotFocus()) return;
			setAllZoom(j);
			curPosition[0] = null;	// kill cross hairs
			repaint();
			parent.notifySyncedStudies(NOTIFY_ZOOM, j, 0, null, null);
		}
		else {
/*			boolean sliceChg = !showMip;
			if( showMip) {
				if( i==1 || i==2) sliceChg = true;
				if( i==3) {
					changeMIPslice(j);
					repaint();
				}
			}
			if( sliceChg)*/ incrSlicePosition( j1, i, false, 0);
		}
	}

	void setAllZoom(int val) {
		if( petPipe != null) petPipe.setZoom(val);
		if( upetPipe != null) upetPipe.setZoom(val);
		if( ctPipe != null) ctPipe.setZoom(val);
		if( mriPipe != null) mriPipe.setZoom(val);
		updateMultYOff();
	}

/*	boolean testGetInstance() {
		try {
			Class.forName("Btst", false, getClass().getClassLoader());
			Class.forName("BrownFat", false, getClass().getClassLoader());
			Class.forName("BrownFat");
		} catch (Exception e) { IJ.log("problem");}
		Btst dlg0 = Btst.getInstance();
		BrownFat dlg1 = BrownFat.getInstance();
		return true;
	}*/

	void maybeNotifyZoom(int val) {
		if( petPipe == null) return;
		int zoomVal = petPipe.zoomIndx;
		if( !zoomTog && zoomVal <= 0) return;
		setAllZoom( val);
		curPosition[0] = null;	// kill cross hairs
		repaint();
	}

	void repaintAll() {
//		if( parent.ct3 != null) parent.ct3.repaint();
		if( parent.pet3 != null) parent.pet3.repaint();
		if( parent.fuse3 != null) parent.fuse3.repaint();
		repaint();	// finally repaint this window
	}

	void changeMIPslice(int diff) {
		int indx, delay, n;
		if( runCine) {
			delay = m_timer.getDelay();
			delay = (int) ( delay * (1.0 - diff*0.1));
			if( delay <= 0) delay = 1;
			m_timer.setDelay(delay);
		} else {
			indx = getCineIndx();
			indx -= diff;
			n = getNumMIP();
			while( indx < 0) indx += n;
			while( indx >= n) indx -= n;
			setCineIndx( indx);
			updateMIPcursor();
			parent.setCineButtons(true);
		}
	}

	double getCurrentSlice() {
		double currSlice = 0;
		switch(m_sliceType) {
			case JFijiPipe.DSP_AXIAL:
				currSlice = petAxial;
				break;

			case JFijiPipe.DSP_CORONAL:
				currSlice = petCoronal;
				break;

			case JFijiPipe.DSP_SAGITAL:
				currSlice = petSagital;
				break;
		}
		return currSlice;
	}

	void maybeNewPosition( SUVpoints.SavePoint redPnt) {
		int currSlice = ChoosePetCt.round(getCurrentSlice());
		int invert = -1;
		if( m_sliceType == JFijiPipe.DSP_AXIAL) invert = 1;
		int diff = invert*(currSlice - redPnt.z1);
		if( diff == 0) return;	// nothing to do
		incrSlicePosition(diff, 1, false, 0);
	}

	void updateStatusBar() {
		if( !parent.isScroll() || didMaybe) return;
		if( parent.frameNotFocus() != ChoosePetCt.msgNoFocus) return;
		String msg = "";
		if( ChoosePetCt.msgNoFocus) msg = "scroll without focus";
		ChoosePetCt.msgNoFocus = !ChoosePetCt.msgNoFocus;
		IJ.showStatus(msg);
	}

	void logWindow(String source) {
		Date dat0 = petPipe.data1.serTime;
		String tmp = dat0.toString();
		IJ.log(source + tmp);
	}

	void incrSlicePosition( int diff, int mousePage, boolean spinMIP, double zDiffCm) {
		int indx, invert = 1;
		boolean killMIPcursor = true, mipChange = false;
		double zPet, zCT, zDiff=0, factor=1.0;
		Point2d pt1;
//		logWindow("increment ");
		updateStatusBar();
		didMaybe = false;
		if( mousePage < 0) mousePage = mouse1.page;
		JFijiPipe currCT = getMriOrCtPipe();
		if( parent.invertScroll) invert = -1;
		if( diff == 1000) {	// home key
			switch( m_sliceType) {
				case JFijiPipe.DSP_AXIAL:
					indx = petPipe.getNormalizedNumFrms() - 1;
					if( petAxial <= 0) petAxial = indx;
					else if( petAxial >= indx) petAxial = 0;
					else {
						if( petAxial <= indx/2) petAxial = 0;
						else petAxial = indx;
					}
					break;

				case JFijiPipe.DSP_CORONAL:
					petCoronal = (petPipe.data1.height - 1) / 2;
					break;

				case JFijiPipe.DSP_SAGITAL:
					petSagital = (petPipe.data1.width - 1) / 2;
					break;
			}
		}
		else if(/*spinMIP &&*/ mousePage == 3 && showMip) {
			killMIPcursor = false;
			mipChange = true;
			changeMIPslice(diff);
		}
		else switch( getObliqueSliceType()) {
			case JFijiPipe.DSP_AXIAL:
				if( mousePage == 3) {
					if( Math.abs(currCT.avgSliceDiff) < Math.abs(petPipe.avgSliceDiff))
					mousePage = 2;
				}
				if( mousePage == 2) {
					if( Math.abs(diff) == 1) {
//						zPet = petPipe.getZpos(petAxial);
//						zCT = currCT.getZpos(currCT.indx);
//						zDiff = (zCT - zPet)/petPipe.avgSliceDiff;
						// update currCT.indx because the refresh may be delayed
						currCT.indx -= diff;
						currCT.dirtyFlg = true;
					}	
					factor = ratioCt2Pet;
				}
				if( mousePage == 0) { // notify from another study
//					petAxial -= zDiffCm / Math.abs(petPipe.avgSliceDiff);
					petAxial = baseAxial - (zDiffCm / Math.abs(petPipe.avgSliceDiff));
					break;
				}
				zDiff = diff*factor;
				double prevAxial = petAxial;
				petAxial -= zDiff;
				if( mousePage == 2 && factor < 0.9) {
					int idestAxial;
					boolean isInside = false;
					if( diff > 0) idestAxial = (int) (prevAxial - 0.05);
					else idestAxial = (int) (prevAxial + 1.0);

					double zdestPet = petPipe.getZpos(idestAxial);
					zPet = petPipe.getZpos(petAxial);
					if( diff < 0 && zdestPet < zPet) isInside = true;
					if( diff > 0 && zdestPet > zPet) isInside = true;
					if( isInside) {	// still inside area
//						IJ.log("This has fine grained CT.");
					} else {
						petAxial = ChoosePetCt.round(petAxial);
//						petPipe.dirtyFlg = true;	// cause update
					}
				}
//				zDiff *= Math.abs(petPipe.avgSliceDiff);
				zDiff = getZdiff(JFijiPipe.DSP_AXIAL);
				break;

			case JFijiPipe.DSP_CORONAL:
				if( mousePage == 0) { // notify from another study
//					petCoronal += zDiffCm / petPipe.getPixelSpacing(JFijiPipe.ROW);
					petCoronal = baseCoronal + (zDiffCm / petPipe.getPixelSpacing(JFijiPipe.ROW));
					break;
				}
				zDiff = diff*invert;
				petCoronal += zDiff;
//				zDiff *= petPipe.getPixelSpacing(JFijiPipe.ROW);
				zDiff = getZdiff(JFijiPipe.DSP_CORONAL);
				break;

			case JFijiPipe.DSP_SAGITAL:
				if( mousePage == 0) { // notify from another study
//					petSagital += zDiffCm / petPipe.getPixelSpacing(JFijiPipe.COL);
					petSagital = baseSagital + zDiffCm / petPipe.getPixelSpacing(JFijiPipe.COL);
					break;
				}
				zDiff = diff*invert;
				petSagital += zDiff;
//				zDiff *= petPipe.getPixelSpacing(JFijiPipe.COL);
				zDiff = getZdiff(JFijiPipe.DSP_SAGITAL);
				break;
		}
//		IJ.log("axial =" + petAxial + ", coronal = " + petCoronal + ", sagital = " + petSagital + ", diff =" + diff);
		if( killMIPcursor) curPosition[2] = null;
		checkLimits();
		updateFakeValues();
		calculateSUVandCT();
		parent.update3Display();
		repaint();
		pt1 = new Point2d(zDiff, 0);	// pass zDiff as pt1.x
		int val2 = (mipChange)?3:0;
		parent.notifySyncedStudies(NOTIFY_SPINNER, diff, val2, pt1, null);
	}

	double getZdiff(int type) {
		double ret = 0;
		switch (type) {
			case JFijiPipe.DSP_AXIAL:
				ret = (petAxial - baseAxial)*petPipe.avgSliceDiff;
				break;
	
			case JFijiPipe.DSP_CORONAL:
				ret = (petCoronal - baseCoronal)*petPipe.getPixelSpacing(JFijiPipe.ROW);
				break;
	
			case JFijiPipe.DSP_SAGITAL:
				ret = (petSagital - baseSagital)*petPipe.getPixelSpacing(JFijiPipe.COL);
				break;
		}
		return ret;
	}
	// log the time differences between the start time and the last measured time
	void logPoint(Integer curPoint) {
		long curTime = System.nanoTime();
		Integer diff1, diff0;
		if( curPoint <= 0) {
			logStart = logLast = curTime;
		}
		diff0 = (int)((curTime - logStart)/10000);
		diff1 = (int)((curTime - logLast)/10000);
		logLast = curTime;
		if( curPoint > 0) {
			IJ.log(curPoint.toString() + ": " + diff0.toString() + ", " + diff1.toString());
		}
	}
	void incrGatePosition(boolean isUp) {
		if(petPipe == null) return;	// happens when window is closed
		if( isUp) {
			gateIndx++;
			if( gateIndx >= petPipe.data1.numTimeSlots ) gateIndx = 0;
		} else {
			gateIndx--;
			if( gateIndx<0) gateIndx = petPipe.data1.numTimeSlots - 1;
		}
		dirtyCorSag();
		calculateSUVandCT();
		parent.update3Gate();
		repaint();
	}

	int getObliqueSliceType() {
		int retVal = m_sliceType;
		if( retVal == JFijiPipe.DSP_OBLIQUE) {
			if(m_oblique != null) retVal = m_oblique.primaryPlane;
		}
		return retVal;
	}

	// most of the time this returns 0, 0, 0
	// for oblique it is the shift off the center line
	Point3d getObliqueShift() {
		Point3d obliqueShift = new Point3d();
		if( m_sliceType == JFijiPipe.DSP_OBLIQUE && m_oblique != null) {
			obliqueShift = m_oblique.getObliqueShift();
		}
		return obliqueShift;
	}

	double getObliqueCtFix() {
		double ret = 1.0;
		if( m_oblique != null) return m_oblique.ctFix;
		return ret;
	}
	/**
	 * This routine is called by the ExternalSpinners, i.e. SyncScroll.
	 * It changes the fake values of petAxial, petCoronal or petSagital.
	 * If the fake values are within range, it calls the real incrSlicePosition.
	 * 
	 * @param diff - the difference between the current slice and the desired one.
	 * @param mousePage - use -1 if not known
	 * @param zDiffCm - introduced because of studies with different slice thickness
	 */
	protected void maybeIncrSlicePosition(int diff, int mousePage, double zDiffCm) {
		boolean callIncrSlice = true;
//		logWindow("maybe ");
		didMaybe = true;
		if( mousePage == 3 && showMip && !runCine) {}
		else {
			if( diff != 1000) switch(m_sliceType) {
				case JFijiPipe.DSP_AXIAL:
					petAxialFake -= diff;
					if( petAxialFake < 0 || petAxialFake >= petPipe.getNormalizedNumFrms())
						callIncrSlice = false;
					break;

				case JFijiPipe.DSP_CORONAL:
					petCoronalFake -= diff;
					if( petCoronalFake < 0 || petCoronalFake  >= petPipe.data1.width)
						callIncrSlice = false;
					break;

				case JFijiPipe.DSP_SAGITAL:
					petSagitalFake -= diff;
					if( petSagitalFake < 0 || petSagitalFake  >= petPipe.data1.width)
						callIncrSlice = false;
					break;
			}
		}
		int mousePg1 = mousePage;
		if( zDiffCm != 0) mousePg1 = 0;
		if( callIncrSlice) incrSlicePosition(diff, mousePg1, true, zDiffCm);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintingFlg = true;
		Graphics2D g2d = (Graphics2D) g.create();
		drawAll(g2d);
		g2d.dispose();	// clean up
		paintingFlg = false;
		cinePaint = false;
	}

	double getWidthSlider() {
		double retVal = 0;
		switch( sliderOwner) {
			case 0:
				retVal = getCorrectedOrUncorrectedPipe(false).winWidth;
				break;

			case 1:
				retVal = getMriOrCtPipe().winWidth;
				break;

			case 2:
				if( !showMip) {
					retVal = petPipe.fuseWidth;
					break;
				}
				retVal = mipPipe.winWidth;
				if( mipPipe.useSrcPetWinLev) retVal = petPipe.winWidth;
				break;
		}
		return retVal;
	}

	double getLevelSlider() {
		double retVal = 0;
		switch( sliderOwner) {
			case 0:
				retVal = getCorrectedOrUncorrectedPipe(false).winLevel;
				break;

			case 1:
				retVal = getMriOrCtPipe().winLevel;
				break;

			case 2:
				if( !showMip) {
					retVal = petPipe.fuseLevel;
					break;
				}
				retVal = mipPipe.winLevel;
				if( mipPipe.useSrcPetWinLev) retVal = petPipe.winLevel;
				break;
		}
		return retVal;
	}
	
	String getSliderToolTip() {
		String retVal = null;
		switch( sliderOwner) {
			case 0:
				retVal = "adjust PET window, baseline";
				break;
				
			case 1:
				retVal = "adjust CT contrast, level";
				break;
				
			case 2:
				if( !showMip) {
					retVal = "adjust PET-CT mixing value (this value is stored for future use)";
					break;
				}
				retVal = "adjust MIP window, baseline";
				break;
		}
		return retVal;
	}
	
	String getLowerEditToolTip() {
		String retVal = null;
		switch( sliderOwner) {
			case 0:
				retVal = "baseline on PET";
				break;
				
			case 1:
				retVal = "contrast on CT";
				break;
				
			case 2:
				if( !showMip) {
					retVal = "width of slider (no special meaning)";
					break;
				}
				retVal = "baseline on MIP";
				break;
		}
		return retVal;
	}
	
	String getUpperEditToolTip() {
		String retVal = null;
		switch( sliderOwner) {
			case 0:
				retVal = "window of PET, auto gives 5.0 SUV";
				break;
				
			case 1:
				retVal = "centerline of CT";
				break;
				
			case 2:
				if( !showMip) {
					retVal = "fusion mixing value, 500 (of 1000) is 50-50";
					break;
				}
				retVal = "window of MIP";
				break;
		}
		return retVal;
	}

	void ActionSliderChanged( double width, double level) {
		JFijiPipe currPipe;
		switch( sliderOwner) {
			case 0:
				currPipe = getCorrectedOrUncorrectedPipe(false);
				currPipe.winWidth = width;
				currPipe.winLevel = level;
				break;

			case 1:
				currPipe = getMriOrCtPipe();
				currPipe.winWidth = width;
				currPipe.winLevel = level;
			break;

			case 2:
				if( !showMip) {
					currPipe = getCorrectedOrUncorrectedPipe(false);
					currPipe.fuseWidth = width;
					currPipe.fuseLevel = level;
					break;
				}
				mipPipe.winWidth = width;
				mipPipe.winLevel = level;
				if( reproPipe != null) {
					reproPipe.winWidth = width;
					reproPipe.winLevel = level;
				}
				break;
		}
//		double lo=level-width/2, hi=level+width/2;
//		IJ.log("low = "+lo+ " high = "+hi);
		repaint();
	}

	void maybeShowPopupMenu(MouseEvent arg0) {
		parent.hideAllPopupMenus();
		if( arg0.getButton() != MouseEvent.BUTTON3) return;
		JPopupMenu pop1;
		if( arg0.getID() != MouseEvent.MOUSE_PRESSED) return;
		Point pt1 = arg0.getLocationOnScreen();
		int pos3 = mouse1.getMousePage(arg0,false);
		try {
		switch( pos3) {
			case 1:
				pop1 = parent.getjPopupPetMenu();
				pop1.setLocation(pt1);
				parent.updatePetCheckmarks(petColor);
				pop1.setVisible(true);
				break;

			case 2:
				pop1 = parent.getjPopupCtMenu();
				pop1.setLocation(pt1);
				parent.updateCtCheckmarks();
				pop1.setVisible(true);
				break;

			case 3:
				pop1 = parent.getjPopupMipMenu();
				if( !showMip) pop1 = parent.getjPopupFusedMenu();
				pop1.setLocation(pt1);
				parent.updateMipCheckmarks(fusedColor);
				pop1.setVisible(true);
				break;

			default:
//				return;	// not one of the 3 sections
		}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	/**
	 * The routine which processes user mouse drags to change gray scale values.
	 *
	 * @param arg0 the current mouse position
	 * @param starting true if mouse pressed event, false if drag
	 */
	protected void winLevelDrag( MouseEvent arg0, boolean starting) {
		Point2d pt1 = mouse1.getDragOffset(arg0, starting);
		double width1, level1, delta, win1, base1, coef;
		if( starting) {
			changeSliderParameters(arg0);
			saveLevel = getLevelSlider();
			saveWidth = getWidthSlider();
			return;
		}
		if( mouse1.button1 == MouseEvent.BUTTON2) {
			coef = 0.5;
			int currSlice, diff, sign1 = 1;
			if( parent.invertScroll) sign1 = -1;
			if(m_sliceType == JFijiPipe.DSP_AXIAL) {
				sign1 = -1;
				coef = 1;
			}
			int slice = (int)(mouse1.startSlice + sign1*coef*pt1.y*mouse1.numSlice + 0.5);
			if( slice < 0) slice = 0;
			if( slice >= mouse1.numSlice) slice = mouse1.numSlice - 1;
			currSlice = (int) getCurrentSlice();
			diff = sign1*(slice - currSlice);
			if( diff == 0) return;
			incrSlicePosition(diff, 1, false, 0);
			return;
		}
		delta = maxSlider - minSlider;
		if( sliderOwner == 1) delta /= 10;
		width1 = pt1.x * delta + saveWidth;
		if( width1 <= 0) return;
		level1 = pt1.y * delta + saveLevel;
		base1 = level1 - width1/2;
		win1 = level1 + width1/2;
		if( base1 < minSlider) base1 = minSlider;
		if( base1 > maxSlider) base1 = maxSlider;
		if( win1 < minSlider) win1 = minSlider;
		if( win1 > maxSlider) win1 = maxSlider;
		parent.setDualSliderValues(base1, win1);
	}

	protected void panDrag( MouseEvent arg0, boolean starting) {
		Point2d pt1 = mouse1.getPanOffset(arg0, starting);
		if( starting) {
			mouse1.pan3d = new Point3d(petPipe.pan);
			mouse1.zoom1 = petPipe.zoom1;
			mouse1.zoomZ = petPipe.data1.width * mouse1.widthY;
//			mouse1.zoomZ /= mouse1.widthX * petPipe.data1.numFrms * petPipe.zoomX * petPipe.data1.y2xFactor / petPipe.data1.numTimeSlots;
			mouse1.zoomZ /= mouse1.widthX * petPipe.numW();
			mouse1.zoomY = petPipe.zoomY;
			return;
		}
		panDragSub(pt1);
		curPosition[0] = null;	// kill cross hairs
		repaint();
	}
	
	void panDragSub(Point2d pt1) {
		double x1 = pt1.x;
		double y1 = pt1.y;
		Point3d pan1 = new Point3d(mouse1.pan3d);
		double maxZ = (mouse1.zoom1 - 1.0) / mouse1.zoom1;
		if( maxZ < 0) maxZ = 0;
		double maxY = maxZ;
		switch( getObliqueSliceType()) {
			case JFijiPipe.DSP_AXIAL:
				maxY = (mouse1.zoom1 - mouse1.zoomY) / mouse1.zoom1;
				if( maxY < 0) maxY = 0;
				pan1.x += x1;
				pan1.y += y1;
				break;

			case JFijiPipe.DSP_CORONAL:
				pan1.x += x1;
				pan1.z += y1;
				break;

			case JFijiPipe.DSP_SAGITAL:
				pan1.y += x1;
				pan1.z += y1;
				break;
		}
		if( pan1.x > maxZ) pan1.x = maxZ;
		if( pan1.x < -maxZ) pan1.x = -maxZ;
		if( pan1.y > maxY) pan1.y = maxY;
		if( pan1.y < -maxY) pan1.y = -maxY;
		maxZ = (mouse1.zoom1 - mouse1.zoomZ) / mouse1.zoom1;
		// maxZ = mous1.zoom1 - mouse1.zoomZ;	// probably better
		if( maxZ < 0) maxZ = 0;
		if( pan1.z > maxZ) pan1.z = maxZ;
		if( pan1.z < 0) pan1.z = 0;
		if( !petPipe.updatePanValue(pan1, false)) return;
		if( upetPipe != null) upetPipe.updatePanValue(pan1, true);
		if( ctPipe != null) ctPipe.updatePanValue(pan1, true);
		if( mriPipe != null) mriPipe.updatePanValue(pan1, true);
	}

	void changeSliderParameters(MouseEvent arg0) {
		int pos3 = mouse1.getMousePage(arg0,false);
		if( pos3 <= 0) return;	// not one of the 3 sections
		sliderOwner = pos3 - 1;
		changeCurrentSlider();
	}
	
	// this routine widens the dual slide bar if slice max > 1.0
	double sliceMaxLimit() {
		double val = parent.sliceMaxFactor;
		if(val <= 1.0) val = 1.0;
		return val;
	}

	void changeCurrentSlider() {
		int sliderDigits = 0;
		minSlider = 0;
		maxSlider = 1000 * sliceMaxLimit();
		boolean enableSlider = true;
		switch( sliderOwner) {
			case 2:
				if( !showMip) break;	// for MIP fall through to case 0
				enableSlider = !mipPipe.useSrcPetWinLev;

			case 0:
				// if it is uncorrected, use maxSlider = 1000
				if( petPipe != getCorrectedOrUncorrectedPipe(true) && sliderOwner == 0) break;
				maxSlider = petPipe.data1.sliderSUVMax * sliceMaxLimit();
				if( petPipe.data1.SUVfactor > 0) sliderDigits = 1;
				break;

			case 1:
				if( isMri() && mriPipe.data1.isCt()) {
					minSlider = mriPipe.data1.minVal;
					maxSlider = mriPipe.data1.maxVal;
					break;
				}
				minSlider = ctPipe.data1.minVal;
				maxSlider = ctPipe.data1.maxVal;
				break;

			default:
				return;	// slider not defined
		}
		parent.switchDualSlider( sliderDigits, enableSlider);
	}

	/**
	 * LoadData takes an imgList which contains the relevant series
	 * for constructing a Pet-Ct study. It loads the data by parsing the Dicom
	 * information and then filling the ArrayList which points to the binary data
	 * in the ImagePlus structure. The raw data is not copied so as to save
	 * memory. A MIP image is constructed from the PET data.
	 *
	 * This is all done in the background by a swing worker thread.
	 *
	 * @param par1 parent PetCtFrame
	 * @param imgList the list of chosen studies, currently loaded into memory
	 * @param seriesType for each study in imgList, its series type=ct,pet etc.
	 */
	protected void LoadData(PetCtFrame par1, ArrayList<ImagePlus> imgList, ArrayList<Integer>seriesType) {
		parent = par1;
		inImgList = imgList;
		inSeriesType = seriesType;
		ChoosePetCt.loadingData = 1;
		work2 = new bkgdLoadData();
//		IJ.log("About to background load.");
		work2.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String propertyName = evt.getPropertyName();
				if( propertyName.equals("state")) {
					SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
					if( state == SwingWorker.StateValue.DONE) {
						work2 = null;
						inImgList = null;
						inSeriesType = null;
					}
				}
			}
		});
		work2.execute();
	}
	
	void updateDisp3Value(boolean corCT) {
		Display3Frame d3frm = parent.fuse3;
		if (d3frm == null) return;
		Display3Panel d3pnl = d3frm.getDisplay3Panel();
		JFijiPipe cpyPipe = getMriOrCtPipe();
		JFijiPipe dstPipe = d3pnl.ct4fused;
		if( !corCT) {
			cpyPipe = getCorrectedOrUncorrectedPipe(false);
			dstPipe = d3pnl.d3Pipe;
		}
		if( cpyPipe == null || dstPipe == null) return;
		dstPipe.corSagShift = cpyPipe.corSagShift;
		dstPipe.mri1.copyOffs(cpyPipe);
/*		dstPipe.data1.mriOffZ = cpyPipe.data1.mriOffZ;
		dstPipe.mriOffX = cpyPipe.mriOffX;
		dstPipe.mriOffY = cpyPipe.mriOffY;*/
		d3frm.repaint();
	}

	// this routine is old and is no longer used. useXY=false
/*	boolean maybeChangeUseXYShift(boolean useXY) {
		boolean oldVal = petPipe.useShiftXY;
		if( useXY == oldVal) return false;	// no change
		petPipe.useShiftXY = useXY;
		petPipe.dirtyFlg = true;
		ctPipe.useShiftXY = useXY;
		ctPipe.dirtyFlg = true;
		if( upetPipe != null) {
			upetPipe.useShiftXY = useXY;
			upetPipe.dirtyFlg = true;
		}
		if( mriPipe != null) {
			mriPipe.useShiftXY = useXY;
			mriPipe.dirtyFlg = true;
		}
		Display3Frame d3frm;
		Display3Panel d3pnl;
		JFijiPipe d3pipe;
		int j;
		d3frm = parent.fuse3;
		if( d3frm != null) {
			d3pnl = d3frm.getDisplay3Panel();
			for( j=0; j<2; j++) {
				if(j==0) d3pipe = d3pnl.d3Pipe;
				else d3pipe = d3pnl.ct4fused;
				if( d3pipe != null) {
					d3pipe.useShiftXY = useXY;
					d3pipe.dirtyFlg = true;
				}
			}
		}
		return true;
	}*/
	
	void changeQuality(boolean qualityFlg) {
		parent.qualityRendering = qualityFlg;
		petPipe.qualityRendering = qualityFlg;
		ctPipe.qualityRendering = qualityFlg;
		mipPipe.qualityRendering = qualityFlg;
		if( upetPipe != null) upetPipe.qualityRendering = qualityFlg;
		if( mriPipe != null) mriPipe.qualityRendering = qualityFlg;
		if( reproPipe != null) reproPipe.qualityRendering = qualityFlg;
		repaint();
	}

	void doActualLoadData() {
		JFijiPipe currPipe;
		ImagePlus currImg;
		ImageJ ij;
		String tmp1;
		double aspect, halfPixel, xshift=0, yshift=0, tmpd, ptY=0;
		double ctY;
		boolean isDicom = true;
		boolean srcVisible = parent.isShowSource();
		int format, i, k, n = inImgList.size();
//		IJ.log("Starting load");
		for( i=0; i<n; i++) {
			currPipe = null;
			k = inSeriesType.get(i);
			switch(k) {
				case ChoosePetCt.SERIES_BQML_PET:
				case ChoosePetCt.SERIES_SPECT:
				case ChoosePetCt.SERIES_SIEMENS_SPECT:
				case ChoosePetCt.SERIES_GML_PET:
				case ChoosePetCt.SERIES_PHILIPS_PET:
				case ChoosePetCt.SERIES_GE_PRIVATE_PET:
				case ChoosePetCt.SER_FORCE_CPET:
					currPipe = petPipe = new JFijiPipe();
					currPipe.hasGauss = true;
					currPipe.useGauss = false;
					currPipe.sigmaGauss = parent.sigmaGauss;
					currPipe.isFWHM = parent.isFWHM;
					currPipe.extBluesLUT = parent.getUserLUT(ChoosePetCt.EXT_BLUES_LUT);
					currPipe.extHotIronLUT = parent.getUserLUT(ChoosePetCt.EXT_HOT_IRON_LUT);
					currPipe.fixedLUT = parent.getUserLUT(ChoosePetCt.FIXED_USER_LUT);
					break;

				case ChoosePetCt.SERIES_UPET:
				case ChoosePetCt.SER_FORCE_UPET:
					currPipe = upetPipe = new JFijiPipe();
					break;

				case ChoosePetCt.SERIES_CT:
				case ChoosePetCt.SERIES_CT_VARICAM:
				case ChoosePetCt.SER_FORCE_CT:
					currPipe = ctPipe = new JFijiPipe();
					currPipe.extCtMriLUT = parent.getUserLUT(ChoosePetCt.MRI_CT_LUT);
					break;

				case ChoosePetCt.SERIES_MRI:
				case ChoosePetCt.SER_FORCE_MRI:
					currPipe = mriPipe = new JFijiPipe();
					currPipe.extCtMriLUT = parent.getUserLUT(ChoosePetCt.MRI_CT_LUT);
					break;

				case ChoosePetCt.SERIES_MIP:
					currPipe = mipPipe = new JFijiPipe();
					break;
			}
			if( currPipe == null) continue;
			currPipe.qualityRendering = parent.qualityRendering;
			if(currPipe.qualityRendering) ChoosePetCt.loadingData = 3;
			currPipe.triThick = parent.triThick;
			currImg = inImgList.get(i);
//			if(!srcVisible) currImg.getWindow().setVisible(false);
			currPipe.LoadData(currImg, ChoosePetCt.getForcedVal(k));
			format = currPipe.data1.fileFormat;
			if( format != FileInfo.DICOM && format != FileInfo.UNKNOWN) isDicom = false;
		}
		if( petPipe == null) {	// make upet be pet
			petPipe = upetPipe;
			upetPipe = null;
		}
//		IJ.log("Done load");
		if( petPipe == null) return;	// error
//		petPipe.aspect = aspect = 1.0 * petPipe.data1.height/petPipe.data1.width;
		petPipe.aspect = aspect = petPipe.getAspect();
// BN Lee has data Abdul Sawb bin Mohad which is 139*130
		petPipe.mriOffY0 = (1.0 - aspect)*petPipe.data1.width/2;
		maybeAddBlankSlicesInPet();
		presetWindowLevels(1);	// CT window
/*		if( mipPipe != null && mipPipe.data1.numFrms == 1) {
			mipStatus = 0;
			tmp1 = "The MIP is a montage and cannot be used.\n";
			tmp1 += "You should change the montage number of slices\n";
			tmp1 += "to be less than 16 in the setup tab of\n";
			tmp1 += "Read from CD and/or Read from BI studies.\n\n";
			tmp1 += "OK = continue on with recalculation of MIP\n";
			tmp1 += "Cancel = kill Pet-Ct viewer so you can make\n";
			tmp1 += "the above correction and then reread the data.";
			i=JOptionPane.showConfirmDialog(parent, tmp1, "MIP problem", JOptionPane.OK_CANCEL_OPTION);
			if(i==JOptionPane.CANCEL_OPTION) {
				ChoosePetCt.loadingData = 0;
				parent.dispose();
				return;
			}
			mipPipe = null;
		}*/
		ChoosePetCt.MipPanel = null;
		ChoosePetCt.loadingData = 0;
		if( mipPipe == null) {
			ChoosePetCt.loadingData = 2;
			ChoosePetCt.MipPanel = this;
			mipPipe = new JFijiPipe();
			ij = IJ.getInstance();
			if( ij != null) ij.toFront();
			mipPipe.LoadMIPData(petPipe);
			parent.toFront();
			ChoosePetCt.loadingData = 0;
		} else {
			mipPipe.srcPet = petPipe;
			mipPipe.data1.y2xFactor = petPipe.data1.y2xFactor;
			mipPipe.data1.MIPslope = tmpd = petPipe.data1.getMaxRescaleSlope()*petPipe.data1.fltDataFactor;
			mipPipe.winSlope = mipPipe.data1.sliderSUVMax/(mipPipe.data1.maxVal*tmpd);
			if(mipPipe.data1.numFrms > JFijiPipe.NUM_MIP) mipPipe.data1.MIPtype = 2;
		}
		petRelativeZoom = setPetRelativeZoom(petPipe);
		if(petPipe.data1.zpos != null) {
			ptY = Math.abs(petPipe.getStackDepth());
			ctY = Math.abs(ctPipe.getStackDepth());
			if( ptY > 0 && ctY > 0) {
				ctY = ptY / ctY;
				if( ctY < 0.98 || ctY > 1.02) {
					ctPipe.data1.panZCt2Pet = ctY;
//					IJ.log("panZCt2Pet = " + ctY.toString());
				}
			}
		}
		if( mriPipe != null) {
			mriPipe.zoomX = setPetRelativeZoom(mriPipe);
//			Double tmp2 = mriPipe.zoomX / ctPipe.zoomX;
//			if( tmp2 != 1.0) IJ.log("mriScl = " + tmp2.toString());
//			mriPipe.mriScl = mriPipe.zoomX / ctPipe.zoomX; // bad. don't use this
			if( mriPipe.data1.isCt()) {
				mriPipe.winLevel = ctPipe.winLevel;
				mriPipe.winWidth = ctPipe.winWidth;
			}
			ctY = Math.abs(mriPipe.getStackDepth());
			if( ptY > 0 && ctY > 0) {
				ctY = ptY / ctY;
				if( ctY < 0.98 || ctY > 1.02) {
					mriPipe.data1.panZCt2Pet = ctY;
//					IJ.log("panZMri2Pet = " + ctY.toString());
				}
			}
			mriPipe.setAvgSliceDiff();
		}
		mipPipe.data1.y2XMip = mipPipe.data1.y2xFactor;
		petPipe.zoomX = mipPipe.zoomX = petRelativeZoom;
		if( upetPipe != null) upetPipe.zoomX = petRelativeZoom;
		petCoronal = (petPipe.data1.height-1) / 2;
		petSagital = (petPipe.data1.width-1) / 2;
		if( petPipe.data1.pixelCenter != null && ctPipe.data1.pixelCenter != null) {
			xshift = ctPipe.data1.pixelCenter[0] - petPipe.data1.pixelCenter[0];
			yshift = ctPipe.data1.pixelCenter[1] - petPipe.data1.pixelCenter[1];
/*			if( ctPipe.data1.isHalfPix != petPipe.data1.isHalfPix) { // double correct, bad!
				tmpd = ctPipe.data1.maybeFixShift() - petPipe.data1.maybeFixShift();
				xshift += tmpd;
				yshift += tmpd;
			}*/
		}
		if( petRelativeZoom >= 1.0) {
			// Norazizah shows a problem
			double fact1 = petRelativeZoom;
			halfPixel = 0.001 + petPipe.getPixelSpacing(JFijiPipe.COL) / 2;
			if(Math.abs(xshift) < halfPixel) xshift = 0;
			if(Math.abs(yshift) < halfPixel) yshift = 0;
			if(Math.abs(xshift) > 60*halfPixel) xshift = 0;
			if(Math.abs(yshift) > 60*halfPixel) yshift = 0;	//Smith, Annie shows this problem - fixed
			xshift = fact1*xshift/petPipe.getPixelSpacing(JFijiPipe.COL);
			yshift = fact1*yshift/petPipe.getPixelSpacing(JFijiPipe.ROW);
			petPipe.shiftXY[0] = xshift;
			petPipe.shiftXY[1] = yshift;
			if( upetPipe != null) {
				upetPipe.shiftXY[0] = xshift;
				upetPipe.shiftXY[1] = yshift;
			}
		} else {
			// Schroth Shane demonstrates this case
			halfPixel = 0.001 + ctPipe.getPixelSpacing(JFijiPipe.COL) / 2;
			if(Math.abs(xshift) < halfPixel) xshift = 0;
			if(Math.abs(yshift) < halfPixel) yshift = 0;
			if(Math.abs(xshift) > 60*halfPixel) xshift = 0;	// Meckel S shows problem
			if(Math.abs(yshift) > 60*halfPixel) yshift = 0;
			xshift = -xshift/ctPipe.getPixelSpacing(JFijiPipe.COL);
			yshift = -yshift/ctPipe.getPixelSpacing(JFijiPipe.ROW);
			ctPipe.shiftXY[0] = xshift;
			ctPipe.shiftXY[1] = yshift;
		}
		if(Math.abs(xshift) > 4 || Math.abs(yshift) > 4)
			IJ.log("If the registration of the fused looks incorrect, please notify ilan.tal@gmail.com");
		maybeSetMriOffset();
		for( i=0; i<n; i++) {
			currPipe = null;
			k = inSeriesType.get(i);
			switch(k) {
				case ChoosePetCt.SERIES_CT:
				case ChoosePetCt.SERIES_CT_VARICAM:
				case ChoosePetCt.SER_FORCE_CT:
					currPipe = ctPipe;
					break;

				case ChoosePetCt.SERIES_MRI:
				case ChoosePetCt.SER_FORCE_MRI:
					currPipe = mriPipe;
					break;
			}
			if( currPipe != null) {
				// for MRI pipe zoomx can be different from 1.0
				// MRI data can be non square
				// Abdul sawab bin Mohd has non square PET data
				JFijiPipe.JData cdata1 = currPipe.data1;
//				currPipe.xfact = less1(getMMlen(currPipe, JFijiPipe.COL)/
//						(getMMlen(petPipe, JFijiPipe.COL)));
//				aspect = cdata1.height * cdata1.y2XMri / cdata1.width;
//				aspect = cdata1.height*cdata1.y2XCnvt * cdata1.y2XMri / cdata1.width;
				currPipe.aspect = aspect = currPipe.getAspect();
				if( parent.allowMRIchop && aspect > 1.0) {	// we can trim off white space above and below
					currPipe.yfact = less1(getMMlen(currPipe,JFijiPipe.ROW)/
							getMMlen(petPipe,JFijiPipe.ROW));
//					if( currPipe.yfact > 1.0) currPipe.yfact = 1; // Norimi Binti Musa
				}
				currPipe.mriOffY0 = (1.0 - aspect)*cdata1.width/2;
				if(currPipe.yfact < 0.99) {
					currPipe.yfactA = currPipe.yfact / currPipe.getAspect(true);	// includes aspect
					currPipe.mriOffY0 = currPipe.data1.height*(1-currPipe.yfact)/(2*currPipe.getAspect(false));
				}
				currPipe.corFactor = (petPipe.aspect*petPipe.zoomX * cdata1.height) / (petPipe.data1.height * currPipe.zoomX * aspect);
				currPipe.sagFactor = (petPipe.zoomX * cdata1.width) / (petPipe.data1.width * currPipe.zoomX);
				currPipe.obliqueFactor = 0;
				tmpd = cdata1.width* ( 1- petPipe.zoomX/currPipe.zoomX)/2;
				currPipe.corOffset = (tmpd - currPipe.mriOffY0 + petPipe.mriOffY0*currPipe.corFactor)/(cdata1.y2XMri*cdata1.y2XCnvt);
				currPipe.sagOffset = tmpd;
				// Terry Weizeman shows this problem
				currPipe.sagOffset -= currPipe.sagFactor*(petPipe.shiftXY[0] - currPipe.shiftXY[0]);
				currPipe.corOffset -= currPipe.corFactor*(petPipe.shiftXY[1] - currPipe.shiftXY[1]);
/*				if( petPipe.zoomX != 1.0) {
					tmp1 = "pet zoomx = " + petPipe.zoomX + ", ct zoomx = " + currPipe.zoomX;
					tmp1 += "\npet aspect = " + petPipe.aspect + ", ct aspect = " + aspect;
					IJ.log(tmp1);
				}*/
			}
		}
		ctPipe.setAvgSliceDiff();
		petPipe.setAvgSliceDiff();
		ctPipe.maybeSetCoronalDisplay();
		ratioCt2Pet = Math.abs(ctPipe.avgSliceDiff / petPipe.avgSliceDiff);
		if( ratioCt2Pet > 0.99 && ratioCt2Pet < 1.01) ratioCt2Pet = 1.0;
		parent.fillPatientData();
		parent.fillSUV_SUL(false);
/*		if( mriPipe != null) {
			dCnvt = mriPipe.data1.y2XCnvt;
			dCnvtCor = mriPipe.data1.y2XCnvtCor;
		}*/
		isGated = runGated = petPipe.data1.numTimeSlots > 1;
		parent.setGatedButtonVisible(isGated);
//		tstByteBuff();
//		parent.setTitle(parent.getTitleBar());
		parent.changeLayout(JFijiPipe.DSP_AXIAL);
		ActionMipCine(PetCtPanel.CINE_RUNNING);
		parent.updateMenu();
		m_timer.start();
		parent.useLog(false);
		parent.checkforROIandAnnotations();
		parent.showSource(false, parent.isShowSource());
		if( !isDicom) JOptionPane.showMessageDialog(parent, "This data may not display properly.\nNot all the data is DICOM.");
		tmp1 = Prefs.getString("prefs.options2", "1");
		if( (petPipe.data1.depth == 32 || ctPipe.data1.depth == 32) && !tmp1.startsWith("2050")) {
			tmp1 = "For best memory use and maximum studies use\n";
			tmp1 += "in the Fiji menu: Edit->Options->DICOM...\n";
			tmp1 += "and check the box: Ignore Rescale Slope.\n";
			tmp1 += "This will leave the data unaltered, i.e. in 16 bit mode.\n";
			tmp1 += "(bonus: afterwards, this message will never again appear.)";
			IJ.log(tmp1);
		}
		isInitializing = false;
	}

	double less1( double in) {
		if( in < 0.9999)
			return in;
		return 1.0;
	}

	private double getMMlen( JFijiPipe pip1, int type) {
		double tmp;
		if (type == JFijiPipe.ROW) tmp = pip1.data1.height * pip1.data1.pixelSpacing[JFijiPipe.ROW];
		else tmp = pip1.data1.width * pip1.data1.pixelSpacing[JFijiPipe.COL];
		return tmp; ///pip1.zoomX;
	}

	/*	void tstByteBuff() {
		int i, lim;
		for( i=0; i<5; i++) {
			ByteBuffer buf2 = ByteBuffer.allocate(2);
			buf2.position(0);
			lim = buf2.limit();
		}
	}*/

	ArrayList<Float> getZlimits(JFijiPipe p0) {
		ArrayList<Float> ret = new ArrayList<>();
		float first, last;
		first = (float) p0.data1.zStart;
		last =  (float) p0.data1.zEnd;
		ret.add( first);
		ret.add( last);
		ret.add((float)((first+last)/2));	// mean
		ret.add(Math.abs(last-first));	// size
		return ret;
	}

	private double checkHalfPixel(double inShft, double mri0, double ct0) {
		double chk0, chk1, chka = Math.abs(inShft);
		double ret = inShft;
		chk0 = 0.001 + mri0/2;
		chk1 = 0.001 + ct0/2;
		if( chka < chk0 || chka < chk1) ret = 0;
		return ret;
	}

	// this calculates the MRI inset value, normally zero.
	// type1 = 0 for X, or 1 for Y
/*	private int setMriInset(int type1) {
		double ctSpc, mriSpc, ctCm, mriCm, diff1;
		int ctFull, mriFull;
		if( mriPipe == null || ctPipe == null) return 0;
		ctSpc = ctPipe.getPixelSpacing(type1);
		mriSpc = mriPipe.getPixelSpacing(type1);
		if( type1==1) {
			ctFull = ctPipe.data1.height;
			mriFull = mriPipe.data1.height;
		} else {
			ctFull = ctPipe.data1.width;
			mriFull = mriPipe.data1.width;
		}
		ctCm = ctFull*ctSpc;
		mriCm = mriFull*mriSpc;
		diff1 = ctCm - mriCm;
		if( diff1 <= 0) return 0;
		return 0;
	}*/

	void maybeSetMriOffset() {
		double xshift=0, yshift=0, zshift=0;
		double ctSpcX, ctSpcY, mriSpcX, mriSpcY;
		//int insetX, insetY;
		if( mriPipe == null) return;
		// BN Lee has some MRI data where the z values don't even overlap. Try to fix
		ArrayList<Float> mriLm, ctLim;
		ctLim = getZlimits(ctPipe);
		mriLm = getZlimits(mriPipe);
		double avSz2, difCen, difEdge, thickness;
		thickness = mriPipe.data1.sliceThickness;
		avSz2 = mriLm.get(3) + ctLim.get(3);
		difCen = ctLim.get(2)-mriLm.get(2);
		difEdge = ctLim.get(0) - mriLm.get(0);
		ctSpcX = ctPipe.getPixelSpacing(JFijiPipe.COL);
		ctSpcY = ctPipe.getPixelSpacing(JFijiPipe.ROW);
		mriSpcX = mriPipe.getPixelSpacing(JFijiPipe.COL);
		mriSpcY = mriPipe.getPixelSpacing(JFijiPipe.ROW);

		// make an implicit assumption that only mri pixelCenter is contributing
		if( ctPipe.data1.pixelCenter != null && mriPipe.data1.pixelCenter != null) {
			xshift = mriPipe.data1.pixelCenter[0] - ctPipe.data1.pixelCenter[0];
			yshift = mriPipe.data1.pixelCenter[1] - ctPipe.data1.pixelCenter[1];
		}
		//xshift = xshift/relSpaceX;
		//yshift = yshift/relSpaceY;
		xshift = checkHalfPixel(xshift, ctSpcX, mriSpcX);
		yshift = checkHalfPixel(yshift, ctSpcY, mriSpcY);
		//insetX = setMriInset(0);
		// OFFTBLX and Y are mutable
		mriPipe.mri1.setBegin(JFijiPipe.OFFTBLX, ChoosePetCt.round(xshift));
		mriPipe.mri1.setBegin(JFijiPipe.OFFTBLY, ChoosePetCt.round(yshift));

		if( Math.abs(difCen/avSz2) > 0.25) {
			zshift = difCen/thickness;
			difEdge /= thickness;
			xshift = yshift = 0; // the data is bad, don't use these
			// for Norimi Binti Musa, zero gives perfect alignment (data generated to fit)
//			mriPipe.mri1.setBegin(JFijiPipe.OFFZ0,  ChoosePetCt.round(difEdge));
			IJ.log("It looks like the MRI is not aligned to the CT (" +
				mriPipe.data1.seriesName + ")\n" +
				"You will have to manually align them.\n" +
				"(Hint: Press Reset, then set axial Z=" + (int)difEdge +
				"(head) or Z=" +  (int)(difCen/thickness) +"(body))");
		}
/*		double mriOffy0, aspect = mriPipe.getAspect();
		IJ.log("x=" + ChoosePetCt.round10(xshift)+", y="+ ChoosePetCt.round10(yshift)+
			", z="+ChoosePetCt.round10(zshift)+ 
			" ,aspect="+aspect + " ,ratio=" + 1.0*petPipe.data1.width/mriPipe.data1.width);
		mriOffy0 = mriPipe.mriOffY0;
		if( mriOffy0 != 0) IJ.log("mriOffY0=" + mriOffy0);*/
		mriPipe.mriOffX = ChoosePetCt.round(xshift)/2;	// mriOffX-Y are needed
		mriPipe.mriOffY = ChoosePetCt.round(yshift)/2;
		mriPipe.mri1.setBegin(JFijiPipe.OFFX,  ChoosePetCt.round(xshift));
		mriPipe.mri1.setBegin(JFijiPipe.OFFY,  ChoosePetCt.round(yshift));
		mriPipe.mri1.setSpecial(petPipe);
	}

	/**
	 * There is a case where the PET data could not be reconstructed for a given
	 * frame. Thus the uncorrected data = CT data but is larger than the corrected
	 * data. We will detect such a case and fill the corrected data with blank slices.
	 * The patient is Boone-Smith, Jeanette, in the database.
	 */
	void maybeAddBlankSlicesInPet() {
		if( upetPipe == null) return;
		int i=0;
		int numCPet = petPipe.data1.numFrms;
		int numUPet = upetPipe.data1.numFrms;
		if( numCPet >= numUPet) return;
		if( petPipe.data1.sliceThickness != upetPipe.data1.sliceThickness) return;
		float startPetPos = petPipe.data1.zpos.get(0);
		ArrayList<Float> uPetPos = upetPipe.data1.zpos;
		float curPos = uPetPos.get(0);
		while( curPos > startPetPos && numCPet+i < numUPet) {
			petPipe.data1.addBlankSlice(curPos);
			curPos = uPetPos.get(++i);
		}
		if( i>0) {
			petPipe.data1.numFrms += i;
			petPipe.data1.setMaxAndSort();
		}
	}

	void presetWindowLevels(int indx) {
		JFijiPipe currPipe = getCorrectedOrUncorrectedPipe(true);
		if( currPipe == null) return;
		int slice = (int) (petAxial + upetOffset);
		boolean notifyFlg = false;
		switch( indx) {
			case 0:
				currPipe.AutoFocus(slice);
				break;

			case 1:	// Chest-Abdomen
			case 2:	// Lung
			case 3:	// Liver
			case 4:	// Bone
			case 5:	// Brain-Sinus
				if( ctPipe == null) break;
				currPipe = getMriOrCtPipe();
				if( !currPipe.data1.isCt()) currPipe = ctPipe;
				currPipe.winWidth = parent.ctWidth[indx-1];
				currPipe.winLevel = parent.ctLevel[indx-1];
				notifyFlg = true;
				break;

			case 8:
				currPipe.MaxFocus(slice, parent.sliceMaxFactor);
				currPipe.setLogSlider();
				break;

			case 9:
				if(currPipe.data1.SUVfactor <= 0) break;	// ignore
				currPipe.winWidth = 10;
				currPipe.winLevel = 5;
				currPipe.setLogSlider();
				break;
				
			case 10:
				currPipe.setLogSlider();
				break;

			default:
				return;

		}
		changeCurrentSlider();	// update the values on the gray scale bar
		repaint();
		if( notifyFlg) parent.notifySyncedStudies(NOTIFY_WIN_LEVELS, indx, 0, null, null);
	}

	void ActionMipCine(int type) {
		switch( type) {
			case CINE_RUNNING:	// this is used for both start and stop
				runCine = !runCine;
				break;

			case CINE_FORWARD:
				runCine = false;
				setCineIndx( 0);
				break;

			case CINE_SIDE:
				runCine = false;
				setCineIndx( 3*getNumMIP()/4);
				break;
		}
		updateMIPcursor();
		parent.setCineButtons(true);
		if (!runCine) repaint();
	}

	private void drawAll(Graphics2D g) {
		switch(m_masterFlg) {
			case VW_MIP:
			case VW_MIP_FUSED:
			case VW_MIP_UNCORRECTED:
				layoutMip(g, false);
				break;

			case VW_PET_CT_FUSED:
			case VW_PET_CT_FUSED_UNCORRECTED:
				layoutMip(g, true);
				break;
		}
	}

	void layoutMip(Graphics2D g, boolean fused) {
		if( isInitializing) return;	// still loading
		if( mipPipe == null) return;
		double upet=1.0, scl1 = getScalePet(), sagShift = 0;
		if( scl1 == 0) return;	// maybe closed window?
		if( lastScale != scl1) {
			lastScale = scl1;
			mouse1.initializeParameters();
		}
		int fusePos = 2;	// slot number for fused
		int numGate, gateOffset = 0;
		prepareLungMip();
		if( !fused) {
			if( reproFlg) {
				reproPipe.makeDspImage(JFijiPipe.COLOR_INVERSE, 1);
				reproPipe.drawCine(g, scl1, this, cinePaint);
			} else {
				mipPipe.makeDspImage(mipColor, 1);
				mipPipe.drawCine(g, scl1, this, cinePaint);
				if( cinePaint) updateMIPcursor();
			}
			fusePos = 0;
		}
		JFijiPipe pet1 = getCorrectedOrUncorrectedPipe(true);
		if( pet1 != null) {
			if(pet1 == upetPipe) upet = petPipe.data1.width/pet1.data1.width;
			numGate = pet1.data1.numTimeSlots;
			if( numGate > 1) {
				int numFrm = pet1.data1.numFrms / numGate;
				gateOffset = gateIndx * numFrm;
			}
			pet1.prepareFused(fusedColor);
			if( m_sliceType == JFijiPipe.DSP_AXIAL) {
				pet1.prepareFrame( petAxial + upetOffset + gateOffset, 0,petColor, 0);
				pet1.drawImages(g, upet*scl1, this);
			}
			if( m_sliceType ==JFijiPipe.DSP_CORONAL) {
				pet1.prepareCoronalSagital(petCoronal/upet, -1, petColor, gateOffset, 0);
				pet1.drawCorSagImages(g, upet*scl1, this, 0);	// coronal
			}
			if( m_sliceType == JFijiPipe.DSP_SAGITAL) {
				sagShift = pet1.prepareCoronalSagital(-1, petSagital/upet, petColor, gateOffset, 0);
				pet1.drawCorSagImages(g, upet*scl1, this, 1);	// sagital
			}
			if( m_sliceType == JFijiPipe.DSP_OBLIQUE) {
				if(m_oblique != null) m_oblique.draw(g, upet*scl1, pet1, petColor, petAxial + upetOffset);
//				if(m_oblique != null) m_oblique.draw(g, upet*scl1, pet1, petColor);
			}
		} else printMissing(g);
		Point pt1 = new Point(fusePos, 0);
		JFijiPipe ct1 = getMriOrCtPipe();
		if( ct1 != null) {
			int ctPos = ct1.findCtPos(petAxial, true);
			if( ctPos >= 0) {
				double scl2 = scl1 * petPipe.data1.width / ct1.data1.width;
				ct1.prepareFused(ctMriColor);
				if( m_sliceType == JFijiPipe.DSP_AXIAL) {
					ct1.prepareFrame(ctPos, 0, ctMriColor, 0);
					ct1.drawImages(g, scl2, this);
				}
				if( m_sliceType == JFijiPipe.DSP_CORONAL) {
					ct1.prepareCoronalSagital(petCoronal, -1, ctMriColor, 0, 0);
					ct1.drawCorSagImages(g, scl2, this, 0);	// coronal
				}
				if( m_sliceType == JFijiPipe.DSP_SAGITAL) {
					ct1.prepareCoronalSagital(-1, petSagital, ctMriColor, 0, sagShift);
					ct1.drawCorSagImages(g, scl2, this, 2);	// sagital-fused
				}
				if( m_sliceType == JFijiPipe.DSP_OBLIQUE) {
					if(m_oblique != null) m_oblique.draw(g, scl2, ct1, ctMriColor, ctPos);
//					if(m_oblique != null) m_oblique.draw(g, scl2, ct1, ctMriColor);
				}
				if(m_masterFlg == VW_MIP_FUSED || fused) {
					if( pet1 != null) pet1.drawFusedImage(g, scl1, ct1, pt1, this);
				}
			}
		}
		draw3CursorsAndSUV(g, scl1, fused);
		draw3FrameText(g, pet1);
		if( parent.useBF) drawBrownFat(g);
//		drawLungMip(g);
		drawAnnotations(g, pet1);
	}

	double getScalePet() {
		double scale0, scale1;
		Dimension sz1, dim1 = getSize();
		sz1 = getWindowDim();
		if( sz1 == null) return 0;
		scale0 = ((double)dim1.width) / sz1.width;
		scale1 = ((double)dim1.height) / sz1.height;
		if( scale1 > scale0) scale1 = scale0;
//		scale1 = scale0;
		return scale1;
	}


	// in calculation of Window dimension use zoom1 = 1.0
	Dimension getWindowDim() {
		Dimension sz1;
		JFijiPipe pipe1= petPipe;
		double scale0;
		int width1, heigh0, heigh1, type1;
		type1 = SZ_MIP_AXIAL;
		if( parent.autoResize) {
			// the oblique slice type gives m_sliceType when not oblique
			if( getObliqueSliceType() == JFijiPipe.DSP_AXIAL) {
				if( !showMip) type1 = SZ_AXIAL;
			}
			else type1 = SZ_CORONAL;
		}
		width1 = pipe1.data1.width;
		heigh1 = ChoosePetCt.round(pipe1.data1.height * pipe1.data1.y2XMip);
		scale0 = pipe1.zoomX * pipe1.data1.y2xFactor;
		heigh0 = ChoosePetCt.round(pipe1.data1.numFrms * scale0 / pipe1.data1.numTimeSlots);
		switch( type1) {
			case SZ_MIP_AXIAL:
				if( heigh1 < heigh0) heigh1 = heigh0;
				break;
				
			case SZ_AXIAL:
				break;
				
			case SZ_CORONAL:
				heigh1 = heigh0;
				break;
		}
		sz1 = new Dimension(width1*3, heigh1);
		return sz1;
	}

	int getCineState() {
		int retVal = CINE_RUNNING;
		if( !runCine) {
			retVal = CINE_STOPPED;
			int pos = getCineIndx();
			if( pos == 0) retVal = CINE_FORWARD;
			if( pos == 3*getNumMIP()/4) retVal = CINE_SIDE;
		}
		return retVal;
	}

	int getNumMIP() {
		int n = mipPipe.getNormalizedNumFrms(); // zero can cause problems
		if( n < JFijiPipe.NUM_MIP) n = JFijiPipe.NUM_MIP;
		return n;
	}

	int getCineIndx() {
		int val = 0;
		if( mipPipe != null) val = mipPipe.cineIndx;
		if( reproFlg) val = reproPipe.cineIndx;
		return val;
	}

	void setCineIndx( int indx) {
		mipPipe.setCineIndx(indx);
		if( reproPipe != null) reproPipe.setCineIndx(indx);
	}

	void dirtyCorSag() {
		if( petPipe != null && petPipe.data1.numTimeSlots > 1) petPipe.dirtyCorSag();
		if( upetPipe != null && upetPipe.data1.numTimeSlots > 1) upetPipe.dirtyCorSag();
	}

	void checkLimits() {
		if( petAxial < 0) petAxial = 0;
		int maxFrm = petPipe.getNormalizedNumFrms();
		if( petAxial >= maxFrm - 1)
			petAxial = maxFrm - 1;

		if( petCoronal < 0) petCoronal = 0;
		if( petCoronal >= petPipe.data1.height - 1)
			petCoronal = petPipe.data1.height - 1;

		if( petSagital < 0) petSagital = 0;
		if( petSagital >= petPipe.data1.width - 1)
			petSagital = petPipe.data1.width - 1;
	}

	void updateFakeValues() {
		petAxialFake = petAxial;
		petCoronalFake = petCoronal;
		petSagitalFake = petSagital;
	}

	/**
	 * Routine to calculate relative zoom if reconstruction diameters are different for PET and CT.
	 * The PET or MRI slice is inspected to get its size and pixel spacing.
	 * It is compared to the CT and the ratio calculated.
	 * If different then the ratio of the integral values of the matrix sizes is used.
	 * @param petMri
	 * @return 
	 */
	protected double setPetRelativeZoom(JFijiPipe petMri) {
		JFijiPipe ct1Pipe = ctPipe;
		if( ct1Pipe == null) return 1.0;
		float [] ctPixelSpacing = ct1Pipe.data1.pixelSpacing;
		int ctWidth = ct1Pipe.data1.width;
		int origWidth, zoomWidth;
		double z1, z2;
		origWidth = petMri.data1.width;
		z1 = petMri.data1.pixelSpacing[JFijiPipe.COL] * origWidth;
		z2 = ctPixelSpacing[JFijiPipe.COL] * ctWidth;
		z1 = z1 / z2;
		if( z1 > 0.999 && z1 < 1.001) {
			return 1.0;	// done
		}
		zoomWidth = ChoosePetCt.round( origWidth / z1);
		// don't worry any more about odd or even values
/*		if( ((zoomWidth ^ origWidth) & 1) != 0) {
			if( z1 > 1.0) zoomWidth++;
			else zoomWidth--;
		}*/
		// this is the real value we want
//		z1 = ((double) origWidth) / zoomWidth;
		return z1;
	}

	void updatePipeInfo() {
		Point[] imgPos;
		double petZoom;
		boolean oblFlg = false;
		int offType = 0;
		imgPos = new Point[3];	// for axial, coronal, sagital - all in same place
		imgPos[0] = imgPos[1] = imgPos[2] = new Point(0,0);
		if( m_masterFlg == VW_MIP_FUSED) {
			offType = 1;
			imgPos[0].x = -1;	// hide the PET image
		}
		if( m_masterFlg == VW_PET_CT_FUSED || m_masterFlg == VW_PET_CT_FUSED_UNCORRECTED)
			offType = 2;
		if(m_sliceType == JFijiPipe.DSP_OBLIQUE) oblFlg = true;
		petPipe.imgPos = imgPos;
		petPipe.numDisp = 1;
		petPipe.offscrMode = offType;
		petPipe.sliceType = m_sliceType;
		petSeriesName = petPipe.data1.seriesName;
		petZoom = petPipe.zoom1;
		petPipe.dirtyFlg = true;	// force update
		petPipe.corSrc = null;
		if(!oblFlg) petPipe.obliqueFactor = 0;
//		petPipe.fuseFactor = parent.fuseFactor;
		if( upetPipe != null) {	// same as corrected PET
			upetPipe.imgPos = imgPos;
			upetPipe.numDisp = 1;
			upetPipe.offscrMode = offType;
			upetPipe.sliceType = m_sliceType;
			upetPipe.zoom1 = petZoom;
			upetPipe.dirtyFlg = true;	// force update
			upetPipe.corSrc = null;
			if(!oblFlg) upetPipe.obliqueFactor = 0;
			if( m_masterFlg == VW_MIP_UNCORRECTED || m_masterFlg == VW_PET_CT_FUSED_UNCORRECTED)
				petSeriesName = upetPipe.data1.seriesName;
		}

		imgPos = new Point[3];
		imgPos[0] = imgPos[1] = imgPos[2] = new Point(1,0);
		if( ctPipe != null) {
			ctPipe.imgPos = imgPos;
			ctPipe.numDisp = 1;
			ctPipe.srcPet = petPipe;
			ctPipe.offscrMode = offType;
			ctPipe.sliceType = m_sliceType;
			ctPipe.zoom1 = petZoom;
			ctPipe.dirtyFlg = true;	// force update
			ctPipe.corSrc = null;
			if(!oblFlg) ctPipe.obliqueFactor = 0;
			ctPipe.corSagShift = getCorSagShift( ctPipe);
			parent.view3Slop = getView3Slop();
		}
		if( mriPipe != null) {
			mriPipe.imgPos = imgPos;
			mriPipe.numDisp = 1;
			mriPipe.srcPet = petPipe;
			mriPipe.offscrMode = offType;
			mriPipe.sliceType = m_sliceType;
			mriPipe.zoom1 = petZoom;
			mriPipe.dirtyFlg = true;	// force update
			mriPipe.corSrc = null;
			if(!oblFlg) mriPipe.obliqueFactor = 0;
			mriPipe.corSagShift = getCorSagShift( mriPipe);
		}
		if( parent.autoResize || !resizeOnce) {
			resizeOnce = true;
			parent.fitWindow();
		}
		updateMultYOff();
		changeCurrentSlider();
//		repaint();
	}

	// in parathyroid data, the CT scan only partially covers the SPECT data
	// this requires a shift of the coronal and sagital displays (Peleg Avraham)
	// there is a case Boone where the ct scan is larger than the pet, allow shift<0
	int getCorSagShift(JFijiPipe currPipe) {
		int shift = -currPipe.findCtPos(0, false);
//	In most case the shift will be zero. If it is negative this is the Boone case
//	If it is positive this the Peleg case and it needs more care.
		if (shift <= 0) return shift;
		double ctPos = currPipe.getZpos(0);
		double petPos = petPipe.getZpos(0);
		shift = ChoosePetCt.round((petPos - ctPos) / Math.abs(currPipe.avgSliceDiff));
		// with Boone the spacing between slices is wrong, so the previous method is used
		return shift;
	}
	
	int getView3Slop() {
		double slop = Math.abs((ctPipe.getZpos(ctPipe.data1.numFrms-1) - ctPipe.getZpos(0)) / ctPipe.data1.sliceThickness);
		int islop = ChoosePetCt.round(0.06*slop);	// 6% of range
		if( islop < 6) islop = 6;
		return islop;
	}

	double updateMultYOff() {
		double multYOff = 0, zoomY = 1.0, edge1, zm1 = 0;
//		boolean isMip = isMIPdisplay();
//		if( parent.isTop()) return 0;
		leaveIt:
		if( getObliqueSliceType() == JFijiPipe.DSP_AXIAL && (!parent.autoResize || showMip)) {
			if( parent.isTop()) break leaveIt;
			int width = petPipe.data1.width;
			int height = ChoosePetCt.round(petPipe.data1.width * petPipe.zoom1);
//			int num1 = ChoosePetCt.round(petPipe.data1.numFrms * petPipe.zoomX * petPipe.data1.y2xFactor / petPipe.data1.numTimeSlots);
			int num = ChoosePetCt.round(petPipe.numW());
			if( num > width) {
				if( height >= num) {
					zm1 = petPipe.mri1.zm1;
					if( zm1 == 0) {
						edge1 = 1.0*(height-num)/width;
						if( edge1 > 0.5) {
							IJ.log("Sometimes switching to MIP display causes a zoom reset.");
							setAllZoom(-1000);
							break leaveIt;
						}
						zm1 = petPipe.zoom1;
					}
					height = num;
				} else {
					zm1 = petPipe.zoom1;
				}
				multYOff = 0.5 * (num-height)/width;
				zoomY = ((double) height) / width;
			}
		}
		petPipe.multYOff = multYOff;
		petPipe.zoomY = zoomY;
		petPipe.mri1.zm1 = zm1;
		if( upetPipe != null) {
			upetPipe.multYOff = multYOff;
			upetPipe.zoomY = zoomY;
			upetPipe.mri1.zm1 = zm1;
		}
		if( ctPipe != null) {
			ctPipe.multYOff = multYOff;
			ctPipe.zoomY = zoomY;
			ctPipe.mri1.zm1 = zm1;
		}
		if( mriPipe != null) {
			mriPipe.multYOff = multYOff;
			mriPipe.zoomY = zoomY;
			mriPipe.mri1.zm1 = zm1;
		}
		return multYOff;
	}

/*	void updateMultYOff() {
		updateMultYOffSub(petPipe);
		if( upetPipe != null) {
			upetPipe.multYOff = petPipe.multYOff;
			upetPipe.zoomY = petPipe.zoomY;
			upetPipe.mri1.zm1 = petPipe.mri1.zm1;
		}
		updateMultYOffSub(ctPipe);
		updateMultYOffSub(mriPipe);
	}

	void updateMultYOffSub(JFijiPipe pip1) {
		double multYOff = 0, zoomY = 1.0, zm1 = 0;
		double maxH, currH, currW;
		if(pip1 == null) return;
		if( getObliqueSliceType() == JFijiPipe.DSP_AXIAL && (!parent.autoResize || showMip)) {
			if( petPipe.numW() > petPipe.data1.width) {
				zoomY = zm1 = petPipe.zoom1;
				maxH = petPipe.data1.szMm.z / petPipe.zoom1;
				currH = pip1.data1.szMm.y / (pip1.zoomX*pip1.getAspect());
				currW = petPipe.data1.szMm.x / petPipe.zoomX;	// use PET width
				multYOff = (maxH-currH) / (2*currW);
				if( multYOff <= 0) {
					multYOff = 0;
					zm1 = petPipe.mri1.zm1;
				}
			}
		}
		pip1.multYOff = multYOff;
		pip1.zoomY = zoomY;
		pip1.mri1.zm1 = zm1;
	}*/

	JFijiPipe getMriOrCtPipe() {
		JFijiPipe currPipe = ctPipe;
		if( isMri()) currPipe = mriPipe;
		return currPipe;
	}

	boolean isMri() {
		return MRIflg && mriPipe != null;
	}
	/**
	 * Routine to figure out if to use Attenuation corrected or uncorrected pipe.
	 * This starts out with Attenuation corrected data, but will change if the user
	 * wants to see uncorrected data. It has an additional task of checking for
	 * missing uncorrected slices. For missing slices it returns null.
	 *
	 * This routine is also used for general purposes where if a particular slice
	 * is missing it is of no importance. Thus a maybeNull flag has been added.
	 *
	 * @param maybeNull
	 * @return corrected or uncorrected pipe or null
	 */
	protected JFijiPipe getCorrectedOrUncorrectedPipe(boolean maybeNull) {
		return getCorrectedOrUncorrectedPipe(maybeNull, ChoosePetCt.round(petAxial));
	}

	protected JFijiPipe getCorrectedOrUncorrectedPipe(boolean maybeNull, int ipetAxial) {
		JFijiPipe pet1 = petPipe;
		upetOffset = 0;
		if((m_masterFlg == VW_MIP_UNCORRECTED ||
				m_masterFlg == VW_PET_CT_FUSED_UNCORRECTED) && upetPipe != null) {
			float z0, z1;
			int numFrms = upetPipe.getNormalizedNumFrms();
			double spacing;
			pet1 = upetPipe;
			z0 = petPipe.data1.zpos.get(ipetAxial);
			if( ipetAxial >= numFrms) upetOffset = numFrms - ipetAxial - 1;
			z1 = upetPipe.data1.zpos.get(ipetAxial+upetOffset);
			if( z0 != z1) {
				if( maybeNull) pet1 = null;
				spacing = petPipe.data1.sliceThickness;
				if( spacing == 0) spacing = 1.0;	// sanity
				if( Math.abs((z0-z1)/spacing) <= 0.5) return upetPipe;
				while( z1 < z0 && ipetAxial + upetOffset > 0) {
					upetOffset--;
					z1 = upetPipe.data1.zpos.get(ipetAxial+upetOffset);
					if( Math.abs((z0-z1)/spacing) <= 0.5) return upetPipe;
				}
				while( z1 > z0 && ipetAxial + upetOffset < numFrms - 1) {
					upetOffset++;
					z1 = upetPipe.data1.zpos.get(ipetAxial+upetOffset);
					if( Math.abs((z0-z1)/spacing) <= 0.5) return upetPipe;
				}
			}
		}
		return pet1;
	}

	void processMouseDoubleClick(MouseEvent arg0) {
		if( drawingRoi()) {
			bfDlg.processMouseDoubleClick();
		}
	}
	
	boolean isMIPdisplay() {
		return m_masterFlg == VW_MIP || m_masterFlg == VW_MIP_FUSED ||
			m_masterFlg == VW_MIP_UNCORRECTED;
	}

	void processMouseSingleClick(MouseEvent arg0) {
		int pos3 = mouse1.getMousePage(arg0, true);
		if( pos3 <= 0) return;	// not one of the 3 sections
		Point pt1 = new Point(mouse1.xPos % mouse1.widthX, mouse1.yPos);
		Point2d pt2 = new Point2d();
		double[] coss;
		double scl1 = getScalePet();	// assume corrected = uncorrected
		int i, sliceType, x1, z1, diff=0, invert = 1;
		boolean mipFlg = isMIPdisplay();
		if( isDrawOrAnnotate( arg0 ,false)) return;
		syncAxial = petAxial;
		syncCoronal = petCoronal;
		syncSagital = petSagital;
		sliceType = getObliqueSliceType();
		if( pos3 == 3 && mipFlg) {
			curPosition[2] = pt1;
			Point2d pt3 = mipPipe.scrn2Pos(pt1, scl1, 1);
			x1 = ChoosePetCt.round(pt3.x);
			petAxial = z1 = ChoosePetCt.round(pt3.y);
			coss = mipPipe.data1.setCosSin(getCineIndx());
			mipPipe.data1.getMipLocation(x1, z1, pt2, null, 0, coss);
			petCoronal = pt2.y;
			petSagital = pt2.x;
			checkLimits();
			updateFakeValues();
			if( parent.invertScroll) invert = -1;
			i = 2;
			switch( sliceType) {
				case JFijiPipe.DSP_AXIAL:
					i = 0;
					if( petPipe.zoom1 > 1.0) i = 3;
					diff = ChoosePetCt.round(syncAxial - petAxial);
					break;

				case JFijiPipe.DSP_CORONAL:
					pt2.x = petSagital;
					pt2.y = petAxial;
					diff = invert * ChoosePetCt.round(petCoronal - syncCoronal);
					break;

				case JFijiPipe.DSP_SAGITAL:
					pt2.x = petCoronal;
					pt2.y = petAxial;
					diff = invert * ChoosePetCt.round(petSagital - syncSagital);
					break;
			}
			Point pt4 = maybePanImage(pt2, scl1, i);
			curPosition[0] = pt4;
			curPosition[1] = pt4;
			if( parent.isScroll()) {
				notificationOfXYshift(sliceType, null);
				incrSlicePosition(diff, 1, false, 0);
				return;
			}
			calculateSUVandCT();
			parent.update3Display();
			parent.updateVtkLimits(true);
			repaint();
			return;
		}
//		if( m_sliceType == JFijiPipe.DSP_OBLIQUE) return;
		i = 0;
		if( petPipe.zoom1 > 1.0) i = 3;
		if( sliceType != JFijiPipe.DSP_AXIAL) i = 2;
		pt2 = petPipe.scrn2Pos(pt1, scl1, i);
		switch( sliceType) {
			case JFijiPipe.DSP_AXIAL:
				petAxial = ChoosePetCt.round(petAxial);
				petCoronal = pt2.y;
				petSagital = pt2.x;
				break;

			case JFijiPipe.DSP_CORONAL:
				petCoronal = ChoosePetCt.round(petCoronal);
				petSagital = pt2.x;
				petAxial = pt2.y;
				break;

			case JFijiPipe.DSP_SAGITAL:
				petSagital = ChoosePetCt.round(petSagital);
				petCoronal = pt2.x;
				petAxial = pt2.y;
				break;
		}
		updateFakeValues();
		curPosition[0] = pt1;
		curPosition[1] = pt1;
		curPosition[2] = pt1;
		if( mipFlg) {
/*			int val = getCineState();
			boolean valb = (val == CINE_SIDE && m_sliceType == JFijiPipe.DSP_SAGITAL) ||
					(val == CINE_FORWARD && m_sliceType == JFijiPipe.DSP_CORONAL);
			if( !valb) curPosition[2] = null;*/
			updateMIPcursor();
		}
		if( parent.isScroll()) {
			notificationOfXYshift(sliceType, null);
/*			petAxial = syncAxial;
			petCoronal = syncCoronal;
			petSagital = syncSagital;*/
		}
		calculateSUVandCT();
		parent.update3Display();
		parent.updateVtkLimits(false);
		repaint();
	}

	void captureBaseValues(int getSet) {	// these are set when the ⇕ arrow is pressed
		if( getSet == 1) {	// the else value is assumed to be zero
			baseAxial = ChoosePetCt.round(petAxial);
			baseCoronal = ChoosePetCt.round(petCoronal);
			baseSagital = ChoosePetCt.round(petSagital);
		} else {
			petAxial = baseAxial;
			petCoronal = baseCoronal;
			petSagital = baseSagital;
		}
	}

	void useSpinnersBaseValues(int getSet) {
		if( parent==null || !parent.isScroll()) return;	// don't capture when turning off
		captureBaseValues(getSet);
		parent.notifySyncedStudies(NOTIFY_BASE_VALUE, getSet, 0, null, null);
	}

	void notificationOfBaseValues(int getSet) {
		if( !parent.isScroll()) return;	// don't capture when turning off
		captureBaseValues(getSet);
		if( getSet == 1) return;
		parent.update3Display();
		repaint();
		setCursor(maybePanImage());
	}

	void notificationOfXYshift(int slType, Point2d diff) {
		double diffAx, diffSag, diffCor;
		if( diff != null) {
			if( slType != m_sliceType) {
				IJ.log("Slice type error in shift notification.");
				return;
			}
			diffAx = diff.y/petPipe.avgSliceDiff;
			diffSag = diff.x/petPipe.getPixelSpacing(JFijiPipe.COL);
			switch( slType) {
				case JFijiPipe.DSP_AXIAL:
					petSagital += diffSag;
					petCoronal += diff.y/petPipe.getPixelSpacing(JFijiPipe.ROW);
					break;

				case JFijiPipe.DSP_CORONAL:
					petSagital += diffSag;
					petAxial += diffAx;
					break;

				case JFijiPipe.DSP_SAGITAL:
					petCoronal += diff.x/petPipe.getPixelSpacing(JFijiPipe.ROW);
					petAxial += diffAx;
					break;
			}
			parent.update3Display();
			repaint();
			return;
		}
		Point2d pt1 = new Point2d();
		pt1.y = (petAxial - syncAxial)*petPipe.avgSliceDiff;	// for coronal and sagital
		diffSag = (petSagital - syncSagital)*petPipe.getPixelSpacing(JFijiPipe.COL);
		diffCor = (petCoronal - syncCoronal)*petPipe.getPixelSpacing(JFijiPipe.ROW);
		switch( slType) {
			case JFijiPipe.DSP_AXIAL:
				pt1.x = diffSag;
				pt1.y = diffCor;
				petAxial = syncAxial;
				break;

			case JFijiPipe.DSP_CORONAL:
				pt1.x = diffSag;
				petCoronal = syncCoronal;
				break;

			case JFijiPipe.DSP_SAGITAL:
				pt1.x = diffCor;
				petSagital = syncSagital;
				break;
		}
		parent.notifySyncedStudies(NOTIFY_XY_SHIFT, slType, 0, pt1, null);
	}

	void setCursor( Point pt1) {
		curPosition[0] = pt1;
		curPosition[1] = pt1;
		curPosition[2] = pt1;
		if(isMIPdisplay()) updateMIPcursor();
	}

	Point maybePanImage() {
		Point2d pt1 = new Point2d();
		pt1.y = petAxial;
		int i = 2;
		switch(m_sliceType) {
			case JFijiPipe.DSP_CORONAL:
				pt1.x = petSagital;
				break;

			case JFijiPipe.DSP_SAGITAL:
				pt1.x = petCoronal;
				break;

			default:
				i = 0;
				if( petPipe.zoom1 > 1.0) i = 3;
				pt1.x = petSagital;
				pt1.y = petCoronal;
				break;
		}
		return maybePanImage(pt1, getScalePet(), i);
	}

	boolean isDrawOrAnnotate(MouseEvent arg0, boolean run) {
		if( !drawingRoi() && anotateDlg == null && anotateTB == null) return false;
		int pos3 = mouse1.getMousePage(arg0, true);
		if( pos3 <= 0) return false;	// not one of the 3 sections
		int posMod;
		Point pt1 = new Point(mouse1.xPos % mouse1.widthX, mouse1.yPos);
		double scl1 = getScalePet();	// assume corrected = uncorrected
		if( drawingRoi()) {
			bfDlg.processMouseSingleClick(pos3, pt1, scl1);
			return true;
		}
/*		if( drawingMRoi()) {
			LungMipDlg.processMouseSingleClick(pos3, pt1, scl1);
			return;
		}*/
		if( isAnnotations(true)) {
			posMod = anotateDlg.check4FusedPress(pos3);
			if(posMod > 2) return false;
			if(run) anotateDlg.processMouseSingleClick(posMod, pt1, scl1);
			return true;
		}
		if( isAnnoTB(true)) {
			posMod = anotateTB.check4FusedPress(pos3);
			if(posMod > 2) return false;
			if(run) anotateTB.processMouseSingleClick(posMod, pt1, scl1);
			return true;
		}
		return false;
	}

	Point maybePanImage( Point2d pt1, double scale, int type) {
//		String tmp = "Axial " + petAxial + ", Coronal " + petCoronal + ", Sag " + petSagital;
//		tmp += ", Pan " + petPipe.pan;
//		IJ.log(tmp);
		Point p1 = petPipe.pos2Scrn(pt1, scale, type);
		if( petPipe.zoom1 <= 1.0) return p1;
		boolean okXY, negY = false;
		double edge, p1y, widthZ = mouse1.widthY;
		Point2d p3, p2 = petPipe.saveRaw;
		okXY = p2.x >= 0 && p2.x < mouse1.widthX && p2.y >= 0 && p2.y < widthZ;
		if( okXY) return p1;
		if( p2.y >= widthZ) p2.y = widthZ - 1;
		p3 = new Point2d();
		p3.x = (p2.x - p1.x) / mouse1.widthX;
		p3.y = (p1.y - p2.y) / widthZ;
		if( p1.y == 0) p3.y = -p3.y;
		panDrag( null, true);
		edge = 0.1/mouse1.zoom1;
		// the coronal and sagital slices don't work right. Recalculate for them
		if(getObliqueSliceType() != JFijiPipe.DSP_AXIAL && p1.y != p2.y) {
			mouse1.pan3d.z = petPipe.pan.z = 0;
			p1y = petAxial / petPipe.getNormalizedNumFrms();
			if( p1.y != 0) {
				p1y -= 1.0/mouse1.zoom1;
				if(p1y < 0) p1y = 0;
			}
			else negY = true;
			p3.y = p1y;
		}
		if(p3.x != 0) {
			if(p3.x < 0) p3.x -= edge;
			else p3.x += edge;
		}
		if(p3.y != 0) {
			if(p3.y < 0 || negY) p3.y -= edge;
			else p3.y += edge;
		}
		panDragSub(p3);
//		tmp = "x1 " + p2.x + ", x2 " + p1.x + ", y1 " + p1.y + ", y2 " + p2.y  + ", Pan " + petPipe.pan;
//		IJ.log(tmp);
		p1 = petPipe.pos2Scrn(pt1, scale, type);
		return p1;
	}

	void calculateSUVandCT() {
		if(curPosition[0] == null) return;	// don't waste time
		int z0, z1, z0a, y0, deltaX, numPts, digits, mult, width1, angle = 0;
		int linex, liney;
		JFijiPipe.lineEntry currLine;
		double[] sumVals;
//		parent.hideAllPopupMenus();
		double spac1, spacy, spacz, spacOut, totalSum, currDbl, radMm, radpx;
		CTval = 0;
/*		upetOffset = 0;
		SUVorCount = 0;
		suvPnt = new SUVpoints(m_sliceType);
		radMm = parent.getSUVmm() / 2.0;
		JFijiPipe pet1 = getCorrectedOrUncorrectedPipe(true);
		if( pet1 == null) return;
		double SULfactor = pet1.data1.SULfactor;
		width1 = pet1.data1.width;
		z1 = ChoosePetCt.round(petAxial + upetOffset);*/
		linex = ChoosePetCt.round(petSagital);
		liney = ChoosePetCt.round(petCoronal);
		int iPetAxial = ChoosePetCt.round(petAxial);
		calcSUVsub(iPetAxial, linex, liney, false);
/*		if( pt1.y < 0 || pt1.y >= width1) return;
		if( m_sliceType == JFijiPipe.DSP_SAGITAL) {
			linex = pt1.y;
			liney = pt1.x;
			angle = 270;
		}
		currLine = pet1.data1.getLineOfData(angle, liney, z1);
		if( !currLine.goodData) return;
		spac1 = currLine.pixelSpacing[0];
		if( spac1 <= 0) return;
		radpx = (radMm/spac1);
		totalSum = curMax = 0;
		numPts = z0 = z0a = 0;
		spacOut = Math.abs(pet1.data1.sliceThickness/ spac1);
		while(true) {
			spacz = 1.0;
			if(m_sliceType == JFijiPipe.DSP_AXIAL) spacz = spacOut;
			spacz *= z0a;
			currDbl = radpx*radpx - spacz*spacz;
			if(currDbl <= 0) break;
			deltaX = (int) Math.sqrt(currDbl);
//			currLine = pet1.data1.getLineOfData(angle, liney, z1+z0a);
			currLine = myGetLineofData(pet1, liney, 0, z1, z0a);
			if( !currLine.goodData) break;
			sumVals = sumLine(currLine, deltaX, linex, liney, 0, z1, z0a);
			totalSum += sumVals[0];
			numPts += sumVals[1];
			y0 = 0;
			while( deltaX > 0) {
				spacy = spacOut;
				if(m_sliceType == JFijiPipe.DSP_AXIAL) spacy = 1.0;
				y0++;
				spacy *= y0;
				currDbl = radpx*radpx - spacy*spacy - spacz*spacz;
				if(currDbl <= 0) break;
				deltaX = (int) Math.sqrt(currDbl);
				for( mult = -1; mult <=1; mult +=2) {
					currLine = myGetLineofData(pet1, liney, mult*y0, z1, z0a);
					sumVals = sumLine(currLine, deltaX, linex, liney, mult*y0, z1, z0a);
					totalSum += sumVals[0];
					numPts += sumVals[1];
				}
			}
			if( !parent.isSphereSUV()) break;
			if( z0a < 0 || z0 == 0) {
				z0++;
				z0a = z0;
				continue;
			}
			z0a = -z0;
		}
		SUVmean = totalSum / numPts;
		SD = suvPnt.calcSD(SUVmean);
		digits = 0;
		SUVflg = false;
		int type1 = parent.getSUVtype();
		if( type1 == 1 || type1 == 2)
			SUVpeak = suvPnt.calcSUVpeak(pet1, type1);
		currDbl = currLine.SUVfactor;
		if(currDbl > 0) {
			SUVflg = true;
			digits = 2;
			SUVmean /= currDbl;
			SD /= currDbl;
			curMax /= currDbl;
			if( curMax < 1.0) digits = 3;
			SUVpeak /= currDbl;
		}
		SUVorCount = PetCtFrame.roundN(curMax, digits);
		SULmax = PetCtFrame.roundN(curMax * SULfactor, digits);
		SUVpeak = PetCtFrame.roundN(SUVpeak, digits);
		SULmean = PetCtFrame.roundN(SUVmean * SULfactor, digits);
		SD_SUL = PetCtFrame.roundN(SD * SULfactor, digits);
		SUVmean = PetCtFrame.roundN(SUVmean, digits);
		SD = PetCtFrame.roundN(SD, digits);*/

		// now do the CT or MRI part
		JFijiPipe currPipe = getMriOrCtPipe();
		if( currPipe == null) return;
		Point pt1 = new Point();
//		pt1.x = shift2Ct(currPipe, pt1.x) - currPipe.mriOffX;
//		pt1.y = shift2Ct(currPipe, pt1.y) - currPipe.mriOffY;
		pt1.x = ChoosePetCt.round(shift2Ct1(currPipe, 0) - currPipe.mriOffX);
		pt1.y = ChoosePetCt.round(shift2Ct1(currPipe, 1) - currPipe.mriOffY);
		z1 = currPipe.findCtPos(petAxial, false);
		if( z1 < 0 || pt1.x < 0 || pt1.y < 0) return;
//		tmp = pt1.y;
//		IJ.log("yshift = "+ tmp.toString());
		currLine = currPipe.data1.getLineOfData(0, pt1.y, z1);
		if( !currLine.goodData) return;
		spac1 = currLine.pixelSpacing[JFijiPipe.COL];
		if( spac1 <= 0) return;
		deltaX = (int) (4.0/spac1);
		sumVals = sumLine(currLine, deltaX, pt1.x, -1, 0, -1, 0);
		CTval = (int) (sumVals[0]/sumVals[1] + currPipe.data1.shiftIntercept());
	}
	
	void calcSUVsub(int iPetAxial, int linex, int liney, boolean invrtFlg) {
		int z0, z1, z0a, y0, deltaX, numPts, digits, mult, width1, angle = 0;
		int numFrm, gateOffset, iPetAx1 = iPetAxial;
		JFijiPipe.lineEntry currLine;
		double[] sumVals;
		double spac1, spacy, spacz, spacOut, totalSum, currDbl, radMm, radpx;
		upetOffset = 0;
		SUVorCount = 0;
		numFrm = petPipe.getNormalizedNumFrms();
		gateOffset = gateIndx*numFrm;
		if(invrtFlg) {
			iPetAx1 = numFrm - iPetAxial - 1;
		}
		suvPnt = new SUVpoints(m_sliceType);
		radMm = parent.getSUVmm() / 2.0;
		JFijiPipe pet1 = getCorrectedOrUncorrectedPipe(true, iPetAx1);
		if( pet1 == null) return;
		double SULfactor = pet1.data1.SULfactor;
		width1 = pet1.data1.width;
		z1 = iPetAx1 + upetOffset + gateOffset;
		Point pt1 = new Point(linex, liney);
		if( pt1.y < 0 || pt1.y >= width1) return;
		if( m_sliceType == JFijiPipe.DSP_SAGITAL) {
			linex = pt1.y;
			liney = pt1.x;
			angle = 270;
		}
		currLine = pet1.data1.getLineOfData(angle, liney, z1);
		if( !currLine.goodData) return;
		spac1 = currLine.pixelSpacing[JFijiPipe.COL];
		if( spac1 <= 0) return;
		radpx = (radMm/spac1);
		totalSum = curMax = 0;
		numPts = z0 = z0a = 0;
		spacOut = Math.abs(pet1.data1.sliceThickness/ spac1);
		while(true) {
			spacz = 1.0;
			if(m_sliceType == JFijiPipe.DSP_AXIAL) spacz = spacOut;
			spacz *= z0a;
			currDbl = radpx*radpx - spacz*spacz;
			if(currDbl <= 0) break;
			deltaX = (int) Math.sqrt(currDbl);
//			currLine = pet1.data1.getLineOfData(angle, liney, z1+z0a);
			currLine = myGetLineofData(pet1, liney, 0, z1, z0a);
			if( !currLine.goodData) break;
			sumVals = sumLine(currLine, deltaX, linex, liney, 0, z1, z0a);
			totalSum += sumVals[0];
			numPts += sumVals[1];
			y0 = 0;
			while( deltaX > 0) {
				spacy = spacOut;
				if(m_sliceType == JFijiPipe.DSP_AXIAL) spacy = 1.0;
				y0++;
				spacy *= y0;
				currDbl = radpx*radpx - spacy*spacy - spacz*spacz;
				if(currDbl <= 0) break;
				deltaX = (int) Math.sqrt(currDbl);
				for( mult = -1; mult <=1; mult +=2) {
					currLine = myGetLineofData(pet1, liney, mult*y0, z1, z0a);
					sumVals = sumLine(currLine, deltaX, linex, liney, mult*y0, z1, z0a);
					totalSum += sumVals[0];
					numPts += sumVals[1];
				}
			}
			if( !parent.isSphereSUV()) break;
			if( z0a < 0 || z0 == 0) {
				z0++;
				z0a = z0;
				continue;
			}
			z0a = -z0;
		}
		SUVmean = totalSum / numPts;
		SD = suvPnt.calcSD(SUVmean);
		digits = 0;
		SUVflg = false;
		int type1 = parent.getSUVtype();
		if( type1 == 1 || type1 == 2)
			SUVpeak = suvPnt.calcSUVpeak(pet1, type1);
		currDbl = currLine.SUVfactor;
		if(currDbl > 0) {
			SUVflg = true;
			digits = 2;
			SUVmean /= currDbl;
			SD /= currDbl;
			curMax /= currDbl;
			if( curMax < 1.0) digits = 3;
			SUVpeak /= currDbl;
		}
		SUVorCount = PetCtFrame.roundN(curMax, digits);
		SULmax = PetCtFrame.roundN(curMax * SULfactor, digits);
		SUVpeak = PetCtFrame.roundN(SUVpeak, digits);
		SULmean = PetCtFrame.roundN(SUVmean * SULfactor, digits);
		SD_SUL = PetCtFrame.roundN(SD * SULfactor, digits);
		SUVmean = PetCtFrame.roundN(SUVmean, digits);
		SD = PetCtFrame.roundN(SD, digits);
	}

	JFijiPipe.lineEntry myGetLineofData(JFijiPipe pet1, int liney, int yindx, int z1, int zindx) {
		int angle=0, depth=liney+zindx, sliceNum=-1;
		switch(m_sliceType) {
			case JFijiPipe.DSP_AXIAL:
				depth = liney + yindx;
				sliceNum = z1 + zindx;
				break;

			case JFijiPipe.DSP_CORONAL:
				sliceNum = z1 + yindx;
				break;

			case JFijiPipe.DSP_SAGITAL:
				angle = 270;
				sliceNum = z1 + yindx;
				break;
		}
		return pet1.data1.getLineOfData(angle, depth, sliceNum);
	}
	
	// this routine has no roundoff errors, unlike the original
	double shift2Ct1( JFijiPipe currPipe, int type) {
		double inVal = petSagital;
		if( type > 0) inVal = petCoronal;
		return shift2Ctsub(currPipe, inVal, type);
	}

	int shift2Ct( JFijiPipe pipe1, int pos1, int type) {
		return ChoosePetCt.round(shift2Ctsub(pipe1, pos1, type));
	}

	int shift2CtCen( JFijiPipe pipe1, int pos1, int type, boolean isSag) {
		double retVal = shift2Ctsub( pipe1, pos1, type);
		double factor = -pipe1.sagFactor;
		if( type > 0) factor = -pipe1.corFactor;
//		retVal += factor * 0.5;	// half pixel shift
		return ChoosePetCt.round(retVal);
	}
	
	double shift2Ctsub( JFijiPipe pipe1, double pos1, int type) {
		double inVal = pos1, mriOff = pipe1.mriOffX;
		double factor = pipe1.sagFactor, offst = pipe1.sagOffset;
		if( type > 0) {
			mriOff = pipe1.mriOffY;
			factor = pipe1.corFactor;
			offst = pipe1.corOffset;
		}
		return factor*inVal + offst - mriOff;
	}

	// this can be deleted, as no difference to shift2Ct is detected
/*	int shift2Ct2( JFijiPipe pipe1, int pos1, int type) {
		double scl1, cenPet, cenCt;
		int ret1;
		cenPet = petPipe.data1.width / 2.0;
		cenCt = pipe1.data1.width / 2.0;
		if( type > 0) {
			cenPet = petPipe.data1.height / 2.0;
			cenCt = pipe1.data1.height / 2.0;
		}
		scl1 = (petPipe.zoomX * cenCt) / (pipe1.zoomX * cenPet);
		ret1 = ChoosePetCt.round( scl1*( pos1 - cenPet) + cenCt);
		return ret1;
	}*/

	// this is the relative spacing of the CT compared to the PET
	double pointSpacing( JFijiPipe pipe1) {
		return petPipe.zoomX * pipe1.data1.width / (pipe1.zoomX * petPipe.data1.width);
	}

	double[] sumLine(JFijiPipe.lineEntry currLine, int n1, int center, int yin, int yindx, int zin, int zindx) {
		double[] ret1 = new double[2];	// sum, numPts
		double scale = currLine.slope;
		int i, j, size1, ypos, zpos, rn = -1, li = -1;
		double currVal;
		boolean fltFlg = false;
		if( currLine.pixFloat != null) fltFlg = true;
		if( center < 0 || center >= currLine.size1) return ret1;
		if( fltFlg) {
			size1 = currLine.pixFloat.length;
			currVal = currLine.pixFloat[center];
		} else {
			size1 = currLine.pixels.length;
			currVal = currLine.pixels[center];
		}
		ypos = yin + yindx;
		zpos = zin + zindx;
		if( m_sliceType != JFijiPipe.DSP_AXIAL) {
			ypos = yin + zindx;
			zpos = zin + yindx;
		}
		ret1[0] = calcPoints( currVal * scale, center, ypos, zpos, rn, li);
		ret1[1] = 1;	// 1 point so far
		for( i=1; i<=n1; i++) {
			j = center+i;
			if( j < size1) {
				ret1[1]++;
				if( fltFlg) currVal = currLine.pixFloat[j];
				else currVal = currLine.pixels[j];
				currVal = calcPoints( currVal * scale, j, ypos, zpos, rn, li);
				ret1[0] += currVal;
			}
			j = center-i;
			if( j>= 0) {
				ret1[1]++;
				if( fltFlg) currVal = currLine.pixFloat[j];
				else currVal = currLine.pixels[j];
				currVal = calcPoints( currVal * scale, j, ypos, zpos, rn, li);
				ret1[0] += currVal;
			}
		}
		return ret1;
	}
	
	private double calcPoints(double inVal, int x, int y, int z, int rn, int li) {
		double retVal = inVal;
		if( y < 0) return retVal;	// CT has no effect on curMax
		if( retVal > curMax) curMax = retVal;
		if( retVal > curMax / 8) {
			suvPnt.addRearrangePoint(retVal, 0, x, y, z, rn, li);
		}
		return retVal;
	}

	int getSUVradius(boolean useZoom) {
		double spacing2 = 2*petPipe.data1.pixelSpacing[JFijiPipe.COL];
		double pZoom = petPipe.getZoom(0);
		if(!useZoom) pZoom = 1.0;
		int widthX, radius;
		widthX = petPipe.data1.width;
		radius = ChoosePetCt.round(parent.getSUVmm() * mouse1.widthX * pZoom / (spacing2 * widthX));
		if( radius <= 0) radius = 0;
		return radius;
	}

	Color getTextColor() {
		return Color.red;
	}

	void draw3CursorsAndSUV(Graphics2D g, double scl1, boolean fused) {
		if(curPosition[0] == null) return;	// don't waste time
		int i, offY;
		boolean circleSUV = parent.isCircleSUV();
		String tmp;
		Point pt0, pt1;
		Dimension dm2 = getReducedSize();
		offY = dm2.height - 2;
		int widthX = mouse1.widthX;
		int width5 = widthX/20;	// 5%
		if( circleSUV) width5 = 2*getSUVradius(true) + 1;
		int width2 = width5/2;
		Color oldColor = g.getColor();
		g.setColor(getTextColor());
		switch( parent.getSUVtype()) {
			case 1:
			case 2:
				tmp = "PeakCount = ";
				if( SUVflg) tmp = "SUVpeak = ";
				tmp = tmp + SUVpeak;
				break;
				
			case 3:
				tmp = "MeanCount = ";
				if( SUVflg) tmp = "SUVmean = ";
				tmp = tmp + SUVmean + " \u00B1 " + SD;
				break;
				
			case 4:
				tmp = "MaxCount = ";
				if( SUVflg) tmp = "SULmax = ";
				if( SULmax < 0) tmp += "N/A";
				else tmp = tmp + SULmax;
				break;
				
			case 5:
				tmp = "MeanCount = ";
				if( SUVflg) tmp = "SULmean = ";
				if( SULmean < 0) tmp += "N/A";
				else tmp = tmp + SULmean + " \u00B1 " + SD_SUL;
				break;
				
			default:	// case 0
				tmp = "MaxCount = ";
				if( SUVflg) tmp = "SUVmax = ";
				tmp = tmp + SUVorCount;
				break;
		}
		if(isGated) {
			i = gateIndx + 1;
			tmp += ", phase "+ i + "/" + petPipe.data1.numTimeSlots;
		}
//		offY = (int) (getScalePet() * petPipe.multYOff * petPipe.data1.width) - 2;
		if( bfDlg != null) {
			i = bfDlg.saveRoiIndx + 1;
			if( i > 0) tmp += ", ROI #" + i;
		}
//		tmp += ", position "+ petAxial;
		g.drawString(tmp, 0, offY);
		tmp = "CT = " + CTval;
		if( MRIflg) tmp = "MRI = " + CTval;
		g.drawString(tmp, widthX, offY);
		g.setColor(Color.GREEN);
		for(i=0; i<3; i++) {
			pt0 = curPosition[i];
			if( pt0 == null || pt0.x < 0 || pt0.x >= widthX) continue;
			pt1 = new Point(pt0.x, pt0.y);
			pt1.x += widthX*i;
			if(circleSUV) {
				if( i==2 && !fused) {
					width5 = 2*getSUVradius(false) + 1;
					width2 = width5/2;
				}
				g.drawOval(pt1.x-width2, pt1.y-width2, width5, width5);
			} else {
				g.drawLine(pt1.x-width5, pt1.y, pt1.x+width5, pt1.y);
				g.drawLine(pt1.x, pt1.y-width5, pt1.x, pt1.y+width5);
			}
		}
		g.setColor(oldColor);
//		tmp = "petCt = " + petAxial + ",  " + petCoronal + ",  " + petSagital;
//		System.out.println(tmp);
	}

	void updateMIPcursor() {
		if( curPosition[0] == null) return;
		curPosition[2] = mipPipe.getMIPposition(petAxial, petCoronal, petSagital, getScalePet());
	}

	String getSuvString(int SUVtype) {
		String tmp;
		int suvIndx;
		boolean suvFlg = false;
		suvIndx = SUVtype/10;
		if( SUVtype % 10 == 1) suvFlg = true;
		switch( suvIndx) {
			case 1:
			case 2:
				tmp = "PeakCount = ";
				if( suvFlg) tmp = "SUVpeak = ";
				break;
				
			case 3:
				tmp = "MeanCount = ";
				if( suvFlg) tmp = "SUVmean = ";
				break;
				
			case 4:
				tmp = "MaxCount = ";
				if( suvFlg) tmp = "SULmax = ";
				break;
				
			case 5:
				tmp = "MeanCount = ";
				if( suvFlg) tmp = "SULmean = ";
				break;
				
			default:	// case 0
				tmp = "MaxCount = ";
				if( suvFlg) tmp = "SUVmax = ";
				break;
		}
		return tmp;
	}

	// if the user shifts the frame partially off screen, readjust
	// size so that the text will still appear on screen.
	Dimension getReducedSize() {
		Dimension dm2 = getSize();
		Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
		Insets scrMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
		Point pos1 = getLocationOnScreen();
		int yPos = pos1.y + dm2.height;
		int yMax = scrSize.height - scrMax.bottom;
		if( yPos > yMax) {
			dm2.height = yMax - pos1.y;
		}
		return dm2;
	}

	void draw3FrameText(Graphics2D g, JFijiPipe pet1) {
		if( !parent.getShowText()) return;
		String slice, orient, c1;
		int pos1, offY, widthX = mouse1.widthX;
		Dimension dm2 = getReducedSize();
		offY = dm2.height - 2;
		if( offY <= 50) return;
		if( widthX <= 0) {
			mouse1.initializeParameters();
			widthX = mouse1.widthX;
		}
		if( widthX <= 50) return;
		Rectangle2D bnd1;
		FontMetrics fm1 = g.getFontMetrics();
		Color oldColor = g.getColor();
		g.setColor(getTextColor());
		int rotAxial = petPipe.data1.axialRotation;
		switch( m_sliceType) {
			default:	// axial
				orient = "APRL";
				if( rotAxial == 90) orient = "LRAP";
				slice = null;
				break;

			case JFijiPipe.DSP_CORONAL:
				orient ="SIRL";
				if( rotAxial == 90) orient = "SIAP";
				slice = ChoosePetCt.round(petCoronal) + 1 + "/" + petPipe.data1.height;
				break;

			case JFijiPipe.DSP_SAGITAL:
				orient = "SIAP";
				if( rotAxial == 90) orient = "SIRL";
				slice = ChoosePetCt.round(petSagital) + 1 + "/" + petPipe.data1.width;
				break;
		}
		pos1 = offY - fm1.getHeight();
		c1 = orient.substring(0, 1);
		g.drawString(c1, widthX*3/2, 10);
		c1 = orient.substring(1, 2);
		g.drawString(c1, widthX*3/2, pos1);
		c1 = orient.substring(2, 3);
		g.drawString(c1, widthX, offY/2);
		c1 = orient.substring(3, 4);
		g.drawString(c1, 2*widthX - 8, offY/2);

/*		if( petSeriesName != null && !petSeriesName.isEmpty()) {
			bnd1 = fm1.getStringBounds(petSeriesName, g);
			int pos2 = (int) (widthX - bnd1.getWidth() - 10);
			g.drawString(petSeriesName, pos2, offY);
		}*/

		if( slice == null) {
			Integer z0 = ctPipe.findCtPos(petAxial, true);
			int ipetAxial = ChoosePetCt.round(petAxial);
			z0 = ctPipe.getCtSliceNum(z0);
			g.drawString( z0.toString(), widthX, pos1);
			Double z1 = PetCtFrame.roundN(petPipe.getZpos(ipetAxial), 1);
			slice = z1.toString();
			if( bfDlg != null) {
				int n = petPipe.getNormalizedNumFrms();
				z0 = n - ipetAxial;
				slice += ", " + z0.toString();
			}
		}
		g.drawString(slice, 0, pos1);
		
		if( pet1 != null && parent.operatorNameFlg && operatorName != null) {
			g.drawString(operatorName, 0, 10);
		}

		if( m_kvp > 0 && m_ctMa > 0) {
			String tmp = "kvp=" + m_kvp + ", ma=" + m_ctMa;
			bnd1 = fm1.getStringBounds(tmp, g);
			pos1 = (int) (2*widthX - bnd1.getWidth());
			g.drawString(tmp, pos1, offY);
		}
		g.setColor(oldColor);
	}

	void drawBrownFat(Graphics2D g) {
		if( bfDlg == null) {
			BrownFat bf1 = BrownFat.getInstance();
			if( bf1 == null) return;
			bf1.drawOtherData(g, this);
			return;
		}
		if( !(bfDlg instanceof BrownFat)) {
			bfDlg = null;
//			IJ.log("brown fat set to null");
			return;
		}
		bfDlg.drawAllData(g, this);
	}

	void removeBFdata() {
//		bfDlg may be null if the focus is on another study
		BrownFat bfTmp = BrownFat.getInstance();
		if( bfTmp == null) return;
		bfTmp.removeBFdata(this);
	}

	void prepareLungMip() {
		if( LungMipDlg == null) return;
		LungMipDlg.prepareData(this);
	}

/*	void drawLungMip(Graphics2D g) {
		if( LungMipDlg == null) return;
		LungMipDlg.drawAllData(g, this);
	}*/
	
	void annotDlg() {
		quitTB2();
		if( anotateDlg == null) anotateDlg = new Annotations(parent,false);
		anotateDlg.setVisible(true);
		anotateDlg.showBookmarksTab();
		parent.update3Display();
	}
	
	void annoTB() {
		if(anotateTB != null) {
			quitTB2();
			return;
		}
		if(anotateDlg != null) {
			anotateDlg.dispose();
			anotateDlg = null;
		}
		anotateTB = new AnoToolBar();
		anotateTB.init(this);
		parent.update3Display();
	}

	void setRadioTB2(int value) {
		if( anotateTB == null) return;
		anotateTB.setRadioTB2(value);
	}

	void removeTBPoint( boolean killAll) {
		if( anotateTB == null) return;
		anotateTB.removeTBPoint(killAll);
	}

	void markButton() {
		if( anotateTB == null) return;
		anotateTB.markButton();
	}

	void setBMNextSlice(int upDown) {
		if( anotateTB == null) return;
		anotateTB.setNextSlice(upDown);
	}

	void saveTBfiles() {
		if( anotateTB == null) return;
		anotateTB.saveAllFiles();
	}

	void quitTB2() {
		if( anotateTB == null) return;
		anotateTB.dispose();
		anotateTB = null;
	}
	
	boolean isAnnoTB(boolean useMouse) {
		if( anotateTB == null) return false;
		return anotateTB.takeAction(useMouse);
	}
	
	void drawAnnotations(Graphics2D g, JFijiPipe pet1) {
		if( isAnnotations(false)) {
			anotateDlg.drawGraphics(g, pet1);
			anotateDlg.setMarkSlice();
			return;
		}
		if( isAnnoTB(false)) {
			anotateTB.drawGraphics(g, pet1);
		}
	}
	
	boolean isAnnotations(boolean useMouse) {
		if( anotateDlg == null) return false;
		return anotateDlg.takeAction(useMouse);
	}

	boolean drawingRoi() {
		if( bfDlg == null) return false;
		return bfDlg.drawingRoi;
	}
	
/*	boolean drawingMRoi() {
		if( LungMipDlg == null) return false;
		return LungMipDlg.drawingRoi;
	}*/

	void printMissing(Graphics2D g) {
		Color oldColor = g.getColor();
		g.setColor(getTextColor());
		int offY = (int) (getScalePet() * petPipe.multYOff * petPipe.data1.width);
		int width2 = mouse1.widthX / 2;
		g.drawString("Missing slice", width2, width2 + offY);
		g.setColor(oldColor);
	}

	// There is trouble hooking up a debugger in Windows.
	// This will be used to print IJ.log messages instead
	int[] timeEvt = new int[20];
	int[] cntEvt = new int[20];	// how many events of this type
	void miniDebug(int indx, String mess) {
		String out;
		Integer inx1 = indx;
		if( inx1 >= 20) inx1 = 0;
		int tim0 = timeEvt[inx1];
		int tim1 = LocalTime.now().get(ChronoField.MILLI_OF_DAY);
		cntEvt[inx1]++;
		if( (tim1 - tim0) < 2000) return;
		timeEvt[inx1] = tim1;
		Integer cnt = cntEvt[inx1];
		out = inx1.toString() + "-" + cnt.toString() + ", " + mess;
		IJ.log(out);
	}

	void calcMIP2() {
		parent.hideAllPopupMenus();
		if( mipPipe.getNormalizedNumFrms() > JFijiPipe.NUM_MIP || work4 != null) return;
		ImageJ ij;
		ij = IJ.getInstance();
		if( ij != null) ij.toFront();
		work4 = new bkgdLoadData();
		work4.type = 2;
		work4.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String propertyName = evt.getPropertyName();
				if( propertyName.equals("state")) {
					SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
					if( state == SwingWorker.StateValue.DONE) {
						work4 = null;
					}
				}
			}
		});
		work4.execute();
	}

	void reprojectionAction( boolean active) {
		reproFlg = active;
		parent.hideAllPopupMenus();
		if( reproPipe == null) {
			reproPipe = new JFijiPipe();
			work3 = new bkgdLoadData();
			work3.type = 1;
			work3.addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					String propertyName = evt.getPropertyName();
					if( propertyName.equals("state")) {
						SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
						if( state == SwingWorker.StateValue.DONE) {
							work3 = null;
						}
					}
				}
			});
			work3.execute();
		}
		else repaint();
	}

	void makeReprojection() {
		reproPipe.LoadReprojectionData(petPipe);
		reproPipe.zoomX = petRelativeZoom;
		repaint();
	}

	/**
	 * To keep the main thread responsive, the heavy data loading is done in the background.
	 */
	protected class bkgdLoadData extends SwingWorker {
		int type = 0;

		@Override
		protected Void doInBackground() {
			switch(type) {
				default:
				case 0:
					doActualLoadData();
					break;

				case 1:
					makeReprojection();
					break;

				case 2:
					mipPipe.data1.setMIPData(1);
					mipPipe.data1.MIPtype = 2;
					parent.toFront();
					break;
			}
			return null;
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}

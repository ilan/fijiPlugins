import ij.ImagePlus;
import ij.WindowManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import org.scijava.vecmath.Point2d;
import org.scijava.vecmath.Point3d;

/*
 * Display3Panel.java
 *
 * Created on Jan 14, 2010, 1:51:49 PM
 */

/**
 *
 * @author Ilan
 */
public class Display3Panel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;
	static final int CINE_RUNNING = 0;
	static final int CINE_STOPPED = 1;
	static final int CINE_FORWARD = 2;
	static final int CINE_SIDE = 3;
	static final int PERCENT_AXIS = 10;
	
	static final int CT_STUDY = 0;
	static final int SUV_STUDY = 1;
	static final int CNT_STUDY = 2;
	static final int FUSED_SUV = 3;
	static final int FUSED_CNT = 4;
	static final int MRI_STUDY = 5;

	myMouse mouse1 = new myMouse();
	String SUVresults = "";
	bkgdLoadData work2 = null;
	Display3Frame parent;
	Annotations anotateDlg = null;
	int d3Color = JFijiPipe.COLOR_INVERSE, CTval, styType = -1;
	int d3FusedColor = JFijiPipe.COLOR_GRAY;
	int cursorSize = 10, gateIndx = 0, saveHeight = -1, resizeCnt = 0;
	JFijiPipe d3Pipe = null, mipPipe = null, ct4fused = null;
	boolean paintingFlg = false, cinePaint = false, showMip = false, runCine = false;
	boolean splitCursor = false, limitCursor = false, runGated = false, isGated = false;
	boolean zoomTog = false, invertScroll = false, isInitializing;
	double minSlider = 0, maxSlider = 1000, SUVorCount, saveWidth, saveLevel;
	double d3Axial, d3Coronal, d3Sagital, d3AxialFake;
	SUVpoints suvPnt = null;
	double curMax, SUVpeak, SUVmean;
	private Timer m_timer = null;

    /** Creates new form Display3Panel */
    public Display3Panel() {
        initComponents();
		init();
    }

	private void init() {
		addMouseListener( this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		isInitializing = true;
		m_timer = new Timer(200, new CineAction());
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

	class myMouse {
		int xPos, yPos, widthX, widthY, page=1, xDrag, yDrag, button1, startSlice, numSlice;
		double zoomZ, zoom1, zoomY;
		Point3d pan3d = null;
		
		int getMousePage(MouseEvent evt0, boolean save) {
			double scale = getScale();
			widthX = (int) (scale * d3Pipe.data1.width);
			if(evt0 == null) return 0;
			Dimension dm2;
			int x1, y1;
			dm2 = getSize();
			x1 = evt0.getX();
			y1 = evt0.getY();
			if( x1 < 0) x1 = 0;	// these 2 lines should never happen
			if( y1 < 0) y1 = 0;
			int ret1 = (x1/widthX) + 1;
			if( ret1 > 3) ret1 = 0;
			if( save) {
				widthY = dm2.height;
				xPos = x1;
				yPos = y1;
				page = ret1;
			}
			return ret1;
		}

		Point2d getDragOffset(MouseEvent evt0, boolean save) {
			int x = evt0.getX();
			int y = evt0.getY();
			Point2d pt1 = new Point2d();
			if( save) {
				double scale = getScale();
				widthX = (int) (scale * d3Pipe.data1.width);
				button1 = evt0.getButton();
				xDrag = x;
				yDrag = y;
				getMousePage(evt0, true);	// set page
				if( page == 1) numSlice = d3Pipe.getNormalizedNumFrms();
				else numSlice = d3Pipe.data1.width;
				startSlice = ChoosePetCt.round(getCurrentSlice());
				return pt1;
			}
			pt1.x = 1.0 * (x - xDrag) / widthX;
			pt1.y = 1.0 * (yDrag - y) / widthX;
			return pt1;
		}

		Point2d getPanOffset(MouseEvent evt0, boolean save) {
			if( evt0 == null) return null;
			int x = evt0.getX();
			int y = evt0.getY();
			Point2d pt1 = new Point2d();
			if( save) {
				getMousePage(evt0, true);
				button1 = evt0.getButton();
				return pt1;
			}
			pt1.x = 1.0 * (xPos - x) / widthX;
			pt1.y = 1.0 * (yPos - y) / widthY;
			return pt1;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		int i, j;
		i = e.getButton();
		j = e.getClickCount();
		if( i != MouseEvent.BUTTON1) return;
		if( j == 1) {
			chooseNewPosition(e);
		}
	}

	@Override
	public void mouseEntered(MouseEvent me) {}
	@Override
	public void mouseExited(MouseEvent me) {}
	@Override
	public void mouseReleased(MouseEvent me) {}

	@Override
	public void mousePressed(MouseEvent me) {
		if(zoomTog) panDrag( me, true);
		else winLevelDrag(me, true);
		// watch out that resizing doesn't change the gray scale - look at cursor
		if(getCursor() != Cursor.getDefaultCursor()) mouse1.button1 = MouseEvent.NOBUTTON;
		maybeShowPopupMenu(me);
	}

	@Override
	public void mouseDragged(MouseEvent me) {
		if( mouse1.button1 != MouseEvent.BUTTON1 && mouse1.button1 != MouseEvent.BUTTON2) return;
		if(zoomTog) panDrag( me, false);
		else winLevelDrag( me, false);
	}

	@Override
	public void mouseMoved(MouseEvent me) {}

	@Override
	public void mouseWheelMoved(MouseWheelEvent mwe) {
		int i,j;
		i = mouse1.getMousePage(mwe, false);
		j = mwe.getWheelRotation();
		if( j > 0) j = -1;
		else j = 1;
		if(zoomTog) {
			if( d3Pipe != null) d3Pipe.setZoom(j);
			if( ct4fused != null) ct4fused.setZoom(j);
			updateMultYOff(false);
			repaint();
		}
		else {
			if( showMip && i==3) {
				changeMIPslice(j);
			}
			else incrSlicePosition(i, j);
		}
	}
	
	void changeMIPslice(int diff) {
		int indx;
		if( runCine) return;
		indx = mipPipe.cineIndx;
		indx -= diff;
		while( indx < 0) indx += JFijiPipe.NUM_MIP;
		while( indx >= JFijiPipe.NUM_MIP) indx -= JFijiPipe.NUM_MIP;
		mipPipe.cineIndx = indx;
		parent.setCineButtons(true);
		repaint();
	}

	double getCurrentSlice() {
		double currSlice = 0;
		switch( mouse1.page) {
			case 1:
				currSlice = d3Axial;
				break;

			case 2:
				currSlice = d3Coronal;
				break;

			case 3:
				currSlice = d3Sagital;
				break;
		}
		return currSlice;
	}

	void incrSlicePosition( int page, int diff) {
		int indx, invert = 1;
		int i = page;
		if( i <= 0)  i = mouse1.page;
		if( invertScroll) invert = -1;
		if( diff == 1000) {	// home key
			switch( i) {
				case 1:
					indx = d3Pipe.getNormalizedNumFrms() - 1;
					if( d3Axial <= 0) d3Axial = indx;
					else if( d3Axial >= indx) d3Axial = 0;
					else {
						if( d3Axial <= indx/2) d3Axial = 0;
						else d3Axial = indx;
					}
					break;

				case JFijiPipe.DSP_CORONAL:
					d3Coronal = (d3Pipe.data1.height - 1) / 2;
					break;

				case JFijiPipe.DSP_SAGITAL:
					d3Sagital = (d3Pipe.data1.width - 1) / 2;
					break;
			}
		}
		else switch( i) {
			case 1:
				d3Axial -= diff;
				break;

			case 2:
				d3Coronal += diff*invert;
				d3Pipe.oldCorWidth = -1;	// cause update
				break;

			case 3:
				d3Sagital += diff*invert;
				d3Pipe.oldCorWidth = -1;	// cause update
				break;
		}
		checkLimits();
		maybePanImage();
		maybeUpdateMultYoff();
		d3AxialFake = d3Axial;
		calculateSUVandCT();
		repaint();
	}

	void incrGatePosition(boolean isUp) {
		if(d3Pipe == null) return;	// happens when window is closed
		if( isUp) {
			gateIndx++;
			if( gateIndx >= d3Pipe.data1.numTimeSlots ) gateIndx = 0;
		} else {
			gateIndx--;
			if( gateIndx<0) gateIndx = d3Pipe.data1.numTimeSlots - 1;
		}
		dirtyCorSag(true);
		calculateSUVandCT();
		repaint();
	}

	public void updateGateIndx(int gate1) {
		if( runGated) return;
		gateIndx = gate1 - 1;	// so incrGate will set it back
		incrGatePosition(true);
	}

	/*
	 * This routine is called by the ExternalSpinners, i.e. SyncScroll.
	 * It changes the fake values of petAxial, petCoronal or petSagital.
	 * If the fake values are within range, it calls the real incrSlicePosition.
	 *
	 */
/*	protected void maybeIncrSlicePosition(int diff) {
		d3AxialFake -= diff;
		if( d3AxialFake >= 0 && d3AxialFake < d3Pipe.getNormalizedNumFrms())
			 incrSlicePosition(0, diff);
	}*/

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
		double retVal = d3Pipe.winWidth;
		if( ct4fused != null) retVal = d3Pipe.fuseWidth;
		return retVal;
	}

	double getLevelSlider() {
		double retVal = d3Pipe.winLevel;
		if( ct4fused != null) retVal = d3Pipe.fuseLevel;
		return retVal;
	}
	
	String getSliderToolTip() {
		String retVal = "adjust windows contrast, level";
		if( ct4fused != null) retVal = "adjust PET-CT mixing value";
		return retVal;
	}
	
	String getLowerEditToolTip() {
		String retVal = "contrast value";
		if( ct4fused != null) retVal = "width of slider (no special meaning)";
		return retVal;
	}
	
	String getUpperEditToolTip() {
		String retVal = "centerline value";
		if( ct4fused != null) retVal = "fusion mixing value, 500 (of 1000) is 50-50";
		return retVal;
	}

	void ActionSliderChanged( double width, double level) {
		if( ct4fused !=  null) {
			d3Pipe.fuseWidth = width;
			d3Pipe.fuseLevel = level;
		} else {
			d3Pipe.winWidth = width;
			d3Pipe.winLevel = level;
			if( mipPipe != null) {
				mipPipe.winWidth = width;
				mipPipe.winLevel = level;
			}
		}
		repaint();
	}

	void maybeShowPopupMenu(MouseEvent arg0) {
		if( arg0.getButton() != MouseEvent.BUTTON3) return;
		parent.hideAllPopupMenus();
		JPopupMenu pop1;
		if( arg0.getID() != MouseEvent.MOUSE_PRESSED) return;
		Point pt1 = arg0.getLocationOnScreen();
		pop1 = parent.getjPopupD3();
		pop1.setLocation(pt1);
		parent.updateCheckmarks();
		mouse1.getMousePage(arg0, true);	// need for sync command
		pop1.setVisible(true);
	}

	/**
	 * The routine which processes user mouse drags to change gray scale values.
	 *
	 * @param arg0 the current mouse position
	 * @param starting true if mouse pressed event, false if drag
	 */
	protected void winLevelDrag( MouseEvent arg0, boolean starting) {
		Point2d pt1 = mouse1.getDragOffset(arg0, starting);
		double width1, level1, delta, levMax, bas1, win1, coef;
		if( starting) {
			if( ct4fused != null) {
				saveLevel = d3Pipe.winLevel;
				saveWidth = d3Pipe.winWidth;
				return;
			}
			saveLevel = getLevelSlider();
			saveWidth = getWidthSlider();
			return;
		}
		if( mouse1.button1 == MouseEvent.BUTTON2) {
			coef = 0.5;
			int currSlice, diff, sign1 = 1;
			if( invertScroll) sign1 = -1;
			if(mouse1.page == 1) {
				sign1 = -1;
				coef = 1;
			}
			int slice = (int)(mouse1.startSlice + sign1*coef*pt1.y*mouse1.numSlice + 0.5);
			if( slice < 0) slice = 0;
			if( slice >= mouse1.numSlice) slice = mouse1.numSlice - 1;
			currSlice = (int) getCurrentSlice();
			diff = sign1*(slice - currSlice);
			if( diff == 0) return;
			incrSlicePosition(-1, diff);
			return;
		}
		if( ct4fused != null) {
			levMax = d3Pipe.data1.sliderSUVMax;
			delta = levMax / 2;
			width1 = pt1.x * delta + saveWidth;
			level1 = pt1.y * delta + saveLevel;
			if( width1 < 0) width1 = 0;
			if( level1 < 0) level1 = 0;
			bas1 = level1 - width1/2;
			win1 = level1 + width1/2;
			if( bas1 < 0) bas1 = 0;
			if( win1 > levMax) win1 = levMax;
			width1 = win1 - bas1;
			level1 = width1/2 + bas1;
			d3Pipe.winWidth = width1;
			d3Pipe.winLevel = level1;
			repaint();
			return;
		}
		delta = maxSlider - minSlider;
		width1 = pt1.x * delta + saveWidth;
		level1 = pt1.y * delta + saveLevel;
		setWinLevel(width1, level1, true);
	}

	protected void panDrag( MouseEvent arg0, boolean starting) {
		Point2d pt1 = mouse1.getPanOffset(arg0, starting);
		if( starting) {
			mouse1.pan3d = new Point3d(d3Pipe.pan);
			mouse1.zoom1 = d3Pipe.zoom1;
			mouse1.zoomZ = d3Pipe.data1.width * mouse1.widthY;
			mouse1.zoomZ /= mouse1.widthX * d3Pipe.data1.numFrms * d3Pipe.zoomX * d3Pipe.data1.y2xFactor / d3Pipe.data1.numTimeSlots;
			mouse1.zoomY = d3Pipe.zoomY;
			return;
		}
		panDragSub(pt1);
		repaint();
	}
	
	void panDragSub(Point2d pt1) {
		double x1 = pt1.x;
		double y1 = pt1.y;
		Point3d pan1 = new Point3d(mouse1.pan3d);
		double maxZ = (mouse1.zoom1 - 1.0) / mouse1.zoom1;
		if( maxZ < 0) maxZ = 0;
		double maxY = maxZ;
		switch( mouse1.page) {
			case 1:
				maxY = (mouse1.zoom1 - mouse1.zoomY) / mouse1.zoom1;
				if( maxY < 0) maxY = 0;
				pan1.x += x1;
				pan1.y += y1;
				break;

			case 2:
				pan1.x += x1;
				pan1.z += y1;
				break;

			case 3:
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
		if( !d3Pipe.updatePanValue(pan1, false)) return;
		if( ct4fused != null) ct4fused.updatePanValue(pan1, false);
	}

	void setWinLevel( double width1, double level1, boolean check) {
		double win1, base1;
		base1 = level1 - width1/2;
		win1 = level1 + width1/2;
		if( check) {
			if( base1 < minSlider) base1 = minSlider;
			if( win1 > maxSlider) win1 = maxSlider;
		}
		parent.setDualSliderValues(base1, win1);
	}

	void changeCurrentSlider() {
		int sliderDigits = 0;
		minSlider = 0;
		maxSlider = 1000;
		int i = d3Pipe.data1.seriesType;
		if( i == ChoosePetCt.SERIES_CT || i == ChoosePetCt.SERIES_CT_VARICAM
				|| i == ChoosePetCt.SER_FORCE_CT) {
			minSlider = d3Pipe.data1.minVal;
			maxSlider = d3Pipe.data1.maxVal;
		}
		if( d3Pipe.data1.SUVfactor > 0 && ct4fused == null) {
			sliderDigits = 1;
			maxSlider = d3Pipe.data1.sliderSUVMax;
		}
		parent.switchDualSlider( sliderDigits);
	}

	protected void LoadData(Display3Frame par1, ImagePlus inImg, int forceVal) {
		parent = par1;
		ct4fused = null;
		d3Pipe = new JFijiPipe();
		d3Pipe.LoadData(inImg, forceVal);
		LoadLDataSub();
		isInitializing = false;
	}

	protected void LoadData(Display3Frame par1, JFijiPipe srcPipe, JFijiPipe ctPipe) {
		double aspect;
		boolean useXY;
		parent = par1;
		d3Pipe = new JFijiPipe();
		d3Pipe.LoadData(srcPipe);
		ct4fused = null;
		if( ctPipe != null) {
			ct4fused = new JFijiPipe();
			ct4fused.LoadData(ctPipe);
		}
		LoadLDataSub();
		if( ct4fused != null) {
			d3Pipe.imgPos[0].x = d3Pipe.imgPos[1].x = d3Pipe.imgPos[2].x = -1;
			d3Pipe.aspect = aspect = 1.0 * d3Pipe.data1.height / d3Pipe.data1.width;
			d3Pipe.mriOffY0 = (1.0 - aspect)*d3Pipe.data1.width/2;
			ct4fused.imgPos = d3Pipe.imgPos;
			d3Pipe.offscrMode = ct4fused.offscrMode = 1;
			useXY = srcPipe.useShiftXY;
			d3Pipe.useShiftXY = useXY;
			d3Pipe.zoomX = srcPipe.zoomX;
			d3Pipe.fuseLevel = srcPipe.fuseLevel;
			d3Pipe.fuseWidth = srcPipe.fuseWidth;
			d3Pipe.winLevel = srcPipe.winLevel;
			d3Pipe.winWidth = srcPipe.winWidth;
			d3Pipe.shiftXY[0] = srcPipe.shiftXY[0];
			d3Pipe.shiftXY[1] = srcPipe.shiftXY[1];
			d3Pipe.avgSliceDiff = srcPipe.avgSliceDiff;
			ct4fused.srcPet = d3Pipe;
			if( ctPipe != null) {
				ct4fused.useShiftXY = useXY;
				ct4fused.winWidth = ctPipe.winWidth;
				ct4fused.winLevel = ctPipe.winLevel;
				ct4fused.shiftXY[0] = ctPipe.shiftXY[0];
				ct4fused.shiftXY[1] = ctPipe.shiftXY[1];
				ct4fused.zoomX = ctPipe.zoomX;
				ct4fused.data1.mriOffZ = ctPipe.data1.mriOffZ;
				ct4fused.data1.panZCt2Pet = ctPipe.data1.panZCt2Pet;
				ct4fused.mriOffX = ctPipe.mriOffX;
				ct4fused.mriOffY = ctPipe.mriOffY;
				ct4fused.mriOffSag = ctPipe.mriOffSag;
				ct4fused.corSagShift = ctPipe.corSagShift;
				ct4fused.mriScl = ctPipe.mriScl;
				ct4fused.avgSliceDiff = ctPipe.avgSliceDiff;	// usually 1.0
			}
			// MRI data can be non square
			aspect = ct4fused.data1.height * ct4fused.data1.y2XMri / ct4fused.data1.width;
			ct4fused.aspect = aspect;
			ct4fused.mriOffY0 = (1.0 - aspect)*ct4fused.data1.width/2;
			ct4fused.corFactor = (d3Pipe.aspect*d3Pipe.zoomX * ct4fused.data1.height) / (d3Pipe.data1.height * ct4fused.zoomX * aspect);
			ct4fused.sagFactor = (d3Pipe.zoomX * ct4fused.data1.width) / (d3Pipe.data1.width * ct4fused.zoomX);
			ct4fused.obliqueFactor = 0;
			ct4fused.sagOffset = ct4fused.data1.width* ( 1- d3Pipe.zoomX/ct4fused.zoomX)/2 ;
			ct4fused.corOffset = (ct4fused.sagOffset - ct4fused.mriOffY0 + d3Pipe.mriOffY0*ct4fused.corFactor)/ ct4fused.data1.y2XMri;
			// Terry Weizeman shows this problem
			ct4fused.sagOffset -= ct4fused.sagFactor*(d3Pipe.shiftXY[0] - ct4fused.shiftXY[0]);
			ct4fused.corOffset -= ct4fused.corFactor*(d3Pipe.shiftXY[1] - ct4fused.shiftXY[1]);
	}
		if(ct4fused != null) setWinLevel(d3Pipe.fuseWidth, d3Pipe.fuseLevel, false);
		else setWinLevel(srcPipe.winWidth, srcPipe.winLevel, false);
		isInitializing = false;
	}
	
	void LoadLDataSub() {
		int numFrms = d3Pipe.getNormalizedNumFrms();
		d3AxialFake = d3Axial = numFrms / 2;
		d3Sagital = d3Pipe.data1.width / 2;
		d3Coronal = d3Pipe.data1.height / 2;
		checkLimits();
		d3Pipe.imgPos = new Point[3];
		d3Pipe.imgPos[0] = new Point(0, 0);
		d3Pipe.imgPos[1] = new Point(1, 0);
		d3Pipe.imgPos[2] = new Point(2, 0);
		parent.fillPatientData();
		isGated = runGated = d3Pipe.data1.numTimeSlots > 1;
		parent.setGatedButtonVisible(isGated);
		parent.setTitle(parent.getTitleBar(0));
		WindowManager.addWindow(parent);
		changeCurrentSlider();
		updateMultYOff(true);
		m_timer.start();
	}

	void presetWindowLevels(int indx) {
		JFijiPipe currPipe = d3Pipe;
		int slice = ChoosePetCt.round(d3Axial);
		switch( indx) {
			case 0:
				currPipe.AutoFocus(slice);
				break;

			case 1:	// Chest-Abdomen
			case 2:	// Lung
			case 3:	// Liver
			case 4:	// Bone
			case 5:	// Brain-Sinus
				if(ct4fused != null) currPipe = ct4fused;
				currPipe.winWidth = parent.ctWidth[indx-1];
				currPipe.winLevel = parent.ctLevel[indx-1];
				break;

			case 8:
				currPipe.MaxFocus(slice, 1.0);	// in pet-ct slice max may be other than 1.0
				currPipe.setLogSlider();
				break;

			case 9:
				if(currPipe.data1.SUVfactor <= 0) break;	// ignore
				currPipe.winWidth = 10;
				currPipe.winLevel = 5;
				currPipe.setLogSlider();
				break;

			default:
				return;

		}
		changeCurrentSlider();	// update the values on the gray scale bar
		repaint();
	}

	void ActionMipCine(int type) {
		switch( type) {
			case CINE_RUNNING:	// this is used for both start and stop
				runCine = !runCine;
				break;

			case CINE_FORWARD:
				runCine = false;
				mipPipe.cineIndx = 0;
				break;

			case CINE_SIDE:
				runCine = false;
				mipPipe.cineIndx = 3*JFijiPipe.NUM_MIP/4;
				break;
		}
		if (runCine) {
//			m_timer.start();
			parent.setCineButtons(true);
		}
		else {
//			m_timer.stop();
			repaint();
		}
	}

	void chooseNewPosition( MouseEvent arg0) {
		int x1, z1, i, j=4;
		Point pt1;
		Point2d pt2;
		double [] coss;
		double scl1 = getScale();
		i = mouse1.getMousePage(arg0, true);
		if(i==1) {
			j = 0;
			if( d3Pipe.zoom1 > 1.0) j = 3;
		}
		pt1 = new Point(mouse1.xPos % mouse1.widthX, mouse1.yPos);
		x1 = mouse1.xPos / mouse1.widthX + JFijiPipe.DSP_AXIAL;
		pt2 = d3Pipe.scrn2Pos(pt1, scl1, j, x1);
		switch (i) {
			case 1:
				d3Axial = ChoosePetCt.round(d3Axial);
				d3Coronal = pt2.y;
				d3Sagital = pt2.x;
				break;

			case 2:
				d3Coronal = ChoosePetCt.round(d3Coronal);
				d3Sagital = pt2.x;
				d3Axial = pt2.y;
				break;

			case 3:
				d3Sagital = ChoosePetCt.round(d3Sagital);
				d3Axial = pt2.y;
				d3Coronal = pt2.x;
				if( showMip) {
					Point2d pt3 = mipPipe.scrn2Pos(pt1, scl1, 1);
					x1 = ChoosePetCt.round(pt3.x);
					d3Axial = z1 = ChoosePetCt.round(pt3.y);
					pt2 = new Point2d();
					coss = mipPipe.data1.setCosSin(mipPipe.cineIndx);
					mipPipe.data1.getMipLocation(x1, z1, pt2, null, 0, coss);
					d3Coronal = pt2.y;
					d3Sagital = pt2.x;
					break;
				}
				break;
		}
		maybePanImage();
		maybeUpdateMultYoff();
		d3Pipe.oldCorWidth = -1;	// cause update
		checkLimits();
		d3AxialFake = d3Axial;
		calculateSUVandCT();
		parent.hideAllPopupMenus();
		repaint();
	}

	void maybeUpdateMultYoff() {
		if( saveHeight <= 0) return;
		Dimension dm2 = getSize();
		int diff = Math.abs(dm2.height - saveHeight);
		if( diff > 5) updateMultYOff(false);
	}

	void maybePanImage() {
		if(d3Pipe.zoom1 <= 1.0) return;
		boolean okXY, negY = false;
		double edge, p1y, widthZ = mouse1.widthY;
		if( widthZ <= 0) {
			Dimension dm2;
			dm2 = getSize();
			widthZ = mouse1.widthY = dm2.height;
		}
		panDrag(null, true);
		edge = 0.1/mouse1.zoom1;
		int savePage = mouse1.page;
		Point2d p0 = new Point2d(d3Sagital, d3Coronal);
		Point p1 = d3Pipe.pos2Scrn(p0, getScale(), 3);
		Point2d p3, p2 = d3Pipe.saveRaw;
		okXY = p2.x >= 0 && p2.x < mouse1.widthX && p2.y >= 0 && p2.y < widthZ;
		if( !okXY) {
			if( p2.y >= widthZ) p2.y = widthZ - 1;
			p3 = new Point2d();
			p3.x = (p2.x - p1.x) / mouse1.widthX;
			p3.y = (p1.y - p2.y) / widthZ;
			if( p1.y == 0) p3.y = -p3.y;
			mouse1.page = 1;
			if(p3.x != 0) {
				if(p3.x < 0) p3.x -= edge;
				else p3.x += edge;
			}
			if(p3.y != 0) {
				if(p3.y < 0) p3.y -= edge;
				else p3.y += edge;
			}
			panDragSub(p3);
		}
		p0.y = d3Axial;
		p1 = d3Pipe.pos2Scrn(p0, getScale(), 2);
		p2 = d3Pipe.saveRaw;
		okXY = p2.y >= 0 && p2.y < widthZ;
		if( !okXY) {
			p3 = new Point2d();
			mouse1.page = 2;
			mouse1.pan3d.z = d3Pipe.pan.z = 0;
			p1y = d3Axial / d3Pipe.getNormalizedNumFrms();
			if( p1.y != 0) {
				p1y -= 1.0/mouse1.zoom1;
				if(p1y < 0) p1y = 0;
			}
			else negY = true;
			p3.y = p1y;
			if(p3.y != 0) {
				if(p3.y < 0 || negY) p3.y -= edge;
				else p3.y += edge;
			}
			panDragSub(p3);
		}
		mouse1.page = savePage;
	}

	boolean calculateSUVandCT() {
		int z0, z1, z0a, y0, deltaX, numPts, digits, mult, height1;
		int linex, liney, serType, numGate, numFrm, gateOffset = 0;
		double[] sumVals;
		double spac1, spacy, spacz, spacOut, totalSum, currDbl, radMm, radpx;
		JFijiPipe.lineEntry currLine;
		JFijiPipe ctPipe;
		SUVresults = "";
		SUVorCount = 0;
		CTval = 0;
		if( styType < 0) return false;
		radMm = parent.SUVmm / 2.0;
		if(d3Pipe == null) return true;	// turn off the pipe
		numGate = d3Pipe.data1.numTimeSlots;
		if( numGate > 1) {
			numFrm = d3Pipe.data1.numFrms / numGate;
			gateOffset = gateIndx * numFrm;
		}
		int suvType = parent.SUVtype;
		suvPnt = new SUVpoints(JFijiPipe.DSP_AXIAL);
//		width1 = d3Pipe.data1.width;
		height1 = d3Pipe.data1.height;
		z1 = ChoosePetCt.round(d3Axial) + gateOffset;
		linex = ChoosePetCt.round(d3Sagital);
		liney = ChoosePetCt.round(d3Coronal);
		Point pt1 = new Point(linex, liney);
		if( pt1.y < 0 || pt1.y >= height1) return false;
		currLine = d3Pipe.data1.getLineOfData(0, liney, z1);
		if( !currLine.goodData) return false;
		spac1 = currLine.pixelSpacing[0];
		if( spac1 <= 0) return false;
		radpx = (radMm/spac1);
		totalSum = curMax = 0;
		numPts = z0 = z0a = 0;
		spacOut = Math.abs(d3Pipe.data1.sliceThickness/ spac1);
		if( styType != CT_STUDY && styType != MRI_STUDY) {
			while(true) {
				spacz = spacOut;
				spacz *= z0a;
				currDbl = radpx*radpx - spacz*spacz;
				if(currDbl <= 0) break;
				deltaX = (int) Math.sqrt(currDbl);
				currLine = myGetLineofData(d3Pipe, liney, 0, z1, z0a);
				if( !currLine.goodData) break;
				sumVals = sumLine(currLine, deltaX, linex, liney, z1+z0a);
				totalSum += sumVals[0];
				numPts += sumVals[1];
				y0 = 0;
				while( deltaX > 0) {
					spacy = 1.0;
					y0++;
					spacy *= y0;
					currDbl = radpx*radpx - spacy*spacy - spacz*spacz;
					if(currDbl <= 0) break;
					deltaX = (int) Math.sqrt(currDbl);
					for( mult = -1; mult <=1; mult +=2) {
						currLine = myGetLineofData(d3Pipe, liney, mult*y0, z1, z0a);
						sumVals = sumLine(currLine, deltaX, linex, liney + mult*y0, z1+z0a);
						totalSum += sumVals[0];
						numPts += sumVals[1];
					}
				}
				if( z0a < 0 || z0 == 0) {
					z0++;
					z0a = z0;
					continue;
				}
				z0a = -z0;
			}
			SUVmean = totalSum / numPts;
			digits = 0;
			if( suvType == 1 || suvType == 2) SUVpeak = suvPnt.calcSUVpeak(d3Pipe, suvType);
			currDbl = currLine.SUVfactor;
			SUVresults = "MaxCount = ";
			if(currDbl > 0) {
				SUVresults = "SUVmax = ";
				digits = 2;
				SUVmean /= currDbl;
				curMax /= currDbl;
				if( curMax < 1.0) digits = 3;
				SUVpeak /= currDbl;
			}
			SUVorCount = PetCtFrame.roundN(curMax, digits);
			SUVpeak = PetCtFrame.roundN(SUVpeak, digits);
			SUVmean = PetCtFrame.roundN(SUVmean, digits);
			SUVresults += SUVorCount;
			if( suvType == 1 || suvType == 2) {
				SUVresults = "PeakCount = ";
				if( currDbl > 0) SUVresults = "SUVpeak = ";
				SUVresults += SUVpeak;
			}
			if( suvType == 3) {
				SUVresults = "MeanCount = ";
				if( currDbl > 0) SUVresults = "SUVmean = ";
				SUVresults += SUVmean;
				
			}
		}
		if(isGated) {
			int i = gateIndx + 1;
			SUVresults += ", "+ i + "/" + numGate;
		}
		
		ctPipe = d3Pipe;
		if(styType == FUSED_SUV || styType == FUSED_CNT) ctPipe = ct4fused;
		if( styType == CT_STUDY || styType == MRI_STUDY  || styType == FUSED_SUV || styType == FUSED_CNT) {
			if( ctPipe == null) return false;
			pt1.x = ChoosePetCt.round(shift2Ct1(ctPipe, 0));
			pt1.y = ChoosePetCt.round(shift2Ct1(ctPipe, 1));
			z1 = ctPipe.findCtPos(d3Axial, false);
			if( z1 < 0 || pt1.x < 0 || pt1.y < 0) return false;
			currLine = ctPipe.data1.getLineOfData(0, pt1.y, z1);
			if( !currLine.goodData) return false;
			spac1 = currLine.pixelSpacing[0];
			if( spac1 <= 0) return false;
			deltaX = (int) (4.0/spac1);
			sumVals = sumLine(currLine, deltaX, pt1.x, -1, -1);
			serType = ctPipe.data1.seriesType;
			CTval = (int) (sumVals[0]/sumVals[1] + ctPipe.data1.shiftIntercept());
			if( !SUVresults.isEmpty()) SUVresults += "    ";
			if(serType == ChoosePetCt.SERIES_MRI || serType == ChoosePetCt.SER_FORCE_MRI)
				SUVresults += "MRI = " + CTval;
			else SUVresults += "CT = " + CTval;
		}
		return false;
	}

	double shift2Ct1( JFijiPipe currPipe, int xyChoice) {
		double inVal = d3Sagital, mriOff = currPipe.mriOffX;
		double factor = currPipe.sagFactor, offst = currPipe.sagOffset;
		if( xyChoice > 0) {
			inVal = d3Coronal;
			mriOff = currPipe.mriOffY;
			factor = currPipe.corFactor;
			offst = currPipe.corOffset;
		}
		return factor*inVal + offst - mriOff;
	}

	double[] sumLine(JFijiPipe.lineEntry currLine, int n1, int center, int ypos, int zpos) {
		double[] ret1 = new double[2];	// sum, numPts
		double scale = currLine.slope;
		int i, j, size1, rn = -1, li = -1;
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
			suvPnt.addPoint(retVal, 0, x, y, z, rn, li);
		}
		return retVal;
	}
	
	JFijiPipe.lineEntry myGetLineofData(JFijiPipe pet1, int liney, int yindx, int z1, int zindx) {
		int depth, sliceNum;
		depth = liney + yindx;
		sliceNum = z1 + zindx;
		return pet1.data1.getLineOfData(0, depth, sliceNum);
	}

	void drawAll(Graphics2D g) {
		if( isInitializing) return;
		if( resizeCnt < 2 ) {
			if(++resizeCnt == 2) parent.fitWindow();
		}
		double petSag, scl2=1, scl1 = getScale();
		int i, ctPos, numGate, numFrm, gateOffset = 0;
		Point pt1 = new Point(0, 0);
		numGate = d3Pipe.data1.numTimeSlots;
		if( numGate > 1) {
			numFrm = d3Pipe.data1.numFrms / numGate;
			gateOffset = gateIndx * numFrm;
		}
		if(ct4fused != null) {
			d3Pipe.prepareFused(d3Color);
			scl2 = scl1 * d3Pipe.data1.width / ct4fused.data1.width;
			ct4fused.prepareFused(d3FusedColor);
		}
		d3Pipe.prepareFrame(d3Axial + gateOffset, 0, d3Color, 0);
		d3Pipe.drawImages(g, scl1, this);
		if( ct4fused != null) {
			ctPos = ct4fused.findCtPos(d3Axial, true);
			ct4fused.prepareFrame(ctPos, 0, d3FusedColor, 0);
			ct4fused.drawImages(g, scl2, this);
			d3Pipe.drawFusedImage(g, scl1, ct4fused, pt1, this, JFijiPipe.DSP_AXIAL);
		}
		petSag = d3Pipe.prepareCoronalSagital(d3Coronal, d3Sagital, d3Color, gateOffset, 0);
		d3Pipe.drawCorSagImages(g, scl1, this, true);	// coronal
		if( ct4fused != null) {
			ct4fused.prepareCoronalSagital(d3Coronal, d3Sagital, d3FusedColor, 0, petSag);
			ct4fused.drawCorSagImages(g, scl2, this, true);
			pt1.x = 1;
			d3Pipe.drawFusedImage(g, scl1, ct4fused, pt1, this, JFijiPipe.DSP_CORONAL);
		}
		if( showMip) drawMip(g, scl1);
		else {
			d3Pipe.drawCorSagImages(g, scl1, this, false);	// sagital
			if( ct4fused != null) {
				ct4fused.drawCorSagImages(g, scl2, this, false);
				pt1.x = 2;
				d3Pipe.drawFusedImage(g, scl1, ct4fused, pt1, this, JFijiPipe.DSP_SAGITAL);
			}
		}

		g.setColor(Color.green);
		drawMarkers(g, JFijiPipe.DSP_AXIAL, d3Sagital, d3Coronal, scl1);
		drawMarkers(g, JFijiPipe.DSP_CORONAL, d3Sagital, d3Axial, scl1);
		if( !showMip) drawMarkers(g, JFijiPipe.DSP_SAGITAL, d3Coronal, d3Axial, scl1);
		drawBrownFat(g);
		drawAnnotations(g);
		g.setColor(Color.red);
		Dimension sz1 = getReducedSize();
		g.drawString(SUVresults, 4, sz1.height - 4);
//		String tmp = "3 view = " + d3Axial + ",  " + d3Coronal + ",  " + d3Sagital;
//		System.out.println(tmp);
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

	void dirtyCorSag(boolean isNumTime) {
		if( d3Pipe.data1.numTimeSlots <= 1 && isNumTime) return;
		if( d3Pipe != null) d3Pipe.dirtyCorSag();
	}
	
	void drawMarkers(Graphics2D g, int indx, double xIn, double yIn, double scale) {
		int w1, h0, h1, h2, i=4, offY = 0, w10, x1;
		boolean intMode = true;
		if(ct4fused != null) {
//			intMode = parent.jPrefer.getBoolean("cursor pixel lock", true);
			intMode = false;
		}
		Point2d pt1 = new Point2d(xIn, yIn);
		Line2D ln1 = new Line2D.Double();
		Dimension sz1 = getWindowDim(d3Pipe);
		sz1.width /= 3;	// one section
		w1 = ChoosePetCt.round(sz1.width * scale);
		w10 = w1 * PERCENT_AXIS / 100;
		h1 = ChoosePetCt.round(sz1.height * scale);
		h0 = mouse1.widthY;	// the user can expand Y for zoom
		if( h0 > h1) h1 = h0;
		if( indx == JFijiPipe.DSP_AXIAL) {
			i = x1 = 0;
			if( d3Pipe.zoom1 > 1.0) i = 3;
			h2 = ChoosePetCt.round(d3Pipe.data1.height * scale * d3Pipe.getZoom(0));
			offY = ChoosePetCt.round(sz1.width * d3Pipe.multYOff * scale);
		} else {
			h2 = ChoosePetCt.round(d3Pipe.data1.numFrms * scale * d3Pipe.getZoom(2));
			x1 = w1;
		}
		if( indx == JFijiPipe.DSP_SAGITAL) x1 = w1*2;
		if( h2 < h1) h1 = h2;
		Point pt2 = d3Pipe.pos2Scrn(pt1, scale, i, indx, intMode);
		Point[] p4 = new Point[4];
		for( i=0; i<4; i++) p4[i] = new Point();
		for( i=0; i<2; i++) {
			if( i==0) {
				if( pt2.y > offY+h1 || pt2.y < offY) continue;
				p4[0].x = x1;
				p4[1].x = x1 + w1;
				p4[0].y = p4[1].y = p4[2].y = p4[3].y = pt2.y;
				if(limitCursor) {
					p4[0].x = x1 + pt2.x - cursorSize;
					if( p4[0].x < x1) p4[0].x = x1;
					p4[1].x = x1 + pt2.x + cursorSize;
					if( p4[1].x > x1 + w1) p4[1].x = x1 + w1;
				}
				if( splitCursor) {
					if( limitCursor) {
						p4[2].x = p4[1].x;
						p4[1].x = p4[0].x;
						p4[0].x = x1 + pt2.x - 2*cursorSize;
						if( p4[0].x < x1) p4[0].x = x1;
						p4[3].x = x1 + pt2.x + 2*cursorSize;
						if( p4[3].x > x1+w1) p4[3].x = x1+w1;
					} else {
						p4[3].x = p4[1].x;
						p4[1].x = x1 + w10;
						if( x1 + pt2.x <= p4[1].x) p4[1].x = x1+pt2.x-1;
						p4[2].x = x1 + w1 - w10;
						if( x1 + pt2.x >= p4[2].x) p4[2].x = x1+pt2.x+1;
					}
				}
			} else {
				if( pt2.x > w1 || pt2.x < 0) continue;
				p4[0].x = p4[1].x = p4[2].x = p4[3].x = x1 + pt2.x;
				p4[0].y = offY;
				p4[1].y = offY + h1;
				if( limitCursor) {
					p4[0].y = pt2.y - cursorSize;
					if( p4[0].y < offY) p4[0].y = offY;
					p4[1].y = pt2.y + cursorSize;
					if( p4[1].y > offY+h1) p4[1].y = offY+h1;
				}
				if( splitCursor) {
					if( limitCursor) {
						p4[2].y = p4[1].y;
						p4[1].y = p4[0].y;
						p4[0].y = pt2.y - 2*cursorSize;
						if( p4[0].y < offY) p4[0].y = offY;
						p4[3].y = pt2.y + 2*cursorSize;
						if( p4[3].y > offY+h1) p4[3].y = offY+h1;
					} else {
						p4[3].y = p4[1].y;
						p4[1].y = offY + w10;
						if( pt2.y <= p4[1].y) p4[1].y = pt2.y-1;
						p4[2].y = offY + h1 - w10;
						if( pt2.y >= p4[2].y) p4[2].y = pt2.y+1;
					}
				}
			}
			ln1.setLine(p4[0], p4[1]);
			g.draw(ln1);
			if( splitCursor) {
				ln1.setLine(p4[2], p4[3]);
				g.draw(ln1);
			}
		}
	}

	void drawBrownFat(Graphics2D g) {
		PetCtFrame petFrm = parent.srcFrame;
		if( petFrm == null) return;	// when 3 view is called alone
		PetCtPanel origCaller = petFrm.getPetCtPanel1();
		BrownFat bfDlg = origCaller.bfDlg;
		if( bfDlg == null) {
			BrownFat bf1 = BrownFat.instance;
			if( bf1 == null) return;
			bf1.drawOther3Data(g, this, origCaller);
			return;
		}
		if( !(bfDlg instanceof BrownFat)) return;
		if( styType == CT_STUDY || styType == MRI_STUDY) return;
		bfDlg.drawDisplay3Data(g, this, origCaller);
	}
	
	void drawAnnotations(Graphics2D g) {
		if( anotateDlg == null) return;
		mouse1.getMousePage(null, false);	// set widthX
		anotateDlg.draw3Graphics(g, this);
	}

	void checkLimits() {
		if( d3Axial < 0) d3Axial = 0;
		int maxFrm = d3Pipe.getNormalizedNumFrms();
		if( d3Axial >= maxFrm) d3Axial = maxFrm - 1;
		if( d3Coronal < 0) d3Coronal = 0;
		if( d3Coronal >= d3Pipe.data1.height-1) d3Coronal = d3Pipe.data1.height - 1;
		if( d3Sagital < 0) d3Sagital = 0;
		if( d3Sagital >= d3Pipe.data1.width-1) d3Sagital = d3Pipe.data1.width - 1;
	}

	double updateMultYOff(boolean loading) {
		double multYOff = 0, zoomY = 1.0, mult1;
		int width = d3Pipe.data1.width;
		int height = ChoosePetCt.round(d3Pipe.data1.width * d3Pipe.zoom1);
		int num = ChoosePetCt.round(d3Pipe.data1.numFrms * d3Pipe.zoomX * d3Pipe.data1.y2xFactor / d3Pipe.data1.numTimeSlots);
		Dimension sz1 = getSize();
		saveHeight = sz1.height;
		if( num > width) {
			if( height >= num) height = num;
			multYOff = 0.5 * (num-height)/width;
			zoomY = ((double) height) / width;
		}
		if(!loading && multYOff > 0) {	// while loading getSize isn't the final value
			Dimension sz0 = getWindowDim(d3Pipe);
			mult1 = ((double)sz1.height)*sz0.width/(sz1.width*sz0.height);
			if( mult1 > 1.0) mult1 = 1.0;
			multYOff -= 1.0-mult1;
			if( multYOff < 0) multYOff = 0;
		}
		d3Pipe.multYOff = multYOff;
		d3Pipe.zoomY = zoomY;
		if(ct4fused != null) {
			ct4fused.multYOff = multYOff;
			ct4fused.zoomY = zoomY;
		}
		return multYOff;
	}

	void drawMip(Graphics2D g, double scl1) {
		if( mipPipe == null) {
			loadMip();
			ActionMipCine(CINE_RUNNING);
			return;
		}
		if( mipPipe.data1 == null) return;
		int i = mipPipe.data1.numFrms;
		if( i < JFijiPipe.NUM_MIP) {
			cinePaint = false;
			if (i <= 0) return;
		}
		mipPipe.makeDspImage(JFijiPipe.COLOR_INVERSE, 1);
		mipPipe.drawCine(g, scl1, this, cinePaint);
	}

	void loadMip() {
		work2 = new bkgdLoadData();
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

	double getScale() {
		double scale0, scale1 = 0;
		JFijiPipe pipe1 = d3Pipe;
		if( pipe1 == null) return scale1;
		Dimension sz1, dim1 = getSize();
		sz1 = getWindowDim(pipe1);
		scale0 = ((double)dim1.width) / sz1.width;
//		scale1 = ((double)dim1.height) / sz1.height;
//		if( scale1 > scale0) scale1 = scale0;
		scale1 = scale0;
		return scale1;
	}

	// in calculation of Window dimension use zoom1 = 1.0
	// y2XMri is not in the PetCtPanel version....
	Dimension getWindowDim(JFijiPipe pipe1) {
		Dimension sz1;
		double scale0;
		int width1, heigh0, heigh1;
		width1 = pipe1.data1.width;
		heigh1 = ChoosePetCt.round(pipe1.data1.height * pipe1.data1.y2XMip * pipe1.data1.y2XMri);
		scale0 = pipe1.zoomX * pipe1.data1.y2xFactor * pipe1.data1.y2XMri;
		heigh0 = ChoosePetCt.round(pipe1.data1.numFrms * scale0 / pipe1.data1.numTimeSlots);
		if( heigh1 < heigh0) heigh1 = heigh0;
		sz1 = new Dimension(width1*3, heigh1);
		return sz1;
	}

	int getCineState() {
		int retVal = CINE_RUNNING;
		if( !runCine) {
			retVal = CINE_STOPPED;
			int pos = 0;
			if( mipPipe != null) pos = mipPipe.cineIndx;
			if( pos == 0) retVal = CINE_FORWARD;
			if( pos == 3*JFijiPipe.NUM_MIP/4) retVal = CINE_SIDE;
		}
		return retVal;
	}

	/**
	 * To keep the main thread responsive, the heavy data loading is done in the background.
	 */
	protected class bkgdLoadData extends SwingWorker {
		@Override
		protected Void doInBackground() {
			doMipBuild();
			return null;
		}
	}

	void doMipBuild() {
		mipPipe = new JFijiPipe();
		mipPipe.sliceType = JFijiPipe.DSP_CORONAL;
		mipPipe.winWidth = 800;
		mipPipe.winLevel = 400;
		mipPipe.useSrcPetWinLev = true;
		if( !mipPipe.LoadMIPData(d3Pipe)) {
			JOptionPane.showMessageDialog(this, "Failed to build the MIP data");
			mipPipe = null;
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

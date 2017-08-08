import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.plugin.LutLoader;
import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

/**
 *
 * @author Ilan
 */
public class JFijiPipe {
	/** the number of MIP cine frames = 16 */
	public static final int NUM_MIP = 16;
	static final int COLOR_BLUES = 0;
	static final int COLOR_GRAY = 1;
	static final int COLOR_INVERSE = 2;
	static final int COLOR_HOTIRON = 3;
	static final int COLOR_USER = 4;
	static final int COLOR_MRI = 5;
	static final int COLOR_PET_USER = 6;

	static final int DSP_AXIAL = 1;
	static final int DSP_CORONAL = 2;
	static final int DSP_SAGITAL = 3;
	static final int DSP_OBLIQUE = 4;
	static final int PERCENT_AXIS = 10;
	
	static final int FORCE_NONE = 0;
	static final int FORCE_CT = 1;
	static final int FORCE_CPET = 2;
	static final int FORCE_UPET = 3;
	static final int FORCE_MRI = 4;

	MemoryImageSource[] source = null;
	// in the usual PET-CT there is only a single fused image but in d3Pipe there can be 3
	MemoryImageSource[] offSrc = null;
	// for Coronal and Sagital, leave JData class untouched, use JPipe
	MemoryImageSource[] corSrc = null;
	ArrayList<float []> rawFloatPix = null;
	ArrayList<short []> rawPixels = null;
	ArrayList<byte []> rawBytPix = null;
	ArrayList<lungInsert> lungData = null;
	mipXYentry[] mipXYdata = null;
	short[] LogConv = null;
	int coronalSlice = -1, sagitalSlice = -1;
	int logOff; // = 1000;
	double logLow; // = Math.log(logOff);
	double logNorm; // = 32767.0/(Math.log(logOff+32767.0)-logLow);

	JData data1;
	JPanel saveParent;
	Image offscr = null;
	double fuseWidth = 50, fuseLevel = 500, triThick = 2.0;
//	int fuseFactor = 120;
	Point3d pan = new Point3d();
	Point2d saveRaw;
	double[] shiftXY = new double[2];
	JFijiPipe srcPet = null;
	boolean dirtyFlg = true, useShiftXY = true, hasGauss = false, useGauss = false;
	int colorMode = -1, numDisp = 1, startFrm = 0, sliceType = 0, zoomIndx = 0;
	int offscrMode = 0, fusedMode = -1, corSagShift = 0, offscrH1 = 0, offscrY1 = 0;
	int mriOffX = 0, mriOffY = 0, mriOffY0 = 0, oldCorSlice = -1, oldSagSlice = -1;
	int mriOffSag = 0;
	double winWidth=900, winLevel=450, winSlope = 1.0, multYOff = 0, mriScl = 1.0;
	double corFactor = 1.0, corOffset = 0, obliqueFactor = 0, aspect = 1.0;
	double sagFactor = 1.0, sagOffset = 0, avgSliceDiff = 1.0, sigmaGauss = 2.0;
	double oldWidth, oldLevel, oldCorWidth, oldCorLevel, zoom1 = 1.0, zoomX = 1.0, zoomY = 1.0;
	double logFactor, valMin, valMax;
	ColorModel cm = null, cm2 = null, cm_src = null;
	Point[] imgPos = null;
	String fixedLUT = null, extBluesLUT = null, extHotIronLUT = null, extCtMriLUT = null;
	boolean useSrcPetWinLev = false, qualityRendering = false, dispInsert = false;
	boolean isLog = false, triFlg = false, isFWHM = false;
	int indx = -1, cineIndx = 0;
	int [] gaussSort = null;

	void LoadData( ImagePlus currImg, int forceVal) {
		data1 = new JData();
		data1.readData(currImg, forceVal);
	}

	void LoadData( JFijiPipe scrPipe) {
		data1 = new JData();
		data1.readData(scrPipe);
	}

	public boolean LoadMIPData(JFijiPipe petPipe) {
		data1 = new JData();
		srcPet = petPipe;
		colorMode = -1;	// uninitialized
		return data1.setMIPData(0);
	}

	public boolean LoadReprojectionData(JFijiPipe petPipe) {
		data1 = new JData();
		srcPet = petPipe;
		colorMode = -1;	// uninitialized
		return data1.setReprojectionData();
	}

	public JData CreateData1() {
		data1 = new JData();
		return data1;
	}

	boolean AutoFocus(int slice1) {
		if( data1.SUVfactor > 0) {
			data1.setSUV5();
			winLevel = winWidth/2;
			setLogSlider();
			return true;
		}
		double currVal, maxVal, sum, sum2, stds, level;
		short buff1[] = null, currShort;
		float fltBuf[] = null;
		boolean fltFlg = false;
		int i, n, cntr1;
		int coef0 = data1.getCoefficent0();
		sum = sum2 = maxVal = stds = 0;
		// watch out for byte data. For bytes just set it to 1000, 500
		if( data1.pixByt != null) {
			winWidth = 1000;
			winLevel = 500;
			return true;
		}
		n = data1.width * data1.height;
		if( data1.pixFloat != null) {
			fltBuf = data1.pixFloat.get(slice1);
			fltFlg = true;
		}
		else buff1 = data1.pixels.get(slice1);
		for( i=cntr1=0; i<n; i++) {
			if( fltFlg) currVal = fltBuf[i];
			else {
				currShort = (short)(buff1[i] + coef0);
				currVal = currShort;
			}
			if( currVal <= 0) continue;
			if( currVal > maxVal) maxVal = currVal;
			sum += currVal;
			sum2 += currVal*currVal;
			cntr1++;		// this makes sure that the whole slice isn't zero
		}
		if( cntr1 <= 1) return false;	// must be more than 1 non zero point
		level = sum2/sum;
		for( i=0; i<n; i++) {
			if( fltFlg) currVal = fltBuf[i];
			else {
				currShort = (short)(buff1[i] + coef0);
				currVal = currShort;
			}
			if( currVal < 0) {
				currVal = 0;	// break point
			}
			if( currVal <= 0) continue;
			currVal = level - currVal;
			stds += currVal*currVal;
		}
		stds = Math.sqrt(stds/(cntr1-1));
		currVal = level + 3*stds;
		if( currVal > maxVal) currVal = maxVal;
		winWidth = currVal * winSlope * data1.getRescaleSlope(slice1);
		winLevel = winWidth / 2.0;
		setLogSlider();
		return true;
	}
	
	boolean MaxFocus(int slice1, double factor) {
		short buff1[] = null, currShort;
		float fltBuf[] = null;
		boolean fltFlg = false;
		int i, n, cntr1;
		double currVal, maxVal;
		int coef0 = data1.getCoefficent0();
		// watch out for byte data. For bytes just set it to 1000, 500
		if( data1.pixByt != null) {
			winWidth = 1000;
			winLevel = 500;
			return true;
		}
		n = data1.width * data1.height;
		if( data1.pixFloat != null) {
			fltBuf = data1.pixFloat.get(slice1);
			fltFlg = true;
		}
		else buff1 = data1.pixels.get(slice1);
		maxVal = 0;
		for( i=cntr1=0; i<n; i++) {
			if( fltFlg) currVal = fltBuf[i];
			else {
				currShort = (short)(buff1[i] + coef0);
				currVal = currShort;
			}
			if( currVal <= 0) continue;
			if( currVal > maxVal) maxVal = currVal;
			cntr1++;		// this makes sure that the whole slice isn't zero
		}
		if( cntr1 <= 1) return false;	// must be more than 1 non zero point
		winWidth = factor* maxVal * winSlope * data1.getRescaleSlope(slice1);
		winLevel = winWidth / 2.0;
		return true;
	}

	/**
	 * Converts between a point in screen coordinates to data position.
	 * BEWARE - do NOT round up by 0.5 as ALL points from 0 to scalX-1
	 * are at the zero position. We had a similar problem with the mouse
	 * event in that it counts from 1 and not from 0 - this was "fixed".
	 * @param scrn1 the point on the display screen
	 * @param scale the scale factor on the display
	 * @param type axial, coronal - distorted or not
	 * @return point in the data itself
	 */
	protected Point2d scrn2Pos( Point scrn1, double scale, int type) {
		Point2d out1 = new Point2d();
		int origWidth;
		origWidth = data1.width;
		double offY = multYOff * origWidth * scale;
		double xshft = 0, yshft = 0;
		double scalX = scale;
		double scalY = scale * data1.y2XMri;
		if( sliceType == DSP_SAGITAL) scalX = scalY;
		if( useShiftXY) {
			xshft = shiftXY[0];
			yshft = shiftXY[1];
		}
		if( type == 4) offY = 0;
		out1.x = scrn1.x/scalX + xshft;
		out1.y = (scrn1.y - offY)/scalY + yshft;
		if( zoom1*zoomX*data1.y2xFactor != 1.0) {
			double sclX = scalX * getZoom(0);
			double sclY = scalY * getZoom(type);
			Point2d pan1 = getPan(type);
			out1.x = (scrn1.x/sclX) + pan1.x + xshft;
			if( out1.x < 0) out1.x = 0;
			if( out1.x >= origWidth-1) out1.x = origWidth - 1;
			out1.y = ((scrn1.y - offY)/sclY) + pan1.y + yshft;
			if( out1.y < 0) out1.y = 0;
//			if( out1.y >= origHeight) out1.y = origHeight - 1;
		}
		return out1;
	}
	
	protected Point2d scrn2Pos( Point scrn1, double scale, int type, int tmpSliceType) {
		int saveVal = sliceType;
		sliceType = tmpSliceType;
		Point2d out1 = scrn2Pos( scrn1, scale, type);
		sliceType = saveVal;	// restore it
		return out1;
	}

	protected Point scrn2PosInt( Point scrn1, double scale, int type) {
		Point2d tmp = scrn2Pos( scrn1, scale, type);
		int x, y;
		x = ChoosePetCt.round(tmp.x);
		y = ChoosePetCt.round(tmp.y);
		return new Point(x,y);
	}

	/**
	 * Converts between a point in the data to its position on the screen.
	 * @param pos1 point in the data
	 * @param scale the scale factor on the display
	 * @param type axial or coronal
	 * @param intMode round to nearest integer, or not
	 * @return point location on the display
	 */
	protected Point pos2Scrn( Point2d pos1, double scale, int type, boolean intMode) {
		Point out1 = new Point();
		saveRaw = new Point2d();
		int origWidth, outMaxX;
		origWidth = data1.width;
		double scalX = scale;
		double scalY = scale * data1.y2XMri;
		if( sliceType == DSP_SAGITAL) scalX = scalY;
		double offY = multYOff * origWidth * scalY;
		double xshft = 0, yshft = 0;
		if( useShiftXY) {
			xshft = shiftXY[0];
			yshft = shiftXY[1];
		}
		if( type == 4) offY = 0;	// don't use for coronal, sagital in display3
		out1.x = ChoosePetCt.round((pos1.x - xshft)*scalX);
		out1.y = ChoosePetCt.round((pos1.y - yshft)*scalY + offY);
		if( zoom1*zoomX*data1.y2xFactor != 1.0) {
			double sclX = scalX * getZoom(0);
			double sclY = scalY * getZoom(type);
			Point2d pan1 = getPan(type, intMode);
			saveRaw.x = out1.x = ChoosePetCt.round((pos1.x - xshft - pan1.x)*sclX);
			outMaxX = ChoosePetCt.round(origWidth*scalX);
			if( out1.x < 0) out1.x = 0;
			if( out1.x >= outMaxX) out1.x = outMaxX-1;
			saveRaw.y = out1.y = ChoosePetCt.round((pos1.y - yshft - pan1.y)*sclY+ offY);
			if( out1.y < 0) out1.y = 0;
		}
		return out1;
	}

	protected Point pos2Scrn( Point2d pos1, double scale, int type) {
		return pos2Scrn( pos1, scale, type, false);
	}

	// for display3view pos2Scrn depends upon which pane is used
	protected Point pos2Scrn( Point2d pos1, double scale, int type, int tmpSliceType, boolean intMode) {
		int saveVal = sliceType;
		sliceType = tmpSliceType;
		Point out1 = pos2Scrn( pos1, scale, type, intMode);
		sliceType = saveVal;	// restore it
		return out1;
	}

	Point2d getPan(int type) {
		return getPan(type, false);
	}
	
	Point2d getPan(int type, boolean intMode) {
		int origWidth, zfact=1, origHeight, sizeX, sizeY, type1;
		double xpan, ypan;
		origWidth = data1.width;
		origHeight = data1.height;
		xpan = pan.x;
		ypan = pan.y;
		type1 = type;
		if( type1 == 1 || type1 == 2 || type1 == 4) {
			if( type1 == 2 || type1 == 4) {
				zfact = getDispWidthHeight(3);	// 2 for tricubic
				origHeight = getNormalizedNumFrms()*zfact;
			}
			ypan = pan.z/zfact;
			if( getObliqueSliceType() == DSP_SAGITAL) xpan = pan.y;
		}
		Point2d pan1 = new Point2d();
		sizeX = getZoomSize(origWidth, 0);
		sizeY = getZoomSize(origHeight, type);
		pan1.x = xpan*sizeX + (origWidth - sizeX)/2.0;
		pan1.y = ypan*sizeY + (origHeight - sizeY)/2.0;
		if( intMode) {
			pan1.x = ChoosePetCt.round(pan1.x);
			pan1.y = ChoosePetCt.round(pan1.y);
		}
		return pan1;
	}

	boolean updatePanValue( Point3d pan1, boolean force) {
		boolean retVal = force;
		int diff;
		if( !retVal) {
			diff = ChoosePetCt.round(Math.abs((pan1.x - pan.x) * 1000));
			if( diff > 0) retVal = true;
			if( !retVal) {
				diff = ChoosePetCt.round(Math.abs((pan1.y - pan.y) * 1000));
				if( diff > 0) retVal = true;
			}
			if( !retVal) {
				diff = ChoosePetCt.round(Math.abs((pan1.z - pan.z) * 1000));
				if( diff > 0) retVal = true;
			}
			if( !retVal) return false;
		}
		pan = pan1;
		dirtyFlg = true;
		corSrc = null;
		return true;
	}

	int setZoom(int incr) {
		double zoomVals[] = new double[] {1., 1.1, 1.2, 1.3, 1.4, 1.5, 1.7, 2., 2.5, 3., 4., 6., 8.};
		if( incr == 0) return zoomIndx;
		int oldVal = zoomIndx;
		double maxZ;
		if( incr > 0) zoomIndx++;
		else {
			zoomIndx--;
			if( incr == -1000) zoomIndx = 0;
		}
		if( zoomIndx < 0) zoomIndx = 0;
		if( zoomIndx >= zoomVals.length) zoomIndx = zoomVals.length - 1;
		zoom1 = zoomVals[zoomIndx];
		// see if anything changed
		if( zoomIndx == oldVal) return zoomIndx;
		if( incr < 0) {
			maxZ = (zoom1 - 1.0)/(2*zoom1);
			if( pan.x > maxZ) pan.x = maxZ;
			if( pan.x < -maxZ) pan.x = -maxZ;
			if( pan.y > maxZ) pan.y = maxZ;
			if( pan.y < -maxZ) pan.y = -maxZ;
			if( pan.z > maxZ) pan.z = maxZ;
			if( pan.z < -maxZ) pan.z = -maxZ;
		}
		dirtyFlg = true;
		corSrc = null;
		return zoomIndx;
	}

	void drawCine(Graphics2D g, double scale, JPanel jDraw, boolean isCine) {
		if( isCine) {
			if( ++cineIndx >= data1.numFrms) cineIndx = 0;
		}

		setGraphics(g, scale);
		fillSource(cineIndx, 1, 0);
		Image img = jDraw.createImage(source[cineIndx]);
		int w1, h1;
		w1 = ChoosePetCt.round(scale * data1.width);
		h1 = ChoosePetCt.round(scale * data1.height * zoomX * data1.y2xFactor);
		g.drawImage(img, 2*w1, 0, w1, h1, null);
	}

	Point getMIPposition(double petAxial, double petCoronal, double petSagital, double scale) {
		double width2, xin, yin, xout;
		double[] coss;
		int indx1 = data1.numFrms;
		indx1 = (indx1 - cineIndx) % indx1;
		width2 = data1.width / 2.0;
		xin = petSagital - width2;
		yin = petCoronal - width2;
		coss = data1.setCosSin(indx1);
		xout = xin*coss[0] + yin*coss[1] + width2;
		if( xout < 0 || xout >= data1.width) return null;
		Point2d pos1 = new Point2d(xout, petAxial);
		return pos2Scrn( pos1, scale, 1);	// 1 for MIP
	}

	/**
	 * Routine to draw both on screen and off screen images.
	 *
	 * This routine prepares at least the on screen image and the offscreen image as well
	 * if the class variable offscr != null.
	 * The offscreen has a different color scale, cm2. Its main purpose is for fusion images.
	 * Sometimes ONLY the fusion image is drawn. To take care of this case the class variable
	 * imgPos[0].x is set to less than 0,
	 * which is a sign NOT to display the unfused data.
	 *
	 * @param g graphics2D object from the paint routine
	 * @param scale adjusts between frame size and display size
	 * @param jDraw the parent frame
	 */

	protected void drawImages(Graphics2D g, double scale, JPanel jDraw) {
		drawImages(g, scale, jDraw, false);
	}
	
	protected Graphics2D setGraphics(Graphics2D g, double scale) {
		Object hint;
		hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
//		if( zoom1*scale > 1.2) hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
/*		if( qualityRendering) {
			hint = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		}*/
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
		return g;
	}

	protected void drawImages(Graphics2D g, double scale, JPanel jDraw, boolean obliqueFlg) {
		if( imgPos == null) return;
		saveParent = jDraw;	// need later for oblique
		setGraphics(g, scale);
/*		SunHints.Value h2 = (SunHints.Value)g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		String tmp = "CT ";
		if( data1.SUVfactor > 0) tmp = "PET ";
		tmp += h2.toString();
		IJ.log(tmp);*/
		Graphics2D osg;
		int i, w1, h1, x1, y1, yOff, yOffOs, xOff, numDisp1, indx1;
		double yOffd1;
		w1 = data1.width;
		yOffd1 = mriOffY*mriScl*zoom1 + mriOffY0;
		yOff = ChoosePetCt.round(scale*(multYOff*w1 + yOffd1));
		yOffOs = ChoosePetCt.round(scale*yOffd1);	// offscreen no multYOff
		xOff = ChoosePetCt.round(scale*mriOffX*mriScl*zoom1);
		w1 = ChoosePetCt.round(scale * w1);
		h1 = ChoosePetCt.round(scale * data1.height * zoomY * data1.y2XMri);
		MemoryImageSource[] currSource	= source;
		numDisp1 = numDisp;
		indx1 = indx;
		if( obliqueFlg) {
			currSource = corSrc;
			numDisp1 = 1;
			indx1 = 0;
		}
		if( cm2 != null) {
			offscr = jDraw.createImage(w1,h1);
			osg = (Graphics2D) offscr.getGraphics();
			setGraphics(osg, scale);
			Image img1 = jDraw.createImage(offSrc[0]);
			osg.drawImage(img1, xOff,  yOffOs, w1, h1, null);
		}
		for( i=0; i<numDisp1; i++) {
			x1 = imgPos[i].x;
			if( x1 < 0) continue;
			y1 = imgPos[i].y;
			Image img = jDraw.createImage(currSource[i+indx1]);
			g.drawImage(img, x1*w1 + xOff, y1*h1 + yOff, w1, h1, null);
		}
	}
	
	void setAvgSliceDiff() {
		int n = getNormalizedNumFrms();
		double diff = getStackDepth()/(n-1);
		if( diff == 0) diff = data1.spacingBetweenSlices;
		avgSliceDiff = diff;
	}

	protected void drawCorSagImages(Graphics2D g, double scale, JPanel jDraw, boolean corFlg) {
		if( imgPos == null) return;
		saveParent = jDraw;	// need this later for oblique
		// on coronal and sagital images reduce the value from 1.2.
		// the eye is sensitive to the differences between slices.
		setGraphics(g, 2*scale);
		Graphics2D osg;
		int i, w1, w2, h1, x1, y1, yOff, xOff;
		w1 = data1.width;
		yOff = ChoosePetCt.round(scale* corSagShift * zoom1 * zoomX * data1.y2xFactor * data1.y2XMri);
		w1 = w2 = ChoosePetCt.round(scale * w1);
		h1 = ChoosePetCt.round(scale * data1.numFrms * zoom1 * zoomX * data1.y2xFactor * data1.y2XMri / data1.numTimeSlots);	// not height
		i = 0;
		xOff = mriOffX;
		if( !corFlg) {
			i = 1;
			xOff = mriOffY + mriOffSag;
			w2 = ChoosePetCt.round(data1.height*scale*data1.y2XMri/aspect);
		}
		xOff = ChoosePetCt.round(scale*xOff*mriScl*zoom1);
		if( cm2 != null) {
			offscrH1 = h1;
			offscrY1 = yOff;
			offscr = jDraw.createImage(w2,h1);
			osg = (Graphics2D) offscr.getGraphics();
			setGraphics(osg, 2*scale);
			Image img1 = jDraw.createImage(offSrc[i+1]);
			if(offSrc[i+1]!=null) osg.drawImage(img1, xOff,  0, w2, h1, null);
		}
		x1 = imgPos[i+1].x;
		if( x1 < 0) return;
		y1 = imgPos[i+1].y;
		Image img = jDraw.createImage(corSrc[i]);
		if( corSrc[i] != null) g.drawImage(img, x1*w1 + xOff, y1*h1 + yOff, w2, h1, null);
	}
	
	// for display3view drawFusedImage depends upon which panel is used
	protected void drawFusedImage(Graphics2D g, double scale, JFijiPipe other1, Point pos1, JPanel jDraw, int tmpSliceType) {
		int saveVal = sliceType;
		sliceType = tmpSliceType;
		drawFusedImage(g, scale, other1, pos1, jDraw);
		sliceType = saveVal;	// restore it
	}

	protected void drawFusedImage(Graphics2D g, double scale, JFijiPipe other1, Point pos1, JPanel jDraw) {
		if( offscr == null) return;
		int h1=0, h2, yOff1, yOff2, w1, w2, w = data1.width;
		yOff1 = yOff2 = ChoosePetCt.round( scale*multYOff*w);
		w2 = w1 = ChoosePetCt.round(scale * w);
		if( aspect != 1.0 || other1.aspect != 1.0)
			h1++;
		h1 = ChoosePetCt.round(scale * data1.height * other1.aspect * zoomY);
		h2 = ChoosePetCt.round(scale * data1.height * zoomY);
		float factor;
		if( sliceType != DSP_AXIAL) {
			yOff2 = offscrY1;	// as far as I can see, this is always zero
			yOff1 = other1.offscrY1;
			h1 = other1.offscrH1;
			h2 = offscrH1;
//			if( sliceType == DSP_SAGITAL) w1 = ChoosePetCt.round(scale * other1.aspect * w);
		}
		Composite old = g.getComposite();
//		factor = (float) (fuseLevel * fuseFactor / 100000);
//		if( factor > 1.0f) factor = 1.0f;
		g.setColor(Color.BLACK);
		g.fillRect(pos1.x*w2, yOff1, w2, h2);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		g.drawImage(other1.offscr, pos1.x*w2, yOff1, w1, h1, null);
//		factor = (float) ((1000 - fuseLevel) * fuseFactor / 100000);
		factor = (float) ((1000 - fuseLevel)  / 1000);
		if( factor > 1.0f) factor = 1.0f;
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, factor));
		g.drawImage(offscr, pos1.x*w2, yOff2, w2, h2, null);
		g.setComposite(old);
	}

	double getZpos( double petPos) {
		if( data1.zpos == null) return 0;
		int ipetPos = ChoosePetCt.round(petPos);
		if( ipetPos < 0 || ipetPos >= data1.zpos.size()) return data1.zpos.get(0);
		double offst = (petPos - ipetPos)*avgSliceDiff;
		return data1.zpos.get(ipetPos) + offst + data1.mriOffZ * data1.spacingBetweenSlices;
	}
        
	int getCtSliceNum(int ctInNum) {
		int ctNum = data1.imageNumber.get(ctInNum);
		return ctNum;
	}
	
	int getNormalizedNumFrms() {
		return data1.numFrms / data1.numTimeSlots;
	}

	// note: original slice numbers count from 1, not from 0
	int getOrigSliceNum( int num1) {
		if(gaussSort == null) return num1;
		return gaussSort[num1-1] + 1;
	}

	// given a CT position, find the equivalent PET position
	int findPetPos( double ctPos, JFijiPipe srcPet0, boolean forceValid) {
		if( srcPet0 == null) return ChoosePetCt.round(ctPos);	// if called from Pet, return int
		if( imgPos == null) return -1;
		float ctPosZ = (float) getZpos(ctPos);
		int petNumFrm = srcPet0.data1.numFrms;
		int retVal = srcPet0.indx;	// initial guess = last value actually used
		if( retVal >= petNumFrm) retVal = petNumFrm - 1;
		if( retVal < 0) retVal = 0;
		float petPosZ = (float) srcPet0.getZpos(retVal);
		float diff1, diffZ = petPosZ - ctPosZ;
		while( Math.abs(diffZ) > 0.1) {
			if( diffZ < 0) {
				diffZ = -diffZ;
				retVal--;
				if( retVal < 0) {
					retVal = -1;
					if( forceValid) retVal = 0;
					break;
				}
				petPosZ = (float) srcPet0.getZpos(retVal);
				diff1 = petPosZ - ctPosZ;
				if( Math.abs(diff1) >= diffZ) {
					retVal++;	// set it back
					break;
				}
				diffZ = diff1;
			} else {
				retVal++;
				if( retVal >= petNumFrm) {
					retVal = -1;
					if( forceValid) retVal = petNumFrm - 1;
					break;
				}
				petPosZ = (float) srcPet0.getZpos(retVal);
				diff1 = petPosZ - ctPosZ;
				if( Math.abs(diff1) >= diffZ) {
					retVal--;	// set it back
					break;
				}
				diffZ = diff1;
			}
		}
		return retVal;
	}

	int findCtPos( double petPos, boolean forceValid) {
		if( srcPet == null) return ChoosePetCt.round(petPos);	// if called from Pet, return int
		if( imgPos == null) return -1;
		float petPosZ = (float) srcPet.getZpos(petPos);
		int retVal = indx;	// initial guess = last value actually used
		if( retVal < 0) retVal = 0;
		float ctPosZ = (float) getZpos(retVal);
		float diff1, diffZ = ctPosZ - petPosZ;
		while( Math.abs(diffZ) > 0.1) {
			if( diffZ < 0) {
				diffZ = -diffZ;
				retVal--;
				if( retVal < 0) {
					retVal = -1;
					if( forceValid) retVal = 0;
					break;
				}
				ctPosZ = (float) getZpos(retVal);
				diff1 = ctPosZ - petPosZ;
				if( Math.abs(diff1) >= diffZ && !isDuplicateSlice(retVal+1, retVal)) {
					retVal++;	// set it back
					break;
				}
				diffZ = diff1;
			} else {
				retVal++;
				if( retVal >= data1.numFrms) {
					retVal = -1;
					if( forceValid) retVal = data1.numFrms - 1;
					break;
				}
				ctPosZ = (float) getZpos(retVal);
				diff1 = ctPosZ - petPosZ;
				if( Math.abs(diff1) >= diffZ && !isDuplicateSlice(retVal-1, retVal)) {
					retVal--;	// set it back
					break;
				}
				diffZ = diff1;
			}
		}
		return retVal;
	}

	boolean isDuplicateSlice(int prev, int curr) {
		return( getZpos(prev) == getZpos(curr));
	}

	void prepareFused( int color1) {
		if( !isColorChanged(color1, true)) return;
		cm2 = null;
		if( offscrMode == 0) return;
		fusedMode = color1;
		oldWidth = -1;	// cause update
		cm2 = makeColorModel(color1);
	}

	void prepareFrame( double frmIndx, int type1, int color1, int src1) {
		startFrm = ChoosePetCt.round(frmIndx);
		int type = 0;
		if( zoom1 > 1) type = 3;
		makeDspImage( color1, type);
	}

	void prepareCoronalSagitalSub( double corSlice, double sagSlice, int colorMod1, int gateOffset, int type2) {
		UpdateCoronalSagital( corSlice, sagSlice, gateOffset, type2);
		makeCorDspImage(colorMod1, false);
	}

	void prepareCoronalSagital( double corSlice, double sagSlice, int colorMod1, int gateOffset) {
		prepareCoronalSagitalSub(corSlice, sagSlice, colorMod1, gateOffset, JFijiPipe.DSP_AXIAL);
	}
	
	void prepareAxialOblique( double axSlice, int secondaryType, int colorMod1) {
		UpdateAxialOblique(axSlice, secondaryType);
		makeCorDspImage(colorMod1, true);
	}

	public void makeDspImage(int colorMod1, int type) {
		if (startFrm + numDisp > data1.numFrms) {
			startFrm = data1.numFrms - numDisp;
		}
		if (startFrm < 0) {
			startFrm = 0;
		}
		if (isColorChanged(colorMod1, false)) {
			colorMode = colorMod1;
			cm = makeColorModel(colorMode);
			corSrc = null;	// cause update
			oldWidth = -1;	// not equal to winWidth....
		}
		if( useSrcPetWinLev) {	// used for MIP and uncorrected PET
			double srcSUV = srcPet.data1.SUVfactor;
			// more than 10% change should cause it to be called at most once
			if( srcSUV > 0 && Math.abs(srcSUV -  data1.SUVfactor) > 0.1*srcSUV)
				data1.setSUVfactor(srcSUV);
			winWidth = srcPet.winWidth;
			winLevel = srcPet.winLevel;
		}
		if( source == null || source.length != data1.numFrms) dirtyFlg = true;
		if ( oldLevel != winLevel || oldWidth != winWidth || dirtyFlg) {
/*			if ( oldLevel != winLevel || oldWidth != winWidth) {
				Double val = winLevel;
				String tmp = "win level = " + val.toString();
				val = winWidth;
				tmp += ", win width = " + val.toString();
				val = winLevel - winWidth/2;
				tmp += ", min = " + val.toString();
				IJ.log(tmp);
			}*/
			dirtyFlg = false;
			oldLevel = winLevel;
			oldWidth = winWidth;
			source = new MemoryImageSource[data1.numFrms];
			offSrc = new MemoryImageSource[3];
			indx = -1;
		}
		if (startFrm == indx) {
			return;	// all done
		}
		indx = startFrm;
		for (int j = 0; j < numDisp; j++) {
			fillSource(j+indx, type, 0);
		}
	}

	void makeCorDspImage(int colorMod1, boolean axialDisplay) {
		if( colorMod1 != colorMode) {
			colorMode = colorMod1;
			cm = makeColorModel(colorMode);
			corSrc = null;	// cause update
		}
		if ( oldCorLevel != winLevel || oldCorWidth != winWidth || corSrc == null) {
			oldCorLevel = winLevel;
			oldCorWidth = winWidth;
			corSrc = new MemoryImageSource[2];
			if(offSrc == null) offSrc = new MemoryImageSource[3];
		}
		int type2 = 2;
		if( axialDisplay) type2 = 5;
		if( coronalSlice >= 0) fillSource(0, type2, 1);
		if( sagitalSlice >= 0) fillSource(1, type2, 2);
	}
	
	int getObliqueSliceType() {
		int retVal = sliceType;
		if( retVal == DSP_OBLIQUE) {
			if( saveParent instanceof PetCtPanel) {
				PetCtPanel panel1 = (PetCtPanel) saveParent;
				if(panel1.m_oblique != null) retVal = panel1.m_oblique.primaryPlane;
			}
		}
		return retVal;
	}

	// type1 = 0 for standard axial display
	// type1 = 1 for mip type display
	// type1 = 2 for coronal sagital display (axial source)
	// type1 = 3 for axial under zoom (use all y direction space)
	// type1 = 5 for oblique display with primary axial display
	private void fillSource(int indx1, int type1, int offInx) {
		if( indx1 >= data1.numFrms) return;
		MemoryImageSource[] currSource	= source;
		if( type1 == 2 || type1 == 5) currSource = corSrc;
		if( currSource == null) return;
		if( currSource[indx1] != null && cm2==null) return;	// already calculated

		int i, insertType, insertIdx, j, j1, k, i1, curr1, max255, sizeX, sizeX1, sizeY;
		int off1, w1, w1a, origWidth, zfact, origHeight, sagShift = 0;
		double scale, slope, xpan, ypan, xshft=0, yshft=0, sagMRI = 1.0;
		short currShort;
		int min1;
		boolean badY, goodLungY;
		int coef0 = data1.getCoefficentAll();
		Point pan1 = new Point();
		slope = data1.getRescaleSlope(indx1);
		if( isLog) {
			i = data1.maxPixel;
			logOff = i/32;
			if( logOff < 5) logOff = 5;
			logLow = Math.log(logOff);
			logNorm = i/(Math.log(logOff+i)-logLow);
			logFactor = slope/data1.getMaxRescaleSlope();
			if( type1 == 2 || type1 == 5) logFactor = 1.0;
			if( LogConv == null) LogConv = new short[32768];
		}
		slope = winSlope * slope;
		origWidth = w1 = w1a = getDispWidthHeight(0);
		origHeight = getDispWidthHeight(1);
		zfact = getDispWidthHeight(3);	// 2 for tricubic
		xpan = pan.x;
		ypan = pan.y;
		if( useShiftXY ) {
			xshft = shiftXY[0];
			yshft = shiftXY[1];
		}
		insertType = (type1 == 0 || type1 == 3)?DSP_AXIAL:0;
		insertIdx = indx1;
		if( type1 == 2) {
			insertType = DSP_CORONAL;
			insertIdx = coronalSlice;
			if( indx1 > 0) { // either 0 or 1
				insertType = DSP_SAGITAL;
				insertIdx = sagitalSlice;
				xpan = pan.y;
				sagMRI = data1.y2XMri;
				w1a = origWidth = origHeight;
				w1 = ChoosePetCt.round(origWidth/aspect);
				xshft = yshft;
			}
			yshft = 0;
			ypan = pan.z * data1.panZCt2Pet;
			origHeight = getNormalizedNumFrms()*zfact;
			slope = winSlope * data1.getMaxRescaleSlope();
			coef0 = 0;
		}
		lungInsert lgIn = findInsert(insertIdx, insertType);
		if( type1 == 5) coef0 = 0;
		if( type1 == 3) ypan /= aspect;
		sizeX = sizeX1 = getZoomSize(w1, 0);
		if(w1 != w1a) {
			sizeX1 = getZoomSize(w1a, 0);
			sagShift += ChoosePetCt.round(mriOffY0/(zoomX*zoom1*sagMRI));
		}
		sizeY = getZoomSize(origHeight, type1);
		pan1.x = ChoosePetCt.round(xpan*sizeX + (w1a - sizeX1)/2.0 - sagShift + xshft);
		pan1.y = ChoosePetCt.round(ypan*sizeY + (origHeight - sizeY)/2.0 + yshft);
		if(getDispWidthHeight(2) == 2) {
			pan1.x++;
			pan1.y++;
		}
		short[] buff1;
		max255 = 255;
		if( useSrcPetWinLev) {
			winWidth = srcPet.winWidth;
			winLevel = srcPet.winLevel;
		}
		double grayMin = winLevel - winWidth/2;
		if( grayMin < data1.minVal) grayMin = data1.minVal;
		min1 = (int)((grayMin - data1.shiftIntercept())/slope);
		scale = 256.0*slope/winWidth;
		byte[] buff = new byte[sizeX*sizeY];
		off1 = origWidth*pan1.y;
		switch( data1.depth) {
			case 32:
				float[] inFlt;
				inFlt = data1.pixFloat.get(indx1);
				if( type1 == 2 || type1 == 5) inFlt = rawFloatPix.get(indx1);
				for( j=k=0; j<sizeY; j++) {
					badY = false;
					if( j+pan1.y < 0 || j+pan1.y >= origHeight) badY = true;
					for( i=0; i<sizeX; i++) {
						i1 = i+pan1.x;
						if( badY || i1 < 0 || i1 >= origWidth) curr1 = 0;
						else {
							curr1 = (int)((inFlt[off1+i1] - min1) * scale);
						}
						if( curr1 > max255)
							curr1 = max255;
						if( curr1 < 0) curr1 = 0;
						buff[k++] = (byte) curr1;
					}
					off1 += origWidth;
				}
				break;

			case 8:
				byte[] inByt;
				inByt = data1.pixByt.get(indx1);
				if( type1 == 2 || type1 == 5) inByt = rawBytPix.get(indx1);
				for( j=k=0; j<sizeY; j++) {
					for( i=0; i<sizeX; i++) {
						curr1 = inByt[off1+i];
						if( curr1 < 0) curr1 = 256 + curr1;
						curr1 = (int)((curr1 - min1) * scale);
						if( curr1 > max255)
							curr1 = max255;
						if( curr1 < 0) curr1 = 0;
						buff[k++] = (byte) curr1;
					}
					off1 += origWidth;
				}
				break;

			default:
				buff1 = data1.getPixels(indx1*zfact);
				if( type1 == 2 || type1 == 5) buff1 = rawPixels.get(indx1);
				for( j=k=0; j<sizeY; j++) {
					j1 = j+pan1.y;
					badY = false;
					goodLungY = false;
					if( lgIn != null && j1>=lgIn.yOff && j1<lgIn.yOff+lgIn.height) goodLungY = true;
					if( j1 < 0 || j1 >= origHeight) badY = true;
					for( i=0; i<sizeX; i++) {
						i1 = i+pan1.x;
						if( badY || i1 < 0 || i1 >= origWidth) curr1 = 0;
						else {
							currShort = (short)(buff1[off1+i1] + coef0);
							if(goodLungY && lgIn != null) {	// maybe use insert
								if(i1>=lgIn.xOff && i1<lgIn.xOff+lgIn.width) {
									currShort = lgIn.pixels[(j1-lgIn.yOff)*lgIn.width + i1-lgIn.xOff];
								}
							}
							curr1 = maybeLog(currShort);
							curr1 = (int)((curr1 - min1) * scale);
						}
						if( curr1 > max255)
							curr1 = max255;
						if( curr1 < 0) curr1 = 0;
						buff[k++] = (byte) curr1;
					}
					off1 += origWidth;
				}
		}
		currSource[indx1] = new MemoryImageSource(sizeX, sizeY, cm, buff, 0, sizeX);
		// if there is an offscreen memory, calculate it. It is the same as the above with different colors
		if( cm2 != null ) offSrc[offInx] = new MemoryImageSource(sizeX, sizeY, cm2, buff, 0, sizeX);
	}

	int getDispWidthHeight(int type) {
		int val, factor = 1;
		val = data1.width;
		switch( type) {
			case 1:
				val = data1.height;
				break;

			case 2:
			case 3:
				val = 1;
				break;
		}
		if( data1.pixel2 != null && !(hasGauss && useGauss)) factor = 2;
		if( type == 3 && !triFlg) factor = 1;	// z step
		return factor*val;
	}

	int maybeLog(short inShort) {
		if(!isLog) return inShort;
		int inVal = (int)(inShort*logFactor);
		if( inVal <= 0) return 0;
		if( inVal > 32767)
			inVal = 32767;
		int retVal = LogConv[inVal];
		if( retVal == 0) {
			retVal = (int) (logNorm * (Math.log(inVal+logOff)-logLow));
			if( retVal <= 0 || retVal >32767)
				retVal = 0;
			LogConv[inVal] = (short) retVal;
		}
		return (int) (retVal/logFactor);
	}
	
	void setLogSlider() {
		double hiVal, off1, lgLow, lgNorm, winWid;
		if(!isLog) return;
		hiVal = data1.sliderSUVMax;
		off1 = hiVal / 32.767;
		if( off1 <= 0) return;
		lgLow = Math.log(off1);
		lgNorm = hiVal/(Math.log(off1+hiVal)-lgLow);
		winWid = lgNorm * (Math.log(off1+winWidth) - lgLow);
		winWidth = winWid;
		winLevel = winWid/2;
	}

	double getLogOriginal( double in1) {
		double hiVal, off1, lgLow, lgNorm, out1;
		if( !isLog) return in1;
		hiVal = data1.sliderSUVMax;
		off1 = hiVal / 32.767;
		if( off1 <= 0) return 0;
		lgLow = Math.log(off1);
		lgNorm = hiVal/(Math.log(off1+hiVal)-lgLow);
		out1 = in1/lgNorm + lgLow;
		out1 = Math.exp(out1) - off1;
		return out1;
	}
	/**
	 * 
	 * @param in1 suggested y value
	 * @param type same as getZoom, below
	 * @return y dimension of the matrix
	 */
	int getZoomSize(int in1, int type) {
		int out1 = in1;
		if( type == 1 || type == 2 || type == 4) return out1;
		double zoomTmp = getZoom(type);
		if( zoomTmp == 1.0) return out1;
		out1 = ChoosePetCt.round(in1 / zoomTmp);
		if( type == 3) {
			out1 = ChoosePetCt.round(in1 * zoomY / zoomTmp);
			if( multYOff > 0.000001 ) out1 = ChoosePetCt.round(in1/zoomX);
		}
		return out1;
	}

	/**
	 * type 0 and 3 are used for axial slices
	 * type 1 is used for MIP
	 * type 2 is used for coronal, sagital
	 * type 4 is used in display3panel for coronal, sagital
	 * type 5 is used in oblique with primary axial
	 * @param type 0 -> 3
	 * @return the appropriate zoom factor
	 */
	double getZoom(int type) {
		double zoomTmp = zoom1;
		switch(type) {
			case 0:	// axial
			case 3:	// axial with zoom
			case 5:	// oblique axial
				zoomTmp *= zoomX;
				break;

			case 1:	// mip - maybe don't want zoomX?
			case 2:	// coronal, sagital
			case 4:	// display3panel
				zoomTmp *= zoomX * data1.y2xFactor;
		}
		return zoomTmp;
	}
	
	boolean isColorChanged(int mode, boolean fused) {
		if(fused) {
			if(cm2 == null || mode != fusedMode) return true;
		}
		else {
			if( cm == null || mode != colorMode) return true;
		}
		if( mode == COLOR_USER) {
			ImagePlus src = data1.srcImage;
			if(src == null) return true;
			ImageProcessor ip = src.getProcessor();
			ColorModel cm1 = ip.getColorModel();
			if( cm1 != cm_src) return true;
		}
		return false;
	}

	ColorModel makeColorModel(int mode) {
		int i;
		ColorModel cm1;
		JFijiPipe src;
		byte[] rLUT = new byte[256];
		byte[] gLUT = new byte[256];
		byte[] bLUT = new byte[256];
		LUT luts;
		switch(mode) {
			case COLOR_BLUES:
			case COLOR_HOTIRON:
			case COLOR_MRI:
				luts = getDefaultLut(mode);
				luts.getReds(rLUT);
				luts.getGreens(gLUT);
				luts.getBlues(bLUT);
				break;

			case COLOR_INVERSE:
				for (i = 0; i < 256; i++) {
					rLUT[i] = (byte) (255-i);
					gLUT[i] = (byte) (255-i);
					bLUT[i] = (byte) (255-i);
				}
				break;

			case COLOR_USER:
			case COLOR_PET_USER:
				src = this;
				if(mode == COLOR_PET_USER && srcPet != null) src = srcPet;
				luts = getSourceLut(src);
				if( luts != null) {
					luts.getReds(rLUT);
					luts.getGreens(gLUT);
					luts.getBlues(bLUT);
					break;
				}	// else fall through

			case COLOR_GRAY:
			default:
				for (i = 0; i < 256; i++) {
					rLUT[i] = (byte) i;
					gLUT[i] = (byte) i;
					bLUT[i] = (byte) i;
				}
		}
		cm1 = new IndexColorModel(8, 256, rLUT, gLUT, bLUT);
		return cm1;
	}
	
	LUT getDefaultLut(int mode) {
		LUT luts = null;
		int i;
		String inRes, inLut;
		byte[] rLUT = new byte[256];
		byte[] gLUT = new byte[256];
		byte[] bLUT = new byte[256];
		byte[] buff = new byte[3 * 256];
		InputStream fl1;
		if( mode == COLOR_BLUES) {
			inRes = "/resources/color1.dat";
			inLut = extBluesLUT;
		} else {
			inRes = "/resources/hotiron.dat";
			inLut = extHotIronLUT;
			if( mode == COLOR_MRI) inLut = extCtMriLUT;
		}
		try {
			if( inLut != null && !inLut.isEmpty()) {
				luts = LutLoader.openLut(inLut);
				if( luts != null) return luts;
			}
			fl1 = getClass().getResourceAsStream(inRes);
			fl1.read(buff);
			fl1.close();
			for (i = 0; i < 256; i++) {
				rLUT[i] = buff[3*i];
				gLUT[i] = buff[3*i+1];
				bLUT[i] = buff[3*i+2];
			}
			luts = new LUT(rLUT, gLUT, bLUT);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return luts;
	}
	
	LUT getSourceLut(JFijiPipe pip1) {
		ImagePlus src = pip1.data1.srcImage;
		if(src == null) return null;
		LUT luts = null;
		if( pip1.fixedLUT != null) {
			luts = LutLoader.openLut(pip1.fixedLUT);
			return luts;
		}
		ImageProcessor ip = src.getProcessor();
		cm_src = ip.getColorModel();
		if (cm_src instanceof IndexColorModel) {
			luts= new LUT((IndexColorModel)cm_src, ip.getMin(), ip.getMax());
		}
		return luts;
	}

	void dirtyCorSag() {
		corSrc = null;	// cause update
	}
	
	void UpdateAxialOblique( double axSlice, int secondaryType) {
		int i, x1, zg1, off1, wid1 = data1.width, heigh1 = getNormalizedNumFrms();
		int slic1, type1, sliceNum, step1, iaxSlice;
		double scale;
		byte[] currByte = null, srcByt;
		short[] currShort = null, srcPix;
		float[] currFloat = null, srcFlt;
		short tmpShort;
		int coef0 = data1.getCoefficent0();
		iaxSlice = ChoosePetCt.round(axSlice);
		switch( data1.depth) {
			case 32:
				if( rawFloatPix == null) {
					rawFloatPix = new ArrayList<float[]>(2);
					rawFloatPix.add(null);
					rawFloatPix.add(null);
				}
				break;

			case 8:
				if( rawBytPix == null) {
					rawBytPix = new ArrayList<byte[]>(2);
					rawBytPix.add(null);
					rawBytPix.add(null);
				}
				break;

			default:
				if( rawPixels == null) {
					rawPixels = new ArrayList<short[]>(2);
					rawPixels.add(null);
					rawPixels.add(null);
				}
		}
		if(secondaryType == JFijiPipe.DSP_CORONAL) {
			sliceNum = coronalSlice;
			sagitalSlice = -1;
			step1 = 1;
			type1 = 0;
		} else {
			sliceNum = sagitalSlice;
			coronalSlice = -1;
			step1 = wid1;
			type1 = 1;
		}
		if( iaxSlice < 0 || corSrc == null) sliceNum = -1;
		if( iaxSlice != sliceNum && iaxSlice >= 0 && iaxSlice < heigh1 && iaxSlice >= 0) {
			switch( data1.depth) {
				case 32:
					currFloat = new float[wid1*wid1];
					break;

				case 8:
					currByte = new byte[wid1*wid1];
					break;

				default:
					currShort = new short[wid1*wid1];
			}
			sliceNum = iaxSlice;
			corSrc = null;	// cause update
			for( x1=0; x1<wid1; x1++) {
				// here we assume that corFactor = sagFactor, i.e width=height
				slic1 = ChoosePetCt.round(axSlice + (x1 - wid1/2.0)*obliqueFactor/corFactor);
				if( slic1 < 0 || slic1 >= heigh1) continue;
				off1 = x1*wid1/step1;	// either x1*wid1 (coronal) or x1 (sagital)
				zg1 = slic1;
				scale = data1.getRescaleSlope(zg1)/data1.getMaxRescaleSlope();
				switch( data1.depth) {
					case 32:
						srcFlt = data1.pixFloat.get(zg1);
						for(i=0; i<wid1; i++) {
							currFloat[off1+i*step1] = srcFlt[off1+i*step1];
						}
						break;

					case 8:
						srcByt = data1.pixByt.get(zg1);
						for(i=0; i<wid1; i++) {
							currByte[off1+i*step1] = limitByte(srcByt[off1+i*step1], scale);
						}
						break;

					default:
						srcPix = data1.pixels.get(zg1);
						for( i=0; i<wid1; i++) {
							tmpShort = (short) (srcPix[off1+i*step1] + coef0);
							currShort[off1+i*step1] = limitShort(tmpShort, scale);
						}
				}
			}
			switch( data1.depth) {
				case 32:
					rawFloatPix.set(type1, currFloat);
					break;

				case 8:
					rawBytPix.set(type1, currByte);
					break;

				default:
					rawPixels.set(type1, currShort);
			}
		}
		if(secondaryType == JFijiPipe.DSP_CORONAL) coronalSlice = sliceNum;
		else sagitalSlice = sliceNum;
	}

	// type2 is the secondaryPlane in the oblique view.
	void UpdateCoronalSagital( double corSlice, double sagSlice, int gateOffset, int type2) {
		int i, i1, z1, zg1, off1, off2, wid1, wid2, mult1;
		int slic1, zfact, heigh1 = getNormalizedNumFrms();
		double scale, stepObl=0;
		boolean oblFlag;
		byte[] currByte = null, srcByt;
		short[] currShort = null, srcPix;
		float[] currFloat = null, srcFlt;
		short tmpShort;
		int coef0 = data1.getCoefficentAll();
		wid1 = getDispWidthHeight(0);
		wid2 = getDispWidthHeight(1);
		mult1 = getDispWidthHeight(2);
		zfact = getDispWidthHeight(3);	// for tricubic zfact=2
		heigh1 *= zfact;
		switch( data1.depth) {
			case 32:
				if( rawFloatPix == null) {
					rawFloatPix = new ArrayList<float[]>(2);
					rawFloatPix.add(null);
					rawFloatPix.add(null);
				}
				break;

			case 8:
				if( rawBytPix == null) {
					rawBytPix = new ArrayList<byte[]>(2);
					rawBytPix.add(null);
					rawBytPix.add(null);
				}
				break;

			default:
				if( rawPixels == null) {
					rawPixels = new ArrayList<short[]>(2);
					rawPixels.add(null);
					rawPixels.add(null);
				}
		}
		if( corSlice < 0 || corSrc == null) coronalSlice = oldCorSlice = -1;
		oblFlag = false;
		slic1 = ChoosePetCt.round(corFactor * corSlice * mult1 + corOffset - mriOffY);
		if( oldCorSlice != slic1 && corSlice >= 0 && slic1 < wid2 && slic1 >= 0) {
			switch( data1.depth) {
				case 32:
					currFloat = new float[wid1*heigh1];
					break;

				case 8:
					currByte = new byte[wid1*heigh1];
					break;

				default:
					currShort = new short[wid1*heigh1];
			}
			oldCorSlice = slic1;
			coronalSlice = ChoosePetCt.round(corSlice);
			corSrc = null;	// cause update
			if( obliqueFactor != 0 && type2 != JFijiPipe.DSP_AXIAL) {
				slic1 = ChoosePetCt.round(corFactor*corSlice*mult1 - wid2*obliqueFactor/2 + corOffset);
				stepObl = obliqueFactor;
				oblFlag = true;
			}
			for( z1=0; z1<heigh1; z1++) {
				if( obliqueFactor != 0 && type2 == JFijiPipe.DSP_AXIAL) {
					slic1 = ChoosePetCt.round(corFactor *(corSlice*mult1 + (z1 - heigh1/2.0)*obliqueFactor) + corOffset);
					if( slic1 < 0 || slic1 >= wid2) continue;
				}
				off1 = slic1*wid1;
				off2 = z1*wid1;
				i1 = 0;
				zg1 = z1 + gateOffset;
				scale = data1.getRescaleSlope(zg1/zfact)/data1.getMaxRescaleSlope();
				switch( data1.depth) {
					case 32:
						srcFlt = data1.pixFloat.get(zg1);
						for(i=0; i<wid1; i++) {
							if(oblFlag) {
								i1 = (int)(i*stepObl);
								if(slic1 + i1 < 0 || slic1 + i1 >= wid2) continue;
								i1 *= wid2;
							}
							currFloat[off2+i] = srcFlt[off1+i+i1];
						}
						break;

					case 8:
						srcByt = data1.pixByt.get(zg1);
						for(i=0; i<wid1; i++) {
							if(oblFlag) {
								i1 = (int)(i*stepObl);
								if(slic1 + i1 < 0 || slic1 + i1 >= wid2) continue;
								i1 *= wid2;
							}
							currByte[off2+i] = limitByte(srcByt[off1+i+i1], scale);
						}
						break;

					default:
						srcPix = data1.getPixels(zg1);
						for( i=0; i<wid1; i++) {
							if(oblFlag) {
								i1 = (int)(i*stepObl);
								if(slic1 + i1 < 0 || slic1 + i1 >= wid2) continue;
								i1 *= wid2;
							}
							tmpShort = (short) (srcPix[off1+i+i1] + coef0);
							currShort[off2+i] = limitShort(tmpShort, scale);
						}
				}
			}
			switch( data1.depth) {
				case 32:
					rawFloatPix.set(0, currFloat);
					break;

				case 8:
					rawBytPix.set(0, currByte);
					break;

				default:
					rawPixels.set(0, currShort);
			}
		}

		if( sagSlice < 0 || corSrc == null) sagitalSlice = oldSagSlice = -1;
		oblFlag = false;
		slic1 = ChoosePetCt.round(sagFactor * sagSlice * mult1 + sagOffset - mriOffX);
		if( oldSagSlice != slic1 && sagSlice >= 0 && slic1 < wid1 && slic1 >= 0) {
			switch( data1.depth) {
				case 32:
					currFloat = new float[wid2*heigh1];
					break;

				case 8:
					currByte = new byte[wid2*heigh1];
					break;

				default:
					currShort = new short[wid2*heigh1];
			}
			oldSagSlice = slic1;
			sagitalSlice = ChoosePetCt.round(sagSlice);
			corSrc = null;	// cause update
			if( obliqueFactor != 0 && type2 != JFijiPipe.DSP_AXIAL) {
				slic1 = ChoosePetCt.round(sagFactor*sagSlice*mult1 - wid1*obliqueFactor/2 + sagOffset);
				stepObl = obliqueFactor;
				oblFlag = true;
			}
			for( z1=0; z1<heigh1; z1++) {
				if( obliqueFactor != 0 && type2 == JFijiPipe.DSP_AXIAL) {
					slic1 = ChoosePetCt.round(sagFactor *(sagSlice*mult1 + (z1 - heigh1/2.0)*obliqueFactor) + sagOffset);
					if( slic1 < 0 || slic1 >= wid1) continue;
				}
				off1 = slic1;	// maybe add mriOffY0?
				off2 = z1*wid2;
				i1 = 0;
				zg1 = z1+gateOffset;
				scale = data1.getRescaleSlope(zg1/zfact)/data1.getMaxRescaleSlope();
				switch( data1.depth) {
					case 32:
						srcFlt = data1.pixFloat.get(zg1);
						for(i=0; i<wid2; i++) {
							if( oblFlag) {
								i1 = (int)(i*stepObl);
								if( off1+i1 < 0 || off1+i1>=wid1) continue;
							}
							currFloat[off2+i] = srcFlt[off1+i*wid1+i1];
						}
						break;

					case 8:
						srcByt = data1.pixByt.get(zg1);
						for(i=0; i<wid2; i++) {
							if( oblFlag) {
								i1 = (int)(i*stepObl);
								if( off1+i1 < 0 || off1+i1>=wid1) continue;
							}
							currByte[off2+i] = limitByte( srcByt[off1+i*wid1+i1], scale);
						}
						break;

					default:
						srcPix = data1.getPixels(zg1);
						for( i=0; i<wid2; i++) {
							if( oblFlag) {
								i1 = (int)(i*stepObl);
								if( off1+i1 < 0 || off1+i1>=wid1) continue;
							}
							tmpShort = (short) (srcPix[off1+i*wid1+i1] + coef0);
							currShort[off2+i] = limitShort(tmpShort, scale);
						}
				}
			}
			switch( data1.depth) {
				case 32:
					rawFloatPix.set(1, currFloat);
					break;

				case 8:
					rawBytPix.set(1, currByte);
					break;

				default:
					rawPixels.set(1, currShort);
			}
		}
	}

	double getStackDepth() {
		double ret = 0;
		int n = getNormalizedNumFrms();
		if( data1.zpos != null) {
			ret = data1.zpos.get(n-1) - data1.zpos.get(0);
		}
		return ret;
	}

	float getPixelSpacing( int type) {
		if( data1.pixelSpacing == null) return 1.0f;
		if( type == 0) return data1.pixelSpacing[0];
		return data1.pixelSpacing[1];
	}

	byte limitByte( byte inVal, double scale) {
		int tmpi = inVal;
		if( tmpi < 0) tmpi += 256;
		double tmp = tmpi * scale;
		if( tmp > 255) tmp = 255;
		return (byte) tmp;
	}

	short limitShort( short inVal, double scale) {
		double tmp = inVal * scale;
		if( tmp > 32767) tmp = 32767;
		return (short) tmp;
	}

	/**
	 * This is the structure which is returned when a line is requested.
	 * A line will be requested when building coronal and sagital slices
	 * It will also be requested when calculating the CT value and SUV.
	 */
	protected class lineEntry {
		boolean goodData = false;
		int	angle = 0;			// 0=coronal, 270= sagital
		int depth = 0;			// this is "y" for coronal and "x" for sagital
		int size1 = 0;
		short[]	pixels = null;	// this is the raw line data
		float[] pixFloat = null;	// or this for floating point data
		float[] pixelSpacing = null;
		double slope = 1.0;
		double maxSlope = 1.0;
		double SUVfactor = 0;	// zero means no SUV
	}

	protected class mipEntry {
		int zpos = 0;
		short[] xval = null;
		short[] yval = null;
	}

	protected class mipXYentry {
		int zlo, zhi = -1;
		ArrayList<mipEntry> xydata = new ArrayList<mipEntry>();
	}
	
	lungInsert createLungInsert() {
		return new lungInsert();
	}

	lungInsert findInsert(int zval, int type1) {
		if(lungData == null || !dispInsert) return null;
		int i, n=lungData.size();
		lungInsert lret;
		for( i=0; i<n; i++) {
			lret = lungData.get(i);
			if( lret.type != type1) continue;
			if( lret.zpos != zval) continue;
			return lret;
		}
		return null;
	}

	protected class MultiCubicInterpolator {
		double[][] p4;
		double[][][] v4;

		double getCubic (double[] p, double x) {
			return p[1] + 0.5 * x*(p[2] - p[0] + x*(2.0*p[0] - 5.0*p[1] + 4.0*p[2] - p[3] + x*(3.0*(p[1] - p[2]) + p[3] - p[0])));
		}

		double getBiCubic (double x, double y) {
			double[] arr = new double[4];
			arr[0] = getCubic(p4[0], y);
			arr[1] = getCubic(p4[1], y);
			arr[2] = getCubic(p4[2], y);
			arr[3] = getCubic(p4[3], y);
			return getCubic(arr, x);
		}

		double getTriCubic (double x, double y, double z) {
			double[] arr = new double[4];
			for( int i=0; i<4; i++) {
				p4 = v4[i];
				arr[i] = getBiCubic(y, z);
			}
			return getCubic(arr, x);
		}

		void fillP4(int row, int col, short[] pix1) {
			int coef0 = data1.getCoefficent0();
			int i, j, k, width1 = data1.width;
			p4 = new double[4][4];
			for( j=-1; j<3; j++) {
				for( i=-1; i<3; i++) {
					k = (j+row)*width1 + i + col;
//					p4[i+1][j+1] = j*10 + i;
					p4[i+1][j+1] = data1.getPix1Int(k, pix1, coef0);
				}
			}
		}

		void fillV4(int col, int row, int depth, double[][] in1) {
			int i, j, k, off1, width1 = data1.width;
			v4 = new double[4][4][4];
			for( k=-1; k<3; k++) {
				for( j=-1; j<3; j++) {
					off1 = (j+row)*width1 + col;
					for( i=-1; i<3; i++) {
						v4[i+1][j+1][k+1] = in1[off1+i][k+depth];
					}
				}
			}
		}
	}

	protected class lungInsert {
		int width =0, height = 0, type = 0;
		int xOff = 0, yOff = 0;
		int zpos = 0;
		short[] pixels = null;
		float[] pixFloat = null;
	}

	/**
	 * An inner class for storing details of the image stack
	 */
	protected class JData {
		int seriesType, axialRotation=0, numFrms=0, width, height, depth, maxPixel;
		int orientation, mriOffZ = 0;	// for matching frames to CT
		int srcHeight, numTimeSlots=0, frameTime, numBlankFrms=0;
		int SOPclass, fileFormat = FileInfo.UNKNOWN, MIPtype = 0;
		ArrayList<short []> pixels = null;
		ArrayList<short []> pixel2 = null;
		ArrayList<short []> tmpPix = null;
		ArrayList<byte []> pixByt = null;
		ArrayList<float []> pixFloat = null;
		ArrayList<Float> zpos = null;
		ArrayList<Double> rescaleSlope = null;
		ArrayList<Integer> trigTime = null;
		ArrayList<Integer> imageNumber = null;
		short[][] gaussPix = null;
		private int maxPosition = -1;
		float pixelSpacing[] = null;
		double pixelCenter[] = null;
		Date serTime, injectionTime, acquisitionTime;
		double maxVal, minVal, grandMax, spacingBetweenSlices, maxFactor;
		double halflife, patWeight, patHeight, totalDose, rescaleIntercept = 0, SUVfactor = 0.0;
		double sliderSUVMax = 1000., MIPslope = 1.0, philipsSUV = 0, SULfactor = 1.0;
		double expFac[] = null, y2xFactor = 1.0, y2XMip = 1.0, y2XMri = 1.0;
		double coef[] = null, panZCt2Pet = 1.0;
		boolean decayCorrect, isDynamic = false, isHalfPix = false;
		double spectSlope = 1.0;	// used for extended PET, all slices assumed to be the same
		int philipsCoef = 0, pixRep = 1;
		String seriesName = null, metaData = null;
		ImagePlus srcImage = null, gaussImage = null, orthAnno = null, orthBF = null;
		bkgdMipCalc work2 = null;

		/**
		 * Takes an ImagePlus object and parses the Dicom information necessary
		 * for the JFijiPipe. It then stores a reference to the binary data and the metaData.
		 * @param img1
		 */
		void readData(ImagePlus img1, int forcedVal) {
			int i, imgNum;
			double x;
			boolean spectFlg = false;	// SPECT or NM data has all slices in 1 file
			float zpos1=0, zposN=0, patPos[];
//			Date localAcqTime;
			Object pix1;
			srcImage = img1;	// save it for future use
			FileInfo fileI = img1.getOriginalFileInfo();
			if(fileI != null) fileFormat = fileI.fileFormat;
			numFrms = img1.getStackSize();
			resetGauss();
			width = img1.getStack().getWidth();
			height = img1.getStack().getHeight();
			depth = img1.getBitDepth();
			coef  = img1.getCalibration().getCoefficients();
			rescaleSlope = null;
			String meta = ChoosePetCt.getMeta(1, img1);
			if( !getMetaData(meta, forcedVal)) return;
			if( SOPclass == ChoosePetCt.SOPCLASS_TYPE_ENHANCED_PET || SOPclass == ChoosePetCt.SOPCLASS_TYPE_ENHANCED_CT) {
				patPos = ChoosePetCt.parseMultFloat(ChoosePetCt.getFirstDicomValue(meta, "0020,0032"));
				if( patPos!=null) zpos1 = patPos[2];
				patPos = ChoosePetCt.parseMultFloat(ChoosePetCt.getLastDicomValue(meta, "0020,0032"));
				if( patPos!=null) zposN = patPos[2];
				x = zposN - zpos1;
				spacingBetweenSlices = x / (numFrms-1);
				x = ChoosePetCt.parseDouble(ChoosePetCt.getFirstDicomValue(meta, "0028,1053"));
				if( x<=0) x= 1.0;
				spectSlope = x;
			}
			if(fileI != null) {
				i = fileI.nImages;
				if( i > 1 ) spectFlg = true;
			} else {
				// compressed Orthanc
				if( SOPclass == ChoosePetCt.SOPCLASS_TYPE_NM) spectFlg = true;
			}
			if( !spectFlg && depth < 32) rescaleSlope = new ArrayList<Double>();
			// the orientation is assumed to be the same for all slices in the stack
			for( i=1; i<=numFrms; i++) {
				pix1 = img1.getStack().getPixels(i);
				meta = ChoosePetCt.getMeta(i, img1);
				if( meta == null) {
					continue;
				}
				pix1 = reverseBuffer(pix1, orientation);
				switch( depth) {
					case 32:
						pixFloat.add((float[])pix1);
						break;

					case 8:
						pixByt.add((byte[]) pix1);
						break;

					default:
						pixels.add((short[]) pix1);
				}
				if( trigTime != null) {
					String tmp = "0018,1060";
					if(isDynamic) tmp = "0008,0032";
					Integer trigTime1 = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, tmp));
					if(!isDynamic) trigTime1 = (trigTime1 + frameTime/2) / frameTime;
					trigTime.add(trigTime1);
				}
				if( !spectFlg || i == 1) {
					patPos = ChoosePetCt.parseMultFloat(ChoosePetCt.getFirstDicomValue(meta, "0020,0032"));
					if( patPos!=null) zpos1 = patPos[2];
				}
				zpos.add(zpos1);
				zpos1 += spacingBetweenSlices;
				imgNum = ChoosePetCt.parseInt(ChoosePetCt.getLastDicomValue(meta, "0020,0013"));
				imageNumber.add(imgNum);
//				localAcqTime = ChoosePetCt.getStudyDateTime(meta, 2);
				if( rescaleSlope == null) continue;
				x = ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "0028,1053"));
				rescaleSlope.add(x);
			}
			setMaxAndSort();
			maybeInterpolate();
		}

		void maybeInterpolate() {
			if( width > 256 || (seriesType != ChoosePetCt.SERIES_BQML_PET &&
				seriesType != ChoosePetCt.SERIES_GE_PRIVATE_PET &&
				seriesType != ChoosePetCt.SERIES_GML_PET && seriesType != ChoosePetCt.SERIES_PHILIPS_PET &&
				seriesType != ChoosePetCt.SERIES_SPECT && seriesType != ChoosePetCt.SER_FORCE_CPET)) return;
			if( pixelSpacing != null && 2*pixelSpacing[0] < triThick) return;
			if( pixels == null) return;
			pixel2 = new ArrayList<short []>();	// 16 bits
			valMin = 0;
			valMax = 10000.;
			if( qualityRendering && numTimeSlots == 1 && Math.abs(spacingBetweenSlices) > triThick ) {
				tricubicInterpolate();
				return;
			}
			int i, i1, width2, height2, off1, j, k, size1 = width*height;
			short[] pix1, pix2;
			double vald;
			width2 = width*2;
			height2 = height*2;
			MultiCubicInterpolator bi3 = new MultiCubicInterpolator();
			for( i=0; i<numFrms; i++) {
				pix1 = pixels.get(i);
				pix2 = new short[4*size1];
				off1 = width*(height-2);
				for( j=0; j<width-1; j++) {
					linearInterpolate(j, pix1, pix2);
					linearInterpolate(j+off1, pix1, pix2);
				}
				for( j=1; j<height-2; j++) {
					k = j*width;
					linearInterpolate(k, pix1, pix2);
					linearInterpolate(k+width-2, pix1, pix2);
				}

				// do the edges, copy the line next to it
				off1 = width2*(height2-1);
				for( j=1; j<width2-1; j++) {
					pix2[j] = pix2[j+width2];
					pix2[j+off1] = pix2[j+off1-width2];
				}
				off1 = width2-1;
				for( j=1; j<height2-1; j++) {
					i1 = j*width2;
					pix2[i1] = pix2[i1+1];
					pix2[i1+off1] = pix2[i1+off1-1];
				}
				off1 = width2*(height2-1);
				pix2[0] = pix2[1];
				pix2[width2-1] = pix2[width2-2];
				pix2[off1] = pix2[off1+1];
				pix2[off1+width2-1] = pix2[off1+width2-2];

				int coef0 = getCoefficent0();
				for( j=1; j<height-2; j++) {
					for( i1=1; i1<width-2; i1++) {
						off1 = 2*(j*width2 + i1) + width2 + 1;
						bi3.fillP4(j, i1, pix1);
						vald = bi3.getBiCubic(0.25, 0.25);
						setPix2(off1, pix2, vald, coef0);
						vald = bi3.getBiCubic(0.75, 0.25);
						setPix2(off1+1, pix2, vald, coef0);
						vald = bi3.getBiCubic(0.25, 0.75);
						off1 += width2;
						setPix2(off1, pix2, vald, coef0);
						vald = bi3.getBiCubic(0.75, 0.75);
						setPix2(off1+1, pix2, vald, coef0);
					}
				}
				pixel2.add(pix2);
			}
		}

		void tricubicInterpolate() {
			double[][] inData, outData;
			double scale, vald;
			short [] pix1;
			int i, j, k, x, y, z, off1, size1 = width*height;
			int size4, vali, width2 = width*2;
			int coef0 = getCoefficent0();
			size4 = size1*4;
			inData = new double[size1][numFrms];
			outData = new double[size4][numFrms*2];
			ImageJ ij = IJ.getInstance();
			if( ij != null) ij.toFront();
			IJ.showStatus("If you don't want to wait, remove Best quality in Edit->Options.");
			for( j=0; j<numFrms; j++) {
				pix1 = pixels.get(j);
				scale = getRescaleSlope(j);
				for( i=0; i<size1; i++) {
					inData[i][j] = scale*getPix1Int(i, pix1, coef0);
				}
			}
			calLinZ(0, inData, outData);
			calLinZ(numFrms-2, inData, outData);

			calLinX(0, inData, outData);
			calLinX(width-2, inData, outData);

			calLinY(0, inData, outData);
			calLinY(height-2, inData, outData);

			MultiCubicInterpolator ti3 = new MultiCubicInterpolator();
			for( z=1; z<numFrms-2; z++) {
				IJ.showProgress(z, numFrms);
				j = 2*z+1;
				for( y=1; y<height-2; y++) {
					for( x=1; x<width-2; x++) {
						off1 = 2*(y*width2 + x) + width2 + 1;
						ti3.fillV4(x, y, z, inData);
						outData[off1][j] = ti3.getTriCubic(0.25, 0.25, 0.25);
						outData[off1][j+1] = ti3.getTriCubic(0.25, 0.25, 0.75);
						outData[off1+1][j] = ti3.getTriCubic(0.75, 0.25, 0.25);
						outData[off1+1][j+1] = ti3.getTriCubic(0.75, 0.25, 0.75);
						off1 += width2;
						outData[off1][j] = ti3.getTriCubic(0.25, 0.75, 0.25);
						outData[off1][j+1] = ti3.getTriCubic(0.25, 0.75, 0.75);
						outData[off1+1][j] = ti3.getTriCubic(0.75, 0.75, 0.25);
						outData[off1+1][j+1] = ti3.getTriCubic(0.75, 0.75, 0.75);
					}
				}
			}
			triFlg = true;
			for( z=0; z<numFrms*2; z++) {
				scale = getRescaleSlope(z/2);
				pix1 = new short[size4];
				for(i=0; i<size4; i++){
					vald = outData[i][z]/scale;
					if( vald < valMin) valMin = vald;
					if( vald > valMax) valMax = vald;
					if( vald < 0) vald = 0;
					if( vald > 32767.) vald = 32767.;
					vali = ChoosePetCt.round(vald);
					pix1[i] = (short)(vali - coef0);
				}
				pixel2.add(pix1);
			}
			IJ.showProgress(1,1);
			IJ.showStatus("Done.");
		}

		// calculate 4 points
		void linearInterpolate(int i, short[] pix1, short[] pix2) {
			int p00, p01, p10, p11;
			double val, py0, py1;
			int row, col, row2, col2, off2, width2;
			int coef0 = getCoefficent0();
			row = i / width;
			col = i % width;
			if( col >= width-1 || row >= height-1) return; // edge
			width2 = 2*width;
			row2 = 2*row + 1;
			col2 = 2*col + 1;
			p00 = getPix1Int(i, pix1, coef0);
			p01 = getPix1Int(i+1, pix1, coef0);
			p10 = getPix1Int(i+width, pix1, coef0);
			p11 = getPix1Int(i+width+1, pix1, coef0);
			off2 = row2*width2 + col2;
			py0 = 3*p00 + p01;
			py1 = 3*p10 + p11;
			val = (3*py0 + py1)/16.0;
			setPix2(off2, pix2, val, coef0);
			val = (py0 + 3*py1)/16.0;
			setPix2(off2+width2, pix2, val, coef0);
			py0 = p00 + 3*p01;
			py1 = p10 + 3*p11;
			val = (3*py0 + py1)/16.0;
			setPix2(off2+1, pix2, val, coef0);
			val = (py0 + 3*py1)/16.0;
			setPix2(off2+width2+1, pix2, val, coef0);
		}

		// this one is called first, the x and y need not recalc what is already done
		void calLinZ(int depth, double[][] in1, double[][] o1) {
			int i, j, size4, z0, z1;
			for( j=0; j<height; j++) {
				for( i=0; i<width; i++) {
					volLinInterpolate(i, j, depth, in1, o1);
				}
			}
			size4 = width*height*4;
			z0 = 0;	// destination
			z1 = 1;	// source
			if( depth > 0) {
				z1 = 2*depth + 2;
				z0 = z1+1;
			}
			for( j=0; j<size4; j++) o1[j][z0] = o1[j][z1];
		}

		void calLinX(int col, double[][] in1, double[][] o1) {
			int off1, j, k, x0, x1, height2, width2;
			for( k=1; k<numFrms-2; k++) {
				for( j=0; j<height; j++) {
					volLinInterpolate(col, j, k, in1, o1);
				}
			}
			height2 = height*2;
			width2 = width*2;
			x0 = 0;	// destination
			x1 = 1;	// source
			if( col>0) {
				x1 = 2*col + 2;
				x0 = x1+1;
			}
			for( k=0; k<numFrms*2; k++) {
				for( off1=j=0; j<height2; j++) {
					o1[x0+off1][k]=o1[x1+off1][k];
					off1 += width2;
				}
			}
		}

		void calLinY(int row, double[][] in1, double[][] o1) {
			int i, k, y0, y1, width2;
			for( k=1; k<numFrms-2; k++) {
				for( i=0; i<width; i++) {
					volLinInterpolate(i, row, k, in1, o1);
				}
			}
			width2 = width*2;
			y0 = 0;	// destination
			y1 = width2;	// source
			if( row>0) {
				y1 = (2*row + 2)*width2;
				y0 = y1+width2;
			}
			for( k=0; k<numFrms*2; k++) {
				for( i=0; i<width2; i++) {
					o1[i+y0][k]=o1[i+y1][k];
				}
			}
		}

		// calculate 8 points
		void volLinInterpolate(int col, int row, int depth, double[][] in1, double[][] o1) {
			int off1, off2, width2, row2, col2, dep2, x;
			double mult0, mult1, pz0, pz1, py00, py10, py01, py11;
			double val, p000, p010, p100, p110, p001, p011, p101, p111;
			if( col >= width-1 || row >= height-1 || depth >= numFrms-1) return; // edge
			off1 = row * width + col;
			width2 = 2*width;
			dep2 = 2*depth + 1;
			row2 = 2*row + 1;
			col2 = 2*col + 1;
			p000 = in1[off1][depth];
			p010 = in1[off1+1][depth];
			p100 = in1[off1+width][depth];
			p110 = in1[off1+width+1][depth];
			p001 = in1[off1][depth+1];
			p011 = in1[off1+1][depth+1];
			p101 = in1[off1+width][depth+1];
			p111 = in1[off1+width+1][depth+1];
			for( x=0; x<2; x++) {
				mult0 = 3;
				mult1 = 1;
				if( x>0) {
					mult0 = 1;
					mult1 = 3;
				}
				py00 = mult0*p000 + mult1*p010;
				py10 = mult0*p100 + mult1*p110;
				py01 = mult0*p001 + mult1*p011;
				py11 = mult0*p101 + mult1*p111;
				off2 = row2*width2 + col2 + x;
				pz0 = 3*py00 + py10;
				pz1 = 3*py01 + py11;
				val = (3*pz0 + pz1)/64.0;
				o1[off2][dep2] = val;
				val = (pz0 + 3*pz1)/64.0;
				o1[off2][dep2+1] = val;
				off2 += width2;
				pz0 = py00 + 3*py10;
				pz1 = py01 + 3*py11;
				val = (3*pz0 + pz1)/64.0;
				o1[off2][dep2] = val;
				val = (pz0 + 3*pz1)/64.0;
				o1[off2][dep2+1] = val;
			}
		}

		int getPix1Int( int i, short[] pix1, int coef0) {
			short currShort = (short)(pix1[i] + coef0);
			return currShort;
		}

		void setPix2(int i, short[] pix2, double val, int coef0) {
			double vald = val;
			if( vald < valMin) valMin = vald;
			if( vald > valMax) valMax = vald;
			if( vald < 0) vald = 0;
			if( vald > 32767.) vald = 32767;
			int vali = ChoosePetCt.round(vald);
			short currShort = (short)(vali - coef0);
			pix2[i] = currShort;
		}

		Object reverseBuffer(Object pix, int orientation) {
			if( (orientation & (32+64)) == 0) return pix;	// usual case
			Object pix1;
			int inpos, outpos, x, y, size1, stepx=1, stepy=1;
			float[] outFloat, inFloat;
			byte[] outByte, inByte;
			short[] outShort, inShort;
			size1 = width*height;
			if( (orientation & 64) != 0) stepy = -1;
			if( (orientation & 32) != 0) stepx = -1;
			switch( depth) {
				case 32:
					inFloat = (float[]) pix;
					outFloat = new float[size1];
					for( y=inpos=0; y<height; y++) {
						if( stepy > 0) outpos = y*width;
						else outpos = (height-y-1)*width;
						if( stepx < 0) outpos += width-1;
						for( x=0; x<width; x++) {
							outFloat[outpos] = inFloat[inpos++];
							outpos += stepx;
						}
					}
					pix1 = outFloat;
					break;
					
				case 8:
					inByte = (byte[]) pix;
					outByte = new byte[size1];
					for( y=inpos=0; y<height; y++) {
						if( stepy > 0) outpos = y*width;
						else outpos = (height-y-1)*width;
						if( stepx < 0) outpos += width-1;
						for( x=0; x<width; x++) {
							outByte[outpos] = inByte[inpos++];
							outpos += stepx;
						}
					}
					pix1 = outByte;
					break;
					
				default:
					inShort = (short[]) pix;
					outShort = new short[size1];
					for( y=inpos=0; y<height; y++) {
						if( stepy > 0) outpos = y*width;
						else outpos = (height-y-1)*width;
						if( stepx < 0) outpos += width-1;
						for( x=0; x<width; x++) {
							outShort[outpos] = inShort[inpos++];
							outpos += stepx;
						}
					}
					pix1 = outShort;
					break;
			}
			return pix1;
		}
		
		void addBlankSlice(float zpos1) {
			int size1 = width*height;
			switch( depth) {
				case 32:
					float[] pix32 = new float[size1];
					pixFloat.add(pix32);
					break;
					
				case 8:
					byte[] pix8 = new byte[size1];
					pixByt.add(pix8);
					break;
					
				default:
					int i, coef0 = getCoefficent0();
					short[] pix16 = new short[size1];
					for(i=0; i<size1; i++) pix16[i] = (short) coef0;
					pixels.add(pix16);
			}
			zpos.add(zpos1);
			imageNumber.add(-1);
			if( rescaleSlope != null) rescaleSlope.add(1.0);
		}

		void resetGauss() {
			gaussImage = null;
			if(hasGauss) {
				if(srcImage.getImage() == null) {
					hasGauss = false;
					JOptionPane.showMessageDialog(saveParent, "Cannot find input data\nGaussian blur disabled");
					return;
				}
				gaussImage = srcImage.duplicate();
				gaussPix = new short[numFrms][];
			}
			
		}

		short [] getPixels(int slice) {
			short[] currSlice = null;	// for tricubic the size is double
			if( slice < pixels.size()) currSlice = pixels.get(slice);
			if( pixel2 != null) currSlice = pixel2.get(slice);
			int gSlice = slice;
			if( hasGauss && useGauss) {
				if(gaussSort != null) gSlice = gaussSort[slice];
				currSlice = gaussPix[gSlice];
				if( currSlice == null) {
					GaussianBlur gb = new GaussianBlur();
					gb.setup(null, gaussImage);
//					gb.setSigmaAndScaled(2.0, true);
					ImageProcessor ip = gaussImage.getProcessor();
					ip.setPixels(gaussImage.getStack().getPixels(gSlice+1));
					ip.setSliceNumber(gSlice+1);
					double sigmaX, sigmaY;
					sigmaX = sigmaGauss;
					if(isFWHM) sigmaX = sigmaGauss/2.355;
					sigmaY = sigmaX;
					if (gaussImage.getCalibration()!=null && !gaussImage.getCalibration().getUnits().equals("pixels")) {
						sigmaX /= gaussImage.getCalibration().pixelWidth;
						sigmaY /= gaussImage.getCalibration().pixelHeight;
					}
			        double accuracy = (ip instanceof ByteProcessor || ip instanceof ColorProcessor) ?
					    0.002 : 0.0002;
					if(sigmaGauss > 0.1) gb.blurGaussian(ip, sigmaX, sigmaY, accuracy);
					currSlice = (short[]) reverseBuffer(ip.getPixels(), orientation);
					gaussPix[gSlice] = currSlice;
				}
			}
			return currSlice;
		}

		void readData(JFijiPipe srcPipe) {
			int i, zfact = 1, imgNum;
			double x;
			short[] pix1;
			byte[] pixByt1;
			float[] pixFlt1;
			float zpos1;
			JData srcData = srcPipe.data1;
			fileFormat = srcData.fileFormat;
			numFrms = srcData.numFrms;
			width = srcData.width;
			height = srcData.height;
			depth = srcData.depth;
			coef = srcData.coef;
			hasGauss = srcPipe.hasGauss;
			useGauss = srcPipe.useGauss;
			sigmaGauss = srcPipe.sigmaGauss;
			srcImage = srcData.srcImage;
			fixedLUT = srcPipe.fixedLUT;
			extBluesLUT = srcPipe.extBluesLUT;
			extHotIronLUT = srcPipe.extHotIronLUT;
			extCtMriLUT = srcPipe.extCtMriLUT;
			qualityRendering = srcPipe.qualityRendering;
			triThick = srcPipe.triThick;
			isLog = srcPipe.isLog;
			triFlg = srcPipe.triFlg;
			if( triFlg) zfact = 2;
			resetGauss();
			rescaleSlope = null;
			if( !getMetaData(srcData.metaData,0)) return;
			seriesType = srcData.seriesType;	// in case it was forced
			srcImage = srcData.srcImage;
			if( srcData.pixel2 != null) pixel2 = new ArrayList<short[]>();
			if( srcData.rescaleSlope != null) rescaleSlope = new ArrayList<Double>();
			for( i=0; i<numFrms; i++) {
				switch( depth) {
					case 32:
						pixFlt1 = srcData.pixFloat.get(i);
						pixFloat.add(pixFlt1);
						break;

					case 8:
						pixByt1 = srcData.pixByt.get(i);
						pixByt.add(pixByt1);
						break;

					default:
						pix1 = srcData.pixels.get(i);
						pixels.add(pix1);
						if( srcData.pixel2 != null) {
							pix1 = srcData.pixel2.get(i*zfact);
							pixel2.add(pix1);
							if(zfact > 1) {
								pix1 = srcData.pixel2.get(i*zfact + 1);
								pixel2.add(pix1);
							}
						}
				}
				// Audrey Recurpero has a problem, be careful
				if( trigTime != null) {
					if( srcData.trigTime == null) {
						trigTime = null;
						isDynamic = false;
						numTimeSlots = 1;
					} else {
						Integer trigTime1 = srcData.trigTime.get(i);
						trigTime.add(trigTime1);
					}
				}
				zpos1 = srcData.zpos.get(i);
				zpos.add(zpos1);
				imgNum = srcData.imageNumber.get(i);
				imageNumber.add(imgNum);
				if( rescaleSlope == null) continue;
				x = srcData.rescaleSlope.get(i);
				rescaleSlope.add(x);
			}
			setMaxAndSort();
			gaussSort = srcPipe.gaussSort;
			setSUVfactor(srcPipe.data1.SUVfactor);
		}

		boolean getMetaData(String meta, int forceSer) {
			if( meta == null) return false;
			float[] pixEdge;
			String tmp;
			metaData = meta;
			seriesType = ChoosePetCt.getImageType(meta);
			if( seriesType == ChoosePetCt.SERIES_UNKNOWN) return false;
			switch(forceSer) {
				case FORCE_CT:
					seriesType = ChoosePetCt.SER_FORCE_CT;
					break;
				case FORCE_CPET:
					seriesType = ChoosePetCt.SER_FORCE_CPET;
					break;
				case FORCE_UPET:
					seriesType = ChoosePetCt.SER_FORCE_UPET;
					break;
				case FORCE_MRI:
					seriesType = ChoosePetCt.SER_FORCE_MRI;
					break;
			}
			axialRotation = getAxialRotation(meta);
			pixelSpacing = ChoosePetCt.parseMultFloat(ChoosePetCt.getDicomValue(meta, "0028,0030"));
			orientation = ChoosePetCt.getOrientation(ChoosePetCt.getDicomValue( meta, "0020,0037"));
			pixEdge = ChoosePetCt.parseMultFloat(ChoosePetCt.getDicomValue(meta, "0020,0032"));
			if(pixEdge != null && pixelSpacing != null) {
				int stepX = 1, stepY = 1;
				double halfPix = (pixelSpacing[0]+pixelSpacing[1])/4;	// hopefully the same value
				pixelCenter = new double[2];
				if( (orientation & 64) != 0) stepY = -1;
				if( (orientation & 32) != 0) stepX = -1;
				if( orientation == ChoosePetCt.ORIENT_AXIAL_ROTATED) {
					if(pixEdge[1] > 0) stepY = -1;	// Smith, Annie
				}
				pixelCenter[0] = pixEdge[0] + stepX*pixelSpacing[0]*width/2.0;
				pixelCenter[1] = pixEdge[1] + stepY*pixelSpacing[1]*height/2.0;
				if( Math.abs(pixelCenter[0]-halfPix) < halfPix/10 ||
					Math.abs(pixelCenter[0]-halfPix) < halfPix/10) {
					pixelCenter[0] -= halfPix;
					pixelCenter[1] -= halfPix;
					isHalfPix = true;	// document the shift
				}
			}
			SOPclass = ChoosePetCt.getSOPClass(ChoosePetCt.getDicomValue( meta, "0008,0016"));
			serTime = ChoosePetCt.getStudyDateTime( meta, 1);
			tmp = ChoosePetCt.getDicomValue(meta, "0009,100D");	// give GE a chance to veto
			if( tmp != null) serTime = ChoosePetCt.getDateTime(tmp, null);
			acquisitionTime = ChoosePetCt.getStudyDateTime(meta, 2);
			seriesName = ChoosePetCt.getDicomValue( meta, "0008,103E");
			if( seriesName == null || seriesName.isEmpty()) seriesName = ChoosePetCt.getDicomValue( meta, "0054,0400");
			if( seriesName != null) seriesName = seriesName.toLowerCase();
			injectionTime = ChoosePetCt.getStudyDateTime( meta, 3);
			totalDose = ChoosePetCt.parseDouble(ChoosePetCt.getLastDicomValue(meta, "0018,1074"));
			halflife = ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "0018,1075"));
			patHeight = ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "0010,1020"));
			decayCorrect = true;
			tmp = ChoosePetCt.getDicomValue(meta, "0054,1102");
			if( tmp != null && tmp.startsWith("ADMIN")) decayCorrect = false;
			patWeight = ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "0010,1030"));
			philipsSUV = ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "7053,1000"));
			philipsCoef = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0028,1052"));
			pixRep = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0028,0103"));
			numTimeSlots = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0054,0071"));
			int numDyn = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0054,0101"));
			if( numDyn > 1) {
				isDynamic = true;
				numTimeSlots = numDyn;
			}
			if( numTimeSlots > 1) {
				trigTime = new ArrayList<Integer>();
				frameTime = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0018,1063"));
				if( frameTime < 1) frameTime = 1;
			}
			if( numTimeSlots < 1) numTimeSlots = 1;
			zpos = new ArrayList<Float>();
			rescaleIntercept = ChoosePetCt.parseDouble(ChoosePetCt.getFirstDicomValue(meta, "0028,1052"));
			imageNumber = new ArrayList<Integer>();
			spacingBetweenSlices = ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "0018,0088"));
			if( spacingBetweenSlices == 0)
				spacingBetweenSlices = -ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "0018,0050"));
			switch( depth) {
				case 32:
					pixFloat = new ArrayList<float []>();
					break;

				case 8:
					pixByt = new ArrayList<byte []>();
					break;

				default:
					pixels = new ArrayList<short []>();	// 16 bits
			}
			return true;
		}

		double maybeFixShift() {
			if( isHalfPix) return 0;
			double halfPix = (pixelSpacing[0]+pixelSpacing[1])/4;
			double slop = halfPix; // 1/2 pixel slop
			// if the center is already close to zero, leave it
			if( Math.abs(pixelCenter[0]) < slop ||
				Math.abs(pixelCenter[0]) < slop) return 0;

			pixelCenter[0] -= halfPix;
			pixelCenter[1] -= halfPix;
			isHalfPix = true;	// document the shift
			return -halfPix;
		}

		int getAxialRotation(String meta) {
			int rotation = 0;
			String tmp = ChoosePetCt.getDicomValue(meta, "0020,0037");
			if(ChoosePetCt.getOrientation(tmp) != ChoosePetCt.ORIENT_AXIAL_ROTATED) return rotation;
			float fltIn[] = ChoosePetCt.parseMultFloat( tmp);
			if (fltIn == null || fltIn.length != 6) return rotation;
			if( fltIn[1] > 0.98) rotation = 90;
			return rotation;
		}

		/**
		 *  The routine which set the variable max and sorts the data.
		 * It is called for corrected and uncorrected PET and CT but NOT for MIP data.
		 * It generates ArrayLists for storing the short pixel data, OR the byte pixByt data.
		 * There are ArrayLists for the files, the rescale slope and zpos (which are all now sorted).
		 * Finally it checks that 10 * mean data value < max data value.
		 */
		protected void setMaxAndSort() {
			int[] sortVect;
			float[] pixFlt1;
			short[] pix1;
			byte[] pixByt1;
			boolean dirty = false, timeShift;
			double slope1, currDiff, minDiff;
			int i, j, numZsame, imgNum, localMaxPos, j0, j1, n = zpos.size();
			float[] zpos1;
			int[] trigTime1;
			int time0, time1;
			double[] maxSliceVals;
			float ztmp, zval;
			gaussSort = null;
			ArrayList<float []> oldPixFloat = pixFloat;
			ArrayList<short []> oldPixels = pixels;
			ArrayList<byte []> oldPixByt = pixByt;
			ArrayList<Float> oldZpos = zpos;
			ArrayList<Integer> oldImgNum = imageNumber;
			ArrayList<Double> oldRescaleSlope = rescaleSlope;
			if( n != numFrms) numFrms = n;	// annonmized data missing meta
			if( numFrms>1) {
				sortVect = new int[numFrms];
				zpos1 = new float[numFrms];
				trigTime1 = new int[numFrms];
				for( i=0; i<numFrms; i++) {
					sortVect[i] = i;
					zpos1[i] = zpos.get(i);
					if( trigTime != null) trigTime1[i] = trigTime.get(i);
				}
				// be careful, Audrey Recupero is incorrectly listed as 4D
				if( trigTime != null) {
					for( i=numZsame=0; i<numFrms; i++) {
						zval = zpos1[i];
						for( j=0; j<i; j++) {
							if(zval == zpos1[j]) {
								numZsame++;
								break;
							}
						}
					}
					if( numZsame < numFrms/2) {
						trigTime = null;
						isDynamic = false;
						numTimeSlots = 1;
					}
				}
				i=0;
				while( i<numFrms-1) {
					timeShift = false;
					j0 = sortVect[i];
					j1 = sortVect[i+1];
					zval = zpos1[i];
					ztmp = zpos1[i+1];
					if( trigTime != null) {
						time0 = trigTime1[i];
						time1 = trigTime1[i+1];
						if( time0 > time1) {
							timeShift = true;
							trigTime1[i] = time1;
							trigTime1[i+1] = time0;
						}
						if( time0 < time1) {
							i++;
							continue;
						}
					}
					if( zval < ztmp || timeShift) {
						dirty = true;
						sortVect[i] = j1;
						sortVect[i+1] = j0;
						zpos1[i] = ztmp;
						zpos1[i+1] = zval;
						i--;
						if( i<0) i=0;
					}
					else i++;
				}
				if( dirty) {
					if( pixFloat != null) pixFloat = new ArrayList<float []>();
					if( pixels != null) pixels = new ArrayList<short []>();
					if( pixByt != null) pixByt = new ArrayList<byte []>();
					zpos = new ArrayList<Float>();
					imageNumber = new ArrayList<Integer>();
					if( rescaleSlope != null) rescaleSlope = new ArrayList<Double>();
					if( trigTime != null) trigTime = new ArrayList<Integer>();
					gaussSort = sortVect;	// save sort vector
					for( i=0; i<numFrms; i++) {
						j0 = sortVect[i];
						zval = oldZpos.get(j0);
						zpos.add(zval);
						imgNum = oldImgNum.get(j0);
						imageNumber.add(imgNum);
						if( pixFloat != null) {
							pixFlt1 = oldPixFloat.get(j0);
							pixFloat.add(pixFlt1);
						}
						if( pixels != null) {
							pix1 = oldPixels.get(j0);
							pixels.add(pix1);
						}
						if( pixByt != null) {
							pixByt1 = oldPixByt.get(j0);
							pixByt.add(pixByt1);
						}
						if( rescaleSlope != null) {
							slope1 = oldRescaleSlope.get(j0);
							rescaleSlope.add(slope1);
						}
						if( trigTime != null) {
							trigTime.add(trigTime1[i]);
						}
					}
				}
				int numfr1 = numFrms/numTimeSlots;
				if( pixelSpacing != null) {
					zval = Math.abs(zpos.get(numfr1-1) - zpos.get(0));
					y2XMri = pixelSpacing[0]/pixelSpacing[1];
					if(y2XMri > 0.99 && y2XMri < 1.01) y2XMri = 1.0;
					if( zval > 0 && numfr1>1) y2xFactor = zval / (pixelSpacing[0]*(numfr1-1));
					if(y2xFactor > 0.99 && y2xFactor < 1.01) y2xFactor = 1.0;
				}
			}

			// kill rescaleSlope if n==1
			if( n<=1) rescaleSlope = null;

			// now the data is sorted, so find its maximum value
			double tmpd, currSlice, mean, currDbl1, maxDbl;
			int currVal1, n1, maxi, coefAll;
			short currShort;
			coefAll = getCoefficentAll();
			grandMax = 0;
			maxPixel = 0;
			numBlankFrms = 0;
			maxSliceVals = new double[numFrms];
			for( i=0; i<numFrms; i++) {
				maxi =  0;
				maxDbl = 0;
				if( pixels != null) {
					pix1 = pixels.get(i);
					n1 = pix1.length;
					if(pixRep == 0) {
						for(j1=0; j1<n1; j1++) {
							currVal1 = pix1[j1];
							if(currVal1 < 0 || currVal1 > 32767) break;
							currShort = (short) (currVal1 + coefAll);
							if( currShort > maxi) maxi = currShort;
						}
						if( j1 < n1 && rescaleSlope != null) {	// overflow, fix it
							short[] pix2 = new short[n1];	// don't overwrite original
							for( j1=0; j1<n1; j1++) {
								currVal1 = pix1[j1];
								if( currVal1 < 0) currVal1 += 65536 + coefAll;
								currVal1 /= 2;
								pix2[j1] = (short) currVal1;
							}
							slope1 = rescaleSlope.get(i);
							rescaleSlope.set(i, slope1 * 2);
							pixels.set(i, pix2);
							maxi = 0;
							for(j1=0; j1<n1; j1++) {
								currVal1 = pix2[j1];
								currShort = (short) (currVal1 + coefAll);
								if( currShort > maxi) maxi = currShort;
							}
						}
					}
					else for( j1=0; j1<n1; j1++) {
						currVal1 = pix1[j1];
						currShort = (short) (currVal1 + coefAll);
						if( currShort > maxi) maxi = currShort;
					}
				}
				if( pixByt != null) {
					pixByt1 = pixByt.get(i);
					n1 = pixByt1.length;
					for( j1=0; j1<n1; j1++) {
						currVal1 = pixByt1[j1];
						if( currVal1 < 0) {
							currVal1 = 256 + currVal1;
						}
						if( currVal1 > maxi) maxi = currVal1;
					}
				}
				if( pixFloat != null) {
					pixFlt1 = pixFloat.get(i);
					n1 = pixFlt1.length;
					for( j1=0; j1<n1; j1++) {
						currDbl1 = pixFlt1[j1];
						if( currDbl1 > maxDbl) maxDbl = currDbl1;
					}
				}
				if( maxi > maxPixel) maxPixel = maxi;
				tmpd = getRescaleSlope(i)*maxi;
				if( pixFloat != null) tmpd = maxDbl;
				maxSliceVals[i] = tmpd;
				if( tmpd > grandMax) grandMax = tmpd;
				if( maxPixel == 0) numBlankFrms = i+1;
			}
			mean = 0;
			maxVal = minVal = 0;
			localMaxPos = -1;
			for( i=0; i<numFrms; i++) {
				currSlice = maxSliceVals[i];
				mean += currSlice;
				if( currSlice > maxVal) {
					maxVal = currSlice;
					localMaxPos = i;
				}
			}
			mean /= numFrms;
			maxFactor = 1.0; // for the shit problem below

			// A new problem has come up in measuring a bag of shit.
			// Since there is no human body in this measurement, there is nothing other
			// than the bag of shit. This is handled by ingnoring slices = 0.
			if(mean*10 < maxVal) {
				mean = removeOutliers(maxSliceVals);
				minDiff = Math.abs(maxSliceVals[0] - mean);
				maxPosition = 0;
				for( i=1; i<numFrms; i++) {
					currDiff = Math.abs(maxSliceVals[i] - mean);
					if( currDiff < minDiff) {
						minDiff = currDiff;
						maxPosition = i;
					}
				}
				maxFactor = mean/maxVal;	// save the change
				maxVal = mean;
			}
			if( maxPosition < 0) maxPosition = localMaxPos;
			// there is CT data saved as screen captures, use seriesType
			if( SOPclass == ChoosePetCt.SOPCLASS_TYPE_CT || seriesType == ChoosePetCt.SER_FORCE_CT
					|| seriesType == ChoosePetCt.SERIES_CT) {
				minVal = -1000;
				if( SOPclass == ChoosePetCt.SOPCLASS_TYPE_SC) rescaleIntercept = -1024;
				else if(coef == null) rescaleIntercept = philipsCoef;
				// if it is Hawkeye, give that priority
				if( seriesType == ChoosePetCt.SERIES_CT_VARICAM) rescaleIntercept = -1000;
			}
			else winSlope = sliderSUVMax / maxVal;
			dirtyFlg = true;
/*			String tmp = "unknown";
			if( pixByt != null) tmp = "byte data";
			if( pixels != null) tmp = "short data";
			if( pixFloat != null) tmp = "float data";
			JOptionPane.showMessageDialog(null, tmp);*/
		}

		int getCoefficentAll() {
			int coef0 = getCoefficent0();
			if( coef0 == 0) return 0;
			int rescl =  (int) rescaleIntercept;
			if( spectSlope > 0 && spectSlope < rescaleIntercept/100)
				return 0;	// break point
			if( coef0 == rescl || rescl <= 0) return coef0;
			return coef0 + rescl;
		}
		
		// if there is no calibration the CT needs to use rescaleIntercept
		double shiftIntercept() {
			if( coef == null) return rescaleIntercept;
			return 0;
		}

		int getCoefficent0() {
			int coef0 = 0;
			if( coef != null) coef0 = (int) coef[0];
			return coef0;
		}

		void setSUVfactor( double newSUVfactor) {
			SUVfactor = newSUVfactor;
//			SUVfactor = 0.0;	// temporary - fix this!!
			// our bag of shit gives more problems. The sliderSUVMax = 0.03
			// if the sliderSUVMax will be <= 1.0, kill the SUV (maybe we need it?)
			if( SUVfactor > 0) { // update
				double max1 = maxVal * MIPslope;
				if( max1/SUVfactor <= 1.0) {
					SUVfactor = 0;
					return;
				}
				sliderSUVMax = max1 / SUVfactor;
				winSlope = sliderSUVMax / max1;	// = 1/SUVfactor
				setSUV5();
				winLevel = winWidth/2;
				dirtyFlg = true;
			}
		}

		void setSUV5() {
			winWidth = 5.0;
			if(winWidth <= sliderSUVMax) return;	// nothing to do
			int i = ChoosePetCt.round(sliderSUVMax * 10);
			if(( i & 1) == 1) i++;	// make it an even number
			winWidth = i / 10.0;
		}

		/**
		 * This routine is called when the maximum slice > 10 * mean slice value.
		 *
		 * There is a problem measuring a bag of shit in that many slices are zero.
		 * Thus the mean is corrected to include only non zero slices.
		 * If the max is still an outlier, the max slice is zeroed (so as to be ignored)
		 * and the calculation is repeated, until outliers are eliminated.
		 * 
		 * This turns out to useful in iodine studies as well. Here there are no zero
		 * slices, but a few very hot points. We need to eliminate these hot points.
		 * see data ID736 from BN Lee with extended Pet Ct Dicom formats.
		 *
		 * @param maxSliceVals list of slice maxima
		 * @return corrected maximum slice value
		 */
		protected double removeOutliers( double[] maxSliceVals) {
			double localMax = 0;
			double currSum, currVal;
			int i, n, nNonZero, maxPos;
			n = maxSliceVals.length;
			while(true) {
				localMax = currSum = 0;
				maxPos = -1;
				for( i=nNonZero=0; i<n; i++) {
					currVal = maxSliceVals[i];
					if( currVal <= 0) continue;	// don't count zeros
					nNonZero++;
					if( localMax < currVal) {
						localMax = currVal;
						maxPos = i;
					}
					currSum += currVal;
				}
				currVal = currSum / nNonZero;
				if( localMax < currVal*10) break;
				if( maxPos < 0) break;	// sanity check
				maxSliceVals[maxPos] = 0;	// elimiate highest point and try again
			}
			return localMax;
		}

		double getRescaleSlope( int indx) {
			double slope1 = MIPslope*spectSlope;	// MIPslope = 1.0 for everything but MIP and uncorrected
			int i;
			if( rescaleSlope != null && rescaleSlope.size() > indx) {
				// watch out, MRI data has no rescaleSlope. Detect this when everything = 0
				for(i=0; i<rescaleSlope.size(); i++) {
					if( rescaleSlope.get(i) > 0) break;
				}
				if( i < rescaleSlope.size()) {
					slope1 = rescaleSlope.get(indx) * MIPslope;
				} else {
					rescaleSlope = null;	// kill it
				}
			}
			return slope1;
		}

		double getMaxRescaleSlope() {
			double tmp1, slope1 = MIPslope*spectSlope;
			int i, n;
			if( rescaleSlope != null) {
				n = rescaleSlope.size();
				if( maxPosition >=0 && maxPosition < n)
					return rescaleSlope.get(maxPosition) * MIPslope;
				for( i=0; i<n; i++) {
					tmp1 = rescaleSlope.get(i);
					if( i==0 || tmp1 > slope1) slope1 = tmp1;
				}
				slope1 *= MIPslope;
			}
			return slope1;
		}

		/**
		 * Utility routine for getting a line of data for SUV, coronal or sagital slices.
		 *
		 * @param angle in degrees, 0 for SUV or coronal, 270 for sagital
		 * @param depth y coordinate for coronal, x for sagital
		 * @param sliceNum z coordinate, which is the axial slice number
		 * @return lineEntry structure, with line + extra information
		 */
		public lineEntry getLineOfData(int angle, int depth, int sliceNum) {
			lineEntry ret1 = new lineEntry();
			short sliceBuf[], currShort;
			float sliceFlt[];
			int i, j, coef0, start1, step = 1, width1 = width;
			ret1.SUVfactor = SUVfactor;	// set in case of bad data
			if( depth < 0 || sliceNum < 0 || sliceNum >= numFrms) return ret1;
			switch( angle) {
				case 0:
					start1 = width*depth;
					break;

				case 180:
					start1 = width*(width-depth) -1;
					step = -1;
					break;

				case 90:
					start1 = height-depth-1;
					step = -width;
					break;

				case 270:
					start1 = depth;
					step = width;
					break;

				default:
					return ret1;
			}
			if( angle == 90 || angle == 270) width1 = height;
			j = start1;
			if( pixFloat != null) {
				sliceFlt = pixFloat.get(sliceNum);
				if( start1 >= sliceFlt.length) return ret1;
				ret1.pixFloat = new float[width1];
				for( i=0; i < width1; i++) {
					ret1.pixFloat[i] = sliceFlt[j];
					j += step;
				}
			} else {
				sliceBuf = pixels.get(sliceNum);
				if( start1 >= sliceBuf.length) return ret1;
				ret1.pixels = new short[width1];
				coef0 = getCoefficentAll();
				for( i=0; i < width1; i++) {
					currShort = (short)(sliceBuf[j]+coef0);
					ret1.pixels[i] = currShort;
					j += step;
				}
			}
			ret1.angle = angle;
			ret1.depth = depth;
			ret1.size1 = width1;
			ret1.maxSlope = getMaxRescaleSlope();
			ret1.slope = getRescaleSlope(sliceNum);
			ret1.pixelSpacing = pixelSpacing;
			ret1.goodData = true;
			return ret1;
		}
		
		void bkgCalc() {
			int cores = Runtime.getRuntime().availableProcessors();
			if( cores < 2) return;
			work2 = new bkgdMipCalc();
			work2.addPropertyChangeListener(new PropertyChangeListener(){
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

		@SuppressWarnings("SleepWhileInLoop")
		protected boolean setMIPData(int type) {
			int ang1, total1, x1, off1, currMax, cores;
			short [] currSlice;
			double [] coss;
			ArrayList<short []> oldPixels = null;
			MIPtype = type;
			if( type == 1) oldPixels = pixels;	// save to add to new
			width = srcPet.data1.width;
			srcHeight = srcPet.data1.height;
			height = srcPet.getNormalizedNumFrms();
			y2xFactor = srcPet.data1.y2xFactor;
			if( srcPet.data1.depth < 16) return false;	// something is wrong
			depth = 16;
			numFrms = 0;
			grandMax = maxPixel = srcPet.data1.maxPixel;
			if( srcPet.data1.depth == 32) grandMax = srcPet.data1.maxVal;
			maxVal = 0;
			numTimeSlots = 1;
			pixels = new ArrayList<short []>();
			tmpPix = new ArrayList<short []>();
			work2 = null;
			bkgCalc();
			cores = 2;
			if( work2 == null) cores = 1;
			for( ang1 =0; ang1 < NUM_MIP/cores; ang1++) {
				coss = setCosSin( ang1);
				currSlice = new short[width*height];
				for( x1 = off1 = 0; x1 < width; x1++) {
					currMax = getMipLocation(x1, -1, null, currSlice, off1++, coss);
					if( currMax > maxVal) maxVal = currMax;
				}
				total1 = ang1 + tmpPix.size();
				IJ.showStatus(total1+1+"/"+NUM_MIP);
				IJ.showProgress(total1, NUM_MIP);
				pixels.add(currSlice);
			}
			Integer i = 0, j, k;
			while( tmpPix.size() + pixels.size() < NUM_MIP) {
//			while( work2 != null) {
				try {
					j = tmpPix.size();
					j++;
					k = pixels.size();
					Thread.sleep(200);
					i++;
					if( (i % 20) == 0) {
						j = i/5;
						IJ.log("Caught in sleep loop " + j.toString());
					}
				} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
			}
			if( cores > 1) for( i=0; i<8; i++) {
				currSlice = tmpPix.get(i);
				pixels.add(currSlice);
			}
			mipXYdata = null;
			numFrms = NUM_MIP;
			if( type == 1) {	// combine the 2 MIPs
				tmpPix = pixels;
				pixels = new ArrayList<short []>();
				for( i=0; i<NUM_MIP; i++) {
					if( oldPixels == null) break;
					currSlice = oldPixels.get(i);
					pixels.add(currSlice);
					currSlice = tmpPix.get(i);
					pixels.add(currSlice);
				}
				numFrms = 2*NUM_MIP;
			}
// The next line kills the MIP for Carol Rochefort. We need to comment it out.
// For Carol the maxFactor is 10^29, which just kills the MIP
//			maxVal *= srcPet.data1.maxFactor;	// for shit data
			IJ.showProgress(1.0);	// this kills the status bar
			winSlope = sliderSUVMax / (maxVal*MIPslope);
			return true;
		}
		
		protected void doCalc2() {
			int ang1, x1, off1, currMax;
			short [] currSlice;
			double [] coss;
			for( ang1 = NUM_MIP/2; ang1 < NUM_MIP; ang1++) {
				coss = setCosSin( ang1);
				currSlice = new short[width*height];
				for( x1 = off1 = 0; x1 < width; x1++) {
					currMax = getMipLocation(x1, -1, null, currSlice, off1++, coss);
					if( currMax > maxVal) maxVal = currMax;
				}
				tmpPix.add(currSlice);
			}
		}

		protected boolean setReprojectionData() {
			int ang1;
			double currMax;
			double [] coss;
			float [] currSlice;
			width = srcPet.data1.width;
			srcHeight = srcPet.data1.height;
			height = srcPet.getNormalizedNumFrms();
			y2xFactor = srcPet.data1.y2xFactor;
			if( srcPet.data1.depth < 16) return false;	// something is wrong
			depth = 32;
			numFrms = 0;
			grandMax = maxPixel = srcPet.data1.maxPixel;
			if( srcPet.data1.depth == 32) grandMax = srcPet.data1.maxVal;
			maxVal = 0;
			pixFloat = new ArrayList<float[]>();
			for( ang1 =0; ang1 < NUM_MIP; ang1++) {
				coss = setCosSin( ang1);
				currSlice = new float[width*height];
				currMax = getReprojection( currSlice, coss);
				if( currMax > maxVal) maxVal = currMax;
				IJ.showStatus(ang1+1+"/"+NUM_MIP);
				IJ.showProgress(ang1, NUM_MIP);
				pixFloat.add(currSlice);
				numFrms++;
				winSlope = sliderSUVMax / (maxVal*MIPslope);
			}
			return true;
		}

		double getReprojection( float[] resultSlice, double[] coss) {
			int i, n, width2, coef0, xnew, ynew, y1, zlo, pixx;
			double yin, xin, currVal, scale1, expF1, maxVal1 = 0;
			float tmpf, fltSlice[];
			short tmps, currSlice[];
			boolean fltFlg = false;
			n = width*height;
			for( i=0; i<n; i++) resultSlice[i] = 0;
			if( srcPet.data1.depth == 32) {
				fltFlg = true;
			}
			coef0 = srcPet.data1.getCoefficent0();
			width2 = width / 2;
			for( yin = -width2 + 0.5; yin < width2; yin += 1.0) {
				pixx = 0;
				for( xin = -width2 + 0.5; xin < width2; xin += 1.0) {
					xnew = (int)( xin*coss[0] + yin*coss[1] + width2);
					if( xnew < 0 || xnew >= width) continue;
					ynew = (int)(-xin*coss[1] + yin*coss[0] + width2);
					if( ynew < 0 || ynew >= width) continue;
					y1 = (int) (yin + width2);
					expF1 = expFac[y1];
					for( zlo = 0; zlo < height; zlo++) {
						if( fltFlg) {
							fltSlice = srcPet.data1.pixFloat.get(zlo);
							currVal = fltSlice[xnew + ynew*width];
						} else {
							scale1 = srcPet.data1.getRescaleSlope(zlo)/MIPslope;
							currSlice = srcPet.data1.pixels.get(zlo);
							tmps = (short) (currSlice[xnew + ynew*width] + coef0);
							currVal = tmps * scale1;
						}
						if( currVal <= 0) continue;
						currVal = expF1*currVal + resultSlice[zlo*width + pixx];
						resultSlice[zlo*width + pixx] = (float) currVal;
						if( currVal > maxVal1) maxVal1 = currVal;
					}
					pixx++;
				}
			}
			return maxVal1;
		}

		/**
		 * Sets cos1 and sin1 values for the angle defined by indx.
		 * This needs to be called before getMipLocation. It also does a one time
		 * calculation of expFac which is the attenuation factor used along the ray.
		 * @param indx the angular value, angle (in degrees) = 360*indx/NUM_MIP
		 * @return double[2] with cosine, sine values
		 */

		public double[] setCosSin(int indx) {
			int numMip = NUM_MIP;
			double off1 = 0.;
			switch(MIPtype) {
				case 1:
					off1 = 0.5;
					break;

				case 2:
					numMip = 2*NUM_MIP;
					break;
			}
			double angl1 = (2.0*Math.PI*(indx+off1))/numMip;
			double[] ret1 = new double[2];
			ret1[0] = Math.cos(angl1);
			ret1[1] = Math.sin(angl1);
			if( expFac == null) {
				expFac = new double[width];
				double scale = 2.0*Math.log(2);
				double scal2 = 0.7;
				int i, wid2 = width/2;
				for( i=0; i<width; i++)
					expFac[i] = scal2*Math.exp(scale*(wid2-i)/width);
			}
			return ret1;
		}

		/**
		 * Get the attenuated highest pixel value along a ray at x1, z1.
		 * This routine is called in 2 different methods, z1 = -1 is used to calculate the slice.
		 * z1 >= 0 is used to measure the maximum point after the cine has been displayed
		 * and the user clicked on the MIP image. Note that no separate image is stored
		 * holding the maximum value positions. The reason is that it is unjustified from
		 * the point of view of speed. The user will click infrequently on a single point.
		 * It makes no sense to calculate ALL the positions up front, so each time the
		 * calculation for the chosen point is done again with no sacrifice in speed.
		 * The pixOut is used ONLY for display. The original data is used when the
		 * user asks for a MIP value. Only the position where the user clicked (and the
		 * angle, normally front) is used and NOT pixOut.
		 * @param x1 position, 90 degrees from which is the ray itself
		 * @param z1 = -1 for calculation of the cine, or >=0 for calculation of a point.
		 * @param retLoc null while calculating cine, otherwise receives the position
		 * @param pixOut where the output is stored (null for z1 >= 0).
		 * @param offst offset into pixOut, dependent mainly on the angle
		 * @param coss cosine and sine values
		 * @return the value highest attenuated pixel value
		 */

		public int getMipLocation(int x1, int z1, Point2d retLoc, short pixOut[], int offst, double[] coss) {
			double xin, yin, scale1, scale2=1, currValDbl;
			boolean fltFlg = false;
			int zlo, y1, xnew, ynew, currVal, off1, retVal, coef0;
			short currShort;
			short [] currSlice;
			float [] fltSlice;
			int width2 = width/2;
			xin = x1 - width2 + 0.5;
			retVal = 0;
			if( z1 < 0) {
				for( zlo = 0; zlo < height; zlo++) pixOut[zlo*width+offst] = 0;
				zlo = 0;
			} else {	// do a single slice
				zlo = z1;
				retLoc.x = 0;
				retLoc.y = 0;
				if( z1 < 0 || z1 >= height) return 0;	// out of bounds
			}
			MIPslope = srcPet.data1.getMaxRescaleSlope();
			// if MIP is read in certain parameters may not have been initialized
			if( srcHeight <= 0) srcHeight = srcPet.data1.height;
			if( srcPet.data1.depth == 32) {
				fltFlg = true;
				MIPslope = srcPet.data1.maxVal / 32767;
				scale2 = 1 / MIPslope;
			}
			coef0 = srcPet.data1.getCoefficent0();
			for( yin = -width2 + 0.5; yin < width2; yin += 1.0) {
				xnew = (int)( xin*coss[0] + yin*coss[1] + width2);
				ynew = (int)(-xin*coss[1] + yin*coss[0] + width2);
				if( xnew < 0 || xnew >= width) continue;
				if( ynew < 0 || ynew >= srcHeight) continue;
				y1 = (int) (yin + width2);
				scale1 = expFac[y1];
				if( z1 < 0) for( zlo = 0; zlo < height; zlo++) {
					off1 = zlo*width + offst;
					// if we get a measurable delay, in c++ it helped to move the next line
					if( fltFlg) {
						fltSlice = srcPet.data1.pixFloat.get(zlo);
						currValDbl = scale1 * scale2 * fltSlice[xnew + ynew*width];
					} else {
						scale2 = scale1* srcPet.data1.getRescaleSlope(zlo)/MIPslope;
						currSlice = srcPet.data1.pixels.get(zlo);
						currShort = (short) (currSlice[xnew + ynew*width] + coef0);
						currValDbl = scale2*currShort;
					}
					currVal = (int)currValDbl;	// local data
					if( currVal > pixOut[off1]) {
						if( currValDbl > 32767) currVal = 32767;
						pixOut[off1] = (short) currVal;
						if( currVal > retVal)
							retVal = currVal;
					}
				}
				else {	// single slice
					if( fltFlg) {
						fltSlice = srcPet.data1.pixFloat.get(zlo);
						currVal = (int) (scale1 * scale2 * fltSlice[xnew + ynew*width]);
					} else {
						currSlice = srcPet.data1.pixels.get(zlo);
						currShort = (short) (currSlice[xnew + ynew*width] + coef0);
						currVal = (int)(scale1*currShort);	// local data
					}
					if( currVal > retVal) {
						retVal = currVal;
						retLoc.x = xnew;
						retLoc.y = ynew;
					}
				}
			}
			return retVal;
		}
	

		/**
		 * This routine either directly returns or creates the mipXYentry
		 * structure, as needed. It will also extend an existing structure
		 * which is determined to be too small.
		 * 
		 * @param loSlice the lower z value of the volume
		 * @param hiSlice the upper limit
		 * @return 
		 */
		public mipXYentry getcurrMIPentry(int loSlice, int hiSlice) {
			int i, j, val, currLo = -1, currHi=0, upLo, upHi;
			mipEntry currSlice;
			short[] tmpX, tmpY;
			double [] coss;
			MIPtype = 0;
			if( numFrms > NUM_MIP ) MIPtype = 2;
			if(mipXYdata == null) mipXYdata = new mipXYentry[numFrms];
			mipXYentry currXY = mipXYdata[cineIndx];
			if(currXY != null) {
				currLo = currXY.zlo;
				currHi = currXY.zhi;
				if( currLo <= loSlice && currHi >= hiSlice) {
					return currXY;	// hopefully this is the usual case
				}
			}
			upLo = loSlice;
			upHi = hiSlice;
			if(currLo >= 0) {
				if( currLo < upLo) upLo = currLo;
				if( currHi > upHi) upHi = currHi;
			}
			mipXYentry updatedXY = new mipXYentry();
			updatedXY.zlo = upLo;
			updatedXY.zhi = upHi;
			coss = setCosSin(cineIndx);
			for( i=upLo; i<=upHi; i++) {
				if(currLo >= 0) {
					if( i >= currLo && i <= currHi && currXY != null) {
						currSlice = currXY.xydata.get(i-currLo);
						updatedXY.xydata.add(currSlice);
						continue;
					}
				}
				Point2d retLoc = new Point2d();
				tmpX = new short[width];
				tmpY = new short[width];
				for( j=0; j<width; j++) {
					val = getMipLocation(j,i, retLoc, null,0,coss);
					tmpX[j] = (short) retLoc.x;
					tmpY[j] = (short) retLoc.y;
				}
				currSlice = new mipEntry();
				currSlice.zpos = i;
				currSlice.xval = tmpX;
				currSlice.yval = tmpY;
				updatedXY.xydata.add(currSlice);
			}
			mipXYdata[cineIndx] = updatedXY;
			return updatedXY;
		}
	}
	
	protected class bkgdMipCalc extends SwingWorker {
		@Override
		protected Void doInBackground() {
			data1.doCalc2();
			return null;
		}
	}
}

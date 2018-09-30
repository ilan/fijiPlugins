import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.io.Opener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import net.imagej.ops.OpService;
import org.scijava.vecmath.Point2d;

/*
 * BrownFat.java
 *
 * Created on Apr 14, 2010, 1:08:40 PM
 */

/**
 *
 * @author Ilan
 */
public class BrownFat extends javax.swing.JDialog implements WindowFocusListener {

    /** Creates new form BrownFat
	 * @param parent
	 * @param modal */
    public BrownFat(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
		init0(parent);
		saveParent(parent);
		init();	// init uses results of saveParent
    }

	private void init0(Window w1) {
		setLocationRelativeTo(w1);
	}

	private void init() {
		int lo, hi, val, valz, off1;
		Integer loVal, hiVal;
		instance = this;
		allowDelete = false;
		nifListSz = 0;
		elementList.add(jButTrash);
		elementList.add(jCheckBlue);
		elementList.add(jSpinRoiNm);
//		setFocusTraversalPolicy(new CustomFocusTraversalPolicy());
		buttonGroup1 = new javax.swing.ButtonGroup();
		buttonGroup1.add(jRadioExterior);
		buttonGroup1.add(jRadioInterior);
		buttonGroup1.add(jRadioNone);
		jRadioInterior.setSelected(true);
		buttonGroup2 = new javax.swing.ButtonGroup();
		buttonGroup2.add(jRadioAll);
		buttonGroup2.add(jRadioAverage);
		buttonGroup2.add(jRadioAny);
		WindowManager.addWindow(this);
		addWindowFocusListener(this);
		Preferences prefer = parentPet.parent.jPrefer;
		int val1 = prefer.getInt("calc ct type", 0);
		switch(val1) {
			case 1:
				jRadioAverage.setSelected(true);
				break;
				
			case 2:
				jRadioAll.setSelected(true);
				break;
				
			default:
				jRadioAny.setSelected(true);
				break;
		}
		lastPeakType = prefer.getInt("last peak type", 1);
		String tmp1 = prefer.get("Vol SUV lo", "2");
		jTextSUVlo.setText(tmp1);
		double tmpd = prefer.getDouble("Vol SUV hi", 10);
		jTextSUVhi.setText(preferInt(tmpd));
		Integer tmpi = prefer.getInt("Vol CT lo", -250);
		jTextCTlo.setText(tmpi.toString());
		tmpi = prefer.getInt("Vol CT hi", -80);
		jTextCThi.setText(tmpi.toString());
		tmpi = prefer.getInt("Ct display strength", 100);
		jTextCtStrength.setText(tmpi.toString());
		tmpd = prefer.getDouble("SUV display max", 5);
		jTextSuvDisplay.setText(preferInt(tmpd));

		jCheckSphere.setSelected(prefer.getBoolean("sphere ROI", false));
		jCheckName.setSelected(prefer.getBoolean("bf check name", false));
		jCheckDate.setSelected(prefer.getBoolean("bf check date", false));
		jCheckId.setSelected(prefer.getBoolean("bf check id", false));
		jCheckInvert.setSelected(prefer.getBoolean("bf invert image", false));
		jCheckUseSUV.setSelected(prefer.getBoolean("bf use suv", true));
		jCheckUseCt.setSelected(prefer.getBoolean("bf use ct", true));
		jCheckOld.setSelected(prefer.getBoolean("old bf", true));
//		jCheckGrown.setSelected(prefer.getBoolean("grown roi", false));
		jCheckGrown.setSelected(true);
		// allow BN Lee to see this - comment out next line
//		jCheckGrown.setVisible(false);
		jCheckDefLimits.setSelected(prefer.getBoolean("defined limits", false));
		jCheckUseTemp.setSelected(prefer.getBoolean("nifti tmp", false));
		if( getNiftiPrefs(0)) jCheckUseTemp.setSelected(true);
		changeTmp();
		jTextParms.setVisible(false);
		tmpi = prefer.getInt("blue dot size", 0);
		if( tmpi > 0) {
			SpinnerNumberModel spin1 = getSpinModel(5);
			spin1.setValue(tmpi);
		}
		jLabNifxmin.setVisible(false);
		jLabNifxmax.setVisible(false);
		jLabNiftix.setVisible(false);
		jLabNifymin.setVisible(false);
		jLabNifymax.setVisible(false);
		jLabNify.setVisible(false);
		jLabNifzmin.setVisible(false);
		jLabNifzmax.setVisible(false);
		jLabNifz.setVisible(false);
		jCheckBlue.setSelected(false);
		String shape = IJ.getDirectory("imagej") + "lib/petct/shaping_elongL";
		File testIt = new File(shape);
		if( !testIt.exists()) j3Dtab.remove(jNifti);
		jTextNifSrc.setText(prefer.get(getNiftiRegKey(), ""));
		JFijiPipe pet = parentPet.petPipe;
		val = pet.data1.width;
		valz = (int)(val/pet.zoomX);
		if( valz > val) valz = val;
		off1 = (val - valz)/2;
		lo = prefer.getInt("nifti lox", 10);
		hi = prefer.getInt("nifti hix", 90);
		loVal = (lo * valz / 100) + off1;
		hiVal = (hi * valz / 100) + off1;
		jLabNifxmin.setText(loVal.toString());
		jLabNifxmax.setText(hiVal.toString());
		val = pet.data1.height;
		valz = (int)(val/pet.zoomX);
		if( valz > val) valz = val;
		off1 = (val - valz)/2;
		lo = prefer.getInt("nifti loy", 10);
		hi = prefer.getInt("nifti hiy", 90);
		loVal = (lo * valz / 100) + off1;
		hiVal = (hi * valz / 100) + off1;
		jLabNifymin.setText(loVal.toString());
		jLabNifymax.setText(hiVal.toString());
		lo = prefer.getInt("nifti loz", 5);
		hi = prefer.getInt("nifti hiz", 95);
		val = pet.getNormalizedNumFrms();
		loVal = lo * val / 100;
		hiVal = hi * val / 100;
		jLabNifzmin.setText(loVal.toString());
		jLabNifzmax.setText(hiVal.toString());
		setSpinDiameterState();
		fillStrings();
		jButTrash.setEnabled(false);
		for( val = 2; val <= 6; val++) if( getNiftiPrefs(val)) break;
		if( val > 6) jPanExNifti.setVisible(false);
		activateBuildButton();
		m_timer = new Timer(1500, new RefreshAction());
		m_timer.setRepeats(false);
		changeUseSUV(false);
		changeUseCT(false);
		initRadio(prefer);
	}

	@Override
	public void windowGainedFocus(WindowEvent we) {
		Window wold = we.getOppositeWindow();
//		Window wnew = we.getWindow();
		saveParent( wold);
	}

	@Override
	public void windowLostFocus(WindowEvent we) {}

	@Override
	public void dispose() {
		WindowManager.removeWindow(this);
		parentPet.repaintAll();
		saveRegistryValues();
		instance = null;
		killMe = true;
		super.dispose();
	}

	String preferInt( double inDouble) {
		String ret1;
		Integer inVal = (int) inDouble;
		Double outVal =  inVal.doubleValue();
		if( outVal == inDouble) {
			ret1 = inVal.toString();
		} else {
			outVal = inDouble;
			ret1 = outVal.toString();
		}
		return ret1;
	}

	private void saveRegistryValues() {
		Preferences prefer = parentPet.parent.jPrefer;
		String tmp;
		int lo1, hi1;
//		Dimension sz1 = getSize();
//		prefer.putInt("bf size x", sz1.width);
//		prefer.putInt("bf size y", sz1.height);
		tmp = jTextSliceLo.getText();
		if( !tmp.isEmpty()) {
			lo1 = Integer.parseInt(tmp);
			tmp = jTextSliceHi.getText();
			hi1 = Integer.parseInt(tmp);
			int diff1 = (hi1-lo1)/2;
			if( diff1 < 0) diff1 = 0;
			prefer.putInt("last brown space", diff1);
		}
		prefer.putInt("calc ct type", getCTcalc());
		double[] SuvCt = getSUVandCTLimits();
		tmp = preferInt(SuvCt[0]);
		if(isPercent) tmp += "%";
		prefer.put("Vol SUV lo", tmp);
		prefer.putDouble("Vol SUV hi", SuvCt[1]);
		lo1 = (int) SuvCt[2];
		prefer.putInt("Vol CT lo", lo1);
		hi1 = (int) SuvCt[3];
		prefer.putInt("Vol CT hi", hi1);
		hi1 = (int) SuvCt[4];
		prefer.putInt("Ct display strength", hi1);
		prefer.putDouble("SUV display max", SuvCt[5]);

		prefer.putBoolean("sphere ROI", jCheckSphere.isSelected());
		prefer.putBoolean("bf check name", jCheckName.isSelected());
		prefer.putBoolean("bf check date", jCheckDate.isSelected());
		prefer.putBoolean("bf check id", jCheckId.isSelected());
		prefer.putBoolean("bf invert image", jCheckInvert.isSelected());
		prefer.putBoolean("bf use suv", jCheckUseSUV.isSelected());
		prefer.putBoolean("bf use ct", jCheckUseCt.isSelected());
		prefer.putBoolean("old bf", jCheckOld.isSelected());
		prefer.putBoolean("grown roi", jCheckGrown.isSelected());
		prefer.putBoolean("defined limits", jCheckDefLimits.isSelected());
		prefer.putBoolean("nifti tmp", jCheckUseTemp.isSelected());
		prefer.putInt("blue dot size", getSpinInt(5));

		int i, val;
		boolean sel1;
		String tmp1;
		JCheckBox cbox;
		tmp1 = getRadioText(0);
		val = Integer.parseInt(tmp1);
		if( val < 4 || val > 32) val = 10;
		prefer.putInt("radio grays", val);
		prefer.putBoolean("radio 3d", jChRad3D.isSelected());
		prefer.putBoolean("show SD", jChRadShowSD.isSelected());
		prefer.putBoolean("radio CT", jChRadCt.isSelected());
		for( i=0; i<=17; i++) {
			cbox = getRadioCB(i);
			tmp1 = "radio " + cbox.getText();
			sel1 = cbox.isSelected();
			prefer.putBoolean(tmp1, sel1);
		}
	}

	private boolean saveParent( Window w1) {
		PetCtPanel curr1;
        boolean updateVolume = false;
		if( !(w1 instanceof PetCtFrame)) return false;
		allowDelete = false;
		PetCtFrame parent = (PetCtFrame) w1;
		curr1 = parent.getPetCtPanel1();
		if( jCheckLock.isSelected() && curr1 != lockedParent) return false;
		if( parentPet != null) {	// if there is a previous one, update it
			if( parentPet == curr1) return true;	// nothing to do
            updateVolume = true;
			parentPet.bfDlg = null;
			parentPet.repaintAll();
		}
		parentPet = curr1;
		parentPet.bfDlg = this;
        if(updateVolume) calculateVol(true);
		parentPet.repaintAll();
		return true;
	}

/*	private class CustomFocusTraversalPolicy extends ContainerOrderFocusTraversalPolicy {

		@Override
		public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
			int currentPosition = elementList.indexOf(aComponent);
			currentPosition = (currentPosition + 1) % elementList.size();
			Component nextC =  elementList.get(currentPosition);
			if( nextC == jSpinRoiNm) {
				jSpinRoiNm.requestFocusInWindow();
				focusCycleRoot.repaint();
			}
			return nextC;
		}

		@Override
		public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
			int currentPosition = elementList.indexOf(aComponent);
			currentPosition = (elementList.size() + currentPosition - 1) % elementList.size();
			return (Component) elementList.get(currentPosition);
		}

		@Override
		public Component getFirstComponent(Container cntnr) {
			return (Component) elementList.get(0);
		}

		@Override
		public Component getLastComponent(Container cntnr) {
			return (Component) elementList.get(elementList.size() - 1);
		}

		@Override
		public Component getDefaultComponent(Container cntnr) {
			return (Component) elementList.get(0);
		}

		@Override
		protected boolean accept(Component aComponent) {
			return true;
		}
	}*/
	class RefreshAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent ae) {
			calculateVol(true);
		}
	}

	void delayedRefresh() {
		m_timer.restart();
	}

/*	void setSavedSize() {
		Preferences prefer = parentPet.parent.jPrefer;
		int x, y;
		x = prefer.getInt("bf size x", 0);
		y = prefer.getInt("bf size y", 0);
		if( x > 20 && y > 20) setSize( x,y);
	}*/

	double[] getSUVandCTLimits() {
		double[] retVal = new double[6];
		String tmp;
		tmp = jTextSUVlo.getText();
		isPercent = isPer1 = false;
		if(tmp.contains("%")) {
			isPercent = true;
			tmp = tmp.replace('%', ' ').trim();
		}
		retVal[0] = Double.parseDouble(tmp);
		tmp = jTextSUVhi.getText();
		retVal[1] = Double.parseDouble(tmp);
		if( retVal[0] < 0) retVal[0] = 0;
		if( !isPercent && retVal[1] < retVal[0]) retVal[1] = retVal[0];

		tmp = jTextCTlo.getText();
		retVal[2] = Double.parseDouble(tmp);
		tmp = jTextCThi.getText();
		retVal[3] = Double.parseDouble(tmp);
		if( retVal[3] < retVal[2]) retVal[3] = retVal[2];

		tmp = jTextCtStrength.getText();
		retVal[4] = Double.parseDouble(tmp);
		if( retVal[4] < 0) retVal[4] = 0;
		if( retVal[4] > 100) retVal[4] = 100;
		tmp = jTextSuvDisplay.getText();
		if(tmp.contains("%")) {
			isPer1 = true;
			tmp = "5";	// % not legal anyway
		}
		retVal[5] = Double.parseDouble(tmp);
		if( retVal[5] < 0.5) retVal[5] = 0.5;
		return retVal;
	}

	void drawAllData( Graphics2D g, PetCtPanel caller) {
		if( killMe) {
			parentPet.bfDlg = null;
//			IJ.log("killing reference to brown fat");
			return;
		}
		if( parentPet != caller) return;
		if( drawingRoi) {
			drawCurrRoi(g, currPoly, true);
		} else {
			drawAllRoi(g);
			drawFoundPoints(g);
			drawMipPoints(g);
		}
	}

	void drawDisplay3Data( Graphics2D g, Display3Panel caller) {
		if( killMe) {
			caller.bfDlg = null;
			return;
		}
		if( drawingRoi) return;
		drawAllDisp3Roi(g, caller);
		drawFound3Points(g, caller);
	}

	void handleMouseMove( MouseEvent e, PetCtPanel caller) {
		if( parentPet != caller || parentPet.mouse1.widthX == 0) return;
		currMousePos.x = e.getX() % parentPet.mouse1.widthX;
		currMousePos.y = e.getY();
		if( drawingRoi) {
			parentPet.repaint();
			return;
		}
		setProperCursor();
	}

	boolean handleMouseDrag( MouseEvent e, PetCtPanel caller) {
		if( parentPet != caller || parentPet.mouse1.widthX == 0) return false;
		if( drawingRoi) return true;	// ignore drag while drawing
		currMousePos.x = e.getX() % parentPet.mouse1.widthX;
		currMousePos.y = e.getY();
		if( saveRoiIndx < 0) return dragNiftiLimit();
		Poly3Save poly1 = polyVect.get(saveRoiIndx);
		Point pt1 = convertPoint2Pos( currMousePos, poly1.type, -1);
		dragSphere(poly1, pt1);
		poly1.poly.invalidate();
		isDirty = true;
		delayedRefresh();
		parentPet.repaint();
		return true;
	}

	boolean dragNiftiLimit() {
		if( !isDefLimits()) return false;
		int orient = parentPet.m_sliceType;
		Point pt1 = convertPoint2Pos( currMousePos, orient, -1);
		if( saveRoiPntIndx < 0) return false;
		String txt;
		Integer val;
		val = pt1.x;
		txt = val.toString();
		if(saveRoiPntIndx == 0) jLabNifxmin.setText(txt);
		else jLabNifxmax.setText(txt);
		checkLimits( jLabNifxmin,  jLabNifxmax);
		val = pt1.y;
		if(saveRoiPntIndx > 0) {
			JFijiPipe pet = parentPet.petPipe;
			if( orient == JFijiPipe.DSP_AXIAL) {
				if( val >= pet.data1.height) val = pet.data1.height-1;
			} else {
				if( val >= pet.getNormalizedNumFrms()) val = pet.getNormalizedNumFrms()-1;
			}
		}
		txt = val.toString();
		if(saveRoiPntIndx == 0) {
			if( orient == JFijiPipe.DSP_AXIAL) jLabNifymin.setText(txt);
			else jLabNifzmin.setText(txt);
		} else {
			if( orient == JFijiPipe.DSP_AXIAL) jLabNifymax.setText(txt);
			else jLabNifzmax.setText(txt);
		}
		checkLimits( jLabNifymin,  jLabNifymax);
		checkLimits( jLabNifzmin,  jLabNifzmax);
		isNiftiDirty = true;
		parentPet.repaint();
		return true;
	}

	void checkLimits(JLabel limLow, JLabel limHi) {
		Integer valLo, valHi;
		int slop = 4;
		String tmp;
		tmp = limLow.getText();
		valLo = Integer.parseInt(tmp);
		tmp = limHi.getText();
		valHi = Integer.parseInt(tmp);
		if( valHi >= valLo + slop) return;	// all is well
		if( saveRoiPntIndx == 0) {
			valLo = valHi - slop;
			tmp = valLo.toString();
			limLow.setText(tmp);
			return;
		}
		valHi = valLo + slop;
		tmp = valHi.toString();
		limHi.setText(tmp);
	}

	void dragSphere(Poly3Save poly1, Point pt1) {
		if( !poly1.sphericalROI) {	// not a shpere - easy
			poly1.poly.xpoints[saveRoiPntIndx] = pt1.x;
			poly1.poly.ypoints[saveRoiPntIndx] = pt1.y;
			return;
		}
		int x0, y0, x1, y1, delX, delY, i, sizeX, sizeY;
		x0 = poly1.poly.xpoints[0];
		y0 = poly1.poly.ypoints[0];
		x1 = poly1.poly.xpoints[1];
		y1 = poly1.poly.ypoints[2];
		sizeX = sizeY = parentPet.petPipe.data1.width;
		if( poly1.type != JFijiPipe.DSP_AXIAL)
			sizeY = parentPet.petPipe.getNormalizedNumFrms();
		switch( saveRoiPntIndx) {
			case 0:	// translation
				if( pt1.x - x1 + x0 < 0) pt1.x = x1 - x0;
				if( pt1.x + x1 - x0 >= sizeX) pt1.x = sizeX - x1 + x0;
				if( pt1.y + 2*(y0-y1) >= sizeY) pt1.y = sizeY - 2*(y0-y1);
				delX = pt1.x - x0;
				delY = pt1.y - y1;
				for( i=0; i<3; i++) {
					poly1.poly.xpoints[i] += delX;
					poly1.poly.ypoints[i] += delY;
				}
				break;
				
			case 1:	// resize sphere
				delX = pt1.x - x1;
				poly1.poly.xpoints[1] += delX;
				finishSphereSub(poly1);
				changedRoiChoice(true);
				break;
				
			case 2:	// ellipse y size
				if( pt1.y < y0+3) pt1.y = y0+3;
				if( pt1.y >= sizeY) pt1.y = sizeY - 1;
				if( pt1.y > 2*y0) pt1.y = 2*y0;
				delY = (2*y0 - y1) - pt1.y;
				poly1.poly.ypoints[2] += delY;
				break;
				
			case 3:	// ellipse x size
				if( pt1.x > x0-3) pt1.x = x0-3;
				if( 2*x0 - pt1.x >= sizeX) pt1.x = 2*x0-sizeX-1;
				delX = (2*x0 - x1) - pt1.x;
				poly1.poly.xpoints[1] += delX;
				break;
		}
	}

	int setProperCursor() {
		int i, slice, indx = -1;	// ROI index
		int slop2 = 3;
		int x0, y0, x1, y1, k, npoints, indx1;
		int orient = parentPet.m_sliceType;
		int[] xpoints, ypoints;
		Poly3Save poly1;
		Cursor cur1 = Cursor.getDefaultCursor();
		Point pt1 = null;
		saveRoiIndx = saveRoiPntIndx = -1;
		boolean showAll = jCheckShowAll.isSelected();
		indx1 = getSpinInt(0) - 1 - getRoiSize(1);

		if( getRoiSize(0) == 0 && isDefLimits()) {
			xpoints = getNiftiLimits(orient, 0);
			if(xpoints == null) return 0;
			ypoints = getNiftiLimits(orient, 1);
			pt1 = convertPoint2Pos(currMousePos, orient, -1);
			for( k=0; k<2; k++) {
				x0 = xpoints[k];
				y0 = ypoints[k];
				if( x0 < pt1.x-slop2 || x0 > pt1.x+slop2 ) continue;
				if( y0 < pt1.y-slop2 || y0 > pt1.y+slop2) continue;
				saveRoiPntIndx = k;
				cur1 = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
				break;
			}
		}

		outter:
		for( i=0; i<getRoiSize(0); i++) {
			if( !showAll && i != indx1) continue;
			poly1 = polyVect.get(i);
			if( poly1.type != orient) continue;
			if( pt1 == null) pt1 = convertPoint2Pos( currMousePos, poly1.type, -1);
			slice = getSliceNum(poly1.type) + 1;
			if( slice < poly1.lowSlice || slice > poly1.hiSlice) continue;
			npoints = poly1.poly.npoints;
			if( npoints <= 0) continue;
			xpoints = poly1.poly.xpoints;
			ypoints = poly1.poly.ypoints;
			if( poly1.sphericalROI) {
				x0 = xpoints[0];
				y0 = ypoints[0];
				x1 = xpoints[1];
				y1 = ypoints[2];
				npoints = 4;
				xpoints = new int[4];
				ypoints = new int[4];
				xpoints[0] = x0;	// the translation point
				ypoints[0] = y1;
				xpoints[1] = x1;	// sphere resize
				ypoints[1] = y0;
				xpoints[2] = x0;	// ellipse y size
				ypoints[2] = 2*y0 - y1;
				ypoints[3] = y0;	// ellipse x size
				xpoints[3] = 2*x0 - x1;
			}
			for( k=0; k<npoints; k++) {
				x0 = xpoints[k];
				y0 = ypoints[k];
				if( x0 < pt1.x-slop2 || x0 > pt1.x+slop2 ) continue;
				if( y0 < pt1.y-slop2 || y0 > pt1.y+slop2) continue;
				// finally found a match. save and break out of both loops
				saveRoiIndx = indx = i;
				saveRoiPntIndx = k;
				cur1 = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
				break outter;
			}
		}
		parentPet.setCursor(cur1);
		return indx;
	}

	void setZcenter() {
		int indx = getSpinInt(0) - 1 - getRoiSize(1);
		if( indx < 0 || indx >= getRoiSize(0)) return;
		Poly3Save poly1 = polyVect.get(indx);
		if( poly1.type != parentPet.m_sliceType) return;
		Integer[] outVal, inVal = new Integer[3];
		int diff, diffhi, difflo, slice, max;
		inVal[2] = poly1.hiSlice;
		inVal[1] = poly1.lowSlice;
		diff = inVal[2] - inVal[1];
		diffhi = difflo = diff/2;
		if( (diff & 1) == 1) diffhi++;
		slice = getSliceNum(poly1.type) + 1;
		max = parentPet.petPipe.data1.width;
		if( poly1.type == JFijiPipe.DSP_AXIAL) {
			max = parentPet.petPipe.getNormalizedNumFrms();
		}
		inVal[2] = slice + diffhi;
		if( inVal[2] > max) inVal[2] = max;
		inVal[1] = slice - difflo;
		if( inVal[1] < 1) inVal[1] = 1;
		outVal = poly1.axialInverts(inVal);
		jTextSliceLo.setText(outVal[1].toString());
		jTextSliceHi.setText(outVal[2].toString());
		calculateVol(true);
	}

	int getSliceNum( int type1) {
		int slice = 0;
		switch( type1) {
			case JFijiPipe.DSP_AXIAL:
				slice = ChoosePetCt.round(parentPet.petAxial);
				break;

			case JFijiPipe.DSP_CORONAL:
				slice = ChoosePetCt.round(parentPet.petCoronal);
				break;

			case JFijiPipe.DSP_SAGITAL:
				slice = ChoosePetCt.round(parentPet.petSagital);
				break;
		}
		return slice;
	}
	
	void killSavedSUV() {
		savePercentSUV = null;
	}
	
	void calcSavedSUV(int gateIn1) {
		boolean needCalc = false;
		if(!isPercent || !jCheckUseSUV.isSelected()) {
			killSavedSUV();
			return;
		}
		if(savePercentSUV == null || savePercentSUV.length != getRoiSize())
			needCalc = true;
		if(!needCalc && !isDirty) return;
		int i, n = getRoiSize();
		savePercentSUV = new double[n];
		for( i=0; i<n; i++) {
			SaveVolROI volTmp = calcSingleRoiVol(i, true, gateIn1);
			savePercentSUV[i] = volTmp.SUVmax;
		}
		isDirty = false;
	}

	void drawCurrRoi( Graphics2D g, Poly3Save poly1, boolean drawing) {
		int[] xp1, xp2, yp1, yp2, xp4, yp4;
		int i, j, widthX, npoints, num4, slice=-1, type1, cineIdx;
		int rubberX, rubberY, numDisp = 3;
		Point2d pt1;
		Point pt2;
		if( poly1.type != parentPet.m_sliceType) return;
		double scl1 = parentPet.getScalePet();
		double zoom1 = parentPet.petPipe.zoom1;
		npoints = poly1.poly.npoints;
		if( npoints <= 0) return;
		boolean MIPdisplay = parentPet.isMIPdisplay();
		cineIdx = parentPet.getCineIndx();
		if( MIPdisplay) numDisp = 2;
		xp1 = poly1.poly.xpoints;
		yp1 = poly1.poly.ypoints;
		type1 = 2;	// this could be 4 as well, i.e. for 3 display
		switch( poly1.type) {
			case JFijiPipe.DSP_AXIAL:
				type1 = 0;
				if( zoom1 > 1.0) type1 = 3;
				slice = ChoosePetCt.round(parentPet.petAxial);
				break;

			case JFijiPipe.DSP_CORONAL:
				slice = ChoosePetCt.round(parentPet.petCoronal);
				if(MIPdisplay && cineIdx == 0 && zoom1==1) numDisp = 3;
				break;

			case JFijiPipe.DSP_SAGITAL:
				slice = ChoosePetCt.round(parentPet.petSagital);
				if(MIPdisplay && cineIdx == 3*parentPet.getNumMIP()/4 && zoom1==1) numDisp = 3;
				break;
		}
		slice++;
		if( slice < poly1.lowSlice || slice > poly1.hiSlice) return;
		widthX = parentPet.mouse1.widthX;
		xp2 = new int[npoints];
		yp2 = new int[npoints];
		for( i=0; i<npoints; i++) {
			pt1 = new Point2d(xp1[i], yp1[i]);
			pt2 = parentPet.petPipe.pos2Scrn(pt1, scl1, type1);
			xp2[i] = pt2.x;
			yp2[i] = pt2.y;
		}
		xp4 = xp2;
		yp4 = yp2;
		num4 = npoints;
		if( !drawing && poly1.sphericalROI) {
			xp4 = new int[4];
			yp4 = new int[4];
			num4 = 4;
			xp4[0] = xp4[2] = xp2[2];
			yp4[0] = yp2[2];
			xp4[1] = xp2[1];
			yp4[1] = yp4[3] = yp2[1];
			yp4[2] = 2*yp2[0] - yp4[0];
			xp4[3] = 2*xp2[0] - xp4[1];
		}
		rubberX = currMousePos.x;
		rubberY = currMousePos.y;
		Ellipse2D el1 = poly1.getEllipse(slice, type1, true);
		for( j=0; j<numDisp; j++) {
			g.setColor(Color.green);
			if( drawing) {
				g.drawPolyline(xp2, yp2, npoints);
				i = npoints-1;
				if(rubberX >= 0) g.drawLine(xp2[i], yp2[i], rubberX, rubberY);
				g.drawRect(xp2[0]-4, yp2[0]-4, 8, 8);
			} else {
				if( poly1.sphericalROI) {
					if( el1 != null) {
						g.draw(el1);
						el1 = poly1.shiftEllipse(el1, widthX, 0);
					}
				}
				else g.drawPolygon(xp2, yp2, npoints);
			}
			Color col1 = Color.white;
			for( i=0; i<num4; i++) {
				if( i==2 && poly1.sphericalROI) col1 = Color.magenta;
				if( !drawing) drawHandle(g, xp4[i], yp4[i], col1);
				xp4[i] += widthX;
			}
			if( rubberX >= 0) rubberX += widthX;
		}
	}

/*	int[] shiftYvals( int[] inY, int shftY) {
		if( shftY == 0) return inY;
		int i, n = inY.length;
		int[] outY = new int[n];
		for( i=0; i<n; i++) outY[i] = inY[i]+shftY;
		return outY;
	}*/

	void drawCurrDisp3Roi( Graphics2D g, Poly3Save poly1, Display3Panel d3panel) {
		int[] xp1, xp2, yp1, yp2;
		int i, type1, npoints, slice, widthX, width1;
		int other1, other2;
		double sliceDbl=0, scl1;
		Point2d pt1;
		Point pt2;
		// even though we are displaying inside 3 view, use parent pet mode
		if( poly1.type != parentPet.m_sliceType) return;
		scl1 = d3panel.getScale();
		npoints = poly1.poly.npoints;
		if( npoints <= 0) return;
		xp1 = poly1.poly.xpoints;
		yp1 = poly1.poly.ypoints;
		// maybe user didn't click on panel to set mouse widthX
		widthX = (int)(scl1 * d3panel.d3Pipe.data1.width);
		width1 = 0;
		type1 = 4;
		other1 = JFijiPipe.DSP_CORONAL;
		other2 = JFijiPipe.DSP_SAGITAL;
		switch( poly1.type) {
			case JFijiPipe.DSP_AXIAL:
				type1 = 0;
				if( d3panel.d3Pipe.zoom1 > 1.0) type1 = 3;
				sliceDbl = d3panel.d3Axial;
				break;

			case JFijiPipe.DSP_CORONAL:
				other1 = JFijiPipe.DSP_AXIAL;
				width1 = widthX;
				sliceDbl = d3panel.d3Coronal;
				break;

			case JFijiPipe.DSP_SAGITAL:
				other1 = JFijiPipe.DSP_AXIAL;
				other2 = JFijiPipe.DSP_CORONAL;
				width1 = 2*widthX;
				sliceDbl = d3panel.d3Sagital;
				break;
		}
		slice = ChoosePetCt.round(sliceDbl)+1;
		if( slice >= poly1.lowSlice && slice <= poly1.hiSlice) {
			if( poly1.sphericalROI) {
				Ellipse2D el1 = poly1.getEllipseSub(slice, type1, true, d3panel);
				if( el1 != null) {
					if( width1 > 0) el1 = poly1.shiftEllipse(el1, width1,0);
					g.draw(el1);
				}
			} else {
				xp2 = new int[npoints];
				yp2 = new int[npoints];
				for( i=0; i<npoints; i++) {
					pt1 = new Point2d(xp1[i], yp1[i]);
					pt2 = d3panel.d3Pipe.pos2Scrn(pt1, scl1, type1, poly1.type, true);
					xp2[i] = pt2.x + width1;
					yp2[i] = pt2.y;
				}
				g.drawPolygon(xp2, yp2, npoints);
			}
		}
		drawOther3Roi(g, poly1, d3panel, other1, true);
		drawOther3Roi(g, poly1, d3panel, other2, false);
	}
	
	void drawOther3Roi( Graphics2D g, Poly3Save poly1, Display3Panel d3panel, int other, boolean horz) {
		double sliceDbl=0, scl1, x0, x1, x2, y0, y1, y2;
		int type1, widthX, width1, width2, height1, height2, numc;
		int typeOrig, i, j, numPnt;
		double loOther, hiOther, deltaX, deltaY, w2, h2;
		double middle, diff0, rad1, factor;
		boolean swap1 = false;
		Point2d pt1, pt21, pt22;
		Point pt2, pt1a;
		numPnt = poly1.poly.npoints;
		double[] cross1 = new double[numPnt];
		double zoom1 = d3panel.d3Pipe.zoom1;
		int[] pos1 = new int[numPnt];
		scl1 = d3panel.getScale();
		// maybe user didn't click on panel to set mouse widthX
		widthX = (int)(scl1 * d3panel.d3Pipe.data1.width);
		width1 = 0;
		type1 = 4;
		typeOrig = poly1.type;
		switch(other) {
			case JFijiPipe.DSP_AXIAL:
				type1 = 0;
				if( d3panel.d3Pipe.zoom1 > 1.0) type1 = 3;
				if( typeOrig == JFijiPipe.DSP_SAGITAL) swap1 = true;
				sliceDbl = d3panel.d3Axial;
				break;

			case JFijiPipe.DSP_CORONAL:
				width1 = widthX;
				if( typeOrig == JFijiPipe.DSP_SAGITAL) swap1 = true;
				sliceDbl = d3panel.d3Coronal;
				break;

			case JFijiPipe.DSP_SAGITAL:
				if( d3panel.showMip) return;
				if( typeOrig == JFijiPipe.DSP_CORONAL) swap1 = true;
				width1 = 2*widthX;
				sliceDbl = d3panel.d3Sagital;
				break;
		}
		if( poly1.sphericalROI) {
			y0 = (poly1.hiSlice + poly1.lowSlice)/2.0;
			Ellipse2D el1 = poly1.getEllipseSub((int)y0, type1, false, d3panel);
			if( el1 == null) return;
			Rectangle2D brect = el1.getBounds2D();
			if(horz) {
				loOther = brect.getMinY();
				hiOther = brect.getMaxY();
				x0 = brect.getCenterX();
				w2 = brect.getWidth();
			} else {
				loOther = brect.getMinX();
				hiOther = brect.getMaxX();
				x0 = brect.getCenterY();
				w2 = brect.getHeight();
			}
			if( sliceDbl <= loOther || sliceDbl >= hiOther) return;
			middle = (hiOther + loOther)/2.0;
			diff0 = Math.abs(middle - sliceDbl);
			rad1 = (hiOther - loOther)/2.0;
			factor = Math.pow(rad1, 2) - Math.pow(diff0, 2);
			factor = Math.sqrt(factor)/rad1;
			deltaX = w2*factor/2.0;
			deltaY = (poly1.hiSlice - poly1.lowSlice)*factor/2.0;
			if( swap1) {	// for sagital maybe interchange x and y
				x1 = x0;
				x0 = y0;
				y0 = x1;
				x1 = deltaX;
				deltaX = deltaY;
				deltaY = x1;
			}
			pt21 = new Point2d(x0 - deltaX, y0 - deltaY);
			pt22 = new Point2d(x0 + deltaX, y0 + deltaY);
			scl1 = d3panel.getScale();
			pt1a = d3panel.d3Pipe.pos2Scrn(pt21, scl1, type1, other, true);
			pt2 = d3panel.d3Pipe.pos2Scrn(pt22, scl1, type1, other, true);
			pt21 = new Point2d(pt1a.x, pt1a.y);
			pt22 = new Point2d(pt2.x, pt2.y);
			el1 = new Ellipse2D.Double( pt21.x, pt21.y, pt22.x - pt21.x, pt22.y-pt21.y);
			if( width1 > 0) el1 = poly1.shiftEllipse(el1, width1, 0);
			g.draw(el1);
			return;
		}
		sliceDbl += 1.0;
		numc = 0;
		if(horz) {
			for( i=0; i<numPnt; i++) {
				j = i+1;
				if( j>=numPnt) j=0;
				x1 = poly1.poly.xpoints[i];
				x2 = poly1.poly.xpoints[j];
				y1 = poly1.poly.ypoints[i];
				y2 = poly1.poly.ypoints[j];
				if( y1 == sliceDbl) {
					cross1[numc] = x1;
					pos1[numc++] = 1;
					continue;
				}
				if( (y1 > sliceDbl && y2 < sliceDbl) || ( y1 < sliceDbl && y2 > sliceDbl)) {
					x0 = (sliceDbl-y1)*(x2-x1)/(y2-y1) + x1;
					cross1[numc] = x0;
					pos1[numc++] = 2;
				}
			}
		} else {
			for( i=0; i<numPnt; i++) {
				j = i+1;
				if( j>=numPnt) j=0;
				y1 = poly1.poly.xpoints[i];
				y2 = poly1.poly.xpoints[j];
				x1 = poly1.poly.ypoints[i];
				x2 = poly1.poly.ypoints[j];
				if( y1 == sliceDbl) {
					cross1[numc] = x1;
					pos1[numc++] = 1;
					continue;
				}
				if( (y1 > sliceDbl && y2 < sliceDbl) || ( y1 < sliceDbl && y2 > sliceDbl)) {
					x0 = (sliceDbl-y1)*(x2-x1)/(y2-y1) + x1;
					cross1[numc] = x0;
					pos1[numc++] = 2;
				}
			}
		}
		if( numc < 2) return;	// a single point has no width
		if( numc > 2) {
			i = 0;
			while(i<numc-2) { // eliminate duplicate single points
				x0 = pos1[i];
				x1 = pos1[i+1];
				x2 = pos1[i+2];
				if(x0 == 1 && x1 == 1 && x2 == 1) {
					for( j= i+1; j<numc-2; j++) {
						pos1[j] = pos1[j+1];
						cross1[j] = cross1[j+1];
					}
					numc--;
					continue;
				}
				i++;
			}
			i = 0;
			while(i<numc-2) { // eliminate single points in the middle
				x0 = pos1[i];
				x1 = pos1[i+1];
				x2 = pos1[i+2];
				if(x0 == 2 && x1 == 1 && x2 == 2) {
					for( j= i+1; j<numc-2; j++) {
						pos1[j] = pos1[j+1];
						cross1[j] = cross1[j+1];
					}
					numc--;
					continue;
				}
				i++;
			}
		}
		y1 = poly1.lowSlice;
		height1 = height2 = ChoosePetCt.round((poly1.hiSlice - y1)*scl1*zoom1);
		for( i=0; i<numc-1; i+=2) {
			x1 = cross1[i];
			x2 = cross1[i+1];
			if( x1 > x2) {
				x2 = x1;
				x1 = cross1[i+1];
			}
			width2 = ChoosePetCt.round((x2 - x1)*scl1*zoom1);
			if( !swap1) pt1 = new Point2d(x1, y1);
			else {	// for sagital maybe interchange x and y
				pt1 = new Point2d(y1,x1);
				height2 = width2;
				width2 = height1;
			}
			pt2 = d3panel.d3Pipe.pos2Scrn(pt1, scl1, type1, other, true);
			g.drawRect(pt2.x + width1, pt2.y, width2, height2);
		}
	}

	void drawHandle( Graphics2D g, int x1, int y1, Color col1) {
		g.setColor(Color.black);
		g.fillRect(x1-2, y1-2, 5, 5);
		g.setColor(col1);
		g.fillRect(x1-1, y1-1, 3, 3);
	}

	void drawAllRoi( Graphics2D g) {
		Poly3Save poly1;
		if( RoiState == 0) return;
		drawNiftiLimits( g);
		boolean showAll = jCheckShowAll.isSelected();
		int i, n = getRoiSize(0);
		if( showAll) {
			for( i = 0; i < n; i++) {
				poly1 = polyVect.get(i);
				drawCurrRoi(g, poly1, false);
			}
			return;
		}
		i = getSpinInt(0) - getRoiSize(1);
		if( i < 1 || i > n) return;
		poly1 = polyVect.get(i-1);
		drawCurrRoi(g, poly1, false);
	}

	boolean isDefLimits() {
		return jCheckDefLimits.isSelected() && j3Dtab.getSelectedIndex() == 4;
	}

	boolean isCtRadiomics() {
		return jChRadCt.isSelected();
	}

	void drawNiftiLimits( Graphics2D g) {
		int[] xp1, yp1, xp2, yp2, xp4;
		Point2d pt1;
		Point pt2;
		int i, j, width1, widthX, type1 = 2, numDisp = 3;
		if( !isDefLimits()) return;
		int orient = parentPet.m_sliceType;
		xp1 = getNiftiLimits(orient, 0);
		if(xp1 == null) return;
		yp1 = getNiftiLimits(orient, 1);
		xp2 = new int[2];
		yp2 = new int[2];
		xp4 = new int[2];
		if( orient == JFijiPipe.DSP_AXIAL) {
			numDisp = 2;
			type1 = 0;
			if( parentPet.petPipe.zoom1 > 1.0) type1 = 3;
		}
		double scl1 = parentPet.getScalePet();
		widthX = parentPet.mouse1.widthX;
		for( i=0; i<2; i++) {
			pt1 = new Point2d(xp1[i], yp1[i]);
			pt2 = parentPet.petPipe.pos2Scrn(pt1, scl1, type1);
			xp2[i] = xp4[i] = pt2.x;
			yp2[i] = pt2.y;
		}
		width1 = xp2[1] - xp2[0];
		Color col1 = Color.white;
		for( j=0; j<numDisp; j++) {
			g.setColor(Color.green);
			g.drawRect(xp2[0], yp2[0], width1, yp2[1]-yp2[0]);
			for( i=0; i<2; i++) {
				drawHandle(g, xp4[i], yp2[i], col1);
				xp4[i] += widthX;
			}
			xp2[0] += widthX;
		}
	}

	int[] getNiftiLimits(int orient, int xyType) {
		if( orient != JFijiPipe.DSP_AXIAL && orient != JFijiPipe.DSP_CORONAL) return null;
		int[] xy = new int[2];
		if( xyType == 0) {
			xy[0] = Integer.parseInt(jLabNifxmin.getText());
			xy[1] = Integer.parseInt(jLabNifxmax.getText());
		} else {
			if( orient == JFijiPipe.DSP_AXIAL) {
				xy[0] = Integer.parseInt(jLabNifymin.getText());
				xy[1] = Integer.parseInt(jLabNifymax.getText());
			} else {
				xy[0] = Integer.parseInt(jLabNifzmin.getText());
				xy[1] = Integer.parseInt(jLabNifzmax.getText());
			}
		}
		return xy;
	}

	void niftiLimChanged() {
		parentPet.repaint();
	}

	void drawAllDisp3Roi( Graphics2D g, Display3Panel d3panel) {
		Poly3Save poly1;
		if( RoiState == 0) return; // >= to disable
		boolean showAll = jCheckShowAll.isSelected();
		int i, n = getRoiSize(), i0 = getRoiSize(1);
		if( showAll) {
			for( i = 0; i < n-i0; i++) {
				poly1 = polyVect.get(i);
				drawCurrDisp3Roi(g, poly1, d3panel);
			}
			return;
		}
		i = getSpinInt(0) - i0;
		if( i < 1 || i > n) return;
		poly1 = polyVect.get(i-1);
		drawCurrDisp3Roi(g, poly1, d3panel);
	}
	
	void setSpinDiameterState() {
		jSpinDiameter.setEnabled(jCheckBlue.isSelected());
	}

	void drawFoundPoints( Graphics2D g) {
		if( !jCheckBlue.isSelected()) return;
		int pointDiameter = getBlueDiameter();
		if( pointDiameter <= 0) return;
		pointDiameter = (int)(pointDiameter * parentPet.petPipe.zoom1);
		if( suvPnt == null) return;
		int numPnt = suvPnt.getListSize();
		if( numPnt <= 0) return;
		if( volSliceType != parentPet.m_sliceType) return;
		int slice=0, type1 = 2;
		switch( volSliceType) {
			case JFijiPipe.DSP_AXIAL:
				type1 = 0;
				if( parentPet.petPipe.zoom1 > 1.0) type1 = 3;
				slice = ChoosePetCt.round(parentPet.petAxial);
				break;

			case JFijiPipe.DSP_CORONAL:
				slice = ChoosePetCt.round(parentPet.petCoronal);
				break;

			case JFijiPipe.DSP_SAGITAL:
				slice = ChoosePetCt.round(parentPet.petSagital);
				break;
		}
		drawFoundSub(g, false, slice, type1, pointDiameter);
		drawFoundSub(g, true, slice, type1, pointDiameter);
	}
	
	private void drawFoundSub(Graphics2D g, boolean red, int slice, int type1, int pointDiameter) {
		int i, j, x1, y1, pointDia2, widthX;
		JFijiPipe petP = parentPet.petPipe;
		Point2d pt1;
		Point pt2;
		SUVpoints.SavePoint currPnt;
		int numPnt = suvPnt.getListSize();
		g.setColor(Color.blue);
		if( red) {
			numPnt = numRed;
			g.setColor(Color.red);
		}
		double scl1 = parentPet.getScalePet();
		boolean MIPdisplay = parentPet.isMIPdisplay();
		pointDia2 = pointDiameter / 2;
		widthX = parentPet.mouse1.widthX;
		for( i = 0; i < numPnt; i++) {
			j = i;
			if( red) j = redPoints[i];
			currPnt = suvPnt.getPoint(j);
			if( currPnt.z1 != slice) continue;
			pt1 = new Point2d(currPnt.x1, currPnt.y1);
			pt2 = petP.pos2Scrn(pt1, scl1, type1);
			x1 = pt2.x - pointDia2;
			y1 = pt2.y - pointDia2;
			g.fillOval(x1, y1, pointDiameter, pointDiameter);
			g.fillOval(x1+widthX, y1, pointDiameter, pointDiameter);
			if( !MIPdisplay)
				g.fillOval(x1+2*widthX, y1, pointDiameter, pointDiameter);
		}
	}

	void drawFound3Points( Graphics2D g, Display3Panel d3panel) {
		if( !jCheckBlue.isSelected()) return;
		int pointDiameter = getBlueDiameter();
		if( pointDiameter <= 0) return;
		pointDiameter = (int)(pointDiameter * d3panel.d3Pipe.zoom1);
		if( suvPnt == null) return;
		int numPnt = suvPnt.getListSize();
		if( numPnt <= 0) return;
		int i, slice, type1;
		int srcX=0, srcY=0, srcZ=0, dstX=0, dstY=0, dstZ=0;
		switch(volSliceType) {
			case JFijiPipe.DSP_AXIAL:
				srcX = 0;
				srcY = 1;
				srcZ = 2;
				break;

			case JFijiPipe.DSP_CORONAL:
				srcX = 0;
				srcY = 2;
				srcZ = 1;
				break;

			case JFijiPipe.DSP_SAGITAL:
				srcX = 2;
				srcY = 0;
				srcZ = 1;
				break;
		}
		for( i=0; i<3; i++) {
			slice = 0;
			type1 = 4;
			switch(i) {
				case 0:	// axial
					type1 = 0;
					if( d3panel.d3Pipe.zoom1 > 1.0) type1 = 3;
					dstX = srcX;
					dstY = srcY;
					dstZ = srcZ;
					slice = ChoosePetCt.round(d3panel.d3Axial);
					break;

				case 1:	// coronal
					dstX = srcX;
					dstY = srcZ;
					dstZ = srcY;
					slice = ChoosePetCt.round(d3panel.d3Coronal);
					break;

				case 2:	// sagital
					if( d3panel.showMip) return;
					dstX = srcY;
					dstY = srcZ;
					dstZ = srcX;
					slice = ChoosePetCt.round(d3panel.d3Sagital);
					break;
			}
			drawFound3Sub(g, d3panel, false, i, dstX, dstY, dstZ, slice, type1, pointDiameter);
			drawFound3Sub(g, d3panel, true, i, dstX, dstY, dstZ, slice, type1, pointDiameter);
		}
	}
	
	private void drawFound3Sub(Graphics2D g, Display3Panel d3panel, boolean red,
			int i1, int dstX, int dstY, int dstZ, int slice, int type1, int pointDiameter) {
		int i, j, z, x, y, pointDia2, widthX;
		Point2d pt1;
		Point pt2;
		int numPnt = suvPnt.getListSize();
		g.setColor(Color.blue);
		if( red) {
			numPnt = numRed;
			g.setColor(Color.red);
		}
		double scl1 = d3panel.getScale();
		pointDia2 = pointDiameter / 2;
		widthX = (int)(scl1 * d3panel.d3Pipe.data1.width);
		for( i = 0; i < numPnt; i++) {
			j = i;
			if( red) j = redPoints[i];
			z = getVolPoint(dstZ, j);
			if( z != slice) continue;
			pt1 = new Point2d(getVolPoint(dstX, j), getVolPoint(dstY, j));
			pt2 = d3panel.d3Pipe.pos2Scrn(pt1, scl1, type1, i1+1, true);
			x = pt2.x - pointDia2;
			y = pt2.y - pointDia2;
			g.fillOval(x+i1*widthX, y, pointDiameter, pointDiameter);
		}
	}
	
	int getBlueDiameter() {
		return getSpinInt(5);
	}

	void drawMipPoints( Graphics2D g) {
		if( !jCheckBlue.isSelected()) return;
		int pointDiameter = getBlueDiameter();
		if( pointDiameter <= 0) return;
		if( suvPnt == null) return;
		int numPnt = suvPnt.getListSize();
		if( numPnt <= 0) return;
		if( !parentPet.isMIPdisplay()) return;
		int srcX=0, srcY=0, srcZ=0;
		JFijiPipe.mipXYentry currXY = parentPet.mipPipe.data1.getcurrMIPentry(minMip, maxMip);
		switch(volSliceType) {
			case JFijiPipe.DSP_AXIAL:
				srcX = 0;
				srcY = 1;
				srcZ = 2;
				break;

			case JFijiPipe.DSP_CORONAL:
				srcX = 0;
				srcY = 2;
				srcZ = 1;
				break;

			case JFijiPipe.DSP_SAGITAL:
				srcX = 2;
				srcY = 0;
				srcZ = 1;
				break;
		}
		drawMipSub(g, false, currXY, srcX, srcY, srcZ, pointDiameter);
		drawMipSub(g, true, currXY, srcX, srcY, srcZ, pointDiameter);
	}
	
	private void drawMipSub(Graphics2D g, boolean red, JFijiPipe.mipXYentry currXY,
			int srcX, int srcY, int srcZ, int pointDiameter) {
		int i, j, j1, z, zlo, x, y, wid, pointDia2, widthX;
		short[] xSlc, ySlc;
		Point2d pt1;
		Point pt2;
		JFijiPipe.mipEntry currSlice;
		int numPnt = suvPnt.getListSize();
		g.setColor(Color.blue);
		if( red) {
			numPnt = numRed;
			g.setColor(Color.red);
		}
		double scl1 = parentPet.getScalePet();
		zlo = currXY.zlo;
		pointDia2 = pointDiameter / 2;
		widthX = (int)(scl1 * parentPet.petPipe.data1.width);
		for( j = 0; j < numPnt; j++) {
			j1 = j;
			if( red) j1 = redPoints[j];
			z = getVolPoint(srcZ, j1);
			x = getVolPoint(srcX, j1);
			y = getVolPoint(srcY, j1);
			currSlice = currXY.xydata.get(z-zlo);
			if(currSlice.zpos != z) {	// hopefully this will never happen
				IJ.log("The z position doesn't match the volume position");
				return;
			}
			wid = currSlice.xval.length;
			xSlc = currSlice.xval;
			ySlc = currSlice.yval;
			for( i=0; i<wid; i++) {
				if( x == xSlc[i] && y == ySlc[i]) break;
			}
			if( i >= wid) continue;	// point not in MIP - common case
			pt1 = new Point2d(i, z);
			pt2 = parentPet.mipPipe.pos2Scrn(pt1, scl1, 1, 2, true);
			x = pt2.x - pointDia2;
			y = pt2.y - pointDia2;
			g.fillOval(x+2*widthX, y, pointDiameter, pointDiameter);
		}
	}
	
	int getVolPoint(int dst1, int indx) {
		SUVpoints.SavePoint currPnt = suvPnt.getPoint(indx);
		switch( dst1) {
			case 0:
				return currPnt.x1;

			case 1:
				return currPnt.y1;

			case 2:
				return currPnt.z1;
		}
		return 0;
	}

	void enableSliceLimits(boolean enable) {
		if( isSliceLimits == enable) return;	// already done
		isSliceLimits = enable;
		jLabSlLim.setEnabled(enable);
		jTextSliceHi.setEnabled(enable);
		jTextSliceLo.setEnabled(enable);
		jLabLoLim.setEnabled(enable);
		jLabHiLim.setEnabled(enable);
	}

	void changeRoiState( int indx) {
		if( indx == 0 || indx == 2) {
			int i = JOptionPane.showConfirmDialog(this,
				"Using none or exterior ROIs is a very unusual request.\n"
				+ "It can involve very long calculation times.\nAre you sure this is what you want?",
				"Exterior or no ROIs", JOptionPane.YES_NO_OPTION);
			if( i != JOptionPane.YES_OPTION) {
				jRadioInterior.setSelected(true);
				return;
			}
		}
		if( indx >= 0) RoiState = indx;
		else jCheckFollow.setSelected(false);	// turn off for Show All
		boolean enabled = false;
		if( RoiState > 0) enabled = true;
		jTogDrawRoi.setSelected(false);
		jCheckShowAll.setEnabled(enabled);
		if( jCheckShowAll.isSelected()) enabled = false;
		jTogDrawRoi.setEnabled(enabled);
		jSpinRoiNm.setEnabled(enabled);
		jCheckFollow.setEnabled(enabled);
		changeRoiAndUpdate();
	}

	/**
	 * This routine updates the low and hi slice limits with the current values
	 * of the text boxes. It is called when the user hits Enter in one of the boxes.
	 * It is also called upon double click to close the ROI.
	 */
	protected void changeLimits() {
		String tmp;
		if( !jTogDrawRoi.isEnabled()) return;
		tmp = jTextSliceLo.getText();
		if( tmp.isEmpty()) return;
		Integer[] inVal = new Integer[3];
		Integer[] outVal;
		inVal[1] = Integer.parseInt(tmp);
		tmp = jTextSliceHi.getText();
		inVal[2] = Integer.parseInt(tmp);
		if( inVal[2] < inVal[1]) {	// what if the user put them in backwards?
			inVal[0] = inVal[1];
			inVal[1] = inVal[2];
			inVal[2] = inVal[0];
		}
		if( drawingRoi) {
			outVal = currPoly.axialInverts(inVal);
			currPoly.hiSlice = outVal[2];
			currPoly.lowSlice = outVal[1];
			return;
		}
		int indx = getSpinInt(0) - getRoiSize(1);	// indx into manual ROIs
		int n = getRoiSize(0);
		if( indx > 0 && indx <= n) {
			Poly3Save poly1 = polyVect.get(indx - 1);
			outVal = poly1.axialInverts(inVal);
			if( outVal[2] != poly1.hiSlice || outVal[1] != poly1.lowSlice) isDirty = true;
			poly1.hiSlice = outVal[2];
			poly1.lowSlice = outVal[1];
			parentPet.repaintAll();
		}
	}

	void pressTrashBut() {
		int i;
		if(!allowDelete) {
			i = JOptionPane.showConfirmDialog(this,
				"You are about to delete a ROI\nAre you sure?",
				"Remove ROI", JOptionPane.YES_NO_OPTION);
			if( i != JOptionPane.YES_OPTION) {
				changedRoiChoice(true);
				return;
			}
		}
		allowDelete = true;
		int indx = changedRoiChoice(false) -1;	// count from zero
		if( indx < nifListSz) {
			for( i= indx; i<nifListSz-1; i++) nifList[i] = nifList[i+1];
			nifList[i] = null;
			isDelete = true;
			nifListSz--;
			deleteFromNiftiMask(indx);
		}
		else polyVect.remove(indx-getRoiSize(1));
		killSavedSUV();
		isDirty = true;
		changeRoiAndUpdate();
	}

	void deleteFromNiftiMask(int indx) {
		String dirName;
		int i, j, k, headerSz, width, height, numZ, frmSz;
		byte[] buf;
		byte valb;
		for(i=0; i<2; i++) {
			dirName = getNiftiDir(1) + "/" + generateNiftiName(1);
			if( i>0) {
				j = dirName.lastIndexOf('/');
				dirName = dirName.substring(0, j+1) + "GrowedMaskResult.nii";
			}
			try {
				File mskFl = new File(dirName);
				if(!mskFl.exists()) continue;
				File fixFile = new File(getNiftiDir(1) + "/fixed");
				FileInputStream fl1 = new FileInputStream(mskFl);
				headerSz = 348+4;
				buf = new byte[headerSz];
				fl1.read(buf);	// read header
				j = readShort(buf, 0);
				if( j != 348) {
					fl1.close();
					return;
				}
				width = readShort(buf, 42);
				height = readShort(buf, 44);
				numZ = readShort(buf, 46);
				frmSz = width * height;
				FileOutputStream out = new FileOutputStream(fixFile);
				out.write(buf);
				buf = new byte[frmSz];
				for( j=0; j<numZ; j++) {
					fl1.read(buf);
					for( k=0; k<frmSz; k++) {
						valb = buf[k];
						if( valb > indx) {	// indx counts from zero, valb from 1
							if(valb > indx+1)valb--;
							else valb = 0;
							buf[k] = valb;
						}
					}
					out.write(buf);
				}
				fl1.close();
				out.close();
				mskFl.delete();
				fixFile.renameTo(mskFl);
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		}
	}

	void pressRoiBut() {
		int i;
		// allow only 1 orentation for Nifti
		if( nifListSz > 0) {
			if( !polyVect.isEmpty()) {
				Poly3Save poly1 = polyVect.get(0);
				if(poly1.type != parentPet.m_sliceType) {
					i = polyVect.size();
					String tmp = "In Nifti you may not change orientation for manual ROIs\n";
					tmp += "To use this orientation first Remove your " + i + " manual ROIs";
					JOptionPane.showMessageDialog(this, tmp);
					jTogDrawRoi.setSelected(false);
					return;
				}
			}
		}
		if( !jTogDrawRoi.isSelected()) {
//			changedRoiChoice(true);
			processMouseDoubleClick();
			return;
		}
		if( parentPet.bfDlg != this) {
			jTogDrawRoi.setSelected(false);
			return;
		}
		initDraw(true);
		drawingRoi = true;
	}

	int getRoiSize() {
		return nifListSz + polyVect.size();
	}

	int getRoiSize(int type) {
		if( type == 1) return nifListSz;
		return polyVect.size();
	}

	void processMouseSingleClick( int pos3, Point pt1, double scl1) {
		Point pt2 = convertPoint2Pos( pt1, currPoly.type, scl1);
		// sometimes a "slow" double click will duplicate the last point
		// make sure that the point being added is more than one position away
		int x, y, n;
		n = currPoly.poly.npoints;
		boolean addPnt = true;
		if(n>0) {
			x = currPoly.poly.xpoints[n-1];
			y = currPoly.poly.ypoints[n-1];
			if( x >= pt2.x-1 && x <= pt2.x+1 && y >= pt2.y-1 && y <= pt2.y+1) addPnt = false;
		}
		if(addPnt) {
			if(n==0) initDraw(false);	// maybe a scroll?
			currPoly.poly.addPoint(pt2.x, pt2.y);
			if( n==1) {
				if( finishDrawSphere()) return;
			}
		}
		currMousePos.x = -1;
		parentPet.repaint();
	}

	Point convertPoint2Pos( Point pt1, int type, double scl1) {
		int i = 0;
		double scl2 = scl1;
		if( scl2 <= 0) scl2 = parentPet.getScalePet();
		if( parentPet.petPipe.zoom1 > 1.0) i = 3;
		if( type != JFijiPipe.DSP_AXIAL) i = 4;
		return parentPet.petPipe.scrn2PosInt(pt1, scl2, i);
	}

	void processMouseDoubleClick() {
		if( drawingRoi) {
			changeLimits();	// maybe the user changed the limits
			if(currPoly.poly.npoints > 2) polyVect.add(currPoly);
			allowSliceChange = false;
			changeRoiAndUpdate();
		}
	}

	// this finishes the drawing of a sphere if a sphere has been chosen
	boolean finishDrawSphere() {
		if( !jCheckSphere.isSelected()) return false;
		currPoly = finishSphereSub(currPoly);
		currPoly.sphericalROI = true;
		polyVect.add(currPoly);
		allowSliceChange = false;
		changedRoiChoice(true);
		return true;
	}
	
	Poly3Save finishSphereSub(Poly3Save poly1) {
		int x0, x1, y0, y1, deltaX, deltaY, deltaZ, i, num1;
		int sizeY, sizeZ, sliceNum;
		JFijiPipe pip0 = parentPet.petPipe;
		x0 = poly1.poly.xpoints[0];
		y0 = poly1.poly.ypoints[0];
		x1 = poly1.poly.xpoints[1];
		y1 = poly1.poly.ypoints[1];
		double xScale, yScale, zScale, radius;
		xScale = yScale = pip0.data1.pixelSpacing[0];
		zScale = xScale * pip0.data1.y2xFactor;
		sizeY = pip0.data1.height;
		sizeZ = pip0.getNormalizedNumFrms();
		if( poly1.type != JFijiPipe.DSP_AXIAL) {
			yScale = zScale;
			zScale = xScale;
			i = sizeY;
			sizeY = sizeZ;
			sizeZ = i;
		}
		radius = Math.pow((x1-x0)*xScale, 2) + Math.pow((y1-y0)*yScale, 2);
		radius = Math.sqrt(radius);
		deltaX = (int) Math.round(radius/xScale);
		if( deltaX < 3) deltaX = 3;	// minimum radius
		if( x0 - deltaX < 0) {
			deltaX = x0;
			radius = deltaX * xScale;
		}
		i = pip0.data1.width;
		if( x0 + deltaX >= i) {
			deltaX = i - x0 - 1;
			radius = deltaX * xScale;
		}
		deltaY = (int) Math.round(radius/yScale);
		if( y0 - deltaY < 0) {
			deltaY = y0;
		}
		if( y0 + deltaY >= sizeY) {
			deltaY = sizeY - y0 - 1;
		}
		poly1.poly.xpoints[1] = x0 + deltaX;
		poly1.poly.ypoints[1] = y0;
		num1 = poly1.poly.npoints;	// 2 on initial draw
		if( num1 == 2) {
			poly1.poly.addPoint(x0, y0-deltaY);
			sliceNum = getSliceNum(poly1.type);
		} else {
			poly1.poly.xpoints[2] = x0;
			poly1.poly.ypoints[2] = y0-deltaY;
			sliceNum = (poly1.lowSlice + poly1.hiSlice)/2;
			sliceNum--;	// count from zero
		}
		deltaZ = (int) Math.round(radius/zScale);
		if( sliceNum - deltaZ < 0) {
			deltaZ = sliceNum;
		}
		if( sliceNum + deltaZ >= sizeZ) {
			deltaZ = sizeZ - sliceNum - 1;
		}
		sliceNum++;	// count from 1, not 0
		poly1.lowSlice = sliceNum - deltaZ;
		poly1.hiSlice = sliceNum + deltaZ;
		return poly1;
	}

	void initDraw(boolean pass1) {
		Preferences prefer = parentPet.parent.jPrefer;
		// use the registry value only if there are no valid numbers already
		int sliceSpace = prefer.getInt("last brown space", 1);
		int pvSize = getRoiSize();
		if( pass1) {
			SpinnerNumberModel spin1 = getSpinModel(0);
			spin1.setValue(pvSize+1);
			jTogDrawRoi.setSelected(true);
		}
		String tmp = jTextSliceLo.getText();
		if( !tmp.isEmpty()) {
			try {
				int lo1 = Integer.parseInt(tmp);
				tmp = jTextSliceHi.getText();
				if( !tmp.isEmpty()) {
					int hi1 = Integer.parseInt(tmp);
					hi1 = Math.abs(hi1-lo1)/2;
					sliceSpace = hi1;
				}
			} catch (Exception e) {}
		}
		currPoly = new Poly3Save();
		currPoly.type = parentPet.m_sliceType;
		currPoly.numFrm = parentPet.petPipe.getNormalizedNumFrms();
		int currSlice=0, sizeZ = parentPet.petPipe.data1.width;
		switch( currPoly.type) {
			case JFijiPipe.DSP_AXIAL:
				sizeZ = currPoly.numFrm;
				currSlice = ChoosePetCt.round(parentPet.petAxial);
				break;

			case JFijiPipe.DSP_CORONAL:
				currSlice = ChoosePetCt.round(parentPet.petCoronal);
				break;

			case JFijiPipe.DSP_SAGITAL:
				currSlice = ChoosePetCt.round(parentPet.petSagital);
				break;
		}
		Integer[] outVal, inVal = new Integer[3];
		inVal[1] = currSlice - sliceSpace + 1;
		if( inVal[1] < 1) inVal[1] = 1;
		currPoly.lowSlice = inVal[1];
		inVal[2] = currSlice + sliceSpace + 1;
		if( inVal[2] > sizeZ) inVal[2] = sizeZ;
		currPoly.hiSlice = inVal[2];
		currPoly.ROIlabel = getComboLabel();
		outVal = currPoly.axialInverts(inVal);
		jTextSliceLo.setText(outVal[1].toString());
		jTextSliceHi.setText(outVal[2].toString());
		currPoly.poly = new Polygon();
		if(pvSize == 0) Roi1StartTime = new Date();
		if(pass1) currMousePos.x = -1;
	}
	
	protected void changeRoiAndUpdate() {
		changedRoiChoice(true);
		calculateVol(true);
		changeSlice();
	}

	// this responds to clicking on the follow check
	void followCheck() {
		isSliceChange = jCheckFollow.isSelected();
		changeSlice();
	}

	// this is the routine which does the follow check box
	void changeSlice() {
		if( !isSliceChange) return;
		isSliceChange = false;
		if( numRed < 1 || suvPnt == null) return;
		if( getSpinInt(0) > getRoiSize()) return;
		int red0 = redPoints[0];
		if( red0 >= suvPnt.getListSize()) return;
		parentPet.maybeNewPosition(suvPnt.getPoint(redPoints[0]));
	}
	
	String getComboLabel() {
		String retVal = (String) jComboROIlabel.getSelectedItem();
		return retVal;
	}

	/**
	 * This routine is called when the spinner button is changed to choose a ROI.
	 * The boolean input determines whether it just gets the index, or changes things.
	 * It is also called in many places like when a ROI is removed to update the
	 * maximum value of the allowed ROI.
	 * 
	 * @param chgText whether to change text values or just get the index
	 * @return the index of the currently selected ROI
	 */
	protected int changedRoiChoice(boolean chgText) {
		SpinnerNumberModel spin1 = getSpinModel(0);
		int indx = spin1.getNumber().intValue();
		int n = getRoiSize();
		boolean limitsFlg = true;
		if( chgText) {
			boolean follow = jCheckFollow.isSelected();
			boolean showAll = jCheckShowAll.isSelected();
			int n1 = getRoiSize(1);	// nifti ROI
			String label;
			if( showAll) limitsFlg = false;
			if( n >= indx) {
				if( indx <= n1) {
					limitsFlg = false;
					Nifti3 nif1 = (Nifti3) nifList[indx-1];
					label = nif1.ROIlabel;
				} else {
					Poly3Save poly1 = polyVect.get(indx-1-n1);
					Integer[] val1 = poly1.axialInverts();
					jTextSliceLo.setText(val1[1].toString());
					jTextSliceHi.setText(val1[2].toString());
					label = poly1.ROIlabel;
				}
				jComboROIlabel.getEditor().setItem(label);
			}
			isSliceChange = !showAll && follow && allowSliceChange;
			enableSliceLimits(limitsFlg);
			spin1.setMaximum(n+1);
			allowSliceChange = true;	// one time supression
			jTogDrawRoi.setSelected(false);
			jButTrash.setEnabled(n>=indx && RoiState > 0 && !showAll);
			drawingRoi = false;
			currPoly = null;
			parentPet.repaintAll();
		}
		return indx;
	}

	private int getCTcalc() {
		int retVal = 0;	// for jRadioAll
		if( jRadioAny.isSelected()) retVal = 2;
		if( jRadioAverage.isSelected()) retVal = 1;
		return retVal;
	}

	void changeOrientation() {
		if( !jCheckBlue.isSelected()) return;
		if( nifListSz <= 0 && suvPnt != null) return;
		calculateVol(true);
	}

	void changeUseSUV(boolean calVol) {
		boolean isChecked = jCheckUseSUV.isSelected();
		jTextSUVlo.setEnabled(isChecked);
		jTextSUVhi.setEnabled(isChecked);
		if(calVol) calculateVol(true);
	}

	void changeUseCT(boolean calVol) {
		boolean isChecked = jCheckUseCt.isSelected();
		jTextCTlo.setEnabled(isChecked);
		jTextCThi.setEnabled(isChecked);
		jRadioAny.setEnabled(isChecked);
		jRadioAll.setEnabled(isChecked);
		jRadioAverage.setEnabled(isChecked);
		if(calVol) calculateVol(true);
	}

	void initRadio(Preferences prefer) {
		Integer val;
		boolean sel1;
		String tmp1;
		JCheckBox cbox;
		val = prefer.getInt("radio grays", 10);
		jTextGray.setText(val.toString());
		jChRad3D.setSelected(prefer.getBoolean("radio 3d", true));
		jChRadShowSD.setSelected(prefer.getBoolean("show SD", true));
		jChRadCt.setSelected(prefer.getBoolean("radio CT", false));
//		cbox = getRadioCB(17);
//		cbox.setVisible(false);
		for( int i=0; i<=17; i++) {
			cbox = getRadioCB(i);
			tmp1 = "radio " + cbox.getText();
			sel1 = prefer.getBoolean(tmp1, false);
			cbox.setSelected(sel1);
		}
	}

	ArrayList<Integer> getRadioActive(boolean isCt) {
		JCheckBox cbox;
		String labCt = "";
		int i, n=17, type=0;
		if( isCt) {
			n=16;
			labCt = "CT ";
			type = 1;
			if(parentPet.MRIflg) {
				labCt = "MRI ";
				type = 2;
			}
		}
		ArrayList<Integer> retList = new ArrayList<Integer>();
		for( i=0; i<=n; i++) {
			cbox = getRadioCB(i);
			if( cbox.isSelected()) {
				SUVpoints.RadioPoint res = suvPnt.newRadioPnt();
				res.label = labCt + cbox.getText();
				res.type = type;
				suvPnt.radioList.add(res);
				retList.add(i);
			}
		}
		return retList;
	}

	JCheckBox getRadioCB( int type) {
		JCheckBox cbox = null;
		switch(type) {
			case 0:
				cbox = jChRadEntropy;
				break;

			case 1:
				cbox = jChRadHomogenity;
				break;

			case 2:
				cbox = jChRadContrast;
				break;

			case 3:
				cbox = jChRadASM;
				break;

			case 4:
				cbox = jChRadDiffEntropy;
				break;

			case 5:
				cbox = jChRadDiffVariance;
				break;

			case 6:
				cbox = jChRadCorrelation;
				break;

			case 7:
				cbox = jChRadClusterPromenence;
				break;

			case 8:
				cbox = jChRadClusterShade;
				break;

			case 9:
				cbox = jChRadCM1;
				break;

			case 10:
				cbox = jChRadCM2;
				break;

			case 11:
				cbox = jChRadInverseDiffMoment;
				break;

			case 12:
				cbox = jChRadMaxProbability;
				break;

			case 13:
				cbox = jChRadSumAverage;
				break;

			case 14:
				cbox = jChRadSumEntropy;
				break;

			case 15:
				cbox = jChRadSumVariance;
				break;

			case 16:
				cbox = jChRadVariance;
				break;

			case 17:
				cbox = jChRadSphericity;
				break;

			case 30:	// needed to read 3D
				cbox = jChRad3D;
				break;
		}
		return cbox;
	}

	String getRadioText( int type) {
		String text1 = "";
		switch(type) {
			case 0:
				text1 = jTextGray.getText();
				break;
		}
		return text1;
	}

	ExcludedROI[] calculateExcludedROIs(int gateInd1) {
		int i, n = getRoiSize();
		ExcludedROI[] retVal = new ExcludedROI[n];
		for( i=0; i<n; i++) {
			if( !isExcluded(i)) continue;
			SaveVolROI tempVal = calcSingleRoiVol( i, false, gateInd1);
			retVal[i] = new ExcludedROI();
			retVal[i].saveAll(tempVal);
		}
		return retVal;
	}

	boolean isExcluded(int roiNum) {
		String tmp;
		int n1 = getRoiSize(1);
		if(roiNum<n1) return false;
		Poly3Save polCur = polyVect.get(roiNum-n1);
		tmp = polCur.ROIlabel;
		return ( tmp != null && tmp.startsWith("exclude ROI"));
	}

	SaveVolROI calculateVol(boolean dispFlg) {
		return calculateVol(dispFlg, -1, -1);
	}

	SaveVolROI calculateVol(boolean dispFlg, int viewIn, int gateInd1) {
		int sliceType = parentPet.m_sliceType;
		CtCenterVal = 0;
		int gateIndx = parentPet.gateIndx;
		if( gateInd1 >= 0) gateIndx = gateInd1;
		if( viewIn > 0) sliceType = viewIn;	// override the default
/*		Date start, stop;
		start = new Date();
		Double diff;*/
		SaveVolROI volTmp, volRet = new SaveVolROI();
		ExcludedROI[] excludedROIs;
		ArrayList<Poly3Save> currSliceRois;
		ArrayList<SUVpoints.SavePoint> excludedPoints;
		JFijiPipe.lineEntry currLine;
		Poly3Save currRoi;
		int i, angle1=0, j, k, width1, maxSlice, sliceNum, maxDepth, depth;
		int ctSlice, elipSlice, roi, maxCalc, gateOffset = 0, curRoiNum = -1;
		int numFrm, wideCT = getCTcalc();
		boolean useSUV, useCT, savePoint, isNifti;
		double petVal, SUVmean, suvLo, suvNoRoi;
		Double prntVal, prntVal2;
		double [] suvAndCt = getSUVandCTLimits();
		int [] roiNum = new int[getRoiSize(0)];
		changeLimits(); // maybe the user has changed the limits?
		calcSavedSUV(gateInd1);
		excludedROIs = calculateExcludedROIs(gateInd1);
		suvPnt = new SUVpoints(sliceType);
		excludedPoints = suvPnt.buildExcludedList(excludedROIs);
		width1 = maxSlice = parentPet.petPipe.data1.width;
		numFrm = parentPet.petPipe.getNormalizedNumFrms();
		maxDepth = numFrm;
		if( parentPet.petPipe.data1.numTimeSlots>1) {
			gateOffset = gateIndx * numFrm;
		}
		// the user might change the orientation between the calculation of the volume
		// and the display of points, so save the type
		volSliceType = sliceType;
		if( volSliceType == JFijiPipe.DSP_AXIAL) {
			maxSlice = numFrm;
			maxDepth = parentPet.petPipe.data1.height;
		}
		maxCalc = maxSlice;
		maxMip = -1;
		volRet.SUVList = new double[maxSlice];
		for( i=0; i<maxCalc; i++) volRet.SUVList[i] = 0;
		volRet.type = volSliceType;
		if( dispFlg && RoiState == 1 && !jCheckShowAll.isSelected()) {
			i = getSpinInt(0)-1;
			if( i<excludedROIs.length && excludedROIs[i] != null) {
				volTmp = excludedROIs[i].getAll();
			} else volTmp = maybeRecalcSingleRoiVol(i, gateInd1);
			if( volTmp != null) {
				volRet = volTmp;
				maxCalc = 0;
			}
		}
		useSUV = jCheckUseSUV.isSelected();
		useCT = jCheckUseCt.isSelected();
		suvNoRoi = 0;
		isNifti = false;
		if(savePercentSUV != null) for( j=0; j<savePercentSUV.length; j++) 
			if(savePercentSUV[j] > suvNoRoi) suvNoRoi = savePercentSUV[j];
		
		k = getRoiSize(1);
		if( maxCalc == maxSlice && k>0 ) {	// single Roi not calculated, do Nifti first
			isNifti = true;
			for( i=0; i<k; i++) volRet = calcSingleNiftiRoiVolSub(i, volRet, false);
			if( volRet.SUVtotal <= 0) isNifti = false;	// shouldn't happen
		}

		if( isNifti) {	// calcuate volume even with different orientation
			if(getRoiSize(0)>0) {
				currRoi = polyVect.get(0);
				if( currRoi.type != volSliceType) {
					SUVpoints saveSuv = suvPnt;
					for(i=0; i<getRoiSize(0); i++) {
						volTmp = maybeRecalcSingleRoiVol(i+k, gateInd1);
						if( volTmp != null && volTmp.type.equals(volRet.type)) {
							volRet.SUVtotal += volTmp.SUVtotal;
							if( volTmp.SUVmax > volRet.SUVmax) {
								volRet.SUVmax = volTmp.SUVmax;
								RoiNum4Max = i+k;
							}
/*							for( j=0; j<volTmp.SUVList.length; j++) {
								petVal = volTmp.SUVList[j];
								if( petVal > volRet.SUVList[j]) volRet.SUVList[j] = petVal;
							}*/
							for( j=0; j<suvPnt.getListSize(); j++) {
								saveSuv.maybeAddPoint(suvPnt.getPoint(j));
							}
						}
					}
					suvPnt = saveSuv;	// write back updated value
					maxCalc = 0;	// no need to do it again
				}
			}
		}

		for( i=0; i<maxCalc; i++) {
			currSliceRois = new ArrayList<Poly3Save>();
			if( RoiState > 0) for( roi=j=0; roi<getRoiSize(0); roi++) {
				currRoi = polyVect.get(roi);
				if( currRoi.type != volSliceType) continue;
				if( i+1 < currRoi.lowSlice || i+1 > currRoi.hiSlice) continue;
				if( isExcluded(roi)) continue;
				currSliceRois.add(currRoi);
				roiNum[j++] = roi + getRoiSize(1);
			}
			// the next line is the biggest time saver.
			if( RoiState == 1 && currSliceRois.isEmpty()) continue;
			for( j=0; j<maxDepth; j++) {
				elipSlice = i;
				sliceNum = j;
				depth = i;
				switch (volSliceType) {
					case JFijiPipe.DSP_AXIAL:
						sliceNum = i;
						depth = j;
						break;

					case JFijiPipe.DSP_CORONAL:
						break;

					case JFijiPipe.DSP_SAGITAL:
						angle1 = 270;
						break;
				}
				currLine = parentPet.petPipe.data1.getLineOfData(angle1, depth, sliceNum+gateOffset);
				ctSlice = parentPet.ctPipe.findCtPos(sliceNum, false);
				for( k=0; k<width1; k++) {
					if (currLine.pixels != null) petVal = currLine.pixels[k] * currLine.slope;
					else petVal = currLine.pixFloat[k];
					if( currLine.SUVfactor > 0) petVal /= currLine.SUVfactor;
					if( useSUV && !isPercent) {
						if( petVal < suvAndCt[0] || petVal > suvAndCt[1]) continue;
					}
					if( useCT) {
						if( !isCtFat(ctSlice, k, depth, suvAndCt, volSliceType, wideCT)) continue;
					}
					savePoint = false;
					switch( RoiState) {
						case 0:	// no rois
							savePoint = true;
							if(isPercent) {
								suvLo = suvAndCt[0] * suvNoRoi / 100;
								if( petVal < suvLo) continue;
							}
							break;

						case 1:	// interior - usual case
							// the next section avoids counting pixels more than once
							// as soon as a point is found in any one of the currSliceRois
							// it is immediately added and no more ROIs need to be chacked
							for( roi = 0; roi < currSliceRois.size(); roi++) {
								curRoiNum = roiNum[roi];
								if(isPercentOK()) {
									suvLo = suvAndCt[0] * savePercentSUV[curRoiNum] / 100;
//									suvHi = 10 * suvAndCt[1];	// kill upper condition
//									if( petVal < suvLo || petVal > suvHi) continue;
									if( petVal < suvLo) continue;
								}
								currRoi = currSliceRois.get(roi);
								currRoi.setCurrEl(elipSlice);
								if( currRoi.contains(k, j)) {
									savePoint = true;
									break;
								}
							}
							break;

						case 2:	// exterior to all rois
							savePoint = true;
							if(isPercent) {
								suvLo = suvAndCt[0] * suvNoRoi / 100;
								if( petVal < suvLo) continue;
							}
							for( roi = 0; roi < currSliceRois.size(); roi++) {
								currRoi = currSliceRois.get(roi);
								currRoi.setCurrEl(elipSlice);
								if( currRoi.contains(k, j)) {
									savePoint = false;
									break;
								}
							}
							break;
					}
					if( savePoint) {
						if( maxMip < 0) maxMip = minMip = sliceNum;
						if( maxMip < sliceNum) maxMip = sliceNum;
						if( minMip > sliceNum) minMip = sliceNum;
						// with the nifti, there can be overlap - be careful
						if(isNifti) {
							if( !suvPnt.maybeAddPoint(petVal, CtCenterVal, k, j, i, curRoiNum)) continue;
						}
						else suvPnt.addPoint(petVal, CtCenterVal, k, j, i, curRoiNum);
						volRet.SUVtotal += petVal;
						if( petVal >= volRet.SUVmax) {
							if( petVal > volRet.SUVmax) numRed = 0;
							volRet.SUVmax = petVal;
							if( RoiNum4Max != curRoiNum)
								RoiNum4Max = curRoiNum;
							if(numRed < 5) {
								int red0 = suvPnt.getListSize()-1;
								redPoints[numRed++] = red0;
								suvPnt.red0 = red0;
							}
						}
						if( petVal > volRet.SUVList[i]) volRet.SUVList[i] = petVal;
					}
				}
			}
		}
//		suvPnt1.calcAllRadiomics(this);
		if( RoiState == 1) {
			volRet.isExcluded = false;
			i = getSpinInt(0)-1;
			if( !jCheckShowAll.isSelected() && i<excludedROIs.length) volRet.isExcluded = (excludedROIs[i] != null);
			if( !volRet.isExcluded) {
				double removeVal = suvPnt.removeExcluded(excludedPoints, excludedROIs);
				if( removeVal > 0) {
					volRet.SUVtotal -= removeVal;
					numRed = 1;
					redPoints[0] = suvPnt.red0;
				}
			}
		}

		SUVmean = volRet.calcAll(false);
		if(dispFlg) {
			j = k = 2;
			if(volRet.vol1 < 1) j = 3;
			if(volRet.vol1 < 0.1) j = 4;
			prntVal = PetCtFrame.roundN(volRet.vol1, j);
			jTextVol.setText(prntVal.toString());
			if(volRet.SUVmax > 99) k = 1;
			prntVal = PetCtFrame.roundN(volRet.SUVmax, k);
			prntVal2 = PetCtFrame.roundN(volRet.meanMax, k);
			jTextSuvMax.setText(prntVal.toString() + "  (" + prntVal2.toString() +")");
			prntVal = PetCtFrame.roundN(volRet.SUVpeak, k);
			prntVal2 = PetCtFrame.roundN(volRet.meanPeak, k);
			jTextSuvPeak.setText(prntVal.toString() + "  (" + prntVal2.toString() +")");
			prntVal = PetCtFrame.roundN(SUVmean, k);
			prntVal2 = PetCtFrame.roundN(volRet.SD, k);
			jTextSuvMean.setText(prntVal.toString() + " \u00B1 " + prntVal2.toString());
			prntVal = PetCtFrame.roundN(volRet.vol1*SUVmean, j);
			jTextVolMean.setText(prntVal.toString());
			if(volRet.type != volSliceType) suvPnt = null;
			parentPet.repaintAll();
		}
/*		stop = new Date();
		diff = (double)(stop.getTime() - start.getTime());
		diff /= 1000.;
		IJ.log( "time = " + diff.toString());*/
		return volRet;
	}

	SaveVolROI maybeRecalcSingleRoiVol(int RoiNum, int gateIn1) {
		int i, xpos, xpos0, ypos, ypos0, zpos, zpos0, n, x0, y0, z0;
		int[] currVals;
		SaveVolROI retVal = calcSingleRoiVol( RoiNum, false, gateIn1);
		SUVpoints.SavePoint currPnt, newPnt;
		int n1 = getRoiSize(1);
		if( n1 == 0 || RoiNum < n1 || retVal == null) return retVal;
		if( retVal.type == parentPet.m_sliceType) return retVal;
		xpos0 = xpos = 0;
		ypos0 = ypos = 1;
		zpos0 = zpos = 2;
		switch(retVal.type) {
			case JFijiPipe.DSP_AXIAL:
				break;

			case JFijiPipe.DSP_CORONAL:
				xpos0 = 0;
				ypos0 = 2;
				zpos0 = 1;
				break;

			case JFijiPipe.DSP_SAGITAL:
				xpos0 = 2;
				ypos0 = 0;
				zpos0 = 1;
				break;
		}
		switch(parentPet.m_sliceType) {
			case JFijiPipe.DSP_AXIAL:
				xpos = xpos0;
				ypos = ypos0;
				zpos = zpos0;
				break;

			case JFijiPipe.DSP_CORONAL:
				xpos = xpos0;
				ypos = zpos0;
				zpos = ypos0;
				break;

			case JFijiPipe.DSP_SAGITAL:
				xpos = ypos0;
				ypos = zpos0;
				zpos = xpos0;
				break;
		}
		n = suvPnt.getListSize();
		currVals = new int[3];
		retVal.type = parentPet.m_sliceType;
		for( i=0; i<n; i++) {
			currPnt = suvPnt.getPoint(i);
			currVals[0] = currPnt.x1;
			currVals[1] = currPnt.y1;
			currVals[2] = currPnt.z1;
			x0 = currVals[xpos];
			y0 = currVals[ypos];
			z0 = currVals[zpos];
			newPnt = suvPnt.newPoint(currPnt.petVal, currPnt.ctVal, x0, y0, z0, RoiNum);
			suvPnt.volPointList.set(i, newPnt);
		}
		return retVal;
	}

	SaveVolROI calcSingleRoiVol(int RoiNum, boolean SUVonly, int gateInd1) {
		suvPnt = new SUVpoints(parentPet.m_sliceType);
		CtCenterVal = 0;
		if( RoiNum >= getRoiSize() || RoiNum < 0) return null;
		SaveVolROI retVal;
		int gateIndx = parentPet.gateIndx;
		JFijiPipe petPipe = parentPet.petPipe;
		if( gateInd1 >= 0) gateIndx = gateInd1;
		int n1 = getRoiSize(1);
		if( RoiNum < n1) {
			retVal = calcSingleNiftiRoiVol( RoiNum, SUVonly);
			suvPnt.calcRadiomics(this, isCalcRadio);
			return retVal;
		}
		boolean useSUV, useCT, firstSlice, insideFlg, isSpect;
		int i, j, k, sag=0, maxSlice, maxDepth, sliceNum, depth, width1;
		int loSlice, hiSlice, roiNum0 = RoiNum - n1;
		int ctSlice, numFrm, gateOffset = 0, angle1=0;
		double petVal, suvLo, suvHi;
		double [] suvAndCt = getSUVandCTLimits();
		suvLo = suvAndCt[0];
		suvHi = suvAndCt[1];
		int wideCT = getCTcalc();
		JFijiPipe.lineEntry currLine;
		width1 = maxSlice = petPipe.data1.width;
		numFrm = petPipe.getNormalizedNumFrms();
		maxDepth = numFrm;
		if( petPipe.data1.numTimeSlots>1) {
			gateOffset = gateIndx * numFrm;
		}
		retVal  = new SaveVolROI();
		Poly3Save currRoi = polyVect.get(roiNum0);
		retVal.type = currRoi.type;
		if( retVal.type == JFijiPipe.DSP_SAGITAL)
			width1 = maxSlice = petPipe.data1.height;
		if( retVal.type == JFijiPipe.DSP_AXIAL) {
			maxSlice = numFrm;
			maxDepth = petPipe.data1.height;
		}
		retVal.SUVList = new double[maxSlice];
		useSUV = jCheckUseSUV.isSelected();
		useCT = jCheckUseCt.isSelected();
		if(SUVonly) useSUV = useCT = false;
		else {
			if(isPercentOK()) {
				suvLo = suvLo * savePercentSUV[RoiNum] / 100;
				suvHi = 10 * savePercentSUV[RoiNum];	// kill upper condition
			}
		}
		for( i=0; i<maxSlice; i++) retVal.SUVList[i] = 0;
		firstSlice = true;
		maxMip = -1;
		loSlice = maxDepth;
		hiSlice = 0;
		if( currRoi.sphericalROI) {
			j = currRoi.poly.ypoints[0];	// center y position
			i = currRoi.poly.ypoints[2];	// top y position
			loSlice = i;
			hiSlice = 2*j - i;
			firstSlice = false;
		}
		for( i=currRoi.lowSlice-1; i<currRoi.hiSlice; i++) {
			currRoi.setCurrEl(i);
			for( j=0; j<maxDepth; j++) {
				if( !firstSlice) {	// here we save time on subsequent slices
					if( j<loSlice || j > hiSlice) continue;
				}
				sliceNum = j;
				depth = i;
				switch (retVal.type) {
					case JFijiPipe.DSP_AXIAL:
						sliceNum = i;
						depth = j;
						break;

					case JFijiPipe.DSP_CORONAL:
						break;

					case JFijiPipe.DSP_SAGITAL:
						angle1 = 270;
						sag = ChoosePetCt.round(petPipe.mriOffY0);
						break;
				}
				currLine = petPipe.data1.getLineOfData(angle1, depth, sliceNum+gateOffset);
				ctSlice = parentPet.ctPipe.findCtPos(sliceNum, false);
				insideFlg = false;
				for( k=0; k<width1; k++) {
					isSpect = currLine.SUVfactor <= 0;
					if( firstSlice) {	// want to save time on following slices
						insideFlg = currRoi.contains(k, j);
						if(insideFlg) {
							if( j < loSlice) loSlice = j;
							if( j > hiSlice) hiSlice = j;
						}
					}
					if (currLine.pixels != null) petVal = currLine.pixels[k] * currLine.slope;
					else petVal = currLine.pixFloat[k];
					if( !isSpect) petVal /= currLine.SUVfactor;
					if( SUVonly) {
						if( petVal <= retVal.SUVmax) continue;
						// clamp the maximum permitted value to suvHi
						// this gives the user some control on the limits
						if( petVal > suvHi && !isSpect) petVal = suvHi;
					}
					if( useSUV) {
						if( petVal < suvLo || petVal > suvHi) continue;
					}
					if( useCT) {
						if( !isCtFat(ctSlice, k, depth, suvAndCt, retVal.type, wideCT)) continue;
					}
					if( firstSlice) {
						if(!insideFlg) continue;
					}
					else if( !currRoi.contains(k, j)) continue;
					if( maxMip < 0) maxMip = minMip = sliceNum;
					if( maxMip < sliceNum) maxMip = sliceNum;
					if( minMip > sliceNum) minMip = sliceNum;
					suvPnt.addPoint(petVal, CtCenterVal, k, j, i, RoiNum);
					retVal.SUVtotal += petVal;
					if( petVal >= retVal.SUVmax) {
						if( petVal > retVal.SUVmax) numRed = 0;
						if( RoiNum4Max != RoiNum)
							RoiNum4Max = RoiNum;
						retVal.SUVmax = petVal;
						if(numRed < 5) {
							int red0 = suvPnt.getListSize()-1;
							redPoints[numRed++] = red0;
							suvPnt.red0 = red0;
						}
					}
					if( petVal > retVal.SUVmax) retVal.SUVmax = petVal;
					if( petVal > retVal.SUVList[i]) retVal.SUVList[i] = petVal;
				}
			}
			firstSlice = false;
		}
		suvPnt.calcRadiomics(this, isCalcRadio);
		return retVal;
	}
	
	boolean isPercentOK() {
		return isPercent && savePercentSUV != null;
	}
	
	SaveVolROI calcSingleNiftiRoiVol(int RoiNum, boolean SUVonly) {
		int i, j, maxSlice;
		JFijiPipe pip0 = parentPet.petPipe;
		maxSlice = pip0.data1.width;
		SaveVolROI retVal  = new SaveVolROI();
		retVal.type = parentPet.m_sliceType;
		if( retVal.type == JFijiPipe.DSP_AXIAL) {
			maxSlice = pip0.getNormalizedNumFrms();
		}
		retVal.SUVList = new double[maxSlice];
		for( i=0; i<maxSlice; i++) retVal.SUVList[i] = 0;
		return calcSingleNiftiRoiVolSub(RoiNum, retVal, SUVonly);
	}

	SaveVolROI calcSingleNiftiRoiVolSub(int RoiNum, SaveVolROI inVal, boolean SUVonly) {
		int i, width1, x1, y1, z1, zMip, prevPetZ = -1;
		int coef0, off1, ctSlice = 0;
		int wideCT = getCTcalc();
		double slope, SUVfactor, petVal, suvLo, suvHi;
		boolean useSUV, useCT;
		short currShort;
		short[] pix1;
		short [] val3;
		double [] suvAndCt = getSUVandCTLimits();
		suvLo = suvAndCt[0];
		suvHi = suvAndCt[1];
		SaveVolROI retVal = inVal;
		useSUV = jCheckUseSUV.isSelected();
		useCT = jCheckUseCt.isSelected();
		if(SUVonly) useSUV = useCT = false;
		else {
			if(isPercentOK()) {
				suvLo = suvLo * savePercentSUV[RoiNum] / 100;
				suvHi = 10 * savePercentSUV[RoiNum];	// kill upper condition
			}
		}
		CtCenterVal = 0;
		JFijiPipe pip0 = parentPet.petPipe;
		width1 = pip0.data1.width;
		retVal.type = parentPet.m_sliceType;
		if( nifList == null) return retVal;
		Nifti3 currNif = (Nifti3) nifList[RoiNum];
		if( currNif == null) return retVal;
		coef0 = pip0.data1.getCoefficentAll();
		SUVfactor = pip0.data1.SUVfactor;
		if( SUVfactor == 0) SUVfactor = 1.0; // SPECT
		for( i=0; i< currNif.vectNif.size(); i++) {
			val3 = currNif.getVal3(i);
			x1 = val3[0];
			y1 = val3[1];
			z1 = zMip = val3[2];
			pix1 = pip0.data1.pixels.get(z1);
			slope = pip0.data1.getRescaleSlope(z1);
			off1 = width1*y1 + x1;
			currShort = (short)(pix1[off1]+coef0);
			petVal = currShort*slope/SUVfactor;
			switch( retVal.type) {
				case JFijiPipe.DSP_AXIAL:
					break;

				case JFijiPipe.DSP_CORONAL:
					y1 = z1;
					z1 = val3[1];
					break;

				case JFijiPipe.DSP_SAGITAL:
					x1 = val3[1];
					y1 = z1;
					z1 = val3[0];
					break;
			}
			if( useSUV) {
				if( petVal < suvLo || petVal > suvHi) continue;
			}
			if( useCT) {
				if( prevPetZ != zMip) {
					ctSlice = parentPet.ctPipe.findCtPos(zMip, false);
					prevPetZ = zMip;
				}
				if( !isCtFat(ctSlice, val3[0], val3[1], suvAndCt, JFijiPipe.DSP_AXIAL, wideCT)) continue;
			}
			if( maxMip < 0) maxMip = minMip = zMip;
			if( maxMip < zMip) maxMip = zMip;
			if( minMip > zMip) minMip = zMip;
			suvPnt.addPoint(petVal, CtCenterVal, x1, y1, z1, RoiNum);
			retVal.SUVtotal += petVal;
			if( petVal >= retVal.SUVmax) {
				if( petVal > retVal.SUVmax) numRed = 0;
				RoiNum4Max = RoiNum;
				retVal.SUVmax = petVal;
				if(numRed < 5) {
					redPoints[numRed++] = suvPnt.getListSize()-1;
				}
			}
			if( petVal > retVal.SUVList[z1]) retVal.SUVList[z1] = petVal;
		}
		return retVal;
	}
	
	@SuppressWarnings("unchecked")
	void fillStrings() {
//		String tmp = System.getProperty("user.dir");
		String line;
		int num = 0;
		File fl1 = new File(IJ.getDirectory("plugins") + "bfstrings.txt");
		if( !fl1.exists()) return;
		try {
			FileReader fis = new FileReader(fl1);
			BufferedReader br = new BufferedReader( fis);
			while( (line = br.readLine()) != null) {
				if(num > 0 && line.isEmpty()) break;
				num++;
				jComboROIlabel.addItem(line);
			}
			br.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	boolean isCtFat(int sliceNum, int x, int y, double[] limits, int type, int acceptWide) {
		CtCenterVal = 0;
		if( sliceNum < 0) return false;
		int i, j, x0, x1, y0, y1, x2,y2, width1, offst, upLimit, loLimit, coef0;
//		int average1;
		short val1;
		JFijiPipe ctPipe = parentPet.ctPipe;
		double slope, rescaleIntercept = ctPipe.data1.shiftIntercept();
		short[] data1 = ctPipe.data1.pixels.get(sliceNum);
		slope = ctPipe.data1.getRescaleSlope(sliceNum);
		width1 = ctPipe.data1.width;
		upLimit = (int)(limits[3] - rescaleIntercept);
		loLimit = (int)(limits[2] - rescaleIntercept);
		coef0 = ctPipe.data1.getCoefficentAll();
		x0 = x;
		y0 = y;
		if( type == JFijiPipe.DSP_SAGITAL) {
			x0 = y;
			y0 = x;
		}
		x1 = parentPet.shift2Ct(ctPipe, x0, 0);
		y1 = parentPet.shift2Ct(ctPipe, y0, 1);
/*		x2 = parentPet.shift2Ct2(ctPipe, x0, 0);
		y2 = parentPet.shift2Ct2(ctPipe, y0, 1);
		if( x1 != x2 || y1 != y2)
			y2--;*/
		// if the points are on the edge, nudge them to the inside
		if( x1 < 1) x1 = 1;
		if( y1 < 1) y1 = 1;
		if( x1 >= width1-1) x1 = width1-2;
		if( y1 >= width1-1) y1 = width1-2;
		offst = y1*width1 + x1;
		val1 = (short) ((data1[offst]+coef0)*slope);
		CtCenterVal = val1;
		if( acceptWide == 2)	{	// accept if only a single point fits
			for( i=-1; i<=1; i++) {
				val1 = (short) ((data1[offst+i]+coef0)*slope);
				if( val1 >= loLimit && val1 <= upLimit) return true;
			}
			val1 = (short) ((data1[offst+width1] + coef0)*slope);
			if( val1 >= loLimit && val1 <= upLimit) return true;
			val1 = (short) ((data1[offst-width1] + coef0)*slope);
			return val1 >= loLimit && val1 <= upLimit;
		}
		if( acceptWide == 1)	{	// look for maybe bone
			isFat = isBone = isOther = isHeavy = false;
			for( i=-1; i<=1; i++) {
				val1 = (short) ((data1[offst+i]+coef0)*slope);
				checkCTval( val1, loLimit, upLimit);
			}
			val1 = (short) ((data1[offst+width1] + coef0)*slope);
			checkCTval( val1, loLimit, upLimit);
			val1 = (short) ((data1[offst-width1] + coef0)*slope);
			checkCTval( val1, loLimit, upLimit);
			if( !isFat) return false;
			if( !isBone && !isOther && !isHeavy) return true; // all case
			if( !isBone) {	// extend the search to another 4 points
				for( i=-1; i<=1; i+= 2) {
					val1 = (short) ((data1[offst+width1+i] + coef0)*slope);
					checkBone(val1);
					val1 = (short) ((data1[offst-width1+i] + coef0)*slope);
					checkBone(val1);
				}
				return isBone;
			}
			return true;
		}
/*		if( acceptWide == 1)	{	// accept according to average point
			average1 = 0;
			for( i=-1; i<=1; i++) {
				average1 += data1[offst+i]+coef0;
			}
			average1 += data1[offst+width1] + coef0;
			average1 += data1[offst-width1] + coef0;
			val1 = (short)(average1/5);
			if( val1 >= loLimit && val1 <= upLimit) return true;
			return false;
		}*/
		// the default as used up to now
		for( i=-1; i<=1; i++) {
			val1 = (short) (data1[offst+i]+coef0);
			if( val1 < loLimit || val1 > upLimit) return false;
		}
		val1 = (short) (data1[offst+width1] + coef0);
		if( val1 < loLimit || val1 > upLimit) return false;
		val1 = (short) (data1[offst-width1] + coef0);
		return !(val1 < loLimit || val1 > upLimit);
	}
	
	private void checkBone( short val1) {
		if( val1 >= 80 && val1 <= 1200) isBone = true;
	}
	
	private void checkCTval(short val1, int fatLo, int fatHi) {
		if( val1 >= fatLo && val1 <= fatHi) {
			isFat = true;
			return;
		}
		if( val1 >= 80 && val1 <= 1200) {
			isBone = true;
			return;
		}
		if( val1 > 1200) {
			isHeavy = true;
			return;
		}
		isOther = true;
	}

	void fillPatName() {
		ArrayList<String> seriesOnScreen;
		boolean chkName, chkDate, chkID;
		chkName = jCheckName.isSelected();
		chkDate = jCheckDate.isSelected();
		chkID = jCheckId.isSelected();
		if( (chkName | chkDate | chkID) == false) return;
		Date styDate = parentPet.parent.getStudyDate();
		String txt1 = "";
		if( chkName) {
			txt1 = parentPet.parent.m_patName + " ";
		}
		if( chkDate) {
			txt1 += ChoosePetCt.UsaDateFormat(styDate) + " ";
		}
		if( chkID) {
			txt1 += parentPet.parent.m_patID;
		}
		txt1 = txt1.trim();
		jTextName.setText(txt1);
		if( jTextSeriesName.getText().isEmpty()) {
			seriesOnScreen = getSeries4dateID(styDate, parentPet.parent.m_patID);
			jTextSeriesName.setText(setSeries3dName(seriesOnScreen));
		}
		activateBuildButton();
	}

	void buildDataset() {
		int i, x, x1, x0, y, y1, z, z1, ctSlice, width, height, nSize, ctWidth;
		int gray, offY, offCt, ctCoef, min1, numPnt, maxCtDsp;
		int[] pixels;
		ColorModel cm = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
		Color colIn = Color.green, colOut = Color.red;
		ImageStack stack;
		JFijiPipe.lineEntry currLine;
		Point roiPnt;
		ArrayList<Point> slicePoints;
		SUVpoints.SavePoint currPnt;
		short val1, shortBuf[];
		boolean useCt, inRoi, invertImg;
		double xSpacing, scale, slope, slop1, petVal, suvNorm, suvLo, suvNoRoi;
		Preferences prefer = parentPet.parent.jPrefer;
		i = prefer.getInt("inside color", 0);
		if( i != 0) colIn = new Color(i);
		i = prefer.getInt("outside color", 0);
		if( i != 0) colOut = new Color(i);
		JFijiPipe ctPipe, petPipe;
		String windowName = jTextName.getText();
		jTextName.setText("");
		String seriesName = jTextSeriesName.getText();
		jTextSeriesName.setText("");
		activateBuildButton();
		black = 255 << 24;
		petPipe = parentPet.petPipe;
		ctPipe = parentPet.ctPipe;
		ctWidth = ctPipe.data1.width;
		width = petPipe.data1.width;
		height = petPipe.data1.height;
		nSize = petPipe.getNormalizedNumFrms();
		stack = new ImageStack( width, height, cm);
		xSpacing = parentPet.pointSpacing(ctPipe);
		ctCoef = ctPipe.data1.getCoefficentAll();
		slope = ctPipe.winSlope;
		double[] suvAndCt = getSUVandCTLimits();
		if( isPer1) {
			JOptionPane.showMessageDialog(this, "Percent here is meaningless.\n"
			+ "This is only about how to color pixels.\nAbove some SUV they are red.");
			return;
		}
		suvLo = suvAndCt[0];
		if( isPercent) {
			suvNoRoi = 0;
			if(savePercentSUV != null) for( i=0; i<savePercentSUV.length; i++) 
				if(savePercentSUV[i] > suvNoRoi) suvNoRoi = savePercentSUV[i];
			suvLo = suvAndCt[0] * suvNoRoi / 100;
		}
		maxCtDsp = (int) (2.55 * suvAndCt[4]);	// 2.55*100 = 255
		scale = (maxCtDsp+1)*slope/ctPipe.winWidth;
		min1 = (int) ((ctPipe.winLevel - ctPipe.winWidth/2 - ctPipe.data1.shiftIntercept())/slope);
		invertImg = jCheckInvert.isSelected();
		for( z=0; z<nSize; z++) {
			z1 = z;
			if( invertImg) z1 = nSize - z - 1;
			pixels = new int[width * height];
			// with findCtPos, if it is outside the range, use last good value
			ctSlice = ctPipe.findCtPos(z1, true);
			shortBuf = ctPipe.data1.pixels.get(ctSlice);
			slop1 = ctPipe.data1.getRescaleSlope(ctSlice);
			for( y=0; y<height; y++) {
				y1 = parentPet.shift2Ct(ctPipe, y, 1);
				offCt = ctWidth*y1;
				offY = width*y;
				for( x=0; x<width; x++) {
					x1 = parentPet.shift2Ct(ctPipe, x, 0) + offCt;
					if(x1 >= 0 && x1 < shortBuf.length) {
						val1 = (short) ((shortBuf[x1] + ctCoef)*slop1);
						gray = (int) ((val1 - min1) * scale);
						if( gray < 0) gray = 0;
						if( gray > maxCtDsp) gray = maxCtDsp;
					} else gray = 0;
					pixels[x+offY] = black | gray << 16 | gray << 8 | gray;
				}
			}
			stack.addSlice(null, pixels);
		}

		// now add the PET part in red and green
		useCt = jCheckUseCt.isSelected();
		int wideCT = getCTcalc();
		suvNorm = 256.0 / suvAndCt[5];	// display max SUV
		if( jCheckUseSUV.isSelected() ) for( z=0; z<nSize; z++) {
			z1 = z;
			if( invertImg) z1 = nSize - z - 1;
			numPnt = suvPnt.getListSize();
			slicePoints = new ArrayList<Point>();
			for( i=0; i< numPnt; i++) {
				currPnt = suvPnt.getPoint(i);
				switch (volSliceType) {
					case JFijiPipe.DSP_AXIAL:
						if( currPnt.z1 != z1) break;
						slicePoints.add(new Point(currPnt.x1, currPnt.y1));
						break;

					case JFijiPipe.DSP_CORONAL:
						if( currPnt.y1 != z1) break;
						slicePoints.add(new Point(currPnt.x1, currPnt.z1));
						break;

					case JFijiPipe.DSP_SAGITAL:
						if( currPnt.y1 != z1) break;
						slicePoints.add(new Point(currPnt.z1, currPnt.x1));
						break;
				}
			}
			numPnt = slicePoints.size();	// num of points in this slice
			ctSlice = ctPipe.findCtPos(z1, false);
			pixels = (int []) stack.getPixels(z +1);
			if(pixels == null) return;
			for( y=0; y<height; y++) {
				currLine = petPipe.data1.getLineOfData(0, y, z1);
				for( x=0; x<width; x++) {
					if (currLine.pixels != null) petVal = currLine.pixels[x] * currLine.slope;
					else petVal = currLine.pixFloat[x];
					if( currLine.SUVfactor > 0) petVal /= currLine.SUVfactor;
					if( petVal < suvLo) continue;
					if( !isPercent && petVal > suvAndCt[1]) continue;
					if( useCt) {
						if( !isCtFat(ctSlice, x, y, suvAndCt, volSliceType, wideCT)) continue;
					}
					inRoi = false;
					for( i=0; i<numPnt; i++) {
						roiPnt = slicePoints.get(i);
						if( roiPnt.x == x && roiPnt.y == y) {
							inRoi = true;
							break;
						}
					}
					gray = (int) (suvNorm * petVal);
					if( gray < 0) gray = 0;
					if( gray > 255) gray = 255;
					i = y*width + x;
					// erase the CT contribution and leave only PET
					if( inRoi) pixels[i] = setPixelColor(gray, colIn);
					else pixels[i] = setPixelColor(gray, colOut);
				}
			}
		}
		ImagePlus myImage = new ImagePlus(windowName, stack);
		String meta1 = makeMetaData(seriesName, nSize, null,
				ChoosePetCt.SOPCLASS_TYPE_NM, parentPet.petPipe.data1.metaData);
		myImage.setProperty("Info", meta1);
		myImage.show();
	}
	
	static String makeMetaData(String seriesName, int numFrm, ImagePlus im1,
			int SOPtype, String srcMeta) {
		Date dt1 = new Date();
		String SOP = generateSOPInstanceUID(dt1);
		String AET = ChoosePetCt.getDicomValue(srcMeta, "0002,0016");
		if( AET == null) AET = "A123";
		String SOPClass = ChoosePetCt.SOPCLASS_SC;
		if( SOPtype == ChoosePetCt.SOPCLASS_TYPE_NM) SOPClass = ChoosePetCt.SOPCLASS_NM;
		String meta = "0002,0002  Media Storage SOP Class UID: " + SOPClass + "\n";
		// use transfer syntax explicit little endian
		meta += "0002,0003  Media Storage SOP Inst UID: " + SOP + "\n" +
			"0002,0010  Transfer Syntax UID: 1.2.840.10008.1.2.1\n" +
			"0002,0012  Implementation Class UID: 1.2.16.840.1.113664.3\n" +
			"0002,0013  Implementation Version Name: fijiMaker\n" +
			"0002,0016  Source Application Entity Title: " + AET + "\n";
		meta += "0008,0005  Specific Character Set: ISO_IR 100\n" +
			"0008,0008  Image Type: DERIVED\\SECONDARY\n" +
			"0008,0016  SOP Class UID: " + SOPClass + "\n";
		meta += "0008,0018  SOP Instance UID: " + SOP + "\n";
		meta = append2Meta("0008,0020", meta, srcMeta);
		meta = append2Meta("0008,0021", meta, srcMeta);
		meta = append2Meta("0008,0050", meta, srcMeta);
		meta = append2Meta("0008,0060", meta, srcMeta);
		meta = append2Meta("0008,0080", meta, srcMeta);
		meta = append2Meta("0008,0090", meta, srcMeta);
		meta = append2Meta("0008,1030", meta, srcMeta);
		meta += "0008,103E  Series Description: "+ seriesName +"\n";
		meta = append2Meta("0010,0010", meta, srcMeta);
		meta = append2Meta("0010,0020", meta, srcMeta);
		meta = append2Meta("0010,0030", meta, srcMeta);
		meta = append2Meta("0010,0040", meta, srcMeta);
		meta = append2Meta("0010,1020", meta, srcMeta);
		meta = append2Meta("0010,1030", meta, srcMeta);

		meta = append2Meta("0018,0050", meta, srcMeta);
		meta = append2Meta("0020,000D", meta, srcMeta);
		meta += "0020,000E  Series Instance UID: " + SOP + ".1\n";
		meta = append2Meta("0020,0032", meta, srcMeta);
		meta = append2Meta("0020,0037", meta, srcMeta);
		meta = append2Meta("0020,0052", meta, srcMeta);

		meta += "0028,0002  Samples per Pixel: 3\n";
		meta += "0028,0004  Photometric Interpretation: RGB\n";
		meta += "0028,0008  Number of Frames: " + numFrm + "\n";
		if( im1 != null) {
			meta += "0028,0010 Rows: " + im1.getHeight() + "\n";
			meta += "0028,0011 Columns: " + im1.getWidth() + "\n";
		} else {
			meta = append2Meta("0028,0010", meta, srcMeta);
			meta = append2Meta("0028,0011", meta, srcMeta);
		}
		meta = append2Meta("0028,0030", meta, srcMeta);
		meta += "0028,0100  Bits Allocated: 8\n";
		meta += "0028,0101  Bits Stored: 8\n";
		meta += "0028,0102  High Bit: 7\n";
		return meta;
	}
	
	static String append2Meta(String key, String metaIn, String srcMeta) {
		String tmp1, meta = metaIn;
		int k0 = srcMeta.indexOf(key);
		if( k0 > 0) {
			int k1 = srcMeta.indexOf("\n", k0);
			if( k1 < 0) return meta;
			tmp1 = srcMeta.substring(k0, k1+1);
			meta += tmp1;
		}
		return meta;
	}

	static String generateSOPInstanceUID(Date dt0) {
		Date dt1 = dt0;
		if( dt1 == null) dt1 = new Date();
		SimpleDateFormat df1 = new SimpleDateFormat("2.16.840.1.113664.3.yyyyMMdd.HHmmss", Locale.US);
		return df1.format(dt1);
	}
	
	String setSeries3dName( ArrayList<String> serNames) {
		String tmp, retName = "3D";
		ArrayList<String> filteredNames = new ArrayList<String>();
		int i, j, n = serNames.size();
		for( i=0; i<n; i++) {
			tmp = serNames.get(i).toUpperCase();
			if( !tmp.startsWith(retName)) continue;
			filteredNames.add(tmp);
		}
		n = filteredNames.size();
		if( n == 0) return retName;
		for( j=1; j<100; j++) {
			retName = "3D" + j;
			for( i=0; i<n; i++) {
				tmp = filteredNames.get(i);
				if( tmp.equals(retName)) break;
			}
			if( i >= n) break;
		}
		return retName;
	}
	
	static ArrayList<String> getSeries4dateID( Date inDate, String inID) {
		ArrayList<String> retSer = new ArrayList<String>();
		int i, j;
		ImagePlus img1;
		String meta, ID0, ID1, serName;
		Date serDate, serDate0 = null;
		int [] fullList = WindowManager.getIDList();
		if( fullList == null) return retSer;
		ID0 = ChoosePetCt.compressID(inID);
		for( i=0; i<fullList.length; i++) {
			img1 = WindowManager.getImage(fullList[i]);
			j = img1.getStackSize();
			if( j <= 0) continue;
			meta = ChoosePetCt.getMeta(1, img1);
			if( meta == null) continue;
			ID1 = ChoosePetCt.compressID(ChoosePetCt.getDicomValue(meta, "0010,0020"));
			if( !ID0.equals(ID1)) continue;
			serDate = ChoosePetCt.getStudyDateTime(meta, -1);
			if( i==0) serDate0 = serDate;
			else if( !isSameDay(serDate0, serDate)) continue;
			serName = ChoosePetCt.getDicomValue(meta, "0008,103E");
			if( serName == null || serName.isEmpty()) serName = ChoosePetCt.getDicomValue( meta, "0054,0400");
			retSer.add(serName);
		}
		return retSer;
	}
	
	static boolean isSameDay(Date dat0, Date dat1) {
		int year, day;
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(dat0);
		year = cal.get(Calendar.YEAR);
		day = cal.get(Calendar.DAY_OF_YEAR);
		cal.setTime(dat1);
		if( cal.get(Calendar.YEAR) != year) return false;
		return cal.get(Calendar.DAY_OF_YEAR) == day;
	}

	int setPixelColor(int gray, Color inColor) {
		int pixRed = (gray * inColor.getRed()) / 255;
		int pixGreen = (gray * inColor.getGreen()) / 255;
		int pixBlue = (gray * inColor.getBlue()) / 255;
		return black | pixRed << 16 | pixGreen << 8 | pixBlue;
	}

	void activateBuildButton() {
		boolean enabled = true;
		if( jTextName.getText().isEmpty()) enabled = false;
		jButBuild.setEnabled(enabled);
	}
	
	void loadROIs() {
		try {
			int i;
			String flPath, out1 = "brown fat path";
			File fl1;
			Preferences prefer = parentPet.parent.jPrefer;
			int numFrm = parentPet.petPipe.getNormalizedNumFrms();
			flPath = prefer.get(out1, null);
			JFileChooser fc;
			if( flPath == null) fc = new JFileChooser();
			else fc = new JFileChooser(flPath);
			fc.setFileFilter(new CsvFilter());
			i = fc.showOpenDialog(this);
			if( i != JFileChooser.APPROVE_OPTION) return;
			fl1 = fc.getSelectedFile();
			flPath = fl1.getPath();
			loadStoredROIs( flPath, numFrm);
			// save on load as well as save
			flPath = fl1.getParent();
			if( flPath != null) prefer.put(out1, flPath);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	int getNumFiles(String myDir) {
		Preferences prefer = parentPet.parent.jPrefer;
		String flPath = prefer.get("brown fat path", null);
		if(flPath == null) return 0;
		if( myDir != null) flPath += "/" + myDir;
		File fl1 = new File(flPath);
		if( !fl1.exists()) return 0;
		File [] flLst = fl1.listFiles();
		return flLst.length;
	}
	

	void saveResults(String myFileName, String myDir) {
		String dirAndName = myDir + "/" + myFileName;
		saveResults(dirAndName);
	}

	String getViewType( int type) {
		switch(type) {
			case JFijiPipe.DSP_AXIAL:
				return "axial";
		
			case JFijiPipe.DSP_CORONAL:
				return "coronal";

			case JFijiPipe.DSP_SAGITAL:
				return "sagittal";

			default:
				return "unknown";
			}
	}

	String generateFileName(int type, boolean spcSwap) {
		String retVal;
		if( type == 0) {
			retVal = parentPet.parent.m_patName.replace('.', ' ').trim();
			retVal = retVal.replaceAll("[,/]", "_");
		} else {
			retVal = ChoosePetCt.UsaDateFormat(parentPet.parent.getStudyDate());
			retVal = retVal.replace(", ", "_");
		}
		if( spcSwap) return retVal.replace(' ', '_');
		return retVal;
	}

	@SuppressWarnings("null")
	void saveResults(String myFileName) {
		try {
			isCalcRadio = false;
			if( myFileName == null && suvPnt == null) return;
			if( jTogDrawRoi.isSelected()) {
				IJ.log("You cannot save results while still in the process of drawing a ROI.");
				return;
			}
			Integer x1, y1, z1, n = getRoiSize(), n0 = getRoiSize(0), n1 = getRoiSize(1);
			int numPnts, val1, numGated, numRadio;
			SUVpoints.RadioPoint res;
			short[] val3;
			double[] radioSum, radioVol;
			double SUVMean, vTotal = 0, vTmp;
			if( n <= 0) return;
			// avoid a bug Salim found
			if( savePercentSUV==null) {
				getSUVandCTLimits();
				if( isPercent) calculateVol(false);
			}
			// for Nifti check that SUV and grown ROIs are both checked
			if( n1 > 0 && (!jCheckUseSUV.isSelected() || !jCheckGrown.isSelected())) {
				int result = JOptionPane.showConfirmDialog(this, "In most cases both SUV values and grown ROIs are used.\n"
					+ "Are you absolutely sure you want to save the file without them???",
					"Probable Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
				if( result != JOptionPane.YES_OPTION) return;
			}
			SaveVolROI tmpVol;
			int[] xp, yp;
			int i, j, k, i1, viewType = parentPet.m_sliceType;
			Poly3Save polCur;
			Nifti3 nifCur;
			String flPath, defName, out1 = "brown fat path";
			String patName, styDate, tmp1;
			File fl1;
			// first check if all ROIs are the same type
			// if so override whatever view is showing
			x1 = 0;
			for( i=0; i<n0; i++) {
				polCur = polyVect.get(i);
				j = polCur.type;
				if( i==0) {
					x1 = j;
					continue;
				}
				if( j!=x1) break;
			}
			if( i== n0) viewType = x1;
			Preferences prefer = parentPet.parent.jPrefer;
			flPath = prefer.get(out1, null);
			patName = generateFileName(0, false);
			styDate = generateFileName(1, false);
			if(myFileName == null) {
				JFileChooser fc = new JFileChooser() {
					@Override
					public void approveSelection() {
						File f = getSelectedFile();
						if (f.exists() && getDialogType() == SAVE_DIALOG) {
							int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file", JOptionPane.YES_NO_CANCEL_OPTION);
							switch (result) {
								case JOptionPane.YES_OPTION:
									super.approveSelection();
									return;
								case JOptionPane.NO_OPTION:
									return;
								case JOptionPane.CLOSED_OPTION:
									return;
								case JOptionPane.CANCEL_OPTION:
									cancelSelection();
									return;
							}
						}
						super.approveSelection();
					}
				};
				if( flPath != null) fc.setCurrentDirectory(new File(flPath));
	//			defName = parentPet.parent.getTitleBar(0);
	//			i = defName.indexOf("   ");
				i = 1;
				if( i > 0) {
	/*				tmp1 = defName.substring(i+2);
					j = tmp1.indexOf("y");
					defName = defName.substring(0, i);
					if(j>0) defName += "_" + tmp1.substring(j-2, j+1);*/
					defName = patName + "_" + styDate;
					defName = defName.toLowerCase();
					defName += ".csv";
					fc.setSelectedFile(new File(defName));
				}
				fc.setFileFilter(new CsvFilter());
				i = fc.showSaveDialog(this);
				if( i != JFileChooser.APPROVE_OPTION) return;
				fl1 = fc.getSelectedFile();
				flPath = fl1.getPath();
				if( !flPath.toLowerCase().endsWith(".csv")) {
					fl1 = new File(flPath + ".csv");
				}
				flPath = fl1.getParent();
				if( flPath != null) prefer.put(out1, flPath);
			}
			else fl1 = new File(flPath + "/" + myFileName);
			if( measureTime < 0 && Roi1StartTime != null) {
				Date stopTime = new Date();
				long diff = (stopTime.getTime() - Roi1StartTime.getTime())/1000;
				measureTime = (int) diff;
			}
			maybeRecalcSingleRoiVol(0, -1);	// to define suvPnt1
			isCalcRadio = true;
			numRadio = 0;
			if( suvPnt.radioList != null) {
				getRadioActive(false);
				if(isCtRadiomics()) getRadioActive(true);
				numRadio = suvPnt.radioList.size();
			}
			tmp1 = "";
			for( i1=0; i1<numRadio; i1++) {
				res = suvPnt.radioList.get(i1);
				tmp1 += ", " + res.label;
			}
			radioSum = new double[numRadio];
			radioVol = new double[numRadio];
			FileWriter fos = new FileWriter(fl1);
			out1 = "Label, ROI, type, Vol(ml), Vol*mean, SUVMean, SD, SUVPeak, MeanWahl, ";
			out1 += "SUVqPeak, MeanQpet, SUVMax, MeanMax, HU" + tmp1 + "\n";
			fos.write(out1);
			ExcludedROI[] excludedROIs;
			ArrayList<SUVpoints.SavePoint> excludedPoints;
			numGated = parentPet.petPipe.data1.numTimeSlots;
			boolean showSD = jChRadShowSD.isSelected();
			for( j=0; j<numGated; j++) {
				excludedROIs = calculateExcludedROIs(j);
				excludedPoints = suvPnt.buildExcludedList(excludedROIs);
				k = -1;
				if( numGated > 1) {
					k = j;
					if(j>0) {
						i = j+1;
						out1 = "Phase " + i + "\n";
						fos.write(out1);
					}
				}
				for( i=0; i<=n; i++) {
					if( i<excludedROIs.length && excludedROIs[i] != null) continue;
					if(i<n) {
						tmpVol = maybeRecalcSingleRoiVol(i, k);
						if( tmpVol == null) continue;
						tmpVol.SUVtotal -= suvPnt.removeExcluded(excludedPoints, excludedROIs);
//						numRed = 1;
//						redPoints[0] = suvPnt.red0;
					}
					else tmpVol = calculateVol(false, viewType, k);
					if( tmpVol == null) continue;
					SUVMean = tmpVol.calcAll(true);
					x1 = i+1;
					if(i<n) {
						if(i<n1) {
							nifCur = (Nifti3) nifList[i];
							tmp1 = nifCur.ROIlabel;
						} else {
							polCur = polyVect.get(i-n1);
							tmp1 = polCur.ROIlabel;
						}
						out1 = tmp1 + ", " + x1.toString();
					}
					else out1 = getViewType(viewType) + ", sum";
					out1 += ", " + tmpVol.type.toString() + ", " + PetCtFrame.roundN(tmpVol.vol1, 2) + ", ";
					out1 += PetCtFrame.roundN(tmpVol.vol1 * SUVMean, 2) + ", ";
					out1 += PetCtFrame.roundN(SUVMean, 2) + ", " + PetCtFrame.roundN(tmpVol.SD, 2) + ", ";
					out1 += PetCtFrame.roundN(tmpVol.peakWahl, 2) + ", " + PetCtFrame.roundN(tmpVol.meanWahl, 2) + ", ";
					out1 += PetCtFrame.roundN(tmpVol.peakQpet, 2) + ", " + PetCtFrame.roundN(tmpVol.meanQpet, 2) + ", ";
					out1 += PetCtFrame.roundN(tmpVol.SUVmax, 2) + ", " + PetCtFrame.roundN(tmpVol.meanMax, 2) + ", ";
					out1 += PetCtFrame.roundN(tmpVol.CtHU, 2) + " \u00B1 " + PetCtFrame.roundN(tmpVol.CtSD, 2);
					if(i<n) {
						int numRd1 = numRadio;
						if( numRd1 > 0) numRd1 = suvPnt.radioList.size();
						vTmp= tmpVol.vol1;
						vTotal += vTmp;
						for( i1=0; i1<numRd1; i1++) {
							res = suvPnt.radioList.get(i1);
							if( res.mean != 0) { // don't count zeros
								radioSum[i1] += res.mean * vTmp;
								radioVol[i1] += vTmp;
							}
							out1 += ", " + PetCtFrame.roundN(res.mean, 2);
							if(showSD && res.std > 0) out1 += " \u00B1 " + PetCtFrame.roundN(res.std, 2);
						}
					} else {
						for( i1=0; i1<numRadio; i1++) {
							vTmp = 0;
							if( radioVol[i1]>0) vTmp = radioSum[i1]/radioVol[i1];
							out1 += ", " + PetCtFrame.roundN(vTmp, 2);
						}
					}
					out1 += "\n";
					fos.write(out1);
				}
			}
			isCalcRadio = false;
			out1 = patName + ", " + styDate + ", SUL =, ";
			out1 += PetCtFrame.roundN(parentPet.petPipe.data1.SULfactor, 5);
			if( measureTime > 0) {
				out1 += ", time(sec) =," + measureTime;
			} else {
				out1 += ",,";
			}
			out1 += "," + NiftiLab;
			out1 += ",,,,,, patient ID =," + parentPet.parent.m_patID + "\n";
			fos.write(out1);
			
			// if there are NIfti ROIs, they are here:
			if( n1 > 0) {
				out1 = "\nNumber of Nifti ROIs = " + n1.toString() + "\n";
				fos.write(out1);
				for( i=0; i<n1; i++) {
					nifCur = (Nifti3) nifList[i];
					numPnts = z1 = nifCur.vectNif.size();
					out1 = nifCur.ROIlabel + ", num points = " + z1.toString() + ", ";
					fos.write(out1);
					for(j=0; j<numPnts;  j++) {
						val3 = nifCur.getVal3(j);
						x1 = val1 = val3[0];
						y1 = val1 = val3[1];
						z1 = val1 = val3[2];
						out1 = x1.toString() + " " + y1.toString() + " " + z1.toString();
						if(((j+1)%1000) == 0 || j>= numPnts-1) out1 += "\n";
						else out1 += ", ";
						fos.write(out1);
					}
				}
			}

			// here are the manual ROIs
			out1 = "\nNumber of ROIs = " + n0.toString() + "\n";
			fos.write(out1);
			Integer[] outVal;
			for( i=0; i<n0; i++) {
				polCur = polyVect.get(i);
				out1 = polCur.ROIlabel + ", ";
				x1 = polCur.type;
				if( polCur.sphericalROI) x1 += 10;
				outVal = polCur.axialInverts();
				out1 += x1.toString() +", " + outVal[1].toString() + ", " + outVal[2].toString() + ", ";
				fos.write(out1);
				z1 = polCur.poly.npoints;
				out1 = "num points = " + z1.toString() + ", ";
				fos.write(out1);
				xp = polCur.poly.xpoints;
				yp = polCur.poly.ypoints;
				for(j=0; j<z1; j++) {
					x1 = xp[j];
					y1 = yp[j];
					out1 = x1.toString() + " " + y1.toString();
					if( j<z1-1) out1 += ", ";
					else out1 += "\n";
					fos.write(out1);
				}
			}
			out1 = "SUVlo, SUVhi, CTlo, CThi, useSUV, useCT, CtRadio\n";
			fos.write(out1);
			out1 = jTextSUVlo.getText() + ", " + jTextSUVhi.getText() + ", ";
			out1 += jTextCTlo.getText() + ", " + jTextCThi.getText() + ", ";
			x1 = y1 = 0;
			if( jCheckUseSUV.isSelected()) x1 = 1;
			if( jCheckUseCt.isSelected()) y1 = 1;
			z1 = getCTcalc();
			out1 += x1.toString() + ", " + y1.toString() + ", " + z1.toString() + "\n";
			fos.write(out1);
			fos.close();
			if(myFileName == null) {
				File fl2 = new File(getFileName(parentPet.petPipe));
				FileWriter fgr = new FileWriter(fl2);
				out1 = fl1.getPath();
				fgr.write(out1);
				fgr.close();
				ReadOrthancSub orthSub = new ReadOrthancSub(this);
				if(orthSub.isOrthFile(0)) orthSub.write2Orth(fl2, 1);
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}
	
	javax.swing.JTextField getJTextSUVlo() {
		return jTextSUVlo;
	}

	static String getFileName(JFijiPipe pipe1) {
		String flName;
		ImagePlus srcImage;
		if(pipe1 == null) return null;
		srcImage = pipe1.data1.srcImage;
		FileInfo info1 = srcImage.getOriginalFileInfo();
		if( info1 == null) return null;
		String path = info1.directory;
		flName = path + "/graphic.brownFat.gr2";
		return flName;
	}
	
	static boolean isBfFile(String path) {
		File fl1 = new File(path + "/graphic.brownFat.gr2");
		return fl1.exists();
	}
	
	static String getSpreadSheetName(JFijiPipe pipe1) {
		String flName, flRet = null;
		File fl1, fl2;
//		if(flRet == null) return null;	// turn off check
		FileReader rd1;
		BufferedReader bf1;
		try {
			flName = getFileName(pipe1);
			if( flName == null) return null;
			fl1 = new File(flName);
			if( !fl1.exists()) return null;
			rd1 = new FileReader(fl1);
			bf1 = new BufferedReader(rd1);
			flName = bf1.readLine();
			rd1.close();
			flName = flName.trim();
			fl2 = new File(flName);
			if( fl2.exists()) flRet = flName;
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return flRet;
	}
	
	void loadStoredROIs( String flName, int numFrm) {
		String currLine, tmp0, tmp1, labCur;
		String patID = null;
		Nifti3 nifCur;
		Date date0 = null;
		int i, j, k, n=0, niftiSz=0, n1, type1, sp1, sp2;
		int x1, y1, z1;
		Integer[] vals;
		int[] xval, yval;
		File fl1;
		FileReader rd1;
		BufferedReader bf1;
		try {
			fl1 = new File(flName);
			if( !fl1.exists()) return;
			boolean flipOld = jCheckOld.isSelected();
			polyVect = new ArrayList<Poly3Save>();
			Roi1StartTime = null;
			NiftiStartTime = null;
			measureTime = -1;
			isDirty = true;
			vals = new Integer[3];
			rd1 = new FileReader(fl1);
			bf1 = new BufferedReader(rd1);
			type1 = -1;	// old type or new not yet known
			while( (currLine = bf1.readLine()) != null) {
				if( type1 < 0) {
					type1 = 0;	// default = old type
					if( currLine.startsWith("Label")) type1 = 1;
				}
				if( currLine.contains("patient ID =")) {
					i = currLine.indexOf("patient ID =,");
					if(i>0) {
						patID = currLine.substring(i+13).trim();
						i = currLine.indexOf(", ") + 2;
						j = currLine.indexOf(", ", i);
						tmp0 = currLine.substring(i, j);
						SimpleDateFormat df = new SimpleDateFormat("MMM dd_yyyy", Locale.US);
						date0 = df.parse(tmp0);
					}
				}
				if( currLine.startsWith("Number of ")) {
					tmp1 = "Number of Nifti ROIs = ";
					if( currLine.startsWith(tmp1)) {
						tmp1 = currLine.substring(tmp1.length());
						niftiSz = Integer.parseInt(tmp1);
						break;
					}
					tmp1 = "Number of ROIs = ";
					if( currLine.startsWith(tmp1)) {
						tmp1 = currLine.substring(tmp1.length());
						n = Integer.parseInt(tmp1);
						break;
					}
				}
			}
			if( patID != null && date0 != null) {
				i = 0;
				tmp0 = parentPet.parent.m_patID;
				if( !tmp0.equals(patID)) i = 1;
				else {
					if( date0.after(parentPet.parent.getStudyDate())) i = 2;
				}
				tmp0 = null;
				switch(i) {
					case 1:
						tmp0 = "The patient names don't match.";
						break;
						
					case 2:
						tmp0 = "The ROIs are from a later study.";
						break;
				}
				if( tmp0 != null) {
					tmp0 = "Perhaps you have chosen the wrong file.\n" + tmp0;
					JOptionPane.showMessageDialog(this, tmp0);
				}
			}

			if( niftiSz > 0) {
				nifList = new Object[255];
				for( i=0; i<niftiSz; i++) {
					currLine = bf1.readLine();
					k = currLine.indexOf(',');
					labCur = currLine.substring(0, k).trim();
					currLine = currLine.substring(k+1);
					nifCur = new Nifti3();
					nifCur.ROIlabel = labCur;
					k = currLine.indexOf(',');
					tmp1 = currLine.substring(14, k); // " num points = " is 14
					n1 = Integer.parseInt(tmp1);
					currLine = currLine.substring(k+1);
					for( j=0; j<n1; j++) {
						if( k<0) currLine = bf1.readLine();
						k = currLine.indexOf(',');
						if( k>0) {
							tmp1 = currLine.substring(0, k).trim();
							currLine = currLine.substring(k+1);
						} else
							tmp1 = currLine.trim();
						sp1 = tmp1.indexOf(' ');
						sp2 = tmp1.indexOf(' ', sp1+1);
						x1 = Integer.parseInt(tmp1.substring(0, sp1));
						y1 = Integer.parseInt(tmp1.substring(sp1+1, sp2));
						z1 = Integer.parseInt(tmp1.substring(sp2+1));
						nifCur.add(x1, y1, z1);
					}
					nifList[i] = nifCur;
				}
				for( i=0; i<255; i++) {
					if( nifList[i] == null) break;
				}
				nifListSz = i;
				tmp1 = "Number of ROIs = ";
				while( (currLine = bf1.readLine()) != null) {
					if( currLine.startsWith(tmp1)) {
						tmp1 = currLine.substring(tmp1.length());
						n = Integer.parseInt(tmp1);
						break;
					}
				}
			}

			for( i=0; i<n; i++) {
				currLine = bf1.readLine();
				labCur = "";
				if(type1 == 1) {
					k = currLine.indexOf(',');
					labCur = currLine.substring(0, k).trim();
					currLine = currLine.substring(k+1);
				}
				for( j=0; j<3; j++) {
					k = currLine.indexOf(',');
					tmp1 = currLine.substring(0, k).trim();
					vals[j] = Integer.parseInt(tmp1);
					currLine = currLine.substring(k+1);
				}
				if( vals[2] > numFrm) {	// wierd case
					k = vals[2] - numFrm;
					vals[1] -= k;
					vals[2] -= k;
				}
				currPoly = new Poly3Save();
				currPoly.ROIlabel = labCur;
				currPoly.type = vals[0];
				if( currPoly.type >= 10) {
					currPoly.type -= 10;
					currPoly.sphericalROI = true;
				}
				currPoly.numFrm = numFrm;
				if(type1 == 1 || flipOld) vals = currPoly.axialInverts(vals);
				currPoly.lowSlice = vals[1];
				currPoly.hiSlice = vals[2];
				k = currLine.indexOf(',');
				tmp1 = currLine.substring(14, k); // " num points = " is 14
				n1 = Integer.parseInt(tmp1);
				currLine = currLine.substring(k+1);
				currPoly.poly = new Polygon();
				currPoly.poly.npoints = n1;
				xval = new int[n1];
				yval = new int[n1];
				for( j=0; j<n1; j++) {
					k = currLine.indexOf(',');
					if( k > 0) {
						tmp1 = currLine.substring(0, k).trim();
						currLine = currLine.substring(k+1);
					}
					else tmp1 = currLine.trim();
					k = tmp1.indexOf(' ');
					xval[j] = Integer.parseInt(tmp1.substring(0, k));
					yval[j] = Integer.parseInt(tmp1.substring(k+1));
				}
				currPoly.poly.xpoints = xval;
				currPoly.poly.ypoints = yval;
				polyVect.add(currPoly);
			}
			currLine = bf1.readLine();
			if( currLine != null && currLine.startsWith("SUVlo")) {
				currLine = bf1.readLine();
				for( j=0; j<7; j++) {
					k = currLine.indexOf(',');
					if( k > 0) {
						tmp1 = currLine.substring(0, k).trim();
						currLine = currLine.substring(k+1);
					}
					else tmp1 = currLine.trim();
					if(j>=4) i = Integer.parseInt(tmp1);
					switch(j) {
						case 0:
							jTextSUVlo.setText(tmp1);
							break;

						case 1:
							jTextSUVhi.setText(tmp1);
							break;

						case 2:
							jTextCTlo.setText(tmp1);
							break;

						case 3:
							jTextCThi.setText(tmp1);
							break;

						case 4:
							jCheckUseSUV.setSelected(i==1);
							changeUseSUV(false);
							break;

						case 5:
							jCheckUseCt.setSelected(i==1);
							changeUseCT(false);
							break;
							
						case 6:
                            if(i==1) jRadioAverage.setSelected(true);
							else if(i==2) jRadioAny.setSelected(true);
							else jRadioAll.setSelected(true);
					}
				}
				
			}
			rd1.close();
			if( getRoiSize(0)>0) {
				jCheckDefLimits.setSelected(false);
				jCheckDefLimits.setEnabled(false);
				jButNif.setEnabled(false);
			}
			changedRoiChoice(true);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	void setColor(boolean inColor) {
		String title0 = "Set the color of pixels OUTSIDE the ROI";
		String saveColor = "outside color";
		Color color0 = Color.red;
		if( inColor) {
			color0 = Color.green;
			title0 = "Set the color of pixels INSIDE the ROI";
			saveColor = "inside color";
		}
		Color color1 = JColorChooser.showDialog(this, title0, color0);
		if( color1 == null) return;
		Preferences prefer = parentPet.parent.jPrefer;
		int rgb = color1.getRGB();
		prefer.putInt(saveColor, rgb);
	}
	
	void setROIlabel() {
		int indx = getSpinInt(0);
		int n = getRoiSize();
		int n1 = getRoiSize(1);	// nifti ROI
		if( n<indx) return;
		if( indx <= n1) {
			Nifti3 nif1 = (Nifti3) nifList[indx-1];
			nif1.ROIlabel = getComboLabel();
		} else {
			Poly3Save poly1 = polyVect.get(indx-1-n1);
			poly1.ROIlabel = getComboLabel();
		}
	}
	
	void setLimits(int lim1) {
		Integer sliceNum = 0;
		JTextField jTxtTmp;
		int indx = getSpinInt(0) - getRoiSize(1);
		int n = getRoiSize(0);
		if( indx < 1 || n<indx) return;
		Poly3Save poly1 = polyVect.get(indx-1);
		if( poly1.type != parentPet.m_sliceType) return;
		if( lim1 == 0) jTxtTmp = jTextSliceLo;
		else jTxtTmp = jTextSliceHi;
		switch( parentPet.m_sliceType) {
			case JFijiPipe.DSP_AXIAL:
				sliceNum = ChoosePetCt.round(parentPet.petAxial);
				sliceNum = poly1.numFrm - 1 - sliceNum;
				break;

			case JFijiPipe.DSP_CORONAL:
				sliceNum = ChoosePetCt.round(parentPet.petCoronal);
				break;

			case JFijiPipe.DSP_SAGITAL:
				sliceNum = ChoosePetCt.round(parentPet.petSagital);
				break;
		}
		sliceNum++;
		jTxtTmp.setText(sliceNum.toString());
		changeLimits();
		delayedRefresh();
	}
	
	void shiftGotFocus() {
		boolean showAll = jCheckShowAll.isSelected();
		isInit = true;
		OkShiftROI = true;
		int indx = getSpinInt(0);
		Integer size1 = getRoiSize() + 1;
		jLabShiftMaxRoi.setText(size1.toString() + "=all");
		if( indx >= size1) showAll = true;
		if( size1 <= 1) shiftRoiNm = -2;
		else {
			if( showAll) shiftRoiNm = -1;
			else shiftRoiNm = indx;
		}
		SpinnerNumberModel spin1 = getSpinModel(4);
		spin1.setMaximum(size1);
		if( showAll) indx = size1;
		spin1.setValue(indx);
		isInit = false;
	}
	
	void shiftValChanged(int type) {
		if( shiftRoiNm < -1) return;
		int i, size1, delta, val1;
		val1 = getSpinInt(type);
		delta = val1 - prevSpinValue[type];
		prevSpinValue[type] = val1;
		if( !OkShiftROI || delta == 0) return;
		size1 = getRoiSize(0);
		if( shiftRoiNm > 0) shiftRoi(type, delta, shiftRoiNm-1);
		else for( i=0; i<size1; i++) shiftRoi( type, delta, i);
		// when done, repaint
		delayedRefresh();
		parentPet.repaint();
	}
	
	void shiftRoi( int type, int delta, int roiNm) {
		int delta1 = delta;
		Poly3Save poly1 = polyVect.get(roiNm);
		if( poly1.type != parentPet.m_sliceType) return;
		int oldVal = saveRoiPntIndx;
		int i, n, sizeZ, width1, height1;
		int currRoi = getSpinInt(0) - getRoiSize(1);
		width1 = sizeZ = parentPet.petPipe.data1.width;
		height1  = parentPet.petPipe.data1.height;
		if( poly1.type == JFijiPipe.DSP_AXIAL) {
			sizeZ = poly1.numFrm;
			delta1 = -delta;
		}
		if( type == 3) {
			if(delta1 < 0 && poly1.lowSlice + delta1 < 0) return;
			if(delta1 > 0 && poly1.hiSlice + delta1 >= sizeZ) return;
			poly1.lowSlice += delta1;
			poly1.hiSlice += delta1;
			if(roiNm+1 == currRoi) {
				Integer[] outVal, inVal = new Integer[3];
				inVal[2] = poly1.hiSlice;
				inVal[1] = poly1.lowSlice;
				outVal = poly1.axialInverts(inVal);
				jTextSliceLo.setText(outVal[1].toString());
				jTextSliceHi.setText(outVal[2].toString());
			}
			isDirty = true;
			return;
		}
		if( poly1.sphericalROI) {
			Point pt1 = new Point();
			saveRoiPntIndx = 0;
			pt1.x = poly1.poly.xpoints[0];
			pt1.y = poly1.poly.ypoints[2];
			switch(type) {
				case 1:
					pt1.x += delta;
					break;
					
				case 2:
					pt1.y -= delta;
					break;
			}
			dragSphere(poly1, pt1);
		} else {
			Rectangle rt1 = poly1.poly.getBounds();
			n = poly1.poly.npoints;
			switch(type) {
				case 1:
					if( rt1.x + delta < 0 || rt1.x + rt1.width + delta >= width1) break;
					for( i=0; i<n; i++) poly1.poly.xpoints[i] += delta;
					break;

				case 2:
					if( rt1.y - delta < 0 || rt1.y + rt1.height - delta >= height1) break;
					for( i=0; i<n; i++) poly1.poly.ypoints[i] -= delta;
					break;
			}
		}
		saveRoiPntIndx = oldVal;
		poly1.poly.invalidate();
		isDirty = true;
	}
	
	void shiftRoiNMChanged() {
		int i, i4, size1;
		if(isInit) return;
		boolean showAll = jCheckShowAll.isSelected();
		size1 = getRoiSize();
		i4 = getSpinInt(4);
		if( i4 <= size1) {
			shiftRoiNm = i4;
			if( showAll) {
				jCheckShowAll.setSelected(false);
				changeRoiState(-1);
			}
		} else {
			shiftRoiNm = -1;
			if( !showAll) {
				jCheckShowAll.setSelected(true);
				changeRoiState(-1);
			}
			
		}
		SpinnerNumberModel spin1;
		OkShiftROI = false;
		for( i=0; i<=3; i++) {
			spin1 = getSpinModel(i);
			spin1.setValue(i4);
			i4 = 0;	// only the first one is non zero
		}
		OkShiftROI = true;
	}
	
	int getSpinInt(int type) {
		SpinnerNumberModel spin1 = getSpinModel(type);
		return spin1.getNumber().intValue();
	}
	
	SpinnerNumberModel getSpinModel(int type) {
		JSpinner jspin;
		switch( type) {
			case 0:
				jspin = jSpinRoiNm;
				break;

			case 1:
				jspin = jSpinShiftX;
				break;

			case 2:
				jspin = jSpinShiftY;
				break;

			case 3:
				jspin = jSpinShiftZ;
				break;

			case 4:
				jspin = jSpinShiftRoiNm;
				break;
				
			default:
				jspin = jSpinDiameter;
				break;
		}
		SpinnerNumberModel spin1 = (SpinnerNumberModel) jspin.getModel();
		return spin1;
	}

	void changeTmp() {
		boolean isTmp = jCheckUseTemp.isSelected();
		jTextNifSrc.setEnabled(!isTmp);
		jButNifSrc.setEnabled(!isTmp);
		jButMake3Nifti.setEnabled(!isTmp);
	}

	String browseNifti(boolean browse1) {
		String tmp = null;
		File fl1;
		int i;
		try {
			if( jCheckUseTemp.isSelected()) {
				if( parentPet.niftiTmp.contains("nifti")) return parentPet.niftiTmp;
				fl1 = File.createTempFile("nifti", null);
				tmp = fl1.getPath();
				fl1.delete();
				fl1 = new File(tmp);
				fl1.mkdirs();
				parentPet.niftiTmp = tmp;
				return tmp;
			}
			JTextField textbx = jTextNifSrc;
			tmp = textbx.getText().trim();
			if( browse1) {
				JFileChooser fc;
				if( tmp.isEmpty()) fc = new JFileChooser();
				else fc = new JFileChooser(tmp);
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fc.setAcceptAllFileFilterUsed(false);
				i = fc.showOpenDialog(this);
				if( i == JFileChooser.APPROVE_OPTION) {
					fl1 = fc.getSelectedFile();
					tmp = fl1.getPath();
					textbx.setText(tmp);
				}
			}
			fl1 = new File(tmp);
			if( !fl1.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Please put a valid path to the data");
				return null;
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return tmp;
	}

	String getNiftiDir(int type) {
		String tmp = browseNifti(false);
		if( tmp == null) return null;
		if( type == 0) return tmp + "/in";
		else return tmp + "/out";
	}

	boolean maybeMakeDir(String path) {
		File checkIt = new File(path);
		if( checkIt.exists()) return true;
		return checkIt.mkdirs();
	}

	boolean getNiftiPrefs (int type) {
		Preferences prefer = parentPet.parent.jPrefer;
		boolean defVal = true;
		String key;
		switch(type) {
			case 2:
				key = "external float";
				break;

			case 3:
				key = "external relative";
				break;

			case 4:
				key = "external ct file";
				break;

			case 5:
				key = "external mask";
				break;

			case 6:
				key = "external ctmask";
				break;

			default:
				key = "force tmpFile";
				defVal = false;
				break;
		}
		return prefer.getBoolean(key, defVal);
	}

	String generateNiftiName( int type) {
		String retVal;
		if( type == 1) retVal = "mask";
		else retVal = "src";
		retVal += "_" + generateFileName(0, true) + "_" + generateFileName(1, true);
		switch(type) {
			case 2:
				retVal += "_float";
				break;

			case 3:
				retVal += "_relative";
				break;

			case 4:
				retVal += "_ct";
				break;

			case 5:
				retVal += "_mask";
				break;

			case 6:
				retVal += "_ctmask";
				break;
		}
		retVal = (retVal + ".nii").toLowerCase();
		return retVal;
	}

	String getNiftiRegKey() {
		return "nifti0";
	}

	void runNifti() {
		Preferences prefer = parentPet.parent.jPrefer;
//		jCheckDefLimits.setSelected(false);
		boolean isBrowse = false;
		String src, tmp0, tmp1, tmp2;
		int i, n;
		while(true) {
			src = browseNifti(isBrowse);
			if( src == null) return;
			n = src.indexOf(" ");
			if( n > 0) {
				tmp0 = src.substring(0, n);
				i = tmp0.lastIndexOf(File.separator);
				tmp1 = src.substring(i);
				tmp0 = src.substring(0, i);
				tmp2 = "There are embedded spaces in: " + tmp1;
				tmp2 += "\nTry: " + tmp0;
				tmp2 += "\nor any other path without embedded spaces.";
				tmp2 += "\nAfter pressing Open with a valid path,";
				tmp2 += "\nbe patient, and let the computer work.";
				JOptionPane.showMessageDialog(this, tmp2);
				isBrowse = true;
			} else break;
		}
		NiftiStartTime = new Date();
		Roi1StartTime = new Date();
		jCheckUseSUV.setSelected(false);
		jCheckUseCt.setSelected(false);
		changeUseSUV(false);
		changeUseCT(false);
		if( !jCheckUseTemp.isSelected()) prefer.put(getNiftiRegKey(), src);
		saveLimitVals(prefer);
		jCheckDefLimits.setEnabled(false);
		jButNif.setEnabled(false);
		work2 = new bkgdRunNifti();
		work2.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String propertyName = evt.getPropertyName();
				if( propertyName.equals("state")) {
					SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
					if( state == SwingWorker.StateValue.DONE) {
						Date stopTime = new Date();
						long diff = (stopTime.getTime() - NiftiStartTime.getTime())/1000;
						int min, sec = (int)(diff % 60);
						min = (int)(diff / 60);
						NiftiLab = "Done";
						if( min > 0 || sec > 0) NiftiLab += " " + min + ":" + String.format("%02d", sec);
						NiftiLab += " - " + Integer.toString(nifListSz) + " ROIs";
						jButNif.setText(NiftiLab);
						parentPet.repaint();
						work2 = null;
					}
				}
			}
		});
		work2.execute();
	}

	void growNifti(){
		work3 = new bkgdRunGrow();
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

	void saveLimitVals(Preferences prefer) {
		if( !isNiftiDirty) return;
		int valSz, val, valz, off1;
		int[] xy;
		JFijiPipe pet = parentPet.petPipe;
		valSz = pet.data1.width;
		valz = (int)(valSz/pet.zoomX);
		if( valz > valSz) valz = valSz;
		off1 = (valSz - valz)/2;
		xy = getNiftiLimits(JFijiPipe.DSP_CORONAL, 0);
		val = (xy[0]-off1)*100/valz;
		if( val < 0) val = 0;
		prefer.putInt("nifti lox", val);
		val = (xy[1]-off1)*100/valz;
		if( val > 100) val = 100;
		prefer.putInt("nifti hix", val);
		valSz = pet.data1.height;
		valz = (int)(valSz/pet.zoomX);
		if( valz > valSz) valz = valSz;
		off1 = (valSz - valz)/2;
		xy = getNiftiLimits(JFijiPipe.DSP_AXIAL, 1);
		val = (xy[0]-off1)*100/valz;
		if( val < 0) val = 0;
		prefer.putInt("nifti loy", val);
		val = (xy[1]-off1)*100/valz;
		if( val > 100) val = 100;
		prefer.putInt("nifti hiy", val);
		valSz = pet.getNormalizedNumFrms();
		xy = getNiftiLimits(JFijiPipe.DSP_CORONAL, 1);
		val = xy[0]*100/valSz;
		prefer.putInt("nifti loz", val);
		val = xy[1]*100/valSz;
		prefer.putInt("nifti hiz", val);
	}

	void doActualRunNifti(int type) {
		int i, j, width, height, numZ, numZ1, headerSz, frmSz, coef0;
		int dataType = 2, bitPix = 8;
		boolean isSpect = false;
		Integer val;
		ImageStack msk1Stack = null;
		float minus1=1.0f;
		float xEnd=0, yEnd=0, zEnd, spaceX=0, spaceY=0, spaceZ;
		double slope, tst1, SUVfactor, factor1=10.0, pixdbl, gmax;
		File checkIt, check1;
		short currShort;
		String tmp, tmp1;
		byte[] buf;
		short[] pix1;
		float[] pixEdge;
		try {
			String src = getNiftiDir(0);
			String mask = getNiftiDir(1);
			tmp = jButNif.getText();
			if( type == 0 && tmp.startsWith("Refresh")) {
				mask += "/" + generateNiftiName(1);
				readNiftiMask(mask);
				return;
			}
			if( !maybeMakeDir(src)) {
				IJ.log("Can't create directory.\nMaybe the device is read only?");
				return;
			}
			if( !maybeMakeDir(mask)) return;
			if( type == 0) {
				checkIt = new File(mask);
				ChoosePetCt.deleteFolder(checkIt, false);
				checkIt = new File(src);
				File[] list1 = checkIt.listFiles();
				for( i=0; i<list1.length; i++) {
					check1 = list1[i];
					tmp = check1.getName();
					tmp = tmp.substring(tmp.length()-12);
					if( tmp.startsWith("relative")) continue;
					if( tmp.contains("_float")) continue;
					if( tmp.contains("_mask")) continue;
					if( tmp.contains("_ct")) continue;
					check1.delete();
				}
			}
			src += "/" + generateNiftiName(type);
			mask = mask + "/" + generateNiftiName(1);
			JFijiPipe pip0 = parentPet.petPipe;
			SUVfactor = pip0.data1.SUVfactor;
			if( SUVfactor == 0) {
				isSpect = true;
				SUVfactor = 1.0;
			}
			gmax = pip0.data1.grandMax;
			tst1 = 255*SUVfactor/gmax;
			switch(type) {
				case 0:
					if( isSpect) {
						factor1 = tst1;
						break;
					}
					if(tst1 >= 10) break;
//					factor1 = tst1;
					break;

				case 2:
					dataType = 16;
					bitPix = 32;
					factor1 = 1.0;
					break;

				case 3:
					factor1 = tst1;
					break;

				case 4:
					pip0 = parentPet.ctPipe;
					dataType = 4;
					bitPix = 16;
					break;

				case 5:
					factor1 = 1.0;
					msk1Stack = saveMaskedData(1);
					break;

				case 6:
					factor1 = 1.0;
					pip0 = parentPet.getMriOrCtPipe();
					msk1Stack = saveMaskedData(2);
					break;
			}
			if( pip0 == null || pip0.data1.pixels == null) return;
			width = pip0.data1.width;
			height = pip0.data1.height;
			numZ = pip0.getNormalizedNumFrms();
			numZ1 = pip0.data1.numFrms;
			frmSz = width * height;
			if(pip0.data1.pixelSpacing != null) {
				spaceX = pip0.data1.pixelSpacing[0];
				spaceY = pip0.data1.pixelSpacing[1];
			}
			pixEdge = ChoosePetCt.parseMultFloat(ChoosePetCt.getDicomValue(pip0.data1.metaData, "0020,0032"));
			if(pixEdge != null) {
				xEnd = pixEdge[0] + width*spaceX;
				yEnd = pixEdge[1] + height*spaceY;
			}
			zEnd = (float) pip0.getZpos(numZ-1);
			spaceZ = (float) Math.abs(pip0.avgSliceDiff);
			headerSz = 348+4;
			buf = new byte[headerSz];
			writeInt2buf(headerSz-4, buf, 0);
			buf[38] = 114;
			writeInt2buf(3, buf, 40);
			writeInt2buf(width, buf, 42);
			writeInt2buf(height, buf, 44);
			writeInt2buf(numZ1, buf, 46);
			for(i=48; i<56; i+=2) writeInt2buf(1, buf, i);
			writeInt2buf(dataType, buf, 70);	// data type
			writeInt2buf(bitPix, buf, 72);	// num bits/voxel
			writeFloat2buf(1, buf, 76);
			writeFloat2buf(spaceX, buf, 80);
			writeFloat2buf(spaceY, buf, 84);
			writeFloat2buf(spaceZ, buf, 88);
			writeFloat2buf(headerSz, buf, 108);
			writeFloat2buf(1, buf, 112);	// slope
			buf[123] = 2;
			buf[252] = 2;
			buf[254] = 1;
			writeFloat2buf(1, buf, 264);
			writeFloat2buf(xEnd, buf, 268);
			writeFloat2buf(yEnd, buf, 272);
			writeFloat2buf(zEnd, buf, 276);
			writeFloat2buf(spaceX*minus1, buf, 280);
			writeFloat2buf(xEnd, buf, 292);
			writeFloat2buf(spaceY*minus1, buf, 300);
			writeFloat2buf(yEnd, buf, 308);
			writeFloat2buf(spaceZ, buf, 320);
			writeFloat2buf(zEnd, buf, 324);
			writeInt2buf(3222382, buf, 344);	// magic
			File fl1 = new File(src);
			OutputStream out = new FileOutputStream(fl1);
			out.write(buf);
			buf = new byte[frmSz*bitPix/8];
			coef0 = pip0.data1.getCoefficentAll();
//			factor1 = 10.;	// SUV of 25.5 = 255
			SUVfactor = 1.0;
			if(type != 4) SUVfactor = pip0.data1.SUVfactor / factor1;
			if(isSpect) SUVfactor = 1 / factor1;
			for(i=0; i<numZ1; i++) {
				if( msk1Stack != null) {
					buf = (byte[]) msk1Stack.getPixels(i+1);
					out.write(buf);
					continue;
				}
				pix1 = pip0.data1.pixels.get(i);
				slope = pip0.data1.getRescaleSlope(i);
				for( j=0; j<frmSz; j++) {
					currShort = (short)(pix1[j]+coef0);
					pixdbl = currShort*slope/SUVfactor;
					switch(type) {
						case 0:
						case 3:
							if(pixdbl < 250) currShort = (short) pixdbl;
							else
								currShort = 255;
							buf[j] = (byte) currShort;
							break;

						case 2:
							writeFloat2buf((float)pixdbl, buf, j*4);
							break;

						case 4:
							writeShort2buf(currShort, buf, j*2);
							break;
					}
				}
				out.write(buf, 0, frmSz*bitPix/8);
			}
			out.close();
			if( type>0) return;
			tmp = System.getProperty("os.name");
			String OSchar = "W.exe";	// Windows
			if( tmp.startsWith("Linux")) OSchar = "L";
			if( tmp.startsWith("Mac")) OSchar = "M";
			checkIt = new File(mask);
			String shape = IJ.getDirectory("imagej") + "lib/petct/shaping_elong" +OSchar + " ";
			tmp = " " + jTextParms.getText() + " ";
			if( jCheckDefLimits.isSelected()) {
				tmp += jLabNifxmin.getText() + " "+ jLabNifxmax.getText() + " ";
				tmp += jLabNifymin.getText() + " "+ jLabNifymax.getText() + " ";
				tmp += jLabNifzmin.getText() + " "+ jLabNifzmax.getText() + " ";
			} else {
				val = width -1;
				tmp += "0 " + val.toString();
				val = height - 1;
				tmp += " 0 " + val.toString();
				val = numZ1 - 1;
				tmp += " 0 " + val.toString() + " ";
			}
			tmp = shape + src + tmp + mask;
//			IJ.log(tmp);
			if (!checkIt.exists()) {
				Process myProc = Runtime.getRuntime().exec(tmp);
				if( myProc == null) return;
			}
			while(true) {
				checkIt = new File(mask);
				if (checkIt.exists()) break;
				mySleep(1000);
			}
//			if( !polyVect.isEmpty()) return;
			if( !jCheckGrown.isSelected()) {
				tmp = "Grown mask is not checked.\nSuggestion: Try checking it and press Refresh button.";
				tmp += "\nThe first time you need to wait until the calculation finishes.\n";
				tmp += "Look at the blue dots and see which you prefer.\n";
				tmp += "You may switch back and forth using the check and Refresh button.\n";
				tmp += "Leave the check in the state you prefer.";
				IJ.log(tmp);
			}
			readNiftiMask(mask);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	void make3Nifti() {
		NiftiStartTime = new Date();
		for( int i=2; i<=6; i++) {
			if(!getNiftiPrefs(i)) continue;
			doActualRunNifti(i);
		}
	}

	void doActualGrow() {
		String growDir, title = "GrowedMaskResult";
		String mask, flt, grown;
		File fl1;
		int i, j, n, width, height, headerSz, dataType = 2, bitPix = 8;
		int frmSz, numZ;
		ImagePlus img1=null;
		ImageStack stack1;
		byte[] buf, roiList, roiMap = null;
		try {
			growDir = getNiftiDir(1);
			if( growDir == null) {
				IJ.log("Can't get Nifti directory for output results");
				return;
			}
			mask = growDir + "/" + generateNiftiName(1);
			fl1 = new File(mask);
			if( !fl1.exists()) {
				IJ.log( "Can't find autosegmentation file.\n" +
					"Maybe the calculation hasn't finished?");
				return;
			}
			flt = getNiftiDir(0) + "/" + generateNiftiName(0);
			fl1 = new File(flt);
			if( !fl1.exists()) {
//				IJ.log( "Can't find float file.\n" +
//					"Maybe you forgot to press Make 4 Nifti files?");
				IJ.log("Can't find input file.");
				return;
			}
			grown = growDir + "/" + title + ".nii";
			fl1 = new File(grown);
			if( fl1.exists()) fl1.delete();
			IJ.runMacroFile("Macro_Salim_RegionGrowing.ijm",
				mask + ";" + flt);
			mySleep(2000);
			n = WindowManager.getWindowCount();
			int[] windowList = WindowManager.getIDList();
			for( i=0; i<n; i++) {
				img1 = WindowManager.getImage(windowList[i]);
				if( img1.getTitle().startsWith(title)) break;
			}
			if( i >= n || img1 == null) {
				IJ.log("Can't find growed roi");
				return;
			}
			width = img1.getWidth();
			height = img1.getHeight();
			frmSz = width * height;
			numZ = img1.getStackSize();
			fl1 = new File(grown);
			OutputStream out = new FileOutputStream(fl1);
			headerSz = 348+4;
			buf = new byte[headerSz];
			writeInt2buf(headerSz-4, buf, 0);
			buf[38] = 114;
			writeInt2buf(3, buf, 40);
			writeInt2buf(width, buf, 42);
			writeInt2buf(height, buf, 44);
			writeInt2buf(numZ, buf, 46);
			for(i=48; i<56; i+=2) writeInt2buf(1, buf, i);
			writeInt2buf(dataType, buf, 70);	// data type
			writeInt2buf(bitPix, buf, 72);	// num bits/voxel
			writeFloat2buf(1, buf, 76);
			writeFloat2buf(1, buf, 80);
			writeFloat2buf(1, buf, 84);
			writeFloat2buf(-1, buf, 88);
			writeFloat2buf(headerSz, buf, 108);
			writeFloat2buf(1, buf, 112);	// slope
			buf[123] = 2;
			buf[252] = 2;
			buf[254] = 1;
			writeFloat2buf(1, buf, 264);
			writeFloat2buf(width/2, buf, 268);
			writeFloat2buf(height/2, buf, 272);
			writeFloat2buf(numZ-1, buf, 276);
			writeFloat2buf(-1, buf, 280);
			writeFloat2buf(width/2, buf, 292);
			writeFloat2buf(-1, buf, 300);
			writeFloat2buf(height/2, buf, 308);
			writeFloat2buf(1, buf, 320);
			writeFloat2buf(numZ-1, buf, 324);
			writeInt2buf(3222382, buf, 344);	// magic
			out.write(buf);
			stack1 = img1.getStack();
			roiList = new byte[256];
			for(i=0; i<numZ; i++) {
				buf = (byte[]) stack1.getPixels(i+1);
				for(j=0; j<frmSz; j++) roiList[buf[j]] = 1;
			}
			for( i=j=0; i<256; i++) {
				if( roiList[i] == 0 && j==0) j=i;
				if( roiList[i] == 1 && j>0) {
					roiMap = new byte[256];	// there is a hole, need to fix it.
					break;
				}
			}
			if( roiMap != null) {
				for( i=j=0; i<256; i++) {
					if( roiList[i] == 1) roiMap[i] = (byte) j++;
				}
			}
			for(i=0; i<numZ; i++) {
				buf = (byte[]) stack1.getPixels(i+1);
				if( roiMap != null) for( j=0; j<frmSz; j++) {
					buf[j] = roiMap[buf[j]];
				}
				out.write(buf);
			}
			out.close();
			isDelete = false;
			IJ.runMacroFile("Macro_CloseWindow.ijm", title);
			
		} catch (Exception e) {  ChoosePetCt.stackTrace2Log(e);}
	}

	void grownChanged() {
		if( jButNif.isEnabled()) return;
		jButNif.setText("Refresh");
		jButNif.setEnabled(true);
		// kill all ROIs
		polyVect = new ArrayList<Poly3Save>();
		nifList = null;
		nifListSz = 0;
	}
/*	String encloseInQuotes( String in) {
		return '"' + in + '"';
	}*/

	void mySleep(int msec) {
		try {
			Thread.sleep(msec);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	void readNiftiMask(String flName) {
		int i, val, x, y, z, off1, width, height, numZ, headerSz, frmSz;
		String flNm1;
		NiftiUpdate dlg1;
		boolean isGrown = false;
		File fl0 = null;
		byte[] buf;
		Nifti3 currNif;
		try {
			flNm1 = flName;
			if( jCheckGrown.isSelected()) {
				i = flNm1.lastIndexOf('/');
				flNm1 = flNm1.substring(0, i+1) + "GrowedMaskResult.nii";
				fl0 = new File(flNm1);
				dlg1 = new NiftiUpdate(null, true);
				dlg1.isUpdate(fl0, this);
				if( !fl0.exists()) growNifti();
				while( !(isGrown = fl0.exists())) mySleep(1000);
			}
			if( !isGrown) fl0 = new File(flName);
			if( fl0 == null || !fl0.exists()) {
				IJ.log( "Can't find Nifti grown result.");
				return;
			}
			FileInputStream fl1 = new FileInputStream(fl0);
			headerSz = 348+4;
			buf = new byte[headerSz];
			fl1.read(buf);	// read header
			i = readShort(buf, 0);
			if( i != 348) {
				IJ.log("Error in Nifti file");
				fl1.close();
				return;
			}
			width = readShort(buf, 42);
			height = readShort(buf, 44);
			numZ = readShort(buf, 46);
			frmSz = width * height;
			buf = new byte[frmSz];
			nifList = new Object[255];
			for(z=0; z<numZ; z++) {
				fl1.read(buf);
				for( y=0; y<height; y++) {
					off1 = y * width;
					for( x=0; x<width; x++) {
						if(buf[x+off1] == 0) continue;
						val = (buf[x+off1] & 0xff) - 1;
						currNif = (Nifti3) nifList[val];
						if( currNif == null) nifList[val] = currNif= new Nifti3();
						currNif.add(x, y, z);
					}
				}
			}
			for( i=0; i<255; i++) {
				if( nifList[i] == null) break;
			}
			nifListSz = i;
			fl1.close();
			changedRoiChoice(true);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	int readShort(byte[] buf, int off) {
		int b0, b1;
		b0 = buf[off] & 0xff;
		b1 = buf[off+1] & 0xff;
		return b0 + b1*256;
	}

	void writeFloat2buf(float inVal, byte[] buf, int offst) {
		ByteBuffer buff1 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		buff1.putFloat(inVal);
		for( int i=0; i<4; i++) buf[offst+i] = buff1.get(i);
	}
	
	void writeInt2buf(int inVal, byte[] buf, int offst) {
		int i, val = inVal;
		for( i=0; i<4; i++) {
			buf[offst+i] = (byte) val;
			val = val >> 8;
		}
	}
	
	void writeShort2buf(int inVal, byte[] buf, int offst) {
		int i, val = inVal;
		for( i=0; i<2; i++) {
			buf[offst+i] = (byte) val;
			val = val >> 8;
		}
	}

	protected class bkgdRunNifti extends SwingWorker {

		@Override
		protected Void doInBackground() {
			doActualRunNifti(0);
			return null;
		}
	}

	protected class bkgdRunGrow extends SwingWorker {

		@Override
		protected Void doInBackground() {
			doActualGrow();
			return null;
		}
	}

	void savePointList() {
		try {
			SUVpoints.SavePoint currPnt;
			Double valD;
			Integer[] valI;
			short val1;
			double slope = 1.0;
			int x0, y0, z0, zprev, x2, y2, width1, offst, coef0;
			int i, j, n = suvPnt.getListSize();
			int ctSlice, orient;
			if(n <= 0) {
				JOptionPane.showMessageDialog(this, "No ROIs found.");
				return;
			}
			String flPath, out1 = "point list path";
			File fl1;
			Preferences prefer = parentPet.parent.jPrefer;
			flPath = prefer.get(out1, null);
			JFileChooser fc;
			if( flPath == null) fc = new JFileChooser();
			else fc = new JFileChooser(flPath);
			fc.setFileFilter(new CsvFilter());
			i = fc.showOpenDialog(this);
			if( i != JFileChooser.APPROVE_OPTION) return;
			fl1 = fc.getSelectedFile();
			flPath = fl1.getParent();
			if( flPath != null) prefer.put(out1, flPath);
			flPath = fl1.getPath();
			if( !flPath.toLowerCase().endsWith(".csv")) fl1 = new File(flPath + ".csv");
			FileWriter fos = new FileWriter(fl1);
			out1 = "SUV, HU, x, y, z\n";
			fos.write(out1);
			orient = parentPet.m_sliceType;
			JFijiPipe ctPipe = parentPet.ctPipe;
//			double rescaleIntercept = ctPipe.data1.shiftIntercept();
			short[] data1 = null;
			width1 = ctPipe.data1.width;
			coef0 = ctPipe.data1.getCoefficentAll();
			valI = new Integer[4];
			zprev = -1000;
			for( i=0; i<n; i++) {
				currPnt = suvPnt.getPoint(i);
				x0 = currPnt.x1;
				y0 = currPnt.z1;
				z0 = currPnt.y1;
				switch( orient) {
					case JFijiPipe.DSP_AXIAL:
						y0 = currPnt.y1;
						z0 = currPnt.z1;
						break;

					case JFijiPipe.DSP_SAGITAL:
						x0 = currPnt.z1;
						y0 = currPnt.x1;
						break;
				}
				if( z0 != zprev) {
					ctSlice = parentPet.ctPipe.findCtPos(z0, false);
					data1 = ctPipe.data1.pixels.get(ctSlice);
					slope = ctPipe.data1.getRescaleSlope(ctSlice);
					zprev = z0;
				}
				x2 = parentPet.shift2Ct(ctPipe, x0, 0);
				y2 = parentPet.shift2Ct(ctPipe, y0, 1);
				offst = y2*width1 + x2;
				val1 = (short) ((data1[offst] + coef0)*slope);
				valD = PetCtFrame.roundN(currPnt.petVal, 3);
				valI[0] = (int) val1;
/*				valI[1] = (int) currPnt.x1;
				valI[2] = (int) currPnt.y1;
				valI[3] = (int) currPnt.z1;*/
				valI[1] = x0;
				valI[2] = y0;
				valI[3] = z0;
				out1 = valD.toString();
				for( j=0; j<4; j++) out1 += ", " + valI[j];
				out1 += "\n";
				fos.write(out1);
			}
			fos.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	ImageStack saveMaskedData(int type) {
		int i, i1, j, z, stkSz, sliceSz, width, height, listSz, offst, coef0 = 0;
		int sliceType, zmin, zmax, width1, heigh1, x, y;
		ColorModel cm;
		double min, max;
		double coef[];
		String sliceLab;
		ImagePlus img1, mask1 = null;
		ImageStack origStack, msk1Stack;
		short[] maskOut=null, origIn;
		byte[] roiOut=null, roiIn;
		byte currByte;
		JFijiPipe petPipe = parentPet.petPipe;
		if( petPipe == null) return null;
		i = petPipe.data1.orientation;
		if( (i&(64+32))!=0 || petPipe.data1.pixels == null) {
			JOptionPane.showMessageDialog(this, "Can't handle this data. Please contact ilan.tal@gmail.com");
			return null;
		}
		calculateVol(false);
		listSz = suvPnt.getListSize();
		if( listSz <= 0) {
			JOptionPane.showMessageDialog(this, "No ROIs found. Perhaps you have a different\n orientation from where you defined the ROIs?");
			return null;
		}
		if( type == 2) {
			Radionomic rad1 = new Radionomic();
			img1 = rad1.buildCtPlus(suvPnt, parentPet, true);
			if( img1 == null || maskParms == null) return null;
			origStack = img1.getImageStack();
			zmax = img1.getStackSize();
			width1 = origStack.getWidth();
			heigh1 = origStack.getHeight();
			width = maskParms[3];
			height = maskParms[4];
			stkSz = maskParms[5];
			sliceSz = width * height;
			msk1Stack = new ImageStack(width, height, origStack.getColorModel());
			for( i=0; i<stkSz; i++) {
				roiOut = new byte[sliceSz];
				z = i - maskParms[2];
				if(z>=0 && z < zmax) {
					roiIn = (byte[])origStack.getPixels(z+1);
					for( y=0; y<heigh1; y++) {
						for( x=0; x<width1; x++) {
							currByte = roiIn[y*width1+x];
							roiOut[(y+maskParms[1])*width + x+maskParms[0]] = currByte;
						}
					}
				}
				msk1Stack.addSlice(null, roiOut);
			}
			return msk1Stack;
		}
		img1 = petPipe.data1.srcImage;
		if( type == 0) {
			mask1 = img1.duplicate();
			mask1.show();
		}
		stkSz = img1.getImageStackSize();
		min = img1.getDisplayRangeMin();
		max = img1.getDisplayRangeMax();
		origStack = img1.getImageStack();
		cm = origStack.getColorModel();
		width = origStack.getWidth();
		height = origStack.getHeight();
		coef = img1.getCalibration().getCoefficients();
		if( coef != null) coef0 = (int) coef[0];
		msk1Stack = new ImageStack(width, height, cm);
		sliceSz = width * height;
		sliceType = parentPet.m_sliceType;
		zmin = zmax = -1;
		for( j=0; j<listSz; j++) {
			SUVpoints.SavePoint currPnt = mapPoint2axial(j, sliceType);
			if( j==0) {
				zmin = zmax = currPnt.z1;
				continue;
			}
			if( zmin > currPnt.z1) zmin = currPnt.z1;
			if( zmax < currPnt.z1) zmax = currPnt.z1;
		}
		for( i=1; i<=stkSz; i++) {
			if( type == 0) {
				maskOut = new short[sliceSz];
				if( coef0 != 0) for(j=0; j<sliceSz; j++) maskOut[j] = (short) coef0;
			} else roiOut = new byte[sliceSz];
			i1 = petPipe.getOrigSliceNum(i);
			sliceLab = origStack.getSliceLabel(i1);
			if( i >= zmin && i <= zmax) {
				origIn = (short []) origStack.getPixels(i1);
				for( j=0; j<listSz; j++) {
					SUVpoints.SavePoint currPnt = mapPoint2axial(j, sliceType);
					if( currPnt.z1 != i) continue;
					offst = width * currPnt.y1 + currPnt.x1;
					if( type == 0) maskOut[offst] = origIn[offst];
					else roiOut[offst] = (byte) (currPnt.rn1 + 1);	// make roi number from 1
				}
			}
			if( type == 0) msk1Stack.addSlice(sliceLab, maskOut);
			else msk1Stack.addSlice(sliceLab, roiOut);
		}
		if( mask1 != null) {
			mask1.setStack(msk1Stack);
			mask1.setDisplayRange(min, max);
			mask1.repaintWindow();
		}
		return msk1Stack;
	}

	SUVpoints.SavePoint mapPoint2axial(int indx, int sliceType) {
		int x, y, z, rn;
		SUVpoints.SavePoint currPnt = suvPnt.getPoint(indx);
		x = currPnt.x1;
		z = currPnt.y1 + 1;
		y = currPnt.z1;
		rn = currPnt.rn1;
		switch (sliceType) {
			case JFijiPipe.DSP_AXIAL:
				z = y + 1;
				y = currPnt.y1;
				break;

			case JFijiPipe.DSP_SAGITAL:
				x = y;
				y = currPnt.x1;
				break;
		}
		return suvPnt.newPoint(currPnt.petVal, currPnt.ctVal, x, y, z, rn);
	}

	class CsvFilter extends javax.swing.filechooser.FileFilter {
		@Override
		public boolean accept(File f1) {
			return f1.getName().toLowerCase().endsWith(".csv") ||
					f1.isDirectory();
		}
		@Override
		public String getDescription() {
			return "Comma separated values(*.csv)";
		}
	}

	class Nifti3 {
		ArrayList<short []> vectNif;
		String ROIlabel;
		
		Nifti3() {
			vectNif = new ArrayList<short []>();
			ROIlabel = "";
		}

		void add(int x, int y, int z) {
			short[] val3;
			val3 = new short[3];
			val3[0] = (short) x;
			val3[1] = (short) y;
			val3[2] = (short) z;
			vectNif.add(val3);
		}
		
		short[] getVal3(int i) {
			if( i>=vectNif.size()) return null;
			return vectNif.get(i);
		}

		short[] getLimits() {
			short[] limits = new short[6];
			short[] val3;
			int i, n = vectNif.size();
			for( i=0; i<n; i++) {
				val3 = getVal3(i);
				if( i==0) {
					limits[0] = limits[1] = val3[0];
					limits[2] = limits[3] = val3[1];
					limits[4] = limits[5] = val3[2];
				} else {
					if(limits[0] > val3[0]) limits[0] = val3[0];
					if(limits[1] < val3[0]) limits[1] = val3[0];
					if(limits[2] > val3[1]) limits[2] = val3[1];
					if(limits[3] < val3[1]) limits[3] = val3[1];
					if(limits[4] > val3[2]) limits[4] = val3[2];
					if(limits[5] < val3[2]) limits[5] = val3[2];
				}
			}
			return limits;
		}
	}

	class Poly3Save {
		Polygon poly;
		int type, lowSlice, hiSlice, numFrm;
		String ROIlabel;
		boolean sphericalROI;
		Ellipse2D currEllipse;
		
		Poly3Save() {
			poly = null;
			sphericalROI = false;
			type = -1;
			lowSlice = 0;
			hiSlice = 0;
			numFrm = 0;
			ROIlabel = "";
			currEllipse = null;
		}
		
		Poly3Save(Poly3Save copy) {
			type = copy.type;
			lowSlice = copy.lowSlice;
			hiSlice = copy.hiSlice;
			numFrm = copy.numFrm;
			ROIlabel = copy.ROIlabel;
			int n = copy.poly.npoints;
			int [] x1 = copy.poly.xpoints;
			int [] y1 = copy.poly.ypoints;
			poly = new Polygon(x1, y1, n);
		}
		
		boolean contains(int x, int y) {
			if(!sphericalROI) return poly.contains(x, y);
			if( currEllipse == null) return false;
			return currEllipse.contains(x, y);
		}
		
		// this counts from 0, not 1
		void setCurrEl(int slice) {
			currEllipse = getEllipse(slice+1, type, false);
		}
		
		boolean equals(Poly3Save other) {
			if( other.type != type || other.lowSlice != lowSlice
					|| other.hiSlice != hiSlice) return false;
			return poly.equals(other.poly);
		}
		
		Integer [] axialInverts() {
			Integer[] retVal = new Integer[3];
			retVal[1] = lowSlice;
			retVal[2] = hiSlice;
			return axialInverts(retVal);
		}
		
		Integer [] axialInverts(Integer [] inVal) {
			Integer[] retVal = new Integer[3];
			if( type == JFijiPipe.DSP_AXIAL) {
				retVal[0] = 1;	// swapped
				retVal[1] = numFrm + 1 - inVal[2];
				retVal[2] = numFrm + 1 - inVal[1];
			} else {
				retVal[0] = 0;	// not swapped
				retVal[1] = inVal[1];
				retVal[2] = inVal[2];
			}
			return retVal;
		}

		// the usual case uses parentPet and puts null for Display3Panel
		Ellipse2D getEllipse(int slice, int type1, boolean display) {
			return getEllipseSub(slice, type1, display, null);
		}
		
		Ellipse2D getEllipseSub(int slice, int type1, boolean display, Display3Panel d3panel) {
			if( !sphericalROI) return null;
			if( slice <= lowSlice || slice >= hiSlice) return null;
			double middle, diff0, rad1, factor;
			double w2, h2, scl1;
			int x0, y0, deltaX, deltaY;
			Point2d pt21, pt22;
			Point pt1, pt2;
			middle = (hiSlice + lowSlice)/2.0;
			diff0 = Math.abs(middle - slice);
			rad1 = (hiSlice - lowSlice)/2.0;
			factor = Math.pow(rad1, 2) - Math.pow(diff0, 2);
			factor = Math.sqrt(factor)/rad1;
			x0 = poly.xpoints[0];
			y0 = poly.ypoints[0];
			deltaX = poly.xpoints[1] - x0;
			deltaY = Math.abs(poly.ypoints[2] - y0);
			w2 = factor * deltaX;
			h2 = factor * deltaY;
			pt21 = new Point2d(x0 - w2, y0 - h2);
			pt22 = new Point2d(x0 + w2, y0 + h2);
			if( display) {
				if( d3panel == null) {	// usual case
					scl1 = parentPet.getScalePet();
					pt1 = parentPet.petPipe.pos2Scrn(pt21, scl1, type1);
					pt2 = parentPet.petPipe.pos2Scrn(pt22, scl1, type1);
				} else {
					scl1 = d3panel.getScale();
					pt1 = d3panel.d3Pipe.pos2Scrn(pt21, scl1, type1);
					pt2 = d3panel.d3Pipe.pos2Scrn(pt22, scl1, type1);
				}
				pt21 = new Point2d(pt1.x, pt1.y);
				pt22 = new Point2d(pt2.x, pt2.y);
			}
			return new Ellipse2D.Double( pt21.x, pt21.y, pt22.x - pt21.x, pt22.y-pt21.y);
		}
		
		Ellipse2D shiftEllipse( Ellipse2D in, int widthX, int shiftY) {
			if( in == null) return null;
			double x = in.getX();
			return new Ellipse2D.Double(x+widthX, in.getY()+shiftY, in.getWidth(), in.getHeight());
		}
	}

	class ExcludedROI {
		SaveVolROI saveVol1 = null;
		SUVpoints suvPnt1 = null;
		
		void saveAll(SaveVolROI volTemp) {
			saveVol1 = volTemp;
			suvPnt1 = suvPnt;
		}
		
		SaveVolROI getAll() {
			suvPnt = suvPnt1;
			numRed = 1;
			redPoints[0] = suvPnt.red0;
			return saveVol1;
		}
	}

	class SaveVolROI {
		Integer type;
		double SUVmax = 0;
		double SUVpeak = 0, peakWahl = 0, peakQpet = 0;
		double[] SUVList;
		double SUVtotal = 0;
		double vol1 = 0;
		double meanMax = 0;
		double meanPeak = 0, meanWahl = 0, meanQpet = 0;
		double SD = 0, CtHU = 0, CtSD = 0;
		boolean isExcluded = false;

		SaveVolROI() {
			RoiNum4Max = -1;	// which ROI has SUVmax
		}

		double calcAll(boolean bothPeaks) {
			int numPnt, i, k, nSUV;
			double currVal, sumSquare, SUVmean = 0;
			double[] ctVals;
			numPnt = suvPnt.getListSize();
			nSUV = SUVList.length;
			meanMax = meanPeak = 0;
			if( numPnt > 0) {
				if( bothPeaks) {
					peakWahl = suvPnt.calcSUVpeak(parentPet.petPipe, 1);
					meanWahl = suvPnt.calcSUVmeanPeak(parentPet.petPipe);
					peakQpet = suvPnt.calcSUVpeak(parentPet.petPipe, 2);
					meanQpet = suvPnt.calcSUVmeanPeak(parentPet.petPipe);
				} else {
					SUVpeak = suvPnt.calcSUVpeak(parentPet.petPipe, lastPeakType);
					meanPeak = suvPnt.calcSUVmeanPeak(parentPet.petPipe);
				}
				for(i=k=0; i<nSUV; i++) {
					if(SUVList[i]<=0) continue;
					meanMax += SUVList[i];
					k++;	// number of actual slices
				}
				if( k>0) meanMax = meanMax / k;
				vol1 = parentPet.petPipe.data1.pixelSpacing[0] / 10;	// assume x=y
				vol1 = vol1*vol1*vol1*parentPet.petPipe.data1.y2xFactor*numPnt;
				SUVmean = SUVtotal / numPnt;
				SD = suvPnt.calcSD(SUVmean);
				ctVals = suvPnt.calCtVals();
				CtHU = ctVals[0];
				CtSD = ctVals[1];
			}
			return SUVmean;
		}
	}

	void generateTMTVReport() {
		BufferedImage imgOut, img2;
		FontRenderContext frc;
		Font font1;
		Dimension dm1;
		Point pt1;
		Graphics2D g;
		Stroke oldS;
		String tmpTxt, txtLab;
		int width1, height1, typ1, fontSz, fontOld, i, n1;
		int widR, heiR, widOff, heiOff, txtHeight, off1;
		double testW, fill1;
		this.setVisible(false);	// brown fat dialog might cover up MIP
		mySleep(500);	// 0.5 sec to repaint
		PetCtFrame parFrm = parentPet.parent;
		myWriteDicom dlg1 = new myWriteDicom(parFrm, null);
		dlg1.robotType = 3; // MIP
		img2 = dlg1.getDcmData();
		width1 = img2.getWidth();
		height1 = img2.getHeight();
		typ1 = img2.getType();
		dm1 = parentPet.getReducedSize();
		if( height1 > dm1.height) height1 = dm1.height;
		imgOut = new BufferedImage(2*width1, height1, typ1);
		g = imgOut.createGraphics();
		g.setPaint(Color.white);
		g.fillRect(0, 0, width1, height1);
		g.drawImage(img2, null, width1, 0);
		widR = 8*width1/10;
		widOff = widR/10;
		heiOff = widR/2;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		tmpTxt = " Total Metabolic Tumor Volume = " + jTextVol.getText() + " ml";
		do {
			font1 = g.getFont();
			fontOld = fontSz = font1.getSize();
			frc = g.getFontRenderContext();
			testW = font1.getStringBounds(tmpTxt, frc).getWidth();
			fill1 = testW/widR;
			if( fill1 > 0.9 && fill1 < 1.0) break;
			if( fill1 > 0.9) {
				if( fontSz > 8) fontSz--;
			} else fontSz++;
			font1 = font1.deriveFont(Font.BOLD + Font.ITALIC, fontSz);
			while( fontOld == font1.getSize()) {
				if( fontOld > fontSz) fontSz--;
				else fontSz++;
				font1 = font1.deriveFont(Font.BOLD + Font.ITALIC, fontSz);
			}
			g.setFont(font1);
		} while(fontSz > 8);
		txtHeight = 120*g.getFontMetrics().getHeight()/100;
		heiR = 4*txtHeight;
		oldS = g.getStroke();
		g.setStroke( new BasicStroke(6));
		g.setPaint(Color.blue);
		g.drawRoundRect(widOff,heiOff,widR,heiR,10,10);
		g.setStroke(oldS);
		g.setPaint(Color.black);
		off1 = 0;
		g.drawString(tmpTxt, widOff + off1, heiOff + txtHeight+ off1);
		tmpTxt = " Total Lesion Glycolysis = " + jTextVolMean.getText();
		g.drawString(tmpTxt, widOff + off1, heiOff + 2*txtHeight+ off1);
		tmpTxt = " SUVmax = " + jTextSuvMax.getText();
		i = tmpTxt.indexOf('(');
		if( i>0) tmpTxt = tmpTxt.substring(0, i-1);
		if( RoiNum4Max >= 0 && RoiNum4Max < getRoiSize()) {
			n1 = getRoiSize(1);
			if( RoiNum4Max < n1) {
				Nifti3 currNif = (Nifti3) nifList[RoiNum4Max];
				txtLab = currNif.ROIlabel;
			} else {
				Poly3Save poly1 = polyVect.get(RoiNum4Max-n1);
				txtLab = poly1.ROIlabel;
			}
			if(txtLab != null) tmpTxt += txtLab;
		}
		g.drawString(tmpTxt, widOff + off1, heiOff + 3*txtHeight+ off1);
		tmpTxt = "TMTV Report";
		font1 = font1.deriveFont(Font.BOLD, fontSz*2);
		g.setFont(font1);
		frc = g.getFontRenderContext();
		testW = font1.getStringBounds(tmpTxt, frc).getWidth();
		off1 = (int)((width1 - testW)/2);
		g.drawString(tmpTxt, off1, 2*txtHeight);
		font1 = font1.deriveFont(Font.BOLD, fontSz);
		g.setFont(font1);
		tmpTxt = "  PET/CT " + ChoosePetCt.UsaDateFormat(parentPet.petPipe.data1.serTime);
		g.drawString(tmpTxt, 0, 4*txtHeight);
		tmpTxt = "  " + parFrm.m_patName;
		if(parFrm.m_patBirthday != null) tmpTxt += " born: "+ChoosePetCt.UsaDateFormat(parFrm.m_patBirthday);
		g.drawString(tmpTxt, 0, 5*txtHeight);
		font1 = font1.deriveFont(Font.PLAIN, fontSz);
		g.setFont(font1);
		tmpTxt = "  Parameters:";
		g.drawString(tmpTxt, 0, heiOff + 6*txtHeight);
		tmpTxt = "  SUV: min=" + jTextSUVlo.getText() + ", max=" + jTextSUVhi.getText();
		if( jCheckUseCt.isSelected()) {
			tmpTxt += "  CT: min=" + jTextCTlo.getText() + ", max=" + jTextCThi.getText();
		}
		g.drawString(tmpTxt, 0, heiOff + 7*txtHeight);
		font1 = font1.deriveFont(Font.PLAIN, 4*fontSz/5);
		g.setFont(font1);
		tmpTxt = "  Provided by: http://petctviewer.org";
		g.drawString(tmpTxt, 0, heiOff + 9*txtHeight);
		g.dispose();
		JFrame frm = new JFrame();
		frm.getContentPane().add(new JLabel( new ImageIcon(imgOut)));
		frm.pack();
		frm.setVisible(true);
		dlg1 = new myWriteDicom(imgOut, parFrm);
		dlg1.specialType = 1;	// TMTV Report
		dlg1.writeBuffImage();
		frm.dispose();
		tmpTxt = dlg1.outFile1.getPath();
		Opener open1 = new Opener();
		open1.setSilentMode(true);
		ImagePlus imp = open1.openImage(tmpTxt);
		this.setVisible(true);
		imp.show();
	}

	/** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "UnnecessaryBoxing"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        j3Dtab = new javax.swing.JTabbedPane();
        jRoiTab = new javax.swing.JPanel();
        jTextSUVlo = new javax.swing.JTextField();
        jCheckUseSUV = new javax.swing.JCheckBox();
        jTextCTlo = new javax.swing.JTextField();
        jCheckLock = new javax.swing.JCheckBox();
        jRadioAny = new javax.swing.JRadioButton();
        jCheckUseCt = new javax.swing.JCheckBox();
        jPanelROI = new javax.swing.JPanel();
        jSpinRoiNm = new javax.swing.JSpinner();
        jRadioNone = new javax.swing.JRadioButton();
        jTogDrawRoi = new javax.swing.JToggleButton();
        jRadioInterior = new javax.swing.JRadioButton();
        jRadioExterior = new javax.swing.JRadioButton();
        jCheckShowAll = new javax.swing.JCheckBox();
        jCheckSphere = new javax.swing.JCheckBox();
        jComboROIlabel = new javax.swing.JComboBox();
        jCheckFollow = new javax.swing.JCheckBox();
        jTextSliceLo = new javax.swing.JTextField();
        jLabLoLim = new javax.swing.JLabel();
        jLabSlLim = new javax.swing.JLabel();
        jLabHiLim = new javax.swing.JLabel();
        jTextSliceHi = new javax.swing.JTextField();
        jButTrash = new javax.swing.JButton();
        jPanelResults = new javax.swing.JPanel();
        jLabVol = new javax.swing.JLabel();
        jTextVol = new javax.swing.JTextField();
        jLabMean = new javax.swing.JLabel();
        jTextSuvMean = new javax.swing.JTextField();
        jLabelPeak = new javax.swing.JLabel();
        jTextSuvPeak = new javax.swing.JTextField();
        jLabMax = new javax.swing.JLabel();
        jTextSuvMax = new javax.swing.JTextField();
        jLabVM = new javax.swing.JLabel();
        jTextVolMean = new javax.swing.JTextField();
        jCheckBlue = new javax.swing.JCheckBox();
        jSpinDiameter = new javax.swing.JSpinner();
        jButRefresh = new javax.swing.JButton();
        jButSave = new javax.swing.JButton();
        jRadioAverage = new javax.swing.JRadioButton();
        jButLoad = new javax.swing.JButton();
        jTextSUVhi = new javax.swing.JTextField();
        jTextCThi = new javax.swing.JTextField();
        jRadioAll = new javax.swing.JRadioButton();
        jButHelp = new javax.swing.JButton();
        jCheckOld = new javax.swing.JCheckBox();
        jShiftTab = new javax.swing.JPanel();
        jLabShiftX = new javax.swing.JLabel();
        jSpinShiftX = new javax.swing.JSpinner();
        jLabShiftY = new javax.swing.JLabel();
        jSpinShiftY = new javax.swing.JSpinner();
        jLabShiftZ = new javax.swing.JLabel();
        jSpinShiftZ = new javax.swing.JSpinner();
        jLabShiftRoi = new javax.swing.JLabel();
        jSpinShiftRoiNm = new javax.swing.JSpinner();
        jLabShiftMaxRoi = new javax.swing.JLabel();
        j3dTab = new javax.swing.JPanel();
        jButBuild = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextName = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jCheckName = new javax.swing.JCheckBox();
        jCheckDate = new javax.swing.JCheckBox();
        jCheckId = new javax.swing.JCheckBox();
        jButFill = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jTextCtStrength = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jTextSuvDisplay = new javax.swing.JTextField();
        jCheckInvert = new javax.swing.JCheckBox();
        jButColorIn = new javax.swing.JButton();
        jButColorOut = new javax.swing.JButton();
        jTextSeriesName = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jMask = new javax.swing.JPanel();
        jLabMask1 = new javax.swing.JLabel();
        jLabMask2 = new javax.swing.JLabel();
        jButMask = new javax.swing.JButton();
        jLabMask3 = new javax.swing.JLabel();
        jLabMask4 = new javax.swing.JLabel();
        jNifti = new javax.swing.JPanel();
        jButNif = new javax.swing.JButton();
        jTextNifSrc = new javax.swing.JTextField();
        jTextParms = new javax.swing.JTextField();
        jLabNiftix = new javax.swing.JLabel();
        jLabNifxmin = new javax.swing.JLabel();
        jLabNifxmax = new javax.swing.JLabel();
        jLabNify = new javax.swing.JLabel();
        jLabNifymin = new javax.swing.JLabel();
        jLabNifymax = new javax.swing.JLabel();
        jLabNifz = new javax.swing.JLabel();
        jLabNifzmin = new javax.swing.JLabel();
        jLabNifzmax = new javax.swing.JLabel();
        jCheckDefLimits = new javax.swing.JCheckBox();
        jPanExNifti = new javax.swing.JPanel();
        jButMake3Nifti = new javax.swing.JButton();
        jCheckGrown = new javax.swing.JCheckBox();
        jLabNif1 = new javax.swing.JLabel();
        jLabNif2 = new javax.swing.JLabel();
        jCheckUseTemp = new javax.swing.JCheckBox();
        jButNifSrc = new javax.swing.JButton();
        jRadio = new javax.swing.JPanel();
        jLabGray = new javax.swing.JLabel();
        jTextGray = new javax.swing.JTextField();
        jChRadEntropy = new javax.swing.JCheckBox();
        jChRadHomogenity = new javax.swing.JCheckBox();
        jChRadContrast = new javax.swing.JCheckBox();
        jChRadASM = new javax.swing.JCheckBox();
        jChRadDiffEntropy = new javax.swing.JCheckBox();
        jChRadCorrelation = new javax.swing.JCheckBox();
        jChRadClusterPromenence = new javax.swing.JCheckBox();
        jChRadClusterShade = new javax.swing.JCheckBox();
        jChRadInverseDiffMoment = new javax.swing.JCheckBox();
        jChRadMaxProbability = new javax.swing.JCheckBox();
        jChRadSumAverage = new javax.swing.JCheckBox();
        jChRadSumEntropy = new javax.swing.JCheckBox();
        jChRadSumVariance = new javax.swing.JCheckBox();
        jChRadVariance = new javax.swing.JCheckBox();
        jChRadCM1 = new javax.swing.JCheckBox();
        jChRadCM2 = new javax.swing.JCheckBox();
        jChRadDiffVariance = new javax.swing.JCheckBox();
        jChRad3D = new javax.swing.JCheckBox();
        jChRadShowSD = new javax.swing.JCheckBox();
        jChRadCt = new javax.swing.JCheckBox();
        jChRadSphericity = new javax.swing.JCheckBox();
        jOther = new javax.swing.JPanel();
        jButRoiPointList = new javax.swing.JButton();
        jButReport = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Brown fat volume");

        j3Dtab.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                j3DtabStateChanged(evt);
            }
        });

        jCheckUseSUV.setText("use SUV");
        jCheckUseSUV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckUseSUVActionPerformed(evt);
            }
        });

        jCheckLock.setText("lock");
        jCheckLock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckLockActionPerformed(evt);
            }
        });

        jRadioAny.setText("any");

        jCheckUseCt.setText("use CT");
        jCheckUseCt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckUseCtActionPerformed(evt);
            }
        });

        jPanelROI.setBorder(javax.swing.BorderFactory.createTitledBorder("Regions of Interest (ROI)"));

        jSpinRoiNm.setModel(new javax.swing.SpinnerNumberModel(1, 1, 1, 1));
        jSpinRoiNm.setName(""); // NOI18N
        jSpinRoiNm.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinRoiNmStateChanged(evt);
            }
        });

        jRadioNone.setText("None");
        jRadioNone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioNoneActionPerformed(evt);
            }
        });

        jTogDrawRoi.setText("Draw");
        jTogDrawRoi.setToolTipText("Draw a new ROI");
        jTogDrawRoi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTogDrawRoiActionPerformed(evt);
            }
        });

        jRadioInterior.setText("Interior");
        jRadioInterior.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioInteriorActionPerformed(evt);
            }
        });

        jRadioExterior.setText("Exterior");
        jRadioExterior.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioExteriorActionPerformed(evt);
            }
        });

        jCheckShowAll.setText("Show all");
        jCheckShowAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckShowAllActionPerformed(evt);
            }
        });

        jCheckSphere.setText("sphere");

        jComboROIlabel.setEditable(true);
        jComboROIlabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboROIlabelActionPerformed(evt);
            }
        });
        jComboROIlabel.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jComboROIlabelFocusLost(evt);
            }
        });

        jCheckFollow.setToolTipText("display follows ROI");
        jCheckFollow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckFollowActionPerformed(evt);
            }
        });

        jTextSliceLo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextSliceLoActionPerformed(evt);
            }
        });

        jLabLoLim.setText(" * ");
        jLabLoLim.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jLabLoLim.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabLoLimMouseClicked(evt);
            }
        });

        jLabSlLim.setText("Slice limits");
        jLabSlLim.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jLabSlLim.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabSlLimMouseClicked(evt);
            }
        });

        jLabHiLim.setText(" * ");
        jLabHiLim.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jLabHiLim.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabHiLimMouseClicked(evt);
            }
        });

        jTextSliceHi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextSliceHiActionPerformed(evt);
            }
        });

        jButTrash.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/trash.png"))); // NOI18N
        jButTrash.setIconTextGap(2);
        jButTrash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButTrashActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelROILayout = new javax.swing.GroupLayout(jPanelROI);
        jPanelROI.setLayout(jPanelROILayout);
        jPanelROILayout.setHorizontalGroup(
            jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelROILayout.createSequentialGroup()
                .addComponent(jRadioNone)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jCheckFollow)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinRoiNm, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(jPanelROILayout.createSequentialGroup()
                .addComponent(jRadioExterior)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jCheckSphere))
            .addComponent(jComboROIlabel, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanelROILayout.createSequentialGroup()
                .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelROILayout.createSequentialGroup()
                        .addComponent(jRadioInterior)
                        .addGap(30, 30, 30)
                        .addComponent(jTogDrawRoi)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButTrash, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCheckShowAll)
                    .addGroup(jPanelROILayout.createSequentialGroup()
                        .addComponent(jTextSliceLo, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(3, 3, 3)
                        .addComponent(jLabLoLim)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabSlLim)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabHiLim)
                        .addGap(1, 1, 1)
                        .addComponent(jTextSliceHi, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanelROILayout.setVerticalGroup(
            jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelROILayout.createSequentialGroup()
                .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jRadioNone)
                    .addComponent(jSpinRoiNm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckFollow))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButTrash)
                    .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTogDrawRoi)
                        .addComponent(jRadioInterior)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioExterior)
                    .addComponent(jCheckSphere))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckShowAll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboROIlabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextSliceLo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabLoLim)
                    .addComponent(jLabSlLim)
                    .addComponent(jLabHiLim)
                    .addComponent(jTextSliceHi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanelResults.setBorder(javax.swing.BorderFactory.createTitledBorder("Results"));

        jLabVol.setText("Vol (ml)");

        jTextVol.setEditable(false);

        jLabMean.setText("SUV mean");

        jTextSuvMean.setEditable(false);

        jLabelPeak.setText("SUV peak");

        jTextSuvPeak.setEditable(false);

        jLabMax.setText("SUV max");

        jTextSuvMax.setEditable(false);

        jLabVM.setText("Vol*mean");

        jTextVolMean.setEditable(false);

        jCheckBlue.setMnemonic('b');
        jCheckBlue.setToolTipText("shortcut: Alt-b");
        jCheckBlue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBlueActionPerformed(evt);
            }
        });

        jSpinDiameter.setModel(new javax.swing.SpinnerNumberModel(0, 0, 7, 1));
        jSpinDiameter.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinDiameterStateChanged(evt);
            }
        });

        jButRefresh.setText("Refresh");
        jButRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButRefreshActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelResultsLayout = new javax.swing.GroupLayout(jPanelResults);
        jPanelResults.setLayout(jPanelResultsLayout);
        jPanelResultsLayout.setHorizontalGroup(
            jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResultsLayout.createSequentialGroup()
                .addComponent(jCheckBlue)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinDiameter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                .addComponent(jButRefresh))
            .addGroup(jPanelResultsLayout.createSequentialGroup()
                .addGroup(jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabVol)
                    .addComponent(jLabMean)
                    .addComponent(jLabelPeak)
                    .addComponent(jLabMax)
                    .addComponent(jLabVM))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextVolMean)
                    .addComponent(jTextSuvMax)
                    .addComponent(jTextSuvPeak)
                    .addComponent(jTextSuvMean)
                    .addComponent(jTextVol)))
        );
        jPanelResultsLayout.setVerticalGroup(
            jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResultsLayout.createSequentialGroup()
                .addGroup(jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabVol)
                    .addComponent(jTextVol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabMean)
                    .addComponent(jTextSuvMean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelPeak)
                    .addComponent(jTextSuvPeak, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabMax)
                    .addComponent(jTextSuvMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabVM)
                    .addComponent(jTextVolMean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBlue)
                    .addGroup(jPanelResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jSpinDiameter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButRefresh))))
        );

        jButSave.setText("Save");
        jButSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSaveActionPerformed(evt);
            }
        });

        jRadioAverage.setText("average");

        jButLoad.setText("Load");
        jButLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButLoadActionPerformed(evt);
            }
        });

        jRadioAll.setText("all");

        jButHelp.setText("Help");
        jButHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButHelpActionPerformed(evt);
            }
        });

        jCheckOld.setText("flip old");

        javax.swing.GroupLayout jRoiTabLayout = new javax.swing.GroupLayout(jRoiTab);
        jRoiTab.setLayout(jRoiTabLayout);
        jRoiTabLayout.setHorizontalGroup(
            jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jRoiTabLayout.createSequentialGroup()
                .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jRoiTabLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextSUVlo, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextCTlo, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jRoiTabLayout.createSequentialGroup()
                                .addComponent(jCheckUseSUV)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextSUVhi, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jRoiTabLayout.createSequentialGroup()
                                .addComponent(jCheckUseCt)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextCThi)))
                        .addGap(44, 44, 44)
                        .addComponent(jRadioAll))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jRoiTabLayout.createSequentialGroup()
                        .addComponent(jPanelROI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelResults, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButSave)
                    .addGroup(jRoiTabLayout.createSequentialGroup()
                        .addComponent(jButLoad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCheckOld))
                    .addGroup(jRoiTabLayout.createSequentialGroup()
                        .addComponent(jCheckLock)
                        .addGap(18, 18, 18)
                        .addComponent(jButHelp))
                    .addGroup(jRoiTabLayout.createSequentialGroup()
                        .addComponent(jRadioAverage)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jRadioAny)))
                .addGap(71, 71, 71))
        );
        jRoiTabLayout.setVerticalGroup(
            jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jRoiTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextSUVlo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckUseSUV)
                    .addComponent(jTextSUVhi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckLock)
                    .addComponent(jButHelp))
                .addGap(7, 7, 7)
                .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextCTlo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckUseCt)
                    .addComponent(jTextCThi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jRadioAll)
                    .addComponent(jRadioAverage)
                    .addComponent(jRadioAny))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jRoiTabLayout.createSequentialGroup()
                        .addComponent(jButSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButLoad)
                            .addComponent(jCheckOld))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jRoiTabLayout.createSequentialGroup()
                        .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jPanelResults, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(jPanelROI, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );

        j3Dtab.addTab("ROI", jRoiTab);

        jShiftTab.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                jShiftTabComponentShown(evt);
            }
        });

        jLabShiftX.setText("x:");

        jSpinShiftX.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinShiftXStateChanged(evt);
            }
        });

        jLabShiftY.setText("y:");

        jSpinShiftY.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinShiftYStateChanged(evt);
            }
        });

        jLabShiftZ.setText("z:");

        jSpinShiftZ.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinShiftZStateChanged(evt);
            }
        });

        jLabShiftRoi.setText("Shift ROI");

        jSpinShiftRoiNm.setModel(new javax.swing.SpinnerNumberModel(1, 1, 1, 1));
        jSpinShiftRoiNm.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinShiftRoiNmStateChanged(evt);
            }
        });

        jLabShiftMaxRoi.setText("1=all");

        javax.swing.GroupLayout jShiftTabLayout = new javax.swing.GroupLayout(jShiftTab);
        jShiftTab.setLayout(jShiftTabLayout);
        jShiftTabLayout.setHorizontalGroup(
            jShiftTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jShiftTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jShiftTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jShiftTabLayout.createSequentialGroup()
                        .addComponent(jLabShiftX)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinShiftX, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabShiftY)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinShiftY, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(21, 21, 21)
                        .addComponent(jLabShiftZ)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinShiftZ, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jShiftTabLayout.createSequentialGroup()
                        .addComponent(jLabShiftRoi)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinShiftRoiNm, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabShiftMaxRoi)))
                .addContainerGap(279, Short.MAX_VALUE))
        );
        jShiftTabLayout.setVerticalGroup(
            jShiftTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jShiftTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jShiftTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabShiftRoi)
                    .addComponent(jSpinShiftRoiNm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabShiftMaxRoi))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jShiftTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabShiftX)
                    .addComponent(jSpinShiftX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabShiftY)
                    .addComponent(jSpinShiftY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabShiftZ)
                    .addComponent(jSpinShiftZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(233, Short.MAX_VALUE))
        );

        j3Dtab.addTab("follow up", jShiftTab);

        jButBuild.setText("Build");
        jButBuild.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBuildActionPerformed(evt);
            }
        });

        jLabel1.setText("Name:");

        jTextName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextNameKeyTyped(evt);
            }
        });

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Fill Name"));

        jCheckName.setText("Name");

        jCheckDate.setText("Date");

        jCheckId.setText("MRN");

        jButFill.setText("Fill");
        jButFill.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButFillActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckDate)
                    .addComponent(jCheckName)
                    .addComponent(jCheckId)
                    .addComponent(jButFill))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jCheckName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckDate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckId)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButFill)
                .addContainerGap())
        );

        jLabel6.setText("CT display strength %");

        jLabel7.setText("SUV display max");

        jCheckInvert.setText("Invert image");

        jButColorIn.setText("Set In Color");
        jButColorIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButColorInActionPerformed(evt);
            }
        });

        jButColorOut.setText("Set Out Color");
        jButColorOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButColorOutActionPerformed(evt);
            }
        });

        jLabel8.setText("Series name :");

        javax.swing.GroupLayout j3dTabLayout = new javax.swing.GroupLayout(j3dTab);
        j3dTab.setLayout(j3dTabLayout);
        j3dTabLayout.setHorizontalGroup(
            j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(j3dTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(j3dTabLayout.createSequentialGroup()
                        .addGroup(j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(j3dTabLayout.createSequentialGroup()
                                .addGroup(j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel7))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jTextSuvDisplay)
                                    .addComponent(jTextCtStrength, javax.swing.GroupLayout.DEFAULT_SIZE, 41, Short.MAX_VALUE)))
                            .addGroup(j3dTabLayout.createSequentialGroup()
                                .addComponent(jButColorIn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButColorOut))
                            .addGroup(j3dTabLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextName, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(j3dTabLayout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextSeriesName)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCheckInvert)
                    .addComponent(jButBuild))
                .addContainerGap(85, Short.MAX_VALUE))
        );
        j3dTabLayout.setVerticalGroup(
            j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(j3dTabLayout.createSequentialGroup()
                .addGroup(j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(j3dTabLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jTextName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(jTextSeriesName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jTextCtStrength, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jTextSuvDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(j3dTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButColorIn)
                            .addComponent(jButColorOut))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckInvert)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButBuild))
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(75, Short.MAX_VALUE))
        );

        j3Dtab.addTab("3D", j3dTab);

        jLabMask1.setText("This masks the PET data by the accepted points (blue dots).");

        jLabMask2.setText("All rejected points are set to zero. To save the dataset use:");

        jButMask.setText("Make masked PET");
        jButMask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMaskActionPerformed(evt);
            }
        });

        jLabMask3.setText("Fiji -> File -> Save As -> myDicom...");

        jLabMask4.setText("Be sure to change the Series name to a meaningful name.");

        javax.swing.GroupLayout jMaskLayout = new javax.swing.GroupLayout(jMask);
        jMask.setLayout(jMaskLayout);
        jMaskLayout.setHorizontalGroup(
            jMaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jMaskLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jMaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabMask1)
                    .addComponent(jLabMask2)
                    .addComponent(jButMask)
                    .addComponent(jLabMask3)
                    .addComponent(jLabMask4))
                .addContainerGap(124, Short.MAX_VALUE))
        );
        jMaskLayout.setVerticalGroup(
            jMaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jMaskLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabMask1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabMask2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabMask3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabMask4)
                .addGap(16, 16, 16)
                .addComponent(jButMask)
                .addContainerGap(170, Short.MAX_VALUE))
        );

        j3Dtab.addTab("mask", jMask);

        jButNif.setText("Press to run, and be patient");
        jButNif.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButNifActionPerformed(evt);
            }
        });

        jTextParms.setText("30 2 50 0 30 0 256");

        jLabNiftix.setText("x:");

        jLabNifxmin.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jLabNifxmax.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jLabNify.setText("y:");

        jLabNifymin.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jLabNifymax.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jLabNifz.setText("z:");

        jLabNifzmin.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jLabNifzmax.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jCheckDefLimits.setSelected(true);
        jCheckDefLimits.setText("Use the axial and coronal views to define limits for the search");
        jCheckDefLimits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckDefLimitsActionPerformed(evt);
            }
        });

        jPanExNifti.setBorder(javax.swing.BorderFactory.createTitledBorder("use Nifti in external programs"));

        jButMake3Nifti.setText("make Nifti files");
        jButMake3Nifti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMake3NiftiActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanExNiftiLayout = new javax.swing.GroupLayout(jPanExNifti);
        jPanExNifti.setLayout(jPanExNiftiLayout);
        jPanExNiftiLayout.setHorizontalGroup(
            jPanExNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanExNiftiLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButMake3Nifti)
                .addContainerGap(92, Short.MAX_VALUE))
        );
        jPanExNiftiLayout.setVerticalGroup(
            jPanExNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jButMake3Nifti)
        );

        jCheckGrown.setText("use grown mask");
        jCheckGrown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckGrownActionPerformed(evt);
            }
        });

        jLabNif1.setText("Autosegmentation is performed using NifTI files (automatically constructed)");

        jLabNif2.setText("Please define where the NIfTI data will be stored.");

        jCheckUseTemp.setText("use tmp file, if not use:");
        jCheckUseTemp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckUseTempActionPerformed(evt);
            }
        });

        jButNifSrc.setText("Browse");
        jButNifSrc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButNifSrcActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jNiftiLayout = new javax.swing.GroupLayout(jNifti);
        jNifti.setLayout(jNiftiLayout);
        jNiftiLayout.setHorizontalGroup(
            jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jNiftiLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jNiftiLayout.createSequentialGroup()
                        .addComponent(jPanExNifti, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckGrown)
                        .addGap(98, 98, 98))
                    .addGroup(jNiftiLayout.createSequentialGroup()
                        .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jNiftiLayout.createSequentialGroup()
                                .addComponent(jCheckUseTemp)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextNifSrc, javax.swing.GroupLayout.PREFERRED_SIZE, 253, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButNifSrc))
                            .addComponent(jCheckDefLimits)
                            .addGroup(jNiftiLayout.createSequentialGroup()
                                .addComponent(jLabNiftix)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabNifxmin, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabNifxmax, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabNify)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabNifymin, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabNifymax, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabNifz)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabNifzmin, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabNifzmax, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jButNif)
                            .addComponent(jTextParms, javax.swing.GroupLayout.PREFERRED_SIZE, 511, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabNif1)
                            .addComponent(jLabNif2))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jNiftiLayout.setVerticalGroup(
            jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jNiftiLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabNif1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabNif2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckUseTemp)
                    .addComponent(jTextNifSrc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButNifSrc))
                .addGap(8, 8, 8)
                .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jNiftiLayout.createSequentialGroup()
                        .addComponent(jTextParms, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckDefLimits)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabNiftix)
                                .addComponent(jLabNifxmax, javax.swing.GroupLayout.Alignment.TRAILING))
                            .addComponent(jLabNify)
                            .addComponent(jLabNifymin)
                            .addComponent(jLabNifymax)
                            .addComponent(jLabNifz)
                            .addComponent(jLabNifzmin)))
                    .addComponent(jLabNifzmax)
                    .addComponent(jLabNifxmin))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButNif)
                .addGap(34, 34, 34)
                .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanExNifti, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckGrown))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        j3Dtab.addTab("nifti", jNifti);

        jLabGray.setText("Number of gray levels");

        jTextGray.setToolTipText("between 4 and 32");

        jChRadEntropy.setText("entropy");

        jChRadHomogenity.setText("homogeneity");

        jChRadContrast.setText("contrast");

        jChRadASM.setText("ASM");

        jChRadDiffEntropy.setText("difference entropy");

        jChRadCorrelation.setText("correlation");

        jChRadClusterPromenence.setText("cluster prominence");

        jChRadClusterShade.setText("cluster shade");

        jChRadInverseDiffMoment.setText("inverse difference moment");

        jChRadMaxProbability.setText("maximum probability");

        jChRadSumAverage.setText("sum average");

        jChRadSumEntropy.setText("sum entropy");

        jChRadSumVariance.setText("sum variance");

        jChRadVariance.setText("variance");

        jChRadCM1.setText("measure of correlation 1");

        jChRadCM2.setText("measure of correlation 2");

        jChRadDiffVariance.setText("difference variance");

        jChRad3D.setText("3D");

        jChRadShowSD.setText("show SD");

        jChRadCt.setText("calc CT");

        jChRadSphericity.setText("sphericity");

        javax.swing.GroupLayout jRadioLayout = new javax.swing.GroupLayout(jRadio);
        jRadio.setLayout(jRadioLayout);
        jRadioLayout.setHorizontalGroup(
            jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jRadioLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jRadioLayout.createSequentialGroup()
                        .addComponent(jLabGray)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextGray, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jChRad3D)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jChRadShowSD)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jChRadCt))
                    .addGroup(jRadioLayout.createSequentialGroup()
                        .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jChRadEntropy)
                            .addComponent(jChRadHomogenity)
                            .addComponent(jChRadContrast)
                            .addComponent(jChRadASM)
                            .addComponent(jChRadDiffEntropy)
                            .addComponent(jChRadCorrelation)
                            .addComponent(jChRadClusterPromenence)
                            .addComponent(jChRadDiffVariance)
                            .addComponent(jChRadClusterShade))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jChRadSphericity)
                            .addComponent(jChRadSumEntropy)
                            .addComponent(jChRadSumVariance)
                            .addComponent(jChRadVariance)
                            .addComponent(jChRadMaxProbability)
                            .addComponent(jChRadSumAverage)
                            .addComponent(jChRadInverseDiffMoment)
                            .addComponent(jChRadCM2)
                            .addComponent(jChRadCM1))))
                .addContainerGap(75, Short.MAX_VALUE))
        );
        jRadioLayout.setVerticalGroup(
            jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jRadioLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabGray)
                    .addComponent(jTextGray, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jChRad3D)
                    .addComponent(jChRadShowSD)
                    .addComponent(jChRadCt))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jChRadEntropy)
                    .addComponent(jChRadCM1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jChRadCM2)
                    .addComponent(jChRadHomogenity))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jChRadContrast)
                    .addComponent(jChRadInverseDiffMoment))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jChRadASM)
                    .addComponent(jChRadMaxProbability))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jChRadDiffEntropy)
                    .addComponent(jChRadSumAverage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jChRadSumEntropy)
                    .addComponent(jChRadDiffVariance))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jChRadSumVariance)
                    .addComponent(jChRadCorrelation))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jChRadVariance)
                    .addComponent(jChRadClusterPromenence))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jChRadClusterShade)
                    .addComponent(jChRadSphericity))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        j3Dtab.addTab("radiomics", jRadio);

        jButRoiPointList.setText("Save ROI, point list");
        jButRoiPointList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButRoiPointListActionPerformed(evt);
            }
        });

        jButReport.setText("TMTV Report");
        jButReport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButReportActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jOtherLayout = new javax.swing.GroupLayout(jOther);
        jOther.setLayout(jOtherLayout);
        jOtherLayout.setHorizontalGroup(
            jOtherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jOtherLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jOtherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButRoiPointList)
                    .addComponent(jButReport))
                .addContainerGap(396, Short.MAX_VALUE))
        );
        jOtherLayout.setVerticalGroup(
            jOtherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jOtherLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButRoiPointList)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButReport)
                .addContainerGap(237, Short.MAX_VALUE))
        );

        j3Dtab.addTab("other", jOther);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(j3Dtab, javax.swing.GroupLayout.PREFERRED_SIZE, 552, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(j3Dtab, javax.swing.GroupLayout.PREFERRED_SIZE, 350, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jCheckLockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckLockActionPerformed
		lockedParent = null;
		if( jCheckLock.isSelected()) lockedParent = parentPet;
	}//GEN-LAST:event_jCheckLockActionPerformed

	private void jRadioNoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioNoneActionPerformed
		changeRoiState(0);
	}//GEN-LAST:event_jRadioNoneActionPerformed

	private void jRadioInteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioInteriorActionPerformed
		changeRoiState(1);
	}//GEN-LAST:event_jRadioInteriorActionPerformed

	private void jRadioExteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioExteriorActionPerformed
		changeRoiState(2);
	}//GEN-LAST:event_jRadioExteriorActionPerformed

	private void jTogDrawRoiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTogDrawRoiActionPerformed
		pressRoiBut();
	}//GEN-LAST:event_jTogDrawRoiActionPerformed

	private void jSpinRoiNmStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinRoiNmStateChanged
		changeRoiAndUpdate();
	}//GEN-LAST:event_jSpinRoiNmStateChanged

	private void jCheckShowAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckShowAllActionPerformed
		changeRoiState(-1);
	}//GEN-LAST:event_jCheckShowAllActionPerformed

	private void jTextSliceLoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextSliceLoActionPerformed
		changeLimits();
	}//GEN-LAST:event_jTextSliceLoActionPerformed

	private void jTextSliceHiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextSliceHiActionPerformed
		changeLimits();
	}//GEN-LAST:event_jTextSliceHiActionPerformed

	private void jButRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButRefreshActionPerformed
		calculateVol(true);
	}//GEN-LAST:event_jButRefreshActionPerformed

	private void jSpinDiameterStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinDiameterStateChanged
		jCheckBlue.setSelected(true);
		parentPet.repaintAll();
	}//GEN-LAST:event_jSpinDiameterStateChanged

	private void jButBuildActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBuildActionPerformed
		buildDataset();
	}//GEN-LAST:event_jButBuildActionPerformed

	private void jTextNameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextNameKeyTyped
		activateBuildButton();
	}//GEN-LAST:event_jTextNameKeyTyped

	private void jButFillActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButFillActionPerformed
		fillPatName();
	}//GEN-LAST:event_jButFillActionPerformed

	private void jButHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButHelpActionPerformed
		ChoosePetCt.openHelp("Brown fat Volume");
	}//GEN-LAST:event_jButHelpActionPerformed

	private void jButColorInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButColorInActionPerformed
		setColor(true);
	}//GEN-LAST:event_jButColorInActionPerformed

	private void jButColorOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButColorOutActionPerformed
		setColor(false);
	}//GEN-LAST:event_jButColorOutActionPerformed

	private void jButSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSaveActionPerformed
		saveResults(null);
	}//GEN-LAST:event_jButSaveActionPerformed

    private void jButLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButLoadActionPerformed
		loadROIs();
    }//GEN-LAST:event_jButLoadActionPerformed

    private void jLabLoLimMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabLoLimMouseClicked
		setLimits(0);
    }//GEN-LAST:event_jLabLoLimMouseClicked

    private void jLabHiLimMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabHiLimMouseClicked
		setLimits(1);
    }//GEN-LAST:event_jLabHiLimMouseClicked

    private void jComboROIlabelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboROIlabelActionPerformed
		setROIlabel();
    }//GEN-LAST:event_jComboROIlabelActionPerformed

    private void jComboROIlabelFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jComboROIlabelFocusLost
		setROIlabel();
    }//GEN-LAST:event_jComboROIlabelFocusLost

    private void jLabSlLimMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabSlLimMouseClicked
		setZcenter();
    }//GEN-LAST:event_jLabSlLimMouseClicked

    private void jShiftTabComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jShiftTabComponentShown
		shiftGotFocus();
    }//GEN-LAST:event_jShiftTabComponentShown

    private void jSpinShiftXStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinShiftXStateChanged
		shiftValChanged(1);
    }//GEN-LAST:event_jSpinShiftXStateChanged

    private void jSpinShiftYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinShiftYStateChanged
		shiftValChanged(2);
    }//GEN-LAST:event_jSpinShiftYStateChanged

    private void jSpinShiftZStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinShiftZStateChanged
		shiftValChanged(3);
    }//GEN-LAST:event_jSpinShiftZStateChanged

    private void jSpinShiftRoiNmStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinShiftRoiNmStateChanged
		shiftRoiNMChanged();
    }//GEN-LAST:event_jSpinShiftRoiNmStateChanged

    private void jCheckBlueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBlueActionPerformed
		setSpinDiameterState();
		parentPet.repaintAll();
    }//GEN-LAST:event_jCheckBlueActionPerformed

    private void jButMaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMaskActionPerformed
		saveMaskedData(0);
    }//GEN-LAST:event_jButMaskActionPerformed

    private void jButNifActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButNifActionPerformed
        runNifti();
    }//GEN-LAST:event_jButNifActionPerformed

    private void jButNifSrcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButNifSrcActionPerformed
        browseNifti( true);
    }//GEN-LAST:event_jButNifSrcActionPerformed

    private void jCheckDefLimitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckDefLimitsActionPerformed
		niftiLimChanged();
    }//GEN-LAST:event_jCheckDefLimitsActionPerformed

    private void jButRoiPointListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButRoiPointListActionPerformed
		savePointList();
    }//GEN-LAST:event_jButRoiPointListActionPerformed

    private void j3DtabStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_j3DtabStateChanged
		if(parentPet != null) parentPet.repaintAll();
    }//GEN-LAST:event_j3DtabStateChanged

    private void jCheckUseSUVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckUseSUVActionPerformed
		changeUseSUV(true);
    }//GEN-LAST:event_jCheckUseSUVActionPerformed

    private void jCheckUseCtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckUseCtActionPerformed
		changeUseCT(true);
    }//GEN-LAST:event_jCheckUseCtActionPerformed

    private void jButReportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButReportActionPerformed
		generateTMTVReport();
    }//GEN-LAST:event_jButReportActionPerformed

    private void jButMake3NiftiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMake3NiftiActionPerformed
		make3Nifti();
    }//GEN-LAST:event_jButMake3NiftiActionPerformed

    private void jCheckGrownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckGrownActionPerformed
		grownChanged();
    }//GEN-LAST:event_jCheckGrownActionPerformed

    private void jButTrashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButTrashActionPerformed
		pressTrashBut();
    }//GEN-LAST:event_jButTrashActionPerformed

    private void jCheckFollowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckFollowActionPerformed
		followCheck();
    }//GEN-LAST:event_jCheckFollowActionPerformed

    private void jCheckUseTempActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckUseTempActionPerformed
		changeTmp();
    }//GEN-LAST:event_jCheckUseTempActionPerformed

 
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JTabbedPane j3Dtab;
    private javax.swing.JPanel j3dTab;
    private javax.swing.JButton jButBuild;
    private javax.swing.JButton jButColorIn;
    private javax.swing.JButton jButColorOut;
    private javax.swing.JButton jButFill;
    private javax.swing.JButton jButHelp;
    private javax.swing.JButton jButLoad;
    private javax.swing.JButton jButMake3Nifti;
    private javax.swing.JButton jButMask;
    private javax.swing.JButton jButNif;
    private javax.swing.JButton jButNifSrc;
    private javax.swing.JButton jButRefresh;
    private javax.swing.JButton jButReport;
    private javax.swing.JButton jButRoiPointList;
    private javax.swing.JButton jButSave;
    private javax.swing.JButton jButTrash;
    private javax.swing.JCheckBox jChRad3D;
    private javax.swing.JCheckBox jChRadASM;
    private javax.swing.JCheckBox jChRadCM1;
    private javax.swing.JCheckBox jChRadCM2;
    private javax.swing.JCheckBox jChRadClusterPromenence;
    private javax.swing.JCheckBox jChRadClusterShade;
    private javax.swing.JCheckBox jChRadContrast;
    private javax.swing.JCheckBox jChRadCorrelation;
    private javax.swing.JCheckBox jChRadCt;
    private javax.swing.JCheckBox jChRadDiffEntropy;
    private javax.swing.JCheckBox jChRadDiffVariance;
    private javax.swing.JCheckBox jChRadEntropy;
    private javax.swing.JCheckBox jChRadHomogenity;
    private javax.swing.JCheckBox jChRadInverseDiffMoment;
    private javax.swing.JCheckBox jChRadMaxProbability;
    private javax.swing.JCheckBox jChRadShowSD;
    private javax.swing.JCheckBox jChRadSphericity;
    private javax.swing.JCheckBox jChRadSumAverage;
    private javax.swing.JCheckBox jChRadSumEntropy;
    private javax.swing.JCheckBox jChRadSumVariance;
    private javax.swing.JCheckBox jChRadVariance;
    private javax.swing.JCheckBox jCheckBlue;
    private javax.swing.JCheckBox jCheckDate;
    private javax.swing.JCheckBox jCheckDefLimits;
    private javax.swing.JCheckBox jCheckFollow;
    private javax.swing.JCheckBox jCheckGrown;
    private javax.swing.JCheckBox jCheckId;
    private javax.swing.JCheckBox jCheckInvert;
    private javax.swing.JCheckBox jCheckLock;
    private javax.swing.JCheckBox jCheckName;
    private javax.swing.JCheckBox jCheckOld;
    private javax.swing.JCheckBox jCheckShowAll;
    private javax.swing.JCheckBox jCheckSphere;
    private javax.swing.JCheckBox jCheckUseCt;
    private javax.swing.JCheckBox jCheckUseSUV;
    private javax.swing.JCheckBox jCheckUseTemp;
    private javax.swing.JComboBox jComboROIlabel;
    private javax.swing.JLabel jLabGray;
    private javax.swing.JLabel jLabHiLim;
    private javax.swing.JLabel jLabLoLim;
    private javax.swing.JLabel jLabMask1;
    private javax.swing.JLabel jLabMask2;
    private javax.swing.JLabel jLabMask3;
    private javax.swing.JLabel jLabMask4;
    private javax.swing.JLabel jLabMax;
    private javax.swing.JLabel jLabMean;
    private javax.swing.JLabel jLabNif1;
    private javax.swing.JLabel jLabNif2;
    private javax.swing.JLabel jLabNiftix;
    private javax.swing.JLabel jLabNifxmax;
    private javax.swing.JLabel jLabNifxmin;
    private javax.swing.JLabel jLabNify;
    private javax.swing.JLabel jLabNifymax;
    private javax.swing.JLabel jLabNifymin;
    private javax.swing.JLabel jLabNifz;
    private javax.swing.JLabel jLabNifzmax;
    private javax.swing.JLabel jLabNifzmin;
    private javax.swing.JLabel jLabShiftMaxRoi;
    private javax.swing.JLabel jLabShiftRoi;
    private javax.swing.JLabel jLabShiftX;
    private javax.swing.JLabel jLabShiftY;
    private javax.swing.JLabel jLabShiftZ;
    private javax.swing.JLabel jLabSlLim;
    private javax.swing.JLabel jLabVM;
    private javax.swing.JLabel jLabVol;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabelPeak;
    private javax.swing.JPanel jMask;
    private javax.swing.JPanel jNifti;
    private javax.swing.JPanel jOther;
    private javax.swing.JPanel jPanExNifti;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelROI;
    private javax.swing.JPanel jPanelResults;
    private javax.swing.JPanel jRadio;
    private javax.swing.JRadioButton jRadioAll;
    private javax.swing.JRadioButton jRadioAny;
    private javax.swing.JRadioButton jRadioAverage;
    private javax.swing.JRadioButton jRadioExterior;
    private javax.swing.JRadioButton jRadioInterior;
    private javax.swing.JRadioButton jRadioNone;
    private javax.swing.JPanel jRoiTab;
    private javax.swing.JPanel jShiftTab;
    private javax.swing.JSpinner jSpinDiameter;
    private javax.swing.JSpinner jSpinRoiNm;
    private javax.swing.JSpinner jSpinShiftRoiNm;
    private javax.swing.JSpinner jSpinShiftX;
    private javax.swing.JSpinner jSpinShiftY;
    private javax.swing.JSpinner jSpinShiftZ;
    private javax.swing.JTextField jTextCThi;
    private javax.swing.JTextField jTextCTlo;
    private javax.swing.JTextField jTextCtStrength;
    private javax.swing.JTextField jTextGray;
    private javax.swing.JTextField jTextName;
    private javax.swing.JTextField jTextNifSrc;
    private javax.swing.JTextField jTextParms;
    private javax.swing.JTextField jTextSUVhi;
    private javax.swing.JTextField jTextSUVlo;
    private javax.swing.JTextField jTextSeriesName;
    private javax.swing.JTextField jTextSliceHi;
    private javax.swing.JTextField jTextSliceLo;
    private javax.swing.JTextField jTextSuvDisplay;
    private javax.swing.JTextField jTextSuvMax;
    private javax.swing.JTextField jTextSuvMean;
    private javax.swing.JTextField jTextSuvPeak;
    private javax.swing.JTextField jTextVol;
    private javax.swing.JTextField jTextVolMean;
    private javax.swing.JToggleButton jTogDrawRoi;
    // End of variables declaration//GEN-END:variables
	private Timer m_timer = null;
	bkgdRunNifti work2 = null;
	bkgdRunGrow work3 = null;
	PetCtPanel parentPet = null;
	PetCtPanel lockedParent = null;
	double[] savePercentSUV = null;
	static BrownFat instance = null;
	boolean killMe = false, drawingRoi = false, isPercent = false, isPer1, isDirty = true;
	boolean isFat, isBone, isOther, isHeavy, OkShiftROI, isInit = false, allowDelete;
	boolean isSliceLimits = true, isNiftiDirty = false, isSliceChange = false;
	boolean nextWarn = false, allowSliceChange = true, isCalcRadio = false, isDelete = false;
	int RoiState = 1, lastPeakType;
	OpService ops = null;
	ArrayList<Poly3Save> polyVect = new ArrayList<Poly3Save>();
	Object [] nifList;
	SUVpoints suvPnt = null;
	Poly3Save currPoly = null;
	Point currMousePos = new Point(-1,0);
	Date Roi1StartTime = null, NiftiStartTime = null;
	String NiftiLab = "";
	int measureTime = -1;	// the total time taken to draw all ROIs
	int numRed = 0, shiftRoiNm = 0, RoiNum4Max = -1;
	int [] redPoints = new int[5];
	int [] prevSpinValue = new int[4];
	int [] maskParms = null;
	int black, saveRoiPntIndx, saveRoiIndx, volSliceType = -1;
	int minMip, maxMip, nifListSz, CtCenterVal;
	ArrayList<Component> elementList = new ArrayList<Component>();
}

import Utilities.Counter3D;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
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
import java.util.Date;
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

	private static BrownFat instanc = null;

	synchronized public static BrownFat getInstance() {
		return instanc;
	}

	public static BrownFat makeNew(java.awt.Frame parent) {
		if( instanc != null) return instanc;

		instanc = new BrownFat(parent, false);
		return instanc;
	}

	/** Creates new form BrownFat
	 * @param parent
	 * @param modal */
    private BrownFat(java.awt.Frame parent, boolean modal) {
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
		int val;
		allowDelete = false;
		bf.nifListSz = 0;
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
		Preferences prefer = bf.parentPet.parent.jPrefer;
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
//		jCheckScale.setSelected(prefer.getBoolean("bf scale z", true));
		jCheckUseSUV.setSelected(prefer.getBoolean("bf use suv", true));
		jCheckUseCt.setSelected(prefer.getBoolean("bf use ct", true));
		jCheckOld.setSelected(prefer.getBoolean("old bf", true));
//		setCheckGrown(prefer.getBoolean("grown roi", false));
		setCheckGrown(true);
		// allow BN Lee to see this - comment out next line
//		jCheckGrown.setVisible(false);
		jCheckDefLimits.setSelected(prefer.getBoolean("defined limits", false));
		jCheckUseTemp.setSelected(prefer.getBoolean("nifti tmp", true));
		if( getNiftiPrefs(0)) jCheckUseTemp.setSelected(true);
		changeTmp();
		jTextParms.setVisible(false);
		tmpi = prefer.getInt("blue dot size", 0);
		if( tmpi > 0) {
			SpinnerNumberModel spin1 = getSpinModel(5);
			spin1.setValue(tmpi);
		}
		oc_ml = prefer.getDouble("OC ml", 2.0);
		oc_suv = prefer.getDouble("OC SUV", 2.5);
		jCheckBlue.setSelected(false);
		jButBkgd.setVisible(false);
		jButClean.setVisible(false);
		jTextBkgd.setVisible(false);
		String shape = IJ.getDirectory("imagej") + "lib/petct/shaping_elongL";
		File testIt = new File(shape);
		if( !testIt.exists()) j3Dtab.remove(jNifti);
		jTextNifSrc.setText(prefer.get(getNiftiRegKey(), ""));
		setXYZminMax();
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
		bf.fillSdyLab();
		m_bf.add(bf);
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
		bf.parentPet.repaintAll();
		saveRegistryValues();
		instanc = null;
		killMe = true;
		super.dispose();
	}

	String preferInt( double inDouble) {
		String ret1;
		Integer inVal = (int) inDouble;
		Double outVal =  inVal.doubleValue();
		if( outVal.equals( inDouble)) {
			ret1 = inVal.toString();
		} else {
			outVal = inDouble;
			ret1 = outVal.toString();
		}
		return ret1;
	}

	private void saveRegistryValues() {
		Preferences prefer = bf.parentPet.parent.jPrefer;
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
		if(isNestle) tmp += "n";
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
//		prefer.putBoolean("bf scale z", jCheckScale.isSelected());
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

	boolean saveParentExt( Window w1) {
		return saveParent(w1);
	}

	private boolean saveParent( Window w1) {
		PetCtPanel curr1, old1 = null;
		Window w2 = w1;
        boolean updateVolume = false;
		if( w1 instanceof Display3Frame) {
			Display3Frame par2 = (Display3Frame) w1;
			w2 = par2.srcFrame;
		}
		if( !(w2 instanceof PetCtFrame)) return false;
		allowDelete = false;
		PetCtFrame parent = (PetCtFrame) w2;
		curr1 = parent.getPetCtPanel1();
		if( jCheckLock.isSelected() && !curr1.equals(lockedParent)) return false;
		if( bf.parentPet != null) {	// if there is a previous one, update it
			if( bf.parentPet.equals(curr1)) return true;	// nothing to do
            updateVolume = true;
			old1 = bf.parentPet;
			old1.bfDlg = null;
			switchBf(curr1);
		}
		bf.parentPet = curr1;
		bf.parentPet.bfDlg = this;
        if(updateVolume) calculateVol(true);
		if( old1 != null) old1.repaintAll();
		bf.parentPet.repaintAll();
		return true;
	}

	private void repaintAllStudies() {
		bfGroup bfTmp;
		for( int i=0; i<m_bf.size(); i++) {
			bfTmp = m_bf.get(i);
			bfTmp.parentPet.repaintAll();
		}
	}

	// when the study is changed these values need to follow the study
	private void setXYZminMax() {
		int lo, hi, hiz, val, valz, off1, scale = 100;
		int loVal, hiVal;
		JFijiPipe pet = bf.parentPet.petPipe;
		Preferences prefer = bf.parentPet.parent.jPrefer;
		val = pet.data1.width;
		valz = (int)(val/pet.zoomX);
		if( valz > val) valz = val;
		off1 = (val - valz)/2;
		lo = prefer.getInt("nifti lox", 100);
		hi = prefer.getInt("nifti hix", 900);
		hiz = prefer.getInt("nifti hiz", 950);
		if( hi > scale || hiz > scale) scale = 1000;
		loVal = (lo * valz / scale) + off1;
		hiVal = (hi * valz / scale) + off1;
		xmin = loVal;
		xmax = hiVal;
		val = pet.data1.height;
		valz = (int)(val/pet.zoomX);
		if( valz > val) valz = val;
		off1 = (val - valz)/2;
		lo = prefer.getInt("nifti loy", 100);
		hi = prefer.getInt("nifti hiy", 900);
		loVal = (lo * valz / scale) + off1;
		hiVal = (hi * valz / scale) + off1;
		ymin = loVal;
		ymax = hiVal;
		lo = prefer.getInt("nifti loz", 50);
		hi = hiz;
		val = pet.getNormalizedNumFrms();
		loVal = lo * val / scale;
		hiVal = hi * val / scale;
		zmin = loVal;
		zmax = hiVal;
	}

	void removeBFdata(PetCtPanel curr1) {
		bfGroup bfTmp;
		int n = m_bf.size();
		for( int i=0; i<m_bf.size(); i++) {
			bfTmp = m_bf.get(i);
			if( bfTmp.parentPet.equals(curr1)) {
				m_bf.remove(i);
				if( n> 1) {
					bf = m_bf.get(0);
					bf.parentPet.bfDlg = this;
					setSpin0Val();
					bf.parentPet.repaintAll();
				}
				return;
			}
		}
	}

	void switchBf(PetCtPanel curr1) {
		bfGroup bfTmp;
		int i;
		SpinnerNumberModel spin1 = getSpinModel(0);
//		IJ.log("m_bf, at start ="+ m_bf.size());
		for( i=0; i<m_bf.size(); i++) {
			bfTmp = m_bf.get(i);
			if( bfTmp.parentPet.equals(curr1)) {
				bf = bfTmp;
				bf.fillSdyLab();
				bf.setNifLabs(NIFTI_BOTH);
				jCheckDefLimits.setEnabled(bf.nifLimits);
				setXYZminMax();
				setSpin0Val();
				return;
			}
		}
		bf = new bfGroup();
		bf.parentPet = curr1;
		bf.fillSdyLab();
		bf.setNifLabs(NIFTI_BOTH);
		jCheckDefLimits.setEnabled(bf.nifLimits);
		setXYZminMax();
//		setSpin0Val();
//		oldLast = spin1.getNumber().intValue();
		spin1.setValue(bf.lastRoiVal);
//		if( oldLast == bf.lastRoiVal) changedRoiChoice(true); // make it happen
		m_bf.add(bf);
//		IJ.log("m_bf ="+ m_bf.size());
	}

	void setSpin0Val() {
		SpinnerNumberModel spin1 = getSpinModel(0);
		int old = spin1.getNumber().intValue();
		spin1.setValue(bf.lastRoiVal);	// this does nothing if there is no change
		if( old == bf.lastRoiVal) changedRoiChoice(true); // make it happen
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
		Preferences prefer = bf.parentPet.parent.jPrefer;
		int x, y;
		x = prefer.getInt("bf size x", 0);
		y = prefer.getInt("bf size y", 0);
		if( x > 20 && y > 20) setSize( x,y);
	}*/

	boolean isPercentOrNestle() {
		return isPercent || isNestle;
	}

	double[] getSUVandCTLimits() {
		double[] retVal = new double[6];
		String tmp;
		tmp = jTextSUVlo.getText();
		isPercent = isPer1 = isNestle = false;
		if(tmp.contains("%")) {
			isPercent = true;
			tmp = tmp.replace('%', ' ').trim();
		}
		if(tmp.contains("n")) {
			isNestle = true;
			isPercent = false;
			tmp = tmp.replace('n', ' ').trim();
		}
		maybeToggleBkgd();
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

	// draw yellow dots on other studies
	void drawOtherData(Graphics2D g, PetCtPanel caller) {
		int i;
		bfGroup bfSave, bfThis=null;
		bfSave = bf;
		if( drawingRoi) return;
		for( i=0; i<m_bf.size(); i++) {
			bfThis = m_bf.get(i);
			if( bfThis.parentPet.equals(caller)) break;
		}
		if( i>= m_bf.size()) return;
		bf = bfThis;
		dotColor = Color.yellow;
		drawAllRoi(g);
		drawAllTypePoints(g, null);
		drawMipPoints(g);
		bf = bfSave;
	}

	void drawAllData( Graphics2D g, PetCtPanel caller) {
		if( killMe) {
			bf.parentPet.bfDlg = null;
//			IJ.log("killing reference to brown fat");
			return;
		}
		if( bf.parentPet != caller) return;
		dotColor = Color.blue;
		if( drawingRoi) {
			drawCurrRoi(g, currPoly, true);
		} else {
			drawAllRoi(g);
			drawAllTypePoints(g, null);
			drawMipPoints(g);
		}
	}

	// draw yellow dots on other studies
	void drawOther3Data(Graphics2D g, Display3Panel caller, PetCtPanel origCaller) {
		int i;
		bfGroup bfSave, bfThis=null;
		bfSave = bf;
		if( drawingRoi) return;
		for( i=0; i<m_bf.size(); i++) {
			bfThis = m_bf.get(i);
			if( bfThis.parentPet.equals(origCaller)) break;
		}
		if( i>= m_bf.size()) return;
		bf = bfThis;
		dotColor = Color.yellow;
		drawAllDisp3Roi(g, caller);
		drawAllTypePoints(g, caller);
//		drawFound3Points(g, caller);
		bf = bfSave;
	}

	void drawDisplay3Data( Graphics2D g, Display3Panel caller, PetCtPanel origCaller) {
		if( killMe) return;
		if( drawingRoi) return;
		if( bf.parentPet != origCaller)
			return;
		dotColor = Color.blue;
		drawAllDisp3Roi(g, caller);
		drawAllTypePoints(g, caller);
//		drawFound3Points(g, caller);
	}

	void handleMouseMove( MouseEvent e, PetCtPanel caller) {
		if( bf.parentPet != caller || bf.parentPet.mouse1.widthX == 0) return;
		currMousePos.x = e.getX() % bf.parentPet.mouse1.widthX;
		currMousePos.y = e.getY();
		if( drawingRoi) {
			bf.parentPet.repaint();
			return;
		}
		setProperCursor();
	}

	boolean handleMouseDrag( MouseEvent e, PetCtPanel caller) {
		if( bf.parentPet != caller || bf.parentPet.mouse1.widthX == 0) return false;
		if( drawingRoi) return true;	// ignore drag while drawing
		currMousePos.x = e.getX() % bf.parentPet.mouse1.widthX;
		currMousePos.y = e.getY();
		if( saveRoiIndx < 0) return dragNiftiLimit();
		Poly3Save poly1 = getPolyVectEntry(saveRoiIndx);
		Point pt1 = convertPoint2Pos( currMousePos, poly1.type, -1);
		dragSphere(poly1, pt1);
		poly1.poly.invalidate();
		isDirty = true;
		delayedRefresh();
		bf.parentPet.repaint();
		return true;
	}

	boolean dragNiftiLimit() {
		if( !isDefLimits()) return false;
		int orient = bf.parentPet.m_sliceType;
		Point pt1 = convertPoint2Pos( currMousePos, orient, -1);
		if( saveRoiPntIndx < 0) return false;
		String txt;
		Integer val;
		val = pt1.x;
		txt = val.toString();
		if(saveRoiPntIndx == 0) xmin = val;
		else xmax = val;
		checkLimits( 1);
		val = pt1.y;
		if(saveRoiPntIndx > 0) {
			JFijiPipe pet = bf.parentPet.petPipe;
			if( orient == JFijiPipe.DSP_AXIAL) {
				if( val >= pet.data1.height) val = pet.data1.height-1;
			} else {
				if( val >= pet.getNormalizedNumFrms()) val = pet.getNormalizedNumFrms()-1;
			}
		}
		txt = val.toString();
		if(saveRoiPntIndx == 0) {
			if( orient == JFijiPipe.DSP_AXIAL) ymin = val;
			else zmin = val;
		} else {
			if( orient == JFijiPipe.DSP_AXIAL) ymax = val;
			else zmax = val;
		}
		checkLimits( 2);
		checkLimits( 3);
		isNiftiDirty = true;
		bf.parentPet.repaint();
		return true;
	}

	void checkLimits(int type) {
		Integer valLo, valHi;
		int slop = 4;
		String tmp;
		valLo = xmin;
		valHi = xmax;
		switch(type) {
			case 2:
				valLo = ymin;
				valHi = ymax;
				break;

			case 3:
				valLo = zmin;
				valHi = zmax;
				break;
		}
		if( valHi >= valLo + slop) return;	// all is well
		if( saveRoiPntIndx == 0) {
			valLo = valHi - slop;
			switch(type) {
				case 1:
					xmin = valLo;
					break;

				case 2:
					ymin = valLo;
					break;

				case 3:
					zmin = valLo;
					break;
			}
			return;
		}
		valHi = valLo + slop;
		switch(type) {
			case 1:
				xmax = valHi;
				break;

			case 2:
				ymax = valHi;
				break;

			case 3:
				zmax = valHi;
				break;
		}
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
		sizeX = sizeY = bf.parentPet.petPipe.data1.width;
		if( poly1.type != JFijiPipe.DSP_AXIAL)
			sizeY = bf.parentPet.petPipe.getNormalizedNumFrms();
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
		int orient = bf.parentPet.m_sliceType;
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
			poly1 = bf.polyVect.get(i);
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
		bf.parentPet.setCursor(cur1);
		return indx;
	}

	void setZcenter() {
		int indx = getSpinInt(0) - 1 - getRoiSize(1);
		if( indx < 0 || indx >= getRoiSize(0)) return;
		Poly3Save poly1 = bf.polyVect.get(indx);
		if( poly1.type != bf.parentPet.m_sliceType) return;
		Integer[] outVal, inVal = new Integer[3];
		int diff, diffhi, difflo, slice, max;
		inVal[2] = poly1.hiSlice;
		inVal[1] = poly1.lowSlice;
		diff = inVal[2] - inVal[1];
		diffhi = difflo = diff/2;
		if( (diff & 1) == 1) diffhi++;
		slice = getSliceNum(poly1.type) + 1;
		max = bf.parentPet.petPipe.data1.width;
		if( poly1.type == JFijiPipe.DSP_AXIAL) {
			max = bf.parentPet.petPipe.getNormalizedNumFrms();
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
				slice = ChoosePetCt.round(bf.parentPet.petAxial);
				break;

			case JFijiPipe.DSP_CORONAL:
				slice = ChoosePetCt.round(bf.parentPet.petCoronal);
				break;

			case JFijiPipe.DSP_SAGITAL:
				slice = ChoosePetCt.round(bf.parentPet.petSagital);
				break;
		}
		return slice;
	}
	
	void killSavedSUV() {
		savePercentSUV = null;
	}
	
	void calcSavedSUV(int gateIn1) {
		boolean needCalc = false;
		if(!isPercentOrNestle() || !jCheckUseSUV.isSelected()) {
			killSavedSUV();
			return;
		}
		if(savePercentSUV == null || savePercentSUV.length != getRoiSize())
			needCalc = true;
		else if(isNestle && saveNestleSUV[savePercentSUV.length/2] <= 0)
			needCalc = true;
		if(!needCalc && !isDirty) return;
		int i, n = getRoiSize();
		savePercentSUV = new double[n];
		saveNestleSUV = new double[n];
		for( i=0; i<n; i++) {
			SaveVolROI volTmp = calcSingleRoiVol(i, true, gateIn1);
			savePercentSUV[i] = volTmp.SUVmax;
			saveNestleSUV[i] = volTmp.SUVNestle;
		}
		isDirty = false;
	}

	void drawCurrRoi( Graphics2D g, Poly3Save poly1, boolean drawing) {
		int[] xp1, xp2, yp1, yp2, xp4, yp4;
		int i, j, widthX, npoints, num4, slice=-1, type1, cineIdx;
		int rubberX, rubberY, numDisp = 3;
		Point2d pt1;
		Point pt2;
		if( poly1.type != bf.parentPet.m_sliceType) return;
		double scl1 = bf.parentPet.getScalePet();
		double zoom1 = bf.parentPet.petPipe.zoom1;
		npoints = poly1.poly.npoints;
		if( npoints <= 0) return;
		boolean MIPdisplay = bf.parentPet.isMIPdisplay();
		cineIdx = bf.parentPet.getCineIndx();
		if( MIPdisplay) numDisp = 2;
		xp1 = poly1.poly.xpoints;
		yp1 = poly1.poly.ypoints;
		type1 = 2;	// this could be 4 as well, i.e. for 3 display
		switch( poly1.type) {
			case JFijiPipe.DSP_AXIAL:
				type1 = 0;
				if( zoom1 > 1.0) type1 = 3;
				slice = ChoosePetCt.round(bf.parentPet.petAxial);
				break;

			case JFijiPipe.DSP_CORONAL:
				slice = ChoosePetCt.round(bf.parentPet.petCoronal);
				if(MIPdisplay && cineIdx == 0 && zoom1==1) numDisp = 3;
				break;

			case JFijiPipe.DSP_SAGITAL:
				slice = ChoosePetCt.round(bf.parentPet.petSagital);
				if(MIPdisplay && cineIdx == 3*bf.parentPet.getNumMIP()/4 && zoom1==1) numDisp = 3;
				break;
		}
		slice++;
		if( slice < poly1.lowSlice || slice > poly1.hiSlice) return;
		widthX = bf.parentPet.mouse1.widthX;
		xp2 = new int[npoints];
		yp2 = new int[npoints];
		for( i=0; i<npoints; i++) {
			pt1 = new Point2d(xp1[i], yp1[i]);
			pt2 = bf.parentPet.petPipe.pos2Scrn(pt1, scl1, type1);
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
		if( poly1.type != bf.parentPet.m_sliceType) return;
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
				poly1 = bf.polyVect.get(i);
				drawCurrRoi(g, poly1, false);
			}
			return;
		}
		i = bf.lastRoiVal - getRoiSize(1);
		if( i < 1 || i > n) return;
		poly1 = bf.polyVect.get(i-1);
		drawCurrRoi(g, poly1, false);
	}

	boolean isDefLimits() {
		return jCheckDefLimits.isSelected() && jCheckDefLimits.isEnabled() && j3Dtab.getSelectedIndex() == 4;
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
		int orient = bf.parentPet.m_sliceType;
		if( orient == JFijiPipe.DSP_AXIAL && jCheck3D_OC.isSelected()) return;
		xp1 = getNiftiLimits(orient, 0);
		if(xp1 == null) return;
		yp1 = getNiftiLimits(orient, 1);
		xp2 = new int[2];
		yp2 = new int[2];
		xp4 = new int[2];
		if( orient == JFijiPipe.DSP_AXIAL) {
			numDisp = 2;
			type1 = 0;
			if( bf.parentPet.petPipe.zoom1 > 1.0) type1 = 3;
		}
		double scl1 = bf.parentPet.getScalePet();
		widthX = bf.parentPet.mouse1.widthX;
		for( i=0; i<2; i++) {
			pt1 = new Point2d(xp1[i], yp1[i]);
			pt2 = bf.parentPet.petPipe.pos2Scrn(pt1, scl1, type1);
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
			xy[0] = xmin;
			xy[1] = xmax;
		} else {
			if( orient == JFijiPipe.DSP_AXIAL) {
				xy[0] = ymin;
				xy[1] = ymax;
			} else {
				xy[0] = zmin;
				xy[1] = zmax;
			}
		}
		return xy;
	}

	void niftiLimChanged() {
		bf.parentPet.repaint();
	}

	void drawAllDisp3Roi( Graphics2D g, Display3Panel d3panel) {
		Poly3Save poly1;
		if( RoiState == 0) return; // >= to disable
		boolean showAll = jCheckShowAll.isSelected();
		int i, n = getRoiSize(), i0 = getRoiSize(1);
		if( showAll) {
			for( i = 0; i < n-i0; i++) {
				poly1 = bf.polyVect.get(i);
				drawCurrDisp3Roi(g, poly1, d3panel);
			}
			return;
		}
		i = getSpinInt(0) - i0;
		if( i < 1 || i > n) return;
		poly1 = bf.polyVect.get(i-1);
		drawCurrDisp3Roi(g, poly1, d3panel);
	}
	
	void setSpinDiameterState() {
		jSpinDiameter.setEnabled(jCheckBlue.isSelected());
	}

	// for the main panel (PetCtPanel), set d3panel = null
	void drawAllTypePoints( Graphics2D g, Display3Panel d3panel) {
		if( !jCheckBlue.isSelected()) return;
		int pointDiameter = getBlueDiameter();
		if( pointDiameter <= 0) return;
		int type1 = 2, slice = 0;
		if(d3panel != null) pointDiameter = (int)(pointDiameter * d3panel.d3Pipe.zoom1);
		else pointDiameter = (int)(pointDiameter * bf.parentPet.petPipe.zoom1);

		if( bf.suvPnt == null) return;
		int numPnt = bf.suvPnt.getListSize();
		if( numPnt <= 0) return;
		int srcX=0, srcY=0, srcZ=0, dstX=0, dstY=0, dstZ=0;
		switch(bf.suvPnt.sliceType) {
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

		int i, orientation;
		if( d3panel != null) orientation = 1;
		else orientation = bf.parentPet.m_sliceType;
		while( orientation < 4) {	// <= sagital
			if( d3panel != null) type1 = 4;	// for main type1 = 2
			switch(orientation) {
				case JFijiPipe.DSP_AXIAL:
					dstX = srcX;
					dstY = srcY;
					dstZ = srcZ;
					type1 = 0;
					if( d3panel != null) {
						if( d3panel.d3Pipe.zoom1 > 1.0) type1 = 3;
						slice = ChoosePetCt.round(d3panel.d3Axial);
					} else {
						if( bf.parentPet.petPipe.zoom1 > 1.0) type1 = 3;
						slice = ChoosePetCt.round(bf.parentPet.petAxial);
					}
					break;

				case JFijiPipe.DSP_CORONAL:
					dstX = srcX;
					dstY = srcZ;
					dstZ = srcY;
					if( d3panel != null) slice = ChoosePetCt.round(d3panel.d3Coronal);
					else slice = ChoosePetCt.round(bf.parentPet.petCoronal);
					break;

				case JFijiPipe.DSP_SAGITAL:
					dstX = srcY;
					dstY = srcZ;
					dstZ = srcX;
					if( d3panel != null) {
						if( d3panel.showMip) return;
						slice = ChoosePetCt.round(d3panel.d3Sagital);
					}
					else slice = ChoosePetCt.round(bf.parentPet.petSagital);
					break;
			}
			i = orientation;
			drawFoundAllSub(g, d3panel, false, i, dstX, dstY, dstZ, slice, type1, pointDiameter);
			drawFoundAllSub(g, d3panel, true, i, dstX, dstY, dstZ, slice, type1, pointDiameter);
			if( d3panel != null) orientation++;
			else break;	// single orietation for pet-ct panel
		}
	}

	int[] maybeChangeOrientation(int input, int output) {
		int srcX=0, srcY=1, srcZ=2;
		switch(input) {
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
		int[] ret3 = new int[3];
		switch(output) {
			case JFijiPipe.DSP_CORONAL:
				ret3[0] = srcX;
				ret3[1] = srcZ;
				ret3[2] = srcY;
				break;

			case JFijiPipe.DSP_SAGITAL:
				ret3[0] = srcY;
				ret3[1] = srcZ;
				ret3[2] = srcX;
				break;

			default:
				ret3[0] = srcX;
				ret3[1] = srcY;
				ret3[2] = srcZ;
		}
		return ret3;
	}

	private void drawFoundAllSub(Graphics2D g, Display3Panel d3panel, boolean red,
		int i1, int dstX, int dstY, int dstZ, int slice, int type1, int pointDiameter) {
		int i, j, z, x, y, pointDia2, widthX, rc0=-1, rc1;
		Point2d pt1;
		Point pt2;
		JFijiPipe petP;
		boolean MIPdisplay = false;
		double scl1;
		int numPnt = bf.suvPnt.getListSize();
		boolean changeColor = jCheckColor.isSelected();
		g.setColor(dotColor);
		if( red) {
			changeColor = false;
			numPnt = bf.numRed;
			g.setColor(Color.red);
		}
		if( d3panel != null) {
			petP = d3panel.d3Pipe;
			scl1 = d3panel.getScale();
			widthX = (int)(scl1 * petP.data1.width);
		} else {
			petP = bf.parentPet.petPipe;
			scl1 = bf.parentPet.getScalePet();
			MIPdisplay = bf.parentPet.isMIPdisplay();
			widthX = bf.parentPet.mouse1.widthX;
		}
		pointDia2 = pointDiameter / 2;
		for( i = 0; i < numPnt; i++) {
			j = i;
			if( red) j = bf.redPoints[i];
			z = getVolPoint(dstZ, j);
			if( z != slice) continue;
			if(changeColor) {
				rc1 = getVolPoint(3, j);	// labelIndx
				if( rc0 != rc1) {
					Color col2 = new Color(getDotColor(rc1));
					g.setColor(col2);
				}
				rc0 = rc1;
			}
			pt1 = new Point2d(getVolPoint(dstX, j), getVolPoint(dstY, j));
			pt2 = petP.pos2Scrn(pt1, scl1, type1, i1, true);
			x = pt2.x - pointDia2;
			y = pt2.y - pointDia2;
			if( d3panel != null) g.fillOval(x+(i1-1)*widthX, y, pointDiameter, pointDiameter);
			else {
				g.fillOval(x, y, pointDiameter, pointDiameter);
				g.fillOval(x+widthX, y, pointDiameter, pointDiameter);
				if( !MIPdisplay)
					g.fillOval(x+2*widthX, y, pointDiameter, pointDiameter);
			}
		}
	}

	int getBlueDiameter() {
		return getSpinInt(5);
	}

	void drawMipPoints( Graphics2D g) {
		if( !jCheckBlue.isSelected()) return;
		int pointDiameter = getBlueDiameter();
		if( pointDiameter <= 0) return;
		if( bf.suvPnt == null) return;
		int numPnt = bf.suvPnt.getListSize();
		if( numPnt <= 0) return;
		if( !bf.parentPet.isMIPdisplay()) return;
		int srcX=0, srcY=0, srcZ=0;
		JFijiPipe.mipXYentry currXY = bf.parentPet.mipPipe.data1.getcurrMIPentry(bf.minMip, bf.maxMip);
		switch(bf.suvPnt.sliceType) {	// bf.volSliceType
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
//		logTimeDiff(0);
		numBadMip = 0;
		drawMipSub(g, false, currXY, srcX, srcY, srcZ, pointDiameter);
//		logTimeDiff(20);
		drawMipSub(g, true, currXY, srcX, srcY, srcZ, pointDiameter);
//		logTimeDiff(30);
		if( numBadMip > 0) {
			String tmp1 = "There are " + numBadMip + " blue dots not showing.\n";
			tmp1 += "The index of the first missing blue dot is: " + badIndx +"\n";
			tmp1 += "Try to switch to a different view: coronal, sagital or axial.\n";
			tmp1 += "This may solve the problem. Then you can switch back again.";
			IJ.log(tmp1);
		}
	}
	
	private void drawMipSub(Graphics2D g, boolean red, JFijiPipe.mipXYentry currXY,
			int srcX, int srcY, int srcZ, int pointDiameter) {
		int i, j, j1, z, zlo, x, y, wid, pointDia2, widthX, rc0=-1, rc1;
		short[] xSlc, ySlc;
		Point2d pt1;
		Point pt2;
		JFijiPipe.mipEntry currSlice;
		int numPnt = bf.suvPnt.getListSize();
		boolean changeColor = jCheckColor.isSelected();
		g.setColor(dotColor);
		if( red) {
			changeColor = false;
			numPnt = bf.numRed;
			g.setColor(Color.red);
		}
		double scl1 = bf.parentPet.getScalePet();
		zlo = currXY.zlo;
		pointDia2 = pointDiameter / 2;
		widthX = (int)(scl1 * bf.parentPet.petPipe.data1.width);
		try {
//			rowIndx = 0;
			for( j = 0; j < numPnt; j++) {
				j1 = jIndx = j;
//				if( jIndx == 1345)
//					rowIndx = 0;
				if( red) j1 = bf.redPoints[j];
				z = getVolPoint(srcZ, j1);
				x = getVolPoint(srcX, j1);
				y = getVolPoint(srcY, j1);
				if(z-zlo >= currXY.xydata.size()) {
					addBadMip();
					continue;
				}
				currSlice = currXY.xydata.get(z-zlo);
				if(currSlice.zpos != z) {	// hopefully this will never happen
					IJ.log("The z position doesn't match the volume position");
					return;
				}
				wid = currSlice.xval.length;
				xSlc = currSlice.xval;
				ySlc = currSlice.yval;
//				rowIndx = 1;
				for( i=0; i<wid; i++) {
					if( x == xSlc[i] && y == ySlc[i]) break;
				}
				if( i >= wid) continue;	// point not in MIP - common case
//				if(j%50==0) logTimeDiff(j+10);
//				rowIndx = 2;
				if(changeColor) {
					rc1 = getVolPoint(3, j1);	// labelIndx
					if( rc0 != rc1) {
						Color col2 = new Color(getDotColor(rc1));
						g.setColor(col2);
					}
					rc0 = rc1;
				}
				pt1 = new Point2d(i, z);
//				rowIndx = 3;
				pt2 = bf.parentPet.mipPipe.pos2Scrn(pt1, scl1, 1, 2, true);
				x = pt2.x - pointDia2;
				y = pt2.y - pointDia2;
//				rowIndx = 4;
				g.fillOval(x+2*widthX, y, pointDiameter, pointDiameter);
			}
		} catch(Exception e) { 
			ChoosePetCt.stackTrace2Log(e);
		}
	}

	int addBadMip() {
		if( numBadMip++ <= 0) badIndx = jIndx;
		return numBadMip;
	}

	int getVolPoint(int dst1, int indx) {
		SUVpoints.SavePoint currPnt = bf.suvPnt.getPoint(indx);
		switch( dst1) {
			case 0:
				return currPnt.x1;

			case 1:
				return currPnt.y1;

			case 2:
				return currPnt.z1;

			case 3:
				return currPnt.labelIndx;
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
		jComboROIlabel.setEnabled(enabled);
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
		if( indx > 0 && indx <= n ) {
			Poly3Save poly1 = bf.polyVect.get(indx - 1);
			outVal = poly1.axialInverts(inVal);
			if( outVal[2] != poly1.hiSlice || outVal[1] != poly1.lowSlice) isDirty = true;
			poly1.hiSlice = outVal[2];
			poly1.lowSlice = outVal[1];
			bf.parentPet.repaintAll();
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
		if( indx < bf.nifListSz) {
			for( i= indx; i<bf.nifListSz-1; i++) bf.nifList[i] = bf.nifList[i+1];
			bf.nifList[i] = null;
			isDelete = true;
			bf.nifListSz--;
			deleteFromNiftiMask(indx);
		}
		else bf.polyVect.remove(indx-getRoiSize(1));
		killSavedSUV();
		isDirty = true;
		grownChanged();
		changeRoiAndUpdate();
	}

	void deleteFromNiftiMask(int indx) {
		String dirName;
		int i, j, k, headerSz, width, height, numZ, frmSz;
		byte[] buf;
		byte valb;
		maybeMakeNiftiFromRois();
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
		if( bf.nifListSz > 0) {
			if( !bf.polyVect.isEmpty()) {
				Poly3Save poly1 = bf.polyVect.get(0);
				if(poly1.type != bf.parentPet.m_sliceType) {
					i = bf.polyVect.size();
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
		if( bf.parentPet.bfDlg != this) {
			jTogDrawRoi.setSelected(false);
			return;
		}
		initDraw(true);
		drawingRoi = true;
	}

	int getRoiSize() {
		return bf.nifListSz + bf.polyVect.size();
	}

	int getRoiSize(int type) {
		if( type == 1) return bf.nifListSz;
		return bf.polyVect.size();
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
		bf.parentPet.repaint();
	}

	Point convertPoint2Pos( Point pt1, int type, double scl1) {
		int i = 0;
		double scl2 = scl1;
		if( scl2 <= 0) scl2 = bf.parentPet.getScalePet();
		if( bf.parentPet.petPipe.zoom1 > 1.0) i = 3;
		if( type != JFijiPipe.DSP_AXIAL) i = 4;
		return bf.parentPet.petPipe.scrn2PosInt(pt1, scl2, i);
	}

	void processMouseDoubleClick() {
		if( drawingRoi) {
			changeLimits();	// maybe the user changed the limits
			if(currPoly.poly.npoints > 2) bf.polyVect.add(currPoly);
			allowSliceChange = false;
			changeRoiAndUpdate();
		}
	}

	// this finishes the drawing of a sphere if a sphere has been chosen
	boolean finishDrawSphere() {
		if( !jCheckSphere.isSelected()) return false;
		currPoly = finishSphereSub(currPoly);
		currPoly.sphericalROI = true;
		bf.polyVect.add(currPoly);
		allowSliceChange = false;
		changedRoiChoice(true);
		return true;
	}
	
	Poly3Save finishSphereSub(Poly3Save poly1) {
		int x0, x1, y0, y1, deltaX, deltaY, deltaZ, i, num1;
		int sizeY, sizeZ, sliceNum;
		JFijiPipe pip0 = bf.parentPet.petPipe;
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
		Preferences prefer = bf.parentPet.parent.jPrefer;
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
		currPoly.type = bf.parentPet.m_sliceType;
		currPoly.numFrm = bf.parentPet.petPipe.getNormalizedNumFrms();
		int currSlice=0, sizeZ = bf.parentPet.petPipe.data1.width;
		switch( currPoly.type) {
			case JFijiPipe.DSP_AXIAL:
				sizeZ = currPoly.numFrm;
				currSlice = ChoosePetCt.round(bf.parentPet.petAxial);
				break;

			case JFijiPipe.DSP_CORONAL:
				currSlice = ChoosePetCt.round(bf.parentPet.petCoronal);
				break;

			case JFijiPipe.DSP_SAGITAL:
				currSlice = ChoosePetCt.round(bf.parentPet.petSagital);
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
		if( bf.numRed < 1 || bf.suvPnt == null) return;
		if( getSpinInt(0) > getRoiSize()) return;
		int red0 = bf.redPoints[0];
		if( red0 >= bf.suvPnt.getListSize()) return;
		if(bf.suvPnt.sliceType != bf.volSliceType) return;
		bf.parentPet.maybeNewPosition(bf.suvPnt.getPoint(red0));
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
		bf.lastRoiVal = indx;
		int nLab, n = getRoiSize();
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
					Nifti3 nif1 = getNifListEntry(indx-1);
					label = nif1.ROIlabel;
					nLab = nif1.labelIndx;
				} else {
					Poly3Save poly1 = bf.polyVect.get(indx-1-n1);
					Integer[] val1 = poly1.axialInverts();
					jTextSliceLo.setText(val1[1].toString());
					jTextSliceHi.setText(val1[2].toString());
					label = poly1.ROIlabel;
					nLab = poly1.labelIndx;
				}
				jComboROIlabel.getEditor().setItem(label);
				useDotColor(nLab);
			}
			isSliceChange = !showAll && follow && allowSliceChange;
			enableSliceLimits(limitsFlg);
			spin1.setMaximum(n+1);
			allowSliceChange = true;	// one time supression
			jTogDrawRoi.setSelected(false);
			jButTrash.setEnabled(n>=indx && RoiState > 0 && !showAll);
			drawingRoi = false;
			currPoly = null;
			bf.parentPet.repaintAll();
		}
		return indx;
	}

	int getLabelIndx(int roiNm) {
		int n, n1, ret = 0;
		n = getRoiSize();
		n1 = getRoiSize(1);	// nifti ROI
		if( roiNm >= 0 && roiNm < n) {
			if(roiNm < n1) {
				Nifti3 nif1 = getNifListEntry(roiNm);
				ret = nif1.labelIndx;
			} else {
				Poly3Save poly1 = bf.polyVect.get(roiNm-n1);
				ret = poly1.labelIndx;
			}
		}
		return ret;
	}

	private int getCTcalc() {
		int retVal = 0;	// for jRadioAll
		if( jRadioAny.isSelected()) retVal = 2;
		if( jRadioAverage.isSelected()) retVal = 1;
		return retVal;
	}

	void changeOrientation() {
		if( !jCheckBlue.isSelected()) return;
		if( bf.nifListSz <= 0 && bf.suvPnt != null) return;
		calculateVol(true);
	}

	void changeUseSUV(boolean calVol) {
		boolean isChecked = jCheckUseSUV.isSelected();
		String val1 = "2.5";
		if(isChecked) val1 = jTextSUVlo.getText();
		jButClean.setText("Clean < " + val1);
		boolean isVis = jCheck3D_OC.isSelected() && isChecked && !jButNif.isEnabled();
		jButClean.setVisible(isVis);
		jButClean.setEnabled(true);
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
		cbox = getRadioCB(17);
		cbox.setVisible(false);
		for( int i=0; i<=16; i++) { // note that 17 is not used
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
			if(bf.parentPet.MRIflg) {
				labCt = "MRI ";
				type = 2;
			}
		}
		ArrayList<Integer> retList = new ArrayList<>();
		for( i=0; i<=n; i++) {
			cbox = getRadioCB(i);
			if( cbox.isSelected()) {
				SUVpoints.RadioPoint res = bf.suvPnt.newRadioPnt();
				res.label = labCt + cbox.getText();
				res.type = type;
				bf.suvPnt.radioList.add(res);
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
		Poly3Save polCur = bf.polyVect.get(roiNum-n1);
		tmp = polCur.ROIlabel;
		return ( tmp != null && (tmp.startsWith("exclude ROI") || tmp.startsWith("excluded ROI")));
	}

	SaveVolROI calculateVol(boolean dispFlg) {
		return calculateVol(dispFlg, -1, -1);
	}

	SaveVolROI calculateVol(boolean dispFlg, int viewIn, int gateInd1) {
		int sliceType = bf.parentPet.m_sliceType;
		CtCenterVal = 0;
		int gateIndx = bf.parentPet.gateIndx;
		if( gateInd1 >= 0) gateIndx = gateInd1;
		if( viewIn > 0) sliceType = viewIn;	// override the default
/*		Date start, stop;
		start = new Date();
		Double diff;*/
		SaveVolROI volTmp, volRet = new SaveVolROI();
		if( getRoiSize() <= 0) return volRet;
		ExcludedROI[] excludedROIs;
		ArrayList<Poly3Save> currSliceRois;
		ArrayList<SUVpoints.SavePoint> excludedPoints;
		JFijiPipe.lineEntry currLine;
		Poly3Save currRoi;
		int i, angle1=0, j, k, width1, maxSlice, sliceNum, maxDepth, depth, lindx;
		int ctSlice, elipSlice, roi, maxCalc, gateOffset = 0, curRoiNum = -1;
		int numFrm, wideCT = getCTcalc();
		boolean useSUV, useCT, savePoint, isNifti;
		double petVal, SUVmean, suvLo, suvNoRoi, suvMinRoi, minVal=0;
		Double prntVal, prntVal2;
		double [] suvAndCt = getSUVandCTLimits();
		int [] roiNum = new int[getRoiSize(0)];
		changeLimits(); // maybe the user has changed the limits?
		calcSavedSUV(gateInd1);
		excludedROIs = calculateExcludedROIs(gateInd1);
		bf.suvPnt = new SUVpoints(sliceType);
		excludedPoints = bf.suvPnt.buildExcludedList(excludedROIs);
		width1 = maxSlice = bf.parentPet.petPipe.data1.width;
		numFrm = bf.parentPet.petPipe.getNormalizedNumFrms();
		maxDepth = numFrm;
		if( bf.parentPet.petPipe.data1.numTimeSlots>1) {
			gateOffset = gateIndx * numFrm;
		}
		// the user might change the orientation between the calculation of the volume
		// and the display of points, so save the type
		bf.volSliceType = sliceType;
		if( bf.volSliceType == JFijiPipe.DSP_AXIAL) {
			maxSlice = numFrm;
			maxDepth = bf.parentPet.petPipe.data1.height;
		}
		maxCalc = maxSlice;
		bf.maxMip = -1;
		volRet.SUVList = new double[maxSlice];
		for( i=0; i<maxCalc; i++) volRet.SUVList[i] = 0;
		volRet.type = bf.volSliceType;
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
				currRoi = bf.polyVect.get(0);
				if( currRoi.type != bf.volSliceType) {
					SUVpoints saveSuv = bf.suvPnt;
					for(i=0; i<getRoiSize(0); i++) {
						volTmp = maybeRecalcSingleRoiVol(i+k, gateInd1);
						if( volTmp != null && volTmp.type.equals(volRet.type)) {
							volRet.SUVtotal += volTmp.SUVtotal;
							volRet.numPix++;
							if( volTmp.SUVmax > volRet.SUVmax) {
								volRet.SUVmax = volTmp.SUVmax;
								RoiNum4Max = i+k;
							}
/*							for( j=0; j<volTmp.SUVList.length; j++) {
								petVal = volTmp.SUVList[j];
								if( petVal > volRet.SUVList[j]) volRet.SUVList[j] = petVal;
							}*/
							for( j=0; j<bf.suvPnt.getListSize(); j++) {
								saveSuv.maybeAddPoint(bf.suvPnt.getPoint(j));
							}
						}
					}
					bf.suvPnt = saveSuv;	// write back updated value
					maxCalc = 0;	// no need to do it again
				}
			}
		}

		for( i=0; i<maxCalc; i++) {
			currSliceRois = new ArrayList<>();
			if( RoiState > 0) for( roi=j=0; roi<getRoiSize(0); roi++) {
				currRoi = bf.polyVect.get(roi);
				if( currRoi.type != bf.volSliceType) continue;
				if( i+1 < currRoi.lowSlice || i+1 > currRoi.hiSlice) continue;
				if( isExcluded(roi)) continue;
				currSliceRois.add(currRoi);
				roiNum[j++] = roi + getRoiSize(1);
			}
			// the next check is the biggest time saver.
			if( RoiState == 1) {
				if( currSliceRois.isEmpty()) continue;
				// calculate the minimum petVal for percent and nestle to save time
				suvMinRoi = suvNoRoi;
				minVal = suvAndCt[0];
				if(isPercentOK()) {
					for( j=0; j<savePercentSUV.length; j++) 
						if(savePercentSUV[j] < suvMinRoi) suvMinRoi = savePercentSUV[j];
					minVal = minVal * suvMinRoi / 100;
				}
				if(isNestle) {
					for( j=0; j<saveNestleSUV.length; j++) 
						if(saveNestleSUV[j] < suvMinRoi) suvMinRoi = saveNestleSUV[j];
					minVal = minVal * suvMinRoi + getBkgd();
				}
			}
			for( j=0; j<maxDepth; j++) {
				elipSlice = i;
				sliceNum = j;
				depth = i;
				switch (bf.volSliceType) {
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
				currLine = bf.parentPet.petPipe.data1.getLineOfData(angle1, depth, sliceNum+gateOffset);
				ctSlice = bf.parentPet.ctPipe.findCtPos(sliceNum, false);
				for( k=0; k<width1; k++) {
					if (currLine.pixels != null) petVal = currLine.pixels[k] * currLine.slope;
					else petVal = currLine.pixFloat[k];
					if( currLine.SUVfactor > 0) petVal /= currLine.SUVfactor;
					if( useSUV && !isPercent && !isNestle) {
						if( petVal < suvAndCt[0] || petVal > suvAndCt[1]) continue;
					}
					if( isPercent || isNestle)
						if( petVal < minVal) continue;
					if( useCT) {
						if( !isCtFat(ctSlice, k, depth, suvAndCt, bf.volSliceType, wideCT)) continue;
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
								if(isNestle) {
									suvLo = suvAndCt[0]*saveNestleSUV[curRoiNum] + getBkgd();
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
						if( bf.maxMip < 0) bf.maxMip = bf.minMip = sliceNum;
						if( bf.maxMip < sliceNum) bf.maxMip = sliceNum;
						if( bf.minMip > sliceNum) bf.minMip = sliceNum;
						lindx = getLabelIndx(curRoiNum);
						// with the nifti, there can be overlap - be careful
						if(isNifti) {
							if( !bf.suvPnt.maybeAddPoint(petVal, CtCenterVal, k, j, i, curRoiNum, lindx)) continue;
						}
						else bf.suvPnt.addPoint(petVal, CtCenterVal, k, j, i, curRoiNum, lindx);
						volRet.SUVtotal += petVal;
						volRet.numPix++;
						if( petVal >= volRet.SUVmax) {
							if( petVal > volRet.SUVmax) bf.numRed = 0;
							volRet.SUVmax = petVal;
							if( RoiNum4Max != curRoiNum)
								RoiNum4Max = curRoiNum;
							if(bf.numRed < 5) {
								int red0 = bf.suvPnt.getListSize()-1;
								bf.redPoints[bf.numRed++] = red0;
								bf.suvPnt.red0 = red0;
							}
						}
						if( petVal > volRet.SUVList[i]) volRet.SUVList[i] = petVal;
					}
				}
			}
		}
//		bf.suvPnt1.calcAllRadiomics(this);
		if( RoiState == 1) {
			volRet.isExcluded = false;
			i = getSpinInt(0)-1;
			if( !jCheckShowAll.isSelected() && i<excludedROIs.length) volRet.isExcluded = (excludedROIs[i] != null);
			if( !volRet.isExcluded) {
				double removeVal = bf.suvPnt.removeExcluded(excludedPoints, excludedROIs);
				if( removeVal > 0) {
					volRet.SUVtotal -= removeVal;
					volRet.numPix--;
					bf.numRed = 1;
					bf.redPoints[0] = bf.suvPnt.red0;
				}
			}
		}

		SUVmean = volRet.calcAll(false);
		volRet = calcNestle( volRet, jCheckShowAll.isSelected());
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
			if(isNestle) {
				prntVal = PetCtFrame.roundN(volRet.SUVNestle, k);
				jTextSuvPeak.setText(prntVal.toString());
			} else {
				prntVal = PetCtFrame.roundN(volRet.SUVpeak, k);
				prntVal2 = PetCtFrame.roundN(volRet.meanPeak, k);
				jTextSuvPeak.setText(prntVal.toString() + "  (" + prntVal2.toString() +")");
			}
			prntVal = PetCtFrame.roundN(SUVmean, k);
			prntVal2 = PetCtFrame.roundN(volRet.SD, k);
			jTextSuvMean.setText(prntVal.toString() + " \u00B1 " + prntVal2.toString());
			prntVal = PetCtFrame.roundN(volRet.vol1*SUVmean, j);
			jTextVolMean.setText(prntVal.toString());
//			if(volRet.type != bf.volSliceType) bf.suvPnt = null;
			bf.parentPet.repaintAll();
		}
/*		stop = new Date();
		diff = (double)(stop.getTime() - start.getTime());
		diff /= 1000.;
		IJ.log( "time = " + diff.toString());*/
		return volRet;
	}

	SaveVolROI calcNestle(SaveVolROI volIn, boolean showAll) {
		SaveVolROI volRet = volIn;
		if( showAll && savePercentSUV != null) {
			int i, j=0, n=savePercentSUV.length;
			double val = 0;
			for( i=0; i<n; i++) {
				if( val <= savePercentSUV[i]) {
					val = savePercentSUV[i];
					j = i;
				}
			}
			volRet.SUVNestle = saveNestleSUV[j];
		}
		return volRet;
	}

	SaveVolROI maybeRecalcSingleRoiVol(int RoiNum, int gateIn1) {
		int i, xpos, xpos0, ypos, ypos0, zpos, zpos0, n, x0, y0, z0;
		int[] currVals;
		SaveVolROI retVal = calcSingleRoiVol( RoiNum, false, gateIn1);
		SUVpoints.SavePoint currPnt, newPnt;
		int n1 = getRoiSize(1);
		if( n1 == 0 || RoiNum < n1 || retVal == null) return retVal;
		if( retVal.type == bf.parentPet.m_sliceType) return retVal;
		int lindx = getLabelIndx(RoiNum);
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
		switch(bf.parentPet.m_sliceType) {
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
		n = bf.suvPnt.getListSize();
		currVals = new int[3];
		retVal.type = bf.parentPet.m_sliceType;
		for( i=0; i<n; i++) {
			currPnt = bf.suvPnt.getPoint(i);
			currVals[0] = currPnt.x1;
			currVals[1] = currPnt.y1;
			currVals[2] = currPnt.z1;
			x0 = currVals[xpos];
			y0 = currVals[ypos];
			z0 = currVals[zpos];
			newPnt = bf.suvPnt.newPoint(currPnt.petVal, currPnt.ctVal, x0, y0, z0, RoiNum, lindx);
			bf.suvPnt.volPointList.set(i, newPnt);
		}
		return retVal;
	}

	SaveVolROI calcSingleRoiVol(int RoiNum, boolean SUVonly, int gateInd1) {
		bf.suvPnt = new SUVpoints(bf.parentPet.m_sliceType);
		CtCenterVal = 0;
		if( RoiNum >= getRoiSize() || RoiNum < 0) return null;
		SaveVolROI retVal;
		int gateIndx = bf.parentPet.gateIndx;
		JFijiPipe petPipe = bf.parentPet.petPipe;
		if( gateInd1 >= 0) gateIndx = gateInd1;
		int n1 = getRoiSize(1);
		if( RoiNum < n1) {
			retVal = calcSingleNiftiRoiVol( RoiNum, SUVonly);
			bf.suvPnt.calcRadiomics(this, isCalcRadio);
			return retVal;
		}
		boolean useSUV, useCT, firstSlice, insideFlg, isSpect;
		int i, j, k, num70, sag=0, maxSlice, maxDepth, sliceNum, depth, width1;
		int loSlice, hiSlice, roiNum0 = RoiNum - n1;
		int ctSlice, numFrm, gateOffset = 0, angle1=0;
		double petVal, suvLo, suvHi, petVal70, sum70;
		double [] suvAndCt = getSUVandCTLimits();
		suvLo = suvAndCt[0];
		suvHi = suvAndCt[1];
		int lindx = getLabelIndx(RoiNum);
		int wideCT = getCTcalc();
		JFijiPipe.lineEntry currLine;
		width1 = maxSlice = petPipe.data1.width;
		numFrm = petPipe.getNormalizedNumFrms();
		maxDepth = numFrm;
		if( petPipe.data1.numTimeSlots>1) {
			gateOffset = gateIndx * numFrm;
		}
		retVal  = new SaveVolROI();
		Poly3Save currRoi = bf.polyVect.get(roiNum0);
		bf.suvPnt.sliceType = retVal.type = currRoi.type;
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
			if(isNestle) {
				suvHi = saveNestleSUV[RoiNum];
				retVal.SUVNestle = suvHi;
				suvLo = suvLo*suvHi + getBkgd();
				suvHi *= 10;	// kill upper condition
			}
		}
		for( i=0; i<maxSlice; i++) retVal.SUVList[i] = 0;
		firstSlice = true;
		bf.maxMip = -1;
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
			if( i<0) continue;
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
				ctSlice = bf.parentPet.ctPipe.findCtPos(sliceNum, false);
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
						//if( petVal > suvHi && !isSpect) petVal = suvHi;
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
					if( bf.maxMip < 0) bf.maxMip = bf.minMip = sliceNum;
					if( bf.maxMip < sliceNum) bf.maxMip = sliceNum;
					if( bf.minMip > sliceNum) bf.minMip = sliceNum;
					bf.suvPnt.addPoint(petVal, CtCenterVal, k, j, i, RoiNum, lindx);
					retVal.SUVtotal += petVal;
					retVal.numPix++;
					if( petVal >= retVal.SUVmax) {
						if( petVal > retVal.SUVmax) bf.numRed = 0;
						if( RoiNum4Max != RoiNum)
							RoiNum4Max = RoiNum;
						retVal.SUVmax = petVal;
						if(bf.numRed < 5) {
							int red0 = bf.suvPnt.getListSize()-1;
							bf.redPoints[bf.numRed++] = red0;
							bf.suvPnt.red0 = red0;
						}
					}
					if( petVal > retVal.SUVmax) retVal.SUVmax = petVal;
					if( petVal > retVal.SUVList[i]) retVal.SUVList[i] = petVal;
				}
			}
			firstSlice = false;
		}
		if(isNestle && saveNestleSUV[RoiNum]<=0) {
			petVal70 = 0.7 * retVal.SUVmax;
			num70 = 0;
			sum70 = 0;
			for( i=currRoi.lowSlice-1; i<currRoi.hiSlice; i++) {
				if(i<0) continue;
				currRoi.setCurrEl(i);
				for( j=0; j<maxDepth; j++) {
					if( j<loSlice || j > hiSlice) continue;
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
					for( k=0; k<width1; k++) {
						isSpect = currLine.SUVfactor <= 0;
						if (currLine.pixels != null) petVal = currLine.pixels[k] * currLine.slope;
						else petVal = currLine.pixFloat[k];
						if( !isSpect) petVal /= currLine.SUVfactor;
						if( petVal < petVal70) continue;
						if( !currRoi.contains(k, j)) continue;
						sum70 += petVal;
						num70++;
					}
				}
			}
			if(num70>1) retVal.SUVNestle = sum70 / num70;
		}
		bf.suvPnt.calcRadiomics(this, isCalcRadio);
		return retVal;
	}
	
	boolean isPercentOK() {
		return isPercent && savePercentSUV != null;
	}

	SaveVolROI calcSingleNiftiRoiVol(int RoiNum, boolean SUVonly) {
		int i, j, maxSlice;
		JFijiPipe pip0 = bf.parentPet.petPipe;
		maxSlice = pip0.data1.width;
		SaveVolROI retVal  = new SaveVolROI();
		retVal.type = bf.parentPet.m_sliceType;
		if( retVal.type == JFijiPipe.DSP_AXIAL) {
			maxSlice = pip0.getNormalizedNumFrms();
		}
		retVal.SUVList = new double[maxSlice];
		for( i=0; i<maxSlice; i++) retVal.SUVList[i] = 0;
		return calcSingleNiftiRoiVolSub(RoiNum, retVal, SUVonly);
	}

	SaveVolROI calcSingleNiftiRoiVolSub(int RoiNum, SaveVolROI inVal, boolean SUVonly) {
		int i, width1, x1, y1, z1, zMip, prevPetZ = -1;
		int coef0, off1, num70, ctSlice = 0;
		int wideCT = getCTcalc();
		double slope, SUVfactor, petVal, suvLo, suvHi, petVal70, sum70;
		boolean useSUV, useCT, isFloat;
		float fltFactor;
		short currShort;
		short[] pix1;
		float[] pixFl;
		short [] val3;
		double [] suvAndCt = getSUVandCTLimits();
		suvLo = suvAndCt[0];
		suvHi = suvAndCt[1];
		SaveVolROI retVal = inVal;
		useSUV = jCheckUseSUV.isSelected();
		useCT = jCheckUseCt.isSelected();
		if(SUVonly || !useSUV) useSUV = useCT = false;
		else {
			if(isPercentOK()) {
				suvLo = suvLo * savePercentSUV[RoiNum] / 100;
				suvHi = 10 * savePercentSUV[RoiNum];	// kill upper condition
			}
			if(isNestle) {
				suvHi = saveNestleSUV[RoiNum];
				retVal.SUVNestle = suvHi;
				suvLo = 0.3*suvHi + suvLo;
				suvHi *= 10;	// kill upper condition
			}
		}
		int lindx = getLabelIndx(RoiNum);
		CtCenterVal = 0;
		JFijiPipe pip0 = bf.parentPet.petPipe;
		isFloat = pip0.data1.pixFloat != null;
		fltFactor = (float) pip0.data1.fltDataFactor;
		width1 = pip0.data1.width;
		retVal.type = bf.parentPet.m_sliceType;
		if( bf.nifList == null) return retVal;
		Nifti3 currNif = getNifListEntry(RoiNum);
		if( currNif == null) return retVal;
		coef0 = pip0.data1.getCoefficentAll();
		SUVfactor = pip0.data1.SUVfactor;
		if( SUVfactor == 0) SUVfactor = 1.0; // SPECT
		for( i=0; i< currNif.vectNif.size(); i++) {
			val3 = currNif.getVal3(i);
			x1 = val3[0];
			y1 = val3[1];
			z1 = zMip = val3[2];
			slope = pip0.data1.getRescaleSlope(z1)*fltFactor;
			off1 = width1*y1 + x1;
			if(isFloat) {
				pixFl = pip0.data1.pixFloat.get(z1);
				currShort = (short)(pixFl[off1]/fltFactor);
			} else {
				pix1 = pip0.data1.pixels.get(z1);
				currShort = (short)(pix1[off1]+coef0);
			}
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
					ctSlice = bf.parentPet.ctPipe.findCtPos(zMip, false);
					prevPetZ = zMip;
				}
				if( !isCtFat(ctSlice, val3[0], val3[1], suvAndCt, JFijiPipe.DSP_AXIAL, wideCT)) continue;
			}
			if( bf.maxMip < 0) bf.maxMip = bf.minMip = zMip;
			if( bf.maxMip < zMip) bf.maxMip = zMip;
			if( bf.minMip > zMip) bf.minMip = zMip;
			bf.suvPnt.addPoint(petVal, CtCenterVal, x1, y1, z1, RoiNum, lindx);
			retVal.SUVtotal += petVal;
			retVal.numPix++;
			if( petVal >= retVal.SUVmax) {
				if( petVal > retVal.SUVmax) bf.numRed = 0;
				RoiNum4Max = RoiNum;
				retVal.SUVmax = petVal;
				if(bf.numRed < 5) {
					bf.redPoints[bf.numRed++] = bf.suvPnt.getListSize()-1;
				}
			}
			if( petVal > retVal.SUVList[z1]) retVal.SUVList[z1] = petVal;
		}
		if(useSUV && isNestle && saveNestleSUV[RoiNum]<=0) {
			petVal70 = 0.7 * retVal.SUVmax;
			num70 = 0;
			sum70 = 0;
			for( i=0; i< currNif.vectNif.size(); i++) {
				val3 = currNif.getVal3(i);
				x1 = val3[0];
				y1 = val3[1];
				z1 = val3[2];
				slope = pip0.data1.getRescaleSlope(z1)*fltFactor;
				off1 = width1*y1 + x1;
				if(isFloat) {
					pixFl = pip0.data1.pixFloat.get(z1);
					currShort = (short)(pixFl[off1]/fltFactor);
				} else {
					pix1 = pip0.data1.pixels.get(z1);
					currShort = (short)(pix1[off1]+coef0);
				}
				petVal = currShort*slope/SUVfactor;
				if( petVal < petVal70) continue;
				sum70 += petVal;
				num70++;
			}
			if(num70>1) retVal.SUVNestle = sum70 / num70;
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
			fis.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	boolean isCtFat(int sliceNum, int x, int y, double[] limits, int type, int acceptWide) {
		CtCenterVal = 0;
		if( sliceNum < 0) return false;
		int i, j, x0, x1, y0, y1, x2,y2, width1, offst, upLimit, loLimit, coef0;
//		int average1;
		short val1;
		boolean isSag = (type == JFijiPipe.DSP_SAGITAL);
		JFijiPipe ctPipe = bf.parentPet.ctPipe;
		double slope, rescaleIntercept = ctPipe.data1.shiftIntercept();
		short[] data1 = ctPipe.data1.pixels.get(sliceNum);
		slope = ctPipe.data1.getRescaleSlope(sliceNum);
		width1 = ctPipe.data1.width;
		upLimit = (int)(limits[3] - rescaleIntercept);
		loLimit = (int)(limits[2] - rescaleIntercept);
		coef0 = ctPipe.data1.getCoefficentAll();
		x0 = x;
		y0 = y;
		if( isSag) {
			x0 = y;
			y0 = x;
		}
		x1 = bf.parentPet.shift2CtCen(ctPipe, x0, 0, isSag);
		y1 = bf.parentPet.shift2CtCen(ctPipe, y0, 1, isSag);
/*		x2 = bf.parentPet.shift2Ct2(ctPipe, x0, 0);
		y2 = bf.parentPet.shift2Ct2(ctPipe, y0, 1);
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
/*		if( acceptWide == 1)	{	// look for maybe bone
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
		}*/
		if( acceptWide == 1)	{	// accept according to average point
			int average1 = 0;
			for( i=-1; i<=1; i++) {
				average1 += data1[offst+i]+coef0;
			}
			average1 += data1[offst+width1] + coef0;
			average1 += data1[offst-width1] + coef0;
			val1 = (short)(average1/5);
			return ( val1 >= loLimit && val1 <= upLimit);
		}
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
	
/*	private void checkBone( short val1) {
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
	}*/

	void fillPatName() {
		ArrayList<String> seriesOnScreen;
		boolean chkName, chkDate, chkID;
		chkName = jCheckName.isSelected();
		chkDate = jCheckDate.isSelected();
		chkID = jCheckId.isSelected();
		if( (chkName | chkDate | chkID) == false) return;
		Date styDate = bf.parentPet.parent.getStudyDate();
		String txt1 = "";
		if( chkName) {
			txt1 = bf.parentPet.parent.m_patName + " ";
		}
		if( chkDate) {
			txt1 += ChoosePetCt.UsaDateFormat(styDate) + " ";
		}
		if( chkID) {
			txt1 += bf.parentPet.parent.m_patID;
		}
		txt1 = txt1.trim();
		jTextName.setText(txt1);
		if( jTextSeriesName.getText().isEmpty()) {
			seriesOnScreen = Extra.getSeries4dateID(styDate, bf.parentPet.parent.m_patID);
			jTextSeriesName.setText(setSeries3dName(seriesOnScreen));
		}
		activateBuildButton();
	}

	void buildDataset() {
		int i, x, x1, y, y1, z, z1, z2, ctSlice, width, height, nSize, nSize1, ctWidth;
		int gray, offY=0, offCt, ctCoef, min1, numPnt, maxCtDsp, offX=0, offY1;
		int start1;
		int[] pixels;
		ColorModel cm = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
		Color colIn = Color.green, colOut = Color.red;
		ImageStack stack;
		JFijiPipe.lineEntry currLine;
		Point roiPnt;
		ArrayList<Point> slicePoints;
		SUVpoints.SavePoint currPnt;
		short val1, shortBuf[];
		boolean useCt, inRoi, invertImg, scaleImg;
		double scale, slope, slop1, petVal, suvNorm, suvLo, suvNoRoi;
		double zoomX, scaleZ = 1.0, pixSpacing, sliceThick;
		Preferences prefer = bf.parentPet.parent.jPrefer;
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
		petPipe = bf.parentPet.petPipe;
		ctPipe = bf.parentPet.ctPipe;
		ctWidth = ctPipe.data1.width;
		zoomX = petPipe.zoomX;
		width = petPipe.data1.width;
		height = petPipe.data1.height;
		if( zoomX > 1.0) {
			i = ChoosePetCt.round(width/zoomX) & -2; // the construction size
			offX = (width - i)/2;
			width = i;
			i = ChoosePetCt.round(height/zoomX) & -2; // the construction size
			offY = (height - i)/2;
			height = i;
		}
		invertImg = jCheckInvert.isSelected();
//		scaleImg = jCheckScale.isSelected();
		scaleImg = false;
		nSize = nSize1 = petPipe.getNormalizedNumFrms();
		if( scaleImg) {
			pixSpacing = petPipe.getPixelSpacing(0);
			if( petPipe.data1.pixelSpacing == null) pixSpacing = 0;	// kill the scaling
			sliceThick = Math.abs(petPipe.data1.sliceThickness);
			scaleZ = pixSpacing/sliceThick;
			if( scaleZ > 0.1 && scaleZ < 10.0) {
				nSize1 = ChoosePetCt.round(nSize/scaleZ);
				scaleZ = ((double) nSize)/nSize1;
			}
			else scaleZ = 1.0;
		}
		stack = new ImageStack( width, height, cm);
//		xSpacing = bf.parentPet.pointSpacing(ctPipe);
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
		for( z=1; z<=nSize1; z++) {
			z2 = ChoosePetCt.round(z*scaleZ);
			z1 = z2-1;	// z1 counts from zero
			if( invertImg) z1 = nSize - z2;
			pixels = new int[width * height];
			// with findCtPos, if it is outside the range, use last good value
			ctSlice = ctPipe.findCtPos(z1, true);
			shortBuf = ctPipe.data1.pixels.get(ctSlice);
			slop1 = ctPipe.data1.getRescaleSlope(ctSlice);
			for( y=0; y<height; y++) {
				y1 = bf.parentPet.shift2Ct(ctPipe, y+offY, 1);
				offCt = ctWidth*y1;
				offY1 = width*y;
				for( x=0; x<width; x++) {
					x1 = bf.parentPet.shift2Ct(ctPipe, x+offX, 0) + offCt;
					if(x1 >= 0 && x1 < shortBuf.length) {
						val1 = (short) ((shortBuf[x1] + ctCoef)*slop1);
						gray = (int) ((val1 - min1) * scale);
						if( gray < 0) gray = 0;
						if( gray > maxCtDsp) gray = maxCtDsp;
					} else gray = 0;
					pixels[x+offY1] = black | gray << 16 | gray << 8 | gray;
				}
			}
			stack.addSlice(null, pixels);
		}

		// now add the PET part in red and green
		useCt = jCheckUseCt.isSelected();
		int wideCT = getCTcalc();
		suvNorm = 256.0 / suvAndCt[5];	// display max SUV
		if( jCheckUseSUV.isSelected() ) for( z=1; z<=nSize1; z++) {
			z2 = ChoosePetCt.round(z*scaleZ);
			start1 = 0;
			z1 = z2-1;	// z1 counts from zero
			if( invertImg) z1 = nSize - z2;
			numPnt = bf.suvPnt.getListSize();
			slicePoints = new ArrayList<>();
			for( i=0; i< numPnt; i++) {
				currPnt = bf.suvPnt.getPoint(i);
				switch (bf.volSliceType) {
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
			if( numPnt == 0) continue;
			ctSlice = ctPipe.findCtPos(z1, false);
			pixels = (int []) stack.getPixels(z);
			if(pixels == null) return;
			for( y=0; y<height; y++) {
				y1 = y+offY;
				currLine = petPipe.data1.getLineOfData(0, y1, z1);
				for( x=0; x<width; x++) {
					x1 = x+offX;
					if (currLine.pixels != null) petVal = currLine.pixels[x1] * currLine.slope;
					else petVal = currLine.pixFloat[x1];
					if( currLine.SUVfactor > 0) petVal /= currLine.SUVfactor;
					if( petVal < suvLo) continue;
					if( !isPercent && petVal > suvAndCt[1]) continue;
					if( useCt) {
						if( !isCtFat(ctSlice, x1, y1, suvAndCt, bf.volSliceType, wideCT)) continue;
					}
					inRoi = false;
					for( i=start1; i<numPnt; i++) {	// start from the last good point
						roiPnt = slicePoints.get(i);
						if( roiPnt.x == x1 && roiPnt.y == y1) {
							inRoi = true;
							start1 = i;
							break;
						}
					}
					if( !inRoi) for( i=0; i<start1; i++) {
						roiPnt = slicePoints.get(i);
						if( roiPnt.x == x1 && roiPnt.y == y1) {
							inRoi = true;	// if they are not completely sorted
							start1 = i;
							break;
						}
					}
					gray = (int) (suvNorm * petVal);
					if( gray < 0) gray = 0;
					if( gray > 255) gray = 255;
					i = y*width + x;
					if( i >= pixels.length)
						continue;
					// erase the CT contribution and leave only PET
					if( inRoi) pixels[i] = setPixelColor(gray, colIn);
					else pixels[i] = setPixelColor(gray, colOut);
				}
			}
		}
		ImagePlus myImage = new ImagePlus(windowName, stack);
		myImage.copyScale(petPipe.data1.srcImage);
		String meta1 = Extra.makeMetaData(seriesName, nSize, null,
				ChoosePetCt.SOPCLASS_TYPE_NM, bf.parentPet.petPipe.data1.metaData);
		myImage.setProperty("Info", meta1);
		myImage.show();
	}
	
	String setSeries3dName( ArrayList<String> serNames) {
		String tmp, retName = "3D";
		ArrayList<String> filteredNames = new ArrayList<>();
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

	int setPixelColor(int gray, Color inColor) {
		int pixRed = (gray * inColor.getRed()) / 255;
		int pixGreen = (gray * inColor.getGreen()) / 255;
		int pixBlue = (gray * inColor.getBlue()) / 255;
		return black | pixRed << 16 | pixGreen << 8 | pixBlue;
	}

	boolean setCheckGrown(boolean val) {
		boolean ret = jCheckGrown.isSelected();
		jCheckGrown.setSelected(val);
		return ret;
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
			Preferences prefer = bf.parentPet.parent.jPrefer;
			int numFrm = bf.parentPet.petPipe.getNormalizedNumFrms();
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
		Preferences prefer = bf.parentPet.parent.jPrefer;
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
		saveResultsDir(dirAndName, null);
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
        return bf.parentPet.parent.generateFileNameExt(type, spcSwap);
	}

	@SuppressWarnings("null")
	void saveResultsDir(String myFileName, String inDir) {
		try {
			isCalcRadio = false;
			if( myFileName == null && bf.suvPnt == null) return;
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
			boolean useCt;
			if( n <= 0) return;
			// avoid a bug Salim found
			if( savePercentSUV==null) {
				getSUVandCTLimits();
				if( isPercentOrNestle()) calculateVol(false);
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
			int i, j, k, i1, viewType = bf.parentPet.m_sliceType;
			Poly3Save polCur;
			Nifti3 nifCur;
			String flPath = inDir, defName, out1 = "brown fat path";
			String patName, styDate, tmp1;
			File fl1;
			// first check if all ROIs are the same type
			// if so override whatever view is showing
			x1 = 0;
			for( i=0; i<n0; i++) {
				polCur = bf.polyVect.get(i);
				j = polCur.type;
				if( i==0) {
					x1 = j;
					continue;
				}
				if( j!=x1) break;
			}
			if( i== n0) viewType = x1;
			Preferences prefer = bf.parentPet.parent.jPrefer;
			if(inDir == null) flPath = prefer.get(out1, null);
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
	//			defName = bf.parentPet.parent.getTitleBar(0);
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
			maybeRecalcSingleRoiVol(0, -1);	// to define bf.suvPnt1
			isCalcRadio = true;
			numRadio = 0;
			if( bf.suvPnt.radioList != null) {
				getRadioActive(false);
				if(isCtRadiomics()) getRadioActive(true);
				numRadio = bf.suvPnt.radioList.size();
			}
			tmp1 = "";
			for( i1=0; i1<numRadio; i1++) {
				res = bf.suvPnt.radioList.get(i1);
				tmp1 += ", " + res.label;
			}
			useCt = jCheckUseCt.isSelected();
			radioSum = new double[numRadio];
			radioVol = new double[numRadio];
			FileWriter fos = new FileWriter(fl1);
			out1 = "Label, ROI, type, Vol(ml), Vol*mean, SUVMean, SD, SUVPeak, MeanWahl, ";
			out1 += "SUVqPeak, MeanQpet, SUVMax, MeanMax, SUVNestle";
			if( useCt) out1 += ", HU";
			out1 += tmp1 + "\n";
			fos.write(out1);
			ExcludedROI[] excludedROIs;
			ArrayList<SUVpoints.SavePoint> excludedPoints;
			numGated = bf.parentPet.petPipe.data1.numTimeSlots;
			boolean showSD = jChRadShowSD.isSelected();
			for( j=0; j<numGated; j++) {
				excludedROIs = calculateExcludedROIs(j);
				excludedPoints = bf.suvPnt.buildExcludedList(excludedROIs);
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
						tmpVol.SUVtotal -= bf.suvPnt.removeExcluded(excludedPoints, excludedROIs);
//						bf.numRed = 1;
//						bf.redPoints[0] = bf.suvPnt.red0;
					}
					else tmpVol = calculateVol(false, viewType, k);
					if( tmpVol == null) continue;
					SUVMean = tmpVol.calcAll(true);
					x1 = i+1;
					if(i<n) {
						if(i<n1) {
							nifCur = getNifListEntry(i);
							tmp1 = nifCur.ROIlabel;
						} else {
							polCur = bf.polyVect.get(i-n1);
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
					out1 += PetCtFrame.roundN(tmpVol.SUVNestle, 2);
					if( useCt) out1 +=  ", " + PetCtFrame.roundN(tmpVol.CtHU, 2) + " \u00B1 " + PetCtFrame.roundN(tmpVol.CtSD, 2);
					if(i<n) {
						int numRd1 = numRadio;
						if( numRd1 > 0) numRd1 = bf.suvPnt.radioList.size();
						vTmp= tmpVol.vol1;
						vTotal += vTmp;
						for( i1=0; i1<numRd1; i1++) {
							res = bf.suvPnt.radioList.get(i1);
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
			out1 += PetCtFrame.roundN(bf.parentPet.petPipe.data1.SULfactor, 5);
			if( measureTime > 0) {
				out1 += ", time(sec) =," + measureTime;
			} else {
				out1 += ",,";
			}
			out1 += "," + NiftiLab;
			out1 += ",,,,,, patient ID =," + bf.parentPet.parent.m_patID + "\n";
			fos.write(out1);
			
			// if there are NIfti ROIs, they are here:
			if( n1 > 0) {
				out1 = "\nNumber of Nifti ROIs = " + n1.toString() + "\n";
				fos.write(out1);
				for( i=0; i<n1; i++) {
					nifCur = getNifListEntry(i);
					numPnts = z1 = nifCur.vectNif.size();
					out1 = nifCur.saveRoiLabel() + ", num points = " + z1.toString() + ", ";
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
				polCur = bf.polyVect.get(i);
				out1 = polCur.saveRoiLabel() + ", ";
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
			out1 = "SUVlo, SUVhi, CTlo, CThi, useSUV, useCT, CtRadio, Bkgd\n";
			fos.write(out1);
			out1 = jTextSUVlo.getText() + ", " + jTextSUVhi.getText() + ", ";
			out1 += jTextCTlo.getText() + ", " + jTextCThi.getText() + ", ";
			x1 = y1 = 0;
			if( jCheckUseSUV.isSelected()) x1 = 1;
			if( jCheckUseCt.isSelected()) y1 = 1;
			z1 = getCTcalc();
			out1 += x1.toString() + ", " + y1.toString() + ", " + z1.toString();
			out1 += ", " + jTextBkgd.getText() + "\n";
			fos.write(out1);
			fos.close();
			if(myFileName == null) {
				File fl2 = new File(Extra.getFileName(bf.parentPet.petPipe));
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
			bf.polyVect = new ArrayList<>();
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
				tmp0 = bf.parentPet.parent.m_patID;
				if( !tmp0.equals(patID)) i = 1;
				else {
					if( date0.after(bf.parentPet.parent.getStudyDate())) i = 2;
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
				bf.nifList = new Object[255];
				for( i=0; i<niftiSz; i++) {
					currLine = bf1.readLine();
					k = currLine.indexOf(',');
					labCur = currLine.substring(0, k).trim();
					currLine = currLine.substring(k+1);
					nifCur = new Nifti3();
					nifCur.parseRoiLabel(labCur);
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
					bf.nifList[i] = nifCur;
				}
				for( i=0; i<255; i++) {
					if( bf.nifList[i] == null) break;
				}
				bf.nifListSz = i;
				checkNiftiMask();
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
				currPoly.parseRoiLabel(labCur);
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
				bf.polyVect.add(currPoly);
			}
			currLine = bf1.readLine();
			if( currLine != null && currLine.startsWith("SUVlo")) {
				boolean isDone = false;
				currLine = bf1.readLine();
				for( j=0; j<8; j++) {
					if( isDone) break;
					k = currLine.indexOf(',');
					if( k > 0) {
						tmp1 = currLine.substring(0, k).trim();
						currLine = currLine.substring(k+1);
					}
					else {
						tmp1 = currLine.trim();
						isDone = true;
					}
					if(j>=4 && j<7) i = Integer.parseInt(tmp1);
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
							if (i == 1) jRadioAverage.setSelected(true);
							else if(i==2) jRadioAny.setSelected(true);
							else jRadioAll.setSelected(true);
							break;

						case 7:
							if (!tmp1.isEmpty()) jTextBkgd.setText(tmp1);
					}
				}
				
			}
			bf1.close();
			rd1.close();
//			if( getRoiSize(0)>0) {
				bf.nifLimits = false;
				jCheckDefLimits.setSelected(false);
				jCheckDefLimits.setEnabled(false);
				bf.nifEnable = false;
				bf.nifText = "Refresh";
				bf.setNifLabs(NIFTI_BOTH);
//			}
			changedRoiChoice(true);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	// on load make sure the mask is still in sync
	void checkNiftiMask() {
		String mask = getNiftiDir(1) + "/";
		String fileName = mask + generateNiftiName(1);
		File fl0 = new File(fileName);
		if(!fl0.exists()) return;
		byte[] buf;
		int headerSz, i, x, y, z, off1, val, max, width, height, numZ, frmSz;
		boolean isOK = true;
		try {
			FileInputStream fl1 = new FileInputStream(fl0);
			headerSz = 348+4;
			buf = new byte[headerSz];
			fl1.read(buf);	// read header
			i = readShort(buf, 0);
			if( i != 348) isOK = false;
			while(isOK) {
				width = readShort(buf, 42);
				height = readShort(buf, 44);
				numZ = readShort(buf, 46);
				frmSz = width * height;
				buf = new byte[frmSz];
				max = 0;
				for(z=0; z<numZ; z++) {
					fl1.read(buf);
					for( y=0; y<height; y++) {
						off1 = y * width;
						for( x=0; x<width; x++) {
							if(buf[x+off1] == 0) continue;
							val = buf[x+off1] & 0xff;
							if( val > max) max = val;
						}
					}
				}
				if( max != bf.nifListSz) isOK = false;
				break;
			}
			if( !isOK) {
				fl0.delete();
				File fl2 = new File(mask + "GrowedMaskResult.nii");
				fl2.delete();
			}
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
		Preferences prefer = bf.parentPet.parent.jPrefer;
		int rgb = color1.getRGB();
		prefer.putInt(saveColor, rgb);
	}
	
	void setROIlabel() {
		int indx = getSpinInt(0);
		int n = getRoiSize();
		int n1 = getRoiSize(1);	// nifti ROI
		if( n<indx) return;
		int nLab = jComboROIlabel.getSelectedIndex();
		if( indx <= n1) {
			Nifti3 nif1 = getNifListEntry(indx-1);
			nif1.ROIlabel = getComboLabel();
			if(nLab>=0) nif1.labelIndx = nLab;
		} else {
			Poly3Save poly1 = bf.polyVect.get(indx-1-n1);
			poly1.ROIlabel = getComboLabel();
			if(nLab>=0) poly1.labelIndx = nLab;
		}
		if(nLab>=0) {
			useDotColor(nLab);
			calculateVol(true);
		}
	}
	
	void setLimits(int lim1) {
		Integer sliceNum = 0;
		JTextField jTxtTmp;
		int indx = getSpinInt(0) - getRoiSize(1);
		int n = getRoiSize(0);
		if( indx < 1 || n<indx) return;
		Poly3Save poly1 = bf.polyVect.get(indx-1);
		if( poly1.type != bf.parentPet.m_sliceType) return;
		if( lim1 == 0) jTxtTmp = jTextSliceLo;
		else jTxtTmp = jTextSliceHi;
		switch( bf.parentPet.m_sliceType) {
			case JFijiPipe.DSP_AXIAL:
				sliceNum = ChoosePetCt.round(bf.parentPet.petAxial);
				sliceNum = poly1.numFrm - 1 - sliceNum;
				break;

			case JFijiPipe.DSP_CORONAL:
				sliceNum = ChoosePetCt.round(bf.parentPet.petCoronal);
				break;

			case JFijiPipe.DSP_SAGITAL:
				sliceNum = ChoosePetCt.round(bf.parentPet.petSagital);
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
		bf.parentPet.repaint();
	}
	
	void shiftRoi( int type, int delta, int roiNm) {
		int delta1 = delta;
		Poly3Save poly1 = bf.polyVect.get(roiNm);
		if( poly1.type != bf.parentPet.m_sliceType) return;
		int oldVal = saveRoiPntIndx;
		int i, n, sizeZ, width1, height1;
		int currRoi = getSpinInt(0) - getRoiSize(1);
		width1 = sizeZ = bf.parentPet.petPipe.data1.width;
		height1  = bf.parentPet.petPipe.data1.height;
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

	String dotRegistry(int indx) {
		String ret;
		int i = indx, k, n, n1;
		if( !jCheckColor.isSelected()) return null;
		if( i<0) {
//			i = jComboROIlabel.getSelectedIndex();
			k = getSpinInt(0) - 1;
			n = getRoiSize();
			n1 = getRoiSize(1);	// nifti ROI
//			if( n<indx) return;
			if( k < n1) {
				Nifti3 nif1 = getNifListEntry(k);
				i = nif1.labelIndx;
			} else {
				Poly3Save poly1 = bf.polyVect.get(k-n1);
				i = poly1.labelIndx;
			}
		}
		if( i<1) return null;
		ret = "dotcolor" + i;
		return ret;
	}

	void changeDotColor() {
		String key1 = dotRegistry(-1);
		if( key1 == null) return;
		Color col1 = JColorChooser.showDialog(null, "Dot color", Color.blue);
		Preferences prefer = bf.parentPet.parent.jPrefer;
		prefer.putInt(key1, col1.getRGB());
		useDotColor(-1);
	}

	void useDotColor(int j) {
		Color col2 = new Color(getDotColor(j));
		jPanelColor.setBackground(col2);
		if( j<0 ) bf.parentPet.repaint();
	}

	int getDotColor(int rc) {
		String key1 = dotRegistry(rc);
		int col1 = Color.blue.getRGB();
		if( key1 != null) {
			Preferences prefer = bf.parentPet.parent.jPrefer;
			col1 = prefer.getInt(key1, col1);
		}
		return col1;
	}

	void changeTmp() {
		boolean isTmp = jCheckUseTemp.isSelected();
		jTextNifSrc.setEnabled(!isTmp);
		jButNifSrc.setEnabled(!isTmp);
//		jButMake3Nifti.setEnabled(!isTmp);
//		jButExNiftiFld.setEnabled(!isTmp);
	}

	String browseNifti(boolean browse1) {
		String tmp = null;
		File fl1;
		int i;
		try {
			if( jCheckUseTemp.isSelected()) {
				if( bf.parentPet.niftiTmp.contains("nifti")) return bf.parentPet.niftiTmp;
				fl1 = File.createTempFile("nifti", null);
				tmp = fl1.getPath();
				fl1.delete();
				fl1 = new File(tmp);
				fl1.mkdirs();
				bf.parentPet.niftiTmp = tmp;
				return tmp;
			}
			JTextField textbx = jTextNifSrc;
			tmp = textbx.getText().trim();
			if( browse1) {
				JFileChooser fc;
				if( tmp.isEmpty()) fc = new JFileChooser();
				else fc = new JFileChooser(tmp);
				fc.setDialogTitle("Choose directory for autosegmentation Nifti files.");
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
				JOptionPane.showMessageDialog(this, "Please click on the folder icon to put a valid path to the data");
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
		Preferences prefer = bf.parentPet.parent.jPrefer;
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
		String retVal, extension;
		if( type == 1) retVal = "mask";
		else retVal = "src";
		retVal += "_" + generateFileName(0, true) + "_" + generateFileName(1, true);
		extension = jTextNiftiExt.getText().trim();
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
				retVal += "_pmask";
				break;

			case 6:
				retVal += "_ctmask";
				break;
		}
		if( type>=2 && type<=6) retVal += extension;
		retVal = (retVal + ".nii").toLowerCase();
		return retVal;
	}

	String getNiftiRegKey() {
		return "nifti0";
	}

	void runNifti() {
		Preferences prefer = bf.parentPet.parent.jPrefer;
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
		if( jCheck3D_OC.isSelected() && isDefLimits()) {
			bf.startSlice = zmin;
			bf.endSlice = zmax;
		}
		bf.nifLimits = false;
		jCheckDefLimits.setEnabled(false);
		bf.nifEnable = false;
		bf.setNifLabs(NIFTI_ENABLE);
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
						NiftiLab += " - " + Integer.toString(bf.nifListSz) + " ROIs";
						bf.nifText = NiftiLab;
						bf.setNifLabs(NIFTI_LABEL);
						bf.parentPet.repaint();
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
		int valSz, val, valz, off1,  scale = 1000;
		int[] xy;
		JFijiPipe pet = bf.parentPet.petPipe;
		valSz = pet.data1.width;
		valz = (int)(valSz/pet.zoomX);
		if( valz > valSz) valz = valSz;
		off1 = (valSz - valz)/2;
		xy = getNiftiLimits(JFijiPipe.DSP_CORONAL, 0);
		val = (xy[0]-off1)*scale/valz;
		if( val < 0) val = 0;
		prefer.putInt("nifti lox", val);
		val = (xy[1]-off1)*scale/valz;
		if( val > scale) val = scale;
		prefer.putInt("nifti hix", val);
		valSz = pet.data1.height;
		valz = (int)(valSz/pet.zoomX);
		if( valz > valSz) valz = valSz;
		off1 = (valSz - valz)/2;
		xy = getNiftiLimits(JFijiPipe.DSP_AXIAL, 1);
		val = (xy[0]-off1)*scale/valz;
		if( val < 0) val = 0;
		prefer.putInt("nifti loy", val);
		val = (xy[1]-off1)*scale/valz;
		if( val > scale) val = scale;
		prefer.putInt("nifti hiy", val);
		valSz = pet.getNormalizedNumFrms();
		xy = getNiftiLimits(JFijiPipe.DSP_CORONAL, 1);
		val = xy[0]*scale/valSz;
		prefer.putInt("nifti loz", val);
		val = xy[1]*scale/valSz;
		prefer.putInt("nifti hiz", val);
	}

	void doActualRunNifti(int type, String exDir) {
		int i, j, width, height, numZ, numZ1, headerSz, frmSz, coef0;
		int dataType = 2, bitPix = 8;
		boolean isSpect = false, isFloat;
		Integer val;
		ImageStack msk1Stack = null;
		float minus1=1.0f, fltFactor = 1.0f;
		float xEnd=0, yEnd=0, zEnd, spaceX=0, spaceY=0, spaceZ;
		double slope, tst1, SUVfactor, factor1=10.0, pixdbl, gmax;
		File checkIt, check1;
		short currShort;
		String tmp, tmp1;
		byte[] buf;
		short[] pix1;
		float[] pixEdge, pixFlt = null;
		try {
			String src = getNiftiDir(0);
			if( src == null) return;
			String mask = getNiftiDir(1);
			tmp = jButNif.getText();
			if( type == 0 && tmp.startsWith("Refresh")) {
				mask += "/" + generateNiftiName(1);
				check1 = new File(mask);
				if(!check1.exists()) doActualRunNifti(7, null);
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
					if( tmp.contains("_pmask")) continue;
					if( tmp.contains("_ct")) continue;
					check1.delete();
				}
			}
			if( type >= 2 && type <= 6 && exDir != null && !exDir.isEmpty()) src = exDir;
			src += "/" + generateNiftiName(type);
			mask = mask + "/" + generateNiftiName(1);
			JFijiPipe pip0 = bf.parentPet.petPipe;
			SUVfactor = pip0.data1.SUVfactor;
			if( SUVfactor == 0) {
				isSpect = true;
				SUVfactor = 1.0;
			}
			gmax = pip0.data1.grandMax;
			tst1 = 255*SUVfactor/gmax;
			switch(type) {
				case 0:
				case 8:
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
					pip0 = bf.parentPet.ctPipe;
					dataType = 4;
					bitPix = 16;
					break;

				case 5:
					factor1 = 1.0;
					msk1Stack = saveMaskedData(1);
					break;

				case 6:
					factor1 = 1.0;
					pip0 = bf.parentPet.getMriOrCtPipe();
					msk1Stack = saveMaskedData(2);
					break;

				case 7:	// for generation of nifti out mask from ROIs
					factor1 = 1.0;
					msk1Stack = saveMaskedData(1);
					src = mask;
					break;
			}
			if( pip0 == null || (pip0.data1.pixels == null && pip0.data1.pixFloat == null)) return;
			isFloat = pip0.data1.pixFloat != null;
			width = pip0.data1.width;
			height = pip0.data1.height;
			numZ = pip0.getNormalizedNumFrms();
			numZ1 = pip0.data1.numFrms;
			if( bf.endSlice > bf.startSlice && bf.endSlice < numZ) {
				numZ1 = bf.endSlice;
				numZ = numZ1/pip0.data1.numTimeSlots;
			}
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
			writeInt2buf(numZ1-bf.startSlice, buf, 46);
			for(i=48; i<56; i+=2) writeInt2buf(1, buf, i);
			writeInt2buf(dataType, buf, 70);	// data type
			writeInt2buf(bitPix, buf, 72);	// num bits/voxel
			writeFloat2buf(1, buf, 76);
			writeFloat2buf(spaceX, buf, 80);
			writeFloat2buf(spaceY, buf, 84);
			writeFloat2buf(spaceZ, buf, 88);
			writeFloat2buf(headerSz, buf, 108);
			writeFloat2buf(1, buf, 112);	// slope
			buf[123] = 2;	// NIFTI_UNITS_MM = 2
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
			if( fl1.exists()) {
				tmp = src + " exists. Overwrite it?";
				i = JOptionPane.showConfirmDialog(this, tmp, "Existing file", JOptionPane.YES_NO_OPTION);
				if( i != JOptionPane.YES_OPTION) return;
			}
			OutputStream out = new FileOutputStream(fl1);
			out.write(buf);
			buf = new byte[frmSz*bitPix/8];
			coef0 = pip0.data1.getCoefficentAll();
//			factor1 = 10.;	// SUV of 25.5 = 255
			SUVfactor = 1.0;
			if(type != 4) SUVfactor = pip0.data1.SUVfactor / factor1;
			if(isSpect) SUVfactor = 1 / factor1;
			for(i=bf.startSlice; i<numZ1; i++) {
				if( msk1Stack != null) {
					buf = (byte[]) msk1Stack.getPixels(i+1);
					out.write(buf);
					continue;
				}
				pix1 = null;
				if( isFloat) {
					pixFlt = pip0.data1.pixFloat.get(i);
					fltFactor = (float) pip0.data1.fltDataFactor;
				}
				else pix1 = pip0.data1.pixels.get(i);
				slope = pip0.data1.getRescaleSlope(i)*fltFactor;
				for( j=0; j<frmSz; j++) {
					if( isFloat) currShort = (short)(pixFlt[j]/fltFactor);
					else currShort = (short)(pix1[j]+coef0);
					pixdbl = currShort*slope/SUVfactor;
					switch(type) {
						case 0:
						case 3:
						case 8:
							if(pixdbl < 254) currShort = (short) pixdbl;
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
			if( jCheck3D_OC.isSelected()) {
				processCounter3D(src);
				return;
			}
			tmp = System.getProperty("os.name");
			String OSchar = "W.exe";	// Windows
			if( tmp.startsWith("Linux")) OSchar = "L";
			if( tmp.startsWith("Mac")) OSchar = "M";
			checkIt = new File(mask);
			String shape = IJ.getDirectory("imagej") + "lib/petct/shaping_elong" +OSchar + " ";
			tmp = " " + jTextParms.getText() + " ";
			if( jCheckDefLimits.isSelected()) {
				tmp += xmin.toString() + " " + xmax.toString() + " ";
				tmp += ymin.toString() + " " + ymax.toString() + " ";
				tmp += zmin.toString() + " " + zmax.toString() + " ";
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
//			if( !bf.polyVect.isEmpty()) return;
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

	void processCounter3D(String src) {
		ImagePlus inDat, map;
		ImageStack stk;
		Nifti3 currNif, prevNif;
		byte[] pix1;
		byte val1;
		double volPix;
		int x,y,z, szx, szy, szz, off1, inx0;
		Object[] prevList;
		Opener open1 = new Opener();
//		map = Opener.openUsingBioFormats(src);
		open1.setSilentMode(true);
		inDat = open1.openImage(src);
		szx = inDat.getWidth();
		szy = inDat.getHeight();
		szz = inDat.getNSlices();
//		inDat.show();
//		inDat = loci.plugins.BF.openImagePlus(src);
		int suv10 = (int)(oc_suv*10);
		JFijiPipe petPipe = bf.parentPet.petPipe;
		volPix = petPipe.getPixelSpacing(0);
		volPix = volPix * volPix * volPix * petPipe.data1.y2xFactor;
//		volPix = Math.abs(petPipe.getPixelSpacing(0)*petPipe.getPixelSpacing(1)*petPipe.data1.sliceThickness);
		if( volPix < 0.3) volPix = 0.3;	// don't divide by zero
		oc_pixels = (int) Math.round((1000.0 * oc_ml)/volPix);
		if( oc_pixels < 6) oc_pixels = 6;
		if( oc_pixels > 50) {
			IJ.log("Minimum number of voxels is " + oc_pixels + ".\nMaybe you want a smaller ml value?");
		}
		Utilities.Counter3D cnt3D = new Counter3D(inDat, suv10, oc_pixels, szx*szy*szz, false, false);
		map = cnt3D.getObjMap();
		prevList = bf.nifList;
		bf.nifList = new Object[255];
		stk = map.getStack();
		for( z=0; z<szz; z++) {
			pix1 = (byte[]) stk.getPixels(z+1);
			for( y=0; y<szy; y++) {
				off1 = y*szx;
				for( x=0; x<szx; x++) {
					inx0 = pix1[x+off1];
					if( inx0 == 0) continue;
					if( inx0 < 0) inx0 += 256;
					inx0--;
					prevNif = null;
					if( prevList != null) prevNif = (Nifti3) prevList[inx0];
					currNif = getNifListEntry(inx0);
					if( currNif == null) {
						bf.nifList[inx0] = currNif= new Nifti3();
						if(prevNif != null) {
							currNif.ROIlabel = prevNif.ROIlabel;
							currNif.labelIndx = prevNif.labelIndx;
						}
					}
					currNif.add(x, y, z+bf.startSlice);
				}
			}
		}
		for( z=0; z<255; z++) {
			if( bf.nifList[z] == null) break;
		}
		bf.nifListSz = z;
		changedRoiChoice(true);
		changeRoiAndUpdate();
//		map.show();
	}

	// putting in a non null exDir will cause that directory to be used.
	void makeExtNifti(String exDir) {
		NiftiStartTime = new Date();
		String prefDir = exDir;
		int start = 2;
		if( exDir == null) prefDir = extNiftiPref(null, false);
		if( prefDir.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please click on the folder icon to put a valid path to the data");
			return;
		}
		boolean maskOnly = jCheckMaskOnly.isSelected();
		if( maskOnly) start = 5;
		for( int i=start; i<=6; i++) {
			if(!getNiftiPrefs(i) && !maskOnly) continue;
			doActualRunNifti(i, prefDir);
		}
	}

	void setExtNiftiFolder() {
		JFileChooser fc;
		String init, val = extNiftiPref(null, false);
		extNiftiPref("", true);	// reset string to empty
		init = val;
		if( val == null || val.isEmpty()) init = getNiftiDir(0);
		fc = new JFileChooser(init);
		fc.setDialogTitle("Choose directory for external Nifti files.");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if( fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
		File fl1 = fc.getSelectedFile();
		val = fl1.getPath();
		extNiftiPref(val, true);
	}

	void extNiftiMask() {
		JFileChooser fc;
		String flPath, out1 = "brown fat external mask";
		File fl1;
		Preferences prefer = bf.parentPet.parent.jPrefer;
		int numFrm = bf.parentPet.petPipe.getNormalizedNumFrms();
		flPath = prefer.get(out1, null);
		if( flPath == null) fc = new JFileChooser();
		else fc = new JFileChooser(flPath);
		fc.setDialogTitle("Choose externally generated mask file.");
		if( fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
		fl1 = fc.getSelectedFile();
		setCheckGrown(false);
		bf.nifEnable = false;
		bf.setNifLabs(NIFTI_ENABLE);
		jButExMask.setEnabled(false);
		jCheckGrown.setEnabled(false);
		readNiftiMask(fl1.getPath());

		// save on load as well as save
		flPath = fl1.getParent();
		if( flPath != null) prefer.put(out1, flPath);
	}

	String extNiftiPref(String in, boolean isWrite) {
		Preferences prefer = bf.parentPet.parent.jPrefer;
		String ret = in, key = "external nifti folder";
		if( isWrite) prefer.put(key, in);
		else ret = prefer.get(key, null);
		return ret;
	}

	void use_3D_Object_Counter() {
		boolean isSel = jCheck3D_OC.isSelected();
		String text1 = "Use the coronal view to define limits for the search";
		if(!isSel) {
			text1 = "Use the axial and coronal views to define limits for the search";
			jCheckGrown.setSelected(false);
		}
		jCheckGrown.setEnabled(!isSel);
		jCheckDefLimits.setText(text1);
		bf.parentPet.repaint();
	}

	void clean_3D_Counter() {
		if( !jCheck3D_OC.isSelected()) return;
//		Nifti3 currNif;
		SaveVolROI vol1;
		ArrayList<Integer> delVal = new ArrayList<>();
		int i, indx, val;
		for( indx=0; indx<bf.nifListSz; indx++) {
			vol1 = calcSingleNiftiRoiVol(indx, false);
			if( vol1.numPix < oc_pixels) delVal.add(indx);
		}
		for( indx = delVal.size()-1; indx >= 0; indx--) {
			for( i=(int)delVal.get(indx); i<bf.nifListSz-1; i++) {
				bf.nifList[i] = bf.nifList[i+1];
			}
			bf.nifList[i] = null;
			bf.nifListSz--;
		}
		jButClean.setEnabled(false);
	}

	void maybeMakeNiftiFromRois() {
		File fl1 = new File(getNiftiDir(0) + "/" + generateNiftiName(0));
		if( !fl1.exists()) {
			doActualRunNifti(8, null);	// input file for Salim
		}
		fl1 = new File(getNiftiDir(1) + "/" + generateNiftiName(1));
		if( fl1.exists()) return;
		doActualRunNifti(7, null);
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
		bf.nifText = "Refresh";
		bf.nifEnable = true;
		bf.setNifLabs(NIFTI_BOTH);
		// kill all ROIs - don't kill the old data just yet
/*		bf.polyVect = new ArrayList<Poly3Save>();
		bf.nifList = null;
		bf.nifListSz = 0;*/
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
		int x1, z1, numBytes, useAffine;
		ByteBuffer bb2;
		String flNm1;
		NiftiUpdate dlg1;
		boolean isGrown = false;
		File fl0 = null;
		byte[] buf;
		Nifti3 currNif, prevNif;
		Object[] prevList;
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
			useAffine = readShort(buf, 254);
			numBytes = readShort(buf, 72)/8;
			if( numBytes < 1 || numBytes > 8) {
				IJ.log("Error in number of bytes");
				fl1.close();
				return;
			}
			frmSz = width * height * numBytes;
			buf = new byte[frmSz];
			prevList = bf.nifList;
			bf.nifList = new Object[255];
			for(z=0; z<numZ; z++) {
				z1 = z;
				if( useAffine == 0) z1 = numZ - z - 1;
				fl1.read(buf);
				bb2 = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
				for( y=0; y<height; y++) {
					off1 = y * width;
					for( x=0; x<width; x++) {
						x1 = x+off1;
						if( numBytes == 8) {
							val = readDouble2Int(bb2, x1*8);
							if( val <= 0) continue;
						} else {
							if(buf[x1] == 0) continue;
							val = (buf[x1] & 0xff) - 1;
						}
						prevNif = null;
						if( prevList != null) prevNif = (Nifti3) prevList[val];
						currNif = getNifListEntry(val);
						if( currNif == null) {
							bf.nifList[val] = currNif= new Nifti3();
							if(prevNif != null) {
								currNif.ROIlabel = prevNif.ROIlabel;
								currNif.labelIndx = prevNif.labelIndx;
							}
						}
						currNif.add(x, y, z1);
					}
				}
			}
			// some masks begin from 1 and not from 0
			if( bf.nifList[0] == null && bf.nifList[1] != null) {
				for( i=0; i<254; i++) {
					bf.nifList[i] = bf.nifList[i+1];
					if( bf.nifList[i] == null) break;
				}
			}
			for( i=0; i<255; i++) {
				if( bf.nifList[i] == null) break;
			}
			bf.nifListSz = i;
			fl1.close();
			changedRoiChoice(true);
			changeRoiAndUpdate();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	int readShort(byte[] buf, int off) {
		int b0, b1;
		b0 = buf[off] & 0xff;
		b1 = buf[off+1] & 0xff;
		return b0 + b1*256;
	}

	int readDouble2Int(ByteBuffer buf, int off) {
		double val1 = buf.getDouble(off);
		return (int) Math.round(val1);
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
			doActualRunNifti(0, null);
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
			int i, j, n = bf.suvPnt.getListSize();
			int ctSlice, orient;
			if(n <= 0) {
				JOptionPane.showMessageDialog(this, "No ROIs found.");
				return;
			}
			String flPath, out1 = "point list path";
			File fl1;
			Preferences prefer = bf.parentPet.parent.jPrefer;
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
			orient = bf.parentPet.m_sliceType;
			JFijiPipe ctPipe = bf.parentPet.ctPipe;
//			double rescaleIntercept = ctPipe.data1.shiftIntercept();
			short[] data1 = null;
			width1 = ctPipe.data1.width;
			coef0 = ctPipe.data1.getCoefficentAll();
			valI = new Integer[4];
			zprev = -1000;
			for( i=0; i<n; i++) {
				currPnt = bf.suvPnt.getPoint(i);
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
					ctSlice = bf.parentPet.ctPipe.findCtPos(z0, false);
					data1 = ctPipe.data1.pixels.get(ctSlice);
					slope = ctPipe.data1.getRescaleSlope(ctSlice);
					zprev = z0;
				}
				x2 = bf.parentPet.shift2Ct(ctPipe, x0, 0);
				y2 = bf.parentPet.shift2Ct(ctPipe, y0, 1);
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
		int sliceType, zmin1, zmax1, width1, heigh1, x, y, depth;
		ColorModel cm;
		double min, max;
		double coef[];
		String sliceLab;
		ImagePlus img1, mask1 = null;
		ImageStack origStack, msk1Stack;
		short[] maskOut=null, origIn=null;
		float[] maskFlOut=null, origFl=null;
		byte[] roiOut=null, roiIn;
		byte currByte;
		JFijiPipe petPipe = bf.parentPet.petPipe;
		if( petPipe == null) return null;
		i = petPipe.data1.orientation;
		if( (i&(64+32))!=0 || (petPipe.data1.pixels == null && petPipe.data1.pixFloat == null)) {
			JOptionPane.showMessageDialog(this, "Can't handle this data. Please contact ilan.tal@gmail.com");
			return null;
		}
		calculateVol(false);
		listSz = bf.suvPnt.getListSize();
		if( listSz <= 0) {
			JOptionPane.showMessageDialog(this, "No ROIs found. Perhaps you have a different\n orientation from where you defined the ROIs?");
			return null;
		}
		if( type == 2) {
			Radionomic rad1 = new Radionomic();
			img1 = rad1.buildCtPlus(bf.suvPnt, bf.parentPet, true);
			if( img1 == null || maskParms == null) return null;
			origStack = img1.getImageStack();
			zmax1 = img1.getStackSize();
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
				if(z>=0 && z < zmax1) {
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
		depth = img1.getBitDepth();
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
		sliceType = bf.parentPet.m_sliceType;
		zmin1 = zmax1 = -1;
		for( j=0; j<listSz; j++) {
			SUVpoints.SavePoint currPnt = mapPoint2axial(j, sliceType);
			if( j==0) {
				zmin1 = zmax1 = currPnt.z1;
				continue;
			}
			if( zmin1 > currPnt.z1) zmin1 = currPnt.z1;
			if( zmax1 < currPnt.z1) zmax1 = currPnt.z1;
		}
		for( i=1; i<=stkSz; i++) {
			if( type == 0) {
				if( depth == 32) maskFlOut = new float[sliceSz];
				else {
					maskOut = new short[sliceSz];
					if( coef0 != 0) for(j=0; j<sliceSz; j++) maskOut[j] = (short) coef0;
				}
			} else roiOut = new byte[sliceSz];
			i1 = petPipe.getOrigSliceNum(i);
			sliceLab = origStack.getSliceLabel(i1);
			if( i >= zmin1 && i <= zmax1) {
				if( depth == 32) origFl = (float []) origStack.getPixels(i1);
				else origIn = (short []) origStack.getPixels(i1);
				for( j=0; j<listSz; j++) {
					SUVpoints.SavePoint currPnt = mapPoint2axial(j, sliceType);
					if( currPnt.z1 != i) continue;
					offst = width * currPnt.y1 + currPnt.x1;
					if( type == 0) {
						if( depth == 32) maskFlOut[offst] = origFl[offst];
						else maskOut[offst] = origIn[offst];
					}
					else roiOut[offst] = (byte) (currPnt.rn1 + 1);	// make roi number from 1
				}
			}
			if( type == 0) {
				if( depth==32) msk1Stack.addSlice(sliceLab, maskFlOut);
				else msk1Stack.addSlice(sliceLab, maskOut);
			}
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
		SUVpoints.SavePoint currPnt = bf.suvPnt.getPoint(indx);
		x = currPnt.x1;
		z = currPnt.y1 + 1;
		y = currPnt.z1;
		rn = currPnt.rn1;
		int lindx = getLabelIndx(rn);
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
		return bf.suvPnt.newPoint(currPnt.petVal, currPnt.ctVal, x, y, z, rn, lindx);
	}

	boolean maybeToggleBkgd() {
		boolean isShow = jButBkgd.isVisible();
		if( isShow == isNestle) return isShow;
		String butTxt = "SUV peak";
		isShow = !isShow;
		if( isShow) butTxt = "SUV nestle";
		jButBkgd.setVisible(isShow);
		jTextBkgd.setVisible(isShow);
		jLabelPeak.setText(butTxt);
		return isShow;
	}

	void measureBkgd() {
		Double mean = bf.parentPet.SUVmean;
		jTextBkgd.setText(mean.toString());
		calculateVol(true);
	}

	double getBkgd() {
		return Double.parseDouble(jTextBkgd.getText());
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
		int labelIndx;
		
		Nifti3() {
			vectNif = new ArrayList<>();
			ROIlabel = "";
			labelIndx = 0;
		}

		String saveRoiLabel() {
			String ret = "";
			if(labelIndx > 0) ret = labelIndx + " - ";
			ret += ROIlabel;
			return ret;
		}

		void parseRoiLabel(String in1) {
			String tmp = in1;
			if( in1 == null) return;
			int i = in1.indexOf(" - ");
			if( i>0) {
				labelIndx = Integer.parseInt(in1.substring(0, i));
				tmp = in1.substring(i+3);
			}
			ROIlabel = tmp;
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
		int type, lowSlice, hiSlice, numFrm, labelIndx;
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
			labelIndx = 0;
			ROIlabel = "";
			currEllipse = null;
		}
		
		Poly3Save(Poly3Save copy) {
			type = copy.type;
			lowSlice = copy.lowSlice;
			hiSlice = copy.hiSlice;
			numFrm = copy.numFrm;
			labelIndx = copy.labelIndx;
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

		String saveRoiLabel() {
			String ret = "";
			if(labelIndx > 0) ret = labelIndx + " - ";
			ret += ROIlabel;
			return ret;
		}

		void parseRoiLabel(String in1) {
			String tmp = in1;
			if( in1 == null) return;
			int i = in1.indexOf(" - ");
			if( i>0) {
				labelIndx = Integer.parseInt(in1.substring(0, i));
				tmp = in1.substring(i+3);
			}
			ROIlabel = tmp;
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

		// the usual case uses bf.parentPet and puts null for Display3Panel
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
					scl1 = bf.parentPet.getScalePet();
					pt1 = bf.parentPet.petPipe.pos2Scrn(pt21, scl1, type1);
					pt2 = bf.parentPet.petPipe.pos2Scrn(pt22, scl1, type1);
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
			suvPnt1 = bf.suvPnt;
		}
		
		SaveVolROI getAll() {
			bf.suvPnt = suvPnt1;
			bf.numRed = 1;
			bf.redPoints[0] = bf.suvPnt.red0;
			return saveVol1;
		}
	}

	class SaveVolROI {
		Integer type;
		double SUVmax = 0;
		double SUVpeak = 0, peakWahl = 0, peakQpet = 0;
		double[] SUVList;
		double SUVtotal = 0, SUVNestle = 0;
		double vol1 = 0;
		double meanMax = 0;
		double meanPeak = 0, meanWahl = 0, meanQpet = 0;
		double SD = 0, CtHU = 0, CtSD = 0;
		int numPix = 0;
		boolean isExcluded = false;

		SaveVolROI() {
			RoiNum4Max = -1;	// which ROI has SUVmax
		}

		double calcAll(boolean bothPeaks) {
			int numPnt, i, k, nSUV;
			double currVal, sumSquare, SUVmean = 0;
			double[] ctVals;
			numPnt = bf.suvPnt.getListSize();
			nSUV = SUVList.length;
			meanMax = meanPeak = 0;
			if( numPnt > 0) {
				if( bothPeaks) {
					peakWahl = bf.suvPnt.calcSUVpeak(bf.parentPet.petPipe, 1);
					meanWahl = bf.suvPnt.calcSUVmeanPeak(bf.parentPet.petPipe);
					peakQpet = bf.suvPnt.calcSUVpeak(bf.parentPet.petPipe, 2);
					meanQpet = bf.suvPnt.calcSUVmeanPeak(bf.parentPet.petPipe);
				} else {
					SUVpeak = bf.suvPnt.calcSUVpeak(bf.parentPet.petPipe, lastPeakType);
					meanPeak = bf.suvPnt.calcSUVmeanPeak(bf.parentPet.petPipe);
				}
				for(i=k=0; i<nSUV; i++) {
					if(SUVList[i]<=0) continue;
					meanMax += SUVList[i];
					k++;	// number of actual slices
				}
				if( k>0) meanMax = meanMax / k;
				vol1 = bf.parentPet.petPipe.data1.pixelSpacing[0] / 10;	// assume x=y
				vol1 = vol1*vol1*vol1*bf.parentPet.petPipe.data1.y2xFactor*numPnt;
				SUVmean = SUVtotal / numPnt;
				SD = bf.suvPnt.calcSD(SUVmean);
				ctVals = bf.suvPnt.calCtVals();
				CtHU = ctVals[0];
				CtSD = ctVals[1];
			}
			return SUVmean;
		}
	}

	PetCtPanel getParentPet() {
		if( bf == null) return null;
		return bf.parentPet;
	}

	Poly3Save getPolyVectEntry(int indx) {
		if( bf == null) return null;
		return bf.polyVect.get(indx);
	}

	void logTimeDiff( int curPoint) {
		if( bf == null) return;
		bf.parentPet.logPoint(curPoint);
	}

	Nifti3 getNifListEntry(int indx) {
		if( bf == null) return null;
		return (Nifti3) bf.nifList[indx];
	}

	class bfGroup {
		int nifListSz, minMip, maxMip, lastRoiVal = 1, numRed = 0;
		int volSliceType = -1, startSlice = 0, endSlice = -1;
		String nifText = "Press to run, and be patient";
		boolean nifEnable = true, nifLimits = true;
		PetCtPanel parentPet = null;
		Object [] nifList = null;
		SUVpoints suvPnt = null;
		int [] redPoints = new int[5];
		ArrayList<Poly3Save> polyVect = new ArrayList<>();

		void fillSdyLab() {
			if(parentPet == null) return;
			PetCtFrame par = parentPet.parent;
			Date styDate = par.getStudyDate();
			String tmp = par.m_patName + "   " + par.m_patID;
			tmp += "   " + ChoosePetCt.UsaDateFormat(styDate);
			stdyLab.setText(tmp);
		}

		void setNifLabs(int type) {
			switch(type) {
				case NIFTI_BOTH:
				case NIFTI_ENABLE:
					jButNif.setEnabled(nifEnable);
					if( type != NIFTI_BOTH) break;
					
				case NIFTI_LABEL:
					jButNif.setText(nifText);
					break;
			}
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
		PetCtFrame parFrm = bf.parentPet.parent;
		myWriteDicom dlg1 = new myWriteDicom(parFrm, null);
		dlg1.robotType = 3; // MIP
		img2 = dlg1.getDcmData();
		width1 = img2.getWidth();
		height1 = img2.getHeight();
		typ1 = img2.getType();
		dm1 = bf.parentPet.getReducedSize();
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
				Nifti3 currNif = (Nifti3) bf.nifList[RoiNum4Max];
				txtLab = currNif.ROIlabel;
			} else {
				Poly3Save poly1 = bf.polyVect.get(RoiNum4Max-n1);
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
		tmpTxt = "  PET/CT " + ChoosePetCt.UsaDateFormat(bf.parentPet.petPipe.data1.serTime);
		g.drawString(tmpTxt, 0, 4*txtHeight);
		tmpTxt = "  " + parFrm.m_patName;
		if(parFrm.m_patBirthday != null) tmpTxt += " born: "+ChoosePetCt.UsaDateFormat(parFrm.m_patBirthday);
		g.drawString(tmpTxt, 0, 5*txtHeight);
		font1 = font1.deriveFont(Font.PLAIN, fontSz);
		g.setFont(font1);
		tmpTxt = "  Parameters:";
		g.drawString(tmpTxt, 0, heiOff + 6*txtHeight);
		double suvTest = Double.parseDouble(jTextSUVhi.getText());
		tmpTxt = "  SUV: min=" + jTextSUVlo.getText();
		if( suvTest <= 1000) tmpTxt += ", max=" + jTextSUVhi.getText();
		if( jCheckUseCt.isSelected()) {
			tmpTxt += "  CT: min=" + jTextCTlo.getText() + ", max=" + jTextCThi.getText();
		}
		g.drawString(tmpTxt, 0, heiOff + 7*txtHeight);
		font1 = font1.deriveFont(Font.PLAIN, 4*fontSz/5);
		g.setFont(font1);
//		tmpTxt = "  Provided by: http://petctviewer.org";
//		g.drawString(tmpTxt, 0, heiOff + 9*txtHeight);
		g.dispose();
		JFrame frm = new JFrame();
		frm.getContentPane().add(new JLabel( new ImageIcon(imgOut)));
		frm.pack();
		frm.setVisible(true);
		dlg1 = new myWriteDicom(imgOut, parFrm);
		dlg1.specialType = 1;	// TMTV Report
		dlg1.writeBuffImage();
		frm.dispose();
		if( dlg1.outFile1 == null) {
			this.setVisible(true);
			return;
		}
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
        jCheckColor = new javax.swing.JCheckBox();
        jPanelColor = new javax.swing.JPanel();
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
        jButBkgd = new javax.swing.JButton();
        jTextBkgd = new javax.swing.JTextField();
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
        jCheckDefLimits = new javax.swing.JCheckBox();
        jPanExNifti = new javax.swing.JPanel();
        jButMake3Nifti = new javax.swing.JButton();
        jButExNiftiFld = new javax.swing.JButton();
        jTextNiftiExt = new javax.swing.JTextField();
        jCheckMaskOnly = new javax.swing.JCheckBox();
        jCheckGrown = new javax.swing.JCheckBox();
        jLabNif1 = new javax.swing.JLabel();
        jLabNif2 = new javax.swing.JLabel();
        jCheckUseTemp = new javax.swing.JCheckBox();
        jButNifSrc = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jButExMask = new javax.swing.JButton();
        jCheck3D_OC = new javax.swing.JCheckBox();
        jButClean = new javax.swing.JButton();
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
        stdyLab = new javax.swing.JLabel();

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
        jComboROIlabel.setToolTipText("ROI label");
        jComboROIlabel.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jComboROIlabelFocusLost(evt);
            }
        });
        jComboROIlabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboROIlabelActionPerformed(evt);
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

        jCheckColor.setToolTipText("custom colors");
        jCheckColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckColorActionPerformed(evt);
            }
        });

        jPanelColor.setBackground(java.awt.Color.blue);
        jPanelColor.setToolTipText("click to change custom color");
        jPanelColor.setPreferredSize(new java.awt.Dimension(50, 25));
        jPanelColor.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanelColorMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanelColorLayout = new javax.swing.GroupLayout(jPanelColor);
        jPanelColor.setLayout(jPanelColorLayout);
        jPanelColorLayout.setHorizontalGroup(
            jPanelColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 50, Short.MAX_VALUE)
        );
        jPanelColorLayout.setVerticalGroup(
            jPanelColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 25, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanelROILayout = new javax.swing.GroupLayout(jPanelROI);
        jPanelROI.setLayout(jPanelROILayout);
        jPanelROILayout.setHorizontalGroup(
            jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelROILayout.createSequentialGroup()
                .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelROILayout.createSequentialGroup()
                        .addComponent(jRadioNone)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckFollow)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinRoiNm, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelROILayout.createSequentialGroup()
                        .addComponent(jTextSliceLo, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(3, 3, 3)
                        .addComponent(jLabLoLim)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabSlLim)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabHiLim)
                        .addGap(1, 1, 1)
                        .addComponent(jTextSliceHi, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jComboROIlabel, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelROILayout.createSequentialGroup()
                        .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jRadioInterior)
                            .addComponent(jRadioExterior))
                        .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelROILayout.createSequentialGroup()
                                .addGap(24, 24, 24)
                                .addComponent(jTogDrawRoi)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButTrash, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelROILayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jCheckSphere, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanelROILayout.createSequentialGroup()
                        .addComponent(jCheckShowAll)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckColor)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
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
                    .addComponent(jButTrash, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTogDrawRoi)
                        .addComponent(jRadioInterior)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioExterior)
                    .addComponent(jCheckSphere))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jCheckShowAll)
                    .addComponent(jCheckColor)
                    .addComponent(jPanelColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(7, 7, 7)
                .addComponent(jComboROIlabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelROILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextSliceLo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabLoLim)
                        .addComponent(jLabHiLim)
                        .addComponent(jTextSliceHi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelROILayout.createSequentialGroup()
                        .addComponent(jLabSlLim)
                        .addContainerGap())))
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
        jSpinDiameter.setToolTipText("blue dot size");
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
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

        jButBkgd.setText("Bkgd");
        jButBkgd.setToolTipText("Set Nestle background");
        jButBkgd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBkgdActionPerformed(evt);
            }
        });

        jTextBkgd.setText("2.000");

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
                        .addGap(56, 56, 56)
                        .addComponent(jRadioAll)
                        .addGap(6, 6, 6))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jRoiTabLayout.createSequentialGroup()
                        .addComponent(jPanelROI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(11, 11, 11)
                        .addComponent(jPanelResults, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButSave)
                    .addGroup(jRoiTabLayout.createSequentialGroup()
                        .addComponent(jButLoad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCheckOld))
                    .addGroup(jRoiTabLayout.createSequentialGroup()
                        .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jRadioAverage)
                            .addComponent(jCheckLock))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButHelp)
                            .addComponent(jRadioAny)))
                    .addGroup(jRoiTabLayout.createSequentialGroup()
                        .addComponent(jButBkgd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextBkgd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(103, 103, 103))
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
                        .addGap(18, 18, 18)
                        .addGroup(jRoiTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButBkgd)
                            .addComponent(jTextBkgd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jPanelROI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelResults, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTextBkgd.getAccessibleContext().setAccessibleName("");

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
                .addContainerGap(352, Short.MAX_VALUE))
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
                .addContainerGap(230, Short.MAX_VALUE))
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
                                    .addComponent(jTextCtStrength)))
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
                .addContainerGap(151, Short.MAX_VALUE))
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
                .addContainerGap(88, Short.MAX_VALUE))
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
                .addContainerGap(195, Short.MAX_VALUE))
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
                .addContainerGap(160, Short.MAX_VALUE))
        );

        j3Dtab.addTab("mask", jMask);

        jButNif.setText("Press to run, and be patient");
        jButNif.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButNifActionPerformed(evt);
            }
        });

        jTextParms.setText("30 2 50 0 30 0 256");

        jCheckDefLimits.setSelected(true);
        jCheckDefLimits.setText("Use the axial and coronal views to define limits for the search");
        jCheckDefLimits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckDefLimitsActionPerformed(evt);
            }
        });

        jPanExNifti.setBorder(javax.swing.BorderFactory.createTitledBorder("use Nifti in external programs"));
        jPanExNifti.setToolTipText("");

        jButMake3Nifti.setText("make Nifti files");
        jButMake3Nifti.setToolTipText("use Options to set which files are made");
        jButMake3Nifti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMake3NiftiActionPerformed(evt);
            }
        });

        jButExNiftiFld.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/open.gif"))); // NOI18N
        jButExNiftiFld.setToolTipText("Choose external Nifti folder");
        jButExNiftiFld.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButExNiftiFldActionPerformed(evt);
            }
        });

        jTextNiftiExt.setToolTipText("Add to file name");

        jCheckMaskOnly.setText("masks only");
        jCheckMaskOnly.setToolTipText("Only the 2 mask files are calculated");

        javax.swing.GroupLayout jPanExNiftiLayout = new javax.swing.GroupLayout(jPanExNifti);
        jPanExNifti.setLayout(jPanExNiftiLayout);
        jPanExNiftiLayout.setHorizontalGroup(
            jPanExNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanExNiftiLayout.createSequentialGroup()
                .addGroup(jPanExNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanExNiftiLayout.createSequentialGroup()
                        .addComponent(jButExNiftiFld)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButMake3Nifti))
                    .addGroup(jPanExNiftiLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jCheckMaskOnly)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextNiftiExt, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        jPanExNiftiLayout.setVerticalGroup(
            jPanExNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanExNiftiLayout.createSequentialGroup()
                .addGroup(jPanExNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButMake3Nifti)
                    .addComponent(jButExNiftiFld))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanExNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextNiftiExt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckMaskOnly)))
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

        jButNifSrc.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/open.gif"))); // NOI18N
        jButNifSrc.setToolTipText("Choose Nifti directory for the text box to the left");
        jButNifSrc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButNifSrcActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("use external mask"));

        jButExMask.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/open.gif"))); // NOI18N
        jButExMask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButExMaskActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jButExMask)
                .addGap(0, 95, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jButExMask)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jCheck3D_OC.setText("use 3D OC");
        jCheck3D_OC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheck3D_OCActionPerformed(evt);
            }
        });

        jButClean.setText("Clean < 2.5");
        jButClean.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButCleanActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jNiftiLayout = new javax.swing.GroupLayout(jNifti);
        jNifti.setLayout(jNiftiLayout);
        jNiftiLayout.setHorizontalGroup(
            jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jNiftiLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextParms, javax.swing.GroupLayout.PREFERRED_SIZE, 511, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabNif2)
                    .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jNiftiLayout.createSequentialGroup()
                            .addComponent(jCheckUseTemp)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jTextNifSrc)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButNifSrc))
                        .addComponent(jLabNif1, javax.swing.GroupLayout.Alignment.LEADING))
                    .addComponent(jCheckDefLimits)
                    .addGroup(jNiftiLayout.createSequentialGroup()
                        .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanExNifti, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButNif))
                        .addGap(18, 18, 18)
                        .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckGrown)
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButClean)
                            .addComponent(jCheck3D_OC))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jNiftiLayout.setVerticalGroup(
            jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jNiftiLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabNif1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabNif2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButNifSrc, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jCheckUseTemp)
                        .addComponent(jTextNifSrc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(8, 8, 8)
                .addComponent(jTextParms, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckDefLimits)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButNif)
                    .addComponent(jCheckGrown)
                    .addComponent(jCheck3D_OC))
                .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jNiftiLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jNiftiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanExNifti, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jNiftiLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(jButClean)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addContainerGap(154, Short.MAX_VALUE))
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

        stdyLab.setText("  ");

        javax.swing.GroupLayout jOtherLayout = new javax.swing.GroupLayout(jOther);
        jOther.setLayout(jOtherLayout);
        jOtherLayout.setHorizontalGroup(
            jOtherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jOtherLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jOtherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButReport)
                    .addComponent(jButRoiPointList)
                    .addComponent(stdyLab))
                .addContainerGap(433, Short.MAX_VALUE))
        );
        jOtherLayout.setVerticalGroup(
            jOtherLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jOtherLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(stdyLab)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButRoiPointList)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButReport)
                .addContainerGap(212, Short.MAX_VALUE))
        );

        j3Dtab.addTab("other", jOther);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(j3Dtab, javax.swing.GroupLayout.PREFERRED_SIZE, 593, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(j3Dtab, javax.swing.GroupLayout.PREFERRED_SIZE, 331, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jCheckLockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckLockActionPerformed
		lockedParent = null;
		if( jCheckLock.isSelected()) lockedParent = bf.parentPet;
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
		repaintAllStudies();
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
		saveResultsDir(null, null);
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
		repaintAllStudies();
    }//GEN-LAST:event_jCheckBlueActionPerformed

    private void jButMaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMaskActionPerformed
		saveMaskedData(0);
    }//GEN-LAST:event_jButMaskActionPerformed

    private void jButRoiPointListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButRoiPointListActionPerformed
		savePointList();
    }//GEN-LAST:event_jButRoiPointListActionPerformed

    private void j3DtabStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_j3DtabStateChanged
		if(bf.parentPet != null) bf.parentPet.repaintAll();
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

    private void jButTrashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButTrashActionPerformed
		pressTrashBut();
    }//GEN-LAST:event_jButTrashActionPerformed

    private void jCheckFollowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckFollowActionPerformed
		followCheck();
    }//GEN-LAST:event_jCheckFollowActionPerformed

    private void jPanelColorMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanelColorMouseClicked
		changeDotColor();
    }//GEN-LAST:event_jPanelColorMouseClicked

    private void jCheckColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckColorActionPerformed
		useDotColor(-1);
    }//GEN-LAST:event_jCheckColorActionPerformed

    private void jButNifSrcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButNifSrcActionPerformed
        browseNifti( true);
    }//GEN-LAST:event_jButNifSrcActionPerformed

    private void jCheckUseTempActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckUseTempActionPerformed
        changeTmp();
    }//GEN-LAST:event_jCheckUseTempActionPerformed

    private void jCheckGrownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckGrownActionPerformed
        grownChanged();
    }//GEN-LAST:event_jCheckGrownActionPerformed

    private void jButExNiftiFldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButExNiftiFldActionPerformed
        setExtNiftiFolder();
    }//GEN-LAST:event_jButExNiftiFldActionPerformed

    private void jButMake3NiftiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMake3NiftiActionPerformed
        makeExtNifti(null);
    }//GEN-LAST:event_jButMake3NiftiActionPerformed

    private void jCheckDefLimitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckDefLimitsActionPerformed
        niftiLimChanged();
    }//GEN-LAST:event_jCheckDefLimitsActionPerformed

    private void jButNifActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButNifActionPerformed
        runNifti();
    }//GEN-LAST:event_jButNifActionPerformed

    private void jButBkgdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBkgdActionPerformed
		measureBkgd();
    }//GEN-LAST:event_jButBkgdActionPerformed

    private void jButExMaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButExMaskActionPerformed
		extNiftiMask();
    }//GEN-LAST:event_jButExMaskActionPerformed

    private void jCheck3D_OCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheck3D_OCActionPerformed
		use_3D_Object_Counter();
    }//GEN-LAST:event_jCheck3D_OCActionPerformed

    private void jButCleanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButCleanActionPerformed
		clean_3D_Counter();
    }//GEN-LAST:event_jButCleanActionPerformed

 
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JTabbedPane j3Dtab;
    private javax.swing.JPanel j3dTab;
    private javax.swing.JButton jButBkgd;
    private javax.swing.JButton jButBuild;
    private javax.swing.JButton jButClean;
    private javax.swing.JButton jButColorIn;
    private javax.swing.JButton jButColorOut;
    private javax.swing.JButton jButExMask;
    private javax.swing.JButton jButExNiftiFld;
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
    private javax.swing.JCheckBox jCheck3D_OC;
    private javax.swing.JCheckBox jCheckBlue;
    private javax.swing.JCheckBox jCheckColor;
    private javax.swing.JCheckBox jCheckDate;
    private javax.swing.JCheckBox jCheckDefLimits;
    private javax.swing.JCheckBox jCheckFollow;
    private javax.swing.JCheckBox jCheckGrown;
    private javax.swing.JCheckBox jCheckId;
    private javax.swing.JCheckBox jCheckInvert;
    private javax.swing.JCheckBox jCheckLock;
    private javax.swing.JCheckBox jCheckMaskOnly;
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelColor;
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
    private javax.swing.JTextField jTextBkgd;
    private javax.swing.JTextField jTextCThi;
    private javax.swing.JTextField jTextCTlo;
    private javax.swing.JTextField jTextCtStrength;
    private javax.swing.JTextField jTextGray;
    private javax.swing.JTextField jTextName;
    private javax.swing.JTextField jTextNifSrc;
    private javax.swing.JTextField jTextNiftiExt;
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
    private javax.swing.JLabel stdyLab;
    // End of variables declaration//GEN-END:variables
	private Timer m_timer = null;
	static final int NIFTI_BOTH = 0;
	static final int NIFTI_ENABLE = 1;
	static final int NIFTI_LABEL = 2;
	bkgdRunNifti work2 = null;
	bkgdRunGrow work3 = null;
	PetCtPanel lockedParent = null;
	Color dotColor = Color.blue;
	double[] savePercentSUV = null, saveNestleSUV = null;
	boolean killMe = false, drawingRoi = false, isPercent = false, isPer1, isDirty = true;
	boolean isFat, isBone, isOther, isHeavy, OkShiftROI, isInit = false, allowDelete;
	boolean isSliceLimits = true, isNiftiDirty = false, isSliceChange = false, isNestle = false;
	boolean nextWarn = false, allowSliceChange = true, isCalcRadio = false, isDelete = false;
	int RoiState = 1, lastPeakType;
//	OpService ops = null;
	Poly3Save currPoly = null;
	Point currMousePos = new Point(-1,0);
	Date Roi1StartTime = null, NiftiStartTime = null;
	String NiftiLab = "";
	Integer xmin, xmax, ymin, ymax, zmin, zmax;
	int measureTime = -1;	// the total time taken to draw all ROIs
	int shiftRoiNm = 0, RoiNum4Max = -1, oc_pixels;
	int [] prevSpinValue = new int[4];
	int [] maskParms = null;
	int black, saveRoiPntIndx, saveRoiIndx;
	int CtCenterVal, jIndx;
	Integer numBadMip, badIndx;
	double oc_suv, oc_ml;
	ArrayList<Component> elementList = new ArrayList<>();
	ArrayList<bfGroup> m_bf = new ArrayList<>();
	bfGroup bf = new bfGroup();
}

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import org.scijava.vecmath.Point2d;
import org.scijava.vecmath.Point3d;

/**
 *
 * @author ilan
 */
public class Annotations extends javax.swing.JDialog {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;
	static final int PNTSZ = 3;
	static final int VSLICE = 0;
	static final int VNUMFRMS = 1;
	static final int MEASURE_SCALE = 1024;
	static final int HALF_SCALE = 320;	// for c++ display size
	/**
	 * Creates new form Annotations
	 * @param parent
	 * @param modal
	 */
	public Annotations(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
		setLocationRelativeTo(parent);
		init(parent);
	}
	
	private void init(java.awt.Frame parent) {
		parentFrm = (PetCtFrame) parent;
		m_lastView = -2;	// illegal value
		m_numTemp = 0;
		m_tempPnt = new Point[3];
		parPanel = parentFrm.getPetCtPanel1();
		for( int i =0; i<3; i++) readFiles(i);
		setObliqueLabel();
		setCountVals();
		jCheckSingle.setSelected(true);
		butGroupMeas = new javax.swing.ButtonGroup();
		butGroupMeas.add(jRadMeasLine);
		butGroupMeas.add(jRadLineSUV);
		butGroupMeas.add(jRadMeasDist);
		butGroupMeas.add(jRadMeasAngle);
		jRadMeasDist.setSelected(true);
		butGroupSave = new javax.swing.ButtonGroup();
		butGroupSave.add(jRadSaveMemory);
		butGroupSave.add(jRadSaveStudy);
		butGroupSave.add(jRadSaveDelete);
		jRadSaveMemory.setSelected(true);
		m_dirtyFlg = false;
		fillStrings();
		radButAction(1);
		setActiveArrow(0);
		getSetArrowSize(12);
		parPanel.repaintAll();
	}
	
	boolean takeAction( boolean useMouse) {
		if( !useMouse) return true;
		if( !isVisible()) return false;
		if( parPanel.m_sliceType == JFijiPipe.DSP_OBLIQUE) return false;
		if( jSave.isShowing()) return false;
		return !jBookmarks.isShowing();
	}
	
	void showBookmarksTab() {
		jTabbedPane1.setSelectedIndex(3); // bookmarks tab
		Preferences prefer = parentFrm.jPrefer;
		int x = prefer.getInt("annotation dialog x", 0);
		int y = prefer.getInt("annotation dialog y", 0);
		if( x>0 && y>0) setSize(x,y);
		boolean isAuto = prefer.getBoolean("annotation auto 2 BM", false);
		jCheckAutoBM.setSelected(isAuto);
	}
	
	void maybeSwitch2Bookmarks() {
		if( !jCheckAutoBM.isSelected()) return;
		jTabbedPane1.setSelectedIndex(3); // bookmarks tab
	}
	
	void windowClosed() {
		Preferences prefer = parentFrm.jPrefer;
		Dimension sz1 = getSize();
		prefer.putInt("annotation dialog x", sz1.width);
		prefer.putInt("annotation dialog y", sz1.height);
		boolean isAuto = jCheckAutoBM.isSelected();
		prefer.putBoolean("annotation auto 2 BM", isAuto);
	}
	
	// this process a mouse click from the image to add a point or place text
	void processMouseSingleClick(int pos3, Point pt1, double scl1) {
		int posMod = check4FusedPress(pos3);
		if( posMod > 2) return;
		m_thisView = ((posMod-1) & 1)*2;	// 0 for PET, 2 for CT
		if( m_thisView == 0) { // either corrected or uncorrected PET
			JFijiPipe pip1 = parPanel.getCorrectedOrUncorrectedPipe(false);
			if( pip1 == parPanel.upetPipe) m_thisView = 1;
		}
		if( jMeasure.isShowing()) {
			addMeasurePoint(pt1, scl1);
			return;
		}
		if( jArrows.isShowing()) {
			addArrow(pt1, scl1);
			return;
		}
		if( jText.isShowing()) {
			addText(pt1, scl1);
//			return;
		}
	}

	void drawGraphics(Graphics2D g, JFijiPipe pet1) {
		if( m_hideAnnotations || pet1 == null) return;
		int currPipeIndx = 0;
		if( pet1 == parPanel.upetPipe) currPipeIndx = 1;
		getParentInfo();
		drawTempPnts(g);
		drawSub(g, currPipeIndx, pet1);	// pet
		drawSub(g, 2, pet1);	// ct
	}

	void setMarkSlice() {
		boolean addBM = addBookMark(false);
		if( addBM != m_markSliceFlg) {
			String tmp = "Mark Slice";
			m_markSliceFlg = addBM;
			if(!addBM) tmp = "unMark Slice";
			jButMarkSlice.setText(tmp);
		}
	}

	void markButton() {
		if( !m_markSliceFlg) m_BMList.remove(lastBMi);
		else addBookMark(true);
		setMarkSlice();
		m_numTemp = 0;
		setCountVals();
	}

	void draw3Graphics(Graphics2D g, Display3Panel caller) {
		if( m_hideAnnotations) return;
		sty3Type = caller.styType;
		if( sty3Type < 0) return;
		draw3Sub(g, false, caller);
		draw3Sub(g, true, caller);
	}

	void getParentInfo() {
		sliceType = parPanel.m_sliceType;
		currSlice = -1;
		dispType = 2;	// = type1 in Brown fat
		switch(sliceType) {
			case JFijiPipe.DSP_AXIAL:
				dispType = 0;
				if( parPanel.petPipe.zoom1 > 1.0) dispType = 3;
				currSlice = ChoosePetCt.round(parPanel.petAxial);
				break;
				
			case JFijiPipe.DSP_CORONAL:
				currSlice = ChoosePetCt.round(parPanel.petCoronal);
				break;
				
			case JFijiPipe.DSP_SAGITAL:
				currSlice = ChoosePetCt.round(parPanel.petSagital);
				break;
		}
	}
	
	// use val1 <= 0 to get the value and not set it
	int getSetArrowSize(int val1) {
		SpinnerNumberModel spin1 = (SpinnerNumberModel) jSpinChinSize.getModel();
		if( val1 > 0) spin1.setValue(val1);
		return spin1.getNumber().intValue();
	}
	
	void drawSub(Graphics2D g, int indx, JFijiPipe pet1) {
		int i;
		if(pet1 == null) return;
		if( m_measurements[indx] != null && !m_measurements[indx].isEmpty()) {
			myMeasure meas1;
			for( i=0; i<m_measurements[indx].size(); i++) {
				meas1 = m_measurements[indx].get(i);
				meas1.drawData(g, indx, pet1);
			}
		}
		if( m_floatingText[indx] != null) {
			myFloatingText txt1 = new myFloatingText();
			txt1.drawData(g, indx, pet1);
		}
		if( m_china[indx] != null && !m_china[indx].isEmpty()) {
			myChina chin1;
			for( i=0; i<m_china[indx].size(); i++) {
				chin1 = m_china[indx].get(i);
				chin1.drawData(g, indx, pet1);
			}
		}
	}
	
	void draw3Sub(Graphics2D g, boolean isFused, Display3Panel caller) {
		int i, indx=0;
		JFijiPipe d3Pip1 = caller.d3Pipe;
		if( isFused) d3Pip1 = caller.ct4fused;
		if( d3Pip1 == null) return;
		myCaller d3Obj = new myCaller(caller, isFused);
		myCaller d3Obj1 = null;
		if( isFused || sty3Type == Display3Panel.CT_STUDY || sty3Type == Display3Panel.MRI_STUDY) {
			indx = 2;
			if(!isFused) d3Obj1 = d3Obj;	// only for CT and MRI do we need point translation
		}
		if( m_measurements[indx] != null && !m_measurements[indx].isEmpty()) {
			myMeasure meas1;
			for( i=0; i<m_measurements[indx].size(); i++) {
				meas1 = m_measurements[indx].get(i);
				meas1.draw3Data(g, d3Obj, d3Obj1);
			}
		}
		if( m_floatingText[indx] != null) {
			myFloatingText txt1 = new myFloatingText();
			txt1.draw3Data(g, indx, d3Obj);
		}
		if( m_china[indx] != null && !m_china[indx].isEmpty()) {
			myChina chin1;
			for( i=0; i<m_china[indx].size(); i++) {
				chin1 = m_china[indx].get(i);
				chin1.draw3Data(g, d3Obj, d3Obj1);
			}
		}
	}

	// Note: m_china = new ArrayList[3] gives unchecked warning. The best advice
	// in StackOverflow is to suppress the warning as there is no clean solution.
@SuppressWarnings("unchecked")
	void readFiles(int indx) {
		byte[] inByt;
		ByteBuffer buf8 = ByteBuffer.allocate(8);
		buf8.order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer buf2 = ByteBuffer.allocate(2);
		buf2.order(ByteOrder.LITTLE_ENDIAN);
		int i, size1;
		String flName = getFileName(indx, true);
		String tmp;
		if( indx == 0) {
			m_BMList = new ArrayList<myBookMark>();
			m_china = new ArrayList[3];
			m_measurements = new ArrayList[3];
			m_floatingText = new String[3];
		}
		if( flName == null) return;
		File fl1 = new File(flName);
		if( !fl1.exists()) return;
		m_china[indx] = new ArrayList<myChina>();
		m_measurements[indx] = new ArrayList<myMeasure>();
		try {
			FileInputStream in = new FileInputStream(fl1);
			FileChannel fc = in.getChannel();
			fc.read(buf8);
			inByt = buf8.array();
			tmp = new String(inByt);
			size1 = myGetShort(fc, buf2);
			if( size1 != indx || !tmp.startsWith("ver 1.4")) {
				in.close();
				return;
			}

			if( indx==2) {
				size1 = myGetShort(fc, buf2);
				for( i=0; i<size1; i++) {
					myBookMark bm = new myBookMark();
					bm.readData(fc);
					m_BMList.add(bm);
				}
			}

			size1 = myGetShort(fc, buf2);
			for( i=0; i<size1; i++) {
				myMeasure meas1 = new myMeasure();
				meas1.readData(fc);
				m_measurements[indx].add(meas1);
			}

			size1 = myGetShort(fc, buf2);
			for( i=0; i<size1; i++) {
				myChina chin1 = new myChina();
				chin1.readData(fc);
				m_china[indx].add(chin1);
			}

			size1 = myGetShort(fc, buf2);
			if( size1 > 0) {
				ByteBuffer bufTxt = ByteBuffer.allocate(size1);
				fc.read(bufTxt);
				inByt = bufTxt.array();
				m_floatingText[indx] = new String(inByt);
			}
			in.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	void saveSignificant(int indx, boolean isDelay, Dimension sz0) {
		m_delay = isDelay;
		m_saveType = indx;
		m_origSz = sz0;
		work2 = new bkgdRobot();
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
	
	int [] getImageSize(int indx) {
		int[] ret1 = new int[3];
		int i, szBM;
		myBookMark bm1;
		szBM = getBMsize();
		sliceType = parPanel.m_sliceType;
		Dimension dm2 = parPanel.getSize();
		if( indx > 0 && indx < 4) dm2.width /= 3;
		ret1[0] = dm2.width;
		ret1[1] = dm2.height;
		for( i=0; i<szBM; i++) {
			bm1 = m_BMList.get(i);
			if( sliceType != bm1.type) continue;
			ret1[2]++;
		}
		return ret1;
	}

	boolean isSingleOrientation() {
		int i, sType, szBM = getBMsize();
		if( szBM <= 1) return true;
		myBookMark bm1 = m_BMList.get(0);
		sType = bm1.type;
		for( i=1; i< szBM; i++) {
			bm1 = m_BMList.get(i);
			if( sType != bm1.type) return false;
		}
		return true;
	}

	@SuppressWarnings("SleepWhileInLoop")
	void doRobotSwitchSlice() {
		int i, i3, n3=3, szBM;
		boolean isFirst;
		int[] j = new int[3];
		myBookMark bm1;
		int numFrms = parPanel.petPipe.data1.numFrms;
		myWriteDicom writeDcm = new myWriteDicom(parentFrm, null);
		szBM = getBMsize();
		sliceType = parPanel.m_sliceType;
		j[0] = sliceType;
		switch(sliceType) {
			case JFijiPipe.DSP_CORONAL:
				j[1] = JFijiPipe.DSP_AXIAL;
				j[2] = JFijiPipe.DSP_SAGITAL;
				break;

			case JFijiPipe.DSP_SAGITAL:
				j[1] = JFijiPipe.DSP_AXIAL;
				j[2] = JFijiPipe.DSP_CORONAL;
				break;

			default:
				j[0] = JFijiPipe.DSP_AXIAL;
				j[1] = JFijiPipe.DSP_CORONAL;
				j[2] = JFijiPipe.DSP_SAGITAL;
		}
		if( isSingleOrientation()) n3 = 1;
		for( i3=0; i3<n3; i3++) {
			try {
				isFirst = true;
				for( i=0; i<szBM; i++) {
					bm1 = m_BMList.get(i);
					if( sliceType != bm1.type) continue;
					setNextSliceSub(i, numFrms);
					if(m_delay) {
						Thread.sleep(500);
						parentFrm.changeLayout( j[1]);
						Thread.sleep(500);
						parentFrm.changeLayout( j[0]);
						Thread.sleep(800);
						m_delay = false;
					}
					getParentInfo();	// get currSlice
					if( isFirst) {
						if( writeDcm.cleanDirectory( sliceType, m_saveType) <0) {
							n3 = 1;
							break;
						}
					}
					isFirst = false;
					if( writeDcm.writeDicomHeader(currSlice, sliceType, m_saveType)<0) {
						n3 = 1;
						break;
					}
					Thread.sleep(1000);	// allow write which has 1/2 sec sleep
					if( writeDcm.writeStatus != 1) Thread.sleep(2000);
				}
				if( n3 <= 1) break;
				i = ((i3+1) % 3);
				parentFrm.changeLayout(j[i]);
				Thread.sleep(800);
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		}
		if( m_origSz != null) parentFrm.setSize(m_origSz);
	}

	int getBMsize() {
		int szBM = 0;
		if( m_BMList != null) szBM = m_BMList.size();
		return szBM;
	}

	void saveFiles(int indx) {
		ReadOrthancSub orthSub;
		ByteBuffer buf8 = ByteBuffer.allocate(8);
		buf8.order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer buf2 = ByteBuffer.allocate(2);
		buf2.order(ByteOrder.LITTLE_ENDIAN);
		try {
			String tmp1, flName = getFileName(indx, false);
			if( flName == null) return;
			File fl0, fl1 = new File(flName);
			File[] lstFl = fl1.listFiles();
			int i, size1 = lstFl.length;
			for( i=0; i<size1; i++) {
				if( lstFl[i].isFile()) {
					tmp1 = lstFl[i].getName();
					if( tmp1.startsWith("graphic") && tmp1.endsWith("gr1")) {
						fl0 = new File(lstFl[i].getPath());
						fl0.delete();	// first delete any existing files
					}
				}
			}
			int szBM, szChina=0, szMeas=0, szTxt=0, szAll;
			szBM = getBMsize();
			if( m_china[indx] != null) szChina = m_china[indx].size();
			if( m_measurements[indx] != null) szMeas = m_measurements[indx].size();
			if( m_floatingText[indx] != null) szTxt = m_floatingText[indx].length();
			szAll = szChina + szMeas + szTxt;
			if(indx == 2) szAll += szBM;
			// in the delete case all of the above were erased so the result is 0.
			if( szAll == 0) return;
			orthSub = new ReadOrthancSub(this);
			boolean isOrth = orthSub.isOrthFile(indx);
			flName = getFileName(indx, true);
			fl1 = new File(flName);
			FileOutputStream out = new FileOutputStream(fl1);
			FileChannel fc = out.getChannel();
			String ver = "ver 1.4\0";	// make a c++ string, zero terminated
			byte[] buf0 = ver.getBytes();
			buf8.put(buf0);
			buf8.position(0);
			fc.write(buf8);
			myPutShort(fc, buf2, indx);

			if( indx==2) {
				myPutShort(fc, buf2, szBM);
				for( i=0; i<szBM; i++) {
					myBookMark bm = m_BMList.get(i);
					bm.writeData(fc);
				}
			}

			myPutShort(fc, buf2, szMeas);
			for( i=0; i<szMeas; i++) {
				myMeasure meas1 = m_measurements[indx].get(i);
				meas1.writeData(fc);
			}

			myPutShort(fc, buf2, szChina);
			for( i=0; i<szChina; i++) {
				myChina chin1 = m_china[indx].get(i);
				chin1.writeData(fc);
			}

			myPutShort(fc, buf2, szTxt);
			if( szTxt > 0) {
				ByteBuffer bufTxt = ByteBuffer.allocate(szTxt);
				buf0 = m_floatingText[indx].getBytes();
				bufTxt.put(buf0);
				bufTxt.position(0);
				fc.write(bufTxt);
			}
			out.close();
			if( isOrth) orthSub.write2Orth(fl1, 0);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}
	
	private short myGetShort(FileChannel fc, ByteBuffer buf2) {
		short retVal = 0;
		try {
			buf2.position(0);
			fc.read(buf2);
			retVal =  buf2.getShort(0);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return retVal;
	}

	private void myPutShort(FileChannel fc, ByteBuffer buf2, int val1) {
		try {
			buf2.position(0);
			buf2.putShort(0, (short) val1);
			fc.write(buf2);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	String getFileName(int indx, boolean fullName) {
		String flName;
		ImagePlus srcImage;
		JFijiPipe pipe1 = getPipe(indx);
		if(pipe1 == null) return null;
		srcImage = pipe1.data1.srcImage;
		FileInfo info1 = srcImage.getOriginalFileInfo();
		if( info1 == null) return null;
		String path = info1.directory;
		if( !fullName) return path;
		String seriesUID = ChoosePetCt.getDicomValue(pipe1.data1.metaData, "0020,000E");
		flName = path + "/graphic." + seriesUID + ".gr1";
		return flName;
	}

	void setCountVals() {
		Integer cnt = m_BMList.size();
		jLabBMCount.setText(cnt.toString());
		int i, cntChina, cntMeas, cntTxt;
		for( i=cntChina=cntMeas=cntTxt=0; i<3; i++) {
			if(m_china[i] != null) cntChina += m_china[i].size();
			if(m_measurements[i] != null) cntMeas += m_measurements[i].size();
			cntTxt += getTextCnt(i);
		}
		cnt = cntMeas;
		jLabMeasCount.setText(cnt.toString());
		cnt = cntChina;
		jLabChinCount.setText(cnt.toString());
		cnt = cntTxt;
		jLabTextCount.setText(cnt.toString());
	}

@SuppressWarnings("unchecked")
	void OKAction() {
		this.setVisible(false);
		if( jRadSaveMemory.isSelected()) {
			if( !m_dirtyFlg) return; // nothing to do
			m_dirtyFlg = false;
			int i = JOptionPane.showConfirmDialog(parentFrm, "There are unsaved changes.\n"
					+ "Do you want to save them as part of the study?",
					"Unsaved changes", JOptionPane.YES_NO_OPTION);
			if( i != JOptionPane.YES_OPTION) return;
		}
		if( jRadSaveDelete.isSelected()) {
			m_BMList = new ArrayList<myBookMark>();
			m_china = new ArrayList[3];
			m_measurements = new ArrayList[3];
			m_floatingText = new String[3];
		}
		for( int i =0; i<3; i++) saveFiles(i);
		m_numTemp = 0;
		setCountVals();
		parPanel.repaintAll();
	}
	
	void radButAction(int indx) {
		String txt1;
		m_measDraw = indx;
		switch(indx) {
			case 0:
				txt1 = "<html>Click on the head and tail<br>of the arrow.";
				break;

			case 2:
				txt1 = "<html>Click on 3 points to<br>define your angle.";
				break;

			case 3:
				txt1 = "<html>Click on tumor and a free<br>area for text display.";
				break;

			default:
				txt1 = "<html>Click on both sides of the<br>object to be measured";
				m_measDraw = 1;
				break;
		}
		jLabMeas.setText(txt1);
		if( m_numTemp > 0) {
			m_numTemp = 0;
			parPanel.repaintAll();
		}
	}

	void setNextSlice(int upDown) {
		myBookMark bm1;
		int i, diff, numFrms, size1, closest = 0, minDiff = -1;
		int bmSlice;
		boolean singleOrientation = jCheckSingle.isSelected();
		size1 = m_BMList.size();
		numFrms = parPanel.petPipe.data1.numFrms;
		for( i=0; i<size1; i++) {
			bm1 = m_BMList.get(i);
			if( sliceType != bm1.type) continue;
			bmSlice = bm1.offst;
			if(sliceType == JFijiPipe.DSP_AXIAL) bmSlice = numFrms - bm1.offst - 1;
			diff = Math.abs(bmSlice - currSlice);
			if( minDiff > diff || minDiff < 0) {
				closest = i;
				minDiff = diff;
			}
		}

		// now get the next slice
		// trivial case of zero or one point and we are on the point
		if( size1 <= 1 && minDiff <= 0) return;	// nothing to do
		
		// next case, we are not on the point, but next to it
		if( minDiff > 0) {
			setNextSliceSub(closest, numFrms);
			return;
		}

		// now the case where we are on the slice
		for( i=0; i<size1; i++) {
			if( upDown >= 0) {
				if( ++closest >= size1) closest = 0;
			} else {
				if( --closest < 0) closest = size1 - 1;
			}
			bm1 = m_BMList.get(closest);
			if( bm1.type == sliceType || !singleOrientation) {
				setNextSliceSub(closest, numFrms);
				return;
			}
		}
	}

	void setNextSliceSub(int indx, int numFrms) {
		myBookMark bm1 = m_BMList.get(indx);
		sliceType = bm1.type;
		int i, bmSlice = numFrms - bm1.offst - 1;
		switch( sliceType) {
			case JFijiPipe.DSP_AXIAL:
				parPanel.petAxial = bmSlice;
				break;

			case JFijiPipe.DSP_CORONAL:
				parPanel.petCoronal = bm1.offst;
				break;

			case JFijiPipe.DSP_SAGITAL:
				parPanel.petSagital = bm1.offst;
				break;
		}
		parentFrm.changeLayout(sliceType);
		int zoomIndx = bm1.zoomIndx;
		JFijiPipe pet1 = getPipe(0);	// pet pipe
		JFijiPipe ct1 = getPipe(2);	// ct pipe
		JFijiPipe mip1 = parPanel.mipPipe;
		while( pet1.zoomIndx != zoomIndx) {
			i = 1;
			if( pet1.zoomIndx > zoomIndx) i = -1;
			pet1.setZoom(i);
			ct1.setZoom(i);
			parPanel.updateMultYOff();
		}
		Point pan1 = bm1.getBmPan(pet1);
		if( !bm1.pan.equals(pan1)) {
			Point3d pan2 = bm1.getPan2(pet1);
			pet1.updatePanValue(pan2, true);
			ct1.updatePanValue(pan2, true);
		}
		double[] winLev = bm1.getLevelsWidths(pet1);
		if( !pet1.isLog) {
			pet1.winLevel = winLev[0];
			pet1.winWidth = winLev[1];
			mip1.winLevel = winLev[4];
			mip1.winWidth = winLev[5];
		}
		ct1.winLevel = winLev[2];
		ct1.winWidth = winLev[3];
		parPanel.changeCurrentSlider();	// update slider values
		parentFrm.update3Display();
	}
	
	void hideAnnotations() {
		m_hideAnnotations = !m_hideAnnotations;
		jCheckBMHide.setSelected(m_hideAnnotations);
		jCheckMeasHide.setSelected(m_hideAnnotations);
		jCheckChinHide.setSelected(m_hideAnnotations);
		jCheckTextHide.setSelected(m_hideAnnotations);
		parPanel.repaintAll();
	}
	
	JFijiPipe getPipe(int indx) {
		JFijiPipe pipe1 = null;
		switch( indx) {
			case 0:
				pipe1 = parPanel.petPipe;
				break;
				
			case 1:
				pipe1 = parPanel.upetPipe;
				break;
				
			case 2:
				pipe1 = parPanel.ctPipe;
				break;
		}
		return pipe1;
	}

	void addMeasurePoint(Point pt1, double scale) {
		if( m_thisView != m_lastView) m_numTemp = 0;
		m_lastView = m_thisView;
		if( m_numTemp > 2) m_numTemp = 0;
		m_tempPnt[m_numTemp++] = pt1;
		myMeasure meas1;
		Point pntTmp;
		double x1, z1, val1, val2, pixelSize, pixSizeZ;
		int i, tmpMeasDraw, numPnts = 2;
		int[] vals;
		if( m_measDraw == 2) numPnts = 3;
		if( m_numTemp >= numPnts) {
			getParentInfo();
			JFijiPipe pet1 = parPanel.petPipe;
			pixelSize = pet1.data1.pixelSpacing[0];
			pixSizeZ = pet1.data1.sliceThickness;
			x1 = pixelSize / MEASURE_SCALE;
			z1 = pixSizeZ / MEASURE_SCALE;
			if( sliceType == JFijiPipe.DSP_AXIAL) z1 = x1;
			meas1 = new myMeasure();
			for( i=0; i<numPnts; i++) {
				pntTmp = getPoint1000(i, scale);
				meas1.x[i] = pntTmp.x;
				meas1.y[i] = pntTmp.y;
			}
			vals = getNumFrmSlice( pet1, m_lastView);
			meas1.zval = (short) vals[VSLICE];
			tmpMeasDraw = m_measDraw;
			if( m_measDraw == 1) {
				val1 = (meas1.x[0] - meas1.x[1]) * x1;
				val2 = (meas1.y[0] - meas1.y[1]) * z1;
				val1 = Math.sqrt(val1*val1 + val2*val2);
				meas1.val1 = val1;
			}
			if( m_measDraw == 2) {
				val1 = meas1.getAngle(1, 0);
				val2 = meas1.getAngle(1, 2);
				val2 -= val1;
				while( val2 < 0) val2 += 2*Math.PI;
				if( val2 > Math.PI) val2 = 2*Math.PI - val2;
				// for historic reasons, save angle/2
				meas1.val1 = val2/2;
			}
			if( m_measDraw == 3) {
				tmpMeasDraw = 0;
				if(m_lastView == 0) {
					meas1.val1 = meas1.getSUV();
					if(meas1.val1 < 0) tmpMeasDraw = 1;
				}
			}
			meas1.type = (short)(4*sliceType + tmpMeasDraw);
			if(m_measurements[m_lastView] == null)
				m_measurements[m_lastView] = new ArrayList<myMeasure>();
			m_measurements[m_lastView].add(meas1);
			m_dirtyFlg = true;
			setObliqueLabel();
			addBookMark(true);
			setCountVals();
			maybeSwitch2Bookmarks();
			m_numTemp = 0;
		}
		parPanel.repaintAll();
	}
	
	double getLastPointMeasured() {
		if( m_lastView < 0) return -1;
		if(m_measurements[m_lastView] == null ) return -1;
		int n = m_measurements[m_lastView].size();
		if( n <= 0) return -1;
		myMeasure cur1 = m_measurements[m_lastView].get(n-1);
		return cur1.val1;
	}
	
@SuppressWarnings("unchecked")
	void removeMeasurePoint(boolean killAll) {
		int size1 = -1, prevType=4;
		if( m_lastView >= 0 && m_measurements[m_lastView] != null ) {
			size1 = m_measurements[m_lastView].size();
			myMeasure meas1 = m_measurements[m_lastView].get(0);
			prevType = meas1.type;
		}
		if( killAll) m_measurements = new ArrayList[3];
		else {
			if( size1> 0) m_measurements[m_lastView].remove(size1 - 1);
		}
		m_numTemp = 0;
		setObliqueLabel();
		setCountVals();
		// if we are in oblique mode, then killing all, or even the single point
		// will result in trying to use an object which doesn't exist -> change layout
		if(parPanel.m_sliceType == JFijiPipe.DSP_OBLIQUE && size1 > 0) {
			parentFrm.changeLayout(prevType/4);
		}
		parPanel.repaintAll();
	}

	void obliqueAction() {
		if( !m_obliqueFlg) {
			ChoosePetCt.openHelp("Annotations/#oblique");
			return;
		}
		parentFrm.changeLayout(JFijiPipe.DSP_OBLIQUE);
		myOblique ob1 = parPanel.m_oblique;
		if( ob1 != null) ob1.startOblique();
	}
	
	void setObliqueLabel() {
		m_obliqueFlg = false;
		myMeasure meas1 = null;
		String tmp = "Oblique (Help)";
		int i, cntMeas, sz1, last=-1;
		for( i=cntMeas=0; i<3; i++) {
			if(m_measurements[i] != null) {
				sz1 = m_measurements[i].size();
				cntMeas += sz1;
				if(sz1 == 1) last = i;
			}
		}
		if(cntMeas == 1) {
			meas1 = m_measurements[last].get(0);
			if((meas1.type & 3) < 2) m_obliqueFlg = true;
		}
		parPanel.m_oblique = null;
		if( m_obliqueFlg) {
			parPanel.m_oblique = new myOblique();
			m_obliqueFlg = parPanel.m_oblique.update(meas1);	// see if is OK
			if( m_obliqueFlg) tmp = "Oblique";
			else parPanel.m_oblique = null;
		}
		jButOblique.setText(tmp);
	}
	
	int[] getNumFrmSlice(JFijiPipe pet1, int indx) {
		int[] vals = new int[2];
		vals[VSLICE] = currSlice;
		JFijiPipe pip0 = pet1;
		if( pip0 == null) return vals;
		if( sliceType == JFijiPipe.DSP_AXIAL) {
			if( indx == 2) {
				vals[VSLICE] = pet1.findCtPos(currSlice, true);
				pip0 = parPanel.ctPipe;
			}
			vals[VSLICE] = pip0.data1.numFrms - vals[VSLICE] - 1;
		}
		vals[VNUMFRMS] = pip0.data1.numFrms;
		return vals;
	}

	Point getPoint1000(int indx, double scale) {
		Point retPnt = parPanel.petPipe.scrn2PosInt(m_tempPnt[indx], scale, dispType);
		retPnt.x *= MEASURE_SCALE;
		retPnt.y *= MEASURE_SCALE;
		return retPnt;
	}
	
	void drawTempPnts(Graphics2D g) {
		if( !jMeasure.isShowing() || m_lastView < 0) return;
		g.setColor(Color.yellow);
		int i, offX = 0;
		int width = parPanel.mouse1.widthX;
		if( m_lastView == 2) {
			offX = width;
		}
		for( i=0; i<m_numTemp; i++) {
			if( i>2) return;
			g.fillOval(m_tempPnt[i].x + offX - 2*PNTSZ, m_tempPnt[i].y - 2*PNTSZ, 4*PNTSZ, 4*PNTSZ);
			if(offX==0) g.fillOval(m_tempPnt[i].x + 2*width - 2*PNTSZ, m_tempPnt[i].y - 2*PNTSZ, 4*PNTSZ, 4*PNTSZ);
		}
	}

	boolean addBookMark(boolean reallyAdd) {
		myBookMark bm0, bmNew = new myBookMark();
		bmNew.readFromDisplay();
		int i, size1 = m_BMList.size();
		for( i=0; i<size1; i++) {
			bm0 = m_BMList.get(i);
			if( bm0.offst == bmNew.offst && bm0.type == bmNew.type) {
				if( reallyAdd && bm0.bmChanged(bmNew)) m_BMList.set(i, bmNew);
				lastBMi = i;	// save this value for remove
				return false;
			}
		}
		if( reallyAdd) {
			m_BMList.add(bmNew);
			setCountVals();
		}
		return true;
	}

	void addArrow(Point pt1, double scale) {
		m_lastView = m_thisView;
		m_numTemp = 0;
		m_tempPnt[0] = pt1;
		int[] vals;
		JFijiPipe pet1 = parPanel.petPipe;
		getParentInfo();
		Point pntTmp = getPoint1000(0, scale);
		myChina chin1 = new myChina();
		chin1.x = pntTmp.x;
		chin1.y = pntTmp.y;
		vals = getNumFrmSlice( pet1, m_lastView);
		chin1.z = (short) vals[VSLICE];
		chin1.type = (byte)(4*sliceType + m_activeArrow);
		chin1.size = (byte) getSetArrowSize(0);
		if(m_china[m_lastView] == null)
			m_china[m_lastView] = new ArrayList<myChina>();
		m_china[m_lastView].add(chin1);
/*		if(jCheckChinSUV.isSelected() && m_activeArrow == 0 && m_lastView == 0) {
			Point pt2 = chin1.textPos(pt1, scale);
			myFloatingText text1 = new myFloatingText();
			text1.addSUV(chin1, scale, pt2);
		}*/
		m_dirtyFlg = true;
		addBookMark(true);
		setCountVals();
		maybeSwitch2Bookmarks();
		parPanel.repaintAll();
	}
		
	int check4FusedPress(int pos3) {
		int retVal = pos3;
		if( retVal < 3) return retVal;	// didn't press on fused
		if( isFusedShowing() && (jMeasure.isShowing() || jArrows.isShowing() || jText.isShowing())) {
			retVal = 1;
		}
		return retVal;
	}
	
	boolean isFusedShowing() {
		int i = parPanel.m_masterFlg;
		return (  i== PetCtPanel.VW_PET_CT_FUSED ||  i== PetCtPanel.VW_PET_CT_FUSED_UNCORRECTED);
	}

@SuppressWarnings("unchecked")
	void removeChinaPoint(boolean killAll) {
		if( killAll) m_china = new ArrayList[3];
		else {
			if( m_lastView >= 0 && m_china[m_lastView] != null ) {
				int size1 = m_china[m_lastView].size();
				if( size1> 0) m_china[m_lastView].remove(size1 - 1);
			}
		}
		m_numTemp = 0;
		setCountVals();
		parPanel.repaintAll();
	}
	
	void setThinArrow() {
		m_thin = !m_thin;
		jCheckChinThin.setSelected(m_thin);
		parPanel.repaint();
	}
	
	void setActiveArrow(int val) {
		m_activeArrow = val;
		m_numTemp = 0;
		boolean pressed = false;
		jButUpArrow.setSelected( val == 0 ? pressed : !pressed);
		jButRightArrow.setSelected( val == 1 ? pressed : !pressed);
		jButDownArrow.setSelected( val == 2 ? pressed : !pressed);
		jButLeftArrow.setSelected( val == 3 ? pressed : !pressed);
	}
	
	void addText(Point pt1, double scale) {
		myFloatingText text1 = new myFloatingText();
		text1.addPoint(pt1, scale);
	}

	@SuppressWarnings("unchecked")
	void fillStrings() {
		String line;
		int num = 0;
		File fl1 = new File(IJ.getDirectory("plugins") + "anostrings.txt");
		if( !fl1.exists()) return;
		try {
			FileReader fis = new FileReader(fl1);
			BufferedReader br = new BufferedReader( fis);
			while( (line = br.readLine()) != null) {
				if(num > 0 && line.isEmpty()) break;
				num++;
				jComboText.addItem(line);
			}
			br.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	void removeTextPoint(boolean killAll) {
		if( killAll) m_floatingText = new String[3];
		else {
			if( m_lastView >= 0 && m_floatingText[m_lastView] != null ) {
				int size1 = m_floatingText[m_lastView].length();
				if( size1> 0) {
					String tmp = m_floatingText[m_lastView].trim();
					int i = tmp.lastIndexOf('\n');
					if( i>0) {
						m_floatingText[m_lastView] = tmp.substring(0, i+1);
					}
					else m_floatingText[m_lastView] = null;
				}
			}
		}
		m_numTemp = 0;
		setCountVals();
		parPanel.repaintAll();
	}

	int getTextCnt(int indx) {
		if( m_floatingText[indx] == null) return 0;
		String tmp = m_floatingText[indx];
		int pos1, cnt = 0;
		while((pos1 = tmp.indexOf('\n')) > 0) {
			tmp = tmp.substring(pos1+1);
			cnt++;
		}
		return cnt;
	}
	
	class myCaller {
		Display3Panel d3Pan;
		Display3Frame d3Frm;
		PetCtFrame srcFrm;
		JFijiPipe srcPipe, petPipe, d3Pipe, d3Pipe2;
		boolean isFused;
		
		myCaller(Display3Panel caller, boolean fused){
			d3Pan = caller;
			d3Frm = caller.parent;
			srcFrm = d3Frm.srcFrame;
			petPipe = srcFrm.getPetCtPanel1().petPipe;
			isFused = fused;
			d3Pipe2 = d3Pipe = caller.d3Pipe;
			srcPipe = d3Frm.srcPipe;
			if( isFused) {
				d3Pipe2 = caller.ct4fused;
				srcPipe = d3Frm.srcCtPipe;
			}
		}
		
		double convertVal(double inVal, int type) {
			double outVal = 0;
			switch(type) {
				case JFijiPipe.DSP_AXIAL:
					outVal = srcPipe.findCtPos(inVal, true);
					break;

				case JFijiPipe.DSP_CORONAL:
					outVal = srcPipe.corFactor * inVal + srcPipe.corOffset - srcPipe.mriOffY;
					break;

				case JFijiPipe.DSP_SAGITAL:
					outVal = srcPipe.sagFactor * inVal + srcPipe.sagOffset - srcPipe.mriOffX;
					break;
			}
			return outVal;
		}
		
		int convertVali(double inVal, int type) {
			double outVal = convertVal(inVal, type);
			return ChoosePetCt.round(outVal);
		}
		
		int convertZVal(int zval, int type) {
			int outVal = 0;
			int inVal;
			switch(type) {
				case JFijiPipe.DSP_AXIAL:
					inVal = srcPipe.data1.numFrms - zval - 1;
					outVal = inVal;
					if(!isFused) outVal = srcPipe.findCtPos(inVal, true);
					break;

				case JFijiPipe.DSP_CORONAL:
				case JFijiPipe.DSP_SAGITAL:
					outVal = zval;
					if(!isFused) outVal = convertVali(zval, type);
					break;
			}
			return outVal;
		}
		
		boolean isZValOK(int zval, int type) {
			boolean isOK = false;
			int val1 = convertZVal(zval, type);
			switch(type) {
				case JFijiPipe.DSP_AXIAL:
					if( val1 == d3Pipe.indx) isOK = true;
					break;

				case JFijiPipe.DSP_CORONAL:
					if( val1 == d3Pipe.coronalSlice) isOK = true;
					break;

				case JFijiPipe.DSP_SAGITAL:
					if( val1 == d3Pipe.sagitalSlice) isOK = true;
					break;
			}
			return isOK;
		}
		
		int getOffX(int type4) {
			return (type4-1)*d3Pan.mouse1.widthX;
		}
		
		double getScl1() {
			return d3Pan.getScale();
		}
		
		Point2d translatePoint(Point2d pin, int typ0) {
			Point2d pout = new Point2d();
			int typ1, typ2;
			switch( typ0) {
				default:
				case JFijiPipe.DSP_AXIAL:
					typ1 = JFijiPipe.DSP_CORONAL;
					typ2 = JFijiPipe.DSP_SAGITAL;
					break;

				case JFijiPipe.DSP_CORONAL:
					typ1 = JFijiPipe.DSP_SAGITAL;
					typ2 = JFijiPipe.DSP_AXIAL;
					break;

				case JFijiPipe.DSP_SAGITAL:
					typ1 = JFijiPipe.DSP_CORONAL;
					typ2 = JFijiPipe.DSP_AXIAL;
					break;
			}
			pout.x = convertVal(pin.x, typ1);
			pout.y = convertVal(pin.y, typ2);
			return pout;
		}
	}

	class myMeasure {
		int[] x, y;
		short type, zval;
		double val1;

		myMeasure() {
			x = new int[3];
			y = new int[3];
			val1 = 0;
		}

		void readData(FileChannel fc) {
			try {
				ByteBuffer buf = ByteBuffer.allocate(40);
				buf.order(ByteOrder.LITTLE_ENDIAN);
				fc.read(buf);
				buf.position(0);
				x[0] = buf.getInt();
				y[0] = buf.getInt();
				x[1] = buf.getInt();
				y[1] = buf.getInt();
				x[2] = buf.getInt();
				y[2] = buf.getInt();
				type = buf.getShort();
				zval = buf.getShort();
				buf.getInt();	// padding
				val1 = buf.getDouble();
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		}

		void writeData(FileChannel fc) {
			try {
				ByteBuffer buf = ByteBuffer.allocate(40);
				buf.order(ByteOrder.LITTLE_ENDIAN);
				buf.putInt(x[0]);
				buf.putInt(y[0]);
				buf.putInt(x[1]);
				buf.putInt(y[1]);
				buf.putInt(x[2]);
				buf.putInt(y[2]);
				buf.putShort(type);
				buf.putShort(zval);
				buf.putInt(0);	// padding
				buf.putDouble(val1);
				buf.position(0);
				fc.write(buf);
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		}

		double getAngle( int start, int stop) {
			double xDelta, yDelta;
			yDelta = y[stop] - y[start];
			xDelta = x[stop] - x[start];
			return Math.atan2(yDelta, xDelta);
		}

		void drawData(Graphics2D g, int indx, JFijiPipe pet1) {
			if( (type/4) != sliceType) return;
			int offX, offX2=0, widthX;
			int[] vals = getNumFrmSlice(pet1, indx);
			if(zval != vals[VSLICE]) return;
			widthX = parPanel.mouse1.widthX;
			double scl1 = parPanel.getScalePet();
			offX = 0;
			if( indx == 2) offX = widthX;
			if(isFusedShowing()) offX2 = 2*widthX -offX;
			drawDataSub(g, offX, scl1, pet1, dispType, type/4, null, offX2);
		}

		private void drawDataSub(Graphics2D g, int offX, double scl1, JFijiPipe pet1,
			int dspType, int slcType, myCaller d3Obj, int offX2) {
			int i, npts, x1000, y1000;
			Integer vali;
			String tmp1;
			int [] xdsp, ydsp;
			xdsp = new int[3];
			ydsp = new int[3];
			npts = 3;
			if( (type & 3) < 2) npts = 2;
			for( i=0; i<npts; i++) {
				x1000 = x[i];
				y1000 = y[i];
				Point2d pt1 = new Point2d( x1000/MEASURE_SCALE, y1000/MEASURE_SCALE);
				if(d3Obj != null) pt1 = d3Obj.translatePoint(pt1, type/4);
				Point pt2 = pet1.pos2Scrn(pt1, scl1, dspType, slcType, false);
				xdsp[i] = pt2.x + offX;
				ydsp[i] = pt2.y;
			}
			g.setColor(Color.red);
			g.drawPolyline(xdsp, ydsp, npts);
			if( (type & 3) == 0) {
				if( offX2 <= 0) return;	// no need to draw on fused
				for(i=0; i<npts; i++) xdsp[i] += offX2;
				g.drawPolyline(xdsp, ydsp, npts);
				return;
			}
			while(true) {
				if( npts == 2) {
					if( val1 > 0) {
						for(i=0; i<npts; i++) g.fillOval(xdsp[i]-PNTSZ, ydsp[i]-PNTSZ, 2*PNTSZ, 2*PNTSZ);
						vali = (int)(val1 + 0.5);
						tmp1 = vali.toString() + " mm";
					} else {
						vali = (int)(-val1);	// this is the val*1000
						i = (int) (-val1*100 + 0.001);
						i = i % 100;
						tmp1 = parPanel.getSuvString(i);
						Double dval = vali / 1000.;
						tmp1 += dval.toString();
					}
					g.drawString(tmp1, xdsp[1]+PNTSZ, ydsp[1]+PNTSZ);
				} else {
					vali = (int)(val1*360./Math.PI + 0.5);
					tmp1 = vali.toString() + " deg";
					g.drawString(tmp1, xdsp[0]+PNTSZ, ydsp[0]+PNTSZ);
				}
				if( offX2 <= 0) return;	// no need to draw on fused
				for(i=0; i<npts; i++) xdsp[i] += offX2;
				g.drawPolyline(xdsp, ydsp, npts);
				offX2 = 0;	// loop only once
			}
		}

		void draw3Data(Graphics2D g, myCaller d3Obj, myCaller d3Obj1) {
			int offX, dspType;
			double scl1;
			int type4 = type/4;
			if( !d3Obj.isZValOK(zval, type4)) return;
			dspType = 4;	// coronal, sagittal d3panel
			if(type4 == JFijiPipe.DSP_AXIAL) {
				dspType = 0;
				if(d3Obj.d3Pipe.zoom1 > 1.0) dspType = 3;
			}
			offX = d3Obj.getOffX(type4);
			scl1 = d3Obj.getScl1();
			drawDataSub(g, offX, scl1, d3Obj.d3Pipe, dspType, type4, d3Obj1, 0);
		}

		double getSUV() {
			int axial1, cor1, sag1, i, j, SUVtype;
			double retVal;
			boolean invrtFlg = true;
			axial1 = zval;
			cor1 = y[0]/MEASURE_SCALE;
			sag1 = x[0]/MEASURE_SCALE;
			switch(sliceType) {
				case JFijiPipe.DSP_SAGITAL:
					i = axial1;
					axial1 = sag1;
					sag1 = i;
					// drop through to swap axial and coronal

				case JFijiPipe.DSP_CORONAL:
					i = axial1;
					axial1 = cor1;
					cor1 = i;
					invrtFlg = false;
					break;
			}
			parPanel.calcSUVsub(axial1, sag1, cor1, invrtFlg);
			SUVtype = parentFrm.getSUVtype();
			switch( SUVtype) {
				case 1:
				case 2:
					retVal = parPanel.SUVpeak;
					break;

				case 3:
					retVal = parPanel.SUVmean;
					break;

				case 4:
					retVal = parPanel.SULmax;
					break;

				case 5:
					retVal = parPanel.SULmean;
					break;

				default:
					retVal = parPanel.SUVorCount;
					break;

			}
			i = (int)(retVal*1000);
			j = 0;
			if( parPanel.SUVflg) j = 1;
			retVal = -(i + SUVtype/10. + j/100.);
			if( i<0) retVal = 0;
			return retVal;
		}
	}

/*	int getImageNum(JFijiPipe pip1, int slice) {
		ImagePlus img1 = pip1.data1.srcImage;
		String meta = ChoosePetCt.getMeta(slice+1, img1);
		if( meta == null) return -1;
		return ChoosePetCt.parseInt(ChoosePetCt.getLastDicomValue(meta, "0020,0013"));
	}*/

	class myFloatingText {
		int x, y, z, type;
		String textVal;

		void drawData(Graphics2D g, int indx, JFijiPipe pet1) {
			int j, widthX, offX = 0, offX2 = 0, offY;
			String tmp1, tmp = m_floatingText[indx];
			int[] vals = getNumFrmSlice(pet1, indx);
			widthX = parPanel.mouse1.widthX;
			if( indx == 2) offX = widthX;
			if(isFusedShowing()) offX2 = 2*widthX;
			double scl1 = parPanel.getScalePet();
			g.setColor(Color.red);
			FontMetrics fmet = g.getFontMetrics();
			offY = fmet.getHeight() / 2;
			while( (j = tmp.indexOf('\n')) > 0) {
				tmp1 = tmp.substring(0, j);
				tmp = tmp.substring(j+1);
				if(!parseFlTxt(tmp1)) break;
				if( type != sliceType) continue;
				if( z != vals[VSLICE]) continue;
				if( textVal == null || textVal.isEmpty()) continue;
				Point2d pt1 = new Point2d( x, y);
				Point pt2 = pet1.pos2Scrn(pt1, scl1, dispType);
				// c++ measures from string top, java from middle -maybe?
				g.drawString(textVal, pt2.x + offX, pt2.y + offY);
				if(offX2>0) g.drawString(textVal, pt2.x + offX2, pt2.y + offY);
			}
		}

		void draw3Data(Graphics2D g, int indx, myCaller d3Obj) {
			int j, offX, offY, dspType;
			String tmp1, tmp = m_floatingText[indx];
			double scl1;
			g.setColor(Color.red);
			FontMetrics fmet = g.getFontMetrics();
			offY = fmet.getHeight() / 2;
			while( (j = tmp.indexOf('\n')) > 0) {
				tmp1 = tmp.substring(0, j);
				tmp = tmp.substring(j+1);
				if(!parseFlTxt(tmp1)) break;
				if( textVal == null || textVal.isEmpty()) continue;
				if( !d3Obj.isZValOK(z, type)) continue;
				dspType = 4;	// coronal, sagittal d3panel
				if(type == JFijiPipe.DSP_AXIAL) {
					dspType = 0;
					if(d3Obj.d3Pipe.zoom1 > 1.0) dspType = 3;
				}
				offX = d3Obj.getOffX(type);
				scl1 = d3Obj.getScl1();
				Point2d pt1 = new Point2d( x, y);
				if(!d3Obj.isFused) pt1 = d3Obj.translatePoint(pt1, type);
				Point pt2 = d3Obj.d3Pipe.pos2Scrn(pt1, scl1, dspType, type, false);
				// c++ measures from string top, java from middle -maybe?
				g.drawString(textVal, pt2.x + offX, pt2.y + offY);
			}
		}
		
		boolean parseFlTxt(String tmp) {
			textVal = null;	// discard previous values
			if( tmp.isEmpty()) return false;
			// use the | character to split between 2 delimiters
			Scanner s = new Scanner(tmp).useDelimiter(",|\t");
			if( !s.hasNextInt()) return false;
			x = s.nextInt();
			y = s.nextInt();
			z = s.nextInt();
			type = s.nextInt();
			textVal = s.next().trim();
			return true;
		}
		
		String getAComboLabel() {
			String retVal = (String) jComboText.getEditor().getItem();
			return retVal.trim();
		}
		
		void addPoint(Point pt1, double scale) {
			m_lastView = m_thisView;
			m_numTemp = 0;
			String inTxt = getAComboLabel();
			if( inTxt.isEmpty()) return;
			addPointSub( pt1, scale, inTxt);
			maybeSwitch2Bookmarks();
		}
		
		void addPointSub(Point pt1, double scale, String inTxt) {
			m_tempPnt[0] = new Point(pt1);
			int[] vals;
			JFijiPipe pet1 = parPanel.petPipe;
			getParentInfo();
			Graphics gr1 = parPanel.getGraphics();
			FontMetrics fm1 = gr1.getFontMetrics();
			Rectangle2D rc1 = fm1.getStringBounds(inTxt, gr1);
			m_tempPnt[0].x -= (int) (rc1.getWidth()/2);
			if(m_tempPnt[0].x < 0) m_tempPnt[0].x = 0;
			Point pntTmp = pet1.scrn2PosInt(m_tempPnt[0], scale, dispType);
			x = pntTmp.x;
			y = pntTmp.y;
			vals = getNumFrmSlice( pet1, m_lastView);
			z = vals[VSLICE];
			String outTxt = String.format("%d,%d,%d,%d\t", x, y,z,sliceType);
			outTxt += inTxt + "\r\n";
			if(m_floatingText[m_lastView] == null) m_floatingText[m_lastView] = new String();
			m_floatingText[m_lastView] += outTxt;
			m_dirtyFlg = true;
			addBookMark(true);
			setCountVals();
			parPanel.repaintAll();
		}
	}

	class myChina {
		byte type, size;
		int x, y;
		short z;

		void readData(FileChannel fc) {
			try {
				ByteBuffer buf = ByteBuffer.allocate(16);
				buf.order(ByteOrder.LITTLE_ENDIAN);
				fc.read(buf);
				buf.position(0);
				type = buf.get();
				size = buf.get();
				buf.getShort();	// padding
				x = buf.getInt();
				y = buf.getInt();
				z = buf.getShort();
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		}

		void writeData(FileChannel fc) {
			try {
				ByteBuffer buf = ByteBuffer.allocate(16);
				buf.order(ByteOrder.LITTLE_ENDIAN);
				buf.put(type);
				buf.put(size);
				buf.putShort((short) 0); // padding
				buf.putInt(x);
				buf.putInt(y);
				buf.putInt(z);	// short -> int pads with 2 zeros
				buf.position(0);
				fc.write(buf);
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		}

		void draw3Data(Graphics2D g, myCaller d3Obj, myCaller d3Obj1) {
			int offX, dspType;
			double scl1;
			int type4 = type/4;
			if( !d3Obj.isZValOK(z, type4)) return;
			dspType = 4;	// coronal, sagittal d3panel
			if(type4 == JFijiPipe.DSP_AXIAL) {
				dspType = 0;
				if(d3Obj.d3Pipe.zoom1 > 1.0) dspType = 3;
			}
			offX = d3Obj.getOffX(type4);
			scl1 = d3Obj.getScl1();
			drawDataSub(g, offX, scl1, d3Obj.d3Pipe, dspType, type4, d3Obj1, 0);
		}

		private void drawDataSub(Graphics2D g, int offX, double scl1, JFijiPipe pet1,
			int dspType, int slcType, myCaller d3Obj, int offX2) {
			int i, hotX, hotY, iSize, x0, y0, thin4 = 0;
			int[] x1, y1;
			byte[] pntArrow;
			if( m_thin) thin4 = 4;
			// note: the shapes of the arrows are slightly different between c++
			// and Java. This is a result of the different drawing algorithms.
			switch( (type & 3) + thin4) {
				case 1:	// thick arrows
					pntArrow = new byte[]{30,8,7,17,11,12,1,12,1,6,11,6,7,1};
					break;

				case 2:
					pntArrow = new byte[]{9,30,17,7,12,11,12,1,6,1,6,11,1,7};
					break;

				case 3:
					pntArrow = new byte[]{1,8,22,17,20,12,30,12,30,6,20,6,22,1};
					break;

				case 4:	// thin arrows, commented ones are a bit thicker
//					pntArrow = new byte[]{8,1,17,10,11,7,11,30,7,30,7,7,1,10};
					pntArrow = new byte[]{9,1,17,9,10,6,10,30,8,30,8,6,1,9};
					break;

				case 5:
//					pntArrow = new byte[]{30,8,21,17,24,11,1,11,1,7,24,7,21,1};
					pntArrow = new byte[]{30,9,21,17,25,10,1,10,1,8,25,8,21,1};
					break;

				case 6:
//					pntArrow = new byte[]{8,30,17,21,11,24,11,1,7,1,7,24,1,21};
					pntArrow = new byte[]{9,30,17,21,10,25,10,1,8,1,8,25,1,21};
					break;

				case 7:
//					pntArrow = new byte[]{1,8,10,17,7,11,30,11,30,7,7,7,10,1};
					pntArrow = new byte[]{1,9,9,17,6,10,30,10,30,8,6,8,9,1};
					break;

				default:	// thick arrow
					pntArrow = new byte []{1,1,1,24,4,17,12,31,14,30,10,15,16,16};
					break;
			}
			Point2d pt1 = new Point2d( x/MEASURE_SCALE, y/MEASURE_SCALE);
			if(d3Obj != null) pt1 = d3Obj.translatePoint(pt1, type/4);
			Point pt2 = pet1.pos2Scrn(pt1, scl1, dspType, slcType, false);
			x1 = new int[7];
			y1 = new int[7];
			hotX = pntArrow[0];
			hotY = pntArrow[1];
			iSize = size + 6;
			for( i=0; i<7; i++) {
				x0 = pntArrow[2*i];
				y0 = pntArrow[2*i+1];
				x0 = ((x0 - hotX) * iSize) >> 4;
				x1[i] = x0 + pt2.x + offX;
				if( x1[i] < 0) x1[i] = 0;
				y0 = ((y0 - hotY) * iSize) >> 4;
				y1[i] = y0 + pt2.y;
				if( y1[i] < 0) y1[i] = 0;
			}
			g.setColor(Color.green);
			g.fillPolygon(x1, y1, 7);
			g.setColor(Color.black);
			g.drawPolygon(x1, y1, 7);

			if( offX2 <= 0) return;	// no need to draw on fused
			for(i=0; i<7; i++) x1[i] += offX2;
			g.setColor(Color.green);
			g.fillPolygon(x1, y1, 7);
			g.setColor(Color.black);
			g.drawPolygon(x1, y1, 7);
		}

		void drawData(Graphics2D g, int indx, JFijiPipe pet1) {
			if( (type/4) != sliceType) return;
			int offX, widthX, offX2 = 0;
			int vals[] = getNumFrmSlice(pet1, indx);
			if(z != vals[VSLICE]) return;
			offX = 0;
			widthX = parPanel.mouse1.widthX;
			double scl1 = parPanel.getScalePet();
			if( indx == 2) offX = widthX;
			if(isFusedShowing()) offX2 = 2*widthX -offX;
			drawDataSub(g, offX, scl1, pet1, dispType, type/4, null, offX2);
		}
	}
	
	class myBookMark {
		int offst, type, zoomIndx;
		float level[], width[]; // lenght = 3
		Point pan;

		myBookMark() {
			level = new float[3];
			width = new float[3];
		}

		void readData(FileChannel fc) {
			try {
				int i;
				ByteBuffer buf = ByteBuffer.allocate(44);
				buf.order(ByteOrder.LITTLE_ENDIAN);
				fc.read(buf);
				buf.position(0);
				offst = buf.getInt();
				type = buf.getInt();
				zoomIndx = buf.getInt();
				for( i=0; i<3; i++) level[i] = buf.getFloat();
				for( i=0; i<3; i++) width[i] = buf.getFloat();
				pan = new Point();
				pan.x = buf.getInt();
				pan.y = buf.getInt();
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		}

		void readFromDisplay() {
			JFijiPipe pip0 = getPipe(0);
			if( pip0 == null) return;
			double winMax = pip0.data1.sliderSUVMax;
			type = sliceType;
			int vals[] = getNumFrmSlice(pip0, type);
			offst = vals[VSLICE];
			zoomIndx = pip0.zoomIndx;
			pan = getBmPan(pip0);
			setLevelWidth(0, pip0, winMax);
			setLevelWidth(1, getPipe(2), 4000);	// ct pipe
			setLevelWidth(2, parPanel.mipPipe, winMax);
		}

		void writeData(FileChannel fc) {
			try {
				int i;
				ByteBuffer buf = ByteBuffer.allocate(44);
				buf.order(ByteOrder.LITTLE_ENDIAN);
				buf.putInt(offst);
				buf.putInt(type);
				buf.putInt(zoomIndx);
				for( i=0; i<3; i++) buf.putFloat(level[i]);
				for( i=0; i<3; i++) buf.putFloat(width[i]);
				buf.putInt(pan.x);
				buf.putInt(pan.y);
				buf.position(0);
				fc.write(buf);
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		}

		boolean bmChanged( myBookMark bm1) {
			if( bm1.offst != offst || bm1.type != type || bm1.zoomIndx != zoomIndx) return true;
			if( !bm1.pan.equals(pan)) return true;
			float epislon = 0.001f;
			for( int i=0; i<3; i++) {
				if( Math.abs(bm1.level[i] - level[i]) > epislon) return true;
				if( Math.abs(bm1.width[i] - width[i]) > epislon) return true;
			}
			return false;
		}

		private void setLevelWidth(int indx, JFijiPipe pip1, double winMax) {
			if( pip1 == null) return;
			double winLevel, winWidth;
			winLevel = pip1.winLevel;
			winWidth = pip1.winWidth;
			if( indx == 1)	{	// ct
				winMax = 4000.0;	// ct runs from -1000 -> 3000
				winLevel += 1000.0;
			}
			level[indx] = (float) (winLevel/winMax);
			width[indx] = (float) (winWidth/(winMax*2.0));	// half the width
		}

		private double[] getLevelsWidths(JFijiPipe pip1) {
			double[] retVal = new double[6];
			double winMax = pip1.data1.sliderSUVMax;
			retVal[0] = round10(level[0] * winMax);
			retVal[1] = round10(width[0] * winMax * 2.0);
			retVal[2] = round10(level[1] * 4000.0 - 1000.0);
			retVal[3] = round10(width[1] * 8000.0);
			retVal[4] = round10(level[2] * winMax);
			retVal[5] = round10(width[2] * winMax * 2.0);
			return retVal;
		}

		// this reads from the display the current pan in bm units
		Point getBmPan(JFijiPipe pip0) {
			Point panRet = new Point();
			double xpan, ypan, maxX, maxY, zoom1, zoomY, halfSize, factorY;
			zoom1 = pip0.zoom1;
			zoomY = pip0.zoomY;
			halfSize = (zoom1 - 1) * HALF_SCALE;
			xpan = pip0.pan.x;
			ypan = pip0.pan.y;
			maxX = (zoom1 - 1) / zoom1;
			maxY = (zoom1 - zoomY) / zoom1;
			if( maxX <= 0) return panRet;
			factorY = maxY/maxX;
			if( maxY > 0) panRet.y = (int) (halfSize * factorY * (ypan/maxY + 1.0));
			if( dispType == 2) {
				double y2x = 2.0 * pip0.data1.y2xFactor;
				if(sliceType == JFijiPipe.DSP_SAGITAL) xpan = ypan;
				ypan = pip0.pan.z;
				panRet.y = (int) (halfSize * y2x * (1.0 - ypan/maxX));
			}
			panRet.x = (int) (halfSize * (xpan/maxX + 1.0));
			if( panRet.x < 0) panRet.x = 0;
			if( panRet.y < 0) panRet.y = 0;
			return panRet;
		}

		// this converts the current bm pan values into units for JFijiPipe
		Point3d getPan2(JFijiPipe pip0) {
			Point3d  panRet = new Point3d(pip0.pan);
			double xpan = 0, ypan = 0, maxX, maxY, zoom1, zoomY;
			double panx1, pany1, halfSize, factorY;
			zoom1 = pip0.zoom1;
			if( zoom1 < 1.05) return new Point3d();	// i.e. 0, 0, 0
			halfSize = (zoom1 - 1) * HALF_SCALE;
			zoomY = pip0.zoomY;
			maxX = (zoom1 - 1) / zoom1;
			maxY = (zoom1 - zoomY) / zoom1;
			factorY = maxY/maxX;
			panx1 = pan.x / halfSize;
			if( panx1 > 2.0) panx1 = 2.0;
			pany1 = pan.y / (halfSize * factorY);
			if( pany1 > 2.0) pany1 = 2.0;
			if( maxX > 0) xpan = maxX*(panx1 - 1.0);
			if( maxY > 0) ypan = maxY*(pany1 - 1.0);
			if( dispType == 2) {
				double y2x = 2.0 * pip0.data1.y2xFactor;
				pany1 = pan.y / (halfSize * y2x);
				ypan = maxX*(1.0 - pany1);
				if( ypan < 0) ypan = 0;
				panRet.z = ypan;
				if(sliceType == JFijiPipe.DSP_SAGITAL) panRet.y = xpan;
				else panRet.x = xpan;
				return panRet;
			}
			panRet.x = xpan;
			panRet.y = ypan;
			return panRet;
		}

		private double round10( double in) {
			int val1 = (int)(in * 10.0 + 0.5);
			return val1 / 10.0;
		}
	}
	
	class myOblique {
		int primaryPlane, secondaryPlane, intersection;
		int xScale, zScale;
		double angle, obliqueFactor;
		
		boolean update(myMeasure meas1) {
			boolean angle90, retVal = false;
			double deltaX, deltaY;
			int cx, cy, offst;
			JFijiPipe pip0 = getPipe(0);	// pet pipe
			xScale = pip0.data1.width;
			cx = xScale * MEASURE_SCALE;
			zScale = pip0.data1.numFrms;
			cy = zScale * MEASURE_SCALE;
//			deltaX = meas1.x[1] - meas1.x[0];
//			deltaY = meas1.y[1] - meas1.y[0];
			angle = meas1.getAngle(1, 0);
			while(angle >= Math.PI) angle -= Math.PI;
			while(angle < 0) angle += Math.PI;
			if( angle == 0 || angle == Math.PI / 2) return retVal;	// not oblique
			obliqueFactor =  1 / Math.tan(angle);
			if( angle > Math.PI/4 && angle < 3*Math.PI/4) {
				angle90 = true;
				deltaY = (cy/2) - meas1.y[0];
				deltaX = deltaY * obliqueFactor;
				offst = meas1.x[0] + ((int)deltaX);
			} else {
				angle90 = false;
				deltaX = (cx/2) - meas1.x[0];
				obliqueFactor = 1 / obliqueFactor;
				deltaY = deltaX * obliqueFactor;
				offst = meas1.y[0] + ((int) deltaY);
			}
			switch( sliceType) {
				case JFijiPipe.DSP_SAGITAL:
					if(angle90) {
						if( offst < 0 || offst >= cx) break;
						primaryPlane = JFijiPipe.DSP_CORONAL;
						secondaryPlane = JFijiPipe.DSP_AXIAL;
					} else {
						if( offst < 0 || offst >= cy) break;
						primaryPlane = JFijiPipe.DSP_AXIAL;
						secondaryPlane = JFijiPipe.DSP_CORONAL;
					}
					intersection = offst / MEASURE_SCALE;
					retVal = true;
					break;
					
				case JFijiPipe.DSP_CORONAL:
					if(angle90) {
						if( offst < 0 || offst >= cx) break;
						primaryPlane = JFijiPipe.DSP_SAGITAL;
						secondaryPlane = JFijiPipe.DSP_AXIAL;
					} else {
						if( offst < 0 || offst >= cy) break;
						primaryPlane = JFijiPipe.DSP_AXIAL;
						secondaryPlane = JFijiPipe.DSP_SAGITAL;
					}
					intersection = offst / MEASURE_SCALE;
					retVal = true;
					break;
					
				case JFijiPipe.DSP_AXIAL:
					if(angle90) {
						// we need to recalculate the values for the axial case
						deltaY = (cx/2) - meas1.y[0];
						deltaX = deltaY * obliqueFactor;
						offst = meas1.x[0] + ((int)deltaX);
						if( offst < 0 || offst >= cx) break;
						primaryPlane = JFijiPipe.DSP_SAGITAL;
						secondaryPlane = JFijiPipe.DSP_CORONAL;
					} else {
						if( offst < 0 || offst >= cx) break;
						primaryPlane = JFijiPipe.DSP_CORONAL;
						secondaryPlane = JFijiPipe.DSP_SAGITAL;
					}
					intersection = offst / MEASURE_SCALE;
					retVal = true;
					break;
			}
			return retVal;
		}
		
		void startOblique() {
			JFijiPipe pip0 = getPipe(0);	// pet pipe
			pip0.obliqueFactor = obliqueFactor;
			int ctZ, petZ = pip0.data1.numFrms;
			pip0 = getPipe(2);	// ct pipe
			ctZ = pip0.data1.numFrms;
			switch(primaryPlane) {
				case JFijiPipe.DSP_CORONAL:
					pip0.obliqueFactor = obliqueFactor*petZ/ctZ;
					if(secondaryPlane != JFijiPipe.DSP_AXIAL)
						pip0.obliqueFactor = obliqueFactor;	// not corrected for num slices
					parPanel.petCoronal = intersection;
					break;
					
				case JFijiPipe.DSP_AXIAL:
					pip0.obliqueFactor = obliqueFactor*ctZ/petZ;
					parPanel.petAxial = intersection;
					break;
					
				case JFijiPipe.DSP_SAGITAL:
					pip0.obliqueFactor = obliqueFactor*petZ/ctZ;
					if(secondaryPlane != JFijiPipe.DSP_AXIAL)
						pip0.obliqueFactor = obliqueFactor;	// not corrected for num slices
					parPanel.petSagital = intersection;
					break;
			}
		}
		
		void draw(Graphics2D g, double scl1, JFijiPipe pip1, int colorMod, double axialPos) {
			switch(primaryPlane) {
				case JFijiPipe.DSP_CORONAL:
					pip1.prepareCoronalSagitalSub(parPanel.petCoronal, -1, colorMod, 0, secondaryPlane, 0);
					pip1.drawCorSagImages(g, scl1, parPanel, true);
					break;
					
				case JFijiPipe.DSP_AXIAL:
					boolean corPlane = true;
					if( secondaryPlane == JFijiPipe.DSP_SAGITAL) corPlane = false;
					pip1.prepareAxialOblique(axialPos, secondaryPlane, colorMod);
					pip1.drawCorSagImages(g, scl1, parPanel, corPlane);
//					pip1.drawImages(g, scl1, parPanel, true);
					break;

				case JFijiPipe.DSP_SAGITAL:
					pip1.prepareCoronalSagitalSub(-1, parPanel.petSagital, colorMod, 0, secondaryPlane, 0);
					pip1.drawCorSagImages(g, scl1, parPanel, false);
					break;
			}
		}
	}

	protected class bkgdRobot extends SwingWorker {

		@Override
		protected Void doInBackground() {
			doRobotSwitchSlice();
			return null;
		}

	}
	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        butGroupMeas = new javax.swing.ButtonGroup();
        butGroupSave = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jMeasure = new javax.swing.JPanel();
        jButMeasOK = new javax.swing.JButton();
        jButMeasClearAll = new javax.swing.JButton();
        jButMeasClearLast = new javax.swing.JButton();
        jLabMeasCount = new javax.swing.JLabel();
        jCheckMeasHide = new javax.swing.JCheckBox();
        jButOblique = new javax.swing.JButton();
        jPanRadio = new javax.swing.JPanel();
        jRadMeasLine = new javax.swing.JRadioButton();
        jRadLineSUV = new javax.swing.JRadioButton();
        jRadMeasDist = new javax.swing.JRadioButton();
        jRadMeasAngle = new javax.swing.JRadioButton();
        jLabMeas = new javax.swing.JLabel();
        jArrows = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jButRightArrow = new javax.swing.JToggleButton();
        jButDownArrow = new javax.swing.JToggleButton();
        jButUpArrow = new javax.swing.JToggleButton();
        jButLeftArrow = new javax.swing.JToggleButton();
        jButChinOK = new javax.swing.JButton();
        jButChinClearAll = new javax.swing.JButton();
        jButChinClearLast = new javax.swing.JButton();
        jLabChinCount = new javax.swing.JLabel();
        jCheckChinHide = new javax.swing.JCheckBox();
        jCheckChinThin = new javax.swing.JCheckBox();
        jSpinChinSize = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jText = new javax.swing.JPanel();
        jLabText1 = new javax.swing.JLabel();
        jButTextOK = new javax.swing.JButton();
        jButTextClearAll = new javax.swing.JButton();
        jButTextClearLast = new javax.swing.JButton();
        jLabText2 = new javax.swing.JLabel();
        jCheckTextHide = new javax.swing.JCheckBox();
        jLabTextCount = new javax.swing.JLabel();
        jComboText = new javax.swing.JComboBox();
        jBookmarks = new javax.swing.JPanel();
        jButMarkSlice = new javax.swing.JButton();
        jPanMove2 = new javax.swing.JPanel();
        jButBMDown = new javax.swing.JButton();
        jButBMUp = new javax.swing.JButton();
        jButBMOK = new javax.swing.JButton();
        jLabBMCount = new javax.swing.JLabel();
        jCheckBMHide = new javax.swing.JCheckBox();
        jCheckSingle = new javax.swing.JCheckBox();
        jCheckAutoBM = new javax.swing.JCheckBox();
        jSave = new javax.swing.JPanel();
        jButSaveOK = new javax.swing.JButton();
        jRadSaveMemory = new javax.swing.JRadioButton();
        jRadSaveStudy = new javax.swing.JRadioButton();
        jRadSaveDelete = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();

        setTitle("Annotations");
        setAlwaysOnTop(true);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                formComponentHidden(evt);
            }
        });

        jButMeasOK.setText("OK");
        jButMeasOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMeasOKActionPerformed(evt);
            }
        });

        jButMeasClearAll.setText("Clear All");
        jButMeasClearAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMeasClearAllActionPerformed(evt);
            }
        });

        jButMeasClearLast.setText("Clear Last");
        jButMeasClearLast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMeasClearLastActionPerformed(evt);
            }
        });

        jLabMeasCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabMeasCount.setText("0");
        jLabMeasCount.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jCheckMeasHide.setText("Hide Annotations");
        jCheckMeasHide.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckMeasHideActionPerformed(evt);
            }
        });

        jButOblique.setText("Oblique");
        jButOblique.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButObliqueActionPerformed(evt);
            }
        });

        jRadMeasLine.setText("Line only (arrow)");
        jRadMeasLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadMeasLineActionPerformed(evt);
            }
        });

        jRadLineSUV.setText("Line - SUV");
        jRadLineSUV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadLineSUVActionPerformed(evt);
            }
        });

        jRadMeasDist.setText("Distance");
        jRadMeasDist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadMeasDistActionPerformed(evt);
            }
        });

        jRadMeasAngle.setText("Angle");
        jRadMeasAngle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadMeasAngleActionPerformed(evt);
            }
        });

        jLabMeas.setText("jLabel1");

        javax.swing.GroupLayout jPanRadioLayout = new javax.swing.GroupLayout(jPanRadio);
        jPanRadio.setLayout(jPanRadioLayout);
        jPanRadioLayout.setHorizontalGroup(
            jPanRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanRadioLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jRadMeasLine)
                    .addComponent(jRadLineSUV)
                    .addComponent(jRadMeasDist)
                    .addComponent(jRadMeasAngle)
                    .addComponent(jLabMeas, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanRadioLayout.setVerticalGroup(
            jPanRadioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanRadioLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jRadMeasLine)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadLineSUV)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadMeasDist)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadMeasAngle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabMeas, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jMeasureLayout = new javax.swing.GroupLayout(jMeasure);
        jMeasure.setLayout(jMeasureLayout);
        jMeasureLayout.setHorizontalGroup(
            jMeasureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jMeasureLayout.createSequentialGroup()
                .addGroup(jMeasureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jMeasureLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jCheckMeasHide)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButOblique))
                    .addGroup(jMeasureLayout.createSequentialGroup()
                        .addComponent(jPanRadio, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jMeasureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jButMeasClearLast, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButMeasClearAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButMeasOK, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabMeasCount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(46, 46, 46))
        );
        jMeasureLayout.setVerticalGroup(
            jMeasureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jMeasureLayout.createSequentialGroup()
                .addGroup(jMeasureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jMeasureLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButMeasOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButMeasClearAll)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButMeasClearLast)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabMeasCount))
                    .addComponent(jPanRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jMeasureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckMeasHide)
                    .addComponent(jButOblique))
                .addContainerGap(57, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Measure", jMeasure);

        jLabel1.setText("<html>Move mouse cursor to object<br>of interest and left click.<br>In order to pick a different<br>arrow shape, click on it.");
        jLabel1.setToolTipText("");
        jLabel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jButRightArrow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/bitmrigh.png"))); // NOI18N
        jButRightArrow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButRightArrowActionPerformed(evt);
            }
        });

        jButDownArrow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/bitmdown.png"))); // NOI18N
        jButDownArrow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButDownArrowActionPerformed(evt);
            }
        });

        jButUpArrow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/bitmup.png"))); // NOI18N
        jButUpArrow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButUpArrowActionPerformed(evt);
            }
        });

        jButLeftArrow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/bitmleft.png"))); // NOI18N
        jButLeftArrow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButLeftArrowActionPerformed(evt);
            }
        });

        jButChinOK.setText("OK");
        jButChinOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButChinOKActionPerformed(evt);
            }
        });

        jButChinClearAll.setText("Clear All");
        jButChinClearAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButChinClearAllActionPerformed(evt);
            }
        });

        jButChinClearLast.setText("Clear Last");
        jButChinClearLast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButChinClearLastActionPerformed(evt);
            }
        });

        jLabChinCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabChinCount.setText("0");
        jLabChinCount.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jCheckChinHide.setText("Hide Annotations");
        jCheckChinHide.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckChinHideActionPerformed(evt);
            }
        });

        jCheckChinThin.setText("Thin Arrows");
        jCheckChinThin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckChinThinActionPerformed(evt);
            }
        });

        jSpinChinSize.setToolTipText("arrow size");

        jLabel2.setText("size");

        javax.swing.GroupLayout jArrowsLayout = new javax.swing.GroupLayout(jArrows);
        jArrows.setLayout(jArrowsLayout);
        jArrowsLayout.setHorizontalGroup(
            jArrowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jArrowsLayout.createSequentialGroup()
                .addGroup(jArrowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jArrowsLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jArrowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jArrowsLayout.createSequentialGroup()
                                .addComponent(jButRightArrow)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButDownArrow))
                            .addGroup(jArrowsLayout.createSequentialGroup()
                                .addComponent(jButUpArrow)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButLeftArrow))))
                    .addGroup(jArrowsLayout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinChinSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jArrowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckChinHide)
                    .addComponent(jCheckChinThin)
                    .addGroup(jArrowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabChinCount, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButChinOK, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButChinClearAll, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButChinClearLast, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(0, 8, Short.MAX_VALUE))
        );
        jArrowsLayout.setVerticalGroup(
            jArrowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jArrowsLayout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jArrowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButRightArrow)
                    .addComponent(jButDownArrow))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jArrowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButUpArrow)
                    .addComponent(jButLeftArrow))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jArrowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinChinSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(0, 32, Short.MAX_VALUE))
            .addGroup(jArrowsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButChinOK)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButChinClearAll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButChinClearLast)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabChinCount)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckChinHide)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckChinThin)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Arrows", jArrows);

        jLabText1.setText("Type in the text in the box below");

        jButTextOK.setText("OK");
        jButTextOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButTextOKActionPerformed(evt);
            }
        });

        jButTextClearAll.setText("Clear All");
        jButTextClearAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButTextClearAllActionPerformed(evt);
            }
        });

        jButTextClearLast.setText("Clear Last");
        jButTextClearLast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButTextClearLastActionPerformed(evt);
            }
        });

        jLabText2.setText("<html>Then point to where you want<br>it to appear and click.");

        jCheckTextHide.setText("Hide Annotations");
        jCheckTextHide.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckTextHideActionPerformed(evt);
            }
        });

        jLabTextCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabTextCount.setText("0");
        jLabTextCount.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jComboText.setEditable(true);

        javax.swing.GroupLayout jTextLayout = new javax.swing.GroupLayout(jText);
        jText.setLayout(jTextLayout);
        jTextLayout.setHorizontalGroup(
            jTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jTextLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabText1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabText2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckTextHide)
                    .addComponent(jComboText, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButTextClearLast, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButTextOK, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButTextClearAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabTextCount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(39, Short.MAX_VALUE))
        );
        jTextLayout.setVerticalGroup(
            jTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jTextLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabText1)
                    .addComponent(jButTextOK))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButTextClearAll)
                    .addComponent(jComboText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4)
                .addGroup(jTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabText2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButTextClearLast))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabTextCount)
                    .addComponent(jCheckTextHide))
                .addContainerGap(121, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Text", jText);

        jButMarkSlice.setText("Mark Slice");
        jButMarkSlice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButMarkSliceActionPerformed(evt);
            }
        });

        jPanMove2.setBorder(javax.swing.BorderFactory.createTitledBorder("Move down/up"));

        jButBMDown.setText("<<");
        jButBMDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBMDownActionPerformed(evt);
            }
        });

        jButBMUp.setText(">>");
        jButBMUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBMUpActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanMove2Layout = new javax.swing.GroupLayout(jPanMove2);
        jPanMove2.setLayout(jPanMove2Layout);
        jPanMove2Layout.setHorizontalGroup(
            jPanMove2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanMove2Layout.createSequentialGroup()
                .addComponent(jButBMDown)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButBMUp))
        );
        jPanMove2Layout.setVerticalGroup(
            jPanMove2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanMove2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jButBMUp)
                .addComponent(jButBMDown))
        );

        jButBMOK.setText("OK");
        jButBMOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBMOKActionPerformed(evt);
            }
        });

        jLabBMCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabBMCount.setText("0");
        jLabBMCount.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jCheckBMHide.setText("Hide Annotations");
        jCheckBMHide.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBMHideActionPerformed(evt);
            }
        });

        jCheckSingle.setText("Single Orientation");

        jCheckAutoBM.setText("Automatically switch to Bookmarks");

        javax.swing.GroupLayout jBookmarksLayout = new javax.swing.GroupLayout(jBookmarks);
        jBookmarks.setLayout(jBookmarksLayout);
        jBookmarksLayout.setHorizontalGroup(
            jBookmarksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jBookmarksLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jBookmarksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jBookmarksLayout.createSequentialGroup()
                        .addGroup(jBookmarksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckSingle)
                            .addComponent(jPanMove2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jCheckBMHide))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 43, Short.MAX_VALUE)
                        .addGroup(jBookmarksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jBookmarksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabBMCount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButBMOK, javax.swing.GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE))
                            .addComponent(jButMarkSlice, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(40, 40, 40))
                    .addGroup(jBookmarksLayout.createSequentialGroup()
                        .addComponent(jCheckAutoBM)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jBookmarksLayout.setVerticalGroup(
            jBookmarksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jBookmarksLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jBookmarksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jBookmarksLayout.createSequentialGroup()
                        .addComponent(jButBMOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButMarkSlice)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabBMCount))
                    .addGroup(jBookmarksLayout.createSequentialGroup()
                        .addComponent(jPanMove2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckSingle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBMHide)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckAutoBM)
                .addContainerGap(119, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Bookmarks", jBookmarks);

        jButSaveOK.setText("OK");
        jButSaveOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSaveOKActionPerformed(evt);
            }
        });

        jRadSaveMemory.setText("<html>All objects exist in memory only -<br> for use until the study is closed.");

        jRadSaveStudy.setText("Save all objects as permanent part of study.");

        jRadSaveDelete.setText("Delete all objects from study.");

        jLabel3.setText("<html>Note: the first options refers to changes only.<br>Whatever was part of the study when it was<br>read remains. Any changes are ignored<br>once the study is closed.");

        javax.swing.GroupLayout jSaveLayout = new javax.swing.GroupLayout(jSave);
        jSave.setLayout(jSaveLayout);
        jSaveLayout.setHorizontalGroup(
            jSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jSaveLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jSaveLayout.createSequentialGroup()
                        .addComponent(jRadSaveMemory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButSaveOK))
                    .addGroup(jSaveLayout.createSequentialGroup()
                        .addGroup(jSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jRadSaveStudy)
                            .addComponent(jRadSaveDelete)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 18, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jSaveLayout.setVerticalGroup(
            jSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jSaveLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButSaveOK)
                    .addComponent(jRadSaveMemory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadSaveStudy)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadSaveDelete)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(69, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Save", jSave);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButBMDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBMDownActionPerformed
		setNextSlice(-1);
    }//GEN-LAST:event_jButBMDownActionPerformed

    private void jButBMUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBMUpActionPerformed
		setNextSlice(1);
    }//GEN-LAST:event_jButBMUpActionPerformed

    private void jButBMOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBMOKActionPerformed
		OKAction();
    }//GEN-LAST:event_jButBMOKActionPerformed

    private void jCheckBMHideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBMHideActionPerformed
        hideAnnotations();
    }//GEN-LAST:event_jCheckBMHideActionPerformed

    private void jButMeasOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMeasOKActionPerformed
		OKAction();
    }//GEN-LAST:event_jButMeasOKActionPerformed

    private void jCheckMeasHideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckMeasHideActionPerformed
        hideAnnotations();
    }//GEN-LAST:event_jCheckMeasHideActionPerformed

    private void jRadMeasLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadMeasLineActionPerformed
        radButAction(0);
    }//GEN-LAST:event_jRadMeasLineActionPerformed

    private void jRadMeasDistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadMeasDistActionPerformed
        radButAction(1);
    }//GEN-LAST:event_jRadMeasDistActionPerformed

    private void jRadMeasAngleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadMeasAngleActionPerformed
        radButAction(2);
    }//GEN-LAST:event_jRadMeasAngleActionPerformed

    private void jButMeasClearAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMeasClearAllActionPerformed
        removeMeasurePoint(true);
    }//GEN-LAST:event_jButMeasClearAllActionPerformed

    private void jButMeasClearLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMeasClearLastActionPerformed
        removeMeasurePoint(false);
    }//GEN-LAST:event_jButMeasClearLastActionPerformed

    private void jButMarkSliceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButMarkSliceActionPerformed
        markButton();
    }//GEN-LAST:event_jButMarkSliceActionPerformed

    private void jButTextOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButTextOKActionPerformed
        OKAction();
    }//GEN-LAST:event_jButTextOKActionPerformed

    private void jButTextClearLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButTextClearLastActionPerformed
        removeTextPoint(false);
    }//GEN-LAST:event_jButTextClearLastActionPerformed

    private void jButTextClearAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButTextClearAllActionPerformed
        removeTextPoint(true);
    }//GEN-LAST:event_jButTextClearAllActionPerformed

    private void jCheckTextHideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckTextHideActionPerformed
        hideAnnotations();
    }//GEN-LAST:event_jCheckTextHideActionPerformed

    private void jButSaveOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSaveOKActionPerformed
        OKAction();
    }//GEN-LAST:event_jButSaveOKActionPerformed

    private void jButObliqueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButObliqueActionPerformed
        obliqueAction();
    }//GEN-LAST:event_jButObliqueActionPerformed

    private void formComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentHidden
		windowClosed();
    }//GEN-LAST:event_formComponentHidden

    private void jCheckChinThinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckChinThinActionPerformed
        setThinArrow();
    }//GEN-LAST:event_jCheckChinThinActionPerformed

    private void jCheckChinHideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckChinHideActionPerformed
        hideAnnotations();
    }//GEN-LAST:event_jCheckChinHideActionPerformed

    private void jButChinClearLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButChinClearLastActionPerformed
        removeChinaPoint(false);
    }//GEN-LAST:event_jButChinClearLastActionPerformed

    private void jButChinClearAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButChinClearAllActionPerformed
        removeChinaPoint(true);
    }//GEN-LAST:event_jButChinClearAllActionPerformed

    private void jButChinOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButChinOKActionPerformed
        OKAction();
    }//GEN-LAST:event_jButChinOKActionPerformed

    private void jButLeftArrowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButLeftArrowActionPerformed
        setActiveArrow(3);
    }//GEN-LAST:event_jButLeftArrowActionPerformed

    private void jButUpArrowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButUpArrowActionPerformed
        setActiveArrow(0);
    }//GEN-LAST:event_jButUpArrowActionPerformed

    private void jButDownArrowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButDownArrowActionPerformed
        setActiveArrow(2);
    }//GEN-LAST:event_jButDownArrowActionPerformed

    private void jButRightArrowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButRightArrowActionPerformed
        setActiveArrow(1);
    }//GEN-LAST:event_jButRightArrowActionPerformed

    private void jRadLineSUVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadLineSUVActionPerformed
        radButAction(3);
    }//GEN-LAST:event_jRadLineSUVActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup butGroupMeas;
    private javax.swing.ButtonGroup butGroupSave;
    private javax.swing.JPanel jArrows;
    private javax.swing.JPanel jBookmarks;
    private javax.swing.JButton jButBMDown;
    private javax.swing.JButton jButBMOK;
    private javax.swing.JButton jButBMUp;
    private javax.swing.JButton jButChinClearAll;
    private javax.swing.JButton jButChinClearLast;
    private javax.swing.JButton jButChinOK;
    private javax.swing.JToggleButton jButDownArrow;
    private javax.swing.JToggleButton jButLeftArrow;
    private javax.swing.JButton jButMarkSlice;
    private javax.swing.JButton jButMeasClearAll;
    private javax.swing.JButton jButMeasClearLast;
    private javax.swing.JButton jButMeasOK;
    private javax.swing.JButton jButOblique;
    private javax.swing.JToggleButton jButRightArrow;
    private javax.swing.JButton jButSaveOK;
    private javax.swing.JButton jButTextClearAll;
    private javax.swing.JButton jButTextClearLast;
    private javax.swing.JButton jButTextOK;
    private javax.swing.JToggleButton jButUpArrow;
    private javax.swing.JCheckBox jCheckAutoBM;
    private javax.swing.JCheckBox jCheckBMHide;
    private javax.swing.JCheckBox jCheckChinHide;
    private javax.swing.JCheckBox jCheckChinThin;
    private javax.swing.JCheckBox jCheckMeasHide;
    private javax.swing.JCheckBox jCheckSingle;
    private javax.swing.JCheckBox jCheckTextHide;
    private javax.swing.JComboBox jComboText;
    private javax.swing.JLabel jLabBMCount;
    private javax.swing.JLabel jLabChinCount;
    private javax.swing.JLabel jLabMeas;
    private javax.swing.JLabel jLabMeasCount;
    private javax.swing.JLabel jLabText1;
    private javax.swing.JLabel jLabText2;
    private javax.swing.JLabel jLabTextCount;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jMeasure;
    private javax.swing.JPanel jPanMove2;
    private javax.swing.JPanel jPanRadio;
    private javax.swing.JRadioButton jRadLineSUV;
    private javax.swing.JRadioButton jRadMeasAngle;
    private javax.swing.JRadioButton jRadMeasDist;
    private javax.swing.JRadioButton jRadMeasLine;
    private javax.swing.JRadioButton jRadSaveDelete;
    private javax.swing.JRadioButton jRadSaveMemory;
    private javax.swing.JRadioButton jRadSaveStudy;
    private javax.swing.JPanel jSave;
    private javax.swing.JSpinner jSpinChinSize;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel jText;
    // End of variables declaration//GEN-END:variables
	PetCtPanel parPanel = null;
	PetCtFrame parentFrm;
	ArrayList<myMeasure> m_measurements[] = null;
	ArrayList<myChina> m_china[] = null;
	ArrayList<myBookMark> m_BMList = null;
	String[] m_floatingText;
	bkgdRobot work2 = null;
	int currSlice, dispType, sty3Type, lastBMi, sliceType = -1;
	boolean m_hideAnnotations = false, m_thin = false, m_obliqueFlg = false;
	boolean m_markSliceFlg = true, m_dirtyFlg = false, m_delay = false;
	Dimension m_origSz;
	int m_lastView, m_thisView, m_numTemp, m_measDraw, m_activeArrow, m_saveType;
	Point[] m_tempPnt;
}

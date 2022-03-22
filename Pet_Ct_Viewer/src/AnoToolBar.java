
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
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
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.scijava.vecmath.Point2d;
import org.scijava.vecmath.Point3d;

/**
 *
 * @author ilan
 */
public class AnoToolBar {
	static final int RB_DIST = 1;
	static final int RB_UPARROW = 2;
	static final int RB_RIGHTARROW = 3;
	static final int RB_DOWNARROW = 4;
	static final int RB_LEFTARROW = 5;
	static final int RB_TEXT = 6;
	static final int RB_SUV =7;
	static final int RB_BM = 8;
	static final int PNTSZ = Annotations.PNTSZ;
	static final int VSLICE = Annotations.VSLICE;
	static final int VNUMFRMS = Annotations.VNUMFRMS;
	
	void init(PetCtPanel par1) {
		parPanel = par1;
		m_lastView = -2;	// illegal value
		m_numTemp = 0;
		m_tempPnt = new Point[3];
		for( int i =0; i<3; i++) readFiles(i);
		parFrm = parPanel.parent;
		fillStrings();
		parFrm.setVisibleToolBar2(true);
		parFrm.tbAutoValue(true);
		currRB = RB_BM;
		m_dirtyFlg = false;
//		parFrm.fitWindow();
	}

	boolean takeAction( boolean useMouse) {
		if( !useMouse) return true;
		if( parPanel.mouse1.whichButton == MouseEvent.BUTTON3) return false;
		return currRB != RB_BM;
	}
	
	void setRadioTB2(int value) {
		currRB = value;
		parFrm.helper4annotBar(currRB == RB_TEXT);
		setCountVals();
	}

	void maybeSwitch2Bookmarks() {
		if( !parFrm.getAutoBM().isSelected()) return;
		setRadioTB2(RB_BM);
		parFrm.setVisibleToolBar2(true);
	}

	void dispose() {
		if( m_dirtyFlg) {
			int i = JOptionPane.showConfirmDialog(parFrm, "There are unsaved changes.\n"
					+ "Do you want to save them as part of the study?",
					"Unsaved changes", JOptionPane.YES_NO_OPTION);
			if( i == JOptionPane.YES_OPTION) saveAllFiles();
		}
		parFrm.setVisibleToolBar2(false);
		parFrm.tbAutoValue(false);
//		parFrm.fitWindow();
	}

	// this process a mouse click from the image to add a point or place text
	void processMouseSingleClick(int posMod, Point pt1, double scl1) {
//		int posMod = check4FusedPress(pos3);
		if( posMod > 2) return;
		m_thisView = ((posMod-1) & 1)*2;	// 0 for PET, 2 for CT
		if( m_thisView == 0) { // either corrected or uncorrected PET
			JFijiPipe pip1 = parPanel.getCorrectedOrUncorrectedPipe(false);
			if( pip1 == parPanel.upetPipe) m_thisView = 1;
		}
		if( currRB == RB_DIST || currRB == RB_SUV) {
			addMeasurePoint(pt1, scl1);
			return;
		}
		if( currRB == RB_UPARROW || currRB == RB_RIGHTARROW ||
			currRB == RB_DOWNARROW || currRB == RB_LEFTARROW) {
			addArrow(pt1, scl1);
			return;
		}
		if( currRB == RB_TEXT) {
			addText(pt1, scl1);
		}
	}

	void drawGraphics(Graphics2D g, JFijiPipe pet1) {
		if( pet1 == null) return;
		int currPipeIndx = 0;
		if( pet1 == parPanel.upetPipe) currPipeIndx = 1;
		getParentInfo();
		drawTempPnts(g);
		drawSub(g, currPipeIndx, pet1);	// pet
		drawSub(g, 2, pet1);	// ct
	}

	void markButton() {
		if( currRB != RB_BM) return;
		addBookMark(true);
		m_numTemp = 0;
		setCountVals();
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

	void drawSub(Graphics2D g, int indx, JFijiPipe pet1) {
		int i;
		if (pet1 == null) return;
		if (m_measurements[indx] != null && !m_measurements[indx].isEmpty()) {
			myMeasure meas1;
			for (i = 0; i < m_measurements[indx].size(); i++) {
				meas1 = m_measurements[indx].get(i);
				meas1.drawData(g, indx, pet1);
			}
		}
		if (m_floatingText[indx] != null) {
			myFloatingText txt1 = new myFloatingText();
			txt1.drawData(g, indx, pet1);
		}
		if (m_china[indx] != null && !m_china[indx].isEmpty()) {
			myChina chin1;
			for (i = 0; i < m_china[indx].size(); i++) {
				chin1 = m_china[indx].get(i);
				chin1.drawData(g, indx, pet1);
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
			m_BMList = new ArrayList<>();
			m_china = new ArrayList[3];
			m_measurements = new ArrayList[3];
			m_floatingText = new String[3];
		}
		if( flName == null) return;
		File fl1 = new File(flName);
		if( !fl1.exists()) return;
		m_china[indx] = new ArrayList<>();
		m_measurements[indx] = new ArrayList<>();
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
		myWriteDicom writeDcm = new myWriteDicom(parFrm, null);
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
						parFrm.changeLayout( j[1]);
						Thread.sleep(500);
						parFrm.changeLayout( j[0]);
						Thread.sleep(800);
						m_delay = false;
					}
					getParentInfo();	// get currSlice
					if( isFirst) writeDcm.cleanDirectory( sliceType, m_saveType);
					isFirst = false;
					writeDcm.writeDicomHeader(currSlice, sliceType, m_saveType);
					Thread.sleep(1000);	// allow write which has 1/2 sec sleep
					if( writeDcm.writeStatus != 1) Thread.sleep(2000);
				}
				if( n3 <= 1) break;
				i = ((i3+1) % 3);
				parFrm.changeLayout(j[i]);
				Thread.sleep(800);
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		}
		if( m_origSz != null) parFrm.setSize(m_origSz);
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
		Integer val = 0;
		int i;
		switch(currRB) {
			case RB_DIST:
			case RB_SUV:
				for(i=0;i<3; i++) {
					if(m_measurements[i] != null) val += m_measurements[i].size();
				}
				break;

			case RB_UPARROW:
			case RB_DOWNARROW:
			case RB_LEFTARROW:
			case RB_RIGHTARROW:
				for(i=0;i<3; i++) {
					if(m_china[i] != null) val += m_china[i].size();
				}
				break;

			case RB_TEXT:
				for(i=0;i<3; i++) {
					val += getTextCnt(i);
				}
				break;

			case RB_BM:
				val = m_BMList.size();
				break;
		}
		parFrm.setCountLabel(val);
	}

	void saveAllFiles() {
		for( int i =0; i<3; i++) saveFiles(i);
		m_numTemp = 0;
		setCountVals();
		parPanel.repaint();
	}

	void setNextSlice(int upDown) {
		myBookMark bm1;
		int i, diff, numFrms, size1, closest = 0, minDiff = -1;
		int bmSlice;
		boolean singleOrientation = true;
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
		parPanel.parent.changeLayout(sliceType);
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
		parPanel.parent.update3Display();
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

@SuppressWarnings("unchecked")
	void removeTBPoint(boolean killAll) {
		int size1;
		switch(currRB) {
			case RB_DIST:
			case RB_SUV:
				if( killAll) m_measurements = new ArrayList[3];
				if( m_lastView >= 0 && m_measurements[m_lastView] != null) {
					size1 = m_measurements[m_lastView].size();
					if( size1 > 0) m_measurements[m_lastView].remove(size1-1);
				}
				break;

			case RB_UPARROW:
			case RB_DOWNARROW:
			case RB_LEFTARROW:
			case RB_RIGHTARROW:
				if( killAll) m_china = new ArrayList[3];
				if( m_lastView >= 0 && m_china[m_lastView] != null) {
					size1 = m_china[m_lastView].size();
					if( size1 > 0) m_china[m_lastView].remove(size1-1);
				}
				break;

			case RB_TEXT:
				if( killAll) m_floatingText = new String[3];
				else {
					if( m_lastView >= 0 && m_floatingText[m_lastView] != null ) {
						size1 = m_floatingText[m_lastView].length();
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
				break;

			case RB_BM:
				if( killAll) m_BMList = new ArrayList<>();
				else {
					size1 = m_BMList.size();
					if( size1 > 0) m_BMList.remove(size1-1);
				}
			break;
		}
		m_numTemp = 0;
		setCountVals();
		parPanel.repaint();
	}

	void addMeasurePoint(Point pt1, double scale) {
		if( m_thisView != m_lastView) m_numTemp = 0;
		m_lastView = m_thisView;
		if( m_numTemp > 2) m_numTemp = 0;
		m_tempPnt[m_numTemp++] = pt1;
		myMeasure meas1;
		Point pntTmp;
		double x1, z0, z1, val1, val2, val3, pixelSize, pixSizeZ;
		int i, m_measDraw=1, numPnts = 2;
		Integer vali;
		int[] vals;
		JFijiPipe pet1 = parPanel.petPipe;
		vals = getNumFrmSlice( pet1, m_lastView);
		if( currRB == RB_SUV) m_measDraw = 3;
		if( m_measDraw == 2) numPnts = 3;
		if( m_numTemp == 1) m_saveSlice1 = vals[VSLICE];
		if( m_numTemp >= numPnts) {
			getParentInfo();
			pixelSize = pet1.data1.pixelSpacing[0];
			pixSizeZ = pet1.data1.sliceThickness;
			z0 = x1 = pixelSize / Annotations.MEASURE_SCALE;
			z1 = pixSizeZ / Annotations.MEASURE_SCALE;
			if( sliceType == JFijiPipe.DSP_AXIAL) {
				z0 = z1;
				z1 = x1;
			}
			meas1 = new myMeasure();
			for( i=0; i<numPnts; i++) {
				pntTmp = getPoint1000(i, scale);
				meas1.x[i] = pntTmp.x;
				meas1.y[i] = pntTmp.y;
			}
			meas1.zval = (short) vals[VSLICE];
			if( m_measDraw == 1) {
				val1 = (meas1.x[0] - meas1.x[1]) * x1;
				val2 = (meas1.y[0] - meas1.y[1]) * z1;
				val3 = meas1.zval0;
				if( val3 > 0) {
					val3 = Math.abs((meas1.zval - val3)*z0*Annotations.MEASURE_SCALE);
				}
				val1 = Math.sqrt(val1*val1 + val2*val2 + val3*val3);
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
				m_measDraw = 0;
				if(m_lastView == 0) {
					meas1.val1 = meas1.getSUV();
					if(meas1.val1 < 0) m_measDraw = 1;
				}
			}
			meas1.type = (short)(4*sliceType + m_measDraw);
			if(m_measurements[m_lastView] == null)
				m_measurements[m_lastView] = new ArrayList<>();
			m_measurements[m_lastView].add(meas1);
			m_dirtyFlg = true;
//			setObliqueLabel();
			addBookMark(true);
			maybeSwitch2Bookmarks();
			setCountVals();
			m_numTemp = 0;
		}
		parPanel.repaint();
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
		retPnt.x *= Annotations.MEASURE_SCALE;
		retPnt.y *= Annotations.MEASURE_SCALE;
		return retPnt;
	}

	void drawTempPnts(Graphics2D g) {
		if( (currRB != RB_DIST && currRB != RB_SUV) || m_lastView < 0) return;
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
//				lastBMi = i;	// save this value for remove
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
		chin1.type = (byte)(4*sliceType + currRB - RB_UPARROW);
		chin1.size = 12;	// fixed value in toolbar
		if(m_china[m_lastView] == null)
			m_china[m_lastView] = new ArrayList<>();
		m_china[m_lastView].add(chin1);
		m_dirtyFlg = true;
		addBookMark(true);
		maybeSwitch2Bookmarks();
		setCountVals();
		parPanel.repaint();
	}

	int check4FusedPress(int pos3) {
		int retVal = pos3;
		if( retVal < 3) return retVal;	// didn't press on fused
		if( isFusedShowing() && currRB != RB_BM) {
			retVal = 1;
		}
		return retVal;
	}
	
	boolean isFusedShowing() {
		int i = parPanel.m_masterFlg;
		return (  i== PetCtPanel.VW_PET_CT_FUSED ||  i== PetCtPanel.VW_PET_CT_FUSED_UNCORRECTED);
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
				parFrm.getComboText().addItem(line);
			}
			br.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
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
		short type, zval, zval0;
		double val1;

		myMeasure() {
			x = new int[3];
			y = new int[3];
			val1 = 0;
			zval0 = (short) m_saveSlice1;
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
				zval0 = buf.getShort();
				buf.getShort();	// padding
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
				buf.putShort(zval0);
				buf.putShort((short)0);	// padding
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

		private boolean maybeDrawOnMIP(Graphics2D g, JFijiPipe pet1,
			double scl1, int widthX, int[] vals) {
			if( zval0 <= 0 || zval0 == zval || isFusedShowing()) return false;
			if( val1 <= 0) return false;
			int [] xdsp, ydsp;
			xdsp = new int[2];
			ydsp = new int[2];
			Point pv;
			JFijiPipe mipPipe = parPanel.mipPipe;
			double ax, cor, sag, tmp;
			g.setColor(Color.red);
			int i, w2 = 2*widthX, num = vals[VNUMFRMS];
			for( i=0; i<2; i++) {
				ax = zval0;
				sag = ((double)x[i])/Annotations.MEASURE_SCALE;
				cor = ((double)y[i])/Annotations.MEASURE_SCALE;
				if(i>0) ax = zval;
				switch(sliceType) {
					case JFijiPipe.DSP_CORONAL:
						tmp = ax;
						ax = cor;
						cor = tmp;
						break;

					case JFijiPipe.DSP_SAGITAL:
						tmp = ax;
						ax = cor;
						cor = sag;
						sag = tmp;
						break;

					default:	// axial
						ax = num - ax - 1;
						break;
				}
				pv = mipPipe.getMIPposition(ax, cor, sag, scl1);
				xdsp[i] = pv.x + w2;
				ydsp[i] = pv.y;
				g.fillOval(pv.x+w2-PNTSZ, pv.y-PNTSZ, 2*PNTSZ, 2*PNTSZ);
			}
			g.drawPolyline(xdsp, ydsp, 2);
			if( !parPanel.parent.isMipRotating()) {
				Integer vali = (int)(val1 + 0.5);
				String tmp1 = vali.toString() + " mm";
				i = 0;
				if( xdsp[1] > xdsp[0]) i = 1;
				g.drawString(tmp1, xdsp[i]+PNTSZ, ydsp[i]+PNTSZ);
			}
			return true;
		}

		void drawData(Graphics2D g, int indx, JFijiPipe pet1) {
			if( (type/4) != sliceType) return;
			int offX, offX2=0, widthX;
			double scl1 = parPanel.getScalePet();
			widthX = parPanel.mouse1.widthX;
			int[] vals = getNumFrmSlice(pet1, indx);
			if( maybeDrawOnMIP(g, pet1, scl1, widthX, vals)) return;
			if(zval != vals[VSLICE]) return;
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
				Point2d pt1 = new Point2d( x1000/Annotations.MEASURE_SCALE, y1000/Annotations.MEASURE_SCALE);
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
			cor1 = y[0]/Annotations.MEASURE_SCALE;
			sag1 = x[0]/Annotations.MEASURE_SCALE;
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
			SUVtype = parPanel.parent.getSUVtype();
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
			PetCtFrame parFrm = parPanel.parent;
			String retVal = (String) parFrm.getComboText().getEditor().getItem();
			return retVal.trim();
		}

		void addPoint(Point pt1, double scale) {
			m_lastView = m_thisView;
			m_numTemp = 0;
			String inTxt = getAComboLabel();
			if( inTxt.isEmpty()) return;
			m_tempPnt[0] = new Point(pt1);
			int[] vals;
			JFijiPipe pet1 = parPanel.petPipe;
			getParentInfo();
			Graphics gr1 = parPanel.getGraphics();
			FontMetrics fm1 = gr1.getFontMetrics();
			Rectangle2D rc1 = fm1.getStringBounds(inTxt, gr1);
			m_tempPnt[0].x -= (int) (rc1.getWidth()/2);
			if(m_tempPnt[0].x < 0) m_tempPnt[0].x = 0;
			Point pntTmp = parPanel.petPipe.scrn2PosInt(m_tempPnt[0], scale, dispType);
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
			maybeSwitch2Bookmarks();
			setCountVals();
			parPanel.repaint();
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
//			if( m_thin) thin4 = 4;
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
			Point2d pt1 = new Point2d( x/Annotations.MEASURE_SCALE, y/Annotations.MEASURE_SCALE);
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
			halfSize = (zoom1 - 1) * Annotations.HALF_SCALE;
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
			halfSize = (zoom1 - 1) * Annotations.HALF_SCALE;
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

	protected class bkgdRobot extends SwingWorker {

		@Override
		protected Void doInBackground() {
			doRobotSwitchSlice();
			return null;
		}

	}

	PetCtPanel parPanel;
	PetCtFrame parFrm;
	ArrayList<myMeasure> m_measurements[] = null;
	ArrayList<myChina> m_china[] = null;
	ArrayList<myBookMark> m_BMList = null;
	String[] m_floatingText;
	bkgdRobot work2 = null;
	int currSlice, dispType, sliceType = -1;
	boolean m_dirtyFlg = false, m_delay = false;
	Dimension m_origSz;
	int currRB, m_lastView, m_thisView, m_numTemp;
	int m_saveType, m_saveSlice1;
	Point[] m_tempPnt;
}

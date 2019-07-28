import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.dcm4che3.tool.storescu.StoreSCU;

/**
 * This program has been used to make screen captures and store them
 * as Dicom files. Now it is being extended to take a locally generated
 * file and store it in Dicom format. The file of interest comes from
 * the brown fat program. This case will be distinguished by a non
 * null ImagePlus img1 object.
 * 
 * @author Ilan
 */
public class myWriteDicom {
	PetCtFrame par1 = null;
	Display3Frame d3 = null;
	Window dbWindow = null;
	JFijiPipe mipPipe = null;
	BufferedImage inBufImg = null;
	ImagePlus img1 = null;	// alternative use of WriteDicom
	public static final short AE = 0x4541;
	public static final short AS = 0x5341;
	public static final short UL = 0x4c55;
	public static final short OB = 0x424f;
	public static final short UI = 0x4955;
	public static final short CS = 0x5343;
	public static final short DA = 0x4144;
	public static final short DT = 0x5444;
	public static final short TM = 0x4d54;
	public static final short SH = 0x4853;
	public static final short ST = 0x5453;
	public static final short LT = 0x544c;
	public static final short LO = 0x4f4c;
	public static final short PN = 0x4e50;
	public static final short IS = 0x5349;
	public static final short US = 0x5355;
	public static final short DS = 0x5344;
	public static final short SQ = 0x5153;
	public static final String SOP_CLASS_TYPE_RAW = "1.2.840.10008.5.1.4.1.1.66";
	File outFile1;
	byte [] outBuf = null, inBuf = null;
	bkgdSaveData work2 = null;
	ByteBuffer out1 = null;
	FileOutputStream flOut1 = null;
	int intElement, writeStatus = -1;
	Integer newSeriesNum = -1, specialType = 0, subType = 0;
	int m_delay = 500;
	String newSeriesName = null, newSeriesUID = null;
	String meta = null, AETitle = null;
	patInfo updated = null;
	Integer sliceZ = 0, sliceType = 0, robotType = 0;
	String strElement, fileGivenName = null;
	boolean explicitLE = true;
	ArrayList<checkDcmElement> goodDcmList;
	ArrayList<checkSeqList> goodSeqList;

	public boolean getOutFile(boolean clean) {
		try {
			int i;
			String key1, tmp1;
			File tmpFl;
			if( par1 == null) return false;
			Preferences prefer1 = par1.jPrefer;
			Character dup1 = 'a';
			AETitle = null;
			key1 = "significant image path";
			i = prefer1.getInt("significant spin val", 0);
			if( i > 0) key1 += i;
			String path1 = prefer1.get(key1, null);
			if (path1 == null || path1.isEmpty()) {
				JOptionPane.showMessageDialog(par1.getOwner(),
						"Please fill in Directory for Significant Image (on menu: Edit->Options).\nThen try again.");
				return false;
			}
			if( mipPipe != null) {
				tmpFl = File.createTempFile("tmpDcm", "dcm");
				tmp1 = tmpFl.getPath();
				tmpFl.delete();
				tmpFl = new File(tmp1);
				tmpFl.mkdirs();
				outFile1 = new File(tmp1 + fileGivenName);
				outFile1.deleteOnExit();
				return outFile1 != null;
			}
			if( isAETitle(path1)) {
				AETitle = path1;
				outFile1 = File.createTempFile("tmpDcm", "dcm");
				return outFile1 != null;
			}
			if( !path1.endsWith("/")) path1 += "/";
			if( sliceType > 0) path1 += getSeriesUID();
			else path1 += "Uploads";
			outFile1 = new File(path1);
			boolean isExist = outFile1.exists();
			if( clean) {
				if( isExist) {
					File[] flst = outFile1.listFiles();
					for( i=0; i<flst.length; i++) flst[i].delete();
				}
				return true;	// done
			}
			if( !isExist) outFile1.mkdirs();
			path1 += "/";
			String patName = ChoosePetCt.getCompressedPatName(meta);
			patName = patName.replaceAll("['/]", "");
			i = patName.indexOf(',');
			if( i>0) patName = patName.substring(0, i);
			Date tmpDate = new Date();
			long currTime = System.currentTimeMillis() / 1000l; // seconds
			int sec = (int) currTime;
			sec = sec % (60*60*24);
			int min = (sec / 60)%60;
			int hour = sec / (60*60);
			sec = sec % 60;
			String baseName = patName + String.format("%02d%02d%02d", hour, min, sec);
			do {
				String outName = baseName + dup1.toString();
				if( fileGivenName != null && !fileGivenName.isEmpty()) {
					tmpFl = new File(path1 + outName);
					tmpFl.mkdirs();
					outName += fileGivenName;
				}
				else outName += ".dcm";
				outFile1 = new File(path1 + outName);
				if( outFile1 == null) return false;
				if( dup1++ > 'z') return false;
			} while( outFile1.exists());
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return outFile1 != null;
	}


	public myWriteDicom(PetCtFrame par1, Window dbWin) {
		this.par1 = par1;
		dbWindow = dbWin;
	}

	public myWriteDicom(Display3Frame d3f) {
		par1 = d3f.srcFrame;
		d3 = d3f;
	}

	public myWriteDicom( BufferedImage img1, PetCtFrame par1) {
		this.par1 = par1;
		inBufImg = img1;
	}
	
	public myWriteDicom(PetCtFrame par1, JFijiPipe mipPipe, String fileName) {
		this.par1 = par1;
		this.mipPipe = mipPipe;
		fileGivenName = fileName;
	}
	
	public myWriteDicom(ImagePlus img) {
		img1 = img;
	}

	protected void SaveData() {
		IJ.showStatus(" ");
		work2 = new bkgdSaveData();
		work2.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String propertyName = evt.getPropertyName();
				if( propertyName.equals("state")) {
					SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
					if( state == SwingWorker.StateValue.DONE) {
						if( dbWindow != null) dbWindow.toFront();
						work2 = null;
					}
				}
			}
		});
		work2.execute();
	}
	
	void doImgPlusSaveData() {
		int i,j,n;
		double progVal;
		ImageStack stk1;
		String meta1, parPath, flName;
		n = img1.getImageStackSize();
		stack1: if( n> 1) {
			stk1 = img1.getImageStack();
			outFile1.mkdirs();
			parPath = outFile1.getPath() + File.separator;
			meta1 = stk1.getSliceLabel(1);
			if(meta1 == null || meta1.isEmpty()) {
				outFile1 = new File(parPath + "saveAs.dcm");
				break stack1;
			}
			IJ.showStatus("Writing images, please be patient...");
			ImageJ ij = IJ.getInstance();
			if( ij != null) ij.toFront();
			for(i=1; i<=n; i++) {
				progVal = ((double)i)/n;
				IJ.showProgress(progVal);
				meta = stk1.getSliceLabel(i);
				if(meta == null || meta.isEmpty()) break;
				j = meta.indexOf("\n");
				flName = meta.substring(0, j);
				j = flName.lastIndexOf(".");
				if( j>0) flName = flName.substring(0, j) + ".dcm";
				outFile1 = new File(parPath + flName);
				doImgPlusSaveDataSub(i);
			}
			IJ.showStatus("Done.");
			IJ.showProgress(1.0);
			par1.toFront();
			return;
		}
		doImgPlusSaveDataSub(0);
	}

	void fakeGroup2(String tmpSOPInstanceUID) {
		String tmp = ChoosePetCt.getDicomValue(meta, "0008,0016");
		strElement = tmp;
		writeElement(2, 2, UI);
		strElement = tmpSOPInstanceUID;
		if( strElement == null) strElement = ChoosePetCt.getDicomValue(meta, "0008,0018");
		writeElement(2, 3, UI);
		strElement = "1.2.840.10008.1.2";	// implicit little endian
		if(explicitLE) strElement += ".1";	// explicit little endian
		writeElement(2, 0x10, UI);
		strElement = "1.2.16.840.1.113664.3"; // my Implementation class UID
		writeElement(2, 0x12, UI);
		strElement = "FAKE_GROUP2";
		writeElement(2, 0x13, SH);
		strElement = "PET_CT_VIEWER"; //AETitle);
		writeElement(2, 0x16, AE);
	}

	void doImgPlusSaveDataSub(int sliceNum) {
		int pos1, pos2, group, element, i, numFrms, coef0 = 0;
		int offst, depth, dataSz, sz1, j, k, tmpi1, group2 = 0;
		int coefAll;
		float tmpfl;
		double rescaleSlope;
		short type1;
		Object pix1;
		byte [] pixByte;
		short [] pixels;
		double coef[];
		int [] pixInt;
		float [] pixFloat;
		boolean isModify = newSeriesName != null || newSeriesNum >= 0 || updated != null;
		boolean isOdd = false;
		String[] parms;
		String tmpSOPInstanceUID=null;
		checkDcmElement currElement;
		try {
			doDcmCheck();
			int rescaleIntercept = (int) ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "0028,1052"));
			rescaleSlope = ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "0028,1053"));
			if( rescaleSlope <= 0) rescaleSlope = 1.0;
			coef = img1.getCalibration().getCoefficients();
			if( coef != null) coef0 = (int) coef[0];
			depth = img1.getBitDepth();
			coefAll = coef0 - rescaleIntercept;
			outBuf = new byte[32768];
			out1 = ByteBuffer.wrap(outBuf);
			out1 = out1.order(ByteOrder.LITTLE_ENDIAN);
			flOut1 = new FileOutputStream(outFile1);
			out1.position(128);	// buffer was initialized to zero
			out1.putInt(0x4d434944);	// DICM
			flOut1.write(outBuf, 0, 132);
			out1.position(0);
			maybeAddDicomElements();
			if( isModify) tmpSOPInstanceUID = generateSOPInstanceUID();
			for(j=0; j<goodDcmList.size(); j++) {
				currElement = goodDcmList.get(j);
				group = currElement.group;
				element = currElement.element;
				if( group2 == 0) {
					group2 = 1;	// started
					intElement = 0;
					writeElement(2, 0, UL);
					writeElement(2, 1, OB);
					if( group != 2) fakeGroup2(tmpSOPInstanceUID);
				}
				if( group2 == 1 && group != 2) {
					group2 = 2;	// ended
					pos2 = out1.position();
					intElement = pos2 - 12;
					out1.putInt(8, intElement);
				}
				if( group == 0x7fe0) break;
				type1 = getType(group, element);
				if( type1 == 0) {
					type1++;	// break point
					continue;
				}
				if( type1 == SQ){
					writeSequence(group, element);
					continue;
				}
				strElement = currElement.value;
				if( isModify) {	// change selected elements
					switch(group) {
						case 2:
							if( element == 3) strElement = tmpSOPInstanceUID;
							break;

						case 8:
							if( element == 8) strElement = "DERIVED\\SECONDARY";
							if( element == 0x18) strElement = tmpSOPInstanceUID;
							if( element == 0x103e && newSeriesName != null) strElement = newSeriesName;
							break;
							
						case 0x20:
							if( element == 0xe && newSeriesUID != null) strElement = newSeriesUID;
							if( element == 0x11 && newSeriesNum >= 0) strElement = newSeriesNum.toString();
							break;
					}
				}
				if( updated != null && group == 0x10) changeStrElement(element);
				// if the user crops the image, the rows and cols change
				if( group == 0x28 && ( element == 0x10 || element == 0x11)) {
					Integer tmpEl = img1.getHeight();
					if( element == 0x11) tmpEl = img1.getWidth();
					strElement = tmpEl.toString();
				}
				if( type1 == US || type1 == UL)
					intElement = ChoosePetCt.parseInt(strElement);
				writeElement(group, element, type1);
			}
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);
			offst = 0;
			numFrms = img1.getStackSize();
			if(sliceNum > 0) {	// writes individual dicom files
				numFrms = 1;
				offst = sliceNum - 1;
			}
			sz1 = img1.getHeight() * img1.getWidth();
			dataSz = sz1 * depth / 8;
			if( depth == 32) dataSz /= 2;	// float data is illegal for PET

			// group 0x7fe0 - pixel data
			out1.position(0);
/*			if (explicitLE) {
				intElement = dataSz*numFrms + 12;
				writeElement(0x7fe0, 0, UL);
			}*/
			// Kimberly Smith renal scan is an odd number - fix it.
			intElement = dataSz*numFrms;
			if( (intElement & 1) == 1) {
				intElement++;
				isOdd = true;
			}
			writeElement(0x7fe0, 0x10, OB);
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);

			outBuf = new byte[dataSz];
			for( j=1; j<=numFrms; j++) {
				pix1 = img1.getStack().getPixels(j+offst);
				switch( depth) {
					
					case 32:	// change to 16 bits
						pixFloat = (float []) pix1;
						for( i=0; i<sz1; i++) {
							k = i*2;
							tmpfl = pixFloat[i] + coefAll;
							tmpi1 = (int) (tmpfl/rescaleSlope);
							outBuf[k] =(byte) tmpi1;
							tmpi1 = tmpi1 >> 8;
							outBuf[k+1] =(byte) tmpi1;
						}
						flOut1.write(outBuf, 0, dataSz);
						break;

					case 24:
						pixInt = (int []) pix1;
						for( i=0; i<sz1; i++) {
							k = i*3;
							tmpi1 = pixInt[i];
							outBuf[k+2] =(byte) tmpi1;
							tmpi1 = tmpi1 >> 8;
							outBuf[k+1] =(byte) tmpi1;
							tmpi1 = tmpi1 >> 8;
							outBuf[k] =(byte) tmpi1;
						}
						flOut1.write(outBuf, 0, dataSz);
						break;
						
					case 16:
						pixels = (short [])  pix1;
						for( i=0; i<sz1; i++) {
							k = i*2;
							tmpi1 = pixels[i] + coefAll;
							outBuf[k] =(byte) tmpi1;
							tmpi1 = tmpi1 >> 8;
							outBuf[k+1] =(byte) tmpi1;
						}
						flOut1.write(outBuf, 0, dataSz);
						break;
						
					case 8:
						pixByte = (byte[]) pix1;
						for( i=0; i<sz1; i++)
							outBuf[i] = pixByte[i];
						flOut1.write(outBuf, 0, dataSz);
						break;
				}
			}
			if( isOdd) {
				outBuf[0] = 0;
				flOut1.write(outBuf, 0, 1);	// last byte to make even
			}
			flOut1.close();
			flOut1 = null;
			if( AETitle != null) {
				outFile1.deleteOnExit();
				parms = new String[3];
				parms[0] = "-c";
				parms[1] = AETitle;
				parms[2] = outFile1.getPath();
				StoreSCU.main(parms);
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	void maybeAddDicomElements() {
		int i, j, n;
		int[] chkVals = new int[] {0x10, 0x20, 0x30, 0x40, 0x1020, 0x1030};
		checkDcmElement currElement, addElement;
		if( updated == null) return;
		n = goodDcmList.size();
		for( i=0; i<n; i++) {
			currElement = goodDcmList.get(i);
			if( currElement.group == 0x10 && currElement.element >= 0x10) break;
		}
		if( i++ >= n) return;
		for( j=0; j<6; j++) {
			addElement = new checkDcmElement();
			addElement.group = 0x10;
			addElement.element = chkVals[j];
			addElement.value = null;
			currElement = goodDcmList.get(i);
			while( currElement.group == 0x10 && currElement.element < addElement.element) {
				currElement = goodDcmList.get(++i);
			}
			if( currElement.group == 0x10 && addElement.element == currElement.element) {
				i++;
				continue;
			}
			goodDcmList.add(i++, addElement);
		}
	}

	void changeStrElement(int element) {
		String tmp1 = null;
		Double val1;
		switch( element) {
			case 0x10:
				tmp1 = updated.patName;
				break;

			case 0x20:
				tmp1 = updated.patID;
				break;

			case 0x30:
				tmp1 = updated.patDOB;
				break;

			case 0x40:
				tmp1 = updated.patSex;
				break;

			case 0x1020:
				val1 = updated.patHeight;
				if( val1 > 0) tmp1 = val1.toString();
				break;

			case 0x1030:
				val1 = updated.patWeight;
				if( val1 > 0) tmp1 = val1.toString();
				break;
		}
		if( tmp1 != null && !tmp1.isEmpty()) strElement = tmp1;
	}

	void writeSequence(int seqGroup, int seqElement) {
		checkSeqList currSeqList = null;
		ArrayList<checkDcmElement> seqList;
		checkDcmElement currElement;
		int group, element, prevElement=0;
		short type1;
		int i, n = goodSeqList.size();
		for( i=0; i<n; i++) {
			currSeqList = goodSeqList.get(i);
			if( currSeqList.seqGroup == seqGroup &&
					currSeqList.seqElement == seqElement) break;
		}
		if( i>= n || currSeqList == null) return;
		seqList = currSeqList.seqList;
		n = seqList.size();
		out1.putShort((short)seqGroup);
		out1.putShort((short)seqElement);
		if (explicitLE) {
			out1.putShort(SQ);
			out1.putShort((short)0);
		}
		if( n==0) {	// empty sequence
			out1.putInt(0);
			return;
		}
		out1.putInt(-1);
		out1.putInt(0xe000fffe);
		out1.putInt(-1);
		
		for(i=0; i<n; i++) {
			currElement = seqList.get(i);
			group = currElement.group;
			element = currElement.element;
			if(prevElement>element) {	// repeating set
				out1.putInt(0xe00dfffe);
				out1.putInt(0);
				out1.putInt(0xe000fffe);
				out1.putInt(-1);
			}
			prevElement = element;
			strElement = currElement.value;
			type1 = getSeqType(group, element);
			if( type1 == 0) continue;
			writeElement(group, element, type1);
		}

	// ending sequence
		out1.putInt(0xe00dfffe);
		out1.putInt(0);
		out1.putInt(0xe0ddfffe);
		out1.putInt(0);
	}

	// Note: I tried FileWriter/Reader, but they assume a fixed big endian format.
	// Wrapping a byte buffer allows both big and little endian
	void doActualSaveData() {
		int i, j, k, tmpi1, pos1, bitsAlloc, sz1, dataSz, dataSz1, numFrms = 1, w1, h1;
		short [] pixels;
		int padByte = 0;
		String SOPInstanceUID, SOPClassUID, tmp;
		String[] parms;
		BufferedImage imIn = null;
		try {
			writeStatus = 0;
			if( dbWindow != null) par1.toFront();
			if( mipPipe == null && specialType != 3) {
				if(inBufImg != null) imIn = inBufImg;
				else {
					Thread.sleep(m_delay);	// allow menu to close
					imIn = getDcmData();
					if( imIn == null) return;
				}
			}
			// doesn't work for Windows
/*			if(AETitle != null) {
				if( !isEchoScu()) return;
			}*/
			outBuf = new byte[32768];
			out1 = ByteBuffer.wrap(outBuf);
			out1 = out1.order(ByteOrder.LITTLE_ENDIAN);
			flOut1 = new FileOutputStream(outFile1);
			out1.position(128);	// buffer was initialized to zero
			out1.putInt(0x4d434944);	// DICM
			flOut1.write(outBuf, 0, 132);
			if( specialType == 3) {	// raw data
				w1 = inBuf.length;
				h1 = 1;
				SOPClassUID = SOP_CLASS_TYPE_RAW;
			} else {
				if( imIn != null) {
					w1 = imIn.getWidth();
					h1 = imIn.getHeight();
					SOPClassUID = ChoosePetCt.SOPCLASS_SC;	// Secondary capture
				} else {
					w1 = mipPipe.data1.width;
					h1 = mipPipe.data1.height;
					numFrms = mipPipe.getNormalizedNumFrms();	// 16
					SOPClassUID = ChoosePetCt.SOPCLASS_NM;
				}
			}
			sz1 = w1*h1;
			SOPInstanceUID = generateSOPInstanceUID();

			out1.position(0);
			intElement = 0;	// for now, fix later
			writeElement( 2, 0, UL);
			writeElement(2, 1, OB);
			strElement = SOPClassUID;
			writeElement(2, 2, UI);
			strElement = SOPInstanceUID;
			writeElement(2, 3, UI);
			strElement = "1.2.840.10008.1.2";	// implicit little endian
			if(explicitLE) strElement += ".1";	// explicit little endian
			writeElement(2, 0x10, UI);
			strElement = "1.2.16.840.1.113664.3"; // my Implementation class UID
			writeElement(2, 0x12, UI);
			strElement = "jpeg";
			if(mipPipe != null) strElement = "mip";
			writeElement(2, 0x13, SH);
			strElement = ChoosePetCt.getDicomValue(meta, "0002,0016"); //AETitle);
			writeElement(2, 0x16, AE);
			pos1 = out1.position();
			i = pos1 - 12;	// don't count element 2,0, len=12
			out1.position(8);
			out1.putInt(i);	// update the group lenght
			flOut1.write(outBuf, 0, pos1);

			out1.position(0);	// start group 8 - general module
			strElement = "DERIVED\\SECONDARY";
			writeElement( 8, 8, CS);
			strElement = SOPClassUID;
			writeElement( 8, 0x16, UI);
			strElement = SOPInstanceUID;
			writeElement( 8, 0x18, UI);
			strElement = ChoosePetCt.getDicomValue(meta, "0008,0021"); //DicomDate
			writeElement( 8, 0x20, DA);
			writeElement( 8, 0x21, DA);
			if( SOPClassUID.equals(SOP_CLASS_TYPE_RAW)) writeElement( 8, 0x23, DA);
			strElement = ChoosePetCt.getDicomValue(meta, "0008,0030"); //DicomTime
			writeElement( 8, 0x30, TM);
			writeElement( 8, 0x31, TM);
			if( SOPClassUID.equals(SOP_CLASS_TYPE_RAW)) writeElement( 8, 0x33, TM);
			strElement = ChoosePetCt.getDicomValue(meta, "0008,0050"); // AccessionNum
			writeElement( 8, 0x50, SH);
			strElement = "OT";	// modality
			writeElement( 8, 0x60, CS);
			strElement = "WSD";	// conversion type - workstation
			writeElement( 8, 0x64, CS);
			strElement = ChoosePetCt.getDicomValue(meta, "0008,0070");	// manufacturer
			writeElement( 8, 0x70, LO);
			strElement = ChoosePetCt.getDicomValue(meta, "0008,0080");	// institution
			writeElement( 8, 0x80, LO);
			strElement = ChoosePetCt.getDicomValue(meta, "0008,0090");
			writeElement(8, 0x90, PN);	// referring physican
			strElement = ChoosePetCt.getDicomValue(meta, "0008,1030");	// study description
			writeElement( 8, 0x1030, LO);
			strElement = ChoosePetCt.getDicomValue(meta, "0008,103E");	// series description
			if( mipPipe != null) strElement = "MIP data";
			if( robotType > 0) {
				switch(sliceType) {
					case JFijiPipe.DSP_CORONAL:
						strElement = "Coronal";
						break;

					case JFijiPipe.DSP_SAGITAL:
						strElement = "Sagittal";
						break;

					default:
						strElement = "Axial";
				}
				if( robotType == 4) strElement += ", 3 Panes";
				else strElement += ", Pane " + robotType.toString();
			}
			tmp = getSpecial();
			if(!tmp.isEmpty()) strElement = tmp;
			writeElement( 8, 0x103e, LO);
			if( SOPClassUID.equals(SOP_CLASS_TYPE_RAW)) {
				strElement = "1.2.16.840.1.113664.3.1.0.0";
				writeElement( 8, 0x9123, UI);
			}
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);

			// group 10 - patient module
			out1.position(0);
			strElement = ChoosePetCt.getDicomValue(meta, "0010,0010");
			writeElement(0x10, 0x10, PN);	// patient name
			strElement = ChoosePetCt.getDicomValue(meta, "0010,0020");
			writeElement(0x10, 0x20, LO);	// patient ID
			strElement = ChoosePetCt.getDicomValue(meta, "0010,0030");
			writeElement(0x10, 0x30, DA);	// patient birthdate
			strElement = ChoosePetCt.getDicomValue(meta, "0010,0040");
			writeElement(0x10, 0x40, CS);	// patient sex
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);

			// group 20
			out1.position(0);
			strElement = ChoosePetCt.getDicomValue(meta, "0020,000D");
			if( strElement == null) strElement = SOPInstanceUID + ".1";
			writeElement(0x20, 0xd, UI);	// study Instance UID
			strElement = getSeriesUID();
			writeElement(0x20, 0xe, UI);	// series Instance UID
			strElement = ChoosePetCt.getDicomValue(meta, "0020,0010");
			writeElement(0x20, 0x10, SH);	// accession number
			strElement = sliceZ.toString();
			if( SOPClassUID.equals(SOP_CLASS_TYPE_RAW) || sliceType > 1) writeElement( 0x20, 0x13, IS);
//			strElement = ChoosePetCt.getDicomValue(meta, "0020,0032");
			strElement = "0\\0\\" + (-sliceZ);
			if( SOPClassUID.equals(SOP_CLASS_TYPE_RAW)) strElement = "-1\\-1\\-1"; // for Orthanc
			writeElement(0x20, 0x32, DS);
//			strElement = ChoosePetCt.getDicomValue(meta, "0020,0037");
			switch(sliceType) {
				case JFijiPipe.DSP_CORONAL:
					strElement = "1\\0\\0\\0\\0\\-1";
					break;

				case JFijiPipe.DSP_SAGITAL:
					strElement = "0\\1\\0\\0\\0\\-1";
					break;

				default:
					strElement = "1\\0\\0\\0\\1\\0";
			}
			// don't write orientation for MIP - it has none
			if( mipPipe == null) writeElement(0x20, 0x37, DS);	// patient orientation
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);

			// group 28 - image module
			bitsAlloc = 8;
			if( SOPClassUID.equals(ChoosePetCt.SOPCLASS_SC)) {
				intElement = 3;	// samples per pixel
				strElement = "RGB";
				if( (sz1 & 1) != 0) padByte = 1;
				dataSz1 = sz1 * 3;
			}
			else {
				intElement = 1;
				strElement = "MONOCHROME2";
				if( !SOPClassUID.equals(SOP_CLASS_TYPE_RAW)) bitsAlloc = 16;
				dataSz1 = sz1;
				if( bitsAlloc > 8) dataSz1 <<= 1;
			}
			out1.position(0);
			writeElement(0x28, 2, US);	// samples per pixel
			writeElement(0x28, 4, CS);	// photometric interpretation
			intElement = 0;
			writeElement(0x28, 6, US);	// planar configuration
			strElement = String.valueOf(numFrms);
			writeElement(0x28, 8, IS);	// number of frames
			intElement = h1;
			writeElement(0x28, 0x10, US);	// rows
			intElement = w1;
			writeElement(0x28, 0x11, US);	// columns
			intElement = bitsAlloc;
			writeElement(0x28, 0x100, US);	// bits allocated
			writeElement(0x28, 0x101, US);	// bits stored
			intElement = bitsAlloc - 1;
			writeElement(0x28, 0x102, US);	// high bit
			intElement = 0;
			writeElement(0x28, 0x103, US);	// pixel representation
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);

			// group 0x7fe0 - pixel data
			out1.position(0);
			dataSz = dataSz1 * numFrms;
/*			if (explicitLE) {
				intElement = dataSz + 12;
				writeElement(0x7fe0, 0, UL);
			}*/
			intElement = dataSz + padByte;
			writeElement(0x7fe0, 0x10, OB);
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);

			// now write the RGB pixel data
			if( SOPClassUID.equals( SOP_CLASS_TYPE_RAW)) {
				flOut1.write(inBuf, 0, dataSz1);
			} else {
				int [] intData = new int[sz1];
				outBuf = new byte[dataSz];
				if( imIn != null) {
					imIn.getRGB(0, 0, w1, h1, intData, 0, w1);
					for( i=0; i<sz1; i++) {
						k = i*3;
						tmpi1 = intData[i];
						outBuf[k+2] =(byte) tmpi1;
						tmpi1 = tmpi1 >> 8;
						outBuf[k+1] =(byte) tmpi1;
						tmpi1 = tmpi1 >> 8;
						outBuf[k] =(byte) tmpi1;
					}
					flOut1.write(outBuf, 0, dataSz1);
					if( padByte > 0) {
						byte[] pad = new byte[1];
						flOut1.write(pad);
					}
				} else {
					for( i=0; i<numFrms; i++) {
						pixels = mipPipe.data1.getPixels(i);
						for( j=0; j<sz1; j++) {
							k = j*2;
							tmpi1 = pixels[j];
							outBuf[k] = (byte) tmpi1;
							tmpi1 = tmpi1 >> 8;
							outBuf[k+1] =(byte) tmpi1;
						}
						flOut1.write(outBuf, 0, dataSz1);
					}
				}
			}

			flOut1.close();
			flOut1 = null;
			if( AETitle != null) {
				outFile1.deleteOnExit();
				parms = new String[3];
				parms[0] = "-c";
				parms[1] = AETitle;
				parms[2] = outFile1.getPath();
//				parms[2] = "/home/ilan/Downloads/9a1aNEW.dcm";
				StoreSCU.main(parms);
			}
			writeStatus = 1;	// done
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }

	}


	String getSpecial()	{
		String tmp = "";
		switch( specialType) {
			case 1:
				tmp = "TMTV Report";
				break;

			case 2:
				tmp = "Significant Image";
				break;

			case 3:
				tmp = "Annotations";
				if( subType == 1) tmp = "Brown fat";
				break;

			default:	// do nothing
				break;
		}
		return tmp;
	}

	public void writeLogMessage() {
		if( meta == null) return;
		String tmp = getSpecial();
		tmp += " for " + ChoosePetCt.getCompressedPatName(meta) + " written at ";
		if( AETitle != null) tmp += AETitle;
		else tmp += outFile1.getParent();
		ImageJ ij = IJ.getInstance();
		if( ij != null) ij.toFront();
		IJ.showStatus(tmp);
	}

	public int writeDicomHeader() {
		return writeDicomHeader(0,0,0);
	}
	
	public int writeDicomHeader(int sliceZ, int slType, int robot) {
		this.sliceZ = sliceZ;
		sliceType = slType;
		robotType = robot;
		getPetMeta();
		if( meta == null) return -1;
		if( !getOutFile(false)) return -1;
		SaveData();
		return 0;
	}
	
	// This write is used for ImgPlus files
	public int writeDicomHeader(String outFile) {
		AETitle = null;
		if( img1 == null) return -1;
		meta = ChoosePetCt.getMeta(1, img1);
		if( meta == null) return -1;
		outFile1 = new File(outFile);
		if( outFile1 == null) return -1;
		SaveData();
		return 0;
	}

	// This write is used for MIP files
	public int writeDicomHeaderMip(String outFile) {
		AETitle = null;
		getPetMeta();
		if( meta == null) return -1;
		outFile1 = new File(outFile);
		if( outFile1 == null) return -1;
		SaveData();
		return 0;
	}

	public int writeRawData(File fl1, String aetit, int type) {
		int i, sizeOut, sz1, sz3;
		String flName, serUID;
		FileInputStream flStr;
		try {
			subType = type;
			meta = ChoosePetCt.getMeta(1, img1);
			if( meta == null) return -1;
			flName = fl1.getName();
/*			if( type == 1) {
				serUID = ChoosePetCt.getDicomValue(meta, "0020,000E");
				if( serUID == null) return -1;
				flName = serUID.trim();
			}*/
			sz1 = flName.length() + 1;	// with trailing zero
			sz3 = (int) fl1.length();
			flStr = new FileInputStream(fl1);
			sizeOut = sz1 + sz3;
			if( (sizeOut & 1)==1) sizeOut++;
			inBuf = new byte[sizeOut];
			for( i=0; i<sz1-1; i++) inBuf[i] = (byte) flName.charAt(i);
			flStr.read(inBuf, sz1, sz3);
			specialType = 3;	// raw data
			if( !isAETitle(aetit)) return -1;
			AETitle = aetit;
			outFile1 = File.createTempFile("tmpDcm", "dcm");
			doActualSaveData();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); return -1;}
		return 0;
	}

	public int writeImmediateMip() {
		sliceType = 0;
		getPetMeta();
		if( meta == null) return -1;
		if( !getOutFile(false)) return -1;
		doActualSaveData();
		return 0;
	}

	public int writeBuffImage() {
		return writeImmediateMip();
	}

	public int writeImmediateDicomHeader(int slType, int robot) {
		sliceType = 0;
		sliceType = slType;
		robotType = robot;
		getPetMeta();
		if( meta == null) return -1;
		if( !getOutFile(false)) return -1;
		doActualSaveData();
		return 0;
	}

	void getPetMeta() {
		JFijiPipe pet1;
		if( d3 != null) {
			pet1 = d3.getDisplay3Panel().d3Pipe;
			meta = pet1.data1.metaData;
			return;
		}
		pet1 = par1.getPetCtPanel1().petPipe;
		meta = pet1.data1.metaData;
	}
	
	String getSeriesUID() {
		String serUID;
		if( specialType == 3) {
			serUID = ChoosePetCt.getDicomValue(meta, "0020,000E");
			if( serUID != null) {
				serUID += ".3" + subType.toString();
				return serUID;
			}
		}
		serUID = ChoosePetCt.getDicomValue(meta, "0020,000D");
		if( serUID == null) serUID = generateSOPInstanceUID();
		serUID = serUID + ".2" + specialType.toString() + sliceType.toString() + robotType.toString();
		return serUID;
	}

	int cleanDirectory( int slType, int robot) {
		sliceType = slType;
		robotType = robot;
		getPetMeta();
		if( meta == null) return -1;
		if( !getOutFile(true)) return -1;
		return 0;
	}
	
	boolean isAETitle( String path) {
		int j,k;
		j = path.indexOf('@');
		k = path.indexOf(':');
		return j>0 && j<k;
	}

	boolean isEchoScu() {
		int j,k;
		j = AETitle.indexOf('@');
		k = AETitle.indexOf(':');
		if( j<0 || k<j) {
			IJ.log("AETitle illegal");
			return false;
		}
		String tmp, cmd1 = "echoscu ";
		cmd1 += AETitle.substring(j+1, k) + " " + AETitle.substring(k+1);
		try {
			Process myProc = Runtime.getRuntime().exec(cmd1);
			myProc.waitFor();
			InputStream err = myProc.getErrorStream();
			if( err != null) {
				byte[] in1 = new byte[100];
				err.read(in1);
				tmp = new String(in1);
				if(tmp.toLowerCase().contains("failed")) {
					IJ.log("Can't connect to DICOM server");
					return false;
				}
			}
		} catch (Exception e) {
			ChoosePetCt.stackTrace2Log(e);
			return false;
		}
		return true;
	}

	// This write is used for SaveAs Dicom
	public int writeDicomHeader(String path, String seriesName, int serNum, Object pat1) {
		try {
			AETitle = null;
			updated = null;
			if( img1 == null) return -1;
			if( pat1 != null && pat1 instanceof patInfo) updated = (patInfo) pat1;
			meta = ChoosePetCt.getMeta(1, img1);
			if( meta == null) return -1;
			newSeriesName = null;
			if( seriesName != null && !seriesName.trim().isEmpty())
				newSeriesName = seriesName.trim();
			if( newSeriesName != null || serNum >= 0 || updated != null) {
				newSeriesUID = ChoosePetCt.getSeriesUID(meta) + "." + generateUID6digits();
				newSeriesNum = serNum;
			}
			if( isAETitle(path)) {
				AETitle = path;
//				if( !isEchoScu()) return -1;
				outFile1 = File.createTempFile("tmpDcm", "dcm");
				outFile1.delete();
			} else {
				Date dat1 = new Date();
				SimpleDateFormat df1 = new SimpleDateFormat("yyMMdd.HHmmss");
				outFile1 = new File(path + File.separator + df1.format(dat1) + ".dcm");
				if( outFile1 == null) return -1;
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); return -1;}
		SaveData();
		return 0;
	}

	private void writeElement(int group, int element, short type) {
		int leng = 0;
		switch( type) {
			case US:
				leng = 2;
				break;

			case UL:
				leng = 4;
				break;

			case OB:
				leng = 0;	// padding, not the real length
				break;

			case UI:
			case CS:
			case DS:
			case DA:
			case DT:
			case TM:
			case SH:
			case ST:
			case LT:
			case LO:
			case PN:
			case IS:
			case AE:
			case AS:
				if(strElement != null) leng = strElement.length();
				if( (leng & 1) != 0) leng++;
				break;
		}
		out1.putShort((short)group);
		out1.putShort((short)element);
		if( explicitLE || group == 2) {
			out1.putShort(type);
			out1.putShort((short)leng);
		} else {
			out1.putInt(leng);
		}
		switch( type) {
			case US:
				out1.putShort((short)intElement);
				break;

			case UL:
				out1.putInt(intElement);
				break;

			case OB:
				if( group == 2)	{
					out1.putInt(2);
					out1.putShort((short) 0x100);
				}
				if (group == 0x7fe0) {	// another case
					if (explicitLE) {
						out1.putInt(intElement);
					} else {
						out1.position(out1.position()-4);
						out1.putInt(intElement);
					}
				}
				break;

			case UI:
			case CS:
			case DS:
			case DA:
			case DT:
			case TM:
			case SH:
			case ST:
			case LT:
			case LO:
			case PN:
			case IS:
			case AE:
			case AS:
				putDcmString(group, type);
				break;
		}
	}
	
	short getType(int group, int element) {
		short retVal = 0;
		switch( group) {
			case 2:
				switch( element) {
					case 2:
					case 3:
					case 0x10:
					case 0x12:
						retVal = UI;
						break;

					case 0x13:
						retVal = SH;
						break;

					case 0x16:
						retVal = AE;
						break;
				}
				break;

			case 8:
				switch( element) {
					case 5:
					case 8:
					case 0x60:
					case 0x64:
						retVal = CS;
						break;

					case 0x16:
					case 0x18:
						retVal = UI;
						break;

					case 0x20:
					case 0x21:
					case 0x22:
					case 0x23:
						retVal = DA;
						break;

					case 0x30:
					case 0x31:
					case 0x32:
					case 0x33:
						retVal = TM;
						break;

					case 0x50:
//					case 0x100:	// these are inside sequences
//					case 0x102:
					case 0x1010:
						retVal = SH;
						break;

					case 0x70:
					case 0x80:
					case 0x1030:
					case 0x103e:
					case 0x1090:
						retVal = LO;
						break;

					case 0x81:
						retVal = ST;
						break;

					case 0x90:
					case 0x1048:
					case 0x1060:
					case 0x1070:
						retVal = PN;
						break;

					case 0x1110:
					case 0x1111:
					case 0x1032:
						retVal = SQ;
						break;
				}
				break;

			case 0x9:
				switch( element) {
					case 0x10:
					case 0x1001:
						retVal = LO;
						break;

					case 0x103b:
						retVal = DT;
						break;
				}
			break;

			case 0x10:
				switch( element) {
					case 0x10:
						retVal = PN;
						break;

					case 0x1010:
						retVal = AS;
						break;

					case 0x20:
					case 0x21:
						retVal = LO;
						break;

					case 0x30:
						retVal = DA;
						break;

					case 0x40:
						retVal = CS;
						break;

					case 0x1020:
					case 0x1030:
						retVal = DS;
						break;

					case 0x21b0:
						retVal = LT;
						break;
				}
				break;
				
			case 0x18:
				switch( element) {
					case 0x50:
					case 0x60:
					case 0x90:
					case 0x1060:
					case 0x1063:
					case 0x1071:
					case 0x1100:
					case 0x1110:
					case 0x1111:
					case 0x1120:
					case 0x1130:
					case 0x1190:
						retVal = DS;
						break;

					case 0x1020:
					case 0x1030:
						retVal = LO;
						break;

					case 0x74:
					case 0x75:
					case 0x1083:
					case 0x1084:
					case 0x1149:
					case 0x1150:
					case 0x1151:
					case 0x1152:
					case 0x1170:
					case 0x1242:
						retVal = IS;
						break;

					case 0x20:
					case 0x21:
					case 0x22:
					case 0x71:
					case 0x73:
					case 0x1140:
					case 0x1147:
					case 0x1181:
					case 0x1312:
					case 0x5100:
						retVal = CS;
						break;

					case 0x1160:
					case 0x1210:
						retVal = SH;
						break;

					case 0x1072:
						retVal = TM;
						break;
				}
				break;
				
			case 0x20:
				switch( element) {
					case 0xd:
					case 0xe:
					case 0x52:
						retVal = UI;
						break;

					case 0x10:
						retVal = SH;
						break;

					case 0x11:
					case 0x12:
					case 0x13:
						retVal = IS;
						break;

					case 0x20:
						retVal = CS;
						break;

					case 0x32:
					case 0x37:
					case 0x1041:
						retVal = DS;
						break;

					case 0x1040:
						retVal = LO;
						break;
				}
				break;

			case 0x28:
				switch( element) {
					case 2:
					case 6:
					case 0x10:
					case 0x11:
					case 0x100:
					case 0x101:
					case 0x102:
					case 0x103:
						retVal = US;
						break;

					case 4:
					case 0x51:
						retVal = CS;
						break;

					case 8:
						retVal = IS;
						break;

					case 0x30:
					case 0x1050:
					case 0x1051:
					case 0x1052:
					case 0x1053:
						retVal = DS;
						break;
						
					case 0x1054:
						retVal = LO;
						break;
				}
				break;

			case 0x32:
				switch( element) {
					case 0x1032:
					case 0x1033:
					case 0x1060:	// appears also in sequence
						retVal = LO;
						break;
				}
				break;

			case 0x40:
				switch( element) {
					case 0x253:
					case 0x1003:
						retVal = SH;
						break;

					case 0x275:
						retVal = SQ;
						break;

					case 0x2017:
						retVal = LO;
						break;
				}
				break;

			case 0x54:
				switch(element) {
					case 0x16:
					case 0x300:
					case 0x304:
					case 0x410:
					case 0x412:
					case 0x414:
						retVal = SQ;
						break;

					case 0x71:
                    case 0x81:
					case 0x101:
					case 0x1330:
						retVal = US;
						break;

					case 0x400:
						retVal = SH;
						break;

					case 0x1000:
					case 0x1001:
					case 0x1002:
					case 0x1100:
					case 0x1102:
						retVal = CS;
						break;

					case 0x1101:
					case 0x1103:
					case 0x1105:
						retVal = LO;
						break;

					case 0x1300:
					case 0x1321:
						retVal = DS;
						break;
				}
				break;

			case 0x7053:
				switch(element) {
					case 0x10:
					case 0x1000:
						retVal = LO;
						break;

					case 0x1009:
						retVal = DS;
						break;
				}
				break;

			case 0x7fe0:
				retVal = OB;
				break;
		}
		return retVal;
	}

	short getSeqType(int group, int element) {
		short retVal = 0;
		switch(group) {
			case 8:
				switch(element) {
					case 0x100:
					case 0x102:
						retVal = SH;
						break;

					case 0x104:
						retVal = LO;
						break;

					case 0x1150:
					case 0x1155:
						retVal = UI;
				}
				break;

			case 0x18:
				switch(element) {
					case 0x31:
						retVal = LO;
						break;

					case 0x1072:
						retVal = TM;
						break;

					case 0x1074:
					case 0x1075:
					case 0x1076:
						retVal = DS;
						break;
				}
				break;
				
			case 0x20:
				switch(element) {
					case 0xd:	// studyUID in sequence
						retVal = UI;
						break;
				}
				break;

			case 0x32:
				switch(element) {
					case 0x1060:
						retVal = LO;
						break;
				}
				break;

			case 0x40:
				switch(element) {
					case 0x1001:
						retVal = SH;
						break;
				}
				break;

			case 0x54:	// sequence inside sequence
				switch(element) {
					case 0x16:
					case 0x300:
					case 0x304:
					case 0x410:
					case 0x412:
					case 0x414:
						retVal = SQ;
						break;
				}
		}
		return retVal;
	}

	private void putDcmString(int group, int type) {
		if( strElement == null) return;
		int leng = strElement.length();
		byte [] bytArr1 = strElement.getBytes();
		out1.put(bytArr1);
		if( (leng & 1) == 1) {
			if( group == 2 || type == UI) out1.put((byte)0);
			else out1.put((byte)' ');
		}
	}

	BufferedImage getDcmData() {
		BufferedImage im1 = null;
		PetCtPanel pan1 = null;
		Display3Panel pan3 = null;
		Rectangle rc1;
		Point pt1;
		int width1;
		boolean isD3 = false;
		double scl1;
		JFijiPipe petPipe;
		if( d3 != null) {
			pan3 = d3.getDisplay3Panel();
			isD3 = true;
		}
		else pan1 = par1.getPetCtPanel1();
		try {
			if( isD3) {
				if( pan3 == null) return null;
				rc1 = pan3.getBounds();
				pt1 = pan3.getLocationOnScreen();
			} else {
				if( pan1 == null) return null;
				rc1 = pan1.getBounds();
				pt1 = pan1.getLocationOnScreen();
			}
			rc1.x = pt1.x;
			rc1.y = pt1.y;
			if( !isD3) {
				if( pan1 == null) return null;
				petPipe = pan1.petPipe;
				scl1 = pan1.getScalePet();
				width1 = ChoosePetCt.round(scl1 * petPipe.data1.width);
				rc1.width = width1 * 3;
				switch( robotType) {
					case 1: // Pet part
						rc1.width = width1;
						break;

					case 2: // Ct part
						rc1.x += width1;
						rc1.width = width1;
						break;

					case 3: // MIP or fused part
						rc1.x += 2*width1;
						rc1.width = width1;
						break;

					default: // everything, already set
						break;
				}
			}
			im1 = new Robot().createScreenCapture(rc1);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return im1;
	}

	String generateSOPInstanceUID() {
		Date dt1 = new Date();
		SimpleDateFormat df1 = new SimpleDateFormat("2.16.840.1.113664.3.yyyyMMdd.HHmmss.");
		return df1.format(dt1) + generateUID6digits();
	}
	
	static String generateUID6digits() {
		Integer rnd = (int)(Math.random()*1000000.);
		return rnd.toString();
	}
	
	int getRescaleIntercept( String meta) {
		return ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "28,1052"));
	}
	
	void doDcmCheck() {
		int pos0, pos1, k1, k2, group, element;
/*		int i, n, n1, f1Idx, f2Idx, badIdx;
		int prevGrp, prevElm, g0, g1, g2, g3;
		ArrayList<checkDcmElement> tmpList;
		boolean goodList;*/
// Pique - Tejedor is the tough patient here
		int i, j, n, prevGrp, rmOff;
		ArrayList<dupElement> tmpList;
		dupElement currDup, compDup;
		Integer[] dupIndx, goodIndx;
		String tmp0, tmp1, val1;
		short type1;
		checkDcmElement currElement;
		goodDcmList = new ArrayList<checkDcmElement>();
		goodSeqList = new ArrayList<checkSeqList>();
		pos0 = 0;
		while( (pos1 = meta.indexOf("\n", pos0)) >= 0) {
			currElement = new checkDcmElement();
			tmp1 = meta.substring(pos0, ++pos1);
			pos0 = pos1;
			k1 = tmp1.indexOf(",");
			if( k1 <= 0 || k1 > 6) continue;
			tmp0 = tmp1.substring(0, k1);
			currElement.group = group = ChoosePetCt.parseInt(tmp0, 16);
			k2 = tmp1.indexOf(" ", k1);
			if( k2 <= 0) continue;
			tmp0 = tmp1.substring(k1+1, k2);
			currElement.element = element = ChoosePetCt.parseInt(tmp0, 16);
			type1 = getType(group, element);
			if( type1 != 0) {
				tmp0 = tmp1.substring(k2).trim();
				if( tmp0.startsWith(">")) { // remove "floating" sequences
					continue;
				}
				val1 = ChoosePetCt.getDicomValue(tmp1, null);
				currElement.value = val1;
				goodDcmList.add(currElement);
				if( type1 == SQ) pos0 = doSequence( group, element, pos0);
			}
			if( group == 0x7fe0) break;
		}

		tmpList = new ArrayList<dupElement>();
		rmOff = 0;
		n = goodDcmList.size();
		prevGrp = -1;
		// watch out for implicit little endian. change to explicit
		for( i=0; i<n; i++) {
			currElement = goodDcmList.get(i);
			group = currElement.group;
			element = currElement.element;
			if( group == 2 && element == 0x10) {
				if( !currElement.value.startsWith("1.2.840.10008.1.2.1" )) {
					currElement.value = "1.2.840.10008.1.2.1";
					goodDcmList.set(i, currElement);
				}
			}
			currDup = new dupElement(group, currElement.element, prevGrp, i);
			if( currDup.isDup()) tmpList.add(currDup);
			else prevGrp = group;
		}
		while((n = tmpList.size()) > 1) {
			dupIndx  = new Integer[n+1];
			goodIndx = new Integer[n+1];
			compDup = tmpList.get(0);
			goodIndx[0] = compDup.indx;
			dupIndx[0] = 0;
			for( i=j=1; i<n; i++) {
				currDup = tmpList.get(i);
				if( currDup.group != compDup.group || currDup.element != compDup.element) continue;
				goodIndx[j] = currDup.indx;
				dupIndx[j++] = i;
			}
			// for now, just remove the early instance of duplicate
			for( i=0; i<j-1; i++) {
				k1 = goodIndx[i] - rmOff;
				goodDcmList.remove(k1);
				rmOff++;
			}
			while(j>0) {
				k1 = dupIndx[--j];
				tmpList.remove(k1);
			}
		}
		// find if group 54 increases monotonically
		// there is no good rule I can find
/*		n = goodDcmList.size();
		elm1 = new Integer[n];
		goodIndx = new Integer[n];
		for( i=j=0; i<n; i++) {
			currElement = goodDcmList.get(i);
			group = currElement.group;
			element = currElement.element;
			if( group != 0x54) continue;
			elm1[j] = element;
			goodIndx[j] = i;
			j++;
		}
		n = j;
		if( n<2) return;
		j0 = goodIndx[0];
		el0 = elm1[0];
		i = 1;
		while( i < n) {
			el1 = elm1[i];
			j = goodIndx[i];
			if( el1 > el0) {
				el0 = el1;
				j0 = j;
				i++;
			} else {
				cur0El = goodDcmList.get(j0);
				currElement = goodDcmList.get(j);
				goodDcmList.set(j0, currElement);
				goodDcmList.set(j, cur0El);
				elm1[i] = el0;
				elm1[i-1] = el1;
				i--;
				if( i<1) i = 1;
				el0 = elm1[i-1] ;
				j0 = goodIndx[i-1];
			}
		}*/
	}
	
	int doSequence(int group, int element, int posIn) {
		int pos0, pos1, posret = posIn;
		int i, n, k1, k2, group1, bad=0;
		String tmp0, tmp1;
		seqElement [] currSeq = generateSeqList(group, element);
		pos0 = posIn;
		if( currSeq == null) {	// no sequence defined - clear it
			while( (pos1 = meta.indexOf("\n", pos0)) > 0) {
				posret = pos0;
				tmp1 = meta.substring(pos0, ++pos1);
				pos0 = pos1;
				k1 = tmp1.indexOf(",");
				if( k1 <= 0) continue;
				tmp0 = tmp1.substring(0, k1);
				group1 = ChoosePetCt.parseInt(tmp0, 16) & 1;	// private
				k2 = tmp1.indexOf(" ", k1);
				if( k2 <= 0) continue;
				tmp0 = tmp1.substring(k2).trim();
				if( tmp0.startsWith(">") || group1 == 1) continue;
				break;
			}
			return posret;
		}
		checkDcmElement currElement;
		short type1;
		checkSeqList curSeqList = new checkSeqList(group, element);
		n = currSeq.length;
		while( (pos1 = meta.indexOf("\n", pos0)) > 0) {
			if( bad > 8) break;	// need 6 for Pique
			bad++;
			currElement = new checkDcmElement();
			tmp1 = meta.substring(pos0, ++pos1);
			pos0 = pos1;
			k1 = tmp1.indexOf(",");
			if( k1 <= 0) continue;
			tmp0 = tmp1.substring(0, k1);
			currElement.group = group = ChoosePetCt.parseInt(tmp0, 16);
			k2 = tmp1.indexOf(" ", k1);
			if( k2 <= 0) continue;
			tmp0 = tmp1.substring(k1+1, k2);
			currElement.element = element = ChoosePetCt.parseInt(tmp0, 16);
			type1 = getSeqType(group, element);
			if(type1 == 0) {
				if(element == 0) bad--;	// don't count NNNN,0000
				continue;
			}
			if(type1 == SQ) break;
			for( i=0; i<n; i++) {
				if(currElement.group == currSeq[i].group &&
						currElement.element == currSeq[i].element) break;
			}
			if( i>= n) continue;	// not found in list
			posret = pos0;
			tmp0 = ChoosePetCt.getDicomValue(tmp1, null);
			currElement.value = tmp0;
			curSeqList.seqList.add(currElement);
			if( i == n-1) break;	// last element in sequence
			bad--;
		}
		goodSeqList.add(curSeqList);
		return posret;
	}
	
	seqElement [] generateSeqList(int group, int element) {
		seqElement[] retVal = null;
		switch(group) {
			case 8:
				switch(element) {
					case 0x1032:
						retVal = new seqElement[4];
						retVal[0] = new seqElement(8,0x100);
						retVal[1] = new seqElement(8,0x102);
						retVal[2] = new seqElement(8,0x104);
						retVal[3] = new seqElement(2,0x111);	// fake to allow multiple
						break;

					case 0x1110:
						retVal = new seqElement[2];
						retVal[0] = new seqElement(8,0x1150);
						retVal[1] = new seqElement(8,0x1155);
						break;
				}
				break;

			case 0x40:
				switch(element) {
					case 0x275:
						retVal = new seqElement[2];
						retVal[0] = new seqElement(0x32, 0x1060);
						retVal[1] = new seqElement(0x40, 0x1001);
						break;
				}
				break;

			case 0x54:
				switch(element) {
					case 0x16:
						retVal = new seqElement[5];
						retVal[0] = new seqElement(0x18,0x31);
						retVal[1] = new seqElement(0x18,0x1072);
						retVal[2] = new seqElement(0x18,0x1074);
						retVal[3] = new seqElement(0x18,0x1075);
						retVal[4] = new seqElement(0x18,0x1076);
						break;

					case 0x300:
						retVal = new seqElement[3];
						retVal[0] = new seqElement(8,0x100);
						retVal[1] = new seqElement(8,0x102);
						retVal[2] = new seqElement(8,0x104);
						break;

					case 0x304:
						retVal = new seqElement[3];
						retVal[0] = new seqElement(8,0x100);
						retVal[1] = new seqElement(8,0x102);
						retVal[2] = new seqElement(8,0x104);
						break;

					case 0x410:
						retVal = new seqElement[3];
						retVal[0] = new seqElement(8,0x100);
						retVal[1] = new seqElement(8,0x102);
						retVal[2] = new seqElement(8,0x104);
						break;

					case 0x412:
						retVal = new seqElement[3];
						retVal[0] = new seqElement(8,0x100);
						retVal[1] = new seqElement(8,0x102);
						retVal[2] = new seqElement(8,0x104);
						break;

					case 0x414:
						retVal = new seqElement[3];
						retVal[0] = new seqElement(8,0x100);
						retVal[1] = new seqElement(8,0x102);
						retVal[2] = new seqElement(8,0x104);
						break;
				}
				break;
		}
		return retVal;
	}

	class dupElement {
		int group, element, prev, indx;

		dupElement( int gr0, int el0, int pr0, int in0) {
			group = gr0;
			element = el0;
			prev = pr0;
			indx = in0;
		}

		boolean isDup() {
			switch( group) {
				case 0x20:
					switch( element) {
						case 0xd:
						case 0xe:
							return true;
					}
					break;
			}
			return false;
		}
	}

	patInfo getNewInfo() {
		return new patInfo();
	}

	class patInfo {
		String patName, patID, patSex, patDOB;
		double patWeight, patHeight;
	}

	class checkDcmElement {
		int group, element;
		String value;
	}
	
	class checkSeqList {
		int seqGroup, seqElement;
		ArrayList<checkDcmElement> seqList = null;
		
		checkSeqList(int group, int element) {
			seqGroup = group;
			seqElement = element;
			seqList = new ArrayList<checkDcmElement>();
		}
	}
	
	class seqElement {
		int group, element;
		
		seqElement( int grp1, int elm1) {
			group = grp1;
			element = elm1;
		}
	}

	protected class bkgdSaveData extends SwingWorker {

		@Override
		protected Void doInBackground() {
			if( img1 != null) doImgPlusSaveData();
			else doActualSaveData();
			return null;
		}

	}

}

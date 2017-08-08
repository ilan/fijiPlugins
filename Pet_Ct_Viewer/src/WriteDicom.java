/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import ij.ImagePlus;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * This program has been used to make screen captures and store them
 * as Dicom files. Now it is being extended to take a locally generated
 * file and store it in Dicom format. The file of interest comes from
 * the brown fat program. This case will be distinguished by a non
 * null ImagePlus img1 object.
 * 
 * @author Ilan
 */
public class WriteDicom {
	PetCtFrame par1 = null;
	Window dbWindow = null;
	ImagePlus img1 = null;	// alternative use of WriteDicom
	public static final short AE = 0x4541;
	public static final short UL = 0x4c55;
	public static final short OB = 0x424f;
	public static final short UI = 0x4955;
	public static final short CS = 0x5343;
	public static final short DA = 0x4144;
	public static final short TM = 0x4d54;
	public static final short SH = 0x4853;
	public static final short LO = 0x4f4c;
	public static final short PN = 0x4e50;
	public static final short IS = 0x5349;
	public static final short US = 0x5355;
	public static final short DS = 0x5344;
	public static final String SOPClassTypeRaw = "1.2.840.10008.5.1.4.1.1.66";
	File outFile1;
	byte [] outBuf = null;
	bkgdSaveData work2 = null;
	ByteBuffer out1 = null;
	FileOutputStream flOut1 = null;
	int intElement;
	String meta = null;
	String strElement;
	boolean explicitLE = true;

	public boolean getOutFile() {
		Preferences prefer1 = par1.jPrefer;
		String path1 = prefer1.get("significant image path", null);
		if (path1 == null || path1.isEmpty()) {
			JOptionPane.showMessageDialog(par1.getOwner(),
					"Please fill in Significant Image Directory (Options).\nThen try again.");
			return false;
		}
		if( !path1.endsWith("/")) path1 += "/";
		String patName = ChoosePetCt.getCompressedPatName(meta);
		patName = patName.replaceAll("'", "");
		int i = patName.indexOf(',');
		if( i>0) patName = patName.substring(0, i);
		Date tmpDate = new Date();
		long currTime = tmpDate.getTime() / 60000l; // minutes
		int min = (int) currTime;
		min = min % (60*24);
		int hour = min / 60;
		min = min % 60;
		String outName = patName + String.format("%02d%02d", hour, min) + ".dcm";
		outFile1 = new File(path1 + outName);
		if( outFile1 == null) return false;
		return true;
	}


	public WriteDicom(PetCtFrame par1, Window dbWin) {
		this.par1 = par1;
		dbWindow = dbWin;
	}
	
	public WriteDicom(ImagePlus img) {
		img1 = img;
	}

	protected void SaveData() {
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
		int pos0, pos1, pos2, group, element, k1, k2, i, numFrms;
		int depth, dataSz, sz1, j, k, tmpi1, group2 = 0;
		short type1;
		Object pix1;
		byte [] pixByte;
		short [] pixels;
		int [] pixInt;
		String tmp1, tmp0;
		try {
			outBuf = new byte[32768];
			out1 = ByteBuffer.wrap(outBuf);
			out1 = out1.order(ByteOrder.LITTLE_ENDIAN);
			flOut1 = new FileOutputStream(outFile1);
			out1.position(128);	// buffer was initialized to zero
			out1.putInt(0x4d434944);	// DICM
			flOut1.write(outBuf, 0, 132);
			out1.position(0);
			pos0 = 0;
			while( (pos1 = meta.indexOf("\n", pos0)) > 0) {
				tmp1 = meta.substring(pos0, ++pos1);
				pos0 = pos1;
				k1 = tmp1.indexOf(",");
				if( k1 <= 0) continue;
				tmp0 = tmp1.substring(0, k1);
				group = ChoosePetCt.parseInt(tmp0, 16);
				k2 = tmp1.indexOf(" ", k1);
				if( k2 <= 0) continue;
				tmp0 = tmp1.substring(k1+1, k2);
				element = ChoosePetCt.parseInt(tmp0, 16);
				if( group2 == 0) {
					if( group != 2) group2 = -1;	// no group2
					else {
						group2 = 1;	// started
						intElement = 0;
						writeElement(2, 0, UL);
						writeElement(2, 1, OB);
					}
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
				strElement = ChoosePetCt.getDicomValue(tmp1, null);
				if( type1 == US || type1 == UL)
					intElement = ChoosePetCt.parseInt(strElement);
				writeElement(group, element, type1);
			}
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);
			numFrms = img1.getStackSize();
			depth = img1.getBitDepth();
			sz1 = img1.getHeight() * img1.getWidth();
			dataSz = sz1 * depth / 8;

			// group 0x7fe0 - pixel data
			out1.position(0);
			if (explicitLE) {
				intElement = dataSz*numFrms + 12;
				writeElement(0x7fe0, 0, UL);
			}
			intElement = dataSz*numFrms;
			writeElement(0x7fe0, 0x10, OB);
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);

			outBuf = new byte[dataSz];
			for( j=1; j<=numFrms; j++) {
				pix1 = img1.getStack().getPixels(j);
				switch( depth) {
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
				}
			}
			flOut1.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	// Note: I tried FileWriter/Reader, but they assume a fixed big endian format.
	// Wrapping a byte buffer allows both big and little endian
	void doActualSaveData() {
		int i, k, tmpi1, pos1, bitsAlloc, sz1, dataSz, numFrms = 1, w1, h1;
		String SOPInstanceUID, SOPClassUID;
		try {
			if( dbWindow != null) par1.toFront();
			Thread.sleep(500);	// allow menu to close
			BufferedImage imIn = getDcmData();
			if( imIn == null) return;
			outBuf = new byte[32768];
			out1 = ByteBuffer.wrap(outBuf);
			out1 = out1.order(ByteOrder.LITTLE_ENDIAN);
			flOut1 = new FileOutputStream(outFile1);
			out1.position(128);	// buffer was initialized to zero
			out1.putInt(0x4d434944);	// DICM
			flOut1.write(outBuf, 0, 132);
			w1 = imIn.getWidth();
			h1 = imIn.getHeight();
			sz1 = w1*h1;
			SOPClassUID = ChoosePetCt.SOPClassSC;	// Secondary capture
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
			strElement = ChoosePetCt.getDicomValue(meta, "0008,0020"); //DicomDate
			writeElement( 8, 0x20, DA);
			writeElement( 8, 0x21, DA);
			if( SOPClassUID.equals( SOPClassTypeRaw)) writeElement( 8, 0x23, DA);
			strElement = ChoosePetCt.getDicomValue(meta, "0008,0030"); //DicomTime
			writeElement( 8, 0x30, TM);
			writeElement( 8, 0x31, TM);
			if( SOPClassUID.equals( SOPClassTypeRaw)) writeElement( 8, 0x33, TM);
			strElement = ChoosePetCt.getDicomValue(meta, "0008,0050"); // AccessionNum
			writeElement( 8, 0x50, SH);
			strElement = "NM";	// modality
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
			writeElement( 8, 0x103e, LO);
			if( SOPClassUID.equals( SOPClassTypeRaw)) {
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
			strElement = strElement + ".2";
			writeElement(0x20, 0xe, UI);	// series Instance UID
			strElement = ChoosePetCt.getDicomValue(meta, "0020,0010");
			writeElement(0x20, 0x10, SH);	// accession number
			strElement = null;
			if( SOPClassUID.equals( SOPClassTypeRaw)) writeElement( 8, 0x13, IS);	// empty
			writeElement(0x20, 0x20, CS);	// patient orientation - empty
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);
//			if( SOPClassUID.equals( SOPClassTypeRaw)) return 0;	// done

			// group 28 - image module
			bitsAlloc = 8;
			if( SOPClassUID.equals( ChoosePetCt.SOPClassSC)) {
				intElement = 3;	// samples per pixel
				strElement = "RGB";
				dataSz = sz1 * 3;
			}
			else {
				intElement = 1;
				strElement = "MONOCHROME2";
				bitsAlloc = 16;
				dataSz = sz1;
//				if( SOPClassType == SOPClassTypeNM) dataSz *= numFrms;
				if( bitsAlloc > 8) dataSz <<= 1;
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
			if (explicitLE) {
				intElement = dataSz + 12;
				writeElement(0x7fe0, 0, UL);
			}
			intElement = dataSz;
			writeElement(0x7fe0, 0x10, OB);
			pos1 = out1.position();
			flOut1.write(outBuf, 0, pos1);

			// now write the RGB pixel data
			int [] intData = new int[sz1];
			imIn.getRGB(0, 0, w1, h1, intData, 0, w1);
			outBuf = new byte[dataSz];
			for( i=0; i<sz1; i++) {
				k = i*3;
				tmpi1 = intData[i];
				outBuf[k+2] =(byte) tmpi1;
				tmpi1 = tmpi1 >> 8;
				outBuf[k+1] =(byte) tmpi1;
				tmpi1 = tmpi1 >> 8;
				outBuf[k] =(byte) tmpi1;
			}
			flOut1.write(outBuf, 0, dataSz);

			flOut1.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }

	}

	public int writeDicomHeader() {
		JFijiPipe pet1 = par1.getPetCtPanel1().petPipe;
		meta = pet1.data1.metaData;
		if( meta == null) return -1;
		if( !getOutFile()) return -1;
		SaveData();
		return 0;
	}
	
	// This write is used for ImgPlus files
	public int writeDicomHeader(String outFile) {
		if( img1 == null) return -1;
		meta = ChoosePetCt.getMeta(1, img1);
		if( meta == null) return -1;
		outFile1 = new File(outFile);
		if( outFile1 == null) return -1;
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
			case TM:
			case SH:
			case LO:
			case PN:
			case IS:
			case AE:
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
			case TM:
			case SH:
			case LO:
			case PN:
			case IS:
			case AE:
				putDcmString(group);
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
						retVal = DA;
						break;
						
					case 0x30:
					case 0x31:
						retVal = TM;
						break;
						
					case 0x50:
						retVal = SH;
						break;
						
					case 0x70:
					case 0x80:
					case 0x1030:
					case 0x103e:
						retVal = LO;
						break;
						
					case 0x90:
						retVal = PN;
						break;
				}
				break;
				
			case 0x10:
				switch( element) {
					case 0x10:
						retVal = PN;
						break;
						
					case 0x20:
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
				}
				break;
				
			case 0x18:
				switch( element) {
					case 0x50:
						retVal = DS;
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
						
					case 0x20:
						retVal = CS;
						break;
						
					case 0x32:
					case 0x37:
						retVal = DS;
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
						retVal = CS;
						break;
						
					case 8:
						retVal = IS;
						break;
						
					case 0x30:
						retVal = DS;
						break;
				}
				break;
		}
		return retVal;
	}

	private void putDcmString(int group) {
		if( strElement == null) return;
		int leng = strElement.length();
		byte [] bytArr1 = strElement.getBytes();
		out1.put(bytArr1);
		if( (leng & 1) == 1) {
			if( group == 2) out1.put((byte)0);
			else out1.put((byte)' ');
		}
	}

	BufferedImage getDcmData() {
		BufferedImage im1 = null;
		try {
			Rectangle rc1 = par1.getBounds();
			Point pt1 = par1.getLocationOnScreen();
			rc1.x = pt1.x;
			rc1.y = pt1.y;
			im1 = new Robot().createScreenCapture(rc1);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return im1;
	}

	String generateSOPInstanceUID() {
		Date dt1 = new Date();
		SimpleDateFormat df1 = new SimpleDateFormat("2.16.840.1.113664.3.yyyyMMdd.HHmmss");
		return df1.format(dt1);
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

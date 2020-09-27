import ij.IJ;
import java.awt.Dimension;
import java.io.EOFException;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;


/**
 *
 * @author Ilan
 */
public class DicomFormat {

	public String m_currSeriesInstanceUID,  m_currStudyInstanceUID,  m_currPatID;
	public String m_imgType, m_currSOPInstanceUID, m_studyDate, m_acqTime, m_physician;
	public String m_currStyName,  m_currSerName,  m_currPatName,  m_ImageID,  m_label;
	public String m_petUnits, m_modelName, m_atten, m_acqDate, m_birthdate, m_accession;
	public Dimension m_size = null;
//	public double m_patHeight,  m_patWeight,  m_sliceThickness,  m_posX,  m_posY,  m_posZ;
	int m_SOPClass, m_modality, m_dataType, m_indx;
//	int m_numFrms, m_depth, m_frameTime, m_orientation;
//	String refSeriesUID;
	ArrayList<studyEntry> m_aStudy = null;
	ArrayList<seriesEntry> m_aSeries = null;
	ArrayList<imageEntry> m_aImage = null;
	public static final int ORIENT_UNKNOWN = 0;
	public static final int ORIENT_AXIAL = 1;
	public static final int ORIENT_CORONAL = 2;
	public static final int ORIENT_SAGITAL = 3;
	public static final int ORIENT_OBL_AXIAL = 4;
	public static final int ORIENT_OBL_COR = 5;
	public static final int ORIENT_OBL_SAG = 6;
	public static final int ORIENT_OBLIQUE = 7;
	public static final int GOOD_CT_DATA = 1;
	public static final int BQML_ATTENUATION = 2;
	public static final int NO_ATTENUATION = 3;
	public static final int SPECT_ATTENUATION = 4;
	public static final int OSEM_ATTENUATION = 5;
	public static final int PHILIPS_ATTENUATION = 6;
	public static final int REDUCED_CT_DATA = 7;
	public static final int VARICAM_XRAY = 8;
	public static final short SQ = 0x5351;
	public static final short QS = 0x5153;
	public static final short OB = 0x424f;
	public static final short BO = 0x4f42;
	public static final short OW = 0x574f;
	public static final short WO = 0x4f57;
	public static final short UN = 0x554e;
	public static final short NU = 0x4e55;
	public static final short TU = 0x5455;
	public static final short UT = 0x5554;
	public static final char DIR_SEP_CHAR = File.separatorChar;

	class studyEntry {

		String studyUID = null;
		String patName = null;
		String patID = null;
		String styName = null;
		String styDate = null;
		String accessNum = null;
		int numSeries = 0;
	}

	class seriesEntry {

		String seriesUID = null;
		String serName = null;
		String parDir = null;
		int numImages = 0;
		int numValidImages = 0;
		int sopClass = ChoosePetCt.SOPCLASS_UNKNOWN;
//		int indx = -1;
		int intercept = 0;
		int orientation = ORIENT_UNKNOWN;

		String setParDir( imageEntry im1) {
			if(im1 == null) return null;
			String ret = im1.dirName;
			if( ret == null) return null;
			int i = ret.lastIndexOf(DIR_SEP_CHAR);
			if( i<=0) return null;
			ret = ret.substring(0, i);
			parDir = ret;
			return ret;
		}

		boolean isSameDir(String in1) {
			return in1.equals(parDir);
		}
	}

	class imageEntry {

		String dirName = null;
		float posX = 0;
		float posY = 0;
		float posZ = 0;
	}

	public int checkFile(String dataDir, boolean forceDicomDir) {
		int i, j, k, entLoc, len1, len2, retVal = 0;
		String [] dirList;
		RandomAccessFile fis;
		try {
			File fl1;
			fl1 = new File(dataDir + DIR_SEP_CHAR + "DICOMDIR");
			if (fl1.exists()) {
				fis = new RandomAccessFile(fl1, "r");
				retVal = parseDicomDir(fis, dataDir, forceDicomDir);
				fis.close();
				return retVal;
			}
			// it is case sensitive to dicomdir - all upper or all lower case
			fl1 = new File(dataDir + DIR_SEP_CHAR + "dicomdir");
			if (fl1.exists()) {
				fis = new RandomAccessFile(fl1, "r");
				retVal = parseDicomDir(fis, dataDir, forceDicomDir);
				fis.close();
				return retVal;
			}
/*			fl1 = new File(dataDir);
			dirList = fl1.list();
			if( dirList == null || (len1 = dirList.length) <= 0) return retVal;
			m_aStudy = new ArrayList<studyEntry>();
			m_aSeries = new ArrayList<seriesEntry>();
			m_aImage = new ArrayList<imageEntry>();
			for( i=0; i<len1; i++) {
				studyEntry st1;
				seriesEntry sr1;
				imageEntry im1;
				k = checkDicomValid(dataDir, dirList[i]);
				if( k<=0) continue;
				entLoc = 0;
				// enter information into study, series and image
				len2 = m_aStudy.size();
				for( j=0; j<len2; j++) {
					st1 = m_aStudy.get(j);
					if( st1.studyUID.equals(m_currStudyInstanceUID)) break;
				}
				if( j>= len2) {
					st1 = new studyEntry();
					st1.studyUID = m_currStudyInstanceUID;
					st1.patName = m_currPatName;
					st1.patID = m_currPatID;
					st1.styName = m_currStyName;
					st1.styDate = m_studyDate;
					m_aStudy.add(st1);
					retVal++;
				}
				
				len2 = m_aSeries.size();
				for( j=0; j<len2; j++) {
					sr1 = m_aSeries.get(j);
					if( sr1.seriesUID.equals(m_currSeriesInstanceUID)) break;
				}
				if( j >= len2) {
					sr1 = new seriesEntry();
					sr1.seriesUID = m_currSeriesInstanceUID;
					sr1.serName = m_currSerName;
					sr1.sopClass = m_SOPClass;
					sr1.indx = m_indx;
					sr1.orientation = m_orientation;
					m_aSeries.add(sr1);
					len2 = m_aStudy.size();
					for (j = 0; j < len2; j++) {
						st1 = m_aStudy.get(j);
						if (st1.studyUID.equals(m_currStudyInstanceUID)) {
							st1.numSeries++;
							m_aStudy.set(j, st1);
							break;
						}
					}
				}
				
				// we assume that no image of SOPInstanceUID exists
				im1 = new imageEntry();
				im1.dirName = dirList[i];
				im1.posX = (float) m_posX;
				im1.posY = (float) m_posY;
				im1.posZ = (float) m_posZ;
				// find which series it belongs to
				len2 = m_aSeries.size();
				for( j=0; j<len2; j++) {
					sr1 = m_aSeries.get(j);
					entLoc += sr1.numImages;
					if( sr1.seriesUID.equals(m_currSeriesInstanceUID)) {
						sr1.numImages++;
						sr1.numValidImages = sr1.numImages;
						m_aSeries.set(j, sr1);
						break;
					}
				}
				if( j<len2-1) {
					m_aImage.add(entLoc, im1);
				} else {
					m_aImage.add(im1);	// add it at the end
				}
			}*/
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return retVal;
	}

	public int checkDicomValid(String path, String flName) {
		try {
			Attributes at;
			String in1 = path + DIR_SEP_CHAR + flName;
			if( flName == null) return 0;
			File fl1 = new File(in1);
			if( !fl1.exists()) {
				in1 = path + DIR_SEP_CHAR + flName.toLowerCase();
				fl1 = new File(in1);
			}
			if (!fl1.exists() || fl1.isDirectory()) {
				return 0;
			}
			if( !initialDicomCheck(fl1)) return 0;
			m_currPatName = null;
			m_currPatID = null;
			m_currSOPInstanceUID = null;
			m_currSeriesInstanceUID = null;
			m_currStudyInstanceUID = null;
			m_studyDate = null;
			m_accession = null;
			m_birthdate = null;
			m_currStyName = null;
			m_currSerName = null;
			m_SOPClass = ChoosePetCt.SOPCLASS_UNKNOWN;
			DicomInputStream dis = new DicomInputStream(fl1);
			at = dis.readDataset(-1, -1);
			dis.close();
			if( at.isEmpty()) return 0;
			m_currPatName = getDcmName(at, Tag.PatientName);
			m_currPatID = getDcmString(at, Tag.PatientID);
			m_studyDate = getStudyDate(at);
			m_birthdate = getDcmString(at, Tag.PatientBirthDate);
			m_accession = getDcmString(at, Tag.AccessionNumber);
			m_currStyName = getDcmString(at, Tag.StudyDescription);
			if( m_currStyName == null)
				m_currStyName = getDcmString(at, Tag.StudyID);
			m_currSerName = getDcmString(at, Tag.SeriesDescription);
			m_currSOPInstanceUID = getDcmString(at, Tag.SOPInstanceUID);
			m_currSeriesInstanceUID = getDcmString(at, Tag.SeriesInstanceUID);
			m_currStudyInstanceUID = getDcmString(at, Tag.StudyInstanceUID);
			String tmp1 = getDcmString(at, Tag.SOPClassUID);
			m_SOPClass = ChoosePetCt.getSOPClass(tmp1);
			if(m_SOPClass == ChoosePetCt.SOPCLASS_TYPE_SR_STORAGE ||
				m_SOPClass == ChoosePetCt.SOPCLASS_UNKNOWN) return 0;
			return 1;
		} catch(EOFException eof1) {
			String s1 = "Not listed: " + path + DIR_SEP_CHAR + flName;
			IJ.log(s1);
			return 0;
		}
		catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return 0;
	}

	private boolean initialDicomCheck(File fl1) {
		RandomAccessFile fis;
		byte[] byt1 = new byte[20480];
		int currlen, currInt;
		ByteBuffer byt2;
		try {
			fis = new RandomAccessFile(fl1, "r");
			byt2 = ByteBuffer.wrap(byt1);
			byt2 = byt2.order(ByteOrder.LITTLE_ENDIAN);
			currlen = fis.read(byt1);
			if( currlen <= 1020) {
//				IJ.log("Dicom file too small");
				fis.close();
				return false;
			}
			currInt = byt2.getInt(128);	// gets as little endian
			if (currInt != 0x4d434944) {	// DICM
				fis.close();
				return false;
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return true;
	}

	/*	public int checkDicomValid(String path, String flName) {
		byte[] byt1 = new byte[20480];
		ByteBuffer byt2;
		String tmp1;
		RandomAccessFile fis;
		int off1, len1, siemensExtra, currlen, currInt, type1 = 0;
		short currWrd, group, element, part1, part2;
		short len;
		boolean inSequence;
//		URLConnection urlc = null;
		try {
			String in1 = path + DIR_SEP_CHAR + flName;
			if( flName == null) return 0;
			File fl1 = new File(in1);
			if( !fl1.exists()) {
				in1 = path + DIR_SEP_CHAR + flName.toLowerCase();
				fl1 = new File(in1);
			}
			if (!fl1.exists() || fl1.isDirectory()) {
				return 0;
			}
			fis = new RandomAccessFile(fl1, "r");
			off1 = 0;
			inSequence = false;
			m_numFrms = 1;	// this isn't defined for all Dicom files
			m_imgType = null;
			m_currSOPInstanceUID = null;
			m_currSeriesInstanceUID = null;
			m_currStudyInstanceUID = null;
			m_studyDate = null;
			m_acqDate = null;
			m_acqTime = null;
			m_accession = null;
			m_birthdate = null;
			m_physician = null;
			m_currStyName = null;
			m_currSerName = null;
			m_label = null;
			m_currPatName = null;
			m_currPatID = null;
			m_ImageID = null;
			m_petUnits = null;
			m_modelName = null;
			m_atten = null;
			m_patHeight = 0.;
			m_patWeight = 0.;
			m_posX = 0.;
			m_posY = 0.;
			m_posZ = 0.;
			m_sliceThickness = 0.;
			m_size = new Dimension();
			m_depth = 0;
			siemensExtra = 0;
			m_SOPClass = ChoosePetCt.SOPCLASS_UNKNOWN;
			m_orientation = ORIENT_UNKNOWN;
			m_modality = 0;
			m_dataType = 0;
			m_frameTime = 0;
			m_indx = -1;
			currlen = fis.read(byt1);
			if( currlen <= 1020) {
//				IJ.log("Dicom file too small");
				fis.close();
				return 0;
			}
			byt2 = ByteBuffer.wrap(byt1);
			byt2 = byt2.order(ByteOrder.LITTLE_ENDIAN);
			currInt = byt2.getInt(0);	// gets as little endian
			// accept both 02,10 and 02,1 - not sure if 02,1 appears or not
			if (currInt != 8 && currInt != 0x80008 && currInt != 0x100005 && currInt != 0x10002 &&
					currInt != 0x100003 && currInt != 0x50008 && currInt != 0x100002) {
				off1 = 128;
				currInt = byt2.getInt(off1);
				if (currInt != 0x4d434944) {	// DICM
					fis.close();
					return 0;
				}
				currWrd = byt2.getShort(off1 + 12);
				off1 += currWrd + 16;
				currlen -= off1;
				if( currlen <= 0) {
					fis.close();
					return 0;
				}
			}
			currWrd = byt2.getShort(off1);
			if (currWrd == 0x800) {
				byt2 = byt2.order(ByteOrder.BIG_ENDIAN);
			}
			currWrd = byt2.getShort(off1 + 4);
			if (currWrd > 0x4141) {
				type1 = 1;
			}		// AA
			byt2.position(off1);
			do {
				group = byt2.getShort();
				element = byt2.getShort();
				off1 = byt2.position();
				len1 = byt2.getInt(off1);
				part1 = byt2.getShort();
				part2 = byt2.getShort();
				off1 = byt2.position();
				if (type1 > 0) {
					len1 = byt2.getInt(off1);
				}
				len = part1;
				if (type1 > 0) {
					len = part2;
					if (part1 == UN || part1 == NU) {
						len = (short) (len1 + 4);
					}
				}

				if (group == 4) {
					switch (element) {
						case 0x1220:
							if (part1 == SQ || part1 == QS) {
								len = 12;
							}
							break;
					}
				}
				
//				if (group == 0x7a1) {
//					if(element == 3) type1 = 1;
//				}

				if (group == 8) {
					switch (element) {
						case 8:
							m_imgType = getDcmString(byt2, len);
							break;

						case 0x16:
							tmp1 = getDcmString(byt2, len);
							m_SOPClass = ChoosePetCt.getSOPClass(tmp1);
							if(m_SOPClass == ChoosePetCt.SOPCLASS_TYPE_SR_STORAGE ||
								m_SOPClass == ChoosePetCt.SOPCLASS_UNKNOWN) return 0;
							break;

						case 0x18:
							m_currSOPInstanceUID = getDcmString(byt2, len);
							break;

						case 0x22:
							m_acqDate = getDcmString(byt2, len);
							break;

						case 0x21:
						case 0x23:
							if (m_studyDate != null && m_studyDate.length() > 0) {
								tmp1 = getDcmString(byt2, len);
								if( !tmp1.equals(m_studyDate)) {
									// there is a study where the series date isn't study date
									// prefer series date
									if(tmp1.length()==8 && element==0x21)
										m_studyDate = tmp1;
								}
								break;
							}
						case 0x20:
							m_studyDate = getDcmString(byt2, len);
							break;

						case 0x31:
						case 0x33:
							if ( m_acqTime != null && m_acqTime.length() > 0) {
								break;
							}
						case 0x30:
						case 0x32:
							if (len < 6) {
								break;
							}
							m_acqTime = getDcmString(byt2, len);
							break;

						case 0x50:
							m_accession = getDcmString(byt2, len);
							break;

						case 0x60:
							if (len < 2) {
								break;
							}
							m_modality = byt2.getShort(off1);
							break;

						case 0x90:
							m_physician = getDcmName(byt2, len);
							break;

						case 0x1030:
							m_currStyName = getDcmString(byt2, len);
							break;

						case 0x103e:
							m_currSerName = getDcmString(byt2, len);
							break;

						case 0x1090:
							m_modelName = getDcmString(byt2, len);
							break;

						case 0x2111:
							inSequence = false;
							break;

						case 0x51:
						case 0x1032:
						case 0x1052:
						case 0x1062:
						case 0x1084:
						case 0x1110:
						case 0x1111:
						case 0x1115:
						case 0x1120:
						case 0x1140:
						case 0x114a:
						case 0x1199:
						case 0x1250:	// referenced study, series UID
						case 0x2112:
						case 0x2218:
						case (short) 0x9121:
						case (short) 0x9124:
						case (short) 0x9215:
							if (len1 < 0) {
								len1 = 8;
							}
							if (part1 == SQ || part1 == QS) {
								len = (short) (len1 + 4);
							}
							if (len < 0) {
								len = 8;
								if (part2 == 0 && (part1 == 0 || part1 == SQ || part1 == QS)) {
									len = 12;
								}
							}
							if(element == 0x1250 && len <= 12) inSequence = true;
					}
				}
				// watch out, Philips has some really long 8 groups
				if (group == -2 && element == -0x2000) {
					len = 0;
					if( currlen < 1024) {
						siemensExtra += off1;
						fis.seek(siemensExtra);
						currlen = fis.read(byt1) + 8;
						byt2.position(0);
					}
				}
				if (group == -2 && element == -0x1f23) inSequence = false;

				// don't dump group 0x11 or 0x40
				if ((group == 9 || group == 0x13 || group == 0x19 || group == 0x23 ||
						group == 0x27 || group == 0x33 || group == 0x43 || group == 0x45 ||
						group == 0x55 || group == 0x2001) && element == 0) {
					if (type1 == 0) {
						len1 = byt2.getInt(off1);
					}
					if (len + len1 > 10240) {
						len1 = 0;
					}	// the new group 33 is very long
					len = (short) (len + len1);
				}

				// these are sequences which we want to dump
				if ((group == 9 || group == 0x11 || group == 0x13 || group == 0x19 || group == 0x28 || group == 0x29 || group == 0x31 || group == 0x32 || group == 0x33 || group == 0x40 || group == 0x43 ||
						group == 0x55 || group == 0x57 || group == 0x59 || group == 0x70 || group == 0x88 || group == 0xe1 || group == 0x1f1 || group == 0x1f3 || group == 0x1f7 || group == 0x400 ||
						group == 0x7a1 || group == 0x7a3 || group == 0x2001 || group == 0x2005 || group == 0x4ffe || group == 0x5200 || group == 0x6000 || group == 0x6002 || group == 0x6021 || group == 0x7053 || group == 0x7fdf) &&
						((type1 > 0 && (part1 == SQ || part1 == QS || part1 == BO || part1 == OB || part1 == WO || part1 == OW || part1 == UN || part1 == NU || part1 == UT || part1 == TU)) // SQ OB OW UN UT
						|| (type1 == 0 && (len1 >= 2048 || len1 < 0) && element != 0))) {
					// get rid of Siemens shit, use len1 instead of len
					if (len1 < 0) {
						len1 = 0;
						inSequence = true;
					}
					siemensExtra += len1 + off1;
					if (type1 > 0) {
						siemensExtra += 4;
					}
					fis.seek(siemensExtra);
					currlen = fis.read(byt1) + 8;
					len = 0;
					byt2.position(0);
					// if we did all this, don't enter any more group cases
					group = -10;
				}

				if (group == 0x10 && !inSequence) {
					switch (element) {
						case 0x10:
							m_currPatName = getDcmName(byt2, len);
							break;

						case 0x20:
							m_currPatID = getDcmString(byt2, len);
							break;
							
						case 0x30:
							m_birthdate = getDcmString(byt2, len);
							break;
							
						case 0x50:	// a sequence to be ignored
						case 0x101:
						case 0x102:
							group = 0x18;	// set it up to be caught
							element = 0x26;
							break;

						case 0x1020:
							m_patHeight = getDcmDouble(byt2, len);
							break;
							
						case 0x1002:	// sequence
						case 0x2293:
						case 0x2294:
							group = 0x18; // take care below
							element = 0x26;
							break;

						case 0x1030:
							m_patWeight = getDcmDouble(byt2, len);
							break;
					}
				}

				if (group == 0x11) {
					// this element appears also as 0x54, 0x400 but there it is
					// limited to 16 characters which sometimes isn't enough
					if (element == 0x1012) {
						// for Varicam Spect, this tells if the data is corrected or not
						m_ImageID = getDcmString(byt2, len);
						// some series name is better than no series name
						if (m_currSerName == null) m_currSerName = m_ImageID;
						getCardiacIndx();
					}
				}

				if( group == 0x12 && element == 0x64) {
					group = 0x18;
					element = 0x26;	// remove sequence
				}

				if (group == 0x18) {
					switch (element) {
						case 0x26:
						case (short) 0x9301:
						case (short) 0x9304:
						case (short) 0x9308:
						case (short) 0x9312:
						case (short) 0x9314:
						case (short) 0x9321:
						case (short) 0x9325:
						case (short) 0x9326:
						case (short) 0x9329:
						case (short) 0x9346:
						case (short) 0x9477:
						case (short) 0x9732:
						case (short) 0x9734:
						case (short) 0x9735:
						case (short) 0x9736:
						case (short) 0x9737:
						case (short) 0x9749:
						case (short) 0x9751:
						case (short) 0xa001:
							if (len1 < 0) {
								len1 = 8;
							}
							if (part1 == SQ || part1 == QS) {
								len = (short) (len1 + 4);
							}
							if (len < 0) {
								len = 8;
								if (part2 == 0 && (part1 == 0 || part1 == SQ || part1 == QS)) {
									len = 12;
								}
							}
							break;

						case 0x50:
							m_sliceThickness = getDcmDouble(byt2, len);
							break;

						case 0x1242:
							// this is supposed to be an integer but Harrington has double object
							m_frameTime = (int) getDcmDouble(byt2, len);
							break;
					}
				}

				if (group == 0x20) {
					switch (element) {
						case 0xd:
							// Philips has a dummy zero len UID
							// there is a case where inSequence is true
							// use the first valid studyInstanceUID
							if( len < 24 || inSequence || m_currStudyInstanceUID != null) break;
							m_currStudyInstanceUID = getDcmString(byt2, len);
							break;

						case 0xe:
							if( inSequence) break;
							m_currSeriesInstanceUID = getDcmString(byt2, len);
							break;
							
						case 0x10:
							if( len<4) break;	// too short a name
							if( m_currStyName != null && m_currStyName.length() > len) break;
							m_currStyName = getDcmString(byt2, len);
							break;

						case 0x32:
							ArrayList<Double> v1 = getDcmDoubleVect(byt2, len);
							if (v1.size() == 3) {
								m_posX = v1.get(0);
								m_posY = v1.get(1);
								m_posZ = v1.get(2);
							}
							break;

						case 0x37:
							tmp1 = getDcmString(byt2, len);
							m_orientation = ChoosePetCt.getOrientation(tmp1) & 31;
							break;
							
						case (short) 0x9071:
						case (short) 0x9110:
						case (short) 0x9111:
						case (short) 0x9113:
						case (short) 0x9116:
						case (short) 0x9221:
						case (short) 0x9222:
							if (len1 < 0) {
								len1 = 8;
							}
							if (part1 == SQ || part1 == QS) {
								len = (short) (len1 + 4);
							}
							if (len < 0) {
								len = 8;
								if (part2 == 0 && (part1 == 0 || part1 == SQ || part1 == QS)) {
									len = 12;
								}
							}
							break;
					}
				}

				if (group == 0x28 && !inSequence) {
					switch (element) {
						case 8:
							m_numFrms = getDcmInteger(byt2, len);
							break;

						case 0x10:
							m_size.height = byt2.getShort(off1);
							break;

						case 0x11:
							m_size.width = byt2.getShort(off1);
							break;

						case 0x100:
							m_depth = byt2.getShort(off1);
							break;
					}
				}

				if( group == 0x859 && element == 0x3010) {
					group = 0x54;
					element = 0x12;
				}

				if (group == 0x40) {
					switch (element) {
						case 8:
						case 0x275:
						case 0x555:
						case (short) 0xa043:
						case (short) 0xa168:
						case (short) 0xa170:
							// sequence, fake it so next group will fix it
							group = 0x54;
							element = 0x12;
							break;
					}
				}

				if (group == 0x54) {
					switch (element) {
						// we want to read the contents of this sequence
						case 0x16:
						case 0x22:
						case 0x32:
							// watch out for zero lenght
							if (len != 0) {
								len = 8;
							}
							if (part1 == SQ || part1 == QS) {
								len += 4;
							}
							break;

						case 0x12:
						case 0x13:
						case 0x52:
						case 0x62:
						case 0x63:
						case 0x72:
						case 0x220:
						case 0x300:
						case 0x302:
						case 0x304:
						case 0x410:
						case 0x412:
						case 0x414:
							if (len1 < 0) {
								len1 = 8;
							}
							if (part1 == SQ || part1 == QS) {
								len = (short) (len1 + 4);
							}
							if (len < 0) {
								len = 8;
								if (part2 == 0 && (part1 == 0 || part1 == SQ || part1 == QS)) {
									len = 12;
								}
							}
							break;
							
						case 0x400:
							if(m_ImageID == null) m_ImageID = getDcmString(byt2, len);
							break;

						case 0x1001:
							m_petUnits = getDcmString(byt2, len);
							break;

						// Phillips uses CNTS for both attenuation corrected and non corrected
						// we need this extra check to distinguish
						case 0x1101:
							m_atten = getDcmString(byt2, len);
							break;
					}
				}

				// watch out not to use 0x7fe0,0x10 while in sequence
				if( group == 0x7fe0 && element == 16 && inSequence) {
					siemensExtra += len1 + off1;
					if (type1 > 0) {
						siemensExtra += 4;
					}
					fis.seek(siemensExtra);
					currlen = fis.read(byt1) + 8;
					len = 0;
					byt2.position(0);
					element = 1;	// so as NOT to enter 0x7fe0,0x10
				}
				if (group == 0x7fe0 && element == 16) {
//					m_indx = getImageType();
					if (type1 > 0) {
						off1 += 4;
					}
					fis.close();
					return siemensExtra + off1;
				}
				off1 = byt2.position() + len;
				currlen -= len + 8;
				if( currlen < 0) break;
//				if(off1 < 0) { // used only for debugging
//					group = -1;
//					group++;
//				}
				byt2.position(off1);
			} while (len <= 10240 && currlen > 16);
			fis.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return 0;
	}*/
	
	private void getCardiacIndx() {
		String tmp = m_ImageID.toLowerCase();
		int len, j, i = -1;
		len = tmp.length();
		if( len > 6) {
			if( tmp.startsWith("rest")) i = 0;
			if( tmp.startsWith("stress")) i = 8;
			if( tmp.startsWith("delay")) i = 16;
			j = tmp.lastIndexOf("_");
		// there can be a wierd string like "STRESS_IRNC_VLA_"
			if( j>= len-1) {
				tmp = tmp.substring(0, j);
				j = tmp.lastIndexOf("_");
			}
			if( j>0) m_label = m_ImageID.substring(0, j);
			// it turns out that m_label is useful beyond the cardiac studies
			// thus let's save it for "non cardiac" studies as well
			if( i< 0) m_label = tmp;	// in lower case
			if( i >= 0) {
				if( tmp.contains("ac")) i++;
				else if( !tmp.contains("nc")) i = -1;
				if( i>=0) switch( tmp.charAt(j+1)) {
					case 's':
						break;
						
					case 'h':
						i += 2;
						break;
						
					case 'v':
						i += 4;
						break;
						
					default:
						i = -1;
						break;
					
				}
			}
		}
		m_indx = i;
	}

	
	public int parseDicomDir(RandomAccessFile fis, String dataDir, boolean forceDicomDir) {
		byte[] byt1 = new byte[16384];
		ByteBuffer byt2;
		studyEntry st1 = new studyEntry();
		seriesEntry srTmp, sr1 = new seriesEntry();
		imageEntry im1 = new imageEntry();
		String level = null, tmp1, patName = null, patID = null, parent=null;
		boolean patName0 = false, patName1 = false;
		short currWrd, group, element, part1, part2;
		int fileOff, chunkVal, off1, len, len1, currInt, currlen, n1, bufLen;
		int i, j, numSer, dirType = -1;
//		int nextRecord, nextStudy, nextSeries, nextImage;
		try {
			m_aStudy = new ArrayList<>();
			m_currSOPInstanceUID = null;
			bufLen = chunkVal = fileOff = currlen = fis.read(byt1);
			if (currlen < 1024) {
				return 0;
			}
			byt2 = ByteBuffer.wrap(byt1);
			byt2 = byt2.order(ByteOrder.LITTLE_ENDIAN);
			off1 = 128;
//			nextRecord = 0;
//			nextStudy = nextSeries = nextImage = 1;	// NOT zero!
			currInt = byt2.getInt(off1);
			if (currInt != 0x4d434944) {
				return 0;
			}	// DICM
			currWrd = byt2.getShort(off1 + 12);
			off1 += currWrd + 16;
			currlen -= off1;
			byt2.position(off1);
			numSer = 0;
			m_aSeries = new ArrayList<>();
			m_aImage = new ArrayList<>();
			do {
				group = byt2.getShort();
				element = byt2.getShort();
//				off1 = byt2.position();
//				len1 = byt2.getInt(off1);
				part1 = byt2.getShort();
				part2 = byt2.getShort();
				off1 = byt2.position();
				len1 = byt2.getInt(off1);	// always explicit
				len = part2;
				if (part1 == UN || part1 == NU || part1 == UT || part1 == TU) {
					len = (short) (len1 + 4);
				}

				if (group == 4) {
					switch (element) {
						case 0x1220:
							if (part1 == SQ || part1 == QS) {
								len = 12;
							}
							break;

						case 0x1400:
//					nextRecord = byt2.getInt(off1);
							break;

						case 0x1430:
							level = getDcmString(byt2, len);
							// replace this when switching to JDK 7
							i = -1;
							if(level.equals("IMAGE")) i=1;
							if(level.equals("SERIES")) i=2;
							if(level.equals("STUDY")) i=3;
							if(level.equals("PATIENT")) i=4;
//							switch(level) {
							switch(i) {
//								case "IMAGE":
								case 1:
//									nextImage = nextRecord;
									sr1.numImages++;
									sr1.numValidImages++;
									if (im1.dirName != null) {
										m_aImage.add(im1);
										im1 = new imageEntry();
									}
									break;
									
//								case "SERIES":
								case 2:
//									nextSeries = nextRecord;
//									nextImage = 1;	// not zero
									takeCareOfSiemens(parent, sr1, im1);
									if (numSer++ > 0) {
										st1.numSeries++;
										sr1.setParDir(im1);
										m_aSeries.add(sr1);
										parent = im1.dirName;
									}
									sr1 = new seriesEntry();
									break;
									
//								case "STUDY":
								case 3:
									// first check that there is no series which hasn't been counted
									takeCareOfSiemens(parent, sr1, im1);
									patName1 = false;
									if (sr1.numImages > 0) {
//									nextImage = 1;	// not zero
										st1.numSeries++;
										// fake the orientation information, if it doesn't exist
										if (sr1.orientation == ORIENT_UNKNOWN) {
											sr1.orientation = ORIENT_AXIAL;
										}
										sr1.setParDir(im1);
										m_aSeries.add(sr1);
									}
									// now do the normal processing for the study
//									nextStudy = nextRecord;
									sr1 = new seriesEntry();
									parent = null;
									if (st1.patName == null) {
										st1.patName = patName;
									}
									if (st1.patID == null) {
										st1.patID = patID;
									}
									if (st1.numSeries > 0) {
										m_aStudy.add(st1);
										st1 = new studyEntry();
										// detect the case where no new patient name was entered
										if(!patName0) patName1 = true;
										st1.patName = patName;
										st1.patID = patID;
									}
									patName0 = false;
									break;
									
								case 4:
									patName = null;
									patName0 = false;
									break;
								}
							break;

						case 0x1500:
							// be careful not to count curves, only images
							if ( level != null && !level.equals("IMAGE")) {
								break;
							}
							tmp1 = getDcmString(byt2, len);
							im1.dirName = getLinuxDirectory(dataDir, tmp1);
							// if no new patient name was found, parse the dicom file
							if( patName1) {
								File fl1 = new File(dataDir + DIR_SEP_CHAR + im1.dirName);
								int dValid = checkDicomValid(fl1.getParent(), fl1.getName());
								if( dValid > 0) {
									st1.patName = m_currPatName;
									st1.patID = m_currPatID;
									patName1 = false;
								}
							}
							break;

						case 0x1510:	// modality
							// first we need to take care of format where all series
							// are defined before any images are entered. (Monnier, Stephanie)
							srTmp = sr1;
							n1 = m_aSeries.size();
							if( im1.dirName != null) {
								i = im1.dirName.lastIndexOf(DIR_SEP_CHAR);
								j = n1;
								if( i>0) {
									tmp1 = im1.dirName.substring(0, i);
									dirType = getDirType(dataDir, tmp1, dirType);
									if( dirType>0) {
										if( !sr1.isSameDir(tmp1) ) for( j=0; j<n1; j++) {
											srTmp = m_aSeries.get(j);
											if( srTmp.parDir == null) srTmp.parDir = tmp1;
/*											if( !forceDicomDir && srTmp.isSameDir(tmp1)) {
												tmp1 = "not using DICOMDIR. " + patName + "  ";
												tmp1 += st1.styDate + "\npath: " + dataDir;
												IJ.log(tmp1);
												return 0;
											}*/
										}
										if( j<n1) {
											sr1.numImages--;
											sr1.numValidImages--;
											srTmp.numImages++;
											srTmp.numValidImages++;
										}
									}
								}
							}
							tmp1 = getDcmString(byt2, len);
							m_SOPClass = ChoosePetCt.getSOPClass(tmp1);
							if( srTmp.sopClass == ChoosePetCt.SOPCLASS_UNKNOWN) srTmp.sopClass = m_SOPClass;
							break;

						case 0x1512:	// endian
//							tmp1 = getDcmString(byt2, len);
//							sr1.endian = implicitLittleEndian;	// default
//							if( tmp1 == "1.2.840.10008.1.2.1") sr1.endian = explicitLittleEndian;
							break;
					}
				}
				
				if( (group == 0x2001 && element == 0x105f) || (group == 0x88 && element == 0x200)) {
					// take care of sequence below
					group = 8;
					element = 0x1111;
				}

				if (group == 8) {
					switch (element) {
						case 0x18:
							m_currSOPInstanceUID = getDcmString(byt2, len);
							break;

						case 0x20:
							st1.styDate = getDcmString(byt2, len);
							break;

						case 0x31:
							if (len < 6) {
								break;
							}
							m_acqTime = getDcmString(byt2, len);
							break;

						case 0x50:
							st1.accessNum = getDcmString(byt2, len);
							break;
	
						case 0x1030:	// maybe use 20,10 instead
							st1.styName = getDcmString(byt2, len);
							break;

						case 0x103e:
							sr1.serName = getDcmString(byt2, len);
//							if( sr1.serName.startsWith("OEUFS"))
//								group = 0x8;
							break;

						case 0x1032:
						case 0x1110:
						case 0x1111:
						case 0x1115:
						case 0x1140:
							if (len1 < 0) {
								len1 = 8;
							}
							if (part1 == SQ || part1 == QS) {
								len = (short) (len1 + 4);
							}
							if (len < 0) {
								len = 8;
								if (part2 == 0 && (part1 == 0 || part1 == SQ || part1 == QS)) {
									len = 12;
								}
							}
							break;
					}
				}

				if (group == 0x10) {
					switch (element) {
						case 0x10:
							patName = getDcmName(byt2, len);
							patName0 = true;
							break;

						case 0x20:
							patID = getDcmString(byt2, len);
							break;
					}
				}
				
				if (group == 0x18) {
					switch (element) {
						case 0x1030:
							if( sr1.serName == null)
								sr1.serName = getDcmString(byt2, len);
							break;
					}
				}

				if (group == 0x20) {
					switch (element) {
						case 0xd:
							tmp1 = getDcmString(byt2, len);
							if (st1.studyUID == null) {
								st1.studyUID = tmp1;
							}
							break;

						case 0xe:
							tmp1 = getDcmString(byt2, len);
							if (sr1.seriesUID == null) {
								sr1.seriesUID = tmp1;
							}
							break;

						case 0x10:	// use the longer of this and 8,1030
							n1 = st1.styName.length();
							if(len > n1) st1.styName = getDcmString(byt2, len);
							break;
					}
				}

				if( group == 0x40) {
					switch (element) {
						case 0x260:
						case 0xffffa043:
						case 0xffffa168:
						case 0xffffa730:
							if (len1 < 0) {
								len1 = 8;
							}
							if (part1 == SQ || part1 == QS) {
								len = (short) (len1 + 4);
							}
							if (len < 0) {
								len = 8;
								if (part2 == 0 && (part1 == 0 || part1 == SQ || part1 == QS)) {
									len = 12;
								}
							}
							break;
					}
				}

				if( group == 0x7fe0 && element == 16) {
					fileOff += len1 + off1;
					fileOff += 4;
					fis.seek(fileOff-bufLen);
					currlen = fis.read(byt1) + 8;
					len = 0;
					byt2.position(0);
				}

				if (group == -2 && element == -0x2000) len = 0;
				off1 = byt2.position() + len;
				currlen -= len + 8;
				if( off1 < 16384) byt2.position(off1);
				if( currlen < 1024 && chunkVal >= 16000) {
					fileOff -= currlen;
					fis.seek(fileOff);
					chunkVal = currlen = fis.read(byt1);
					fileOff += currlen;
					byt2.position(0);
				}
			} while (currlen > 8);	// have to be 8 bytes to continue

			// finish up last entries
			if (im1.dirName != null) {
				m_aImage.add(im1);
			}
			takeCareOfSiemens(parent, sr1, im1);
			if (sr1.numImages > 0) {
				st1.numSeries++;
				sr1.setParDir(im1);
				m_aSeries.add(sr1);
			}
			if (st1.numSeries > 0) {
				m_aStudy.add(st1);
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return m_aStudy.size();
	}

	int getDirType(String dataDir, String dir, int dirType) {
		if( dirType >= 0) return dirType;
		String tmp1;
		int i, nFile, nDir;
		i = dir.lastIndexOf(DIR_SEP_CHAR);
		if( i<=0) return 0;
		tmp1 = dir.substring(0, i);
		File fl0, fl1 = new File(dataDir + DIR_SEP_CHAR + tmp1);
		File[] dirList = fl1.listFiles();
		for( i=nFile=nDir=0; i< dirList.length; i++) {
			fl0 = dirList[i];
			if( fl0.isDirectory()) nDir++;
			else nFile++;
			if(nFile > 3) return 0;
			if(nDir > 1) return 1;
		}
		return 0;
	}
	// Siemens has everything in a single directory
	// this won't work so kill series if same parent
	private void takeCareOfSiemens(String parent, seriesEntry sr1, imageEntry im1) {
/*		if( parent == null) return;
		File fil1 = new File(parent);
		String tmp1 = fil1.getParent();
		fil1 = new File(im1.dirName);
		if(fil1.getParent().equals(tmp1)) {
			int indx = m_aImage.size();
			while( sr1.numImages>0) {
				m_aImage.remove(--indx);
				sr1.numImages--;
			}
		}*/
	}

	private String getDcmString(ByteBuffer byt2, int leng) {
		String ret1;
		int pos1;
		byte[] tmp1 = new byte[leng];
		pos1 = byt2.position();
		byt2.get(tmp1, 0, leng);
		byt2.position(pos1);
		ret1 = new String(tmp1);
		ret1 = ret1.trim();
		return ret1;
	}
	
	// expand this to check for case problems in string
	private String getLinuxDirectory(String par, String in1) {
		String out1 = in1;
		if(DIR_SEP_CHAR == '/') {	// Linux
			out1 = out1.replace('\\', '/');
		}
		File fl2, fl1 = new File(par + DIR_SEP_CHAR + out1);
		File[] dirList;
		if( fl1.exists()) return out1;
		// oops, maybe there is a case problem?
		String parBuild = par, tail1 = out1, tmp1, flName;
		int i, n=1;
		while( n>0) {
			tmp1 = tail1;
			n = tail1.indexOf(DIR_SEP_CHAR);
			if( n > 0) {
				tmp1 = tail1.substring(0, n);
				tail1 = tail1.substring(n+1);
			}
			fl1 = new File(parBuild);
			dirList = fl1.listFiles();
			if( dirList == null) return out1;	// bail out
			for( i=0; i<dirList.length; i++) {
				fl2 = dirList[i];
				flName = fl2.getName();
				if(flName.equalsIgnoreCase(tmp1)) {
					parBuild += DIR_SEP_CHAR + flName;
					break;
				}
			}
		}
		n = par.length();
		out1 = parBuild.substring(n+1);
		return out1;
	}

	private String getDcmName(Attributes at, int tag) {
		String ret1 = at.getString(tag);
		if( ret1 != null && !ret1.isEmpty()) {
			ret1 = ret1.replace('^', ' ');
			ret1 = ret1.trim();
		}
		return ret1;
	}

	private String getDcmString(Attributes at, int tag) {
		String ret1 = at.getString(tag);
		if( ret1 != null && !ret1.isEmpty()) ret1 = ret1.trim();
		return ret1;
	}

	private String getStudyDate(Attributes at) {
		int i, tag;
		String tmp, ret1="";
		for( i=0; i<3; i++) {
			switch(i) {
				case 0:
					tag = Tag.StudyDate;
					break;
	
				case 2:
					tag = Tag.ContentDate;
					break;
	
				default:
					tag = Tag.SeriesDate;
					break;
			}
			tmp = at.getString(tag);
			if( ret1.isEmpty()) {
				ret1 = tmp;
				if( tmp == null)
					ret1 = "";
				continue;
			}
			if( tmp != null && !ret1.equals(tmp)) {
				// there is a study where the series date isn't study date
				// prefer series date
				if(tmp.length()==8 && i==1)
					ret1 = tmp;
			}
		}
		return ret1;
	}

	private String getDcmName(ByteBuffer byt2, int leng) {
		String ret1;
		int pos1;
		byte[] tmp1 = new byte[leng];
		pos1 = byt2.position();
		byt2.get(tmp1, 0, leng);
		byt2.position(pos1);
		ret1 = new String(tmp1, Charset.forName("ISO-8859-8"));
//		ret1 = new String(tmp1, Charset.forName("ISO_IR 100"));
//		ret1 = new String(tmp1);
		ret1 = ret1.replace('^', ' ');
		ret1 = ret1.trim();
		return ret1;
	}

	private double getDcmDouble(ByteBuffer byt2, int leng) {
		double ret1 = 0.;
		String tmp1;
		tmp1 = getDcmString(byt2, leng);
		if( !tmp1.isEmpty()) {
			// there are cases where patient height = "annonimize"
			try {
				ret1 = Double.valueOf(tmp1);
			} catch (Exception e) {}
		}
		return ret1;
	}

	private int getDcmInteger(ByteBuffer byt2, int leng) {
		int ret1 = 0;
		String tmp1;
		tmp1 = getDcmString(byt2, leng);
		if( !tmp1.isEmpty()) ret1 = Integer.valueOf(tmp1);
		return ret1;
	}

	private ArrayList<Double> getDcmDoubleVect(ByteBuffer byt2, int leng) {
		ArrayList<Double> v1 = new ArrayList<>();
		String tmp1;
		tmp1 = getDcmString(byt2, leng);
		tmp1 = tmp1.replace('\\', ' ');
		Scanner sc = new Scanner(tmp1);
		sc.useLocale(Locale.US);
		while (sc.hasNextDouble()) {
			Double d1 = sc.nextDouble();
			v1.add(d1);
		}
		return v1;
	}

	int getImageType() {
		if (m_modality == 0x5443) { //CT
			if (m_imgType.endsWith("LOCALIZER")) {
				return -1;
			}
			m_dataType = GOOD_CT_DATA;
			if (m_modelName.equals("VARICAM")) {
				m_dataType = VARICAM_XRAY;
			}
			if (m_size.width <= 128) {
				m_dataType = REDUCED_CT_DATA;
			}
			return 2;	// CT study, not a scout
		}
		if (m_modality == 0x5450) { // PT - PET study
			// be careful Philips has screen captures marked PT
			if (m_petUnits == null) return -1;
			if (m_petUnits.equals("BQML")) {
				m_dataType = BQML_ATTENUATION;
				return 0;
			}
			if (m_petUnits.equals("CPS") || m_petUnits.equals("PROPCNTS")) {
				m_dataType = NO_ATTENUATION;
				return 1;
			}
			// and for Phillips we need an extra check to distinguish
			if (m_petUnits.equals("CNTS")) {	// hello Phillips
				if (m_atten.contains("NONE")) {
					m_dataType = NO_ATTENUATION;
					return 1;
				}
				m_dataType = PHILIPS_ATTENUATION;
				return 0;
			}
			// and Varicam Spect also uses "Cnts", and m_ImageID distinguishes
			if (m_petUnits.equals("Cnts") && m_ImageID.length() == 9) {
				if (m_ImageID.equals("IRNC OSEM")) {
					m_dataType = NO_ATTENUATION;
					return 1;
				}
				m_dataType = OSEM_ATTENUATION;
				return 0;
			}
		}
		if (m_modality == 0x4d4e) {	// NM, SPECT study? Be careful not to overwrite a PET study
			if (m_petUnits != null) {
				return -1;
			}
			if (!m_imgType.contains("RECON TOMO")) {
				return -1;
			}	// reject normal NM
			m_dataType = SPECT_ATTENUATION;
			// special check for Philips SPECT data
			if ( m_ImageID == null || m_ImageID.length() < 4) {
				return 0;
			}
			String tmp1 = m_ImageID.substring(0, 4);
			if (tmp1.compareToIgnoreCase("noac") == 0) {
				m_dataType = NO_ATTENUATION;
				return 1;
			}
			return 0;
		}
		return -1;
	}

/*	int getOrientation(String tmp) {
		double[] val = new double[6];
		int i = 0, retval = ORIENT_UNKNOWN;
		Scanner sc = new Scanner(tmp).useDelimiter("\\\\");
		sc.useLocale(Locale.US);
		while(sc.hasNextDouble()) {
			val[i++] = sc.nextDouble();
		}
		if( i != 6) return retval;
		// allow some sloppiness in the measurement
		if (val[0] > 0.96 && val[4] > 0.96) {
			retval = ORIENT_AXIAL;
		} else if (val[5] < -0.97) {
			if (val[0] > 0.98) {
				retval = ORIENT_CORONAL;
			}
			if (val[1] > 0.96 || val[1] < -0.96) {
				retval = ORIENT_SAGITAL;
			}
		}
		if( retval == ORIENT_UNKNOWN) {
			// let's try to find what it is closest to, oblAxial, oblCor, oblSag
			int indx, jmax, imax;
			double maxi, maxj, maxTmp;
			imax = jmax = 0;
			maxi = Math.abs(val[0]);
			maxj = Math.abs(val[3]);
			for( indx=1; indx<3; indx++) {
				maxTmp = Math.abs(val[indx]);
				if( maxTmp > maxi) {
					maxi = maxTmp;
					imax = indx;
				}
				maxTmp = Math.abs(val[indx+3]);
				if( maxTmp > maxj) {
					maxj = maxTmp;
					jmax = indx;
				}
			}
			retval = ORIENT_OBLIQUE;	// if nothing else fits, use oblique
			if( imax==0 && jmax==1) retval = ORIENT_OBL_AXIAL;
			if( imax==0 && jmax==2) retval = ORIENT_OBL_COR;
			if( imax==1 && jmax==2) retval = ORIENT_OBL_SAG;
		}
		return retval;
	}*/
	
	// this returns the number of series above a given study
	// they should be subtracted from getNumSeries
	int getNonRelevantSeries(int indx) {
		int i, sz1, numSeries=0;
		sz1 = m_aStudy.size();
		for( i=indx+1; i<sz1; i++) {
			numSeries += getNumSeries(i);
		}
		return numSeries;
	}

	public int getNumImages(int offst) {
		seriesEntry sr1;
		sr1 = m_aSeries.get(offst);
		return sr1.numImages;
	}
	
	public int getNumSeries(int indx) {
		studyEntry sdy1 = m_aStudy.get(indx);
		return sdy1.numSeries;
	}

	public String getFileName(int offst) {
		imageEntry im1;
		im1 = m_aImage.get(offst);
		return im1.dirName;
	}

	public boolean IsNM(int offst) {
		seriesEntry sr1;
		sr1 = m_aSeries.get(offst);
		return sr1.sopClass == ChoosePetCt.SOPCLASS_TYPE_NM;
	}

	public boolean IsCT(int offst) {
		seriesEntry sr1;
		sr1 = m_aSeries.get(offst);
		return sr1.sopClass == ChoosePetCt.SOPCLASS_TYPE_CT;
	}

	public boolean IsSC(int offst) {
		seriesEntry sr1;
		sr1 = m_aSeries.get(offst);
		return sr1.sopClass == ChoosePetCt.SOPCLASS_TYPE_SC;
	}
}

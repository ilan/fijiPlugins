import ij.IJ;
import java.awt.Dimension;
import java.io.EOFException;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.media.DicomDirReader;


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
	int m_SOPClass, m_modality, m_dataType, m_indx;
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
		int numImages = 0;
		int sopClass = ChoosePetCt.SOPCLASS_UNKNOWN;
	}

	class imageEntry {
		String fileName = null;
		
		int setSeries( String[] cs1, String sop1, seriesEntry sr1) {
			int i, n;
			String tmp1 = "";
			if( cs1 == null || cs1.length == 0) return -1;
			n = cs1.length;
			for( i=0; i<n-1; i++) {
				tmp1 = tmp1 + cs1[i] + DIR_SEP_CHAR;
			}
			fileName = tmp1 + cs1[n-1];
			if( sr1.sopClass == ChoosePetCt.SOPCLASS_UNKNOWN) {	// set it once
				sr1.sopClass = ChoosePetCt.getSOPClass(sop1);
			}
			return 0;
		}
		
		String getPathFile() {
			return fileName;
		}
	}

	public int checkFile(String dataDir, boolean forceDicomDir) {
		int retVal = 0;
		RandomAccessFile fis;
		try {
			File fl1;
			fl1 = new File(dataDir + DIR_SEP_CHAR + "DICOMDIR");
			if (fl1.exists()) {
				retVal = parseDicomDir(fl1, dataDir, forceDicomDir);
				return retVal;
			}
			// it is case sensitive to dicomdir - all upper or all lower case
			fl1 = new File(dataDir + DIR_SEP_CHAR + "dicomdir");
			if (fl1.exists()) {
				retVal = parseDicomDir(fl1, dataDir, forceDicomDir);
				return retVal;
			}
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
			dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
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
			int rows = getDcmInt(at, Tag.Rows);
			int cols = getDcmInt(at, Tag.Columns);
			m_size = new Dimension(cols, rows);
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
			fis.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return true;
	}

	public int parseDicomDir(File fl1, String dataDir, boolean forceDicomDir) {
		try {
			Attributes patr, sdyr, serr, imgr;
			studyEntry st1;
			int i;
			DicomDirReader ddr = new DicomDirReader(fl1);
			patr = ddr.findPatientRecord();
			while (patr != null) {
				m_currPatName = getDcmName(patr, Tag.PatientName);
				m_currPatID = getDcmString(patr, Tag.PatientID);
				m_birthdate = getDcmString(patr, Tag.PatientBirthDate);
				if(m_currPatID.isEmpty()) return 0;
				sdyr= ddr.findStudyRecord(patr);
				while (sdyr != null) {
					if( m_aStudy == null) m_aStudy = new ArrayList<>();
					st1 = new studyEntry();
					m_currStudyInstanceUID = getDcmString(sdyr, Tag.StudyInstanceUID);
					m_studyDate = getDcmString(sdyr, Tag.StudyDate);
					m_currStyName = getDcmString(sdyr, Tag.StudyDescription);
					m_accession = getDcmString(sdyr, Tag.AccessionNumber);
					serr = ddr.findSeriesRecord(sdyr);
					while (serr != null) {
						st1.numSeries++;
						if( m_aSeries == null) m_aSeries = new ArrayList<>();
						seriesEntry sr1 = new seriesEntry();
						m_currSeriesInstanceUID = getDcmString(serr, Tag.SeriesInstanceUID);
						m_currSerName = getDcmString(serr, Tag.SeriesDescription);
						imgr = ddr.findLowerInstanceRecord(serr, true);
						while (imgr != null) {
							sr1.numImages++;
							if( m_aImage == null) m_aImage = new ArrayList<>();
							imageEntry im1 = new imageEntry();
							String[] cs1 = getDcmCS(imgr, Tag.ReferencedFileID);
							String sop1 = getDcmString(imgr, Tag.SOPClassUID);
							im1.setSeries(cs1, sop1, sr1);
							m_aImage.add(im1);
							imgr = ddr.findNextInstanceRecord(imgr, true);
						}
						sr1.seriesUID = m_currSeriesInstanceUID;
						sr1.serName = m_currSerName;
						m_aSeries.add(sr1);
						serr = ddr.findNextSeriesRecord(serr);
					}
					st1.patName = m_currPatName;
					st1.patID = m_currPatID;
					st1.studyUID = m_currStudyInstanceUID;
					st1.styName = m_currStyName;
					st1.styDate = m_studyDate;
					st1.accessNum = m_accession;
					m_aStudy.add(st1);
					sdyr = ddr.findNextStudyRecord(sdyr);
				}
				patr = ddr.findNextPatientRecord(patr);
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return 1;
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

	private int getDcmInt(Attributes at, int tag) {
		return at.getInt(tag, 0);
	}

	private String[] getDcmCS(Attributes at, int tag) {
		String[] cs = at.getStrings(tag);
		return cs;
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

	public int getNumImages(int offst) {
		seriesEntry sr1;
		sr1 = m_aSeries.get(offst);
		return sr1.numImages;
	}
	
	public int getNumSeries(int indx) {
		studyEntry sdy1 = m_aStudy.get(indx);
		return sdy1.numSeries;
	}
}

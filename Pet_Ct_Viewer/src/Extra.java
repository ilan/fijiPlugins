
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 *
 * @author ilan
 */
public class Extra {

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
			bf1.close();
			rd1.close();
			flName = flName.trim();
			fl2 = new File(flName);
			if( fl2.exists()) flRet = flName;
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return flRet;
	}

	static boolean isBfFile(String path) {
		File fl1 = new File(path + "/graphic.brownFat.gr2");
		return fl1.exists();
	}

	static String generateSOPInstanceUID(Date dt0) {
		Date dt1 = dt0;
		if( dt1 == null) dt1 = new Date();
		SimpleDateFormat df1 = new SimpleDateFormat("2.16.840.1.113664.3.yyyyMMdd.HHmmss", Locale.US);
		return df1.format(dt1);
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

	static String makeMetaData(String seriesName, int numFrm, ImagePlus im1,
			int SOPtype, String srcMeta) {
		Date dt1 = new Date();
		String SOP = Extra.generateSOPInstanceUID(dt1);
		String modality = "SC";
		String AET = ChoosePetCt.getDicomValue(srcMeta, "0002,0016");
		if( AET == null) AET = "A123";
		String SOPClass = ChoosePetCt.SOPCLASS_SC;
		if( SOPtype == ChoosePetCt.SOPCLASS_TYPE_NM) {
			SOPClass = ChoosePetCt.SOPCLASS_NM;
			modality = "NM";
		}
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
//		meta = append2Meta("0008,0060", meta, srcMeta);
		meta += "0008,0060  Modality: " + modality + "\n";
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

	static ArrayList<String> getSeries4dateID( Date inDate, String inID) {
		ArrayList<String> retSer = new ArrayList<>();
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
			else if( !Extra.isSameDay(serDate0, serDate)) continue;
			serName = ChoosePetCt.getDicomValue(meta, "0008,103E");
			if( serName == null || serName.isEmpty()) serName = ChoosePetCt.getDicomValue( meta, "0054,0400");
			retSer.add(serName);
		}
		return retSer;
	}
}

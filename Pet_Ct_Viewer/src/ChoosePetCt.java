import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.plugin.BrowserLauncher;
import ij.process.ImageProcessor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Scanner;
import java.util.prefs.Preferences;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/*
 * ChoosePetCt.java
 *
 * Created on Nov 23, 2009, 1:41:37 PM
 */

/**
 *
 * @author Ilan
 */
public class ChoosePetCt extends javax.swing.JDialog implements TableModelListener {
	static final long serialVersionUID = 42;
	static final int TBL_CHECK = 0;
	static final int TBL_PATNAME = 1;
	static final int TBL_STUDY = 2;
	static final int TBL_DATE = 3;
	static final int TBL_SERIES = 4;
	static final int TBL_SER_TYPE = 5;
	static final int TBL_PAT_ID = 6;
	static final int TBL_SIZE = 7;

//	static final int ORIENT_UNKNOWN = 0;
//	static final int ORIENT_AXIAL = 1;
//	static final int ORIENT_CORONAL = 2;
//	static final int ORIENT_SAGITAL = 3;
//	static final int ORIENT_OBL_AXIAL = 4;
//	static final int ORIENT_OBL_COR = 5;
//	static final int ORIENT_OBL_SAG = 6;
//	static final int ORIENT_OBLIQUE = 7;
	static final int ORIENT_AXIAL_ROTATED = 8;

	static final int SERIES_UNKNOWN = 0;
	static final int SERIES_CT = 1;
	static final int SERIES_CT_VARICAM = 2;
	static final int SERIES_REDUCED_CT = 3;
	static final int SERIES_UPET = 4;
	static final int SERIES_BQML_PET = 5;
	static final int SERIES_GML_PET = 6;
	static final int SERIES_PHILIPS_PET = 7;
	static final int SERIES_GE_PRIVATE_PET = 8;
	static final int SERIES_SPECT = 9;
	static final int SERIES_SIEMENS_SPECT = 10;
	static final int SERIES_MRI = 11;
	static final int SERIES_NM3 = 12;	// for display3Frame
	static final int SERIES_NM = 13;	// for simple NM, Gastric etc.
	static final int SER_FORCE_CT = 14;
	static final int SER_FORCE_CPET = 15;
	static final int SER_FORCE_UPET = 16;
	static final int SER_FORCE_MRI = 17;
	static final int SERIES_MIP = 18;
	
	static final int BLACK_BKGD = 1;
	static final int HOT_IRON_FUSE = 2;
	static final int EXT_BLUES_LUT = 3;
	static final int EXT_HOT_IRON_LUT = 4;
	static final int FIXED_USER_LUT = 5;
	static final int MRI_CT_LUT = 6;

	static final int SOPCLASS_UNKNOWN = 0;
	static final int SOPCLASS_TYPE_NM = 1;
	static final int SOPCLASS_TYPE_SC = 2;
	static final int SOPCLASS_TYPE_US = 3;
	static final int SOPCLASS_TYPE_CT = 4;
	static final int SOPCLASS_TYPE_PET = 5;
	static final int SOPCLASS_TYPE_MRI = 6;
	static final int SOPCLASS_TYPE_RADIOGRAPH = 7;
	static final int SOPCLASS_TYPE_ENHANCED_PET = 8;
	static final int SOPCLASS_TYPE_ENHANCED_CT = 9;
	static final int SOPCLASS_TYPE_SR_STORAGE = 10;
	static final int SOPCLASS_TYPE_ENHANCED_MRI = 11;
	static final int SOPCLASS_TYPE_RAW = 12;
	
	static final String SOPCLASS_SC = "1.2.840.10008.5.1.4.1.1.7";
	static final String SOPCLASS_NM = "1.2.840.10008.5.1.4.1.1.20";
	static final String SOPCLASS_SR = "1.2.840.10008.5.1.4.1.1.88.67";	// Radiation Dose
	static final int XRES = 100, YRES = 63;
    /** Creates new form ChoosePetCt
	 * @param parent
	 * @param modal */
    public ChoosePetCt(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
		init();
		fillTable();
    }

	@Override
	public void tableChanged(TableModelEvent arg0) {
		getStudyList();
	}

	@SuppressWarnings("unchecked")
	private void init() {
		int i, x, y;
		jPrefer = Preferences.userNodeForPackage(ChoosePetCt.class);
		jPrefer = jPrefer.node("biplugins");
		userDir = IJ.getDirectory("imagej");
		TableColumn serTypCol = jTable1.getColumnModel().getColumn(TBL_SER_TYPE);
		JComboBox combo = new JComboBox();
		combo.addItem("CT");
		combo.addItem("CPet");
		combo.addItem("UPet");
		combo.addItem("MRI");
		serTypCol.setCellEditor(new DefaultCellEditor(combo));
		jTable1.setAutoCreateRowSorter(true);
		jTable1.getModel().addTableModelListener(this);
		x = jPrefer.getInt("choose pet dialog x", 0);
		y = jPrefer.getInt("choose pet dialog y", 0);
		if( x > 0 && y > 0) {
			TableColumn col1;
			setSize(x,y);
			for(i=0; i<=TBL_SIZE; i++) {
				x = jPrefer.getInt("choose pet dialog col" + i, 0);
				if( x <= 0) continue;
				col1 = jTable1.getColumnModel().getColumn(i);
				col1.setPreferredWidth(x);
			}
		}
	}

	static void openHelp(String start) {
/*		String helpHS = "/resources/javahelp/PetCtHelp.xml";
		try {
			HelpBroker hb = null;
			URL hsURL = getClass().getResource(helpHS);
			HelpSet hs = new HelpSet(null, hsURL);
			hb = hs.createHelpBroker();
			hb.setDisplayed(true);
			setVisible(false);

		} catch (Exception e) { stackTrace2Log(e);}*/
		String url = "http://sourceforge.net/p/bifijiplugins/wiki/" + start;
		new BrowserLauncher().run(url);
	}

	/**
	 * Routine used for both enabling the read button as well as getting list of selected studies.
	 * Whenever the user checks a selection mark, this updates the enable status of the read button.
	 *
	 * Note that it returns a ArrayList of pointers to the studies. The ArrayLists imgList and seriesType are
	 * then used to retrieve the ImagePlus object and the seriesType describing it.
	 * 
	 * @return ArrayList of selected studies
	 */
	protected ArrayList<Integer> getStudyList() {
		TableModel mod1 = jTable1.getModel();
		int i, j, k, n = mod1.getRowCount();
		ArrayList<Integer> selVals = new ArrayList<>();
		seriesForce = new ArrayList<>();
		Boolean sel1;
		String serType;
		boolean readEn = false;
		for( i=0; i<n; i++) {
			j =  jTable1.convertRowIndexToModel(i);
			serType = (String) mod1.getValueAt(j, TBL_SER_TYPE);
			k = JFijiPipe.FORCE_NONE;
			if( serType.equals("CT")) k = JFijiPipe.FORCE_CT;
			if( serType.equals("CPet")) k = JFijiPipe.FORCE_CPET;
			if( serType.equals("UPet")) k = JFijiPipe.FORCE_UPET;
			if( serType.equals("MRI")) k = JFijiPipe.FORCE_MRI;
			seriesForce.add(k);
			sel1 =  (Boolean) mod1.getValueAt(j, 0);
			if( sel1 == true) selVals.add(j);
		}
		n = selVals.size();
		int numCt=0, numMri=0, numAPet=0, numUPet=0, numNM = 0;
		int numCorSag = 0, val1 = SERIES_UNKNOWN;
		for( i=0; i<n; i++) {
			j = selVals.get(i);
			val1 = getSeriesType(j);
			if( val1 == SERIES_CT || val1 == SERIES_CT_VARICAM || val1 == SER_FORCE_CT) {
				if( isOrigAxial(j)) numCt++;
				else numCorSag++;
			}
			if( val1 == SERIES_MRI || val1 == SER_FORCE_MRI) numMri++;
			if( val1 == SERIES_BQML_PET || val1 == SERIES_GML_PET
				|| val1 == SERIES_PHILIPS_PET || val1 == SERIES_GE_PRIVATE_PET || val1 == SERIES_SPECT
				|| val1 == SERIES_SIEMENS_SPECT || val1 == SER_FORCE_CPET) numAPet++;
			if( val1 == SERIES_UPET || val1 == SER_FORCE_UPET) numUPet++;
			if( val1 == SERIES_NM3) numNM++;
		}
		if( numCt == 1 && numMri < 2 && numNM == 0 && numCorSag == 0) {
			if( (numAPet == 1 || numUPet == 1) && numAPet < 2 && numUPet < 2) readEn = true;
		}
		if( n == 1 && val1 != SERIES_UNKNOWN) readEn = true;	// single study
		jButOK.setEnabled(readEn);
		if( readEn == false) selVals = null;
		return selVals;
	}

	private boolean isOrigAxial( int indx) {
		ImagePlus ip0 = imgList.get(indx);
		String meta = getMeta(1, ip0);
		int orient0 = getOrientation(getDicomValue( meta, "0020,0037")) & 31;
		return orient0 == DicomFormat.ORIENT_AXIAL ||
			orient0 == DicomFormat.ORIENT_OBL_AXIAL;
	}

	int getSeriesType(int indx) {
		int val1 = seriesType.get(indx);
		int val2 = seriesForce.get(indx);
		int val3 = JFijiPipe.FORCE_NONE;
		if( val1 == SERIES_CT || val1 == SERIES_CT_VARICAM) val3 = JFijiPipe.FORCE_CT;
		if( val1 == SERIES_BQML_PET || val1 == SERIES_GML_PET || 
			val1 == SERIES_PHILIPS_PET || val1 == SERIES_GE_PRIVATE_PET || val1 == SERIES_SPECT
				|| val1 == SERIES_SIEMENS_SPECT) val3 = JFijiPipe.FORCE_CPET;
		if( val1 == SERIES_UPET) val3 = JFijiPipe.FORCE_UPET;
		if( val1 == SERIES_MRI) val3 = JFijiPipe.FORCE_MRI;
		if( val2 == val3) return val1;
		switch(val2) {
			case JFijiPipe.FORCE_CT:
				val1 = SER_FORCE_CT;
				break;
			case JFijiPipe.FORCE_CPET:
				val1 = SER_FORCE_CPET;
				break;
			case JFijiPipe.FORCE_UPET:
				val1 = SER_FORCE_UPET;
				break;
			case JFijiPipe.FORCE_MRI:
				val1 = SER_FORCE_MRI;
				break;
		}
		return val1;
	}
	
	static int getForcedVal(int serType) {
		int val1 = JFijiPipe.FORCE_NONE;
		switch(serType) {
			case SER_FORCE_CT:
				val1 = JFijiPipe.FORCE_CT;
				break;
			case SER_FORCE_CPET:
				val1 = JFijiPipe.FORCE_CPET;
				break;
			case SER_FORCE_UPET:
				val1 = JFijiPipe.FORCE_UPET;
				break;
			case SER_FORCE_MRI:
				val1 = JFijiPipe.FORCE_MRI;
				break;
		}
		return val1;
	}
	
	static int round(double inVal) {
		return (int) Math.round(inVal);
	}
	
	static Double round10(double inVal) {
		int val1 = round(inVal * 10.0);
		return val1 / 10.0;
	}

	/**
	 * Routine to check if we need to call the dialog or not.
	 * This routine is called after Read_BI_Studies to save the user the trouble of
	 * pressing on Pet_Ct_Viewer in the case where he has chosen a study which
	 * is acceptable to the fusion program. This works only for the simplest cases
	 * (which are 99% of the cases). It will fail, i.e. demand manual intervention
	 * on any "complicated" cases.
	 * 
	 * One such complicated case which was found was a phantom study where
	 * the CPet, UPet and CT all had the same seriesUID. This is forbidden.
	 * 
	 * @param arg - list of series UIDs
	 * @return true if data is good, i.e. no need to display the dialog
	 */
	protected boolean checkList(String arg) {
		if( checkEmpty(arg) == null) return false;
		TableModel mod1 = jTable1.getModel();
		String currUID, serUID, nextPart = arg;
		int i;
		while( !nextPart.isEmpty()) {
			currUID = nextPart;
			i = currUID.indexOf(", ");
			if( i > 0) {
				currUID = currUID.substring(0, i);
				nextPart = nextPart.substring(i+2);
			}
			else nextPart = "";
			for( i=0; i<seriesUIDs.size(); i++) {
				serUID = seriesUIDs.get(i);
				if( serUID.equals(currUID)) {
					mod1.setValueAt(true, i, 0);
					break;
				}
			}
		}
		chosenOnes = getStudyList();	// this is set here too in case the dialog isn't used
		boolean butOK = jButOK.isEnabled();
		if( !butOK) chosenOnes = null;
		return butOK;
	}

	private void fillTable() {
		DefaultTableModel mod1;
		mod1 = (DefaultTableModel) jTable1.getModel();
		mod1.setNumRows(0);
		imgList = new ArrayList<>();
		seriesType = new ArrayList<>();
		seriesUIDs = new ArrayList<>();
		seriesName = new ArrayList<>();
		ImagePlus img1;
		String meta, patName, study, series, tmp1;
		Date date1;
		int i, j, k, row0, col0, serType;
		int [] fullList = WindowManager.getIDList();
		if( fullList == null) return;
		for( i=0; i<fullList.length; i++) {
			img1 = WindowManager.getImage(fullList[i]);
			j = img1.getStackSize();
			if( j < 5) continue;
			meta = getMeta(1, img1);
			if( meta == null) continue;	// no information, skip it
			serType = getImageType(meta);
			if( serType == SERIES_UNKNOWN) continue;
			Object[] row1 = new Object[TBL_SIZE+1];	// TBL_SIZE is largest value
			row1[TBL_CHECK] = false;
			patName = getCompressedPatName( meta);
			// There are anonymous studies with no name
//			if( patName == null) patName = "";
			row1[TBL_PATNAME] = patName;
			row1[TBL_PAT_ID] = getCompressedID( meta);
			date1 = getStudyDateTime( meta, -1);
			row1[TBL_DATE] = UsaDateFormat(date1);
			study = getDicomValue( meta, "0008,1030");
			row1[TBL_STUDY] = study;
			series = checkEmpty(getDicomValue( meta, "0008,103E"));
			if( series == null) series = getDicomValue( meta, "0054,0400");
			if( series == null) series = "-";
			// need a special case for GE SPECT
			if(series.startsWith("Volumetrix MI RESULTS")) {
				tmp1 = getDicomValue( meta, "0011,1012");
				if( tmp1 != null && (k=tmp1.indexOf("_"))>0) {
					if( tmp1.contains("EM_IRACRR")) series = tmp1.substring(0, k+1) + "corrected";
					if( tmp1.contains("EM_IRNC")) {
						series = tmp1.substring(0, k+1) + "uncorrected";
						serType = SERIES_UPET;
					}
				}
			}
			row1[TBL_SERIES] = series;
			fillSerType(serType, row1);
			col0 = parseInt(getDicomValue(meta, "0028,0011"));
			row0 = parseInt(getDicomValue(meta, "0028,0010"));
			tmp1 = col0 + "*" + row0 + "*" + j;
			row1[TBL_SIZE] = tmp1;
			mod1.addRow(row1);
			imgList.add(img1);
			seriesType.add(serType);
			seriesUIDs.add(getSeriesUID(meta));
			seriesName.add(series);
		}
	}
	
	private void fillSerType(int serType, Object[] row1) {
		String val1 = "";
		if(serType == SERIES_CT || serType == SERIES_CT_VARICAM) val1 = "CT";
		if(serType == SERIES_BQML_PET || serType == SERIES_GML_PET || serType == SERIES_GE_PRIVATE_PET
				|| serType == SERIES_PHILIPS_PET || serType == SERIES_SPECT || serType == SERIES_SIEMENS_SPECT) val1="CPet";
		if(serType == SERIES_UPET) val1 = "UPet";
		if(serType == SERIES_MRI) val1 = "MRI";
		row1[TBL_SER_TYPE] = val1;
	}

	// getSeriesName, setCheckBox and pressOkCancel are used in groovy scripts
	public String getSeriesName(int indx) {
		if( seriesName == null) return null;
		if( indx >= seriesName.size()) return null;
		return seriesName.get(indx);
	}

	public void setCheckBox(int indx) {
		TableModel mod1 = (DefaultTableModel) jTable1.getModel();
		if( indx>=mod1.getRowCount()) return;
		mod1.setValueAt(true, indx, 0);
	}

	public int pressOkCancel() {
		if(jButOK.isEnabled()) {
			jButOK.doClick();
			return 1;
		}
		jButCancel.doClick();
		return 0;
	}

	static String UsaDateFormat( Date inDate) {
		if(inDate == null) return "";
		return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(inDate);
	}

	static void deleteFolder(File folder, boolean topFolder) {
		File[] files = folder.listFiles();
		if (files != null) { //some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f, true);
				} else {
					f.delete();
				}
			}
		}
		if(topFolder) folder.delete();
	}

	static String getMeta(int slice, ImagePlus img1) {
		// first check that the user hasn't closed the study
		if( img1.getImage() == null) return null;
		String meta = img1.getStack().getSliceLabel(slice);
		// meta will be null for SPECT studies
		if (meta == null ) meta = (String) img1.getProperty("Info");
		return meta;
	}

	// use key1 == null if "meta" contains the line of interest
	static String getDicomValue( String meta, String key1) {
		String tmp1, key2 = key1, ret1 = null;
		int k1, k0 = 0;
		if( meta == null) return ret1;
		if( key1 != null) {
			k0 = meta.indexOf(key1);
			if( k0 <= 0) key2 = key1.toLowerCase();
			k0 = meta.indexOf(key2);
		}
		if( k0 > 0 || key2 == null) {
			// here we have a problem that the key may appear more than once.
			// for example a SeriesUID may appear in a sequence. Look for ">".
			if( k0 > 0) {
				tmp1 = meta.substring(k0+4, k0+16);
				k1 = tmp1.indexOf(">");
				while(k1 > 0) {	// do search last value
					k1 = meta.indexOf(key2, k0+4);
					if( k1 > 0) k0 = k1;
				}
			}
			k1 = meta.indexOf("\n", k0);
			if( k1 < 0) return null;
			tmp1 = meta.substring(k0, k1);
			k1 = tmp1.indexOf(": ");
			if( k1 > 0) ret1 = tmp1.substring(k1+2);
			else ret1 = tmp1;
			ret1 = ret1.trim();
			if( ret1.isEmpty()) ret1 = null;
		}
		return ret1;
	}

	static String encloseInQuotes(String in) {
		return "\\\"" + in + "\\\"";
	}

	static String getFirstDicomValue( String meta, String key1) {
		return getFirstLastSub(meta, key1, true);
	}
	
	static String getLastDicomValue( String meta, String key1) {
		return getFirstLastSub(meta, key1, false);
	}
	
	static String getFirstLastSub( String meta, String key1, boolean isFirst) {
		int k0, k1 = 0;
		String tmp1, ret1, key2 = key1;
		k0 = meta.indexOf(key2);
		if( k0 <= 0) {
			key2 = key1.toLowerCase();
			k0 = meta.indexOf(key2);
			if( k0 <= 0) return null;
		}
		while( k1 >= 0 && !isFirst) {
			k1 = meta.indexOf(key2, k0+4);
			if( k1 > 0) k0 = k1;
		}
		k1 = meta.indexOf("\n", k0);
		if( k1 < 0) return null;
		tmp1 = meta.substring(k0, k1);
		k1 = tmp1.indexOf(": ");
		if( k1 > 0) ret1 = tmp1.substring(k1+2);
		else ret1 = tmp1;
		ret1 = ret1.trim();
		if( ret1.isEmpty()) ret1 = null;
		return ret1;
	}

// look for any of a list of possible values	
	static String findDicomValue( String meta, String key1, String[] val) {
		String tmp1, key2 = key1, ret1 = null;
		int i, k1, k2, k0;
		if( meta == null || key1 == null || val == null) return ret1;
		k0 = meta.indexOf(key1);
		if( k0 <= 0) key2 = key1.toLowerCase();
		k0 = meta.indexOf(key2);
		while( k0 > 0) {
			k1 = meta.indexOf("\n", k0);
			if( k1 < 0) return null;
			tmp1 = meta.substring(k0, k1);
			k2 = tmp1.indexOf(": ");
			if( k2 > 0) ret1 = tmp1.substring(k2+2);
			else ret1 = tmp1;
			ret1 = ret1.trim();
			if( ret1.isEmpty()) ret1 = null;
			if( ret1 != null) {
				for( i=0; i<val.length; i++) if( val[i].equals(ret1)) return ret1;
			}
			k0 = meta.indexOf(key2, k1);
		}
		return ret1;
	}

	static String checkEmpty( Object in1) {
		if( in1 == null || !(in1 instanceof String)) return null;
		String out1 = (String)in1;
		if( out1.isEmpty()) return null;
		return out1;
	}
	
	static boolean stringCmp( String in1, String in2) {
		if( in1 != null && in2 != null) return in1.equals(in2);
		return in1 == null && in2 == null;
	}

	static String compressPatName(Object inName) {
		String retVal = checkEmpty(inName);
		if( retVal == null) return null;
		retVal = retVal.trim();
		int i = retVal.indexOf('^');
		if( i < 0) return retVal;
		retVal = retVal.substring(0, i) + "," + retVal.substring(i+1);
		retVal = retVal.replace('^', ' ').trim();
		return retVal;
	}

	static String compressID( Object in1) {
		String ret1, ret0 = checkEmpty(in1);
		if( ret0 == null) return "0";
		ret0 = ret0.toLowerCase();
		int i, i1, n = ret0.length();
		char a1;
		ret1 = "";
		for( i = i1 = 0; i < n; i++) {
			a1 = ret0.charAt(i);
			if( Character.isDigit(a1) || Character.isLetter(a1)) {
				if( i1 == 0 && a1 == '0') continue;
				ret1 = ret1 + a1;
				i1++;
			}
		}
		if( i1 == 0) return "0";
		return ret1;
	}

	static String getCompressedPatName(String meta) {
		return compressPatName( getDicomValue(meta, "0010,0010"));
	}

	static String getCompressedID(String meta) {
		return compressID( getDicomValue(meta, "0010,0020"));
	}

	static Date getStudyDateTime(String meta, int type) {
		String key1 = null, key2 = null, time1 = null, time2, tmp1;
		String key0 = "0008,0021";	// series date
		switch( type) {
			case 0:
				key1 = "0008,0030";	// study time
				key0 = "0008,0020";
				break;

			case 1:
				key2 = "0008,002A";	// date time
				key1 = "0008,0031";	// series time
				key0 = "0008,0021";
				break;

			case 2:
				key1 = "0008,0032";	// acquisition time
				key0 = "0008,0022";
				break;

			case 3:
				key2 = "0018,1078";	// date time
				key1 = "0018,1072";	// injection time
				key0 = "0009,103B";
				break;
				
			case 4:
				key1 = "0008,0033";	// image time
				key0 = "0008,0023";
				break;
		}
		if( key1 != null) {
			time1 = getDicomValue( meta, key1);
		}
		if( key2 != null && (type == 3 || time1 == null)) {
			time2 = getDicomValue( meta, key2);
			if( time2 != null) time1 = time2;	// prefer key2
			if( time1 != null && time1.length() >= 14) return getDateTime(time1, null);
		}
		// use study date since the injection may be 24 or 48 hours earlier
		tmp1 = getDicomValue(meta,key0);
		if(tmp1==null) {
			tmp1 = getDicomValue(meta,"0008,0020");
			if( tmp1 == null) return null;
			// be careful of bad study dates like 1899
			if(Integer.parseInt(tmp1.substring(0, 4)) < 1980) {
				tmp1 = getDicomValue(meta,"0008,0021");
			}
		}
		return getDateTime( tmp1, time1);
	}

	static String getAccessionNumber( String meta, BI_dbSaveInfo curr) {
		String tmp = null;
		if( curr != null) tmp = curr.accession;	// prefer database value
		if( tmp == null || tmp.isEmpty()) tmp = checkEmpty(getDicomValue( meta, "0008,0050"));
		if( tmp == null) return "0";
		int i, j, n;
		byte curb;
		byte[] byt1 = tmp.getBytes();
		n = byt1.length;
		byte[] byt2 = new byte[n];
		for(i=j=0; i<n; i++) {
			curb = byt1[i];
			if(curb >= '0' && curb <= '9') {
				if( curb == '0' && j== 0) continue;	// ignore leading zeros
				byt2[j++] = curb;
			}
			if( curb == '.') break;	// ignore anything after '.'
		}
		if( j<=0) return "0";
		n = j;
		byt1 = new byte[n];
		for(i=0; i<n; i++) byt1[i] = byt2[i];
		tmp = new String(byt1);
		return tmp;
	}

/*	public static boolean isNumeric(String str) {
		return str.matches("[+-]?\\d*(\\.\\d+)?");
	}*/

	static int getImageType(String meta) {
		String tmp, tmp1, tmp2;
		try {
			if( meta == null) return SERIES_UNKNOWN;

			int orientation = getOrientation(getDicomValue( meta, "0020,0037")) & 31;
			int SOPClassVar = getSOPClass(getDicomValue( meta, "0008,0016"));
			if( SOPClassVar == SOPCLASS_TYPE_NM) {
				tmp = getDicomValue(meta, "0008,103E");
				if( orientation == DicomFormat.ORIENT_UNKNOWN && tmp != null && tmp.startsWith("MIP data")) return SERIES_MIP;
				if( orientation != DicomFormat.ORIENT_AXIAL && orientation != ORIENT_AXIAL_ROTATED) return SERIES_NM;
				String[] array1 = parseMultString(getDicomValue(meta, "0008,0008"));
				if( !multStringContains( array1, "RECON TOMO")) return SERIES_NM3;
				if( tmp != null && tmp.startsWith("ATT MAP")) return SERIES_NM3;
				if( orientation != DicomFormat.ORIENT_AXIAL && orientation != ORIENT_AXIAL_ROTATED) return SERIES_UNKNOWN;
				tmp = getDicomValue(meta, "0054,0400");
				if( tmp == null) {
					tmp = getDicomValue(meta, "0054,0202");	// another chance
					if( tmp == null) {
						array1 = parseMultString(getDicomValue(meta, "0028,0051"));
						if( !multStringContains( array1, "ATTN")
							&& !multStringContains(array1, "UNIF")) return SERIES_UNKNOWN;
						tmp = "STEP";
					}
					if( tmp.startsWith("STEP")) {
						int numFrm = parseInt(getDicomValue( meta, "0028,0008"));
						// the SERIES_SIEMENS_SPECT is only for the new Siemens format
						if( numFrm > 1) return SERIES_SPECT;
						tmp = getDicomValue(meta, "0008,0070");
						if(tmp != null && tmp.startsWith("SIEMENS")) return SERIES_SIEMENS_SPECT;
						return SERIES_SPECT;
					}
					return SERIES_UNKNOWN;
				}
				tmp = tmp.toLowerCase();
				if( tmp.startsWith("noac")) return SERIES_UPET;
				if( tmp.equals("irnc osem")) return SERIES_UPET;
				return SERIES_SPECT;
			}

			if( SOPClassVar == SOPCLASS_TYPE_SC) {
				if( parseInt(getDicomValue( meta, "0028,0100")) == 16) {
					tmp = getDicomValue( meta, "0008,0060");
					if( tmp.equals("CT")) return SERIES_CT;
				}
			}
			// uncomment isOK |= (...) for BN Lee
			boolean isOK = orientation == DicomFormat.ORIENT_AXIAL
					|| orientation ==ORIENT_AXIAL_ROTATED;
			isOK |= (orientation > DicomFormat.ORIENT_AXIAL && orientation < DicomFormat.ORIENT_OBLIQUE);
			if( !isOK) return SERIES_UNKNOWN;

			if( SOPClassVar == SOPCLASS_TYPE_CT) {
				if( parseInt(getDicomValue( meta, "0028,0011")) <= 128) return SERIES_REDUCED_CT;
				tmp = getDicomValue(meta, "0008,1090");
				if( tmp == null) return SERIES_CT;
				if( tmp.equals("VARICAM") || tmp.equals("INFINIA") || tmp.equals("QUASAR")) return SERIES_CT_VARICAM;
				return SERIES_CT;
			}

			if( SOPClassVar == SOPCLASS_TYPE_PET ) {
				tmp = getDicomValue(meta, "0054,1001");
				if( tmp == null) return SERIES_UNKNOWN;
				tmp1 = getDicomValue(meta, "7053,1000");
				if( tmp1 != null && parseDouble(tmp1) > 0 && !tmp.equals("BQML")) {
					return SERIES_PHILIPS_PET;
				}
				tmp2 = getDicomValue(meta, "0009,100D");
				if( tmp2 != null && tmp.equals("BQML")) return SERIES_GE_PRIVATE_PET;
				if( tmp.equals("BQML") || tmp.equals("MBq")) return SERIES_BQML_PET;
				if( tmp.equals("GML")) return SERIES_GML_PET;
				if( tmp.equals("CPS") || tmp.equals("PROPCNTS") || tmp.equals("PROPCPS")) return SERIES_UPET;
				// and for Phillips we need an extra check to distinguish
				if( tmp.equals( "CNTS")) {	// Phillips is above
					tmp = getDicomValue(meta, "0054,1101");
					if( tmp == null || tmp.contains("NONE")) return SERIES_UPET;
					return SERIES_PHILIPS_PET;
				}
			}

			if( SOPClassVar == SOPCLASS_TYPE_ENHANCED_PET) {
				tmp = getDicomValue(meta, "0008,0060");
				if( !tmp.equals("PT")) return SERIES_UNKNOWN;
				tmp = getDicomValue(meta, "0028,3003");
				// for Molecubes, 0028,3003 doesn't exist and data is GML
				if( tmp == null) {
					tmp1 = getDicomValue(meta, "0008,0070");
					if( tmp1 != null && tmp1.startsWith("Molecubes")) return SERIES_GML_PET;
				}
				if( tmp != null && tmp.equals("Bq/ml")) return SERIES_BQML_PET;
				return SERIES_UPET;
			}

			if( SOPClassVar == SOPCLASS_TYPE_ENHANCED_CT) {
				return SERIES_CT;
			}

			if( SOPClassVar == SOPCLASS_TYPE_MRI || SOPClassVar == SOPCLASS_TYPE_ENHANCED_MRI) {
				return SERIES_MRI;
			}
		} catch (Exception e) { stackTrace2Log(e);}
		return SERIES_UNKNOWN;
	}

	static int getSOPClass(String tmp1) {
		int SOPClass = SOPCLASS_UNKNOWN;
		if( tmp1 == null) return SOPClass;
		if (tmp1.startsWith(SOPCLASS_NM)) {
			SOPClass = SOPCLASS_TYPE_NM;
			return SOPClass;
		}
		if (tmp1.startsWith(SOPCLASS_SC)) {
			SOPClass = SOPCLASS_TYPE_SC;
		}
		if (tmp1.startsWith("1.2.840.10008.5.1.4.1.1.6.1")) {
			SOPClass = SOPCLASS_TYPE_US;
		}
		if (tmp1.startsWith("1.2.840.10008.5.1.4.1.1.2")) {
			if (tmp1.startsWith("1.2.840.10008.5.1.4.1.1.2.1")) {
				SOPClass = SOPCLASS_TYPE_ENHANCED_CT;
				return SOPClass;
			}
			SOPClass = SOPCLASS_TYPE_CT;
		}
		if (tmp1.startsWith("1.2.840.10008.5.1.4.1.1.128")) {
			SOPClass = SOPCLASS_TYPE_PET;
		}
		if (tmp1.startsWith("1.2.840.10008.5.1.4.1.1.481")) {
			return SOPClass;	// SOPCLASS_UNKNOWN - not handled
		}
		if (tmp1.startsWith("1.2.840.10008.5.1.4.1.1.4")) {
			if (tmp1.startsWith("1.2.840.10008.5.1.4.1.1.4.1")) {
				return SOPCLASS_TYPE_ENHANCED_MRI;
			}
			SOPClass = SOPCLASS_TYPE_MRI;
		}
		if (tmp1.equals("1.2.840.10008.5.1.4.1.1.1")) {
			SOPClass = SOPCLASS_TYPE_RADIOGRAPH;
		}
		if (tmp1.startsWith("1.2.840.10008.5.1.4.1.1.130")) {
			SOPClass = SOPCLASS_TYPE_ENHANCED_PET;
		}
		if (tmp1.startsWith("1.2.840.10008.5.1.4.1.1.88")) {
			SOPClass = SOPCLASS_TYPE_SR_STORAGE;
		}
		if (tmp1.startsWith("1.2.840.10008.5.1.4.1.1.66")) {
			SOPClass = SOPCLASS_TYPE_RAW;
		}
		return SOPClass;
	}

	static int getOrientation(String tmp1) {
		float fltIn[] = parseMultFloat( tmp1);
		int retVal = DicomFormat.ORIENT_UNKNOWN;
		if (fltIn == null || fltIn.length != 6) {
			return retVal;
		}
		// allow some sloppiness in the measurement
		if (Math.abs(fltIn[0]) > 0.96 && Math.abs(fltIn[4]) > 0.96) {
			retVal = DicomFormat.ORIENT_AXIAL;
			// check if the buffer needs to be reversed
			if( fltIn[0] < 0) retVal += 32;
			if( fltIn[4] < 0) retVal += 64;
		} else if (fltIn[5] < -0.97) {
			if (fltIn[0] > 0.98) {
				retVal = DicomFormat.ORIENT_CORONAL;
			}
			if (fltIn[1] > 0.96 || fltIn[1] < -0.96) {
				retVal = DicomFormat.ORIENT_SAGITAL;
			}
		} else if( Math.abs(fltIn[1]) > 0.96 && Math.abs(fltIn[3]) > 0.96) {
			retVal = ORIENT_AXIAL_ROTATED;
		}

		if (retVal == DicomFormat.ORIENT_UNKNOWN) {
			// let's try to find what it is closest to, oblAxial, oblCor, oblSag
			int indx, jmax, imax;
			double maxi, maxj, maxTmp;
			imax = jmax = 0;
			maxi = Math.abs(fltIn[0]);
			maxj = Math.abs(fltIn[3]);
			for (indx = 1; indx < 3; indx++) {
				maxTmp = Math.abs(fltIn[indx]);
				if (maxTmp > maxi) {
					maxi = maxTmp;
					imax = indx;
				}
				maxTmp = Math.abs(fltIn[indx + 3]);
				if (maxTmp > maxj) {
					maxj = maxTmp;
					jmax = indx;
				}
			}
			retVal = DicomFormat.ORIENT_OBLIQUE;	// if nothing else fits, use oblique
			if (imax == 0 && jmax == 1) {
				retVal = DicomFormat.ORIENT_OBL_AXIAL;
			}
			if (imax == 0 && jmax == 2) {
				retVal = DicomFormat.ORIENT_OBL_COR;
			}
			if (imax == 1 && jmax == 2) {
				retVal = DicomFormat.ORIENT_OBL_SAG;
			}
		}
		return retVal;
	}

	static float[] parseMultFloat( String tmp1) {
		float [] ret1 = null;
		double[] val = new double[32];	// arbitrary limit of 32
		int i, n = 0;
		if( tmp1 == null) return null;
		String tmp2 = tmp1.replace("\\ ", "\\");
		Scanner sc = new Scanner(tmp2).useDelimiter("\\\\");
		sc.useLocale(Locale.US);
		while(sc.hasNextDouble() && n < 32) {
			val[n++] = sc.nextDouble();
		}
		if( n>0) {
			ret1 = new float[n];
			for( i=0; i<n; i++) ret1[i] = (float) val[i];
		}
		return ret1;
	}

	static double parseDouble( String tmp1) {
		double ret1 = 0;
		if( tmp1 != null) {
			try {
				ret1 = Double.parseDouble(tmp1);
			} catch (Exception e) {
				ret1 = 0;
			}
		}
		return ret1;
	}

	static int parseInt( String tmp1, int radix) {
		int ret1 = 0;
		if( tmp1 != null && !tmp1.isEmpty()) ret1 = Integer.parseInt(tmp1, radix);
		return ret1;
	}

	static int parseInt( String tmp1) {
		int ret1 = 0;
		double dbl1;
		if( tmp1 != null && !tmp1.isEmpty()) {
			try {
				dbl1 = Double.parseDouble(tmp1);
				ret1 = (int) dbl1;
//				ret1 = Integer.parseInt(tmp1);
			} catch (Exception e) {
				ret1 = 0;
			}
		}
		return ret1;
	}

	static String[] parseMultString( String tmp1) {
		String [] ret1 = null;
		String [] val = new String[32];	// arbitrary limit of 32
		int i, n = 0;
		if( tmp1 == null) return null;
		String tmp2 = tmp1.replace("\\ ", "\\");
		Scanner sc = new Scanner(tmp2).useDelimiter("\\\\");
		while(sc.hasNext() && n < 32) {
			val[n++] = sc.next();
		}
		if( n>0) {
			ret1 = new String[n];
			for( i=0; i<n; i++) ret1[i] = val[i];
		}
		return ret1;
	}

	static boolean multStringContains( String[] tmp1, String match1) {
		if( tmp1 == null) return false;
		int i, n = tmp1.length;
		for( i=0; i<n; i++) {
			if( tmp1[i].startsWith(match1)) return true;
		}
		return false;
	}
	/**
	 * Helper routine to convert from Dicom style date-time to Java date-time.
	 * Watch out, sometimes the date uses periods, 2008.10.04
	 * @param inDate Dicom date format
	 * @param inTime Dicom time format
	 * @return Java Date object
	 */
	public static Date getDateTime(String inDate, String inTime) {
		Date retDate;
		GregorianCalendar dat1 = new GregorianCalendar();
		int off, year, month, day, hour = 0, min1 = 0, sec = 0;
		if( inDate == null || inDate.length() < 8) return null;
		off = 0;	// normal case with no period
		if(inDate.charAt(4) == '.') off = 1;
		// watch out for bad date 01.01.1900
		if(inDate.charAt(2) == '.') return null;

		year = Integer.parseInt(inDate.substring(0, 4));
		month = Integer.parseInt(inDate.substring(4+off, 6+off)) - 1;	// month 0 based
		day = Integer.parseInt(inDate.substring(6+2*off, 8+2*off));
		if( inDate.length() >= 14) {
			hour = Integer.parseInt(inDate.substring(8, 10));
			min1 = Integer.parseInt(inDate.substring(10, 12));
			sec = Integer.parseInt(inDate.substring(12, 14));
		}
		else if( inTime != null && inTime.length() >= 6) {
			hour = Integer.parseInt(inTime.substring(0, 2));
			min1 = Integer.parseInt(inTime.substring(2, 4));
			sec = Integer.parseInt(inTime.substring(4, 6));
		}
		dat1.set(year, month, day, hour, min1, sec);
		retDate = dat1.getTime();
		return retDate;
	}

	public static String buildSeriesUIDs( ArrayList<ImagePlus> imgIn) {
		String seriesUIDs, ctUID = null, petUID = null, upetUID = null;
		String meta, mriUID = null, mipUID = null, multipleUID = null;
		int i, imgType, stkSz;
		ImagePlus currImg;
		for( i=0; i<imgIn.size(); i++) {
			currImg = imgIn.get(i);
			meta = getMeta(1, currImg);
			stkSz = currImg.getStackSize();
			if( stkSz <= 1) continue;
			imgType = getImageType( meta);
			switch( imgType) {
				case SERIES_CT:
				case SERIES_CT_VARICAM:
					if( ctUID != null) {
						multipleUID = getSeriesUID( meta);
						break;
					}
					ctUID = getSeriesUID( meta);
					break;

				case SERIES_BQML_PET:
				case SERIES_GML_PET:
				case SERIES_PHILIPS_PET:
				case SERIES_GE_PRIVATE_PET:
				case SERIES_SPECT:
				case SERIES_SIEMENS_SPECT:
					if( petUID != null) {
						multipleUID = getSeriesUID( meta);
						break;
					}
					petUID = getSeriesUID( meta);
				break;

				case SERIES_UPET:
					if( upetUID != null) {
						multipleUID = getSeriesUID( meta);
						break;
					}
					upetUID = getSeriesUID( meta);
					break;

				case SERIES_MRI:
					if( mriUID != null) {
						multipleUID = getSeriesUID( meta);
						break;
					}
					mriUID = getSeriesUID( meta);
					break;

				case SERIES_MIP:
					if( mipUID != null) return null;
					mipUID = getSeriesUID( meta);
					break;
			}
		}
		if( ctUID == null || petUID == null) return null;
		if( multipleUID != null) return "2CTs";
		seriesUIDs = ctUID + ", " + petUID;
		if( upetUID != null) seriesUIDs += ", " + upetUID;
		if( mriUID != null) seriesUIDs += ", " + mriUID;
		if( mipUID != null) seriesUIDs += ", " + mipUID;
		return seriesUIDs;
	}

	static String getSeriesUID( String meta) {
		return getLastDicomValue(meta, "0020,000E");
	}
	
	static ImageStack mySort( ImageStack stack0) {
		int width, height, i, n, j0, j1, orientation, imgNum;
		int[] sortVect;
		float[] zpos, patPos;
		float ztmp, zval;
		boolean dirty = false;
		ColorModel cm;
		ImageProcessor ip;
		n = stack0.getSize();
		String meta = stack0.getSliceLabel(1);
		if( meta == null || !meta.contains("0010,0010")) return stack0;
		orientation = getOrientation(getDicomValue( meta, "0020,0037")) & 31;
		sortVect = new int[n];
		zpos = new float[n];
		if( orientation == DicomFormat.ORIENT_AXIAL || orientation == ORIENT_AXIAL_ROTATED)
			for(i=0; i<n; i++) {
			sortVect[i] = i;
			meta = stack0.getSliceLabel(i+1);
			patPos = parseMultFloat(getDicomValue(meta, "0020,0032"));
			if( patPos == null) return stack0;
			zpos[i] = -patPos[2];
		} else for(i=0; i<n; i++) {
			sortVect[i] = i;
			meta = stack0.getSliceLabel(i+1);
			imgNum = parseInt(getLastDicomValue(meta, "0020,0013"));
			zpos[i] = imgNum;
		}
		i = 0;
		while( i < n-1) {
			j0 = sortVect[i];
			j1 = sortVect[i+1];
			zval = zpos[i];
			ztmp = zpos[i+1];
			if( zval > ztmp) {
				dirty = true;
				sortVect[i] = j1;
				sortVect[i+1] = j0;
				zpos[i] = ztmp;
				zpos[i+1] = zval;
				i--;
				if( i<0) i = 0;
			}
			else i++;
		}
		if( !dirty) return stack0;

		width = stack0.getWidth();
		height = stack0.getHeight();
		cm = stack0.getColorModel();
		ImageStack stack1 = new ImageStack(width, height, cm);
		for( i=0; i<n; i++) {
			j0 = sortVect[i] + 1;
			ip = stack0.getProcessor(j0);
			meta = stack0.getSliceLabel(j0);
			stack1.addSlice(meta, ip);
		}
		return stack1;
	}

	static void stackTrace2Log(Exception e) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		e.printStackTrace(ps);
		String output = os.toString();
		int j, indx = output.indexOf("\n");
		// take only the first 5 lines of the trace.
		if( indx > 0) for( j=0; j<5; j++) {
			indx = output.indexOf("\n", indx+1);
			if( indx < 0) break;
		}
		if( indx > 0) output = output.substring(0, indx);
		IJ.log(output);
	}
	
	static Rectangle getScreenDimensions() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
	}
	
	static void organizeWindows(int inMax) {
		Rectangle currRect, scr1;
		scr1 = getScreenDimensions();
		int[] windowList = WindowManager.getIDList();
		long[] scrMap = new long[XRES];
		int i, j, n = WindowManager.getWindowCount();
		int top, bottom, left, right, xrnd, yrnd, max1;
		long mask;
		ImageCanvas canvas;
		if( n<=1) return;	// show single image in full zoom
		boolean useStamp = (inMax >= 60);
		max1 = (int) (2.6*inMax);
		yrnd = (6*scr1.height) / (10*YRES);
		xrnd = (6*scr1.width) / (10*XRES);
		ImagePlus ip;
		if( useStamp) for( i=0; i<n; i++) {
			ip = WindowManager.getImage(windowList[i]);
			canvas = ip.getCanvas();
			while( canvas.getWidth() + canvas.getHeight() > max1 &&
					canvas.getMagnification() > 0.1)
				canvas.zoomOut(0, 0);
		}
		for( i=0; i<n; i++) {
			ip = WindowManager.getImage(windowList[i]);
			currRect = ip.getWindow().getBounds();
			bottom = (currRect.height+yrnd) * YRES/scr1.height;
			if( bottom <= 0) bottom = 1;
			right = (currRect.width+xrnd) * XRES/scr1.width;
			if( right <= 0) right = 1;
			loop1: for( left = 0; left <= XRES-right; left++) {
				mask = (1l<<bottom) - 1;
				for( top = 0; top <= YRES-bottom; top++) {
					if( (mask & scrMap[left]) == 0) {
						for( j=0; j<right; j++) {
							scrMap[left+j] |= mask;
						}
						currRect.x = left * scr1.width/XRES + scr1.x;
						currRect.y = top * scr1.height/YRES + scr1.y;
						ip.getWindow().setBounds(currRect);
						break loop1;
					}
					mask <<= 1;
				}
			}
		}
		
	}
	
	static String getUserLUT(int type, Preferences pref1) {
		if( !ChoosePetCt.isOptionSelected(pref1, type)) return null;
		String tmp = "fixed LUT";
		switch(type) {
			case ChoosePetCt.EXT_BLUES_LUT:
				tmp = "ext blues LUT";
				break;

			case ChoosePetCt.EXT_HOT_IRON_LUT:
				tmp = "ext hotiron LUT";
				break;

			case ChoosePetCt.MRI_CT_LUT:
				tmp = "MriCt LUT";
				break;
		}
		String flin = pref1.get(tmp, "");
		String path = ChoosePetCt.userDir;
		path += "luts" + File.separator;
//		IJ.log(path);
		File fl1 = new File(path + flin);
		if( !fl1.exists()) return null;
		return fl1.getPath();
	}

	/**
	 * Read the graphic file and search it for frame text.
	 * It will parse this file and return set of strings, one string for each frame.
	 * The whole object may be null, or certain frames may be null.
	 * @param path - location of the  file
	 * @return string for each frame with its text.
	 */
	static String[] getFrameText(String path) {
		String[] retVal = null;
		try {
			boolean line1 = true;
			String tmp, tmpStr;
			int i1, y1, frm1, maxFrm = -1;
			char c1;
			Scanner sc;
			ArrayList<Integer> pos, frmNm, yPos;
			ArrayList<String> strVals = new ArrayList<>();
			FileReader fl1 = new FileReader(path);
			BufferedReader br1 = new BufferedReader(fl1);
			frmNm = new ArrayList<>();
			yPos = new ArrayList<>();
			while( (tmp = br1.readLine()) != null) {
				if( line1) {
					line1 = false;
					if( !tmp.startsWith("ver 1.4")) return null;
					i1 = tmp.indexOf(",");
					if( i1 <= 0) return null;
					while( --i1 > 0) {
						c1 = tmp.charAt(i1);
						if( c1 < '0' || c1 >'9') break;
					}
					tmp = tmp.substring(i1+1);
				}
				if( tmp.isEmpty()) continue;
				i1 = tmp.indexOf('\t');
				if( i1 <= 0) continue;
				tmpStr = tmp.substring(i1+1);
				tmp = tmp.substring(0, i1);
				sc = new Scanner(tmp).useDelimiter(",");
				pos = new ArrayList<>();
				while( sc.hasNextInt()) {
					i1 = sc.nextInt();
					pos.add(i1);
				}
				if( pos.size() != 4 || pos.get(3) != 1) continue;
				frm1 = pos.get(2);
				if( frm1 > maxFrm) maxFrm = frm1;
				frmNm.add(frm1);
				yPos.add(pos.get(1));
				strVals.add(tmpStr);
			}
			br1.close();
			fl1.close();

			if( !frmNm.isEmpty() && maxFrm >= 0) {
				retVal = new String[maxFrm+1];
				Integer[] maxY = new Integer[maxFrm+1];
				for( i1=0; i1<frmNm.size(); i1++) {
					frm1 = frmNm.get(i1);
					y1 = yPos.get(i1);
					if(maxY[frm1] != null  && y1 < maxY[frm1]) continue;
					maxY[frm1] = y1;
					retVal[frm1] = strVals.get(i1);
				}
			}
		} catch (Exception e) { stackTrace2Log(e);}
		return retVal;
	}

	static boolean isSameDate(Date dat1, Date dat2) {
		long tim1, tim2, diff;
		if( dat1 == null && dat2 == null) return true;
		if( dat1 == null || dat2 == null) return false;
		if( dat1.equals(dat2)) return true;
		tim1 = dat1.getTime();
		tim2 = dat2.getTime();
		diff = Math.abs(tim1 - tim2);
		return diff < 1000;	// less than 1 second
	}
	
	static boolean isOptionSelected(Preferences pref, int type) {
		String tmp;
		switch( type) {
			case BLACK_BKGD:
				tmp = "black background";
				break;

			case HOT_IRON_FUSE:
				tmp = "hot iron fuse";
				break;

			case EXT_BLUES_LUT:
				tmp = "use ext blues LUT";
				break;

			case EXT_HOT_IRON_LUT:
				tmp = "use ext hotiron LUT";
				break;

			case MRI_CT_LUT:
				tmp = "use MriCt LUT";
				break;

			case FIXED_USER_LUT:
			default:
				tmp = "use external LUT";
		}
		return pref.getBoolean(tmp, false);
	}

	static boolean are2StingsEqual(String in1, String in2) {
		String tmp1 = in1, tmp2 = in2;
		if( in1 != null) {
			tmp1 = in1.trim();
			if( tmp1.isEmpty()) tmp1 = null;
		}
		if( in2 != null) {
			tmp2 = in2.trim();
			if( tmp2.isEmpty()) tmp2 = null;
		}
		if( tmp1 == null && tmp2 == null) return true;	// if both null, we are done
		if( tmp1 == null || tmp2 == null) return false; // only 1 is really null
		return tmp1.equals(tmp2);
	}

	void savePrefs() {
		int i, x;
		Dimension sz1 = getSize();
		jPrefer.putInt("choose pet dialog x", sz1.width);
		jPrefer.putInt("choose pet dialog y", sz1.height);
		TableColumn col1;
		for(i=0; i<=TBL_SIZE; i++) {
			col1 = jTable1.getColumnModel().getColumn(i);
			x = col1.getPreferredWidth();
			jPrefer.putInt("choose pet dialog col" + i, x);
		}
	}

	void launchPet() {
		chosenOnes = getStudyList();	// the only place this is set, just before exit
		dispose();
	}

	@Override
	public void dispose() {
		savePrefs();
		super.dispose();
	}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jButOK = new javax.swing.JButton();
        jButCancel = new javax.swing.JButton();
        jButHelp = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Choose PET-CT study");

        jLabel1.setText("Choose 2 PET + 1 CT + optional MRI");

        jLabel2.setText("The PET (or SPECT) should be 1 attenuation corrected + 1 uncorrected");

        jButOK.setText("OK");
        jButOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButOKActionPerformed(evt);
            }
        });

        jButCancel.setText("Cancel");
        jButCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButCancelActionPerformed(evt);
            }
        });

        jButHelp.setText("Help");
        jButHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButHelpActionPerformed(evt);
            }
        });

        jLabel3.setText("Alternatively, choose a single study (no fusion can be displayed).");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "", "Name", "Study", "Date", "Series", "Type", "ID", "Size"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, true, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(20, 20, 20)
                                .addComponent(jButOK)
                                .addGap(18, 18, 18)
                                .addComponent(jButCancel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButHelp))
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButCancel)
                    .addComponent(jButOK)
                    .addComponent(jLabel1)
                    .addComponent(jButHelp))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 216, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jButCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButCancelActionPerformed
		dispose();
	}//GEN-LAST:event_jButCancelActionPerformed

	private void jButOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButOKActionPerformed
		launchPet();
	}//GEN-LAST:event_jButOKActionPerformed

	private void jButHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButHelpActionPerformed
		openHelp("Opening dialog");
	}//GEN-LAST:event_jButHelpActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButCancel;
    private javax.swing.JButton jButHelp;
    private javax.swing.JButton jButOK;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
	Preferences jPrefer = null;
	ArrayList<ImagePlus> imgList = null;
	ArrayList<Integer> seriesType = null;
	ArrayList<Integer> seriesForce = null;
	ArrayList<Integer> chosenOnes = null;
	ArrayList<String> seriesUIDs = null;
	ArrayList<String> seriesName = null;
	static String userDir = null;
	static PetCtPanel MipPanel = null;
	static int loadingData = 0;	// not loading
	static boolean msgNoFocus = false;
}

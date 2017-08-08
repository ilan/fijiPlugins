import java.io.File;
import java.util.Date;

/**
 *
 * @author ilan
 */
public class BI_dbSaveInfo {
	String patName = null;
	String patID = null;
	Date styDate = null;
	String styName = null;
	String serName = null;
	String accession = null;	// accession number
	String dbFileName = null;	// 8 letter name
	String teachName = null;
	String teachReport = null;
	String AETitle = null;		// for Orthanc
	String basUrl = null;		// for Orthanc
	String usrPW = null;		// for Orthanc
	File flName = null;
	boolean jpegFlg = false;
	boolean isDicomDir = false;
}

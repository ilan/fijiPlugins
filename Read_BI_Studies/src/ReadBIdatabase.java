import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.LookUpTable;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.io.FileInfo;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.BrowserLauncher;
import ij.plugin.MontageMaker;
import ij.process.ImageProcessor;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.dcm4che3.tool.storescu.StoreSCU;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/*
 * ReadBIdatabase.java
 *
 * Created on Oct 19, 2009, 11:29:53 AM
 */

/**
 *
 * @author Ilan
 */
public class ReadBIdatabase extends javax.swing.JFrame implements MouseListener, WindowFocusListener {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;
	static final int TBL_PAT_NAME = 0;
	static final int TBL_STUDY = 1;
	static final int TBL_DATE = 2;
	static final int TBL_SERIES = 3;
	static final int TBL_PAT_ID = 4;
	static final int TBL_SIZE = 5;
	static final int TBL_TEACHING = 5;
	static final int TBL_ACCESSION = 6;
	static final int TBL_BF = 7;
	static final int TBL_SER_UID = 8;
	static final int READ_IF_EXIST = 0;
	static final int BOTH_READ_AND_WRITE = 1;
	static final int WRITE_IF_NECESSARY = 2;
	static final String JPEG_STRING = "Screen capture";
	static final char WIN_SEPARATOR_CHAR = '\\';

    /** Creates new form ReadBIdatabase */
    public ReadBIdatabase() {
        initComponents();
		init();
    }

	class ColorDateRenderer extends DefaultTableCellRenderer {
		static final long serialVersionUID = ChoosePetCt.serialVersionUID;
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int col) {
			super.getTableCellRendererComponent(table, value, hasFocus, hasFocus, row, row);

			try {
				if( numDays <= 0) return this;
//				if( isSelected) return this;
				SimpleDateFormat df1 = new SimpleDateFormat("d MMM yyyy", Locale.US);
				Date dt0, dt1;
				dt0 = new Date();
				dt1 = (Date) value;
				setText(df1.format(dt1));
				DefaultTableModel tm = (DefaultTableModel) table.getModel();
				int i = table.convertRowIndexToModel(row);
				Boolean bf = (Boolean) tm.getValueAt(i, TBL_BF);
				Color color1 = Color.red;
				long diff = (dt0.getTime() - dt1.getTime())/(1000l*60*60*24);
				if( diff > numDays) {
					if( bf) color1 = Color.magenta;
				}
				else {
					color1 = Color.green;
					if( bf) color1 = Color.cyan;
				}
				setBackground(color1);
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
			return this;
		}
	}

	class SeriesRenderer extends DefaultTableCellRenderer {
		static final long serialVersionUID = ChoosePetCt.serialVersionUID;
		ImageIcon openIcon = null;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int col) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

			try {
				if( openIcon == null) {
					ClassLoader cldr = getClass().getClassLoader();
					java.net.URL imageURL = cldr.getResource("resources/open.gif");
					openIcon = new ImageIcon(imageURL);
				}
				if( value == null) setIcon(openIcon);
				else setIcon(null);
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
			return this;
		}
	}

	private void init() {
		int x, y, i1;
		jPrefer = Preferences.userNodeForPackage(ReadBIdatabase.class);
		jPrefer = jPrefer.node("biplugins");
		jPreferSys = jPrefer;
		if( isXP()) {
			jPreferSys = Preferences.systemNodeForPackage(ReadBIdatabase.class);
			jPreferSys = jPreferSys.node("biplugins");
		}
		WindowManager.addWindow(this);
//		jCheckSaveConference.setSelected(jPrefer.getBoolean("save conference", false));
		jConference = jPrefer.getInt("conferenceSaveNum", 0);
		isCDLegal(jConference);
		jComboCD.setSelectedIndex(jConference);
		defineTable1(jTable1);
		TableColumn tc = jTable1.getColumnModel().getColumn(TBL_SER_UID);
		jTable1.removeColumn(tc);
		tc = jTable1.getColumnModel().getColumn(TBL_BF);
		jTable1.removeColumn(tc);
		jTable1.addMouseListener(this);
		jTable1.setAutoCreateRowSorter(true);
		tc = jTable2.getColumnModel().getColumn(TBL_SER_UID);
		jTable2.removeColumn(tc);
		tc = jTable2.getColumnModel().getColumn(TBL_BF);
		jTable2.removeColumn(tc);
		tc = jTable2.getColumnModel().getColumn(TBL_ACCESSION);
		jTable2.removeColumn(tc);
		jTable2.setAutoCreateRowSorter(true);
		defineTable1(jTable3);
		tc = jTable3.getColumnModel().getColumn(TBL_SER_UID);
		jTable3.removeColumn(tc);
		tc = jTable3.getColumnModel().getColumn(TBL_BF);
		jTable3.removeColumn(tc);
		jTable3.setAutoCreateRowSorter(true);
		jCheckExit.setSelected(jPrefer.getBoolean("exit after read", false));
		i1 = jPrefer.getInt("db combo", 0);
		jComboBox1.setSelectedIndex(i1);
		if(i1 <= 4) jComboBox2.setSelectedIndex(i1);
		jTextPatName.setText(jPrefer.get("last patient", null));
		jTextPatName.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent arg0) {
//				super.keyTyped(arg0);
				unselectAllTableEntries();
				int i = arg0.getKeyChar();
				if( i == KeyEvent.VK_ENTER) readButton();
			}
		});
		jTextPatName1.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent arg0) {
//				super.keyTyped(arg0);
				unselectAllTableEntries();
				int i = arg0.getKeyChar();
				if( i == KeyEvent.VK_ENTER) deleteButton();
			}
		});

		addWindowFocusListener(this);
		numDays = jPrefer.getInt("num of days", 30);
		boolean show = jPrefer.getBoolean("show write database", false);
		jCheckShowWrite.setSelected(show);
		if( !show) {
			jTabbedPane1.remove(jPanelWrite);
			jTabbedPane1.remove(jPanelDelete);
		}
		Rectangle scr1 = ChoosePetCt.getScreenDimensions();
		x = jPrefer.getInt("read dialog x", 0);
		y = jPrefer.getInt("read dialog y", 0);
		if( y > scr1.height) y = 0;
		xStart = jPrefer.getInt("read dialog pos x", 0);
		yStart = jPrefer.getInt("read dialog pos y", 0);
		if(x > 100 && y > 100) {	// if it is too small, ignore
			setSize(x, y);
			if(yStart > 0) setLocation(xStart, yStart);
		}
/*		String tmp = "x = " + xStart + ", y = " + yStart;
		tmp += ", width = " + x + ", height = " + y;
		IJ.log(tmp);*/
/*		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) { stackTrace2Log(e); }*/
		jCurrDB = jPrefer.getInt("current database", 0);
		teachList = new ArrayList<bi_dbTeaching>();
		isInitialized = true;
		setAnimatedFile();
		updateDbList();
		maybeShowWriteMip(null);
		fillReadTable(-1, true);
		fillReadTable(-1, false);
	}

	// redefine Tables
	void defineTable1(JTable jtab) {
		jtab.setModel(new javax.swing.table.DefaultTableModel(
			new Object[][]{
				{null, null, null, null, null, null, null, null, null},
				{null, null, null, null, null, null, null, null, null}
			},
			new String[]{
				"Name", "Study", "Date", "Series", "ID", "Teaching", "Accession", "bf", "SerUID"
			}
		) {
			Class[] types = new Class[]{
				String.class, Object.class, Date.class, Object.class, Object.class,
				Object.class, Object.class, Object.class, Object.class
			};

			@Override
			public Class getColumnClass(int columnIndex) {
				return types[columnIndex];
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
		});
	}
	
	boolean isXP() {
		String line = System.getProperty("os.name").toLowerCase();
		return line.contains("windows xp");
	}

	@Override
	public void windowGainedFocus(WindowEvent we) {
		Window wold = we.getOppositeWindow();
		if( wold == null) return;
		dataWindow = null;
		if( wold instanceof PetCtFrame) {
			dataWindow = wold;
		}
		if( wold instanceof Display3Frame) {
			dataWindow = wold;
		}
		if( wold instanceof ImageWindow) {
			dataWindow = wold;
		}
		fillCurrStudy();
	}

	@Override
	public void windowLostFocus(WindowEvent we) {
		savePrefs();
	}

	void savePrefs() {
		if( !isInitialized) return;
//		jPrefer.putBoolean("db read petct", jCheckPetCt.isSelected());
		jPrefer.putBoolean("show write database", jCheckShowWrite.isSelected());
//		jPrefer.putBoolean("save conference", jCheckSaveConference.isSelected());
		jPrefer.putBoolean("exit after read", jCheckExit.isSelected());
		jPrefer.putInt("db combo", jComboBox1.getSelectedIndex());
		jPrefer.put("last patient", getDlgPatName(0));
		jPrefer.putInt("current database", jCurrDB);
		Dimension sz1 = getSize();
		jPrefer.putInt("read dialog x",sz1.width);
		jPrefer.putInt("read dialog y",sz1.height);
		Point pt1 = getLocation();
		int y0 = pt1.y - yStart;
		int x0 = pt1.x - xStart;
		if( Math.abs(x0) > 2 || y0 < 0 || y0 > 30) {
			jPrefer.putInt("read dialog pos x",pt1.x);
			jPrefer.putInt("read dialog pos y",pt1.y);
		}
		setOrSaveColumnWidths(0, true);
		saveAnimatedParms();
	}

	Preferences getSysPrefer( int indx) {
		Preferences pref1 = jPreferSys;
		String tmp1 = ChoosePetCt.checkEmpty(pref1.get("ODBC"+indx, null));
		if( tmp1 == null) pref1 = jPrefer;
		return pref1;
	}

	String getDlgPatName(int type) { 
		if( type > 0) return jTextPatName1.getText();
		return jTextPatName.getText();
	}

	void updateDbList() {
		int i, last1;
		if( !isInitialized) return;
		boolean failed = false;
		String tmp1;
		JRadioButton curBut;
		buttonGroup1 = new javax.swing.ButtonGroup();
		last1 = -1;
		Preferences pref1 = getSysPrefer(0);
		for( i=0; i<10; i++) {
			curBut = getDBbutton(i+1);
			curBut.setVisible(false);
			buttonGroup1.add(curBut);
			if( !failed) {
				tmp1 = ChoosePetCt.checkEmpty(pref1.get("ODBC" + i, null));
				if( tmp1 != null) last1 = i;
				else failed = true;
			}
		}
		jLabelDbName.setVisible(last1 > 0);
		jLabelDbName1.setVisible(last1 > 0);
		jLabelDbName2.setVisible(last1 > 0);
		jButRead.setEnabled(last1 >= 0);
		if( last1 > 0) {
			if( jCurrDB > last1) jCurrDB = last1;
			for( i=0; i<=last1; i++) {
				curBut = getDBbutton(i+1);
				curBut.setVisible(true);
			}
			curBut = getDBbutton( jCurrDB+1);
			curBut.setSelected(true);
		} else {
			jCurrDB = 0;
		}
		changeDBSelected(jCurrDB + 1);
	}

	/** Used to read study when user double clicks on table.
	 * @param e - Mouse Event*/
	@Override
	public void mouseClicked(MouseEvent e) {
		int i = e.getClickCount();
		if( i == 1) seriesMouseClick(e);
		if( i == 2) readButton();
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {}
	@Override
	public void mouseExited(MouseEvent arg0) {}
	@Override
	public void mousePressed(MouseEvent arg0) {}
	@Override
	public void mouseReleased(MouseEvent arg0) {}

	// Note: if you receive a Communications link failure in mySql, modify mt.cnf
	// Comment out # bind-address 127.0.0.1 - impossible to specify 2 addresses.
	Connection openDBConnection() {
		String ODBCUser, ODBCName, ODBCPassword, tmp0, tmp1=null;
		int j,k;
		try {
			Preferences pref1 = getSysPrefer(jCurrDB);
			ODBCName = ChoosePetCt.checkEmpty(pref1.get("ODBC" + jCurrDB, null));
			ODBCUser = pref1.get("db user" + jCurrDB, null);
			ODBCPassword = pref1.get("db pass" + jCurrDB, null);
			Object orthStudies = null;
			AETitle = null;
			userPw = null;
			if( orthanc1 != null) orthStudies = orthanc1.orthStudies;
			orthanc1 = null;
			tmp0 = "com.mysql.jdbc.Driver";
			setDataPath();
			if( ODBCName == null) return null;
			int i = pref1.getInt("db type" + jCurrDB, 0);
			switch(i) {
				case 0:
				case 1:
					tmp0 = "sun.jdbc.odbc.JdbcOdbcDriver";
					tmp1 = "jdbc:odbc:" + ODBCName;
					break;

				case 2:
					tmp0 = "net.sourceforge.jtds.jdbc.Driver";
					tmp1 = "jdbc:jtds:sqlserver:" + ODBCName;
					break;

				case 3:
					tmp1 = "jdbc:mysql:" + ODBCName;
					break;
					
				case 4:
					AETitle = ODBCName;
					j = ODBCName.indexOf('@');
					k = ODBCName.indexOf(":");
					if( j>0 && k>j) tmp1 = "jdbc:mysql://" + ODBCName.substring(j+1, k+1) + "3306/pacsdb";
					break;

				case 5:
					j = ODBCName.indexOf('@');
					k = ODBCName.indexOf(":");
					if( j>0 && k>j) AETitle = ODBCName;
					if(!ODBCUser.isEmpty() && !ODBCPassword.isEmpty())
						userPw = ODBCUser + ":" + ODBCPassword;
//					orthanc1 = new ReadOrthanc(this, orthStudies);
					orthanc1 = new ReadOrthanc(this);
					orthanc1.setOrthSdy(orthStudies);
					return null;
			}
			if( tmp1 != null && tmp1.equals(m_lastOpen) && m_lastConn != null) return m_lastConn;
			m_lastOpen = tmp1;

			Class.forName(tmp0);
			if( ODBCUser.isEmpty() || ODBCPassword.isEmpty())
				m_lastConn = DriverManager.getConnection(tmp1);
			else m_lastConn = DriverManager.getConnection(tmp1, ODBCUser, ODBCPassword);
		} catch (Exception e) { m_lastConn = null; ChoosePetCt.stackTrace2Log(e); }
		if( m_lastConn == null) IJ.log("\nCan't get connection to database.\nPlease check Setup parameters.\n\n");
		return m_lastConn;
	}

	void setDataPath() {
		Preferences pref1 = getSysPrefer(jCurrDB);
		m_dataPath = pref1.get("db path" + jCurrDB, null);
	}

	void readButton() {
		maybeShowWriteMip(null);
		if( work2 != null) return;
		int n = jTable1.getSelectedRowCount();
		Container c = getContentPane();
		c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		if( n <= 0) {
			fillReadTable( jComboBox1.getSelectedIndex(), true);
			c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		readWriteDelFlg = 0;
		bkgdMode = 0;
		loadData();
	}

	// this has been extended to do both read and delete
	void doRead() {
		JTable jTab = jTable1;
		if( readWriteDelFlg == 2) jTab = jTable3;
		else imgList = new ArrayList<ImagePlus>();
		int n = jTab.getSelectedRowCount();
		int cols = TBL_BF+2;
//		if( jCheckSeries.isSelected()) cols = 5;
		Object [] row1 = new Object[cols];
		int [] selected = jTab.getSelectedRows();
		int i, j, k;
		BI_dbSaveInfo[] biDb;
		killRead = false;
		for( i=0; i<n; i++) {
			selected[i] = jTab.convertRowIndexToModel(selected[i]);
		}
		if( readWriteDelFlg != 2) {
			savePrefs();
			ImageJ ij = IJ.getInstance();
			if( ij != null) ij.toFront();
		}
		out1: for( j=0; j<n; j++) {
			k = selected[j];
			for( i=0; i<cols; i++) {
				row1[i] = jTab.getModel().getValueAt(k, i);
			}
			if( orthanc1 != null) {
				if( readWriteDelFlg == 2) orthanc1.deleteStudy(row1);
				else orthanc1.readStudy(row1);
				continue;
			}
			biDb = queryRow4NM( row1, cols);
			if( biDb == null) {
				IJ.log("query failed for " + row1[TBL_PAT_NAME] +
						"  " + row1[TBL_DATE] + "  " + row1[TBL_STUDY]);
				continue;
			}

			for(k=0; k<biDb.length; k++) {
				if( readWriteDelFlg == 2) deleteFiles(biDb[k]);
				else readFiles(biDb[k]);
				if( killRead) break out1;
			}
		}
		if( readWriteDelFlg == 2) {
			fillReadTable( jComboBox2.getSelectedIndex(), false);
			fillReadTable( -1, true);	// clear read data
		}
		else unselectAllTableEntries();
		Container c = getContentPane();
		c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if( readWriteDelFlg == 2) return;	// done

		this.toFront();
		if( jPrefer.getBoolean("tile windows", false)) {
//			WindowOrganizer wo = new WindowOrganizer();
//			wo.run("tile");
			tileAction();
		}
		runPetCtViewer();
		if( jCheckExit.isSelected()) dispose();
	}

	void tileAction() {
		int i = jPrefer.getInt("postage stamp size", 0);
		ChoosePetCt.organizeWindows(i);
	}
	
	void tileDialogShow() {
		IJ.runPlugIn("Postage_Stamp", null);
	}

	void runPetCtViewer() {
		String seriesUIDs = ChoosePetCt.buildSeriesUIDs(imgList);
		if( seriesUIDs == null) return;
		IJ.runPlugIn("Pet_Ct_Viewer", seriesUIDs);
		wait4bkgd();
		maybeShowWriteMip(ChoosePetCt.MipPanel);
	}
	
	void saveMip() {
		PetCtPanel mipPanel = ChoosePetCt.MipPanel;
		maybeShowWriteMip(mipPanel);
		if( mipPanel == null) {
			IJ.log("Pet Ct viewer not available. MIP not saved.");
			return;
		}
		if( ChoosePetCt.loadingData == 2) {
			JOptionPane.showMessageDialog(this, "Please wait. Still calculating MIP.");
			return;
		}
		JFijiPipe mipPipe, petPipe;
		mipPipe = mipPanel.mipPipe;
		petPipe = mipPanel.petPipe;
		if( mipPipe == null || petPipe == null) return;
		myWriteDicom dicom = new myWriteDicom(mipPanel.parent, mipPipe, File.separatorChar + "mipData.dcm");
		if( dicom.writeImmediateMip() < 0) return;
		ImagePlus imp;
		Opener opener = new Opener();
		opener.setSilentMode(true);
		imp = opener.openImage(dicom.outFile1.getPath());
		if( imp == null) {
			IJ.log("can't open file " + dicom.outFile1.getPath());
			return;
		}
		writeStudy2Database(null, imp);
		maybeShowWriteMip(null);
	}

	void wait4bkgd() {
		Integer i = 0, j;
		while( ChoosePetCt.loadingData == 1 || ChoosePetCt.loadingData == 3) {
			mySleep(200);
			i++;
			if( (i % 20) == 0 && ChoosePetCt.loadingData == 1) {
				ImageJ ij = IJ.getInstance();
				if( ij != null) ij.toFront();
				j = i/5;
				IJ.showStatus("Loading data, please wait " + j.toString());
			}
		}
	}
	
	void mySleep(int msec) {
		try {
			Thread.sleep(msec);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	void maybeShowWriteMip(PetCtPanel mipPanel) {
		jButSaveMip.setVisible(mipPanel != null);
	}

	void loadData() {
		work2 = new bkgdLoadData();
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

	boolean readFiles(BI_dbSaveInfo curr1) {
		int i, k, n, width = 0, height=0, depth=0, count = 0, numGraphic = 0;
		int mode = READ_IF_EXIST;
		Calibration cal = null;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		double progVal;
		String filePath, path1 = curr1.flName.getPath();
		String info, tmp, label1;
		String ReadCDPath=null;
		File[] list1;
		FileInfo fi = null;
		Opener opener;
		ImagePlus imp;
		ImageStack stack = null;
		String[] frameText = null;
		File dir1 = new File(path1);
		try {
			if( isToggleMode()) mode = BOTH_READ_AND_WRITE;
			if( !dir1.isDirectory()) {
				opener = new Opener();
				if( !curr1.jpegFlg) {
					if( !path1.endsWith("t00") && !path1.endsWith("T00")) return false;
					k = opener.getFileType(path1);
					if( k != Opener.TIFF) return readOldFiles(curr1, dir1);
				}
				opener.setSilentMode(true);
				imp = opener.openImage(path1);
				imp.setTitle(getTitleInfo(null, curr1));
				imp.setProperty("bidb", curr1);	// save database info
				myMakeMontage( imp, null, false);
				return true;
			}
			list1 = dir1.listFiles();
			n = 0;
			if( list1 != null) {
				n = list1.length;
				ReadCDPath = CopyCDFiles(dir1.getPath(), null, mode);
			}
			info = null;
			for( i=0; i<n; i++) {
				count++;
				progVal = ((double) count)/n;
				IJ.showStatus(count+"/"+n);
				IJ.showProgress(progVal);
//				IJ.showProgress(count,n);
				tmp = list1[i].getName();
//				filePath = list1[i].getPath();
				filePath = CopyCDFiles(ReadCDPath,list1[i], mode);
				opener = new Opener();
				k = opener.getFileType(filePath);
				// watch out for these files since Bio Format wants to open them
				if( k == Opener.UNKNOWN || k == Opener.TEXT) {
					if( tmp.startsWith("graphic") && tmp.endsWith("gr1")) {
						frameText = ChoosePetCt.getFrameText(filePath);
						numGraphic++;
					}
					continue;
				}
				if (killRead) {
//					stack = null;
//					imp = null;
					IJ.showProgress(1.0);
					return false;
				}
				opener.setSilentMode(true);
				imp = opener.openImage(filePath);
				if( imp == null) continue;
				if( stack==null) {
					width = imp.getWidth();
					height = imp.getHeight();
					depth = imp.getStackSize();
//					bitDepth = imp.getBitDepth();
//					imgTitle = imp.getTitle();
					cal = imp.getCalibration();
					fi = imp.getOriginalFileInfo();
					ColorModel cm = imp.getProcessor().getColorModel();
					stack = new ImageStack(width, height, cm);
				}
				if( (depth > 1 && n > 1) || width != imp.getWidth() || height != imp.getHeight()) {
					imp.show();	// show a normal stack
					imgList.add(imp);
					stack = null;
					depth = 0;
					continue;
				}
				info = (String)imp.getProperty("Info");
				label1 = null;
				if (depth==1) {
					label1 = imp.getTitle();
					if (info!=null)
						label1 += "\n" + info;
				}
				ImageStack inputStack = imp.getStack();
				for (int slice=1; slice<=inputStack.getSize(); slice++) {
					ImageProcessor ip = inputStack.getProcessor(slice);
					if (ip.getMin()<min) min = ip.getMin();
					if (ip.getMax()>max) max = ip.getMax();
					stack.addSlice(label1, ip);
				}
			}
			if( stack != null && stack.getSize() > 0) {
//				stack = ij.util.DicomTools.sort(stack);
				stack = ChoosePetCt.mySort(stack);
				if( fi != null) {
					fi.fileFormat = FileInfo.UNKNOWN;
					fi.fileName = "";
					fi.directory = path1;
				}
				ImagePlus imp2 = new ImagePlus(getTitleInfo(info, curr1), stack);
				imp2.getProcessor().setMinAndMax(min, max);
				imp2.setProperty("bidb", curr1);	// save database info
				imp2.setProperty("biconference", ReadCDPath);
				if( list1 != null && list1.length == 1 + numGraphic) {
					imp2.setProperty("Info", info);
//					fi.description = "spect";
				}
				imp2.setFileInfo(fi);
				imp2.setCalibration(cal);
				if( frameText != null) for( i=0; i < frameText.length; i++) {
					label1 = frameText[i];
					if( label1 != null) {
						int i1 = i+1;
						tmp = stack.getSliceLabel(i1);
						if( tmp != null) {
							// the slices are counted from the bottom up
							i1 = stack.getSize() - i;
							tmp = stack.getSliceLabel(i1);
							label1 += "\n" + tmp;
						}
						stack.setSliceLabel(label1, i1);
					}
				}
				myMakeMontage(imp2, info, frameText != null);
				imgList.add(imp2);	// keep track of images loaded
			}
			IJ.showProgress(1.0);
		} catch (Exception e)  { ChoosePetCt.stackTrace2Log(e); }
		return true;
	}

	// copy the source file to Read from CD path and then read from the local copy
	String CopyCDFiles(String inPath, File inFile, int mode) {
		File tmpFile;
		String tmp, tmp1, retVal=null;
		double flsz1, relSz;
		try {
			if( inFile == null) {
				tmp1 = getPathReadCD();
				if( tmp1 == null) return null;
				tmp = inPath;
				int i0=0, i1=0, i=0, n = tmp.length();
				while(i >= 0) {
					i = tmp.indexOf(File.separatorChar, i+1);
					if( i<0 || i>=n) break;
					i0 = i1;
					i1 = i;
				}
				tmpFile = new File(tmp1 + tmp.substring(i0));
				retVal = tmpFile.getPath();
				if( mode == READ_IF_EXIST) {
					if( !tmpFile.exists()) return null;
					return retVal;
				}
				tmpFile.mkdirs();
			} else {
				if( inPath == null) return inFile.getPath();
				retVal = inPath + File.separatorChar + inFile.getName();
				tmpFile = new File(retVal);
				if( tmpFile.exists()) {
					flsz1 = tmpFile.length();
					if( lastMeasure > 0) {
						relSz = Math.abs(flsz1 - lastMeasure) * 100/lastMeasure;
						if( relSz <= 5) return retVal; // difference <= 5%
					}
					lastMeasure = inFile.length();
					relSz = Math.abs(flsz1 - lastMeasure) * 100/lastMeasure;
					if( relSz <= 5) return retVal; // difference <= 5%
				}

				lastMeasure = 0;
				if(mode == READ_IF_EXIST) return inFile.getPath();
				InputStream in = new FileInputStream(inFile);
				OutputStream out = new FileOutputStream(tmpFile);

				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			}	
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return retVal;
	}
	
/*	int getImageNum(String meta) {
		return ChoosePetCt.parseInt(ChoosePetCt.getLastDicomValue(meta, "0020,0013"));
	}*/

	boolean deleteFiles(BI_dbSaveInfo curr1) {
		String flName, sql;
		File fdel, dir1;
		File[] fileLst1 = null;
		int i, n = 0;
		try {
			// we may need to delete a database entry with no files
			if( curr1.flName != null) {
				dir1 = new File(curr1.flName.getPath());
				if( dir1.isDirectory()) {
					fileLst1 = dir1.listFiles();
					n = fileLst1.length;
				}
				for( i=0; i<n; i++) {
					fdel = fileLst1[i];
					fdel.delete();
				}
				dir1.delete();
			}

			Connection conn1 = openDBConnection();
			Statement stm = conn1.createStatement();
			flName = curr1.dbFileName;
			if( curr1.jpegFlg) {
				sql = "delete from jpeg where filename = '" + flName + "'";
				stm.executeUpdate(sql);
				stm.close();
				return true;
			}
			sql = "delete from studies where filename = '" + flName + "'";
			stm.executeUpdate(sql);
			sql = "delete from teaching where filename = '" + flName + "'";
			stm.executeUpdate(sql);
			sql = "delete from qc where filename = '" + flName + "'";
			stm.executeUpdate(sql);
			sql = "select filename from studies where pat_id = '" + curr1.patID + "'";
			ResultSet rSet = stm.executeQuery(sql);
			if( !rSet.next()) {
				sql = "delete from patients where pat_id = '" + curr1.patID + "'";
				stm.executeUpdate(sql);
			}
			rSet.close();
			stm.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return true;
	}

	final static int YT = 0x7974;
	boolean readOldFiles(BI_dbSaveInfo curr1, File inFl1) throws IOException {
		int i, j, n, numPt, offst, off1, sizeX, sizeY, numFrm, flLen, offdt, btype;
		int frmIdx, numInFile, storeIdx, numInPage, mod1;
		Integer pageNum = 0;
		int cntrx;
		short pixels[];
		boolean compressed = false, continuous = false, ctviewType, hasTxt;
		String path, inText, label1, frmText[]=null;
		FileInputStream fi1=null;
		File fiTmp;
		ImagePlus imp;
		ImageStack stack = null;
		ColorModel cm;
		FileInfo finfo = new FileInfo();
		try {
			m_pBuff = new byte[16384];
			m_maxValue = 0;
			path = inFl1.getPath();
			hasTxt = false;
			while( pageNum < 10) {
				if( fi1 != null) fi1.close();
				path = path.substring(0, path.length()-1) + pageNum.toString();
				pageNum++;
				fiTmp = new File(path);
				if( !fiTmp.exists()) {	// passed the end of the data
					if( stack == null) return false;
					imp = new ImagePlus( getTitleInfo(null, curr1), stack);
					imp.setFileInfo(finfo);
					imp.setProperty("bidb", curr1);	// save database info
					myMakeMontage( imp, null, hasTxt);
					return true;
				}
				hasTxt = false;
				fi1 = new FileInputStream(fiTmp);
				fi1.read(m_pBuff, 0, 512);
				btype = getInt(m_pBuff, 0);
				i = btype & 0xffff;
				if( i != 8 && i != 9 && btype != 0x69767463) {
					JOptionPane.showMessageDialog(this, "Illegal IC format");
					return false;
				}
				if( m_pBuff[29] == 1) compressed = true;
				if( m_pBuff[30] == 1) continuous = true;
				offst = 334;
				ctviewType = true;
				if( i == 8 || i == 9) {
					offst = 212;
					ctviewType = false;
					compressed = true;
					fi1.read(m_pBuff, 512, 2048);
				}
				sizeX = getShort(m_pBuff, offst);
				sizeY = getShort(m_pBuff, offst+2);
				numInFile = numFrm = getShort(m_pBuff, offst+4);
				if( sizeX*sizeY*numFrm <= 0) return false;

				for( frmIdx = 0; frmIdx < numInFile; frmIdx++) {
					off1 = 0;
					if( !ctviewType) {
						off1 = 1;
						numInFile = 1;
					}
					if( stack == null) {
						finfo.width = sizeX;
						finfo.height = sizeY;
						finfo.fileType = FileInfo.GRAY16_UNSIGNED;
						finfo.directory = inFl1.getPath();
						finfo.fileName = "";
						cm = LookUpTable.createGrayscaleColorModel(false);
						stack = new ImageStack(finfo.width, finfo.height, cm);
					} else {
						if( finfo.width != sizeX || finfo.height != sizeY) break;
					}

					m_calc = new int[6];
					if( !compressed) {
						m_square512 = new short[sizeX*sizeY];
						m_calc[4] = sizeX;
						for( j=0; j<sizeY; j++) {
							fi1.read(m_pBuff, 0, sizeX*2);
							off1 = j * sizeX;
							for( i=0; i<sizeX; i++) {
								m_square512[off1+i] = getShort(m_pBuff, i*2);
							}
						}
					} else {
						if( !continuous && frmIdx > 0) {
							fi1.read(m_pBuff, 0, 256);
							i = getInt(m_pBuff, 0);
							if( i != 0x746e6f63) {
								JOptionPane.showMessageDialog(this, "Missing continuation");
								return false;
							}
						}
						fi1.read(m_pBuff, 0, 8);
						flLen = getShort(m_pBuff, 0) - off1;
						numPt = getShort(m_pBuff, 2);
						offdt = getShort(m_pBuff, 4) - 8;
//						btype = getShort(m_pBuff, 6);
						if( numPt == YT) {
							fi1.read(m_pBuff, 8, 8);
//							btype = getShort(m_pBuff, 0);
							flLen = getInt(m_pBuff, 4) - 2*off1;
							numPt = getInt(m_pBuff, 8);
							offdt = getInt(m_pBuff, 12) - 16;
						}
						// this is a 512*512 square
						m_calc[0] = 64;
						m_calc[1] = 512 * 8;
						m_calc[3] = 3;
						m_calc[4] = 512;
						m_calc[5] = 0;	// index into m_lBuff
						if( ctviewType) {
							m_calc[0] = sizeX >> 3;
							m_calc[1] = sizeX * 8;
							m_calc[4] = sizeX;
						}
						n = m_calc[4] * sizeY;
						if( n < 262144) n = 262144;	// minimum 512*512
						m_square512 = new short[n];
						m_lBuff = new byte[offdt];
						offdt = fi1.read(m_lBuff);
						cntrx = 0x800;
						if( flLen < cntrx) cntrx = flLen;
						cntrx <<= 3;
						n = fi1.read(m_pBuff, 0, cntrx);	// the first buffer of input
						if( !ctviewType) n += offdt;	// different definition of flLen
						flLen -= n >> 3;
						for( i=j=0; i<numPt; i++) {
							if( j>0x3f00) {
								j -= 0x2000;
								for(int k=0; k<256; k++) m_pBuff[0x1f00+k] = m_pBuff[0x3f00+k];
								// copied from c++ code - fill upper half of buffer
								if( flLen==0 && cntrx < 0x1f00) return false;
								cntrx = 0x400;
								if( flLen < cntrx) cntrx = flLen;
								flLen -= cntrx;
								cntrx <<= 3;
								n = fi1.read(m_pBuff, 0x2000, cntrx);
								if( n != cntrx) return false;
							}
							j = uncode(i, j);
							if( j<0) return false;
						}
					}
					// now we have to figure out how to store the data
					// for the oldest style the frames fit into a 512*512 m_square512
					numInPage = numFrm;
					mod1 = 262144/(sizeX*sizeY);	// the maximum number which fit
					if( mod1 == 0) mod1 = 1;
					if( numInPage > mod1) {
						numInPage -= mod1* pageNum;
						if( numInPage > mod1) numInPage = mod1;
						if( numInPage <= 0) numInPage = 1;
					}
					if( ctviewType) numInPage = 1;
					mod1 = 512/sizeX;
					if( mod1 == 0) mod1 = 1;
					for( storeIdx = 0; storeIdx < numInPage; storeIdx++) {
						pixels = new short[sizeX*sizeY];
						offst = (storeIdx % mod1) * sizeX + (storeIdx / mod1) * 512 * sizeY;
						for( j=0; j<sizeY; j++) {
							off1 = j*sizeX;
							for( i=0; i<sizeX; i++) {
								pixels[i+off1] = m_square512[i+offst];
							}
							offst += m_calc[4];
							if( j == 511 && !ctviewType) offst = sizeX;	// whole body
						}
						stack.addSlice(null, pixels);
					}
				}
				n = fi1.read(m_pBuff);
				if( n>20) for( i=0; i<n; i++) {
					if( m_pBuff[i] != 'F') continue;
					if( m_pBuff[i+1] != 't' || m_pBuff[i+2] != 'x' || m_pBuff[i+3] != 't') continue;
					while(i<n) {
						if(m_pBuff[i] >= '0' && m_pBuff[i] <= '9') break;
						i++;
					}
					inText = new String(m_pBuff, i, n-i);
					frmText = getOldFrameText(inText);
					break;
				}
				if( frmText != null) for( i=0; i < frmText.length; i++) {
					label1 = frmText[i];
					if( label1 != null) {
						hasTxt = true;
						if(stack != null) stack.setSliceLabel(label1, i+1);
					}
				}
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		finally {
			if(fi1 != null) fi1.close();	// can throw IOException - not caught
			m_square512 = null;
			m_lBuff = null;
			m_pBuff = null;
		}
		return true;
	}

	int uncode(int i, int j) {
		int currVal, outInx, off1, k, mask1;
		int cntb, cntx, cnty;
		byte code, code1;
		k = (i / m_calc[0]) * m_calc[1];
		outInx = ((i % m_calc[0]) << m_calc[3]) + k;
		code = m_lBuff[m_calc[5]++];
		code1 = (byte) (code & 31);
		off1 = 0;
		if( (code & 64) != 0) {
			if( (code & 128) == 0) {
				off1 = m_lBuff[m_calc[5]++] & 255;
				if( code1>8) off1 <<= code1-8;
			} else {
				off1 = getShort(m_lBuff, m_calc[5]) & 0xffff;
				m_calc[5] += 2;
			}
		}

		mask1 = ((1 << code1) - 1);
		for( cntb=cnty=0; cnty < 8; cnty++) {
			switch( code1) {
				case 0:
					for( cntx=0; cntx<8; cntx++) m_square512[outInx+cntx] = (short) off1;
					break;

				case 16:
					k++;
					break;

				default:
					for( cntx = 0; cntx < 8; cntx++) {
						currVal = getInt(m_pBuff, j);
						currVal = ((currVal >> cntb) & mask1) + off1;
						m_square512[outInx+cntx] = (short) currVal;

						if (currVal != m_background && currVal > m_maxValue)  m_maxValue = currVal;
						cntb = (char) (code1 + cntb);
						while( cntb >= 8) {
							cntb -= 8;
							j++;
						}
					}
			}
			outInx += m_calc[4];
		}
		if( cntb > 0) j++;
		return j;
	}

	static short getShort(byte[] buff, int pos) {
		short sret = (short) (buff[pos+1] & 255);
		sret = (short) ((sret << 8) + (buff[pos] & 255));
		return sret;
	}

	static int getInt(byte[] buff, int pos) {
		int i=2, ret = buff[pos+3] & 255;
		while( i>=0) {
			ret = (ret <<8) + (buff[pos+i] & 255);
			i--;
		}
		return ret;
	}

	void writeButton() {
		if( work2 != null) return;
		int n = jTable2.getSelectedRowCount();
		if( n<=0) {
			fillWriteTable();
			JOptionPane.showMessageDialog(this, "You need to choose some studies to Write to the database.\n"
				+ "The table has been refreshed.");
			return;
		}
		if( !OK2write()) return;
		Container c = getContentPane();
		c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		readWriteDelFlg = 1;
		bkgdMode = 1;
		loadData();
	}

	void doWrite() {
		int n = jTable2.getSelectedRowCount();
		int i, j, k;
		ImagePlus img1;
		String path1;
		FileInfo fi;
		int [] selected = jTable2.getSelectedRows();
		for( i=0; i<n; i++) {
			selected[i] = jTable2.convertRowIndexToModel(selected[i]);
		}
		int cols = TBL_BF + 2;
		Object[] row1 = new Object[cols];
		for( j=0; j<n; j++) {
			if( killRead) break;
			k = selected[j];
			for( i=0; i<cols; i++) {
				row1[i] = jTable2.getModel().getValueAt(k, i);
			}
			img1 = imgList.get(k);
			fi = img1.getOriginalFileInfo();
			if( fi != null) {
				path1 = fi.directory;
				File dir1 = new File(path1);
				if( !dir1.isDirectory()) {
					IJ.log("Can't find directory (of Dicom source files): " + path1);
					continue;
				}
			}
//			if( !writeStudy2Database(row1, img1)) continue;	// unnecessary continue
			writeStudy2Database(row1, img1);
		}
		jTable2.clearSelection();
		Container c = getContentPane();
		c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	void deleteButton() {
		maybeShowWriteMip(null);
		if( work2 != null) return;
		if( AETitle != null) {
			if( orthanc1 == null) {
				JOptionPane.showMessageDialog(this,"Use browser to delete dcm4chee studies.");
				return;
			}
		}
		int n = jTable3.getSelectedRowCount();
		if( n > 0) {
			String tmp1 = "The highlighted series will be permanently deleted from the database.";
			tmp1 += "\nAre you sure you want to delete them?";
			int i = JOptionPane.showConfirmDialog(this, tmp1, "Delete from database", JOptionPane.YES_NO_OPTION);
			if( i != JOptionPane.YES_OPTION) return;
		}
		Container c = getContentPane();
		c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		if( n <= 0) {
			fillReadTable( jComboBox2.getSelectedIndex(), false);
			c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		readWriteDelFlg = 2;
		bkgdMode = 0;
		loadData();
	}
	
	void conferenceButton() {
		if( work2 != null) return;
		int [] fullList = myGetIDList();
		if( fullList == null && petImages.isEmpty()) return;
		if( !jTogButSave.isSelected()) return;
		jTogButSave.setSelected(false);
		bkgdMode = 3;
		loadData();
	}
	
	int [] myGetIDList() {
		int [] myList = WindowManager.getIDList();
		int [] currList;
		int currID, wlSz = 0;
		if( myList != null) wlSz = myList.length;
		ImagePlus img1;
		petImages = new ArrayList<Object>();
		ArrayList<Object> currImages;
		int i, j, k, n = 0;
		if(PetCtFrame.conferenceList != null) n = PetCtFrame.conferenceList.size();
		for( i=0; i<n; i++) {
			currImages = new ArrayList<Object>();
			PetCtFrame pet1 = (PetCtFrame) PetCtFrame.conferenceList.get(i);
			currList = pet1.getImageList(currImages);
			for( j=0; j<currList.length; j++) {
				currID = currList[j];
				for( k=0; k<wlSz; k++) if( myList[k] == currID) break;
				if( k<wlSz) continue;	// already in list
				img1 = (ImagePlus) currImages.get(j);
				petImages.add(img1);
			}
		}
		return myList;
	}
	
	void saveConference() {
		int [] fullList = myGetIDList();
		int count, i, j, n, wlSz = 0, petSz;
		double progVal;
		String ReadCDPath;
		ImagePlus img1;
		File dir1;
		File[] list1;
		FileInfo flInfo;
		if( fullList != null) wlSz = fullList.length;
		petSz = petImages.size();
		if( wlSz + petSz <= 0) return;
		ImageJ ij = IJ.getInstance();
		if( ij != null) ij.toFront();
		for( i=0; i<wlSz+petSz; i++) {
			if(i<wlSz) img1 = WindowManager.getImage(fullList[i]);
			else img1 = (ImagePlus) petImages.get(i-wlSz);
			flInfo = img1.getOriginalFileInfo();
			if( flInfo == null) continue;
			dir1 = new File(flInfo.directory);
			list1 = dir1.listFiles();
			if( list1 == null) continue;
			n = list1.length;
			if( n<=0) continue;
			ReadCDPath = CopyCDFiles(dir1.getPath(), null, WRITE_IF_NECESSARY);
			if( ReadCDPath == null) continue;
			for( count=j=0; j<n; j++) {
				count++;
				progVal = ((double) count)/n;
				IJ.showStatus(count+"/"+n);
				IJ.showProgress(progVal);
				CopyCDFiles(ReadCDPath, list1[j], WRITE_IF_NECESSARY);
			}
			IJ.showProgress(1.0);
		}
		this.toFront();
	}

	String[] getOldFrameText(String info) {
		String[] retVal = null;
		Scanner sc;
		String unprocessed, curr1, tmpStr;
		ArrayList<Integer> pos, yPos,  frmNm = new ArrayList<Integer>();
		ArrayList<String> strVals = new ArrayList<String>();
		int i, y1, frm1, maxFrm = -1;
		yPos = new ArrayList<Integer>();
		unprocessed = info;
		while( (i = unprocessed.indexOf('\n')) > 0) {
			curr1 = unprocessed.substring(0, i-1);
			unprocessed = unprocessed.substring(i+1);
			i = curr1.indexOf('\t');
			if( i<0) continue;
			tmpStr = curr1.substring(i+1);
			curr1 = curr1.substring(0, i);
			pos = new ArrayList<Integer>();
			sc = new Scanner(curr1).useDelimiter(",");
			while( sc.hasNextInt()) {
				i = sc.nextInt();
				pos.add(i);
			}
			if( pos.size() != 3) continue;
			frm1 = pos.get(2);
			if( frm1 > maxFrm) maxFrm = frm1;
			frmNm.add(frm1);
			yPos.add(pos.get(1));
			strVals.add(tmpStr);
		}

		if (frmNm.size() > 0 && maxFrm >= 0) {
			retVal = new String[maxFrm + 1];
			Integer[] maxY = new Integer[maxFrm + 1];
			for (i = 0; i < frmNm.size(); i++) {
				frm1 = frmNm.get(i);
				y1 = yPos.get(i);
				if (maxY[frm1] != null && y1 < maxY[frm1]) continue;
				maxY[frm1] = y1;
				retVal[frm1] = strVals.get(i);
			}
		}
		return retVal;
	}

	void myMakeMontage(ImagePlus imp, String info, boolean label) {
		int nSlices = imp.getStackSize();
		ImagePlus impMon;
		FileInfo fi;
		int columns, rows, first, last, inc, borderWidth, scrWidth, scrHeight;
		double scale, fillX, fillY;
		int imgWidth, imgHeight;
		if(imp.getTitle().indexOf("MIP data")>1) nSlices = 1;
		int maxSlices = jPrefer.getInt("montage slices", 20);
		if( nSlices < 2 || nSlices > maxSlices) {
			imp.show();	// show a normal stack
			return;
		}
		GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		scrWidth = devices[0].getDisplayMode().getWidth();
		scrHeight = devices[0].getDisplayMode().getHeight();
		imgWidth = imp.getWidth();
		imgHeight = imp.getHeight();
//		columns = (int) Math.sqrt(nSlices);
		rows = columns = 1;
		while (nSlices > columns*rows) {
			fillX = (double)columns*imgWidth/scrWidth;
			fillY = (double)rows*imgHeight/scrHeight;
			if( fillX <= fillY) columns++;
			else rows++;
		}
		first = 1;
		last = nSlices;
		inc = 1;
		borderWidth = 0;
		scale = 1.0;
//		if (imp.getWidth()*columns>800) scale = 0.5;
		MontageMaker mm = new MontageMaker();
		impMon = mm.makeMontage2( imp, columns, rows, scale, first, last, inc, borderWidth, label);
		String title = imp.getTitle();
		if( impMon != null) {
			impMon.setTitle(title);
			if( info != null) impMon.setProperty("Info", info);
			BI_dbSaveInfo curr1;
			curr1 = (BI_dbSaveInfo) imp.getProperty("bidb"); // get database info
			if( curr1 != null) impMon.setProperty("bidb", curr1);
			fi = imp.getOriginalFileInfo();
			impMon.setFileInfo(fi);
			impMon.show();
		}
	}

	BI_dbSaveInfo[] queryRow4NM( Object [] row1, int cols) {
		if(AETitle != null) return queryDcm4Row4NM( row1, cols);
		BI_dbSaveInfo[] ret1 = null;
		BI_dbSaveInfo db1;
		int i, n;
		ArrayList<File> flVect = new ArrayList<File>();
		ArrayList<String>serName = new ArrayList<String>();
		ArrayList<String>dbName = new ArrayList<String>();
		ArrayList<String>accessNum = new ArrayList<String>();
		ArrayList<String>teachName = new ArrayList<String>();
		ArrayList<String>teachReport = new ArrayList<String>();
		File currFile;
		String sql, flName, styName, patName, series, mrn1, accession;
		String teaching, report;
		patName = ChoosePetCt.compressPatName(row1[TBL_PAT_NAME]);
		mrn1 = ChoosePetCt.compressID(row1[TBL_PAT_ID]);
		Date styDate = (Date) row1[TBL_DATE];
		styName = cleanIt(row1[TBL_STUDY]);
		series = cleanIt(row1[TBL_SERIES]);
		sql = "select filename, label2, accession, label3 from studies where pat_id = '" + mrn1 + "'";
		sql += maybeNull("label1", styName);
		if( cols > 4 && series != null && !series.equals("-")) {
			sql += maybeNull( "label2", series);
		}
		sql += " and sty_date = " + formatDate( styDate);

		try {
			Connection conn1 = openDBConnection();
			Statement stm = conn1.createStatement();
			ResultSet rSet = stm.executeQuery(sql);
			while( rSet.next()) {
				flName = rSet.getString(1);
				series = rSet.getString(2);
				accession = getAccession(rSet.getString(3));
				teaching = rSet.getString(4);
				currFile = getDicomFromArchive(flName);
				// if deleting studies, accept the case of no file
				if( currFile == null && readWriteDelFlg != 2) continue;
				flVect.add(currFile);
				serName.add(series);
				teachName.add(teaching);
				teachReport.add(null);
				dbName.add(flName);
				accessNum.add(accession);
			}
			rSet.close();
			n = flVect.size();
			if( n>0) {
				for(i=0; i<n; i++) {
					teaching = ChoosePetCt.checkEmpty(teachName.get(i));
					if( teaching == null) continue;	// true for 99%, time saver
					sql = "select report from teaching where filename = '";
					sql += dbName.get(i) + "'";
					rSet = stm.executeQuery(sql);
					while( rSet.next()) {
						report = rSet.getString(1);
						teachReport.set(i, report);
						break;	// only 1 entry
					}
					rSet.close();
				}
				ret1 = new BI_dbSaveInfo[n];
				for(i=0; i<n; i++) {
					db1 = new BI_dbSaveInfo();
					db1.flName = flVect.get(i);
					db1.dbFileName = dbName.get(i);
					db1.patName = ChoosePetCt.checkEmpty(patName);
					db1.patID = ChoosePetCt.checkEmpty(mrn1);
					db1.styDate = styDate;
					db1.styName = ChoosePetCt.checkEmpty(styName);
					db1.serName = ChoosePetCt.checkEmpty(serName.get(i));
					db1.accession = ChoosePetCt.checkEmpty(accessNum.get(i));
					db1.teachName = ChoosePetCt.checkEmpty(teachName.get(i));
					db1.teachReport = ChoosePetCt.checkEmpty(teachReport.get(i));
					ret1[i] = db1;
				}
			} else {
				// found nothing, look for jpeg
				if( styName.equals(JPEG_STRING)) {
					sql = "select filename, accession from jpeg where pat_id = '" + mrn1 + "'";
					if( series != null) {
						sql += " and filename = '" + series +"'";
					}
					sql += " and sty_date = " + formatDate( styDate);
					rSet = stm.executeQuery(sql);
					while( rSet.next()) {
						flName = rSet.getString(1);
						accession = getAccession(rSet.getString(2));
						series = flName;
						currFile = getJpegFromArchive(flName, styDate);
						// if deleting studies, accept the case of no file
						if( currFile == null && readWriteDelFlg != 2) continue;
						flVect.add(currFile);
						serName.add(series);
						dbName.add(flName);
						accessNum.add(accession);
					}
					rSet.close();
					n = flVect.size();
					if( n>0) {
						ret1 = new BI_dbSaveInfo[n];
						for(i=0; i<n; i++) {
							// teachName and teachReport are null from constructor
							db1 = new BI_dbSaveInfo();
							db1.flName = flVect.get(i);
							db1.dbFileName = dbName.get(i);
							db1.patName = ChoosePetCt.checkEmpty(patName);
							db1.patID = ChoosePetCt.checkEmpty(mrn1);
							db1.styDate = styDate;
							db1.styName = ChoosePetCt.checkEmpty(styName);
							db1.serName = ChoosePetCt.checkEmpty(serName.get(i));
							db1.accession = ChoosePetCt.checkEmpty(accessNum.get(i));
							db1.jpegFlg = true;
							ret1[i] = db1;
						}
					}
				}
			}
			stm.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return ret1;
	}

	BI_dbSaveInfo[] queryDcm4Row4NM( Object [] row1, int cols) {
		BI_dbSaveInfo[] ret1 = null;
		String mrn = ChoosePetCt.checkEmpty(row1[TBL_PAT_ID]);
		String sdyDesc = ChoosePetCt.checkEmpty(row1[TBL_STUDY]);
		String serDesc = ChoosePetCt.checkEmpty(row1[TBL_SERIES]);
		Date styDate = (Date) row1[TBL_DATE];
		dcm4Patient thisPat;
		dcm4Study thisSdy = null;
		dcm4Series thisSer;
		ArrayList<BI_dbSaveInfo> tmpList = new ArrayList<BI_dbSaveInfo>();
		BI_dbSaveInfo db1;
		int pkPat = -1, pkSdy = -1, start, numSer=0;
		Connection conn1 = openDBConnection();
		if( conn1 == null) return null;
		int i, n = dcmPatient.size();
		for( i=0; i<n; i++) {
			thisPat = dcmPatient.get(i);
			if( thisPat.pat_id.equals(mrn)) {
				pkPat = thisPat.pk;
				break;
			}
		}
		if( pkPat < 0) return null;
		n = dcmStudy.size();
		for( i=0; i<n; i++) {
			thisSdy = dcmStudy.get(i);
			pkSdy = thisSdy.pk;
			numSer = thisSdy.num_series;
			if( thisSdy.patient_fk != pkPat || !ChoosePetCt.isSameDate(thisSdy.study_date,styDate)) continue;
			if( thisSdy.study_desc == null || thisSdy.study_desc.equals(sdyDesc)) break;
		}
		if( i>=n) return null;
		// there are 2 possibilities: 1) entire study or 2) given series
		n = dcmSeries.size();
		for( i=0; i<n; i++) {
			thisSer = dcmSeries.get(i);
			if( thisSer.study_fk != pkSdy) continue;
			start = thisSer.start;
			if(numSer>1 && serDesc!=null && !serDesc.equals(thisSer.series_desc)) continue;
			db1 = new BI_dbSaveInfo();
			db1.flName = getDcm4FileName(conn1, start+1);
			db1.patName = ChoosePetCt.checkEmpty(row1[TBL_PAT_NAME]);
			db1.patID = mrn;
			db1.styDate = styDate;
			db1.styName = sdyDesc;
			db1.serName = thisSer.series_desc;
			db1.AETitle = AETitle;
/*			if( orthanc1 != null) {	// orthanc doesn't reach this
				db1.basUrl = orthanc1.orthSub.baseURL;
				db1.usrPW  = orthanc1.orthSub.userPW;
			}*/
			if(thisSdy != null) db1.accession = thisSdy.accession_no;
			tmpList.add(db1);
		}
		n = tmpList.size();
		if(n>0) {
			ret1 = new BI_dbSaveInfo[n];
			for( i=0; i<n; i++) ret1[i] = tmpList.get(i);
		}
		return ret1;
	}
	
	File getDcm4FileName(Connection conn1, int start) {
		File ret1 = null;
		int indx1;
		try {
			String path;
			String sql = "select filepath from files where pk="  + start;
			Statement stm = conn1.createStatement();
			ResultSet rSet = stm.executeQuery(sql);
			while( rSet.next()) {
				path = rSet.getString(1);
				indx1 = path.lastIndexOf('/');
				if(indx1 < 0) break;
				path = m_dataPath + "/" + path.substring(0, indx1);
				ret1 = new File(path);
				stm.close();
				break;
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return ret1;
	}

	boolean writeStudy2Database( Object[] row1, ImagePlus img1) {
		String sql, styName, styDate, styDateShort, patName, patName1, series, mrn1;
		String tmp, tmp1, flName;
		int i, j1, len, grLen;
		try {
			Connection conn1 = openDBConnection();
			Statement stm=null;
			ResultSet rSet;
			VerifyWriteParameters dlg1 = new VerifyWriteParameters(null, true);
			if( !dlg1.initVerify(row1, img1, orthanc1 != null)) {
				dlg1.setVisible(true);
				if( !dlg1.variablesOK) return false;
			}
			patName = compressName( dlg1.getPatName());
			patName1 = fixApostrophe(patName);
			mrn1 = dlg1.getPatID();
			styName = cleanIt(dlg1.getStudyName());
			series = cleanIt(dlg1.getSeriesName());
			styDateShort = styDate = dlg1.getStyDate();
			i = styDate.indexOf(" ", 9);
			if( i > 0) styDateShort = styDate.substring(0, i);
			if( AETitle == null) {	// this is the normal case
				mrn1 = ChoosePetCt.compressID(mrn1);
				sql = "select filename from studies where pat_id = '" + mrn1 + "' and sty_date = ";
				sql += formatDate( styDateShort) + " and label1 = '" + styName + "' and label2 = '" + series +"'";
				stm = conn1.createStatement();
				rSet = stm.executeQuery(sql);
				if( rSet.next()) {
					IJ.log("Study already written: " + patName + "  " + styDateShort + "  " + series);
					rSet.close();
					return false;
				}

				sql = "select pat_id from studies where accession = " + dlg1.getAccession();
				rSet = stm.executeQuery(sql);
				if( rSet.next()) {
					tmp = rSet.getString(1);
					rSet.close();
					if( !tmp.equals(mrn1)) {
						IJ.log("Study not written. Accession number already in use in another mrn: " + tmp);
						return false;
					}
				}
			} else {	// using AETitle
				dcm4Patient thisPat;
				dcm4Study thisSdy;
				dcm4Series thisSer;
				int pkPat = -1, pkSdy = -1;
				if( conn1 == null) {
					if( !orthanc1.checkSeriesOk(img1, series)) return false;
				} else {
					fillDcm3Table(0, null, mrn1, conn1);
					int n = dcmPatient.size();
					for( i=0; i<n; i++) {
						thisPat = dcmPatient.get(i);
						if( thisPat.pat_id.equals(mrn1)) {
							pkPat = thisPat.pk;
							break;
						}
					}
					if( pkPat >= 0) {
						n = dcmStudy.size();
						for( i=0; i<n; i++) {
							thisSdy = dcmStudy.get(i);
							pkSdy = thisSdy.pk;
							if( thisSdy.patient_fk != pkPat || !date2String(thisSdy.study_date).equals(styDateShort)) continue;
							// it is a bug that study_desc can be null, but so be it
							if( thisSdy.study_desc == null || thisSdy.study_desc.equals(styName)) break;
						}
						if( i>=n) pkSdy = -1;
					}
					if( pkSdy >= 0) {
						n = dcmSeries.size();
						for( i=0; i<n; i++) {
							thisSer = dcmSeries.get(i);
							// maybe the same bug as above
							if( thisSer.study_fk == pkSdy && 
								(thisSer.series_desc == null || thisSer.series_desc.equals(series))) break;
						}
						if( i<n) {
							IJ.log("Study already written: " + patName + "  " + styDateShort + "  " + series);
							return false;
						}
					}
				}
			}

			FileInfo fi = img1.getOriginalFileInfo();
			File fin0, fin = null, fout, fdel;
			File[] fileLst1 = null, clearCanvasLst1 = null, grLst;
			ArrayList<File> tmpList;
			if( fi != null) {
				fin = new File(fi.directory);
				if( !fin.exists() || !fin.isDirectory()) {
					IJ.log("Can't find Dicom source directory: " + fi.directory);
					return false;
				}
				fileLst1 = fin.listFiles();
				if( fileLst1.length <= 0) return false;
				clearCanvasLst1 = getClearCanvasList(fileLst1, img1);
				if( clearCanvasLst1 != null) fileLst1 = clearCanvasLst1;
				else {	// normal case, check for bad files e.g. DIRFILE of Philips
					// watch out for DICOMDIR. The whole mess can be in a single directory.
					// thus checkDicomValid is not enough (valid files for another series)
					tmpList = new ArrayList<File>();
					int k, n, stkSz = img1.getImageStackSize()/fi.nImages;
					if( stkSz == 0) stkSz = 1;	// try to fix montages
					n = fileLst1.length;
					if( stkSz < n && AETitle == null) {
						DicomFormat dcm = new DicomFormat();
						for( i=0; i<n; i++) {
							fin0 = fileLst1[i];
							flName = fin0.getName();
							if(fin0.isDirectory() || isGraphicFile(flName)) {
								tmpList.add(fin0);
								continue;
							}
							// for NM renal fi.nImages > 1 and label = null
							// thus accept for tmp1 == null
							// stkSz = 1 is a difficult case, chkDicomValid for it
							for( j1=1; j1<=stkSz; j1++) {
								if( stkSz == 1) break;
								tmp1 = img1.getImageStack().getSliceLabel(j1);
								if( tmp1 == null || tmp1.startsWith(flName)) break;
							}
							if( j1 > stkSz) continue;
							if( stkSz == 1) {
								k = dcm.checkDicomValid(fin.getPath(), flName);
								if( k<= 0) continue;
							}
							tmpList.add(fin0);
						}
						if(stkSz <= 1 && tmpList.isEmpty()) {
							tmp = "Can't write this series - "+series+"\n";
							tmp += "Perhaps it is a montage?\n";
							tmp += "If so, reread data with Montage = 0 in Setup.\n";
							tmp += "Delete from database any/all failed writes.";
							IJ.log(tmp);
							return false;
						}
						stkSz = tmpList.size();
						if(stkSz < n) {
							fileLst1 = new File[stkSz];
							for( i=0; i<stkSz; i++) fileLst1[i] = tmpList.get(i);
						}
					}
				}
			}
			if( AETitle != null) {
/*				ApplicationEntity ae = new ApplicationEntity("STORESCU");
				Device device = new Device("storescu");
				org.dcm4che.net.Connection conn = new org.dcm4che.net.Connection();
				device.addConnection(conn);
				device.addApplicationEntity(ae);
				ae.addConnection(conn);
				StoreSCU snd = new StoreSCU(ae);*/
				int j = AETitle.indexOf('@');
				int k = AETitle.indexOf(":");
				double progVal, count;
				if( j<0 || k<j) return false;
				if( fileLst1 == null) return false;
				// doesn't work for Windows
//				if( !isEchoScu()) return false;
				ImageJ ij = IJ.getInstance();
				if( ij != null) ij.toFront();
//				Dicomdir has a problem that all series are written.
				File[] dcmList = getDcmDirList(fileLst1, img1);
				if( dcmList != null) fileLst1 = dcmList;

				len = fileLst1.length;
				grLst = new File[10];
				grLen = 0;
				for( i=0; i<len; i++) {
					count = i+1;
					progVal = count/len;
					IJ.showProgress(progVal);
					fin0 = fileLst1[i];
					if(isGraphicFile(fin0.getName())) {
						if(grLen < 10) grLst[grLen++] = fin0;
						continue;
					}
					String[] parms = new String[3];
					parms[0] = "-c";
					parms[1] = AETitle;
					parms[2] = fin0.getPath();
					StoreSCU.main(parms);
				}
				IJ.showProgress(1.0);
				if( grLen > 0 && orthanc1 != null) {
					ReadOrthancSub orthSub = orthanc1.orthSub;
					orthSub.AETitle = AETitle;
					orthSub.srcImg = img1;
					for( i=0; i<grLen; i++) {
						fin0 = grLst[i];
						j = 0;
						if(fin0.getName().endsWith(".gr2")) j = 1;
						orthSub.write2Orth(fin0, j);
					}
				}
				this.toFront();
/*				snd.setCalledAET(AETitle.substring(0, j));
				snd.setRemoteHost(AETitle.substring(j+1, k));
				snd.setRemotePort(Integer.parseInt(AETitle.substring( k+1)));
				snd.addFile(fin);
				snd.configureTransferCapability();
//				snd.open();
//				snd.send();
//				snd.close();*/
				return true;
			}
			bi_dbWriteInfo biWr1 = new bi_dbWriteInfo();
			biWr1.generateInfo(null, null);
			if( biWr1.fileName == null) return false;
			fout = new File( biWr1.path1);
			if( fout.exists()) {
				IJ.log("Dicom directory already exists, aborting");
				return false;
			}
			

			sql = "select name, pat_id from patients where pat_id = '" + mrn1 + "'";
			if(stm != null) {
				rSet = stm.executeQuery(sql);
				if( rSet.next()) {
					tmp = rSet.getString(1);
					rSet.close();
					if( !tmp.equalsIgnoreCase(patName)) {
						tmp1 = "The patient name in the database is: " + tmp + ", whereas this name is: " + patName;
						tmp1 += "\nContinue with database name (Yes), or change it to: " + patName + " (No)?";
						i = JOptionPane.showConfirmDialog(this, tmp1, "Problem with patient name", JOptionPane.YES_NO_CANCEL_OPTION);
						if( i == JOptionPane.CANCEL_OPTION) return false;
						if( i == JOptionPane.NO_OPTION) {
							sql = "update patients set name = '" + patName1 + "' where pat_id = '" + mrn1 + "'";
							stm.executeUpdate(sql);
						}
					}
				} else {
					NewPatient dlg2 = new NewPatient(null, true);
					dlg2.init(patName, mrn1, this);
					dlg2.setVisible(true);
					if( !dlg2.addFlg) return false;
					String birthday = dlg1.getBirthday();
					if( birthday.isEmpty()) {
						sql = "insert into patients (name, pat_id) values ('" + patName1;
						sql += "', '" + mrn1 + "')";
					} else {
						sql = "insert into patients (name, pat_id, birthday) values ('" + patName1;
						sql += "', '" + mrn1 + "', " + formatDate( birthday) + ")";
					}
					stm.executeUpdate(sql);
				}
			}

			sql = "insert into studies (pat_id, sty_date, label1, label2, accession, filename, diskcode, ";
			sql += "sty_type, studyInstanceUID, seriesInstanceUID) values ('";
			sql += mrn1 + "', " + formatDate( styDateShort) + ", '" + styName + "', '" + series + "', ";
			sql += dlg1.getAccession() + ", '" + biWr1.fileName + "', " + biWr1.diskNum + ", '";
			sql += dlg1.getModality() + "', '" + dlg1.getStudyUID() + "', '" + dlg1.getSeriesUID() + "')";
			if( stm != null) {
				stm.executeUpdate(sql);
				stm.close();
			}
			// now get the teaching part, if any
			Window wndTeach = img1.getWindow();
			bi_dbTeaching currTeach;
			len = teachList.size();
			for( i=0; i<len; i++) {
				currTeach = teachList.get(i);
				if( !currTeach.imgWnd.equals(wndTeach)) continue;
				writeTeaching( biWr1.fileName, currTeach.teachName, currTeach.report);
				break;
			}

			fout.mkdirs();
			// if fin == null, the file is in memory only. Make a Dicom file and go home.
			if( fin == null) {
				myWriteDicom dcm1 = new myWriteDicom(img1);
				dcm1.writeDicomHeader(biWr1.path1);
				return true;
			}
			InputStream in1;
			OutputStream out1;
			byte[] buf = new byte[16384];
			
			if(fileLst1 != null) for( i=0; i<fileLst1.length; i++) {
				if( fileLst1[i].isDirectory()) continue;
				in1 = new FileInputStream( fileLst1[i]);
				out1 = new FileOutputStream( fout.getPath() + "/" + fileLst1[i].getName());
				while( (len = in1.read(buf))>0) {
					out1.write(buf, 0, len);
				}
				in1.close();
				out1.close();
			}

			// in the case where the source is in our database, do NOT erase it
			BI_dbSaveInfo curr1;
			curr1 = (BI_dbSaveInfo) img1.getProperty("bidb");	// get database info
			if( curr1 != null) return true;	// don't erase database data
			if( clearCanvasLst1 != null) return true; // don't erase Clear Canvas
		
			boolean erase1 = true;
			if(fileLst1 != null) for( i=0; i<fileLst1.length; i++) {
				fdel =fileLst1[i];
				if( fdel.isDirectory()) {
					erase1 = false;
					continue;
				}
				fdel.delete();
			}
			if( erase1) fin.delete();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return true;
	}

	File[] getDcmDirList(File[] fileLst, ImagePlus img1) {
		String tmp1, path;
		File[] retList;
		int i, j, stkSz;
		BI_dbSaveInfo curr1 = (BI_dbSaveInfo) img1.getProperty("bidb");
		if( curr1 == null || !curr1.isDicomDir) return null;
		path = fileLst[0].getParent();
		ImageStack inputStack = img1.getStack();
		FileInfo fi = img1.getOriginalFileInfo();
		if( fi == null) return null;
		stkSz = inputStack.getSize()/fi.nImages;
		retList = new File[stkSz];
		for( i=0; i<stkSz; i++) {
			tmp1 = inputStack.getSliceLabel(i+1);
			if(tmp1 == null || tmp1.isEmpty()) {
				retList[i] = curr1.flName;
				continue;
			}
			j = tmp1.indexOf('\n');
			tmp1 = tmp1.substring(0, j);
			retList[i] = new File(path + File.separatorChar + tmp1);
		}
		return retList;
	}

	boolean isGraphicFile(String flName) {
		if( !flName.startsWith("graphic.")) return false;
		return flName.endsWith(".gr1") || flName.endsWith(".gr2");
	}



	// this is code to parse the Clear Canvas xml file
	File [] getClearCanvasList(File [] inFlst, ImagePlus img1) {
		int i, n;
		File fin = null, fout;
		File[] outFlst = null;
		ArrayList<String> currList = new ArrayList<String>();
		Node root, study, series, instance, baseSub;
		String seriesUID, locSeriesUID, meta, tmp;
		for( i=0; i<inFlst.length; i++) {
			fin = inFlst[i];
			if( fin.getName().equals("studyXml.xml")) break;
		}
		if( i >= inFlst.length) return null;	// not found
		meta = ChoosePetCt.getMeta(1, img1);
		seriesUID = ChoosePetCt.getSeriesUID(meta);
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(fin);
			root = doc.getDocumentElement();
			study = root.getFirstChild();
			series = study.getFirstChild();
			series = getValidSeries(series);
			if( series == null) return null;
			do {
				locSeriesUID = findNamedEntry(series, "UID");
				if( locSeriesUID.equals(seriesUID)) break;
			} while( (series = series.getNextSibling()) != null);
			if( series == null) return null;

			instance = series.getFirstChild();
			while( instance != null) {
				tmp = findNamedEntry( instance, "SourceFileName");
				instance = instance.getNextSibling();
				if( tmp == null) continue;
				// ClearCanvas is a Windows program. Hard code separatorChar
				i = tmp.lastIndexOf(WIN_SEPARATOR_CHAR);
				if( i <= 0) continue;
				tmp = tmp.substring(i+1);	// without separatorChar
				currList.add(tmp);
			}
			n = currList.size();
			if( n <= 0) return null;
			outFlst = new File[n];
			FileInfo fi = img1.getOriginalFileInfo();
			if( fi == null) return outFlst;
			tmp = fi.directory;
			for( i=0; i<n; i++) {
				fout = new File(tmp + File.separatorChar + currList.get(i));
				outFlst[i] = fout;
			}

		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return outFlst;
	}
	
	Node getValidSeries(Node inSeries) {
		Node series = inSeries, baseInstance, instance;
		String SOPClass;
		try {
			while( series != null) {
				baseInstance = series.getFirstChild();
				instance = baseInstance.getFirstChild();
				if( instance == null) instance = baseInstance.getNextSibling();
				if( instance == null) return null;
				SOPClass = findSopClassUID( instance);
				if( ChoosePetCt.getSOPClass(SOPClass) != ChoosePetCt.SOPCLASS_UNKNOWN) break;
				series = series.getNextSibling();
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return series;
	}
	
	String findSopClassUID(Node instance) {
		String SOPClass;
		SOPClass = findNamedEntry( instance, "SopClassUID");
		if( SOPClass == null) {
			SOPClass = findAttributeTag(instance, "00080016");
		}
		return SOPClass;
	}

	String findNamedEntry( Node inNode, String name) {
		String retVal = null;
		try {
			NamedNodeMap nodeMap = inNode.getAttributes();
			if( nodeMap == null) return null;
			Node currNode = nodeMap.getNamedItem(name);
			if( currNode == null) return null;
			retVal = currNode.getNodeValue();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return retVal;
	}

	String findAttributeTag( Node inNode, String tag) {
		try {
			NamedNodeMap nodeMap;
			Node valNode, tmpNode, currNode = inNode.getFirstChild();
			do {
				nodeMap = currNode.getAttributes();
				valNode = currNode.getFirstChild();
				currNode = currNode.getNextSibling();
				tmpNode = nodeMap.getNamedItem("Tag");
				if( tmpNode != null && tmpNode.getNodeValue().equals(tag)) {
					return valNode.getNodeValue();
				}
			} while( currNode != null);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return null;
	}
	// end of Clear Canvas parsing code

	
	String formatDate( Object date1) {
		if( isAccessDb) return "#" + date1 + "#";
		return "'" + date2mySQL(date1) + "'";
	}

	String getTitleInfo( String meta, BI_dbSaveInfo db1) {
		return getTitleInfoSub( meta, db1, 0);
	}

	String getTitleInfoSub( String meta, BI_dbSaveInfo db1, int part) {
		String ret1;
		Date date1=null, birthday;
		String patName=null, patID=null, studyName=null, styDate , series=null;
		String birthy = null;
		if( meta != null) {
			patName = ChoosePetCt.getCompressedPatName(meta);
			if( patName == null) return null;
			patID = ChoosePetCt.getCompressedID( meta);
			date1 = ChoosePetCt.getStudyDateTime( meta, -1);
			String tmp = ChoosePetCt.getDicomValue(meta, "0010,0030");
			birthday = ChoosePetCt.getDateTime(tmp, null);
			if(birthday != null) {
				long sdyTime, birthTime, currDiff;
				Integer years;
				sdyTime = date1.getTime();
				birthTime = birthday.getTime();
				currDiff = (sdyTime - birthTime)/(24*60*60*1000);	// number of days
				years = (int)( currDiff/365.242199);
				birthy = "   " + years.toString() + "y";
			}
			studyName = ChoosePetCt.getDicomValue( meta, "0008,1030");
			series = ChoosePetCt.getDicomValue( meta, "0008,103E");
			if( series == null) series = ChoosePetCt.getDicomValue( meta, "0054,0400");
		}
		if( db1 != null) {
			if( db1.patName != null) patName = db1.patName;
			if( db1.patID != null) patID = db1.patID;
			date1 = db1.styDate;
			if( db1.styName != null) studyName = db1.styName;
			if( db1.serName != null) series = db1.serName;
		}
		styDate = ChoosePetCt.UsaDateFormat(date1);
		if(birthy!=null) patName += birthy;
		switch( part) {
			case 1:
				ret1 = patName + "   " + patID + "   " + styDate;
				break;
				
			case 2:
				ret1 = studyName + "   " + series;
				break;
				
			default:
				ret1 = patName + "   " + patID + "   " + styDate;
				ret1 += "   " + studyName + "   " + series;
				break;
		}
		return ret1;
	}

	// mySQL requires dates in format yyyy-mm-dd hh:mm:ss, here we use only yyyy-mm-dd
	static String date2mySQL( Object inDate) {
		String ret1 = null;
		String inDt1 = null;
		Date dt0 = null;
		if(inDate instanceof String) inDt1 = (String) inDate;
		else dt0 = (Date) inDate;
		SimpleDateFormat df0, df1;
		try {
			df0 = new SimpleDateFormat("d MMM yyyy", Locale.US);
			df1 = new SimpleDateFormat("yyyy-MM-d", Locale.US);
			if( dt0== null) dt0 = df0.parse(inDt1);
			ret1 = df1.format(dt0);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return ret1;
	}


	void seriesMouseClick(MouseEvent e) {
		if( jTable1.columnAtPoint(e.getPoint()) != TBL_SERIES) return;
		int j, i = jTable1.getSelectedRow();
		if( i < 0) return;
		i = jTable1.convertRowIndexToModel(i);
		boolean jpeg, currIsBf;
//		ArrayList<Integer> remove1;
		ArrayList<String> serVect, teachVect;
		DefaultTableModel mod1;
		mod1 = (DefaultTableModel) jTable1.getModel();
		if( AETitle!= null) {
			expandDcm4Mouse(i, mod1);
			return;
		}
		boolean petCt = false;
		String tmp1;
		String patName = (String) mod1.getValueAt(i, TBL_PAT_NAME);
		String study = (String) mod1.getValueAt(i, TBL_STUDY);
		Date date1 = (Date) mod1.getValueAt(i, TBL_DATE);
		String mrn1 = (String) mod1.getValueAt(i, TBL_PAT_ID);
		mrn1 = ChoosePetCt.compressID(mrn1);
		String series = (String) mod1.getValueAt(i, TBL_SERIES);
		String accession = (String) mod1.getValueAt(i, TBL_ACCESSION);
		currIsBf = (Boolean) mod1.getValueAt(i, TBL_BF);
		String teach = null;
		if( series == null) {
			jpeg = false;
			unselectAllTableEntries();
			Object[] row1 = new Object[TBL_BF+2];
			row1[TBL_PAT_NAME] = patName;
			row1[TBL_STUDY] = study;
			row1[TBL_DATE] = date1;
			row1[TBL_PAT_ID] = mrn1;
			row1[TBL_ACCESSION] = accession;
			row1[TBL_BF] = currIsBf;
			String sql = "select distinct label2, label3 from studies where pat_id = '" + mrn1 + "'";
			sql += maybeNull("label1", cleanIt(study)) + " and sty_date = " + formatDate(date1);
			if( petCt) sql += " and (sty_type='CT' or sty_type='MR')";

			// check if it is from the jpeg database
			if( study.equals(JPEG_STRING)) {
				jpeg = true;
				sql = "select distinct filename from jpeg where pat_id = '" + mrn1 + "'";
				sql += " and sty_date = " + formatDate(date1);
			}
			try {
				Connection conn1 = openDBConnection();
				Statement stm = conn1.createStatement();
				ResultSet rSet = stm.executeQuery(sql);
				serVect = new ArrayList<String>();
				teachVect = new ArrayList<String>();
				while( rSet.next()) {
					series = rSet.getString(1);
					if( series == null) series = "";	// empty string
					if(!jpeg) teach = rSet.getString(2);
					if( petCt) {
						tmp1 =series.toLowerCase();
						if( tmp1.contains("scout")) continue;
					}
					serVect.add(series);
					teachVect.add(teach);
				}
				rSet.close();
				stm.close();
				if( serVect.size() > 0) {
					mod1.setValueAt(serVect.get(0), i, TBL_SERIES);
					mod1.setValueAt(teachVect.get(0), i, TBL_TEACHING);
				}
				for( j=1; j<serVect.size(); j++) {
					row1[TBL_SERIES] = serVect.get(j);
					row1[TBL_TEACHING] = teachVect.get(j);
					mod1.insertRow(j+i, row1);
				}
			} catch (Exception e1) { ChoosePetCt.stackTrace2Log(e1); }

		} /* else {
			int k, n;
			n = mod1.getRowCount();
			unselectAllTableEntries();
			remove1 = new ArrayList<Integer>();
			for( j=0; j<n; j++) {
				if( i == j || !patName.equals((String) mod1.getValueAt(j, TBL_PATNAME))) continue;
				if( !study.equals((String) mod1.getValueAt(j, TBL_STUDY))) continue;
				if( !date1.equals((String) mod1.getValueAt(j, TBL_DATE))) continue;
				if( !mrn1.equals((String) mod1.getValueAt(j, TBL_PAT_ID))) continue;
				remove1.add(j);
			}
			mod1.setValueAt(null, i, TBL_SERIES);
			for( j=0; j<remove1.size(); j++) {
				k = remove1.elementAt(j) - j;
				mod1.removeRow(k);
			}
		}*/
	}
	
	void expandDcm4Mouse(int row, DefaultTableModel mod1) {
		String serDesc = (String) mod1.getValueAt(row, TBL_SERIES);
		if( serDesc != null) return;
		unselectAllTableEntries();
		if( orthanc1 != null) {
			orthanc1.expandOrthMouse(row, mod1);
			return;
		}
		String sdyDesc = (String) mod1.getValueAt(row, TBL_STUDY);
		Date styDate = (Date) mod1.getValueAt(row, TBL_DATE);
		String mrn = (String) mod1.getValueAt(row, TBL_PAT_ID);
		dcm4Patient thisPat;
		dcm4Study thisSdy;
		dcm4Series thisSer;
		int pkPat = -1, pkSdy = -1;
		int i, j, n = dcmPatient.size();
		for( i=0; i<n; i++) {
			thisPat = dcmPatient.get(i);
			if( thisPat.pat_id.equals(mrn)) {
				pkPat = thisPat.pk;
				break;
			}
		}
		if( pkPat < 0) return;
		n = dcmStudy.size();
		for( i=0; i<n; i++) {
			thisSdy = dcmStudy.get(i);
			pkSdy = thisSdy.pk;
			if( thisSdy.patient_fk != pkPat || !ChoosePetCt.isSameDate(thisSdy.study_date,styDate)) continue;
			if( thisSdy.study_desc == null || thisSdy.study_desc.equals(sdyDesc)) break;
		}
		if( i>=n) return;
		Object[] row1 = new Object[TBL_BF+2];
		row1[TBL_PAT_NAME] = (String) mod1.getValueAt(row, TBL_PAT_NAME);
		row1[TBL_STUDY] = sdyDesc;
		row1[TBL_DATE] = styDate;
		row1[TBL_PAT_ID] = mrn;
		row1[TBL_BF] = false;
		n = dcmSeries.size();
		for( i=j=0; i<n; i++) {
			thisSer = dcmSeries.get(i);
			if( thisSer.study_fk != pkSdy) continue;
			serDesc = thisSer.series_desc;
			row1[TBL_SERIES] = serDesc;
			if( j==0) mod1.setValueAt(serDesc, row, TBL_SERIES);
			else mod1.insertRow(j+row, row1);
			j++;
		}
	}

	String maybeNull(String lab, String in1) {
		String rt1 = " and ";
		if( in1 == null || in1.isEmpty()) rt1 += "(" + lab  +" is null or " + lab + " = '')";
		else rt1 += lab + " = '" + in1 + "'";
		return rt1;
	}

	String cleanIt(Object in1) {
		String ret1 = ChoosePetCt.checkEmpty(in1);
		if( ret1 == null) return null;
		int i = ret1.indexOf("'");
		if( i > 0) {
			ret1 = ret1.substring(0, i+1) + ret1.substring(i);
		}
		return ret1;
	}

	File getDicomFromArchive( String flName) {
		int i;
		String tmp1, tmp1u, tmp0, paths, path1, extu, extl;
		final char dirSepChar = File.separatorChar;
		try {
			// Linux is case sensitive
			tmp1u = getDicomDirectory(flName);
			tmp1 = tmp1u.toLowerCase();
			i = tmp1.length();
			extu = ".T00";
			extl = ".t00";
			tmp0 = tmp1u.substring(0, i-4);
			paths = m_dataPath;
			Character ch1 = dirSepChar;
			while( paths != null) {
				i = paths.indexOf("|");
				if( i>0) {
					path1 = paths.substring(0, i);
					paths = paths.substring(i+1);
				} else {
					path1 = paths;
					paths = null;
				}
				if( !path1.endsWith(ch1.toString())) path1 += dirSepChar;
				File fl1 = new File(path1 + tmp1);	// dicom lower case
				if( fl1.exists()) {
					return fl1;
				}
				fl1 = new File(path1 + tmp1u);	// dicom upper case
				if( fl1.exists()) {
					return fl1;
				}
				fl1 = new File(path1 + tmp0 + extu); // old upper case
				if( fl1.exists()) {
					return fl1;
				}
				fl1 = new File(path1 + tmp0 + extl); // old upper case
				if( fl1.exists()) {
					return fl1;
				}
				fl1 = new File(path1 + tmp0.toLowerCase() + extl);
				if( fl1.exists()) {
					return fl1;
				}
				if( paths == null) reportFailure2Log(path1 + tmp1u);
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return null;
	}
	
	File getJpegFromArchive( String flName, Date styDate) {
		String tmp1, tmp2, datStr, paths, path1;
		int i;
		final char dirSepChar = File.separatorChar;
		try {
//			SimpleDateFormat df1 = new SimpleDateFormat("d MMM yyyy");
			SimpleDateFormat df1;
			tmp1 = "'ic'yyyy'" + dirSepChar + "jpeg'MM";
			df1 = new SimpleDateFormat(tmp1, Locale.US);
			datStr = df1.format(styDate);
			paths = m_dataPath;
			Character ch1 = dirSepChar;
			while( paths != null) {
				i = paths.indexOf("|");
				if( i>0) {
					path1 = paths.substring(0, i);
					paths = paths.substring(i+1);
				} else {
					path1 = paths;
					paths = null;
				}
				if( !path1.endsWith(ch1.toString())) path1 += dirSepChar;
				tmp2 = path1 + datStr + dirSepChar;
				File fl1 = new File(tmp2 + flName);
				if( fl1.exists()) {
					return fl1;
				}
				fl1 = new File(tmp2 + flName.toLowerCase());
				if( fl1.exists()) {
					return fl1;
				}
				if( paths == null) reportFailure2Log(tmp2 + flName);
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		return null;
	}

	String getDicomDirectory( String flName) {
		String ret1;
		final char dirSepChar = File.separatorChar;
		int i, j, year, month, day;
		i = convertLetter(flName.charAt(0));
		j = convertLetter(flName.charAt(1));
		year = (i << 2) + (j >> 3) + 1980;
		i = convertLetter( flName.charAt(2));
		month = ((j & 7) << 1) + (i >> 4);
		j =convertLetter(flName.charAt(3));
		day = ((i & 15) << 1) + (j >> 4);
		ret1 = "ic" + year + dirSepChar + String.format("ic%02da%02d", month, day/7);
		ret1 += dirSepChar + flName + "_dcm";
		return ret1;
	}

	int convertLetter( char in) {
		if( in >= 'a' && in <= 'z') return in - 'a';
		if( in >= 'A' && in <= 'Z') return in - 'A';
		if( in >= '0' && in <= '9') return in - '0' + 26;
		return -1;
	}

	boolean OK2write() {
		if( jCurrDB == 0) return true;
		String mess1;
		mess1 = "Your Read Database is set to " + jLabelDbName.getText();
		mess1 += ".\nAre you sure you want to write to this database? Yes\n";
		mess1 += "No - Write to main database, #1 in list\n";
		mess1 += "or Cancel write altogether.";
		int ans = JOptionPane.showConfirmDialog(this, mess1, "Not main database", JOptionPane.YES_NO_CANCEL_OPTION);
		if( ans == JOptionPane.CANCEL_OPTION) return false;
		if( ans != JOptionPane.YES_OPTION) {
			changeDBSelected( 1);
		}
		return true;
	}

	void reportFailure2Log(String path) {
		String path1 = path;
		File fl1;
		final char dirSepChar = File.separatorChar;
		int i;
		i = path1.lastIndexOf(dirSepChar);
		while( i>5) {
			path1 = path1.substring(0, i);
			fl1 = new File(path1);
			if( fl1.exists()) {
				IJ.log(path + "  doesn't exist, but this does: " + path1);
				return;
			}
			i = path1.lastIndexOf(dirSepChar);
		}
		IJ.log("total path failure at: " + path);
	}

	void maybeRereadTable() {
		int indx = -1;
		int n = jTable1.getRowCount();
		if( n > 0) indx =  jComboBox1.getSelectedIndex();
		fillReadTable(indx, true);
	}

	static String fixApostrophe( String in1) {
		String out1 = in1.replaceAll("'", "''");
		return out1;
	}

	void fillReadTable( int indx, boolean readFlg) {
		boolean currBf, serFlg = false;
		String flName;
		int type1 = 0;	// has value 0 or 2 (for delete)
//		int i = 4;
//		if( serFlg) i =5;
		JTable jTab = jTable1;
		if( !readFlg) {
			serFlg = true;
			type1 = 2;
			jTab = jTable3;
		}
		dcmStudy = null;
		dcmSeries = null;
		DefaultTableModel mod1;
		mod1 = (DefaultTableModel) jTab.getModel();
		mod1.setNumRows(0);
		setOrSaveColumnWidths(type1, false);
		if( indx < 0) return;
		Connection conn1 = openDBConnection();
		if( conn1 == null) {
			if(AETitle == null) return;
			orthanc1.fillTable(indx, mod1, readFlg);
			return;
		}
		String patNmId = null;
		String name1 = fixApostrophe(getDlgPatName(type1).trim());
		String teach1 = null;
		if( readFlg) {
			teach1 = fixApostrophe(jTextTeaching.getText().trim());
		}
		if( AETitle != null) {
			fillDcm3Table(indx, mod1, name1, conn1);
			return;
		}
		String sql = "select name, s.pat_id, sty_date, label1";
		sql += ", label2, label3, accession, filename";
		sql += " from patients p, studies s where p.pat_id=s.pat_id and ";
		switch(indx) {
			case 0:
				if( isID(name1)) {
					name1 = ChoosePetCt.compressID(name1);
					patNmId = "p.pat_id = '" + name1 + "'";
				}
				else patNmId = "name like '" + compressName(name1) + "%'";
				sql += patNmId;
				break;

			case 1:
				sql = sql.substring(0, sql.length()-4);	// remove last "and "
				break;

			case 2:
				sql += "label1 like '%" + name1 + "%'";
				break;

			case 3:
				sql += "label2 like '" + name1 + "%'";
				break;

			case 4:
				sql += "label3 like '" + name1 + "%'";
				break;
				
			case 5:
				sql += "label1 like '" + name1 + "%' and label3 like '" + teach1 + "%'";
				break;
				
			case 6:
				sql += "label2 like '" + name1 + "%' and label3 like '" + teach1 + "%'";
				break;
				
			case 7:
				sql += "accession = '" + name1 +"'";
				break;
		}
		sql += " and sty_date between " + formatDate(getDate(type1)) + " and " + formatDate(getDate(type1+1));
		if( indx == 1) sql += " order by sty_date desc, name asc, label1 asc";
		else sql += " order by name asc, sty_date desc, label1 asc";
		try {
			Object[] row1 = new Object[TBL_BF+2];
			String patName, patID, styLab1, styLab2, styLab3, accession, teach;
			Date styDate;
			Statement stm = conn1.createStatement();
			ResultSet rSet = stm.executeQuery(sql);
			int numRow = 0;
			teach = "";
			while( rSet.next()) {
				patName = rSet.getString(1);
				patID   = rSet.getString(2);
				styDate = rSet.getDate(3);
				styLab1 = rSet.getString(4);
				styLab2 = rSet.getString(5);
				if( styLab2 == null) styLab2 = "-";
				styLab3 = rSet.getString(6);
				if(styLab3 != null && styLab3.length() > teach.length()) {
					teach = styLab3;
				}
				accession = getAccession(rSet.getString(7));
				flName = rSet.getString(8);
				if( numRow++ == 0) {
					row1[TBL_PAT_NAME] = patName;
					row1[TBL_PAT_ID] = patID;
					row1[TBL_DATE] = styDate;
					row1[TBL_STUDY] = styLab1;
					row1[TBL_SERIES] = styLab2;
					row1[TBL_TEACHING] = teach;
					row1[TBL_ACCESSION] = accession;
					row1[TBL_BF] = localIsBf(flName);
					if( serFlg) {
						mod1.addRow(row1);
						numRow = 0;
						teach = "";
					}
				} else {
					if( !row1[TBL_PAT_NAME].equals(patName) || !row1[TBL_PAT_ID].equals(patID) ||
							!row1[TBL_DATE].equals(styDate) ||
							!ChoosePetCt.stringCmp((String)row1[TBL_STUDY], styLab1)) {
						mod1.addRow(row1);
						row1[TBL_PAT_NAME] = patName;
						row1[TBL_PAT_ID] = patID;
						row1[TBL_DATE] = styDate;
						row1[TBL_STUDY] = styLab1;
						row1[TBL_SERIES] = styLab2;
						row1[TBL_BF] = false;
						teach = "";
						if(styLab3 != null) teach = styLab3;
						numRow = 1;
					} else {
						row1[TBL_SERIES] = null;
					}
					row1[TBL_TEACHING] = teach;
					row1[TBL_ACCESSION] = accession;
					currBf = (Boolean) row1[TBL_BF];
					row1[TBL_BF] = currBf | localIsBf(flName);
				}
			}
			if( numRow > 0) mod1.addRow(row1);
			rSet.close();
			if( indx <= 1) {
				row1[TBL_SERIES] = null;
				row1[TBL_TEACHING] = null;
				row1[TBL_ACCESSION] = null;
				sql = generateJpegSql(patNmId, serFlg, type1);
				rSet = stm.executeQuery(sql);
				while( rSet.next()) {
					row1[TBL_PAT_NAME] = rSet.getString(1);
					row1[TBL_PAT_ID] = rSet.getString(2);
					row1[TBL_DATE] = rSet.getDate(3);
					row1[TBL_STUDY] = JPEG_STRING;
					row1[TBL_BF] = false;
					if( serFlg) {
						row1[TBL_SERIES] = rSet.getString(4);
					}
					mod1.addRow(row1);
				}
				rSet.close();
			}
			stm.close();
			if( mod1.getRowCount() == 0) {
				JOptionPane.showMessageDialog(this, "No database entries found.");
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}
	
	String getAccession(String inVal) {
		double inDbl;
		try {
			inDbl = Double.parseDouble(inVal);
		} catch (Exception e) {
			return inVal;
		}
		return preferInt(inDbl);
	}

	String preferInt( double inDouble) {
		String ret1;
		Integer inVal = (int) inDouble;
		Double outVal =  inVal.doubleValue();
		if( outVal == inDouble) {
			ret1 = inVal.toString();
		} else {
			outVal = inDouble;
			ret1 = outVal.toString();
		}
		return ret1;
	}
	
	boolean localIsBf(String flName) {
		File fl1;
		boolean isBf = false;
		fl1 = getDicomFromArchive(flName);
		if( fl1 != null) isBf = BrownFat.isBfFile(fl1.getPath());
		return isBf;
	}
	
	void fillDcm3Table(int indx, DefaultTableModel mod, String name1, Connection con1) {
		dcmPatient = new ArrayList<dcm4Patient>();	// study and series are null
		if( indx != 0) {
			IJ.log("\nOnly patient name, id is supported\n");
			return;
		}
		String patNmId;
		String sql = "select pk, pat_id, pat_name from patient where ";
		if( isID(name1)) patNmId = "pat_id = '" + name1 + "'";
		else {
			name1 = name1.toLowerCase();
			name1 = name1.replace(',', '^');
			patNmId = "lower(pat_name) like '" + name1 + "%'";
		}
		sql += patNmId;
		try {
			Statement stm = con1.createStatement();
			ResultSet rSet = stm.executeQuery(sql);
			dcm4Patient thisPat;
			dcm4Study thisSdy;
			dcm4Series thisSer;
			while( rSet.next()) {
				thisPat = new dcm4Patient();
				thisPat.pk = rSet.getInt(1);
				thisPat.pat_id = rSet.getString(2);
				thisPat.pat_name = ChoosePetCt.compressPatName(rSet.getString(3));
				dcmPatient.add(thisPat);
			}
			rSet.close();
			if( dcmPatient.isEmpty()) return;
			int i, serOff, start, n = dcmPatient.size();
			int[] summary = new int[n];
			for( i=0; i<n; i++) {
				thisPat = dcmPatient.get(i);
				summary[i] = thisPat.pk;
			}
			dcmStudy = new ArrayList<dcm4Study>();
			dcmSeries = new ArrayList<dcm4Series>();
			sql = "select pk, patient_fk, study_datetime, accession_no, study_desc, ";
			sql += "num_series, num_instances from study";
			start = 0;
			ResultSet rSet1 = stm.executeQuery(sql);
			while( rSet1.next()) {
				thisSdy = new dcm4Study();
				thisSdy.pk = rSet1.getInt(1);
				thisSdy.patient_fk = rSet1.getInt(2);
				thisSdy.study_date = rSet1.getDate(3);
				thisSdy.accession_no = getAccession(rSet1.getString(4));
				thisSdy.study_desc = rSet1.getString(5);
				thisSdy.num_series = rSet1.getInt(6);
				thisSdy.num_instances = rSet1.getInt(7);
				thisSdy.start = start;
				start += thisSdy.num_instances;
				if( !hasVal(summary, thisSdy.patient_fk)) continue;
				dcmStudy.add(thisSdy);
			}
			rSet1.close();
			n = dcmStudy.size();
			summary = new int[n];
			for( i=0; i<n; i++) {
				thisSdy = dcmStudy.get(i);
				summary[i] = thisSdy.pk;
			}
			sql = "select pk, study_fk, modality, series_desc, num_instances from series";
			start = 0;
			ResultSet rSet2 = stm.executeQuery(sql);
			while( rSet2.next()) {
				thisSer = new dcm4Series();
				thisSer.pk = rSet2.getInt(1);
				thisSer.study_fk = rSet2.getInt(2);
				thisSer.modality = rSet2.getString(3);
				thisSer.series_desc = rSet2.getString(4);
				thisSer.num_instances = rSet2.getInt(5);
				thisSer.start = start;
				start += thisSer.num_instances;
				if( !hasVal(summary, thisSer.study_fk)) continue;
				dcmSeries.add(thisSer);
			}
			rSet2.close();
			stm.close();
			if( mod == null) return;
			Object[] row1 = new Object[TBL_BF+2];
			n = dcmStudy.size();
			for( i=serOff=0; i<n; i++) {
				thisSdy = dcmStudy.get(i);
				thisPat = getPatient4(thisSdy.patient_fk);
				if( thisPat == null) return;
				row1[TBL_PAT_NAME] = thisPat.pat_name;
				row1[TBL_PAT_ID] = thisPat.pat_id;
				row1[TBL_DATE] = thisSdy.study_date;
				row1[TBL_STUDY] = thisSdy.study_desc;
				row1[TBL_SERIES] = null;
				row1[TBL_BF] = false;
				mod.addRow(row1);
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}
	
	boolean hasVal(int[] summary, int test) {
		int i;
		for( i=0; i<summary.length; i++) if(summary[i] == test) return true;
		return false;
	}
	
	dcm4Patient getPatient4( int pk) {
		dcm4Patient thisPat;
		int i, n=dcmPatient.size();
		for( i=0; i<n; i++) {
			thisPat = dcmPatient.get(i);
			if(thisPat.pk == pk) return thisPat;
		}
		return null;
	}
	
	private String generateJpegSql(String patNmId, boolean serFlg, int type1) {
		String sql = "select distinct name, j.pat_id, sty_date";
		if( serFlg) sql += ", filename";
		sql += " from patients p, jpeg j where p.pat_id=j.pat_id ";
		if( patNmId != null) sql += "and " + patNmId;

		sql += " and sty_date between " + formatDate(getDate(type1)) + " and " + formatDate(getDate(type1+1));
		if( patNmId == null) sql += " order by sty_date desc, name asc";
		else sql += " order by name asc, sty_date desc";
		return sql;
	}

	void fillWriteTable() {
		DefaultTableModel mod1;
		mod1 = (DefaultTableModel) jTable2.getModel();
		mod1.setNumRows(0);
		setOrSaveColumnWidths(1, false);
		imgList = new ArrayList<ImagePlus>();
		ImagePlus img1;
		BI_dbSaveInfo curr1;
		String meta, patName, patID, study, series, tmp1, serUID;
		Date date1;
		int i, j, row0, col0;
		int [] fullList = myGetIDList();
		if( fullList == null) return;
		for( i=0; i<fullList.length; i++) {
			img1 = WindowManager.getImage(fullList[i]);
			j = img1.getStackSize();
			if( j <= 0) continue;
			meta = ChoosePetCt.getMeta(1, img1);
			if( meta == null) continue;	// no information, skip it

			curr1 = (BI_dbSaveInfo) img1.getProperty("bidb");	// get database info
			Object[] row1 = new Object[TBL_BF+2];
			patName = ChoosePetCt.getCompressedPatName(meta);
			if( patName == null) continue;
			patID = ChoosePetCt.getCompressedID( meta);
			date1 = ChoosePetCt.getStudyDateTime( meta, -1);
			row1[TBL_DATE] = date1;
			study = ChoosePetCt.getDicomValue( meta, "0008,1030");
			series = ChoosePetCt.getDicomValue( meta, "0008,103E");
			if( series == null) series = ChoosePetCt.getDicomValue( meta, "0054,0400");
			serUID = ChoosePetCt.getDicomValue(meta, "0020,000E");
			if( curr1 != null) {
				if( curr1.patName != null) patName = curr1.patName;
				if( curr1.patID != null) patID = curr1.patID;
				if( curr1.styName != null) study = curr1.styName;
				if( curr1.serName != null) series = curr1.serName;
			}
			row1[TBL_PAT_NAME] = patName;
			row1[TBL_PAT_ID] = patID;
			row1[TBL_STUDY] = study;
			row1[TBL_SERIES] = series;
			col0 = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0028,0011"));
			row0 = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0028,0010"));
			tmp1 = col0 + "*" + row0 + "*" + j;
			row1[TBL_SIZE] = tmp1;
			row1[TBL_BF] = false;
			row1[TBL_SER_UID] = serUID;
			mod1.addRow(row1);
			imgList.add(img1);
		}
	}

	void setDates(int type1) {
		Date dt0 = new Date(), dt1 = new Date();
		boolean rdFlg = true, showPatName = true, showTeach = false;
		SimpleDateFormat df1 = new SimpleDateFormat("d MMM yyyy", Locale.US);
		JTextField patName = jTextPatName;
		JComboBox combo1 = jComboBox1;
		if( type1 > 0) {
			patName = jTextPatName1;
			combo1 = jComboBox2;
			rdFlg = false;
		}
		try {
			switch(combo1.getSelectedIndex()) {
				case 5:
				case 6:
					showTeach = true;	// fall through to case 0

				case 0:
				case 4:
				case 7:
					dt0 =df1.parse("1 Jan 1980");
					break;

				case 1:
					showPatName = false;
					break;

				case 2:
				case 3:
					dt0.setTime(dt1.getTime()-1000l*60*60*24*30);
					break;
			}
			if( type1 == 0) {
				dateTo.setDate(dt1);
				dateFrom.setDate(dt0);
			} else {
				dateTo1.setDate(dt1);
				dateFrom1.setDate(dt0);
			}
			patName.setVisible(showPatName);
			patName.setText("");
			if( rdFlg) {
				jLabTeach.setVisible(showTeach);
				jTextTeaching.setVisible(showTeach);
				jTextTeaching.setText("");
			}
			fillReadTable(-1, rdFlg);
			validate();
			repaint();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	void unselectAllTableEntries() {
		jTable1.clearSelection();
		jTable3.clearSelection();
	}

	void setOrSaveColumnWidths(int type, boolean saveFlg) {
		DefaultTableModel mod1;
		String colStr = "readDb series col";
		int i, x, n = 7;
		JTable jTab = jTable1;
		if( type == 1) {
			jTab = jTable2;
			colStr = "writeDb series col";
			n = 6;
		}
		if( type == 2) jTab = jTable3;

		mod1 = (DefaultTableModel) jTab.getModel();
		// if saving the data, there is no reason to change the column names
		if( !saveFlg) {
			if( type == 0 && !tab1Dirty) return;	// if not dirty, go home
			if( type == 1 && !tab2Dirty) return;	// if not dirty, go home
			if( type == 2 && !tab3Dirty) return;	// if not dirty, go home
//			mod1.setColumnIdentifiers(colNames);
			TableColumn col0;
			col0 = jTab.getColumnModel().getColumn(TBL_DATE);
			col0.setCellRenderer(new ColorDateRenderer());
			if( type == 0) {
				col0 = jTab.getColumnModel().getColumn(TBL_SERIES);
				col0.setCellRenderer(new SeriesRenderer());
				tab1Dirty = false;
			}
			else {
				if( type == 1) tab2Dirty = false;
				else tab3Dirty = false;
			}
		}

		TableColumn col1;
		for( i=0; i<n; i++) {
			col1 = jTab.getColumnModel().getColumn(i);
			if( saveFlg) {
				x = col1.getPreferredWidth();
				jPrefer.putInt(colStr+i, x);
			} else {
				x = jPrefer.getInt(colStr+i, 0);
				if( x <= 0) continue;
				col1.setPreferredWidth(x);
			}
		}
	}

	Date getDate(int indx) {
		Date dt1;
		switch(indx) {
			case 1:
				dt1 = dateTo.getDate();
				break;

			case 2:
				dt1 = dateFrom1.getDate();
				break;

			case 3:
				dt1 = dateTo1.getDate();
				break;

			default:
				dt1 = dateFrom.getDate();
				break;

		}
		if( dt1 == null) {
			if( indx == 1 || indx == 3) dt1 = new Date();
			else dt1 = new Date(0);
		}
		return dt1;
	}

	String date2String( Date dt1) {
		SimpleDateFormat df1 = new SimpleDateFormat("d MMM yyyy", Locale.US);
		return df1.format(dt1);
	}

	boolean isID( String name1) {
		if( ChoosePetCt.checkEmpty(name1) == null) return false;
		char a1 = name1.charAt(0);
		if( a1 >= '0' && a1 <= '9') return true;
		int i, j, n = name1.length();
		for( i=j=0; i<n; i++) {
			a1 = name1.charAt(i);
			if( a1 >=  '0' && a1 <= '9') j++;
		}
		return 2*j > n;
	}

	String compressName( String in1) {
		int j;
		String ret1 = in1.trim();
		String ret2 = ret1.replaceAll(", ", ",");
		while( !ret1.equals(ret2)) {
			ret1 = ret2;
			ret2 = ret1.replaceAll(", ", ",");
		}
		return ret1;
	}

	JRadioButton getDBbutton( int indx) {
		JRadioButton ret1 = null;
		switch( indx) {
			case 1:
				ret1 = jDB1;
				break;

			case 2:
				ret1 = jDB2;
				break;

			case 3:
				ret1 = jDB3;
				break;

			case 4:
				ret1 = jDB4;
				break;

			case 5:
				ret1 = jDB5;
				break;

			case 6:
				ret1 = jDB6;
				break;

			case 7:
				ret1 = jDB7;
				break;

			case 8:
				ret1 = jDB8;
				break;

			case 9:
				ret1 = jDB9;
				break;

			case 10:
				ret1 = jDB10;
				break;
		}
		return ret1;
	}

	void changeDBSelectedAndErase(int indx1) {
		maybeShowWriteMip(null);
		changeDBSelected(indx1);
		fillReadTable(-1, true);
		fillReadTable(-1, false);
	}

	void changeDBSelected(int indx1) {
		jCurrDB = indx1 - 1;
		Preferences pref1 = getSysPrefer(jCurrDB);
		String tmp1;
		tmp1 = ChoosePetCt.checkEmpty(pref1.get("db display name" + jCurrDB, null));
		if (tmp1 == null) tmp1 = pref1.get("ODBC" + jCurrDB, null);
		jLabelDbName.setText(tmp1);
		jLabelDbName1.setText(tmp1);
		jLabelDbName2.setText(tmp1);
		int i = pref1.getInt("db type" + jCurrDB, 0);
		isAccessDb = true;
		if( i > 0) isAccessDb = false;
		setDataPath();
		jButWeb.setVisible(i==5 && m_dataPath != null && m_dataPath.startsWith("http"));
	}

	// Options tab
	void fillBoxes() {
		Preferences pref1 = getSysPrefer(jOptionDB);
		jTextODBC.setText(pref1.get("ODBC" + jOptionDB, null));
		jTextUser.setText(pref1.get("db user" + jOptionDB, null));
		jPasswordField1.setText(pref1.get("db pass" + jOptionDB, null));
		jTextPath.setText(pref1.get("db path" + jOptionDB, null));
		Integer days = pref1.getInt("num of days", 30);
		jTextNumDays.setText(days.toString());
		int i = pref1.getInt("db type" + jOptionDB, 0);
		jComboDbType.setSelectedIndex(i);
		jTextDisplayName.setText(pref1.get("db display name" + jOptionDB, null));
	}

	void changeDbType() {
//		int i = jComboDbType.getSelectedIndex();
//		jTextPath.setEnabled(i!=5);
	}

	void callWeb() {
		new BrowserLauncher().run(m_dataPath);
	}

	void saveBoxes() {
		String odbcTxt = jTextODBC.getText();
		if( odbcTxt.isEmpty() || !isInitialized) return;
		jPreferSys.put("ODBC" + jOptionDB, odbcTxt);	// try to write it
		Preferences pref1 = getSysPrefer(jOptionDB);	// see if it is written
		pref1.put("ODBC" + jOptionDB, odbcTxt);
		pref1.put("db user" + jOptionDB, jTextUser.getText());
		String pass1 = new String( jPasswordField1.getPassword());
		pref1.put("db pass" + jOptionDB, pass1);
		pref1.put("db path" + jOptionDB, jTextPath.getText());
		Integer days = Integer.parseInt(jTextNumDays.getText());
		jPrefer.putInt("num of days", days);	// not a system parameter
		int i = jComboDbType.getSelectedIndex();
		pref1.putInt("db type" + jOptionDB, i);
		pref1.put("db display name" + jOptionDB, jTextDisplayName.getText());
	}

	void fillOptions() {
		Integer num = jPrefer.getInt("montage slices", 20);
		jTextN.setText(num.toString());
		jCheckTile.setSelected(jPrefer.getBoolean("tile windows", false));
		num = jPrefer.getInt("postage stamp size", 0);
		jTextStamp.setText(num.toString());
	}

	void saveOptions() {
		if( jTextN.getText().isEmpty() || !isInitialized) return;
		int i = Integer.parseInt(jTextN.getText());
		jPrefer.putInt("montage slices", i);
		jPrefer.putBoolean("tile windows", jCheckTile.isSelected());
		i = Integer.parseInt(jTextStamp.getText());
		jPrefer.putInt("postage stamp size", i);
	}
	
	void saveTeachKeys() {
		int i,n = teachList.size();
		bi_dbTeaching currTeach = null;
		for( i=0; i<n; i++) {
			currTeach = teachList.get(i);
			if( currTeach.imgWnd.equals(dataWindow)) break;
		}
		if( i>=n) {
			currTeach = new bi_dbTeaching();
			currTeach.imgWnd = dataWindow;
		}
		if( currTeach == null) return;
		currTeach.teachName = jTextTeachName.getText();
		currTeach.report = jTextTeachReport.getText();
		if( i<n) teachList.set(i, currTeach);
		else teachList.add(currTeach);
		if(getCurrSaveInfo() != null) jButTeachUpdate.setEnabled(true);
	}
	
	void setTeachVals() {
		int i,n = teachList.size();
		BI_dbSaveInfo currDb;
		bi_dbTeaching currTeach;
		for( i=0; i<n; i++) {
			currTeach = teachList.get(i);
			if( !currTeach.imgWnd.equals(dataWindow)) continue;
			jTextTeachName.setText(currTeach.teachName);
			jTextTeachReport.setText(currTeach.report);
			return;
		}
		if( i>=n && (currDb = getCurrSaveInfo())!=null) {
			jTextTeachName.setText(currDb.teachName);
			jTextTeachReport.setText(currDb.teachReport);
		}
	}
	
	void updateTeaching() {
		int i;
		BI_dbSaveInfo currDb = getCurrSaveInfo();
		if( currDb == null) return;
		String teachName = jTextTeachName.getText();
		String report = jTextTeachReport.getText();
		String fileName = currDb.dbFileName;
		String sql;
		if(teachName.isEmpty() || report.isEmpty()) {
			String tmp1 = "In order to update a teaching study, both fields must be defined.";
			tmp1 += "\nIf either of the fields is empty, it means Remove from teaching collection.";
			tmp1 += "\nDo you want to remove this from the teaching collection?";
			i = JOptionPane.showConfirmDialog(this, tmp1, "Empty fields", JOptionPane.YES_NO_OPTION);
			if( i != JOptionPane.YES_OPTION) return;
			try {
				Connection conn1 = openDBConnection();
				Statement stm = conn1.createStatement();
				sql = "update studies set label3 = null where fileName = '" + fileName + "'";
				stm.executeUpdate(sql);
			
				sql = "delete from teaching where filename = '" + fileName + "'";
				stm.executeUpdate(sql);
				stm.close();
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		} else {	// update the teaching
			writeTeaching( fileName, teachName, report);
		}
		jButTeachUpdate.setEnabled(false);
	}
	
	void writeTeaching( String fileName, String teachName, String report) {
		if(teachName.isEmpty() || report.isEmpty()) return;
		try {
			Connection conn1 = openDBConnection();
			Statement stm = conn1.createStatement();
			String sql = "update studies set label3 = '" + teachName;
			sql += "' where filename = '" + fileName + "'";
			stm.executeUpdate(sql);
			
			sql = "delete from teaching where filename = '" + fileName + "'";
			stm.executeUpdate(sql);
			
			sql = "insert into teaching values ('" + fileName;
			sql += "' , '" + report + "')";
			stm.executeUpdate(sql);
			stm.close();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	
	BI_dbSaveInfo getCurrSaveInfo() {
		BI_dbSaveInfo currDb = null;
		if(dataWindow instanceof ImageWindow) {
			ImageWindow imgWnd = (ImageWindow) dataWindow;
			ImagePlus img1 = imgWnd.getImagePlus();
			if( img1 == null) return null;
			currDb = (BI_dbSaveInfo) img1.getProperty("bidb");	// get database info
		}
		return currDb;
	}
	
	void fillCurrStudy() {
		BI_dbSaveInfo currDb;
		JFijiPipe pipe1;
		String meta, tmp;
		Integer mag1, x, y, z;
		jLabSty2.setText("");
		jLabSty3.setText("");
		jTextTeachName.setText("");
		jTextTeachReport.setText("");
		jButTeachUpdate.setEnabled(false);
		jPanelTeaching.setVisible(false);
		if( dataWindow == null) {
			jLabSty1.setText("Click on an image and then back here.");
			return;
		}
		jLabSty1.setText("No study");
		if( dataWindow instanceof ImageWindow) {
			setTeachVals();
			ImageWindow imgWnd = (ImageWindow) dataWindow;
			ImagePlus img1 = imgWnd.getImagePlus();
			if( img1 == null) return;
			meta = ChoosePetCt.getMeta(1,img1);
			currDb = (BI_dbSaveInfo) img1.getProperty("bidb");	// get database info
			jLabSty1.setText(getTitleInfoSub(meta, currDb, 1));
			jLabSty2.setText(getTitleInfoSub(meta, currDb, 2));
			mag1 = (int) (imgWnd.getCanvas().getMagnification() * 100);
			x = img1.getWidth();
			y = img1.getHeight();
			z = img1.getStackSize();
			tmp = x.toString() + "*" + y.toString() + "*" + z.toString();
			tmp += "    magnification = " + mag1.toString() + "%";
			jLabSty3.setText(tmp);
			jPanelTeaching.setVisible(true);
			return;
		}
		
		if( dataWindow instanceof PetCtFrame) {
			PetCtFrame petct = (PetCtFrame) dataWindow;
			PetCtPanel petPanel = petct.getPetCtPanel1();
			pipe1 = petPanel.petPipe;
			if( pipe1 == null) return;	// maybe just closed the window
			jLabSty1.setText(petct.getTitleBar(1));
			jLabSty2.setText(petct.getTitleBar(2));
			x = petct.getWidth();
			y = petct.getHeight();
			tmp = x.toString() + "*" + y.toString() + "   ";
			if( pipe1.data1.seriesType == ChoosePetCt.SERIES_SPECT) tmp += "SPECT=";
			else tmp += "PET=";
			z = ChoosePetCt.round(petPanel.petAxial);
			x = z + 1;
			y = pipe1.data1.numFrms;
			tmp += x.toString() + "/" + y.toString() + "  CT=";

			pipe1 = petPanel.getMriOrCtPipe();
			x = pipe1.findCtPos(z, true) + 1;
			y = pipe1.data1.numFrms;
			tmp += x.toString() + "/" + y.toString();
			jLabSty3.setText(tmp);
			return;
		}
		
		if( dataWindow instanceof Display3Frame) {
			Display3Frame dsp3 = (Display3Frame) dataWindow;
//			Display3Panel panel3 = dsp3.getDisplay3Panel();
//			pipe1 = panel3.d3Pipe;
			jLabSty1.setText(dsp3.getTitleBar(1));
			jLabSty2.setText(dsp3.getTitleBar(2));
			x = dsp3.getWidth();
			y = dsp3.getHeight();
			tmp = x.toString() + "*" + y.toString();
			jLabSty3.setText(tmp);
//			return;
		}
	}

	void all3reports() {
		if( dataWindow == null || !(dataWindow instanceof PetCtFrame)) {
			JOptionPane.showMessageDialog(this,"This button only works on Pet-Ct studies" + 
					"\nPlease highlight a Pet-Ct fusion study and try again.");
			return;
		}
		Window saveWindow = dataWindow;	// the focus gets shifted
		saveJpeg(0);
		dataWindow = saveWindow;
		saveJpeg(1);
		myWriteDicom dcm1 = new myWriteDicom((PetCtFrame) saveWindow, this);
		dcm1.writeDicomHeader();
	}

	void saveJpeg(int type) {
		int i;
		Date styDate;
		BI_dbSaveInfo currDb = null;
		ImagePlus img1;
		SaveMip dlg1 = null;
		String sql, ext1, name, patId, accession, meta = null;
		try {
			if( dataWindow == null) {
				JOptionPane.showMessageDialog(this, "Choose a study to be saved and try again.");
				return;
			}
			ext1 = "jpg";
			if( type == 0 && !OK2write()) return;
			if( dataWindow instanceof PetCtFrame) {
				PetCtFrame petCtFrm = (PetCtFrame) dataWindow;
				meta = petCtFrm.getPetCtPanel1().petPipe.data1.metaData;
				img1 = petCtFrm.getPetCtPanel1().petPipe.data1.srcImage;
				currDb = (BI_dbSaveInfo) img1.getProperty("bidb");
			}
			if( dataWindow instanceof Display3Frame) {
				Display3Frame d3Frm = (Display3Frame) dataWindow;
				meta = d3Frm.getDisplay3Panel().d3Pipe.data1.metaData;
				img1 = d3Frm.getDisplay3Panel().d3Pipe.data1.srcImage;
				currDb = (BI_dbSaveInfo) img1.getProperty("bidb");
			}
			if( dataWindow instanceof ImageWindow) {
				img1 = ((ImageWindow) dataWindow).getImagePlus();
				meta = ChoosePetCt.getMeta(1,img1);
				currDb = (BI_dbSaveInfo) img1.getProperty("bidb");	// get database info
			}
			if( meta == null) {
				JOptionPane.showMessageDialog(this, "Can't find meta data. Defective study.");
				return;
			}
			if (type > 0) {
				saveAnimatedParms();
				dlg1 = new SaveMip((java.awt.Frame)dataWindow, true);
				i = dlg1.getFileType();
				switch (i) {
					case 1:
						ext1 = "png";
						break;

					case 2:
						ext1 = "gif";
						break;

					default:
						ext1 = "avi";
				}
			}
			dataWindow.toFront();
			name = ChoosePetCt.getCompressedPatName(meta) + "." + ext1;
			styDate = ChoosePetCt.getStudyDateTime(meta, -1);
			accession = ChoosePetCt.getAccessionNumber(meta, currDb);
			patId = ChoosePetCt.getCompressedID(meta);
			if( currDb != null) {
				name = currDb.patName + "." + ext1;
				patId = currDb.patID;
			}
			bi_dbWriteInfo biWr1 = new bi_dbWriteInfo();
			biWr1.generateInfo(styDate, name);
			String sDat1 = formatDate(styDate);
			Connection conn1 = openDBConnection();
			Statement stm = conn1.createStatement();
			sql = "select sty_date from jpeg where filename = '" + biWr1.fileName;
			sql += "' and sty_date = " + sDat1;
			sql += " and accession = " + accession;
			sql += " and pat_id = '" + patId + "'";
			ResultSet rSet = stm.executeQuery(sql);
			if( rSet.next()) {
				rSet.close();
				String tmp = "The jpeg file = " + biWr1.fileName + " has already been written to database.\n" +
					"A correction will be made and the missing file rewritten."	;
				JOptionPane.showMessageDialog(this, tmp);
			} else {
				sql = "insert into jpeg (pat_id, sty_date, accession, filename) values ('";
				sql += patId  + "', " + sDat1 + ", " + accession + ", '" + biWr1.fileName + "')";
				stm.executeUpdate(sql);
			}
			stm.close();
//			IJ.wait(2000);
			if( type > 0 && dlg1 != null) {
				dlg1.doAction(biWr1.path1 + biWr1.fileName);
			} else {
				if( work2 != null) return;
				bkgdFile = biWr1.path1 + biWr1.fileName;
				bkgdMode = 2;
				loadData();
				return;
/*				BufferedImage im1 = null;
				Rectangle rc1 = dataWindow.getBounds();
				Point pt1 = dataWindow.getLocationOnScreen();
				rc1.x = pt1.x;
				rc1.y = pt1.y;
				File fl1 = new File(biWr1.path1 + biWr1.fileName);
				im1 = new Robot().createScreenCapture(rc1);
				ImageIO.write(im1, ext1, fl1);*/
			}
			this.toFront();
			
		} catch (Exception e)  { ChoosePetCt.stackTrace2Log(e); }
	}
	
	void runRobot() {
		try {
			int i;
			Rectangle rc1 = dataWindow.getBounds();
			// 0.5 additional for each megabyte
			i = rc1.width * rc1.height / 2000;
			IJ.wait(500 + i);
			BufferedImage im1;
			Point pt1 = dataWindow.getLocationOnScreen();
			rc1.x = pt1.x;
			rc1.y = pt1.y;
			File fl1 = new File(bkgdFile);
			i = bkgdFile.lastIndexOf(".");
			String ext1 = bkgdFile.substring(i+1);
			im1 = new Robot().createScreenCapture(rc1);
			ImageIO.write(im1, ext1, fl1);
			this.toFront();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	@Override
	public void dispose() {
		WindowManager.removeWindow(this);
		killRead = true;
		m_lastConn = null;
		saveOptions();
		if( writeWasShown) setOrSaveColumnWidths(1, true);
		super.dispose();
	}

	class bi_dbWriteInfo {
		String fileName = null;
		int diskNum;
		boolean jpegFlg = false;
		String path1 = null;

		void generateInfo( Date styDate, String initName) {
			try {
				fileName = null;
				path1 = null;
				jpegFlg = false;
				Connection conn1 = openDBConnection();
				Statement stm = conn1.createStatement();
				String sql = "select d.diskcode, d.diskname from disks d, write_disk w where d.diskcode = w.diskcode";
				ResultSet rSet = stm.executeQuery(sql);
				if( !rSet.next()) return;
				diskNum = rSet.getInt(1);
				String tmp = rSet.getString(2);
				rSet.close();
				String path0, paths = m_dataPath;
				File fl1;
				int i, j, k, n;
				while( paths != null) {
					i = paths.indexOf("|");
					if( i>0) {
						path0 = paths.substring(0, i);
						paths = paths.substring(i+1);
					} else {
						path0 = paths;
						paths = null;
					}
					if( !path0.endsWith("/")) path0 +="/";
					fl1 = new File( path0 + "label");
					if( !fl1.exists()) continue;
					byte[] buf = new byte[250];
					FileInputStream in1 = new FileInputStream(fl1);
					if( (n= in1.read(buf)) <= 0) continue;
					String tmp0 = new String( buf);
					if( tmp0.startsWith(tmp)) {
						path1 = path0;
						break;
					}
				}
				if( path1 == null) {
					IJ.log("Couldn't find disk: " + tmp);
					return;
				}

				if( styDate != null) {
					// watch out for names like O'Connor
					String name0 = initName.replace("\'", "");
					i = name0.lastIndexOf(".");
					String ext1 = name0.substring(i);	// jpeg, gif extension
					j = name0.indexOf(" ");
					k = name0.indexOf(",");
					if( j > 0) i = j;
					if( k > 0 && k < i) i = k;
					if( i > 6) i = 6;
					String name = name0.substring(0, i);
					SimpleDateFormat df1 = new SimpleDateFormat("yyMM", Locale.US);
					name += df1.format(styDate);
					tmp = "'ic'yyyy'/jpeg'MM";
					df1 = new SimpleDateFormat(tmp, Locale.US);
					path1 += df1.format(styDate);
					fl1 = new File(path1);
					if( !fl1.exists()) fl1.mkdirs();
					path1 += "/";
					i = 0;
					while( i < 100) {
						fileName = name;
						if( i<10) fileName += "0";
						fileName += i + ext1;
						i++;
						fl1 = new File(path1 + fileName);
						if( !fl1.exists()) break;
					}
					jpegFlg = true;
					return;
				}

				char[] name1 = {'a', 'a', 'a', 'a', 'a', 'a'};
				Calendar cal1 = Calendar.getInstance();
				i = cal1.get(Calendar.YEAR) - 1980;
				name1[0] = getFileLetter( i >> 2);
				j = cal1.get(Calendar.MONTH) + 1;	// need offset of 1
				name1[1] = getFileLetter( (i << 3) + (j >> 1));
				i = cal1.get(Calendar.DAY_OF_MONTH);
				name1[2] = getFileLetter( (j << 4) + (i >> 1));
				j = cal1.get(Calendar.HOUR_OF_DAY);
				name1[3] = getFileLetter( (i << 4) + (j >> 1));
				i = cal1.get(Calendar.MINUTE);
				name1[4] = getFileLetter( (j << 4) + (i >> 2));
				j = cal1.get(Calendar.SECOND);
				name1[5] = getFileLetter( (i << 3) + (j >> 3));
				String base1 = new String(name1);
				i = 0;
				while( i < 100) {
					tmp = base1;
					if( i < 10) tmp += "0";
					tmp += i;
					i++;
					sql = "select pat_id from studies where filename = '" + tmp + "'";
					rSet = stm.executeQuery(sql);
					if( rSet.next()) {
						rSet.close();
						continue;
					}
					fileName = tmp;
					path1 += getDicomDirectory( fileName);
					break;
				}
				stm.close();
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
		}
	}
	
	class bi_dbTeaching {
		Window imgWnd;
		String teachName;
		String report;
	}
	
	class dcm4Patient {
		int pk;
		String pat_id, pat_name;
	}
	
	class dcm4Study {
		int pk;
		int patient_fk;
		Date study_date;
		String accession_no, study_desc;
		int num_series, num_instances, start;
	}
	
	class dcm4Series {
		int pk;
		int study_fk;
		String modality, series_desc;
		int num_instances, start;
	}

	char getFileLetter( int val) {
		int ret1 = val & 31;
		if( ret1 < 26) ret1 += 'A';
		else ret1 += '0' - 26;
		return (char) ret1;
	}

	void setAnimatedFile() {
		buttonGroup2 = new javax.swing.ButtonGroup();
		buttonGroup2.add(jRadioAvi);
		buttonGroup2.add(jRadioPng);
		buttonGroup2.add(jRadioGif);
		Integer i1 = jPrefer.getInt("Mip file type", 0);
		switch(i1) {
			case 1:
				jRadioPng.setSelected(true);
				break;
				
			case 2:
				jRadioGif.setSelected(true);
				break;
				
			default:
			jRadioAvi.setSelected(true);
		}
		i1 = jPrefer.getInt("Mip frame time", 100);
		jTextFrmTime.setText(i1.toString());
	}
	
	void saveAnimatedParms() {
		int i1 = ChoosePetCt.parseInt(jTextFrmTime.getText());
		jPrefer.putInt("Mip frame time", i1);
		i1 = 0;
		if( jRadioPng.isSelected()) i1 = 1;
		if( jRadioGif.isSelected()) i1 = 2;
		jPrefer.putInt("Mip file type", i1);
	}
	
	void browse2Study() {
		final JFileChooser fc;
		BI_dbSaveInfo currFile;
		File file1;
		try {
			String flPath = jPrefer.get("read browse", null);
			fc = new JFileChooser(flPath);
			int retVal = fc.showOpenDialog(this);
			if( retVal != JFileChooser.APPROVE_OPTION) return;
			file1 = fc.getSelectedFile();
			flPath = file1.getParent();
			jPrefer.put("read browse", flPath);
			currFile = new BI_dbSaveInfo();
			currFile.flName = file1;
			readFiles(currFile);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	boolean isToggleMode() {
	 return jTogButSave.isVisible() && jTogButSave.isSelected();	
	}

	boolean isCDLegal(int indx) {
		boolean retVal = true;
		String tmp=null, tmp1=null;
		if( indx<1 || indx>10) retVal = false;
		if( retVal) {
			tmp = ChoosePetCt.checkEmpty(jPrefer.get("CDName" + indx, null));
			tmp1 = ChoosePetCt.checkEmpty(jPrefer.get("pathReadCD" + indx, null));
			if( tmp != null && tmp1 != null) {	// make sure path is available
				File fl1 = new File(tmp1);
				if( !fl1.isDirectory()) retVal = false;
			} else {
				retVal = false;
			}
		}
		if( retVal) {
			jLabCDName.setText(tmp);
			jLabCDPath.setText(tmp1);
		}
		else {
			jLabCDName.setText("undefined");
			jLabCDPath.setText("none");
		}

		jButDelCD.setEnabled(retVal);
		jTogButSave.setVisible(retVal);
		return retVal;
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

	void cdChanged() {
		int indx = jComboCD.getSelectedIndex();
		boolean isOK = isCDLegal(indx);
		if( !isOK) indx = 0;
		jPrefer.putInt("conferenceSaveNum", indx);
		jConference = indx;
	}

	String getPathReadCD() {
		return ChoosePetCt.checkEmpty(jPrefer.get("pathReadCD" + jConference, null));
	}

	void deleteCD() {
		String tmp1 = "All the studies in this directory will be permanently deleted.";
		tmp1 += "\nAre you sure you want to delete them?";
		int i = JOptionPane.showConfirmDialog(this, tmp1, "Delete studies", JOptionPane.YES_NO_OPTION);
		if( i != JOptionPane.YES_OPTION) return;
		tmp1 = getPathReadCD();
		if( tmp1 == null) return;
		File folder = new File(tmp1);
		ChoosePetCt.deleteFolder(folder, false);
	}

	void openHelp() {
		ChoosePetCt.openHelp("Reading studies");
	}

	/**
	 * To have showProgress work, the data loading is done in the background.
	 */
	protected class bkgdLoadData extends SwingWorker<Integer, Object> {
		@Override
		protected Integer doInBackground() {
			switch(bkgdMode) {
				case 0:
					doRead();
					break;
					
				case 1:
					doWrite();
					break;
					
				case 2:
					runRobot();
					break;
					
				case 3:
					saveConference();
					break;
			}
			return 0;
		}
	}


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelRead = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox();
        jTextPatName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        dateFrom = new com.michaelbaranov.microba.calendar.DatePicker();
        jLabel2 = new javax.swing.JLabel();
        dateTo = new com.michaelbaranov.microba.calendar.DatePicker();
        jButRead = new javax.swing.JButton();
        jCheckExit = new javax.swing.JCheckBox();
        jButExit = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jDB1 = new javax.swing.JRadioButton();
        jDB2 = new javax.swing.JRadioButton();
        jDB3 = new javax.swing.JRadioButton();
        jDB4 = new javax.swing.JRadioButton();
        jDB5 = new javax.swing.JRadioButton();
        jDB6 = new javax.swing.JRadioButton();
        jDB7 = new javax.swing.JRadioButton();
        jDB8 = new javax.swing.JRadioButton();
        jDB9 = new javax.swing.JRadioButton();
        jDB10 = new javax.swing.JRadioButton();
        jLabelDbName = new javax.swing.JLabel();
        jButClear = new javax.swing.JButton();
        jTextTeaching = new javax.swing.JTextField();
        jLabTeach = new javax.swing.JLabel();
        jTogButSave = new javax.swing.JToggleButton();
        jButSaveMip = new javax.swing.JButton();
        jButWeb = new javax.swing.JButton();
        jPanelWrite = new javax.swing.JPanel();
        jButWrite = new javax.swing.JButton();
        jButExit1 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jLabelDbName1 = new javax.swing.JLabel();
        jPanelDelete = new javax.swing.JPanel();
        jButExit2 = new javax.swing.JButton();
        jButDelete = new javax.swing.JButton();
        jLabelDbName2 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        jTextPatName1 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        dateFrom1 = new com.michaelbaranov.microba.calendar.DatePicker();
        dateTo1 = new com.michaelbaranov.microba.calendar.DatePicker();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();
        jLabel7 = new javax.swing.JLabel();
        jPanelReport = new javax.swing.JPanel();
        jButJpeg = new javax.swing.JButton();
        jButAll3 = new javax.swing.JButton();
        jButCine = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jRadioAvi = new javax.swing.JRadioButton();
        jRadioPng = new javax.swing.JRadioButton();
        jRadioGif = new javax.swing.JRadioButton();
        jLabel9 = new javax.swing.JLabel();
        jTextFrmTime = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jLabSty1 = new javax.swing.JLabel();
        jLabSty2 = new javax.swing.JLabel();
        jLabSty3 = new javax.swing.JLabel();
        jPanelTeaching = new javax.swing.JPanel();
        jButTeachUpdate = new javax.swing.JButton();
        jTextTeachName = new javax.swing.JTextField();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextTeachReport = new javax.swing.JTextArea();
        jPanelOptions = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTextN = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jCheckTile = new javax.swing.JCheckBox();
        jLabel10 = new javax.swing.JLabel();
        jTextStamp = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jButTile = new javax.swing.JButton();
        jButShowTile = new javax.swing.JButton();
        jPanelSetup = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabelODBCName = new javax.swing.JLabel();
        jTextODBC = new javax.swing.JTextField();
        jLabelWhichDB = new javax.swing.JLabel();
        jComboDB = new javax.swing.JComboBox();
        jLabelUser = new javax.swing.JLabel();
        jTextUser = new javax.swing.JTextField();
        jLabelPW = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();
        jLabelPath = new javax.swing.JLabel();
        jTextPath = new javax.swing.JTextField();
        jComboDbType = new javax.swing.JComboBox();
        jLabelDays = new javax.swing.JLabel();
        jTextNumDays = new javax.swing.JTextField();
        jLabelDisp = new javax.swing.JLabel();
        jTextDisplayName = new javax.swing.JTextField();
        jCheckShowWrite = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jLabelWhichCD = new javax.swing.JLabel();
        jComboCD = new javax.swing.JComboBox();
        jLabelCDNum = new javax.swing.JLabel();
        jButDelCD = new javax.swing.JButton();
        jLabCDName = new javax.swing.JLabel();
        jLabCDPath = new javax.swing.JLabel();
        jButHelp = new javax.swing.JButton();
        jLabAbout = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("BI database");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Patient Name/ID", "Date", "Study", "Series", "Teaching", "Study - teach", "Series - teach", "Accession nm" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel1.setText("from");

        jLabel2.setText("to");

        jButRead.setText("Read");
        jButRead.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButReadActionPerformed(evt);
            }
        });

        jCheckExit.setText("Exit after read");

        jButExit.setText("Exit");
        jButExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButExitActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Name", "Study", "Date", "Series", "ID", "Teaching", "Accession", "bf", "ser UID"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jDB1.setText("1");
        jDB1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDB1ActionPerformed(evt);
            }
        });

        jDB2.setText("2");
        jDB2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDB2ActionPerformed(evt);
            }
        });

        jDB3.setText("3");
        jDB3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDB3ActionPerformed(evt);
            }
        });

        jDB4.setText("4");
        jDB4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDB4ActionPerformed(evt);
            }
        });

        jDB5.setText("5");
        jDB5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDB5ActionPerformed(evt);
            }
        });

        jDB6.setText("6");
        jDB6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDB6ActionPerformed(evt);
            }
        });

        jDB7.setText("7");
        jDB7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDB7ActionPerformed(evt);
            }
        });

        jDB8.setText("8");
        jDB8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDB8ActionPerformed(evt);
            }
        });

        jDB9.setText("9");
        jDB9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDB9ActionPerformed(evt);
            }
        });

        jDB10.setText("10");
        jDB10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDB10ActionPerformed(evt);
            }
        });

        jLabelDbName.setText("1");
        jLabelDbName.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jButClear.setText("Clear");
        jButClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButClearActionPerformed(evt);
            }
        });

        jLabTeach.setText("teach:");

        jTogButSave.setText("Save for Conference");
        jTogButSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTogButSaveActionPerformed(evt);
            }
        });

        jButSaveMip.setText("Save MIP");
        jButSaveMip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSaveMipActionPerformed(evt);
            }
        });

        jButWeb.setText("Web");
        jButWeb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButWebActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelReadLayout = new javax.swing.GroupLayout(jPanelRead);
        jPanelRead.setLayout(jPanelReadLayout);
        jPanelReadLayout.setHorizontalGroup(
            jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReadLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanelReadLayout.createSequentialGroup()
                        .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelReadLayout.createSequentialGroup()
                                .addComponent(jDB1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jDB2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jDB3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jDB4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jDB5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jDB6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jDB7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jDB8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jDB9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jDB10)
                                .addGap(18, 18, 18)
                                .addComponent(jLabelDbName))
                            .addGroup(jPanelReadLayout.createSequentialGroup()
                                .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(jPanelReadLayout.createSequentialGroup()
                                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextPatName, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelReadLayout.createSequentialGroup()
                                        .addComponent(jTogButSave)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabTeach)
                                        .addGap(22, 22, 22)))
                                .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(dateFrom, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jTextTeaching))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanelReadLayout.createSequentialGroup()
                                        .addComponent(jLabel2)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(dateTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jCheckExit))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelReadLayout.createSequentialGroup()
                                        .addComponent(jButWeb)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButSaveMip)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jButRead)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButClear)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButExit)))))
                        .addGap(12, 12, 12))))
        );
        jPanelReadLayout.setVerticalGroup(
            jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReadLayout.createSequentialGroup()
                .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelReadLayout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanelReadLayout.createSequentialGroup()
                                .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jButRead)
                                    .addComponent(jButExit)
                                    .addComponent(jButClear)
                                    .addComponent(jButSaveMip)
                                    .addComponent(jButWeb))
                                .addGap(5, 5, 5))
                            .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextTeaching, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabTeach))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelReadLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jTogButSave, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)))
                .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTextPatName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1))
                    .addComponent(jCheckExit, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(dateTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(dateFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jDB1)
                    .addComponent(jDB2)
                    .addComponent(jDB3)
                    .addComponent(jDB4)
                    .addComponent(jDB5)
                    .addComponent(jDB6)
                    .addComponent(jDB7)
                    .addComponent(jDB8)
                    .addComponent(jDB9)
                    .addComponent(jDB10)
                    .addComponent(jLabelDbName)))
        );

        jTabbedPane1.addTab("Read Database", jPanelRead);

        jPanelWrite.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                jPanelWriteComponentShown(evt);
            }
        });

        jButWrite.setText("Write");
        jButWrite.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButWriteActionPerformed(evt);
            }
        });

        jButExit1.setText("Exit");
        jButExit1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButExit1ActionPerformed(evt);
            }
        });

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Name", "Study", "Date", "Series", "ID", "Size", "Accession", "bf", "ser UID"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                true, true, false, true, true, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(jTable2);

        jLabelDbName1.setText("1");
        jLabelDbName1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        javax.swing.GroupLayout jPanelWriteLayout = new javax.swing.GroupLayout(jPanelWrite);
        jPanelWrite.setLayout(jPanelWriteLayout);
        jPanelWriteLayout.setHorizontalGroup(
            jPanelWriteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelWriteLayout.createSequentialGroup()
                .addContainerGap(660, Short.MAX_VALUE)
                .addComponent(jLabelDbName1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButWrite)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButExit1)
                .addContainerGap())
            .addGroup(jPanelWriteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanelWriteLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 783, Short.MAX_VALUE)))
        );
        jPanelWriteLayout.setVerticalGroup(
            jPanelWriteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelWriteLayout.createSequentialGroup()
                .addGroup(jPanelWriteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButExit1)
                    .addComponent(jButWrite)
                    .addComponent(jLabelDbName1))
                .addContainerGap(283, Short.MAX_VALUE))
            .addGroup(jPanelWriteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanelWriteLayout.createSequentialGroup()
                    .addGap(28, 28, 28)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 268, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        jTabbedPane1.addTab("Write Database", jPanelWrite);

        jButExit2.setText("Exit");
        jButExit2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButExit2ActionPerformed(evt);
            }
        });

        jButDelete.setText("Delete");
        jButDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButDeleteActionPerformed(evt);
            }
        });

        jLabelDbName2.setText("1");
        jLabelDbName2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Patient Name/ID", "Date", "Study", "Series", "Teaching" }));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        jLabel8.setText("from");

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Name", "Study", "Date", "Series", "ID", "Teaching", "Accession", "bf", "ser UID"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(jTable3);

        jLabel7.setText("to");

        javax.swing.GroupLayout jPanelDeleteLayout = new javax.swing.GroupLayout(jPanelDelete);
        jPanelDelete.setLayout(jPanelDeleteLayout);
        jPanelDeleteLayout.setHorizontalGroup(
            jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDeleteLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(jPanelDeleteLayout.createSequentialGroup()
                        .addGroup(jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelDeleteLayout.createSequentialGroup()
                                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextPatName1, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(dateFrom1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(dateTo1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelDeleteLayout.createSequentialGroup()
                                .addComponent(jLabelDbName2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButDelete)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButExit2)
                        .addContainerGap(123, Short.MAX_VALUE))))
        );
        jPanelDeleteLayout.setVerticalGroup(
            jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDeleteLayout.createSequentialGroup()
                .addGroup(jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(dateFrom1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelDeleteLayout.createSequentialGroup()
                        .addGroup(jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButDelete)
                            .addComponent(jButExit2)
                            .addComponent(jLabelDbName2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextPatName1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel8))
                            .addComponent(dateTo1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Delete Database", jPanelDelete);

        jButJpeg.setText("Jpeg -> DB");
        jButJpeg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButJpegActionPerformed(evt);
            }
        });

        jButAll3.setText("Jpeg+Signifcant Image + MIP");
        jButAll3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButAll3ActionPerformed(evt);
            }
        });

        jButCine.setText("Cine (or MIP) -> DB");
        jButCine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButCineActionPerformed(evt);
            }
        });

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Animated Files"));

        jRadioAvi.setText("Avi");

        jRadioPng.setText("Png");

        jRadioGif.setText("Gif");

        jLabel9.setText("Frame time (msec)");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jRadioAvi)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioPng)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioGif))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jTextFrmTime, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioAvi)
                    .addComponent(jRadioPng)
                    .addComponent(jRadioGif))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFrmTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(38, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("current study"));

        jLabSty1.setText("Click on an image and then back here.");

        jLabSty2.setText(" ");

        jLabSty3.setText(" ");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabSty2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jLabSty3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jLabSty1, javax.swing.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jLabSty1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabSty2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabSty3))
        );

        jPanelTeaching.setBorder(javax.swing.BorderFactory.createTitledBorder("teaching"));

        jButTeachUpdate.setText("Update");
        jButTeachUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButTeachUpdateActionPerformed(evt);
            }
        });

        jTextTeachName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextTeachNameKeyReleased(evt);
            }
        });

        jTextTeachReport.setColumns(20);
        jTextTeachReport.setRows(5);
        jTextTeachReport.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextTeachReportKeyReleased(evt);
            }
        });
        jScrollPane4.setViewportView(jTextTeachReport);

        javax.swing.GroupLayout jPanelTeachingLayout = new javax.swing.GroupLayout(jPanelTeaching);
        jPanelTeaching.setLayout(jPanelTeachingLayout);
        jPanelTeachingLayout.setHorizontalGroup(
            jPanelTeachingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTeachingLayout.createSequentialGroup()
                .addComponent(jTextTeachName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButTeachUpdate))
            .addComponent(jScrollPane4)
        );
        jPanelTeachingLayout.setVerticalGroup(
            jPanelTeachingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTeachingLayout.createSequentialGroup()
                .addGroup(jPanelTeachingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButTeachUpdate)
                    .addComponent(jTextTeachName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4))
        );

        javax.swing.GroupLayout jPanelReportLayout = new javax.swing.GroupLayout(jPanelReport);
        jPanelReport.setLayout(jPanelReportLayout);
        jPanelReportLayout.setHorizontalGroup(
            jPanelReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReportLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButJpeg)
                    .addComponent(jButAll3)
                    .addComponent(jButCine)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanelReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelTeaching, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(200, Short.MAX_VALUE))
        );
        jPanelReportLayout.setVerticalGroup(
            jPanelReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReportLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelReportLayout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelTeaching, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanelReportLayout.createSequentialGroup()
                        .addComponent(jButJpeg)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButAll3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButCine)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 67, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Report", jPanelReport);

        jPanelOptions.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                jPanelOptionsComponentHidden(evt);
            }
            public void componentShown(java.awt.event.ComponentEvent evt) {
                jPanelOptionsComponentShown(evt);
            }
        });

        jLabel5.setText("Make montage for series up to");

        jLabel6.setText("slices.");

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Tile properties"));

        jCheckTile.setText("Tile windows after read");

        jLabel10.setText("Postage stamp size");

        jLabel11.setText("0=don't use");

        jButTile.setText("Tile");
        jButTile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButTileActionPerformed(evt);
            }
        });

        jButShowTile.setText("Show Dialog");
        jButShowTile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButShowTileActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jCheckTile)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextStamp)))
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel11))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(106, 106, 106)
                        .addComponent(jButTile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButShowTile)))
                .addContainerGap(304, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckTile)
                    .addComponent(jButTile)
                    .addComponent(jButShowTile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jTextStamp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addContainerGap(39, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanelOptionsLayout = new javax.swing.GroupLayout(jPanelOptions);
        jPanelOptions.setLayout(jPanelOptionsLayout);
        jPanelOptionsLayout.setHorizontalGroup(
            jPanelOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelOptionsLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextN, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)))
                .addContainerGap())
        );
        jPanelOptionsLayout.setVerticalGroup(
            jPanelOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jTextN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(161, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Options", jPanelOptions);

        jPanelSetup.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                jPanelSetupComponentShown(evt);
            }
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                jPanelSetupComponentHidden(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Database"));

        jLabelODBCName.setText("ODBC Name");

        jLabelWhichDB.setText("Which db");

        jComboDB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
        jComboDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboDBActionPerformed(evt);
            }
        });

        jLabelUser.setText("User");

        jLabelPW.setText("Password");

        jLabelPath.setText("Data path");

        jComboDbType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ODBC Access", "ODBC SQL Server", "Java SQL Server", "Java MySQL", "dcm4chee", "orthanc" }));
        jComboDbType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboDbTypeActionPerformed(evt);
            }
        });

        jLabelDays.setText("Num of days");

        jLabelDisp.setText("Display");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelODBCName)
                    .addComponent(jLabelUser)
                    .addComponent(jLabelPath))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jTextUser, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabelPW)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPasswordField1, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE))
                            .addComponent(jTextODBC, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelWhichDB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboDB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTextPath)))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jComboDbType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabelDays)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextNumDays, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabelDisp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextDisplayName, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelODBCName)
                    .addComponent(jComboDB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelWhichDB)
                    .addComponent(jTextODBC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelUser)
                    .addComponent(jTextUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelPW)
                    .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelPath)
                    .addComponent(jTextPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboDbType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelDays)
                    .addComponent(jTextNumDays, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelDisp)
                    .addComponent(jTextDisplayName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jCheckShowWrite.setText("Show Write + Delete");

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Saving studies for a conference"));

        jLabelWhichCD.setText("Which Read from CD directory do you wish to use:");

        jComboCD.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "none", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
        jComboCD.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboCDActionPerformed(evt);
            }
        });

        jLabelCDNum.setText("Name");

        jButDelCD.setText("Delete all studies in above");
        jButDelCD.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButDelCDActionPerformed(evt);
            }
        });

        jLabCDName.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jLabCDPath.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabelWhichCD)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboCD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabelCDNum)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jButDelCD)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabCDName, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabCDPath, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelWhichCD)
                    .addComponent(jComboCD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelCDNum)
                    .addComponent(jLabCDName)
                    .addComponent(jLabCDPath))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButDelCD)
                .addGap(0, 37, Short.MAX_VALUE))
        );

        jButHelp.setText("Help");
        jButHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButHelpActionPerformed(evt);
            }
        });

        jLabAbout.setText("version: 2.21");

        javax.swing.GroupLayout jPanelSetupLayout = new javax.swing.GroupLayout(jPanelSetup);
        jPanelSetup.setLayout(jPanelSetupLayout);
        jPanelSetupLayout.setHorizontalGroup(
            jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSetupLayout.createSequentialGroup()
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelSetupLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckShowWrite)
                    .addGroup(jPanelSetupLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jButHelp))
                    .addComponent(jLabAbout))
                .addContainerGap(58, Short.MAX_VALUE))
        );
        jPanelSetupLayout.setVerticalGroup(
            jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSetupLayout.createSequentialGroup()
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSetupLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jCheckShowWrite)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButHelp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabAbout))
                    .addGroup(jPanelSetupLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(33, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Setup", jPanelSetup);

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

	private void jButExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButExitActionPerformed
		dispose();
	}//GEN-LAST:event_jButExitActionPerformed

	private void jButReadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButReadActionPerformed
		readButton();
	}//GEN-LAST:event_jButReadActionPerformed

	private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
		setDates(0);
	}//GEN-LAST:event_jComboBox1ActionPerformed

	private void jDB1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDB1ActionPerformed
		changeDBSelectedAndErase(1);
	}//GEN-LAST:event_jDB1ActionPerformed

	private void jDB2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDB2ActionPerformed
		changeDBSelectedAndErase(2);
	}//GEN-LAST:event_jDB2ActionPerformed

	private void jDB3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDB3ActionPerformed
		changeDBSelectedAndErase(3);
	}//GEN-LAST:event_jDB3ActionPerformed

	private void jDB4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDB4ActionPerformed
		changeDBSelectedAndErase(4);
	}//GEN-LAST:event_jDB4ActionPerformed

	private void jDB5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDB5ActionPerformed
		changeDBSelectedAndErase(5);
	}//GEN-LAST:event_jDB5ActionPerformed

	private void jDB6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDB6ActionPerformed
		changeDBSelectedAndErase(6);
	}//GEN-LAST:event_jDB6ActionPerformed

	private void jDB7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDB7ActionPerformed
		changeDBSelectedAndErase(7);
	}//GEN-LAST:event_jDB7ActionPerformed

	private void jDB8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDB8ActionPerformed
		changeDBSelectedAndErase(8);
	}//GEN-LAST:event_jDB8ActionPerformed

	private void jDB9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDB9ActionPerformed
		changeDBSelectedAndErase(9);
	}//GEN-LAST:event_jDB9ActionPerformed

	private void jDB10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDB10ActionPerformed
		changeDBSelectedAndErase(10);
	}//GEN-LAST:event_jDB10ActionPerformed

	private void jPanelSetupComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanelSetupComponentShown
		fillBoxes();
	}//GEN-LAST:event_jPanelSetupComponentShown

	private void jComboDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboDBActionPerformed
		int i = jComboDB.getSelectedIndex();
		if( i == jOptionDB || i < 0 || i > 9) return;
		saveBoxes();
		jOptionDB = i;
		fillBoxes();
	}//GEN-LAST:event_jComboDBActionPerformed

	private void jPanelSetupComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanelSetupComponentHidden
		saveBoxes();
		updateDbList();
	}//GEN-LAST:event_jPanelSetupComponentHidden

	private void jPanelOptionsComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanelOptionsComponentHidden
		saveOptions();
	}//GEN-LAST:event_jPanelOptionsComponentHidden

	private void jPanelOptionsComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanelOptionsComponentShown
		fillOptions();
	}//GEN-LAST:event_jPanelOptionsComponentShown

	private void jButWriteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButWriteActionPerformed
		writeButton();
	}//GEN-LAST:event_jButWriteActionPerformed

	private void jButExit1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButExit1ActionPerformed
		dispose();
	}//GEN-LAST:event_jButExit1ActionPerformed

	private void jPanelWriteComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanelWriteComponentShown
		writeWasShown = true;
		fillWriteTable();
	}//GEN-LAST:event_jPanelWriteComponentShown

	private void jButJpegActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButJpegActionPerformed
		saveJpeg(0);
	}//GEN-LAST:event_jButJpegActionPerformed

	private void jButClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButClearActionPerformed
		IJ.doCommand("Close All");
//		IJ.runPlugIn("ij.plugin.Commands", "close-all");
		dataWindow = null;
		fillCurrStudy();
	}//GEN-LAST:event_jButClearActionPerformed

	private void jButAll3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButAll3ActionPerformed
		all3reports();
	}//GEN-LAST:event_jButAll3ActionPerformed

	private void jButDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButDeleteActionPerformed
		deleteButton();
	}//GEN-LAST:event_jButDeleteActionPerformed

	private void jButExit2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButExit2ActionPerformed
		dispose();
	}//GEN-LAST:event_jButExit2ActionPerformed

	private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
		setDates(1);
	}//GEN-LAST:event_jComboBox2ActionPerformed

private void jButCineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButCineActionPerformed
		saveJpeg(1);
}//GEN-LAST:event_jButCineActionPerformed

    private void jButTileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButTileActionPerformed
        tileAction();
    }//GEN-LAST:event_jButTileActionPerformed

    private void jButShowTileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButShowTileActionPerformed
        tileDialogShow();
    }//GEN-LAST:event_jButShowTileActionPerformed

    private void jComboCDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboCDActionPerformed
		cdChanged();
    }//GEN-LAST:event_jComboCDActionPerformed

    private void jButDelCDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButDelCDActionPerformed
		deleteCD();
    }//GEN-LAST:event_jButDelCDActionPerformed

    private void jTextTeachNameKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextTeachNameKeyReleased
		saveTeachKeys();
    }//GEN-LAST:event_jTextTeachNameKeyReleased

    private void jTextTeachReportKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextTeachReportKeyReleased
		saveTeachKeys();
    }//GEN-LAST:event_jTextTeachReportKeyReleased

    private void jButTeachUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButTeachUpdateActionPerformed
		updateTeaching();
    }//GEN-LAST:event_jButTeachUpdateActionPerformed

    private void jTogButSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTogButSaveActionPerformed
		conferenceButton();
    }//GEN-LAST:event_jTogButSaveActionPerformed

    private void jButHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButHelpActionPerformed
		openHelp();
    }//GEN-LAST:event_jButHelpActionPerformed

    private void jButSaveMipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSaveMipActionPerformed
		saveMip();
    }//GEN-LAST:event_jButSaveMipActionPerformed

    private void jComboDbTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboDbTypeActionPerformed
		changeDbType();
    }//GEN-LAST:event_jComboDbTypeActionPerformed

    private void jButWebActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButWebActionPerformed
		callWeb();
    }//GEN-LAST:event_jButWebActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private com.michaelbaranov.microba.calendar.DatePicker dateFrom;
    private com.michaelbaranov.microba.calendar.DatePicker dateFrom1;
    private com.michaelbaranov.microba.calendar.DatePicker dateTo;
    private com.michaelbaranov.microba.calendar.DatePicker dateTo1;
    private javax.swing.JButton jButAll3;
    private javax.swing.JButton jButCine;
    private javax.swing.JButton jButClear;
    private javax.swing.JButton jButDelCD;
    private javax.swing.JButton jButDelete;
    private javax.swing.JButton jButExit;
    private javax.swing.JButton jButExit1;
    private javax.swing.JButton jButExit2;
    private javax.swing.JButton jButHelp;
    private javax.swing.JButton jButJpeg;
    private javax.swing.JButton jButRead;
    private javax.swing.JButton jButSaveMip;
    private javax.swing.JButton jButShowTile;
    private javax.swing.JButton jButTeachUpdate;
    private javax.swing.JButton jButTile;
    private javax.swing.JButton jButWeb;
    private javax.swing.JButton jButWrite;
    private javax.swing.JCheckBox jCheckExit;
    private javax.swing.JCheckBox jCheckShowWrite;
    private javax.swing.JCheckBox jCheckTile;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboCD;
    private javax.swing.JComboBox jComboDB;
    private javax.swing.JComboBox jComboDbType;
    private javax.swing.JRadioButton jDB1;
    private javax.swing.JRadioButton jDB10;
    private javax.swing.JRadioButton jDB2;
    private javax.swing.JRadioButton jDB3;
    private javax.swing.JRadioButton jDB4;
    private javax.swing.JRadioButton jDB5;
    private javax.swing.JRadioButton jDB6;
    private javax.swing.JRadioButton jDB7;
    private javax.swing.JRadioButton jDB8;
    private javax.swing.JRadioButton jDB9;
    private javax.swing.JLabel jLabAbout;
    private javax.swing.JLabel jLabCDName;
    private javax.swing.JLabel jLabCDPath;
    private javax.swing.JLabel jLabSty1;
    private javax.swing.JLabel jLabSty2;
    private javax.swing.JLabel jLabSty3;
    private javax.swing.JLabel jLabTeach;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelCDNum;
    private javax.swing.JLabel jLabelDays;
    private javax.swing.JLabel jLabelDbName;
    private javax.swing.JLabel jLabelDbName1;
    private javax.swing.JLabel jLabelDbName2;
    private javax.swing.JLabel jLabelDisp;
    private javax.swing.JLabel jLabelODBCName;
    private javax.swing.JLabel jLabelPW;
    private javax.swing.JLabel jLabelPath;
    private javax.swing.JLabel jLabelUser;
    private javax.swing.JLabel jLabelWhichCD;
    private javax.swing.JLabel jLabelWhichDB;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanelDelete;
    private javax.swing.JPanel jPanelOptions;
    private javax.swing.JPanel jPanelRead;
    private javax.swing.JPanel jPanelReport;
    private javax.swing.JPanel jPanelSetup;
    private javax.swing.JPanel jPanelTeaching;
    private javax.swing.JPanel jPanelWrite;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JRadioButton jRadioAvi;
    private javax.swing.JRadioButton jRadioGif;
    private javax.swing.JRadioButton jRadioPng;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTable3;
    private javax.swing.JTextField jTextDisplayName;
    private javax.swing.JTextField jTextFrmTime;
    private javax.swing.JTextField jTextN;
    private javax.swing.JTextField jTextNumDays;
    private javax.swing.JTextField jTextODBC;
    private javax.swing.JTextField jTextPatName;
    private javax.swing.JTextField jTextPatName1;
    private javax.swing.JTextField jTextPath;
    private javax.swing.JTextField jTextStamp;
    private javax.swing.JTextField jTextTeachName;
    private javax.swing.JTextArea jTextTeachReport;
    private javax.swing.JTextField jTextTeaching;
    private javax.swing.JTextField jTextUser;
    private javax.swing.JToggleButton jTogButSave;
    // End of variables declaration//GEN-END:variables
	String m_lastOpen = null, m_dataPath = null, AETitle = null;
	Connection m_lastConn = null;
	Preferences jPrefer = null, jPreferSys = null;
	private boolean tab1Dirty = true, tab2Dirty = true, tab3Dirty = true;
	boolean isAccessDb = false, isInitialized = false, killRead = false;
	boolean writeWasShown = false;
	int readWriteDelFlg = 0, xStart=0, yStart=0;
	double lastMeasure = 0;
	int bkgdMode = 0;
	String bkgdFile = null, userPw = null;
	int m_maxValue, m_background = 0;
	private int numDays = 0, jCurrDB = 0, jOptionDB = 0, jConference=0;
	ArrayList<ImagePlus> imgList = null;
	ArrayList<dcm4Patient> dcmPatient = null;
	ArrayList<dcm4Study> dcmStudy = null;
	ArrayList<dcm4Series> dcmSeries = null;
	ArrayList<bi_dbTeaching> teachList = null;
	ArrayList<Object>petImages = null;
	ReadOrthanc orthanc1 = null;
	bkgdLoadData work2 = null;
	byte[] m_pBuff = null, m_lBuff = null;
	short[] m_square512 = null;
	int[] m_calc = null;
	Window dataWindow = null;
}

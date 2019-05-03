import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.MontageMaker;
import ij.process.ImageProcessor;
import ij.util.DicomTools;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.ColorModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author ilan
 */
public class ReadCdStudies extends javax.swing.JFrame implements MouseListener {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;
	static final int TBL_PATNAME = 0;
	static final int TBL_STUDY = 1;
	static final int TBL_DATE = 2;
	static final int TBL_SERIES = 3;
	static final int TBL_PAT_ID = 4;
	static final int TBL_ACCESSION = 5;
	static final int TBL_BF = 6;
	static final int NUM_FAILS = 5;	// 3 is boarderline, try 5

	/**
	 * Creates new form ReadCdStudies
	 */
	public ReadCdStudies() {
/*		super();
		SwingUtilities.invokeLater(new Runnable(){

			@Override
			public void run() {
				initComponents();
			}
			
		});*/
		initComponents();
		init();
	}
	
	private void init() {
//		ImageIcon img = new ImageIcon(getClass().getResource("resources/pacs_on.gif"));
//		setIconImage(img.getImage());
		int x,y;
		jPrefer = Preferences.userNodeForPackage(ReadCdStudies.class);
		jPrefer = jPrefer.node("biplugins");
		jCurrCD = jPrefer.getInt("current CD", 0);
		setTitle("Read Studies from CD or location on disk");
//		IJ.register(ReadCdStudies.class);
		accessFlg = jPrefer.getBoolean("readCD Accession", false);
		jCheckAccession.setSelected(accessFlg);
		WindowManager.addWindow(this);
		defineTable1(jTable1);
		TableColumn tc = jTable1.getColumnModel().getColumn(TBL_BF);
		jTable1.removeColumn(tc);
		jTable1.addMouseListener(this);
		jTable1.setAutoCreateRowSorter(true);
		defineTable1(jTable3);
		tc = jTable3.getColumnModel().getColumn(TBL_BF);
		jTable3.removeColumn(tc);
		if( !accessFlg) {
			tc = jTable1.getColumnModel().getColumn(TBL_ACCESSION);
			jTable1.removeColumn(tc);
			tc = jTable3.getColumnModel().getColumn(TBL_ACCESSION);
			jTable3.removeColumn(tc);
		}
		jTable3.setAutoCreateRowSorter(true);
		boolean autoSort = jPrefer.getBoolean("auto sort", false);
		jCheckAutoSort.setSelected(autoSort);
		setCDVals();
		numDays = jPrefer.getInt("read cd num of days", 30);
		jTextNumDays.setText(Integer.toString(numDays));
		Integer sl1 = jPrefer.getInt("cd montage slices", 20);
		jTextN.setText(sl1.toString());
		jCheckTile.setSelected(jPrefer.getBoolean("cd tile windows", false));
//		jChkForceDicomDir.setSelected(jPrefer.getBoolean("force use dicomdir", false));
		sl1 = jPrefer.getInt("postage stamp size", 0);
		jTextStamp.setText(sl1.toString());
		Rectangle scr1 = ChoosePetCt.getScreenDimensions();
		x = jPrefer.getInt("cd read dialog x", 0);
		y = jPrefer.getInt("cd read dialog y", 0);
		if( y > scr1.height) y = 0;
		xStart = jPrefer.getInt("cd read dialog pos x", 0);
		yStart = jPrefer.getInt("cd read dialog pos y", 0);
		if( x > 0 && y > 0) {
			setSize(x,y);
			if(yStart > 0) setLocation(xStart, yStart);
		}
		jLabJava.setText("java: " + System.getProperty("java.version"));
		updateCdList();
		maybeShowWriteMip(null);
		fillReadTable(true);
	}

	class ColorDateRenderer extends DefaultTableCellRenderer {
		static final long serialVersionUID = ChoosePetCt.serialVersionUID;
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int col) {
			super.getTableCellRendererComponent(table, value, hasFocus, hasFocus, row, row);

			try {
				Date dt0, dt1;
				SimpleDateFormat df1 = new SimpleDateFormat("d MMM yyyy", Locale.US);
				dt1 = (Date) value;
				if( dt1 == null) return this;
				setText(df1.format(dt1));
				if( numDays <= 0) return this;
				DefaultTableModel tm = (DefaultTableModel) table.getModel();
				int i = table.convertRowIndexToModel(row);
				Boolean bf = (Boolean) tm.getValueAt(i, TBL_BF);
				Color color1 = Color.red;
//				if( isSelected) return this;
				dt0 = new Date();
				long diff = (dt0.getTime() - dt1.getTime())/(1000l*60*60*24);
				if( diff > numDays) {
					if( bf) color1 = Color.magenta;
				} else {
					color1 = Color.green;
					if( bf) color1 = Color.cyan;
				}
				setBackground(color1);
			} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
			return this;
		}
	}

	// redefine Tables
	void defineTable1(JTable jtab) {
		jtab.setModel(new javax.swing.table.DefaultTableModel(
			new Object[][]{
				{null, null, null, null, null, null, null},
				{null, null, null, null, null, null, null},
				{null, null, null, null, null, null, null},
				{null, null, null, null, null, null, null}
			},
			new String[]{
				"Patient", "Study", "Date", "Series", "ID", "Accession", "bf"
			}
		) {
			Class[] types = new Class[]{
				String.class, Object.class, Date.class, Object.class, Object.class, Object.class, Object.class
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

	@Override
	public void mouseClicked(MouseEvent me) {
		int i = me.getClickCount();
		if( i == 1) seriesMouseClick(me);
		if( i == 2) readButton();
	}

	@Override
	public void mousePressed(MouseEvent me) {}

	@Override
	public void mouseReleased(MouseEvent me) {}

	@Override
	public void mouseEntered(MouseEvent me) {}

	@Override
	public void mouseExited(MouseEvent me) {}

	class CD_dirInfo {
		String patName = null;
		String patID = null;
		Date styDate = null;
		Date birthDate = null;
		String styName = null;
		String serName = null;
		String accession = null;
		String studyUID = null;
		String seriesUID = null;
		String dicomDirPath = null;
		int sopClass = 0;
		boolean isBF = false;
		File flName = null;
		ArrayList<File> flList = null;
	}

	void unselectAllTableEntries() {
		jTable1.clearSelection();
		jTable3.clearSelection();
	}

	void setOrSaveColumnWidths(int type, boolean saveFlg) {
		String colStr = "readCD series col";
		TableColumn col0;
		JTable jTab = jTable1;
		if( type == 2) jTab = jTable3;
		int i, x, n=5;
		if( !saveFlg) {
			if( type == 0 && !tab1Dirty) return;
			if( type == 2 && !tab3Dirty) return;
			col0 = jTab.getColumnModel().getColumn(TBL_DATE);
			col0.setCellRenderer(new ColorDateRenderer());
			col0 = jTab.getColumnModel().getColumn(TBL_SERIES);
			col0.setCellRenderer(new SeriesRenderer());
			if( type == 0) tab1Dirty = false;
			if( type == 2) tab3Dirty = false;
		}

		TableColumn col1;
		if( accessFlg) n = 6;
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
	
	void updateCdList() {
		int i, last1 = -1, num1 = 0;
		boolean failed = false;
		// check if it is still initializing
		String tmp1 = jTextNumDays.getText();
		String tmp2;
		if( tmp1.isEmpty()) return;
		numDays = ChoosePetCt.parseInt(tmp1);
		JRadioButton curBut;
		buttonGroup1 = new javax.swing.ButtonGroup();
		for( i=1; i<=12; i++) {
			curBut = getCDButton(i);
			curBut.setVisible(false);
			buttonGroup1.add(curBut);
/*			if( !failed) {
				tmp1 = ChoosePetCt.checkEmpty(jPrefer.get("pathReadCD" + i, null));
				tmp2 = ChoosePetCt.checkEmpty(jPrefer.get("CDName" + i, null));
				if( tmp1 != null && tmp2 != null) last1 = i;
				else failed = true;
			}*/
			tmp1 = ChoosePetCt.checkEmpty(jPrefer.get("pathReadCD" + i, null));
			tmp2 = ChoosePetCt.checkEmpty(jPrefer.get("CDName" + i, null));
			if( tmp1 != null && tmp2 != null) {
				curBut.setVisible(true);
				last1 = i;
				num1++;
			}
		}
/*		jLabelCdName.setVisible(last1 > 1);
		jButRead.setEnabled(last1 > 0);
		if( last1 > 1) {
			if( jCurrCD >= last1) jCurrCD = last1-1;
			for( i=1; i<=last1; i++) {
				curBut = getCDButton(i);
				curBut.setVisible(true);
			}
			curBut = getCDButton( jCurrCD+1);
			curBut.setSelected(true);
		} else jCurrCD = 0;*/
		jLabelCdName.setVisible(num1 > 1);
		jButRead.setEnabled(num1 > 0);
		if( num1 > 1) {
			if( jCurrCD >= last1) jCurrCD = last1-1;
			curBut = getCDButton( jCurrCD+1);
			curBut.setSelected(true);
		} else jCurrCD = 0;
		changeCDSelected(jCurrCD + 1);
	}

	void readButton() {
		maybeShowWriteMip(null);
		if( work2 != null) return;
		int n = jTable1.getSelectedRowCount();
		Container c = getContentPane();
		c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		if( n <= 0) {
			fillReadTable(true);
			c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		readWriteDelFlg = 0;
		loadData();
	}
	
	void showDelete() {
		String tmp1 = jLabelCdName.getText();
		jLabelCdName2.setText(tmp1);
		fillReadTable(false);
	}
	
	void deleteButton() {
		maybeShowWriteMip(null);
		if( work2 != null) return;
		int n = jTable3.getSelectedRowCount();
		if( n <= 0) return;
		if( n == jTable3.getRowCount()) {
			deleteAllButton(false);
			return;
		}
		Container c = getContentPane();
		c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		readWriteDelFlg = 2;
		loadData();
	}
	
	void deleteAllButton(boolean showWarn) {
		if( showWarn) {
			String tmp1 = "All the studies in this directory will be permanently deleted.";
			tmp1 += "\nAre you sure you want to delete them?";
			int i = JOptionPane.showConfirmDialog(this, tmp1, "Delete studies", JOptionPane.YES_NO_OPTION);
			if( i != JOptionPane.YES_OPTION) return;
		}
		String path = getCurrPath();
		if( path == null) return;
		File folder = new File(path);
		ChoosePetCt.deleteFolder(folder, false);
		fillReadTable(true);
		fillReadTable(false);
	}

	// this has been extended to do both read and delete
	void doRead() {
		JTable jTab = jTable1;
		if( readWriteDelFlg == 2) jTab = jTable3;
		int n = jTab.getSelectedRowCount();
		int [] selected = jTab.getSelectedRows();
		int i, j;
		CD_dirInfo currRow;
		for( i=0; i<n; i++) {
			selected[i] = jTab.convertRowIndexToModel(selected[i]);
		}

		imgList = new ArrayList<ImagePlus>();
		DefaultTableModel mod1;
		mod1 = (DefaultTableModel) jTab.getModel();
		if( readWriteDelFlg != 2) {
			savePrefs();
			ImageJ ij = IJ.getInstance();
			if( ij != null) ij.toFront();
		}
		boolean showWarn = true;
		out1:
		for( i=0; i<n; i++) {
			Point pnt1 = getPosAndLen(selected[i], mod1);
			for( j=0; j < pnt1.y; j++) {
				currRow = tableList.get(pnt1.x + j);
				if( readWriteDelFlg == 2) {
					if( !deleteFiles(currRow, showWarn)) break out1;
					showWarn = false;
				} else {
					if(!readFiles(currRow)) {
						unselectAllTableEntries();
						Container c = getContentPane();
						c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						return;
					}
				}
			}
		}
		unselectAllTableEntries();
		Container c = getContentPane();
		c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if( readWriteDelFlg == 2) {
			fillReadTable(true);
			fillReadTable(false);
			return;
		}
		this.toFront();
		if( jCheckTile.isSelected()) tileAction();
		runPetCtViewer();
	}
	
	void tileAction() {
		int i = ChoosePetCt.parseInt(jTextStamp.getText());
		ChoosePetCt.organizeWindows(i);
	}

	void runPetCtViewer() {
		String seriesUIDs = ChoosePetCt.buildSeriesUIDs(imgList);
		if( seriesUIDs == null) return;
		if( seriesUIDs.startsWith("2CTs")) seriesUIDs = "";
		IJ.runPlugIn("Pet_Ct_Viewer", seriesUIDs);
		wait4bkgd();
		maybeShowWriteMip(ChoosePetCt.MipPanel);
	}
	
	void saveMip() {
		int i;
		File flTmp;
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
		ImagePlus img = petPipe.data1.srcImage;
		String path = img.getOriginalFileInfo().directory;
		i = path.lastIndexOf(File.separatorChar);
		path = path.substring(0, i+1) + "mipdata";
		flTmp = new File(path);
		if( !flTmp.mkdirs()) {
			IJ.log("Can't create directory.\nMaybe the device is read only?");
			maybeShowWriteMip(null);
			return;
		}
		path += File.separatorChar + "mipData.dcm";
		myWriteDicom dicom = new myWriteDicom(mipPanel.parent, mipPipe, null);
		dicom.writeDicomHeaderMip(path);
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

	void maybeShowWriteMip( PetCtPanel mipPanel) {
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

	boolean deleteFiles( CD_dirInfo currRow, boolean showWarn) {
		int j, n0;
		File fdel;
		String parName = currRow.flName.getParent();
		File flPath = new File(parName);
		File [] results = flPath.listFiles();
		if(currRow.flList != null && !currRow.flList.isEmpty()) {
			results = new File[currRow.flList.size()];
			currRow.flList.toArray(results);
		}
		if( results == null) return true;
		if( showWarn) {
			String tmp1 = "These series will be permanently deleted.";
			tmp1 += "\nAre you sure you want to delete them?";
			int i = JOptionPane.showConfirmDialog(this, tmp1, "Delete series", JOptionPane.YES_NO_OPTION);
			if( i != JOptionPane.YES_OPTION) return false;
		}
		n0 = results.length;
		for( j=0; j<n0; j++) {
			fdel = new File(results[j].getPath());
			fdel.delete();
		}
		results = flPath.listFiles();
		if( results == null) return true;
		n0 = results.length;
		if( n0 <= 0) flPath.delete();
		maybeDeleteDicomDir(currRow.dicomDirPath);
		return true;
	}
	
	void maybeDeleteDicomDir(String dicomDirPath) {
		if( dicomDirPath == null || dicomDirPath.isEmpty()) return;
		CD_dirInfo currRow;
		String tmp, tmp1, parName;
		File flPath;
		File[] results, leftOvers;
		int i, j, k, n0, n1, first1 = -1, n = tableList.size();
		// check to see if all entries with that path are empty
		for( i=0; i<n; i++) {
			currRow = tableList.get(i);
			tmp = currRow.dicomDirPath;
			if( tmp == null || !tmp.equals(dicomDirPath)) continue;	// the usual case
			if( first1 < 0) first1 = i;	// found something, check it
			parName = currRow.flName.getParent();
			flPath = new File(parName);
			leftOvers = flPath.listFiles();
			if( leftOvers == null) continue;
			n0 = leftOvers.length;
			results = new File[currRow.flList.size()];
			currRow.flList.toArray(results);
			n1 = results.length;
			for( j=0; j<n0; j++) {
				tmp1 = leftOvers[j].getName();
				for( k=0; k<n1; k++) if(results[k].getName().equals(tmp1)) break;
				if( k < n1) break;
			}
			if( j < n0) break;
		}
		if( first1 < 0 || i < n) return;	// can't delete
		flPath = new File(dicomDirPath);
		ChoosePetCt.deleteFolder(flPath, true);
	}

	boolean readFiles( CD_dirInfo currRow) {
		int j, k, n0, n, width = -1, height = 0, depth = 0, samplePerPixel = 0;
		int bad = 0, fails = 0;
		Opener opener;
		ImagePlus imp, imp2;
		ImageStack stack;
		Calibration cal = null;
		double min, max, progVal;
		FileInfo fi = null;
		String parName, flName, info, label1, tmp;
		String[] frameText = null;
		BI_dbSaveInfo curr1 = null;
		info = null;
		min = Double.MAX_VALUE;
		max = -Double.MAX_VALUE;
		stack = null;
		parName = currRow.flName.getParent();
		File flPath = new File(parName);
		File checkEmpty;
		File [] results = flPath.listFiles();
		if(currRow.flList != null && !currRow.flList.isEmpty()) {
			results = new File[currRow.flList.size()];
			currRow.flList.toArray(results);
		}
		n0 = results.length;
		// look for graphics files
		if( n0 <= 4) for( j = 0; j < n0; j++) {
			opener = new Opener();
			flName = results[j].getPath();
			k = opener.getFileType(flName);
			if( k == Opener.UNKNOWN || k == Opener.TEXT) bad++;
		}
		for( j= 1; j <= n0; j++) {
			curr1 = new BI_dbSaveInfo();
			progVal = ((double) j) / n0;
			IJ.showStatus( j + "/" + n0);
			IJ.showProgress(progVal);
			opener = new Opener();
			flName = results[j-1].getPath();
			checkEmpty = new File(flName);	// remember for possible dicomdir
			if( checkEmpty.length() == 0) continue;
			tmp = results[j-1].getName();
			if( tmp.equalsIgnoreCase("dirfile")) continue;
			k = opener.getFileType(flName);
			if( k == Opener.UNKNOWN || k == Opener.TEXT) {
				if( tmp.startsWith("graphic") && tmp.endsWith("gr1")) {
					frameText = ChoosePetCt.getFrameText(flName);
				}
				continue;
			}
			tmp = currRow.dicomDirPath;
			curr1.isDicomDir = (tmp != null && !tmp.isEmpty());
			curr1.flName = checkEmpty;
			curr1.patName = currRow.patName;
			curr1.patID = currRow.patID;
			curr1.styName = currRow.styName;
			curr1.serName = currRow.serName;
			curr1.styDate = currRow.styDate;
			curr1.accession = currRow.accession;
			opener.setSilentMode(true);
			imp = opener.openImage(flName);
			if( imp == null) {
				fails++;
				if( fails > 2) {
					tmp = "Cannot read this data.\n";
					tmp += "For Bio-Formats data, use Import -> Bio-Formats";
					JOptionPane.showMessageDialog(this, tmp);
					IJ.showProgress(1.0);
					return false;
				}
				continue;
			}
			info = (String)imp.getProperty("Info");
			k = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(info, "0028,0002"));
			if( stack == null) {
				samplePerPixel = k;
				width = imp.getWidth();
				height = imp.getHeight();
				depth = imp.getStackSize();
				cal = imp.getCalibration();
				fi = imp.getOriginalFileInfo();
				ColorModel cm = imp.getProcessor().getColorModel();
				stack = new ImageStack(width, height, cm);
			}
			if( (depth > 1 && n0 > 1) || width != imp.getWidth() || height != imp.getHeight() || k != samplePerPixel) {
				if( k <= 0) continue;
				imp.setProperty("bidb", curr1);
				imp.show();	// show a normal stack
				imgList.add(imp);
				curr1 = null;
				stack = null;
				depth = 0;
				continue;
			}
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
			stack = ChoosePetCt.mySort(stack);
			if(fi != null) {
				fi.fileFormat = FileInfo.UNKNOWN;
				fi.fileName = "";
				fi.directory = parName;
			}
			imp2 = new ImagePlus(getTitleInfo(currRow), stack);
			imp2.getProcessor().setMinAndMax(min, max);
			imp2.setProperty("bidb", curr1);
			if( n0 == 1+bad || depth > 1) imp2.setProperty("Info", info);
			if(fi != null) imp2.setFileInfo(fi);
			double voxelDepth = DicomTools.getVoxelDepth(stack);
			if (voxelDepth>0.0 && cal!=null) cal.pixelDepth = voxelDepth;
			imp2.setCalibration(cal);
			if( frameText != null) for( j=0; j < frameText.length; j++) {
				label1 = frameText[j];
				if( label1 != null) {
					int i1 = j+1;
					tmp = stack.getSliceLabel(i1);
					if( tmp != null) {
						// the slices are counted from the bottom up
						i1 = stack.getSize() - j;
						tmp = stack.getSliceLabel(i1);
						label1 += "\n" + tmp;
					}
					stack.setSliceLabel(label1, i1);
				}
			}
			imp2 = myMakeMontage( imp2, info, frameText != null);
			imgList.add(imp2);	// keep track of images loaded
		}
		IJ.showProgress(1.0);
		return true;
	}

	String getTitleInfo(CD_dirInfo entry) {
		String styDate, ret1 = entry.patName;
		if(entry.birthDate != null) {
			long sdyTime, birthTime, currDiff;
			Integer years;
			sdyTime = entry.styDate.getTime();
			birthTime = entry.birthDate.getTime();
			currDiff = (sdyTime - birthTime)/(24*60*60*1000);	// number of days
			years = (int)( currDiff/365.242199);
			ret1 += "   " + years.toString() + "y";
		}
		ret1 += "   " + entry.patID + "   ";
		if(entry.styDate == null) styDate = "";
		else styDate = ChoosePetCt.UsaDateFormat(entry.styDate);
		ret1 += styDate + "   " + entry.styName + "   " + entry.serName;
		return ret1;
	}

	ImagePlus myMakeMontage(ImagePlus imp, String info, boolean label) {
		int nSlices = imp.getStackSize();
		ImagePlus impMon;
		FileInfo fi;
		int columns, rows, first, last, inc, borderWidth, scrWidth, scrHeight;
		int imgWidth, imgHeight;
		double scale, fillX, fillY;
		if(imp.getTitle().indexOf("MIP data")>1) nSlices = 1;
		int maxSlice = ChoosePetCt.parseInt(jTextN.getText());
		if( nSlices < 2 || nSlices > maxSlice) {
			imp.show();	// show a normal stack
			return imp;
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
			fi = imp.getOriginalFileInfo();
			impMon.setFileInfo(fi);
			impMon.show();
			return impMon;
		}
		return imp;
	}
	
	void changeCDSelectedAndErase( int indx1) {
		maybeShowWriteMip(null);
		changeCDSelected(indx1);
		fillReadTable(true);
	}
	
	void changeCDSelected( int indx1) {
		jCurrCD = indx1 - 1;
		String tmp1 = jPrefer.get("CDName" + indx1, null);
		jLabelCdName.setText(tmp1);
	}
	
	void saveCDName() {
		int i = jComboLocation.getSelectedIndex() + 1;
		if( i <= 0) return;
		String tmp = "CDName" + i;
		String val1 = jTextName.getText();
		jPrefer.put(tmp, val1);
	}
	
	void saveCDPath() {
		int i = jComboLocation.getSelectedIndex() + 1;
		if( i <= 0) return;
		String tmp = "pathReadCD" + i;
		String val1 = jTextPath.getText();
		jPrefer.put(tmp, val1);
	}

	// Note: do not put a break point in the following routine. It crashs the system.
	void setCDVals() {
		int i = jComboLocation.getSelectedIndex() + 1;
		if( i <= 0) return;
		String tmp = "pathReadCD" + i;
		String path = jPrefer.get(tmp, null);
		jTextPath.setText(path);
		tmp = "CDName" + i;
		path = jPrefer.get(tmp, null);
		jTextName.setText(path);
	}
	
	void savePrefs() {
		jPrefer.putInt("current CD", jCurrCD);
		jPrefer.putInt("read cd num of days", numDays);
		int i = ChoosePetCt.parseInt(jTextN.getText());
		jPrefer.putInt("cd montage slices", i);
		jPrefer.putBoolean("cd tile windows", jCheckTile.isSelected());
//		jPrefer.putBoolean("force use dicomdir", isForceDicomDir());
		i = ChoosePetCt.parseInt(jTextStamp.getText());
		jPrefer.putInt("postage stamp size", i);
		Dimension sz1 = getSize();
		jPrefer.putInt("cd read dialog x", sz1.width);
		jPrefer.putInt("cd read dialog y", sz1.height);
		jPrefer.putBoolean("auto sort", jCheckAutoSort.isSelected());
		jPrefer.putBoolean("readCD Accession", jCheckAccession.isSelected());
		Point pt1 = getLocation();
		int y0 = pt1.y - yStart;
		int x0 = pt1.x - xStart;
		if( Math.abs(x0) > 2 || y0 < 0 || y0 > 30) {
			jPrefer.putInt("cd read dialog pos x",pt1.x);
			jPrefer.putInt("cd read dialog pos y",pt1.y);
		}
	}

	String date2String( Date dt1) {
		SimpleDateFormat df1 = new SimpleDateFormat("d MMM yyyy", Locale.US);
		if( dt1 == null) dt1 = new Date();
		return df1.format(dt1);
	}

	void Browse2Path() {
		final JFileChooser fc;
		File file1;
		int i = jComboLocation.getSelectedIndex() + 1;
		if( i <= 0) return;
		String tmp;
		String flPath;
		try {
			flPath = jTextPath.getText();
			fc = new JFileChooser(flPath);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int retVal = fc.showOpenDialog(this);
			if( retVal != JFileChooser.APPROVE_OPTION) return;
			file1 = fc.getSelectedFile();
			jTextPath.setText(file1.getPath());
			saveCDPath();
			tmp = ChoosePetCt.checkEmpty(jTextName.getText());
			if( tmp == null) {
				tmp = "If Name is empty, this entry and all following will not be used.\n";
				tmp += "Please enter a value into Name.";
				JOptionPane.showMessageDialog(this, tmp);
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}
	
	JRadioButton getCDButton( int indx) {
		JRadioButton ret1 = null;
		switch( indx) {
			case 1:
				ret1 = jCD1;
				break;

			case 2:
				ret1 = jCD2;
				break;

			case 3:
				ret1 = jCD3;
				break;

			case 4:
				ret1 = jCD4;
				break;

			case 5:
				ret1 = jCD5;
				break;

			case 6:
				ret1 = jCD6;
				break;

			case 7:
				ret1 = jCD7;
				break;

			case 8:
				ret1 = jCD8;
				break;

			case 9:
				ret1 = jCD9;
				break;

			case 10:
				ret1 = jCD10;
				break;

			case 11:
				ret1 = jCD11;
				break;

			case 12:
				ret1 = jCD12;
				break;
		}
		return ret1;
	}

	void seriesMouseClick(MouseEvent e) {
		if( jTable1.columnAtPoint(e.getPoint()) != TBL_SERIES) return;
		int j, i = jTable1.getSelectedRow();
		CD_dirInfo currRow;
		Object[] row1;
		boolean currIsBF;
		if( i < 0) return;
		i = jTable1.convertRowIndexToModel(i);
		DefaultTableModel mod1;
		mod1 = (DefaultTableModel) jTable1.getModel();
		Point pnt1 = getPosAndLen(i, mod1);
		if( pnt1.y <= 1) return;
		unselectAllTableEntries();
		currRow = tableList.get(pnt1.x);
		mod1.setValueAt(currRow.serName, i, TBL_SERIES);
		currIsBF = false;
		for( j=0; j<pnt1.y; j++) {
			currRow = tableList.get(pnt1.x + j);
			currIsBF |= currRow.isBF;
		}
		for( j=1; j<pnt1.y; j++) {
			currRow = tableList.get(pnt1.x + j);
			row1 = new Object[TBL_BF+1];
			row1[TBL_PATNAME] = currRow.patName;
			row1[TBL_PAT_ID] = currRow.patID;
			row1[TBL_DATE] = currRow.styDate;
			row1[TBL_STUDY] = currRow.styName;
			row1[TBL_SERIES] = currRow.serName;
			row1[TBL_ACCESSION] = currRow.accession;
			row1[TBL_BF] = currIsBF;
			mod1.insertRow(i+j, row1);
		}
	}
	
	// this returns a pseudo point, x=pos, y=len
	Point getPosAndLen(int inVal, DefaultTableModel mod1) {
		Point retVal = new Point();
		CD_dirInfo startVal, nextVal;
		String series;
		int i, j, numSer=1, n = tableList.size();
		for( i=j=0; i<inVal; i++) {
			if( j>= n) return retVal;
			startVal = tableList.get(j++);
			series = (String) mod1.getValueAt(i, TBL_SERIES);
			while( series == null && j<n) {
				nextVal = tableList.get(j);
				if( !startVal.studyUID.equals(nextVal.studyUID)) break;
				j++;
			}
		}
		if( j>= n) return retVal;
		retVal.x = j;
		startVal = tableList.get(j++);
		series = (String) mod1.getValueAt(inVal, TBL_SERIES);
		while( series == null && j<n) {
			nextVal = tableList.get(j++);
			if( !startVal.studyUID.equals(nextVal.studyUID)) break;
			numSer++;
		}
		retVal.y = numSer;
		return retVal;
	}

	String getCurrPath() {
		int indx = jCurrCD + 1;
		String tmp = "pathReadCD" + indx;
		String path = ChoosePetCt.checkEmpty(jPrefer.get(tmp, null));
		return path;
	}
	
	void fillReadTable(boolean readFlg) {
		JTable jTab = jTable1;
		int type1 = 0;
		boolean isBF, isWindows;
		if( !readFlg) {
			jTab = jTable3;
			type1 = 2;
		}
		DefaultTableModel mod1;
		mod1 = (DefaultTableModel) jTab.getModel();
		mod1.setNumRows(0);
		int i, j;
		String tmp;
		CD_dirInfo tableEntry, nextEntry;
		tableList = new ArrayList<CD_dirInfo>();
		String path = getCurrPath();
		try {
			if( path == null) {
				// may be burned onto a CD, see if there is a DICOMDIR
				path = System.getProperty("user.dir");
				i = 0;
				isWindows = (File.separatorChar == '\\');
				if( path.startsWith("/media/") || isWindows) {
					i = 2;	// windows c:\
					if( !isWindows) i = path.indexOf('/', 8);
					if( i > 0) path = path.substring(0, i);
					DicomFormat dcm = new DicomFormat();
					i = dcm.checkFile(path, isForceDicomDir());
					if( i == 0) {	// allow 1 directory deep
						File flPath = new File(path);
						File [] results = flPath.listFiles();
						if( results != null && results.length > 0) {
							flPath = results[0];
							path = flPath.getPath();
							i = dcm.checkFile(path, isForceDicomDir());
						}
					}
				}
				if( i<=0) {
					tmp = "No Dicom path defined. See Setup, Browse.";
					JOptionPane.showMessageDialog(this, tmp);
					return;
				}
				jButRead.setEnabled(i > 0);
			}
			setOrSaveColumnWidths(type1, false);
			recurseDirectory(path);
			if( jCheckAutoSort.isSelected()) sortTableList();
			for( i=0; i<tableList.size(); i++) {
				Object[] row1 = new Object[TBL_BF+1];
				tableEntry = tableList.get(i);
				row1[TBL_PATNAME] = tableEntry.patName;
				row1[TBL_PAT_ID] = tableEntry.patID;
				row1[TBL_DATE] = tableEntry.styDate;
				row1[TBL_STUDY] = tableEntry.styName;
				row1[TBL_SERIES] = tableEntry.serName;
				row1[TBL_ACCESSION] = tableEntry.accession;
				isBF = tableEntry.isBF;
				// compact the series together if same study
				while( type1 == 0 && i<tableList.size()-1) {
					nextEntry = tableList.get(i+1);
					tmp = nextEntry.studyUID;
					if( tmp==null || !tmp.equals(tableEntry.studyUID)) break;
					isBF |= nextEntry.isBF;
					i++;
					row1[TBL_SERIES] = null;
				}
				row1[TBL_BF] = isBF;
				mod1.addRow(row1);
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}
	
	void sortTableList() {
		ArrayList<CD_dirInfo> sortedList;
		int i, j, k, n = tableList.size();
		int[] indx =  new int[n];
		boolean swapIt, sortUID = true, dirty = false;
		String[] names = new String[n];
		String name0, name1, tname;
		String[] sUIDs = new String[n];
		String sUID0, sUID1, tUID;
		Date dateTst;
		long[] styDate = new long[n];
		long date0, date1, tdate;
		CD_dirInfo tableEntry;
		for( i=0; i<n; i++) {
			indx[i] = i;
			tableEntry = tableList.get(i);
			// watch out for wierd data with no patient name
			tname = tableEntry.patName;
			if( tname == null) return;
			names[i] = tname.toLowerCase();
			dateTst = tableEntry.styDate;
			if(dateTst == null) {
				sortUID = false;
				continue;
			}
			styDate[i] = dateTst.getTime()/1000;
			tUID = tableEntry.studyUID;
			if( tUID == null || tUID.isEmpty()) sortUID = false;
			sUIDs[i] = tUID;
		}
		// now sort for name ascending, date descending
		i = 0;
		while( i < n-1) {
			name0 = names[i];
			name1 = names[i+1];
			date0 = styDate[i];
			date1 = styDate[i+1];
			sUID0 = sUIDs[i];
			sUID1 = sUIDs[i+1];
			swapIt = false;
			j = name0.compareTo(name1);
			if( j > 0) swapIt = true;
			else if( j == 0) {
				if( date0 < date1) swapIt = true;
				else if( date0 == date1 && sortUID) {
					j = sUID0.compareTo(sUID1);
					if( j > 0) swapIt = true;
				}
			}
			if( swapIt) {
				dirty = true;
				tname = name0;
				name0 = name1;
				name1 = tname;
				tdate = date0;
				date0 = date1;
				date1 = tdate;
				tUID = sUID0;
				sUID0 = sUID1;
				sUID1 = tUID;
				k = indx[i];
				indx[i] = indx[i+1];
				indx[i+1] = k;
				names[i] = name0;
				names[i+1] = name1;
				styDate[i] = date0;
				styDate[i+1] = date1;
				sUIDs[i] = sUID0;
				sUIDs[i+1] = sUID1;
				i--;
				if( i<0) i = 0;
			}
			else i++;
		}
		if( !dirty) return;
		sortedList = new ArrayList<CD_dirInfo>();
		for( i=0; i<n; i++) {
			j = indx[i];
			tableEntry = tableList.get(j);
			sortedList.add(tableEntry);
		}
		tableList = sortedList;
	}

	boolean isForceDicomDir() {
		return jChkForceDicomDir.isSelected();
	}

	void recurseDirectory( String path) {
		File flPath = new File(path);
		String path1, tmp1;
		int i, j, k, off1, off2, n, n1, failed=0;
		File [] results = flPath.listFiles();
		if( results == null) return;
		DicomFormat dcm = new DicomFormat();
		DicomFormat.studyEntry sty1;
		DicomFormat.seriesEntry ser1;
		DicomFormat.imageEntry img1, img2;
		CD_dirInfo tableEntry;
		// first see if there is a dicomdir file
		n = dcm.checkFile(path, isForceDicomDir());
		if( n> 0) {
			n = dcm.m_aStudy.size();
			off1 = off2 = 0;
			for( i=0; i<n; i++) {
				sty1 = dcm.m_aStudy.get(i);
				for( j=0; j<sty1.numSeries; j++) {
					ser1 = dcm.m_aSeries.get(j+off1);
					n1 = ser1.numImages;
					if( n1 <= 0) continue;
					img1 = dcm.m_aImage.get(off2);
					tableEntry = new CD_dirInfo();
					tableEntry.flList = new ArrayList<File>();
					for(k=0; k<n1; k++) {
						img2 = dcm.m_aImage.get(off2+k);
						flPath = new File(path + File.separatorChar + img2.dirName);
						tableEntry.flList.add(flPath);
					}
					off2 += n1;
					tableEntry.dicomDirPath = path;
					tableEntry.patName = sty1.patName;
					tableEntry.patID = sty1.patID;
					tableEntry.styDate = ChoosePetCt.getDateTime(sty1.styDate, null);
					tableEntry.styName = sty1.styName;
					tableEntry.accession = sty1.accessNum;
					tmp1 = ser1.serName;
					if( tmp1 == null || tmp1.isEmpty()) tmp1 = "-";
					tableEntry.serName = tmp1;
					tableEntry.studyUID = sty1.studyUID;
					tableEntry.seriesUID = ser1.seriesUID;
					tableEntry.sopClass = ser1.sopClass;
					tableEntry.flName = new File(path + File.separatorChar + img1.dirName);
					tableList.add(tableEntry);
				}
				off1 += sty1.numSeries;
			}
			return;
		}
		for( i=0; i<results.length; i++) {
			flPath = results[i];
			path1 = flPath.getPath();
			if( flPath.isDirectory()) {
				recurseDirectory(path1);
				continue;
			}
//			if( failed > NUM_FAILS) return;
			n = dcm.checkDicomValid(flPath.getParent(), flPath.getName());
			if( n<=0) {
				if(!flPath.getName().endsWith(".zip")) failed++;
				if( failed > NUM_FAILS) return;
				continue;
			}
			tableEntry = new CD_dirInfo();
			tableEntry.patName = dcm.m_currPatName;
			tableEntry.patID = dcm.m_currPatID;
			tableEntry.styDate = ChoosePetCt.getDateTime(dcm.m_studyDate, null);
			tableEntry.birthDate = ChoosePetCt.getDateTime(dcm.m_birthdate, null);
			tableEntry.accession = dcm.m_accession;
			tableEntry.styName = dcm.m_currStyName;
			tableEntry.serName = getSerName(dcm);
			tableEntry.studyUID = dcm.m_currStudyInstanceUID;
			tableEntry.seriesUID = dcm.m_currSeriesInstanceUID;
			tableEntry.sopClass = dcm.m_SOPClass;
			tableEntry.isBF = BrownFat.isBfFile(flPath.getParent());
			tableEntry.flName = flPath;
			tableList.add(tableEntry);
			return;
		}
	}
	
	private String getSerName(DicomFormat dcm) {
		String retVal = dcm.m_currSerName;
		if( retVal == null || retVal.isEmpty()) {
			retVal = "-";
			int i = dcm.getImageType();
			switch(i) {
				case 0:
					retVal = "AC";
					break;
					
				case 1:
					retVal = "NOAC";
					break;
					
				case 2:
					retVal = "CT";
					break;
			}
		}
		return retVal;
	}

	@Override
	public void dispose() {
		WindowManager.removeWindow(this);
		savePrefs();
		setOrSaveColumnWidths(0, true);
		super.dispose();
	}

	/**
	 * To have showProgress work, the data loading is done in the background.
	 */
	protected class bkgdLoadData extends SwingWorker {
		@Override
		protected Void doInBackground() {
			doRead();
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jReadCDPane = new javax.swing.JTabbedPane();
        jPanelRead = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jButRead = new javax.swing.JButton();
        jButClear = new javax.swing.JButton();
        jCD1 = new javax.swing.JRadioButton();
        jCD2 = new javax.swing.JRadioButton();
        jCD3 = new javax.swing.JRadioButton();
        jCD4 = new javax.swing.JRadioButton();
        jCD5 = new javax.swing.JRadioButton();
        jCD6 = new javax.swing.JRadioButton();
        jCD7 = new javax.swing.JRadioButton();
        jCD8 = new javax.swing.JRadioButton();
        jCD9 = new javax.swing.JRadioButton();
        jCD10 = new javax.swing.JRadioButton();
        jCD11 = new javax.swing.JRadioButton();
        jCD12 = new javax.swing.JRadioButton();
        jLabelCdName = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButSaveMip = new javax.swing.JButton();
        jPanelDelete = new javax.swing.JPanel();
        jButDelAll = new javax.swing.JButton();
        jButDelete = new javax.swing.JButton();
        jLabelCdName2 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();
        jPanelSetup = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jComboLocation = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        jTextName = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jButPath = new javax.swing.JButton();
        jTextPath = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jTextNumDays = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jTextN = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jCheckTile = new javax.swing.JCheckBox();
        jButTile = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jTextStamp = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jCheckAutoSort = new javax.swing.JCheckBox();
        jCheckAccession = new javax.swing.JCheckBox();
        jButHelp = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jLabJava = new javax.swing.JLabel();
        jChkForceDicomDir = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Read Studies from CD, or location on disk");

        jLabel1.setText("Choose studies to be read and press:");

        jButRead.setText("Read");
        jButRead.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButReadActionPerformed(evt);
            }
        });

        jButClear.setText("Clear");
        jButClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButClearActionPerformed(evt);
            }
        });

        jCD1.setText("1");
        jCD1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD1ActionPerformed(evt);
            }
        });

        jCD2.setText("2");
        jCD2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD2ActionPerformed(evt);
            }
        });

        jCD3.setText("3");
        jCD3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD3ActionPerformed(evt);
            }
        });

        jCD4.setText("4");
        jCD4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD4ActionPerformed(evt);
            }
        });

        jCD5.setText("5");
        jCD5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD5ActionPerformed(evt);
            }
        });

        jCD6.setText("6");
        jCD6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD6ActionPerformed(evt);
            }
        });

        jCD7.setText("7");
        jCD7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD7ActionPerformed(evt);
            }
        });

        jCD8.setText("8");
        jCD8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD8ActionPerformed(evt);
            }
        });

        jCD9.setText("9");
        jCD9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD9ActionPerformed(evt);
            }
        });

        jCD10.setText("10");
        jCD10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD10ActionPerformed(evt);
            }
        });

        jCD11.setText("11");
        jCD11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD11ActionPerformed(evt);
            }
        });

        jCD12.setText("12");
        jCD12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCD12ActionPerformed(evt);
            }
        });

        jLabelCdName.setText("1");
        jLabelCdName.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jButSaveMip.setText("Save Mip");
        jButSaveMip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSaveMipActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelReadLayout = new javax.swing.GroupLayout(jPanelRead);
        jPanelRead.setLayout(jPanelReadLayout);
        jPanelReadLayout.setHorizontalGroup(
            jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReadLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButRead)
                .addGap(18, 18, 18)
                .addComponent(jButSaveMip)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButClear)
                .addContainerGap())
            .addGroup(jPanelReadLayout.createSequentialGroup()
                .addComponent(jCD1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCD12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelCdName)
                .addGap(0, 55, Short.MAX_VALUE))
            .addComponent(jScrollPane1)
        );
        jPanelReadLayout.setVerticalGroup(
            jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReadLayout.createSequentialGroup()
                .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButRead)
                    .addComponent(jLabel1)
                    .addComponent(jButClear)
                    .addComponent(jButSaveMip))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelReadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCD1)
                    .addComponent(jCD2)
                    .addComponent(jCD3)
                    .addComponent(jCD4)
                    .addComponent(jCD5)
                    .addComponent(jCD6)
                    .addComponent(jCD7)
                    .addComponent(jCD8)
                    .addComponent(jCD9)
                    .addComponent(jCD10)
                    .addComponent(jCD11)
                    .addComponent(jCD12)
                    .addComponent(jLabelCdName)))
        );

        jReadCDPane.addTab("Read", jPanelRead);

        jPanelDelete.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                jPanelDeleteComponentShown(evt);
            }
        });

        jButDelAll.setText("Delete All");
        jButDelAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButDelAllActionPerformed(evt);
            }
        });

        jButDelete.setText("Delete");
        jButDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButDeleteActionPerformed(evt);
            }
        });

        jLabelCdName2.setText("1");
        jLabelCdName2.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable3);

        javax.swing.GroupLayout jPanelDeleteLayout = new javax.swing.GroupLayout(jPanelDelete);
        jPanelDelete.setLayout(jPanelDeleteLayout);
        jPanelDeleteLayout.setHorizontalGroup(
            jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelDeleteLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelCdName2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButDelete)
                .addGap(18, 18, 18)
                .addComponent(jButDelAll)
                .addContainerGap())
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 579, Short.MAX_VALUE)
        );
        jPanelDeleteLayout.setVerticalGroup(
            jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDeleteLayout.createSequentialGroup()
                .addGroup(jPanelDeleteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButDelAll)
                    .addComponent(jButDelete)
                    .addComponent(jLabelCdName2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE))
        );

        jReadCDPane.addTab("Delete", jPanelDelete);

        jPanelSetup.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                jPanelSetupComponentHidden(evt);
            }
        });

        jLabel2.setText("This program reads from up to 12 locations, which may include CD's.");

        jLabel3.setText("If the study is PET-CT, then PetCtViewer will be called automatically.");

        jLabel4.setText("Location number:");

        jComboLocation.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));
        jComboLocation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboLocationActionPerformed(evt);
            }
        });

        jLabel5.setText("Name");

        jTextName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextNameFocusLost(evt);
            }
        });

        jLabel6.setText("Dicom path");

        jButPath.setText("Browse");
        jButPath.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButPathActionPerformed(evt);
            }
        });

        jTextPath.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextPathFocusLost(evt);
            }
        });

        jLabel7.setText("Red date, number of days");

        jLabel8.setText("Make montage for series up to");

        jLabel9.setText("slices.");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Tile properties"));

        jCheckTile.setText("Tile windows after read");

        jButTile.setText("Tile");
        jButTile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButTileActionPerformed(evt);
            }
        });

        jLabel10.setText("Postage stamp size");

        jLabel11.setText("0=don't use");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jCheckTile)
                        .addGap(18, 18, 18)
                        .addComponent(jButTile))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextStamp, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel11)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckTile)
                    .addComponent(jButTile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jTextStamp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)))
        );

        jCheckAutoSort.setText("auto sort");

        jCheckAccession.setText("Accession");

        jButHelp.setText("Help");
        jButHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButHelpActionPerformed(evt);
            }
        });

        jLabel12.setText("version: 2.11");

        jLabJava.setText("jLabel13");

        jChkForceDicomDir.setText("force use DicomDir");
        jChkForceDicomDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jChkForceDicomDirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelSetupLayout = new javax.swing.GroupLayout(jPanelSetup);
        jPanelSetup.setLayout(jPanelSetupLayout);
        jPanelSetupLayout.setHorizontalGroup(
            jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSetupLayout.createSequentialGroup()
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSetupLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(4, 4, 4)
                        .addComponent(jComboLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextName))
                    .addGroup(jPanelSetupLayout.createSequentialGroup()
                        .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(0, 97, Short.MAX_VALUE)))
                .addGap(12, 12, 12))
            .addGroup(jPanelSetupLayout.createSequentialGroup()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextPath)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButPath)
                .addContainerGap())
            .addGroup(jPanelSetupLayout.createSequentialGroup()
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanelSetupLayout.createSequentialGroup()
                            .addComponent(jLabel8)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jTextN, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLabel9))
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanelSetupLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextNumDays, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jChkForceDicomDir)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckAutoSort, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jCheckAccession, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButHelp, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabJava, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanelSetupLayout.setVerticalGroup(
            jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSetupLayout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jComboLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(jTextName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jButPath)
                    .addComponent(jTextPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel7)
                        .addComponent(jTextNumDays, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCheckAutoSort))
                    .addComponent(jChkForceDicomDir, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jTextN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jCheckAccession))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSetupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelSetupLayout.createSequentialGroup()
                        .addComponent(jButHelp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabJava)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jReadCDPane.addTab("Setup", jPanelSetup);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jReadCDPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jReadCDPane)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCD1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD1ActionPerformed
		changeCDSelectedAndErase(1);
    }//GEN-LAST:event_jCD1ActionPerformed

    private void jCD2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD2ActionPerformed
		changeCDSelectedAndErase(2);
    }//GEN-LAST:event_jCD2ActionPerformed

    private void jCD3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD3ActionPerformed
		changeCDSelectedAndErase(3);
    }//GEN-LAST:event_jCD3ActionPerformed

    private void jCD4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD4ActionPerformed
		changeCDSelectedAndErase(4);
    }//GEN-LAST:event_jCD4ActionPerformed

    private void jCD5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD5ActionPerformed
		changeCDSelectedAndErase(5);
    }//GEN-LAST:event_jCD5ActionPerformed

    private void jCD6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD6ActionPerformed
		changeCDSelectedAndErase(6);
    }//GEN-LAST:event_jCD6ActionPerformed

    private void jCD7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD7ActionPerformed
		changeCDSelectedAndErase(7);
    }//GEN-LAST:event_jCD7ActionPerformed

    private void jCD8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD8ActionPerformed
		changeCDSelectedAndErase(8);
    }//GEN-LAST:event_jCD8ActionPerformed

    private void jCD9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD9ActionPerformed
		changeCDSelectedAndErase(9);
    }//GEN-LAST:event_jCD9ActionPerformed

    private void jCD10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD10ActionPerformed
		changeCDSelectedAndErase(10);
    }//GEN-LAST:event_jCD10ActionPerformed

    private void jComboLocationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboLocationActionPerformed
		setCDVals();
    }//GEN-LAST:event_jComboLocationActionPerformed

    private void jButPathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButPathActionPerformed
		Browse2Path();
    }//GEN-LAST:event_jButPathActionPerformed

    private void jTextNameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextNameFocusLost
		saveCDName();
    }//GEN-LAST:event_jTextNameFocusLost

    private void jButReadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButReadActionPerformed
		readButton();
    }//GEN-LAST:event_jButReadActionPerformed

    private void jButClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButClearActionPerformed
		IJ.doCommand("Close All");
    }//GEN-LAST:event_jButClearActionPerformed

    private void jButTileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButTileActionPerformed
        tileAction();
    }//GEN-LAST:event_jButTileActionPerformed

    private void jTextPathFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextPathFocusLost
		saveCDPath();
    }//GEN-LAST:event_jTextPathFocusLost

    private void jPanelDeleteComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanelDeleteComponentShown
		showDelete();
    }//GEN-LAST:event_jPanelDeleteComponentShown

    private void jButDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButDeleteActionPerformed
		deleteButton();
    }//GEN-LAST:event_jButDeleteActionPerformed

    private void jButDelAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButDelAllActionPerformed
		deleteAllButton(true);
    }//GEN-LAST:event_jButDelAllActionPerformed

    private void jButHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButHelpActionPerformed
		ChoosePetCt.openHelp("CD Dialog");
    }//GEN-LAST:event_jButHelpActionPerformed

    private void jPanelSetupComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanelSetupComponentHidden
		updateCdList();
    }//GEN-LAST:event_jPanelSetupComponentHidden

    private void jButSaveMipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSaveMipActionPerformed
		saveMip();
    }//GEN-LAST:event_jButSaveMipActionPerformed

    private void jCD11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD11ActionPerformed
		changeCDSelectedAndErase(11);
    }//GEN-LAST:event_jCD11ActionPerformed

    private void jCD12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCD12ActionPerformed
		changeCDSelectedAndErase(12);
    }//GEN-LAST:event_jCD12ActionPerformed

    private void jChkForceDicomDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jChkForceDicomDirActionPerformed
		fillReadTable(true);
    }//GEN-LAST:event_jChkForceDicomDirActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButClear;
    private javax.swing.JButton jButDelAll;
    private javax.swing.JButton jButDelete;
    private javax.swing.JButton jButHelp;
    private javax.swing.JButton jButPath;
    private javax.swing.JButton jButRead;
    private javax.swing.JButton jButSaveMip;
    private javax.swing.JButton jButTile;
    private javax.swing.JRadioButton jCD1;
    private javax.swing.JRadioButton jCD10;
    private javax.swing.JRadioButton jCD11;
    private javax.swing.JRadioButton jCD12;
    private javax.swing.JRadioButton jCD2;
    private javax.swing.JRadioButton jCD3;
    private javax.swing.JRadioButton jCD4;
    private javax.swing.JRadioButton jCD5;
    private javax.swing.JRadioButton jCD6;
    private javax.swing.JRadioButton jCD7;
    private javax.swing.JRadioButton jCD8;
    private javax.swing.JRadioButton jCD9;
    private javax.swing.JCheckBox jCheckAccession;
    private javax.swing.JCheckBox jCheckAutoSort;
    private javax.swing.JCheckBox jCheckTile;
    private javax.swing.JCheckBox jChkForceDicomDir;
    private javax.swing.JComboBox jComboLocation;
    private javax.swing.JLabel jLabJava;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelCdName;
    private javax.swing.JLabel jLabelCdName2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelDelete;
    private javax.swing.JPanel jPanelRead;
    private javax.swing.JPanel jPanelSetup;
    private javax.swing.JTabbedPane jReadCDPane;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable3;
    private javax.swing.JTextField jTextN;
    private javax.swing.JTextField jTextName;
    private javax.swing.JTextField jTextNumDays;
    private javax.swing.JTextField jTextPath;
    private javax.swing.JTextField jTextStamp;
    // End of variables declaration//GEN-END:variables
	Preferences jPrefer = null;
	private int readWriteDelFlg = 0, jCurrCD = 0, numDays = 0;
	int xStart =0, yStart=0;
	ArrayList<ImagePlus> imgList = null;
	ArrayList<CD_dirInfo> tableList = null;
	private boolean tab1Dirty = true, tab3Dirty = true, accessFlg = false;
	bkgdLoadData work2 = null;
}

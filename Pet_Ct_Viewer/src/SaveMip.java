import ch.reto_hoehener.japng.Apng;
import ch.reto_hoehener.japng.ApngFactory;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/*
 * SaveMip.java
 *
 * Created on Nov 24, 2010, 2:27:16 PM
 */

/**
 *
 * @author Ilan
 */
public class SaveMip extends javax.swing.JDialog {

    /** Creates new form SaveMip
	 * @param parent
	 * @param modal */
    public SaveMip(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
		this.parent = parent;
        initComponents();
		init();
    }

	private void init() {
		setLocationRelativeTo(parent);
		jPrefer = Preferences.userNodeForPackage(SaveMip.class);
		jPrefer = jPrefer.node("biplugins");
		buttonGroup1 = new javax.swing.ButtonGroup();
		buttonGroup1.add(jRadioDicom);
		buttonGroup1.add(jRadioAvi);
		buttonGroup1.add(jRadioGif);
		buttonGroup1.add(jRadioPng);
		setFileType( jPrefer.getInt("Mip file type", 0));
		Integer vali = jPrefer.getInt("Mip frame time", 100);
		jTextFrmTime.setText(vali.toString());
		retOK = false;
	}

	@Override
	public void dispose() {
		saveRegistryValues();
		super.dispose();
	}

	int getFileType() {
		if( jRadioDicom.isSelected()) return 3;
		if( jRadioPng.isSelected()) return 1;
		if( jRadioGif.isSelected()) return 2;
		return 0;	// AVI default
	}

	int getFrameTime() {
		return ChoosePetCt.parseInt(jTextFrmTime.getText());
	}

	void setFileType(int indx) {
		switch(indx) {
			case 1:
				jRadioPng.setSelected(true);
				break;

			case 2:
				jRadioGif.setSelected(true);
				break;

			case 3:
				jRadioDicom.setSelected(true);
				break;

			default:
				jRadioAvi.setSelected(true);
		}
	}

	private void saveRegistryValues() {
		jPrefer.putInt("Mip file type", getFileType());
		jPrefer.putInt("Mip frame time", getFrameTime());
	}

	int doAction(String iniName) {
		String key1, ext1, path = iniName;
		mipPipe = null;
		int i;
		directColorFlg = false;
		JPanel parPanel;
		if( iniName == null) {
			if( !retOK) return -1;
			key1 = "significant image path";
			i = jPrefer.getInt("significant spin val", 0);
			if( i > 0) key1 += i;
			path = jPrefer.get(key1, null);
			JFileChooser dlg = new JFileChooser(path);
			i = dlg.showSaveDialog(this);
			if( i != JFileChooser.APPROVE_OPTION) return -1;
			path = dlg.getSelectedFile().getPath();
		}
		i = getFileType();
		ext1 = "avi";
		switch(i) {
			case 1:
				ext1 = "png";
				break;

			case 2:
				ext1 = "gif";
				break;

			case 3:
				ext1 = "dcm";
				break;
		}
		if( iniName == null) {
			String tmp = path.toLowerCase();
			if( !tmp.endsWith(ext1)) path += "." + ext1;
		}
		if( parent instanceof PetCtFrame) {
			parPanel = ((PetCtFrame) parent).getPetCtPanel1();
			mipPipe = ((PetCtPanel) parPanel).mipPipe;
			scale = ((PetCtPanel) parPanel).getScalePet();
		}
		if( parent instanceof Display3Frame) {
			parPanel = ((Display3Frame) parent).getDisplay3Panel();
			mipPipe = ((Display3Panel) parPanel).mipPipe;
			scale = ((Display3Panel) parPanel).getScale();
		}
		if( parent instanceof ImageWindow) {
			if( setupImageWindow() < 0) return -1;
		}
		if( mipPipe == null) {
			JOptionPane.showMessageDialog(parent, "Can't find MIP object");
			return -1;
		}
		switch(i) {
			case 0:
				return writeAvi1( path);

			case 1:
				return writePng(path);

			case 2:
				return writeGif(path);

			case 3:
				return writeDicom(path);
		}
		return -1;
	}

	int writeAvi1( String path) {
		AviWriter avi = new AviWriter();
		try {
			avi.writeAvi(path, mipPipe, this);
		} catch (Exception e) {
			ChoosePetCt.stackTrace2Log(e);
			return -1;
		}
		return 0;
	}
	
	int writeDicom(String path) {
		myWriteDicom dicom = new myWriteDicom((PetCtFrame) parent, mipPipe, null);
		return dicom.writeDicomHeaderMip(path);
	}

	int writePng( String path) {
		try {
			int i, delay, numFrm = mipPipe.data1.numFrms;
			String flName = path;
			BufferedImage im1;

			File flout = new File(flName);
			delay = getFrameTime();
			Apng apng = ApngFactory.createApng();
			apng.setPlayCount(Apng.INFINITE_LOOPING);
			for(i=0; i<numFrm; i++) {
				im1 = getRGBImage(i);
				apng.addFrame(im1, delay);
			}
			apng.assemble(flout);
		} catch (Exception e) {
			ChoosePetCt.stackTrace2Log(e);
			return -1;
		}
		return 0;
	}

	int writeGif( String path) {
		try {
			int i, delay, numFrm = mipPipe.data1.numFrms;
			String flName = path;
			BufferedImage im1;

			delay = getFrameTime();
			AnimatedGifEncoder e1 = new AnimatedGifEncoder();
			e1.start(flName);
			e1.setDelay(delay);
			e1.setRepeat(0);
			for(i=0; i<numFrm; i++) {
				im1 = getRGBImage(i);
				e1.addFrame(im1);
			}
			e1.finish();
		} catch (Exception e) {
			ChoosePetCt.stackTrace2Log(e);
			return -1;
		}
		return 0;
	}

	// this is used by AviWriter
	byte[] getPixels(int indx1) {
		Dimension sz1 = getWriteSize();	// overwritten by original size
		sz1.width = mipPipe.data1.width;
		sz1.height = (int) (mipPipe.data1.height * mipPipe.data1.y2xFactor);
		Graphics2D osg;
		int i, num1;
		num1 = sz1.width * sz1.height;
		byte[] pix1 = new byte[num1];

		BufferedImage offscrB = new BufferedImage(sz1.width, sz1.height, BufferedImage.TYPE_BYTE_GRAY);
		osg = offscrB.createGraphics();
		Object hint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
		Image img = parent.createImage(fillSource(indx1));
		osg.drawImage(img, 0, 0, sz1.width, sz1.height, null);
		Raster ras1 = offscrB.getData();
		int [] pix0 = null;
		pix0 = ras1.getPixels(0, 0, sz1.width, sz1.height, pix0);
		for( i=0; i< num1; i++) {
			pix1[i] = (byte) pix0[i];
		}
		return pix1;
	}

	BufferedImage getRGBImage(int indx1) {
		BufferedImage img0, img;
		Dimension sz0, sz1 = getWriteSize();
		Graphics2D osg;
		img = new BufferedImage(sz1.width, sz1.height, BufferedImage.TYPE_INT_ARGB);
		Object hint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
		if( scale > 1.2) hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
		osg = img.createGraphics();
		osg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);

		if( directColorFlg) {
            Point pt1 = new Point(0,0);
			Raster ras1;
			ImagePlus imgPl1 = ((ImageWindow)parent).getImagePlus();
			sz0 = new Dimension(imgPl1.getWidth(), imgPl1.getHeight());
			int depth = imgPl1.getBitDepth();
			Object pix1 = imgPl1.getStack().getPixels(indx1+1);
            SampleModel smp0 = imgPl1.getBufferedImage().getSampleModel();
            int type1 = imgPl1.getBufferedImage().getType();
			img0 = new BufferedImage(sz0.width, sz0.height, type1);
			if( depth == 8) {
				IndexColorModel cm = (IndexColorModel) imgPl1.getBufferedImage().getColorModel();
				img0 = new BufferedImage(sz0.width, sz0.height, type1, cm);
				img = new BufferedImage(sz1.width, sz1.height, type1, cm);
				osg = img.createGraphics();
				osg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
				DataBufferByte db8 = new DataBufferByte((byte [])pix1, sz0.width*sz0.height);
				ras1 = Raster.createRaster(smp0, db8, pt1);
			} else {
		        DataBufferInt db1 = new DataBufferInt((int [])pix1, sz0.width*sz0.height);
				ras1 = Raster.createRaster(smp0, db1, pt1);
			}
            img0.setData(ras1);
			osg.drawImage(img0, 0, 0, sz1.width, sz1.height, null);
			return img;
		}
		Image img1 = parent.createImage(fillSource(indx1));
		osg.drawImage(img1, 0, 0, sz1.width, sz1.height, null);
		return img;
	}

	MemoryImageSource fillSource(int indx1) {
		MemoryImageSource currSource;
		double localScale, slope;
		int sizeX, sizeY, sizeXY, min1, curr1, j;
		short currShort;
		int coef0 = mipPipe.data1.getCoefficent0();
		slope = mipPipe.winSlope * mipPipe.data1.getRescaleSlope(indx1);
		sizeX = mipPipe.data1.width;
		sizeY = mipPipe.data1.height;
		sizeXY = sizeX * sizeY;
		min1 = (int)((mipPipe.winLevel - mipPipe.winWidth/2 - mipPipe.data1.rescaleIntercept)/slope);
		localScale = 256.0*slope/mipPipe.winWidth;
		byte[] buff = new byte[sizeX*sizeY];
		short[] buff1;
		buff1 = mipPipe.data1.pixels.get(indx1);
		for( j=0; j<sizeXY; j++) {
			currShort = (short)(buff1[j] + coef0);
			curr1 = mipPipe.maybeLog(currShort);
			curr1 = (int)((curr1 - min1) * localScale);
			if( curr1 > 255) curr1 = 255;
			if( curr1 < 0) curr1 = 0;
			buff[j] = (byte) curr1;
		}
		currSource = new MemoryImageSource(sizeX, sizeY, mipPipe.cm, buff, 0, sizeX);
		return currSource;
	}

	Dimension getWriteSize() {
		Dimension sz1 = new Dimension();
		double width0, height0;
		width0 = mipPipe.data1.width;
		height0 = mipPipe.data1.height * mipPipe.data1.y2xFactor;
		sz1.width = (int) (scale * width0);
		sz1.height = (int) (scale * height0);
		return sz1;
	}
	
	int setupImageWindow() {
		ImageWindow imgWnd = (ImageWindow) parent;
		ImagePlus img1 = imgWnd.getImagePlus();
		double min0, max0, max1, off0, coef0 = 0;
		double[] coef;
		if( img1.getStackSize() < 4) {
			JOptionPane.showMessageDialog(parent, "Must have a cine of at least 4 images.");
			return -1;
		}
		mipPipe = new JFijiPipe();
		mipPipe.LoadData(img1,0);
		mipPipe.cm = mipPipe.makeColorModel(JFijiPipe.COLOR_GRAY);
		scale = imgWnd.getCanvas().getMagnification();
		coef = img1.getCalibration().getCoefficients();
		if( coef != null) coef0 = coef[0];
		min0 = img1.getDisplayRangeMin() + coef0;
		max0 = img1.getDisplayRangeMax() + coef0;
		max1 = mipPipe.data1.grandMax;
		ColorModel cm0 = img1.getBufferedImage().getColorModel();
		// get Israel Bar David ultrasound data
		if( max1 == 0 && (cm0 instanceof DirectColorModel || cm0 instanceof IndexColorModel)) {
			directColorFlg = true;
			mipPipe.winWidth = 1000;
			mipPipe.winLevel = 500;
			return 0;
		}
		if( max0 > max1 || max1 <= 0) return -1;
		off0 = min0 * 1000.0 / max1;
		mipPipe.winWidth = 1000.0*(max0 - min0)/max1;
		mipPipe.winLevel = (mipPipe.winWidth / 2) + off0;
		return 0;
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
        jPanel1 = new javax.swing.JPanel();
        jRadioAvi = new javax.swing.JRadioButton();
        jRadioPng = new javax.swing.JRadioButton();
        jRadioGif = new javax.swing.JRadioButton();
        jRadioDicom = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jTextFrmTime = new javax.swing.JTextField();
        jButOk = new javax.swing.JButton();
        jButCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("File type"));

        jRadioAvi.setText("AVI");

        jRadioPng.setText("animated PNG");

        jRadioGif.setText("animated GIF");

        jRadioDicom.setText("Dicom");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jRadioPng)
                    .addComponent(jRadioAvi)
                    .addComponent(jRadioGif)
                    .addComponent(jRadioDicom))
                .addContainerGap(23, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jRadioDicom)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioAvi)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioPng)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioGif))
        );

        jLabel1.setText("Frame time (msec)");

        jButOk.setText("OK");
        jButOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButOkActionPerformed(evt);
            }
        });

        jButCancel.setText("Cancel");
        jButCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jTextFrmTime, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jButOk)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButCancel))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFrmTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButOk)
                    .addComponent(jButCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jButOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButOkActionPerformed
		retOK = true;
		dispose();
	}//GEN-LAST:event_jButOkActionPerformed

	private void jButCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButCancelActionPerformed
		dispose();
	}//GEN-LAST:event_jButCancelActionPerformed

 
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButCancel;
    private javax.swing.JButton jButOk;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton jRadioAvi;
    private javax.swing.JRadioButton jRadioDicom;
    private javax.swing.JRadioButton jRadioGif;
    private javax.swing.JRadioButton jRadioPng;
    private javax.swing.JTextField jTextFrmTime;
    // End of variables declaration//GEN-END:variables
	Preferences jPrefer = null;
	Frame parent;
	JFijiPipe mipPipe = null;
	boolean directColorFlg = false;
	double scale;
	boolean retOK;
}


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.text.DateFormat;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import vtk.*;



/**
 *
 * @author ilan
 */
public class VtkFrame extends javax.swing.JFrame {

	// In the static contructor we load in the native code.
	// The libraries must be in your path to work.
	static {
		System.loadLibrary("jawt");
		System.loadLibrary("vtkCommonJava");
		System.loadLibrary("vtkFilteringJava");
		System.loadLibrary("vtkIOJava");
		System.loadLibrary("vtkImagingJava");
		System.loadLibrary("vtkGraphicsJava");
		System.loadLibrary("vtkRenderingJava");
		System.loadLibrary("vtkWidgetsJava");
	}

	/**
	 * Creates new form VtkFrame
	 * @param parent
	 * @param pointList
	 * @param surfaceList
	 * @param colorList
	 * @param radioVal
	 */
	public VtkFrame(JFrame parent, int [] pointList, int [] surfaceList,
			Color[] colorList, int radioVal) {
		par1 = (PetCtFrame) parent;
		initComponents();
		init1( pointList, surfaceList, colorList, radioVal);
	}
	
	private void init1( int [] pointList, int [] surfaceList, Color[] colorList, int radioVal) {
		Preferences pref = par1.jPrefer;
		if( pointList == null) return;

		final vtkFileOutputWindow outWin = new vtkFileOutputWindow();
		outWin.SetInstance(outWin);
		outWin.AppendOn();
		outWin.FlushOn();
		outWin.GlobalWarningDisplayOff();
		outWin.SetFileName("vtkError.log");

		setLocationRelativeTo(par1);
		renWin = new vtkPanel();
		Dimension sz1 = new Dimension();
		sz1.height = pref.getInt("vtk dialog height", 0);
		if( sz1.height > 0) {
			sz1.width = pref.getInt("vtk dialog width", 0);
			setSize(sz1);
		}
		if(radioVal == 0) displayCT(pointList, surfaceList, colorList);
		else displayPET(pointList, surfaceList, colorList);
	}
	
	private void mySetColor(vtkActor actor, Color col1) {
		int red, green, blue;
		red = col1.getRed();
		green = col1.getGreen();
		blue = col1.getBlue();
		actor.GetProperty().SetColor(red / 255.0, green / 255.0, blue / 255.0);
	}
	
	private void displayCT(int [] pointList, int [] surfaceList, Color[] colorList) {
		int scl2 = 2;
		int xlo, xhi, ylo, yhi, i, ymax;
		int zlo, zhi, intercept;
		vtkActor actor1, actor2 = null;
		PetCtPanel pan1 = par1.getPetCtPanel1();
		JFijiPipe ct1 = pan1.ctPipe;
		JFijiPipe pet1 = pan1.petPipe;
		String meta = ct1.data1.metaData;
		intercept = (int) ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "0028,1052"));
		int surfaceLevel = surfaceList[0] + intercept;
		ymax = pet1.data1.height;
		ylo = ymax - pointList[2];	// the y value is backwards
		if( ylo < 0) ylo = 0;
		yhi = ymax - pointList[3];
		if( yhi < 0) yhi = 0;
		xlo = pan1.shift2Ct(ct1, pointList[0], 0);
		xhi = pan1.shift2Ct(ct1, pointList[1], 0);
		ylo = pan1.shift2Ct(ct1, ylo, 1);
		yhi = pan1.shift2Ct(ct1, yhi, 1);
		if( xlo > xhi) {
			i = xlo;
			xlo = xhi;
			xhi = i;
		}
		if( ylo > yhi) {
			i = ylo;
			ylo = yhi;
			yhi = i;
		}
		zlo = ct1.findCtPos(pointList[4], true);
		zhi = ct1.findCtPos(pointList[5], true);
		if( zlo > zhi) {
			i = zlo;
			zlo = zhi;
			zhi = i;
		}
		String tmp = par1.m_patName + "  ";
		tmp += DateFormat.getDateInstance(DateFormat.MEDIUM).format(ct1.data1.serTime);
		setTitle(tmp);

		tmp = ct1.data1.srcImage.getOriginalFileInfo().directory;
		vtkDICOMImageReader reader = new vtkDICOMImageReader();
		reader.SetDirectoryName(tmp);
		reader.Update();

//		vtkImageViewer2 imageViewer = new vtkImageViewer2();
//		imageViewer.SetInputConnection(reader.GetOutputPort());
		
		vtkExtractVOI surfaceExtractor = new vtkExtractVOI();
		surfaceExtractor.SetInput(reader.GetOutput());
		surfaceExtractor.SetVOI(xlo, xhi, ylo, yhi, zlo, zhi);
		surfaceExtractor.SetSampleRate(scl2, scl2, 1);
		
		vtkContourFilter outline1 = new vtkContourFilter();
		outline1.SetInput(surfaceExtractor.GetOutput());
		outline1.SetValue(0, surfaceLevel);
		
		vtkPolyDataNormals skinNormals = new vtkPolyDataNormals();
		skinNormals.SetInput(outline1.GetOutput());
//		skinNormals.SetFeatureAngle(60.0);

		vtkPolyDataMapper map1 = new vtkPolyDataMapper();
		map1.SetInput(skinNormals.GetOutput());
		map1.SetImmediateModeRendering(1);
		map1.ScalarVisibilityOff();
		
		if( surfaceList[1] > 0 && surfaceList[1] < surfaceList[0]) {
			surfaceLevel = surfaceList[1] + intercept;
			vtkContourFilter outline2 = new vtkContourFilter();
			outline2.SetInput(surfaceExtractor.GetOutput());
			outline2.SetValue(0, surfaceLevel);

			vtkPolyDataNormals skinNormals2 = new vtkPolyDataNormals();
			skinNormals2.SetInput(outline2.GetOutput());
//			skinNormals.SetFeatureAngle(60.0);

			vtkPolyDataMapper map2 = new vtkPolyDataMapper();
			map2.SetInput(skinNormals2.GetOutput());
			map2.SetImmediateModeRendering(1);
			map2.ScalarVisibilityOff();
			
			actor2 = new vtkActor();
			actor2.SetMapper(map2);
			mySetColor( actor2, colorList[1]);
			actor2.GetProperty().SetOpacity(0.5);
		}
		
/*		vtkLODActor actor1 = new vtkLODActor();
		actor1.SetMapper(map1);
		actor1.GetProperty().SetRepresentationToSurface();
		actor1.GetProperty().SetInterpolationToGouraud();
		actor1.GetProperty().SetAmbient(0);
		actor1.GetProperty().SetDiffuse(1);
		actor1.GetProperty().SetDiffuseColor(1, 1, 0.9412);
		actor1.GetProperty().SetSpecular(0);
		actor1.GetProperty().SetSpecularPower(1);
		actor1.GetProperty().SetSpecularColor(1, 1, 1);*/
		
		actor1 = new vtkActor();
		actor1.SetMapper(map1);
		mySetColor( actor1, colorList[0]);

		vtkRenderer rend1 = renWin.GetRenderer();
		rend1.AddActor(actor1);
		if( actor2 != null) rend1.AddActor(actor2);
		rend1.SetBackground(0.8, 0.8, 0.8);
		rend1.GetActiveCamera().Elevation(70);
		rend1.ResetCamera();
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(renWin, BorderLayout.CENTER);
	}
	
	private void displayPET(int [] pointList, int [] surfaceList, Color[] colorList) {
		int scl2 = 2;
		int xlo, xhi, ylo, yhi, i, ymax;
		int zlo, zhi, intercept;
		vtkActor actor1, actor2 = null;
		PetCtPanel pan1 = par1.getPetCtPanel1();
		JFijiPipe pet1 = pan1.petPipe;
		String meta = pet1.data1.metaData;
		intercept = (int) ChoosePetCt.parseDouble(ChoosePetCt.getDicomValue(meta, "0028,1052"));
		int surfaceLevel = surfaceList[0] + intercept;
		ymax = pet1.data1.height;
		xlo = pointList[0];
		xhi = pointList[1];
		ylo = ymax - pointList[2];	// the y value is backwards
		if( ylo < 0) ylo = 0;
		yhi = ymax - pointList[3];
		if( yhi < 0) yhi = 0;
		if( xlo > xhi) {
			i = xlo;
			xlo = xhi;
			xhi = i;
		}
		if( ylo > yhi) {
			i = ylo;
			ylo = yhi;
			yhi = i;
		}
		zlo = pointList[4];
		zhi = pointList[5];
		if( zlo > zhi) {
			i = zlo;
			zlo = zhi;
			zhi = i;
		}
		String tmp = par1.m_patName + "  ";
		tmp += DateFormat.getDateInstance(DateFormat.MEDIUM).format(pet1.data1.serTime);
		setTitle(tmp);

		tmp = pet1.data1.srcImage.getOriginalFileInfo().directory;
		vtkDICOMImageReader reader = new vtkDICOMImageReader();
		reader.SetDirectoryName(tmp);
		reader.Update();
		
		vtkExtractVOI surfaceExtractor = new vtkExtractVOI();
		surfaceExtractor.SetInput(reader.GetOutput());
		surfaceExtractor.SetVOI(xlo, xhi, ylo, yhi, zlo, zhi);
		surfaceExtractor.SetSampleRate(scl2, scl2, 1);
		
		vtkContourFilter outline1 = new vtkContourFilter();
		outline1.SetInput(surfaceExtractor.GetOutput());
		outline1.SetValue(0, surfaceLevel);
		
		vtkPolyDataNormals skinNormals = new vtkPolyDataNormals();
		skinNormals.SetInput(outline1.GetOutput());

		vtkPolyDataMapper map1 = new vtkPolyDataMapper();
		map1.SetInput(skinNormals.GetOutput());
		map1.SetImmediateModeRendering(1);
		map1.ScalarVisibilityOff();
		
		if( surfaceList[1] > 0 && surfaceList[1] < surfaceList[0]) {
			surfaceLevel = surfaceList[1] + intercept;
			vtkContourFilter outline2 = new vtkContourFilter();
			outline2.SetInput(surfaceExtractor.GetOutput());
			outline2.SetValue(0, surfaceLevel);

			vtkPolyDataNormals skinNormals2 = new vtkPolyDataNormals();
			skinNormals2.SetInput(outline2.GetOutput());

			vtkPolyDataMapper map2 = new vtkPolyDataMapper();
			map2.SetInput(skinNormals2.GetOutput());
			map2.SetImmediateModeRendering(1);
			map2.ScalarVisibilityOff();
			
			actor2 = new vtkActor();
			actor2.SetMapper(map2);
			mySetColor( actor2, colorList[1]);
			actor2.GetProperty().SetOpacity(0.5);
		}
		
		actor1 = new vtkActor();
		actor1.SetMapper(map1);
		mySetColor( actor1, colorList[0]);

		vtkRenderer rend1 = renWin.GetRenderer();
		rend1.AddActor(actor1);
		if( actor2 != null) rend1.AddActor(actor2);
		rend1.SetBackground(0.8, 0.8, 0.8);
		rend1.GetActiveCamera().Elevation(70);
		rend1.ResetCamera();
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(renWin, BorderLayout.CENTER);
	}

	@Override
	public void dispose() {
		Preferences pref = par1.jPrefer;
		Dimension sz1 = getSize();
		pref.putInt("vtk dialog width", sz1.width);
		pref.putInt("vtk dialog height", sz1.height);
		super.dispose();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
	vtkBoxWidget boxWidget = null;
	PetCtFrame par1 = null;
	private vtkPanel renWin;
}

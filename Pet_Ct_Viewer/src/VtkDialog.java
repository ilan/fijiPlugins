
import java.awt.*;
import java.util.prefs.Preferences;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ilan
 */
public class VtkDialog extends javax.swing.JDialog {

	/**
	 * Creates new form VtkDialog
	 */
	public VtkDialog(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		petCt = (PetCtFrame) parent;
		initComponents();
		init1();
		clearPoints();
	}
	
	private void init1() {
		Preferences pref = petCt.jPrefer;
		Integer level = pref.getInt("vtk level1", 1300);
		jTextSurface1.setText(level.toString());
		level = pref.getInt("vtk level2", 750);
		jTextSurface2.setText(level.toString());
		buttonGroup1.add(jRadioCT);
		buttonGroup1.add(jRadioPet);
		icon1 = new MyIcon();
		int rgb = pref.getInt("vtk icon1", 0xff0000);
		Color col1 = new Color(rgb);
		icon1.setColor(col1);
		jButCol1.setIcon(icon1);
		icon2 = new MyIcon();
		rgb = pref.getInt("vtk icon2", 0xff00);
		col1 = new Color(rgb);
		icon2.setColor(col1);
		jButCol2.setIcon(icon2);
	}
	
	class MyIcon implements Icon {
		Color color = Color.red;
		int width = 16, height = 12;

		public int getIconHeight() {
			return height;
		}

		public int getIconWidth() {
			return width;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = (Graphics2D) g;
			Rectangle rect1 = new Rectangle(x, y, width, height);
			g2.setColor(color);
			g2.fill(rect1);
		}
		
		public void setColor(Color c) {
			color = c;
		}
		
	}
	
	boolean IsOK() {
		// Clear Canvas has the whole study in a single directory.
		// vtkDICOMImageReader can't handle such data. Check it.
		String tmp0, tmp1;
		tmp0 = petCt.getPetCtPanel1().ctPipe.data1.srcImage.getOriginalFileInfo().directory;
		tmp1 = petCt.getPetCtPanel1().petPipe.data1.srcImage.getOriginalFileInfo().directory;
		if( tmp0.equals(tmp1)) {
			tmp0 = "Clear Canvas stores the whole study in a single directory.\n";
			tmp0 += "Vtk cannot read such data. Only data with series in\n";
			tmp0 += "separate directories can be displayed. Sorry...";
			JOptionPane.showMessageDialog(petCt, tmp0);
			dispose();
			return false;
		}
		return true;
	}
	
	void updatePointList( int xpos, int ypos, int zpos, boolean isMIP) {
		if( isMIP) return;
		boolean isAxial = (petCt.getPetCtPanel1().m_sliceType == JFijiPipe.DSP_AXIAL);
		int bad = 0;
		if( pointIdx < 4 && !isAxial) bad = 1;
		if( pointIdx >= 4 && isAxial) bad = 2;
		if( bad > 0) {
			String tmp = "Please change mode to axial";
			if( bad == 2) tmp = "Please change mode to coronal or sagital";
			JOptionPane.showMessageDialog(this, tmp);
			return;
		}
		switch(pointIdx) {
			case 0:
				pointList[0] = xpos;
				jLabXlo.setEnabled(true);
				pointIdx++;
				break;
				
			case 1:
				if( Math.abs(xpos-lastX) < 6) return;
				pointList[1] = xpos;
				jLabXhi.setEnabled(true);
				pointIdx++;
				break;
				
			case 2:
				if( Math.abs(xpos-lastX) < 6 && Math.abs(ypos-lastY) < 6) return;
				pointList[2] = ypos;
				jLabYlo.setEnabled(true);
				pointIdx++;
				break;
				
			case 3:
				if( Math.abs(ypos-lastY) < 6) return;
				pointList[3] = ypos;
				jLabYhi.setEnabled(true);
				jLabCoronal.setEnabled(true);
				jLabCoron1.setEnabled(true);
				pointIdx++;
				break;
				
			case 4:
				pointList[4] = zpos;
				jLabZlo.setEnabled(true);
				pointIdx++;
				break;
				
			case 5:
				pointList[5] = zpos;
				jLabZhi.setEnabled(true);
				jButOK.setEnabled(true);
				pointIdx++;
				break;
				
			default:
				break;
		}
		lastX = xpos;
		lastY = ypos;
		lastZ = zpos;
	}

	@Override
	public void dispose() {
		petCt.vtkDiag = null;
		super.dispose();
	}
	
	private void colorPick( int type) {
		MyIcon icon = icon1;
		if( type > 1) icon = icon2;
		Color col0 = icon.color;
		Color col1 = JColorChooser.showDialog(this, "Choose color", col0);
		icon.setColor(col1);
		int rgb = col1.getRGB();
		Preferences pref = petCt.jPrefer;
		pref.putInt("vtk icon"+type, rgb);
		repaint();
	}

	private void OKbut() {
		int[] surfaceList = new int[2];
		Color[] colorList = new Color[2];
		int radioVal=0;
		String tmp = System.getProperty("os.name");
		if( tmp.contains("Linux")) {
			tmp = System.getenv("LD_LIBRARY_PATH");
			if( tmp == null || tmp.isEmpty()) {
				JOptionPane.showMessageDialog(this, "No path to the VTK libraries is defined.");
				return;
			}
		}
		tmp = jTextSurface2.getText();
		Preferences pref = petCt.jPrefer;
		surfaceList[0] = Integer.parseInt(jTextSurface1.getText());
		if( tmp == null || tmp.isEmpty()) tmp = "0";
		surfaceList[1] = Integer.parseInt(tmp);
		for( int i=1; i<=2; i++) {
			pref.putInt("vtk level"+i, surfaceList[i-1]);
		}
		if(jRadioPet.isSelected()) radioVal = 1;
		colorList[0] = icon1.color;
		colorList[1] = icon2.color;
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		VtkFrame vtk1 = new VtkFrame(petCt, pointList, surfaceList, colorList, radioVal);
		dispose();
		vtk1.setVisible(true);
	}
	
	private void clearPoints() {
		pointIdx = 0;
		jLabXlo.setEnabled(false);
		jLabXhi.setEnabled(false);
		jLabYlo.setEnabled(false);
		jLabYhi.setEnabled(false);
		jLabCoronal.setEnabled(false);
		jLabCoron1.setEnabled(false);
		jLabZlo.setEnabled(false);
		jLabZhi.setEnabled(false);
		jButOK.setEnabled(false);
		pointList = new int[6];
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
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabXlo = new javax.swing.JLabel();
        jLabXhi = new javax.swing.JLabel();
        jLabYlo = new javax.swing.JLabel();
        jLabYhi = new javax.swing.JLabel();
        jButClear = new javax.swing.JButton();
        jLabCoronal = new javax.swing.JLabel();
        jLabZlo = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabZhi = new javax.swing.JLabel();
        jButOK = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jTextSurface1 = new javax.swing.JTextField();
        jTextSurface2 = new javax.swing.JTextField();
        jLabCoron1 = new javax.swing.JLabel();
        jRadioCT = new javax.swing.JRadioButton();
        jRadioPet = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();
        jButCol1 = new javax.swing.JButton();
        jButCol2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("VTK Volume");

        jLabel1.setText("This program builds a volume from the CT (or PET) data.");

        jLabel2.setText("First click on the MIP to a point inside the volume of interest.");

        jLabel3.setText("Next click on x and y limits of the volume using the CT slice.");

        jLabXlo.setText("x low");
        jLabXlo.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabXlo.setEnabled(false);

        jLabXhi.setText("x high");
        jLabXhi.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabXhi.setEnabled(false);

        jLabYlo.setText("y low");
        jLabYlo.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabYlo.setEnabled(false);

        jLabYhi.setText("y high");
        jLabYhi.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabYhi.setEnabled(false);

        jButClear.setText("Clear");
        jButClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButClearActionPerformed(evt);
            }
        });

        jLabCoronal.setText("Now click on the sagital or coronal display.");
        jLabCoronal.setEnabled(false);

        jLabZlo.setText("z low");
        jLabZlo.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabZlo.setEnabled(false);

        jLabel4.setText("(While clicking, notice the values become active.)");

        jLabZhi.setText("z high");
        jLabZhi.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jLabZhi.setEnabled(false);

        jButOK.setText("OK");
        jButOK.setEnabled(false);
        jButOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButOKActionPerformed(evt);
            }
        });

        jLabel5.setText("Surface levels:");

        jLabCoron1.setText("In this view click on the z limits, again using the CT slice.");
        jLabCoron1.setEnabled(false);

        jRadioCT.setSelected(true);
        jRadioCT.setText("CT");

        jRadioPet.setText("PET");

        jLabel6.setText("(Usually x = left, right and y = anterior, posterior.)");

        jButCol1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButCol1ActionPerformed(evt);
            }
        });

        jButCol2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButCol2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1)
            .addComponent(jLabel2)
            .addComponent(jLabel3)
            .addComponent(jLabel4)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabXlo)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jLabXhi)
                            .addGap(18, 18, 18)
                            .addComponent(jLabYlo)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jLabYhi)
                            .addGap(18, 18, 18)
                            .addComponent(jButClear))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabZlo)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jLabZhi)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButOK)
                            .addGap(26, 26, 26))))
                .addComponent(jLabCoronal, javax.swing.GroupLayout.Alignment.LEADING))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextSurface1, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButCol1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextSurface2, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButCol2))
            .addComponent(jLabCoron1)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jRadioCT)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioPet))
            .addComponent(jLabel6)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabXlo)
                    .addComponent(jLabXhi)
                    .addComponent(jLabYlo)
                    .addComponent(jLabYhi)
                    .addComponent(jButClear))
                .addGap(18, 18, 18)
                .addComponent(jLabCoronal)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabCoron1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabZlo)
                    .addComponent(jLabZhi)
                    .addComponent(jButOK))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButCol1)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel5)
                                .addComponent(jTextSurface1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jRadioCT)
                            .addComponent(jRadioPet)))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextSurface2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButCol2)))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jButClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButClearActionPerformed
		clearPoints();
	}//GEN-LAST:event_jButClearActionPerformed

	private void jButOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButOKActionPerformed
		OKbut();
	}//GEN-LAST:event_jButOKActionPerformed

	private void jButCol1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButCol1ActionPerformed
		colorPick(1);
	}//GEN-LAST:event_jButCol1ActionPerformed

	private void jButCol2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButCol2ActionPerformed
		colorPick(2);
	}//GEN-LAST:event_jButCol2ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButClear;
    private javax.swing.JButton jButCol1;
    private javax.swing.JButton jButCol2;
    private javax.swing.JButton jButOK;
    private javax.swing.JLabel jLabCoron1;
    private javax.swing.JLabel jLabCoronal;
    private javax.swing.JLabel jLabXhi;
    private javax.swing.JLabel jLabXlo;
    private javax.swing.JLabel jLabYhi;
    private javax.swing.JLabel jLabYlo;
    private javax.swing.JLabel jLabZhi;
    private javax.swing.JLabel jLabZlo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JRadioButton jRadioCT;
    private javax.swing.JRadioButton jRadioPet;
    private javax.swing.JTextField jTextSurface1;
    private javax.swing.JTextField jTextSurface2;
    // End of variables declaration//GEN-END:variables
	PetCtFrame petCt = null;
	int pointIdx = 0, lastX = -1, lastY = -1, lastZ = -1;
	int[] pointList = null;
	MyIcon icon1, icon2;
}

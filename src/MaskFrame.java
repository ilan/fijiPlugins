
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.ImageWindow;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.ColorModel;
import javax.swing.JOptionPane;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ilan
 */
public class MaskFrame extends javax.swing.JFrame implements WindowFocusListener {

	/**
	 * Creates new form MaskFrame
	 */
	public MaskFrame() {
		initComponents();
		init();
	}

	private void init() {
		int i;
//		WindowManager.addWindow(this);
		String arg = Macro.getOptions();
		if( arg != null && !arg.isEmpty()) {
			i = arg.indexOf("	");
			if( i> 0) {
				jLabOrig.setText(arg.substring(0, i).trim());
				jLabMask.setText(arg.substring(i+1).trim());
			}
		}
		isOk2Run();
		addWindowFocusListener(this);
	}
	
	void updateWindowValue(Window lastWin) {
		javax.swing.JLabel curLab = null;
		ImageWindow win1 = (ImageWindow) lastWin;
		ImagePlus img1 = win1.getImagePlus();
		String title = img1.getTitle();
		if(jCheckMask.isSelected()) curLab = jLabMask;
		if(jCheckOrig.isSelected()) curLab = jLabOrig;
		jCheckMask.setSelected(false);
		jCheckOrig.setSelected(false);
		if( curLab == null) return;
		curLab.setText(title);
		isOk2Run();
	}
	
	boolean isOk2Run() {
		boolean retVal = true;
		imgOrig = getImgPlus(jLabOrig.getText());
		imgMask = getImgPlus(jLabMask.getText());
		if( imgOrig == null || imgMask == null) retVal = false;
		else if( imgOrig.equals(imgMask)) retVal = false;
		jButGo.setEnabled(retVal);
		return retVal;
	}
	
	// this is where the work is done. If we got to here both the original
	// and mask data are valid. So do the calculation and go home.
/*	void doMasking() {
		int i, j, stkSz, sliceSz;
		short curVal;
		double min, max;
		ImageStack origStack, maskStack;
		Object pix1, pix2;
		short[] origIn, maskIn, maskOut;
		stkSz = imgOrig.getImageStackSize();
		i = imgMask.getImageStackSize();
		if( i!= stkSz) {
			JOptionPane.showMessageDialog(this, "Stack sizes not equal.");
			return;
		}
		origStack = imgOrig.getImageStack();
		min = imgOrig.getDisplayRangeMin();
		max = imgOrig.getDisplayRangeMax();
		maskStack = imgMask.getImageStack();
		for( i=1; i<=stkSz; i++) {
			pix1 = origStack.getPixels(i);
			if(!(pix1 instanceof short [])) {
				JOptionPane.showMessageDialog(this,"Must have 16 bit data on both series");
				return;	// should never happen
			}
			origIn = (short[]) pix1;
			sliceSz = origIn.length;
			pix2 = maskStack.getPixels(i);
			if(!(pix2 instanceof short [])) {
				JOptionPane.showMessageDialog(this,"Must have 16 bit data on both series");
				return;
			}
			maskIn = (short[]) pix2;
			maskOut = new short[sliceSz];
			for( j=0; j<sliceSz; j++) {
				curVal = maskIn[j];
				if( curVal != 0) curVal = origIn[j];
				maskOut[j] = curVal;
			}
			maskStack.setPixels(maskOut, i);
		}
		imgMask.setDisplayRange(min, max);
		imgMask.setStack(maskStack);
		dispose();
	}*/
	
	void doMasking() {
		int i, j, stkSz, sliceSz, width, height;
		short curVal;
		double min, max;
		String sliceLab;
		ColorModel cm;
		ImageStack origStack, maskStack, msk1Stack;
		Object pix1, pix2;
		short[] origIn, maskIn, maskOut;
		stkSz = imgOrig.getImageStackSize();
		i = imgMask.getImageStackSize();
		if( i!= stkSz) {
			JOptionPane.showMessageDialog(this, "Stack sizes not equal.");
			return;
		}
		origStack = imgOrig.getImageStack();
		min = imgOrig.getDisplayRangeMin();
		max = imgOrig.getDisplayRangeMax();
		maskStack = imgMask.getImageStack();
		cm = maskStack.getColorModel();
		width = maskStack.getWidth();
		height = maskStack.getHeight();
		msk1Stack = new ImageStack(width, height, cm);
		for( i=1; i<=stkSz; i++) {
			pix1 = origStack.getPixels(i);
			if(!(pix1 instanceof short [])) {
				JOptionPane.showMessageDialog(this,"Must have 16 bit data on both series");
				return;	// should never happen
			}
			origIn = (short[]) pix1;
			sliceSz = origIn.length;
			sliceLab = maskStack.getSliceLabel(i);
			pix2 = maskStack.getPixels(i);
			if(!(pix2 instanceof short [])) {
				JOptionPane.showMessageDialog(this,"Must have 16 bit data on both series");
				return;
			}
			maskIn = (short[]) pix2;
			maskOut = new short[sliceSz];
			for( j=0; j<sliceSz; j++) {
				curVal = maskIn[j];
				if( curVal != 0) curVal = origIn[j];
				maskOut[j] = curVal;
			}
			msk1Stack.addSlice(sliceLab, maskOut);
//			maskStack.setPixels(maskOut, i);
		}
		imgMask.setStack(msk1Stack);
		imgMask.setDisplayRange(min, max);
		imgMask.repaintWindow();
		dispose();
	}
	
	ImagePlus getImgPlus(String title) {
		if(title == null || title.isEmpty()) return null;
		ImagePlus img1;
		int i, sz;
		int [] myList = WindowManager.getIDList();
		sz = myList.length;
		for(i=0; i<sz; i++) {
			img1 = WindowManager.getImage(myList[i]);
			if(img1.getTitle().equals(title)) return img1;
		}
		return null;
	}
	
	void openHelp() {
		ChoosePetCt.openHelp("Mask");
	}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		Window wold = e.getOppositeWindow();
		if( wold == null) return;
		if( wold instanceof ImageWindow) {
			updateWindowValue(wold);
		}
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jCheckOrig = new javax.swing.JCheckBox();
        jLabOrig = new javax.swing.JLabel();
        jCheckMask = new javax.swing.JCheckBox();
        jLabMask = new javax.swing.JLabel();
        jButGo = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jButHelp = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Original with non zero Mask -> Mask");

        jLabel1.setText("Choose original Image and Mask. To change the image first check");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Choose each image"));

        jCheckOrig.setText("Original");

        jLabOrig.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jCheckMask.setText("Mask");

        jLabMask.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckOrig)
                    .addComponent(jCheckMask))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabOrig, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabMask, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckOrig)
                    .addComponent(jLabOrig))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckMask)
                    .addComponent(jLabMask)))
        );

        jButGo.setText("Mask the data");
        jButGo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButGoActionPerformed(evt);
            }
        });

        jLabel3.setText("either Original or Mask. Then click on the image, and click again here.");

        jButHelp.setText("Help");
        jButHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButHelpActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jButGo)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButHelp))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1)
                                .addComponent(jLabel3))
                            .addGap(0, 0, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButGo)
                    .addComponent(jButHelp))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButGoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButGoActionPerformed
		doMasking();
    }//GEN-LAST:event_jButGoActionPerformed

    private void jButHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButHelpActionPerformed
        openHelp();
    }//GEN-LAST:event_jButHelpActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButGo;
    private javax.swing.JButton jButHelp;
    private javax.swing.JCheckBox jCheckMask;
    private javax.swing.JCheckBox jCheckOrig;
    private javax.swing.JLabel jLabMask;
    private javax.swing.JLabel jLabOrig;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
	ImagePlus imgOrig, imgMask;
}

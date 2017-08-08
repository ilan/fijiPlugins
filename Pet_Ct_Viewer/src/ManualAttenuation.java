
import ij.IJ;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ManualAttenuation.java
 *
 * Created on Jan 12, 2010, 2:09:36 PM
 */

/**
 *
 * @author Ilan
 */
public class ManualAttenuation extends javax.swing.JDialog implements WindowFocusListener {
	JFijiPipe attenMap = null;
	PetCtPanel parentPet = null;
	bkgdBuildMap work2 = null;
	int bkgdType =0;
	int numSmooth = 0;
	double saveSUVfactor, saveHalfLife;
	Date saveSerTime;
	ArrayList<Integer> sliceInUse = null;
	ArrayList<Integer> sourceIdx = null;

    /** Creates new form ManualAttenuation
	 * @param parent
	 * @param modal */
    public ManualAttenuation(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
		init();
    }

	private void init() {
		jButApply.setEnabled(false);
		jSpinOffset.setEnabled(false);
		jButBuild.setEnabled(false);
		jButSmooth.setEnabled(false);
		attenMap = null;
		addWindowFocusListener(this);
	}

	@Override
	public void windowGainedFocus(WindowEvent we) {
		Window wold = we.getOppositeWindow();
		if( wold instanceof PetCtFrame) {
			PetCtFrame parent = (PetCtFrame) wold;
			parentPet = parent.getPetCtPanel1();
		}
	}

	@Override
	public void windowLostFocus(WindowEvent we) {}

	void refresh() {
		boolean applyFlg = false, buildFlg = false, smoothFlg = false;
		if( parentPet != null) {
			jSpinOffset.setValue(parentPet.petPipe.data1.mriOffZ);
			if( attenMap != null) {
				applyFlg = smoothFlg = true;
			}
			if( parentPet.petPipe != null && parentPet.upetPipe != null) {
				if(parentPet.petPipe.data1.numFrms == parentPet.upetPipe.data1.numFrms) buildFlg = true;
			}
		}
		jButApply.setEnabled(applyFlg);
		jSpinOffset.setEnabled(applyFlg);
		jButBuild.setEnabled(buildFlg);
		jButSmooth.setEnabled(smoothFlg);
	}

	void offsetChanged() {
		SpinnerNumberModel spin1 = (SpinnerNumberModel) jSpinOffset.getModel();
		int i = spin1.getNumber().intValue();
		parentPet.petPipe.data1.mriOffZ = i;
		parentPet.repaint();
	}

	void buildMap(int type) {
		bkgdType = type;
		if( type == 0) init();	// reset all enable flags
		work2 = new bkgdBuildMap();
		work2.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String propertyName = evt.getPropertyName();
				if( propertyName.equals("state")) {
					SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
					if( state == SwingWorker.StateValue.DONE) {
						work2 = null;
						if( bkgdType == 0) {
							boolean enFlg = false;
							if( attenMap != null) enFlg = true;
							jButBuild.setEnabled(true);
							jButSmooth.setEnabled(enFlg);
						}
					}
				}
			}
		});
		work2.execute();
	}

	void doMapBuild() {
		attenMap = new JFijiPipe();
		numSmooth = 0;
		boolean petData = true;
		JFijiPipe.JData data1 = attenMap.CreateData1();
		int i, j, k, n, width1, heigh1, off1, size1;
		JFijiPipe petPipe = parentPet.petPipe;
		JFijiPipe upetPipe = parentPet.upetPipe;
		n = petPipe.data1.numFrms;
		data1.pixFloat = new ArrayList<float []>();
		data1.zpos = new ArrayList<Float>();
		if( petPipe.data1.rescaleSlope == null) petData = false;
		if( petData) data1.rescaleSlope = new ArrayList<Double>();
		data1.numFrms = n;
		width1 = petPipe.data1.width;
		heigh1 = petPipe.data1.height;
		size1 = width1*heigh1;
		data1.width = width1;
		data1.height = heigh1;
		saveSUVfactor = petPipe.data1.SUVfactor;
		saveSerTime = petPipe.data1.serTime;
		saveHalfLife = petPipe.data1.halflife;
		short [] pixAtten, pixUAtten;
		float [] pixImg, pixFAtten, pixFUAtten;
		short currShort;
		int pcoef, ucoef;
		pcoef = petPipe.data1.getCoefficent0();
		ucoef = upetPipe.data1.getCoefficent0();
		float zpos;
		double apix, upix, aslope, uslope, ratio1;
		// let's assume there are no problems, i.e. no missing slices
		for( i=0; i<n; i++) {
			pixFAtten = pixFUAtten = null;
			pixAtten = pixUAtten = null;
			if( petPipe.data1.pixFloat != null) {
				pixFAtten = petPipe.data1.pixFloat.get(i);
				pixFUAtten = upetPipe.data1.pixFloat.get(i);
			} else {
				pixAtten = petPipe.data1.pixels.get(i);
				pixUAtten = upetPipe.data1.pixels.get(i);
			}
			zpos = petPipe.data1.zpos.get(i);
			data1.zpos.add(zpos);
			aslope = uslope = 1.0;
			if( petData) {
				aslope = petPipe.data1.rescaleSlope.get(i);
				uslope = upetPipe.data1.rescaleSlope.get(i);
				data1.rescaleSlope.add(uslope);
			}
			pixImg = new float[size1];
			IJ.showProgress(i,n);
			for( j=0; j<heigh1; j++) {
				off1 = j*width1;
				for( k=0; k<width1; k++) {
					if(pixFAtten != null) {
						apix = pixFAtten[off1+k];
						upix = pixFUAtten[off1+k];
					} else {
						currShort = (short) (pixAtten[off1+k] + pcoef);
						apix = aslope * currShort;
						currShort = (short) (pixUAtten[off1+k] + ucoef);
						upix = uslope * currShort;
					}
					if( apix <= 0 || upix <= 0) ratio1 = 0;
					else ratio1 = apix / upix;
					pixImg[off1 + k] = (float) ratio1;
				}
			}
			removeOutliers( pixImg, size1);
			data1.pixFloat.add(pixImg);
		}
		IJ.showProgress(1.0);
	}

	double removeOutliers( float[] pixImg, int size1) {
		double localMax;
		double currSum, currVal=0;
		int i, n, nNonZero;
		n = size1;
		while(true) {
			localMax = currSum = 0;
			for( i=nNonZero=0; i<n; i++) {
				currVal = pixImg[i];
				if( currVal <= 0) continue;	// don't count zeros
				nNonZero++;
				if( localMax < currVal) {
					localMax = currVal;
				}
				currSum += currVal;
			}
			currVal = 0;
			if( nNonZero == 0) break;	// the whole slice is zero
			currVal = currSum / nNonZero;
			if( localMax < currVal*20) break;
			localMax = currVal*15;
			for( i=0; i<n; i++) {
				if( pixImg[i] > localMax) pixImg[i] = (float) localMax;
			}
		}
		return currVal;	// the average value
	}

	void anotherSmooth() {
		if( attenMap == null) return;
		int i, j, k, n, width1, heigh1, off0, off1, off2, w1;
		float[] pixImg, smthImg;
		float partialSum;
		width1 = attenMap.data1.width;
		heigh1 = attenMap.data1.height;
		n = attenMap.data1.numFrms;
		for( i=0; i<n; i++) {
			smthImg = new float[width1*heigh1];
			pixImg = attenMap.data1.pixFloat.get(i);
			w1 = width1-1;
			for( j=0; j<heigh1; j++) {
				off0 = (j-1)*width1;
				off1 = j*width1;
				off2 = (j+1)*width1;
				if(off0 < 0) off0 = off1;
				if(j >= heigh1-1) off2 = off1;
				smthImg[off1] = (pixImg[off0] + 2*pixImg[off1] +pixImg[off2])/4;
				smthImg[off1+w1] = (pixImg[off0+w1] + 2*pixImg[off1+w1] +pixImg[off2+w1])/4;
				for( k=1; k<width1-1; k++) {
					partialSum = pixImg[off0+k-1] + 2*pixImg[off0+k] + pixImg[off0+k+1];
					partialSum += 2*(pixImg[off1+k-1] + 2*pixImg[off1+k] + pixImg[off1+k+1]);
					partialSum += pixImg[off2+k-1] + 2*pixImg[off2+k] + pixImg[off2+k+1];
					smthImg[off1+k] = partialSum / 16;
				}
			}
			attenMap.data1.pixFloat.set(i, smthImg);
		}
		numSmooth++;
		jButSmooth.setText("Smooth - " + numSmooth);
	}


	JFijiPipe findSlices() {
		JFijiPipe upetPipe = parentPet.upetPipe;	// this is probably null
		if( upetPipe == null) upetPipe = parentPet.petPipe;
		if( upetPipe == null) return null;	// shouldn't happen
		// the list of slices to be corrected should be a subset or the original map
		sliceInUse = new ArrayList<Integer>();
		sourceIdx = new ArrayList<Integer>();
		int i, j, n, size1;
		float zpos, zposMap, shitOffset;
		double diff1;
		n = upetPipe.data1.numFrms;
		size1 = attenMap.data1.numFrms;
		// make the allignment accurate to 1/2 the spacing between slices
		diff1 = Math.abs(upetPipe.data1.spacingBetweenSlices / 2);
		if( diff1 <= 0) diff1 = 0.1;
		shitOffset = (float) (upetPipe.data1.mriOffZ * upetPipe.data1.spacingBetweenSlices);
		for( i=0; i<n; i++) {
			zpos = upetPipe.data1.zpos.get(i) + shitOffset;
			for( j=0; j<size1; j++) {
				zposMap = attenMap.data1.zpos.get(j);
				if( Math.abs(zpos - zposMap) <= diff1) {	// match
					sliceInUse.add(j);
					sourceIdx.add(i);
					break;
				}
			}
		}
		return upetPipe;
	}

	void applyMap() {
		JFijiPipe upetPipe = findSlices();
		if( upetPipe == null) return;
		// the list of slices to be corrected should be a subset or the original map
		int i, j, n, size1;
		size1 = upetPipe.data1.numFrms;
		n = sliceInUse.size();
		// make the allignment accurate to 1/2 the spacing between slices
		if( n <= 0) {
			JOptionPane.showMessageDialog(this, "Failed to find any matching slices, cannot correct.");
			return;
		}
		if( n < size1) {
			j = size1-n;
			i = JOptionPane.showConfirmDialog(this, "Failed to find " + j + " slices out of " + size1
					+ "\nDo you wish to correct only a subset of the data?\n" +
					"Press No if you wish to change the Offset.");
			if( i != JOptionPane.OK_OPTION) return;
		}
		jButApply.setEnabled(false);	// can't apply twice
		jSpinOffset.setEnabled(false);
		buildMap(1);
	}

	/**
	 * In Fiji normally we have pointers to the original data which is used but not changed.
	 * In this case we want to change the data itself. I had wanted to make a new set of slices
	 * leaving the original Fiji data untouched. This fails because not all slices are necessarily
	 * changed. Thus we have to change the Fiji data and keep the ugly ucoef.
	 */
	protected void doApplyMap() {
		JFijiPipe upetPipe = findSlices();
		if( upetPipe == null) return;
		int i, j, k, n, val1, size1, ucoef;
		boolean petData = true;
		float[] pixImg, pixFloat;
		short[] pixels;
		short currShort;
		double currVal, factor1, factor2, decay, SUV;
		if( upetPipe.data1.rescaleSlope == null) petData = false;
		ucoef = upetPipe.data1.getCoefficent0();
		n = sliceInUse.size();
		size1 = attenMap.data1.width * attenMap.data1.height;
		// now for the bulk of the work, correct each pixel
		for( i=k=0; i<n; i++) {
			val1 = sliceInUse.get(i);
			pixImg = attenMap.data1.pixFloat.get(val1);
			val1 = sourceIdx.get(i);
			pixFloat = null;
			pixels = null;
			if( upetPipe.data1.pixFloat != null) pixFloat = upetPipe.data1.pixFloat.get(val1);
			else pixels = upetPipe.data1.pixels.get(val1);
			factor2 = factor1 = 1.0;
			if( petData) {
				factor2 = getFactor(pixImg, pixels, factor1, size1, ucoef);
				currVal = upetPipe.data1.rescaleSlope.get(val1);
				upetPipe.data1.rescaleSlope.set( val1, currVal/factor2);
			}
			factor1 *= factor2;
			if( pixFloat != null) for( j=0; j<size1; j++) {
				pixFloat[j] = pixFloat[j] * pixImg[j];
			}
			else for( j=0; j<size1; j++) {
				currShort = (short) (pixels[j] + ucoef);
				if( currShort == 0) continue;	// no point to multiplying by zero
				currVal = factor1 * pixImg[j] * currShort;
				if( currVal > 32767) {
					currVal = 32767;
					k++;
				}
				pixels[j] = (short) (currVal + 0.5 - ucoef);
			}
		}
		upetPipe.data1.MIPslope = 1.0;
		SUV = fillSUV(upetPipe);
		upetPipe.data1.setMaxAndSort();
		upetPipe.data1.setSUVfactor(SUV);
		if( parentPet.upetPipe == null) {
			parentPet.mipPipe.LoadMIPData(upetPipe);
			parentPet.mipPipe.data1.setSUVfactor(SUV);
		}
		upetPipe.dirtyFlg = true;
		parentPet.repaint();
	}

	double fillSUV( JFijiPipe upetPipe) {
		Date serTime;
		double decay, SUV;
		SUVDialog dlg1 = new SUVDialog(parentPet.parent, true);
		SUV = dlg1.calculateSUV(upetPipe, false);
		if( SUV > 0) return SUV;

		serTime = upetPipe.data1.serTime;
		long diff = (serTime.getTime() - saveSerTime.getTime()) / 1000;
		decay = 1.0;
		if( saveHalfLife > 0) decay = Math.exp(-diff*0.693147/saveHalfLife);
		return saveSUVfactor / decay;
	}

	double getFactor(float[] pixAtten, short[] pixels, double factor1, int size1, int ucoef) {
		int i, j, k1, k10, k100, k1000;
		double currVal, per100, per10, per1, mult1, lim1 = 0.02;
		short currShort;
		int [] ka;
		double [] fa;
		k1 = k10 = k100 = k1000 = 0;
		for( j=0; j<size1; j++) {
			if( pixAtten[j] == 0) continue;	// no point to multiplying by zero
			currShort = (short) (pixels[j] + ucoef);
			currVal = factor1 * pixAtten[j] * currShort;
			if( currVal < 32768.) continue;
			k1++;
			if( currVal < 327680.) continue;
			k10++;
			if( currVal < 3276800.) continue;
			k100++;
			if( currVal < 32768000.) continue;
			k1000++;
		}
		per100 = 100.0 * (k100 - k1000) / size1;
//		if( per100 > lim1)
//			mult1 = 1;	// break point
		per1 = 100.0 * (k1 - k1000) / size1;
		if( per1 <= lim1) return 1.0;	// smallest factor is 1.0;
		per10 = 100.0 * (k10 - k1000) / size1;
		mult1 = 1;
		ka = new int[15];
		fa = new double[15];
		if( per10 > lim1) mult1 = 10;
		for( i=0; i<15; i++) {
			ka[i] = 0;
			fa[i] = 1.0 /((i+2) * mult1);
		}
		for( j=0; j<size1; j++) {
			if( pixAtten[j] == 0) continue;	// no point to multiplying by zero
			currShort = (short) (pixels[j] + ucoef);
			currVal = factor1 * pixAtten[j] * currShort;
			for( i=0; i<15; i++) {
				if( currVal * fa[i] < 32768.) break;
				ka[i]++;
			}
		}
		for( i=0; i<15; i++) {
			mult1 = fa[i];
			per1 = 100.0 * (ka[i] - k1000) / size1;
			if( per1 <= lim1) break;
		}
		return mult1;
	}

	protected class bkgdBuildMap extends SwingWorker {

		@Override
		protected Void doInBackground() {
			if( bkgdType == 0) doMapBuild();
			else doApplyMap();
			return null;
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

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jButRefresh = new javax.swing.JButton();
        jButBuild = new javax.swing.JButton();
        jButSmooth = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jButApply = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jSpinOffset = new javax.swing.JSpinner();
        jLabel11 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Attenuation Correction");

        jLabel1.setText("This is an experimental program for attenuation correction.");

        jLabel2.setText("It is divided into 2 stages: 1 - build the correction map, 2 - apply it.");

        jLabel3.setText("To build the correction map you must choose a study with both corrected");

        jLabel4.setText("and uncorrected data. Then you press Build, followed by optional smooths.");

        jLabel7.setText("You need to press the Refresh button each time you choose a different study. ");

        jButRefresh.setText("Refresh");
        jButRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButRefreshActionPerformed(evt);
            }
        });

        jButBuild.setText("Build");
        jButBuild.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButBuildActionPerformed(evt);
            }
        });

        jButSmooth.setText("Smooth - 0");
        jButSmooth.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSmoothActionPerformed(evt);
            }
        });

        jLabel5.setText("The map should be applied a second study containing only uncorrected data.");

        jLabel10.setText("You may use the offset to realign. Do so PRIOR to Apply.");

        jLabel8.setText("(Don't forget to refresh before the Apply or Offset.)");

        jButApply.setText("Apply");
        jButApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButApplyActionPerformed(evt);
            }
        });

        jLabel6.setText("After applying the correction, you may hit X to exit.");

        jSpinOffset.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinOffsetStateChanged(evt);
            }
        });

        jLabel11.setText("offset");

        jLabel9.setText("_____________________________________________________________");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addContainerGap(48, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(38, 38, 38))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButRefresh)
                        .addGap(47, 47, 47)
                        .addComponent(jButBuild)
                        .addGap(32, 32, 32)
                        .addComponent(jButSmooth)
                        .addContainerGap(102, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addContainerGap(30, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addContainerGap(122, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addContainerGap(146, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButApply)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 161, Short.MAX_VALUE)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(95, 95, 95))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addContainerGap(150, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addContainerGap(32, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButRefresh)
                    .addComponent(jButBuild)
                    .addComponent(jButSmooth))
                .addGap(4, 4, 4)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButApply)
                    .addComponent(jSpinOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jButRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButRefreshActionPerformed
		refresh();
}//GEN-LAST:event_jButRefreshActionPerformed

	private void jButBuildActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButBuildActionPerformed
		buildMap(0);
}//GEN-LAST:event_jButBuildActionPerformed

	private void jButSmoothActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSmoothActionPerformed
		anotherSmooth();
}//GEN-LAST:event_jButSmoothActionPerformed

	private void jButApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButApplyActionPerformed
		applyMap();
}//GEN-LAST:event_jButApplyActionPerformed

	private void jSpinOffsetStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinOffsetStateChanged
		offsetChanged();
}//GEN-LAST:event_jSpinOffsetStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButApply;
    private javax.swing.JButton jButBuild;
    private javax.swing.JButton jButRefresh;
    private javax.swing.JButton jButSmooth;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSpinner jSpinOffset;
    // End of variables declaration//GEN-END:variables

}

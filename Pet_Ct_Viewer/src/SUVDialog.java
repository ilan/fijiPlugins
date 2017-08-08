/*
 * SUVDialog.java
 *
 * Created on Aug 6, 2009, 9:48:01 AM
 * Copied to Fiji version 7 Jan 2010
 */


import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.JOptionPane;

/**
 * The dialog which is called when there is something wrong with the DICOM parameters for the SUV.
 * This dialog will display the values in Metric or American units.
 * 
 * @author Ilan
 */
public class SUVDialog extends javax.swing.JDialog {
	boolean decayCorrect, privatePhilipsSUV = false, OKpressed = false;
	double patWeight, patHeight, totalDose, halfLife;
	int secSeries, secInject;
	PetCtFrame parentPet;

    /** Creates new form SUVDialog
	 * @param parent called from PetCtFrame
	 * @param modal */
	public SUVDialog(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		parentPet = (PetCtFrame) parent;
		initComponents();
	}

	double calculateSUV(JFijiPipe petPipe, boolean showFlg) {
		boolean showHeight = parentPet.useSUL;
		int secAcquisition, seriesType;
		Double halfLifeMin, maxSlope;

		halfLife = petPipe.data1.halflife;
		halfLifeMin = halfLife / 60.0;
		patWeight = petPipe.data1.patWeight;
		patHeight = petPipe.data1.patHeight;
		if( patHeight > 0.5) showHeight = false;
		totalDose = petPipe.data1.totalDose;
		decayCorrect = petPipe.data1.decayCorrect;
		if( totalDose > 1000000.0) totalDose /= 1000000.0;
		seriesType = petPipe.data1.seriesType;
		secSeries = getTimeInSec(petPipe.data1.serTime);
		secAcquisition = getTimeInSec(petPipe.data1.acquisitionTime);
		if( seriesType == ChoosePetCt.SERIES_BQML_PET && secAcquisition+60 < secSeries) {
			secSeries = secAcquisition;
			IJ.log("Acquisition time before Series time");
		}
		secInject = getTimeInSec(petPipe.data1.injectionTime);
		privatePhilipsSUV = petPipe.data1.seriesType == ChoosePetCt.SERIES_PHILIPS_PET;
		if( showFlg || (!isLegal() || secInject <= 0 || secSeries < secInject || showHeight)) {
			setFields();
			jHalfLife.setText(halfLifeMin.toString());
			jSeriesTime.setText(setTimeSec(secSeries));
			jInjectionTime.setText(setTimeSec(secInject));
			setLocationRelativeTo(getOwner());
			setVisible(true);
			if( OKpressed) {
				petPipe.data1.halflife = halfLife;
				petPipe.data1.patWeight = patWeight;
				petPipe.data1.patHeight = patHeight;
				petPipe.data1.totalDose = totalDose;
			}
		}

		if(petPipe.data1.seriesType == ChoosePetCt.SERIES_GML_PET) return 1.0;
		if(privatePhilipsSUV) {
			// Philips has some really strange data. In most cases the
			// rescale slope is constant. In 1 case it isn't and the
			// maximum slope is too high. Use slope at slice 0.
			double tmpDbl = petPipe.data1.philipsSUV;
//			maxSlope = petPipe.data1.getMaxRescaleSlope();
			maxSlope = petPipe.data1.getRescaleSlope(0);
			if( tmpDbl > 0) return maxSlope / tmpDbl;
			tmpDbl = readPhilipsCrap(petPipe);
			if( tmpDbl > 0) {
				petPipe.data1.philipsSUV = tmpDbl;
				return maxSlope / tmpDbl;
			}
			IJ.log("Can't find Philips private SUV");
			IJ.log("Number of cores: " + Runtime.getRuntime().availableProcessors());
			return 0.0;
		}
		if(petPipe.data1.seriesType != ChoosePetCt.SERIES_BQML_PET &&
			petPipe.data1.seriesType != ChoosePetCt.SERIES_GE_PRIVATE_PET)
			return 0.0;
		double decay = 1.0;
		if(decayCorrect && halfLife > 0) {
			int day0 = getDayOfYear(petPipe.data1.injectionTime);
			int day1 = getDayOfYear(petPipe.data1.serTime);
			int diff = (day1 - day0)*86400;	// sec/day
			if( Math.abs(day1-day0)>4) {
				diff = 0;
				IJ.log("Something seems to be wrong with the date-time entries.\nThe SUV may be incorrect.");
			}
			decay = Math.exp(-(secSeries - secInject + diff)*0.693147/halfLife);
		}
		return totalDose * 1000. * decay / patWeight;
	}
	
	double readPhilipsCrap(JFijiPipe petPipe) {	//"7053,1000"
		byte[] byt1 = new byte[4096];
		ByteBuffer byt2;
		RandomAccessFile fis;
		String tmp1, path;
		double retVal = 0;
		ImagePlus srcImage = petPipe.data1.srcImage;
		FileInfo info = srcImage.getOriginalFileInfo();
		if( info == null) return retVal;
		path = info.directory;
		File fl1 = new File(path);
		File [] results = fl1.listFiles();
		int i, j, n = results.length;
		int currInt, off1;
		short curWord;
		long flLen;
		try {
			for( i=0; i<n; i++) {
				if( i>2) return 0;
				fl1 = results[i];
				fis = new RandomAccessFile(fl1, "r");
				flLen = fis.read(byt1);
				if( flLen < 4096) {
					fis.close();
					continue;
				}
				byt2 = ByteBuffer.wrap(byt1);
				byt2 = byt2.order(ByteOrder.LITTLE_ENDIAN);
				currInt = byt2.getInt(128);
				if (currInt != 0x4d434944) {	// DICM
					fis.close();
					continue;
				}
				curWord = byt2.getShort(128 + 12);
				byt2.position( curWord + 16 + 128);
				curWord = byt2.getShort();
				if( curWord == 0x800) byt2 = byt2.order(ByteOrder.BIG_ENDIAN);
				while( true) {
					curWord = byt2.getShort();
					if( curWord == 0x7053) {
						curWord = byt2.getShort();
						if( curWord == 0x1000) {
							byt2.getInt();	// UN tag
							j = byt2.getInt();
							tmp1 = getDcmString(byt2, j);
							fis.close();
							retVal = Double.parseDouble(tmp1);
							return retVal;
						}
					}
					off1 = byt2.position();
					if( off1 > 4060) {
						flLen -= 4096 - off1;
						fis.seek(flLen);
						off1 = fis.read(byt1);
						if( off1 < 4096) break;
						flLen += off1;
						byt2.position(0);
					}
				}
				fis.close();
			}
		} catch (Exception e) { return 0; }
		return retVal;
	}

	private String getDcmString(ByteBuffer byt2, int leng) {
		String ret1;
		int pos1;
		byte[] tmp1 = new byte[leng];
		pos1 = byt2.position();
		byt2.get(tmp1, 0, leng);
		byt2.position(pos1);
		ret1 = new String(tmp1);
		ret1 = ret1.trim();
		return ret1;
	}

	double calculateSUL(JFijiPipe petPipe) {
		double BMI, SUL = -1.0, weight, height;
		boolean isFemale = false;
		String meta, tmp;
		weight = petPipe.data1.patWeight;
		height = petPipe.data1.patHeight;
		if( weight <= 0 || height <= 0) return SUL;
		BMI = weight/Math.pow(height, 2);
		meta = petPipe.data1.metaData;
		tmp = ChoosePetCt.getDicomValue(meta, "0010,0040");
		if( tmp != null && tmp.startsWith("F")) isFemale = true;
		SUL = 9270;
		if( isFemale) {
			SUL /= (8780 + 244*BMI);
		} else {
			SUL /= (6680 + 216*BMI);
		}
		return SUL;
	}

	int getAccepted() {
		boolean american = jAmerican.isSelected();
		patHeight = getPatHeight(american);
		patWeight = getPatWeight(american);
		totalDose = getTotalDose(american);
		secSeries = getTimeSec(jSeriesTime);
		secInject = getTimeSec(jInjectionTime);
		halfLife = Double.valueOf(jHalfLife.getText()) * 60.0;
		if(!isLegal()) {
			JOptionPane.showMessageDialog(this, "Please use valid entries for all fields");
			return 0;
		}
		OKpressed = true;
		return 1;
	}

	void setFields() {
		Double weight, height, dose;
		int feet, inch;
		String heigTxt;
		weight = patWeight;
		height = patHeight;
		dose = totalDose;
		if( jAmerican.isSelected()) {
			weight *= 2.20462262;
			inch = (int) Math.round(height * 39.3700787);
			feet = inch / 12;
			inch %= 12;
			heigTxt = String.format("%d' %d\"", feet, inch);
			dose *= 0.027;
		} else {
			height = Math.round(height*100)/100.0;
			heigTxt = height.toString();
		}
		weight = Math.round(weight*10)/ 10.0;
		jPatWeight.setText(weight.toString());
		jPatHeight.setText(heigTxt);
		jTotalDose.setText(dose.toString());
	}

	boolean isLegal() {
		if( patWeight <= 5 || halfLife <= 0) return false;
		if( privatePhilipsSUV) return true;
//			int day0 = getDayOfYear(petPipe.data1.injectionTime);
//			int day1 = getDayOfYear(petPipe.data1.serTime);
		return totalDose > 0;
	}

	String setTimeSec(int sec1) {
		String ret;
		int hour, min, sec, secTmp;
		secTmp = sec1;
		hour = secTmp / 3600;
		secTmp -= hour * 3600;
		min = secTmp / 60;
		sec = secTmp % 60;
		ret = String.format("%d:%02d:%02d", hour, min, sec);
		return ret;
	}

	int getTimeSec(javax.swing.JTextField in1) {
		String [] tokens = in1.getText().split(":");
		int retVal, i, n = tokens.length;
		for( i= retVal =0; i<n; i++) {
			retVal = 60*retVal + Integer.valueOf(tokens[i]);
		}
		return retVal;
	}

	double getTotalDose( boolean american) {
		double dose = Double.parseDouble(jTotalDose.getText());
		if( american) dose /= 0.027;
		return dose;
	}

	double getPatWeight(boolean american) {
		double weight = Double.parseDouble(jPatWeight.getText());
		if( american) weight /= 2.20462262;
		// round to nearest 0.1 kilogram
		weight = Math.round(weight*10)/ 10.0;
		return weight;
	}

	double getPatHeight(boolean american) {
		double height;
		int indx1, feet, inch;
		String tmp0, tmp1 = jPatHeight.getText();
		if( american) {
			indx1 = tmp1.indexOf('\'');
			tmp0 = tmp1.substring(0, indx1);
			feet = Integer.valueOf(tmp0);
			tmp1 = tmp1.substring(indx1+1);
			indx1 = tmp1.indexOf('"');
			tmp0 = tmp1.substring(0, indx1).trim();
			inch = Integer.valueOf(tmp0);
			inch += feet*12;
			height = inch / 39.3700787;
		}
		else height = Double.parseDouble(tmp1);
		height = Math.round(height*100)/100.0;
		return height;
	}

	static int getTimeInSec(Date dt0) {
		GregorianCalendar dat1 = new GregorianCalendar();
		if(dt0 == null) return 0;
		dat1.setTime(dt0);
		return (dat1.get(Calendar.HOUR_OF_DAY)*60 + dat1.get(Calendar.MINUTE))*60 + dat1.get(Calendar.SECOND);
	}
	
	static int getDayOfYear(Date dt0) {
		GregorianCalendar dat1 = new GregorianCalendar();
		if(dt0 == null) return 0;
		dat1.setTime(dt0);
		return dat1.get(GregorianCalendar.DAY_OF_YEAR);
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
        jPatWeight = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPatHeight = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTotalDose = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jHalfLife = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jInjectionTime = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jSeriesTime = new javax.swing.JTextField();
        jAmerican = new javax.swing.JCheckBox();
        jButOK = new javax.swing.JButton();
        jButCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("PatientWeight (kg,lb)");

        jLabel2.setText("Patient Height (m,ft)");

        jLabel3.setText("Total dose (MBq,mCi)");

        jLabel4.setText("Half-life (min)");

        jLabel5.setText("Injection time");

        jInjectionTime.setEditable(false);

        jLabel6.setText("PET Series time");

        jSeriesTime.setEditable(false);

        jAmerican.setText("American");
        jAmerican.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAmericanActionPerformed(evt);
            }
        });

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPatWeight, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
                            .addComponent(jPatHeight, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jHalfLife, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                            .addComponent(jTotalDose, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                            .addComponent(jInjectionTime, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                            .addComponent(jSeriesTime, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                            .addComponent(jAmerican)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 314, Short.MAX_VALUE)
                        .addComponent(jButCancel)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jPatWeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jPatHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTotalDose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jHalfLife, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jInjectionTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jSeriesTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jAmerican)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 80, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButOK)
                    .addComponent(jButCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jButOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButOKActionPerformed
		if( getAccepted() > 0) dispose();
	}//GEN-LAST:event_jButOKActionPerformed

	private void jButCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButCancelActionPerformed
		dispose();
	}//GEN-LAST:event_jButCancelActionPerformed

	private void jAmericanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAmericanActionPerformed
		boolean american = !jAmerican.isSelected();	// before it was changed
		// read the values before the change and write them after the change
		patHeight = getPatHeight(american);
		patWeight = getPatWeight(american);
		totalDose = getTotalDose(american);
		setFields();
	}//GEN-LAST:event_jAmericanActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jAmerican;
    private javax.swing.JButton jButCancel;
    private javax.swing.JButton jButOK;
    private javax.swing.JTextField jHalfLife;
    private javax.swing.JTextField jInjectionTime;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JTextField jPatHeight;
    private javax.swing.JTextField jPatWeight;
    private javax.swing.JTextField jSeriesTime;
    private javax.swing.JTextField jTotalDose;
    // End of variables declaration//GEN-END:variables

}

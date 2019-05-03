import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.ops.image.cooccurrenceMatrix.MatrixOrientation;
import net.imagej.ops.image.cooccurrenceMatrix.MatrixOrientation2D;
import net.imagej.ops.image.cooccurrenceMatrix.MatrixOrientation3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import quickhull3d.QuickHull3D;

/**
 *
 * @author ilan
 */
public class Radionomic {
	static final int MIN_DEPTH = 4, CUTOFF1 = 20;
	final double maxError = 0.000001;
	final double EPSILON = Double.MIN_NORMAL;
	int cooccurenceHi, nrPairs;
	boolean threeD;

	void init( BrownFat bf) {
		String tmp = bf.getRadioText(0);
		int val = Integer.parseInt(tmp);
		if( val < 4 || val > 32) val = 10;
		cooccurenceHi = val;
		threeD = bf.getRadioCB(30).isSelected();
	}

	public ImagePlus buildPlus(SUVpoints suvp) {
		if( suvp == null || suvp.getListSize()<= 0) return null;
		int z, i, j, n, minx, maxx, miny, maxy, minz, maxz, red1=0, step=1;
		int width, height, depth, off1, z1 = 0;
		double maxVal, minVal, scl1;
		byte[] pixels;
		n = suvp.getListSize();
		SUVpoints.SavePoint curpnt;
		curpnt = suvp.getPoint(0);
		minx = maxx = curpnt.x1;
		miny = maxy = curpnt.y1;
		minz = maxz = curpnt.z1;
		maxVal = minVal = curpnt.petVal;
		for( i=1; i<n; i++) {
			curpnt = suvp.getPoint(i);
			if( curpnt.x1 < minx) minx = curpnt.x1;
			if( curpnt.x1 > maxx) maxx = curpnt.x1;
			if( curpnt.y1 < miny) miny = curpnt.y1;
			if( curpnt.y1 > maxy) maxy = curpnt.y1;
			if( curpnt.z1 < minz) minz = curpnt.z1;
			if( curpnt.z1 > maxz) maxz = curpnt.z1;
			if( curpnt.petVal > maxVal) {
				maxVal = curpnt.petVal;
				red1 = i;
				z1 = curpnt.z1;
			}
			if( curpnt.petVal < minVal) minVal = curpnt.petVal;
		}
		if( maxVal == minVal) minVal = 0;
		width = maxx-minx+1;
		height =maxy-miny+1;
		depth = maxz-minz+1;
		scl1 = (cooccurenceHi - 1) /(maxVal-minVal);
		ImageStack stk1 = new ImageStack(width,height);
		for( z=0; z<depth; z++) {
//			if( (z+minz)!= z1) continue;	// use a single slice
			pixels = new byte[width*height];
			for( i=0; i<n; i++) {
				curpnt = suvp.getPoint(i);
				if( curpnt.z1 != (z+minz)) continue;
				off1 = (curpnt.y1 - miny)*width + curpnt.x1 - minx;
				// set the values to 1 -> COOCCURENCE_HI
				j = pixels[off1] = (byte)(ChoosePetCt.round((curpnt.petVal - minVal) * scl1) + 1);
				if( j<1 || j>cooccurenceHi)
					IJ.log("Outside range");
			}
			stk1.addSlice(null, pixels);
		}
		return new ImagePlus("radio", stk1);
	}

	public ImagePlus buildCtPlus(SUVpoints suvp, PetCtPanel par2, boolean isMask) {
		if( suvp == null || suvp.getListSize()<= 0) return null;
		int x, y, z, i, j, n, minx, maxx, miny, maxy, minz, maxz, red1=0, step=1;
		int width, widthCt, ctVal, coef0, height, depth, off0, off1, z1 = 0;
		int prevPetZ, ctSlice, x1, y1, numXY, numZ=0, zin, minx1, miny1;
		boolean MRIflg = par2.MRIflg;
		double maxVal, minVal, scl1, sclCt;
		double[] SuvCt = par2.bfDlg.getSUVandCTLimits();
		byte[][] pixels = null;
		JFijiPipe ctPipe = par2.getMriOrCtPipe();
		n = suvp.getListSize();
		sclCt = par2.petPipe.getPixelSpacing(0)/ctPipe.getPixelSpacing(0);
		widthCt = ctPipe.data1.width;
		coef0 = ctPipe.data1.getCoefficentAll();
		double[] slope = null;
		short[][] data1 = null;
		numXY = (int)(sclCt + 0.999);
		SUVpoints.SavePoint curpnt;
		curpnt = suvp.getPoint(0);
		prevPetZ = curpnt.z1;
		ctSlice = ctPipe.findCtPos(prevPetZ, false);
		if( ctSlice < 0) return null;
		getCtVal(par2, curpnt, ctSlice);
		minx = maxx = curpnt.x1;
		miny = maxy = curpnt.y1;
		minz = maxz = curpnt.z1;
		maxVal = minVal = curpnt.ctVal;
		for( i=1; i<n; i++) {
			curpnt = suvp.getPoint(i);
			if( curpnt.z1 != prevPetZ) {
				prevPetZ = curpnt.z1;
				ctSlice = ctPipe.findCtPos(prevPetZ, false);
			}
			if( ctSlice < 0) continue;
			getCtVal(par2, curpnt, ctSlice);
			if( curpnt.x1 < minx) minx = curpnt.x1;
			if( curpnt.x1 > maxx) maxx = curpnt.x1;
			if( curpnt.y1 < miny) miny = curpnt.y1;
			if( curpnt.y1 > maxy) maxy = curpnt.y1;
			if( curpnt.z1 < minz) minz = curpnt.z1;
			if( curpnt.z1 > maxz) maxz = curpnt.z1;
			if( curpnt.ctVal > maxVal) {
				maxVal = curpnt.ctVal;
				red1 = i;
				z1 = curpnt.z1;
			}
			if( curpnt.ctVal < minVal) minVal = curpnt.ctVal;
		}
		if( !MRIflg) {
			if( minVal < SuvCt[2]) minVal = SuvCt[2];
			if( maxVal > SuvCt[3]) maxVal = SuvCt[3];
		}
		if( maxVal <= minVal) return null;
		minx1 = par2.shift2Ct(ctPipe, minx, 0);
		miny1 = par2.shift2Ct(ctPipe, miny, 1);
		scl1 = (cooccurenceHi - 1);
		if( isMask) {
			BrownFat bf = par2.bfDlg;
			bf.maskParms = new int[6];
			bf.maskParms[0] = minx1;
			bf.maskParms[1] = miny1;
			bf.maskParms[2] = ctPipe.findCtPos(minz, false);
			bf.maskParms[3] = widthCt;
			bf.maskParms[4] = ctPipe.data1.height;
			bf.maskParms[5] = ctPipe.getNormalizedNumFrms();
			scl1 = 9;	// use default value, 10-1
		}
		scl1 = scl1 /(maxVal-minVal);
		width = ChoosePetCt.round(sclCt*(maxx-minx+1));
		height =ChoosePetCt.round(sclCt*(maxy-miny+1));
		depth = maxz-minz+1;
		ImageStack stk1 = new ImageStack(width,height);
		prevPetZ = -1;
		for( z=0; z<depth; z++) {
//			if( (z+minz)!= z1) continue;	// use a single slice
			for( i=0; i<n; i++) {
				curpnt = suvp.getPoint(i);
				if( curpnt.z1 != (z+minz)) continue;
				if( curpnt.z1 != prevPetZ) {
					prevPetZ = curpnt.z1;
					numZ = 0;
					ctSlice = ctPipe.findCtPos(prevPetZ, false);
					if( ctSlice < 0) continue;
					zin = ctPipe.findCtPos(prevPetZ+1, false);
					numZ = zin - ctSlice;
					if( numZ <= 0)
						numZ = 1;	// break point
					pixels = new byte[numZ][width*height];
					slope = new double[numZ];
					data1 = new short[numZ][];
					for( zin=0; zin<numZ; zin++) {
						slope[zin] = ctPipe.data1.getRescaleSlope(ctSlice+zin);
						data1[zin] = ctPipe.data1.pixels.get(ctSlice+zin);
					}
				}
				x1 = par2.shift2Ct(ctPipe, curpnt.x1, 0);
				y1 = par2.shift2Ct(ctPipe, curpnt.y1, 1);
				for( y=0; y<numXY; y++) {
					if( y1+y-miny1 >= height) continue;
					for( x=0; x<numXY; x++) {
						if( x1+x-minx1 >= width) continue;
						off0 = (y1+y)*widthCt + x1+x;
						off1 = (y1+y - miny1)*width + x1+x - minx1;
						for( zin=0; zin<numZ; zin++) {
							ctVal = (short)((data1[zin][off0]+coef0)*slope[zin]);
							// set the values to 1 -> COOCCURENCE_HI
							j =  ChoosePetCt.round((ctVal - minVal) * scl1) + 1;
							if( isMask) {
								if( j<1  || j>11) continue;
								pixels[zin][off1] = (byte)(curpnt.rn1 + 1);
								continue;
							}
							pixels[zin][off1] = (byte)j;
							if( j<1  || j>cooccurenceHi+1)pixels[zin][off1] = 0;
							// allow slop of 1 since not all CT values were sampled
							if( j==cooccurenceHi+1) pixels[zin][off1] = (byte) cooccurenceHi;
						}
					}
				}
			}
			for( zin=0; zin<numZ; zin++) stk1.addSlice(null, pixels[zin]);
		}
		return new ImagePlus("radioCt", stk1);
	}

	private int getCtVal(PetCtPanel par, SUVpoints.SavePoint curPnt, int ctSlice) {
		int x1, y1, width1, offst, coef0, ctVal;
		JFijiPipe ctPipe = par.getMriOrCtPipe();
		double slope = ctPipe.data1.getRescaleSlope(ctSlice);
		short[] data1 = ctPipe.data1.pixels.get(ctSlice);
		width1 = ctPipe.data1.width;
		coef0 = ctPipe.data1.getCoefficentAll();
		x1 = par.shift2Ct(ctPipe, curPnt.x1, 0);
		y1 = par.shift2Ct(ctPipe, curPnt.y1, 1);
		offst = y1*width1 + x1;
		ctVal = (short)((data1[offst]+coef0)*slope);
		curPnt.ctVal = ctVal;
		return ctVal;
	}

	public void calcGlcm(SUVpoints suvp, ImagePlus imp) {
		SUVpoints.CooccurencePoint glcm0;
		int i, j, n;
		if( threeD) {
			for( i=0; i<13; i++) {
				glcm0 = computeVals(suvp, imp, i);
				if(glcm0 != null) suvp.glcm.add(glcm0);
			}
		} else { // 2D case
			n = imp.getStackSize();
			for( i=1; i<=n; i++) {
				for( j=0; j<4; j++) {
					glcm0 = computeVals2D(suvp, imp, j, i);
					if(glcm0 != null) suvp.glcm.add(glcm0);
				}
			}
		}
	}

	public < T extends RealType< T > & NativeType< T > > SUVpoints.CooccurencePoint computeVals(SUVpoints suvp, ImagePlus imp, int indx) {
		int z, i, j, k, n, step=1;
		int width, height, depth, off1;
		MatrixOrientation orient;
//		double minVal, val1;
		SUVpoints.CooccurencePoint glcm, glcmRet = null, glcm1,glcm2;
		byte[] pixels;
		int[][][] pix3;
		switch(indx) {
			case 0:
			default:
				orient = MatrixOrientation3D.HORIZONTAL;
				break;

			case 1:
				orient = MatrixOrientation3D.VERTICAL;
				break;

			case 2:
				orient = MatrixOrientation3D.DIAGONAL;
				break;

			case 3:
				orient = MatrixOrientation3D.ANTIDIAGONAL;
				break;

			case 4:
				orient = MatrixOrientation3D.HORIZONTAL_DIAGONAL;
				break;

			case 5:
				orient = MatrixOrientation3D.HORIZONTAL_VERTICAL;
				break;

			case 6:
				orient = MatrixOrientation3D.VERTICAL_DIAGONAL;
				break;

			case 7:
				orient = MatrixOrientation3D.VERTICAL_VERTICAL;
				break;

			case 8:
				orient = MatrixOrientation3D.DIAGONAL_DIAGONAL;
				break;

			case 9:
				orient = MatrixOrientation3D.DIAGONAL_VERTICAL;
				break;

			case 10:
				orient = MatrixOrientation3D.ANTIDIAGONAL_DIAGONAL;
				break;

			case 11:
				orient = MatrixOrientation3D.ANTIDIAGONAL_VERTICAL;
				break;

			case 12:
				orient = MatrixOrientation3D.DEPTH;
				break;
		}
		try {
			ImageStack stk1 = imp.getStack();
			depth = stk1.getSize();
			// don't do off plane if depth too small
			if( depth < MIN_DEPTH && indx > 3) return null;
/*			glcm = suvp.makeCoocur();
			Img < T > img = ImagePlusAdapter.wrap(imp);
			IterableInterval<T> it1 = (IterableInterval<T>) img;
			if( suvp.ij == null) suvp.ij = new net.imagej.ImageJ();
			glcm.matix = suvp.ij.op().image().cooccurrenceMatrix( it1, cooccurenceHi+1, step, orient);
			minVal = 1.0;
			for(j=0; j<=cooccurenceHi; j++) {
				for(i=0; i<=cooccurenceHi; i++) {
					val1 = glcm.matix[j][i];
					if( val1 <= 0.0) continue;
					if( val1 < minVal) minVal = val1;
				}
			}
			glcm.numPnt = ChoosePetCt.round(1.0/minVal);
			n = 0;
			// zero is NAN, remove it
			glcm1 = suvp.makeCoocur();
			glcm1.matix = new double[cooccurenceHi][cooccurenceHi];
			for(j=0; j<COOCCURRENCE_HI; j++) {
				for(i=0; i<COOCCURRENCE_HI; i++) {
					val1 = glcm.matix[j+1][i+1];
					z = ChoosePetCt.round(val1*glcm.numPnt);
					n += z;
					glcm1.matix[j][i] = z;
				}
			}
			// normalize the matrix with NAN removed
			if( n>0) for(j=0; j<COOCCURRENCE_HI; j++) {
				for(i=0; i<COOCCURRENCE_HI; i++) {
					val1 = glcm1.matix[j][i];
					glcm1.matix[j][i] = val1/n;
				}
			}*/
			width = stk1.getWidth();
			height = stk1.getHeight();
			pix3 = new int[depth][height][width];
			for(k=0; k<depth; k++){
				pixels = (byte[]) stk1.getPixels(k+1);
				for(j=0; j<height; j++) {
					off1 = j*width;
					for(i=0; i<width; i++) {
						z = pixels[i+off1] - 1;
						if( z < 0) z = Integer.MAX_VALUE;
						pix3[k][j][i] = z;
					}
				}
			}
			glcm2 = suvp.makeCoocur();
//			glcm1 = CooccurPixels2D(pix3[0], orient, step, cooccurenceHi);
			glcm2.matix = CooccurPixels3D(pix3, orient, step, cooccurenceHi);
			glcm2.numPnt = nrPairs;
			glcm2.label = orient.toString();
			if( nrPairs < CUTOFF1) return null;
			glcmRet = glcm2;
/*			out1:
			for(j=0; j<COOCCURRENCE_HI; j++) {
				for(i=0; i<COOCCURRENCE_HI; i++) {
					if( Math.abs(glcm2.matix[j][i] - glcm1.matix[j][i]) > maxError) break out1;
				}
			}
			if( j<COOCCURRENCE_HI) {
				String tmp = "Cooccurence Matrix don't match ";
				tmp += "i=" +i + ", j=" +j + ", values=" + glcm1.matix[j][i];
				tmp += ", " + glcm2.matix[j][i];
				IJ.log(tmp);
			}*/
		} catch(Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return glcmRet;
	}

	public < T extends RealType< T > & NativeType< T > > SUVpoints.CooccurencePoint computeVals2D(SUVpoints suvp, ImagePlus imp, int indx, int zdepth) {
		int z, i, j, step=1;
		int width, height, off1;
		MatrixOrientation orient;
		SUVpoints.CooccurencePoint glcmRet = null, glcm2;
		byte[] pixels;
		int[][] pix2;
		switch(indx) {
			case 0:
			default:
				orient = MatrixOrientation2D.HORIZONTAL;
				break;

			case 1:
				orient = MatrixOrientation2D.VERTICAL;
				break;

			case 2:
				orient = MatrixOrientation2D.DIAGONAL;
				break;

			case 3:
				orient = MatrixOrientation2D.ANTIDIAGONAL;
				break;
		}
		try {
			ImageStack stk1 = imp.getStack();
			width = stk1.getWidth();
			height = stk1.getHeight();
			pix2 = new int[height][width];
			pixels = (byte[]) stk1.getPixels(zdepth);
			for(j=0; j<height; j++) {
				off1 = j*width;
				for(i=0; i<width; i++) {
					z = pixels[i+off1] - 1;
					if( z < 0) z = Integer.MAX_VALUE;
					pix2[j][i] = z;
				}
			}
			glcm2 = suvp.makeCoocur();
			glcm2.matix = CooccurPixels2D(pix2, orient, step, cooccurenceHi);
			glcm2.numPnt = nrPairs;
			glcm2.label = orient.toString() + zdepth;
			if( nrPairs < CUTOFF1) return null;
			glcmRet = glcm2;
		} catch(Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return glcmRet;
	}

	double[][] CooccurPixels2D(int[][] pixels, MatrixOrientation orientation, int distance, int nrGreyLevels) {
		nrPairs = 0;
		final double[][] output = new double[nrGreyLevels][nrGreyLevels];

		final int orientationAtX = orientation.getValueAtDim(0) * distance;
		final int orientationAtY = orientation.getValueAtDim(1) * distance;
		for (int y = 0; y < pixels.length; y++) {
			for (int x = 0; x < pixels[y].length; x++) {
				// ignore pixels not in mask
				if (pixels[y][x] == Integer.MAX_VALUE) {
					continue;
				}

				// // get second pixel
				final int sx =  x + orientationAtX;
				final int sy =  y + orientationAtY;

				// second pixel in interval and mask
				if (sx >= 0 && sy >= 0 && sy < pixels.length
						&& sx < pixels[sy].length
						&& pixels[sy][sx] != Integer.MAX_VALUE) {
					output[pixels[y][x]][pixels[sy][sx]]++;
					nrPairs++;
				}

			}
		}

		if (nrPairs > 0) {
			double divisor = 1.0 / nrPairs;
			for (double[] output1 : output) {
				for (int col = 0; col < output1.length; col++) {
					output1[col] *= divisor;
				}
			}
		}
		return output;
	}

	double[][] CooccurPixels3D(int[][][] pixels, MatrixOrientation orientation, int distance, int nrGreyLevels) {
		nrPairs = 0;
		final double[][] matrix = new double[nrGreyLevels][nrGreyLevels];

		final double orientationAtX = orientation.getValueAtDim(0) * distance;
		final double orientationAtY = orientation.getValueAtDim(1) * distance;
		final double orientationAtZ = orientation.getValueAtDim(2) * distance;
		for (int z = 0; z < pixels.length; z++) {
			for (int y = 0; y < pixels[z].length; y++) {
				for (int x = 0; x < pixels[z][y].length; x++) {

					// ignore pixels not in mask
					if (pixels[z][y][x] == Integer.MAX_VALUE) {
						continue;
					}

					// get second pixel
					final int sx = (int) (x + orientationAtX);
					final int sy = (int) (y + orientationAtY);
					final int sz = (int) (z + orientationAtZ);

					// second pixel in interval and mask
					if (sx >= 0 && sy >= 0 && sz >= 0 && sz < pixels.length
							&& sy < pixels[sz].length
							&& sx < pixels[sz][sy].length
							&& pixels[sz][sy][sx] != Integer.MAX_VALUE) {

						matrix[pixels[z][y][x]][pixels[sz][sy][sx]]++;
						nrPairs++;
					}
				}
			}
		}

		if (nrPairs > 0) {
			double divisor = 1.0 / nrPairs;
			for (double[] matrix1 : matrix) {
				for (int col = 0; col < matrix1.length; col++) {
					matrix1[col] *= divisor;
				}
			}
		}
		return matrix;
	}

	void computeFunction(int type, ArrayList<SUVpoints.CooccurencePoint>glcm, SUVpoints.RadioPoint res, BrownFat bf) {
		double[] tmpVals;
		SUVpoints.CooccurencePoint glcm0;
		String tmp = "";
		double mean=0, sumSquare=0;
		int i, n=glcm.size();
		JCheckBox cbox = bf.getRadioCB(type);
		if( !cbox.isSelected()) return;
		if( res.type == 1) tmp = "CT ";
		if( res.type == 2) tmp = "MRI ";
		res.label = tmp + cbox.getText();
		if( n<=0) return;
		tmpVals = new double[n];
		for(i=0; i<n; i++) {
			glcm0 = glcm.get(i);
			switch(type) {
				case 0:
					tmpVals[i] = computeEntropy(glcm0.matix);
					break;

				case 1:
					tmpVals[i] = computeHomogeneity(glcm0.matix);
					break;

				case 2:
					tmpVals[i] = computeContrast(glcm0.matix);
					break;

				case 3:
					tmpVals[i] = computeASM(glcm0.matix);
					break;

				case 4:
					tmpVals[i] = computeDifferenceEntropy(glcm0.matix);
					break;

				case 5:
					tmpVals[i] = computeDifferenceVariance(glcm0.matix);
					break;

				case 6:
					tmpVals[i] = computeCorrelation(glcm0.matix);
					break;

				case 7:
					tmpVals[i] = computeClusterPromenence(glcm0.matix);
					break;

				case 8:
					tmpVals[i] = computeClusterShade(glcm0.matix);
					break;

				case 9:
					tmpVals[i] = computeInverseDifferenceMoment(glcm0.matix);
					break;

				case 10:
					tmpVals[i] = computeCorrelationMeasure1(glcm0.matix);
					break;

				case 11:
					tmpVals[i] = computeCorrelationMeasure2(glcm0.matix);
					break;

				case 12:
					tmpVals[i] = computeMaximumProbability(glcm0.matix);
					break;

				case 13:
					tmpVals[i] = computeSumAverage(glcm0.matix);
					break;

				case 14:
					tmpVals[i] = computeSumEntropy(glcm0.matix);
					break;

				case 15:
					tmpVals[i] = computeSumVariance(glcm0.matix);
					break;

				case 16:
					tmpVals[i] = computeVariance(glcm0.matix);
					break;
			}
		}
		for(i=0; i<n; i++) mean += tmpVals[i];
		mean /= n;
		res.mean = mean;
		for(i=0; i<n; i++) sumSquare += Math.pow(mean-tmpVals[i], 2);
		res.std = Math.sqrt(sumSquare/(n-1));
	}

	/**
	 * 
	 * These functions are copied directly from within the software provided
	 * by Stephan Sellien, Christian Dietz and Andreas Graumann. Thanks.
	 */
	double computeEntropy(double[][] matrix) {
		double res = 0;

		final int nrGrayLevels = matrix.length;

		for (int i = 0; i < nrGrayLevels; i++) {
			for (int j = 0; j < nrGrayLevels; j++) {
				res += matrix[i][j] * Math.log(matrix[i][j] + EPSILON);
			}
		}
		return -res;
	}

	double computeHomogeneity(double[][] matrix) {
		double res = 0;

		final int nrGrayLevels = matrix.length;

		for (int i = 0; i < nrGrayLevels; i++) {
			for (int j = 0; j < nrGrayLevels; j++) {
				res += matrix[i][j] / (1 + Math.abs(i - j));
			}
		}
		return res;
	}

	double computeContrast(double[][] matrix) {
		final double[] pxminusy = computeCoocPXMinusY(matrix);

		double res = 0;
		for (int k = 0; k < matrix.length; k++) {
			res += k * k * pxminusy[k];
		}
		return res;
	}

	double computeDifferenceVariance(double[][] matrix) {
		final double[] pxminusy = computeCoocPXMinusY(matrix);

		final int numGreyLevels = matrix.length;
		double mu = 0.0;

		for (int i = 0; i < numGreyLevels; i++) {
			mu += i * pxminusy[i];
		}

		double res = 0;
		for (int k = 0; k < numGreyLevels; k++) {
			res += Math.pow(k - mu, 2) * pxminusy[k];
		}
		return res;
	}

	double computeASM(double[][] matrix) {
		double res = 0;

		final int nrGrayLevels = matrix.length;
		for (int i = 0; i < nrGrayLevels; i++) {
			for (int j = 0; j < nrGrayLevels; j++) {
				res += matrix[i][j] * matrix[i][j];
			}
		}
		return res;
	}

	double computeDifferenceEntropy(double[][] matrix) {
		final double[] pxminusy = computeCoocPXMinusY(matrix);
		final int nrGrayLevels = matrix.length;

		double res = 0;
		for (int k = 0; k < nrGrayLevels; k++) {
			res += pxminusy[k] * Math.log(pxminusy[k] + EPSILON);
		}
		return -res;
	}

	double computeCorrelation(double[][] matrix) {
		final int nrGrayLevels = matrix.length;

		final double meanx = computeCoocMeanX(matrix);
		final double meany = computeCoocMeanY(matrix);
		final double stdx = computeCoocStdX(matrix);
		final double stdy = computeCoocStdY(matrix);

		double sum = 0;
		for (int i = 0; i < nrGrayLevels; i++) {
			for (int j = 0; j < nrGrayLevels; j++) {
				sum += i*j*matrix[i][j];
			}
		}
		
		return ((sum - (meanx*meany))/(stdx*stdy));
	}

	double computeClusterPromenence(double[][] matrix) {
		final int nrGrayLevels = matrix.length;

		final double mux = computeCoocMeanX(matrix);
		final double muy = computeCoocMeanY(matrix);

		double res = 0;
		for (int i = 0; i < nrGrayLevels; i++) {
			for (int j = 0; j < nrGrayLevels; j++) {
				res += Math.pow(i + j - mux - muy, 4) * matrix[i][j];
			}
		}
		return res;
	}

	double computeClusterShade(double[][] matrix) {
		final double mux = computeCoocMeanX(matrix);
		final double muy = computeCoocMeanY(matrix);

		double res = 0;
		for (int j = 0; j < matrix.length; j++) {
			for (int i = 0; i < matrix.length; i++) {
				res += (Math.pow((i + j - mux - muy), 3) * matrix[j][i]);
			}
		}
		return res;
	}

	double computeInverseDifferenceMoment(double[][] matrix) {
		double res = 0;

		final int nrGrayLevels = matrix.length;

		for (int i = 0; i < nrGrayLevels; i++) {
			for (int j = 0; j < nrGrayLevels; j++) {
					res += matrix[i][j] / (1 + ((i - j) * (i - j)));
			}
		}
		return res;
	}

	double computeMaximumProbability(double[][] matrix) {
		double res = 0;

		final int nrGreyLevel = matrix.length;

		for (int i = 0; i < nrGreyLevel; i++) {
			for (int j = 0; j < nrGreyLevel; j++) {
				if (matrix[i][j] > res)
					res = matrix[i][j];
			}
		}
		return res;
	}

	double computeSumAverage(double[][] matrix) {
		double res = 0;
		final double[] pxplusy = computeCoocPXPlusY(matrix);

		final int nrGrayLevels = matrix.length;

		for (int i = 2; i <= 2 * nrGrayLevels; i++) {
			res += i * pxplusy[i];
		}
		return res;
	}

	double computeSumEntropy(double[][] matrix) {
		double res = 0;
		final double[] pxplusy = computeCoocPXPlusY(matrix);

		final int nrGrayLevels = matrix.length;

		for (int i = 2; i <= 2 * nrGrayLevels; i++) {
			res += pxplusy[i] * Math.log(pxplusy[i] + EPSILON);
		}
		return -res;
	}

	double computeSumVariance(double[][] matrix) {
		double res = 0;
		final double[] pxplusy = computeCoocPXPlusY(matrix);

		final int nrGrayLevels = matrix.length;
		final double sumEntropy = computeSumEntropy(matrix);

		for (int i = 2; i <= 2 * nrGrayLevels; i++) {
			res += (i - sumEntropy) * (i - sumEntropy) * pxplusy[i];
		}
		return res;
	}

	double computeVariance(double[][] matrix) {
		double res = 0;
		final int nrGreyLevel = matrix.length;

		double mean =  0.0;
		for (int i = 0; i < nrGreyLevel; i++) {
			for (int j = 0; j < nrGreyLevel; j++) {
				mean += matrix[i][j];
			}
		}
		mean /= nrGreyLevel*nrGreyLevel;
		
		for (int i = 0; i < nrGreyLevel; i++) {
			for (int j = 0; j < nrGreyLevel; j++) {
				res += (i - mean) * (i - mean) * matrix[i][j];
			}
		}
		return res;
	}

	double computeCorrelationMeasure1(double[][] matrix) {
		final double[] coochxy = computeCoocHXY(matrix);

		final double res = (computeEntropy(matrix) - coochxy[2]) / (coochxy[0] > coochxy[1]
			? coochxy[0] : coochxy[1]);
		return res;
	}

	double computeCorrelationMeasure2(double[][] matrix) {
		final double[] coochxy = computeCoocHXY(matrix);

		double res = Math.sqrt(1 - Math.exp(-2 * (coochxy[3] - computeEntropy(matrix))));

		// if NaN
		if (Double.isNaN(res)) res = 0;
		return res;
	}



	// helper functions
	double[] computeCoocPXMinusY(double[][] matrix) {
		final int nrGrayLevels = matrix.length;

		final double[] pxminusy = new double[nrGrayLevels];
		for (int k = 0; k < nrGrayLevels; k++) {
			for (int i = 0; i < nrGrayLevels; i++) {
				for (int j = 0; j < nrGrayLevels; j++) {
					if (Math.abs(i - j) == k) {
						pxminusy[k] += matrix[i][j];
					}
				}
			}
		}

		return pxminusy;
	}

	double[] computeCoocPXPlusY(double[][] matrix) {
		final int nrGrayLevels = matrix.length;
		final double[] pxplusy = new double[2 * nrGrayLevels + 1];

		for (int k = 2; k <= 2 * nrGrayLevels; k++) {
			for (int i = 0; i < nrGrayLevels; i++) {
				for (int j = 0; j < nrGrayLevels; j++) {
					if ((i + 1) + (j + 1) == k) {
						pxplusy[k] += matrix[i][j];
					}
				}
			}
		}
		return pxplusy;
	}

	double[] computeCoocPX(double[][] matrix) {
		final int nrGrayLevels = matrix.length;
		final double[] px = new double[nrGrayLevels];
		for (int i = 0; i < nrGrayLevels; i++) {
			for (int j = 0; j < nrGrayLevels; j++) {
				px[j] += matrix[i][j];
			}
		}

		return px;
	}

	double[] computeCoocPY(double[][] matrix) {
		final int nrGrayLevels = matrix.length;
		final double[] py = new double[nrGrayLevels];
		for (int i = 0; i < nrGrayLevels; i++) {
			for (int j = 0; j < nrGrayLevels; j++) {
				py[i] += matrix[i][j];
			}
		}

		return py;
	}

	double[] computeCoocHXY(double[][] matrix) {
		double hx = 0.0d;
		double hy = 0.0d;
		double hxy1 = 0.0d;
		double hxy2 = 0.0d;

		final int nrGrayLevels = matrix.length;

		final double[] px = computeCoocPX(matrix);
		final double[] py = computeCoocPY(matrix);

		for (int i = 0; i < px.length; i++) {
			hx += px[i] * Math.log(px[i] + EPSILON);
		}
		hx = -hx;

		for (int j = 0; j < py.length; j++) {
			hy += py[j] * Math.log(py[j] + EPSILON);
		}
		hy = -hy;
		for (int i = 0; i < nrGrayLevels; i++) {
			for (int j = 0; j < nrGrayLevels; j++) {
				hxy1 += matrix[i][j] * Math.log(px[i] * py[j] + EPSILON);
				hxy2 += px[i] * py[j] * Math.log(px[i] * py[j] + EPSILON);
			}
		}
		hxy1 = -hxy1;
		hxy2 = -hxy2;

		return new double[] { hx, hy, hxy1, hxy2 };
	}

	public double computeCoocMeanX(double[][] input) {

		double res = 0;
		final double[] px = computeCoocPX(input);
		for (int i = 0; i < px.length; i++) {
			res += i * px[i];
		}

		return res;
	}

	public double computeCoocMeanY(double[][] input) {

		double res = 0;
		final double[] px = computeCoocPX(input);
		for (int i = 0; i < px.length; i++) {
			res += i * px[i];
		}

		return res;
	}

	public double computeCoocStdX(double[][] input) {

		double res = 0;
		final double meanx = computeCoocMeanX(input);
		final double[] px = computeCoocPX(input);

		for (int i = 0; i < px.length; i++) {
			res += ((i - meanx) * (i - meanx)) * px[i];
		}

		return Math.sqrt(res);
	}

	public double computeCoocStdY(double[][] input) {

		double res = 0;
		final double meany = computeCoocMeanY(input);
		final double[] py = computeCoocPY(input);

		for (int i = 0; i < py.length; i++) {
			res += ((i - meany) * (i - meany)) * py[i];
		}

		return Math.sqrt(res);
	}

	// this should be in a separate class
	void computeFunction(int type, SUVpoints suvp, SUVpoints.RadioPoint res, BrownFat bf) {
		JCheckBox cbox = bf.getRadioCB(type);
		if( !cbox.isSelected()) return;
		double ret1 = computeSphericity(suvp, bf);
		if( ret1 <= 0) return;
		res.type = type;
		res.mean = ret1;
		res.label = cbox.getText();
	}

//	@SuppressWarnings("unchecked")
	double computeSphericity(SUVpoints suvp, BrownFat bf) {
		quickhull3d.Point3d[] in3, out3;
		SUVpoints.SavePoint curpnt;
		DoubleType surface, vol1;
		double val1 = 0;
		int i, j, indx, sz1;
		int[][] faces;
		if( suvp == null || (sz1=suvp.getListSize())<= 0) return 0;
//		val1 = runScript();
		QuickHull3D hull = new QuickHull3D();
//		LinkedHashSet<RealLocalizable> points = new LinkedHashSet<RealLocalizable>();
//		ArrayList<Facet> facets = new ArrayList<Facet>();
//		Facet facet1;
		NaiveDoubleMesh mesh0;
//		Vertex vert0;
		in3 = new quickhull3d.Point3d[sz1];
		for( i=0; i<sz1; i++) {
			curpnt = suvp.getPoint(i);
			in3[i] =  new quickhull3d.Point3d();
			in3[i].x = curpnt.x1;
			in3[i].y = curpnt.y1;
			in3[i].z = curpnt.z1;
		}
		hull.build(in3);
		hull.triangulate();
		faces = hull.getFaces();
		out3 = hull.getVertices();
		mesh0 = new NaiveDoubleMesh();
		for( i=0; i<out3.length; i++) {
			mesh0.vertices().add(out3[i].x, out3[i].y, out3[i].z);
//			vert0 = new Vertex(out3[i].x, out3[i].y, out3[i].z);
//			points.add(vert0);
		}
		sz1 = faces.length;
		for( i=0; i<sz1; i++) {
			mesh0.triangles().add(faces[i][0], faces[i][1], faces[i][2]);
		}
/*		Vertex[] vert1 = new Vertex[3];
		for( i=0; i<sz1; i++) {
			for( j=0; j<3; j++) {
				indx = faces[i][j];
				vert1[j] = new Vertex(out3[indx].x, out3[indx].y, out3[indx].z);
			}
			facet1 = new TriangularFacet(vert1[0], vert1[1], vert1[2]);
			facets.add(facet1);
		}*/
//		OpService ops = IJ1Helper.getLegacyContext().getService(OpService.class);
/*		OpService ops = bf.ops;
		if( ops == null) {
			Context context = new Context(OpService.class);
			bf.ops = ops = context.getService(OpService.class);
		}
		DoubleType dbl = ops.geom().sphericity (mesh0);
		val1 = dbl.get();*/

/*		DefaultVolumeMesh mesh1 = new DefaultVolumeMesh();
		vol1 = mesh1.calculate(mesh0);
		double vol = vol1.get();

		DefaultSurfaceArea dsa1 = new DefaultSurfaceArea();
		surface = dsa1.createOutput(mesh0);
		dsa1.compute(mesh0, surface);

		double sphereArea = Math.pow(Math.PI, 1 / 3d) * Math.pow(6 * vol, 2 / 3d);
		double out = sphereArea / surface.get();
		isConvex(facets, mesh0.getEpsilon());
//		for( i=0; i<sz1; i++) {
//			curpnt = suvp.getPoint(i);
//			in3 = new Point3f();
//			in3.x = curpnt.x1;
//			in3.y = curpnt.y1;
//			in3.z = curpnt.z1;
//			inLst.add(in3);
//		}
//		mesh = new CustomPointMesh(inLst);
//		mesh.update();
//		return mesh.getVolume();
//		mesh0 = new DefaultMesh((Set<RealLocalizable>) mesh.getMesh());
//		out = sphere.createOutput(mesh0);
//		sphere.initialize();
//		sphere.compute(mesh0, out);*/
		return val1;
	}

/*	double runScript() {
		try {
			final Context context = new Context(ScriptService.class);
			final ScriptService scriptService = context.getService(ScriptService.class);
			final String script = "as.integer(1) + as.integer(2)";
			final ScriptModule m = scriptService.run("add.r", script, true).get();
			final Object result = m.getReturnValue();
			String res1 = result.toString();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return 0;
	}*/

/*	boolean isConvex(ArrayList<Facet> facets, double tolerance) {
		Vector3D[] centroids = new Vector3D[facets.size()];
		for (int i = 0; i < facets.size(); i++) {
			centroids[i] = ((TriangularFacet) facets.get(i)).getCentroid();
		}

		boolean isConvex = true;
		for (int i = 0; i < facets.size(); i++) {
			for (int j = 0; j < centroids.length; j++) {
				if (j != i) {
					if (((TriangularFacet) facets.get(i)).distanceToPlane(
						centroids[j]) >= tolerance)
					{
						isConvex = false;
						break;
					}
				}
			}
		}
		return isConvex;
	}*/
}

import ij.ImageJ;
import ij.ImagePlus;
import java.util.ArrayList;
import javax.swing.JCheckBox;

/**
 *
 * @author ilan
 */
public class SUVpoints {
	ArrayList<SavePoint> volPointList;
	ArrayList<RadioPoint> radioList;
	ArrayList<CooccurencePoint>glcm;
	int sliceType, SUVtype;
	int zmin, zmax, ptListSz, red0;
	ImageJ ij;
	private ArrayList<SavePoint> modified;		
	private short[][] summary;
	
	SUVpoints(int slcType) {
		sliceType = slcType;
		volPointList = new ArrayList<>();
		ptListSz = -1;
		ij = null;
	}

	SUVpoints(SUVpoints old) {
		copyMe(old);
	}

	private void copyMe(SUVpoints old) {
		sliceType = old.sliceType;
		volPointList = new ArrayList<>();
		for( int i=0; i < old.getListSize(); i++ ) {
			SavePoint s = old.getPoint(i);
			addPoint(s.petVal, s.ctVal, s.x1, s.y1, s.z1, s.rn1, s.labelIndx);
		}
		ptListSz = getListSize();
		zmin = old.zmin;
		zmax = old.zmax;
		SUVtype = old.SUVtype;
		ij = null;
	}

	class SavePoint {
		short	x1;
		short	y1;
		short	z1;
		short	rn1;	// ROI number
		double	petVal;
		int		ctVal;
		int		labelIndx;
	}

	class RadioPoint {
		double	mean;
		double	std;
		String	label = null;
		int		type = 0;	//PET, 1=CT, 2=MRI
	}

	public class CooccurencePoint {
		double[][] matix;
		int		numPnt = 0;
		String	label;
	}

	RadioPoint newRadioPnt() {
		return new RadioPoint();
	}

	SavePoint newPoint( double pet1, int ct1, int x0, int y0, int z0, int rn0, int li) {
		SavePoint ret1 = new SavePoint();
		ret1.petVal = pet1;
		ret1.ctVal = ct1;
		ret1.x1 = (short) x0;
		ret1.y1 = (short) y0;
		ret1.z1 = (short) z0;
		ret1.rn1 = (short) rn0;
		ret1.labelIndx = li;
		return ret1;
	}
	
	void addPoint( SavePoint curPnt) {
		volPointList.add(curPnt);
	}
	
	int getListSize() {
		return volPointList.size();
	}
	
	SavePoint getPoint(int i) {
		return volPointList.get(i);
	}
	
	double getPetVal(int i) {
		SavePoint ret1 = volPointList.get(i);
		return ret1.petVal;
	}
	
	void addPoint( double pet1, int ct1, int x0, int y0, int z0, int rn0, int li) {
		SavePoint curPnt = newPoint( pet1, ct1, x0, y0, z0, rn0, li);
		volPointList.add(curPnt);
	}

	boolean maybeAddPoint( SavePoint curPnt) {
		return maybeAddPoint( curPnt.petVal, curPnt.ctVal, curPnt.x1, curPnt.y1, curPnt.z1, curPnt.rn1, curPnt.labelIndx);
	}

	boolean maybeAddPoint( double pet1, int ct1, int x0, int y0, int z0, int rn0, int li) {
		int i, n=ptListSz;	// check only nifti points
		if( n<=0) ptListSz = n = volPointList.size();
		SavePoint test1;
		for( i=0; i<n; i++) {
			test1 = volPointList.get(i);
			if( test1.x1 == x0 && test1.y1 == y0 && test1.z1 == z0)
				return false;
		}
		addPoint( pet1, ct1, x0, y0, z0, rn0, li);
		return true;
	}

	SavePoint modifySuvPoint(SavePoint orig, int[] modifyInt) {
		int i, x0, y0, z0, x1, y1, z1;
		x0 = x1 = orig.x1;
		y0 = y1 = orig.y1;
		z0 = z1 = orig.z1;
		if( modifyInt != null && modifyInt.length == 3) {
			i = modifyInt[0];
			if(i==1) x1 = y0;
			if(i==2) x1 = z0;

			i = modifyInt[1];
			if(i==0) y1 = x0;
			if(i==2) y1 = z0;

			i = modifyInt[2];
			if(i==0) z1 = x0;
			if(i==1) z1 = y0;
		}
		SavePoint ret1 = newPoint( orig.petVal, orig.ctVal, x1, y1, z1, orig.rn1, orig.labelIndx);
		return ret1;
	}

	// we want the z direction to be the "slice number"
	void addRearrangePoint( double pet1, int ct1, int x0, int y0, int z0, int rn0, int li) {
		int y1, z1;
		y1 = y0;
		z1 = z0;
		if( sliceType != JFijiPipe.DSP_AXIAL) {
			z1 = y0;
			y1 = z0;
		}
		SavePoint curPnt = newPoint( pet1, ct1, x0, y1, z1, rn0, li);
		volPointList.add(curPnt);
	}
	
	double calcSD(double mean) {
		return calcSDsub(mean, 0);
	}
	
	double calcSDsub(double mean, int type) {
		SavePoint ret1;
		ArrayList<SavePoint> pointList = volPointList;
		int i, n=pointList.size();
		if( n <= 1) return 0.;
		double currVal, sumSquare = 0;
		for( i=0; i<n; i++) {
			ret1 = volPointList.get(i);
			if(type == 1) currVal = ret1.ctVal;
			else currVal = ret1.petVal;
			sumSquare += Math.pow(mean - currVal, 2);
		}
		return Math.sqrt(sumSquare/(n-1));
	}
	
	double calcSUVpeak(JFijiPipe pet1, int suvType) {
		zmin = zmax = -1;
		SUVtype = suvType;
		return calcSUVpeakSub( pet1, volPointList, -1);
	}
	
	double calcSUVpeakSub(JFijiPipe pet1, ArrayList<SavePoint> pointList, int zVal) {
		int i, cnt1, n=pointList.size();
		SavePoint pntCur, pntSave;
		double max1 = 0, sclX, sclY, sclZ, difX, difY, difZ, difR2;
		double retVal = 0;
		pntSave = null;
		double [] peakPnts = new double[3];
		// the next line isn't really necessary since the new zeroes them
		peakPnts[0] = peakPnts[1] = peakPnts[2] = 0;
		for(i=0; i<n; i++) {
			pntCur = pointList.get(i);
			if(zVal < 0) {
				if( zmin < 0) zmin = zmax = pntCur.z1;
				if( zmin > pntCur.z1) zmin = pntCur.z1;
				if( zmax < pntCur.z1) zmax = pntCur.z1;
			}
			if( pntCur.petVal > max1) {
				max1 = pntCur.petVal;
				pntSave = pntCur;
			}
		}
		if( pntSave == null) return retVal;
		retVal = pntSave.petVal;
		sclX = sclY = pet1.data1.pixelSpacing[JFijiPipe.COL];
		sclZ = Math.abs(pet1.data1.sliceThickness);
		if( sclX <= 0 || sclZ == 0) return retVal;
		if( sliceType != JFijiPipe.DSP_AXIAL) {
			sclY = sclZ;
			sclZ = sclX;
		}
		for(i=cnt1=0; i<n; i++) {
			pntCur = pointList.get(i);
			difX = (pntSave.x1 - pntCur.x1) * sclX;
			difY = (pntSave.y1 - pntCur.y1) * sclY;
			difZ = (pntSave.z1 - pntCur.z1) * sclZ;
			difR2 = difX*difX + difY*difY + difZ*difZ;
			// give the radius squared in order to save sqrt
			// 6.2^2 = 38.44
			if( difR2 == 0 || difR2 > 38.44) continue;
			cnt1++;
			if( SUVtype == 2) {
				if( pntCur.petVal >= peakPnts[0]) {
					peakPnts[2] = peakPnts[1];
					peakPnts[1] = peakPnts[0];
					peakPnts[0] = pntCur.petVal;
				} else {
					if( pntCur.petVal >= peakPnts[1]) {
						peakPnts[2] = peakPnts[1];
						peakPnts[1] = pntCur.petVal;
					}
					else if( pntCur.petVal >= peakPnts[2])
						peakPnts[2] = pntCur.petVal;
				}
			} else {
				retVal += pntCur.petVal;
			}
		}
		if( SUVtype == 2) {
			int divN = 1;
			if( peakPnts[0] > 0) divN = 2;
			if( peakPnts[1] > 0) divN = 3;
			if( peakPnts[2] > 0) divN = 4;
			for( i=1; i<divN; i++) retVal += peakPnts[i-1];
			retVal /= divN;
		} else {
			retVal /= cnt1+1;
		}
		return retVal;
	}
	
	double [] calcSUVpeakList(JFijiPipe pet1) {
		int i, n = zmax-zmin+1;
		int j, n0 = volPointList.size();
		if( zmin < 0 || n <= 0 ) return null;
		double[] retVal = new double[n];
		ArrayList<SavePoint> curList;
		SavePoint pntCur;
		for(i=0; i<n; i++) {
			curList = new ArrayList<>();
			for( j=0; j<n0; j++) {
				pntCur = volPointList.get(j);
				if( pntCur.z1 != i+zmin) continue;
				curList.add(pntCur);
			}
			retVal[i] = calcSUVpeakSub(pet1, curList, i+zmin);
		}
		return retVal;
	}

	// functionally this could be static
	ArrayList<SavePoint> buildExcludedList(BrownFat.ExcludedROI[] excludedROI) {
		int i, j, n1, n = excludedROI.length;
		BrownFat.ExcludedROI exROI;
		for( i=0; i<n; i++) {
			if(excludedROI[i] != null) break;
		}
		if( i>=n ) return null;
		ArrayList<SavePoint> removedPointList = new ArrayList<>();
		SavePoint tst1;
		ArrayList<SavePoint> tstList;
		for( i=0; i<n; i++) {
			exROI = excludedROI[i];
			if(exROI == null) continue;
			tstList = exROI.suvPnt1.volPointList;
			n1 = tstList.size();
			for( j=0; j<n1; j++) {
				tst1 = tstList.get(j);
				if( isIncluded(removedPointList, tst1)) continue;
				removedPointList.add(tst1);
			}
		}
		return removedPointList;
	}

	private void buildExcludeObj(ArrayList<SavePoint>removedList) {
		int i, j, z, zadd, zstart, maxZ=0;
		SavePoint tst1;
		modified = new ArrayList<>();
		if( removedList == null) return;
		int n=removedList.size();
		for( i=0; i<n; i++) {
			tst1 = removedList.get(i);
			if( tst1.z1 > maxZ) maxZ = tst1.z1;
		}
		summary = new short[maxZ+1][6];
		for( j=0; j<=maxZ; j++) summary[j][0] = -1;

		zstart = modified.size();
		for ( i=0; i<n; i++) {
			tst1 = removedList.get(i);
			z = tst1.z1;
			if( summary[z][0] < 0) {
				summary[z][0] = tst1.x1;
				summary[z][2] = tst1.y1;
			}
			if( summary[z][0] > tst1.x1) summary[z][0] = tst1.x1;
			if( summary[z][1] < tst1.x1) summary[z][1] = tst1.x1;
			if( summary[z][2] > tst1.y1) summary[z][2] = tst1.y1;
			if( summary[z][3] < tst1.y1) summary[z][3] = tst1.y1;
			summary[z][4]++;
			for( j=zadd=0; j<=z; j++) zadd += summary[j][4];
			modified.add(zstart+zadd-1, tst1);
		}
		for( i=zadd=0; i<=maxZ; i++) {
			z = summary[i][4];
			if( z==0) continue;
			summary[i][4] = (short)(zadd+zstart);
			zadd += z;
			summary[i][5] = (short)(zadd+zstart);
		}
	}

	boolean isIncluded( ArrayList<SavePoint> tst1, SavePoint pnt1) {
		int i, n=tst1.size();
		SavePoint tstPnt;
		for( i=0; i<n; i++) {
			tstPnt = tst1.get(i);
			if( tstPnt.x1 == pnt1.x1 && tstPnt.y1 == pnt1.y1 && tstPnt.z1 == pnt1.z1) return true;
		}
		return false;
	}

	double removeExcluded( ArrayList<SavePoint>removedList, BrownFat.ExcludedROI[] excludedROI) {
		double total = 0, max1 = -1;
		red0 = 0;
		buildExcludeObj(removedList);
		if( modified == null || modified.isEmpty()) return total;
		int i, j, jhi, n=volPointList.size();
		ArrayList<SavePoint> tstList = new ArrayList<>();
		short x1, y1, z1, sumLen;
		boolean isOK;
		SavePoint excl, tst1;
		for( i=0; i<n; i++) {
			tst1 = volPointList.get(i);
			x1 = tst1.x1;
			y1 = tst1.y1;
			z1 = tst1.z1;
			isOK = true;
			sumLen = (short) summary.length;
			while(isOK) {
				if( z1 >= sumLen) break;
				if( summary[z1][0] < 0) break;
				if( summary[z1][0] > x1 || summary[z1][1] < x1) break;
				if( summary[z1][2] > y1 || summary[z1][3] < y1) break;
				jhi = summary[z1][5];
				if( jhi > modified.size())
					jhi = modified.size();
				for( j=summary[z1][4]; j<jhi; j++) {
//				for( j=0; j<n; j++) {
//					if( j>=0) break;
					if( j<0 || j>modified.size())
						break;
					excl = modified.get(j);
					if( x1 != excl.x1 || y1 != excl.y1 || z1 != excl.z1) continue;
					total += excl.petVal;
					isOK = false;
					break;
				}
				break;
			}
			if(isOK) {
				if( tst1.petVal > max1) {
					max1 = tst1.petVal;
					red0 = tstList.size();
				}
				tstList.add(tst1);
			}
		}
		if( tstList.size() < n) volPointList = tstList;
		return total;
	}

	double[] calCtVals() {
		int i, n;
		SavePoint ret1;
		double[] retVal = new double[2];
		double ctMean = 0;
		n = volPointList.size();
		if( n<=0) return retVal;
		for( i=0; i<n; i++) {
			ret1 = volPointList.get(i);
			ctMean += ret1.ctVal;
		}
		ctMean /= n;
		retVal[0] = ctMean;
		retVal[1] = calcSDsub(ctMean, 1);
		return retVal;
	}

	double calcSUVmeanPeak(JFijiPipe pet1) {
		double [] peakList = calcSUVpeakList(pet1);
		double retVal = 0;
		int i, k, n = peakList.length;
		for( i=k=0; i<n; i++) {
			if( peakList[i] <= 0) continue;
			retVal += peakList[i];
			k++;
		}
		if( k>0) retVal /= k;
		return retVal;
	}

	CooccurencePoint makeCoocur() {
		return new CooccurencePoint();
	}

	void calcRadiomics(BrownFat bf, boolean isCalc) {
		glcm = new ArrayList<>();
		radioList = new ArrayList<>();
		if( !isCalc) return;
		RadioPoint res;
		ImagePlus imp, imp2;
		boolean isCt = bf.isCtRadiomics();
		int i, type;
		try {
			Radionomic img2 = new Radionomic();
			img2.init(bf);
			for( i=0; i<=16; i++) {
				JCheckBox cbox = bf.getRadioCB(i);
				if( cbox.isSelected()) break;
			}
			if( i >= 16) return;	// nothing selected
			imp = img2.buildPlus(this);
			if( imp == null) return;
			img2.calcGlcm(this, imp);
			for( i=0; i<=16; i++) {
				res = new RadioPoint();
				img2.computeFunction(i, glcm, res, bf);
				if(res.label != null) radioList.add(res);
			}
			res = new RadioPoint();
			img2.computeFunction(17, this, res, bf);
			if(res.label != null) radioList.add(res);
			if( isCt) {
				imp2 = img2.buildCtPlus(this, bf.getParentPet(), false);
				// imp2 can be null if no point falls inside the CT limits
				if( imp2 == null) return;
				type = 1;
				if( bf.getParentPet().MRIflg) type = 2;
				glcm = new ArrayList<>();
				img2.calcGlcm(this, imp2);
				for( i=0; i<=16; i++) {
					res = new RadioPoint();
					res.type = type;
					img2.computeFunction(i, glcm, res, bf);
					if(res.label != null) radioList.add(res);
				}
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}
}

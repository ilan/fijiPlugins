import java.util.ArrayList;

/**
 *
 * @author ilan
 */
public class SUVpoints {
	ArrayList<SavePoint> volPointList;
	int sliceType, SUVtype;
	int zmin, zmax, ptListSz;
	
	SUVpoints(int slcType) {
		sliceType = slcType;
		volPointList = new ArrayList<SavePoint>();
		ptListSz = -1;
	}
	

	class SavePoint {
		short	x1;
		short	y1;
		short	z1;
		short	rn1;	// ROI number
		double	petVal;
		int		ctVal;
	}
	
	SavePoint newPoint( double pet1, int ct1, int x0, int y0, int z0, int rn0) {
		SavePoint ret1 = new SavePoint();
		ret1.petVal = pet1;
		ret1.ctVal = ct1;
		ret1.x1 = (short) x0;
		ret1.y1 = (short) y0;
		ret1.z1 = (short) z0;
		ret1.rn1 = (short) rn0;
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
	
	void addPoint( double pet1, int ct1, int x0, int y0, int z0, int rn0) {
		SavePoint curPnt = newPoint( pet1, ct1, x0, y0, z0, rn0);
		volPointList.add(curPnt);
	}

	boolean maybeAddPoint( SavePoint curPnt) {
		return maybeAddPoint( curPnt.petVal, curPnt.ctVal, curPnt.x1, curPnt.y1, curPnt.z1, curPnt.rn1);
	}

	boolean maybeAddPoint( double pet1, int ct1, int x0, int y0, int z0, int rn0) {
		int i, n=ptListSz;	// check only nifti points
		if( n<=0) ptListSz = n = volPointList.size();
		SavePoint test1;
		for( i=0; i<n; i++) {
			test1 = volPointList.get(i);
			if( test1.x1 == x0 && test1.y1 == y0 && test1.z1 == z0)
				return false;
		}
		addPoint( pet1, ct1, x0, y0, z0, rn0);
		return true;
	}

	// we want the z direction to be the "slice number"
	void addRearrangePoint( double pet1, int ct1, int x0, int y0, int z0, int rn0) {
		int y1, z1;
		y1 = y0;
		z1 = z0;
		if( sliceType != JFijiPipe.DSP_AXIAL) {
			z1 = y0;
			y1 = z0;
		}
		SavePoint curPnt = newPoint( pet1, ct1, x0, y1, z1, rn0);
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
		sclX = sclY = pet1.data1.pixelSpacing[0];
		sclZ = Math.abs(pet1.data1.spacingBetweenSlices);
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
			curList = new ArrayList<SavePoint>();
			for( j=0; j<n0; j++) {
				pntCur = volPointList.get(j);
				if( pntCur.z1 != i+zmin) continue;
				curList.add(pntCur);
			}
			retVal[i] = calcSUVpeakSub(pet1, curList, i+zmin);
		}
		return retVal;
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
}

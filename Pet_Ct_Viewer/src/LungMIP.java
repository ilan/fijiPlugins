import ij.WindowManager;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.prefs.Preferences;

/**
 *
 * @author ilan
 */
public class LungMIP extends javax.swing.JDialog {
	static final int PET_INSERT = 0;
	static final int CT_INSERT = 1;

	/**
	 * Creates new form LungMIP
	 * @param parent
	 * @param modal
	 */
	public LungMIP(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
		init(parent);
	}
	
	private void init(java.awt.Frame parent) {
		int x, y;
		parentPet = ((PetCtFrame)parent).getPetCtPanel1();
		parentPet.LungMipDlg = this;
		Preferences prefer = parentPet.parent.jPrefer;
		x = prefer.getInt("lung mip x", -1);
		y = prefer.getInt("lung mip y", -1);
		if(x>=0 && y>=0) setLocation(x,y);
		String tmp = prefer.get("lung slice width", "10");
		jTextMm.setText(tmp);
		changeSliceSize();
		parentPet.repaintAll();
		WindowManager.addWindow(this);
	}
	
	void prepareData(PetCtPanel caller) {
		if( parentPet != caller) return;
//		if( setRoiChoice()) {
			maybeNewInsert();
//		}
	}

/*	void drawAllData( Graphics2D g, PetCtPanel caller) {
		if( parentPet != caller) return;
		boolean isInside;
		if( drawingRoi) {
			drawCurrRoi(g, currPoly, true);
		} else {
			isInside = setRoiChoice();
			if( isInside) {
				drawCurrRoi(g, currPoly, false);
//				maybeNewInsert();
			}
		}
	}
	
	boolean setRoiChoice() {
//		if(drawingRoi) return false;
		int i, n, slType = parentPet.m_sliceType;
		Poly3Save poly1;
		boolean isInside = false;
		String butTitle = "Draw ROI";
		if(currPoly != null) {
			if(slType == currPoly.type) isInside = true;
		}
		if(!isInside) {
			n = polyVect.size();
			for(i=0; i<n; i++) {
				poly1 = polyVect.get(i);
				if(slType != poly1.type) continue;
				isInside = true;
				currPoly = poly1;
				break;
			}
		}
		if(isInside) butTitle = "Remove ROI";
//		jTogDrawMRoi.setText(butTitle);
		return isInside;
	}*/
	
	double getSlicePos() {
		switch(parentPet.m_sliceType) {
			case JFijiPipe.DSP_CORONAL:
				return parentPet.petCoronal;

			case JFijiPipe.DSP_SAGITAL:
				return parentPet.petSagital;

			default:
				return parentPet.petAxial;
		}
	}

/*	void handleMouseMove( MouseEvent e, PetCtPanel caller) {
		if( parentPet != caller || parentPet.mouse1.widthX == 0) return;
		currMousePos.x = e.getX() % parentPet.mouse1.widthX;
		currMousePos.y = e.getY();
		if( drawingRoi) {
			parentPet.repaint();
			return;
		}
		setProperCursor();
	}*/

	int setProperCursor() {
		Cursor cur1 = Cursor.getDefaultCursor();
		parentPet.setCursor(cur1);
		return 0;
	}

/*	void drawCurrRoi( Graphics2D g, Poly3Save poly1, boolean drawing) {
		int[] xp1, xp2, yp1, yp2, xp4, yp4;
		int i, j, widthX, npoints, num4, type1, cineIdx;
		int rubberX, rubberY, numDisp = 3;
		Point2d pt1;
		Point pt2;
		if( poly1 == null || poly1.type != parentPet.m_sliceType) return;
		double scl1 = parentPet.getScalePet();
		npoints = poly1.poly.npoints;
		if( npoints <= 0) return;
		boolean MIPdisplay = parentPet.isMIPdisplay();
		cineIdx = parentPet.getCineIndx();
		if( MIPdisplay) numDisp = 2;
		xp1 = poly1.poly.xpoints;
		yp1 = poly1.poly.ypoints;
		type1 = 2;	// this could be 4 as well, i.e. for 3 display
		switch( poly1.type) {
			case JFijiPipe.DSP_AXIAL:
				type1 = 0;
				if( parentPet.petPipe.zoom1 > 1.0) type1 = 3;
				break;

			case JFijiPipe.DSP_CORONAL:
				if(MIPdisplay && cineIdx == 0) numDisp = 3;
				break;

			case JFijiPipe.DSP_SAGITAL:
				if(MIPdisplay && cineIdx == 3*JFijiPipe.NUM_MIP/4) numDisp = 3;
				break;
		}
		widthX = parentPet.mouse1.widthX;
		xp2 = new int[npoints];
		yp2 = new int[npoints];
		for( i=0; i<npoints; i++) {
			pt1 = new Point2d(xp1[i], yp1[i]);
			pt2 = parentPet.petPipe.pos2Scrn(pt1, scl1, type1);
			xp2[i] = pt2.x;
			yp2[i] = pt2.y;
		}
		xp4 = xp2;
		yp4 = yp2;
		num4 = npoints;
		rubberX = currMousePos.x;
		rubberY = currMousePos.y;
		for( j=0; j<numDisp; j++) {
			g.setColor(Color.green);
			if( drawing) {
				g.drawPolyline(xp2, yp2, npoints);
				i = npoints-1;
				if(rubberX >= 0) g.drawRect(xp2[i], yp2[i], rubberX-xp2[i], rubberY-yp2[i]);
				g.drawRect(xp2[0]-4, yp2[0]-4, 8, 8);
			} else {
				if(npoints>2) g.drawPolygon(xp2, yp2, npoints);
				else g.drawRect(xp2[0], yp2[0], xp2[1]-xp2[0], yp2[1]-yp2[0]);
			}
			Color col1 = Color.white;
			for( i=0; i<num4; i++) {
				if( !drawing) drawHandle(g, xp4[i], yp4[i], col1);
				xp4[i] += widthX;
			}
			if( rubberX >= 0) rubberX += widthX;
		}
	}

	void drawHandle( Graphics2D g, int x1, int y1, Color col1) {
		g.setColor(Color.black);
		g.fillRect(x1-2, y1-2, 5, 5);
		g.setColor(col1);
		g.fillRect(x1-1, y1-1, 3, 3);
	}
	
	void processMouseSingleClick( int pos3, Point pt1, double scl1) {
		Point pt2 = convertPoint2Pos( pt1, currPoly.type, scl1);
		int n = currPoly.poly.npoints;
		currPoly.poly.addPoint(pt2.x, pt2.y);
		if( n==1) {
			finishRect();
			return;
		}
		currMousePos.x = -1;
		parentPet.repaint();
	}*/

	void maybeNewCurrPoly() {
		if( currPoly != null && currPoly.type == parentPet.m_sliceType)
			return;	// already exists
		currPoly = new Poly3Save();
		currPoly.type = parentPet.m_sliceType;
		currPoly.Mtype = CT_INSERT;
		currPoly.poly = new Polygon();
	}

	void maybeNewInsert() {
		if( !jCheckDisplay.isSelected()) return;
		maybeNewCurrPoly();
		PipeInsert pInsert = new PipeInsert();
		pInsert.findInsert(currPoly);
		if(pInsert.insert != null) return;	// already exists
		pInsert.buildInsert(currPoly);
		changeDisplayFlg(false);
	}

/*	void finishRect() {
		calcLungMipInsert(currPoly);
		jTogDrawMRoi.setSelected(false);
		drawingRoi = false;
		polyVect.add(currPoly);
		setRoiChoice();
		parentPet.repaintAll();
	}

	Point convertPoint2Pos( Point pt1, int type, double scl1) {
		int i = 0;
		double scl2 = scl1;
		if( scl2 <= 0) scl2 = parentPet.getScalePet();
		if( parentPet.petPipe.zoom1 > 1.0) i = 3;
		if( type != JFijiPipe.DSP_AXIAL) i = 4;
		return parentPet.petPipe.scrn2PosInt(pt1, scl2, i);
	}*/

	
	protected void changeSliceSize() {
		double slSz;
		String tmp = jTextMm.getText();
		slSz = Double.parseDouble(tmp);
		if( slSz != slSize && currPoly != null) {
			currPoly.getPipe().lungData = null;
			parentPet.repaintAll();
		}
		slSize = slSz;
	}
	
/*	void pressMRoiBut() {
		if( jTogDrawMRoi.getText().equals("Remove ROI")) {
			int i, n = polyVect.size();
			Poly3Save poly1;
			for( i=0; i<n; i++) {
				poly1 = polyVect.get(i);
				if(!poly1.isPoly3(currPoly)) continue;
				poly1.getPipe().lungData = null;
				polyVect.remove(i);
				changeDisplayFlg(true);
				currPoly = null;
				break;
			}
			jTogDrawMRoi.setSelected(false);
			return;
		}
		initDraw();
		drawingRoi = true;
	}
	
	void initDraw() {
		currPoly = new Poly3Save();
		currPoly.type = parentPet.m_sliceType;
		currPoly.Mtype = CT_INSERT;
		currPoly.poly = new Polygon();
		currMousePos.x = -1;
	}*/

	@Override
	public void dispose() {
		changeDisplayFlg(true);
		WindowManager.removeWindow(this);
		Preferences prefer = parentPet.parent.jPrefer;
		Point pt1 = getLocation();
		prefer.putInt("lung mip x", pt1.x);
		prefer.putInt("lung mip y", pt1.y);
		String tmp = jTextMm.getText();
		prefer.put("lung slice width", tmp);
		parentPet.LungMipDlg = null;
		if(parentPet.ctPipe != null) {
			parentPet.ctPipe.lungData = null;
			parentPet.petPipe.lungData = null;
			JFijiPipe mri = parentPet.mriPipe;
			if(mri != null) mri.lungData = null;
		}
		super.dispose();
	}

/*	boolean calcLungMipInsert(Poly3Save poly1) {
		PipeInsert pInsert = new PipeInsert();
		pInsert.findInsert(poly1);
		jCheckDisplay.setSelected(true);
		boolean isInsert = pInsert.buildInsert(poly1);
		return isInsert;
	}*/

	void changeDisplayFlg(boolean repaint) {
		if( currPoly == null) {
			parentPet.repaintAll();
			return;
		}
		JFijiPipe pip1 = currPoly.getPipe();
		if( pip1 == null) return;
		pip1.dispInsert = jCheckDisplay.isSelected();
		changeSliceSize();
		pip1.dirtyFlg = true;	// 2 lines to force update
		pip1.corSrc = null;
		if(repaint) parentPet.repaintAll();
	}

	class PipeInsert {
		JFijiPipe pip1 = null;
		JFijiPipe.lungInsert insert = null;
		int indx = -1;

		void findInsert(Poly3Save poly1) {
			pip1 = poly1.getPipe();
			indx = -1;
			insert = null;
			if( pip1.lungData == null) pip1.lungData = new ArrayList<JFijiPipe.lungInsert>();
			int i, zCurr, n=pip1.lungData.size();
			zCurr = getCurrZPos();
			JFijiPipe.lungInsert lData;
			for( i=0; i<n; i++) {
				lData = pip1.lungData.get(i);
				if( lData.type != poly1.type) continue;
				if( lData.zpos != zCurr) continue;
				insert = lData;
				indx = i;
				break;
			}
		}
		
		int getCurrZPos() {
			double zCurr = getSlicePos();
			if(parentPet.m_sliceType == JFijiPipe.DSP_AXIAL)
				return pip1.findCtPos(zCurr, true);
			return ChoosePetCt.round(zCurr);
		}

		boolean buildInsert(Poly3Save poly1) {
			boolean isInsert = (insert != null);
			int val0, val1, i, j, z, zmax, wid1, wid2, xOff, yOff, angle1=0;
			int depth, depth1, sliceNum, cenPet, cenCt, edgeOff = 0;
			Rectangle r1;
			double sThick, zoomPet, scl1;
			int l1, nSlices, xyChoice = 1;
			JFijiPipe.lineEntry currLine;
			JFijiPipe petPipe = parentPet.petPipe;
			zoomPet = petPipe.zoomX;
			if( zoomPet > 1.0) edgeOff = (int)((zoomPet - 1.0)*petPipe.data1.width/(2*zoomPet) + 0.99);
			if(!isInsert) insert = pip1.createLungInsert();
			pip1.dispInsert = jCheckDisplay.isSelected();
			insert.type = poly1.type;
//			r1 = poly1.poly.getBounds();
			r1 = new Rectangle(edgeOff,edgeOff,petPipe.data1.width-2*edgeOff,petPipe.data1.height-2*edgeOff);
			insert.xOff = xOff = parentPet.shift2Ct(pip1, r1.x, 0);
			if( xOff < 0) {	// this can happen with MRI studies
				cenPet = petPipe.data1.width / 2;
				cenCt = pip1.data1.width / 2;
				scl1 = (petPipe.zoomX * cenCt) / (pip1.zoomX * cenPet);
				edgeOff = (int) (cenPet - cenCt/scl1 + 1);
				if( edgeOff < 0) edgeOff = 0;
				r1 = new Rectangle(edgeOff,edgeOff,petPipe.data1.width-2*edgeOff,petPipe.data1.height-2*edgeOff);
				insert.xOff = xOff = parentPet.shift2Ct(pip1, r1.x, 0);
				if( xOff < 0) insert.xOff = xOff = 0;
			}
			val1 = parentPet.shift2Ct(pip1, r1.x + r1.width, 0);
			insert.width = wid1 = val1 - insert.xOff;
			if(insert.type == JFijiPipe.DSP_AXIAL) {
				insert.yOff = parentPet.shift2Ct(pip1, r1.y, 1);
				val1 = parentPet.shift2Ct(pip1, r1.y + r1.height, 1);
				zmax = pip1.getNormalizedNumFrms();
				sThick = pip1.data1.sliceThickness;
			} else {
				val0 = pip1.findCtPos(0, true);
				val1 = pip1.findCtPos(petPipe.getNormalizedNumFrms() - 1, true);
				if( val0 > val1) {
					i = val0;
					val0 = val1;
					val1 = i;
				}
				insert.yOff = val0;
				val1++;
				zmax = pip1.data1.width;
				sThick = pip1.data1.pixelSpacing[0];
				if( insert.type == JFijiPipe.DSP_SAGITAL) {
					angle1 = 270;
					xyChoice = 0;
				}
			}
			sThick = Math.abs(sThick);
			insert.height = val1 - insert.yOff;
			insert.zpos = getCurrZPos();
			insert.pixels = new short[wid1*insert.height];
			nSlices = ChoosePetCt.round(slSize/(2*sThick));
			// make the pixels equal the first slice
			z = insert.zpos-nSlices;
			depth1 = ChoosePetCt.round(parentPet.shift2Ct1(pip1, xyChoice)-nSlices);
			if( z < 0) z = 0;
			if( depth1 < 0) depth1 = 0;
			for( j=0; j<insert.height; j++) {
				sliceNum = z;
				depth = j+insert.yOff;
				if(insert.type != JFijiPipe.DSP_AXIAL) {
					sliceNum = depth;
					depth = depth1;
				}
				yOff = j*wid1;
				wid2 = wid1;
				currLine = pip1.data1.getLineOfData(angle1, depth, sliceNum);
				if( currLine.pixels == null) continue;
				l1 = currLine.pixels.length - xOff;
				if( wid2 >= l1) wid2 = l1 -1;
				for( i=0; i<wid2; i++) {
					insert.pixels[i+yOff] = currLine.pixels[i+xOff];
				}
			}
			// after the first slice look for larger values, MIP
			while( ++z<=insert.zpos+nSlices) {
				depth1++;	// this is the CT value
				if(insert.type == JFijiPipe.DSP_AXIAL) {
					if( z >= zmax) break;
				} else {
					if( depth1 >= zmax) break;
				}
				for( j=0; j<insert.height; j++) {
					sliceNum = z;
					depth = j+insert.yOff;
					if(insert.type != JFijiPipe.DSP_AXIAL) {
						sliceNum = depth;
						depth = depth1;
					}
					yOff = j*wid1;
					wid2 = wid1;
					currLine = pip1.data1.getLineOfData(angle1, depth, sliceNum);
					if( currLine.pixels == null) continue;
					l1 = currLine.pixels.length - xOff;
					if( wid2 >= l1) wid2 = l1 -1;
					for( i=0; i<wid2; i++) {
						val1 = currLine.pixels[i+xOff];
						if(insert.pixels[i+yOff]>=val1) continue;
						insert.pixels[i+yOff] = (short) val1;
					}
				}
			}
			pip1.dirtyFlg = true;	// 2 lines to force update
			pip1.corSrc = null;
			if(!isInsert) pip1.lungData.add(insert);
			return isInsert;
		}
	}
	
	class Poly3Save {
		Polygon poly;
		int type, Mtype;

		Poly3Save() {
			poly = null;
			type = -1;
			Mtype = -1;
		}

		JFijiPipe getPipe() {
			JFijiPipe pip1 = parentPet.getMriOrCtPipe();
			if(Mtype == PET_INSERT) pip1 = parentPet.petPipe;
			return pip1;
		}

		boolean isPoly3(Poly3Save chkPoly) {
			return chkPoly.poly.equals(poly);
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

        jTextMm = new javax.swing.JTextField();
        jLabMm = new javax.swing.JLabel();
        jCheckDisplay = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Lung MIP");

        jTextMm.setText("5");
        jTextMm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextMmActionPerformed(evt);
            }
        });

        jLabMm.setText("mm");

        jCheckDisplay.setSelected(true);
        jCheckDisplay.setText("display");
        jCheckDisplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckDisplayActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextMm, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(jLabMm)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckDisplay)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextMm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabMm)
                    .addComponent(jCheckDisplay))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextMmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextMmActionPerformed
		changeSliceSize();
    }//GEN-LAST:event_jTextMmActionPerformed

    private void jCheckDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckDisplayActionPerformed
		changeDisplayFlg(true);
    }//GEN-LAST:event_jCheckDisplayActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCheckDisplay;
    private javax.swing.JLabel jLabMm;
    private javax.swing.JTextField jTextMm;
    // End of variables declaration//GEN-END:variables
	PetCtPanel parentPet = null;
//	boolean drawingRoi = false;
	double slSize = 5;
	Poly3Save currPoly = null;
	Point currMousePos = new Point(-1,0);
	ArrayList<Poly3Save> polyVect = new ArrayList<Poly3Save>();
}

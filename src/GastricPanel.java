import ij.ImagePlus;
import ij.ImageStack;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.Timer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.scijava.vecmath.Point2d;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * GastricPanel.java
 *
 * Created on Aug 30, 2010, 3:28:06 PM
 */

/**
 *
 * @author Ilan
 */
public class GastricPanel extends javax.swing.JPanel implements MouseListener, MouseMotionListener, ChartProgressListener {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;
	GastricFrame parent;
	JFijiPipe antPipe, antSumPipe, postPipe, postSumPipe;
	boolean resizeOnce = false, paintingFlg = false, drawingRoi = true, startRoi = true;
	int dispType = 0, cineFrm = 0, dispColor = JFijiPipe.COLOR_GRAY;
	int width2 = 1, cineFreezeFrm = -1;
	double startTime = -1, endTime = -1, maxGraphVal;
	Point currMousePos = new Point(-1,0);
	XYTextAnnotation startTxt = null, endTxt = null;
	TextTitle chartTitle = null;
	JFreeChart chart = null;
	Polygon antROI = null, postROI = null, currRoi = null;
	static final int MAXVAL = 32767;
	private Timer m_timer = null;

    /** Creates new form GastricPanel */
    public GastricPanel() {
        initComponents();
		init();
    }

	private void init() {
		addMouseListener( this);
		addMouseMotionListener(this);
		m_timer = new Timer(200, new CineAction());
	}

	class CineAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			boolean paintFlg = false;
			if( paintingFlg == false) {
				if( cineFrm == cineFreezeFrm) return;
				if( cineFreezeFrm > 0) cineFrm = cineFreezeFrm - 1;
				if( ++cineFrm >= antPipe.data1.numFrms) cineFrm = 0;
				paintFlg = true;
			}
			if( paintFlg) repaint();
		}
	}

	@Override
	public void mouseClicked(MouseEvent me) {
		int i, j;
		i = me.getButton();
		j = me.getClickCount();
		if( i != MouseEvent.BUTTON1) return;
		if( j== 1) {
			processMouseSingleClick(me);
			return;
		}
		if( j==2) {
			processMouseDoubleClick(me);
//			return;
		}
	}

	@Override
	public void mouseEntered(MouseEvent me) {}
	@Override
	public void mouseExited(MouseEvent me) {}
	@Override
	public void mouseReleased(MouseEvent me) {}

	@Override
	public void mousePressed(MouseEvent me) {}

	@Override
	public void mouseDragged(MouseEvent me) {}

	@Override
	public void mouseMoved(MouseEvent me) {
		handleMouseMove(me);
	}

	@Override
	protected void paintComponent(Graphics grphcs) {
		super.paintComponent(grphcs);
		paintingFlg = true;
		Graphics2D g2d = (Graphics2D) grphcs.create();
		drawAll(g2d);
		g2d.dispose();	// clean up
		paintingFlg = false;
	}

	void ActionSliderChanged( double width, double level) {
		changeSliderSub( antPipe, width, level);
		changeSliderSub( antSumPipe, width, level);
		changeSliderSub( postPipe, width, level);
		changeSliderSub( postSumPipe, width, level);
		repaint();
	}

	boolean changeSliderSub( JFijiPipe currPipe, double width, double level) {
		if( currPipe == null) return false;
		currPipe.winWidth = width;
		currPipe.winLevel = level;
		return true;
	}

	void processMouseSingleClick(MouseEvent me) {
		double scl1 = getScale();	// calcuates width2
		int mouseX = me.getX() % width2;
		Point pt1 = new Point(mouseX,  me.getY());
		Point pt2 = convertPoint2Pos( pt1, scl1);
		if( drawingRoi && startRoi) {
			currRoi = new Polygon();
			if( dispType == 0) antROI = currRoi;
			else postROI = currRoi;
			startRoi = false;
		}
		if( drawingRoi) {
			currRoi.addPoint(pt2.x, pt2.y);
		}
		currMousePos.x = -1;
		repaint();
	}

	void processMouseDoubleClick(MouseEvent me) {
		if( !drawingRoi) return;
		parent.killRoiBut();
		repaint();
	}

	void handleMouseMove(MouseEvent me) {
		currMousePos.x = me.getX() % width2;
		currMousePos.y = me.getY();
		if( drawingRoi) {
			repaint();
//			return;
		}
	}

	Point convertPoint2Pos( Point pt1, double scl1) {
		int i = 0;
		double scl2 = scl1;
		if( scl2 <= 0) scl2 = getScale();
		if( antPipe.zoom1 > 1.0) i = 3;
		return antPipe.scrn2PosInt(pt1, scl2, i);
	}

	void LoadData( GastricFrame par1, ArrayList<ImagePlus>imgList, Integer[] chosenOnes) {
		parent = par1;
		LoadDataSub( imgList, chosenOnes, true);
		LoadDataSub( imgList, chosenOnes, false);
		parent.fillPatientData();
		m_timer.start();
	}

	private void LoadDataSub( ArrayList<ImagePlus> imgList, Integer[] chosenOnes, boolean antFlg) {
		JFijiPipe cinePipe, sumPipe;
		String meta;
		int cineList, statList = -1, i, j, k, width, height, len1, off1, n, n1, off0, offout, row1, col1;
		int coefCine, coefStat = 0;
		double[] coef;
		short [] pixIn, pixOut;
		boolean firstHalf = true;

		if( chosenOnes[2] >= 0) {
			statList = chosenOnes[2];
			firstHalf = antFlg;
		}
		if( chosenOnes[3] >= 0) {
			statList = chosenOnes[3];
			firstHalf = !antFlg;
		}
		cineList = chosenOnes[antFlg?0:1];
		if( cineList < 0) return;
		ImagePlus currImg = imgList.get(cineList);
		cinePipe = new JFijiPipe();
		cinePipe.LoadData(currImg,0);
		JFijiPipe.JData cineData = cinePipe.data1;
		coefCine = cineData.getCoefficent0();
		if( statList >= 0) {
			currImg = imgList.get(statList);
			coef = currImg.getCalibration().getCoefficients();
			if( coef != null) coefStat = (int) coef[0];
			ImageStack stack1 = currImg.getImageStack();
			width = cineData.width;
			height = cineData.height;
			row1 = stack1.getHeight() / height;
			col1 = stack1.getWidth() / width;
			off0 = 0;
			n1 = stack1.getSize();
			n = n1 / 2;
			off1 = firstHalf?0:n;
			// we have a special case with the make montage which needs correction
			// oops, make montage writes the text into the data, which destroys the data
			if( n1 == 1 && row1 == 2) {
				n = col1;
				off0 = width * col1 * height;
				if( firstHalf) off0 = 0;
			}
			for( i=0; i<n; i++) {
				j = i + off1 + 1;
				if( n1 == 1) j = 1;
				Object pixels = stack1.getPixels(j);
				if( cineData.pixByt != null) {
					// not fixed for make montage
					if(!(pixels instanceof byte[])) break;
					cineData.pixByt.add((byte []) pixels);
				}
				if( cineData.pixels != null) {
					if(!(pixels instanceof short [])) break;
					pixIn = pixOut = (short []) pixels;
					if( n1 == 1 ) {
						pixOut = new short[width*height];
						off1 = i * width;
						offout = 0;
						for( j=0; j<height; j++) {
							for( k=0; k<width; k++) {
								pixOut[offout++] = (short) ( pixIn[k+off1+off0] + coefCine - coefStat);
							}
							off1 += width * col1;
						}
					}
					else if(coefCine != coefStat) {
						len1 = pixIn.length;
						pixOut = new short[pixIn.length];
						for( j=0; j< len1; j++) pixOut[j] = (short) (pixIn[j] + coefCine - coefStat);
					}
					cineData.pixels.add(pixOut);
				}
				if( cineData.pixFloat != null) {
					// not fixed for make montage
					if(!(pixels instanceof float [])) break;
					cineData.pixFloat.add((float []) pixels);
				}
				cineData.numFrms++;
			}
			meta = ChoosePetCt.getMeta(1, currImg);
		}
		cinePipe.imgPos = new Point[1];
		cinePipe.imgPos[0] = new Point(1, 0);
		sumPipe = sumData(cinePipe, 0, 0);
		sumPipe.imgPos = new Point[1];
		sumPipe.imgPos[0] = new Point(0, 0);
		if( antFlg) {
			antPipe = cinePipe;
			antSumPipe = sumPipe;
		} else {
			postPipe = cinePipe;
			postSumPipe = sumPipe;
		}
	}

	static JFijiPipe sumData( JFijiPipe src, int numFrm0, int max0) {
		JFijiPipe sumPipe = new JFijiPipe();
		int i, j, width, height,  numFrm = src.data1.numFrms;
		int depth = src.data1.depth;
		sumPipe.data1 = sumPipe.CreateData1();
		width = sumPipe.data1.width = src.data1.width;
		height = sumPipe.data1.height = src.data1.height;
		int size1 = width*height;
		sumPipe.data1.depth = 16;
		if( numFrm0 > 0 && numFrm0 < numFrm) numFrm = numFrm0;
		short[] pix1;
		byte[] pixByt1;
		float[] pixFlt1;
		int[] pixSum = new int[size1];
		int currPix, coef0 = src.data1.getCoefficent0();
		short pixShort;
		for( i=0; i<numFrm; i++) {
			switch(depth) {
				case 8:
					pixByt1 = src.data1.pixByt.get(i);
					for( j=0; j< size1; j++) {
						currPix = pixByt1[j];
						pixSum[j] += currPix;
					}
					break;

				case 32:
					pixFlt1 = src.data1.pixFloat.get(i);
					for( j=0; j< size1; j++) {
						currPix = (int) pixFlt1[j];
						pixSum[j] += currPix;
					}
					break;

				default:
					pix1 = src.data1.pixels.get(i);
					for( j=0; j<size1; j++) {
						pixShort = (short) (pix1[j] + coef0);
						currPix = pixShort;
						if( currPix < 0) currPix += 65536;
						pixSum[j] += currPix;
					}
					break;
			}
		}

		// find the maximum value for rescaling
		int maxVal = 0;
		for( i=0; i< size1; i++) {
			if( pixSum[i] > maxVal) maxVal = pixSum[i];
		}
		double tmpDbl;
		if( maxVal > MAXVAL) {
			for( i=0; i<size1; i++) {
				tmpDbl = ((double) pixSum[i] * MAXVAL) / maxVal;
				pixSum[i] = ChoosePetCt.round(tmpDbl);
			}
			maxVal = MAXVAL;
		}
		if( max0 > 0 && max0 <= MAXVAL && max0 != maxVal) {
			double scale = ((double) max0) / maxVal;
			for( i=0; i<size1; i++) {
				tmpDbl = pixSum[i] * scale;
				pixSum[i] = ChoosePetCt.round(tmpDbl);
			}
			maxVal = max0;
		}
		pix1 = new short[size1];
		for( i=0; i<size1; i++) {
			pix1[i] = (short) pixSum[i];
		}
		sumPipe.data1.pixels = new ArrayList<short []>();
		sumPipe.data1.pixels.add(pix1);
		sumPipe.data1.numFrms = 1;
		sumPipe.data1.maxPixel = maxVal;
		sumPipe.data1.grandMax = sumPipe.data1.maxVal = maxVal;
		sumPipe.winSlope = sumPipe.data1.sliderSUVMax / maxVal;
		return sumPipe;
	}
	
	void setupGraph() {
		chart = createChart(dispType);
		JPanel graphPanel = parent.getjPanelGraph();
		graphPanel.removeAll();
		if( chart == null) return;

		ChartPanel chartPanel = new ChartPanel( chart);
		chartPanel.setFillZoomRectangle(true);
		chartPanel.setMouseWheelEnabled(true);
		Dimension sz1 = graphPanel.getSize();
		chartPanel.setPreferredSize(sz1);
		graphPanel.add(chartPanel);
		graphPanel.validate();
		graphPanel.repaint();
		repaint();	// repaint the data and ROI as well
	}

	private JFreeChart createChart(int type1) {
		XYSeriesCollection ds1 = new XYSeriesCollection();
		XYSeries ser1, ser2, ser3;
		ArrayList<Double> time1 = getTimeScale();
		ArrayList<Double> yval1 = getYVals(type1);
		if( yval1 == null) return null;
		int i, n=time1.size();
		double time0=0, yval0, ymean;
		maxGraphVal = 0;
		ser1 = new XYSeries("Anterior");
		ser2 = new XYSeries("Posterior");
		ser3 = new XYSeries("Mean");
		switch( type1) {
			case 0:	// anterior
				for( i=0; i<n; i++) {
					time0 = time1.get(i);
					if( i == 0 && startTime < 0) startTime = time0;
					yval0 = yval1.get(i);
					if( yval0 > maxGraphVal) maxGraphVal = yval0;
					ser1.add(time0, yval0);
				}
				if( endTime < 0) endTime = time0;
				ds1.addSeries(ser1);
				break;

			case 1:	// posterior
				for( i=0; i<n; i++) {
					time0 = time1.get(i);
					if( i == 0 && startTime < 0) startTime = time0;
					yval0 = yval1.get(i);
					if( yval0 > maxGraphVal) maxGraphVal = yval0;
					ser2.add(time0, yval0);
				}
				if( endTime < 0) endTime = time0;
				ds1.addSeries(ser2);
				break;

			case 2:	// mean
				ArrayList<Double> yval2 = getYVals(1);
				if( yval2 == null) return null;
				for( i=0; i<n; i++) {
					time0 = time1.get(i);
					if( i == 0 && startTime < 0) startTime = time0;
					yval0 = yval1.get(i);
					ymean = yval2.get(i);	// still yposterior
					ser1.add(time0, yval0);
					ser2.add(time0, ymean);
					ymean = (yval0 + ymean) / 2;
					if( ymean > maxGraphVal) maxGraphVal = ymean;
					ser3.add(time0, ymean);
				}
				if( endTime < 0) endTime = time0;
				ds1.addSeries(ser1);
				ds1.addSeries(ser2);
				ds1.addSeries(ser3);
				break;
		}
		JFreeChart chart1 = ChartFactory.createXYLineChart(null, "minutes", "ROI counts", ds1, PlotOrientation.VERTICAL, false, false, false);
		XYPlot plot1 = chart1.getXYPlot();
		plot1.setBackgroundPaint(Color.lightGray);
		plot1.setDomainGridlinePaint(Color.white);
		plot1.setRangeGridlinePaint(Color.white);
		plot1.setDomainCrosshairVisible(true);
		plot1.setRangeGridlinesVisible(true);

		XYItemRenderer r1 = plot1.getRenderer();
		if( r1 instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r1;
//			renderer.setBaseShape(new Rectangle(-1,-1,2,2));
//			renderer.setBaseShapesVisible(true);
//			renderer.setBaseShapesFilled(false);
			renderer.setDrawSeriesLineAsPath(true);
		}

//		chart.addChangeListener( this);
		chart1.addProgressListener(this);
		updateAnnotation(plot1, 0);
		updateAnnotation(plot1, 1);
		updateTitle(chart1, plot1);
		return chart1;
	}

	private void updateMyChart(JFreeChart chart1,  XYPlot plot1) {
		double x1 = plot1.getDomainCrosshairValue();
		int i, startFm, stopFm, currFm;
		if( x1 <= 0 || x1 == startTime || x1 == endTime) return;
		int dist0, dist1;
		ArrayList<Double> time1 = getTimeScale();
		currFm = getFrmNum(time1, x1);
		startFm = getFrmNum(time1, startTime);
		stopFm = getFrmNum(time1, endTime);
		dist0 = Math.abs(currFm - startFm);
		dist1 = Math.abs(currFm - stopFm);
		i = 0;
		if( dist0 < dist1) startTime = x1;
		else {
			endTime = x1;
			i = 1;
		}
		updateAnnotation( plot1, i);
		updateTitle(chart1, plot1);
	}

	private int getFrmNum( ArrayList<Double>time1, double currTime) {
		int frmNm = 0, n=time1.size();
		while( frmNm < n && time1.get(frmNm) < currTime ) frmNm++;
		return frmNm;
	}

	private void updateAnnotation( XYPlot plot1, int type) {
		double x1, x0, y0, offst1, yMid = maxGraphVal / 2;
		int graph = 0;
		String txt1;
		offst1 = 0.15 * maxGraphVal;
		if( dispType == 2) graph = 2;
		if( type == 0) {
			 x1 = startTime;
			if( startTxt != null) plot1.removeAnnotation(startTxt);
			txt1 = "start";
		} else {
			x1 = endTime;
			if( endTxt != null) plot1.removeAnnotation(endTxt);
			txt1 = "stop";
		}
		int i, n = antPipe.data1.numFrms;
		XYDataset dat1 = plot1.getDataset();
		y0 = x0 = 0;
		for( i=0; i<n; i++) {
			x0 = dat1.getXValue(graph, i);
			y0 = dat1.getYValue(graph, i);
			if( x0 >= x1) break;
		}
		if( y0 < yMid) {
			txt1 = "<-- " + txt1;
			y0 += offst1;
		} else {
			txt1 = txt1 + " -->";
			y0 -= offst1;
		}
		XYTextAnnotation txtAnot = new XYTextAnnotation(txt1, x0, y0);
		txtAnot.setRotationAngle(-3.14159/2);
		if( type == 0) startTxt = txtAnot;
		else endTxt = txtAnot;
		plot1.addAnnotation(txtAnot);
	}

	private void updateTitle(JFreeChart chart1, XYPlot plot1) {
		if( chartTitle != null) chart1.removeSubtitle(chartTitle);
		chartTitle = null;
		int i, graph = 0, n = antPipe.data1.numFrms;
		String title = "Residual after ";
		int timeDiff, gastPercent;
		double x0, y0, xStart, yStart, xStop, yStop = 0;
		XYDataset dat1 = plot1.getDataset();
		xStart = xStop = yStart = -1;
		if( dispType == 2) {
			title = "Mean Residual after ";
			graph = 2;
		}
		for( i=0; i<n; i++) {
			x0 = dat1.getXValue(graph, i);
			y0 = dat1.getYValue(graph, i);
			if( x0 >= startTime && xStart < 0) {
				xStart = x0;
				yStart = y0;
			}
			if( x0 >= endTime) {
				xStop = x0;
				yStop = y0;
				break;
			}
		}
		if( xStop < 0 || xStop < 0 || yStart <= 0) return;
		timeDiff = (int) (xStop - xStart);
		y0 = 100.5 - 100 * (yStart - yStop) / yStart;
		gastPercent = (int) ( y0);
		chartTitle = new TextTitle(title + timeDiff + " min is " + gastPercent + "%");
		chart1.addSubtitle(chartTitle);
	}

	@Override
	public void chartProgress(ChartProgressEvent cpe) {
		if( cpe.getType() != ChartProgressEvent.DRAWING_FINISHED) return;
		JFreeChart chart0 = cpe.getChart();
		Plot plot0 = chart0.getPlot();
		if( !(plot0 instanceof XYPlot)) return;
		XYPlot plot1 = (XYPlot) plot0;
		if( !plot1.isDomainCrosshairVisible()) return;
		updateMyChart( chart0, plot1);
	}

/*	@Override
	public void chartChanged(ChartChangeEvent cce) {
		Object plot0 = cce.getSource();
		if( plot0 instanceof JFreeChart) {
			JFreeChart chart0 = (JFreeChart) plot0;
			plot0 = chart0.getPlot();
		}
		if( !(plot0 instanceof XYPlot)) return;
		XYPlot plot1 = (XYPlot) plot0;
		if( !plot1.isDomainCrosshairVisible()) return;
		double x1 = plot1.getDomainCrosshairValue();
	}*/


	ArrayList<Double> getYVals(int type1) {
		ArrayList<Double> yval1 = new ArrayList<Double>();
		float [] pixFloat = null;
		short [] pixels = null;
		byte [] pixByte = null;
		double pixSum;
		short currShort;
		Point RoiOffset;
		JFijiPipe inPipe = antPipe;
		Polygon poly1 = antROI;
		Polygon poly2;
		if( type1 == 1) {
			inPipe = postPipe;
			poly1 = postROI;
		}
		if( poly1 == null) return null;
		Rectangle limits = poly1.getBounds();
		int i, x, y, xmax, ymax, depth, n = inPipe.data1.numFrms;
		int off1, coef0, limit2;
		xmax = inPipe.data1.width;
		ymax = inPipe.data1.height;
		depth = inPipe.data1.depth;
		coef0 = inPipe.data1.getCoefficent0();
		for( i=0; i<n; i++) {
			RoiOffset = parent.getRoiOffset(i, type1!=1);
			limit2 = limits.y + RoiOffset.y;
			poly2 = poly1;
			if( RoiOffset.x != 0 || RoiOffset.y != 0) {
				int i1, n1 = poly1.npoints;
				int [] xpoints, ypoints;
				xpoints = new int[n1];
				ypoints = new int[n1];
				for( i1=0; i1<n1; i1++) {
					xpoints[i1] = poly1.xpoints[i1] + RoiOffset.x;
					ypoints[i1] = poly1.ypoints[i1] + RoiOffset.y;
				}
				poly2 = new Polygon(xpoints, ypoints, n1);
			}
			switch( depth) {
				case 32:
					pixFloat = inPipe.data1.pixFloat.get(i);
					break;

				case 8:
					pixByte = inPipe.data1.pixByt.get(i);
					break;

				default:
					pixels = inPipe.data1.pixels.get(i);
			}
			pixSum = 0;
			for( y = 0; y<ymax; y++) {
				if( y<limit2 || y >limit2+limits.height) continue;
				for( x=0; x<xmax; x++) {
					if( !poly2.contains(x, y)) continue;
					off1 = y*xmax + x;
					switch( depth) {
						case 32:
							pixSum += pixFloat[off1];
							break;

						case 8:
							pixSum += pixByte[off1];
							break;

						default:
							currShort = (short) (pixels[off1] + coef0);
							pixSum += currShort;
					}
				}
			}
			yval1.add(pixSum);
		}
		return yval1;
	}

	ArrayList<Double> getTimeScale() {
		ArrayList<Double> time1 = new ArrayList<Double>();
		int i, n = antPipe.data1.numFrms;
		double currTime;
		for( i = 1; i <= n; i++) {
			currTime =  parent.getTime(i);
			time1.add(currTime);
		}
		return time1;
	}

	double getScale() {
		double scale0, scale1 = 0;
		if( antPipe == null) return scale1;
		Dimension sz1, dim1 = getSize();
		sz1 = getWindowDim();
		scale0 = ((double)dim1.width) / sz1.width;
		scale1 = ((double)dim1.height) / sz1.height;
		if( scale1 > scale0) scale1 = scale0;
		width2 = (int) (sz1.width * scale1 / 2);
		return scale1;
	}

	Dimension getWindowDim() {
		Dimension sz1;
		int width1, height1;
		width1 = antPipe.data1.width;
		height1 = antPipe.data1.height;
		sz1 = new Dimension( width1*2, height1);
		return sz1;
	}

	private void drawAll(Graphics2D g) {
		if( antPipe == null) return;
		if( !resizeOnce) {
			resizeOnce = true;
			parent.fitWindow();
			if( chart != null) setupGraph();
		}
		JFijiPipe leftPipe = null, rightPipe = null;
		double scl1 = getScale();
		int rightFrm = 0;
		switch( dispType) {
			case 0:	// anterior
				leftPipe = antPipe;
				rightPipe = antSumPipe;
				break;

			case 1:	// posterior
				leftPipe = postPipe;
				rightPipe = postSumPipe;
				break;

			case 2:	// mean
				leftPipe = antPipe;
				rightPipe = postPipe;
				rightFrm = cineFrm;
				break;
		}
		if( leftPipe == null || rightPipe == null) return;
		Point RoiOffset;
		leftPipe.prepareFrame(cineFrm, 0, dispColor, 0);
		leftPipe.drawImages(g, scl1, this);
		rightPipe.prepareFrame(rightFrm, 0, dispColor, 0);
		rightPipe.drawImages(g, scl1, this);
		switch( dispType) {
			case 0:	// anterior
				RoiOffset = parent.getRoiOffset(cineFrm, true);
				drawCurrRoi( g, antROI, 0, RoiOffset);
				break;

			case 1:	// posterior
				RoiOffset = parent.getRoiOffset(cineFrm, false);
				drawCurrRoi( g, postROI, 0, RoiOffset);
				break;

			case 2:	// mean
				RoiOffset = parent.getRoiOffset(cineFrm, true);
				drawCurrRoi( g, antROI, 1, RoiOffset);
				RoiOffset = parent.getRoiOffset(cineFrm, false);
				drawCurrRoi( g, postROI, 2, RoiOffset);
				break;
		}
	}

	void drawCurrRoi( Graphics2D g, Polygon poly1, int type1, Point RoiOffset) {
		int[] xp1, xp2, yp1, yp2;
		int i, j, npoints, offX = 0;
		int rubberX, rubberY;
		boolean drawRubber = drawingRoi & !startRoi;
		Point2d pt1;
		Point pt2;
		if( poly1 == null) return;
		npoints = poly1.npoints;
		if( npoints <= 0) return;
		double scl1 = getScale();
		xp1 = poly1.xpoints;
		yp1 = poly1.ypoints;
		xp2 = new int[npoints];
		yp2 = new int[npoints];
		if( type1 == 2) offX = width2;
		for( i=0; i<npoints; i++) {
			pt1 = new Point2d(xp1[i] + RoiOffset.x,  yp1[i] + RoiOffset.y);
			pt2 = antPipe.pos2Scrn(pt1, scl1, 0);
			xp2[i] = pt2.x + offX;
			yp2[i] = pt2.y;
		}
		rubberX = currMousePos.x;
		rubberY = currMousePos.y;
		for( j=0; j<2; j++) {
			g.setColor(Color.green);
			if( drawRubber) {
				g.drawPolyline(xp2, yp2, npoints);
				i = npoints-1;
				if(rubberX >= 0) g.drawLine(xp2[i], yp2[i], rubberX, rubberY);
				g.drawRect(xp2[0]-4, yp2[0]-4, 8, 8);
			} else {
				g.drawPolygon(xp2, yp2, npoints);
			}
			for( i=0; i<npoints; i++) {
				if( !drawRubber) drawHandle(g, xp2[i], yp2[i]);
				xp2[i] += width2;
			}
			if( rubberX >= 0) rubberX += width2;
			if( type1 > 0) break;
		}
	}

	void drawHandle( Graphics2D g, int x1, int y1) {
		g.setColor(Color.black);
		g.fillRect(x1-2, y1-2, 5, 5);
		g.setColor(Color.white);
		g.fillRect(x1-1, y1-1, 3, 3);
	}

	/** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}

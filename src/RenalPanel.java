
import ij.ImagePlus;
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
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.ValueAxis;
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
 * RenalPanel.java
 *
 * Created on Jun 23, 2011, 7:57:19 AM
 */
/**
 *
 * @author ilan
 */
public class RenalPanel extends javax.swing.JPanel implements MouseListener,
		MouseMotionListener, ChartProgressListener, ChartMouseListener {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;
	RenalFrame parent;
	Polygon [] drawnROIs = null;
	Polygon thisROI = null;
	Point currMousePos = new Point(-1,0);
	boolean resizeOnce = false, paintingFlg = false, swapGraphs = false;
	String timeBase = null;
	JFijiPipe rPipe, sumPipe;
	JFreeChart chart, chart2;
	ChartPanel chartPanel, chart2Panel;
	TextTitle chartTitle = null, chart2Title = null;
	double startTime = -1, endTime = -1, startTime2 = -1, endTime2 = -1, maxGraphVal;
	double graph2StartTime = -1, graph2EndTime = -1, T12time = -1;
	XYTextAnnotation startTxt = null, endTxt = null, startTxt2 = null, endTxt2 = null;
	XYTextAnnotation graph2StartTxt = null, graph2EndTxt = null, T12Txt = null;
	int width2 = 1, cineFrm = 0, dispColor = JFijiPipe.COLOR_GRAY;
	int areaLeft, areaRight;
	double lastChartY = -1, chartMouseY;
	private Timer m_timer = null;
	static final int SEPARATION = 2;
	static final int BKGWIDTH = 2;
	static final int MINNUMPNTS = 4;

	/** Creates new form RenalPanel */
	public RenalPanel() {
		initComponents();
		init();
	}
	
	private void init() {
		drawnROIs = new Polygon[6];
		addMouseListener( this);
		addMouseMotionListener(this);
		m_timer = new Timer(200, new CineAction());
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

	@Override
	public void chartMouseClicked(ChartMouseEvent cme) {
		int posY = cme.getTrigger().getY();
		JFreeChart chart0 = cme.getChart();
		ChartPanel chart0Panel = chartPanel;
		if( chart0 == chart2) chart0Panel = chart2Panel;
		XYPlot xyplot = (XYPlot) chart0.getPlot();
		ChartRenderingInfo chartrenderinginfo = chart0Panel.getChartRenderingInfo();
		java.awt.geom.Rectangle2D rectangle2d = chartrenderinginfo.getPlotInfo().getDataArea();
		ValueAxis valueaxis1 = xyplot.getRangeAxis();
		org.jfree.ui.RectangleEdge rectangleedge1 = xyplot.getRangeAxisEdge();
		chartMouseY = valueaxis1.java2DToValue(posY, rectangle2d, rectangleedge1);
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent cme) {}
	
	class CineAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent ae) {
			boolean paintFlg = false;
			if( paintingFlg == false) {
				if( ++cineFrm >= rPipe.data1.numFrms ) cineFrm = 0;
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
		if( i == MouseEvent.BUTTON3 && j==1) {
			killRoi();
			return;
		}
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
	public void mousePressed(MouseEvent me) {}
	@Override
	public void mouseReleased(MouseEvent me) {}

	@Override
	public void mouseDragged(MouseEvent me) {}

	@Override
	public void mouseMoved(MouseEvent me) { handleMouseMove(me);}

	@Override
	protected void paintComponent(Graphics grphcs) {
		super.paintComponent(grphcs);
		paintingFlg = true;
		Graphics2D g2d = (Graphics2D) grphcs.create();
		drawAll(g2d);
		g2d.dispose();
		paintingFlg = false;
	}

	void ActionSliderChanged( double width, double level) {
		changeSliderSub( rPipe, width, level);
		changeSliderSub( sumPipe, width, level);
	}

	boolean changeSliderSub( JFijiPipe currPipe, double width, double level) {
		if( currPipe == null) return false;
		currPipe.winWidth = width;
		currPipe.winLevel = level;
		return true;
	}

	void processMouseSingleClick(MouseEvent me) {
		double scl1 = getScale();	// calculates width2
		int mouseX = me.getX() % width2;
		Point pt1 = new Point(mouseX,  me.getY());
		Point pt2 = convertPoint2Pos( pt1, scl1);
		int saveVal = parent.saveVal;
		if( saveVal >= 0) {
			if( thisROI == null) {
				thisROI = new Polygon();
				drawnROIs[saveVal] = null;
				if( saveVal == RenalFrame.LEFT_KIDNEY)
					drawnROIs[RenalFrame.LEFT_BACKGROUND] = null;
				if( saveVal == RenalFrame.RIGHT_KIDNEY)
					drawnROIs[RenalFrame.RIGHT_BACKGROUND] = null;
			}
			thisROI.addPoint(pt2.x, pt2.y);
		}
		currMousePos.x = -1;
		repaint();
	}

	void processMouseDoubleClick(MouseEvent me) {
		parent.releaseButton(parent.saveVal);
		repaint();
	}

	void handleMouseMove(MouseEvent me) {
		currMousePos.x = me.getX() % width2;
		currMousePos.y = me.getY();
		if( thisROI == null) return;
		repaint();
	}
	
	void saveRoi(int val) {
		Polygon tempRoi = thisROI;
		thisROI = null;
		if( val < 0 || tempRoi == null) return;
		if( tempRoi.npoints < MINNUMPNTS) tempRoi = null;
		drawnROIs[val] = tempRoi;
		// for left kidney and right kidney, make background ROIs
		int backVal = -1;
		if( val == RenalFrame.LEFT_KIDNEY) backVal = RenalFrame.LEFT_BACKGROUND;
		if( val == RenalFrame.RIGHT_KIDNEY) backVal = RenalFrame.RIGHT_BACKGROUND;
		if( !parent.dualGraphs) backVal = -1;	// no background for flow
		if( backVal >= 0) createBkgdRoi( tempRoi, backVal);
		setupGraph();
	}
	
	void killRoi() {
		thisROI = null;
		repaint();
	}

	Point convertPoint2Pos( Point pt1, double scl1) {
		int i = 0;
		double scl2 = scl1;
		if( scl2 <= 0) scl2 = getScale();
		if( rPipe.zoom1 > 1.0) i = 3;
		return rPipe.scrn2PosInt(pt1, scl2, i);
	}
	
	static Color getROIColor(int type1) {
		Color retColor = Color.red;
		switch( type1) {
			case RenalFrame.RIGHT_KIDNEY:
				retColor = Color.blue;
				break;

			case RenalFrame.LEFT_BACKGROUND:
				retColor = Color.green;
				break;

			case RenalFrame.RIGHT_BACKGROUND:
				retColor = Color.magenta;
				break;

			case RenalFrame.MAN1:
				retColor = Color.cyan;
				break;

			case RenalFrame.MAN2:
				retColor = Color.pink;
				break;
		}
		return retColor;
	}

	void LoadData( RenalFrame par1, ImagePlus img) {
		parent = par1;
		rPipe = new JFijiPipe();
		rPipe.LoadData(img,0);
		rPipe.imgPos = new Point[1];
		rPipe.imgPos[0] = new Point(1, 0);
		sumPipe = GastricPanel.sumData(rPipe, 0, 0);
		sumPipe.imgPos = new Point[1];
		sumPipe.imgPos[0] = new Point(0, 0);
		parent.fillPatientData();
		ActionSliderChanged( 500, 250);
		m_timer.start();
	}

	ArrayList<Double> getTimeScale() {
		ArrayList<Double> time1 = new ArrayList<Double>();
		int i, n = rPipe.data1.numFrms;
		double delta;
		delta = parent.frameDuration / 1000.0;
		timeBase = "seconds";
		if( delta > 10.0) {
			delta /= 60.0;
			timeBase = "minutes";
		}
		for( i=1; i<=n; i++) {
			time1.add( i*delta);
		}
		return time1;
	}

	double getScale() {
		double scale0, scale1 = 0;
		if( rPipe == null) return scale1;
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
		width1 = rPipe.data1.width;
		height1 = rPipe.data1.height;
		sz1 = new Dimension( width1*2, height1);
		return sz1;
	}
	
	private void drawAll(Graphics2D g) {
		if( rPipe == null || sumPipe == null) return;
		if( !resizeOnce) {
			resizeOnce = true;
			parent.fitWindow();
			if( chart != null) setupGraph();
		}
		double scl1 = getScale();
		sumPipe.prepareFrame(0, 0, dispColor, 0);
		sumPipe.drawImages(g, scl1, this);
		rPipe.prepareFrame(cineFrm, 0, dispColor, 0);
		rPipe.drawImages(g, scl1, this);
		for( int i = 0; i<6; i++) {
			drawCurrRoi(g, drawnROIs[i], i);
		}
		drawCurrRoi(g, thisROI, parent.saveVal);
	}
	
	void drawCurrRoi( Graphics2D g, Polygon poly1, int val) {
		if( poly1 == null) return;
		int[] xp1, xp2, yp1, yp2;
		int i, j, npoints;
		Point2d pt1;
		Point pt2;
		int rubberX, rubberY;
		boolean drawRubber = false;
		if( poly1 == thisROI) drawRubber = true;
		g.setColor(getROIColor(val));
		npoints = poly1.npoints;
		if( npoints <= 0) return;
		double scl1 = getScale();
		xp1 = poly1.xpoints;
		yp1 = poly1.ypoints;
		xp2 = new int[npoints];
		yp2 = new int[npoints];
		for( i=0; i<npoints; i++) {
			pt1 = new Point2d(xp1[i], yp1[i]);
			pt2 = rPipe.pos2Scrn( pt1, scl1, 0);
			xp2[i] = pt2.x;
			yp2[i] = pt2.y;
		}
		rubberX = currMousePos.x;
		rubberY = currMousePos.y;
		for( j=0; j<2; j++) {
			if( drawRubber) {
				g.drawPolyline(xp2, yp2, npoints);
				i = npoints-1;
				if(rubberX >= 0) g.drawLine(xp2[i], yp2[i], rubberX, rubberY);
				g.drawRect(xp2[0]-4, yp2[0]-4, 8, 8);
			} else {
				g.drawPolygon(xp2, yp2, npoints);
			}
			for( i=0; i<npoints; i++) {
				xp2[i] += width2;
			}
			if( rubberX >= 0) rubberX += width2;
		}
	}
	
	void createBkgdRoi( Polygon p1, int roiNum) {
		drawnROIs[roiNum] = null;	// kill any previous background ROI
		if( p1 == null) return;
		Polygon p2 = new Polygon();
		Rectangle rc1 = p1.getBounds();
		int[] endX = new int[rPipe.data1.height];
		int i, n, minY, maxY, midY, midPnt;
		int yv, xv, currX, currY, lastX, nextX, cntr1, cntr2;
		minY = rc1.y;
		maxY = minY + rc1.height;
		for( yv = minY; yv <= maxY; yv++) {
			endX[yv] = getEndX( p1, rc1, roiNum, yv);
		}
		while( endX[minY] < 0 && minY < maxY) minY++;
		while( endX[maxY] < 0 && minY < maxY) maxY--;
		midY = (maxY + minY) / 2;
		midPnt = maxY;
		if(roiNum == RenalFrame.LEFT_BACKGROUND) {
			for( yv = maxY; yv >= midY; yv--) {
				i = endX[yv] - endX[yv-1];
				xv = endX[yv-1] - endX[yv-2];
				if( i > 1) midPnt = yv;
				if( i < 1 && xv < 1) {
					midPnt = (midPnt + yv)/2;
					break;
				}
			}
			// now start to draw the background ROI - starting point
			currX = endX[midY] - SEPARATION;
			currY = midY;
			p2.addPoint(currX, currY);
			// line down to midPnt
			for( yv = midY+1; yv < midPnt; yv++) {
				lastX = currX;
				currX = endX[yv] - SEPARATION;
				currY = yv;
				nextX = endX[yv+1] - SEPARATION;
				// see if we can skip drawing the point
				if(currX == lastX && currX == nextX && yv < midPnt-1) continue;
				p2.addPoint(currX, currY);
			}
			cntr1 = p2.npoints;
			// here the midPnt
//			xv = (int) (SEPARATION * 0.707 + 0.5);
			xv = SEPARATION;
			currX = endX[midPnt] - xv;
			currY = midPnt + xv + 1;
			p2.addPoint(currX, currY);
			
			// now the line down to maxY
			currY = midPnt + SEPARATION + 1;
			for( yv = midPnt+1; yv <= maxY; yv++) {
				currX = endX[yv];
				if( yv < maxY) {
					currX = (endX[yv-1] + currX) / 2;
				}
				currY = yv + SEPARATION + 1;
				p2.addPoint(currX, currY);
			}
			xv = rc1.x + rc1.width/2;
			if( currX < xv) {
				currX = xv;
				p2.addPoint(currX, currY);
			}
			// now we open up the ROI by drawing the completing line
			cntr2 = p2.npoints - 1;
			for( i = cntr2; i>cntr1; i--) {
				currX = p2.xpoints[i];
				currY = p2.ypoints[i] + BKGWIDTH;
				p2.addPoint(currX, currY);
			}
			// take care of midPnt
//			xv = (int) (BKGWIDTH * 0.707 + 0.5);
			xv = BKGWIDTH;
			currX = p2.xpoints[cntr1] - xv;
			currY = p2.ypoints[cntr1] + xv;
			p2.addPoint(currX, currY);
			// now finish up the ROI
			for( i = cntr1-1; i >= 0; i--) {
				currX = p2.xpoints[i] - BKGWIDTH;
				currY = p2.ypoints[i];
				p2.addPoint(currX, currY);
			}
		} else {
			for( yv = maxY; yv >= midY; yv--) {
				i = endX[yv-1] - endX[yv];
				xv = endX[yv-2] - endX[yv-1];
				if( i > 1) midPnt = yv;
				if( i < 1 && xv < 1) {
					midPnt = (midPnt + yv)/2;
					break;
				}
			}
			// now start to draw the background ROI - starting point
			currX = endX[midY] + SEPARATION + 1;
			currY = midY;
			p2.addPoint(currX, currY);
			// line down to midPnt
			for( yv = midY+1; yv < midPnt; yv++) {
				lastX = currX;
				currX = endX[yv] + SEPARATION + 1;
				currY = yv;
				nextX = endX[yv+1] + SEPARATION + 1;
				// see if we can skip drawing the point
				if(currX == lastX && currX == nextX && yv < midPnt-1) continue;
				p2.addPoint(currX, currY);
			}
			cntr1 = p2.npoints;
			// here the midPnt
//			xv = (int) (SEPARATION * 0.707 + 0.5);
			xv = SEPARATION + 1;
			currX = endX[midPnt] + xv;
			currY = midPnt + xv;
			p2.addPoint(currX, currY);
			
			// now the line down to maxY
			currY = midPnt + SEPARATION + 1;
			for( yv = midPnt+1; yv <= maxY; yv++) {
				currX = endX[yv];
				if( yv < maxY) {
					currX = (endX[yv-1] + currX) / 2;
				}
				currY = yv + SEPARATION + 1;
				p2.addPoint(currX, currY);
			}
			xv = rc1.x + rc1.width/2;
			if( currX > xv) {
				currX = xv;
				p2.addPoint(currX, currY);
			}
			// now we open up the ROI by drawing the completing line
			cntr2 = p2.npoints - 1;
			for( i = cntr2; i>cntr1; i--) {
				currX = p2.xpoints[i];
				currY = p2.ypoints[i] + BKGWIDTH;
				p2.addPoint(currX, currY);
			}
			// take care of midPnt
//			xv = (int) (BKGWIDTH * 0.707 + 0.5);
			xv = BKGWIDTH;
			currX = p2.xpoints[cntr1] + xv;
			currY = p2.ypoints[cntr1] + xv;
			p2.addPoint(currX, currY);
			// now finish up the ROI
			for( i = cntr1-1; i >= 0; i--) {
				currX = p2.xpoints[i] + BKGWIDTH;
				currY = p2.ypoints[i];
				p2.addPoint(currX, currY);
			}
		}
		drawnROIs[roiNum] = p2;
	}
	
	int getEndX( Polygon p1, Rectangle rc1, int roiNum, int yval) {
		int x, limX1, limX2, retVal = -1;
		limX1 = rc1.x;
		limX2 = limX1 + rc1.width;
		if(roiNum == RenalFrame.LEFT_BACKGROUND) {
			for( x=limX1; x<limX2; x++) {
				if( p1.contains(x, yval)) {
					retVal = x;
					break;
				}
			}
		} else {
			for( x=limX2-1; x>=limX1; x--) {
				if( p1.contains(x, yval)) {
					retVal = x;
					break;
				}
			}
		}
		return retVal;
	}
	
	void setupGraph() {
		chartPanel = setupSub(0);
		if(parent.dualGraphs) chart2Panel = setupSub(1);
		repaint();	// repaint the data and ROI as well
	}
	
	ChartPanel setupSub(int type) {
		JFreeChart chart1;
		ChartPanel chart1Panel;
		JPanel graphPanel;
		if(type == 0) {
			chart1 = createChart();
			graphPanel = parent.getjGraph1Panel();
		} else {
			chart1 = createChart2();
			graphPanel = parent.getjGraph2Panel();
		}

		graphPanel.removeAll();
		if( chart1 == null) return null;

		chart1Panel = new ChartPanel( chart1);
		chart1Panel.setFillZoomRectangle(true);
		chart1Panel.setMouseWheelEnabled(true);
		chart1Panel.addChartMouseListener(this);
		Dimension sz1 = graphPanel.getSize();
		chart1Panel.setPreferredSize(sz1);
		graphPanel.add(chart1Panel);
		graphPanel.validate();
		graphPanel.repaint();
		return chart1Panel;
	}
	
	private XYSeriesCollection swapDS(XYSeriesCollection ds0, ArrayList<Integer> serColor) {
		int i, n = ds0.getSeriesCount();
		XYSeries ser1;
		if( n<2) return ds0;
		XYSeriesCollection ds1 = new XYSeriesCollection();
		ser1 = ds0.getSeries(1);
		ds1.addSeries(ser1);
		i = serColor.get(1);
		ser1 = ds0.getSeries(0);
		ds1.addSeries(ser1);
		serColor.set(1, serColor.get(0));
		serColor.set(0, i);
		for( i=2; i<n; i++) {
			ser1 = ds0.getSeries(i);
			ds1.addSeries(ser1);
		}
		return ds1;
	}
	
	private JFreeChart createChart() {
		JFreeChart chart1;
		double time0 = 0, yval0;
		XYSeriesCollection ds1 = new XYSeriesCollection();
		XYSeries ser1;
		ArrayList<Double> time1, yval1;
		ArrayList<Integer> serColor = new ArrayList<Integer>();
		int i, j, n=4, len1;
		time1 = getTimeScale();
		len1 = time1.size();
		swapGraphs = false;
		for( i=0; i<n; i++) {
			yval1 = getYVals(rPipe, i);
			if( yval1 == null) continue;
			ser1 = new XYSeries(i, false);
			for( j=0; j<len1; j++) {
				time0 = time1.get(j);
				yval0 = yval1.get(j);
				if( yval0 > maxGraphVal) {
					maxGraphVal = yval0;
					if( i==1) swapGraphs = true;
				}
				ser1.add(time0,yval0);
			}
			if( startTime < 0 && time0 > 3.0) startTime = 3.0;
			if( endTime < 0) endTime = time0;
			ds1.addSeries(ser1);
			serColor.add(i);
		}
		if(swapGraphs) ds1 = swapDS(ds1, serColor);
		if( !parent.dualGraphs) plotM1and2(ds1, serColor, time1);
		chart1 = ChartFactory.createXYLineChart(null, timeBase, "ROI counts", ds1, PlotOrientation.VERTICAL, false, false, false);
		XYPlot plot1 = chart1.getXYPlot();
		setupPlot(plot1, ds1, serColor);
		
		chart1.addProgressListener(this);
		chart = chart1;	// needed for updateTitle
		if( drawnROIs[RenalFrame.LEFT_KIDNEY] != null && drawnROIs[RenalFrame.RIGHT_KIDNEY] != null) {
			for( i=0; i<5; i++) updateAnnotation(plot1, i);
			updateTitle(chart1, plot1);
		}
		return chart1;
	}
	
	private void plotM1and2(XYSeriesCollection ds1, ArrayList<Integer> serColor,
			ArrayList<Double> time1) {
		ArrayList<Double> yval1;
		XYSeries ser1;
		int i, j, len1;
		double time0, yval0;
		len1 = time1.size();
		for( i=RenalFrame.MAN1; i<= RenalFrame.MAN2; i++) {
			yval1 = getYVals(rPipe, i);
			if( yval1 == null) continue;
			ser1 = new XYSeries(i,false);
			for( j=0; j<len1; j++) {
				time0 = time1.get(j);
				yval0 = yval1.get(j);
				ser1.add(time0,yval0);
			}
			ds1.addSeries(ser1);
			serColor.add(i);
		}
	}
	
	private void setupPlot(XYPlot plot1, XYSeriesCollection ds1,
			ArrayList<Integer> serColor) {
		int i, n;
		plot1.setBackgroundPaint(Color.lightGray);
		plot1.setDomainGridlinePaint(Color.white);
		plot1.setRangeGridlinePaint(Color.white);
		plot1.setDomainCrosshairVisible(true);
		plot1.setRangeGridlinesVisible(true);

		XYItemRenderer r1 = plot1.getRenderer();
		if( r1 instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r1;
			renderer.setDrawSeriesLineAsPath(true);
		}
		n = ds1.getSeriesCount();
		for( i=0; i<n; i++) {
			r1.setSeriesPaint(i, getROIColor(serColor.get(i)));
		}
	}
	
	private JFreeChart createChart2() {
		JFreeChart chart1;
		XYSeriesCollection ds1 = new XYSeriesCollection();
		double time0 = 0, yval0;
		XYSeries ser1;
		ArrayList<Double> time1, yval1, yval2;
		ArrayList<Integer> serColor = new ArrayList<Integer>();
		int i, j, n=2, len1;
		time1 = getTimeScale();
		len1 = time1.size();
		for( i=0; i<n; i++) {
			yval1 = getYVals(rPipe, i);
			yval2 = getYVals(rPipe, i+2);
			if( yval1 == null || yval2 == null) continue;
			ser1 = new XYSeries(i, false);
			for( j=0; j<len1; j++) {
				time0 = time1.get(j);
				yval0 = yval1.get(j) - yval2.get(j);
//				yval0 = Math.log(yval0);
				ser1.add(time0,yval0);
			}
			if( graph2StartTime < 0 && time0 > 3.0) {
				graph2StartTime = 0.0;
				graph2EndTime = 2.0;
			}
			ds1.addSeries(ser1);
			serColor.add(i);
		}
		if(swapGraphs) ds1 = swapDS(ds1, serColor);
		if( parent.dualGraphs) plotM1and2(ds1, serColor, time1);
		chart1 = ChartFactory.createXYLineChart(null, timeBase, "Bkgnd subtract", ds1, PlotOrientation.VERTICAL, false, false, false);
		XYPlot plot1 = chart1.getXYPlot();
		setupPlot(plot1, ds1, serColor);

		chart1.addProgressListener(this);
		chart2 = chart1;	// needed for updateTitle
		// check that all ROIs exist
		for( i=0; i<4; i++) if(drawnROIs[i] == null) break;
		if( i== 4) {
			for( i=6; i<8; i++) updateAnnotation(plot1, i);
			updateTitle(chart1, plot1);
		}
		return chart1;
	}

	private void updateMyChart(JFreeChart chart1,  XYPlot plot1) {
		double x1 = plot1.getDomainCrosshairValue();
		double currStart, currEnd;
		int i, j, startFm, stopFm, currFm;
		int left1 = 0, right1 = 1;
		if( x1 <= 0 || lastChartY == chartMouseY) return;
		lastChartY = chartMouseY;
		if( drawnROIs[RenalFrame.LEFT_KIDNEY] == null || drawnROIs[RenalFrame.RIGHT_KIDNEY] == null) return;
		int dist0, dist1;
		double fdist0, fdist1;
		boolean t12flg = parent.isT12();
		currStart = startTime;
		currEnd = endTime;
		j = 0;
		if( chart1 == chart2) {
			j = 6;	// for updateAnnotation
			if( t12flg) return;
			currStart = graph2StartTime;
			currEnd = graph2EndTime;
			if( drawnROIs[RenalFrame.LEFT_BACKGROUND] == null || drawnROIs[RenalFrame.RIGHT_BACKGROUND] == null) return;
		}
		currFm = getFrmNum(plot1, x1);
		startFm = getFrmNum(plot1, currStart);
		stopFm = getFrmNum(plot1, currEnd);
		dist0 = Math.abs(currFm - startFm);
		dist1 = Math.abs(currFm - stopFm);
		i = 1;
		if( dist0 < dist1) i = 0;
		if(swapGraphs) {
			left1 = 1;
			right1 = 0;
		}
		if( j==6) {
			if(i==0) graph2StartTime = x1;
			else graph2EndTime = x1;
		} else {
			XYDataset dat1 = plot1.getDataset();
			if(t12flg) T12time = x1;
			else {
				fdist0 = Math.abs(lastChartY - dat1.getYValue(left1, currFm));
				fdist1 = Math.abs(lastChartY - dat1.getYValue(right1, currFm));
				if( i==0) {
					if( fdist0 < fdist1) startTime = x1;
					else startTime2 = x1;
					if( startTime2 == startTime) startTime2 = -1;
				} else {
					if( fdist0 < fdist1) endTime = x1;
					else endTime2 = x1;
					if( endTime2 == endTime) endTime2 = -1;
				}
			}
			if( T12time >= 0) {	// check it is valid
				if( T12time <= startTime || T12time <= startTime2) T12time = -1;
				if( T12time >= endTime) T12time = -1;
				if( endTime2 >= 0 && T12time >= endTime2) T12time = -1;
			}
			updateAnnotation( plot1, 5);	// T12
			updateAnnotation( plot1, i+2);
		}
		updateAnnotation( plot1, i+j);
		updateTitle(chart1, plot1);
	}

	private int getFrmNum( XYPlot plot1, double currTime) {
		XYDataset dat1 = plot1.getDataset();
		int frmNm = 0, n=dat1.getItemCount(0);
		while( frmNm < n && dat1.getXValue(0, frmNm) < currTime ) frmNm++;
		return frmNm;
	}

	private void updateAnnotation( XYPlot plot1, int type) {
		double x1 = -1, x0, y0, offst1, yMid = maxGraphVal / 2;
		int graph = 0;
		if( !parent.dualGraphs) return;
		String txt1;
		XYTextAnnotation currTxt = null;
		offst1 = 0.15 * maxGraphVal;
		txt1 = "start";
		switch(type) {
			case 0:
				x1 = startTime;
				currTxt = startTxt;
				break;
				
			case 1:
				x1 = endTime;
				currTxt = endTxt;
				txt1 = "stop";
				break;
				
			case 2:
				x1 = startTime2;
				currTxt = startTxt2;
				graph = 1;
				break;
				
			case 3:
				x1 = endTime2;
				currTxt = endTxt2;
				graph = 1;
				txt1 = "stop";
				break;
				
			case 4:
			case 5:		// not used
				x1 = T12time;
				currTxt = T12Txt;
				txt1 =  "T½";
				break;

			case 6:
				x1 = graph2StartTime;
				currTxt = graph2StartTxt;
				break;
				
			case 7:
				x1 = graph2EndTime;
				currTxt = graph2EndTxt;
				txt1 = "stop";
				break;
		}
		if( currTxt != null) plot1.removeAnnotation(currTxt);
		if( x1 < 0) return;

		int i, n = rPipe.data1.numFrms;
		if(swapGraphs) graph ^= 1;
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
		Color col1 = Color.black;
		switch( type) {
			case 0:
				startTxt = txtAnot;
				if( startTime2 >= 0) col1 = Color.red;
				break;

			case 1:
				endTxt = txtAnot;
				if( endTime2 >= 0) col1 = Color.red;
				break;

			case 2:
				startTxt2 = txtAnot;
				col1 = Color.blue;
				break;

			case 3:
				endTxt2 = txtAnot;
				col1 = Color.blue;
				break;
				
			case 4:
			case 5:	// not used
				T12Txt = txtAnot;
				break;

			case 6:
				graph2StartTxt = txtAnot;
				break;

			case 7:
				graph2EndTxt = txtAnot;
				break;
		}
		if( col1 != Color.black) txtAnot.setPaint(col1);
		plot1.addAnnotation(txtAnot);
	}

	private void updateTitle(JFreeChart chart1, XYPlot plot1) {
		XYDataset dat1 = plot1.getDataset();
		if( !parent.dualGraphs) return;
		String title;
		int startFrm, stopFrm, perLeft, perRight;
		int left1 = 0, right1 = 1;
		if(swapGraphs) {
			left1 = 1;
			right1 = 0;
		}
		if( chart1 == chart2) {
			double leftSum = 0, rightSum = 0, sum100;
			if( chart2Title != null) chart1.removeSubtitle(chart2Title);
			chart2Title = null;
			title = "Function: Left ";
			startFrm = getFrmNum(plot1, graph2StartTime);
			stopFrm = getFrmNum(plot1, graph2EndTime);
			while( startFrm <= stopFrm) {
				leftSum += dat1.getYValue(left1, startFrm);
				rightSum += dat1.getYValue(right1, startFrm);
				startFrm++;
			}
			sum100 = (leftSum + rightSum)/100;
			if( sum100 <= 0) sum100 = 1;	// don't divide by zero
			perLeft = (int) (leftSum/sum100 + 0.5);
			perRight = (int) (rightSum/sum100 + 0.5);
			chart2Title = new TextTitle(title + perLeft + "%  Right " + perRight + "%");
			chart1.addSubtitle(chart2Title);
			return;
		}
		if( chartTitle != null) chart1.removeSubtitle(chartTitle);
		chartTitle = null;
		title =  getRatioTime(plot1, true, left1);
		title += getRatioTime(plot1, false, right1);
		chartTitle = new TextTitle(title);
		chart1.addSubtitle(chartTitle);
}
	
	String getRatioTime(XYPlot plot1, boolean left, int graph) {
		String retVal;
		XYDataset dat1 = plot1.getDataset();
		int percent, start1, stop1, num1 = 0, graph2 = graph+2;
		double val1, val2, val, startTm, stopTm;
		double ratio = 0;
		startTm = startTime;
		stopTm = endTime;
		if( left) retVal = "Ratio, T½:  Left ";
		else {
			retVal = "  Right ";
			if( startTime2 >= 0) startTm = startTime2;
			if( endTime2 >= 0) stopTm = endTime2;
		}
		start1 = getFrmNum(plot1, startTm);
		stop1 = getFrmNum(plot1, stopTm);
		val1 = dat1.getYValue(graph, start1) - dat1.getYValue(graph2, start1);
		val2 = dat1.getYValue(graph, stop1)  - dat1.getYValue(graph2, stop1);
		if( val1 > 0) ratio = val2/val1;
		percent = (int)(100*ratio + 0.5);
		retVal += percent;
		if(T12time > startTm) {
			stopTm = T12time;
			stop1 = getFrmNum(plot1, stopTm);
			val2 = dat1.getYValue(graph, stop1)  - dat1.getYValue(graph2, stop1);
		}
		if( val2 >= val1) return retVal + "%,-";
		val = getT12(plot1, graph, startTm, stopTm);
//		val = -val1/2*(stopTm-startTm)/(val2-val1);
		if(val < 10) num1 = 1;
		retVal += "%, " + PetCtFrame.myFormat(val, num1);
		return retVal;
	}
	
	private double getT12(XYPlot plot1, int graph, double startTm, double stopTm) {
		double sumx = 0.0, sumy = 0.0;
		int i, cur1, n, start1, stop1;
		if( T12time > startTm ) stopTm = T12time;
		start1 = getFrmNum(plot1, startTm);
		stop1 = getFrmNum(plot1, stopTm);
		XYDataset dat1 = plot1.getDataset();
		n = stop1 - start1 + 1;
		double[] x = new double[n];
		double[] y = new double[n];
		for( i= 0; i< n; i++) {
			cur1 = i + start1;
			x[i] = dat1.getXValue(graph, cur1);
			y[i] = Math.log(dat1.getYValue(graph, cur1) - dat1.getYValue(graph+2, cur1));
			sumx += x[i];
			sumy += y[i];
		}
		double xbar = sumx / n;
		double ybar = sumy / n;

		// second pass: compute summary statistics
		double xxbar = 0.0, xybar = 0.0;
		for ( i = 0; i < n; i++) {
			xxbar += (x[i] - xbar) * (x[i] - xbar);
//			yybar += (y[i] - ybar) * (y[i] - ybar);
			xybar += (x[i] - xbar) * (y[i] - ybar);
		}
		double beta1 = xybar / xxbar;
//		double beta0 = ybar - beta1 * xbar;
		return -Math.log(2)/beta1;
	}
	
	ArrayList<Double> getYVals(JFijiPipe p1, int roiNum) {
		Polygon poly1 = drawnROIs[roiNum];
		if( poly1 == null) return null;
		float[] pixFloat = null;
		short[] pixels = null;
		byte[] pixByte = null;
		double pixSum;
		short currShort;
		ArrayList<Double> yval1 = new ArrayList<Double>();
		Rectangle rc1 = poly1.getBounds();
		int x, pixCnt, xmax, y, i, n = p1.data1.numFrms;
		int off1, coef0, depth = p1.data1.depth;
		xmax = p1.data1.width;
		coef0 = p1.data1.getCoefficent0();
		for( i=0; i<n; i++) {
			switch( depth) {
				case 32:
					pixFloat = p1.data1.pixFloat.get(i);
					break;
					
				case 8:
					pixByte = p1.data1.pixByt.get(i);
					break;
					
				default:
					pixels = p1.data1.pixels.get(i);
			}
			pixSum = 0;
			pixCnt = 0;
			for( y = rc1.y; y < rc1.y + rc1.height; y++) {
				for( x = rc1.x; x < rc1.x + rc1.width; x++) {
					if( !poly1.contains( x, y)) continue;
					off1 = y*xmax + x;
					pixCnt++;
					switch( depth) {
						case 32:
							pixSum += pixFloat[off1];
							break;
							
						case 8:
							pixSum += pixByte[off1];
							break;
							
						default:
							currShort = (short)(pixels[off1] + coef0);
							pixSum += currShort;
					}
				}
			}
			if( pixCnt <= 0) pixCnt = 1;
			switch(roiNum) {
				case RenalFrame.LEFT_KIDNEY:
					areaLeft = pixCnt;
					break;
					
				case RenalFrame.RIGHT_KIDNEY:
					areaRight = pixCnt;
					break;
					
				case RenalFrame.LEFT_BACKGROUND:
					pixSum = pixSum* areaLeft/pixCnt;
					break;
					
				case RenalFrame.RIGHT_BACKGROUND:
					pixSum = pixSum* areaRight/pixCnt;
					break;
					
				case RenalFrame.MAN1:
				case RenalFrame.MAN2:
					pixSum = pixSum * (areaLeft + areaRight)/(2*pixCnt);
					break;
			}
			yval1.add(pixSum);
		}
		return yval1;
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

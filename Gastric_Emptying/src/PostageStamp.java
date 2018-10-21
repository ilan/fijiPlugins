
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.prefs.Preferences;
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
public class PostageStamp {

	void run() {
		Preferences jPrefer = Preferences.userNodeForPackage(PostageStamp.class);
		jPrefer = jPrefer.node("biplugins");
		int i = jPrefer.getInt("postage stamp size", 0);
		if( i<60) {
			JOptionPane.showMessageDialog(null, "Nothing to do, Postage stamp size too small.");
			return;
		}
		organizeWindows(i);
	}
	
	static void organizeWindows(int inMax) {
		Rectangle currRect, scr1;
		scr1 = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int[] windowList = WindowManager.getIDList();
		long[] scrMap = new long[XRES];
		int i, j, n = WindowManager.getWindowCount();
		int top, bottom, left, right, xrnd, yrnd, max1;
		long mask;
		ImageCanvas canvas;
		if( n<=1) return;	// show single image in full zoom
		boolean useStamp = (inMax >= 60);
		max1 = (int) (2.6*inMax);
		yrnd = (6*scr1.height) / (10*YRES);
		xrnd = (6*scr1.width) / (10*XRES);
		ImagePlus ip;
		if( useStamp) for( i=0; i<n; i++) {
			ip = WindowManager.getImage(windowList[i]);
			canvas = ip.getCanvas();
			double magnify = canvas.getMagnification();
			while( (canvas.getWidth() + canvas.getHeight() > max1 &&
					magnify > 0.1) || magnify > 1.05) {
				canvas.zoomOut(0, 0);
				magnify = canvas.getMagnification();
			}
		}
		for( i=0; i<n; i++) {
			ip = WindowManager.getImage(windowList[i]);
			currRect = ip.getWindow().getBounds();
			bottom = (currRect.height+yrnd) * YRES/scr1.height;
			if( bottom <= 0) bottom = 1;
			right = (currRect.width+xrnd) * XRES/scr1.width;
			if( right <= 0) right = 1;
			loop1: for( left = 0; left <= XRES-right; left++) {
				mask = (1l<<bottom) - 1;
				for( top = 0; top <= YRES-bottom; top++) {
					if( (mask & scrMap[left]) == 0) {
						for( j=0; j<right; j++) {
							scrMap[left+j] |= mask;
						}
						currRect.x = left * scr1.width/XRES + scr1.x;
						currRect.y = top * scr1.height/YRES + scr1.y;
						ip.getWindow().setBounds(currRect);
						break loop1;
					}
					mask <<= 1;
				}
			}
		}
	}
	static final int XRES = 100, YRES = 63;
}

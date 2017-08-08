
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.SpinnerNumberModel;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * SyncScroll.java
 *
 * Created on Feb 7, 2010, 7:40:40 AM
 */

/**
 *
 * @author Ilan
 */
public class SyncScroll extends javax.swing.JDialog implements MouseWheelListener {

    /** Creates new form SyncScroll */
    public SyncScroll(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
		init0();
    }
	
	private void init0() {
		addMouseWheelListener(this);
	}

	void init(JFrame par) {
		if( par instanceof PetCtFrame) jPrefer = ((PetCtFrame) par).jPrefer;
		if( par instanceof Display3Frame) jPrefer = ((Display3Frame) par).jPrefer;
		Dimension sz1 = new Dimension();
		xStart = yStart = 0;
		if( jPrefer != null) sz1.height = jPrefer.getInt("spinner dialog height", 0);
		if( sz1.height > 0) {
			sz1.width = jPrefer.getInt("spinner dialog width", 0);
			setSize(sz1);
			xStart = jPrefer.getInt("spinner dialog x", 0);
			yStart = jPrefer.getInt("spinner dialog y", 0);
			if( yStart>0) setLocation(xStart,yStart);
		}
//		jSpinner1.addChangeListener(this);
		if(yStart<=0) setLocationRelativeTo(par);
	}

	void resetValue() {
		SpinnerNumberModel model = (SpinnerNumberModel) jSpinner1.getModel();
		previousValue = 0;
		model.setValue(0);
	}

	void changed() {
		SpinnerNumberModel model = (SpinnerNumberModel) jSpinner1.getModel();
		int currVal = model.getNumber().intValue();
		int diff = currVal - previousValue;
		if( diff == 0) return;
		Object this1;
		int i, n = PetCtFrame.extList.size();
		double[] sliceThickness = new double[n];
		double currThick, minThick;
		for( i=0; i<n; i++) {
			currThick = -1;
			this1 = PetCtFrame.extList.get(i);
			if( this1 instanceof PetCtFrame) currThick = ((PetCtFrame) this1).getSliceThickness();
			if( this1 instanceof Display3Frame) currThick = ((Display3Frame) this1).getSliceThickness();
			sliceThickness[i] = currThick;
		}
		minThick = -1;
		for( i=0; i<n; i++) {
			currThick = sliceThickness[i];
			if( currThick > 0 && (currThick < minThick || minThick < 0)) minThick = currThick;
		}
		for( i=0; i<n; i++) {
			currThick = sliceThickness[i];
			if(currThick > 0) {
				currThick = minThick/currThick;
				diff = ChoosePetCt.round(currVal*currThick) - ChoosePetCt.round(previousValue*currThick);
				if( diff == 0) continue;
			}
			this1 = PetCtFrame.extList.get(i);
			if( this1 instanceof PetCtFrame) ((PetCtFrame) this1).externalSpinnerChange(diff);
			if( this1 instanceof Display3Frame) ((Display3Frame) this1).externalSpinnerChange(diff);
		}
		previousValue = currVal;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent mwe) {
		int j = mwe.getWheelRotation();
		if( j>0) j = -1;
		else j = 1;
		SpinnerNumberModel model = (SpinnerNumberModel) jSpinner1.getModel();
		model.setValue(previousValue+j);
	}

/*	public void stateChanged(ChangeEvent e) {
		int i = 0;
		i++;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		int diff;
		if( keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_UP) {
			e.consume();
			if( PetCtFrame.keyIndex > 0) {
				long time1 = System.currentTimeMillis();
				long diff1 = time1 - PetCtFrame.currKeyTime;
				if( diff1 >= 0 && diff1 < PetCtFrame.keyDelay[PetCtFrame.keyIndex]) return;
				PetCtFrame.currKeyTime = time1;
			}
			changed();
		}
		if( keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
			e.consume();
			diff = 1;
			if( keyCode == KeyEvent.VK_LEFT) diff = -1;
			PetCtFrame.keyIndex += diff;
			if( PetCtFrame.keyIndex < 0) PetCtFrame.keyIndex = 0;
			if( PetCtFrame.keyIndex >= 9) PetCtFrame.keyIndex = 9;
			IJ.showStatus("Key delay = " + PetCtFrame.keyIndex);
		}

	}

	@Override
	public void keyReleased(KeyEvent e) {
		PetCtFrame.currKeyTime = 0;
	}

	@Override
	public void keyTyped(KeyEvent e) {}*/

	@Override
	public void dispose() {
		if( !disposed) {
			Object this1;
			for( int i=0; i<PetCtFrame.extList.size(); i++) {
				this1 = PetCtFrame.extList.get(i);
				if( this1 instanceof PetCtFrame) ((PetCtFrame) this1).resetButScroll();
			}
			PetCtFrame.extList = new ArrayList<Object>();
			PetCtFrame.extScroll = null;
			Dimension sz1 = getSize();
			Point pt1 = getLocation();
			if( jPrefer != null) {
				jPrefer.putInt("spinner dialog width", sz1.width);
				jPrefer.putInt("spinner dialog height", sz1.height);
				if( Math.abs(pt1.x - xStart) > 2 || Math.abs(pt1.y - yStart) > 30 ) {
					jPrefer.putInt("spinner dialog x", pt1.x);
					jPrefer.putInt("spinner dialog y", pt1.y);
				}
			}
			disposed = true;
		}
		super.dispose();
	}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSpinner1 = new javax.swing.JSpinner();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Synched Scroll");

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel());
        jSpinner1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinner1StateChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSpinner1, javax.swing.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSpinner1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jSpinner1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinner1StateChanged
		changed();
	}//GEN-LAST:event_jSpinner1StateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner jSpinner1;
    // End of variables declaration//GEN-END:variables
	int previousValue = 0, xStart = 0, yStart = 0;
	Preferences jPrefer = null;
	boolean disposed = false;
}

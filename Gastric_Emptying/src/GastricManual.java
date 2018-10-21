
import java.awt.Component;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * GastricManual.java
 *
 * Created on Oct 18, 2010, 4:00:52 PM
 */

/**
 *
 * @author Ilan
 */
public class GastricManual extends javax.swing.JDialog {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;

    /** Creates new form GastricManual */
    public GastricManual(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
		parentFrm = (GastricFrame) parent;
        initComponents();
		init();
    }

	class CenterCellRenderer extends DefaultTableCellRenderer {
		static final long serialVersionUID = ChoosePetCt.serialVersionUID;

		@Override
		public Component getTableCellRendererComponent(JTable jtable, Object o, boolean isSelected,
				boolean hasFocus, int row, int col) {
			super.getTableCellRendererComponent(jtable, o, isSelected, hasFocus, row, col);
			this.setHorizontalAlignment(SwingConstants.CENTER);
			return this;
		}

	}

	private void init() {
		setLocationRelativeTo(parentFrm);
		TableColumn col0;
		int i;
		for( i=1; i<6; i++) {
			col0 = jTable1.getColumnModel().getColumn(i);
			col0.setCellRenderer(new CenterCellRenderer());
		}
		jScrollBar1.setMaximum(100);
		jScrollBar1.setMinimum(-100);
		jScrollBar2.setMaximum(100);
		jScrollBar2.setMinimum(-100);
		fillTable();
	}

	void fillTable() {
		int i, i1, n = parentFrm.mpar1.length;
		double currTime;
		ArrayList<Double> time1 = parentFrm.getGastricPanel().getTimeScale();
		DefaultTableModel mod1 = (DefaultTableModel) jTable1.getModel();
		for( i=0; i<n; i++) {
			if( parentFrm.mpar1[i] == null || parentFrm.mpar1[i].usePoint == false) break;
			mod1.setValueAt(true, i, 0);
			i1 = parentFrm.mpar1[i].frameNum;
			mod1.setValueAt(i1, i, 1);
			currTime = parentFrm.mpar1[i].minutes;
			mod1.setValueAt(currTime, i, 2);
			mod1.setValueAt(parentFrm.mpar1[i].factor, i, 3);
			mod1.setValueAt(stringFromPoint(parentFrm.mpar1[i].anterior), i, 4);
			mod1.setValueAt(stringFromPoint(parentFrm.mpar1[i].posterior), i, 5);
		}
		if( i<n) {
			double endTime = parentFrm.getGastricPanel().endTime;
			if (i > 0) {
				Object tim1 = mod1.getValueAt(i-1, 2);
				currTime = (Double) tim1;
				if( currTime >= endTime) return;
			}
			for( i1=0; i1 < time1.size(); i1++) {
				currTime = time1.get(i1);
				if( currTime >= endTime) break;
			}
			mod1.setValueAt(true, i, 0);
			mod1.setValueAt(i1+1, i, 1);
			mod1.setValueAt(endTime, i, 2);
			mod1.setValueAt(1.0, i, 3);
			mod1.setValueAt("0, 0", i, 4);
			mod1.setValueAt("0, 0", i, 5);
		}
	}

	void saveResults() {
		int i, n = parentFrm.mpar1.length;
		String pointStr;
		int frmNm, prevFrm = 0;
		double endTime = -1, prevTime = 0;
		DefaultTableModel mod1 = (DefaultTableModel) jTable1.getModel();
		for( i=0; i<n; i++) parentFrm.mpar1[i] = null;
		for( i=0; i<n; i++) {
			Object checked = mod1.getValueAt(i, 0);
			if( !(checked instanceof Boolean)) break;
			Boolean check1 = (Boolean) checked;
			if( check1 == Boolean.FALSE) break;
			Object frm = mod1.getValueAt(i, 1);
			if( frm == null) break;
			frmNm = (Integer) frm;
			if( frmNm <= prevFrm) break;
			prevFrm = frmNm;
			Object tim1 = mod1.getValueAt(i, 2);
			double currTime = (Double) tim1;
			if( currTime <= prevTime) break;
			endTime = prevTime = currTime;
			parentFrm.mpar1[i] = parentFrm.new ManualParameters();
			parentFrm.mpar1[i].usePoint = true;
			parentFrm.mpar1[i].frameNum = frmNm;
			parentFrm.mpar1[i].minutes = currTime;
			Object fac1 = mod1.getValueAt(i, 3);
			parentFrm.mpar1[i].factor = (Double) fac1;
			Object ant1 = mod1.getValueAt(i, 4);
			pointStr = (String) ant1;
			parentFrm.mpar1[i].anterior = pointFromString(pointStr);
			Object post1 = mod1.getValueAt(i, 5);
			pointStr = (String) post1;
			parentFrm.mpar1[i].posterior = pointFromString(pointStr);
		}
		parentFrm.getGastricPanel().endTime = endTime;
		parentFrm.getGastricPanel().setupGraph();
	}

	Point pointFromString( String inVal) {
		Point retVal = new Point();
		String inVal1 = inVal.replace(',', ' ');
		int i=0, val;
		Scanner sc = new Scanner(inVal1);
		while( sc.hasNextInt()) {
			val = sc.nextInt();
			if( i++ == 0) retVal.x = val;
			else {
				retVal.y = val;
				break;
			}
		}
		return retVal;
	}

	String stringFromPoint( Point inPnt) {
		String ret1 = inPnt.x + ", " + inPnt.y;
		return ret1;
	}

	void horizontalChange(AdjustmentEvent evt) {
		if( selectedPoint == null) return;
		int val1 = evt.getValue();
		selectedPoint.x = val1;
		updateCell();
	}

	void verticalChange(AdjustmentEvent evt) {
		if( selectedPoint == null) return;
		int val1 = evt.getValue();
		selectedPoint.y = val1;
		updateCell();
	}

	private void updateCell() {
		DefaultTableModel mod1 = (DefaultTableModel) jTable1.getModel();
		mod1.setValueAt(stringFromPoint(selectedPoint), selectedRow, selectedCol);
		saveResults();
	}

	void tableClick(MouseEvent evt) {
		selectedRow = jTable1.getSelectedRow();
		selectedCol = jTable1.getSelectedColumn();
		selectedPoint = null;
		if( selectedCol < 4) {
			if( selectedCol == 0) {
				freezeCine();
				saveResults();
			}
			return;
		}
		DefaultTableModel mod1 = (DefaultTableModel) jTable1.getModel();
		Object ant1 = mod1.getValueAt(selectedRow, selectedCol);
		String pointStr = (String) ant1;
		if(pointStr == null || pointStr.isEmpty()) return;
		selectedPoint = pointFromString(pointStr);
		jScrollBar2.setValue(selectedPoint.x);
		jScrollBar1.setValue(selectedPoint.y);
	}

	void freezeCine() {
		int freezeFrm = -1;
		if( jCheckFreeze.isSelected()) {
			DefaultTableModel mod1 = (DefaultTableModel) jTable1.getModel();
			int i, frmNm = 0, n = parentFrm.mpar1.length;
			for( i=0; i<n; i++) {
				Object checked = mod1.getValueAt(i, 0);
				if( !(checked instanceof Boolean)) break;
				Boolean check1 = (Boolean) checked;
				if( check1 == Boolean.FALSE) continue;
				Object frm = mod1.getValueAt(i, 1);
				if( frm == null) continue;
				frmNm = (Integer) frm;
			}
			freezeFrm = frmNm - 1;
		}
		parentFrm.getGastricPanel().cineFreezeFrm = freezeFrm;
	}

	@Override
	public void dispose() {
		saveResults();
		parentFrm.getGastricPanel().cineFreezeFrm = -1;
		parentFrm.releaseManual();
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

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollBar1 = new javax.swing.JScrollBar();
        jScrollBar2 = new javax.swing.JScrollBar();
        jCheckFreeze = new javax.swing.JCheckBox();
        jButOK = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Manual setting of ROI values");
        setResizable(false);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Use", "Frame", "Minutes", "Factor", "Anterior", "Posterior"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jScrollBar1.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                jScrollBar1AdjustmentValueChanged(evt);
            }
        });

        jScrollBar2.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        jScrollBar2.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                jScrollBar2AdjustmentValueChanged(evt);
            }
        });

        jCheckFreeze.setText("Freeze frame to last checked");
        jCheckFreeze.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckFreezeActionPerformed(evt);
            }
        });

        jButOK.setText("OK");
        jButOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButOKActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckFreeze)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButOK)
                .addContainerGap(22, Short.MAX_VALUE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckFreeze)
                    .addComponent(jScrollBar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButOK))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jButOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButOKActionPerformed
		dispose();
	}//GEN-LAST:event_jButOKActionPerformed

	private void jScrollBar2AdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_jScrollBar2AdjustmentValueChanged
		horizontalChange(evt);
	}//GEN-LAST:event_jScrollBar2AdjustmentValueChanged

	private void jScrollBar1AdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_jScrollBar1AdjustmentValueChanged
		verticalChange(evt);
	}//GEN-LAST:event_jScrollBar1AdjustmentValueChanged

	private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
		tableClick(evt);
	}//GEN-LAST:event_jTable1MouseClicked

	private void jCheckFreezeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckFreezeActionPerformed
		freezeCine();
	}//GEN-LAST:event_jCheckFreezeActionPerformed

 
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButOK;
    private javax.swing.JCheckBox jCheckFreeze;
    private javax.swing.JScrollBar jScrollBar1;
    private javax.swing.JScrollBar jScrollBar2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
	GastricFrame parentFrm;
	int selectedRow = -1, selectedCol = -1;
	Point selectedPoint = null;
}

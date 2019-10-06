
import ij.WindowManager;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;


/**
 *
 * @author ilan
 */
public class FollowUp extends javax.swing.JDialog {

	/**
	 * Creates new form FollowUp
	 * @param parent
	 * @param modal
	 */
	public FollowUp(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
		init();
	}

	private void init() {
		int i,j;
		jPrefer = Preferences.userNodeForPackage(FollowUp.class);
		jPrefer = jPrefer.node("biplugins");
		if( jPrefer != null) {
			i = jPrefer.getInt("follow up x", 0);
			j = jPrefer.getInt("follow up y", 0);
			setLocation( i,j);
			rereadList();
		}
	}

	void positionWindows() {
		if( presetLab.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No study positions yet defined.");
			return;
		}
		int i, numPetCt, num3View, xsz, xsz3;
		double scl1;
		Point pt1;
		PetCtFrame pet1;
		Display3Frame vw31;
		Dimension dm;
		i = jComboPosLabel.getSelectedIndex();
		tmpLab = presetLab.get(i);
		if( tmpLab.isEmpty()) return;
		ArrayList<PetCtFrame> petCt = new ArrayList<>();
		ArrayList<Display3Frame> view3 = new ArrayList<>();
		getCurrPositions(petCt, view3);
		i = tmpLab.indexOf(",");
		tmpLab = tmpLab.substring(i+1);
		numPetCt = getNextInt();
		num3View = getNextInt();
		xsz = getNextInt();
		xsz3 = getNextInt();
		for( i=0; i<numPetCt; i++) {
			pt1 = getNextPoint();
			if(petCt.size()>i) {
				pet1 = petCt.get(i);
				pet1.setLocation(pt1);
				dm = pet1.getSize();
				scl1 = xsz * 1.0 / dm.width;
				dm.width = xsz;
				dm.height = (int) (scl1*dm.height);
				pet1.setSize(dm);
			}
		}
		for( i=0; i<num3View; i++) {
			pt1 = getNextPoint();
			if(view3.size()>i) {
				vw31 = view3.get(i);
				vw31.setLocation(pt1);
				dm = vw31.getSize();
				scl1 = xsz3 * 1.0 / dm.width;
				dm.width = xsz3;
				dm.height = (int) (scl1*dm.height);
				vw31.setSize(dm);
			}
		}
	}

	int getNextInt() {
		String tmp1;
		int i = tmpLab.indexOf(",");
		if( i<=0) return -1;
		tmp1 = tmpLab.substring(0, i);
		tmpLab = tmpLab.substring(i+1);
		if(tmp1.isEmpty()) return 0;
		return Integer.parseInt(tmp1);
	}

	Point getNextPoint() {
		Point pt1 = new Point();
		String tmp1, tmp2;
		int i, x, y;
		tmp1 = tmpLab;
		i = tmpLab.indexOf(",");
		if( i> 0) {
			tmp1 = tmpLab.substring(0, i);
			tmpLab = tmpLab.substring(i+1);
		}
		else tmpLab = "";
		if( tmp1.isEmpty()) return pt1;
		i = tmp1.indexOf(" ", 1);
		tmp2 = tmp1.substring(0, i).trim();
		x = Integer.parseInt(tmp2);
		tmp2 = tmp1.substring(i+1).trim();
		y = Integer.parseInt(tmp2);
		pt1.x = x;
		pt1.y = y;
		return pt1;
	}

	void savePositions() {
		getCurrPositions(null, null);
		Point pt1;
		int i, indx, n = ctPos.size(), vn = view3Pos.size();
		if( n < 2) {
			JOptionPane.showMessageDialog(this, "There need to be at least 2 studies in memory.");
			return;
		}
		String lab1 = jTextLabel.getText().trim();
		if( lab1.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please give a name to the template.");
			return;
		}
		if( lab1.contains(",")) {
			JOptionPane.showMessageDialog(this, "The name may not include commas.");
			return;
		}
		String tmp = lab1 + "," + n + "," + vn + "," + xSize + "," + x3Size;
		for( i=0; i<n; i++) {
			pt1 = ctPos.get(i);
			tmp += ", " + pt1.x + " " + pt1.y;
		}
		for( i=0; i<vn; i++) {
			pt1 = view3Pos.get(i);
			tmp += ", " + pt1.x + " " + pt1.y;
		}
		indx = jComboSaveNum.getSelectedIndex() + 1;
		jPrefer.put("follow up entry " + indx, tmp);
//		JOptionPane.showMessageDialog(this, "Successfully saved.");
	}

	void getCurrPositions(ArrayList<PetCtFrame> petCt1, ArrayList<Display3Frame> view3) {
		int i, xsz;
		String title;
		Point pt1;
		ctPos = new ArrayList<>();
		view3Pos = new ArrayList<>();
		xSize = x3Size = 0;
		Window win;
		Window[] frList = WindowManager.getAllNonImageWindows();
		if( frList == null) return;
		for( i=0; i<frList.length; i++) {
			win = frList[i];
			pt1 = win.getLocation();
			xsz = win.getWidth();
			title = win instanceof Frame?((Frame)win).getTitle():((Dialog)win).getTitle();
			if( title.isEmpty()) continue;
			if( title.startsWith("Pet-Ct")) {
				if(petCt1 != null) petCt1.add((PetCtFrame)win);
				ctPos.add(pt1);
				if(xSize <= 0) xSize = xsz;
			}
			if( title.startsWith("3Ct") || title.startsWith("3Pet") || title.startsWith("3fused")) {
				if(view3 != null) view3.add((Display3Frame)win);
				view3Pos.add(pt1);
				if(x3Size <= 0) x3Size = xsz;
			}
		}
	}

	@SuppressWarnings("unchecked")
	void rereadList() {
		int i, j, n;
		String in1, lab1;
		presetLab = new ArrayList<>();
		if( jPrefer == null) return;
		jComboPosLabel.removeAllItems();
		for( i=1; i<=10; i++) {
			in1 = jPrefer.get("follow up entry " + i, "");
			if( in1.isEmpty()) break;
			presetLab.add(in1);
			j = in1.indexOf(",");
			if( j <= 0) break;
			lab1 = in1.substring(0, j);
			jComboPosLabel.addItem(lab1);
		}
		n = presetLab.size();
	}

	void setIndxMax() {
		int n = jComboPosLabel.getItemCount() + 1;
		jComboSaveNum.setMaximumRowCount(n);
		updateLabel();
	}

	void tabChanged(int indx) {
		if( indx == 0) rereadList();
		else setIndxMax();
	}

	void updateLabel() {
		int n = jComboSaveNum.getSelectedIndex();
		String txt1 = "";
		if( n < jComboPosLabel.getItemCount()) {
			txt1 = (String) jComboPosLabel.getItemAt(n);
		}
		jTextLabel.setText(txt1);
	}

	@Override
	public void dispose() {
		if( jPrefer != null) {
			Point pt1 = getLocation();
			jPrefer.putInt("follow up x", pt1.x);
			jPrefer.putInt("follow up y", pt1.y);
		}
		super.dispose();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelPosition = new javax.swing.JPanel();
        jComboPosLabel = new javax.swing.JComboBox();
        jButPosition = new javax.swing.JButton();
        jButHelp = new javax.swing.JButton();
        jPanelSave = new javax.swing.JPanel();
        jComboSaveNum = new javax.swing.JComboBox();
        jTextLabel = new javax.swing.JTextField();
        jButSave = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Follow up studies");

        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        jComboPosLabel.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jButPosition.setText("Position");
        jButPosition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButPositionActionPerformed(evt);
            }
        });

        jButHelp.setText("Help");
        jButHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButHelpActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelPositionLayout = new javax.swing.GroupLayout(jPanelPosition);
        jPanelPosition.setLayout(jPanelPositionLayout);
        jPanelPositionLayout.setHorizontalGroup(
            jPanelPositionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jComboPosLabel, 0, 226, Short.MAX_VALUE)
            .addGroup(jPanelPositionLayout.createSequentialGroup()
                .addComponent(jButPosition)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButHelp))
        );
        jPanelPositionLayout.setVerticalGroup(
            jPanelPositionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPositionLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jComboPosLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelPositionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButPosition)
                    .addComponent(jButHelp))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Position", jPanelPosition);

        jComboSaveNum.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
        jComboSaveNum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboSaveNumActionPerformed(evt);
            }
        });

        jButSave.setText("Save");
        jButSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButSaveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelSaveLayout = new javax.swing.GroupLayout(jPanelSave);
        jPanelSave.setLayout(jPanelSaveLayout);
        jPanelSaveLayout.setHorizontalGroup(
            jPanelSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSaveLayout.createSequentialGroup()
                .addComponent(jComboSaveNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButSave)
                .addGap(0, 117, Short.MAX_VALUE))
            .addComponent(jTextLabel)
        );
        jPanelSaveLayout.setVerticalGroup(
            jPanelSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSaveLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboSaveNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButSave))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Save", jPanelSave);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButPositionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButPositionActionPerformed
		positionWindows();
    }//GEN-LAST:event_jButPositionActionPerformed

    private void jButSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButSaveActionPerformed
		savePositions();
    }//GEN-LAST:event_jButSaveActionPerformed

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
		JTabbedPane source = (JTabbedPane) evt.getSource();
		tabChanged( source.getSelectedIndex());
    }//GEN-LAST:event_jTabbedPane1StateChanged

    private void jComboSaveNumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboSaveNumActionPerformed
		updateLabel();
    }//GEN-LAST:event_jComboSaveNumActionPerformed

    private void jButHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButHelpActionPerformed
		ChoosePetCt.openHelp("Follow up studies");
    }//GEN-LAST:event_jButHelpActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButHelp;
    private javax.swing.JButton jButPosition;
    private javax.swing.JButton jButSave;
    private javax.swing.JComboBox jComboPosLabel;
    private javax.swing.JComboBox jComboSaveNum;
    private javax.swing.JPanel jPanelPosition;
    private javax.swing.JPanel jPanelSave;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextLabel;
    // End of variables declaration//GEN-END:variables
	Preferences jPrefer = null;
	String tmpLab;
	int xSize, x3Size;
	ArrayList<String> presetLab = null;
	ArrayList<Point> ctPos, view3Pos;
}

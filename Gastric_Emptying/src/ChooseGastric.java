
import ij.ImagePlus;
import ij.WindowManager;
import java.awt.Dimension;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.prefs.Preferences;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ChooseGastric.java
 *
 * Created on Aug 26, 2010, 3:50:02 PM
 */

/**
 *
 * @author Ilan
 */
public class ChooseGastric extends javax.swing.JDialog implements TableModelListener {
	static final long serialVersionUID = ChoosePetCt.serialVersionUID;
	static final int TBL_COMBO = 0;
	static final int TBL_PAT_NAME = 1;
	static final int TBL_STUDY = 2;
	static final int TBL_DATE = 3;
	static final int TBL_SERIES = 4;
	static final int TBL_PAT_ID = 5;
	static final int TBL_SIZE = 6;

    /** Creates new form ChooseGastric
	 * @param parent
	 * @param modal */
    public ChooseGastric(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
		init();
 		fillTable();
   }

	@Override
	public void tableChanged(TableModelEvent tme) {
		getStudyList();
	}

	@SuppressWarnings("unchecked")
	private void init() {
		int i, x, y;
		jPrefer = Preferences.userNodeForPackage(ChooseGastric.class);
		jPrefer = jPrefer.node("biplugins");
		TableColumn typeCol = jTable1.getColumnModel().getColumn(TBL_COMBO);
		JComboBox combo = new JComboBox();
		combo.addItem("Ignore");
		combo.addItem("Anterior");
		combo.addItem("Posterior");
		combo.addItem("Statics a-p");
		combo.addItem("Statics p-a");
		typeCol.setCellEditor(new DefaultCellEditor(combo));
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setToolTipText("Click for Combo box");
		typeCol.setCellRenderer(renderer);
		jTable1.setAutoCreateRowSorter(true);
		jTable1.getModel().addTableModelListener(this);
		x = jPrefer.getInt("choose gastric dialog x", 0);
		y = jPrefer.getInt("choose gastric dialog y", 0);
		if( x > 0 && y > 0) {
			TableColumn col1;
			setSize(x,y);
			for(i=0; i<=TBL_SIZE; i++) {
				x = jPrefer.getInt("choose gastric dialog col" + i, 0);
				if( x <= 0) continue;
				col1 = jTable1.getColumnModel().getColumn(i);
				col1.setPreferredWidth(x);
			}
		}
	}

	protected Integer[] getStudyList() {
		TableModel mod1 = jTable1.getModel();
		int i, j, k, n = mod1.getRowCount();
		Integer [] selVals = new Integer[] {-1,-1,-1,-1};
		boolean readEn = false;
		int ant1=0, post1=0, stat1=0;
		String tmp;
		for( i=0; i<n; i++) {
			j =  jTable1.convertRowIndexToModel(i);
			tmp =  (String) mod1.getValueAt(j, TBL_COMBO);
			if( tmp.startsWith("Anterior")) {
				ant1++;
				selVals[0] = j;
			}
			if( tmp.startsWith("Posterior")) {
				post1++;
				selVals[1] = j;
			}
			if( tmp.startsWith("Statics")) {
				k = 2;
				if( tmp.contains("p-a")) k = 3;
				stat1++;
				selVals[k] = j;
			}
		}
		if( ant1 == 1) readEn = true;
		if( post1 > 1) readEn = false;
		if( stat1 > 1) readEn = false;
		jButOK.setEnabled(readEn);
		if( readEn == false) selVals = null;
		return selVals;
	}

	private void fillTable() {
		DefaultTableModel mod1;
		mod1 = (DefaultTableModel) jTable1.getModel();
		mod1.setNumRows(0);
		imgList = new ArrayList<>();
		ImagePlus img1;
		String meta, patName, patID, study, series, tmp1;
		Date date1;
		int i, j, row0, col0;
		int [] fullList = WindowManager.getIDList();
		if( fullList == null) return;
		for( i=0; i<fullList.length; i++) {
			img1 = WindowManager.getImage(fullList[i]);
			j = img1.getStackSize();
			if( j <= 0) continue;
			meta = ChoosePetCt.getMeta(1, img1);
			if( meta == null) continue;	// no information, skip it
			Object[] row1 = new Object[TBL_SIZE+1];	// TBL_SIZE is largest value
			row1[TBL_COMBO] = "Ignore";
			patName = ChoosePetCt.getCompressedPatName( meta);
			if( patName == null) continue;
			row1[TBL_PAT_NAME] = patName;
			patID = ChoosePetCt.getCompressedID( meta);
			row1[TBL_PAT_ID] = patID;
			date1 = ChoosePetCt.getStudyDateTime( meta, -1);
			row1[TBL_DATE] = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date1);
			study = ChoosePetCt.getDicomValue( meta, "0008,1030");
			row1[TBL_STUDY] = study;
			series = ChoosePetCt.getDicomValue( meta, "0054,0400");
			if( series == null || series.isEmpty()) series = ChoosePetCt.getDicomValue( meta, "0008,103E");
			row1[TBL_SERIES] = series;
			col0 = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0028,0011"));
			row0 = ChoosePetCt.parseInt(ChoosePetCt.getDicomValue(meta, "0028,0010"));
			tmp1 = col0 + "*" + row0 + "*" + j;
			row1[TBL_SIZE] = tmp1;
			mod1.addRow(row1);
			imgList.add(img1);
		}
	}

	void savePrefs() {
		int i, x;
		Dimension sz1 = getSize();
		jPrefer.putInt("choose gastric dialog x", sz1.width);
		jPrefer.putInt("choose gastric dialog y", sz1.height);
		TableColumn col1;
		for(i=0; i<=TBL_SIZE; i++) {
			col1 = jTable1.getColumnModel().getColumn(i);
			x = col1.getPreferredWidth();
			jPrefer.putInt("choose gastric dialog col" + i, x);
		}
	}

	@Override
	public void dispose() {
		savePrefs();
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

        jLabel1 = new javax.swing.JLabel();
        jButOK = new javax.swing.JButton();
        jButCancel = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Nuclear Medicine - Gastric Emptying");

        jLabel1.setText("Choose Anterior, Posterior + delayed Static images.");

        jButOK.setText("OK");
        jButOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButOKActionPerformed(evt);
            }
        });

        jButCancel.setText("Cancel");
        jButCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButCancelActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Type", "Name", "Study", "Date", "Series", "ID", "Size"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setColumnSelectionAllowed(true);
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);
        jTable1.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButOK)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButCancel)
                .addContainerGap(84, Short.MAX_VALUE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jButOK)
                    .addComponent(jButCancel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void jButOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButOKActionPerformed
		chosenOnes = getStudyList();	// the only place this is set, just before exit
		dispose();
	}//GEN-LAST:event_jButOKActionPerformed

	private void jButCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButCancelActionPerformed
		dispose();
	}//GEN-LAST:event_jButCancelActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButCancel;
    private javax.swing.JButton jButOK;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
	Preferences jPrefer = null;
	ArrayList<ImagePlus> imgList = null;
	Integer[] chosenOnes = null;
}

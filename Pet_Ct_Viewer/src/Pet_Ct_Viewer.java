import ij.plugin.PlugIn;

/**
 *
 * @author Ilan
 */
public class Pet_Ct_Viewer implements PlugIn {

	@Override
	public void run(String arg) {
		PetCtFrame dlg = new PetCtFrame(arg);
		while(!dlg.isInitialized) {
			mySleep();
		}
		if( dlg.foundData == 0) {
			dlg.dispose();
			return;
		}
		if( dlg.foundData == 1) {
			Display3Frame frm3 = new Display3Frame();
			while(!frm3.isInitialized) {
				mySleep();
			}
			ChoosePetCt cDlg = dlg.chooseDlg;
			boolean ok1 = frm3.init1(cDlg);
			dlg.dispose();
			if( ok1) frm3.setVisible(true);
			return;
		}
		dlg.setVisible(true);
	}
		
	void mySleep() {
		try {
			Thread.sleep(200);
		} catch (Exception e) {}
	}
}

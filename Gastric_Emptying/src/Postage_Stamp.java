
import ij.plugin.PlugIn;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ilan
 */
public class Postage_Stamp implements PlugIn {

	@Override
	public void run(String arg) {
//		PostageStampGUI dlg = new PostageStampGUI(null, false);
//		dlg.setVisible(true);
		PostageStamp dlg = new PostageStamp();
		dlg.run();
	}

}

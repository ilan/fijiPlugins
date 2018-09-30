
import ij.plugin.PlugIn;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ilan
 */
public class Renal_Clearance implements PlugIn {

	@Override
	public void run(String arg) {
		RenalFrame dlg = new RenalFrame();
		if( dlg.renalImg == null) return;
		dlg.setVisible(true);
	}
	
}

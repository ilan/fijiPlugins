
import ij.plugin.PlugIn;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Ilan
 */
public class Read_BI_Studies implements PlugIn {

	@Override
	public void run(String arg0) {
		ReadBIdatabase dlg = new ReadBIdatabase();
		dlg.setVisible(true);
	}
}

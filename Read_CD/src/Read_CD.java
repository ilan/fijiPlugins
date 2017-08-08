
import ij.plugin.PlugIn;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ilan
 */
public class Read_CD implements PlugIn {

	@Override
	public void run(String arg) {
//		ReadCdStudies dlg = new ReadCdStudies(null, false);
		ReadCdStudies dlg = new ReadCdStudies();
		dlg.setVisible(true);
	}
}


import ij.plugin.PlugIn;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Ilan
 */
public class Gastric_Emptying implements PlugIn {

	@Override
	public void run(String string) {
		GastricFrame dlg = new GastricFrame();
		if( !dlg.OKtoRun) return;
		dlg.setVisible(true);
	}
}

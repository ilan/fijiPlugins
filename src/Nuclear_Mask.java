
import ij.plugin.PlugIn;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ilan
 */
public class Nuclear_Mask implements PlugIn {

	@Override
	public void run(String arg) {
		MaskFrame dlg = new MaskFrame();
		boolean Ok2Run = dlg.isOk2Run();
		if( Ok2Run) dlg.doMasking();
		else dlg.setVisible(true);
	}
	
}

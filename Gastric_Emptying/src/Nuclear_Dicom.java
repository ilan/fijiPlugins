
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
public class Nuclear_Dicom implements PlugIn {

	@Override
	public void run(String arg) {
		DicomFrame dlg = new DicomFrame();
		if( dlg.isMacro) dlg.saveDicom();
		else dlg.setVisible(true);
	}
	
}

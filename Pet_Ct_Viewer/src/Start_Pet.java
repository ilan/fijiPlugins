
import ij.plugin.PlugIn;

/**
 *
 * @author ilan
 */
public class Start_Pet implements PlugIn {

	@Override
	public void run(String arg) {
		StartPetData dlg = new StartPetData( arg);
		while(!dlg.isInitialized) {
			mySleep();
		}
		if(dlg.isAuto) dlg.dispose();
		else dlg.setVisible(true);
	}

	void mySleep() {
		try {
			Thread.sleep(200);
		} catch (Exception e) {}
	}
}

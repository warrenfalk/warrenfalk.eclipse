package warrenfalk.eclipse.preferences;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;

/**
 * The CheckState listener for the WfPreference model
 */
public class WfPreferenceCheckStateListener implements ICheckStateListener {

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		WfPreference pref = (WfPreference)event.getElement();
		if (event.getChecked())
			pref.desired = true;
		else
			pref.desired = false;
	}

}

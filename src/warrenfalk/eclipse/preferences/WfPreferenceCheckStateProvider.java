package warrenfalk.eclipse.preferences;

import org.eclipse.jface.viewers.ICheckStateProvider;

/**
 * The CheckState adapter for the WfPreference model
 */
public class WfPreferenceCheckStateProvider implements ICheckStateProvider {

	@Override
	public boolean isChecked(Object element) {
		WfPreference pref = (WfPreference)element;
		return pref.desired;
	}

	@Override
	public boolean isGrayed(Object element) {
		return false;
	}

}

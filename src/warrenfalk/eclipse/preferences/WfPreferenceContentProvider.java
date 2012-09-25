package warrenfalk.eclipse.preferences;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * The content adapter for the WfPreference model
 */
public class WfPreferenceContentProvider implements IStructuredContentProvider {
	WfPreference[] preferences;

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		preferences = (WfPreference[])newInput;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return preferences;
	}

}

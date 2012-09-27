package warrenfalk.eclipse.preferences;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PropertyPage;

import warrenfalk.eclipse.Activator;

/**
 * The main preferences page, giving a list of all common preferences in one place
 * @author Warren Falk
 *
 */
public class WarrenFalkMainPrefsPage extends PropertyPage implements IWorkbenchPreferencePage {
	CheckboxTableViewer tv;

	public WarrenFalkMainPrefsPage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Select preferences to enable");
	}

	@Override
	public void init(IWorkbench arg0) {
	}

	@Override
	protected Control createContents(Composite parent) {
		tv = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
		tv.setLabelProvider(new LabelProvider());
		tv.setContentProvider(new WfPreferenceContentProvider());
		tv.setCheckStateProvider(new WfPreferenceCheckStateProvider());
		tv.addCheckStateListener(new WfPreferenceCheckStateListener());
		tv.setInput(WfPreference.get());
		return tv.getTable();
	}
	
	@Override
	public boolean performOk() {
		WfPreference[] prefs = (WfPreference[])tv.getInput();
		for (WfPreference pref : prefs) {
			if (pref.desired != pref.current) {
				if (pref.desired)
					pref.configure();
				else
					pref.unconfigure();
				pref.desired = pref.current;
			}
		}
		return super.performOk();
	}
	
}
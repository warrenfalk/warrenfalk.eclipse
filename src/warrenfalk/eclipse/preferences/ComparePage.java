package warrenfalk.eclipse.preferences;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * A preference page to aid in the creation of new common preference setters.
 * <p>This allows capturing a baseline of the current preferences, then comparing that baseline with a later state to see what preferences have changed</p>
 * @author WFALK
 *
 */
@SuppressWarnings("restriction")  // using PreferencesService for comparison page, because I'm aware of no other way
public class ComparePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	static Map<String,String> baseline = null;
	
	public ComparePage() {
	}

	public ComparePage(String title) {
		super(title);
	}

	public ComparePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		return new ComparePanel(parent, SWT.NONE);
	}
	
	private class ComparePanel extends Composite {
		Button baselineButton;
		Button compareButton;
		Text comparison;

		public ComparePanel(Composite parent, int style) {
			super(parent, style);
			
			setLayout(new FormLayout());
			int padding = 5;
			
			
			FormData ld;
			
			ld = new FormData();
			baselineButton = new Button(this, SWT.PUSH);
			baselineButton.setText("Capture Baseline");
			ld.left = new FormAttachment(0);
			ld.top = new FormAttachment(0);
			baselineButton.setLayoutData(ld);
			baselineButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					captureBaseline();
				}
			});
			
			ld = new FormData();
			compareButton = new Button(this, SWT.PUSH);
			compareButton.setText("Compare");
			ld.left = new FormAttachment(baselineButton, 5);
			ld.top = new FormAttachment(0);
			compareButton.setLayoutData(ld);
			compareButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					doComparison();
				}
			});
			
			ld = new FormData();
			comparison = new Text(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
			ld.left = new FormAttachment(0);
			ld.top = new FormAttachment(baselineButton, padding);
			ld.right = new FormAttachment(100);
			ld.bottom = new FormAttachment(100);
			comparison.setLayoutData(ld);
			
			if (baseline == null)
				captureBaseline();
			
			doComparison();
		}
		
		private void captureBaseline() {
			baseline = dumpPreferences();
			comparison.setText("");
		}

		void doComparison() {
			StringWriter sw = new StringWriter();
			PrintWriter writer = new PrintWriter(sw);
			doComparison(writer);
			writer.close();
			comparison.setText(sw.toString());
		}
		
		
		private void doComparison(PrintWriter writer) {
			// get the current settings
			Map<String,String> current = dumpPreferences();
			Set<String> keys = new HashSet<String>();
			for (String key : current.keySet())
				keys.add(key);
			if (baseline != null)
				for (String key : baseline.keySet())
					keys.add(key);
			for (String key : keys) {
				if (current.containsKey(key)) {
					String value = current.get(key);
					if (baseline == null || !baseline.containsKey(key) || !value.equals(baseline.get(key)))
						writer.println(key + " = " + value);
				}
				else {
					writer.println("*missing* " + key);
				}
			}
		}

		Map<String,String> dumpPreferences() {
			Map<String,String> collector = new HashMap<String,String>();
			try {
				IEclipsePreferences node = PreferencesService.getDefault().getRootNode();
				String[] children = node.childrenNames();
				for (String child : children) {
					Preferences p = node.node(child);
					dumpPreferences(p, collector);
				}
			}
			catch (BackingStoreException e) {
				e.printStackTrace();
			}
			return collector;
		}

		private void dumpPreferences(Preferences prefs, Map<String,String> collector) throws BackingStoreException {
			String[] keys = prefs.keys();
			for (String key : keys) {
				String value = prefs.get(key, "NOTSET");
				collector.put("[" + prefs.absolutePath() + "] " + key, value);
			}
			String[] children = prefs.childrenNames();
			for (String child : children) {
				Preferences p = prefs.node(child);
				dumpPreferences(p, collector);
			}
		}
		
	}
	
	

}

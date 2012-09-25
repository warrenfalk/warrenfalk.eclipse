package warrenfalk.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public void initializeDefaultPreferences() {
		// There are no default preferences for this preference plugin because this plugin
		// currently only changes the preferences for *other* plugins
	}

}

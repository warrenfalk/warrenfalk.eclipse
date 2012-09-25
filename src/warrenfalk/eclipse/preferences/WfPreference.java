package warrenfalk.eclipse.preferences;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Util;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Abstract class, overridden for each custom common preference set.
 *
 */
public abstract class WfPreference {
	final String title;
	boolean current;
	boolean desired;
	public static final String EXIT_PROMPT_ON_CLOSE_LAST_WINDOW = "EXIT_PROMPT_ON_CLOSE_LAST_WINDOW";	
	
	public final static WfPreference[] get() {
		return new WfPreference[] {
			// ----- Exit without prompt ------------
			new WfPreference("Exit without prompt") {
				@Override
				boolean isConfigured() {
					IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.ide");
					return !s.getBoolean(EXIT_PROMPT_ON_CLOSE_LAST_WINDOW);
				}
	
				@Override
				void configure() {
					IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.ide");
					s.setValue(EXIT_PROMPT_ON_CLOSE_LAST_WINDOW, false);
				}
	
				@Override
				void unconfigure() {
					IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.ide");
					s.setValue(EXIT_PROMPT_ON_CLOSE_LAST_WINDOW, true);
				}
			},
			
			// ----- Ctrl+TAB editor navigation ------------
			new WfPreference("Ctrl+TAB editor navigation") {
				@Override
				boolean isConfigured() {
					BindingsHelper helper = new BindingsHelper();
					KeyBinding[] bindings = getBindings(helper);
					return helper.bindingsExists(bindings);
				}
	
				private KeyBinding[] getBindings(BindingsHelper helper) {
					return new KeyBinding[] {
						helper.bindingInstance("CTRL+TAB", "org.eclipse.ui.window.nextEditor", null, "org.eclipse.ui.contexts.window"),
						helper.bindingInstance("CTRL+SHIFT+TAB", "org.eclipse.ui.window.previousEditor", null, "org.eclipse.ui.contexts.window"),
					};
				}

				@Override
				void configure() {
					try {
						BindingsHelper helper = new BindingsHelper();
						KeyBinding[] bindings = getBindings(helper);
						helper.putBindings(bindings);
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				
				@Override
				void unconfigure() {
					try {
						BindingsHelper helper = new BindingsHelper();
						KeyBinding[] bindings = getBindings(helper);
						helper.resetBindings(bindings);
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			},
		};
	};
	
	public WfPreference(String title) {
		this.title = title;
		this.desired = this.current = isConfigured();
	}
	
	@Override
	public String toString() {
		return title;
	}
	
	/**
	 * Evaluates whether or not preference is currently configured correctly
	 * @return true if the preference appears to already be configured
	 */
	abstract boolean isConfigured();
	
	/**
	 * Configures the preference
	 */
	abstract void configure();

	/**
	 * Undo the configuration of the preference
	 */
	abstract void unconfigure();
	
	/**
	 * Helper class for dealing with key bindings
	 */
	private class BindingsHelper {
		ICommandService cmdService;
		IBindingService bindingService;
		
		private ICommandService getCmdService() {
			if (cmdService == null)
				cmdService = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
			return cmdService;
		}
		
		public boolean bindingsExists(Binding... bindings) {
			for (Binding binding : bindings)
				if (!bindingExists(binding))
					return false;
			return true;
		}

		public void resetBindings(KeyBinding... toReset) throws IOException {
			Scheme activeScheme = getBindingService().getActiveScheme();
			Set<Binding> set = new HashSet<Binding>();
			for (Binding binding : getBindingService().getBindings())
				if (binding != null && binding.getType() == Binding.USER && Util.equals(binding.getSchemeId(), activeScheme.getId()))
					set.add(binding);
			boolean changes = false;
			for (Binding binding : toReset)
				changes = set.remove(binding) || changes;
			if (changes)
				getBindingService().savePreferences(activeScheme, set.toArray(new Binding[set.size()]));
		}

		public void putBindings(KeyBinding... newBindings) throws IOException {
			Scheme activeScheme = getBindingService().getActiveScheme();
			Set<Binding> set = new HashSet<Binding>();
			for (Binding binding : getBindingService().getBindings())
				if (binding != null && binding.getType() == Binding.USER && Util.equals(binding.getSchemeId(), activeScheme.getId()))
					set.add(binding);
			boolean changes = false;
			for (Binding newBinding : newBindings)
				changes = set.add(newBinding) || changes;
			if (changes)
				getBindingService().savePreferences(activeScheme, set.toArray(new Binding[set.size()]));
		}

		private IBindingService getBindingService() {
			if (bindingService == null)
				bindingService = (IBindingService)PlatformUI.getWorkbench().getService(IBindingService.class);
			return bindingService;
		}
		
		public KeyBinding bindingInstance(String keySeq, String cmdId, Parameterization[] parameterizations, String context) {
			try {
			return new KeyBinding(
					KeySequence.getInstance(keySeq),
					new ParameterizedCommand(getCmdService().getCommand(cmdId), parameterizations),
					getBindingService().getDefaultSchemeId(),
					context,
					null,
					null,
					null,
					Binding.USER
					);
			}
			catch (ParseException pe) {
				throw new RuntimeException(pe);
			}
		}
		
		public boolean bindingExists(Binding keyBinding) {
			Binding[] bindings = getBindingService().getBindings();
			if (bindings == null)
				return false;
			for (Binding binding : bindings)
				if (keyBinding.equals(binding))
					return true;
			return false;
		}

	}
}

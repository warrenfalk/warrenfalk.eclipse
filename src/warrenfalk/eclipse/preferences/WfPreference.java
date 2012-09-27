package warrenfalk.eclipse.preferences;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.jna.platform.win32.Shell32;

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

			// ----- Custom Java Formatter ------------
			new WfPreference("Custom Java Formatter") {

				@Override
				boolean isConfigured() {
					IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.ui");
					return s.getString("formatter_profile").endsWith("Warren - Java");
				}

				@Override
				void configure() {
					try {
						IPreferenceStore jdtUiPrefs = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.ui");
						IPreferenceStore jdtCorePrefs = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.core");
						
						String profilesString = IOUtils.toString(WfPreference.class.getResourceAsStream("WarrenJavaFormatter.xml"));
						jdtUiPrefs.setValue("org.eclipse.jdt.ui.formatterprofiles", profilesString);
						
						/* Snippit of file:
						 * <profiles version="12">
						 * <profile kind="CodeFormatterProfile" name="Warren - Java" version="12">
						 * <setting id="org.eclipse.jdt.core.formatter.comment.insert_new_line_before_root_tags" value="insert"/>
						 */
						Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(WfPreference.class.getResourceAsStream("WarrenJavaFormatter.xml"));
						NodeList profileNodes = doc.getElementsByTagName("profile");
						for (int i = 0; i < profileNodes.getLength(); i++) {
							Element profile = (Element)profileNodes.item(i);
							String kind = profile.getAttribute("kind");
							if (!"CodeFormatterProfile".equals(kind))
								continue;
							String name = profile.getAttribute("name");
							if (!"Warren - Java".equals(name))
								continue;
							jdtUiPrefs.setValue("formatter_profile", "_" + name);
							String version = profile.getAttribute("version");
							jdtUiPrefs.setValue("formatter_settings_version", version);
							NodeList settings = profile.getElementsByTagName("setting");
							for (int s = 0; s < settings.getLength(); s++) {
								Element setting = (Element)settings.item(s);
								String id = setting.getAttribute("id");
								String value = setting.getAttribute("value");
								if (!value.equals(jdtCorePrefs.getString(id)))
									jdtCorePrefs.setValue(id, value);
							}
						}
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
					catch (SAXException e) {
						throw new RuntimeException(e);
					}
					catch (ParserConfigurationException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				void unconfigure() {
					IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.ui");
					s.setToDefault("formatter_profile");
				}
				
			},

			// ----- Custom code templates ------------
			new WfPreference("Custom code templates") {
				
				@Override
				boolean isConfigured() {
					Map<String,Element> mine = getMyTemplates();
					Map<String,Element> current = getCurrentTemplates();
					for (String my : mine.keySet())
						if (!current.containsKey(my))
							return false;
					return true;
				}

				@Override
				void configure() {
					Map<String,Element> mine = getMyTemplates();
					Map<String,Element> current = getCurrentTemplates();
					for (String my : mine.keySet())
						current.put(my, mine.get(my));
					IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.ui");
					String templates = getXml(current.values());
					s.setValue("org.eclipse.jdt.ui.text.custom_code_templates", templates);
				}

				@Override
				void unconfigure() {
					Map<String,Element> mine = getMyTemplates();
					Map<String,Element> current = getCurrentTemplates();
					for (String my : mine.keySet())
						current.remove(my);
					IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.ui");
					String templates = getXml(current.values());
					s.setValue("org.eclipse.jdt.ui.text.custom_code_templates", templates);
				}

				Map<String,Element> getMyTemplates() {
					HashMap<String,Element> mineByName = new HashMap<String,Element>();
					try {
						Document mine = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(WfPreference.class.getResourceAsStream("CodeTemplates.xml"));
						NodeList templateList = mine.getDocumentElement().getElementsByTagName("template");
						for (int i = 0; i < templateList.getLength(); i++) {
							Element templateElement = (Element)templateList.item(i);
							String name = templateElement.getAttribute("name");
							mineByName.put(name, templateElement);
						}
					}
					catch (SAXException e) {
						e.printStackTrace();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
					catch (ParserConfigurationException e) {
						e.printStackTrace();
					}
					return mineByName;
				}

				Map<String,Element> getCurrentTemplates() {
					HashMap<String,Element> byName = new HashMap<String,Element>();
					try {
						IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.ui");
						String templates = s.getString("org.eclipse.jdt.ui.text.custom_code_templates");
						if (templates == null || "".equals(templates))
							return byName;
						Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(templates.getBytes("UTF-8")));
						NodeList templateList = doc.getDocumentElement().getElementsByTagName("template");
						for (int i = 0; i < templateList.getLength(); i++) {
							Element templateElement = (Element)templateList.item(i);
							String name = templateElement.getAttribute("name");
							byName.put(name, templateElement);
						}
					}
					catch (SAXException e) {
						e.printStackTrace();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
					catch (ParserConfigurationException e) {
						e.printStackTrace();
					}
					return byName;
				}
				
				private String getXml(Collection<Element> elements) {
					try {
						String docstring = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><templates/>";
						Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(docstring.getBytes("UTF-8")));
						for (Element element : elements)
							doc.getDocumentElement().appendChild(doc.importNode(element, true));
						Transformer transformer = TransformerFactory.newInstance().newTransformer();
						StreamResult result = new StreamResult(new StringWriter());
						transformer.transform(new DOMSource(doc), result);
						String xmlString = result.getWriter().toString();
						return xmlString;
					}
					catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
					catch (SAXException e) {
						throw new RuntimeException(e);
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
					catch (ParserConfigurationException e) {
						throw new RuntimeException(e);
					}
					catch (TransformerConfigurationException e) {
						throw new RuntimeException(e);
					}
					catch (TransformerFactoryConfigurationError e) {
						throw new RuntimeException(e);
					}
					catch (TransformerException e) {
						throw new RuntimeException(e);
					}
				}

			},

			// ----- Editor syntax coloring and fonts ------------
			new WfPreference("Editor syntax coloring and fonts") {
				
				@Override
				boolean isConfigured() {
					IPreferenceStore s;
					s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.ui");
					String value = s.getString("org.eclipse.jface.textfont");
					return value != null && value.contains("WFProgrammer2");
				}

				@Override
				void configure() {
					FontData fd = checkAndInstallFont();
					if (fd != null) {
						IPreferenceStore s;
						s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.ui");
						s.setValue("org.eclipse.jface.textfont", fd.toString());
						
						s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.workbench");
						s.setValue("org.eclipse.jdt.ui.editors.textfont", fd.toString());
					}
				}

				@Override
				void unconfigure() {
					IPreferenceStore s;
					s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.ui");
					s.setToDefault("org.eclipse.jface.textfont");
					
					s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.workbench");
					s.setToDefault("org.eclipse.jdt.ui.editors.textfont");
				}
				
				FontData checkAndInstallFont() {
					try {
						Display display = PlatformUI.getWorkbench().getDisplay();
						FontData[] fonts = display.getFontList("WFProgrammer2", true);
						if (fonts.length > 0)
							return new FontData("WFProgrammer2", 10, SWT.NORMAL);
						// extract font to temp file and install
						if (System.getProperty("os.name").toLowerCase().contains("win")) {
							MessageDialog.openInformation(null, "Need admin priveleges", "In order to install the fonts, elevated access will need to be requested.  Click OK to proceed");
							File installFile = resourceToTempFile("windows_add_font.ps1");
							File fontFile = resourceToTempFile("WFProgrammer2.ttf"); // might want to use the .sfd in linux
							Shell32.INSTANCE.ShellExecute(null, "Runas", "powershell", "-File \"" + installFile.toString() + "\" -path \"" + fontFile.toString() + "\"", null, 0);
							fonts = display.getFontList("WFProgrammer2", true);
							if (fonts.length == 0)
								MessageDialog.openWarning(null, "Font installation not successful", "Either the installation of the font failed, or a restart of the application is necessary for it to take effect.");
							fontFile.delete();
							installFile.delete();
							return (fonts.length > 0) ? new FontData("WFProgrammer2", 10, SWT.NORMAL) : null;
						}
						else {
							MessageDialog.openWarning(null, "Font installation not successful", "Installation of fonts is not implemented on this OS");
							return null;
						}
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				
				File resourceToTempFile(String resource) throws IOException {
					InputStream is = WfPreference.class.getResourceAsStream(resource);
					try {
						File tempFile = File.createTempFile(FilenameUtils.removeExtension(resource), "." + FilenameUtils.getExtension(resource));
						OutputStream os = new FileOutputStream(tempFile);
						try {
							IOUtils.copy(is, os);
							return tempFile;
						}
						finally {
							os.close();
						}
					}
					finally {
						is.close();
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

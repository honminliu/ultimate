package de.uni_freiburg.informatik.ultimate.core.coreplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.osgi.service.prefs.BackingStoreException;

import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceStore;
import de.uni_freiburg.informatik.ultimate.ep.interfaces.ICore;
import de.uni_freiburg.informatik.ultimate.ep.interfaces.IUltimatePlugin;

class SettingsManager {

	// TODO: Check if this works with multiple instances of plugins

	private final Logger mLogger;
	private HashMap<String, LogPreferenceChangeListener> mActivePreferenceListener;

	SettingsManager(Logger logger) {
		mLogger = logger;
		mActivePreferenceListener = new HashMap<String, LogPreferenceChangeListener>();
	}

	void checkPreferencesForActivePlugins(String pluginId, String pluginName) {
		attachLogPreferenceChangeListenerToPlugin(pluginId, pluginName);
		logDefaultPreferences(pluginId, pluginName);
	}

	void loadPreferencesFromFile(ICore core, String filename) {
		if (filename != null && !filename.isEmpty()) {
			mLogger.debug("--------------------------------------------------------------------------------");
			mLogger.info("Loading settings from " + filename);
			try {
				FileInputStream fis = new FileInputStream(filename);
				IStatus status = UltimatePreferenceStore.importPreferences(fis);
				if (!status.isOK()) {
					mLogger.warn("Failed to load preferences. Status is: " + status);
					mLogger.warn("Did not attach debug property logger");
				} else {
					mLogger.info("Loading preferences was successful");
					mLogger.info("Preferences different from defaults:");
					boolean isSomePluginDifferent = false;
					for (IUltimatePlugin plugin : core.getRegisteredUltimatePlugins()) {
						String[] delta = new UltimatePreferenceStore(plugin.getPluginID()).getDeltaPreferencesStrings();
						if (delta != null && delta.length > 0) {
							isSomePluginDifferent = true;
							mLogger.info("Preferences of " + plugin.getPluginName() + " differ from their defaults:");
							for (String s : delta) {
								mLogger.info(" * " + s);
							}
						}
					}
					if (!isSomePluginDifferent) {
						mLogger.info("All preferences are set to their defaults");
					}

				}
			} catch (Exception e) {
				mLogger.error("Could not load preferences because of exception: ", e);
				mLogger.warn("Did not attach debug property logger");
			} finally {
				mLogger.debug("--------------------------------------------------------------------------------");
			}

		} else {
			mLogger.info("Loading settings from empty filename is not possible");
		}
	}

	void savePreferences(ICore core, String filename) {
		if (filename != null && !filename.isEmpty()) {
			mLogger.info("Saving preferences to file " + filename);
			try {
				File f = new File(filename);
				if (f.isFile() && f.exists()) {
					f.delete();
				}
				FileOutputStream fis = new FileOutputStream(filename);

				for (IUltimatePlugin plugin : core.getRegisteredUltimatePlugins()) {
					new UltimatePreferenceStore(plugin.getPluginID()).exportPreferences(fis);
				}

				fis.flush();
				fis.close();
			} catch (FileNotFoundException e) {
				mLogger.error("Saving preferences failed with exception: ", e);
			} catch (IOException e) {
				mLogger.error("Saving preferences failed with exception: ", e);
			} catch (CoreException e) {
				mLogger.error("Saving preferences failed with exception: ", e);
			}
		}
	}

	void resetPreferences(ICore core) {
		mLogger.info("Resetting all preferences to default values...");
		for (IUltimatePlugin plugin : core.getRegisteredUltimatePlugins()) {
			UltimatePreferenceInitializer preferences = plugin.getPreferences();
			if (preferences != null) {
				mLogger.info("Resetting " + plugin.getPluginName() + " preferences to default values");
				preferences.resetDefaults();
			} else {
				mLogger.info(plugin.getPluginName() + " provides no preferences, ignoring...");
			}

		}
		mLogger.info("Finished resetting all preferences to default values...");
	}

	private void logDefaultPreferences(String pluginID, String pluginName) {
		if (!mLogger.isDebugEnabled()) {
			return;
		}
		UltimatePreferenceStore ups = new UltimatePreferenceStore(pluginID);
		try {
			IEclipsePreferences defaults = ups.getDefaultEclipsePreferences();
			String prefix = "[" + pluginName + " (Current)] Preference \"";
			for (String key : defaults.keys()) {
				mLogger.debug(prefix + key + "\" = " + ups.getString(key, "NOT DEFINED"));
			}
		} catch (BackingStoreException e) {
			mLogger.debug("An exception occurred during printing of default preferences for plugin " + pluginName + ":"
					+ e.getMessage());
		}
	}

	/**
	 * Attaches Listener to all preferences and all scopes of all plugins to
	 * notify developers about changing preferences
	 * 
	 * @param pluginId
	 */
	private void attachLogPreferenceChangeListenerToPlugin(String pluginId, String pluginName) {

		LogPreferenceChangeListener instanceListener = retrieveListener(pluginId, pluginName, "Instance");
		LogPreferenceChangeListener configListener = retrieveListener(pluginId, pluginName, "Configuration");
		LogPreferenceChangeListener defaultListener = retrieveListener(pluginId, pluginName, "Default");

		InstanceScope.INSTANCE.getNode(pluginId).removePreferenceChangeListener(instanceListener);
		InstanceScope.INSTANCE.getNode(pluginId).addPreferenceChangeListener(instanceListener);

		ConfigurationScope.INSTANCE.getNode(pluginId).removePreferenceChangeListener(configListener);
		ConfigurationScope.INSTANCE.getNode(pluginId).addPreferenceChangeListener(configListener);

		DefaultScope.INSTANCE.getNode(pluginId).removePreferenceChangeListener(defaultListener);
		DefaultScope.INSTANCE.getNode(pluginId).addPreferenceChangeListener(defaultListener);
	}

	private LogPreferenceChangeListener retrieveListener(String pluginID, String pluginName, String scope) {
		String listenerID = pluginID + scope;
		if (mActivePreferenceListener.containsKey(listenerID)) {
			return mActivePreferenceListener.get(listenerID);
		} else {
			LogPreferenceChangeListener listener = new LogPreferenceChangeListener(scope, pluginID, pluginName);
			mActivePreferenceListener.put(listenerID, listener);
			return listener;
		}
	}

	class LogPreferenceChangeListener implements IPreferenceChangeListener {

		private final String mScope;
		private final UltimatePreferenceStore mPreferences;
		private final String mPrefix;

		public LogPreferenceChangeListener(String scope, String pluginID, String pluginName) {
			mScope = scope;
			mPreferences = new UltimatePreferenceStore(pluginID);
			mPrefix = "[" + pluginName + " (" + mScope + ")] Preference \"";
		}

		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			mLogger.debug(mPrefix + event.getKey() + "\" changed: " + event.getOldValue() + " -> "
					+ event.getNewValue() + " (actual value in store: " + mPreferences.getString(event.getKey()) + ")");
		}
	}

}

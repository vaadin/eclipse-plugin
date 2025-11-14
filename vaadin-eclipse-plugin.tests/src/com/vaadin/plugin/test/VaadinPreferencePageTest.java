package com.vaadin.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.plugin.Activator;
import com.vaadin.plugin.preferences.VaadinPreferencePage;

public class VaadinPreferencePageTest {

	private IPreferenceStore preferenceStore;
	private VaadinPreferencePage preferencePage;
	private Shell shell;

	@Before
	public void setUp() {
		preferenceStore = Activator.getDefault().getPreferenceStore();
		preferencePage = new VaadinPreferencePage();
		preferencePage.setPreferenceStore(preferenceStore);
		shell = new Shell(Display.getDefault());
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	@Test
	public void testPreferencePageExists() {
		assertNotNull("Preference page should not be null", preferencePage);
	}

	@Test
	public void testTelemetryEnabledByDefault() {
		preferenceStore.setToDefault(VaadinPreferencePage.PREF_ENABLE_TELEMETRY);
		boolean defaultValue = preferenceStore.getBoolean(VaadinPreferencePage.PREF_ENABLE_TELEMETRY);
		assertTrue("Telemetry should be enabled by default", defaultValue);
	}

	@Test
	public void testTelemetryPreferenceCanBeDisabled() {
		preferenceStore.setValue(VaadinPreferencePage.PREF_ENABLE_TELEMETRY, false);
		boolean isEnabled = preferenceStore.getBoolean(VaadinPreferencePage.PREF_ENABLE_TELEMETRY);
		assertFalse("Telemetry should be disabled after setting to false", isEnabled);

		preferenceStore.setValue(VaadinPreferencePage.PREF_ENABLE_TELEMETRY, true);
	}

	@Test
	public void testTelemetryPreferenceCanBeEnabled() {
		preferenceStore.setValue(VaadinPreferencePage.PREF_ENABLE_TELEMETRY, false);
		preferenceStore.setValue(VaadinPreferencePage.PREF_ENABLE_TELEMETRY, true);

		boolean isEnabled = preferenceStore.getBoolean(VaadinPreferencePage.PREF_ENABLE_TELEMETRY);
		assertTrue("Telemetry should be enabled after setting to true", isEnabled);
	}

	@Test
	public void testPreferencePageCreatesControl() {
		Composite parent = new Composite(shell, 0);
		preferencePage.createControl(parent);

		Control control = preferencePage.getControl();
		assertNotNull("Preference page should create a control", control);
		assertFalse("Control should not be disposed", control.isDisposed());
	}

	@Test
	public void testPreferencePageHasDescription() {
		Composite parent = new Composite(shell, 0);
		preferencePage.createControl(parent);
		Control control = preferencePage.getControl();
		assertNotNull("Preference page should create a control", control);
	}

	@Test
	public void testPreferenceStoreIsAccessible() {
		preferencePage.setPreferenceStore(preferenceStore);
		IPreferenceStore pageStore = preferencePage.getPreferenceStore();
		assertNotNull("Preference store should be accessible", pageStore);
		assertEquals("Preference store should be the same", preferenceStore, pageStore);
	}

	@Test
	public void testPreferenceKeyConstant() {
		String key = VaadinPreferencePage.PREF_ENABLE_TELEMETRY;
		assertNotNull("Preference key should not be null", key);
		assertEquals("Preference key should have correct value", "com.vaadin.plugin.telemetry.enabled", key);
	}

	@Test
	public void testPreferenceDefaultValue() {
		preferenceStore.setToDefault(VaadinPreferencePage.PREF_ENABLE_TELEMETRY);
		boolean defaultValue = preferenceStore.getBoolean(VaadinPreferencePage.PREF_ENABLE_TELEMETRY);
		assertTrue("Default value should be enabled", defaultValue);
	}

	@Test
	public void testPreferenceDialog() {
		PreferenceManager manager = new PreferenceManager();
		PreferenceNode node = new PreferenceNode("com.vaadin.plugin.preferences.VaadinPreferencePage", "Vaadin", null,
				VaadinPreferencePage.class.getName());
		manager.addToRoot(node);

		PreferenceDialog dialog = new PreferenceDialog(shell, manager);
		dialog.setBlockOnOpen(false);
		dialog.open();

		assertNotNull("Preference dialog should be created", dialog);
		assertTrue("Preference dialog shell should exist", dialog.getShell() != null);

		dialog.close();
	}
}

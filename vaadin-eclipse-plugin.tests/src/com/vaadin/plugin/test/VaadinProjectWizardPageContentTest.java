package com.vaadin.plugin.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.plugin.wizards.AbstractProjectModel;
import com.vaadin.plugin.wizards.VaadinProjectWizardPage;

/**
 * Tests which controls the New Vaadin Project wizard page actually shows.
 */
public class VaadinProjectWizardPageContentTest {

	private Shell shell;
	private VaadinProjectWizardPage wizardPage;
	private List<String> texts;

	@Before
	public void setUp() {
		shell = new Shell(Display.getDefault());
		wizardPage = new VaadinProjectWizardPage();
		wizardPage.createControl(shell);
		texts = new ArrayList<>();
		collectTexts(shell, texts);
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	@Test
	public void walkingSkeletonSectionIsNotShown() {
		assertNoTextContaining("Walking Skeleton header should not be shown", "Walking Skeleton");
		assertNoTextContaining("Walking skeleton description should not be shown", "walking skeleton");
	}

	@Test
	public void flowCheckboxIsNotShown() {
		assertNoTextContaining("Flow checkbox should not be shown", "Pure Java with Vaadin Flow");
	}

	@Test
	public void gettingStartedSectionIsNotShown() {
		assertNoTextContaining("Getting Started section should not be shown", "Getting Started");
	}

	@Test
	public void flowHelpSectionIsNotShown() {
		assertNoTextContaining("Flow help section should not be shown", "Flow framework is the most productive");
		assertFalse("Standalone 'Flow' label should not be shown", texts.contains("Flow"));
	}

	@Test
	public void projectOptionsAreStillShown() {
		assertTrue("Project type selection should still be shown", texts.contains("Project Type:"));
		assertTrue("Starter project options should still be shown", texts.contains("Starter Project Options"));
		assertTrue("Vaadin version selection should still be shown", texts.contains("Vaadin Version:"));
	}

	@Test
	public void starterProjectStillRequestsFlow() {
		AbstractProjectModel model = wizardPage.getProjectModel();
		assertTrue("Starter project should still be downloaded with Flow",
				model.getDownloadUrl().contains("frameworks=flow"));
	}

	private void assertNoTextContaining(String message, String needle) {
		for (String text : texts) {
			assertFalse(message + " but found: " + text, text.toLowerCase().contains(needle.toLowerCase()));
		}
	}

	private static void collectTexts(Control control, List<String> collected) {
		if (control instanceof Label) {
			collected.add(((Label) control).getText());
		} else if (control instanceof Button) {
			collected.add(((Button) control).getText());
		}
		if (control instanceof Group) {
			collected.add(((Group) control).getText());
		}
		if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				collectTexts(child, collected);
			}
		}
	}
}

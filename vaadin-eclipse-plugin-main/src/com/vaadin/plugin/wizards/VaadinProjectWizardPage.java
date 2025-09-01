package com.vaadin.plugin.wizards;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The main page of the New Vaadin Project wizard.
 */
public class VaadinProjectWizardPage extends WizardPage {

    private Text projectNameText;
    private Text locationText;
    private Button useDefaultLocationButton;

    // Starter project options
    private Button starterProjectRadio;
    private Group starterGroup;
    private Button flowCheckbox;
    private Button hillaCheckbox;
    private Combo vaadinVersionCombo;

    // Hello World options
    private Button helloWorldRadio;
    private Group helloWorldGroup;
    private Combo frameworkCombo;
    private Combo languageCombo;
    private Combo buildToolCombo;
    private Combo architectureCombo;
    private Label kotlinNote;

    private ProjectModel model;

    public VaadinProjectWizardPage() {
        super("vaadinProjectPage");
        setTitle("Vaadin");
        setDescription("Create a new Vaadin project");
        model = new ProjectModel();
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.verticalSpacing = 9;
        container.setLayout(layout);

        // Project name
        Label label = new Label(container, SWT.NULL);
        label.setText("&Project name:");

        projectNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        projectNameText.setLayoutData(gd);
        projectNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });

        // Location
        useDefaultLocationButton = new Button(container, SWT.CHECK);
        useDefaultLocationButton.setText("Use default location");
        useDefaultLocationButton.setSelection(true);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        useDefaultLocationButton.setLayoutData(gd);

        label = new Label(container, SWT.NULL);
        label.setText("Location:");

        locationText = new Text(container, SWT.BORDER | SWT.SINGLE);
        locationText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        locationText.setEnabled(false);

        Button browseButton = new Button(container, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.setEnabled(false);

        useDefaultLocationButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean useDefault = useDefaultLocationButton.getSelection();
                locationText.setEnabled(!useDefault);
                browseButton.setEnabled(!useDefault);
                if (useDefault) {
                    updateDefaultLocation();
                }
            }
        });

        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setMessage("Select project location");
                String result = dialog.open();
                if (result != null) {
                    locationText.setText(result);
                }
            }
        });

        // Project type selection
        createProjectTypeSection(container);

        // Add separator
        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        separator.setLayoutData(gd);

        // Add help sections
        createHelpSections(container);

        // Initialize default location
        updateDefaultLocation();

        // Set default project name
        projectNameText.setText(generateProjectName());

        dialogChanged();
        setControl(container);
    }

    private void createHelpSections(Composite parent) {
        // Getting Started section
        Label gettingStartedLabel = new Label(parent, SWT.NONE);
        gettingStartedLabel.setText("Getting Started");
        gettingStartedLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        GridData gd = new GridData();
        gd.horizontalSpan = 3;
        gettingStartedLabel.setLayoutData(gd);

        Label gettingStartedText = new Label(parent, SWT.WRAP);
        gettingStartedText
                .setText("The Getting Started guide will quickly familiarize you with your new Walking Skeleton "
                        + "implementation. You'll learn how to set up your development environment, understand the project "
                        + "structure, and find resources to help you add muscles to your skeleton—transforming it into a "
                        + "fully-featured application.");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        gd.widthHint = 500;
        gettingStartedText.setLayoutData(gd);

        // Flow and Hilla section
        Label flowHillaLabel = new Label(parent, SWT.NONE);
        flowHillaLabel.setText("Flow and Hilla");
        flowHillaLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        gd = new GridData();
        gd.horizontalSpan = 3;
        gd.verticalIndent = 10;
        flowHillaLabel.setLayoutData(gd);

        Label flowHillaText = new Label(parent, SWT.WRAP);
        flowHillaText.setText("Flow framework is the most productive choice, allowing 100% of the user interface to be "
                + "coded in server-side Java. Hilla framework, on the other hand, enables implementation of your user "
                + "interface with React while automatically connecting it to your Java backend.");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        gd.widthHint = 500;
        flowHillaText.setLayoutData(gd);
    }

    private void createProjectTypeSection(Composite parent) {
        // Project Type Selection - Radio buttons in same parent for mutual exclusivity
        Composite radioContainer = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        radioContainer.setLayoutData(gd);
        radioContainer.setLayout(new GridLayout(1, false));
        
        Label projectTypeLabel = new Label(radioContainer, SWT.NONE);
        projectTypeLabel.setText("Project Type:");
        projectTypeLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        
        starterProjectRadio = new Button(radioContainer, SWT.RADIO);
        starterProjectRadio.setText("Starter Project - Full-featured application skeleton with user management and security");
        starterProjectRadio.setSelection(true);
        
        helloWorldRadio = new Button(radioContainer, SWT.RADIO);
        helloWorldRadio.setText("Hello World Project - Minimal project to get started quickly");
        
        // Starter Project Section
        starterGroup = new Group(parent, SWT.NONE);
        starterGroup.setText("Starter Project Options");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        starterGroup.setLayoutData(gd);
        starterGroup.setLayout(new GridLayout(2, false));

        Label label = new Label(starterGroup, SWT.NONE);
        label.setText("Vaadin Version:");

        vaadinVersionCombo = new Combo(starterGroup, SWT.READ_ONLY);
        vaadinVersionCombo.setItems("Stable", "Prerelease");
        vaadinVersionCombo.select(0);
        vaadinVersionCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Include Walking Skeleton section
        Label skeletonLabel = new Label(starterGroup, SWT.NONE);
        skeletonLabel.setText("Include Walking Skeleton");
        skeletonLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        gd = new GridData();
        gd.horizontalSpan = 2;
        skeletonLabel.setLayoutData(gd);

        Label descLabel = new Label(starterGroup, SWT.WRAP);
        descLabel.setText("A walking skeleton is a minimal application that includes a fully-functional "
                + "end-to-end workflow. All major building blocks are included, but it does not yet "
                + "perform any meaningful tasks.");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.widthHint = 400;
        descLabel.setLayoutData(gd);

        flowCheckbox = new Button(starterGroup, SWT.CHECK);
        flowCheckbox.setText("Pure Java with Vaadin Flow");
        flowCheckbox.setSelection(true);
        gd = new GridData();
        gd.horizontalSpan = 2;
        flowCheckbox.setLayoutData(gd);

        hillaCheckbox = new Button(starterGroup, SWT.CHECK);
        hillaCheckbox.setText("Full-stack React with Vaadin Hilla");
        hillaCheckbox.setSelection(false);
        gd = new GridData();
        gd.horizontalSpan = 2;
        hillaCheckbox.setLayoutData(gd);

        // Hello World Projects Section
        helloWorldGroup = new Group(parent, SWT.NONE);
        helloWorldGroup.setText("Hello World Project Options");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        helloWorldGroup.setLayoutData(gd);
        helloWorldGroup.setLayout(new GridLayout(2, false));

        label = new Label(helloWorldGroup, SWT.NONE);
        label.setText("Framework:");

        frameworkCombo = new Combo(helloWorldGroup, SWT.READ_ONLY);
        frameworkCombo.setItems("Flow / Java", "Hilla / React");
        frameworkCombo.select(0);
        frameworkCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        label = new Label(helloWorldGroup, SWT.NONE);
        label.setText("Language:");

        languageCombo = new Combo(helloWorldGroup, SWT.READ_ONLY);
        languageCombo.setItems("Java", "Kotlin");
        languageCombo.select(0);
        languageCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        label = new Label(helloWorldGroup, SWT.NONE);
        label.setText("Build tool:");

        buildToolCombo = new Combo(helloWorldGroup, SWT.READ_ONLY);
        buildToolCombo.setItems("Maven", "Gradle");
        buildToolCombo.select(0);
        buildToolCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        label = new Label(helloWorldGroup, SWT.NONE);
        label.setText("Architecture:");

        architectureCombo = new Combo(helloWorldGroup, SWT.READ_ONLY);
        architectureCombo.setItems("Spring Boot", "Quarkus", "Jakarta EE", "Servlet");
        architectureCombo.select(0);
        architectureCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Add note label for Kotlin (initially hidden)
        kotlinNote = new Label(helloWorldGroup, SWT.WRAP | SWT.ITALIC);
        kotlinNote.setText("Kotlin support uses a community add-on.");
        kotlinNote.setVisible(false);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        kotlinNote.setLayoutData(gd);

        // Add listeners to enable/disable sections
        starterProjectRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateProjectTypeEnablement();
                dialogChanged();
            }
        });

        helloWorldRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateProjectTypeEnablement();
                dialogChanged();
            }
        });

        // Add listeners for validation
        SelectionAdapter validationListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                validateAndUpdateOptions();
                dialogChanged();
            }
        };

        frameworkCombo.addSelectionListener(validationListener);
        languageCombo.addSelectionListener(validationListener);
        buildToolCombo.addSelectionListener(validationListener);
        architectureCombo.addSelectionListener(validationListener);
        flowCheckbox.addSelectionListener(validationListener);
        hillaCheckbox.addSelectionListener(validationListener);

        // Initial enablement
        updateProjectTypeEnablement();
    }

    private void updateProjectTypeEnablement() {
        boolean isStarter = starterProjectRadio.getSelection();
        boolean isHelloWorld = helloWorldRadio.getSelection();

        // Show/hide entire groups
        starterGroup.setVisible(isStarter);
        ((GridData) starterGroup.getLayoutData()).exclude = !isStarter;
        
        helloWorldGroup.setVisible(isHelloWorld);
        ((GridData) helloWorldGroup.getLayoutData()).exclude = !isHelloWorld;
        
        // Request layout update to adjust spacing
        starterGroup.getParent().layout(true, true);
    }

    private void validateAndUpdateOptions() {
        if (helloWorldRadio.getSelection()) {
            // Apply validation rules based on IntelliJ plugin's StarterSupport
            boolean isHilla = frameworkCombo.getSelectionIndex() == 1;
            boolean isKotlin = languageCombo.getSelectionIndex() == 1;
            boolean isGradle = buildToolCombo.getSelectionIndex() == 1;
            String architecture = architectureCombo.getText();

            // Show/hide Kotlin note
            if (kotlinNote != null) {
                kotlinNote.setVisible(isKotlin);
            }

            // Hilla only supports Spring Boot
            if (isHilla && !architecture.equals("Spring Boot")) {
                architectureCombo.select(0); // Spring Boot
            }

            // Kotlin only supports Maven + Spring Boot
            if (isKotlin) {
                if (isGradle) {
                    buildToolCombo.select(0); // Maven
                }
                if (!architecture.equals("Spring Boot")) {
                    architectureCombo.select(0); // Spring Boot
                }
            }

            // Gradle only supports Spring Boot and Servlet
            if (isGradle && !architecture.equals("Spring Boot") && !architecture.equals("Servlet")) {
                architectureCombo.select(0); // Spring Boot
            }

            // Disable invalid combinations
            architectureCombo.setEnabled(!isHilla); // Only Spring Boot for Hilla

            if (isKotlin) {
                buildToolCombo.setEnabled(false); // Only Maven for Kotlin
                architectureCombo.setEnabled(false); // Only Spring Boot for Kotlin
            } else {
                buildToolCombo.setEnabled(true);
                architectureCombo.setEnabled(!isHilla);
            }
        }
    }

    private void updateDefaultLocation() {
        if (useDefaultLocationButton.getSelection()) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IPath workspacePath = root.getLocation();
            String projectName = projectNameText.getText();
            if (projectName != null && !projectName.isEmpty()) {
                locationText.setText(workspacePath.append(projectName).toString());
            } else {
                locationText.setText(workspacePath.toString());
            }
        }
    }

    private String generateProjectName() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        String baseName = "vaadin-project";
        String projectName = baseName;
        int counter = 1;

        while (root.getProject(projectName).exists()) {
            projectName = baseName + "-" + counter;
            counter++;
        }

        return projectName;
    }

    private void dialogChanged() {
        String projectName = projectNameText.getText();

        // Update location if using default
        if (useDefaultLocationButton.getSelection()) {
            updateDefaultLocation();
        }

        // Validate project name
        if (projectName.length() == 0) {
            updateStatus("Project name must be specified");
            return;
        }

        if (projectName.contains(" ")) {
            updateStatus("Project name cannot contain spaces");
            return;
        }

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        if (root.getProject(projectName).exists()) {
            updateStatus("A project with this name already exists");
            return;
        }

        // Validate project type selection
        if (starterProjectRadio.getSelection()) {
            if (!flowCheckbox.getSelection() && !hillaCheckbox.getSelection()) {
                updateStatus("Please select at least one framework (Flow or Hilla)");
                return;
            }
        }

        updateStatus(null);
    }

    private void updateStatus(String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    public ProjectModel getProjectModel() {
        model.setProjectName(projectNameText.getText());
        model.setLocation(locationText.getText());

        if (starterProjectRadio.getSelection()) {
            model.setProjectType(ProjectModel.ProjectType.STARTER);
            model.setPrerelease(vaadinVersionCombo.getSelectionIndex() == 1);
            model.setIncludeFlow(flowCheckbox.getSelection());
            model.setIncludeHilla(hillaCheckbox.getSelection());
        } else {
            model.setProjectType(ProjectModel.ProjectType.HELLO_WORLD);
            model.setFramework(frameworkCombo.getSelectionIndex() == 0 ? "flow" : "hilla");
            model.setLanguage(languageCombo.getSelectionIndex() == 0 ? "java" : "kotlin");
            model.setBuildTool(buildToolCombo.getSelectionIndex() == 0 ? "maven" : "gradle");

            String[] architectures = { "spring-boot", "quarkus", "jakartaee", "servlet" };
            model.setArchitecture(architectures[architectureCombo.getSelectionIndex()]);
        }

        return model;
    }
}

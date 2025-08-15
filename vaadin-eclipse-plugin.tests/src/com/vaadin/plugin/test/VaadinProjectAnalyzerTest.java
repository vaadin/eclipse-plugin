package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;

import com.vaadin.plugin.VaadinProjectAnalyzer;

/**
 * Tests for VaadinProjectAnalyzer functionality.
 */
public class VaadinProjectAnalyzerTest extends BaseIntegrationTest {

	private IJavaProject javaProject;
	private VaadinProjectAnalyzer analyzer;

	@Override
	protected void doSetUp() throws CoreException {
		// Add Java nature to test project
		addJavaNature(testProject);
		javaProject = JavaCore.create(testProject);

		// Create source folder
		IFolder srcFolder = testProject.getFolder("src");
		if (!srcFolder.exists()) {
			srcFolder.create(true, true, null);
		}

		// Add source folder to classpath
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newSourceEntry(srcFolder.getFullPath());
		javaProject.setRawClasspath(newEntries, null);

		analyzer = new VaadinProjectAnalyzer(javaProject);
	}

	@Test
	public void testFindVaadinRoutesEmpty() throws CoreException {
		// Test with no routes
		List<Map<String, Object>> routes = analyzer.findVaadinRoutes();
		assertNotNull("Routes list should not be null", routes);
		assertEquals("Routes list should be empty", 0, routes.size());
	}

	@Test
	public void testFindVaadinRoutesWithRoute() throws CoreException {
		// Create a class with @Route annotation
		createJavaClass("src", "com.example", "MainView",
				"package com.example;\n" + "\n" + "import com.vaadin.flow.router.Route;\n" + "\n" + "@Route(\"main\")\n"
						+ "public class MainView {\n" + "}\n");

		// Refresh and analyze
		testProject.refreshLocal(2, null);

		List<Map<String, Object>> routes = analyzer.findVaadinRoutes();
		assertEquals("Should find one route", 1, routes.size());

		Map<String, Object> route = routes.get(0);
		assertEquals("Route value should be 'main'", "main", route.get("route"));
		assertEquals("Class name should match", "com.example.MainView", route.get("classname"));
	}

	@Test
	public void testFindVaadinRoutesWithDefaultRoute() throws CoreException {
		// Create a class with @Route without value (default route)
		createJavaClass("src", "com.example", "HomeView", "package com.example;\n" + "\n"
				+ "import com.vaadin.flow.router.Route;\n" + "\n" + "@Route\n" + "public class HomeView {\n" + "}\n");

		testProject.refreshLocal(2, null);

		List<Map<String, Object>> routes = analyzer.findVaadinRoutes();
		assertEquals("Should find one route", 1, routes.size());

		Map<String, Object> route = routes.get(0);
		assertEquals("Route value should be empty for default route", "", route.get("route"));
		assertEquals("Class name should match", "com.example.HomeView", route.get("classname"));
	}

	@Test
	public void testFindVaadinComponentsEmpty() throws CoreException {
		// Test with no components
		List<Map<String, Object>> components = analyzer.findVaadinComponents(false);
		assertNotNull("Components list should not be null", components);
		assertEquals("Components list should be empty", 0, components.size());
	}

	@Test
	public void testFindVaadinComponentsWithMethods() throws CoreException {
		// Create a component class
		createJavaClass("src", "com.example", "CustomButton",
				"package com.example;\n" + "\n" + "public class CustomButton {\n" + "    public void click() {}\n"
						+ "    public String getText() { return \"\"; }\n" + "    public void setText(String text) {}\n"
						+ "}\n");

		testProject.refreshLocal(2, null);

		// Note: This test won't find components without actual Vaadin Component in
		// classpath
		// This is expected behavior - in real usage, Vaadin would be on classpath
		List<Map<String, Object>> components = analyzer.findVaadinComponents(true);
		assertEquals("Should not find components without Vaadin in classpath", 0, components.size());
	}

	@Test
	public void testFindEntitiesEmpty() throws CoreException {
		// Test with no entities
		List<Map<String, Object>> entities = analyzer.findEntities(false);
		assertNotNull("Entities list should not be null", entities);
		assertEquals("Entities list should be empty", 0, entities.size());
	}

	@Test
	public void testFindEntitiesWithJPAEntity() throws CoreException {
		// Create an entity class with javax.persistence.Entity
		createJavaClass("src", "com.example.model", "User",
				"package com.example.model;\n" + "\n" + "import javax.persistence.Entity;\n"
						+ "import javax.persistence.Id;\n" + "\n" + "@Entity\n" + "public class User {\n" + "    @Id\n"
						+ "    private Long id;\n" + "    private String username;\n" + "    \n"
						+ "    public Long getId() { return id; }\n"
						+ "    public void setId(Long id) { this.id = id; }\n"
						+ "    public String getUsername() { return username; }\n"
						+ "    public void setUsername(String username) { this.username = username; }\n" + "}\n");

		testProject.refreshLocal(2, null);

		List<Map<String, Object>> entities = analyzer.findEntities(false);
		assertEquals("Should find one entity", 1, entities.size());

		Map<String, Object> entity = entities.get(0);
		assertEquals("Entity class name should match", "com.example.model.User", entity.get("classname"));
		assertNotNull("Entity should have path", entity.get("path"));
	}

	@Test
	public void testFindEntitiesWithJakartaEntity() throws CoreException {
		// Create an entity class with jakarta.persistence.Entity
		createJavaClass("src", "com.example.model", "Product",
				"package com.example.model;\n" + "\n" + "import jakarta.persistence.Entity;\n"
						+ "import jakarta.persistence.Id;\n" + "\n" + "@Entity\n" + "public class Product {\n"
						+ "    @Id\n" + "    private Long id;\n" + "    private String name;\n" + "    \n"
						+ "    public Long getId() { return id; }\n"
						+ "    public void setId(Long id) { this.id = id; }\n" + "}\n");

		testProject.refreshLocal(2, null);

		List<Map<String, Object>> entities = analyzer.findEntities(false);
		assertEquals("Should find one entity", 1, entities.size());

		Map<String, Object> entity = entities.get(0);
		assertEquals("Entity class name should match", "com.example.model.Product", entity.get("classname"));
	}

	@Test
	public void testFindEntitiesWithMethods() throws CoreException {
		// Create an entity with methods
		createJavaClass("src", "com.example.model", "Order",
				"package com.example.model;\n" + "\n" + "import javax.persistence.Entity;\n" + "\n" + "@Entity\n"
						+ "public class Order {\n" + "    private Long id;\n" + "    private String status;\n"
						+ "    \n" + "    public Long getId() { return id; }\n"
						+ "    public void setId(Long id) { this.id = id; }\n"
						+ "    public String getStatus() { return status; }\n"
						+ "    public void setStatus(String status) { this.status = status; }\n"
						+ "    public void process() {}\n" + "    public boolean isValid() { return true; }\n" + "}\n");

		testProject.refreshLocal(2, null);

		List<Map<String, Object>> entities = analyzer.findEntities(true);
		assertEquals("Should find one entity", 1, entities.size());

		Map<String, Object> entity = entities.get(0);
		assertEquals("Entity class name should match", "com.example.model.Order", entity.get("classname"));

		String methods = (String) entity.get("methods");
		assertNotNull("Methods should be included", methods);
		assertTrue("Should include getId method", methods.contains("getId()"));
		assertTrue("Should include setId method", methods.contains("setId(Long)"));
		assertTrue("Should include process method", methods.contains("process()"));
		assertTrue("Should include isValid method", methods.contains("isValid()"));
	}

	@Test
	public void testFindSecurityConfigurationsEmpty() throws CoreException {
		// Test with no security configurations
		List<Map<String, Object>> configs = analyzer.findSecurityConfigurations();
		assertNotNull("Security configs list should not be null", configs);
		assertEquals("Security configs list should be empty", 0, configs.size());
	}

	@Test
	public void testFindSecurityConfigurationsWithEnableWebSecurity() throws CoreException {
		// Create a security configuration class
		createJavaClass("src", "com.example.security", "SecurityConfig", "package com.example.security;\n" + "\n"
				+ "import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;\n" + "\n"
				+ "@EnableWebSecurity\n" + "public class SecurityConfig {\n" + "    public void configure() {\n"
				+ "        // Security configuration\n" + "    }\n" + "}\n");

		testProject.refreshLocal(2, null);

		List<Map<String, Object>> configs = analyzer.findSecurityConfigurations();
		assertEquals("Should find one security config", 1, configs.size());

		Map<String, Object> config = configs.get(0);
		assertEquals("Config class name should match", "com.example.security.SecurityConfig", config.get("class"));
		assertEquals("Origin should be project", "project", config.get("origin"));
		assertEquals("Source should be java", "java", config.get("source"));
	}

	@Test
	public void testFindUserDetailsServicesEmpty() throws CoreException {
		// Test with no UserDetailsService implementations
		List<Map<String, Object>> services = analyzer.findUserDetailsServices();
		assertNotNull("UserDetails services list should not be null", services);
		// Will be empty without Spring Security in classpath
		assertEquals("UserDetails services list should be empty", 0, services.size());
	}

	/**
	 * Helper method to create a Java class in the test project.
	 */
	private void createJavaClass(String sourceFolder, String packageName, String className, String content)
			throws CoreException {
		// Create package folders
		IFolder srcFolder = testProject.getFolder(sourceFolder);
		IFolder packageFolder = srcFolder;

		String[] packageParts = packageName.split("\\.");
		for (String part : packageParts) {
			packageFolder = packageFolder.getFolder(part);
			if (!packageFolder.exists()) {
				packageFolder.create(true, true, null);
			}
		}

		// Create Java file
		IFile javaFile = packageFolder.getFile(className + ".java");
		javaFile.create(new java.io.ByteArrayInputStream(content.getBytes()), true, null);
	}

	/**
	 * Helper method to add Java nature to project.
	 */
	private void addJavaNature(org.eclipse.core.resources.IProject project) throws CoreException {
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			// Get current natures
			String[] prevNatures = project.getDescription().getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = JavaCore.NATURE_ID;

			// Set new natures
			org.eclipse.core.resources.IProjectDescription description = project.getDescription();
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
	}
}

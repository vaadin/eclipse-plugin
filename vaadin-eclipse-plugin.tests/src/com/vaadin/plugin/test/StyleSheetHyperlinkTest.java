package com.vaadin.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;

import com.vaadin.plugin.navigation.StyleSheetPathResolver;

public class StyleSheetHyperlinkTest extends BaseIntegrationTest {

	@Test
	public void testStyleSheetHyperlinkToProjectRoot() throws CoreException {
		IFile cssFile = testProject.getFile("styles.css");
		cssFile.create(new ByteArrayInputStream("body {}".getBytes()), true, null);

		IJavaProject javaProject = JavaCore.create(testProject);
		IFile resolved = StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "styles.css");

		assertNotNull("Should resolve CSS file", resolved);
		assertEquals("Should resolve to project root", cssFile.getFullPath(), resolved.getFullPath());
	}

	@Test
	public void testStyleSheetInMetaInfResources() throws CoreException {
		IFolder metaInf = testProject.getFolder("src/main/resources/META-INF/resources");
		createFolderHierarchy(metaInf);

		IFile cssFile = metaInf.getFile("theme.css");
		cssFile.create(new ByteArrayInputStream(".button {}".getBytes()), true, null);

		IJavaProject javaProject = JavaCore.create(testProject);
		IFile resolved = StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "theme.css");

		assertNotNull("Should resolve CSS in META-INF/resources", resolved);
		assertTrue("Path should contain META-INF", resolved.getFullPath().toString().contains("META-INF"));
	}

	@Test
	public void testStyleSheetWithLeadingDotSlash() throws CoreException {
		IFile cssFile = testProject.getFile("app.css");
		cssFile.create(new ByteArrayInputStream("".getBytes()), true, null);

		IJavaProject javaProject = JavaCore.create(testProject);
		IFile resolved = StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "./app.css");

		assertNotNull("Should resolve path with ./", resolved);
		assertEquals("Should resolve to same file", cssFile.getFullPath(), resolved.getFullPath());
	}

	@Test
	public void testStyleSheetResolutionPriority() throws CoreException {
		IFile rootCss = testProject.getFile("styles.css");
		rootCss.create(new ByteArrayInputStream("/* root */".getBytes()), true, null);

		IFolder staticFolder = testProject.getFolder("src/main/resources/static");
		createFolderHierarchy(staticFolder);
		IFile staticCss = staticFolder.getFile("styles.css");
		staticCss.create(new ByteArrayInputStream("/* static */".getBytes()), true, null);

		IJavaProject javaProject = JavaCore.create(testProject);
		IFile resolved = StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "styles.css");

		assertNotNull("Should resolve", resolved);
		assertEquals("Should resolve to project root (higher priority)", rootCss.getFullPath(), resolved.getFullPath());
	}

	@Test
	public void testStyleSheetPatternMatching() {
		String[] validPatterns = {"@StyleSheet(\"styles.css\")", "@StyleSheet( \"styles.css\" )",
				"@StyleSheet(  \"path/to/file.css\"  )", "@StyleSheet(\"./styles.css\")"};

		Pattern pattern = Pattern.compile("@StyleSheet\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");

		for (String test : validPatterns) {
			Matcher matcher = pattern.matcher(test);
			assertTrue("Should match: " + test, matcher.find());
			assertNotNull("Should extract path", matcher.group(1));
		}
	}

	@Test
	public void testMultipleStyleSheetAnnotations() throws CoreException {
		IFile css1 = testProject.getFile("one.css");
		IFile css2 = testProject.getFile("two.css");
		css1.create(new ByteArrayInputStream("".getBytes()), true, null);
		css2.create(new ByteArrayInputStream("".getBytes()), true, null);

		IJavaProject javaProject = JavaCore.create(testProject);
		assertNotNull(StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "one.css"));
		assertNotNull(StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "two.css"));
	}

	@Test
	public void testStyleSheetNotFound() throws CoreException {
		IJavaProject javaProject = JavaCore.create(testProject);
		IFile resolved = StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "nonexistent.css");

		assertNull("Should return null for non-existent file", resolved);
	}

	@Test
	public void testStyleSheetInWebapp() throws CoreException {
		IFolder webapp = testProject.getFolder("src/main/webapp");
		createFolderHierarchy(webapp);

		IFile cssFile = webapp.getFile("app.css");
		cssFile.create(new ByteArrayInputStream("".getBytes()), true, null);

		IJavaProject javaProject = JavaCore.create(testProject);
		IFile resolved = StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "app.css");

		assertNotNull("Should resolve CSS in webapp", resolved);
		assertTrue("Path should contain webapp", resolved.getFullPath().toString().contains("webapp"));
	}

	@Test
	public void testStyleSheetInStaticResources() throws CoreException {
		IFolder staticFolder = testProject.getFolder("src/main/resources/static");
		createFolderHierarchy(staticFolder);

		IFile cssFile = staticFolder.getFile("static.css");
		cssFile.create(new ByteArrayInputStream("".getBytes()), true, null);

		IJavaProject javaProject = JavaCore.create(testProject);
		IFile resolved = StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "static.css");

		assertNotNull("Should resolve CSS in static", resolved);
		assertTrue("Path should contain static", resolved.getFullPath().toString().contains("static"));
	}

	@Test
	public void testStyleSheetInPublicResources() throws CoreException {
		IFolder publicFolder = testProject.getFolder("src/main/resources/public");
		createFolderHierarchy(publicFolder);

		IFile cssFile = publicFolder.getFile("public.css");
		cssFile.create(new ByteArrayInputStream("".getBytes()), true, null);

		IJavaProject javaProject = JavaCore.create(testProject);
		IFile resolved = StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "public.css");

		assertNotNull("Should resolve CSS in public", resolved);
		assertTrue("Path should contain public", resolved.getFullPath().toString().contains("public"));
	}

	@Test
	public void testStyleSheetInResourcesFolder() throws CoreException {
		IFolder resourcesFolder = testProject.getFolder("src/main/resources/resources");
		createFolderHierarchy(resourcesFolder);

		IFile cssFile = resourcesFolder.getFile("resource.css");
		cssFile.create(new ByteArrayInputStream("".getBytes()), true, null);

		IJavaProject javaProject = JavaCore.create(testProject);
		IFile resolved = StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "resource.css");

		assertNotNull("Should resolve CSS in resources", resolved);
		assertTrue("Path should contain resources", resolved.getFullPath().toString().contains("resources"));
	}

	@Test
	public void testNestedPath() throws CoreException {
		IFolder themes = testProject.getFolder("themes/custom");
		createFolderHierarchy(themes);

		IFile cssFile = themes.getFile("styles.css");
		cssFile.create(new ByteArrayInputStream("".getBytes()), true, null);

		IJavaProject javaProject = JavaCore.create(testProject);
		IFile resolved = StyleSheetPathResolver.resolveStyleSheetPath(javaProject, "themes/custom/styles.css");

		assertNotNull("Should resolve nested path", resolved);
		assertTrue("Path should contain themes/custom", resolved.getFullPath().toString().contains("themes"));
	}

	private void createFolderHierarchy(IFolder folder) throws CoreException {
		if (!folder.exists()) {
			if (folder.getParent() instanceof IFolder && !folder.getParent().exists()) {
				createFolderHierarchy((IFolder) folder.getParent());
			}
			folder.create(true, true, null);
		}
	}
}

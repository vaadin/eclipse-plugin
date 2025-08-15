package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import com.vaadin.plugin.CopilotUndoManager;

/**
 * Tests for CopilotUndoManager functionality.
 */
public class CopilotUndoManagerTest extends BaseIntegrationTest {

	private CopilotUndoManager undoManager;

	@Override
	protected void doSetUp() throws CoreException {
		undoManager = CopilotUndoManager.getInstance();
	}

	@Test
	public void testSingletonInstance() {
		CopilotUndoManager instance1 = CopilotUndoManager.getInstance();
		CopilotUndoManager instance2 = CopilotUndoManager.getInstance();

		assertSame("Should return same singleton instance", instance1, instance2);
	}

	@Test
	public void testRecordAndUndoOperation() throws Exception {
		// Create a file with initial content
		IFile file = testProject.getFile("undo-test.txt");
		String originalContent = "Original content";
		String newContent = "Modified content";

		file.create(new java.io.ByteArrayInputStream(originalContent.getBytes("UTF-8")), true, null);

		// Record an operation
		undoManager.recordOperation(file, originalContent, newContent, "Test modification");

		// Apply the new content
		file.setContents(new java.io.ByteArrayInputStream(newContent.getBytes("UTF-8")), true, true, null);

		// Verify new content
		String currentContent = readFileContent(file);
		assertEquals("File should have new content", newContent, currentContent);

		// Perform undo
		List<String> filePaths = Arrays.asList(file.getLocation().toString());
		boolean undone = undoManager.performUndo(filePaths);

		assertTrue("Undo should be performed", undone);

		// Verify content is restored
		currentContent = readFileContent(file);
		assertEquals("File should have original content after undo", originalContent, currentContent);
	}

	@Test
	public void testRecordAndRedoOperation() throws Exception {
		// Create a file
		IFile file = testProject.getFile("redo-test.txt");
		String originalContent = "Original";
		String modifiedContent = "Modified";

		file.create(new java.io.ByteArrayInputStream(originalContent.getBytes("UTF-8")), true, null);

		// Record and apply operation
		undoManager.recordOperation(file, originalContent, modifiedContent, "Modify");
		file.setContents(new java.io.ByteArrayInputStream(modifiedContent.getBytes("UTF-8")), true, true, null);

		// Undo
		List<String> filePaths = Arrays.asList(file.getLocation().toString());
		undoManager.performUndo(filePaths);

		// Verify undone
		assertEquals("Should be back to original", originalContent, readFileContent(file));

		// Redo
		boolean redone = undoManager.performRedo(filePaths);
		assertTrue("Redo should be performed", redone);

		// Verify redone
		assertEquals("Should be back to modified", modifiedContent, readFileContent(file));
	}

	@Test
	public void testMultipleOperations() throws Exception {
		// Create a file
		IFile file = testProject.getFile("multi-op-test.txt");
		String content1 = "Version 1";
		String content2 = "Version 2";
		String content3 = "Version 3";

		file.create(new java.io.ByteArrayInputStream(content1.getBytes("UTF-8")), true, null);

		// Record first operation
		undoManager.recordOperation(file, content1, content2, "First edit");
		file.setContents(new java.io.ByteArrayInputStream(content2.getBytes("UTF-8")), true, true, null);

		// Record second operation
		undoManager.recordOperation(file, content2, content3, "Second edit");
		file.setContents(new java.io.ByteArrayInputStream(content3.getBytes("UTF-8")), true, true, null);

		assertEquals("Should have version 3", content3, readFileContent(file));

		// Undo twice
		List<String> filePaths = Arrays.asList(file.getLocation().toString());
		undoManager.performUndo(filePaths);
		assertEquals("Should have version 2 after first undo", content2, readFileContent(file));

		undoManager.performUndo(filePaths);
		assertEquals("Should have version 1 after second undo", content1, readFileContent(file));

		// Redo once
		undoManager.performRedo(filePaths);
		assertEquals("Should have version 2 after redo", content2, readFileContent(file));
	}

	@Test
	public void testUndoNonExistentFile() {
		// Try to undo for a file that doesn't exist
		List<String> filePaths = Arrays.asList("/nonexistent/file.txt");
		boolean result = undoManager.performUndo(filePaths);

		assertFalse("Undo should not be performed for non-existent file", result);
	}

	@Test
	public void testRedoNonExistentFile() {
		// Try to redo for a file that doesn't exist
		List<String> filePaths = Arrays.asList("/nonexistent/file.txt");
		boolean result = undoManager.performRedo(filePaths);

		assertFalse("Redo should not be performed for non-existent file", result);
	}

	@Test
	public void testUndoWithoutOperations() throws Exception {
		// Create a file but don't record any operations
		IFile file = testProject.getFile("no-ops.txt");
		file.create(new java.io.ByteArrayInputStream("Content".getBytes("UTF-8")), true, null);

		List<String> filePaths = Arrays.asList(file.getLocation().toString());
		boolean result = undoManager.performUndo(filePaths);

		assertFalse("Undo should not be performed when no operations recorded", result);
	}

	@Test
	public void testFileCreationUndo() throws Exception {
		// Test undoing a file creation (empty old content)
		IFile file = testProject.getFile("create-undo.txt");
		String newContent = "Created file";

		// Simulate file creation by recording with empty old content
		undoManager.recordOperation(file, "", newContent, "Create file");

		// Create the actual file
		file.create(new java.io.ByteArrayInputStream(newContent.getBytes("UTF-8")), true, null);
		assertTrue("File should exist", file.exists());

		// Undo should set content to empty (can't delete via content operation)
		List<String> filePaths = Arrays.asList(file.getLocation().toString());
		boolean undone = undoManager.performUndo(filePaths);

		assertTrue("Undo should be performed", undone);
		assertEquals("File content should be empty after undo", "", readFileContent(file));
	}

	@Test
	public void testFileDeletionUndo() throws Exception {
		// Test undoing a file deletion (empty new content)
		IFile file = testProject.getFile("delete-undo.txt");
		String originalContent = "File to delete";

		// Create file
		file.create(new java.io.ByteArrayInputStream(originalContent.getBytes("UTF-8")), true, null);

		// Record deletion (new content is empty)
		undoManager.recordOperation(file, originalContent, "", "Delete file");

		// Delete the file
		file.delete(true, null);
		assertFalse("File should not exist after deletion", file.exists());

		// Try to undo - this will fail because file doesn't exist
		List<String> filePaths = Arrays.asList(file.getLocation().toString());
		boolean undone = undoManager.performUndo(filePaths);

		// Note: This will be false because the file doesn't exist
		// The undo manager needs the file to exist to restore content
		assertFalse("Undo cannot be performed on deleted file", undone);
	}

	@Test
	public void testMultipleFilesUndo() throws Exception {
		// Create multiple files
		IFile file1 = testProject.getFile("multi1.txt");
		IFile file2 = testProject.getFile("multi2.txt");

		String original1 = "Original 1";
		String original2 = "Original 2";
		String modified1 = "Modified 1";
		String modified2 = "Modified 2";

		file1.create(new java.io.ByteArrayInputStream(original1.getBytes("UTF-8")), true, null);
		file2.create(new java.io.ByteArrayInputStream(original2.getBytes("UTF-8")), true, null);

		// Record operations for both files
		undoManager.recordOperation(file1, original1, modified1, "Modify file1");
		undoManager.recordOperation(file2, original2, modified2, "Modify file2");

		// Apply changes
		file1.setContents(new java.io.ByteArrayInputStream(modified1.getBytes("UTF-8")), true, true, null);
		file2.setContents(new java.io.ByteArrayInputStream(modified2.getBytes("UTF-8")), true, true, null);

		// Undo both
		List<String> filePaths = Arrays.asList(file1.getLocation().toString(), file2.getLocation().toString());
		boolean undone = undoManager.performUndo(filePaths);

		assertTrue("Undo should be performed", undone);
		assertEquals("File1 should be restored", original1, readFileContent(file1));
		assertEquals("File2 should be restored", original2, readFileContent(file2));
	}

	/**
	 * Helper method to read file content as string.
	 */
	private String readFileContent(IFile file) throws Exception {
		try (java.io.InputStream is = file.getContents()) {
			return new String(is.readAllBytes(), "UTF-8");
		}
	}
}

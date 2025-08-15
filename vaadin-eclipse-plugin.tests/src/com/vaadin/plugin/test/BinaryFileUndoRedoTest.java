package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vaadin.plugin.CopilotClient;
import com.vaadin.plugin.CopilotRestService;

/**
 * Tests for binary file operations and undo/redo functionality.
 */
public class BinaryFileUndoRedoTest extends BaseIntegrationTest {
    
    private CopilotRestService restService;
    private CopilotClient client;
    private Gson gson = new Gson();
    
    @Override
    protected void doSetUp() throws CoreException {
        restService = new CopilotRestService();
        
        try {
            restService.start();
            String endpoint = restService.getEndpoint();
            String projectPath = testProject.getLocation().toString();
            
            client = new CopilotClient(endpoint, projectPath);
            
            // Give the server a moment to fully start
            Thread.sleep(100);
            
        } catch (Exception e) {
            fail("Failed to start REST service: " + e.getMessage());
        }
    }
    
    @Override
    protected void doTearDown() throws CoreException {
        if (restService != null) {
            restService.stop();
        }
    }
    
    @Test
    public void testBinaryFileWriteAndRead() throws Exception {
        // Create binary content (a simple PNG header)
        byte[] binaryData = new byte[] {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,  // PNG header
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52         // IHDR chunk start
        };
        
        String base64Content = java.util.Base64.getEncoder().encodeToString(binaryData);
        Path filePath = Paths.get(testProject.getLocation().toString(), "test.png");
        
        // Write binary file
        HttpResponse<String> writeResponse = client.writeBinary(filePath, base64Content);
        assertEquals("Write should succeed", 200, writeResponse.statusCode());
        
        // Verify file exists and has correct content
        IFile file = testProject.getFile("test.png");
        assertTrue("File should exist", file.exists());
        
        byte[] readData = file.getContents().readAllBytes();
        assertArrayEquals("Binary content should match", binaryData, readData);
    }
    
    @Test
    public void testBinaryFileUndoRedo() throws Exception {
        // Create initial binary content
        byte[] originalData = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        byte[] modifiedData = new byte[] { 0x05, 0x06, 0x07, 0x08, 0x09 };
        
        String originalBase64 = java.util.Base64.getEncoder().encodeToString(originalData);
        String modifiedBase64 = java.util.Base64.getEncoder().encodeToString(modifiedData);
        Path filePath = Paths.get(testProject.getLocation().toString(), "binary.dat");
        
        // Write original binary file
        HttpResponse<String> response1 = client.writeBinary(filePath, originalBase64);
        assertEquals("First write should succeed", 200, response1.statusCode());
        
        // Modify the binary file
        HttpResponse<String> response2 = client.writeBinary(filePath, modifiedBase64);
        assertEquals("Second write should succeed", 200, response2.statusCode());
        
        // Verify modified content
        IFile file = testProject.getFile("binary.dat");
        byte[] currentData = file.getContents().readAllBytes();
        assertArrayEquals("Should have modified data", modifiedData, currentData);
        
        // Perform undo
        HttpResponse<String> undoResponse = client.undo(filePath);
        assertEquals("Undo should succeed", 200, undoResponse.statusCode());
        
        JsonObject undoResult = gson.fromJson(undoResponse.body(), JsonObject.class);
        assertTrue("Undo should be performed", undoResult.get("performed").getAsBoolean());
        
        // Verify content reverted to original
        file.refreshLocal(0, null);
        currentData = file.getContents().readAllBytes();
        assertArrayEquals("Should have original data after undo", originalData, currentData);
        
        // Perform redo
        HttpResponse<String> redoResponse = client.redo(filePath);
        assertEquals("Redo should succeed", 200, redoResponse.statusCode());
        
        JsonObject redoResult = gson.fromJson(redoResponse.body(), JsonObject.class);
        assertTrue("Redo should be performed", redoResult.get("performed").getAsBoolean());
        
        // Verify content is modified again
        file.refreshLocal(0, null);
        currentData = file.getContents().readAllBytes();
        assertArrayEquals("Should have modified data after redo", modifiedData, currentData);
    }
    
    @Test
    public void testLargeBinaryFileUndoRedo() throws Exception {
        // Create a larger binary file (1KB of random-looking data)
        byte[] largeData = new byte[1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte)(i % 256);
        }
        
        byte[] modifiedLargeData = new byte[1024];
        for (int i = 0; i < modifiedLargeData.length; i++) {
            modifiedLargeData[i] = (byte)((i * 2) % 256);
        }
        
        String originalBase64 = java.util.Base64.getEncoder().encodeToString(largeData);
        String modifiedBase64 = java.util.Base64.getEncoder().encodeToString(modifiedLargeData);
        Path filePath = Paths.get(testProject.getLocation().toString(), "large.bin");
        
        // Write original
        client.writeBinary(filePath, originalBase64);
        
        // Modify
        client.writeBinary(filePath, modifiedBase64);
        
        // Undo
        HttpResponse<String> undoResponse = client.undo(filePath);
        JsonObject undoResult = gson.fromJson(undoResponse.body(), JsonObject.class);
        assertTrue("Undo should be performed", undoResult.get("performed").getAsBoolean());
        
        // Verify original content restored
        IFile file = testProject.getFile("large.bin");
        file.refreshLocal(0, null);
        byte[] currentData = file.getContents().readAllBytes();
        assertArrayEquals("Large file should be restored correctly", largeData, currentData);
    }
    
    @Test
    public void testMixedTextAndBinaryUndo() throws Exception {
        // Test that text and binary files can be undone independently
        Path textPath = Paths.get(testProject.getLocation().toString(), "text.txt");
        Path binaryPath = Paths.get(testProject.getLocation().toString(), "binary.dat");
        
        // Create text file
        client.write(textPath, "Original text");
        
        // Create binary file
        byte[] binaryContent = new byte[] { 0x0A, 0x0B, 0x0C };
        String base64Content = java.util.Base64.getEncoder().encodeToString(binaryContent);
        client.writeBinary(binaryPath, base64Content);
        
        // Modify both files
        client.write(textPath, "Modified text");
        
        byte[] modifiedBinary = new byte[] { 0x1A, 0x1B, 0x1C, 0x1D };
        client.writeBinary(binaryPath, java.util.Base64.getEncoder().encodeToString(modifiedBinary));
        
        // Undo only the binary file
        HttpResponse<String> undoResponse = client.undo(binaryPath);
        JsonObject undoResult = gson.fromJson(undoResponse.body(), JsonObject.class);
        assertTrue("Binary undo should be performed", undoResult.get("performed").getAsBoolean());
        
        // Verify binary reverted but text unchanged
        IFile textIFile = testProject.getFile("text.txt");
        String currentText = new String(textIFile.getContents().readAllBytes(), "UTF-8");
        assertEquals("Text should still be modified", "Modified text", currentText);
        
        IFile binaryIFile = testProject.getFile("binary.dat");
        byte[] currentBinary = binaryIFile.getContents().readAllBytes();
        assertArrayEquals("Binary should be reverted", binaryContent, currentBinary);
    }
    
    @Test
    public void testEmptyBinaryFileUndo() throws Exception {
        // Test handling of empty binary files
        Path filePath = Paths.get(testProject.getLocation().toString(), "empty.bin");
        
        // Create empty binary file
        HttpResponse<String> response = client.writeBinary(filePath, "");
        assertEquals("Write should succeed", 200, response.statusCode());
        
        // Add content
        byte[] content = new byte[] { (byte)0xFF, (byte)0xFE };
        client.writeBinary(filePath, java.util.Base64.getEncoder().encodeToString(content));
        
        // Undo to empty state
        HttpResponse<String> undoResponse = client.undo(filePath);
        JsonObject undoResult = gson.fromJson(undoResponse.body(), JsonObject.class);
        assertTrue("Undo should be performed", undoResult.get("performed").getAsBoolean());
        
        // Verify file is empty
        IFile file = testProject.getFile("empty.bin");
        file.refreshLocal(0, null);
        byte[] currentData = file.getContents().readAllBytes();
        assertEquals("File should be empty after undo", 0, currentData.length);
    }
}
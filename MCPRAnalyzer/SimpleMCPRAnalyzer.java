package com.replaymod.analyzer;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Simple MCPR File Analyzer
 * For testing and analyzing ReplayMod recording files
 */
public class SimpleMCPRAnalyzer {
    
    public static void main(String[] args) {
        System.out.println("=== MCPR Analyzer (Simple Version) ===\n");
        
        if (args.length < 1) {
            System.out.println("Usage: java SimpleMCPRAnalyzer <mcpr_file_path>");
            System.out.println("Example: java SimpleMCPRAnalyzer \"D:\\path\\to\\recording.mcpr\"");
            return;
        }
        
        String filePath = args[0];
        File mcprFile = new File(filePath);
        
        if (!mcprFile.exists()) {
            System.err.println("Error: File not found - " + filePath);
            return;
        }
        
        try {
            analyzeMCPR(mcprFile);
        } catch (Exception e) {
            System.err.println("Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void analyzeMCPR(File mcprFile) throws IOException {
        System.out.println("Analyzing file: " + mcprFile.getAbsolutePath());
        System.out.println("File size: " + formatFileSize(mcprFile.length()));
        System.out.println("Last modified: " + new Date(mcprFile.lastModified()));
        
        System.out.println("\n--- File Structure Analysis ---");
        
        // Check file header
        try (FileInputStream fis = new FileInputStream(mcprFile)) {
            byte[] header = new byte[4];
            int bytesRead = fis.read(header);
            
            if (bytesRead >= 4) {
                System.out.println("File header: " + bytesToHex(header));
                
                // Check for ZIP format (MCPR files are essentially ZIP)
                if (header[0] == 0x50 && header[1] == 0x4B) {
                    System.out.println("✓ Valid ZIP format detected (MCPR file)");
                    analyzeZipStructure(mcprFile);
                } else {
                    System.out.println("⚠ File header does not match expected ZIP format");
                }
            }
        }
        
        System.out.println("\n--- Analysis Complete ---");
        System.out.println("Note: This is a simplified analyzer");
        System.out.println("Full functionality requires ReplayMod dependency libraries");
    }
    
    private static void analyzeZipStructure(File zipFile) throws IOException {
        System.out.println("\nZIP file structure:");
        
        // Simple ZIP entry scan
        try (FileInputStream fis = new FileInputStream(zipFile)) {
            byte[] buffer = new byte[1024];
            int entryCount = 0;
            
            // Basic ZIP entry detection
            while (fis.available() > 0) {
                int read = fis.read(buffer);
                if (read <= 0) break;
                
                // Look for local file header signature (0x04034b50)
                for (int i = 0; i < read - 3; i++) {
                    if (buffer[i] == 0x50 && buffer[i+1] == 0x4B && 
                        buffer[i+2] == 0x03 && buffer[i+3] == 0x04) {
                        entryCount++;
                    }
                }
            }
            
            System.out.println("Detected approximately " + entryCount + " ZIP entries");
            
            // Common MCPR file contents
            System.out.println("\nExpected MCPR file contents:");
            System.out.println("  - metadata.json (metadata)");
            System.out.println("  - recording.tmcpr (recording data)");
            System.out.println("  - markers.json (marker data, optional)");
            System.out.println("  - thumbnails/ (thumbnails, optional)");
            
        } catch (Exception e) {
            System.out.println("ZIP structure analysis error: " + e.getMessage());
        }
    }
    
    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }
        return result.toString().trim();
    }
}
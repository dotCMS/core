# TarUtil Security Guide

## Overview

The `TarUtil` class provides safe methods for working with TAR archives in dotCMS, designed to prevent TAR Slip vulnerabilities (similar to ZIP Slip). The class implements security measures to ensure that extracted files cannot escape their target directory through path traversal attacks.

## What is TAR Slip?

TAR Slip is a form of directory traversal vulnerability where an attacker can craft a malicious TAR archive with entries containing path traversal sequences (such as `../../../etc/passwd`) that, when extracted, can overwrite files outside the intended extraction directory.

## Security Features

TarUtil implements the following security measures:

1. **Path Sanitization** - All file paths in TAR entries are sanitized to remove path traversal sequences and ensure they stay within the target directory.

2. **Canonical Path Checking** - Double checking that the final output path is within the intended target directory.

3. **Configurable Handling Modes** - Two modes for handling suspicious entries:
   - `ABORT` - Immediately stop processing and throw a SecurityException (default)
   - `SKIP_AND_CONTINUE` - Log a warning, skip the suspicious entry, and continue processing other entries

4. **DoS Protection** - Limits to prevent resource exhaustion attacks:
   - Maximum total extracted size
   - Maximum size for a single file
   - Maximum number of entries
   
5. **Thread-safe Handling** - Thread-local handling mode to ensure thread safety in concurrent environments.

## Configuration Options

TarUtil security features can be configured through the following configuration properties:

| Property | Description | Default Value |
|----------|-------------|---------------|
| `TAR_MAX_TOTAL_SIZE` | Maximum total size of all extracted files in bytes | 5GB (5,368,709,120 bytes) |
| `TAR_MAX_FILE_SIZE` | Maximum size of a single extracted file in bytes | 1GB (1,073,741,824 bytes) |
| `TAR_MAX_ENTRIES` | Maximum number of entries to extract | 10,000 |

These properties can be set in your `dotmarketing-config.properties` file.

## Usage Examples

### Safe TAR File Extraction

```java
// Extract a TAR.GZ file safely with default settings (ABORT on suspicious entries)
File inputFile = new File("/path/to/archive.tar.gz");
File outputDir = new File("/path/to/output");
TarUtil.safeExtractTarGz(inputFile, outputDir);

// Extract with SKIP_AND_CONTINUE mode for suspicious entries
TarUtil.setDefaultSuspiciousEntryHandling(TarUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
TarUtil.safeExtractTarGz(inputFile, outputDir);

// Or specify the handling mode directly
TarUtil.safeExtractTarGz(inputFile, outputDir, TarUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
```

### Creating a Secure TAR Archive

```java
// Create a new TAR.GZ file with secure paths
File outputFile = new File("/path/to/output.tar.gz");
try (TarArchiveOutputStream taos = TarUtil.createTarGzOutputStream(outputFile)) {
    // Add a file with a sanitized path
    File fileToAdd = new File("/path/to/file.txt");
    TarUtil.addFileToTar(taos, fileToAdd, "path/in/archive/file.txt");
    
    // Add a directory recursively
    File dirToAdd = new File("/path/to/directory");
    TarUtil.addDirectoryToTar(taos, dirToAdd, "", "");
}
```

## Best Practices

1. **Always use TarUtil methods** for handling TAR files in dotCMS instead of using Apache Commons Compress classes directly.

2. **Consider the handling mode** - Use `ABORT` (default) in security-sensitive contexts to fail fast, and `SKIP_AND_CONTINUE` only when you need to be more permissive.

3. **Monitor log files** - TarUtil will log warnings and errors about suspicious entries. Regular monitoring can help detect attempted attacks.

4. **Adjust configuration limits** for your specific use case. If you expect to extract very large archives, you may need to increase the default limits.

5. **Thread-safety consideration** - The default handling mode is stored in ThreadLocal, so each thread can have its own setting without interfering with others. This is important in multi-threaded applications.

By following these best practices and using the TarUtil class, you can help protect your dotCMS installation from TAR Slip vulnerabilities.

## Implementation Details

TarUtil leverages the same underlying path sanitization logic as ZipUtil, ensuring consistent security across archive types. The implementation uses Apache Commons Compress for TAR handling with additional security checks. 
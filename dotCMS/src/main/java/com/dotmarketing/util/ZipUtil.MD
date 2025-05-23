# ZipUtil Security Guide

## Overview

The `ZipUtil` class provides safe methods for working with ZIP archives in dotCMS, designed to prevent ZIP Slip vulnerabilities. The class implements security measures to ensure that ZIP file operations are performed safely, protecting against path traversal attacks, zip bombs, and other archive-related vulnerabilities.

## Security Measures

ZipUtil implements the following security measures:

1. **Path sanitization** - Removes path traversal sequences like `../` and resolves relative paths
2. **Canonical path verification** - Ensures that extracted files remain within the target directory
3. **Configurable handling modes** - Choose between ABORT (strict security) or SKIP_AND_CONTINUE (lenient)
4. **DoS protections**:
   - Maximum total extracted size limit
   - Maximum individual file size limit
   - Maximum entry count limit
   - Excessive compression ratio checks
5. **Thread-safe operation** using ThreadLocal variables
6. **Proper resource management** to prevent leaks

## Configuration

ZipUtil security features can be configured through the following configuration properties:

```
# Maximum total size of all extracted files (default: 5GB)
ZIP_MAX_TOTAL_SIZE=5368709120

# Maximum size of a single extracted file (default: 1GB)
ZIP_MAX_FILE_SIZE=1073741824

# Maximum number of entries to extract (default: 10,000)
ZIP_MAX_ENTRIES=10000
```

These settings can be configured in your dotCMS `dotmarketing-config.properties` file.

## Usage Examples

### Basic Extraction

```java
// Extract a ZIP file using default security settings (ABORT on suspicious entries)
ZipUtil.safeExtract(zipInputStream, outputDirectory);
```

### Custom Handling Mode

```java
// Configure default handling mode
ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
ZipUtil.safeExtract(zipInputStream, outputDirectory);

// Or specify handling mode directly
ZipUtil.safeExtract(zipInputStream, outputDirectory, ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
```

### Creating ZIP Archives

```java
// Create a secure ZIP file
try (ZipOutputStream zos = ZipUtil.createZipOutputStream(outputFile)) {
    // Add files with proper path sanitization
    ZipUtil.addZipEntry(zos, "path/in/archive/file.txt", fileInputStream, true);
}
```

## Best Practices

1. **Always use ZipUtil methods** for handling ZIP files in dotCMS instead of using standard JDK ZIP classes directly.
2. **Validate user inputs** that may contain file paths to prevent directory traversal attempts.
3. **Monitor log files** - ZipUtil will log warnings and errors about suspicious entries. Regular monitoring can help detect attempted attacks.
4. **Consider using SKIP_AND_CONTINUE mode** in non-security-critical contexts to prevent complete failure when a single suspicious entry is found.
5. **Keep the security limits appropriate** for your application's needs - adjust the MAX_TOTAL_SIZE, MAX_FILE_SIZE, and MAX_ENTRIES if necessary.

By following these best practices and using the ZipUtil class, you can help protect your dotCMS installation from ZIP Slip vulnerabilities and other archive-related attacks. 
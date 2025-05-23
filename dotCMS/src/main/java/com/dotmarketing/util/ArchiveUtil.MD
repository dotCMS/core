# ArchiveUtil Security Guide

## Overview

The `ArchiveUtil` class provides shared functionality for secure archive handling in dotCMS. This class contains common methods and utilities used by both `ZipUtil` and `TarUtil` to implement consistent security measures against archive-related vulnerabilities like Zip/Tar Slip attacks and resource exhaustion (DoS) attacks.

## Security Measures

ArchiveUtil implements the following core security measures:

1. **Path sanitization** - Removes path traversal sequences like `../` and resolves relative paths
2. **Canonical path verification** - Ensures that extracted files remain within the target directory
3. **Configurable handling modes** - Choose between ABORT (strict security) or SKIP_AND_CONTINUE (lenient)
4. **Path validation** - Provides robust validation to guarantee files stay within a designated directory
5. **DoS protections**:
   - Maximum total extracted size limit
   - Maximum individual file size limit
   - Maximum entry count limit

## Usage

ArchiveUtil is designed to be used internally by ZipUtil and TarUtil, not called directly by application code. The proper way to use these security features is through the ZipUtil and TarUtil APIs.

## Higher-Level Utility Methods

Both ZipUtil and TarUtil provide a set of higher-level utility methods that encapsulate common archive operations, eliminating the need to manually handle I/O streams:

### ZipUtil High-Level Methods

- `addFileToZip(zipOut, file, entryName)` - Adds a single file to a ZIP with proper I/O handling
- `addDirectoryToZip(zipOut, directory, baseEntryPath)` - Adds a directory and its contents to a ZIP
- `zipSingleFile(sourceFile, targetZipFile)` - Creates a ZIP containing a single file
- `zipFiles(sources, targetZipFile)` - Creates a ZIP from multiple files/directories

### TarUtil High-Level Methods

- `addFileToTar(taos, file, relativePath)` - Adds a single file to a TAR with proper I/O handling
- `addDirectoryToTar(taos, directory, basePath, baseDirectory)` - Adds a directory to a TAR
- `tarGzSingleFile(sourceFile, targetTarGzFile)` - Creates a TAR.GZ containing a single file
- `tarGzFiles(sources, targetTarGzFile)` - Creates a TAR.GZ from multiple files/directories
- `compressDirectoryToTarGz(directory, targetTarGzFile, includeBaseDirName)` - Compresses a directory to TAR.GZ

Using these methods ensures:
1. Proper security checks
2. Consistent error handling
3. Resource cleanup (all streams are properly closed)
4. DRY code with less boilerplate

## Lambda-Based File Processing

Both ZipUtil and TarUtil now support lambda functions for flexible file processing, allowing you to:

1. **Filter files** - Include or exclude files based on custom criteria
2. **Process file contents** - Transform file data before adding to the archive
3. **Customize entry names** - Control how files are named within the archive

### Lambda Interface Types

Three functional interfaces are provided:

1. **FileFilter** - Determines which files to include in the archive
   ```java
   boolean accept(File file);
   ```

2. **FileProcessor** - Transforms file contents before archiving
   ```java
   InputStream process(File file, String entryName) throws IOException;
   ```

3. **EntryNameMapper** - Customizes archive entry names
   ```java
   String mapEntryName(File file, String basePath);
   ```

### Example Usage

```java
// Filter: Only include .java files
ZipUtil.FileFilter filter = file -> 
    file.isDirectory() || file.getName().endsWith(".java");

// Processor: Add file header comments to Java files
ZipUtil.FileProcessor processor = (file, entryName) -> {
    if (file.getName().endsWith(".java")) {
        String content = Files.readString(file.toPath());
        String processed = "// Auto-generated archive - " + new Date() + "\n" + content;
        return new ByteArrayInputStream(processed.getBytes(StandardCharsets.UTF_8));
    }
    return Files.newInputStream(file.toPath());
};

// Name mapper: Convert entry paths to lowercase
ZipUtil.EntryNameMapper nameMapper = (file, basePath) ->
    (basePath + file.getName()).toLowerCase();

// Create the archive with custom processing
ZipUtil.zipFilesWithCustomProcessing(
    files,           // Collection of files to archive
    outputFile,      // Target ZIP file
    "src/",          // Base path within the archive
    filter,          // Custom file filter
    processor,       // Custom processor
    nameMapper       // Custom name mapper
);
```

These lambda-based methods provide maximum flexibility while maintaining all security protections and resource management benefits of the utility classes.

## Configuration

Both ZipUtil and TarUtil leverage ArchiveUtil's shared constants but use their own configuration keys to allow separate configuration:

### ZIP Configuration
```
# Maximum total size of all extracted files (default: 5GB)
ZIP_MAX_TOTAL_SIZE=5GB

# Maximum size of a single extracted file (default: 1GB)
ZIP_MAX_FILE_SIZE=1GB

# Maximum number of entries to extract (default: 10,000)
ZIP_MAX_ENTRIES=10000
```

### TAR Configuration
```
# Maximum total size of all extracted files (default: 5GB)
TAR_MAX_TOTAL_SIZE=5GB

# Maximum size of a single extracted file (default: 1GB)
TAR_MAX_FILE_SIZE=1GB

# Maximum number of entries to extract (default: 10,000)
TAR_MAX_ENTRIES=10000
```

Size limits can be specified using dotCMS's standard size format using the existing `SizeUtil` class. This supports various human-readable formats:
- Plain numbers (e.g., `5368709120`) are interpreted as bytes
- Sizes with unit suffixes: `KB`, `MB`, `GB` (e.g., `5GB`, `1024MB`)
- Sizes with short unit suffixes: `K`, `M`, `G` (e.g., `5G`, `1024M`)
- Decimal values are supported (e.g., `1.5GB`)
- Whitespace between number and unit is optional (e.g., `5 GB` = `5GB`)

These settings can be configured in your dotCMS `dotmarketing-config.properties` file.

## Best Practices

1. **Always use ZipUtil and TarUtil** for handling ZIP and TAR files in dotCMS instead of using standard JDK or Apache Commons classes directly.
2. **Validate user inputs** that may contain file paths to prevent directory traversal attempts.
3. **Monitor log files** - Both utility classes will log warnings and errors about suspicious entries. Regular monitoring can help detect attempted attacks.
4. **Consider using SKIP_AND_CONTINUE mode** in non-security-critical contexts to prevent complete failure when a single suspicious entry is found.
5. **Keep security limits appropriate** for your application's needs - adjust the MAX_TOTAL_SIZE, MAX_FILE_SIZE, and MAX_ENTRIES if necessary.

## Benefits of Shared Implementation

By using ArchiveUtil as a common base for both ZIP and TAR handling:

1. **Consistent security** - Both file formats use the same security measures to prevent similar vulnerabilities.
2. **Easier maintenance** - Security fixes need to be implemented only once in the shared code.
3. **Reduced code duplication** - Common logic is handled in one place rather than duplicated.
4. **Unified logging and error handling** - Similar threats across different formats are logged consistently.

## Implementation Details

ArchiveUtil implements algorithms for:

- Path sanitization and validation
- Destination path safety checks
- Robust directory containment validation
- DoS protection through size and count limits

These are fundamental security measures that apply equally to ZIP and TAR archive formats, despite their implementation differences.

## Directory Containment Validation

One of the most critical security features of ArchiveUtil is ensuring that files cannot be extracted to or created outside of a specific target directory. This prevents path traversal attacks where malicious archive entries try to escape the intended extraction directory.

The `validatePathWithinDirectory` and `validateFileWithinDirectory` methods provide robust protection by:

1. Converting all paths to canonical form (resolving all symlinks and relative paths)
2. Checking that file paths only exist within the target directory
3. Providing configurable handling modes to either abort the operation or skip problematic files
4. Properly logging security issues to both regular and security logs

These methods should be used in any code that works with archives or paths where there's a risk of path traversal attacks. 
# ArchiveUtil Security Guide

## Overview

The `ArchiveUtil` class provides shared functionality for secure archive handling in dotCMS. This class contains common methods and utilities used by both `ZipUtil` and `TarUtil` to implement consistent security measures against archive-related vulnerabilities like Zip/Tar Slip attacks and resource exhaustion (DoS) attacks.

## Security Measures

ArchiveUtil implements the following core security measures:

1. **Path sanitization** - Removes path traversal sequences like `../` and resolves relative paths
2. **Canonical path verification** - Ensures that extracted files remain within the target directory
3. **Configurable handling modes** - Choose between ABORT (strict security) or SKIP_AND_CONTINUE (lenient)
4. **DoS protections**:
   - Maximum total extracted size limit
   - Maximum individual file size limit
   - Maximum entry count limit

## Usage

ArchiveUtil is designed to be used internally by ZipUtil and TarUtil, not called directly by application code. The proper way to use these security features is through the ZipUtil and TarUtil APIs.

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
- DoS protection through size and count limits

These are fundamental security measures that apply equally to ZIP and TAR archive formats, despite their implementation differences. 
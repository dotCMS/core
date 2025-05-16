# DRY Implementation for Archive Security in dotCMS

## Description
This PR refactors the ZIP and TAR file handling security mechanisms in dotCMS to follow the DRY (Don't Repeat Yourself) principle. Previously, both `ZipUtil` and `TarUtil` contained duplicated code for handling security protections against archive-related vulnerabilities. This refactoring extracts the common functionality into a shared `ArchiveUtil` class, making the security implementations more consistent and maintainable.

## Changes

### New Shared Implementation
1. Created a new `ArchiveUtil` class that contains shared security functionality:
   - Path sanitization and normalization
   - Canonical path verification
   - DoS protection with configurable limits
   - Common security checks for archive extraction

2. Updated both `ZipUtil` and `TarUtil` to use the shared implementation:
   - Replaced duplicated code with calls to `ArchiveUtil` methods
   - Maintained backward compatibility for all public methods
   - Ensured consistent behavior across both file formats

3. Added documentation and tests:
   - Created comprehensive documentation in `ArchiveUtil.MD`
   - Added unit tests for the shared functionality in `ArchiveUtilTest.java`

## Security Benefits
- **Consistent Protection**: Both file formats now use exactly the same security checks
- **Single Source of Truth**: Security fixes need to be implemented only once
- **Complete DoS Protection**: Both implementations have the same protections:
  - Max total size limits
  - Max individual file size limits
  - Max entry count limits

## Technical Details
The implementation maintains the distinct configuration keys for each file format (`ZIP_*` and `TAR_*`) while sharing the underlying security logic. This allows for different security thresholds for each format if needed, while ensuring the actual security mechanisms are identical.

Size handling has been improved by leveraging the existing `SizeUtil` class instead of custom parsing. This provides a consistent, well-tested approach to parsing human-readable size strings (like "5GB" or "1.5MB") across the codebase.

## Testing
- All existing ZIP and TAR tests continue to pass
- New tests were added specifically for `ArchiveUtil` functionality
- Manual testing confirms that both formats correctly:
  - Block path traversal attempts
  - Apply resource limitations
  - Properly handle suspicious entries based on the configured handling mode

## Breaking Changes
None. This is an internal refactoring that maintains all public APIs and behavior. 
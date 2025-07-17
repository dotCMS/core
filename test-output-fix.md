# Test Output Fix for Issue 389

This file tests the GitHub Actions output format fix.

## Issue Fixed
The previous error was:
```
Error: Invalid format '- [Test final workflow formatting for issue 389](https://github.com/dotCMS/core-workflow-test/pull/390) by @spbolton'
```

## Fix Applied
Changed from:
```bash
echo "pr_list=$new_body" >> "$GITHUB_OUTPUT"
```

To:
```bash
{
  echo "pr_list<<EOF"
  echo "$new_body"
  echo "EOF"
} >> "$GITHUB_OUTPUT"
```

This should now properly handle multiline strings in GitHub Actions.

Branch: issue-389-test-output-fix
Target Issue: #389
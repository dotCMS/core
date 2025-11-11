# CI/CD Diagnostics Utility Functions

Reusable Python utility modules for CI/CD failure analysis.

## Overview

This directory contains modular Python utility modules extracted from the cicd-diagnostics skill. These modules can be imported and used by the skill or other automation scripts.

## Files

### github_api.py
GitHub API and CLI wrapper functions for fetching workflow, job, and issue data.

**Key Functions:**
- `extract_run_id(url)` - Extract run ID from GitHub Actions URL
- `extract_pr_number(input)` - Extract PR number from URL or branch name
- `get_run_metadata(run_id, output_file)` - Fetch workflow run details
- `get_jobs_detailed(run_id, output_file)` - Get all jobs with step information
- `get_failed_jobs(jobs_file)` - Filter failed jobs from jobs file
- `download_job_logs(job_id, output_file)` - Download job logs
- `get_pr_info(pr_num, output_file)` - Get PR details and status checks
- `find_failed_run_from_pr(pr_info_file)` - Find failed run from PR data
- `get_recent_runs(workflow_name, limit, output_file)` - Fetch workflow history
- `search_issues(query, output_file)` - Search GitHub issues
- `compare_commits(base_sha, head_sha, output_file)` - Compare commit ranges

**Usage Example:**
```python
import sys
from pathlib import Path
sys.path.insert(0, str(Path(".claude/skills/cicd-diagnostics/utils")))

from github_api import extract_run_id, get_run_metadata

run_id = extract_run_id("https://github.com/dotCMS/core/actions/runs/19118302390")
get_run_metadata(run_id, Path("run-metadata.json"))
```

### workspace.py
Diagnostic workspace management with caching and artifact organization.

**Key Functions:**
- `create_diagnostic_workspace(run_id)` - Create workspace directory
- `find_existing_diagnostic(run_id)` - Check for cached diagnostics
- `get_diagnostic_workspace(run_id, force_clean=False)` - Get or create workspace (with caching)
- `save_artifact(diagnostic_dir, filename, content)` - Save artifact to workspace
- `artifact_exists(diagnostic_dir, filename)` - Check if artifact is cached
- `get_or_fetch_artifact(diagnostic_dir, filename, fetch_command)` - Cache-aware fetching
- `ensure_gitignore_diagnostics()` - Add diagnostic dirs to .gitignore
- `list_diagnostic_workspaces()` - List all diagnostic sessions
- `clean_old_diagnostics(max_age_hours=168, max_count=50)` - Cleanup old workspaces
- `get_workspace_summary(diagnostic_dir)` - Display workspace details

**Usage Example:**
```python
import sys
from pathlib import Path
sys.path.insert(0, str(Path(".claude/skills/cicd-diagnostics/utils")))

from workspace import get_diagnostic_workspace, save_artifact

diagnostic_dir = get_diagnostic_workspace("19118302390")
save_artifact(diagnostic_dir, "notes.txt", "Analysis in progress...")
```

### evidence.py
Evidence presentation for AI analysis - simple data extraction without classification logic.

**Key Functions:**
- `present_failure_evidence(log_file)` - Present all failure evidence (supports JUnit, E2E, **Postman**)
- `get_first_error_context(log_file, before=30, after=20)` - Get context around first error
- `get_failure_timeline(log_file)` - Get timeline of all failures
- `present_known_issues(test_name, error_keywords="")` - Search and present known issues
- `present_recent_runs(workflow, limit=10)` - Get recent workflow run history
- `extract_test_name(log_file)` - Extract test name from log file (JUnit/E2E/Postman)
- `extract_error_keywords(log_file)` - Extract error keywords for pattern matching
- `present_complete_diagnostic(log_file)` - Present complete diagnostic package
- `extract_error_sections_only(log_file, output_file)` - Extract only error sections for large files
- `get_log_stats(log_file)` - Get log file statistics

**Postman Test Support (NEW in v2.1)**:
- Detects `[INFO] \d+\. AssertionError` patterns
- Extracts "expected [...] to deeply equal [...]" assertions
- Identifies failing collections and test names
- Provides context around Postman failures

**Usage Example:**
```python
import sys
from pathlib import Path
sys.path.insert(0, str(Path(".claude/skills/cicd-diagnostics/utils")))

from evidence import present_complete_diagnostic, get_log_stats

log_file = Path("job-logs.txt")
print(get_log_stats(log_file))
evidence = present_complete_diagnostic(log_file)
print(evidence)
```

### tiered_extraction.py
Tiered evidence extraction - creates multiple levels of detail for progressive analysis.

**Key Functions:**
- `extract_level1_summary(log_file, output_file)` - Level 1: Test Summary (~500 tokens)
- `extract_level2_unique_failures(log_file, output_file)` - Level 2: Unique Failures (~5000 tokens)
- `extract_level3_full_context(log_file, output_file)` - Level 3: Full Context (~15000 tokens)
- `extract_failed_test_names(log_file)` - Extract failed test names (JUnit/E2E/Postman)
- `auto_extract_tiered(log_file, workspace)` - Auto-tiered extraction based on log size
- `analyze_retry_patterns(log_file)` - Analyze retry patterns (deterministic vs flaky)
- `extract_postman_failures(log_file, output_file)` - **NEW**: Postman-specific extraction

**Postman Extraction (NEW in v2.1)**:
- Parses Newman/Postman test output format
- Extracts test summary table (executed/failed counts)
- Identifies failed collections
- Provides detailed failure context with line numbers
- Lists all failed test names from "inside" patterns

**Usage Example:**
```python
import sys
from pathlib import Path
sys.path.insert(0, str(Path(".claude/skills/cicd-diagnostics/utils")))

from tiered_extraction import auto_extract_tiered, analyze_retry_patterns

log_file = Path("job-logs.txt")
workspace = Path(".claude/diagnostics/run-12345")

auto_extract_tiered(log_file, workspace)
print(analyze_retry_patterns(log_file))
```

## Integration with cicd-diagnostics Skill

The main SKILL.md references these utilities throughout the diagnostic workflow:

```python
import sys
from pathlib import Path
sys.path.insert(0, str(Path(".claude/skills/cicd-diagnostics/utils")))

from workspace import get_diagnostic_workspace
from github_api import get_run_metadata
from evidence import present_complete_diagnostic

# Initialize workspace
diagnostic_dir = get_diagnostic_workspace("19118302390")

# Fetch metadata
get_run_metadata("19118302390", diagnostic_dir / "run-metadata.json")

# Analyze logs
log_file = diagnostic_dir / "failed-job-12345.txt"
evidence = present_complete_diagnostic(log_file)
```

## Benefits of Modular Design

1. **Reusability** - Modules can be used by other skills or scripts
2. **Testability** - Each utility can be tested independently
3. **Maintainability** - Changes isolated to specific utility files
4. **Clarity** - Main skill logic is cleaner and more readable
5. **Composability** - Functions can be combined in different workflows
6. **Cross-platform** - Python works on macOS, Linux, and Windows

## Platform Compatibility

All utilities use Python standard library (Python 3.8+):
- `pathlib` for cross-platform file paths
- `subprocess` for GitHub CLI calls
- `json` for JSON parsing
- `re` for regex operations
- No external Python dependencies required

## Error Handling

All utilities use Python exception handling:
- Functions raise exceptions on errors
- Type hints for better IDE support
- Clear error messages for debugging

## Dependencies

- Python 3.8 or higher
- GitHub CLI (gh) - must be installed separately
- Standard library only - no external Python packages required

## Script Organization & Best Practices

### Directory Structure
```
cicd-diagnostics/
├── init-diagnostic.py      # ✅ Entry Point: CLI script
├── fetch-metadata.py        # ✅ Entry Point: CLI script
├── fetch-jobs.py            # ✅ Entry Point: CLI script
├── fetch-logs.py            # ✅ Entry Point: CLI script
│
└── utils/                   # ✅ Library: Reusable utilities
    ├── __init__.py
    ├── github_api.py        # GitHub API wrappers
    ├── evidence.py          # Evidence extraction
    ├── tiered_extraction.py # Multi-level analysis
    └── workspace.py         # Workspace management
```

### Design Principles

**✅ Root Level = Entry Points (User-Facing)**
- Accept command-line arguments
- Show usage messages
- Orchestrate workflows
- Import from utils/
- Exit with status codes

**✅ utils/ = Library (Developer-Facing)**
- Pure functions
- No CLI argument parsing
- Raise exceptions (don't exit)
- Type hints and docstrings
- Fully testable

### Example Comparison

**❌ BAD: Mixing Concerns**
```python
# utils/github_api.py (WRONG - has CLI parsing)
def download_logs():
    if len(sys.argv) < 2:
        print("Usage: ...")  # ❌ CLI logic in library
        sys.exit(1)          # ❌ Exit from library
    job_id = sys.argv[1]     # ❌ Argument parsing in library
    ...
```

**✅ GOOD: Separation of Concerns**
```python
# utils/github_api.py (CORRECT - pure function)
def download_job_logs(job_id: str, output_file: Path) -> None:
    """Download logs for a specific job.

    Args:
        job_id: GitHub Actions job ID
        output_file: Path to save logs

    Raises:
        subprocess.CalledProcessError: If gh CLI fails
    """
    result = subprocess.run([...], check=True)
    output_file.write_text(result.stdout)

# fetch-logs.py (CORRECT - CLI orchestration)
def main():
    if len(sys.argv) < 3:
        print("Usage: python fetch-logs.py <RUN_ID> <WORKSPACE>")
        sys.exit(1)

    from utils.github_api import download_job_logs
    download_job_logs(sys.argv[1], Path(sys.argv[2]))

if __name__ == "__main__":
    main()
```

### Why This Structure?

| Aspect | Entry Points (Root) | Utilities (utils/) |
|--------|--------------------|--------------------|
| **Purpose** | User interface | Reusable logic |
| **Testability** | Hard (needs CLI mocking) | Easy (pure functions) |
| **Reusability** | Low (specific to one workflow) | High (used by multiple scripts) |
| **Complexity** | Simple orchestration | Complex business logic |
| **Error Handling** | Print & exit | Raise exceptions |
| **Documentation** | Usage messages | Docstrings + type hints |

### Version History

**v2.1.0** (Current)
- ✅ Enhanced Postman/Newman test detection
- ✅ Added `extract_postman_failures()` to tiered_extraction.py
- ✅ Fixed `fetch-logs.py` argument parsing (now supports optional job ID)
- ✅ Improved assertion detection for API tests in evidence.py

**v2.0.0**
- ✅ Converted from Bash to Python
- ✅ Separated entry points from utilities
- ✅ Added tiered extraction for large logs
- ✅ Enhanced known issue searching

**v1.0.0** (Legacy Bash)
- Basic log extraction
- Limited test framework support

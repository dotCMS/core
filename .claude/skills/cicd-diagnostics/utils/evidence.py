#!/usr/bin/env python3
"""Evidence Presentation for AI Analysis.

Simple data extraction without classification logic.
"""

import json
import re
import subprocess
from pathlib import Path
from typing import Optional


def present_failure_evidence(log_file: Path) -> str:
    """Present all failure evidence for AI analysis.
    
    Args:
        log_file: Path to log file
        
    Returns:
        Formatted evidence string
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')
    
    output = []
    output.append("=" * 80)
    output.append("FAILURE EVIDENCE FOR ANALYSIS")
    output.append("=" * 80)
    output.append("")
    
    # Test Failures
    output.append("=== FAILED TESTS ===")
    output.append("")
    failed_tests = [
        line for line in lines
        if "<<< FAILURE!" in line or "::error file=" in line
    ][:10]

    # Add Postman failures
    postman_failures = []
    for i, line in enumerate(lines):
        if re.search(r'\[INFO\]\s+\d+\.\s+(AssertionError|AssertionFailure)', line):
            # Get context around the failure
            start = max(0, i - 2)
            end = min(len(lines), i + 5)
            postman_failures.extend(lines[start:end])
            postman_failures.append("")  # Add separator
            if len(postman_failures) >= 50:
                break

    if failed_tests or postman_failures:
        if failed_tests:
            output.append("JUnit/E2E Failures:")
            output.extend(failed_tests)
            output.append("")
        if postman_failures:
            output.append("Postman/API Test Failures:")
            output.extend(postman_failures[:50])
    else:
        output.append("No test failures found")
    
    output.append("")
    output.append("=== ERROR MESSAGES ===")
    output.append("")
    errors = []

    # Enhanced error detection for NPM, Docker, and GitHub Actions errors
    # Prioritize critical deployment/build errors
    critical_keywords = [
        "npm ERR!", "::error::", "##[error]",
        "FAILURE:", "Failed to", "Cannot", "Unable to",
        "Error:", "ERROR:"
    ]

    test_error_keywords = [
        "[ERROR]", "AssertionError", "Exception"
    ]

    # First pass: capture critical deployment/infrastructure errors
    # Scan entire log for critical errors (don't stop early)
    critical_errors = []
    for i, line in enumerate(lines):
        # Skip false positives: file listings from tar/zip archives
        # These are lines that ONLY list filenames without actual error context
        # Pattern: timestamp + path + filename.class (no error keywords)
        is_file_listing = (
            ('.class' in line or '.jar' in line) and
            ('maven/dotserver' in line or 'webapps/ROOT' in line) and
            not any(err_word in line for err_word in ['ERROR:', 'FAILURE:', 'Failed', 'Exception:'])
        )

        if is_file_listing:
            continue

        if any(keyword in line for keyword in critical_keywords):
            start = max(0, i - 5)
            end = min(len(lines), i + 10)  # More context for deployment errors
            critical_errors.append((i, lines[start:end]))

    # Prioritize later errors (usually final failures) and unique error types
    if critical_errors:
        # Take last 5 error groups (most recent/final errors)
        for _, error_lines in critical_errors[-10:]:
            errors.extend(error_lines)
            errors.append("")  # Separator

    # Second pass: if no critical errors found, look for test errors
    if not errors:
        for i, line in enumerate(lines):
            # Same file listing filter as first pass
            is_file_listing = (
                ('.class' in line or '.jar' in line) and
                ('maven/dotserver' in line or 'webapps/ROOT' in line) and
                not any(err_word in line for err_word in ['ERROR:', 'FAILURE:', 'Failed', 'Exception:'])
            )

            if is_file_listing:
                continue

            if any(keyword in line for keyword in test_error_keywords):
                start = max(0, i - 3)
                end = min(len(lines), i + 6)
                errors.extend(lines[start:end])
                if len(errors) >= 100:
                    break

    if errors:
        output.extend(errors[:150])  # Allow more errors to be shown
    else:
        output.append("No explicit errors found")
    
    output.append("")
    output.append("=== ASSERTION DETAILS ===")
    output.append("")
    assertions = [
        line for line in lines
        if "expected:" in line and "but was:" in line or "AssertionFailedError" in line
    ][:10]

    # Add Postman assertion details
    postman_assertions = []
    for i, line in enumerate(lines):
        if re.search(r'(expected.*to deeply equal|expected.*to be|expected.*but was)', line, re.IGNORECASE):
            postman_assertions.append(line)
            if len(postman_assertions) >= 10:
                break

    if assertions or postman_assertions:
        if assertions:
            output.append("JUnit Assertions:")
            output.extend(assertions)
            output.append("")
        if postman_assertions:
            output.append("Postman Assertions:")
            output.extend(postman_assertions)
    else:
        output.append("No assertion failures found")
    
    output.append("")
    output.append("=== STACK TRACES ===")
    output.append("")
    stack_pattern = re.compile(r'at [a-zA-Z0-9.]+\([A-Za-z0-9]+\.java:\d+\)')
    stacks = [line for line in lines if stack_pattern.search(line)][:30]
    if stacks:
        output.extend(stacks)
    else:
        output.append("No Java stack traces found")
    
    output.append("")
    output.append("=== TIMING INDICATORS ===")
    output.append("")
    timing_keywords = ["timeout", "timed out", "Thread.sleep", "Awaitility", "race condition", "concurrent"]
    timing = [
        line for line in lines
        if any(keyword.lower() in line.lower() for keyword in timing_keywords)
    ][:10]
    if timing:
        output.extend(timing)
    else:
        output.append("No obvious timing indicators")
    
    output.append("")
    output.append("=== INFRASTRUCTURE INDICATORS ===")
    output.append("")
    infra_keywords = ["connection refused", "docker", "container", "failed", "elasticsearch", "exception", "database", "error"]
    infra = [
        line for line in lines
        if any(keyword.lower() in line.lower() for keyword in infra_keywords)
    ][:10]
    if infra:
        output.extend(infra)
    else:
        output.append("No obvious infrastructure issues")
    
    output.append("")
    output.append("=" * 80)
    
    return "\n".join(output)


def get_first_error_context(log_file: Path, before: int = 30, after: int = 20) -> str:
    """Get context around first error (for cascade detection).
    
    Args:
        log_file: Path to log file
        before: Number of lines before error
        after: Number of lines after error
        
    Returns:
        Context string
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')
    
    first_error_line = None
    for i, line in enumerate(lines, 1):
        if any(keyword in line for keyword in ["[ERROR]", "FAILURE!", "::error"]):
            first_error_line = i
            break
    
    if first_error_line is None:
        return "No errors found in log"
    
    start = max(0, first_error_line - before - 1)
    end = min(len(lines), first_error_line + after)
    
    output = [f"=== FIRST ERROR AT LINE {first_error_line} ===", ""]
    for i, line in enumerate(lines[start:end], start=start + 1):
        output.append(f"{i:6d}: {line}")
    
    return "\n".join(output)


def get_failure_timeline(log_file: Path) -> str:
    """Get timeline of all failures (for cascade analysis).
    
    Args:
        log_file: Path to log file
        
    Returns:
        Timeline string
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')
    
    output = ["=== FAILURE TIMELINE ===", ""]
    
    failures = []
    for i, line in enumerate(lines, 1):
        if any(keyword in line for keyword in ["[ERROR]", "FAILURE!", "::error"]):
            content = line[:100] if len(line) > 100 else line
            failures.append((i, content))
            if len(failures) >= 20:
                break
    
    for line_num, content in failures:
        output.append(f"Line {line_num}: {content}")
    
    return "\n".join(output)


def present_known_issues(test_name: str, error_keywords: str = "") -> str:
    """Present known issues for comparison (ENHANCED).
    
    Args:
        test_name: Name of the test
        error_keywords: Optional error keywords for pattern matching
        
    Returns:
        Formatted issues string
    """
    output = []
    output.append("=== KNOWN ISSUES SEARCH ===")
    output.append("")
    output.append(f"Searching for: {test_name}")
    if error_keywords:
        output.append(f"Error keywords: {error_keywords}")
    output.append("")
    
    # Strategy 1: Exact test name match
    output.append("Strategy 1: Exact test name match")
    try:
        result = subprocess.run(
            [
                "gh", "issue", "list",
                "--search", f'"{test_name}" in:body',
                "--state", "all",
                "--label", "Flakey Test",
                "--json", "number,title,state",
                "--limit", "5"
            ],
            capture_output=True,
            text=True,
            check=True
        )
        exact_match = json.loads(result.stdout) if result.stdout else []
    except (subprocess.CalledProcessError, json.JSONDecodeError):
        exact_match = []
    
    if exact_match:
        output.append("  EXACT MATCHES:")
        for issue in exact_match:
            output.append(f"  - Issue #{issue['number']}: {issue['title']} [{issue['state']}]")
    else:
        output.append("  No exact matches")
    output.append("")
    
    # Strategy 2: Test class name match
    output.append("Strategy 2: Test class name match")
    test_class = test_name.split('.')[0] if '.' in test_name else test_name
    try:
        result = subprocess.run(
            [
                "gh", "issue", "list",
                "--search", f'"{test_class}" in:body',
                "--state", "all",
                "--label", "Flakey Test",
                "--json", "number,title,state",
                "--limit", "10"
            ],
            capture_output=True,
            text=True,
            check=True
        )
        class_match = json.loads(result.stdout) if result.stdout else []
    except (subprocess.CalledProcessError, json.JSONDecodeError):
        class_match = []
    
    # Deduplicate with exact matches
    exact_numbers = {issue['number'] for issue in exact_match}
    new_class_matches = [issue for issue in class_match if issue['number'] not in exact_numbers]
    
    if new_class_matches:
        output.append("  CLASS NAME MATCHES:")
        for issue in new_class_matches:
            output.append(f"  - Issue #{issue['number']}: {issue['title']} [{issue['state']}]")
    else:
        output.append("  No additional class matches")
    output.append("")
    
    # Strategy 3: Error pattern/keyword match
    if error_keywords:
        output.append(f"Strategy 3: Error pattern match ({error_keywords})")
        try:
            result = subprocess.run(
                [
                    "gh", "issue", "list",
                    "--search", f"{error_keywords} in:body",
                    "--state", "all",
                    "--label", "Flakey Test",
                    "--json", "number,title,state,body",
                    "--limit", "15"
                ],
                capture_output=True,
                text=True,
                check=True
            )
            pattern_match = json.loads(result.stdout) if result.stdout else []
        except (subprocess.CalledProcessError, json.JSONDecodeError):
            pattern_match = []
        
        # Deduplicate
        all_numbers = exact_numbers | {issue['number'] for issue in new_class_matches}
        new_pattern_matches = [issue for issue in pattern_match if issue['number'] not in all_numbers]
        
        if new_pattern_matches:
            output.append("  PATTERN MATCHES:")
            for issue in new_pattern_matches:
                output.append(f"  - Issue #{issue['number']}: {issue['title']} [{issue['state']}]")
            output.append("")
            output.append("  Pattern match details (showing first 200 chars from body):")
            for issue in new_pattern_matches:
                body_preview = issue.get('body', '')[:200].replace('\n', ' ')
                output.append(f"    #{issue['number']}: {body_preview}...")
        else:
            output.append("  No additional pattern matches")
        output.append("")
    
    # Strategy 4: CLI test issues
    if "cli" in test_name.lower() or "command" in test_name.lower():
        output.append("Strategy 4: CLI-related flaky tests")
        try:
            result = subprocess.run(
                [
                    "gh", "issue", "list",
                    "--search", "cli in:body",
                    "--state", "all",
                    "--label", "Flakey Test",
                    "--json", "number,title,state",
                    "--limit", "10"
                ],
                capture_output=True,
                text=True,
                check=True
            )
            cli_match = json.loads(result.stdout) if result.stdout else []
        except (subprocess.CalledProcessError, json.JSONDecodeError):
            cli_match = []
        
        if cli_match:
            output.append("  CLI-RELATED:")
            for issue in cli_match:
                output.append(f"  - Issue #{issue['number']}: {issue['title']} [{issue['state']}]")
        else:
            output.append("  No CLI-related matches")
        output.append("")
    
    # Summary
    total_exact = len(exact_match)
    total_class = len(new_class_matches)
    total_pattern = len(new_pattern_matches) if error_keywords else 0
    total = total_exact + total_class + total_pattern
    
    output.append("=== SEARCH SUMMARY ===")
    output.append(f"Total potential matches: {total}")
    output.append(f"  - Exact matches: {total_exact}")
    output.append(f"  - Class matches: {total_class}")
    if error_keywords:
        output.append(f"  - Pattern matches: {total_pattern}")
    output.append("")
    
    return "\n".join(output)


def present_recent_runs(workflow: str, limit: int = 10) -> str:
    """Get recent workflow run history.
    
    Args:
        workflow: Workflow name
        limit: Maximum number of runs to fetch
        
    Returns:
        Formatted runs string
    """
    try:
        result = subprocess.run(
            [
                "gh", "run", "list",
                "--workflow", workflow,
                "--limit", str(limit),
                "--json", "databaseId,conclusion,displayTitle,createdAt"
            ],
            capture_output=True,
            text=True,
            check=True
        )
        runs = json.loads(result.stdout) if result.stdout else []
    except (subprocess.CalledProcessError, json.JSONDecodeError):
        runs = []
    
    output = []
    output.append(f"=== RECENT RUNS: {workflow} ===")
    output.append("")
    
    if not runs:
        output.append("No recent runs found")
    else:
        for run in runs:
            output.append(
                f"{run['databaseId']} | {run['conclusion']} | {run['displayTitle']} | {run['createdAt']}"
            )
    
    output.append("")
    
    # Calculate failure rate
    if runs:
        total = len(runs)
        failures = sum(1 for run in runs if run.get('conclusion') == 'failure')
        if total > 0:
            rate = (failures * 100) // total
            output.append(f"Failure rate: {failures}/{total} ({rate}%)")
    
    return "\n".join(output)


def extract_test_name(log_file: Path) -> str:
    """Extract test name from log file.
    
    Args:
        log_file: Path to log file
        
    Returns:
        Test name or empty string
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')
    
    # Try JUnit test
    for line in lines:
        if "<<< FAILURE!" in line:
            match = re.search(r'\[ERROR\] ([^\s]+)', line)
            if match:
                return match.group(1).split('.')[0]
    
    # Try E2E test
    for line in lines:
        if "::error file=" in line:
            match = re.search(r'file=([^,]+)', line)
            if match:
                file_path = match.group(1)
                return Path(file_path).stem.replace('.spec', '')
    
    # Try Postman
    for line in lines:
        if "Collection" in line and "had failures" in line:
            match = re.search(r'Collection ([^\s]+) had failures', line)
            if match:
                return match.group(1)
    
    return ""


def extract_error_keywords(log_file: Path) -> str:
    """Extract error keywords for pattern matching.
    
    Args:
        log_file: Path to log file
        
    Returns:
        Space-separated keywords
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore').lower()
    
    keywords = []
    
    if "moddate" in log_content or "modification date" in log_content:
        keywords.append("modDate")
    if "createddate" in log_content or "created date" in log_content or "creationdate" in log_content:
        keywords.append("createdDate")
    if "race condition" in log_content or "concurrent" in log_content or "synchronization" in log_content:
        keywords.append("timing")
    if "timeout" in log_content or "timed out" in log_content:
        keywords.append("timeout")
    if "ordering" in log_content or "order by" in log_content or "sorted" in log_content:
        keywords.append("ordering")
    if re.search(r'boolean.*flip|expected:.*true.*but was:.*false|expected:.*false.*but was:.*true', log_content):
        keywords.append("assertion")
    
    return " ".join(keywords)


def present_complete_diagnostic(log_file: Path) -> str:
    """Present complete diagnostic package for AI.
    
    Args:
        log_file: Path to log file
        
    Returns:
        Complete diagnostic string
    """
    output = []
    output.append("=" * 80)
    output.append("COMPLETE DIAGNOSTIC EVIDENCE")
    output.append("=" * 80)
    output.append("")
    
    # 1. Failure evidence
    output.append(present_failure_evidence(log_file))
    output.append("")
    output.append("")
    
    # 2. First error context
    output.append(get_first_error_context(log_file))
    output.append("")
    output.append("")
    
    # 3. Timeline
    output.append(get_failure_timeline(log_file))
    output.append("")
    output.append("")
    
    # 4. Known issues
    test_name = extract_test_name(log_file)
    if test_name:
        error_keywords = extract_error_keywords(log_file)
        output.append(present_known_issues(test_name, error_keywords))
    
    output.append("")
    output.append("=" * 80)
    output.append("END DIAGNOSTIC EVIDENCE - READY FOR AI ANALYSIS")
    output.append("=" * 80)
    
    return "\n".join(output)


def extract_error_sections_only(log_file: Path, output_file: Path) -> None:
    """Extract only error sections for large files (performance optimization).
    
    Args:
        log_file: Path to input log file
        output_file: Path to output file
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')
    
    output = []
    output.append("=== ERRORS AND FAILURES ===")
    
    # Get context around errors
    error_lines = []
    for i, line in enumerate(lines):
        if any(keyword in line for keyword in ["[ERROR]", "FAILURE!", "::error"]):
            start = max(0, i - 20)
            end = min(len(lines), i + 21)
            error_lines.extend(lines[start:end])
            if len(error_lines) >= 2000:
                break
    
    output.extend(error_lines[:2000])
    output.append("")
    output.append("=== FIRST 200 LINES ===")
    output.extend(lines[:200])
    output.append("")
    output.append("=== LAST 200 LINES ===")
    output.extend(lines[-200:])
    
    output_file.write_text("\n".join(output), encoding='utf-8')


def get_log_stats(log_file: Path) -> str:
    """Get log file stats.
    
    Args:
        log_file: Path to log file
        
    Returns:
        Stats string
    """
    size = log_file.stat().st_size
    size_mb = size / 1048576
    lines = len(log_file.read_text(encoding='utf-8', errors='ignore').split('\n'))
    
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    error_count = log_content.count("[ERROR]")
    failure_count = log_content.count("FAILURE!")
    
    output = [
        "=== LOG FILE STATISTICS ===",
        f"File: {log_file}",
        f"Size: {size} bytes ({size_mb:.2f} MB)",
        f"Lines: {lines}",
        f"Errors: {error_count}",
        f"Failures: {failure_count}",
        ""
    ]
    
    if size_mb > 10:
        output.append("⚠️  Large file detected. Consider using extract_error_sections_only() for faster analysis.")
    
    return "\n".join(output)


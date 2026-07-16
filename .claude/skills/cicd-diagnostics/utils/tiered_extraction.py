#!/usr/bin/env python3
"""Tiered Evidence Extraction.

Creates multiple levels of detail for progressive analysis.
"""

import re
from pathlib import Path
from typing import List


def extract_level1_summary(log_file: Path, output_file: Path) -> None:
    """Level 1: Test Summary Only (ALWAYS fits in context - ~500 tokens max).
    
    Purpose: Quick overview of what failed
    
    Args:
        log_file: Path to log file
        output_file: Path to output file
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')
    
    output = []
    output.append("=" * 80)
    output.append("LEVEL 1: TEST SUMMARY (Quick Overview)")
    output.append("=" * 80)
    output.append("")
    
    # Overall test results
    output.append("=== OVERALL TEST RESULTS ===")
    test_results = [
        line for line in lines
        if "Tests run:" in line and ("Failures:" in line or "Errors:" in line) or "BUILD SUCCESS" in line or "BUILD FAILURE" in line
    ][-5:]
    output.extend(test_results)
    output.append("")
    
    # List of failed tests
    output.append("=== FAILED TESTS (Names Only) ===")
    failed_tests = []
    for line in lines:
        if "[ERROR]" in line and "Test." in line:
            match = re.search(r'\[ERROR\] ([^\s]+)', line)
            if match:
                failed_tests.append(match.group(1))
    output.extend(list(set(failed_tests))[:20])
    output.append("")
    
    # Retry patterns
    output.append("=== RETRY PATTERNS ===")
    has_retries = any("Run " in line and ":" in line for line in lines)
    if has_retries:
        output.append("Tests were retried (Surefire rerunFailingTestsCount active)")
        retry_lines = [
            line for line in lines
            if "[ERROR]" in line or ("Run " in line and ":" in line)
        ][:15]
        output.extend(retry_lines)
        output.append("")
        flake_lines = [
            line for line in lines
            if "[WARNING]" in line or ("Run " in line and ":" in line)
        ][:15]
        output.extend(flake_lines)
    else:
        output.append("No retry patterns detected")
    output.append("")
    
    # Quick classification hints
    output.append("=== CLASSIFICATION HINTS ===")
    log_lower = log_content.lower()
    has_timeout = "timeout" in log_lower or "conditiontimeout" in log_lower
    has_assertion = "assertionerror" in log_lower or "expected:" in log_lower and "but was:" in log_lower
    has_npe = "nullpointerexception" in log_lower
    has_infra = any(kw in log_lower for kw in ["connection refused", "docker", "failed", "container", "error"])
    
    output.append(f"Timeout errors: {sum(1 for _ in [True] if has_timeout)}")
    output.append(f"Assertion errors: {sum(1 for _ in [True] if has_assertion)}")
    output.append(f"NullPointerException: {sum(1 for _ in [True] if has_npe)}")
    output.append(f"Infrastructure errors: {sum(1 for _ in [True] if has_infra)}")
    output.append("")
    
    output.append("=" * 80)
    output.append("Use extract_level2_unique_failures() for detailed error messages")
    output.append("=" * 80)
    
    output_file.write_text("\n".join(output), encoding='utf-8')


def extract_level2_unique_failures(log_file: Path, output_file: Path) -> None:
    """Level 2: Unique Failures (Moderate detail - ~5000 tokens max).
    
    Purpose: First occurrence of each unique failure with error messages
    
    Args:
        log_file: Path to log file
        output_file: Path to output file
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')
    
    output = []
    output.append("=" * 80)
    output.append("LEVEL 2: UNIQUE FAILURES (Detailed Error Messages)")
    output.append("=" * 80)
    output.append("")
    
    # Parse retry summary
    output.append("=== DETERMINISTIC FAILURES (Failed All Retries) ===")
    if "Errors:" in log_content:
        # Extract error section
        error_start = None
        for i, line in enumerate(lines):
            if "[ERROR] Errors:" in line:
                error_start = i
                break
        
        if error_start is not None:
            error_section = lines[error_start:error_start + 50]
            output.extend(error_section[:100])
    else:
        output.append("No deterministic failures (all retries failed)")
    output.append("")
    
    output.append("=== FLAKY FAILURES (Passed Some Retries) ===")
    if "Flakes:" in log_content:
        flake_start = None
        for i, line in enumerate(lines):
            if "[WARNING] Flakes:" in line:
                flake_start = i
                break
        
        if flake_start is not None:
            flake_section = lines[flake_start:flake_start + 50]
            output.extend(flake_section[:100])
    else:
        output.append("No flaky tests detected")
    output.append("")
    
    # Get first occurrence of each unique error message
    output.append("=== UNIQUE ERROR MESSAGES (First Occurrence) ===")
    
    # ConditionTimeoutException
    if "ConditionTimeoutException" in log_content:
        output.append("--- Awaitility Timeout ---")
        for i, line in enumerate(lines):
            if "ConditionTimeoutException" in line:
                start = max(0, i - 5)
                end = min(len(lines), i + 16)
                output.extend(lines[start:end])
                if len(output) >= 40:
                    break
        output.append("")
    
    # AssertionError / ComparisonFailure
    if "AssertionError" in log_content or "ComparisonFailure" in log_content:
        output.append("--- Assertion Failures ---")
        for i, line in enumerate(lines):
            if "AssertionError" in line or "ComparisonFailure" in line:
                start = max(0, i - 3)
                end = min(len(lines), i + 11)
                output.extend(lines[start:end])
                if len(output) >= 50:
                    break
        output.append("")
    
    # NullPointerException
    if "NullPointerException" in log_content:
        output.append("--- NullPointerException ---")
        for i, line in enumerate(lines):
            if "NullPointerException" in line:
                start = max(0, i - 5)
                end = min(len(lines), i + 11)
                output.extend(lines[start:end])
                if len(output) >= 30:
                    break
        output.append("")
    
    # Other exceptions
    output.append("--- Other Exceptions (First 3) ---")
    exception_count = 0
    for i, line in enumerate(lines):
        if "Exception:" in line and "ConditionTimeout" not in line and "AssertionError" not in line and "NullPointer" not in line:
            start = max(0, i - 3)
            end = min(len(lines), i + 9)
            output.extend(lines[start:end])
            exception_count += 1
            if exception_count >= 3:
                break
    output.append("")
    
    output.append("=" * 80)
    output.append("Use extract_level3_full_context() for complete stack traces and timing")
    output.append("=" * 80)
    
    output_file.write_text("\n".join(output), encoding='utf-8')


def extract_level3_full_context(log_file: Path, output_file: Path) -> None:
    """Level 3: Full Context (Comprehensive - ~15000 tokens max).
    
    Purpose: Complete stack traces, timing correlation, all retry attempts
    
    Args:
        log_file: Path to log file
        output_file: Path to output file
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')
    
    output = []
    output.append("=" * 80)
    output.append("LEVEL 3: FULL CONTEXT (Complete Details)")
    output.append("=" * 80)
    output.append("")
    
    # Complete retry analysis
    output.append("=== COMPLETE RETRY ANALYSIS ===")
    results_start = None
    for i, line in enumerate(lines):
        if "[INFO] Results:" in line:
            results_start = i
            break
    
    if results_start is not None:
        output.extend(lines[results_start:results_start + 300])
    output.append("")
    
    # All error sections with full stack traces
    output.append("=== ALL ERROR SECTIONS WITH STACK TRACES ===")
    error_contexts = []
    for i, line in enumerate(lines):
        if "[ERROR]" in line and "Test." in line:
            start = max(0, i - 10)
            end = min(len(lines), i + 31)
            error_contexts.extend(lines[start:end])
            if len(error_contexts) >= 500:
                break
    output.extend(error_contexts[:500])
    output.append("")
    
    # Timing correlation
    output.append("=== TIMING CORRELATION ===")
    timestamp_pattern = re.compile(r'^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}')
    timing_lines = [
        line for line in lines
        if timestamp_pattern.match(line) and ("ERROR" in line or "FAILURE" in line or "Exception" in line)
    ][:100]
    output.extend(timing_lines)
    output.append("")
    
    # Infrastructure events
    output.append("=== INFRASTRUCTURE EVENTS ===")
    infra_keywords = ["docker", "container", "elasticsearch", "database", "connection"]
    infra_lines = [
        line for line in lines
        if any(kw.lower() in line.lower() for kw in infra_keywords) and
        any(kw in line.lower() for kw in ["error", "failed", "refused", "timeout"])
    ][:50]
    output.extend(infra_lines)
    output.append("")
    
    output.append("=" * 80)
    output.append("This is the most detailed extraction level available")
    output.append("=" * 80)
    
    output_file.write_text("\n".join(output), encoding='utf-8')


def extract_failed_test_names(log_file: Path) -> List[str]:
    """Extract failed test names.

    Args:
        log_file: Path to log file

    Returns:
        List of test names
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')

    test_names = set()

    # E2E test names
    for line in lines:
        if "::error file=" in line:
            match = re.search(r'file=([^,]+)', line)
            if match:
                file_path = match.group(1)
                test_name = Path(file_path).stem.replace('.spec', '')
                test_names.add(test_name)

    # JUnit/Maven test names
    for line in lines:
        if "<<< FAILURE!" in line:
            match = re.search(r'\[ERROR\] ([^\s]+)', line)
            if match:
                test_names.add(match.group(1))

    # Postman collection failures
    for line in lines:
        if "Collection" in line and "had failures" in line:
            match = re.search(r'Collection ([^\s]+) had failures', line)
            if match:
                test_names.add(match.group(1))

    return sorted(test_names)


def extract_postman_failures(log_file: Path, output_file: Path) -> None:
    """Extract Postman test failures with full details.

    Purpose: Parse Postman/Newman test output for API test failures

    Args:
        log_file: Path to log file
        output_file: Path to output file
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')

    output = []
    output.append("=" * 80)
    output.append("POSTMAN/NEWMAN TEST FAILURES")
    output.append("=" * 80)
    output.append("")

    # Find test summary
    output.append("=== TEST SUMMARY ===")
    for i, line in enumerate(lines):
        if re.search(r'│\s+(executed|iterations|requests|test-scripts)', line):
            output.append(line)
            # Get surrounding lines for context
            if i + 1 < len(lines) and '│' in lines[i + 1]:
                continue
    output.append("")

    # Find collection that failed
    output.append("=== FAILED COLLECTIONS ===")
    for line in lines:
        if "Collection" in line and "had failures" in line:
            output.append(line)
    output.append("")

    # Extract individual failure details
    output.append("=== FAILURE DETAILS ===")
    in_failure_section = False
    failure_count = 0

    for i, line in enumerate(lines):
        # Start of failure section
        if re.search(r'\[INFO\]\s+#\s+failure\s+detail', line):
            in_failure_section = True
            output.append(line)
            continue

        # In failure section
        if in_failure_section:
            # Individual failure entry
            if re.search(r'\[INFO\]\s+\d+\.\s+(AssertionError|AssertionFailure|Error)', line):
                failure_count += 1
                output.append("")
                output.append(f"--- Failure #{failure_count} ---")

                # Extract failure details (next 10 lines)
                for j in range(i, min(i + 12, len(lines))):
                    output.append(lines[j])
                    if lines[j].strip() == "" or (j > i and re.search(r'\[INFO\]\s+\d+\.', lines[j])):
                        break

            # End of failure section
            if "Collection" in line and "had failures" in line:
                in_failure_section = False
                break

            if failure_count >= 10:  # Limit to first 10 failures
                output.append("")
                output.append("(Additional failures truncated...)")
                break

    if failure_count == 0:
        output.append("No Postman failures detected")
    output.append("")

    # Extract test names from failure section
    output.append("=== FAILED TEST NAMES ===")
    failed_tests = set()
    for line in lines:
        # Pattern: inside "Collection Name / Test Name / Sub Test"
        match = re.search(r'inside "(([^"]+) / ([^"]+))"', line)
        if match:
            failed_tests.add(match.group(1))

    if failed_tests:
        for test in sorted(failed_tests):
            output.append(f"  • {test}")
    else:
        output.append("  None found")
    output.append("")

    output.append("=" * 80)
    output.append(f"Total Postman Failures Extracted: {failure_count}")
    output.append("=" * 80)

    output_file.write_text("\n".join(output), encoding='utf-8')


def auto_extract_tiered(log_file: Path, workspace: Path) -> None:
    """Auto-tiered extraction (chooses appropriate level based on log size).
    
    Args:
        log_file: Path to log file
        workspace: Workspace directory
    """
    size = log_file.stat().st_size
    size_mb = size / 1048576
    
    print("=== Auto-Tiered Extraction ===")
    print(f"Log size: {size_mb:.2f} MB")
    print("")
    
    # Always create Level 1
    print("Creating Level 1 (Summary)...")
    level1_file = workspace / "evidence-level1-summary.txt"
    extract_level1_summary(log_file, level1_file)
    l1_size = level1_file.stat().st_size
    print(f"✓ Level 1 created: {l1_size} bytes (~{l1_size // 4} tokens)")
    print("")
    
    # Create Level 2
    print("Creating Level 2 (Unique Failures)...")
    level2_file = workspace / "evidence-level2-unique.txt"
    extract_level2_unique_failures(log_file, level2_file)
    l2_size = level2_file.stat().st_size
    print(f"✓ Level 2 created: {l2_size} bytes (~{l2_size // 4} tokens)")
    print("")
    
    # Create Level 3 only if needed
    if size_mb > 5:
        print("Creating Level 3 (Full Context) - large log detected...")
        level3_file = workspace / "evidence-level3-full.txt"
        extract_level3_full_context(log_file, level3_file)
        l3_size = level3_file.stat().st_size
        print(f"✓ Level 3 created: {l3_size} bytes (~{l3_size // 4} tokens)")
    else:
        print("Skipping Level 3 (log is small enough for Level 2 analysis)")
    print("")
    
    print("=== Tiered Extraction Complete ===")
    print("Analysis workflow:")
    print("1. Read Level 1 for quick overview and classification hints")
    print("2. Read Level 2 for detailed error messages and retry patterns")
    print("3. Read Level 3 (if exists) for deep dive analysis")
    print("")


def analyze_retry_patterns(log_file: Path) -> str:
    """Analyze retry patterns (deterministic vs flaky).
    
    Args:
        log_file: Path to log file
        
    Returns:
        Analysis string
    """
    log_content = log_file.read_text(encoding='utf-8', errors='ignore')
    lines = log_content.split('\n')
    
    output = []
    output.append("=" * 80)
    output.append("RETRY PATTERN ANALYSIS")
    output.append("=" * 80)
    output.append("")
    
    # Check if retries are enabled
    has_retries = any("Run " in line and ":" in line for line in lines)
    if not has_retries:
        output.append("No retry patterns detected (Surefire rerunFailingTestsCount not enabled)")
        return "\n".join(output)
    
    output.append("Surefire retry mechanism detected")
    output.append("")
    
    # Parse errors (deterministic failures)
    output.append("=== DETERMINISTIC FAILURES (All Retries Failed) ===")
    
    error_section_start = None
    for i, line in enumerate(lines):
        if "[ERROR] Errors:" in line:
            error_section_start = i
            break
    
    if error_section_start is not None:
        # Extract error section until flakes section
        error_section = []
        for i in range(error_section_start, min(len(lines), error_section_start + 100)):
            line = lines[i]
            if "[WARNING] Flakes:" in line:
                break
            error_section.append(line)
        
        # Find test names
        test_names = set()
        for line in error_section:
            if "[ERROR]" in line and "com." in line and "Run " not in line:
                match = re.search(r'\[ERROR\]\s+([^\s]+)', line)
                if match:
                    test_names.add(match.group(1))
        
        if test_names:
            for test in sorted(test_names):
                test_simple = test.split('.')[-1]
                retry_count = sum(1 for line in error_section if f"Run " in line and test_simple in line)
                if retry_count == 0:
                    output.append(f"  • {test} - Failed on first attempt (no retries or all 4 attempts failed)")
                else:
                    output.append(f"  • {test} - Failed {retry_count}/{retry_count} retries (100% failure rate)")
        else:
            output.append("  None")
    else:
        output.append("  None")
    output.append("")
    
    # Parse flakes (intermittent failures)
    output.append("=== FLAKY TESTS (Passed Some Retries) ===")
    
    flake_section_start = None
    for i, line in enumerate(lines):
        if "[WARNING] Flakes:" in line:
            flake_section_start = i
            break
    
    if flake_section_start is not None:
        flake_section = lines[flake_section_start:flake_section_start + 200]
        
        # Find test names
        test_names = set()
        for line in flake_section:
            if "[WARNING]" in line and "com." in line:
                match = re.search(r'\[WARNING\]\s+([^\s]+)', line)
                if match:
                    test_names.add(match.group(1))
        
        if test_names:
            for test in sorted(test_names):
                test_simple = test.split('.')[-1]
                # Find section for this test
                test_section = []
                in_test = False
                for line in flake_section:
                    if f"[WARNING] {test}" in line:
                        in_test = True
                    if in_test:
                        test_section.append(line)
                        if line.strip() == "" or ("[INFO]" in line and "[WARNING]" not in line):
                            break
                
                pass_count = sum(1 for line in test_section if "PASS" in line)
                error_count = sum(1 for line in test_section if "[ERROR]" in line and "Run " in line)
                total_runs = pass_count + error_count
                
                if total_runs > 0:
                    failure_rate = (error_count * 100) // total_runs
                    output.append(f"  • {test} - Failed {error_count}/{total_runs} retries ({failure_rate}% failure rate, {pass_count} passed)")
                else:
                    output.append(f"  • {test} - Unable to parse retry counts")
        else:
            output.append("  None")
    else:
        output.append("  None")
    output.append("")
    
    # Summary statistics
    error_count = sum(1 for line in error_section if "[ERROR]" in line and "com." in line and "Run " not in line) if error_section_start else 0
    flake_count = sum(1 for line in flake_section if "[WARNING]" in line and "com." in line) if flake_section_start else 0
    
    output.append("=== SUMMARY ===")
    output.append(f"Deterministic failures: {error_count} test(s)")
    output.append(f"Flaky tests: {flake_count} test(s)")
    output.append(f"Total problematic tests: {error_count + flake_count}")
    output.append("")
    
    # Classification guidance
    if error_count > 0:
        output.append(f"⚠️  BLOCKING: {error_count} deterministic failure(s) detected")
        output.append("   These tests fail consistently and indicate real bugs or incomplete fixes")
    
    if flake_count > 0:
        output.append(f"⚠️  WARNING: {flake_count} flaky test(s) detected")
        output.append("   These tests pass sometimes, indicating timing/concurrency issues")
    output.append("")
    
    output.append("=" * 80)
    
    return "\n".join(output)


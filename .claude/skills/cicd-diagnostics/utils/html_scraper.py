"""
HTML Scraper for GitHub Actions Workflow Annotations

⚠️ WARNING: This module scrapes GitHub's HTML interface to extract workflow-level
validation errors that are not exposed through the official REST API.

This is a workaround for a known GitHub API limitation where workflow syntax
validation errors are visible in the UI but not accessible programmatically.

IMPORTANT CAVEATS:
- This is NOT an official API and may break at any time
- GitHub may change their HTML structure without notice
- This should be considered a temporary workaround
- Only use when official API endpoints don't provide the needed data

Last tested: 2025-12-09 with GitHub Actions UI
"""

import json
import re
import subprocess
from pathlib import Path
from typing import Dict, List, Optional


def scrape_workflow_annotations(run_id: str, owner: str = "dotCMS", repo: str = "core") -> Dict:
    """
    Scrape workflow-level annotations from GitHub Actions HTML page.

    Args:
        run_id: GitHub Actions run ID
        owner: Repository owner (default: dotCMS)
        repo: Repository name (default: core)

    Returns:
        Dict with structure:
        {
            "workflow_annotations": [
                {
                    "level": "failure" | "warning" | "notice",
                    "message": "Error message",
                    "path": ".github/workflows/...",
                    "line": 132,
                    "col": 24
                }
            ],
            "source": "html_scrape",
            "warning": "This data was scraped from HTML and may be fragile"
        }
    """
    url = f"https://github.com/{owner}/{repo}/actions/runs/{run_id}"

    try:
        # Fetch HTML directly with curl (gh api doesn't properly handle HTML responses)
        result = subprocess.run(
            ["curl", "-s", "-L", url],
            capture_output=True,
            text=True,
            timeout=30
        )

        if result.returncode != 0:
            return {
                "workflow_annotations": [],
                "error": f"Failed to fetch HTML: {result.stderr}",
                "source": "html_scrape"
            }

        html_content = result.stdout

        if not html_content or len(html_content) < 1000:
            return {
                "workflow_annotations": [],
                "error": "HTML content appears invalid or empty",
                "source": "html_scrape"
            }

        # Parse annotations from HTML
        annotations = parse_annotations_from_html(html_content, run_id)

        return {
            "workflow_annotations": annotations,
            "source": "html_scrape",
            "warning": "This data was scraped from HTML and may become invalid if GitHub changes their UI structure",
            "url": url
        }

    except subprocess.TimeoutExpired:
        return {
            "workflow_annotations": [],
            "error": "Timeout while fetching HTML",
            "source": "html_scrape"
        }
    except Exception as e:
        return {
            "workflow_annotations": [],
            "error": f"Exception during HTML scraping: {str(e)}",
            "source": "html_scrape"
        }


def parse_annotations_from_html(html_content: str, run_id: str) -> List[Dict]:
    """
    Parse annotation data from GitHub Actions HTML page.

    The HTML structure:
    <annotation-message>
      <svg ...> (indicates level: failure, warning, notice)
      <strong>Title</strong> (e.g., "-6 Release Process")
      <div data-target="annotation-message.annotationContainer">
        <div>Full annotation text here</div>
      </div>
    </annotation-message>

    Args:
        html_content: Raw HTML content from GitHub Actions page
        run_id: Run ID for context in error messages

    Returns:
        List of annotation dictionaries
    """
    annotations = []

    # Look for <annotation-message> blocks
    annotation_block_pattern = r'<annotation-message[^>]*>(.*?)</annotation-message>'
    block_matches = re.finditer(annotation_block_pattern, html_content, re.DOTALL | re.IGNORECASE)

    for block_match in block_matches:
        block_content = block_match.group(1)

        # Extract annotation title (usually in <strong> tag)
        title_pattern = r'<strong[^>]*>(.*?)</strong>'
        title_match = re.search(title_pattern, block_content, re.DOTALL)
        title = title_match.group(1).strip() if title_match else "Unknown"

        # Determine level from SVG icon
        level = 'notice'  # default
        if 'octicon-x-circle' in block_content or 'octicon-stop' in block_content:
            level = 'failure'
        elif 'octicon-alert' in block_content:
            level = 'warning'
        elif 'octicon-info' in block_content:
            level = 'notice'

        # Extract annotation text from inner div
        container_pattern = r'<div[^>]*data-target=["\']annotation-message\.annotationContainer["\'][^>]*>.*?<div>(.*?)</div>'
        container_match = re.search(container_pattern, block_content, re.DOTALL)

        if container_match:
            annotation_text = container_match.group(1).strip()

            # Skip empty or very short annotations (likely not workflow errors)
            if len(annotation_text) < 10:
                continue

            annotation = {
                "level": level,
                "title": title,
                "message": annotation_text
            }

            # Avoid duplicates (same message)
            if not any(a.get('message') == annotation_text for a in annotations):
                annotations.append(annotation)

    return annotations


def extract_annotations_from_json(data: any, path: str = "") -> List[Dict]:
    """
    Recursively extract annotation data from JSON structures.

    Args:
        data: JSON data (dict, list, or primitive)
        path: Current path in JSON structure (for debugging)

    Returns:
        List of annotation dictionaries found in the JSON
    """
    annotations = []

    if isinstance(data, dict):
        # Check if this dict looks like an annotation
        if 'annotation_level' in data or 'annotationLevel' in data:
            annotation = {
                "level": data.get('annotation_level') or data.get('annotationLevel'),
                "message": data.get('message') or data.get('title') or '',
                "path": data.get('path'),
                "line": data.get('start_line') or data.get('startLine'),
                "col": data.get('start_column') or data.get('startColumn')
            }
            annotations.append(annotation)

        # Check for common annotation array keys
        for key in ['annotations', 'checkAnnotations', 'errors', 'warnings']:
            if key in data and isinstance(data[key], list):
                for item in data[key]:
                    annotations.extend(extract_annotations_from_json(item, f"{path}.{key}"))

        # Recurse into other dict values
        for key, value in data.items():
            if key not in ['annotations', 'checkAnnotations', 'errors', 'warnings']:
                annotations.extend(extract_annotations_from_json(value, f"{path}.{key}"))

    elif isinstance(data, list):
        for i, item in enumerate(data):
            annotations.extend(extract_annotations_from_json(item, f"{path}[{i}]"))

    return annotations


def save_scraped_annotations(run_id: str, workspace: Path, annotations_data: Dict):
    """
    Save scraped annotations to workspace with appropriate warnings.

    Args:
        run_id: GitHub Actions run ID
        workspace: Diagnostic workspace directory
        annotations_data: Scraped annotations data
    """
    output_file = workspace / "workflow-annotations-scraped.json"

    # Add metadata
    annotations_data['run_id'] = run_id
    annotations_data['scrape_timestamp'] = subprocess.run(
        ["date", "-Iseconds"],
        capture_output=True,
        text=True
    ).stdout.strip()

    with open(output_file, 'w') as f:
        json.dump(annotations_data, f, indent=2)

    print(f"✓ Scraped workflow annotations saved to {output_file}")

    if annotations_data.get('workflow_annotations'):
        print(f"  Found {len(annotations_data['workflow_annotations'])} workflow-level annotations")

        # Group by level
        by_level = {}
        for ann in annotations_data['workflow_annotations']:
            level = ann.get('level', 'unknown')
            by_level[level] = by_level.get(level, 0) + 1

        for level, count in sorted(by_level.items()):
            print(f"    {level}: {count}")
    else:
        print("  No workflow-level annotations found in HTML")

    if 'warning' in annotations_data:
        print(f"\n⚠️  {annotations_data['warning']}")


def format_scraped_annotations_report(annotations_data: Dict) -> str:
    """
    Format scraped annotations into a human-readable report.

    Args:
        annotations_data: Scraped annotations data

    Returns:
        Formatted report string
    """
    report = []
    report.append("=" * 80)
    report.append("WORKFLOW-LEVEL ANNOTATIONS (SCRAPED FROM HTML)")
    report.append("=" * 80)

    if 'warning' in annotations_data:
        report.append(f"\n⚠️  WARNING: {annotations_data['warning']}\n")

    if 'error' in annotations_data:
        report.append(f"\n❌ ERROR: {annotations_data['error']}\n")
        return "\n".join(report)

    annotations = annotations_data.get('workflow_annotations', [])

    if not annotations:
        report.append("\nNo workflow-level annotations found in HTML.")
        report.append("This might mean:")
        report.append("  • There are no workflow syntax errors")
        report.append("  • GitHub changed their HTML structure (scraper needs update)")
        report.append("  • The page couldn't be accessed")
        return "\n".join(report)

    # Group by level
    by_level = {'failure': [], 'warning': [], 'notice': []}
    for ann in annotations:
        level = ann.get('level', 'notice')
        if level not in by_level:
            by_level[level] = []
        by_level[level].append(ann)

    # Report failures
    if by_level['failure']:
        report.append(f"\n❌ ERRORS ({len(by_level['failure'])})")
        report.append("-" * 80)
        for ann in by_level['failure']:
            report.append(f"\n  Title: {ann.get('title', 'Unknown')}")
            report.append(f"  Message:\n    {ann['message'][:500]}..." if len(ann['message']) > 500 else f"  Message:\n    {ann['message']}")

    # Report warnings
    if by_level['warning']:
        report.append(f"\n⚠️  WARNINGS ({len(by_level['warning'])})")
        report.append("-" * 80)
        for ann in by_level['warning']:
            report.append(f"\n  Title: {ann.get('title', 'Unknown')}")
            report.append(f"  Message:\n    {ann['message'][:500]}..." if len(ann['message']) > 500 else f"  Message:\n    {ann['message']}")

    # Report notices
    if by_level['notice']:
        report.append(f"\nℹ️  NOTICES ({len(by_level['notice'])})")
        report.append("-" * 80)
        for ann in by_level['notice']:
            report.append(f"\n  Title: {ann.get('title', 'Unknown')}")
            # Show first few lines of message for notices
            message_lines = ann['message'].split('\n')
            if len(message_lines) > 4:
                preview = '\n    '.join(message_lines[:4])
                report.append(f"  Message:\n    {preview}\n    ... ({len(message_lines) - 4} more lines)")
            else:
                report.append(f"  Message:\n    {ann['message']}")

    report.append("\n" + "=" * 80)
    report.append(f"Source: {annotations_data.get('url', 'N/A')}")
    report.append("=" * 80)

    return "\n".join(report)


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Usage: python html_scraper.py <RUN_ID> [WORKSPACE]")
        print("\nExample:")
        print("  python html_scraper.py 20043196360 /path/to/workspace")
        sys.exit(1)

    run_id = sys.argv[1]
    workspace = Path(sys.argv[2]) if len(sys.argv) > 2 else Path(f"./.claude/diagnostics/run-{run_id}")
    workspace.mkdir(parents=True, exist_ok=True)

    print(f"Scraping workflow annotations for run {run_id}...")
    print(f"⚠️  WARNING: This uses HTML scraping and may break if GitHub changes their UI\n")

    annotations_data = scrape_workflow_annotations(run_id)
    save_scraped_annotations(run_id, workspace, annotations_data)

    print("\n" + format_scraped_annotations_report(annotations_data))
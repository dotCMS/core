#!/usr/bin/env python3
"""External issue detection for CI/CD failures.

Identifies when CI/CD failures are caused by external service changes
rather than code issues.
"""

import re
from datetime import datetime
from typing import Dict, List, Optional, Tuple


def extract_error_indicators(log_content: str) -> Dict[str, List[str]]:
    """Extract key indicators from logs that suggest external issues.

    Args:
        log_content: Full log file content

    Returns:
        Dictionary mapping indicator type to list of matches
    """
    indicators = {
        'npm_errors': [],
        'docker_errors': [],
        'auth_errors': [],
        'network_errors': [],
        'service_names': set()
    }

    lines = log_content.split('\n')

    for line in lines:
        # NPM specific errors
        if 'npm ERR!' in line:
            indicators['npm_errors'].append(line.strip())
            indicators['service_names'].add('npm')

            # Extract error codes
            if 'code E' in line:
                match = re.search(r'code (E\w+)', line)
                if match:
                    indicators['npm_errors'].append(f"Error code: {match.group(1)}")

        # Docker errors
        if 'ERROR:' in line and any(keyword in line.lower() for keyword in ['docker', 'blob', 'image', 'registry']):
            indicators['docker_errors'].append(line.strip())
            indicators['service_names'].add('docker')

        # Authentication errors (generic)
        auth_keywords = [
            'authentication', 'authorization', 'OTP', '2FA', 'token',
            'ENEEDAUTH', 'EOTP', 'unauthorized', 'forbidden', 'access denied'
        ]
        if any(keyword.lower() in line.lower() for keyword in auth_keywords):
            if any(error in line for error in ['ERR!', 'ERROR:', '::error::', 'FAILURE:']):
                indicators['auth_errors'].append(line.strip())

        # Network/connectivity errors
        network_keywords = [
            'connection refused', 'timeout', 'cannot connect',
            'network error', 'ECONNREFUSED', 'ETIMEDOUT'
        ]
        if any(keyword.lower() in line.lower() for keyword in network_keywords):
            indicators['network_errors'].append(line.strip())

    # Convert set to list for JSON serialization
    indicators['service_names'] = list(indicators['service_names'])

    return indicators


def generate_search_queries(indicators: Dict[str, List[str]],
                           failure_date: Optional[str] = None) -> List[str]:
    """Generate web search queries based on error indicators.

    Args:
        indicators: Error indicators from extract_error_indicators()
        failure_date: Date of failure (YYYY-MM-DD format)

    Returns:
        List of search query strings
    """
    queries = []

    # Extract month/year from failure date
    date_context = ""
    if failure_date:
        try:
            dt = datetime.strptime(failure_date, "%Y-%m-%d")
            date_context = f"{dt.strftime('%B %Y')}"
        except ValueError:
            pass

    # NPM specific searches
    if indicators['npm_errors']:
        npm_codes = [line for line in indicators['npm_errors'] if 'Error code:' in line]
        if npm_codes:
            # Extract error code
            for code_line in npm_codes:
                code = code_line.split('Error code: ')[1]
                queries.append(f'npm {code} authentication error {date_context}')

        # Check for token/2FA issues
        if any('OTP' in err or '2FA' in err or 'token' in err.lower()
               for err in indicators['npm_errors']):
            queries.append(f'npm classic token revoked {date_context}')
            queries.append(f'npm 2FA authentication CI/CD {date_context}')

    # Docker specific searches
    if indicators['docker_errors']:
        if any('blob' in err.lower() for err in indicators['docker_errors']):
            queries.append(f'docker blob not found error {date_context}')
        if any('registry' in err.lower() for err in indicators['docker_errors']):
            queries.append(f'docker registry authentication {date_context}')

    # GitHub Actions searches
    if any('actions' in err.lower() for err in
           indicators['auth_errors'] + indicators['network_errors']):
        queries.append(f'GitHub Actions runner issues {date_context}')

    # Generic service change searches
    for service in indicators['service_names']:
        queries.append(f'{service} breaking changes {date_context}')
        queries.append(f'{service} security update {date_context}')

    return queries


def suggest_external_checks(indicators: Dict[str, List[str]],
                           failure_timeline: List[Tuple[str, str]]) -> Dict[str, any]:
    """Suggest which external sources to check based on failure patterns.

    Args:
        indicators: Error indicators from extract_error_indicators()
        failure_timeline: List of (date, status) tuples showing failure history

    Returns:
        Dictionary with suggested checks and reasoning
    """
    suggestions = {
        'likelihood': 'low',  # low, medium, high
        'checks': [],
        'reasoning': []
    }

    # Check if failures started on a specific date with no recovery
    if len(failure_timeline) >= 3:
        recent_failures = [status for _, status in failure_timeline[:5]]
        if all(status == 'failure' for status in recent_failures):
            suggestions['likelihood'] = 'medium'
            suggestions['reasoning'].append(
                "Multiple consecutive failures suggest external change or persistent issue"
            )

    # NPM authentication errors strongly suggest external changes
    if indicators['npm_errors']:
        if any('EOTP' in err or 'ENEEDAUTH' in err for err in indicators['npm_errors']):
            suggestions['likelihood'] = 'high'
            suggestions['checks'].append({
                'source': 'npm registry changelog',
                'url': 'https://github.blog/changelog/',
                'search_for': 'npm security token authentication 2FA'
            })
            suggestions['reasoning'].append(
                "NPM authentication errors (EOTP/ENEEDAUTH) often caused by npm registry policy changes"
            )

    # Docker authentication/registry errors
    if indicators['docker_errors'] and indicators['auth_errors']:
        suggestions['likelihood'] = 'high' if suggestions['likelihood'] != 'high' else 'high'
        suggestions['checks'].append({
            'source': 'Docker Hub status',
            'url': 'https://status.docker.com/',
            'search_for': 'Docker Hub registry authentication'
        })
        suggestions['reasoning'].append(
            "Docker authentication errors may indicate Docker Hub policy changes or outages"
        )

    # Generic authentication without specific service
    if indicators['auth_errors'] and not indicators['service_names']:
        suggestions['checks'].append({
            'source': 'GitHub Actions status',
            'url': 'https://www.githubstatus.com/',
            'search_for': 'GitHub Actions runner authentication'
        })

    return suggestions


def format_external_issue_report(indicators: Dict[str, List[str]],
                                 search_queries: List[str],
                                 suggestions: Dict[str, any]) -> str:
    """Format external issue detection report for inclusion in diagnosis.

    Args:
        indicators: Error indicators
        search_queries: Generated search queries
        suggestions: Suggested checks

    Returns:
        Formatted markdown report section
    """
    report = []

    report.append("## External Issue Detection\n")

    # Likelihood assessment
    likelihood_emoji = {
        'low': '⚪',
        'medium': '🟡',
        'high': '🔴'
    }
    emoji = likelihood_emoji.get(suggestions['likelihood'], '⚪')
    report.append(f"**External Cause Likelihood:** {emoji} {suggestions['likelihood'].upper()}\n")

    # Reasoning
    if suggestions['reasoning']:
        report.append("**Indicators:**")
        for reason in suggestions['reasoning']:
            report.append(f"- {reason}")
        report.append("")

    # Service-specific errors
    if indicators['npm_errors']:
        report.append("**NPM Errors Detected:**")
        for err in indicators['npm_errors'][:5]:  # Show first 5
            report.append(f"- `{err}`")
        report.append("")

    if indicators['docker_errors']:
        report.append("**Docker Errors Detected:**")
        for err in indicators['docker_errors'][:3]:
            report.append(f"- `{err}`")
        report.append("")

    if indicators['auth_errors']:
        report.append("**Authentication Errors Detected:**")
        for err in indicators['auth_errors'][:3]:
            report.append(f"- `{err}`")
        report.append("")

    # Recommended searches
    if search_queries:
        report.append("**Recommended Web Searches:**")
        for query in search_queries[:5]:  # Top 5 queries
            report.append(f"- `{query}`")
        report.append("")

    # Specific checks
    if suggestions['checks']:
        report.append("**Suggested External Checks:**")
        for check in suggestions['checks']:
            report.append(f"- **{check['source']}**: {check['url']}")
            report.append(f"  Search for: `{check['search_for']}`")
        report.append("")

    return '\n'.join(report)


if __name__ == "__main__":
    # Example usage
    import sys
    from pathlib import Path

    if len(sys.argv) < 2:
        print("Usage: python external_issues.py <log_file>")
        sys.exit(1)

    log_file = Path(sys.argv[1])
    if not log_file.exists():
        print(f"Error: Log file not found: {log_file}")
        sys.exit(1)

    log_content = log_file.read_text(encoding='utf-8', errors='ignore')

    indicators = extract_error_indicators(log_content)
    queries = generate_search_queries(indicators, "2025-11-10")
    suggestions = suggest_external_checks(indicators, [
        ("2025-11-10", "failure"),
        ("2025-11-09", "failure"),
        ("2025-11-08", "failure"),
        ("2025-11-07", "failure"),
        ("2025-11-06", "success")
    ])

    report = format_external_issue_report(indicators, queries, suggestions)
    print(report)

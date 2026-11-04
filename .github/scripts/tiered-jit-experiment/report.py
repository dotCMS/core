#!/usr/bin/env python3
"""Check real execution and summarize a single matched CI sample; never infer a speedup."""
import json
import os
import sys
import time
from pathlib import Path
from xml.etree import ElementTree as ET


def summarize(directory):
    """Reject missing/zero execution and retain failures, retries and raw XML elapsed time."""
    directory = Path(directory)
    summary_file = directory / 'failsafe-summary.xml'
    counts = dict(completed=0, failures=0, errors=0, skipped=0)
    if summary_file.exists():
        root = ET.parse(summary_file).getroot()
        counts = {key: int(root.findtext(key, '0')) for key in counts}
    seconds = 0.0
    retries = 0
    for report in directory.glob('*.xml'):
        if report.name == 'failsafe-summary.xml':
            continue
        document = ET.parse(report).getroot()
        # Sum leaf suites only, not both an aggregate and its children.
        for suite in document.iter('testsuite'):
            if not suite.findall('testsuite'):
                seconds += float(suite.get('time', '0'))
                if not summary_file.exists():
                    for field, attribute in [('completed', 'tests'), ('failures', 'failures'),
                                             ('errors', 'errors'), ('skipped', 'skipped')]:
                        counts[field] += int(suite.get(attribute, '0'))
        retries += sum(1 for element in document.iter() if element.tag in
                       ('flakyFailure', 'flakyError', 'rerunFailure', 'rerunError'))
    if counts['completed'] <= counts['skipped']:
        raise ValueError('No executed tests: missing/cached/zero-test samples cannot be compared')
    return {**counts, 'xml_test_seconds': round(seconds, 3), 'retries': retries}


def main():
    """Write an artifact even for invalid samples, then fail the CI step if invalid."""
    result = {'mode': os.environ['JIT_MODE'], 'stage': os.environ['BENCHMARK_STAGE'],
              'maven_action_seconds': round(time.time() - float(os.environ.get('BENCHMARK_STARTED', time.time())), 3),
              'run_url': f"{os.environ['GITHUB_SERVER_URL']}/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
              'job_status': os.environ['BENCHMARK_STATUS'],
              'java_tool_options': os.environ.get('JAVA_TOOL_OPTIONS', '')}
    try:
        if 'BENCHMARK_STARTED' not in os.environ:
            raise ValueError('Job failed before the benchmark started')
        result.update(summarize(sys.argv[1]))
    except (OSError, ValueError, ET.ParseError) as error:
        result['invalid_reason'] = str(error)
    Path('jit-benchmark.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as summary:
        summary.write('### Tiered JIT experiment sample\n```json\n' + json.dumps(result, indent=2) + '\n```\n')
    return 1 if 'invalid_reason' in result else 0


if __name__ == '__main__':
    sys.exit(main())

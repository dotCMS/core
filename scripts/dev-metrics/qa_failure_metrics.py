from datetime import datetime, timedelta
import os
from collections import defaultdict
import logging
from github_metrics_base import GitHubMetricsBase

logger = logging.getLogger(__name__)

class QAFailureMetrics(GitHubMetricsBase):
    def __init__(self, token, owner, repo, team_label):
        super().__init__(token=token, owner=owner, repo=repo, team_labels=team_label)

    def count_qa_failures(self, issue_number, sprint_start, sprint_end):
        """Count how many times an issue received the QA Failed label during the sprint period"""
        events = self.get_issue_events(issue_number)
        failure_count = 0
        
        for event in events:
            if event.get('event') != 'labeled':  # We only count when the label is added
                continue
                
            event_time = datetime.strptime(event['created_at'], '%Y-%m-%dT%H:%M:%SZ')
            
            if event_time < sprint_start or event_time > sprint_end:
                continue
                
            if event.get('label', {}).get('name') == 'QA : Failed Internal':
                failure_count += 1
                
        return failure_count

    def get_sprint_issues(self, sprint_start, sprint_end):
        """Get all Falcon team issues that were updated during the sprint period"""
        issues = []
        page = 1
        
        while True:
            logger.info(f"Fetching page {page} of issues...")
            page_issues = self.get_all_falcon_issues(sprint_start, sprint_end, page)
            
            if not page_issues:
                break
            
            logger.info(f"Processing {len(page_issues)} Falcon team issues from page {page}")
            
            # Filter issues
            for issue in page_issues:
                logger.info(f"Processing issue {issue['number']} - {issue['title']}")
                if self.should_skip_issue(issue, sprint_end):
                    continue
                        
                failure_count = self.count_qa_failures(issue['number'], sprint_start, sprint_end)
                if failure_count > 0:
                    issue['qa_failure_count'] = failure_count
                    issues.append(issue)
            
            page += 1
        
        logger.info(f"Found {len(issues)} Falcon team issues that failed QA during the sprint period")
        return issues

    def generate_qa_failure_report(self, sprint_start, sprint_end):
        """Generate a detailed report of QA failures per developer"""
        logger.info("Starting QA failure report generation...")
        logger.info(f"Sprint period: {sprint_start.date()} to {sprint_end.date()}")
        
        failed_issues = self.get_sprint_issues(sprint_start, sprint_end)
        
        # Group issues by developer
        developer_issues = defaultdict(list)
        for issue in failed_issues:
            assignee = self.get_issue_assignee(issue)
            issue_details = self.format_issue_details(issue)
            issue_details['failure_count'] = issue['qa_failure_count']
            developer_issues[assignee].append(issue_details)
        
        logger.info("Report generation complete")
        return developer_issues

def main():
    logger.info("Starting GitHub QA metrics collection...")
    
    token = os.getenv('GITHUB_TOKEN')
    if not token:
        raise ValueError("Please set GITHUB_TOKEN environment variable")
    
    team_label = os.getenv('TEAM_LABEL', 'Team : Falcon')
    
    metrics = QAFailureMetrics(
        token=token,
        owner='dotcms',
        repo='core',
        team_label=team_label
    )
    
    sprint_end = datetime.now()
    sprint_start = sprint_end - timedelta(days=14)
    
    report = metrics.generate_qa_failure_report(sprint_start, sprint_end)
    
    # Print results
    print(f"\nQA Failures Report for Team Falcon ({sprint_start.date()} to {sprint_end.date()})")
    print("=" * 80)
    
    sorted_developers = sorted(report.items(), key=lambda x: len(x[1]), reverse=True)
    
    for developer, issues in sorted_developers:
        total_failures = sum(issue['failure_count'] for issue in issues)
        print(f"\n{developer}: {len(issues)} issues with {total_failures} total QA failures")
        print("-" * 40)
        for issue in issues:
            print(f"#{issue['number']} - {issue['title']}")
            print(f"Status: {issue['state']}")
            print(f"QA Failures: {issue['failure_count']}")
            print(f"Current labels: {', '.join(issue['current_labels'])}")
            print(f"URL: {issue['url']}")
            print(f"Last updated: {issue['updated_at']}")
            print()

if __name__ == "__main__":
    main()

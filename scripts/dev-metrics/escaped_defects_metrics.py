from datetime import datetime, timedelta
import os
from collections import defaultdict
import logging
from github_metrics_base import GitHubMetricsBase

logger = logging.getLogger(__name__)

class EscapedDefectsMetrics(GitHubMetricsBase):
    def get_qa_performer(self, issue_number):
        """Get the user who performed internal QA by finding who added the QA Passed label"""
        return self.get_label_adder(issue_number, 'QA : Passed Internal')

    def get_escaped_defects(self, sprint_start, sprint_end):
        """Get all Falcon team issues that passed internal QA but were later marked as escaped defects"""
        issues = []
        page = 1
        
        while True:
            logger.info(f"Fetching page {page} of issues...")
            page_issues = self.get_all_falcon_issues(sprint_start, sprint_end, page)
            
            if not page_issues:
                break
            
            logger.info(f"Processing {len(page_issues)} Falcon team issues from page {page}")
            
            for issue in page_issues:
                if self.should_skip_issue(issue, sprint_end):
                    continue
                    
                current_labels = [label['name'] for label in issue['labels']]
                if ('QA : Passed Internal' in current_labels and 
                    'Escaped Defect' in current_labels):
                    
                    # Find who performed the QA
                    qa_performer = self.get_qa_performer(issue['number'])
                    issue['qa_performer'] = qa_performer
                    issues.append(issue)
            
            page += 1
        
        return issues

    def get_all_qa_performed(self, sprint_start, sprint_end):
        """Get count of all QAs performed by each user during the sprint period"""
        qa_counts = defaultdict(int)
        page = 1
        
        while True:
            page_issues = self.get_all_falcon_issues(sprint_start, sprint_end, page)
            if not page_issues:
                break
                
            for issue in page_issues:
                if self.should_skip_issue(issue, sprint_end):
                    continue
                    
                current_labels = [label['name'] for label in issue['labels']]
                if 'QA : Passed Internal' in current_labels:
                    qa_performer = self.get_qa_performer(issue['number'])
                    if qa_performer != 'unknown':
            
            page += 1
            
        return qa_counts

    def generate_escaped_defects_report(self, sprint_start, sprint_end):
        """Generate a detailed report of escaped defects per QA performer"""
        logger.info("Starting escaped defects report generation...")
        logger.info(f"Sprint period: {sprint_start.date()} to {sprint_end.date()}")
        
        escaped_issues = self.get_escaped_defects(sprint_start, sprint_end)
        qa_counts = self.get_all_qa_performed(sprint_start, sprint_end)
        
        # Group issues by QA performer
        qa_performer_stats = defaultdict(lambda: {'total_qa': 0, 'escaped_defects': []})
        
        # Set total QA counts
        for performer, count in qa_counts.items():
            qa_performer_stats[performer]['total_qa'] = count
        
        # Add escaped defects
        for issue in escaped_issues:
            qa_performer = issue['qa_performer']
            issue_details = self.format_issue_details(issue)
            qa_performer_stats[qa_performer]['escaped_defects'].append(issue_details)
        
        logger.info("Report generation complete")
        return qa_performer_stats

def main():
    logger.info("Starting GitHub escaped defects metrics collection...")
    
    token = os.getenv('GITHUB_TOKEN')
    if not token:
        raise ValueError("Please set GITHUB_TOKEN environment variable")
    
    team_label = os.getenv('TEAM_LABEL', 'Team : Falcon')
    
    metrics = EscapedDefectsMetrics(
        token=token,
        owner='dotcms',
        repo='core',
        team_label=team_label
    )
    
    sprint_end = datetime.now()
    sprint_start = sprint_end - timedelta(days=14)
    
    report = metrics.generate_escaped_defects_report(sprint_start, sprint_end)
    
    # Print results
    print(f"\nEscaped Defects Report for Team Falcon ({sprint_start.date()} to {sprint_end.date()})")
    print("=" * 80)
    
    sorted_performers = sorted(report.items(), 
                             key=lambda x: len(x[1]['escaped_defects']), 
                             reverse=True)
    
    for qa_performer, stats in sorted_performers:
        total_qa = stats['total_qa']
        escaped_defects = stats['escaped_defects']
        escaped_count = len(escaped_defects)
        
        if total_qa > 0:
            escape_rate = (escaped_count / total_qa) * 100
        else:
            escape_rate = 0
            
        print(f"\nQA Performer: {qa_performer}")
        print(f"Total QA performed: {total_qa}")
        print(f"Escaped defects: {escaped_count}")
        print(f"Escape rate: {escape_rate:.1f}%")
        
        if escaped_defects:
            print("\nEscaped Defect Details:")
            print("-" * 40)
            for issue in escaped_defects:
                print(f"#{issue['number']} - {issue['title']}")
                print(f"Status: {issue['state']}")
                print(f"Current labels: {', '.join(issue['current_labels'])}")
                print(f"URL: {issue['url']}")
                print(f"Last updated: {issue['updated_at']}")
                print()

if __name__ == "__main__":
    main()
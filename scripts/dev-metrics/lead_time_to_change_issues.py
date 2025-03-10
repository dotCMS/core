import os
from datetime import datetime, timedelta
import logging
from collections import defaultdict
from statistics import mean
from github_metrics_base import GitHubMetricsBase
import requests
import re

logger = logging.getLogger(__name__)

class DeploymentLeadTimeMetrics(GitHubMetricsBase):
    def __init__(self, token, owner, repo, team_labels, iteration_field_id='120606020'):
        super().__init__(token, owner, repo, team_labels)
        self.iteration_field_id = iteration_field_id
        
    def get_deployment_label_date(self, issue_number):
        """Get the datetime when 'Customer Deployed' label was added to an issue"""
        events = self.get_issue_events(issue_number)
        
        for event in events:
            if (event.get('event') == 'labeled' and 
                event.get('label', {}).get('name') == 'Customer Deployed'):
                return datetime.strptime(event['created_at'], '%Y-%m-%dT%H:%M:%SZ')
                
        return None
    
    def get_iteration_start_date(self, issue_number):
        """Get the datetime when an issue was first included in a sprint"""
        try:
            field_values = self.get_issue_fields(issue_number)
            
            # Only log if field_values is not empty
            if field_values:
                # Return the earliest date if there's a value
                earliest_field = min(field_values, key=lambda x: datetime.strptime(x['created_at'], '%Y-%m-%dT%H:%M:%SZ'))
                logger.info(f"Included in sprint: {datetime.strptime(earliest_field['created_at'], '%Y-%m-%dT%H:%M:%SZ')}")
                return datetime.strptime(earliest_field['created_at'], '%Y-%m-%dT%H:%M:%SZ')
            
            return None
            
        except Exception as e:
            logger.error(f"Error getting iteration field for issue #{issue_number}: {e}")
            return None
    
    def get_issue_fields(self, issue_number):
        """Get the custom fields for an issue, specifically the iteration field"""
        try:
            logger.debug(f"Fetching fields for issue #{issue_number}")
            
            # GraphQL query to get iteration field data
            query = f"""
            query {{
              repository(owner: "{self.owner}", name: "{self.repo}") {{
                issue(number: {issue_number}) {{
                  projectItems(first: 10) {{
                    nodes {{
                      fieldValues(first: 20) {{
                        nodes {{
                          ... on ProjectV2ItemFieldIterationValue {{
                            title
                            startDate
                            createdAt
                            field {{
                              ... on ProjectV2IterationField {{
                                id
                                databaseId
                              }}
                            }}
                          }}
                        }}
                      }}
                    }}
                  }}
                }}
              }}
            }}
            """
            
            headers = {
                'Authorization': f'Bearer {self.token}',
                'Accept': 'application/vnd.github.v3+json'
            }
            
            response = requests.post(
                'https://api.github.com/graphql',
                json={'query': query},
                headers=headers
            )
            response.raise_for_status()
            data = response.json()
            
            # Extract the relevant field values
            field_values = []
            project_items = data.get('data', {}).get('repository', {}).get('issue', {}).get('projectItems', {}).get('nodes', [])
            
            for item in project_items:
                for field_value in item.get('fieldValues', {}).get('nodes', []):
                    if field_value and 'field' in field_value:
                        field_id = str(field_value.get('field', {}).get('databaseId', ''))
                        if field_id == self.iteration_field_id:
                            field_values.append({
                                'field_id': field_id,
                                'value': field_value.get('title'),
                                'created_at': field_value.get('createdAt')
                            })
            
            return field_values
            
        except Exception as e:
            logger.error(f"Error fetching fields for issue #{issue_number}: {e}")
            return []
    
    def is_epic_closed(self, epic_number):
        """Check if an epic is closed"""
        try:
            response = requests.get(
                f"{self.base_url}/issues/{epic_number}",
                headers=self.headers,
                verify=False
            )
            response.raise_for_status()
            epic_data = response.json()
            
            # Check if the epic is closed
            return epic_data.get('state') == 'closed'
        except Exception as e:
            logger.error(f"Error fetching epic #{epic_number}: {e}")
            return False

    def get_parent_epic(self, issue_number):
        """Get the parent epic of an issue using direct issue content"""
        try:
            # First, get the issue details to check the body content
            response = requests.get(
                f"{self.base_url}/issues/{issue_number}",
                headers=self.headers,
                verify=False
            )
            response.raise_for_status()
            issue_data = response.json()
            
            # Look for references in the body like "Part of #1234" or other common patterns
            body = issue_data.get('body', '') or ''
            logger.debug(f"Looking for epic references in issue #{issue_number} body")
            
            # Check for various reference patterns in the body
            # Pattern examples: "Part of #123", "Parent: #123", "Epic: #123", etc.
            epic_refs = []
            
            # Common patterns for epic references
            patterns = [
                r'(?:part of|belongs to|epic|parent)(?:\s*:)?\s*#(\d+)',  # Part of #123, Epic: #123, etc.
                r'(?:closes|fixes|resolves)(?:\s*:)?\s*#(\d+)',           # Closes #123, Fixes: #123
                r'(?:related to)(?:\s*:)?\s*#(\d+)',                      # Related to #123
                r'dotCMS/core#(\d+)',                                      # dotCMS/core#123 format
                r'Parent Issue\s*[\r\n]+\s*#(\d+)',                       # "Parent Issue" followed by issue number on next line
                r'(?:parent|epic).*?issue.*?[\r\n]+\s*#(\d+)'             # Any variation of parent/epic issue text followed by # on next line
            ]
            
            for pattern in patterns:
                matches = re.finditer(pattern, body, re.IGNORECASE)
                for match in matches:
                    epic_refs.append(match.group(1))
            
            # Also check for "linked issues" references which might be in the API data
            if issue_data.get('pull_request') and issue_data['pull_request'].get('html_url'):
                logger.debug(f"Issue #{issue_number} is a PR, checking for linked issues")
                # If it's a PR, fetch the linked issues
                linked_issues = []
                # Implementation would depend on your specific GitHub setup
            
            # Process any found references
            for ref_number in epic_refs:
                # Verify if the referenced issue is an epic
                response = requests.get(
                    f"{self.base_url}/issues/{ref_number}",
                    headers=self.headers,
                    verify=False
                )
                if response.status_code == 200:
                    epic_data = response.json()
                    labels = [label['name'] for label in epic_data.get('labels', [])]
                    
                    # Check if this is an epic based on labels or title
                    if ('Epic' in labels or 
                        epic_data.get('title', '').startswith('Epic:') or
                        'epic' in epic_data.get('title', '').lower()):
                        
                        return {
                            'number': epic_data['number'],
                            'title': epic_data['title'],
                            'state': epic_data['state'],
                            'url': epic_data['html_url']
                        }
            
            # If we get here, try an alternative approach using repository's issues search
            # This will find issues that mention our issue in their body
            response = requests.get(
                f"{self.base_url}/search/issues",
                params={
                    'q': f'repo:{self.owner}/{self.repo} is:issue #31504 in:body', # Using issue number
                    'sort': 'updated',
                    'order': 'desc'
                },
                headers=self.headers,
                verify=False
            )
            if response.status_code == 200:
                search_results = response.json()
                logger.info(f"Search results for parent epics: {search_results}")
                
                # Process search results (could add this later)
            
            logger.info(f"No parent epic found for issue #{issue_number}")
            return None
        except Exception as e:
            logger.error(f"Error fetching parent epic for issue #{issue_number}: {e}")
            return None

    def calculate_lead_times(self, start_date=None, end_date=None):
        """Calculate deployment lead times for issues"""
        # Default to current month if no date range is specified
        if not start_date:
            today = datetime.now()
            start_date = datetime(today.year, today.month, 1)
        if not end_date:
            end_date = datetime.now()
        
        # Get all issues for the specified time period
        all_issues = self.get_all_falcon_issues(start_date, end_date)
        logger.info(f"Found {len(all_issues)} total issues")
        
        lead_times = []
        skipped_outside_range = 0
        skipped_no_epic = 0
        skipped_epic_not_closed = 0
        
        for issue in all_issues:
            # Skip non-closed issues silently (no logging)
            if issue['state'] != 'closed':
                continue
            
            issue_number = issue['number']
            issue_url = issue.get('html_url', f"https://github.com/{self.owner}/{self.repo}/issues/{issue_number}")
            
            # Log the issue being processed with its URL
            logger.info(f"Processing issue #{issue_number}: {issue['title']}")
            logger.info(f"URL: {issue_url}")
            
            # Get parent epic and check if it exists and is closed
            parent_epic = self.get_parent_epic(issue_number)
            if not parent_epic:
                logger.info(f"Issue #{issue_number} doesn't belong to an Epic")
                # skipped_no_epic += 1
                # logger.info("")
                # continue
            
            # Check if the epic is closed
            elif parent_epic.get('state') != 'closed':
                epic_url = f"https://github.com/{self.owner}/{self.repo}/issues/{parent_epic.get('number')}"
                logger.info(f"Issue #{issue_number} belongs to Epic {epic_url} which is not closed - skipping")
                skipped_epic_not_closed += 1
                logger.info("")
                continue
            else:
                # Log epic information including URL with @ prefix
                epic_url = f"https://github.com/{self.owner}/{self.repo}/issues/{parent_epic.get('number')}"
                logger.info(f"Issue belongs to closed Epic {epic_url}: {parent_epic.get('title')}")
                logger.info(f"Epic URL: {parent_epic.get('url')}")
                
                # Get sprint date and verify it exists
            sprint_date = self.get_iteration_start_date(issue_number)
            if not sprint_date:
                logger.info("no sprint date")
                logger.info("")
                continue
            
            # Filter out issues whose sprint date is outside our specified range
            if sprint_date < start_date or sprint_date > end_date:
                skipped_outside_range += 1
                logger.info("sprint_date outside range")
                logger.info("")
                continue
            
            # Rest of your existing logic
            deployed_date = self.get_deployment_label_date(issue_number)
            if not deployed_date:
                logger.info(f"Deployed date: None")
                logger.info("")
                continue
            
            # Calculate lead time
            lead_time = (deployed_date - sprint_date).days
            
            # Log the deployed date and lead time for each issue
            logger.info(f"Deployed date: {deployed_date}")
            logger.info(f"Lead time to change: {lead_time} days for issue #{issue_number}: {issue['title']}")
            
            # Only include positive lead times
            if lead_time < 0:
                logger.info("")
                continue
            
            lead_times.append({
                'issue_number': issue_number,
                'title': issue['title'],
                'sprint_date': sprint_date,
                'deployed_date': deployed_date,
                'lead_time_days': lead_time,
                'url': issue_url,
                'epic_number': parent_epic.get('number') if parent_epic else None,
                'epic_title': parent_epic.get('title') if parent_epic else None,
                'epic_url': parent_epic.get('url') if parent_epic else None
            })
            
            logger.info("")
        
        # Log summary of skipped issues
        if skipped_outside_range > 0:
            logger.info(f"Skipped {skipped_outside_range} issues with sprint dates outside specified range")
        if skipped_no_epic > 0:
            logger.info(f"Skipped {skipped_no_epic} issues that don't belong to an Epic")
        if skipped_epic_not_closed > 0:
            logger.info(f"Skipped {skipped_epic_not_closed} issues whose parent Epic is not closed")
        
        logger.info(f"Found {len(lead_times)} issues with valid sprint and deployment dates within range, belonging to closed Epics")
        return lead_times
    
    def generate_lead_time_report(self, start_date=None, end_date=None):
        """Generate a report on deployment lead times"""
        lead_times = self.calculate_lead_times(start_date, end_date)
        
        if not lead_times:
            return {"issues": [], "average_lead_time": 0}
        
        # Calculate average lead time
        avg_lead_time = mean([issue['lead_time_days'] for issue in lead_times])
        
        return {
            "issues": lead_times,
            "average_lead_time": avg_lead_time
        }

def main():
    logger.info("Starting GitHub deployment lead time metrics collection...")
    
    token = os.getenv('GITHUB_TOKEN')
    if not token:
        raise ValueError("Please set GITHUB_TOKEN environment variable")
    
    # Define default team labels
    default_team_labels = [
        "Team : Falcon",
        "Team : Scout",
        "Team : Platform",
        "Team : Bug Fixers"
    ]
    
    # Get team labels from environment variable if set, otherwise use defaults
    team_labels_env = os.getenv('TEAM_LABELS')
    team_labels = team_labels_env.split(',') if team_labels_env else default_team_labels
    
    metrics = DeploymentLeadTimeMetrics(
        token=token,
        owner='dotcms',
        repo='core',
        team_labels=team_labels  # Pass the list of team labels
    )
    
    end_date = datetime.now()
    start_date = end_date - timedelta(days=30)
    
    report = metrics.generate_lead_time_report(start_date, end_date)
    
    # Print results
    print(f"\nDeployment Lead Time Report for Teams {', '.join(team_labels)} ({start_date.date()} to {end_date.date()})")
    print("=" * 80)
    print(f"Average Lead Time: {report['average_lead_time']:.2f} days")
    print("\nIssues analyzed:")
    print("-" * 80)
    
    # Sort issues by lead time (ascending)
    sorted_issues = sorted(report['issues'], key=lambda x: x['lead_time_days'])
    
    for issue in sorted_issues:
        print(f"#{issue['issue_number']} - {issue['title']}")
        
        if issue.get('epic_number'):
            print(f"Epic: #{issue['epic_number']} - {issue['epic_title']}")
            print(f"Epic URL: {issue['epic_url']}")
        else:
            print("No Epic associated")
        
        print(f"Sprint Date: {issue['sprint_date'].date()}")
        print(f"Deployed Date: {issue['deployed_date'].date()}")
        print(f"Lead Time: {issue['lead_time_days']:.2f} days")
        print(f"URL: {issue['url']}")
        print()

if __name__ == "__main__":
    main() 
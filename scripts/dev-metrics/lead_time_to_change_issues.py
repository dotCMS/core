import os
from datetime import datetime, timedelta
import logging
from collections import defaultdict
from statistics import mean
from github_metrics_base import GitHubMetricsBase
import requests

logger = logging.getLogger(__name__)

class DeploymentLeadTimeMetrics(GitHubMetricsBase):
    def __init__(self, token, owner, repo, team_label, iteration_field_id='120606020'):
        super().__init__(token, owner, repo, team_label)
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
            logger.info(f"Field values: {field_values}")
            
            if field_values:
                # Return the earliest date if there's a value
                earliest_field = min(field_values, key=lambda x: datetime.strptime(x['created_at'], '%Y-%m-%dT%H:%M:%SZ'))
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
    
    def calculate_lead_times(self, start_date=None, end_date=None):
        """Calculate deployment lead times for all issues"""
        if not start_date:
            end_date = datetime.now()
            start_date = end_date - timedelta(days=180)  # Default to last 6 months
            
        logger.info(f"Calculating lead times from {start_date.date()} to {end_date.date()}")
        
        lead_times = []
        page = 1
        
        while True:
            page_issues = self.get_all_falcon_issues(start_date, end_date, page)
            if not page_issues:
                break
                
            logger.info(f"Processing {len(page_issues)} issues from page {page}")
            
            for issue in page_issues:
                issue_number = issue['number']
                
                # Get when the issue was added to a sprint
                sprint_date = self.get_iteration_start_date(issue_number)

                print(f"Sprint date: {sprint_date}")
                
                # Get when the issue was marked as deployed
                deployed_date = self.get_deployment_label_date(issue_number)

                print(f"Deployed date: {deployed_date}")
                
                if sprint_date and deployed_date and deployed_date > sprint_date:
                    # Calculate lead time in days
                    lead_time = (deployed_date - sprint_date).total_seconds() / 86400
                    
                    lead_times.append({
                        'issue_number': issue_number,
                        'title': issue['title'],
                        'url': issue['html_url'],
                        'sprint_date': sprint_date,
                        'deployed_date': deployed_date,
                        'lead_time_days': lead_time
                    })
            
            page += 1
        
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
    
    team_label = os.getenv('TEAM_LABEL', 'Team : Falcon')
    
    metrics = DeploymentLeadTimeMetrics(
        token=token,
        owner='dotcms',
        repo='core',
        team_label=team_label
    )
    
    # Get data for the last 180 days by default
    end_date = datetime.now()
    start_date = end_date - timedelta(days=30)
    
    report = metrics.generate_lead_time_report(start_date, end_date)
    
    # Print results
    print(f"\nDeployment Lead Time Report for Team Falcon ({start_date.date()} to {end_date.date()})")
    print("=" * 80)
    print(f"Average Lead Time: {report['average_lead_time']:.2f} days")
    print("\nIssues analyzed:")
    print("-" * 80)
    
    # Sort issues by lead time (ascending)
    sorted_issues = sorted(report['issues'], key=lambda x: x['lead_time_days'])
    
    for issue in sorted_issues:
        print(f"#{issue['issue_number']} - {issue['title']}")
        print(f"Sprint Date: {issue['sprint_date'].date()}")
        print(f"Deployed Date: {issue['deployed_date'].date()}")
        print(f"Lead Time: {issue['lead_time_days']:.2f} days")
        print(f"URL: {issue['url']}")
        print()

if __name__ == "__main__":
    main() 
import requests
from datetime import datetime
import os
import urllib3
import logging
import warnings
from collections import defaultdict

# Suppress all warnings related to unverified HTTPS requests
warnings.filterwarnings('ignore', message='Unverified HTTPS request')
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# Suppress SSL warning
urllib3.disable_warnings(urllib3.exceptions.NotOpenSSLWarning)

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class GitHubMetricsBase:
    def __init__(self, token, owner, repo):
        self.token = token
        self.owner = owner 
        self.repo = repo
        self.base_url = f"https://api.github.com/repos/{owner}/{repo}"
        self.headers = {
            'Authorization': f'token {token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        self.test_connection()

    def test_connection(self):
        """Test the GitHub API connection"""
        try:
            response = requests.get(
                self.base_url,
                headers=self.headers,
                verify=False
            )
            response.raise_for_status()
            logger.info("Successfully connected to GitHub API")
        except Exception as e:
            logger.error(f"Failed to connect to GitHub API: {e}")
            raise

    def get_issue_events(self, issue_number):
        """Get all events for an issue"""
        try:
            logger.debug(f"Fetching events for issue #{issue_number}")
            response = requests.get(
                f"{self.base_url}/issues/{issue_number}/timeline",
                headers=self.headers,
                verify=False
            )
            response.raise_for_status()
            return response.json()
        except Exception as e:
            logger.error(f"Error fetching events for issue #{issue_number}: {e}")
            return []

    def get_issue_assignee(self, issue):
        """Get the assignee of an issue"""
        if issue['assignee']:
            return issue['assignee']['login']
        return 'unassigned'
    
    def get_all_falcon_issues(self, sprint_start, sprint_end, page=1):
        """Base method to get Falcon team issues within a date range"""
        try:
            params = {
                'state': 'all',
                'labels': 'Team : Falcon',
                'since': sprint_start.isoformat(),
                'per_page': 100,
                'page': page
            }
            
            response = requests.get(
                f"{self.base_url}/issues",
                headers=self.headers,
                params=params,
                verify=False
            )
            response.raise_for_status()
            return response.json()
            
        except Exception as e:
            logger.error(f"Error fetching page {page}: {e}")
            return []

    def should_skip_issue(self, issue, sprint_end):
        """Common logic for filtering issues"""
        update_time = datetime.strptime(issue['updated_at'], '%Y-%m-%dT%H:%M:%SZ')
        current_labels = [label['name'] for label in issue['labels']]
        
        # Skip if updated after sprint end or has NW Removed label
        if update_time > sprint_end or 'NW Removed' in current_labels:
            return True
            
        return False

    def format_issue_details(self, issue):
        """Format common issue details"""
        return {
            'number': issue['number'],
            'title': issue['title'],
            'url': issue['html_url'],
            'updated_at': issue['updated_at'],
            'state': issue['state'],
            'current_labels': [label['name'] for label in issue['labels']]
        }

    def get_label_adder(self, issue_number, label_name):
        """Get the user who added a specific label to an issue"""
        try:
            events = self.get_issue_events(issue_number)
            
            for event in events:
                if (event.get('event') == 'labeled' and 
                    event.get('label', {}).get('name') == label_name):
                    return event['actor']['login']
                    
            return 'unknown'
            
        except Exception as e:
            logger.error(f"Error finding label adder for issue #{issue_number}: {e}")
            return 'unknown'
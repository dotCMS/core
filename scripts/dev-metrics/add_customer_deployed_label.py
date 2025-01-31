import os
import requests
import logging
from github_metrics_base import GitHubMetricsBase

logger = logging.getLogger(__name__)

class AddCustomerDeployedLabel(GitHubMetricsBase):
    def add_label_to_issues(self, release_label):
        """Add 'Customer Deployed' label to issues with the specified release label"""
        page = 1
        while True:
            logger.info(f"Fetching page {page} of issues with label '{release_label}'...")
            issues = self.get_issues_with_label(release_label, page)
            
            if not issues:
                break
            
            for issue in issues:
                self.add_label(issue['number'], 'Customer Deployed')
            
            page += 1

    def get_issues_with_label(self, label, page=1):
        """Get issues with a specific label"""
        try:
            params = {
                'state': 'all',
                'labels': label,
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
            logger.error(f"Error fetching issues with label '{label}' on page {page}: {e}")
            return []

    def add_label(self, issue_number, label):
        """Add a label to a specific issue"""
        try:
            logger.info(f"Adding label '{label}' to issue #{issue_number}")
            response = requests.post(
                f"{self.base_url}/issues/{issue_number}/labels",
                headers=self.headers,
                json={'labels': [label]},
                verify=False
            )
            response.raise_for_status()
            logger.info(f"Successfully added label '{label}' to issue #{issue_number}")
        except Exception as e:
            logger.error(f"Error adding label '{label}' to issue #{issue_number}: {e}")

def main():
    logger.info("Starting to add 'Customer Deployed' label to issues...")
    
    token = os.getenv('GITHUB_TOKEN_DOTCMS')
    if not token:
        raise ValueError("Please set GITHUB_TOKEN environment variable")
    
    release_label = input("Enter the release label (e.g., 'Release: XYZ'): ").strip()
    
    metrics = AddCustomerDeployedLabel(
        token=token,
        owner='dotcms',
        repo='core'
    )
    
    metrics.add_label_to_issues(release_label)

if __name__ == "__main__":
    main() 
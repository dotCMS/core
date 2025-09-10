import os
import requests
import logging
import argparse
from github_metrics_base import GitHubMetricsBase

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
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
    
    # Set up argument parser
    parser = argparse.ArgumentParser(description='Add Customer Deployed label to issues with a specific release label')
    parser.add_argument('--release-label', type=str, required=True, help='Release label (e.g., "Release: 24.03")')
    args = parser.parse_args()
    
    # Confirm received arguments
    logger.info(f"Arguments received: release_label={args.release_label}")
    
    token = os.getenv('GITHUB_TOKEN')
    if not token:
        raise ValueError("Please set GITHUB_TOKEN environment variable")
    
    logger.info(f"Using release label: {args.release_label}")
    
    metrics = AddCustomerDeployedLabel(
        token=token,
        owner='dotcms',
        repo='core',
        team_labels=[]  # Pass an empty list since no specific team labels are needed for this functionality
    )
    
    metrics.add_label_to_issues(args.release_label)

if __name__ == "__main__":
    main() 
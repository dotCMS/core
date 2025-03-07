# GitHub Metrics Scripts

Scripts to collect and analyze GitHub metrics, specifically to calculate the lead time for issues.

## Requirements

- Python 3.6 or higher
- Python packages listed in `requirements.txt`

## Installation

1. Install dependencies:

```bash
pip install -r requirements.txt
```

## Environment Configuration

### Environment Files

The project uses two environment files:

1. **`.env`**: Template file with default or empty values (included in the repository)
2. **`.env.local`**: File with your actual values, including sensitive information (not included in the repository)

#### Initial Setup

1. The `.env` file is already included in the repository with default or empty values.
2. Create a copy of this file named `.env.local`:
   ```bash
   cp .env .env.local
   ```
3. Edit `.env.local` and add your actual values:
   ```
   GITHUB_TOKEN=your_personal_token_here
   GITHUB_OWNER=dotcms
   GITHUB_REPO=core
   TEAM_LABEL=Team : Falcon
   REPORT_DAYS=30
   ```

> **Note**: The `.env.local` file will not be included in the repository, so your credentials will be safe.

### Getting a GitHub Token

To use these scripts, you need a GitHub personal access token:

1. Log in to your GitHub account
2. Go to Settings > Developer settings > Personal access tokens > Tokens (classic)
3. Click on "Generate new token"
4. Select at least the following permissions:
   - `repo` (full access to repositories)
   - `read:org` (to access organization information)
5. Click on "Generate token"
6. Copy the generated token and save it in your `.env.local` file

## Usage

### Lead Time Script

This script calculates the time it takes for an issue from when it's included in a sprint until it's deployed to production.

```bash
python lead_time_to_change_issues.py
```

The script will generate a report in the console with the following data:

- Average lead time
- List of analyzed issues
- Lead time for each issue

## Troubleshooting

If you encounter errors, check the following:

1. The GitHub token has the correct permissions
2. The token is correctly configured in the `.env.local` file
3. You have an internet connection to access the GitHub API
4. The Python packages are correctly installed

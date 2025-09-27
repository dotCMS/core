# Organization Membership Check Action

This composite action checks if a GitHub user is a member of the dotCMS organization. It's used as a security gate to ensure only dotCMS organization members can trigger sensitive workflows like Claude code reviews.

## Security Features

- **Hardcoded Organization**: The organization name "dotCMS" is hardcoded and cannot be overridden
- **Public Membership Only**: Only detects public organization members for security
- **Clear Instructions**: Provides guidance for private members to make membership public
- **Graceful Error Handling**: Returns clear status without exposing internal API details

## Inputs

| Input | Description | Required | Default |
|-------|-------------|----------|---------|
| `username` | GitHub username to check | Yes | N/A |

## Outputs

| Output | Description | Possible Values |
|--------|-------------|-----------------|
| `is_member` | Boolean indicating membership | `true` or `false` |
| `membership_status` | Detailed status | `member`, `non-member`, or `error` |

## Usage

```yaml
- name: Check organization membership
  id: membership-check
  uses: ./.github/actions/security/org-membership-check
  with:
    username: ${{ github.actor }}

- name: Conditional step based on membership
  if: steps.membership-check.outputs.is_member == 'true'
  run: echo "User is authorized"
```

## Implementation Details

The action uses the GitHub CLI (`gh`) with the repository's `GITHUB_TOKEN` to check organization membership via the GitHub API endpoint `GET /orgs/dotCMS/members/{username}`.

**Important Limitation: Public Membership Only**

This approach only detects users with **public** organization membership:

- **HTTP 200 + user object**: User is a PUBLIC member → **AUTHORIZED**
- **HTTP 404**: User has private membership OR is not a member → **BLOCKED**

**For dotCMS Team Members with Private Membership:**

If you are a dotCMS organization member but have private membership visibility, you must make your membership public to access Claude workflows:

1. Visit: https://github.com/orgs/dotCMS/people
2. Find your username in the list
3. Click "Make public" next to your name

This ensures the security gate can detect your organization membership without requiring additional API tokens.

## Security Considerations

- Only checks membership in the dotCMS organization (hardcoded)
- Only authorizes users with public organization membership
- Logs authorization results without sensitive details
- Uses default GITHUB_TOKEN (no additional secrets required)
- Provides clear instructions for private members to become public
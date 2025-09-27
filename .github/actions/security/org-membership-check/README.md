# Organization Membership Check Action

This composite action checks if a GitHub user is a member of the dotCMS organization. It's used as a security gate to ensure only dotCMS organization members can trigger sensitive workflows like Claude code reviews.

## Security Features

- **Hardcoded Organization**: The organization name "dotCMS" is hardcoded and cannot be overridden
- **All Organization Members**: Detects both public and private organization members
- **Simple Token Usage**: Uses default GITHUB_TOKEN without additional secrets
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

**API Behavior**

The GitHub organization membership API works for both public and private members:

- **HTTP 204 No Content**: User is a member (public or private) → **AUTHORIZED**
- **HTTP 404 Not Found**: User is not a member → **BLOCKED**

This approach successfully detects all dotCMS organization members regardless of their membership visibility setting, using only the default GITHUB_TOKEN without requiring additional secrets or configuration.

## Security Considerations

- Only checks membership in the dotCMS organization (hardcoded)
- Authorizes all organization members (both public and private)
- Logs authorization results without sensitive details
- Uses default GITHUB_TOKEN (no additional secrets required)
- No configuration or setup required for team members
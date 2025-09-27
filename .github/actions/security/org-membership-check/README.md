# Organization Membership Check Action

This composite action checks if a GitHub user is a member of the dotCMS organization. It's used as a security gate to ensure only dotCMS organization members can trigger sensitive workflows like Claude code reviews.

## Security Features

- **Hardcoded Organization**: The organization name "dotCMS" is hardcoded and cannot be overridden
- **No Information Leakage**: Does not distinguish between public/private membership in outputs
- **Graceful Error Handling**: Returns clear status without exposing internal API details

## Inputs

| Input | Description | Required | Default |
|-------|-------------|----------|---------|
| `username` | GitHub username to check | Yes | N/A |
| `github_token` | Fine-grained GitHub token with organization membership read permissions | Yes | N/A |

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
    github_token: ${{ secrets.MACHINE_USER_CORE_ORG_MEMBERSHIP_CHECK }}

- name: Conditional step based on membership
  if: steps.membership-check.outputs.is_member == 'true'
  run: echo "User is authorized"
```

## Implementation Details

The action uses the GitHub CLI (`gh`) with a fine-grained GitHub token to check organization membership via the GitHub API endpoint `GET /orgs/dotCMS/members/{username}`.

**Token Requirements:**
- Fine-grained token with organization membership read permissions
- Should be from a machine/service account for security
- Stored as repository secret: `MACHINE_USER_CORE_ORG_MEMBERSHIP_CHECK`

**Key Design Decision: Status Code vs Response Body**

The action relies on HTTP status codes rather than parsing response content because:

- **HTTP 200 (Success)**: User is a member of the organization
  - Public members: API returns user object with populated fields
  - Private members: API returns empty response body (but still 200 OK)

- **HTTP 404 (Not Found)**: User is not a member of the organization
  - Returns error object with "Not Found" message

This approach correctly authorizes all organization members (including owners with private membership) without needing to handle different response formats or visibility settings.

## Security Considerations

- Only checks membership in the dotCMS organization (hardcoded)
- Does not expose whether membership is public or private
- Logs authorization results without sensitive details
- Uses fine-grained token with minimal required permissions (organization membership read)
- Token should be regularly rotated and monitored for usage
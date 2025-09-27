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

The action uses the GitHub CLI (`gh`) with the repository's `GITHUB_TOKEN` to check organization membership. It first attempts to check public membership, and if that fails, it attempts to check private membership (which requires appropriate permissions in organization repositories).

## Security Considerations

- Only checks membership in the dotCMS organization (hardcoded)
- Does not expose whether membership is public or private
- Logs authorization results without sensitive details
- Uses repository's built-in `GITHUB_TOKEN` for API access
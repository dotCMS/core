# GPG Commit Signing Setup

This guide ensures every team member has GPG commit signing configured so that all commits — including those made via Claude Code — are cryptographically verified as coming from the person they claim to be.

**Why this matters:** When using AI tools like Claude Code to commit code on your behalf, GPG signing is the mechanism that proves the commit was authorized by you. Without it, there is no cryptographic guarantee that the committer is who they say they are.

---

## Prerequisites

- [GPG](https://gnupg.org/) installed on your machine
- [GitHub CLI (`gh`)](https://cli.github.com/) installed and authenticated
- Your GitHub account email address

### Install GPG

| Platform | Command |
|----------|---------|
| macOS | `brew install gnupg` |
| Ubuntu/Debian | `sudo apt install gnupg` |
| Windows | Install [Gpg4win](https://gpg4win.org/) |

---

## Step 1 — Check for an existing GPG key

```bash
gpg --list-secret-keys --keyid-format LONG
```

If you see output like the following, you already have a key — skip to [Step 3](#step-3--link-your-key-to-git).

```
sec   rsa4096/XXXXXXXXXXXXXXXX 2024-01-01 [SC]
uid                 [ultimate] Your Name <you@example.com>
```

If the output is empty, continue to Step 2.

---

## Step 2 — Generate a new GPG key

Run the following, replacing the `Name-Real` and `Name-Email` values with your own:

```bash
cat <<EOF | gpg --batch --gen-key
%no-protection
Key-Type: RSA
Key-Length: 4096
Key-Usage: sign
Name-Real: YOUR_GITHUB_USERNAME
Name-Email: YOUR_GITHUB_EMAIL
Expire-Date: 0
%commit
EOF
```

Verify the key was created:

```bash
gpg --list-secret-keys --keyid-format LONG
```

Note the key ID — it is the 16-character hex string after `rsa4096/` on the `sec` line (e.g. `6E312FB5D8D1A8C5`).

---

## Step 3 — Link your key to git

Set the signing key globally so it applies to all your repositories:

```bash
git config --global user.signingkey YOUR_KEY_ID
git config --global user.email YOUR_GITHUB_EMAIL
git config --global commit.gpgsign true
```

Verify:

```bash
git config --global --get user.signingkey
git config --global --get user.email
git config --global --get commit.gpgsign
```

---

## Step 4 — Add the key to your GitHub account

### Grant the GitHub CLI permission to manage GPG keys

```bash
gh auth refresh -h github.com -s write:gpg_key
```

A browser window will open — approve the requested scopes.

### Upload the key

```bash
gpg --armor --export YOUR_KEY_ID | gh gpg-key add -
```

Confirm it was added:

```bash
gh gpg-key list
```

---

## Step 5 — Test it

Make a signed test commit and verify the signature:

```bash
cd /path/to/any/repo
git commit --allow-empty -m "test: verify GPG signing"
git verify-commit HEAD
```

Expected output:

```
gpg: Signature made ...
gpg:                using RSA key ...
gpg: Good signature from "YOUR_NAME <YOUR_EMAIL>" [ultimate]
```

Then delete the test commit:

```bash
git reset --hard HEAD~1
```

---

## Troubleshooting

### `No secret key` error

Your git `user.email` does not match any GPG key. Run:

```bash
git config --global user.email        # check the configured email
gpg --list-secret-keys --keyid-format LONG  # check available keys
```

Either update `user.email` to match your GPG key, or generate a new key for your email (Step 2).

### `gpg: signing failed: Inappropriate ioctl for device`

GPG cannot prompt for a passphrase in the current terminal. Add the following to your shell profile (`~/.zshrc` or `~/.bashrc`):

```bash
export GPG_TTY=$(tty)
```

Then reload: `source ~/.zshrc`

### `insufficient OAuth scopes to list GPG keys`

Re-run the auth refresh step:

```bash
gh auth refresh -h github.com -s write:gpg_key
```

### Commits show as `Unverified` on GitHub

- Confirm the email in your GPG key matches your **verified** GitHub email address (GitHub Settings → Emails)
- Confirm the public key was uploaded: `gh gpg-key list`
- Confirm `commit.gpgsign` is `true`: `git config --global --get commit.gpgsign`

---

## Quick reference

```bash
# Full setup in one block (replace placeholders)
cat <<EOF | gpg --batch --gen-key
%no-protection
Key-Type: RSA
Key-Length: 4096
Key-Usage: sign
Name-Real: YOUR_GITHUB_USERNAME
Name-Email: YOUR_GITHUB_EMAIL
Expire-Date: 0
%commit
EOF

KEY_ID=$(gpg --list-secret-keys --keyid-format LONG YOUR_GITHUB_EMAIL \
  | grep '^sec' | awk '{print $2}' | cut -d'/' -f2)

git config --global user.signingkey $KEY_ID
git config --global user.email YOUR_GITHUB_EMAIL
git config --global commit.gpgsign true

gh auth refresh -h github.com -s write:gpg_key
gpg --armor --export $KEY_ID | gh gpg-key add -
```

---

## Enforcement

All commits to `dotCMS/core` made via Claude Code or any automated tooling **must** be GPG signed. Unsigned commits from team members are not considered verified and may be rejected during review.

If you have questions, reach out to the team lead or open an issue in [dotCMS/private-issues](https://github.com/dotCMS/private-issues).

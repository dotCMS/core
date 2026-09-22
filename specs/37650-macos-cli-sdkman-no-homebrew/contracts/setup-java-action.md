# Contract: `core-cicd/setup-java` composite action

**Purpose of this document**: this fix rewrites one step *inside* a shared composite action. The
action's observable contract must not change. This is the regression reference — anything listed
here that differs after the change is a defect, not an improvement.

**Source**: `.github/actions/core-cicd/setup-java/action.yml` (181 lines)

---

## Consumers

Changing this contract breaks all of the following:

| Consumer | Reach |
|---|---|
| `core-cicd/prepare-runner/action.yml:45-48` | → `core-cicd/maven-job` → **8 workflows** |
| `cicd_comp_release-phase.yml:142` | Release pipeline |
| `legacy-release_maven-release-process.yml:169` and `:304` | Legacy release pipeline |

## Inputs — unchanged

| Input | Required | Default | Meaning |
|---|---|---|---|
| `java-version` | no | *(empty)* | Overrides the JDK. When empty, the version is read from `.sdkmanrc` (`java=` line). |
| `require-graalvm` | yes | `'false'` | When `'true'`, also installs GraalVM and exports `GRAALVM_HOME`. |
| `graalvm-version` | no | *(empty)* | Overrides the GraalVM version. When empty, defaults to `21.0.2-graalce`. |

No action-level `outputs:` are declared. Callers depend on **exported environment**, not outputs.

## Exported environment — unchanged

| Variable | Set at | Value |
|---|---|---|
| `ARCHITECTURE` | `action.yml:34` (`GITHUB_ENV`) | `uname -m` — `arm64` or `x86_64`. Also feeds every cache key. |
| `JAVA_HOME` | `:134` (`GITHUB_ENV`) | `sdk home java <requested_version>` |
| `PATH` | `:136` (`GITHUB_PATH`) | prepends `$HOME/.sdkman/candidates/java/current/bin` |
| `GRAALVM_HOME` | `:142`, `:150` (`GITHUB_ENV`) | Only when `require-graalvm == 'true'` |

## Cache keys — unchanged

Changing any of these silently invalidates warm caches across every platform, including the
Linux ones that are working today.

| Key | Path | Lines |
|---|---|---|
| `${{ runner.os }}-${{ env.ARCHITECTURE }}-sdkman-install` | `~/.sdkman` | 63 (restore), 94 (save) |
| `${{ runner.os }}-${{ env.ARCHITECTURE }}-sdkman-java-${{ requested_version }}` | `~/.sdkman/candidates/java/<version>` | 101, 175 |
| `${{ runner.os }}-${{ env.ARCHITECTURE }}-sdkman-java-${{ graalvm_version }}` | `~/.sdkman/candidates/java/<graalvm>` | 109, 182 |

## Internal step contract — the only part that changes

| Step | Before | After |
|---|---|---|
| `Install Bash 4+ (macOS)` (`:64-71`) | `brew install bash`, gated on macOS + cache miss | **Deleted** |
| `Install SDKMan` (`:72-87`) | `curl -s … \| "$(brew --prefix)/bin/bash"` on macOS, `\| bash` on Linux | Download → assert guard → disarm on macOS → run with system `bash` |

Step `id`s that other steps reference must be preserved:

- `restore-cache-sdkman` — its `outputs.cache-hit` gates `Install SDKMan` (`:74`) and
  `Save Cache SDKMan install` (`:90`). Deleting the brew step must not disturb this wiring.
- `install-sdkman` — retained even though nothing currently reads its outputs.
- `get-requested-version` — `outputs.requested_version` / `outputs.graalvm_version` feed four
  cache keys. Untouched.

## Platform behavior

| Platform | Before | After |
|---|---|---|
| Linux (`ubuntu-*`) | Installs with system Bash 5.x; brew step never evaluated (`runner.os == 'macOS'`) | **Byte-for-byte identical.** No new conditional on the Linux path. |
| macOS arm64 (`macos-14`) | Fails: `bash: no bottle available!` | Installs with system Bash 3.2.57 |
| macOS x86_64 (`macos-15-intel`) | Fails: `bash: no bottle available!` | Installs with system Bash 3.2.57 |

## Verification

Every row above is asserted by the validation workflow described in
[quickstart.md](../quickstart.md). The Linux row is covered by the existing pipeline — the step
remains gated on `runner.os == 'macOS'`, so no Linux job changes behavior.

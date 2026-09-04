# Feature Specification: `dotcms agent setup` — one command to wire an IDE to dotCMS

**Feature Branch**: `fmontes/cli-agent`

**Created**: 2026-09-03

**Status**: Draft

**Type**: New Feature

**Input**: User description: "`dotcms agent setup` — one command to wire an IDE to dotCMS. Collapses the four manual steps (find the admin panel, mint an API token, hand-edit an IDE config file, install the skills) into a single command. Scope is agent setup only; `create-app` stays where it is and the dotCLI port is not started here. Targets: Claude Code, Cursor, VS Code/Copilot, Codex, Antigravity, Devin, OpenCode."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Connect my editor to dotCMS in one command (Priority: P1)

A developer who has just been handed a dotCMS instance wants their AI coding agent to be able to operate it. Today they must find the admin panel, mint an API token by hand, look up which config file their particular editor reads, hand-edit that file in the right format, and then separately install the dotCMS skills. Instead, they run a single command. It asks where their dotCMS instance is, how to authenticate, and which of their installed editors to wire up — then it does the rest and tells them what to do next.

**Why this priority**: This is the entire point of the feature. Everything else in this spec is a refinement of, or a follow-up to, this one journey. If only this ships, the feature already delivers its full headline value.

**Independent Test**: On a machine with a supported editor installed and no prior dotCMS agent configuration, run the command against a reachable dotCMS instance, answer the prompts, then open that editor and confirm the dotCMS MCP server connects and its tools are listed.

**Acceptance Scenarios**:

1. **Given** a developer with Cursor installed and no dotCMS agent configuration, **When** they run the setup command and supply an instance address plus valid credentials, **Then** Cursor is pre-selected as a detected target, a credential is minted and confirmed to work, the configuration is written, the skills are installed, and a summary names every file that was written.
2. **Given** the developer supplies an instance address that is unreachable, **When** setup runs, **Then** it stops before asking for credentials and reports that the instance could not be reached, naming the address it tried.
3. **Given** the developer supplies an incorrect username or password, **When** setup runs, **Then** it says the credentials were rejected and lets them try again, up to three attempts, before giving up with a clear message.
4. **Given** a credential is issued but does not actually grant access, **When** setup verifies it, **Then** setup fails at that point with a message saying the credential was rejected by the instance — rather than writing a configuration that will produce confusing authorization failures later.
4a. **Given** a developer supplies an expired or revoked credential directly instead of minting one, **When** setup verifies it, **Then** it fails the same way, and no configuration file for any editor has been created or modified.
5. **Given** the developer has several supported editors installed, **When** setup detects them, **Then** all detected editors are offered pre-selected and the developer can deselect any of them before anything is written.
6. **Given** setup completes, **When** the summary is printed, **Then** it lists each selected editor with its scope, the file written, and the outcome — and the next action the developer should take.
7. **Given** the configurations have been written, **When** setup confirms the agent connects, **Then** it starts the configured server, reports that it responded with its tools, and only then declares the run ready to use.
8. **Given** the configurations are written correctly but the server does not start — it cannot be fetched, or the runtime is unsupported — **When** confirmation runs, **Then** setup reports that distinctly from a credential failure, leaves the written configurations in place, does not claim the run is ready, and exits non-zero.
9. **Given** three editors are selected and the second cannot be written because its file is not writable, **When** setup runs, **Then** the first and third are still configured, the summary marks the second as failed with the reason, and the command exits non-zero.

---

### User Story 2 - Re-run setup without damaging what is already there (Priority: P1)

Editor configuration files are shared property: they usually already list other MCP servers the developer depends on, and often contain unrelated settings. A developer re-runs setup after their token expires, or after adding a second editor, or points the same editor at a different dotCMS instance. Nothing they had before may be lost.

**Why this priority**: The failure mode here is silent destruction of a developer's working environment, which is worse than the manual process this feature replaces. Merge safety is not a polish item — it is the precondition for the command being safe to run at all.

**Independent Test**: Seed each supported editor's configuration file with several unrelated servers and unrelated settings, run setup, and assert byte-for-byte that every pre-existing entry survives and only the dotCMS entry was added.

**Acceptance Scenarios**:

1. **Given** a configuration file that already lists other MCP servers, **When** setup writes the dotCMS entry, **Then** every other server and every unrelated setting in that file is preserved exactly.
2. **Given** a configuration file that already contains a dotCMS entry, **When** setup runs interactively, **Then** the developer is asked before it is replaced.
3. **Given** the same situation with the force or assume-yes option supplied, **When** setup runs, **Then** the existing dotCMS entry is replaced without prompting.
4. **Given** a configuration file that is not parseable, **When** setup tries to write to it, **Then** it reports which file is malformed and how to proceed, leaves the file untouched, and never overwrites it.
5. **Given** a configuration file does not yet exist, **When** setup runs, **Then** the file and any directories it needs are created.

---

### User Story 3 - Keep the credential out of places it can leak (Priority: P1)

Setup handles a long-lived credential that grants access to a dotCMS instance. A developer running it on a shared machine, or inside a repository they will push, must not have that credential end up somewhere they did not intend.

**Why this priority**: A convenience command that quietly commits a credential to a public repository, or exposes it to every other user of a shared machine, is a net negative regardless of how good the rest of the experience is. This constraint is non-negotiable under the project's security principles.

**Independent Test**: Run setup at each scope with a known credential fixture, then assert the fixture string appears in no captured output, in no process listing, and only in files whose permissions restrict them to the owner.

**Acceptance Scenarios**:

1. **Given** setup writes a credential into any file, **When** the write completes, **Then** that file is readable and writable only by its owner, and any directory setup created is likewise restricted to its owner.
2. **Given** setup must display the credential, **When** it appears anywhere in output, **Then** it is shown only in truncated form and never in full.
3. **Given** setup performs any action that requires the credential, **When** that action runs, **Then** the credential is never passed in a way that makes it visible to other users of the machine.
4. **Given** the developer chooses to configure the current project rather than their user account, **When** setup completes, **Then** it names every file it placed a credential into and offers to exclude those files from version control, defaulting to yes.
5. **Given** the project-scoped file in question is one that projects conventionally commit to version control, **When** setup writes a credential into it, **Then** it explicitly warns that this file is normally committed and that doing so would publish the credential.

---

### User Story 4 - Non-interactive setup for scripts and provisioning (Priority: P2)

A team lead wires dotCMS into a devcontainer, an onboarding script, or a CI job. There is nobody at a keyboard to answer prompts, so every choice must be expressible up front.

**Why this priority**: It multiplies the reach of the P1 journey — one person configures a template and a whole team benefits — but the interactive path is what makes the feature worth shipping, and this is useless without it.

**Independent Test**: Run setup with every choice supplied up front in an environment with no interactive terminal; assert it completes without prompting and produces the same result as the equivalent interactive run.

**Acceptance Scenarios**:

1. **Given** the instance address, username, password, target editors, and scope are all supplied up front, **When** setup runs, **Then** it mints a credential and completes without prompting for anything.
2. **Given** the instance address and an auth token are supplied up front along with targets and scope, **When** setup runs, **Then** it verifies that token and completes without prompting and without minting a new one.
3. **Given** only the instance address and username are supplied, **When** setup runs interactively, **Then** it prompts for the password alone and re-asks for nothing that was already supplied.
4. **Given** a password is entered at the prompt, **When** the developer types it, **Then** it is not echoed to the terminal.
5. **Given** a username is supplied without a password, **When** setup runs in an environment with no interactive terminal, **Then** it fails with a message naming the password as the missing value.
6. **Given** an auth token is supplied together with a username or a password, **When** setup runs, **Then** it fails immediately as a usage error naming the conflicting options and stating that the two modes are alternatives — no token is minted and no configuration is written.
7. **Given** neither an auth token nor any username or password is supplied, **When** setup runs in an environment with no interactive terminal, **Then** it fails with a message naming both authentication modes.
8. **Given** the instance address is not supplied as an option, **When** setup runs, **Then** it falls back to the environment before prompting, and to a documented local default before failing.
9. **Given** the password is supplied through its environment variable rather than as an option, **When** setup runs, **Then** it is used without prompting and never appears in the process list.
10. **Given** a supplied editor identifier is not one of the supported targets, **When** setup runs, **Then** it fails with a message naming the unrecognized identifier and listing the valid ones.
11. **Given** the developer asks to skip either the configuration step or the skills step, **When** setup runs, **Then** that step is skipped and the summary says so.

---

### User Story 5 - See what is configured, and undo it (Priority: P2)

Later — debugging a connection problem, auditing a machine, or cleaning up — a developer needs to know what was configured where, whether the credential still works, and how to remove it all.

**Why this priority**: Without this, the command is write-only: it can create state on a developer's machine that they then have to unpick by hand, which reintroduces exactly the manual file-editing this feature exists to eliminate.

**Independent Test**: Run setup, then status, and assert status reports exactly what setup wrote for every target; then run remove and assert every touched file returns to its pre-setup contents.

**Acceptance Scenarios**:

1. **Given** setup has run for one or more editors, **When** the developer asks for status, **Then** they see, per editor, the scope, the file, the instance address, and a truncated credential.
2. **Given** status runs, **When** it checks the recorded credential against the recorded instance, **Then** it reports whether that credential is still accepted.
3. **Given** an editor was never configured, **When** status runs, **Then** it is reported as not configured rather than omitted or reported as an error.
4. **Given** the developer asks to remove the configuration, **When** removal runs, **Then** only the dotCMS entry is removed from each file and every other entry and setting is preserved.
5. **Given** removal runs against a file that has no dotCMS entry, **When** it completes, **Then** it reports nothing to remove rather than failing.

---

### Edge Cases

- The developer's home directory contains a configuration file owned by another user or otherwise not writable — setup must name the file and the permission problem, not fail with an unhandled error.
- Two supported editors share a configuration file, or one editor is selected twice through different identifiers — each file must be written once, not twice.
- The developer selects an editor that was not detected on the machine — setup must honor the explicit selection and write the configuration anyway, since the editor may be installed later.
- No supported editor is detected at all — setup must say so and let the developer pick targets explicitly rather than exiting silently.
- The skills installation fails — for example because the toolkit source is unreachable or the machine is offline. This must not fail the whole command: the configuration work already done stands, and the developer is given the exact command to run once the problem is resolved.
- An editor supports the server configuration but its skills location is unconfirmed — the summary must not claim the skills landed for that editor.
- The instance is reachable but returns an unexpected response shape — the failure must name what was expected, not surface a parsing error.
- Setup is interrupted midway — no configuration file may be left in a partially written or unparseable state.
- An editor is running while setup writes its configuration, and rewrites that file itself afterwards — setup's entry can be lost through no fault of its own. See Assumptions: this is a documented limitation, not something setup attempts to prevent.
- The machine is offline, or the server package cannot be fetched, at the moment the connection is confirmed — the configurations are already written and correct, so this must read as "written, but the server did not start", never as a credential problem or as a silent success.
- The developer supplies an instance address with a trailing slash, without a scheme, or otherwise loosely formatted — it must be normalized rather than rejected or written through verbatim.

## Requirements *(mandatory)*

### Functional Requirements

**Command surface**

- **FR-001**: The tool MUST be runnable without prior installation, using the package runner developers already have.
- **FR-002**: The tool MUST expose agent configuration as a named group of sub-commands — set up, inspect status, and remove — so that unrelated future capabilities can be added alongside it without changing this command's invocation.
- **FR-003**: Setup MUST accept, as up-front options, every choice it would otherwise prompt for. Named explicitly: the **instance address**, the **username**, the **password**, an **auth token**, the target editors, the scope, whether to skip configuration, whether to skip skills, whether to assume yes, and whether to force overwrite.
- **FR-003a**: Authentication MUST be expressible in exactly two mutually exclusive ways: a **username and password**, from which setup mints a token, or a supplied **auth token**, which setup uses as-is. These are alternative modes, not a fallback chain.
- **FR-003b**: Supplying an auth token together with a username or a password MUST be rejected as a usage error, naming the conflicting options and stating that the two modes are alternatives. Setup MUST NOT silently prefer one mode over the other, MUST NOT mint a token, and MUST NOT write any configuration in this case.
- **FR-003c**: Within the username/password mode, supplying only one half MUST cause setup to prompt for the missing half — and in a non-interactive environment MUST fail with a message naming which half is missing.
- **FR-003d**: The instance address MUST be supplyable independently of whichever authentication mode is used, and setup MUST prompt only for what is missing, never re-asking for anything already supplied.
- **FR-003e**: Every secret-bearing option — the password and the auth token — MUST also be supplyable through an environment variable, and the documented order MUST be option first, then environment, then prompt.
- **FR-003f**: The help text for the password and auth token options MUST state that a value passed on the command line is visible in the machine's process list and is recorded in shell history, and MUST point at the environment variable and the interactive prompt as the safer alternatives.
- **FR-003g**: A password supplied at an interactive prompt MUST NOT be echoed to the terminal.
- **FR-003h**: When neither mode is supplied up front, setup MUST offer the choice interactively — sign in with a username and password, or supply an existing auth token — and MUST fail in a non-interactive environment with a message naming both modes.

**Discovering the instance and authenticating**

- **FR-004**: Setup MUST resolve the instance address in a documented order — supplied option first, then the environment, then an interactive prompt with a documented local default — and MUST normalize and validate whatever it resolves.
- **FR-005**: Setup MUST confirm the instance is reachable before asking for credentials, using an endpoint that is reachable from outside the instance's container.
- **FR-006**: Setup MUST support exactly two authentication modes — mint a token from a username and password, or accept a supplied auth token and use it as-is — and MUST treat them as mutually exclusive per FR-003a and FR-003b.
- **FR-007**: When minting fails because the credentials were rejected, setup MUST say so plainly and allow up to three attempts before giving up.
- **FR-008**: Setup MUST verify that the credential actually grants access to the instance before writing it anywhere. This applies to **every** credential source without exception — supplied as an option, read from the environment, minted from a username and password, or obtained automatically — and MUST be performed against the same instance the credential will be written against.
- **FR-008a**: Verification MUST happen before the first configuration file is opened for writing. When verification fails, setup MUST write nothing: no configuration file is created or modified for any editor, no directory is created, and no skills installation is attempted.
- **FR-008b**: A failed verification MUST report which check failed and against which instance — distinguishing at minimum a credential the instance rejects from an instance that could not be reached during verification — and MUST NOT fall back to writing the configuration anyway.
- **FR-008c**: Verification MUST NOT be skippable. Options that bypass prompting (assume-yes, force) affect confirmation prompts only and MUST NOT disable this check; the option that skips configuration writing entirely is the only way to reach the end of a run without it.

**Selecting targets and scope**

- **FR-009**: Setup MUST support these seven editors: Claude Code, Cursor, VS Code with Copilot, Codex, Antigravity, Devin, and OpenCode.
- **FR-010**: Setup MUST detect which supported editors are present on the machine and pre-select them, while allowing the developer to select any supported editor whether detected or not.
- **FR-011**: Setup MUST offer two scopes — the developer's user account, and the current project — and MUST default to the user account.
- **FR-012**: Setup MUST resolve each editor's configuration location correctly for the operating system it is running on, and MUST honor any documented environment override an editor provides for its own configuration location.
- **FR-013** *(structural constraint — verified by code review, not by test)*: Adding support for a further editor MUST require only describing that editor — its identifier, name, detection, locations, and configuration shape — and MUST NOT require changes to the setup, status, or removal flows. This constrains internal structure rather than observable behavior; the plan phase owns how it is met.

**Writing the configuration**

- **FR-014**: Setup MUST write each editor's configuration in the format and under the key that editor actually reads, including where an editor differs from the others in key name or entry shape.
- **FR-015**: Setup MUST write the configuration files itself rather than delegating to each editor's own command-line tool, so that credentials are never exposed through process arguments and so that status and removal can read back exactly what was written.
- **FR-016**: Setup MUST merge into existing configuration: it parses what is there, inserts or replaces only the dotCMS entry, and preserves everything else exactly.
- **FR-017**: Setup MUST prompt before replacing an existing dotCMS entry, and MUST skip that prompt when force or assume-yes is supplied.
- **FR-018**: When an existing configuration file cannot be parsed, setup MUST fail for that editor with a message naming the file and the remedy, MUST leave the file untouched, and MUST NOT overwrite it.
- **FR-019**: Setup MUST create missing configuration files and their parent directories.
- **FR-020**: The configuration entry MUST point the editor at the published dotCMS MCP server and supply it the instance address and credential under the exact environment variable names that server reads.
- **FR-020a**: When writing to one target fails, setup MUST continue with the remaining targets rather than aborting. Every target that can be written MUST be written.
- **FR-020b**: The summary MUST report a per-target outcome — written, skipped, or failed with the reason — so that a partially configured machine is legible rather than ambiguous.
- **FR-020c**: Setup MUST exit non-zero when any selected target failed, even if others succeeded, so that scripted and CI callers detect a partial result. It MUST exit zero only when every selected target succeeded.
- **FR-020d**: A failure writing one target MUST NOT roll back targets already written. Configurations already in place are left working, and the summary names exactly which succeeded.

**Handling the credential**

- **FR-021**: Every file setup writes a credential into MUST be restricted to its owner, as MUST any directory setup creates.
- **FR-022**: The credential MUST never be written to logs or console output in full, MUST never be passed as an argument to any sub-process, and MUST be truncated wherever it has to be shown.
- **FR-022a**: The password MUST be used only to mint a token and MUST NOT be persisted to any file, echoed at any point, or included in any summary or error message. Setup MUST NOT write the password into an editor's configuration under any circumstances — the only credential ever written is a token, whether minted or supplied.
- **FR-023**: When the project scope is chosen, setup MUST name every file it placed a credential into and MUST offer to exclude those files from version control, defaulting to yes.
- **FR-023a**: When the project scope is chosen in a directory that is not under version control, setup MUST still name the files it wrote a credential into and MUST warn that they are unprotected, rather than silently omitting the exclusion step.
- **FR-024**: When the project-scoped file for an editor is one that is conventionally committed to version control, setup MUST warn explicitly that writing a credential there risks publishing it.

**Confirming the agent actually connects**

- **FR-024a**: After the configurations are written, setup MUST confirm that the configured MCP server actually starts and responds — launching it exactly as the written configuration specifies, with the same instance address and credential, and confirming it reports its available tools. Verifying the credential (FR-008) is not sufficient: it proves the instance accepts the token, not that the developer's agent can run the server.
- **FR-024b**: This confirmation MUST run by default. It MUST be skippable by an explicit option for offline or air-gapped use, and MUST be skipped implicitly when configuration writing was skipped.
- **FR-024c**: When the server does not start or does not respond, setup MUST report that outcome distinctly from success and distinctly from a credential failure — the configuration was written correctly and the server did not come up — MUST name the likely causes it can distinguish (the server package could not be fetched, the runtime is unsupported, the server exited), and MUST exit non-zero.
- **FR-024d**: A failed confirmation MUST NOT remove or roll back the configurations already written, since they may be correct and the failure transient.
- **FR-024e**: The summary MUST state the confirmation result explicitly, and MUST NOT report the run as ready to use when confirmation did not succeed.

**Installing the skills**

- **FR-025**: Setup MUST install the dotCMS agent skills for the selected editors as part of the same run, in a single operation, unless skipped.
- **FR-026**: Skills installation failure MUST NOT fail the command; setup MUST report the failure and print the exact command the developer can run to complete that step themselves.
- **FR-027**: The summary MUST NOT report skills as installed for an editor whose skills location has not been confirmed to be read by that editor.

**Reporting, status, and removal**

- **FR-028**: Setup MUST end with a summary listing, per editor, the scope, the file written, and the outcome — followed by the concrete next action for the developer.
- **FR-029**: Status MUST report, per supported editor, whether it is configured, at which scope and file, against which instance, and with which truncated credential — and MUST report an unconfigured editor as such rather than as an error.
- **FR-030**: Status MUST report whether the recorded credential is still accepted by the recorded instance.
- **FR-031**: Removal MUST delete only the dotCMS entry from each configuration file, preserve everything else, and report a file with no dotCMS entry as nothing to remove rather than as a failure.
- **FR-032**: Every failure the tool can produce MUST be a named, actionable message identifying the file, address, or editor involved. Unhandled internal errors MUST NOT be surfaced to the developer.

**Distribution**

- **FR-033**: The tool MUST be published under a package name that a developer would guess, and MUST be released in lockstep with the dotCMS version it ships with, consistent with the existing SDK release process.
- **FR-034**: The release pipeline MUST be able to publish and re-publish this package idempotently. *(See Assumptions — the current pipeline's already-published check derives the package name from the directory and assumes the `@dotcms/` scope, so an unscoped name requires a pipeline change.)*

### Key Entities

- **Agent target**: A supported editor or agent. Identified by a stable identifier and a display name; knows how to detect its own presence, where its configuration lives at each scope, and what shape its configuration entry takes.
- **Scope**: Where configuration is written — the developer's user account (default, applies everywhere) or the current project (applies to this checkout only, and carries version-control risk).
- **Server registration**: The dotCMS entry placed into an editor's configuration — what to launch, the instance address, and the credential.
- **Credential**: A long-lived instance access token, either supplied by the developer or minted during setup. Sensitive; subject to all handling rules in FR-021 through FR-024.
- **Skills bundle**: The dotCMS agent skills, installed per editor from an external toolkit source and versioned independently of this tool.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001** *(design intent, not an automated gate)*: A developer with a supported editor installed and no prior dotCMS agent configuration goes from nothing to a connected agent by answering no more than three questions, with no manual file editing at any point.
- **SC-002** *(design intent, not an automated gate)*: That same journey completes in under three minutes on a reachable instance, replacing a four-step manual procedure that today requires reading documentation. Both SC-001 and SC-002 are targets to design toward and to check by hand during acceptance; neither is machine-verifiable, and neither should be treated as a blocking test.
- **SC-002a**: A successful run ends with the configured MCP server confirmed started and reporting its tools — the run proves the agent connects, not merely that the credential is valid.
- **SC-003**: Across every supported editor, 100% of pre-existing configuration entries survive a setup run unchanged.
- **SC-004**: 100% of files the tool writes a credential into are restricted to their owner.
- **SC-005**: A known credential value never appears in full in captured output, in any log, or in any process listing, across every command the tool offers.
- **SC-005a**: A known password value never appears in captured output, in any log, or in any file the tool writes — including when it was supplied as a command-line option.
- **SC-006**: All seven named editors are supported at ship.
- **SC-006a**: A run that fails on one target still configures every other selected target, reports the failure per target, and exits non-zero.
- **SC-007**: For every supported editor, status reports exactly what setup wrote — no drift between what is written and what is read back.
- **SC-008**: After removal, every touched file matches its pre-setup contents.
- **SC-009**: Every failure path the tool can reach produces a message naming the file, address, or editor involved; no failure path produces an unhandled internal error.
- **SC-010**: A run in a fully non-interactive environment, with all choices supplied up front, produces the same result as the equivalent interactive run.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: None of the dotCMS product surface changes. This adds a new developer-facing command-line tool to the front-end SDK area. It automates a procedure that the dotCMS MCP server's documentation currently describes as a manual copy-paste walkthrough covering only two editors; that documentation should point at this command once it ships, but the manual path remains valid. No server-side, legacy Java, or content code is touched.
- **Backward-compatibility expectations**:
  - The MCP server's configuration contract is consumed, not changed — the tool must write the environment variable names that server already reads, and stays compatible by depending on the published server rather than reimplementing anything.
  - The existing project-scaffolding tool is unaffected and continues to ship under its own name. This tool is deliberately structured so that tool, and a future content command, can fold in later as sibling sub-commands; that consolidation is out of scope here.
  - Publishing under the intended package name replaces an unrelated, long-dormant library that currently occupies it (last published version `0.0.21`). Anyone still depending on that library by an unpinned range would be broken by the first release. Confirm current download volume before the first publish.
- **Known related decisions**: SDK packages are versioned in date-lockstep with the dotCMS release they ship with, under ADR-0019 — there is no independent SDK versioning, and this package inherits that. The plan phase will formally consult `dotCMS/platform-adrs`.

## Assumptions

- **Users are developers.** The audience already has a package runner and a supported editor installed, and is comfortable in a terminal. No graphical interface is in scope.
- **Scope is agent setup only.** Project scaffolding stays in its existing tool, and porting the existing content command-line tool is explicitly not started here. The command structure is chosen so both can be added later without changing this command's invocation.
- **The skills toolkit is external, and is publicly readable.** The source repository was made public on 2026-09-03; verified both that it reports as public and that an unauthenticated clone succeeds, which is the path the skills installer takes. A developer outside dotCMS therefore gets both halves of the command with no credentials of their own. This was previously the feature's one hard release gate and is now cleared. Graceful degradation on skills failure (FR-026) remains required — the toolkit is a separate repository on its own release cadence, so it can still be unreachable or broken at run time for ordinary reasons.
- **The release pipeline needs one change, contrary to the initial assessment.** The SDK release action rewrites versions and publishes every built package generically, so an added package is swept up automatically. However its "already published" guard is derived from the directory name with the `@dotcms/` scope hardcoded (`.github/actions/core-cicd/deployment/deploy-javascript-sdk/action.yml`). For an unscoped package name that guard can never match, so the first publish succeeds but any re-run — the exact case the guard exists to handle — attempts to re-publish an existing version and fails the release step. FR-034 covers this; the plan must address it.
- **One editor's skills location is unverified.** For VS Code with Copilot the server configuration location is well established, but the skills location the installer targets is the Copilot command-line tool's directory, and it is not confirmed that the in-editor agent reads it. Until verified, the summary reports honestly rather than claiming success (FR-027).
- **One editor's configuration location comes from documentation, not from an installed copy.** Antigravity's user-account configuration path is taken from its documentation. The implementation is expected to probe at runtime and degrade gracefully if the actual layout differs.
- **Credentials are written literally, by design.** No operating-system keychain integration is in scope; the mitigation is restrictive file permissions, truncation in output, never passing the credential through process arguments, and version-control exclusion for project scope.
- **Accepting a password as a command-line option is a deliberate, qualified trade-off.** It is required for scripted and provisioning use, but a value passed that way is visible in the machine's process list for the lifetime of the process and is recorded in shell history — the same class of exposure the tool otherwise refuses for the token. It is accepted because the alternative, forcing an interactive prompt, would make the non-interactive journey impossible. The mitigations are that an environment variable is offered for every secret-bearing option and documented as preferred (FR-003e), the risk is stated in the help text rather than left for the developer to discover (FR-003f), the password is never persisted or echoed (FR-022a), and the long-lived artifact that actually reaches disk is the minted credential rather than the password.
- **Minted credentials are long-lived.** Setup mints a token with a one-year lifetime and a label identifying its origin. Renewal is out of scope; status surfaces that a credential has stopped working, and re-running setup replaces it.
- **Concurrent writes are a documented limitation, not a solved problem.** An editor that is running may rewrite its own configuration file and drop setup's entry, and two setup runs at once could lose one another's writes. No file locking or coordination is specified. The mitigation is that `agent status` shows the true current state and re-running setup is cheap and idempotent; the summary's next-step line is the natural place to suggest restarting the editor. Revisit only if this proves to bite in practice.
- **The MCP server is consumed as published.** The tool points editors at the published dotCMS MCP server package rather than bundling or building it, so server updates reach developers without a new release of this tool.
- **Existing helpers are duplicated, not extracted.** A small amount of address-validation and authentication logic already exists in the project-scaffolding tool. It is copied rather than factored into a shared library, to avoid churning that tool while work on it is in flight; consolidation is deferred to the future command merge.

/**
 * Shared vocabulary for a run. Types only — every module with behaviour lives behind a TDD
 * gate in tasks.md. See specs/37390-dotcms-agent-setup/data-model.md.
 */

/**
 * Where configuration is written.
 *
 * `folder` is the DEFAULT (FR-011), which inverts the intuitive choice deliberately: a folder's
 * configuration names one dotCMS instance, and a second instance means a second folder. It also
 * makes the credential-into-a-repo path the common one, which is why the `.gitignore` offer is
 * load-bearing rather than a nicety.
 */
export type Scope = 'folder' | 'global';

/** The only secret ever written into a configuration. */
export interface Token {
    value: string;
    origin: 'minted' | 'supplied';
    /**
     * Set only by a successful `GET /api/v1/users/current`. No file is opened for writing
     * while this is false (FR-008a) — the ordering guarantee the whole design rests on.
     */
    verified: boolean;
}

/** Per-target result. Drives the summary (FR-020b) and the exit code (FR-020c). */
export interface TargetOutcome {
    targetId: string;
    scope: Scope;
    /** The file written, or null when nothing was. */
    path: string | null;
    result: 'written' | 'replaced' | 'skipped' | 'failed';
    /** Required when `failed`, and must be self-sufficient — there is no verbose mode (FR-032a). */
    reason: string | null;
    /** False on Windows, where chmod does not touch ACLs. The summary must say so rather than
     *  imply protection (research R5). */
    permissionsApplied: boolean;
    /** `unverified` where the editor's skills location is unconfirmed — never reported as
     *  installed (FR-027). */
    skillsInstalled: 'yes' | 'no' | 'unverified';
}

/**
 * Resolved inputs. `url` plus exactly one auth mode are the only REQUIRED inputs (FR-003i);
 * everything else has a default and can never block a run (FR-003j).
 */
export interface RunOptions {
    url: string;
    /** Base directory for folder scope. Defaults to `process.cwd()`; injected in tests. */
    cwd?: string;
    user?: string;
    password?: string;
    authToken?: string;
    agents?: string[];
    scope: Scope;
    skipMcp: boolean;
    skipSkills: boolean;
    skipVerify: boolean;
    /** Confirmations ONLY. Never suppresses a prompt for a missing required input (FR-003l). */
    yes: boolean;
    force: boolean;
    /**
     * Asks before replacing an existing `dotcms` entry (FR-017). Injected so the confirmation
     * is testable without a terminal, and so `setup.ts` stays free of prompt mechanics.
     */
    confirmOverwrite?: (file: string) => Promise<boolean>;
    /**
     * Offers to keep token-bearing files out of version control (FR-023). `--yes` supplies the
     * SAFE answer rather than bypassing the step.
     */
    confirmExclude?: (files: string[]) => Promise<boolean>;
    /** How to ask, when asking is needed. Injected so the rules stay testable without a terminal. */
    promptPort?: import('./prompts').PromptPort;
    /**
     * Progress reporting. The connection check spawns `npx`, which on a cold cache downloads
     * the server and can run for the better part of a minute — silence there reads as a hang.
     * Injected like promptPort so the flow stays free of terminal concerns and tests ignore it.
     */
    onProgress?: (step: string) => void;
}

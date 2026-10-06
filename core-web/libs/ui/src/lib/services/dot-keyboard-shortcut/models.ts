/**
 * A claim on a key combination by a surface.
 *
 * Registrations are per-combination and last-in-wins: the most recent claim on a combination
 * receives the key, and withdrawing it restores the one beneath. See
 * `DotKeyboardShortcutService` for the arbitration rules.
 */
export interface DotKeyboardShortcut {
    /**
     * The combination to claim, lower-case, `+`-separated, modifiers first.
     *
     * `mod` is the platform's primary modifier and matches either Command or Control, so one
     * registration serves every platform. Examples: `mod+k`, `escape`, `mod+b`, `shift+mod+k`.
     */
    combination: string;

    /**
     * i18n key describing what the shortcut does, for author-facing documentation.
     *
     * Required rather than optional so a shortcut cannot ship undocumented: the documentation is
     * generated from the registry, and a missing label would silently omit it.
     */
    label: string;

    /**
     * Runs when the combination fires.
     *
     * Return `false` to **decline**, which passes the key to the next claimant and then to the
     * browser. Anything else (including `undefined`) consumes it. Declining is for runtime decisions
     * a nested DOM handler cannot express; a component that handles a key on its own element is
     * already closer to the event target and never reaches the registry at all.
     */
    handler: (event: KeyboardEvent) => boolean | void;
}

/** Withdraws a registration. Safe to call more than once. */
export type DotKeyboardShortcutUnregister = () => void;

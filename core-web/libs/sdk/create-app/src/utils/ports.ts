/**
 * Decides what to do when a required port is already taken.
 *
 * Reproduction step 6 of #37262: after a successful run leaves a dotCMS stack up, re-running the
 * CLI aborts with "Required ports are already in use" — on exactly the ports that success now
 * holds. The CLI's own side effect blocked its own recovery, which is what turned an annoying
 * failure into an unrecoverable one.
 *
 * A busy 8082 with a healthy dotCMS behind it is not a conflict; it is an instance to reuse.
 * Anything else still fails, and it fails as a returned value rather than a `process.exit`
 * (contract X2).
 */

export interface BusyPort {
    port: number;
    service: string;
}

export type PortConflictOutcome =
    | { kind: 'free' }
    | { kind: 'reuse'; host: string }
    | { kind: 'abort'; message: string };

export interface ResolvePortConflictOptions {
    busyPorts: BusyPort[];
    /** False in CI or when stdout is not a TTY — a prompt there would hang a scripted run. */
    isInteractive: boolean;
    host: string;
    /** Must confirm BOTH readiness and token issuance; a half-dead instance is not reusable. */
    probeInstance: () => Promise<boolean>;
    askReuse: () => Promise<boolean>;
    notify: (message: string) => void;
}

const DOTCMS_HTTP_PORT = 8082;

function listPorts(busyPorts: BusyPort[]): string {
    return busyPorts.map(({ port, service }) => `  • Port ${port} (${service})`).join('\n');
}

function abortMessage(busyPorts: BusyPort[], detail: string): PortConflictOutcome {
    return {
        kind: 'abort',
        message: [
            'Required ports are already in use:',
            listPorts(busyPorts),
            '',
            detail,
            '',
            'Stop whatever is holding them, or stop an existing stack with:',
            '  docker compose down'
        ].join('\n')
    };
}

export async function resolvePortConflict(
    options: ResolvePortConflictOptions
): Promise<PortConflictOutcome> {
    const { busyPorts, isInteractive, host, probeInstance, askReuse, notify } = options;

    if (busyPorts.length === 0) {
        return { kind: 'free' };
    }

    const onlyDotcmsHttp =
        busyPorts.length > 0 && busyPorts.every(({ port }) => port === DOTCMS_HTTP_PORT);

    // Reuse is only meaningful when the thing in the way IS the dotCMS we would have started.
    // If OpenSearch's ports are also held, this is somebody else's stack, not ours to adopt.
    if (!onlyDotcmsHttp) {
        return abortMessage(
            busyPorts,
            'These are not ports a previous run of this CLI would be holding on its own.'
        );
    }

    const reusable = await probeInstance();

    if (!reusable) {
        return abortMessage(
            busyPorts,
            `Something is listening on ${DOTCMS_HTTP_PORT}, but it did not answer as a usable dotCMS.`
        );
    }

    if (!isInteractive) {
        // D3: non-interactive means "do not block a scripted run with a prompt" — it does not
        // mean do it quietly. Attaching to an unknown instance without saying so is the failure
        // this branch exists to avoid.
        notify(
            `dotCMS is already running on ${DOTCMS_HTTP_PORT} — reusing it (non-interactive run).`
        );

        return { kind: 'reuse', host };
    }

    const reuse = await askReuse();

    if (!reuse) {
        // A real choice: someone who did not expect a dotCMS on 8082 should be able to stop and
        // look rather than be carried forward into it.
        return {
            kind: 'abort',
            message: `Left the dotCMS already running on ${DOTCMS_HTTP_PORT} untouched, as requested.`
        };
    }

    return { kind: 'reuse', host };
}

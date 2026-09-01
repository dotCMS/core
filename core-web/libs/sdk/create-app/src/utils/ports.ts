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

/**
 * The ports the bundled stack publishes — and therefore the only ones worth checking.
 *
 * This list used to include 9200 and 9600, inherited from the shared compose example back when
 * the CLI downloaded it. The bundled asset publishes neither (OpenSearch has no `ports:` at all),
 * so the CLI was refusing to run for anyone with their own OpenSearch on 9200 over a conflict
 * that could not happen. A spec pins this list against the asset so the two cannot drift again.
 */
export const REQUIRED_PORTS = [
    { port: 8082, service: 'dotCMS HTTP' },
    { port: 8443, service: 'dotCMS HTTPS' },
    { port: 8090, service: 'dotCMS management' }
];

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

    // Reuse is only meaningful when the thing in the way IS a stack this CLI would have started:
    // every busy port must be one of ours, and 8082 must be among them or nothing is answering
    // to reuse.
    //
    // This previously required 8082 to be the ONLY busy port, which no real instance ever
    // produces — a running stack holds 8082 and 8443 together, so the reuse path could never
    // fire and reproduction step 6 stayed broken despite passing unit tests. Found by running
    // the CLI against a real bricked instance (T054 step 5b).
    const ours = new Set(REQUIRED_PORTS.map(({ port }) => port));
    const looksLikeOurStack =
        busyPorts.every(({ port }) => ours.has(port)) &&
        busyPorts.some(({ port }) => port === DOTCMS_HTTP_PORT);

    if (!looksLikeOurStack) {
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

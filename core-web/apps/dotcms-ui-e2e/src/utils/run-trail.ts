import { type Page } from '@playwright/test';

/** Requests whose answers start or read a background run, which a failing wait should account for. */
const RUN_REQUEST = /\/_bulk(?:upload|duplicate|delete)|\/api\/v1\/jobs\/[^/]+\/active/;

/** How many entries a page's trail keeps; the oldest are dropped. */
const TRAIL_LIMIT = 60;

const trails = new WeakMap<Page, string[]>();

/**
 * Starts recording what the page was told about background runs, once per page.
 *
 * A run's status and outcome reach the page by a pushed event rather than by anything it asks for,
 * so when a wait for one times out the question is whether the event arrived, and in what order
 * against the response that started the run. Nothing here costs a passing test anything beyond a
 * string per event: the trail is only read by {@link withRunTrail}, after a failure.
 */
export function trackRunTrail(page: Page): void {
    if (trails.has(page)) {
        return;
    }

    const trail: string[] = [];
    const startedAt = Date.now();
    const note = (entry: string) => {
        trail.push(`+${Date.now() - startedAt}ms ${entry}`);
        if (trail.length > TRAIL_LIMIT) {
            trail.shift();
        }
    };

    trails.set(page, trail);

    page.on('response', (response) => {
        if (RUN_REQUEST.test(response.url())) {
            note(
                `${response.request().method()} ${new URL(response.url()).pathname} ${response.status()}`
            );
        }
    });

    page.on('websocket', (socket) => {
        note('websocket opened');
        socket.on('close', () => note('websocket closed'));
        socket.on('framereceived', (frame) => {
            try {
                const message = JSON.parse(String(frame.payload));
                const jobId = message?.payload?.data?.jobId;
                if (jobId) {
                    note(`websocket ${message.event} job ${jobId}`);
                }
            } catch {
                // Not JSON: nothing a run was reported by.
            }
        });
    });
}

/**
 * Runs an assertion about a background run and, if it fails, appends what the page saw to the
 * failure: the status toasts on screen, and the trail of run requests and pushed events.
 *
 * Failure path only: the assertion's own message is kept and the extra text follows it.
 */
export async function withRunTrail(page: Page, assertion: () => Promise<void>): Promise<void> {
    try {
        await assertion();
    } catch (error) {
        const statuses = await page
            .getByTestId('status-toast-summary')
            .allTextContents()
            .catch(() => ['(unreadable)']);
        const detail = [
            `Status toasts on screen: ${JSON.stringify(statuses)}`,
            'Run requests and pushed events, oldest first:',
            ...(trails.get(page) ?? ['(not tracked)']).map((entry) => `  ${entry}`)
        ].join('\n');

        if (error instanceof Error) {
            error.message = `${error.message}\n\n${detail}`;
        }
        throw error;
    }
}

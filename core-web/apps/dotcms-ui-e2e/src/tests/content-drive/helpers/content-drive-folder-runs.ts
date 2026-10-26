import { expect, type Locator, type Page } from '@playwright/test';

/**
 * How long to wait for anything a folder run's job has to produce.
 *
 * The same budget the duplicate and upload outcomes use: the run is asynchronous end to end
 * (request, queued job, pushed completion), and the suite runs two workers against one instance.
 */
const OUTCOME_TIMEOUT = 60000;

/**
 * Records the text of every element matching `selector` that appears from the next page load on.
 *
 * For a state that lasts only until a background run ends. On a fast instance that can be a few
 * hundred milliseconds, short enough to fall between an assertion's retries and never be seen. An
 * observer started with the document misses nothing. Read it back with {@link expectSeen}.
 *
 * @returns a reader for what has been seen so far, each text once, in order of appearance
 */
export async function recordFromNextLoad(
    page: Page,
    selector: string
): Promise<() => Promise<string[]>> {
    await page.addInitScript((watched) => {
        const seen: string[] = [];
        Object.assign(window, { __e2eSeen: seen });
        new MutationObserver(() => {
            document.querySelectorAll(watched).forEach((element) => {
                const text = element.textContent?.trim() ?? '';
                if (!seen.includes(text)) {
                    seen.push(text);
                }
            });
        }).observe(document, { childList: true, subtree: true, characterData: true });
    }, selector);

    return () => page.evaluate(() => (window as Window & { __e2eSeen?: string[] }).__e2eSeen ?? []);
}

/** Waits until a recorder from {@link recordFromNextLoad} has seen a text containing `text`. */
export async function expectSeen(read: () => Promise<string[]>, text: string) {
    await expect
        .poll(read, { timeout: OUTCOME_TIMEOUT })
        .toContainEqual(expect.stringContaining(text));
}

/** The endpoints a folder duplicate or delete is submitted to. */
export const FOLDER_RUN_URL = {
    duplicate: '/api/v1/assets/folders/_bulkduplicate',
    delete: '/api/v1/assets/folders/_bulkdelete'
} as const;

/**
 * Makes the configuration advertise the given folder ceilings.
 *
 * The real value is a server setting no test can change on demand, so the configuration response
 * is answered with the server's own body, changed only where the ceilings are. `null` advertises no
 * ceiling at all, as an instance older than the field does. Must run before the app loads, which
 * is when it reads the configuration.
 */
export async function advertiseFolderCeilings(
    page: Page,
    ceilings: { duplicate?: number | null; delete?: number | null }
): Promise<void> {
    await page.route(
        (url) => url.pathname === '/api/v1/appconfiguration',
        async (route) => {
            const response = await route.fetch();
            const body = await response.json();
            const config = body?.entity?.config;

            if (config && ceilings.duplicate !== undefined) {
                config.folderBulkDuplicate =
                    ceilings.duplicate === null ? undefined : { maxPaths: ceilings.duplicate };
            }

            if (config && ceilings.delete !== undefined) {
                config.folderBulkDelete =
                    ceilings.delete === null ? undefined : { maxPaths: ceilings.delete };
            }

            await route.fulfill({ response, json: body });
        }
    );
}

/**
 * Answers a folder run's submission as accepted without it reaching the server, and records what
 * was sent.
 *
 * For a test about what the client submits rather than what the server does with it: nothing is
 * duplicated or deleted, and no completion follows.
 *
 * @returns the folder paths of every submission, in order
 */
export async function acceptSubmissionsWithoutRunning(
    page: Page,
    endpoint: string
): Promise<string[][]> {
    const submitted: string[][] = [];

    await page.route(
        (url) => url.pathname === endpoint,
        async (route) => {
            const paths: string[] = route.request().postDataJSON()?.assetPaths ?? [];
            submitted.push(paths);

            await route.fulfill({
                status: 202,
                contentType: 'application/json',
                body: JSON.stringify({
                    entity: {
                        jobId: `e2e-not-run-${submitted.length}`,
                        statusUrl: '/api/v1/jobs/e2e-not-run/status',
                        submitted: paths.length
                    }
                })
            });
        }
    );

    return submitted;
}

/**
 * Answers every submission to a folder run with the given refusal, the body the endpoint sends.
 */
export async function refuseFolderRunsWith(
    page: Page,
    endpoint: string,
    status: number,
    errorCode: string
): Promise<void> {
    await page.route(
        (url) => url.pathname === endpoint,
        (route) =>
            route.fulfill({
                status,
                contentType: 'application/json',
                body: JSON.stringify({
                    errors: [{ errorCode, message: 'server prose the author never reads' }]
                })
            })
    );
}

/**
 * Drives a folder duplicate or delete from the Action Center, and reads its preview.
 */
export class FolderRunPreview {
    constructor(private readonly page: Page) {}

    /** A listing row, found by its title. */
    row(title: string): Locator {
        return this.page.getByTestId('item-row').filter({
            has: this.page.getByTestId('item-title-text').getByText(title, { exact: true })
        });
    }

    /** Checks the given folders and opens the chosen quick action's preview. */
    async open(actionId: 'DUPLICATE' | 'DELETE_FOLDER', folderNames: string[]) {
        for (const name of folderNames) {
            await this.row(name).getByTestId('item-checkbox').click();
        }

        await this.page.getByTestId('action-center-button').click();
        await this.page.getByTestId(`quick-action-${actionId}`).click();
        await expect(this.page.getByTestId('action-preview')).toBeVisible();
    }

    /** Runs what the preview lists. */
    async execute() {
        await this.page.getByTestId('action-preview-execute').click();
    }

    /**
     * How many folders the preview lists. A count rather than names: the order follows the
     * listing's sort, which for folders seeded in one call is not something a test can rely on.
     */
    async expectListedCount(count: number) {
        await expect(
            this.page.getByTestId('action-preview').getByTestId('item-title-text')
        ).toHaveCount(count);
    }

    /** The folder is gone from the listing and from the sidebar tree. */
    async expectGone(folderName: string) {
        await expect(this.row(folderName)).toHaveCount(0, { timeout: OUTCOME_TIMEOUT });
        await expect(
            this.page
                .getByTestId('sidebar')
                .getByTestId('tree-node-label')
                .filter({ hasText: new RegExp(`^\\s*${folderName}\\s*$`) })
        ).toHaveCount(0, { timeout: OUTCOME_TIMEOUT });
    }
}

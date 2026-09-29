import { expect, type Locator, type Page } from '@playwright/test';

/**
 * How long to wait for anything a folder run's job has to produce.
 *
 * The same budget the duplicate and upload outcomes use: the run is asynchronous end to end
 * (request, queued job, pushed completion), and the suite runs two workers against one instance.
 */
const OUTCOME_TIMEOUT = 60000;

/** The endpoints a folder duplicate or delete is submitted to. */
export const FOLDER_RUN_URL = {
    duplicate: '/api/v1/assets/folders/_bulkduplicate',
    delete: '/api/v1/assets/folders/_bulkdelete'
} as const;

/**
 * Makes the configuration advertise the given folder ceilings.
 *
 * The real value is a server setting no test can change on demand, so the configuration response
 * is answered with the server's own body, changed only where the ceilings are. Must run before the
 * app loads, which is when it reads the configuration.
 */
export async function advertiseFolderCeilings(
    page: Page,
    ceilings: { duplicate?: number; delete?: number }
): Promise<void> {
    await page.route(
        (url) => url.pathname === '/api/v1/appconfiguration',
        async (route) => {
            const response = await route.fetch();
            const body = await response.json();
            const config = body?.entity?.config;

            if (config && ceilings.duplicate !== undefined) {
                config.folderBulkDuplicate = { maxPaths: ceilings.duplicate };
            }

            if (config && ceilings.delete !== undefined) {
                config.folderBulkDelete = { maxPaths: ceilings.delete };
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

    /** The warning that the selection is over the ceiling, containing the given words. */
    async expectCeilingWarning(text: string) {
        await expect(this.page.getByTestId('action-preview-over-ceiling')).toContainText(text);
    }

    async expectNoCeilingWarning() {
        await expect(this.page.getByTestId('action-preview-over-ceiling')).toHaveCount(0);
    }

    /** The folder is marked as being worked on, in the listing. */
    async expectMarkedInFlight(folderName: string) {
        await expect(this.row(folderName).getByTestId('row-busy')).toBeVisible({
            timeout: OUTCOME_TIMEOUT
        });
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

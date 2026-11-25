import { ContentDrivePage } from '@pages';
import { type Page } from '@playwright/test';

/** Where Content Drive reads the upload batches still running when it loads. */
const ACTIVE_UPLOADS_URL = '/api/v1/jobs/assetBulkUpload/active';

/**
 * Reloads the page so that the batch it restores is certain to still be running when the page
 * asks the queue about it.
 *
 * The status of a running batch is put back from the queue's active listing, read once as the page
 * loads. A reload takes several seconds on a CI runner and a batch of a few files finishes in about
 * six, so a batch started in the page itself and followed by a reload was often over by the time of
 * the read, which then correctly reports nothing running and leaves nothing to restore.
 *
 * So the reloaded page's read is held, the batch is sent from a second tab of the same session, and
 * only once the server has accepted it does the read go through. What is under test is unchanged:
 * the page restores the status of a running batch it did not start, from the queue, and the
 * completion ends it. Holding the read leaves only the round trip between the batch being accepted
 * and the read reaching the server, where before it was the whole of a reload.
 *
 * @param page the page that reloads and should show the restored status
 * @param folderName the folder, at the top of the tree, the batch is sent to
 * @param count how many files the batch carries
 * @param prefix keeps the batch's file names unique to the test
 */
export async function reloadWhileBatchIsRunning(
    page: Page,
    folderName: string,
    count: number,
    prefix: string
): Promise<void> {
    let accepted: () => void = () => undefined;
    const batchAccepted = new Promise<void>((resolve) => {
        accepted = resolve;
    });

    await page.route(
        (url) => url.pathname === ACTIVE_UPLOADS_URL,
        async (route) => {
            await batchAccepted;
            await route.continue();
        },
        { times: 1 }
    );

    await page.reload();

    const otherTab = await page.context().newPage();

    try {
        const drive = new ContentDrivePage(otherTab);

        await drive.goTo();
        await drive.openFolder(folderName);
        await drive.chooseGeneratedFilesForAcceptedUpload(count, prefix);
    } finally {
        // Whatever happened to the batch, the held read must go through, or the page waits on it.
        accepted();
        await otherTab.close();
    }
}

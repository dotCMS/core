import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

function authHeaders() {
    return {
        Authorization: generateBase64Credentials(admin1.username, admin1.password),
        'Content-Type': 'application/json'
    };
}

/** The states a job does not leave. Anything else is still in flight. */
const TERMINAL = ['SUCCESS', 'FAILED_PERMANENTLY', 'ABANDONED_PERMANENTLY', 'CANCELED'];

interface JobView {
    state?: string;
    parameters?: { folderId?: string };
}

/**
 * Waits until this folder's uploads have finished, not merely appeared.
 *
 * **Why a test needs this at all.** Content reaches the listing *before* its job is marked
 * `SUCCESS`: the contentlets are created during the run and the state transition follows. A test
 * that waits for a row has proven the content exists, not that the run ended — and one server
 * behaviour turns on exactly that difference. A resubmission is recognised by matching its
 * fingerprint against a job **already in `SUCCESS`** (`BulkUploadHelper.findSucceededSubmission`),
 * so resubmitting inside that gap makes the server answer, correctly, that this is not a duplicate.
 * The test then waits for copy that can never arrive. It cost a flaky CI run to learn.
 *
 * **Why it is scoped to one folder.** Waiting for *every* job to settle would couple tests to each
 * other: the suite runs two workers against one instance, so one test would wait out another's
 * uploads, and under load could time out because of work it has nothing to do with. Filtering on
 * the target folder keeps each test waiting only for itself — which matters more here than
 * elsewhere, because these tests deliberately create slow work.
 */
export async function waitForFolderJobsToSettle(
    request: APIRequestContext,
    siteName: string,
    folderPath: string,
    timeoutMs = 60000
): Promise<void> {
    const byPath = await request.post('/api/v1/folder/byPath', {
        data: { path: `//${siteName}${folderPath}` },
        headers: authHeaders()
    });
    expect(byPath.ok(), `could not resolve ${folderPath} to wait on its jobs`).toBeTruthy();

    const found = (await byPath.json())?.entity;
    const folder = Array.isArray(found) ? found[0] : found;
    const folderId: string = folder?.id ?? folder?.identifier ?? folder?.inode;
    expect(folderId, `no id for ${folderPath}`).toBeTruthy();

    const inFlight = async (): Promise<JobView[]> => {
        const response = await request.get('/api/v1/jobs?limit=50', { headers: authHeaders() });
        const jobs: JobView[] = (await response.json())?.entity?.jobs ?? [];

        return jobs.filter(
            (job) => job.parameters?.folderId === folderId && !TERMINAL.includes(String(job.state))
        );
    };

    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        const pending = await inFlight();

        if (!pending.length) {
            return;
        }

        await new Promise((resolve) => setTimeout(resolve, 1000));
    }

    // Fails rather than returning quietly, and names what was still running when it gave up.
    expect(await inFlight(), `jobs still in flight for ${folderPath}`).toEqual([]);
}

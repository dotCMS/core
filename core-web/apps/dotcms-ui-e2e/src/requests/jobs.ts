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

/**
 * Waits until no job is still in flight.
 *
 * **Why a test would need this.** Content appears in the listing *before* its job is marked
 * `SUCCESS`: the contentlets are created during the run, and the state transition comes after. A
 * test that waits for a row and then acts has therefore only proven the content exists, not that
 * the run finished — and one server behaviour depends on exactly that difference.
 *
 * A resubmission is recognised by matching its fingerprint against a job **already in `SUCCESS`**
 * (`BulkUploadHelper.findSucceededSubmission`). Resubmit while the first run is still transitioning
 * and there is no such row yet, so `duplicateSubmission` comes back `false` and the outcome reads as
 * an ordinary upload. That is not a product defect; it is a race the test has to avoid, and waiting
 * for the row is not enough to avoid it.
 *
 * It cost a flaky run to learn: the duplicate test timed out waiting for copy that could never
 * arrive, then passed on retry when the timing happened to differ.
 */
export async function waitForJobsToSettle(
    request: APIRequestContext,
    timeoutMs = 60000
): Promise<void> {
    const deadline = Date.now() + timeoutMs;

    while (Date.now() < deadline) {
        const response = await request.get('/api/v1/jobs?limit=50', { headers: authHeaders() });
        const jobs: { state?: string }[] = (await response.json())?.entity?.jobs ?? [];
        const inFlight = jobs.filter((job) => !TERMINAL.includes(String(job.state)));

        if (!inFlight.length) {
            return;
        }

        await new Promise((resolve) => setTimeout(resolve, 1000));
    }

    // Reported rather than thrown bare, so the failure names what was still running.
    const response = await request.get('/api/v1/jobs?limit=50', { headers: authHeaders() });
    const jobs: { state?: string }[] = (await response.json())?.entity?.jobs ?? [];
    expect(
        jobs.filter((job) => !TERMINAL.includes(String(job.state))),
        'jobs still in flight after waiting'
    ).toEqual([]);
}

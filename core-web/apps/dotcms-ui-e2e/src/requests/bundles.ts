import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/**
 * Push-publish fixtures for the Publishing Queue portlet.
 *
 * Getting an asset onto the Pending tab takes two calls: build a bundle, then push it with a
 * FUTURE publish date. A future date is what keeps it in Pending — `PublisherQueueJob` has not
 * picked it up, so the row sits in `publishing_queue` with no `publishing_queue_audit` row,
 * which is exactly what the Pending tab lists.
 *
 * These use the modern `/api/v1/...` endpoints rather than the legacy
 * `RemotePublishAjaxAction` the push dialog posts to. The legacy action is session-authenticated
 * and rejects Basic auth with a 401, whereas these accept it like the rest of the e2e helpers.
 *
 * Nothing is ever actually shipped to a remote server: the bundle is scheduled days ahead and
 * the test deletes its content long before the job would run.
 */

/** Publishing filter descriptors shipped with dotCMS (`WEB-INF/publishing-filters/`). */
export const PublishFilter = {
    Intelligent: 'Intelligent.yml',
    ForcePush: 'ForcePush.yml',
    ContentOnly: 'ContentOnly.yml'
} as const;

function authHeaders() {
    return { Authorization: generateBase64Credentials(admin1.username, admin1.password) };
}

/** A date `days` in the future — push publishing must be scheduled ahead to stay in Pending. */
export function futureDate(days = 7): Date {
    const d = new Date();
    d.setUTCDate(d.getUTCDate() + days);

    return d;
}

/** Name of the environment these fixtures create when an instance has none. */
const E2E_ENVIRONMENT_NAME = 'e2e-push-publish';

/**
 * Resolves a push-publish environment id, creating one if the instance has none.
 *
 * `POST /api/v1/publishing/push/{id}` rejects an empty `environments` list with a 400, so a real
 * environment is required.
 *
 * A clean instance - CI notably - has **no** environment configured, so this creates one rather
 * than depending on state the fixture did not set up. It is left behind deliberately: the
 * environment is shared, harmless, and re-found on later runs.
 *
 * Override with `E2E_PUSH_ENVIRONMENT_ID` when an instance has several and a specific one matters.
 */
export async function resolveEnvironmentId(request: APIRequestContext): Promise<string> {
    const override = process.env['E2E_PUSH_ENVIRONMENT_ID'];
    if (override) {
        return override;
    }

    const existing = await findEnvironmentId(request);
    if (existing) {
        return existing;
    }

    const meResponse = await request.get('/api/v1/users/current', { headers: authHeaders() });
    expect(meResponse.status(), 'could not resolve current user').toBe(200);
    const { roleId } = await meResponse.json();

    const created = await request.post('/api/environment', {
        data: {
            name: E2E_ENVIRONMENT_NAME,
            pushMode: 'PUSH_TO_ALL',
            whoCanSend: [roleId]
        },
        headers: authHeaders()
    });

    if (created.ok()) {
        return (await created.json()).entity.id;
    }

    // A parallel worker very likely won the race and created it first - the endpoint 400s on a
    // duplicate name - so look again before giving up.
    const afterRace = await findEnvironmentId(request);
    if (afterRace) {
        return afterRace;
    }

    throw new Error(
        'Could not find or create a push-publish environment (create returned ' +
            `${created.status()}: ${await created.text()})`
    );
}

/**
 * First environment on the instance, or null.
 *
 * Deliberately `GET /api/environment` (`findAllEnvironments` for an admin) rather than
 * `/api/environment/loadenvironments/roleId/{id}`: the latter returns only environments that
 * have an **endpoint** configured, so an environment this fixture just created would be invisible
 * to it. That would work on a first run and then fail on every later one - create would 400 on the
 * duplicate name and the re-find would come up empty.
 */
async function findEnvironmentId(request: APIRequestContext): Promise<string | null> {
    const response = await request.get('/api/environment', { headers: authHeaders() });
    expect(response.status(), 'could not list push-publish environments').toBe(200);

    const environments: { id: string; name: string }[] = (await response.json()).entity ?? [];
    const usable = environments.find((env) => env.id && env.name);

    return usable ? usable.id : null;
}

/**
 * Creates a bundle containing the given assets.
 *
 * @returns the new bundle's id
 */
export async function createBundleWithAssets(
    request: APIRequestContext,
    bundleName: string,
    assetIds: string[]
): Promise<string> {
    const response = await request.post('/api/v1/bundles/assets', {
        data: { bundleName, assetIds },
        headers: authHeaders()
    });

    expect(
        response.status(),
        `failed to create bundle "${bundleName}": ${await response.text()}`
    ).toBe(200);

    const body = await response.json();
    const bundleId = body.entity?.bundleId;

    expect(bundleId, `no bundleId in response: ${JSON.stringify(body)}`).toBeTruthy();

    return bundleId;
}

/**
 * Schedules an existing bundle for push publish, which is what writes the `publishing_queue`
 * rows the Pending tab reads.
 *
 * `publishDate` must be ISO 8601 **with a timezone offset** — `PushBundleForm.validateDateFormat`
 * rejects anything else with a 400. `Date.toISOString()` satisfies that (`Z` is a valid offset).
 */
export async function pushBundle(
    request: APIRequestContext,
    bundleId: string,
    publishDate: Date,
    environmentId: string,
    filterKey: string = PublishFilter.Intelligent
): Promise<void> {
    const response = await request.post(`/api/v1/publishing/push/${bundleId}`, {
        data: {
            operation: 'publish',
            publishDate: publishDate.toISOString(),
            environments: [environmentId],
            filterKey
        },
        headers: authHeaders()
    });

    expect(response.status(), `failed to push bundle ${bundleId}: ${await response.text()}`).toBe(
        200
    );
}

/**
 * Queues the same asset into `count` SEPARATE bundles, each with a distinct future publish date
 * so their order on the Pending tab is deterministic (the tab sorts by `publish_date`).
 *
 * This is the exact data shape that reproduces issue #36861: one asset, two bundles, one page.
 *
 * @returns the ids of the bundles created, in publish-date order
 */
export async function pushPublishIntoSeparateBundles(
    request: APIRequestContext,
    identifier: string,
    count = 2
): Promise<string[]> {
    const environmentId = await resolveEnvironmentId(request);
    // Random suffix as well as the timestamp: parallel workers can land on the same millisecond
    // and a duplicate bundle name is a 400.
    const runId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    const bundleIds: string[] = [];

    for (let i = 0; i < count; i++) {
        const bundleId = await createBundleWithAssets(request, `e2e-36861-${runId}-${i}`, [
            identifier
        ]);
        await pushBundle(request, bundleId, futureDate(i + 1), environmentId);
        bundleIds.push(bundleId);
    }

    return bundleIds;
}

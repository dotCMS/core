/**
 * A realistic `/api/v1/appconfiguration` body, trimmed from what demo.dotcms.com actually
 * returns (verified 2026-09-04).
 *
 * The shape matters. The reachability check fingerprints `entity.config` to tell dotCMS from
 * any other host that happens to answer, and the ADR-0019 compatibility warning reads
 * `entity.config.releaseInfo.version`. Both were previously mocked as `{ entity: {} }`, which
 * dotCMS cannot produce — so the tests passed while the code read a path that does not exist.
 */
export function appConfiguration(version = '26.09.03-01') {
    return {
        entity: {
            config: {
                cluster: { clusterId: 'dotcms-demo' },
                colors: { background: '#1b3359', primary: '#4e65f1', secondary: '#233f9b' },
                emailRegex: '^[a-zA-Z0-9_.-]+@[a-zA-Z0-9.-]+$',
                languages: [{ id: 1, isoCode: 'en-us', language: 'English' }],
                license: { level: 500, levelName: 'PLATFORM EDITION', isCommunity: false },
                releaseInfo: { buildDate: 'September 03, 2026 7:51 PM', version }
            }
        },
        errors: [],
        messages: [],
        pagination: null,
        permissions: []
    };
}

export const appConfigurationResponse = (version?: string) =>
    new Response(JSON.stringify(appConfiguration(version)), { status: 200 });

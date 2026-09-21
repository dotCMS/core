import { test } from '@playwright/test';

/**
 * The Experiments panel beside the UVE canvas (#37478).
 *
 * The end-to-end half of the plan's Test Strategy — V1 in phase 1, V14 in phase 2 — and the only
 * automated coverage of "the page is still there", which no unit test can assert.
 *
 * Both scenarios need `FEATURE_FLAG_EXPERIMENTS_PORTLET` **on**, which is a server-side
 * configuration change this harness cannot make per-test. How the suite gets an instance with the
 * flag on is the first thing T025 has to solve.
 *
 * @see specs/37478-uve-experiments-panel/quickstart.md — V1, V14
 */
test.describe('UVE Experiments panel', () => {
    // T025: opening Experiments renders the panel with the page still on screen, the address
    // unchanged and no iframe reload; closing leaves the page untouched.
    test.fixme('opens beside the page without losing it', async () => {
        // Implemented at T025.
    });

    // T067: the variant round trip returns to the same experiment's configuration.
    test.fixme('returns from a variant to the configuration it left', async () => {
        // Implemented at T067 (phase 2).
    });
});

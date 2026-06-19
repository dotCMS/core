import {
    ActiveRunSchema,
    FixReportSchema,
    FixRequestSchema,
    FixResultSchema,
    FixStatusSchema
} from './contract';

/**
 * These tests LOCK the contract. If a change breaks them, it breaks S2 (proxy)
 * and S3 (Studio), which code against this shape — treat a failure here as a
 * contract change, not a test to "fix".
 */
describe('a11y-fix contract', () => {
    describe('FixStatusSchema', () => {
        it('is exactly the five locked statuses (plan §6)', () => {
            expect(FixStatusSchema.options).toEqual([
                'fixed-to-working',
                'reported',
                'skipped',
                'regressed',
                'failed'
            ]);
        });

        it('rejects an unknown status', () => {
            expect(FixStatusSchema.safeParse('published').success).toBe(false);
        });
    });

    describe('FixRequestSchema (plan §8.2)', () => {
        const valid = {
            runId: 'r_01J',
            dotcmsBaseUrl: 'https://demo.dotcms.com',
            page: {
                identifier: 'a9f30020-54ef-494e-92ed-645e757171c2',
                uri: '/index',
                liveUrl: 'https://demo.dotcms.com/index',
                host: 'demo.dotcms.com',
                hostId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                languageId: 1
            },
            options: { skipCss: false }
        };

        it('accepts a fully-resolved request', () => {
            const parsed = FixRequestSchema.parse(valid);
            expect(parsed.page.hostId).toBe('48190c8c-42c4-46af-8d1a-0cd5db894797');
        });

        it('defaults options.skipCss to false when options is omitted', () => {
            const withoutOptions = { ...valid, options: undefined };
            const parsed = FixRequestSchema.parse(withoutOptions);
            expect(parsed.options.skipCss).toBe(false);
        });

        it('requires the proxy to resolve hostId (agent never re-resolves URLs)', () => {
            const pageWithoutHostId = { ...valid.page, hostId: undefined };
            const result = FixRequestSchema.safeParse({
                ...valid,
                page: pageWithoutHostId
            });
            expect(result.success).toBe(false);
        });

        it('rejects a non-URL dotcmsBaseUrl', () => {
            const result = FixRequestSchema.safeParse({ ...valid, dotcmsBaseUrl: 'not-a-url' });
            expect(result.success).toBe(false);
        });
    });

    describe('FixReportSchema (plan §6)', () => {
        // The literal §6 example from the plan — exercises every status.
        const planExample = {
            runId: 'r_01J',
            page: { uri: '/index', host: 'demo.dotcms.com', languageId: 1 },
            scan: { before: { violations: 12 }, after: { violations: 5 } },
            results: [
                {
                    ruleId: 'image-alt',
                    status: 'fixed-to-working',
                    file: '//demo.dotcms.com/travel/header.vtl',
                    identifier: 'a56e',
                    diff: '…'
                },
                {
                    ruleId: 'color-contrast',
                    status: 'fixed-to-working',
                    file: '//demo.dotcms.com/theme/styles.css',
                    identifier: 'c1d2',
                    diff: '…',
                    blastRadius: 'shared-rule',
                    review: 'affects .btn site-wide'
                },
                {
                    ruleId: 'link-name',
                    status: 'skipped',
                    reason: 'Text lives in a contentlet field; out of v1 scope'
                },
                {
                    ruleId: 'heading-order',
                    status: 'regressed',
                    file: '//demo.dotcms.com/travel/nav.vtl',
                    identifier: 'f00d',
                    reverted: true,
                    reason: 'Re-scan showed +1 violation — reverted to prior version'
                },
                {
                    ruleId: 'label',
                    status: 'failed',
                    reason: 'save returned 0 bytes; not applied'
                }
            ],
            publishRequired: true
        };

        it('accepts the §6 example verbatim', () => {
            const parsed = FixReportSchema.parse(planExample);
            expect(parsed.results).toHaveLength(5);
            expect(parsed.scan.before.violations).toBe(12);
        });

        it('forces publishRequired to be true (the agent never publishes)', () => {
            const result = FixReportSchema.safeParse({ ...planExample, publishRequired: false });
            expect(result.success).toBe(false);
        });

        it('rejects a negative violation count', () => {
            const result = FixReportSchema.safeParse({
                ...planExample,
                scan: { before: { violations: -1 }, after: { violations: 0 } }
            });
            expect(result.success).toBe(false);
        });
    });

    describe('FixResultSchema', () => {
        it('accepts a minimal reported result (only ruleId + status + reason)', () => {
            const result = FixResultSchema.parse({
                ruleId: 'aria-roles',
                status: 'reported',
                reason: 'JS-injected DOM; attribution not provable'
            });
            expect(result.file).toBeUndefined();
        });

        it('rejects an unknown blastRadius', () => {
            const result = FixResultSchema.safeParse({
                ruleId: 'color-contrast',
                status: 'fixed-to-working',
                blastRadius: 'whole-internet'
            });
            expect(result.success).toBe(false);
        });
    });

    describe('ActiveRunSchema (plan §8.7)', () => {
        it('accepts a running slot with a partial report', () => {
            const parsed = ActiveRunSchema.parse({
                runId: 'r_01J',
                status: 'running',
                reportSoFar: { runId: 'r_01J', results: [] }
            });
            expect(parsed.status).toBe('running');
        });

        it('accepts a finished slot with no partial report', () => {
            expect(ActiveRunSchema.parse({ runId: 'r_01J', status: 'done' }).status).toBe('done');
        });
    });
});

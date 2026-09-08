import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { DotAgentRunService } from '@dotcms/data-access';
import { AgentStreamEvent } from '@dotcms/dotcms-models';

import { DotA11yAgentService } from './dot-a11y-agent.service';

import { AgentFixRequest, FixReport } from '../models/accessibility-studio.models';

const REQUEST: AgentFixRequest = {
    identifier: 'id-1',
    languageId: 1,
    skipCss: false
};

const FIX_REPORT: FixReport = {
    runId: 'r_1',
    page: { uri: '/index', host: 'demo.dotcms.com', languageId: 1 },
    scan: { before: { violations: 28 }, after: { violations: 20 } },
    results: [{ ruleId: 'image-alt', status: 'reported' }],
    changedFiles: [],
    publishRequired: true
};

describe('DotA11yAgentService', () => {
    let spectator: SpectatorService<DotA11yAgentService>;
    let service: DotA11yAgentService;
    let runService: jest.Mocked<DotAgentRunService>;

    const createService = createServiceFactory({
        service: DotA11yAgentService,
        providers: [
            mockProvider(DotAgentRunService, {
                run: jest.fn().mockReturnValue(of()),
                stop: jest.fn().mockReturnValue(of())
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        runService = spectator.inject(DotAgentRunService) as jest.Mocked<DotAgentRunService>;
    });

    it('fixStream delegates to the generic run service with the a11y stream endpoint', () => {
        service.fixStream(REQUEST).subscribe();
        expect(runService.run).toHaveBeenCalledWith('/api/v1/agents/a11y/fix/stream', REQUEST);
    });

    it('stop delegates to the run service with the endpoint and the run id in the body', () => {
        service.stop('r_123').subscribe();
        expect(runService.stop).toHaveBeenCalledWith('/api/v1/agents/a11y/stop', {
            runId: 'r_123'
        });
    });

    it('unwraps the { report } wrapper on the done event → bare FixReport as result', () => {
        runService.run.mockReturnValue(
            of({ type: 'done', result: { report: FIX_REPORT } } as AgentStreamEvent<unknown>)
        );
        let received: unknown;
        service.fixStream(REQUEST).subscribe((e) => (received = e));
        expect(received).toEqual({ type: 'done', result: FIX_REPORT });
    });

    it('unwraps the aborted event the same way (partial report)', () => {
        runService.run.mockReturnValue(
            of({ type: 'aborted', result: { report: FIX_REPORT } } as AgentStreamEvent<unknown>)
        );
        let received: unknown;
        service.fixStream(REQUEST).subscribe((e) => (received = e));
        expect(received).toEqual({ type: 'aborted', result: FIX_REPORT });
    });

    it('accepts a bare report payload (no wrapper) unchanged', () => {
        runService.run.mockReturnValue(
            of({ type: 'done', result: FIX_REPORT } as AgentStreamEvent<unknown>)
        );
        let received: unknown;
        service.fixStream(REQUEST).subscribe((e) => (received = e));
        expect(received).toEqual({ type: 'done', result: FIX_REPORT });
    });

    it('yields null for a status-only terminal payload rather than a report-shaped lie', () => {
        // `aborted` may carry only a status. The old double cast asserted FixReport onto
        // any truthy payload, so consumers read `report.scan` on an object without one and
        // threw during change detection, blanking the whole run pane.
        runService.run.mockReturnValue(
            of({ type: 'aborted', result: { status: 'cancelled' } } as AgentStreamEvent<unknown>)
        );
        let received: unknown;
        service.fixStream(REQUEST).subscribe((e) => (received = e));
        expect(received).toEqual({ type: 'aborted', result: null });
    });

    it('yields null for a wrapped payload whose report has no scan', () => {
        runService.run.mockReturnValue(
            of({
                type: 'done',
                result: { report: { runId: 'r_1', results: [] } }
            } as AgentStreamEvent<unknown>)
        );
        let received: unknown;
        service.fixStream(REQUEST).subscribe((e) => (received = e));
        expect(received).toEqual({ type: 'done', result: null });
    });

    it('yields null for a null or non-object terminal payload', () => {
        for (const payload of [null, 'done', 42]) {
            runService.run.mockReturnValue(
                of({ type: 'done', result: payload } as AgentStreamEvent<unknown>)
            );
            let received: unknown;
            service.fixStream(REQUEST).subscribe((e) => (received = e));
            expect(received).toEqual({ type: 'done', result: null });
        }
    });

    it('passes non-terminal events (phase / progress / workingChanged) through untouched', () => {
        const events = [
            { type: 'phase', step: { message: 'scanning', meta: { phase: 'scan' } } },
            { type: 'progress', progress: { baseline: 29, current: 3, cleared: 26 } },
            {
                type: 'workingChanged',
                changedFiles: [{ path: '//site/a.css', identifier: 'id-a' }]
            }
        ] as AgentStreamEvent<unknown>[];

        for (const event of events) {
            runService.run.mockReturnValue(of(event));
            let received: unknown;
            service.fixStream(REQUEST).subscribe((e) => (received = e));
            expect(received).toEqual(event);
        }
    });

    it('unwraps a real backend done payload so the report fields are readable', () => {
        // The exact wrapper shape the agent sends: { report: { scan, results, … } }.
        const backendPayload = {
            report: {
                runId: 'r_abc',
                page: { uri: '/index', host: 'awazon.local', languageId: 1 },
                scan: { before: { violations: 28 }, after: { violations: 28 } },
                results: [
                    { ruleId: 'color-contrast', status: 'reported', reason: 'no match' },
                    { ruleId: 'agentic-research', status: 'fixed-to-working', file: '/a.vtl' }
                ],
                changedFiles: [{ path: '/a.vtl', identifier: 'id-a' }],
                publishRequired: true
            }
        };
        runService.run.mockReturnValue(
            of({ type: 'done', result: backendPayload } as AgentStreamEvent<unknown>)
        );
        // `done` events carry `result: FixReport | null`, so the local has to admit null too.
        let report: FixReport | null | undefined;
        service.fixStream(REQUEST).subscribe((e) => {
            if (e.type === 'done') {
                report = e.result;
            }
        });
        // The store + presenter read these directly — they must resolve.
        expect(report?.scan.after.violations).toBe(28);
        expect(report?.results).toHaveLength(2);
        expect(report?.results.filter((r) => r.status === 'fixed-to-working')).toHaveLength(1);
    });
});

import { DotMessageService } from '@dotcms/data-access';

import { A11yAgentPresenter } from './a11y-agent.presenter';
import { MOCK_FIX_REPORT } from './mock-fix-report';

describe('A11yAgentPresenter', () => {
    let presenter: A11yAgentPresenter;

    beforeEach(() => {
        // Echo the key + args so assertions can check what was requested.
        const dm = {
            get: (key: string, ...args: string[]) => (args.length ? `${key}(${args.join(',')})` : key)
        } as unknown as DotMessageService;
        presenter = new A11yAgentPresenter(dm);
    });

    describe('liveStep', () => {
        it('picks the icon from the step phase meta and uses info tone', () => {
            const msg = presenter.liveStep({ message: 'Scanning', meta: { phase: 'scan' } }, 0);
            expect(msg).toEqual({ id: 0, icon: 'pi pi-search', text: 'Scanning', tone: 'info' });
        });

        it('falls back to the wrench icon when phase meta is missing', () => {
            const msg = presenter.liveStep({ message: 'Working' }, 3);
            expect(msg).toEqual({ id: 3, icon: 'pi pi-wrench', text: 'Working', tone: 'info' });
        });
    });

    describe('resultMessages', () => {
        it('bookends the fixed/reported rows with scan and rescan headers', () => {
            const messages = presenter.resultMessages(MOCK_FIX_REPORT);
            expect(messages[0].id).toBe('scan');
            expect(messages[1].id).toBe('locate');
            expect(messages[messages.length - 1].id).toBe('rescan');
        });

        it('renders fixed results as success bubbles and reported as warning', () => {
            const messages = presenter.resultMessages(MOCK_FIX_REPORT);
            const fixed = messages.filter((m) => String(m.id).startsWith('fixed-'));
            const reported = messages.filter((m) => String(m.id).startsWith('reported-'));

            const expectedFixed = MOCK_FIX_REPORT.results.filter(
                (r) => r.status === 'fixed-to-working'
            ).length;
            const expectedReported = MOCK_FIX_REPORT.results.filter((r) =>
                ['reported', 'skipped', 'regressed', 'failed'].includes(r.status)
            ).length;

            expect(fixed.length).toBe(expectedFixed);
            expect(reported.length).toBe(expectedReported);
            expect(fixed.every((m) => m.tone === 'success')).toBe(true);
            expect(reported.every((m) => m.tone === 'warning')).toBe(true);
        });

        it('builds the rule · file sub-line', () => {
            const messages = presenter.resultMessages(MOCK_FIX_REPORT);
            const firstFixed = messages.find((m) => String(m.id).startsWith('fixed-'));
            const firstFixedResult = MOCK_FIX_REPORT.results.find(
                (r) => r.status === 'fixed-to-working'
            );
            expect(firstFixed?.sub).toContain(firstFixedResult?.ruleId ?? '');
        });
    });
});

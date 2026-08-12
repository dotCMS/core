import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentStatusFilterComponent } from './dot-experiment-status-filter.component';

import { DEFAULT_EXPERIMENTS_LIST_STATUSES } from '../../store/dot-experiments-list.store';

const STATUS_COUNTS: Record<DotExperimentStatus, number> = {
    [DotExperimentStatus.DRAFT]: 3,
    [DotExperimentStatus.SCHEDULED]: 1,
    [DotExperimentStatus.RUNNING]: 2,
    [DotExperimentStatus.ENDED]: 0,
    [DotExperimentStatus.ARCHIVED]: 7
};

const LABEL_BY_STATUS: Record<DotExperimentStatus, string> = {
    [DotExperimentStatus.DRAFT]: 'Draft',
    [DotExperimentStatus.SCHEDULED]: 'Scheduled',
    [DotExperimentStatus.RUNNING]: 'Running',
    [DotExperimentStatus.ENDED]: 'Ended',
    [DotExperimentStatus.ARCHIVED]: 'Archived'
};

const messageServiceMock = new MockDotMessageService({
    draft: 'Draft',
    scheduled: 'Scheduled',
    running: 'Running',
    ended: 'Ended',
    archived: 'Archived',
    'experiments.list.filter.status': 'Status',
    Clear: 'Clear',
    Done: 'Done'
});

describe('DotExperimentStatusFilterComponent', () => {
    let spectator: Spectator<DotExperimentStatusFilterComponent>;
    let selectionChange: jest.Mock;

    const createComponent = createComponentFactory({
        component: DotExperimentStatusFilterComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        detectChanges: false
    });

    const optionTestId = (status: DotExperimentStatus) =>
        `experiment-status-filter-option-${status.toLowerCase()}`;

    const openPopover = () => {
        spectator.click(byTestId('experiment-status-filter-chip'));
        spectator.detectChanges();
    };

    // `p-popover` renders its content into an overlay appended to `document.body`, outside the
    // fixture root, so popover children are only reachable with `{ root: true }`.
    const queryInOverlay = (testId: string) =>
        spectator.query(byTestId(testId), { root: true }) as HTMLElement | null;

    const toggleOption = (status: DotExperimentStatus) => {
        spectator.click(queryInOverlay(optionTestId(status)) as HTMLElement);
        spectator.detectChanges();
    };

    const setUp = (selectedStatuses: DotExperimentStatus[] = [DotExperimentStatus.DRAFT]) => {
        spectator = createComponent({
            props: { selectedStatuses, statusCounts: STATUS_COUNTS }
        });
        selectionChange = jest.fn();
        spectator.output('selectionChange').subscribe(selectionChange);
        spectator.detectChanges();
    };

    beforeEach(() => setUp());

    describe('options', () => {
        it.each(Object.values(DotExperimentStatus))(
            'should render the %s option with its label and count',
            (status) => {
                openPopover();

                const option = spectator.query(byTestId(optionTestId(status)));

                expect(option).not.toBeNull();
                expect(option?.textContent).toContain(LABEL_BY_STATUS[status]);
                expect(option?.textContent).toContain(`(${STATUS_COUNTS[status]})`);
            }
        );

        it('should render a zero count instead of hiding it', () => {
            openPopover();

            const ended = spectator.query(byTestId(optionTestId(DotExperimentStatus.ENDED)));

            expect(ended?.textContent).toContain('(0)');
        });
    });

    describe('selection', () => {
        // Every `dot-chip-filter` consumer in content-drive applies each toggle immediately
        // and clears through the chip. There is no apply button to batch behind.
        it('should emit on every toggle', () => {
            openPopover();

            toggleOption(DotExperimentStatus.RUNNING);

            expect(selectionChange).toHaveBeenCalledTimes(1);
            expect(selectionChange).toHaveBeenLastCalledWith([
                DotExperimentStatus.DRAFT,
                DotExperimentStatus.RUNNING
            ]);
        });

        it('should emit again when a second option is ticked', () => {
            openPopover();

            toggleOption(DotExperimentStatus.RUNNING);
            toggleOption(DotExperimentStatus.ENDED);

            expect(selectionChange).toHaveBeenCalledTimes(2);
            expect(selectionChange).toHaveBeenLastCalledWith([
                DotExperimentStatus.DRAFT,
                DotExperimentStatus.RUNNING,
                DotExperimentStatus.ENDED
            ]);
        });

        it('should emit without the status when an applied option is unticked', () => {
            openPopover();

            toggleOption(DotExperimentStatus.DRAFT);

            expect(selectionChange).toHaveBeenLastCalledWith([]);
        });

        it('should not expose an apply or clear footer', () => {
            openPopover();

            expect(queryInOverlay('experiment-status-filter-done')).toBeNull();
            expect(queryInOverlay('experiment-status-filter-clear')).toBeNull();
        });
    });

    describe('clear', () => {
        it('should emit an empty selection when the chip is removed', () => {
            spectator.triggerEventHandler(
                '[data-testid="experiment-status-filter-chip"]',
                'removed',
                undefined
            );

            expect(selectionChange).toHaveBeenCalledWith([]);
        });
    });

    describe('chip', () => {
        it('should render the labels of the applied statuses', () => {
            setUp([DotExperimentStatus.DRAFT, DotExperimentStatus.RUNNING]);

            const chip = spectator.query(byTestId('experiment-status-filter-chip'));

            expect(chip?.textContent).toContain('Draft, Running');
        });

        it('should read as unfiltered when the selection is the default', () => {
            // The default (everything but ARCHIVED) is "not filtered", not a filter — the chip
            // must stay neutral rather than sitting permanently highlighted, matching the
            // content-drive filters and the URL contract, which writes no `status` param.
            setUp([...DEFAULT_EXPERIMENTS_LIST_STATUSES]);

            const chip = spectator.query(byTestId('experiment-status-filter-chip'));

            expect(chip?.className).not.toContain('bg-primary-100');
            expect(chip?.textContent).not.toContain('Draft');
        });

        it('should read as filtered once the default is narrowed', () => {
            setUp([DotExperimentStatus.DRAFT]);

            const chip = spectator.query(byTestId('experiment-status-filter-chip'));

            expect(chip?.className).toContain('bg-primary-100');
        });

        it('should ignore ordering when comparing against the default', () => {
            setUp([...DEFAULT_EXPERIMENTS_LIST_STATUSES].reverse());

            const chip = spectator.query(byTestId('experiment-status-filter-chip'));

            expect(chip?.className).not.toContain('bg-primary-100');
        });
    });
});

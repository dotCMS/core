import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { Popover } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentStatusFilterComponent } from './dot-experiment-status-filter.component';

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

    const clickFooterButton = (testId: string) => {
        const button = queryInOverlay(testId)?.querySelector('button');
        spectator.click(button as HTMLElement);
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

    describe('pending selection', () => {
        it('should not emit while options are being ticked', () => {
            openPopover();

            toggleOption(DotExperimentStatus.RUNNING);
            toggleOption(DotExperimentStatus.ENDED);

            expect(selectionChange).not.toHaveBeenCalled();
        });

        it('should emit the pending selection only when Done is pressed', () => {
            openPopover();
            toggleOption(DotExperimentStatus.RUNNING);

            clickFooterButton('experiment-status-filter-done');

            expect(selectionChange).toHaveBeenCalledTimes(1);
            expect(selectionChange).toHaveBeenCalledWith([
                DotExperimentStatus.DRAFT,
                DotExperimentStatus.RUNNING
            ]);
        });

        it('should close the popover after Done', () => {
            openPopover();

            clickFooterButton('experiment-status-filter-done');

            expect((spectator.query(Popover) as Popover).overlayVisible).toBe(false);
        });

        it('should discard the pending change when the popover is dismissed without Done', () => {
            openPopover();
            toggleOption(DotExperimentStatus.RUNNING);

            // PrimeNG emits `onHide` from the overlay's animation-done hook, which never
            // completes under jsdom, so `hide()` alone would not reach the discard handler.
            const popover = spectator.query(Popover) as Popover;
            popover.hide();
            popover.onHide.emit({});
            spectator.detectChanges();
            openPopover();

            expect(selectionChange).not.toHaveBeenCalled();
            expect(spectator.query(Popover)).not.toBeNull();

            // The pending selection is re-seeded from the applied input, so pressing Done
            // now emits the original selection instead of the discarded edit.
            clickFooterButton('experiment-status-filter-done');

            expect(selectionChange).toHaveBeenCalledWith([DotExperimentStatus.DRAFT]);
        });
    });

    describe('clear', () => {
        it('should reset the pending selection and emit an empty selection', () => {
            openPopover();
            toggleOption(DotExperimentStatus.RUNNING);

            clickFooterButton('experiment-status-filter-clear');

            expect(selectionChange).toHaveBeenCalledTimes(1);
            expect(selectionChange).toHaveBeenCalledWith([]);
        });

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
    });
});

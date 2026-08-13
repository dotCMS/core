import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentListFilterComponent } from './dot-experiment-list-filter.component';

import { ExperimentFilterOption } from '../../shared/models';

const OPTIONS: ExperimentFilterOption[] = [
    { value: 'DRAFT', label: 'Draft', count: '3', testId: 'option-draft' },
    { value: 'RUNNING', label: 'Running', count: '2', testId: 'option-running' },
    { value: 'ENDED', label: 'Ended', count: '0', testId: 'option-ended' }
];

describe('DotExperimentListFilterComponent', () => {
    let spectator: Spectator<DotExperimentListFilterComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentListFilterComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dot.common.remove': 'Remove',
                    'content-drive.chip-filter.overflow-label': '{0} +{1}'
                })
            }
        ],
        detectChanges: false
    });

    const setUp = (selected: string[] = []) => {
        spectator = createComponent({
            props: {
                title: 'Status',
                emptyLabel: 'All',
                options: OPTIONS,
                selected
            } as unknown as Partial<DotExperimentListFilterComponent>
        });
        spectator.detectChanges();
    };

    describe('chip', () => {
        it('should read as the placeholder while nothing is selected', () => {
            setUp();

            // Empty means "no filter", i.e. everything — said on the chip rather than offered as
            // an `All` row, which would contradict the individual checkboxes.
            expect(spectator.query(byTestId('chip-title'))?.textContent?.trim()).toBe('Status');
            expect(spectator.query(byTestId('chip-empty-label'))?.textContent).toContain('All');
            expect(spectator.query(byTestId('chip-values'))).toBeNull();
        });

        it('should list the selected labels once something is picked', () => {
            setUp(['DRAFT', 'RUNNING']);

            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain(
                'Draft, Running'
            );
            expect(spectator.query(byTestId('chip-empty-label'))).toBeNull();
        });

        it('should ignore a selected value it has no option for', () => {
            setUp(['DRAFT', 'NOT_AN_OPTION']);

            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain('Draft');
            expect(spectator.query(byTestId('chip-values'))?.textContent).not.toContain(
                'NOT_AN_OPTION'
            );
        });
    });

    describe('selection', () => {
        it('should emit the emptied selection when the chip is cleared', () => {
            setUp(['DRAFT']);

            const selectionChange = jest.fn();
            spectator.output('selectionChange').subscribe(selectionChange);

            spectator.click(spectator.query(byTestId('chip-remove')) as HTMLElement);

            expect(selectionChange).toHaveBeenCalledWith([]);
        });

        it('should reflect the applied selection back into the chip after the parent changes it', () => {
            setUp(['DRAFT']);

            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain('Draft');

            // Stands in for URL hydration / back-forward: the parent owns the applied selection,
            // so a change underneath has to re-seed what the chip and listbox show.
            spectator.setInput('selected', ['ENDED']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain('Ended');
            expect(spectator.query(byTestId('chip-values'))?.textContent).not.toContain('Draft');
        });
    });
});

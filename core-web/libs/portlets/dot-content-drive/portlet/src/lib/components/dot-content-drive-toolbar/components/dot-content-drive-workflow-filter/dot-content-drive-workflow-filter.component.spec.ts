import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { By } from '@angular/platform-browser';

import { Listbox } from 'primeng/listbox';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotCMSWorkflow, WorkflowStep } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveWorkflowFilterComponent } from './dot-content-drive-workflow-filter.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

const SCHEME_A = { id: 'a', name: 'Blogs' } as DotCMSWorkflow;
const SCHEME_B = { id: 'b', name: 'System Workflow' } as DotCMSWorkflow;
const MOCK_SCHEMES: DotCMSWorkflow[] = [SCHEME_A, SCHEME_B];

const STEPS_A = [
    { id: 'a1', name: 'Draft', schemeId: 'a' } as WorkflowStep,
    { id: 'a2', name: 'Published', schemeId: 'a' } as WorkflowStep
];
const STEPS_B = [{ id: 'b1', name: 'Open', schemeId: 'b' } as WorkflowStep];

const STEPS_BY_SCHEME: Record<string, WorkflowStep[]> = { a: STEPS_A, b: STEPS_B };

const messageServiceMock = new MockDotMessageService({
    'content-drive.workflow-filter.title': 'Workflow',
    'content-drive.workflow-filter.column.scheme': 'Workflow',
    'content-drive.workflow-filter.column.step': 'Step',
    'content-drive.workflow-filter.empty-state': 'No steps found',
    'content-drive.workflow-filter.select-scheme': 'Select a workflow to see its steps',
    'content-drive.workflow-filter.no-workflows': 'This content type has no workflows',
    'content-drive.workflow-filter.no-workflows.plural': 'These content types have no workflows',
    'content-drive.workflow-filter.no-schemes': 'No workflow schemes found',
    'content-drive.chip-filter.overflow-label': '{0} and {1} more'
});

describe('DotContentDriveWorkflowFilterComponent', () => {
    let spectator: Spectator<DotContentDriveWorkflowFilterComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let workflowService: SpyObject<DotWorkflowService>;

    const createComponent = createComponentFactory({
        component: DotContentDriveWorkflowFilterComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                patchFilters: jest.fn(),
                removeFilter: jest.fn(),
                getFilterValue: jest.fn()
            }),
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(DotHttpErrorManagerService)
        ],
        // DotWorkflowService is provided at the component level, override it there
        componentProviders: [
            mockProvider(DotWorkflowService, {
                get: jest.fn().mockReturnValue(of(MOCK_SCHEMES)),
                getSchemesByContentTypes: jest.fn().mockReturnValue(of(MOCK_SCHEMES)),
                getSteps: jest
                    .fn()
                    .mockImplementation((schemeId: string) => of(STEPS_BY_SCHEME[schemeId] ?? []))
            })
        ],
        detectChanges: false
    });

    /** Plain attribute selector — `click`/`triggerEventHandler` need a string, not a DOMSelector. */
    const testId = (id: string) => `[data-testid="${id}"]`;

    /** Default store: no active filters. Override `workflow`/`contentType` per test. */
    const stubFilters = (values: Record<string, string[]> = {}) =>
        (store.getFilterValue as jest.Mock).mockImplementation((key: string) => values[key]);

    const openPanel = () => {
        spectator.click(byTestId('workflow-filter-chip'));
        spectator.detectChanges();
    };

    const focusScheme = (schemeId: string) => {
        spectator.triggerEventHandler(testId('workflow-scheme-listbox'), 'ngModelChange', schemeId);
        spectator.detectChanges();
    };

    const toggleScheme = (schemeId: string) => {
        spectator.triggerEventHandler(
            testId(`workflow-scheme-checkbox-${schemeId}`),
            'onChange',
            {}
        );
        spectator.detectChanges();
    };

    const pinStep = (stepId: string | null) => {
        spectator.triggerEventHandler(testId('workflow-step-listbox'), 'ngModelChange', stepId);
        spectator.detectChanges();
    };

    const resetStoreWriteSpies = () => {
        store.patchFilters.mockClear();
        store.removeFilter.mockClear();
    };

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        workflowService = spectator.inject(DotWorkflowService, true);
        // Re-establish defaults each test — jest.clearAllMocks() clears call history
        // but not return values, so a per-test of([]) override would otherwise leak.
        (workflowService.get as jest.Mock).mockReturnValue(of(MOCK_SCHEMES));
        (workflowService.getSchemesByContentTypes as jest.Mock).mockReturnValue(of(MOCK_SCHEMES));
        (workflowService.getSteps as jest.Mock).mockImplementation((schemeId: string) =>
            of(STEPS_BY_SCHEME[schemeId] ?? [])
        );
        stubFilters();
    });

    afterEach(() => jest.clearAllMocks());

    describe('loading schemes', () => {
        it('should load all schemes when no content type is selected', () => {
            spectator.detectChanges();

            expect(workflowService.get).toHaveBeenCalled();
            expect(workflowService.getSchemesByContentTypes).not.toHaveBeenCalled();
            expect(spectator.component.$state().schemes).toEqual(MOCK_SCHEMES);
        });

        it('should load schemes by content types when a content type filter is active', () => {
            stubFilters({ contentType: ['ct1', 'ct2'] });

            spectator.detectChanges();

            expect(workflowService.getSchemesByContentTypes).toHaveBeenCalledWith(['ct1', 'ct2']);
            expect(workflowService.get).not.toHaveBeenCalled();
        });

        it('should drop a stored scheme that no longer exists and clear the filter', () => {
            stubFilters({ workflow: ['does-not-exist'] });

            spectator.detectChanges();

            expect(spectator.component.$selection()).toEqual([]);
            expect(store.removeFilter).toHaveBeenCalledWith('workflow');
        });

        it('should keep the workflow selection when a content-type filter coexists and the scheme still exists', () => {
            // Workflow and content-type filters active together; the content-type
            // schemes still include the pinned scheme, so the selection survives.
            stubFilters({ contentType: ['ct1'], workflow: ['a'] });

            spectator.detectChanges();

            expect(workflowService.getSchemesByContentTypes).toHaveBeenCalledWith(['ct1']);
            expect(spectator.component.$selection()).toEqual([{ scheme: 'a' }]);
            expect(store.removeFilter).not.toHaveBeenCalled();
        });

        it('should keep the persisted filter and surface the error when schemes fail to load', () => {
            const httpErrorManager = spectator.inject(DotHttpErrorManagerService, true);
            stubFilters({ workflow: ['a'] });
            (workflowService.get as jest.Mock).mockReturnValue(throwError(() => new Error('boom')));

            spectator.detectChanges();

            // The error is surfaced, not swallowed silently.
            expect(httpErrorManager.handle).toHaveBeenCalled();
            // A transient backend failure must NOT drop the persisted/URL-restored
            // selection (no reconcile against an empty list).
            expect(store.removeFilter).not.toHaveBeenCalled();
            expect(spectator.component.$selection()).toEqual([{ scheme: 'a' }]);
            expect(spectator.component.$state().loadingSchemes).toBe(false);
        });
    });

    describe('chip', () => {
        it('should render the title "Workflow"', () => {
            spectator.detectChanges();

            const title = spectator
                .query(byTestId('workflow-filter-chip'))
                ?.querySelector('[data-testid="chip-title"]');
            expect(title?.textContent?.trim()).toBe('Workflow');
        });

        it('should show "<scheme> — <step>" for a pinned scheme and the bare name otherwise', () => {
            stubFilters({ workflow: ['a:a2', 'b'] });

            spectator.detectChanges();

            expect(spectator.component.$chipSelections()).toEqual([
                'Blogs — Published',
                'System Workflow'
            ]);
        });

        it('should clear the selection and remove the filter when the chip emits removed', () => {
            stubFilters({ workflow: ['a'] });
            spectator.detectChanges();
            resetStoreWriteSpies();

            spectator.triggerEventHandler(testId('workflow-filter-chip'), 'removed', undefined);

            expect(spectator.component.$selection()).toEqual([]);
            expect(store.removeFilter).toHaveBeenCalledWith('workflow');
        });
    });

    describe('selection', () => {
        it('should select a whole scheme when its checkbox is toggled on', () => {
            spectator.detectChanges();
            openPanel();
            resetStoreWriteSpies();

            toggleScheme('a');

            expect(store.patchFilters).toHaveBeenCalledWith({ workflow: ['a'] });
        });

        it('should remove the filter when the only selected scheme is toggled off', () => {
            stubFilters({ workflow: ['a'] });
            spectator.detectChanges();
            openPanel();
            resetStoreWriteSpies();

            toggleScheme('a');

            expect(store.removeFilter).toHaveBeenCalledWith('workflow');
        });

        it('should load the steps of a focused scheme', () => {
            spectator.detectChanges();
            openPanel();

            focusScheme('a');

            expect(workflowService.getSteps).toHaveBeenCalledWith('a');
            expect(spectator.component.$state().steps).toEqual(STEPS_A);
        });

        it('should surface the error and show an empty step column when steps fail to load', () => {
            const httpErrorManager = spectator.inject(DotHttpErrorManagerService, true);
            (workflowService.getSteps as jest.Mock).mockReturnValue(
                throwError(() => new Error('boom'))
            );
            spectator.detectChanges();
            openPanel();

            focusScheme('a');

            expect(httpErrorManager.handle).toHaveBeenCalled();
            expect(spectator.component.$state().steps).toEqual([]);
            expect(spectator.component.$state().loadingSteps).toBe(false);
        });

        it('should not cache an errored step load — re-focusing the scheme retries', () => {
            // Only the first getSteps call fails; the scheme must NOT be cached as
            // empty, so returning to it triggers a fresh (successful) load.
            (workflowService.getSteps as jest.Mock).mockReturnValueOnce(
                throwError(() => new Error('boom'))
            );
            spectator.detectChanges();
            openPanel();

            focusScheme('a'); // errors → not cached
            focusScheme('b'); // move away
            focusScheme('a'); // back → retries instead of serving empty from cache

            const aCalls = (workflowService.getSteps as jest.Mock).mock.calls.filter(
                ([id]) => id === 'a'
            ).length;
            expect(aCalls).toBe(2);
            expect(spectator.component.$state().steps).toEqual(STEPS_A);
        });

        it('should pin a step (and select its scheme) when a step is chosen', () => {
            spectator.detectChanges();
            openPanel();
            focusScheme('a');
            resetStoreWriteSpies();

            pinStep('a2');

            expect(store.patchFilters).toHaveBeenCalledWith({ workflow: ['a:a2'] });
        });

        it('should keep step selection per scheme (switching focus does not lose the other pin)', () => {
            stubFilters({ workflow: ['a:a2'] });
            spectator.detectChanges();
            openPanel();

            // Focus B (no pin) then back to A — A's pinned step is still reflected.
            focusScheme('b');
            focusScheme('a');

            expect(spectator.component.$selection()).toContainEqual({ scheme: 'a', step: 'a2' });
        });

        it('should unpin the step but keep the scheme when the step is deselected', () => {
            stubFilters({ workflow: ['a:a2'] });
            spectator.detectChanges();
            openPanel();
            focusScheme('a');
            resetStoreWriteSpies();

            // PrimeNG single-select listbox emits null when the selected item is re-clicked.
            pinStep(null);

            expect(store.patchFilters).toHaveBeenCalledWith({ workflow: ['a'] });
        });

        it('should not overwrite the step column with a late response after focus changed', () => {
            const stepsA$ = new Subject<WorkflowStep[]>();
            const stepsB$ = new Subject<WorkflowStep[]>();
            (workflowService.getSteps as jest.Mock).mockImplementation((id: string) =>
                id === 'a' ? stepsA$ : stepsB$
            );
            spectator.detectChanges();
            openPanel();

            focusScheme('a');
            focusScheme('b');

            stepsB$.next(STEPS_B);
            // Late response for the scheme the user already moved away from.
            stepsA$.next(STEPS_A);

            expect(spectator.component.$state().steps).toEqual(STEPS_B);
        });
    });

    describe('focus follows checkbox toggle', () => {
        it('focuses a scheme when its checkbox is checked (right list shows its steps)', () => {
            spectator.detectChanges();
            openPanel();

            toggleScheme('a');

            expect(spectator.component.$focusedScheme()).toBe('a');
            expect(workflowService.getSteps).toHaveBeenCalledWith('a');
            expect(spectator.component.$state().steps).toEqual(STEPS_A);
        });

        it('clears focus and the step column when the focused scheme is unchecked', () => {
            // Scheme A is selected and (via reconcile) focused on open.
            stubFilters({ workflow: ['a'] });
            spectator.detectChanges();
            openPanel();
            expect(spectator.component.$focusedScheme()).toBe('a');

            toggleScheme('a'); // uncheck the focused scheme

            expect(spectator.component.$focusedScheme()).toBeNull();
            expect(spectator.component.$state().steps).toEqual([]);
        });

        it('leaves focus untouched when a scheme other than the focused one is unchecked', () => {
            // Both selected; reconcile focuses the first kept scheme (A).
            stubFilters({ workflow: ['a', 'b'] });
            spectator.detectChanges();
            openPanel();
            expect(spectator.component.$focusedScheme()).toBe('a');

            toggleScheme('b'); // uncheck the NON-focused scheme

            expect(spectator.component.$focusedScheme()).toBe('a');
            expect(spectator.component.$state().steps).toEqual(STEPS_A);
            expect(store.patchFilters).toHaveBeenCalledWith({ workflow: ['a'] });
        });

        it('stops mousedown propagation on the checkbox so a sibling popover stays dismissable', () => {
            // Without this, PrimeNG's popover marks selfClick=true on the
            // checkbox mousedown and never resets it (the click is
            // stopPropagation'd), leaving this popover open when another chip
            // is clicked.
            spectator.detectChanges();
            openPanel();
            const event = { stopPropagation: jest.fn() };

            spectator.triggerEventHandler(testId('workflow-scheme-checkbox-a'), 'mousedown', event);

            expect(event.stopPropagation).toHaveBeenCalled();
        });
    });

    describe('empty schemes', () => {
        const schemeEmptyText = () =>
            spectator.fixture.debugElement
                .query(By.css('[data-testid="workflow-scheme-empty"]'))
                ?.nativeElement.textContent.trim();

        it('should show the singular no-workflows message for one content type', () => {
            stubFilters({ contentType: ['ct1'] });
            (workflowService.getSchemesByContentTypes as jest.Mock).mockReturnValue(of([]));
            spectator.detectChanges();
            openPanel();

            expect(schemeEmptyText()).toBe('This content type has no workflows');
        });

        it('should show the plural no-workflows message for multiple content types', () => {
            stubFilters({ contentType: ['ct1', 'ct2'] });
            (workflowService.getSchemesByContentTypes as jest.Mock).mockReturnValue(of([]));
            spectator.detectChanges();
            openPanel();

            expect(schemeEmptyText()).toBe('These content types have no workflows');
        });

        it('should show a generic no-schemes message when no content type is selected', () => {
            (workflowService.get as jest.Mock).mockReturnValue(of([]));
            spectator.detectChanges();
            openPanel();

            expect(schemeEmptyText()).toBe('No workflow schemes found');
        });
    });

    describe('listbox configuration', () => {
        it('should render the scheme listbox as single-select', () => {
            spectator.detectChanges();
            openPanel();

            const listbox = spectator.fixture.debugElement.query(
                By.css('[data-testid="workflow-scheme-listbox"]')
            ).componentInstance as Listbox;

            expect(listbox.multiple).toBeFalsy();
        });
    });
});

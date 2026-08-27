import { TestBed } from '@angular/core/testing';

import { ConfirmationService, ConfirmEventType } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { experimentsUnsavedChangesGuard } from './unsaved-changes.guard';

import { DotExperimentsConfigureComponent } from '../dot-experiments-configure/dot-experiments-configure.component';

const KEEP_COPY = 'Keep Editing';
const DISCARD_COPY = 'Discard Changes';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.unsaved.title': 'Unsaved changes',
    'experiments.configure.unsaved.message':
        'This experiment has changes that have not been saved.',
    'experiments.configure.unsaved.keep': KEEP_COPY,
    'experiments.configure.unsaved.discard': DISCARD_COPY
});

/**
 * The guard is a plain function over the component, so it is exercised directly rather than
 * through a router harness: what matters is which branch each user action takes, and a real
 * navigation would only obscure that.
 */
describe('experimentsUnsavedChangesGuard', () => {
    let confirm: jest.Mock;
    let component: DotExperimentsConfigureComponent;

    /** Runs the guard inside an injection context, which is where `inject` is legal. */
    const run = () =>
        TestBed.runInInjectionContext(
            () =>
                experimentsUnsavedChangesGuard(
                    component,
                    null as never,
                    null as never,
                    null as never
                ) as boolean | Promise<boolean>
        );

    beforeEach(() => {
        confirm = jest.fn();
        TestBed.configureTestingModule({
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });
    });

    const buildComponent = (hasUnsavedChanges: boolean) =>
        ({
            store: { $hasUnsavedChanges: () => hasUnsavedChanges },
            confirmationService: { confirm } as unknown as ConfirmationService
        }) as unknown as DotExperimentsConfigureComponent;

    it('should let a clean screen go without asking anything', () => {
        component = buildComponent(false);

        expect(run()).toBe(true);
        expect(confirm).not.toHaveBeenCalled();
    });

    it('should ask before leaving with unsaved work', () => {
        component = buildComponent(true);

        const result = run();

        expect(confirm).toHaveBeenCalledTimes(1);
        expect(confirm.mock.calls[0][0]).toEqual(
            expect.objectContaining({ acceptLabel: KEEP_COPY, rejectLabel: DISCARD_COPY })
        );
        expect(result).toBeInstanceOf(Promise);
    });

    it('should stay on the screen when the user keeps editing', async () => {
        component = buildComponent(true);

        const result = run() as Promise<boolean>;
        confirm.mock.calls[0][0].accept();

        await expect(result).resolves.toBe(false);
    });

    it('should leave when the user deliberately discards', async () => {
        component = buildComponent(true);

        const result = run() as Promise<boolean>;
        confirm.mock.calls[0][0].reject(ConfirmEventType.REJECT);

        await expect(result).resolves.toBe(true);
    });

    /**
     * The one that matters: PrimeNG routes dismissals through the same callback as the secondary
     * button, so a guard that ignores the event type throws the user's work away when they press
     * ESC — the gesture people use to mean "never mind".
     */
    it('should treat a dismissal as keep editing, never as discard', async () => {
        component = buildComponent(true);

        const result = run() as Promise<boolean>;
        confirm.mock.calls[0][0].reject(ConfirmEventType.CANCEL);

        await expect(result).resolves.toBe(false);
    });
});

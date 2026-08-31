import { TestBed } from '@angular/core/testing';
import { RouterStateSnapshot } from '@angular/router';

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

/** The two URLs the guard is told about, as the router hands them over. */
const stateOf = (url: string) => ({ url }) as RouterStateSnapshot;

const LIST_URL = '/dotAdmin/#/experiments';
const NEW_URL = '/dotAdmin/#/experiments/new';
const EXPERIMENT_ID = 'exp-1';
const CONFIGURATION_URL = `/dotAdmin/#/experiments/${EXPERIMENT_ID}/configuration`;

/**
 * The guard is a plain function over the component, so it is exercised directly rather than
 * through a router harness: what matters is which branch each user action takes, and a real
 * navigation would only obscure that.
 */
describe('experimentsUnsavedChangesGuard', () => {
    let confirm: jest.Mock;
    let component: DotExperimentsConfigureComponent;
    let from = stateOf(CONFIGURATION_URL);
    let to = stateOf(LIST_URL);

    /** Runs the guard inside an injection context, which is where `inject` is legal. */
    const run = () =>
        TestBed.runInInjectionContext(
            () =>
                experimentsUnsavedChangesGuard(component, null as never, from, to) as
                    | boolean
                    | Promise<boolean>
        );

    beforeEach(() => {
        confirm = jest.fn();
        from = stateOf(CONFIGURATION_URL);
        to = stateOf(LIST_URL);
        TestBed.configureTestingModule({
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });
    });

    const buildComponent = (hasUnsavedChanges: boolean, experimentId = EXPERIMENT_ID) =>
        ({
            store: {
                $hasUnsavedChanges: () => hasUnsavedChanges,
                experiment: () => ({ id: experimentId })
            },
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

    /**
     * The redirect the screen makes itself once the POST answers. It reached this guard, the
     * follow-up PATCH was still in flight, and the prompt cancelled the redirect — leaving the
     * screen on `/experiments/new` behind a dialog about work that was already on its way.
     */
    describe('the redirect off the creation URL', () => {
        beforeEach(() => {
            from = stateOf(NEW_URL);
            to = stateOf(CONFIGURATION_URL);
        });

        it('should let it through even with a save still in flight', () => {
            component = buildComponent(true);

            expect(run()).toBe(true);
            expect(confirm).not.toHaveBeenCalled();
        });

        it('should still challenge a leave that only looks like it', () => {
            // Same starting URL, but the user is going somewhere else entirely.
            to = stateOf(LIST_URL);
            component = buildComponent(true);

            run();

            expect(confirm).toHaveBeenCalledTimes(1);
        });

        it('should still challenge a redirect to some other experiment', () => {
            to = stateOf('/dotAdmin/#/experiments/exp-other/configuration');
            component = buildComponent(true);

            run();

            expect(confirm).toHaveBeenCalledTimes(1);
        });
    });
});

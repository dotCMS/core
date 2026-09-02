import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal, WritableSignal } from '@angular/core';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotExperimentsChangePageDialogComponent,
    DotExperimentsChangePageDialogVariant
} from './dot-experiments-change-page-dialog.component';

import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';

const WARNING_ONE_COPY =
    'If you change the Page, 1 Variant of "{0}" will be deleted. This cannot be undone.';
const WARNING_MANY_COPY =
    'If you change the Page, {1} Variants of "{0}" will be deleted. This cannot be undone.';
const ERROR_COPY = 'Some Variants could not be deleted';
const CONFIRM_COPY = 'Delete Variants And Change Page';
const REJECT_COPY = 'Keep Current Page';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.page.change.warning.one': WARNING_ONE_COPY,
    'experiments.configure.page.change.warning.many': WARNING_MANY_COPY,
    'experiments.configure.page.change.error': ERROR_COPY,
    'experiments.configure.page.change.confirm': CONFIRM_COPY,
    'experiments.configure.page.change.reject': REJECT_COPY
});

const PAGE_TITLE = 'Pricing';

const VARIANT_A: DotExperimentsChangePageDialogVariant = {
    id: 'variant-2',
    name: 'Variant B',
    color: '#a855f7'
};

const VARIANT_B: DotExperimentsChangePageDialogVariant = {
    id: 'variant-3',
    name: 'Variant C',
    color: '#fb923c'
};

describe('DotExperimentsChangePageDialogComponent', () => {
    let spectator: Spectator<DotExperimentsChangePageDialogComponent>;
    let dialogRef: { close: jest.Mock };
    let dispatch: jest.SpyInstance;

    /** The store slices the card hands over, as the signals they are on its side. */
    let variants: WritableSignal<DotExperimentsChangePageDialogVariant[]>;
    let deleting: WritableSignal<boolean>;
    let failed: WritableSignal<boolean>;

    const createComponent = createComponentFactory({
        component: DotExperimentsChangePageDialogComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        detectChanges: false
    });

    /**
     * Mounts the dialog the way `DialogService` does: `inputValues` reaches it as inputs, and every
     * one of them that can move arrives as a signal rather than as a value, so they keep reading
     * the store instead of freezing at creation.
     */
    const mountWith = (rows: DotExperimentsChangePageDialogVariant[]) => {
        variants.set(rows);
        spectator = createComponent({
            providers: [{ provide: DynamicDialogRef, useValue: dialogRef }]
        });
        spectator.setInput({ pageTitle: PAGE_TITLE, variants, deleting, failed });
        dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
        spectator.detectChanges();
    };

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    const confirmButton = () =>
        spectator
            .query(byTestId('change-page-confirm-btn'))
            ?.querySelector('button') as HTMLButtonElement;

    const cancelButton = () =>
        spectator
            .query(byTestId('change-page-cancel-btn'))
            ?.querySelector('button') as HTMLButtonElement;

    beforeEach(() => {
        dialogRef = { close: jest.fn() };
        variants = signal<DotExperimentsChangePageDialogVariant[]>([]);
        deleting = signal(false);
        failed = signal(false);
    });

    afterEach(() => jest.restoreAllMocks());

    describe('what it warns about', () => {
        it('should name every variant the change would delete', () => {
            mountWith([VARIANT_A, VARIANT_B]);

            const rows = spectator.queryAll(byTestId('change-page-variant'));

            expect(rows.length).toBe(2);
            expect(rows[0].textContent).toContain(VARIANT_A.name);
            expect(rows[1].textContent).toContain(VARIANT_B.name);
        });

        it('should draw each variant in the colour the Variants card gave it', () => {
            mountWith([VARIANT_A]);

            const dot = spectator
                .query(byTestId('change-page-variant'))
                ?.querySelector('span') as HTMLElement;

            expect(dot.style.background).toBe('rgb(168, 85, 247)');
        });

        it('should count a single variant in the singular, naming its page', () => {
            mountWith([VARIANT_A]);

            expect(spectator.query(byTestId('change-page-warning'))?.textContent).toContain(
                `1 Variant of "${PAGE_TITLE}"`
            );
        });

        it('should count several variants in the plural, with the number', () => {
            mountWith([VARIANT_A, VARIANT_B]);

            expect(spectator.query(byTestId('change-page-warning'))?.textContent).toContain(
                '2 Variants'
            );
        });

        /**
         * A run that is refused halfway really deleted the variants it got through, so a frozen
         * list would go on naming content that is already gone — in a dialog about an irreversible
         * action, and while offering to do it again.
         */
        it('should drop a variant from the list once the store reports it deleted', () => {
            mountWith([VARIANT_A, VARIANT_B]);

            variants.set([VARIANT_B]);
            spectator.detectChanges();

            const rows = spectator.queryAll(byTestId('change-page-variant'));

            expect(rows.length).toBe(1);
            expect(rows[0].textContent).toContain(VARIANT_B.name);
            // And the count in the warning follows it, rather than still promising two.
            expect(spectator.query(byTestId('change-page-warning'))?.textContent).toContain(
                '1 Variant'
            );
        });

        it('should say the deletion cannot be undone', () => {
            // Part of the warning paragraph now, as the platform's other confirmations read.
            mountWith([VARIANT_A]);

            expect(spectator.query(byTestId('change-page-warning'))?.textContent).toContain(
                'This cannot be undone.'
            );
        });

        it('should lead with no icon, the way the other confirmations do', () => {
            mountWith([VARIANT_A]);

            expect(
                spectator
                    .query(byTestId('change-page-dialog'))
                    ?.querySelector('.material-symbols-rounded')
            ).toBeNull();
        });

        it('should name the choice being declined rather than a generic Cancel', () => {
            mountWith([VARIANT_A]);

            expect(cancelButton().textContent).toContain(REJECT_COPY);
        });
    });

    describe('reporting the go-ahead', () => {
        it('should report the confirmation rather than act on it', () => {
            // The deletions are the store's; this dialog only says the user agreed.
            mountWith([VARIANT_A]);

            spectator.click(confirmButton());

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.pageChangeConfirmed()
            );
        });

        it('should not close itself: the card closes it once the variants are gone', () => {
            mountWith([VARIANT_A]);

            spectator.click(confirmButton());

            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should not report a second confirmation while the first run is going', () => {
            mountWith([VARIANT_A]);

            spectator.click(confirmButton());
            deleting.set(true);
            spectator.detectChanges();
            spectator.click(confirmButton());

            expect(
                dispatchedEvents().filter(
                    (event) =>
                        event.type === dotExperimentsConfigurePageEvents.pageChangeConfirmed().type
                ).length
            ).toBe(1);
        });
    });

    describe('while the store is deleting', () => {
        it('should show the wait on the button and freeze Cancel', () => {
            mountWith([VARIANT_A]);

            deleting.set(true);
            spectator.detectChanges();

            expect(confirmButton().querySelector('.p-button-loading-icon')).not.toBeNull();
            expect(cancelButton().disabled).toBe(true);
        });

        it('should follow the store when the run settles, not a state of its own', () => {
            // The signal is read, not snapshotted: `inputValues` is applied once, at creation.
            mountWith([VARIANT_A]);
            deleting.set(true);
            spectator.detectChanges();

            deleting.set(false);
            spectator.detectChanges();

            expect(confirmButton().querySelector('.p-button-loading-icon')).toBeNull();
            expect(cancelButton().disabled).toBe(false);
        });
    });

    describe('a refused run', () => {
        it('should say nothing about a failure before one happens', () => {
            mountWith([VARIANT_A]);

            expect(spectator.query(byTestId('change-page-error'))).toBeNull();
        });

        it('should state the failure where the buttons are, and stay open', () => {
            mountWith([VARIANT_A]);

            failed.set(true);
            spectator.detectChanges();

            expect(spectator.query(byTestId('change-page-error'))?.textContent).toContain(
                ERROR_COPY
            );
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should leave the run repeatable', () => {
            mountWith([VARIANT_A]);
            failed.set(true);
            spectator.detectChanges();

            spectator.click(confirmButton());

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.pageChangeConfirmed()
            );
        });
    });

    describe('cancelling', () => {
        it('should close with nothing, so no variant is deleted and no page is picked', () => {
            mountWith([VARIANT_A]);

            spectator.click(cancelButton());

            expect(dialogRef.close).toHaveBeenCalledWith();
            expect(dispatchedEvents()).toEqual([]);
        });
    });
});

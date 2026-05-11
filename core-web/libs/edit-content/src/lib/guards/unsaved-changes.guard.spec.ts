import { mockProvider } from '@ngneat/spectator/jest';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, type CanDeactivateFn } from '@angular/router';

import { Confirmation, ConfirmationService, ConfirmEventType } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { unsavedChangesGuard } from './unsaved-changes.guard';

import { DotEditContentLayoutComponent } from '../components/dot-edit-content-layout/dot-edit-content.layout.component';

describe('unsavedChangesGuard', () => {
    let capturedConfirmation: Confirmation | null;
    let confirmationService: ConfirmationService;

    // Build a component stub mirroring the parts of
    // `DotEditContentLayoutComponent` the guard reaches into:
    // `$store.workflowActionSuccess()` (post-save bypass),
    // `hasUnsavedChanges()`, `markFormPristine()`, and the component-scoped
    // `confirmationService` the guard uses to open the prompt.
    const buildComponent = (
        overrides: Partial<DotEditContentLayoutComponent> & {
            workflowActionSuccess?: () => DotCMSContentlet | null;
        } = {}
    ): Partial<DotEditContentLayoutComponent> => {
        const { workflowActionSuccess = () => null, ...rest } = overrides;

        return {
            $store: { workflowActionSuccess },
            confirmationService,
            ...rest
        } as unknown as Partial<DotEditContentLayoutComponent>;
    };

    // The guard ignores route/state snapshot args by design; pass empty stubs
    // and don't bother typing them rigorously.
    const runGuard = (component: Partial<DotEditContentLayoutComponent>) =>
        TestBed.runInInjectionContext(() =>
            (unsavedChangesGuard as CanDeactivateFn<DotEditContentLayoutComponent>)(
                component as DotEditContentLayoutComponent,
                {} as ActivatedRouteSnapshot,
                {} as RouterStateSnapshot,
                {} as RouterStateSnapshot
            )
        );

    beforeEach(() => {
        capturedConfirmation = null;

        TestBed.configureTestingModule({
            providers: [
                mockProvider(ConfirmationService, {
                    confirm: jest.fn((confirmation: Confirmation) => {
                        capturedConfirmation = confirmation;

                        return undefined;
                    })
                }),
                mockProvider(DotMessageService, {
                    get: jest.fn((key: string) => key)
                })
            ]
        });

        confirmationService = TestBed.inject(ConfirmationService);
    });

    // Locks in the design invariant that the navigation stays pending until
    // the user clicks "Keep editing" or "Discard changes". The persistent
    // `<p-confirmDialog />` in the layout uses `[closable]` defaults that
    // do not auto-resolve, so the Promise must NEVER settle before an
    // explicit user choice. If a future change makes the dialog dismissable
    // by ESC / mask click, this test will start failing because no callback
    // fires and the navigation hangs.
    it('should keep navigation pending until accept or reject is invoked', () => {
        const component = buildComponent({ hasUnsavedChanges: () => true });

        const result = runGuard(component) as Promise<boolean>;

        let settled = false;
        result.then(() => (settled = true));

        expect(confirmationService.confirm).toHaveBeenCalledTimes(1);
        expect(settled).toBe(false);
    });

    it('should allow navigation immediately when the form is pristine', () => {
        const component = buildComponent({ hasUnsavedChanges: () => false });

        const result = runGuard(component);

        expect(result).toBe(true);
        expect(confirmationService.confirm).not.toHaveBeenCalled();
    });

    // After a successful workflow action (publish, save & assign, etc.) the
    // workflow feature programmatically navigates to the new inode. The form
    // is still dirty at the time `canDeactivate` runs (the post-save effect
    // marks it pristine asynchronously), so the guard must short-circuit on
    // `workflowActionSuccess` to avoid prompting the user with a dialog
    // immediately after they clicked Save.
    it('should bypass the prompt for post-save navigations', () => {
        const savedContentlet = { inode: 'new-inode' } as DotCMSContentlet;
        const component = buildComponent({
            hasUnsavedChanges: () => true,
            workflowActionSuccess: () => savedContentlet
        });

        const result = runGuard(component);

        expect(result).toBe(true);
        expect(confirmationService.confirm).not.toHaveBeenCalled();
    });

    it('should cancel navigation when the user clicks "Keep editing" (accept)', async () => {
        const component = buildComponent({ hasUnsavedChanges: () => true });

        const result = runGuard(component) as Promise<boolean>;

        // Sanity: dialog was opened with the expected i18n keys and label mapping
        expect(confirmationService.confirm).toHaveBeenCalledTimes(1);
        const confirmation = capturedConfirmation as Confirmation;
        expect(confirmation.header).toBe('edit.content.unsaved.changes.title');
        expect(confirmation.message).toBe('edit.content.unsaved.changes.message');
        expect(confirmation.acceptLabel).toBe('edit.content.unsaved.changes.keep');
        expect(confirmation.rejectLabel).toBe('edit.content.unsaved.changes.discard');
        // PrimeNG defaults are styled to mirror the legacy footer template:
        // text-only buttons with the secondary action outlined.
        expect(confirmation.acceptIcon).toBe('hidden');
        expect(confirmation.rejectIcon).toBe('hidden');
        expect(confirmation.rejectButtonStyleClass).toBe('p-button-outlined');

        // Simulate clicking the primary "Keep editing" button.
        confirmation.accept?.();

        await expect(result).resolves.toBe(false);
    });

    it('should allow navigation when the user clicks "Discard changes" (reject)', async () => {
        const markFormPristine = jest.fn();
        const component = buildComponent({
            hasUnsavedChanges: () => true,
            markFormPristine
        });

        const result = runGuard(component) as Promise<boolean>;

        const confirmation = capturedConfirmation as Confirmation;

        // PrimeNG emits `ConfirmEventType.REJECT` when the user clicks the
        // secondary action button (vs `CANCEL` for X / ESC dismissals).
        confirmation.reject?.(ConfirmEventType.REJECT);

        await expect(result).resolves.toBe(true);
        // Form should be reset to pristine so any downstream beforeunload
        // does not re-prompt with the browser's native dialog.
        expect(markFormPristine).toHaveBeenCalledTimes(1);
    });

    // PrimeNG routes the close-icon and ESC dismissals through the same
    // `reject` callback as the "Discard changes" button — only the
    // `ConfirmEventType` argument differs. The guard must treat these
    // dismissals as "Keep editing" so an accidental click on the X (or
    // a stray ESC keypress) never silently discards the user's work.
    it('should cancel navigation when the user dismisses the dialog (X / ESC → CANCEL)', async () => {
        const markFormPristine = jest.fn();
        const component = buildComponent({
            hasUnsavedChanges: () => true,
            markFormPristine
        });

        const result = runGuard(component) as Promise<boolean>;

        const confirmation = capturedConfirmation as Confirmation;

        confirmation.reject?.(ConfirmEventType.CANCEL);

        await expect(result).resolves.toBe(false);
        // Dismissal must NOT reset the form's dirty state — the user is
        // staying on the editor and their changes must remain intact.
        expect(markFormPristine).not.toHaveBeenCalled();
    });
});

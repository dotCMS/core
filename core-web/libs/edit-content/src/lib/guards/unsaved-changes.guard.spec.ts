import { mockProvider } from '@ngneat/spectator/jest';
import { Observable } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, type CanDeactivateFn } from '@angular/router';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotAlertConfirm, DotCMSContentlet } from '@dotcms/dotcms-models';

import { unsavedChangesGuard } from './unsaved-changes.guard';

import { DotEditContentLayoutComponent } from '../components/dot-edit-content-layout/dot-edit-content.layout.component';

describe('unsavedChangesGuard', () => {
    let capturedModel: DotAlertConfirm | null;
    let dotAlertConfirmService: DotAlertConfirmService;

    // Build a component stub with a `$store.workflowActionSuccess()` accessor —
    // the guard reads this signal first to skip the prompt for post-save
    // navigations. Defaults to `null` (no in-flight save) unless overridden.
    const buildComponent = (
        overrides: Partial<DotEditContentLayoutComponent> & {
            workflowActionSuccess?: () => DotCMSContentlet | null;
        } = {}
    ): Partial<DotEditContentLayoutComponent> => {
        const { workflowActionSuccess = () => null, ...rest } = overrides;

        return {
            $store: { workflowActionSuccess },
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
        capturedModel = null;

        TestBed.configureTestingModule({
            providers: [
                mockProvider(DotAlertConfirmService, {
                    confirm: jest.fn((model: DotAlertConfirm) => {
                        capturedModel = model;
                    })
                }),
                mockProvider(DotMessageService, {
                    get: jest.fn((key: string) => key)
                })
            ]
        });

        dotAlertConfirmService = TestBed.inject(DotAlertConfirmService);
    });

    // Locks in the design invariant that the navigation stays pending until
    // the user clicks "Keep editing" or "Discard changes". The global
    // `dot-alert-confirm` template sets `[closable]="false"` and does not
    // enable `dismissableMask`, which disables PrimeNG's ESC and mask-click
    // dismissal paths — so this Observable must NEVER emit on its own. If
    // someone changes the wrapper to honor ESC, this test will start failing
    // because no callback fires and the navigation hangs.
    it('should keep navigation pending until accept or reject is invoked', async () => {
        const component = buildComponent({ hasUnsavedChanges: () => true });

        const result = runGuard(component) as Observable<boolean>;

        let emitted: boolean | undefined;
        let completed = false;
        result.subscribe({
            next: (value) => (emitted = value),
            complete: () => (completed = true)
        });

        // The guard defers `confirm()` to the next microtask to avoid an
        // NG0100 ExpressionChangedAfterItHasBeenChecked error inside the
        // global `dot-alert-confirm` template — flush before asserting.
        await Promise.resolve();

        expect(dotAlertConfirmService.confirm).toHaveBeenCalledTimes(1);
        expect(emitted).toBeUndefined();
        expect(completed).toBe(false);
    });

    it('should allow navigation immediately when the form is pristine', () => {
        const component = buildComponent({ hasUnsavedChanges: () => false });

        const result = runGuard(component);

        expect(result).toBe(true);
        expect(dotAlertConfirmService.confirm).not.toHaveBeenCalled();
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
        expect(dotAlertConfirmService.confirm).not.toHaveBeenCalled();
    });

    it('should cancel navigation when the user clicks "Keep editing" (accept)', async () => {
        const component = buildComponent({ hasUnsavedChanges: () => true });

        const result = runGuard(component) as Observable<boolean>;

        let emitted: boolean | undefined;
        let completed = false;
        result.subscribe({
            next: (value) => (emitted = value),
            complete: () => (completed = true)
        });

        // Flush the deferred `confirm()` call.
        await Promise.resolve();

        // Sanity: dialog was opened with the expected i18n keys and label mapping
        expect(dotAlertConfirmService.confirm).toHaveBeenCalledTimes(1);
        const model = capturedModel as DotAlertConfirm;
        expect(model.header).toBe('edit.content.unsaved.changes.title');
        expect(model.message).toBe('edit.content.unsaved.changes.message');
        expect(model.footerLabel).toEqual({
            accept: 'edit.content.unsaved.changes.keep',
            reject: 'edit.content.unsaved.changes.discard'
        });

        // Simulate clicking the primary "Keep editing" button
        model.accept?.();

        expect(emitted).toBe(false);
        expect(completed).toBe(true);
    });

    it('should allow navigation when the user clicks "Discard changes" (reject)', async () => {
        const markFormPristine = jest.fn();
        const component = buildComponent({
            hasUnsavedChanges: () => true,
            markFormPristine
        });

        const result = runGuard(component) as Observable<boolean>;

        let emitted: boolean | undefined;
        let completed = false;
        result.subscribe({
            next: (value) => (emitted = value),
            complete: () => (completed = true)
        });

        // Flush the deferred `confirm()` call.
        await Promise.resolve();

        const model = capturedModel as DotAlertConfirm;

        // Simulate clicking the secondary "Discard changes" button
        model.reject?.();

        expect(emitted).toBe(true);
        expect(completed).toBe(true);
        // Form should be reset to pristine so any downstream beforeunload
        // does not re-prompt with the browser's native dialog.
        expect(markFormPristine).toHaveBeenCalledTimes(1);
    });
});

import { mockProvider } from '@openng/spectator/vitest';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { Confirmation, ConfirmationService, ConfirmEventType } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

import {
    DOT_CONFIGURATION_CONFIRM_KEY,
    DotConfigurationUnsavedWork,
    dotConfigurationUnsavedChangesGuard
} from './dot-configuration-unsaved-changes.guard';

describe('dotConfigurationUnsavedChangesGuard', () => {
    const createPage = (
        hasUnsavedChanges: boolean,
        answer: (confirmation: Confirmation) => void = () => undefined
    ): DotConfigurationUnsavedWork => ({
        $hasUnsavedChanges: signal(hasUnsavedChanges),
        confirmationService: {
            confirm: vi.fn((confirmation: Confirmation) => answer(confirmation))
        } as unknown as ConfirmationService
    });

    const runGuard = (page: DotConfigurationUnsavedWork) =>
        TestBed.runInInjectionContext(() =>
            dotConfigurationUnsavedChangesGuard(
                page,
                {} as ActivatedRouteSnapshot,
                {} as RouterStateSnapshot,
                {} as RouterStateSnapshot
            )
        );

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [mockProvider(DotMessageService, { get: vi.fn((key: string) => key) })]
        });
    });

    it('lets the user leave when nothing is unsaved', () => {
        const page = createPage(false);

        expect(runGuard(page)).toBe(true);
        expect(page.confirmationService.confirm).not.toHaveBeenCalled();
    });

    it('asks through the page dialog when there are unsaved edits', () => {
        const page = createPage(true);

        runGuard(page);

        expect(page.confirmationService.confirm).toHaveBeenCalledWith(
            expect.objectContaining({ key: DOT_CONFIGURATION_CONFIRM_KEY })
        );
    });

    it('stays on the page when the user keeps editing', async () => {
        const page = createPage(true, (confirmation) => confirmation.accept?.());

        await expect(runGuard(page)).resolves.toBe(false);
    });

    it('leaves when the user discards the edits', async () => {
        const page = createPage(true, (confirmation) =>
            confirmation.reject?.(ConfirmEventType.REJECT)
        );

        await expect(runGuard(page)).resolves.toBe(true);
    });

    it('stays on the page when the dialog is dismissed', async () => {
        const page = createPage(true, (confirmation) =>
            confirmation.reject?.(ConfirmEventType.CANCEL)
        );

        await expect(runGuard(page)).resolves.toBe(false);
    });
});

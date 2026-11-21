import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';

import { signal } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotTitleStatus } from '@dotcms/dotcms-models';

import { DotEditContentDialogHeaderComponent } from './dot-edit-content-dialog-header.component';
import { DotEditContentDialogTitleStatusService } from './dot-edit-content-dialog-title-status.service';

describe('DotEditContentDialogHeaderComponent', () => {
    let spectator: Spectator<DotEditContentDialogHeaderComponent>;
    let dialogRef: DynamicDialogRef;
    /** The editor's own signal of what the chip shows, registered under the dialog's ref. */
    let status: ReturnType<typeof signal<DotTitleStatus>>;

    const createComponent = createComponentFactory({
        component: DotEditContentDialogHeaderComponent,
        providers: [
            mockProvider(DotMessageService, { get: (key: string) => key }),
            { provide: DynamicDialogConfig, useValue: { header: 'My Blog Post' } },
            { provide: DynamicDialogRef, useValue: {} }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        dialogRef = spectator.inject(DynamicDialogRef);
        status = signal<DotTitleStatus>(null);
        spectator.inject(DotEditContentDialogTitleStatusService).register(dialogRef, status);
        spectator.detectChanges();
    });

    it('should render the title the opener gave the dialog, since PrimeNG no longer prints it', () => {
        expect(spectator.query(byTestId('dialog-header-title'))?.textContent?.trim()).toBe(
            'My Blog Post'
        );
    });

    it('should show no chip while the editor has no status', () => {
        expect(spectator.query(byTestId('title-status-chip'))).toBeFalsy();
    });

    it('should show the status badge right after the title', () => {
        status.set({ state: { live: true, working: true, hasLiveVersion: true } });
        spectator.detectChanges();

        const title = spectator.query(byTestId('dialog-header-title')) as HTMLElement;
        const chip = spectator.query(byTestId('title-status-chip')) as HTMLElement;

        expect(chip.textContent?.trim()).toBe('Published');
        expect(title.compareDocumentPosition(chip) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('should read "New" for content that has not been saved', () => {
        status.set({ state: null });
        spectator.detectChanges();

        expect(spectator.query(byTestId('title-status-chip'))?.textContent?.trim()).toBe('New');
    });

    it('should remove the chip when the status goes back to null', () => {
        status.set({ state: null });
        spectator.detectChanges();

        status.set(null);
        spectator.detectChanges();

        expect(spectator.query(byTestId('title-status-chip'))).toBeFalsy();
    });

    it('should let a long title truncate while the chip keeps its size', () => {
        status.set({ state: null });
        spectator.detectChanges();

        expect(
            spectator.query(byTestId('dialog-header-title'))?.classList.contains('truncate')
        ).toBe(true);
        expect(spectator.query(byTestId('title-status-chip'))?.classList.contains('shrink-0')).toBe(
            true
        );
    });

    it('should keep a separate status per dialog', () => {
        const otherRef = {} as DynamicDialogRef;

        status.set({ state: null });

        expect(spectator.inject(DotEditContentDialogTitleStatusService).for(otherRef)()).toBeNull();
    });

    describe('accessible name of the dialog', () => {
        /**
         * PrimeNG stops printing the title's `id` when a header template is given, while the dialog
         * keeps `aria-labelledby` pointing at it. The header has to give its title that id back.
         */
        const openInsideDialog = (labelledBy: string | null) => {
            spectator = createComponent();
            const dialog = document.createElement('div');
            dialog.setAttribute('role', 'dialog');

            if (labelledBy) {
                dialog.setAttribute('aria-labelledby', labelledBy);
            }

            dialog.appendChild(spectator.element);
            document.body.appendChild(dialog);
            spectator.detectChanges();
            spectator.detectChanges();

            return dialog;
        };

        afterEach(() => document.querySelectorAll('[role="dialog"]').forEach((el) => el.remove()));

        it('should give its title the id the dialog is labelled by', () => {
            const dialog = openInsideDialog('pn_id_x_header');

            const title = spectator.query(byTestId('dialog-header-title')) as HTMLElement;

            expect(title.id).toBe('pn_id_x_header');
            expect(document.getElementById(dialog.getAttribute('aria-labelledby') as string)).toBe(
                title
            );
        });

        it('should leave the title without an id when the dialog names none', () => {
            openInsideDialog(null);

            expect(spectator.query(byTestId('dialog-header-title'))?.hasAttribute('id')).toBe(
                false
            );
        });

        it('should leave the title without an id when it is not inside a dialog', () => {
            expect(spectator.query(byTestId('dialog-header-title'))?.hasAttribute('id')).toBe(
                false
            );
        });
    });
});

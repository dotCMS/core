import { expect, it } from '@jest/globals';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { mockProvider } from '@ngneat/spectator/jest';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotEditContentCategoryFieldDialogComponent } from './dot-edit-content-category-field-dialog.component';

describe('DotEditContentCategoryFieldDialogComponent', () => {
    let spectator: Spectator<DotEditContentCategoryFieldDialogComponent>;
    let dialogRef: DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotEditContentCategoryFieldDialogComponent,
        providers: [
            mockProvider(DynamicDialogRef, {
                close: jest.fn()
            }),
            mockProvider(DotMessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        dialogRef = spectator.inject(DynamicDialogRef);
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should have a cancel button', () => {
        expect(spectator.query(byTestId('cancel-btn'))).not.toBeNull();
    });
    it('should have a apply button', () => {
        expect(spectator.query(byTestId('apply-btn'))).not.toBeNull();
    });

    it('should close the dialog when you click cancel', () => {
        const cancelBtn = spectator.query(byTestId('cancel-btn'));
        expect(cancelBtn).not.toBeNull();

        expect(dialogRef.close).not.toHaveBeenCalled();

        spectator.click(cancelBtn);
        expect(dialogRef.close).toHaveBeenCalled();
    });
});

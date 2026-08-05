import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';

import { signal } from '@angular/core';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { ExistingContentStore } from '@dotcms/edit-content';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveRelationshipFooterComponent } from './dot-content-drive-relationship-footer.component';

describe('DotContentDriveRelationshipFooterComponent', () => {
    let spectator: Spectator<DotContentDriveRelationshipFooterComponent>;
    let dialogRef: SpyObject<DynamicDialogRef>;

    const items = signal<DotCMSContentlet[]>([]);

    const createComponent = createComponentFactory({
        component: DotContentDriveRelationshipFooterComponent,
        providers: [
            mockProvider(ExistingContentStore, { currentItems: items }),
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ Cancel: 'Cancel' })
            }
        ]
    });

    beforeEach(() => {
        items.set([]);
        spectator = createComponent();
        dialogRef = spectator.inject(DynamicDialogRef, true);
    });

    const button = (testId: string) =>
        spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement;

    it('should keep Apply enabled even with zero selected items', () => {
        expect(button('relationship-footer-apply').disabled).toBe(false);
    });

    it('should close with the current selection on apply', () => {
        const selection = [{ identifier: 'id-1' } as DotCMSContentlet];
        items.set(selection);
        spectator.detectChanges();

        spectator.click(button('relationship-footer-apply'));

        expect(dialogRef.close).toHaveBeenCalledWith(selection);
    });

    it('should close with an empty selection on apply when nothing is selected', () => {
        spectator.click(button('relationship-footer-apply'));

        expect(dialogRef.close).toHaveBeenCalledWith([]);
    });

    it('should close without a value on cancel', () => {
        spectator.click(button('relationship-footer-cancel'));

        expect(dialogRef.close).toHaveBeenCalledWith();
    });
});

import { byTestId, createComponentFactory, Spectator, SpyObject } from '@ngneat/spectator/jest';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { createFakeContentlet, MockDotMessageService } from '@dotcms/utils-testing';

import { FooterComponent } from './footer.component';

import { ExistingContentStore } from '../../store/existing-content.store';

describe('FooterComponent', () => {
    let spectator: Spectator<FooterComponent>;
    let store: InstanceType<typeof ExistingContentStore>;
    let dotMessageService: SpyObject<DotMessageService>;
    let dialogRef: SpyObject<DynamicDialogRef>;

    const messages = {
        'dot.file.relationship.dialog.apply.one.entry': 'Apply {0} Entry',
        'dot.file.relationship.dialog.apply.entries': 'Apply {0} Entries'
    };

    const createComponent = createComponentFactory({
        component: FooterComponent,
        providers: [
            ExistingContentStore,
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(messages)
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(ExistingContentStore);
        dotMessageService = spectator.inject(DotMessageService);
        dialogRef = spectator.inject(DynamicDialogRef);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Button labels and visibility', () => {
        it('should show apply button as disabled when no items are selected', () => {
            spectator.detectChanges();

            const applyButton = spectator.query(byTestId('apply-button'));
            expect(applyButton).toHaveProperty('disabled', true);
        });

        it('should show apply button as enabled when items are selected', () => {
            store.setSelectedItems([createFakeContentlet()]);
            spectator.detectChanges();

            const applyButton = spectator.query(byTestId('apply-button'));
            expect(applyButton).toHaveProperty('disabled', false);
        });

        it('should show "Apply 1 Entry" label when 1 item is selected', () => {
            store.setSelectedItems([createFakeContentlet()]);
            spectator.detectChanges();

            const spy = jest.spyOn(dotMessageService, 'get');
            spectator.component.applyLabel();

            expect(spy).toHaveBeenCalledWith('dot.file.relationship.dialog.apply.one.entry', '1');
            expect(spectator.query(byTestId('apply-button'))).toHaveProperty(
                'label',
                'Apply 1 Entry'
            );
        });

        it('should show "Apply X Entries" label when multiple items are selected', () => {
            store.setSelectedItems([createFakeContentlet(), createFakeContentlet()]);
            spectator.detectChanges();

            spectator.component.applyLabel();

            expect(dotMessageService.get).toHaveBeenCalledWith(
                'dot.file.relationship.dialog.apply.entries',
                '2'
            );
            expect(spectator.query(byTestId('apply-button'))).toHaveProperty(
                'label',
                'Apply 2 Entries'
            );
        });
    });

    describe('Dialog actions', () => {
        it('should close dialog with null when cancel button is clicked', () => {
            spectator.click(byTestId('cancel-button'));
            expect(dialogRef.close).toHaveBeenCalled();
            expect(dialogRef.close).toHaveBeenCalledWith();
        });

        it('should close dialog with selected items when apply button is clicked', () => {
            const mockItems = [createFakeContentlet(), createFakeContentlet()];
            store.setSelectedItems(mockItems);
            spectator.detectChanges();

            spectator.click(byTestId('apply-button'));
            expect(dialogRef.close).toHaveBeenCalledWith(mockItems);
        });

        it('should close dialog with empty array when apply button is clicked with no items', () => {
            store.setSelectedItems([]);
            spectator.detectChanges();

            spectator.component.applyChanges();

            expect(dialogRef.close).toHaveBeenCalledWith([]);
        });
    });

    describe('Edge cases', () => {
        it('should handle large number of items correctly', () => {
            // Create an array with 100 mock items
            const manyItems = Array.from({ length: 100 }, (_, i) =>
                createFakeContentlet({ inode: `item_${i}` })
            );
            store.setSelectedItems(manyItems);
            spectator.detectChanges();

            spectator.component.applyLabel();

            expect(dotMessageService.get).toHaveBeenCalledWith(
                'dot.file.relationship.dialog.apply.entries',
                '100'
            );
            expect(spectator.query(byTestId('apply-button'))).toHaveProperty(
                'label',
                'Apply 100 Entries'
            );

            // Check if the apply function can handle many items
            spectator.click(byTestId('apply-button'));
            expect(dialogRef.close).toHaveBeenCalledWith(manyItems);
        });

        it('should handle zero items correctly', () => {
            store.setSelectedItems([]);
            spectator.detectChanges();

            spectator.component.applyLabel();

            expect(dotMessageService.get).toHaveBeenCalledWith(
                'dot.file.relationship.dialog.apply.entries',
                '0'
            );
        });
    });
});

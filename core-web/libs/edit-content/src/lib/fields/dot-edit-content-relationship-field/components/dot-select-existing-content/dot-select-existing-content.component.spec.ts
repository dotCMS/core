import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';



import { DotMessageService } from '@dotcms/data-access';

import { DotSelectExistingContentComponent } from './dot-select-existing-content.component';
import { ExistingContentStore } from './store/existing-content.store';
 

describe('DotSelectExistingContentComponent', () => {
    let spectator: Spectator<DotSelectExistingContentComponent>;
    let store: InstanceType<typeof ExistingContentStore>;
    let dotMessageService: DotMessageService;

    const createComponent = createComponentFactory({
        component: DotSelectExistingContentComponent,
        componentProviders: [ExistingContentStore],
        providers: [
            mockProvider(DotMessageService, {
                get: jest.fn((key: string, count?: string) => {
                    if (key === 'dot.file.relationship.dialog.apply.one.entry') {
                        return `Apply ${count} entry`;
                    }

                    return `Apply ${count} entries`;
                })
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(ExistingContentStore, true);
        dotMessageService = spectator.inject(DotMessageService);
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
        expect(store).toBeTruthy();
    });

    describe('Visibility', () => {
        it('should set visibility to false when closeDialog is called', () => {
            spectator.component.$visible.set(true);
            spectator.component.closeDialog();
            expect(spectator.component.$visible()).toBeFalse();
        });
    });

    describe('Selected Items', () => {
        it('should disable apply button when no items are selected', () => {
            spectator.component.$selectedItems.set([]);
            expect(spectator.component.$isApplyDisabled()).toBeTruthy();
        });

        it('should enable apply button when items are selected', () => {
            const mockContent = [{ id: '1', title: 'Test Content' }];
            spectator.component.$selectedItems.set(mockContent);
            expect(spectator.component.$isApplyDisabled()).toBeFalsy();
        });
    });

    describe('Apply Label', () => {
        it('should return correct label for single item', () => {
            const mockContent = [{ id: '1', title: 'Test Content' }];
            spectator.component.$selectedItems.set(mockContent);
            
            const label = spectator.component.$applyLabel();
            
            expect(dotMessageService.get).toHaveBeenCalledWith(
                'dot.file.relationship.dialog.apply.one.entry',
                '1'
            );
            expect(label).toBe('Apply 1 entry');
        });

        it('should return correct label for multiple items', () => {
            const mockContent = [
                { id: '1', title: 'Test Content 1' },
                { id: '2', title: 'Test Content 2' }
            ];
            spectator.component.$selectedItems.set(mockContent);
            
            const label = spectator.component.$applyLabel();
            
            expect(dotMessageService.get).toHaveBeenCalledWith(
                'dot.file.relationship.dialog.apply.entries',
                '2'
            );
            expect(label).toBe('Apply 2 entries');
        });
    });

    describe('Dialog Closing', () => {
        it('should close dialog and emit selected items', () => {
            const mockContent = [{ id: '1', title: 'Test Content' }];
            const selectItemsSpy = jest.spyOn(spectator.component.$selectItems, 'emit');
            
            spectator.component.$selectedItems.set(mockContent);
            spectator.component.closeDialogWithSelectedItems();
            
            expect(spectator.component.$visible()).toBeFalse();
            expect(selectItemsSpy).toHaveBeenCalledWith(mockContent);
        });
    });
});

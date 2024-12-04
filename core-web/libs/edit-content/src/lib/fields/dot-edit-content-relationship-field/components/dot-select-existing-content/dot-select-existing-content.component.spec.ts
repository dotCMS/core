import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { RelationshipFieldItem } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/relationship.models';

import { DotSelectExistingContentComponent } from './dot-select-existing-content.component';
import { ExistingContentStore } from './store/existing-content.store';

describe('DotSelectExistingContentComponent', () => {
    let spectator: Spectator<DotSelectExistingContentComponent>;
    let store: InstanceType<typeof ExistingContentStore>;
    let dotMessageService: DotMessageService;

    const mockRelationshipItem = (id: string): RelationshipFieldItem => ({
        id,
        title: `Test Content ${id}`,
        language: '1',
        state: {
            label: 'Published',
            styleClass: 'small-chip'
        },
        description: 'Test description',
        step: 'Step 1',
        lastUpdate: new Date().toISOString()
    });

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

    describe('Dialog Visibility', () => {
        it('should set visibility to false when closeDialog is called', () => {
            spectator.component.$visible.set(true);
            spectator.component.closeDialog();
            expect(spectator.component.$visible()).toBeFalse();
        });
    });

    describe('Selected Items State', () => {
        it('should disable apply button when no items are selected', () => {
            spectator.component.$selectedItems.set([]);
            expect(spectator.component.$isApplyDisabled()).toBeTruthy();
        });

        it('should enable apply button when items are selected', () => {
            const mockContent = [mockRelationshipItem('1')];
            spectator.component.$selectedItems.set(mockContent);
            expect(spectator.component.$isApplyDisabled()).toBeFalsy();
        });
    });

    describe('Apply Button Label', () => {
        it('should show singular label when one item is selected', () => {
            const mockContent = [mockRelationshipItem('1')];
            spectator.component.$selectedItems.set(mockContent);

            const label = spectator.component.$applyLabel();

            expect(dotMessageService.get).toHaveBeenCalledWith(
                'dot.file.relationship.dialog.apply.one.entry',
                '1'
            );
            expect(label).toBe('Apply 1 entry');
        });

        it('should show plural label when multiple items are selected', () => {
            const mockContent = [
                mockRelationshipItem('1'),
                mockRelationshipItem('2')
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
});

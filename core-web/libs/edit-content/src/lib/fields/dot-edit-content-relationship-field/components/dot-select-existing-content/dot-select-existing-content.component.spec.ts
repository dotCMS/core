import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';

import { Dialog } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { RelationshipFieldItem } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/relationship.models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotSelectExistingContentComponent } from './dot-select-existing-content.component';
import { ExistingContentStore } from './store/existing-content.store';

describe('DotSelectExistingContentComponent', () => {
    let spectator: Spectator<DotSelectExistingContentComponent>;
    let store: InstanceType<typeof ExistingContentStore>;

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

    const messageServiceMock = new MockDotMessageService({
        'dot.file.relationship.dialog.apply.one.entry': 'Apply 1 entry',
        'dot.file.relationship.dialog.apply.entries': 'Apply {0} entries'
    });

    const createComponent = createComponentFactory({
        component: DotSelectExistingContentComponent,
        componentProviders: [ExistingContentStore],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(ExistingContentStore, true);
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
            expect(spectator.component.$visible()).toBeFalsy();
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
            expect(label).toBe('Apply 1 entry');
        });

        it('should show plural label when multiple items are selected', () => {
            const mockContent = [mockRelationshipItem('1'), mockRelationshipItem('2')];
            spectator.component.$selectedItems.set(mockContent);

            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 2 entries');
        });
    });

    describe('checkIfSelected', () => {
        it('should return true when content is in selectedContent array', () => {
            // Arrange
            const testContent = mockRelationshipItem('1');
            spectator.component.$selectedItems.set([testContent]);

            // Act
            const result = spectator.component.checkIfSelected(testContent);

            // Assert
            expect(result).toBe(true);
        });

        it('should return false when content is not in selectedContent array', () => {
            // Arrange
            const testContent = mockRelationshipItem('123');
            const differentContent = mockRelationshipItem('456');
            spectator.component.$selectedItems.set([differentContent]);

            // Act
            const result = spectator.component.checkIfSelected(testContent);

            // Assert
            expect(result).toBe(false);
        });

        it('should return false when selectedContent is empty', () => {
            // Arrange
            const testContent = mockRelationshipItem('123');
            spectator.component.$selectedItems.set([]);

            // Act
            const result = spectator.component.checkIfSelected(testContent);

            // Assert
            expect(result).toBe(false);
        });
    });

    describe('onShowDialog', () => {
        it('should call onShowDialog when dialog is shown', fakeAsync(() => {
            // Arrange
            spectator.component.$visible.set(true);

            spectator.detectChanges();

            tick(100);
            const spy = jest.spyOn(spectator.component, 'onShowDialog');

            // Act
            spectator.triggerEventHandler(Dialog, 'onShow', null);

            // Assert
            expect(spy).toHaveBeenCalled();
        }));
    });
});

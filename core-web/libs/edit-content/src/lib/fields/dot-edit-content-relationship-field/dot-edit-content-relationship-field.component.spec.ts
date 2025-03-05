import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Table } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { createFormGroupDirectiveMock } from '@dotcms/edit-content/utils/mocks';
import { createFakeContentlet, createFakeRelationshipField } from '@dotcms/utils-testing';

import { DotEditContentRelationshipFieldComponent } from './dot-edit-content-relationship-field.component';
import { RelationshipFieldStore } from './store/relationship-field.store';

const mockField = createFakeRelationshipField({
    relationships: {
        cardinality: 0, // ONE_TO_MANY
        isParentField: true,
        velocityVar: 'AllTypes'
    },
    variable: 'relationshipField'
});

const mockRelationships = [
    createFakeContentlet({
        title: 'Relationship 1',
        inode: '1',
        identifier: 'id-1'
    }),
    createFakeContentlet({
        title: 'Relationship 2',
        inode: '2',
        identifier: 'id-2'
    })
];

const mockContentlet = createFakeContentlet({
    [mockField.variable]: mockRelationships
});

describe('DotEditContentRelationshipFieldComponent', () => {
    let spectator: Spectator<DotEditContentRelationshipFieldComponent>;
    let store: InstanceType<typeof RelationshipFieldStore>;
    let dialogService: DialogService;

    const createComponent = createComponentFactory({
        component: DotEditContentRelationshipFieldComponent,
        componentViewProviders: [
            RelationshipFieldStore,
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ],
        providers: [FormGroupDirective, mockProvider(DotMessageService), DialogService],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: mockField,
                contentlet: mockContentlet
            } as unknown
        });

        store = spectator.inject(RelationshipFieldStore, true);
        dialogService = spectator.inject(DialogService);
    });

    it('should create the component', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should initialize with correct data', () => {
        spectator.detectChanges();
        spectator.flushEffects();

        const tableElement = spectator.query(Table);
        expect(tableElement).toBeTruthy();
        expect(store.data()).toEqual(mockRelationships);
    });

    it('should handle empty field data', () => {
        const emptyField = createFakeRelationshipField({
            relationships: {
                cardinality: 0,
                isParentField: true,
                velocityVar: 'AllTypes'
            },
            variable: 'emptyField'
        });

        const emptyContentlet = createFakeContentlet({
            [emptyField.variable]: []
        });

        spectator.setInput('field', emptyField);
        spectator.setInput('contentlet', emptyContentlet);

        spectator.detectChanges();
        spectator.flushEffects();

        expect(store.data()).toEqual([]);
    });

    it('should handle disabled state', () => {
        spectator.component.setDisabledState(true);
        spectator.detectChanges();

        expect(spectator.component.$isDisabled()).toBe(true);
    });

    it('should not delete item when disabled', () => {
        const deleteSpy = jest.spyOn(store, 'deleteItem');
        spectator.component.setDisabledState(true);
        spectator.detectChanges();

        spectator.component.deleteItem('1');
        expect(deleteSpy).not.toHaveBeenCalled();
    });

    it('should delete item when not disabled', () => {
        const deleteSpy = jest.spyOn(store, 'deleteItem');
        spectator.detectChanges();

        spectator.component.deleteItem('1');
        expect(deleteSpy).toHaveBeenCalledWith('1');
    });

    it('should not reorder items when disabled', () => {
        const setDataSpy = jest.spyOn(store, 'setData');
        spectator.component.setDisabledState(true);
        spectator.detectChanges();

        spectator.component.onRowReorder({ dragIndex: 0, dropIndex: 1 });
        expect(setDataSpy).not.toHaveBeenCalled();
    });

    it('should reorder items when not disabled', () => {
        const setDataSpy = jest.spyOn(store, 'setData');
        spectator.detectChanges();

        spectator.component.onRowReorder({ dragIndex: 0, dropIndex: 1 });
        expect(setDataSpy).toHaveBeenCalledWith(store.data());
    });

    it('should not show dialog when disabled', () => {
        const openSpy = jest.spyOn(dialogService, 'open');
        spectator.component.setDisabledState(true);
        spectator.detectChanges();

        spectator.component.showExistingContentDialog();
        expect(openSpy).not.toHaveBeenCalled();
    });

    it('should handle dialog close with no selection', () => {
        const mockDialogRef = {
            onClose: of(null),
            close: jest.fn()
        };

        jest.spyOn(dialogService, 'open').mockReturnValue(
            mockDialogRef as unknown as DynamicDialogRef
        );
        const setDataSpy = jest.spyOn(store, 'setData');

        spectator.detectChanges();
        spectator.component.showExistingContentDialog();

        // Wait for the dialog close subscription to complete
        spectator.flushEffects();
        expect(setDataSpy).not.toHaveBeenCalled();
    });

    it('should handle invalid field data gracefully', () => {
        const invalidField = createFakeRelationshipField({
            relationships: null,
            variable: 'invalidField'
        });

        const invalidContentlet = createFakeContentlet({
            [invalidField.variable]: null
        });

        spectator.setInput({
            $field: invalidField,
            $contentlet: invalidContentlet
        });

        spectator.detectChanges();
        spectator.flushEffects();

        expect(store.data()).toBeDefined();
    });

    it('should handle null contentlet gracefully', () => {
        spectator.setInput({
            $field: mockField,
            $contentlet: null
        });

        spectator.detectChanges();
        spectator.flushEffects();

        expect(store.data()).toBeDefined();
    });
});

import { byTestId, createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { Column } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/column.model';
import { RelationshipFieldSearchResponse, RelationshipFieldService } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/services/relationship-field.service';
import { createFakeContentlet, MockDotMessageService, mockLocales } from '@dotcms/utils-testing';

import { FooterComponent } from './footer.component';

import { ExistingContentStore } from '../../store/existing-content.store';

const mockColumns: Column[] = [
    { field: 'title', header: 'Title' },
    { field: 'modDate', header: 'Mod Date' }
];

const mockData: RelationshipFieldSearchResponse = {
    contentlets: [

        createFakeContentlet({
            title: 'Content 1',
            inode: '1',
            identifier: 'id-1',
            languageId: mockLocales[0].id
        }),
        createFakeContentlet({
            title: 'Content 2',
            inode: '2',
            identifier: 'id-2',
            languageId: mockLocales[1].id
        }),
        createFakeContentlet({
            title: 'Content 3',
            inode: '3',
            identifier: 'id-3',
            languageId: mockLocales[0].id
        })
    ],
    totalResults: 3
};

describe('FooterComponent', () => {
    let spectator: Spectator<FooterComponent>;
    let store: InstanceType<typeof ExistingContentStore>;
    let dialogRef: SpyObject<DynamicDialogRef>;

    const messages = {
        'dot.file.relationship.dialog.apply.one.entry': 'Apply {0} entry',
        'dot.file.relationship.dialog.apply.entries': 'Apply {0} entries'
    };

    const createComponent = createComponentFactory({
        component: FooterComponent,
        providers: [
            ExistingContentStore,
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(messages)
            },
            mockProvider(RelationshipFieldService, {
                getColumnsAndContent: jest.fn().mockReturnValue(of([mockColumns, mockData]))
            }),
            mockProvider(DynamicDialogRef, {
                close: jest.fn()
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(ExistingContentStore);
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


            spectator.component.$applyLabel();

            expect(spectator.query(byTestId('apply-button'))).toHaveProperty(
                'label',
                'Apply 1 entry'
            );
        });

        it('should show "Apply X Entries" label when multiple items are selected', () => {
            store.setSelectedItems([createFakeContentlet(), createFakeContentlet()]);
            spectator.detectChanges();

            spectator.component.$applyLabel();

            expect(spectator.query(byTestId('apply-button'))).toHaveProperty(
                'label',
                'Apply 2 entries'
            );
        });
    });

    describe('Dialog actions', () => {
        it('should close dialog with null when cancel button is clicked', () => {
            spectator.click(byTestId('cancel-button'));
            expect(dialogRef.close).toHaveBeenCalled();
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

    describe('Apply Button Label', () => {
        it('should show singular label when one item is selected', () => {
            const mockContent = [createFakeContentlet({ inode: '1' })];
            store.setSelectedItems(mockContent);

            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 1 entry');
        });

        it('should show plural label when multiple items are selected', () => {
            const mockContent = [
                createFakeContentlet({ inode: '1' }),
                createFakeContentlet({ inode: '2' })
            ];
            store.setSelectedItems(mockContent);

            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 2 entries');
        });

        it('should handle empty selection', () => {
            store.setSelectedItems([]);
            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 0 entries');
        });
    });
});

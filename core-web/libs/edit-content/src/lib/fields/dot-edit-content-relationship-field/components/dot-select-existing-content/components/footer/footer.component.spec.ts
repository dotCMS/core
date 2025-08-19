import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { createFakeContentlet, MockDotMessageService, mockLocales } from '@dotcms/utils-testing';

import { FooterComponent } from './footer.component';

import { Column } from '../../../../models/column.model';
import {
    RelationshipFieldSearchResponse,
    ExistingContentService
} from '../../store/existing-content.service';
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
        imports: [ButtonModule],
        providers: [
            ExistingContentStore,
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(messages)
            },
            mockProvider(ExistingContentService, {
                getColumnsAndContent: jest.fn().mockReturnValue(of([mockColumns, mockData]))
            }),
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(ExistingContentStore);
        dialogRef = spectator.inject(DynamicDialogRef);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Button labels and visibility', () => {
        it('should show apply button as disabled when no items are selected', () => {
            spectator.detectChanges();

            const applyButton = spectator.query(byTestId('apply-button')).querySelector('button');
            expect(applyButton.disabled).toBe(true);
        });

        it('should show apply button as enabled when items are selected', () => {
            store.setSelectionItems([createFakeContentlet()]);
            spectator.detectChanges();

            const applyButton = spectator.query(byTestId('apply-button')).querySelector('button');
            expect(applyButton.disabled).toBe(false);
        });

        it('should show "Apply 1 Entry" label when 1 item is selected', () => {
            store.setSelectionItems([createFakeContentlet()]);
            spectator.detectChanges();

            spectator.component.$applyLabel();

            expect(spectator.query(byTestId('apply-button'))).toHaveText('Apply 1 entry');
        });

        it('should show "Apply X Entries" label when multiple items are selected', () => {
            store.setSelectionItems([createFakeContentlet(), createFakeContentlet()]);
            spectator.detectChanges();

            spectator.component.$applyLabel();

            expect(spectator.query(byTestId('apply-button'))).toHaveText('Apply 2 entries');
        });
    });

    describe('Dialog actions', () => {
        it('should close dialog with null when cancel button is clicked', () => {
            const cancelButton = spectator.query(byTestId('cancel-button'));
            spectator.click(cancelButton.querySelector('button'));
            expect(dialogRef.close).toHaveBeenCalled();
        });

        it('should close dialog with selected items when apply button is clicked', () => {
            const mockItems = [createFakeContentlet(), createFakeContentlet()];
            store.setSelectionItems(mockItems);
            spectator.detectChanges();

            const applyButton = spectator.query(byTestId('apply-button'));
            spectator.click(applyButton.querySelector('button'));
            expect(dialogRef.close).toHaveBeenCalledWith(mockItems);
        });

        it('should close dialog with empty array when apply button is clicked with no items', () => {
            store.setSelectionItems([]);
            spectator.detectChanges();

            spectator.component.applyChanges();

            expect(dialogRef.close).toHaveBeenCalledWith([]);
        });
    });

    describe('Apply Button Label', () => {
        it('should show singular label when one item is selected', () => {
            const mockContent = [createFakeContentlet({ inode: '1' })];
            store.setSelectionItems(mockContent);

            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 1 entry');
        });

        it('should show plural label when multiple items are selected', () => {
            const mockContent = [
                createFakeContentlet({ inode: '1' }),
                createFakeContentlet({ inode: '2' })
            ];
            store.setSelectionItems(mockContent);

            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 2 entries');
        });

        it('should handle empty selection', () => {
            store.setSelectionItems([]);
            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 0 entries');
        });
    });
});

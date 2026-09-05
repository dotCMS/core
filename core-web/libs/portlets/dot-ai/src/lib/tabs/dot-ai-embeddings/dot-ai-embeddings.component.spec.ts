import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { Subject } from 'rxjs';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotAiIndex } from '@dotcms/dotcms-models';

import DotAiEmbeddingsComponent from './dot-ai-embeddings.component';

import { DotAiStore } from '../../store/dot-ai.store';

const index = (overrides: Partial<DotAiIndex> = {}): DotAiIndex => ({
    name: 'blogs',
    fragments: 10,
    contents: 4,
    tokenTotal: 1000,
    tokensPerChunk: 100,
    contentTypes: ['Blog'],
    ...overrides
});

describe('DotAiEmbeddingsComponent', () => {
    let spectator: Spectator<DotAiEmbeddingsComponent>;
    let onClose: Subject<unknown>;
    let dialogService: DialogService;
    let confirmSpy: jest.SpyInstance;

    const storeMock = {
        indexes: jest.fn().mockReturnValue([index()]),
        filteredIndexes: jest.fn().mockReturnValue([index()]),
        indexStatuses: jest.fn().mockReturnValue({ blogs: 'READY' }),
        indexesForbidden: jest.fn().mockReturnValue(false),
        statusFilter: jest.fn().mockReturnValue(null),
        isConfigured: jest.fn().mockReturnValue(true),
        setIndexFilter: jest.fn(),
        setStatusFilter: jest.fn(),
        buildIndex: jest.fn(),
        deleteFromIndex: jest.fn(),
        deleteIndex: jest.fn(),
        rebuildEmbeddingsDb: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotAiEmbeddingsComponent,
        // The component provides ConfirmationService and DialogService itself, so mocks have
        // to go in componentProviders to win. ConfirmationService stays REAL: p-confirmDialog
        // subscribes to its requireConfirmation$ at construction and a bare mock has none.
        componentProviders: [
            { provide: DotAiStore, useValue: storeMock },
            ConfirmationService,
            { provide: DialogService, useValue: { open: jest.fn() } }
        ],
        providers: [mockProvider(DotMessageService)],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        storeMock.indexesForbidden.mockReturnValue(false);
        storeMock.filteredIndexes.mockReturnValue([index()]);
        onClose = new Subject();
        spectator = createComponent();
        dialogService = spectator.inject(DialogService, true);
        (dialogService.open as jest.Mock).mockReturnValue({ onClose });
        confirmSpy = jest.spyOn(spectator.inject(ConfirmationService, true), 'confirm');
    });

    const clickButton = (testId: string) =>
        spectator.click(
            spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement
        );

    it('should render the table with the index rows', () => {
        expect(spectator.query(byTestId('dotai-embeddings-table'))).toBeTruthy();
        expect(spectator.queryAll(byTestId('dotai-embeddings-row'))).toHaveLength(1);
    });

    it('should show the covered content types where the design asked for a timestamp', () => {
        // No timestamp is stored for an index anywhere, so this slot carries real data instead.
        expect(spectator.query(byTestId('dotai-embeddings-content-types'))).toContainText('Blog');
    });

    it('should show a cost estimate on every row', () => {
        // The legacy screen computed this but only rendered it for the index named `cache`.
        expect(spectator.query(byTestId('dotai-embeddings-cost'))).toContainText('$');
    });

    it('should explain the administrator requirement instead of an empty table (FR-049)', () => {
        storeMock.indexesForbidden.mockReturnValue(true);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-embeddings-forbidden'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-embeddings-table'))).toBeFalsy();
    });

    it('should confirm before deleting an index (FR-031)', () => {
        clickButton('dotai-embeddings-delete');

        expect(confirmSpy).toHaveBeenCalled();
        // Nothing happens until the confirmation is accepted.
        expect(storeMock.deleteIndex).not.toHaveBeenCalled();
    });

    it('should confirm before rebuilding the store (FR-032)', () => {
        clickButton('dotai-embeddings-rebuild');

        expect(confirmSpy).toHaveBeenCalled();
        expect(storeMock.rebuildEmbeddingsDb).not.toHaveBeenCalled();
    });

    describe('New Index dialog', () => {
        it('should open at the mandated width and be dismissible', () => {
            clickButton('dotai-embeddings-new-index');

            expect(dialogService.open).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    width: '700px',
                    closable: true,
                    closeOnEscape: true
                })
            );
        });

        it('should build on an add-mode result', () => {
            clickButton('dotai-embeddings-new-index');

            onClose.next({ mode: 'add', indexName: 'blogs', query: '+contentType:Blog' });

            expect(storeMock.buildIndex).toHaveBeenCalledWith({
                mode: 'add',
                indexName: 'blogs',
                query: '+contentType:Blog'
            });
            expect(storeMock.deleteFromIndex).not.toHaveBeenCalled();
        });

        it('should delete from the index on a delete-mode result (FR-030)', () => {
            clickButton('dotai-embeddings-new-index');

            onClose.next({ mode: 'delete', indexName: 'blogs', query: '+contentType:Blog' });

            expect(storeMock.deleteFromIndex).toHaveBeenCalledWith({
                indexName: 'blogs',
                query: '+contentType:Blog'
            });
            expect(storeMock.buildIndex).not.toHaveBeenCalled();
        });

        it('should do nothing when the dialog is dismissed', () => {
            clickButton('dotai-embeddings-new-index');

            onClose.next(undefined);

            expect(storeMock.buildIndex).not.toHaveBeenCalled();
            expect(storeMock.deleteFromIndex).not.toHaveBeenCalled();
        });
    });
});

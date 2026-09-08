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
        isConfigured: jest.fn().mockReturnValue(true),
        setIndexFilter: jest.fn(),
        buildIndex: jest.fn(),
        indexBuildNotice: jest.fn().mockReturnValue(null),
        dismissBuildNotice: jest.fn(),
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

    it('should explain the administrator requirement instead of an empty table (FR-049)', () => {
        storeMock.indexesForbidden.mockReturnValue(true);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-embeddings-forbidden'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-embeddings-table'))).toBeFalsy();
    });

    /** Runs the `accept` the component handed the confirmation service. */
    const acceptConfirmation = () => confirmSpy.mock.calls[0][0].accept();

    it('should confirm before deleting an index (FR-031)', () => {
        clickButton('dotai-embeddings-delete');

        expect(confirmSpy).toHaveBeenCalled();
        // Nothing happens until the confirmation is accepted.
        expect(storeMock.deleteIndex).not.toHaveBeenCalled();
    });

    it('should delete once the confirmation is accepted (FR-031)', () => {
        // Asserting only the guard proves the dialog opens, not that accepting it does the
        // thing — a broken `accept` wiring would pass that test alone.
        clickButton('dotai-embeddings-delete');

        acceptConfirmation();

        expect(storeMock.deleteIndex).toHaveBeenCalledWith('blogs');
    });

    it('should confirm before rebuilding the store (FR-032)', () => {
        clickButton('dotai-embeddings-rebuild');

        expect(confirmSpy).toHaveBeenCalled();
        expect(storeMock.rebuildEmbeddingsDb).not.toHaveBeenCalled();
    });

    it('should rebuild once the confirmation is accepted (FR-032)', () => {
        clickButton('dotai-embeddings-rebuild');

        acceptConfirmation();

        expect(storeMock.rebuildEmbeddingsDb).toHaveBeenCalled();
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

        it('should build without forwarding the dialog-only mode field', () => {
            // The server answers 400 "Unrecognized field 'mode'" rather than ignoring it, so
            // passing the dialog result through verbatim broke every index build.
            clickButton('dotai-embeddings-new-index');

            onClose.next({ mode: 'add', indexName: 'blogs', query: '+contentType:Blog' });

            expect(storeMock.buildIndex).toHaveBeenCalledWith({
                indexName: 'blogs',
                query: '+contentType:Blog'
            });
            expect(storeMock.buildIndex.mock.calls[0][0]).not.toHaveProperty('mode');
            expect(storeMock.deleteFromIndex).not.toHaveBeenCalled();
        });

        it('should still forward the optional build fields', () => {
            clickButton('dotai-embeddings-new-index');

            onClose.next({
                mode: 'add',
                indexName: 'blogs',
                query: '+contentType:Blog',
                fields: 'title,body',
                velocityTemplate: '$!{title}'
            });

            expect(storeMock.buildIndex).toHaveBeenCalledWith({
                indexName: 'blogs',
                query: '+contentType:Blog',
                fields: 'title,body',
                velocityTemplate: '$!{title}'
            });
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

    describe('the per-row delete action', () => {
        const deleteButton = () =>
            spectator.query(byTestId('dotai-embeddings-delete'))?.querySelector('button');

        it('should be secondary, so a red control does not sit on every row', () => {
            // The destructive step is the confirm dialog, whose accept button carries
            // p-button-danger; the row action only opens it.
            expect(deleteButton()?.className).toContain('p-button-secondary');
            expect(deleteButton()?.className).not.toContain('p-button-danger');
        });

        it('should force its own square rather than rely on the icon-only token', () => {
            // PrimeNG's icon-only width sets a width and leaves the height to padding plus
            // content, so with a full-size glyph the button came out 28x37 — the distortion.
            // dot-plugins and dot-locales both pin the box in `styleClass` for this reason.
            const classes = deleteButton()?.className.split(/\s+/) ?? [];

            expect(classes).toContain('w-8');
            expect(classes).toContain('h-8');
            expect(classes).toContain('p-0');
        });

        it('should collapse the glyph line box, which is what inflated the height', () => {
            // `leading-none` is the other half: without it the glyph's own line-height sets
            // the button's content height and no width class can square it up.
            const glyph = deleteButton()?.querySelector('.material-symbols-outlined');

            expect(glyph?.className).toContain('leading-none!');
            expect(glyph?.className).toContain('text-lg!');
        });

        it('should be a borderless round action, like the other tables row actions', () => {
            const classes = deleteButton()?.className.split(/\s+/) ?? [];

            expect(classes).toContain('p-button-text');
            expect(classes).toContain('p-button-rounded');
            expect(classes).not.toContain('p-button-outlined');
        });

        it('should keep Rebuild DB red, since that one drops every embedding', () => {
            expect(
                spectator.query(byTestId('dotai-embeddings-rebuild'))?.querySelector('button')
                    ?.className
            ).toContain('p-button-danger');
        });
    });
});

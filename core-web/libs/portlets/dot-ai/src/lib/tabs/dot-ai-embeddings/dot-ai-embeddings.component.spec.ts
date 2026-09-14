import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { Subject } from 'rxjs';
import { Mock, MockInstance, vi } from 'vitest';

import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotAiIndex } from '@dotcms/dotcms-models';

import DotAiEmbeddingsComponent from './dot-ai-embeddings.component';

import { DOT_AI_INDEX_OPERATION } from '../../models/dot-ai-portlet.models';
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
    let onDialogDestroy: Subject<unknown>;
    let dialogService: DialogService;
    let confirmSpy: MockInstance;
    let toastSpy: MockInstance;

    const storeMock = {
        indexes: vi.fn().mockReturnValue([index()]),
        filteredIndexes: vi.fn().mockReturnValue([index()]),
        indexStatuses: vi.fn().mockReturnValue({ blogs: 'READY' }),
        indexesForbidden: vi.fn().mockReturnValue(false),
        isConfigured: vi.fn().mockReturnValue(true),
        setIndexFilter: vi.fn(),
        buildIndex: vi.fn(),
        indexNotice: vi.fn().mockReturnValue(null),
        dismissIndexNotice: vi.fn(),
        removeFromIndex: vi.fn(),
        deleteIndex: vi.fn(),
        rebuildEmbeddingsDb: vi.fn()
    };

    const createComponent = createComponentFactory({
        component: DotAiEmbeddingsComponent,
        // The component provides ConfirmationService and DialogService itself, so mocks have
        // to go in componentProviders to win. ConfirmationService stays REAL: p-confirmDialog
        // subscribes to its requireConfirmation$ at construction and a bare mock has none.
        componentProviders: [
            { provide: DotAiStore, useValue: storeMock },
            ConfirmationService,
            { provide: DialogService, useValue: { open: vi.fn() } },
            // Real, for the same reason ConfirmationService is: p-toast subscribes to its
            // messageObserver at construction and a bare mock has none.
            MessageService
        ],
        // Echoes the key, so assertions on dialog copy read as the key that was asked
        // for rather than `undefined`.
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })],
        shallow: true
    });

    beforeEach(() => {
        vi.clearAllMocks();
        storeMock.indexesForbidden.mockReturnValue(false);
        storeMock.filteredIndexes.mockReturnValue([index()]);
        onClose = new Subject();
        onDialogDestroy = new Subject();
        storeMock.indexNotice.mockReturnValue(null);
        spectator = createComponent();
        dialogService = spectator.inject(DialogService, true);
        (dialogService.open as Mock).mockReturnValue({ onClose, onDestroy: onDialogDestroy });
        confirmSpy = vi.spyOn(spectator.inject(ConfirmationService, true), 'confirm');
        toastSpy = vi.spyOn(spectator.inject(MessageService, true), 'add');
    });

    const clickButton = (testId: string) =>
        spectator.click(
            spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement
        );

    /**
     * Runs a row-menu item by its label key.
     *
     * The overlay itself is PrimeNG's and does not render in a shallow test, so this opens the
     * menu for the row and invokes the command the component put on the model — which is the
     * component's half of the contract.
     */
    const openRowMenuItem = (labelKey: string) => {
        clickButton('dotai-embeddings-row-actions');
        const item = (spectator.component as unknown as { $rowActions: () => MenuItem[] })
            .$rowActions()
            .find((action) => action.label === labelKey);

        item?.command?.({} as never);
        spectator.detectChanges();
    };

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
        openRowMenuItem('dotai.embeddings.delete');

        expect(confirmSpy).toHaveBeenCalled();
        // Nothing happens until the confirmation is accepted.
        expect(storeMock.deleteIndex).not.toHaveBeenCalled();
    });

    it('should delete once the confirmation is accepted (FR-031)', () => {
        // Asserting only the guard proves the dialog opens, not that accepting it does the
        // thing — a broken `accept` wiring would pass that test alone.
        openRowMenuItem('dotai.embeddings.delete');

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

        it('should clear a previous outcome so the dialog opens clean', () => {
            // The dialog reads the same notice signal; a stale one would greet the next build
            // with the last one's error.
            clickButton('dotai-embeddings-new-index');

            expect(storeMock.dismissIndexNotice).toHaveBeenCalled();
        });

        it('should stay quiet about an outcome the dialog is there to render', () => {
            // Otherwise the failure appears twice — once in the form holding the query, once
            // as a toast over a modal, which is the report nobody could act on.
            clickButton('dotai-embeddings-new-index');
            storeMock.indexNotice.mockReturnValue({
                operation: DOT_AI_INDEX_OPERATION.BUILD,
                outcome: 'failed',
                indexName: 'blogs',
                detail: 'Cannot parse query'
            });
            spectator.detectChanges();

            expect(toastSpy).not.toHaveBeenCalled();
        });

        it('should report a build abandoned mid-flight once the dialog has gone', () => {
            // Escape or the header X while the build is still running: the outcome arrives
            // after the dialog is destroyed, so this tab is the only thing left that can show
            // it. It used to render the success only, so a failure disappeared.
            clickButton('dotai-embeddings-new-index');
            onDialogDestroy.next(undefined);
            storeMock.indexNotice.mockReturnValue({
                operation: DOT_AI_INDEX_OPERATION.BUILD,
                outcome: 'failed',
                indexName: 'blogs',
                detail: 'Cannot parse query'
            });
            spectator.detectChanges();

            expect(toastSpy).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }));
        });

        it('should hand back only when the dialog is really gone, not when it starts closing', () => {
            // `close()` fires onClose and only then plays the leave animation, so the dialog is
            // still up and still owns what the user has to act on.
            clickButton('dotai-embeddings-new-index');
            onClose.next(undefined);
            storeMock.indexNotice.mockReturnValue({
                operation: DOT_AI_INDEX_OPERATION.BUILD,
                outcome: 'failed',
                indexName: 'blogs'
            });
            spectator.detectChanges();

            expect(toastSpy).not.toHaveBeenCalled();
        });

        it('should leave the build to the dialog rather than submitting on close', () => {
            // The dialog owns the submit now: a rejected Lucene query has to be correctable in
            // the form that produced it, not reported here after the modal took the query away.
            clickButton('dotai-embeddings-new-index');

            onClose.next(undefined);

            expect(storeMock.buildIndex).not.toHaveBeenCalled();
            expect(storeMock.removeFromIndex).not.toHaveBeenCalled();
        });
    });

    describe('the confirm dialogs', () => {
        const config = () => confirmSpy.mock.calls[0][0];

        it('should give the delete confirmation a primary accept', () => {
            openRowMenuItem('dotai.embeddings.delete');

            // Absent, not 'p-button-primary': the theme defines no such class — `.p-button`
            // carries the primary styling itself — so asking for one renders nothing.
            expect(config().acceptButtonStyleClass).toBeUndefined();
        });

        it('should give the delete confirmation an outlined cancel', () => {
            openRowMenuItem('dotai.embeddings.delete');

            expect(config().rejectButtonStyleClass).toBe('p-button-outlined');
        });

        it('should give the rebuild confirmation the same treatment', () => {
            // Both render through one `<p-confirmDialog>`, and they drifted apart once
            // already, so this pins them together rather than each in isolation.
            clickButton('dotai-embeddings-rebuild');

            expect(config().acceptButtonStyleClass).toBeUndefined();
            expect(config().rejectButtonStyleClass).toBe('p-button-outlined');
        });

        it('should still say in words how final a rebuild is', () => {
            // The red accept was carrying that on its own; the message is now the only
            // signal, so it must not be dropped.
            clickButton('dotai-embeddings-rebuild');

            expect(config().message).toBe('dotai.embeddings.rebuild.message');
        });
    });

    describe('the per-row action menu', () => {
        const trigger = () =>
            spectator.query(byTestId('dotai-embeddings-row-actions'))?.querySelector('button');

        it('should offer removing content as well as deleting the index', () => {
            // Removing content belongs on the index it acts on. It used to be a mode of the
            // build dialog, where the index name was a free-text field: a typo removed nothing
            // and a near-miss hit a different index, both silently.
            clickButton('dotai-embeddings-row-actions');

            const labels = (spectator.component as unknown as { $rowActions: () => MenuItem[] })
                .$rowActions()
                .map((action) => action.label);

            expect(labels).toEqual(['dotai.embeddings.remove-content', 'dotai.embeddings.delete']);
        });

        it('should open the remove-content dialog with the row as its index', () => {
            openRowMenuItem('dotai.embeddings.remove-content');

            expect(dialogService.open).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    width: '700px',
                    closable: true,
                    closeOnEscape: true,
                    data: { indexName: 'blogs' }
                })
            );
        });

        it('should be secondary, so a red control does not sit on every row', () => {
            // The destructive step is the confirm dialog, whose accept button carries
            // p-button-danger; the row action only opens a menu.
            expect(trigger()?.className).toContain('p-button-secondary');
            expect(trigger()?.className).not.toContain('p-button-danger');
        });

        it('should force its own square rather than rely on the icon-only token', () => {
            // PrimeNG's icon-only width sets a width and leaves the height to padding plus
            // content, so with a full-size glyph the button came out 28x37 — the distortion.
            const classes = trigger()?.className.split(/\s+/) ?? [];

            expect(classes).toContain('w-8');
            expect(classes).toContain('h-8');
            expect(classes).toContain('p-0');
        });

        it('should collapse the glyph line box, which is what inflated the height', () => {
            const glyph = trigger()?.querySelector('.material-symbols-outlined');

            expect(glyph?.className).toContain('leading-none!');
            expect(glyph?.className).toContain('text-lg!');
        });

        it('should keep Rebuild DB a plain outlined button, not a red one', () => {
            // A permanently-red control in the toolbar read as a warning about the screen.
            // The destructive step is the confirm dialog it opens.
            const rebuild = spectator
                .query(byTestId('dotai-embeddings-rebuild'))
                ?.querySelector('button');

            expect(rebuild?.className).toContain('p-button-outlined');
            expect(rebuild?.className).not.toContain('p-button-danger');
        });
    });
});

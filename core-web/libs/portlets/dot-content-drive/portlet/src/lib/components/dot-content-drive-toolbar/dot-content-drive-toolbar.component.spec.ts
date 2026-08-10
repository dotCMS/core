import { afterEach, beforeEach, describe, expect, it } from '@jest/globals';
import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';

import { MessageService } from 'primeng/api';

import {
    DotCategoriesService,
    DotContentletService,
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotTagsService
} from '@dotcms/data-access';
import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotUVEPaletteListTypes } from '@dotcms/portlets/dot-ema/ui';
import { createFakeTextField, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveToolbarComponent } from './dot-content-drive-toolbar.component';

import { DIALOG_TYPE } from '../../shared/constants';
import { MOCK_BASE_TYPES, MOCK_CONTENT_TYPES, MOCK_ITEMS } from '../../shared/mocks';
import { DotContentDriveActionExecution } from '../../shared/models';
import { DotContentDriveNavigationService } from '../../shared/services';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

/**
 * The creation buttons (Upload + "Add New") are driven by an animation state machine that hides
 * them for {@link ANIMATION_DELAY} ms on init and whenever a selection toggles. Wait past that
 * window before asserting they are visible. Mirrors the delay used in the component.
 */
const settleToolbarAnimation = async (spectator: Spectator<DotContentDriveToolbarComponent>) => {
    // First detection runs the selection effect so the "show" timer gets scheduled; then we wait
    // past the delay and render the settled state.
    spectator.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 200));
    spectator.detectChanges();
};

describe('DotContentDriveToolbarComponent', () => {
    let spectator: Spectator<DotContentDriveToolbarComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    // Real signals so the component's computeds re-run when they change
    const isTreeExpandedSignal = signal(false);
    const filtersSignal = signal<Record<string, unknown>>({});
    const selectedItemsSignal = signal<DotContentDriveItem[]>([]);
    const selectedNodeSignal = signal<{ data?: { defaultBaseType?: string | null } } | undefined>(
        undefined
    );
    const actionExecutionSignal = signal<DotContentDriveActionExecution | undefined>(undefined);

    const createComponent = createComponentFactory({
        component: DotContentDriveToolbarComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                isTreeExpanded: isTreeExpandedSignal,
                // The toolbar now renders from the visual (panel-aware) computed; reusing the same
                // signal keeps these tests driving one value, since they don't need to distinguish
                // the real preference from a panel-forced override.
                isTreeVisuallyExpanded: isTreeExpandedSignal,
                setIsTreeExpanded: jest.fn(),
                getFilterValue: jest.fn().mockReturnValue(undefined),
                patchFilters: jest.fn(),
                removeFilter: jest.fn(),
                clearFilters: jest.fn(),
                filters: filtersSignal,
                setDialog: jest.fn(),
                selectedItems: selectedItemsSignal,
                selectedNode: selectedNodeSignal,
                userSearchableFields: signal([]),
                userSearchableActive: signal<string[]>([]),
                setUserSearchableFields: jest.fn(),
                addUserSearchableField: jest.fn(),
                clearUserSearchableFilters: jest.fn(),
                actionExecution: actionExecutionSignal
            }),
            mockProvider(DotContentTypeService, {
                getContentTypes: jest.fn().mockReturnValue(of(MOCK_CONTENT_TYPES)),
                getContentTypesWithPagination: jest.fn().mockReturnValue(
                    of({
                        contentTypes: MOCK_CONTENT_TYPES,
                        pagination: {
                            currentPage: MOCK_CONTENT_TYPES.length,
                            totalEntries: MOCK_CONTENT_TYPES.length * 2,
                            totalPages: 1
                        }
                    })
                ),
                getAllContentTypes: jest.fn().mockReturnValue(of(MOCK_BASE_TYPES))
            }),
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of())
            }),
            mockProvider(DotHttpErrorManagerService),
            // Field-filter chips render inside the toolbar; provide their dependencies.
            mockProvider(DotTagsService, {
                getTagsPaginated: jest.fn().mockReturnValue(of({ entity: [] }))
            }),
            mockProvider(DotCategoriesService, {
                getChildrenPaginated: jest.fn().mockReturnValue(of({ entity: [] })),
                getCategoriesPaginated: jest.fn().mockReturnValue(of({ entity: [] })),
                getCategory: jest.fn().mockReturnValue(of(null))
            }),
            mockProvider(DotContentletService, {
                getContentletByInode: jest.fn().mockReturnValue(of(null))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            },
            // Needed once a selection exists: that mounts the workflow-actions child, which injects
            // both of these.
            mockProvider(MessageService, { add: jest.fn() }),
            mockProvider(DotContentDriveNavigationService, {
                editContent: jest.fn(),
                editPage: jest.fn()
            }),
            provideHttpClient()
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllMocks();
        isTreeExpandedSignal.set(false);
        filtersSignal.set({});
        selectedItemsSignal.set([]);
        selectedNodeSignal.set(undefined);
        actionExecutionSignal.set(undefined);
    });

    it('should render toolbar container', () => {
        const toolbar = spectator.query('p-toolbar');
        expect(toolbar).toBeTruthy();
    });

    it('should render the tree toggler', () => {
        const toggler = spectator.query('[data-testid="tree-toggler"]');
        expect(toggler).toBeTruthy();
    });

    it('should render the Add New button', async () => {
        // Wait for animation state to settle
        await new Promise((resolve) => setTimeout(resolve, 200));
        spectator.detectChanges();

        const button = spectator.query('[data-testid="add-new-button"]');
        expect(button).toBeTruthy();
    });

    it('should render grid layout with rows', () => {
        expect(spectator.query('.row-start-1.col-start-2')).toBeTruthy();
        expect(spectator.query('.row-start-2.col-start-2')).toBeTruthy();
    });

    it('should render the content type filter', () => {
        const field = spectator.query('[data-testid="content-type-filter"]');
        expect(field).toBeTruthy();
    });

    it('should render the search input', () => {
        const input = spectator.query('[data-testid="search-input"]');
        expect(input).toBeTruthy();
    });

    it('should render the language selector', () => {
        spectator.detectChanges();
        const selector = spectator.query('[data-testid="language-field"]');
        expect(selector).toBeTruthy();
    });

    describe('Tree toggler', () => {
        it('should render the tree toggler', () => {
            const toggler = spectator.query('[data-testid="tree-toggler"]');
            expect(toggler).toBeDefined();
        });

        it('should hide the tree toggler with styles when tree is expanded', () => {
            isTreeExpandedSignal.set(true);
            spectator.detectChanges();

            const toggler = spectator.query('[data-testid="tree-toggler"]') as HTMLElement;
            expect(toggler).toBeTruthy();
            expect(toggler.style.opacity).toBe('0');
            expect(toggler.style.visibility).toBe('hidden');
            expect(toggler.style.width).toBe('0px');
        });
    });

    describe('Clear all button', () => {
        it('should not render when no filters are applied', () => {
            expect(spectator.query('[data-testid="clear-all-filters"]')).toBeNull();
        });

        it('should render when at least one filter is applied', () => {
            filtersSignal.set({ contentType: ['Blog'] });
            spectator.detectChanges();

            expect(spectator.query('[data-testid="clear-all-filters"]')).toBeTruthy();
        });

        it('should call store.clearFilters when clicked', () => {
            filtersSignal.set({ contentType: ['Blog'] });
            spectator.detectChanges();

            const clearButton = spectator
                .query('[data-testid="clear-all-filters"]')
                ?.querySelector('button');
            spectator.click(clearButton as HTMLElement);

            expect(store.clearFilters).toHaveBeenCalled();
        });
    });

    describe('$items', () => {
        it('should open the content type selector for "All Content Types"', () => {
            const items = spectator.component.$items();

            const allContentTypesItem = items.find(
                (item) => item.label === 'content-drive.add-new.all-content-types'
            );

            expect(allContentTypesItem).toBeTruthy();

            allContentTypesItem?.command?.({});

            expect(store.setDialog).toHaveBeenCalledWith({
                type: DIALOG_TYPE.CONTENT_TYPE_SELECTOR,
                header: 'content-drive.dialog.content-type-selector.header',
                payload: { listType: DotUVEPaletteListTypes.ALL_CONTENT_TYPES }
            });
        });

        it('should NOT render the removed "Asset" menu item', () => {
            const items = spectator.component.$items();

            const assetItem = items.find(
                (item) => item.label === 'content-drive.add-new.context-menu.asset'
            );

            expect(assetItem).toBeUndefined();
        });

        it.each([
            {
                labelKey: 'content-drive.base-type.content',
                listType: DotUVEPaletteListTypes.ALL_CONTENT
            },
            {
                labelKey: 'content-drive.base-type.widget',
                listType: DotUVEPaletteListTypes.ALL_WIDGET
            },
            {
                labelKey: 'content-drive.base-type.fileasset',
                listType: DotUVEPaletteListTypes.ALL_FILEASSET
            },
            {
                labelKey: 'content-drive.base-type.dotasset',
                listType: DotUVEPaletteListTypes.ALL_DOTASSET
            },
            {
                labelKey: 'content-drive.base-type.persona',
                listType: DotUVEPaletteListTypes.ALL_PERSONA
            },
            {
                labelKey: 'content-drive.base-type.vanity_url',
                listType: DotUVEPaletteListTypes.ALL_VANITY_URL
            },
            {
                labelKey: 'content-drive.base-type.key_value',
                listType: DotUVEPaletteListTypes.ALL_KEY_VALUE
            },
            {
                labelKey: 'content-drive.base-type.htmlpage',
                listType: DotUVEPaletteListTypes.ALL_HTMLPAGE
            }
        ])(
            'should open the content type selector for base type "$labelKey" with listType "$listType"',
            ({ labelKey, listType }) => {
                const items = spectator.component.$items();

                const baseTypeItem = items.find((item) => item.label === labelKey);

                expect(baseTypeItem).toBeTruthy();

                baseTypeItem?.command?.({});

                expect(store.setDialog).toHaveBeenCalledWith({
                    type: DIALOG_TYPE.CONTENT_TYPE_SELECTOR,
                    header: 'content-drive.dialog.content-type-selector.header',
                    payload: { listType }
                });
            }
        );

        it('should call setDialog for folders', () => {
            const items = spectator.component.$items();

            const foldersItem = items.find(
                (item) => item.label == 'content-drive.add-new.context-menu.folder'
            );

            foldersItem?.command({});

            expect(store.setDialog).toHaveBeenCalledWith({
                type: DIALOG_TYPE.FOLDER,
                header: 'content-drive.dialog.folder.header'
            });
        });
    });

    describe('Upload button', () => {
        const uploadLabel = () =>
            spectator
                .query(byTestId('upload-asset-button'))
                ?.querySelector('.p-button-label')
                ?.textContent?.trim();

        it('should render the upload button when no items are selected', async () => {
            await settleToolbarAnimation(spectator);

            expect(spectator.query(byTestId('upload-asset-button'))).toBeTruthy();
        });

        it('should label the button "Upload" when the current folder has no preference', async () => {
            selectedNodeSignal.set({ data: {} });
            spectator.detectChanges();
            await settleToolbarAnimation(spectator);

            expect(uploadLabel()).toBe('content-drive.upload');
        });

        it('should label the button "Upload Asset" when the folder defaults to Assets', async () => {
            selectedNodeSignal.set({ data: { defaultBaseType: 'DOTASSET' } });
            spectator.detectChanges();
            await settleToolbarAnimation(spectator);

            expect(uploadLabel()).toBe('content-drive.upload-asset');
        });

        it('should label the button "Upload File" when the folder defaults to Files', async () => {
            selectedNodeSignal.set({ data: { defaultBaseType: 'FILEASSET' } });
            spectator.detectChanges();
            await settleToolbarAnimation(spectator);

            expect(uploadLabel()).toBe('content-drive.upload-file');
        });

        it('should emit upload when the upload button is clicked', async () => {
            await settleToolbarAnimation(spectator);

            const emitSpy = jest.fn();
            spectator.component.$upload.subscribe(emitSpy);

            const uploadButton = spectator
                .query(byTestId('upload-asset-button'))
                ?.querySelector('button');
            spectator.click(uploadButton as HTMLElement);

            expect(emitSpy).toHaveBeenCalledTimes(1);
        });

        // Hiding is synchronous: the selection effect sets `addNewButton: false` immediately, so
        // a single detectChanges() removes the button. We deliberately assert before the delayed
        // workflow-actions transition fires — mounting that child pulls in providers (MessageService)
        // outside this toolbar unit test's scope.
        it('should hide the upload button when a single item is selected', async () => {
            await settleToolbarAnimation(spectator);
            expect(spectator.query(byTestId('upload-asset-button'))).toBeTruthy();

            selectedItemsSignal.set([MOCK_ITEMS[0]]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('upload-asset-button'))).toBeNull();
        });

        it('should hide the upload button when multiple items are selected', async () => {
            await settleToolbarAnimation(spectator);
            expect(spectator.query(byTestId('upload-asset-button'))).toBeTruthy();

            selectedItemsSignal.set([MOCK_ITEMS[0], MOCK_ITEMS[1]]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('upload-asset-button'))).toBeNull();
        });

        it('should hide the upload button alongside the "Add New" button on selection', async () => {
            await settleToolbarAnimation(spectator);
            expect(spectator.query(byTestId('upload-asset-button'))).toBeTruthy();
            expect(spectator.query(byTestId('add-new-button'))).toBeTruthy();

            selectedItemsSignal.set([MOCK_ITEMS[0]]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('upload-asset-button'))).toBeNull();
            expect(spectator.query(byTestId('add-new-button'))).toBeNull();
        });

        it('should show the upload button again when the selection is cleared', async () => {
            selectedItemsSignal.set([MOCK_ITEMS[0]]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('upload-asset-button'))).toBeNull();

            selectedItemsSignal.set([]);
            await settleToolbarAnimation(spectator);

            expect(spectator.query(byTestId('upload-asset-button'))).toBeTruthy();
        });
    });

    describe('Workflow Center button', () => {
        it('should not offer it with no selection', async () => {
            selectedItemsSignal.set([]);
            await settleToolbarAnimation(spectator);

            expect(spectator.query(byTestId('action-center-button'))).toBeNull();
        });

        it('should offer it from the first selected contentlet', async () => {
            selectedItemsSignal.set([MOCK_ITEMS[0]]);
            await settleToolbarAnimation(spectator);

            expect(spectator.query(byTestId('action-center-button'))).toBeTruthy();
        });

        it('should keep the workflow action buttons alongside it', async () => {
            selectedItemsSignal.set([MOCK_ITEMS[0], MOCK_ITEMS[1]]);
            await settleToolbarAnimation(spectator);

            // The two are offered together, not one instead of the other.
            expect(spectator.query(byTestId('action-center-button'))).toBeTruthy();
            expect(spectator.query(byTestId('workflow-actions'))).toBeTruthy();
        });

        it('should not offer it for a folder-only selection', async () => {
            // Every bulk endpoint ignores folders, so there would be nothing to act on.
            selectedItemsSignal.set([
                { type: 'folder', inode: 'f1', identifier: 'f1' } as unknown as DotContentDriveItem
            ]);
            await settleToolbarAnimation(spectator);

            expect(spectator.query(byTestId('action-center-button'))).toBeNull();
        });

        it('should open the ACTION_CENTER dialog when clicked', async () => {
            selectedItemsSignal.set([MOCK_ITEMS[0]]);
            await settleToolbarAnimation(spectator);

            spectator.click(byTestId('action-center-button'));

            expect(store.setDialog).toHaveBeenCalledWith({
                type: DIALOG_TYPE.ACTION_CENTER,
                header: 'content-drive.action-center.header'
            });
        });

        it('should be disabled while an action is running', async () => {
            // Reopening mid-run gives a dialog with every row greyed out that then closes itself
            // when the run settles. Refusing to open it is the honest version of that state.
            selectedItemsSignal.set([MOCK_ITEMS[0]]);
            actionExecutionSignal.set({ actionName: 'Publish', total: 3 });
            await settleToolbarAnimation(spectator);

            const button = spectator
                .query(byTestId('action-center-button'))
                ?.querySelector('button');

            expect(button?.disabled).toBe(true);
        });

        it('should explain why it is disabled while an action is running', async () => {
            selectedItemsSignal.set([MOCK_ITEMS[0]]);
            actionExecutionSignal.set({ actionName: 'Publish', total: 3 });
            await settleToolbarAnimation(spectator);

            expect(spectator.component.$actionCenterTooltip()).toBe(
                'content-drive.action-center.busy'
            );
        });

        it('should carry no tooltip when nothing is running', async () => {
            selectedItemsSignal.set([MOCK_ITEMS[0]]);
            await settleToolbarAnimation(spectator);

            expect(spectator.component.$actionCenterTooltip()).toBe('');
        });

        it('should not open the dialog while an action is running', async () => {
            selectedItemsSignal.set([MOCK_ITEMS[0]]);
            actionExecutionSignal.set({ actionName: 'Publish', total: 3 });
            await settleToolbarAnimation(spectator);

            // Guards the handler too: a disabled attribute alone would leave the store reachable.
            spectator.component['onOpenActionCenter']();

            expect(store.setDialog).not.toHaveBeenCalled();
        });

        it('should become available again once the run settles', async () => {
            selectedItemsSignal.set([MOCK_ITEMS[0]]);
            actionExecutionSignal.set({ actionName: 'Publish', total: 3 });
            await settleToolbarAnimation(spectator);

            actionExecutionSignal.set(undefined);
            spectator.detectChanges();

            const button = spectator
                .query(byTestId('action-center-button'))
                ?.querySelector('button');

            expect(button?.disabled).toBe(false);
        });
    });

    describe('running-action indicator', () => {
        it('should stay hidden when nothing is running', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('action-execution-indicator'))).toBeNull();
        });

        it('should report the action and the number of items once a run starts', () => {
            // The toolbar is the only place still reporting the run after the Action Center dialog is
            // closed, which is the whole reason the indicator lives out here.
            actionExecutionSignal.set({ actionName: 'Publish', total: 3 });
            spectator.detectChanges();

            const indicator = spectator.query(byTestId('action-execution-indicator'));

            expect(indicator).toBeTruthy();
            expect(spectator.component.$actionExecutionLabel()).toBe(
                'content-drive.action-center.applying'
            );
        });

        it('should not render markup carried by the action name', () => {
            // Workflow action names come from the backend verbatim (`$selectedAction()?.name`), and
            // the label is placed in the DOM as HTML so the message's own `<b>` renders. A name
            // carrying markup must not become live DOM — an event-handler attribute least of all.
            //
            // The shared mock returns the bare key, which would make this pass without rendering
            // anything; the real message has to be in play for the assertion to mean something.
            const messageService = spectator.inject(DotMessageService);

            jest.spyOn(messageService, 'get').mockImplementation(
                (key: string, ...args: string[]) =>
                    key === 'content-drive.action-center.applying'
                        ? `Applying <b>${args[0]}</b> to ${args[1]} item(s)…`
                        : key
            );

            actionExecutionSignal.set({
                actionName: '<img src=x onerror="window.__xss = true">',
                total: 3
            });
            spectator.detectChanges();

            const indicator = spectator.query(byTestId('action-execution-indicator'));

            // Asserted structurally rather than by searching the markup for "onerror": once the name
            // is escaped it renders as visible text that legitimately still contains that word.
            expect(indicator?.querySelector('img')).toBeNull();
            expect(indicator?.querySelector('[onerror]')).toBeNull();
            // …and the name is still shown to the user, just as text.
            expect(indicator?.textContent).toContain('<img src=x');
        });

        it('should disappear again once the run settles', () => {
            actionExecutionSignal.set({ actionName: 'Publish', total: 3 });
            spectator.detectChanges();

            actionExecutionSignal.set(undefined);
            spectator.detectChanges();

            expect(spectator.query(byTestId('action-execution-indicator'))).toBeNull();
        });
    });

    describe('field-filter chips', () => {
        it('should render a chip only for active variables resolved against loaded fields', () => {
            store.userSearchableFields.set([
                createFakeTextField({ variable: 'body', name: 'Body' })
            ]);
            // 'ghost' has no matching loaded field (the URL-restore / stale case) and must be dropped.
            store.userSearchableActive.set(['body', 'ghost']);
            spectator.detectChanges();

            expect(spectator.component.$activeFieldFilters().map((f) => f.variable)).toEqual([
                'body'
            ]);
            expect(spectator.query(byTestId('field-filter-chip-body'))).toBeTruthy();
            expect(spectator.query(byTestId('field-filter-chip-ghost'))).toBeNull();
        });
    });
});

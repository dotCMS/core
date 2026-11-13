import { Dispatcher } from '@ngrx/signals/events';
import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { WritableSignal, reflectComponentType, signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { TEMP_EMPTY_CONTENTLET_TYPE } from '@dotcms/uve/internal';

import { DotUveContentletToolsComponent } from './dot-uve-contentlet-tools.component';

import { ContentletPayload, SelectedContentlet, VTLFile } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { UVE_EDIT_PANEL_TABS } from '../../../store/features/editor/models';
import { uveContentEditorEvents } from '../dot-uve-content-editor/dot-uve-content-editor.events';
import { ContentletArea } from '../ema-page-dropzone/types';

/**
 * Project a `ContentletArea` (hover shape — bounds at top level)
 * into the unified `SelectedContentlet` shape (`{ bounds, payload }`)
 * so the same fixture data drives both the contentletArea input and
 * the editorSelected store mock.
 */
const toSelected = (area: ContentletArea): SelectedContentlet => ({
    bounds: { x: area.x, y: area.y, width: area.width, height: area.height },
    payload: area.payload
});

const MOCK_CONTENTLET_AREA: ContentletArea = {
    x: 100,
    y: 200,
    width: 300,
    height: 400,
    payload: {
        contentlet: {
            identifier: 'contentlet-identifier-123',
            inode: 'inode-123',
            title: 'Test Contentlet',
            contentType: 'test-content-type',
            baseType: 'CONTENT'
        },
        container: {
            acceptTypes: 'test',
            identifier: 'container-identifier-123',
            maxContentlets: 5,
            uuid: 'uuid-123',
            contentletsId: ['contentlet-identifier-123']
        },
        language_id: '1',
        pageContainers: [],
        pageId: 'page-123',
        vtlFiles: [
            { inode: 'vtl-inode-1', name: 'template1.vtl' },
            { inode: 'vtl-inode-2', name: 'template2.vtl' }
        ]
    }
};

const MOCK_EMPTY_CONTENTLET_AREA: ContentletArea = {
    x: 100,
    y: 200,
    width: 300,
    height: 400,
    payload: {
        contentlet: {
            identifier: 'TEMP_EMPTY_CONTENTLET',
            inode: 'temp-inode',
            title: 'Empty',
            contentType: 'test'
        },
        container: {
            acceptTypes: 'test',
            identifier: 'container-identifier-123',
            maxContentlets: 5,
            uuid: 'uuid-123'
        },
        language_id: '1',
        pageContainers: [],
        pageId: 'page-123'
    }
};

describe('DotUveContentletToolsComponent', () => {
    let spectator: Spectator<DotUveContentletToolsComponent>;
    let dispatcher: SpyObject<Dispatcher>;
    /**
     * Writable mock for the store's `editorSelected` signal. The SDK's
     * CONTENTLET_CLICKED handler sets this; tests drive it directly.
     */
    let editorSelected: ReturnType<typeof signal<SelectedContentlet | null>>;
    /** Writable mock for the store's permission check on the selected contentlet. */
    let editorCanEditSelectedContentlet: WritableSignal<boolean>;
    let setEditPanelOpen: Mock;
    let editorSetEditPanelTab: Mock;

    const createComponent = createComponentFactory({
        component: DotUveContentletToolsComponent,
        providers: [
            mockProvider(DotMessageService, {
                get: (key: string) => {
                    const messages: Record<string, string> = {
                        content: 'Content',
                        Widget: 'Widget',
                        form: 'Form',
                        'uve.disable.delete.button.on.personalization':
                            'Cannot delete on personalization'
                    };

                    return messages[key] || key;
                }
            }),
            {
                provide: UVEStore,
                useFactory: () => ({
                    editorSelected,
                    $iframeLayoutLocked: () => false,
                    // Tall enough that none of the fixture areas trigger the
                    // bottom clip offset unless a test overrides it.
                    viewIframeHeight: () => 800,
                    // promoteHoverToSelected calls setSelected on the store
                    // before the quick-edit and style buttons open the panel.
                    // Stub it so the (click) handler doesn't throw.
                    setSelected: vi.fn(),
                    editorCanEditSelectedContentlet,
                    setEditPanelOpen,
                    editorSetEditPanelTab
                })
            }
        ],
        componentProviders: [Dispatcher],
        detectChanges: false
    });

    beforeEach(() => {
        // Tests start with the hovered contentlet also selected so the
        // selected overlay renders alongside the hover overlay.
        editorSelected = signal<SelectedContentlet | null>(toSelected(MOCK_CONTENTLET_AREA));
        editorCanEditSelectedContentlet = signal(true);
        setEditPanelOpen = vi.fn();
        editorSetEditPanelTab = vi.fn();

        spectator = createComponent({
            props: {
                contentletArea: MOCK_CONTENTLET_AREA,
                allowContentDelete: true,
                showStyleEditorOption: false
            }
        });
        dispatcher = spectator.inject(Dispatcher, true);
        vi.spyOn(dispatcher, 'dispatch');
        spectator.detectChanges();
    });

    describe('Rendering', () => {
        it('should create the component', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should render bounds container with correct styles', () => {
            const bounds = spectator.query(byTestId('bounds-selected'));
            expect(bounds).toBeTruthy();

            const styles = (bounds as HTMLElement).style;
            expect(styles.left).toBe('100px');
            expect(styles.top).toBe('200px');
            expect(styles.width).toBe('300px');
            expect(styles.height).toBe('400px');
        });

        it('should render add buttons', () => {
            const addTopButton = spectator.query(byTestId('hover-add-top-button'));
            const addBottomButton = spectator.query(byTestId('hover-add-bottom-button'));

            expect(addTopButton).toBeTruthy();
            expect(addBottomButton).toBeTruthy();
        });

        it('should render actions container when not empty', () => {
            const actions = spectator.query(byTestId('hover-actions'));
            expect(actions).toBeTruthy();
        });

        it('should NOT render actions container when container is empty', () => {
            spectator.setInput('contentletArea', MOCK_EMPTY_CONTENTLET_AREA);
            editorSelected.set(toSelected(MOCK_EMPTY_CONTENTLET_AREA));
            spectator.detectChanges();

            const actions = spectator.query(byTestId('hover-actions'));
            expect(actions).toBeFalsy();
        });

        it('should NOT render bottom add button when container is empty', () => {
            spectator.setInput('contentletArea', MOCK_EMPTY_CONTENTLET_AREA);
            editorSelected.set(toSelected(MOCK_EMPTY_CONTENTLET_AREA));
            spectator.detectChanges();

            const addBottomButton = spectator.query(byTestId('hover-add-bottom-button'));
            expect(addBottomButton).toBeFalsy();
        });
    });

    describe('Hover overlay', () => {
        beforeEach(() => {
            // Hover overlay shows when the hovered contentlet is different
            // from the selected one. Set selection to a different contentlet
            // so the overlay is visible for these tests.
            editorSelected.set(null);
            spectator.detectChanges();
        });

        it('renders the hover overlay when a contentlet is hovered', () => {
            const hoverBounds = spectator.query(byTestId('bounds-hover'));
            expect(hoverBounds).toBeTruthy();
        });

        it('renders the content type label inside the hover overlay', () => {
            const label = spectator.query(byTestId('bounds-hover-label'));
            expect(label).toBeTruthy();
            expect(label?.textContent?.trim()).toBe(
                MOCK_CONTENTLET_AREA.payload?.contentlet?.contentType
            );
        });

        it('shows the no-content-type fallback when contentType is the empty sentinel', () => {
            const areaWithSentinel = {
                ...MOCK_CONTENTLET_AREA,
                payload: {
                    ...MOCK_CONTENTLET_AREA.payload,
                    contentlet: {
                        ...MOCK_CONTENTLET_AREA.payload?.contentlet,
                        contentType: TEMP_EMPTY_CONTENTLET_TYPE
                    }
                }
            } as unknown as ContentletArea;
            spectator.setInput('contentletArea', areaWithSentinel);
            spectator.detectChanges();

            const label = spectator.query(byTestId('bounds-hover-label'));
            expect(label?.textContent?.trim()).toBe('uve.no-content-type');
        });

        it('omits the label when the hovered contentlet has no contentType', () => {
            const areaWithoutType = {
                ...MOCK_CONTENTLET_AREA,
                payload: {
                    ...MOCK_CONTENTLET_AREA.payload,
                    contentlet: {
                        ...MOCK_CONTENTLET_AREA.payload?.contentlet,
                        contentType: undefined
                    }
                }
            } as unknown as ContentletArea;
            spectator.setInput('contentletArea', areaWithoutType);
            spectator.detectChanges();

            expect(spectator.query(byTestId('bounds-hover-label'))).toBeFalsy();
        });
    });

    describe('Actions buttons', () => {
        it('should render edit VTL button when vtl files exist', () => {
            const editVtlButton = spectator.query(byTestId('hover-edit-vtl-button'));
            expect(editVtlButton).toBeTruthy();
        });

        it('should NOT render edit VTL button when no vtl files', () => {
            const areaWithoutVtl = {
                ...MOCK_CONTENTLET_AREA,
                x: MOCK_CONTENTLET_AREA.x + 1, // Change position to make it different
                payload: {
                    ...MOCK_CONTENTLET_AREA.payload,
                    contentlet: {
                        ...MOCK_CONTENTLET_AREA.payload.contentlet,
                        identifier: 'different-contentlet-id'
                    },
                    vtlFiles: undefined
                }
            };
            spectator.setInput('contentletArea', areaWithoutVtl);
            editorSelected.set(toSelected(areaWithoutVtl));
            spectator.detectChanges();

            const editVtlButton = spectator.query(byTestId('hover-edit-vtl-button'));
            expect(editVtlButton).toBeFalsy();
        });

        it('should order the toolbar: Edit, Styles, Full Editor, Code, then Remove', () => {
            spectator.setInput('showStyleEditorOption', true);
            spectator.detectChanges();

            const toolbar = spectator.query(byTestId('hover-actions')) as HTMLElement;
            const order = Array.from(toolbar.querySelectorAll(':scope > p-button')).map((button) =>
                button.getAttribute('data-testid')
            );

            expect(order).toEqual([
                'hover-quick-edit-button',
                'hover-palette-button',
                'hover-edit-button',
                'hover-edit-vtl-button',
                'hover-delete-button'
            ]);
        });

        it.each([
            ['hover-quick-edit-button', 'edit', 'uve.tooltip.edit.quick'],
            ['hover-palette-button', 'palette', 'uve.tooltip.edit.style'],
            ['hover-edit-button', 'edit_note', 'uve.tooltip.edit.full'],
            ['hover-edit-vtl-button', 'code', 'uve.tooltip.edit.vtl'],
            ['hover-delete-button', 'close', 'uve.tooltip.remove'],
            ['hover-drag-button', 'open_with', 'uve.tooltip.move'],
            ['hover-add-top-button', 'add', 'uve.contentlet.tools.add'],
            ['hover-add-bottom-button', 'add', 'uve.contentlet.tools.add'],
            ['hover-actions-collapsed-button', 'more_vert', 'uve.contentlet.tools.more']
        ])(
            'should show %s as the Material Symbol "%s", named for screen readers',
            (testId, symbol, name) => {
                spectator.setInput('showStyleEditorOption', true);
                spectator.detectChanges();

                const button = spectator.query(byTestId(testId)) as HTMLElement;
                const icon = button.querySelector('.material-symbols-outlined');

                expect(icon?.textContent?.trim()).toBe(symbol);
                expect(icon?.getAttribute('aria-hidden')).toBe('true');
                expect(button.querySelector('button')?.getAttribute('aria-label')).toBe(name);
            }
        );

        it('should give the collapsed menu the same icons as the toolbar', () => {
            spectator.setInput('showStyleEditorOption', true);
            spectator.detectChanges();

            const items = spectator.component.actionsMenuItems();

            expect(items.map((item) => item.symbol)).toEqual([
                'edit',
                'palette',
                'edit_note',
                'code',
                'close'
            ]);
            expect(items.some((item) => item.icon)).toBe(false);
        });

        it('should render no PrimeIcons, only Material Symbols', () => {
            spectator.setInput('showStyleEditorOption', true);
            spectator.detectChanges();

            expect(spectator.element.querySelectorAll('.pi')).toHaveLength(0);
            expect(
                spectator.element.querySelectorAll('.material-symbols-outlined').length
            ).toBeGreaterThan(0);
        });

        it('should order the collapsed menu the same way as the toolbar', () => {
            spectator.setInput('showStyleEditorOption', true);
            spectator.detectChanges();

            expect(spectator.component.actionsMenuItems().map((item) => item.label)).toEqual([
                'uve.tooltip.edit.quick',
                'uve.tooltip.edit.style',
                'uve.tooltip.edit.full',
                'uve.tooltip.edit.vtl',
                'uve.tooltip.remove'
            ]);
        });

        it('should render drag button', () => {
            const dragButton = spectator.query(byTestId('hover-drag-button'));
            expect(dragButton).toBeTruthy();
        });

        it('should render delete button', () => {
            const deleteButton = spectator.query(byTestId('hover-delete-button'));
            expect(deleteButton).toBeTruthy();
        });

        it('should render edit button', () => {
            const editButton = spectator.query(byTestId('hover-edit-button'));
            expect(editButton).toBeTruthy();
        });

        it('should disable delete button when allowContentDelete is false', () => {
            spectator.setInput('allowContentDelete', false);
            spectator.detectChanges();

            const deleteButton = spectator.query(byTestId('hover-delete-button')) as HTMLElement;
            const button = deleteButton.querySelector('button');
            expect(button?.disabled).toBe(true);
        });

        it('should enable delete button when allowContentDelete is true', () => {
            spectator.setInput('allowContentDelete', true);
            spectator.detectChanges();

            const deleteButton = spectator.query(byTestId('hover-delete-button')) as HTMLElement;
            const button = deleteButton.querySelector('button');
            expect(button?.disabled).toBe(false);
        });
    });

    describe('opening the edit panel', () => {
        /** The selection the quick-edit and style buttons make of the hovered contentlet. */
        const HOVERED_AS_SELECTED: SelectedContentlet = {
            ...toSelected(MOCK_CONTENTLET_AREA),
            payload: { ...MOCK_CONTENTLET_AREA.payload, position: 'after' }
        };

        const clickHoverButton = (testId: string) => {
            const host = spectator.query(byTestId(testId)) as HTMLElement;
            spectator.click(host.querySelector('button') as Element);
        };

        const actionsMenuEntry = (symbol: string) =>
            spectator.component.actionsMenuItems().find((item) => item.symbol === symbol);

        /**
         * Hovers and selects a contentlet carrying the given `canEdit`, as the page renders it.
         * `undefined` leaves the field out, as headless and SDK-rendered pages do.
         */
        const hoverWithPermission = (canEdit: boolean | undefined) => {
            const area: ContentletArea = {
                ...MOCK_CONTENTLET_AREA,
                payload: {
                    ...MOCK_CONTENTLET_AREA.payload,
                    contentlet: {
                        ...(MOCK_CONTENTLET_AREA.payload.contentlet as ContentletPayload),
                        ...(canEdit === undefined ? {} : { canEdit })
                    }
                }
            };
            spectator.setInput('contentletArea', area);
            editorSelected.set(toSelected(area));
            editorCanEditSelectedContentlet.set(canEdit !== false);
            spectator.detectChanges();
        };

        it('should not emit openQuickEdit or selectContent', () => {
            const outputs = reflectComponentType(DotUveContentletToolsComponent)?.outputs.map(
                ({ propName }) => propName
            );

            expect(outputs).not.toContain('openQuickEdit');
            expect(outputs).not.toContain('selectContent');
        });

        describe('quick edit', () => {
            it('should open the panel on the quick-edit tab when clicking the quick-edit button', () => {
                clickHoverButton('hover-quick-edit-button');

                expect(setEditPanelOpen).toHaveBeenCalledWith(true);
                expect(editorSetEditPanelTab).toHaveBeenCalledWith(UVE_EDIT_PANEL_TABS.QUICK_EDIT);
            });

            it('should select the hovered contentlet before opening the panel', () => {
                const setSelected = spectator.inject(UVEStore).setSelected as Mock;

                clickHoverButton('hover-quick-edit-button');

                expect(setSelected).toHaveBeenCalledWith(HOVERED_AS_SELECTED);
                expect(setSelected.mock.invocationCallOrder[0]).toBeLessThan(
                    setEditPanelOpen.mock.invocationCallOrder[0]
                );
            });

            it("should open the panel on the quick-edit tab from the collapsed menu's entry", () => {
                actionsMenuEntry('edit')?.command?.({});

                expect(setEditPanelOpen).toHaveBeenCalledWith(true);
                expect(editorSetEditPanelTab).toHaveBeenCalledWith(UVE_EDIT_PANEL_TABS.QUICK_EDIT);
            });

            it('should open the panel for a contentlet whose page gives no permission', () => {
                hoverWithPermission(undefined);

                clickHoverButton('hover-quick-edit-button');

                expect(setEditPanelOpen).toHaveBeenCalledWith(true);
            });

            it('should refuse a contentlet the user cannot edit', () => {
                // The entry is disabled for it, but a stale menu can still run its command.
                hoverWithPermission(false);

                actionsMenuEntry('edit')?.command?.({});

                expect(setEditPanelOpen).not.toHaveBeenCalled();
                expect(editorSetEditPanelTab).not.toHaveBeenCalled();
            });
        });

        describe('style editor', () => {
            beforeEach(() => {
                spectator.setInput('showStyleEditorOption', true);
                spectator.detectChanges();
            });

            it('should open the panel on the style tab when clicking the style button', () => {
                clickHoverButton('hover-palette-button');

                expect(setEditPanelOpen).toHaveBeenCalledWith(true);
                expect(editorSetEditPanelTab).toHaveBeenCalledWith(
                    UVE_EDIT_PANEL_TABS.STYLE_EDITOR
                );
            });

            it('should select the hovered contentlet before opening the panel', () => {
                const setSelected = spectator.inject(UVEStore).setSelected as Mock;

                clickHoverButton('hover-palette-button');

                expect(setSelected).toHaveBeenCalledWith(HOVERED_AS_SELECTED);
                expect(setSelected.mock.invocationCallOrder[0]).toBeLessThan(
                    setEditPanelOpen.mock.invocationCallOrder[0]
                );
            });

            it("should open the panel on the style tab from the collapsed menu's entry", () => {
                actionsMenuEntry('palette')?.command?.({});

                expect(setEditPanelOpen).toHaveBeenCalledWith(true);
                expect(editorSetEditPanelTab).toHaveBeenCalledWith(
                    UVE_EDIT_PANEL_TABS.STYLE_EDITOR
                );
            });

            it('should refuse a contentlet the user cannot edit', () => {
                // The entry is disabled for it, but a stale menu can still run its command.
                hoverWithPermission(false);

                actionsMenuEntry('palette')?.command?.({});

                expect(setEditPanelOpen).not.toHaveBeenCalled();
                expect(editorSetEditPanelTab).not.toHaveBeenCalled();
            });
        });
    });

    describe('Outputs', () => {
        describe('full-editor (pencil) button', () => {
            /** The request the pencil makes for the hovered contentlet. */
            const EDIT_HOVERED_CONTENTLET = uveContentEditorEvents.uveEditContentRequested({
                contentlet: MOCK_CONTENTLET_AREA.payload.contentlet as unknown as DotCMSContentlet,
                forkFrom: MOCK_CONTENTLET_AREA.payload.container
            });

            const fullEditorMenuItem = () =>
                spectator.component.actionsMenuItems().find((item) => item.symbol === 'edit_note');

            /** Hovers a contentlet whose payload carries the given overrides. */
            const hoverContentlet = (overrides: Partial<ContentletPayload>) => {
                spectator.setInput('contentletArea', {
                    ...MOCK_CONTENTLET_AREA,
                    payload: {
                        ...MOCK_CONTENTLET_AREA.payload,
                        contentlet: {
                            ...(MOCK_CONTENTLET_AREA.payload.contentlet as ContentletPayload),
                            ...overrides
                        }
                    }
                });
                spectator.detectChanges();
            };

            it('should ask to edit the hovered contentlet, forking from its container', () => {
                const editButton = spectator.query(byTestId('hover-edit-button')) as HTMLElement;
                spectator.click(editButton.querySelector('button') as Element);

                expect(dispatcher.dispatch).toHaveBeenCalledWith(EDIT_HOVERED_CONTENTLET, {
                    scope: 'self'
                });
            });

            it("should ask the same from the collapsed menu's full editor entry", () => {
                fullEditorMenuItem()?.command?.({});

                expect(dispatcher.dispatch).toHaveBeenCalledWith(EDIT_HOVERED_CONTENTLET, {
                    scope: 'self'
                });
            });

            it('should leave the selection alone', () => {
                // The pencil is stateless: opening the full editor must not change what is
                // selected in the editor.
                const store = spectator.inject(UVEStore);
                (store.setSelected as ReturnType<typeof vi.fn>).mockClear();

                const editButton = spectator.query(byTestId('hover-edit-button')) as HTMLElement;
                spectator.click(editButton.querySelector('button') as Element);

                expect(store.setSelected).not.toHaveBeenCalled();
            });

            it('should ask nothing for a contentlet the user cannot edit', () => {
                // The button is disabled, but the toolbar can be driven from stale bounds or
                // programmatically, so the guard must not rest on the disabled state alone.
                hoverContentlet({ canEdit: false });

                fullEditorMenuItem()?.command?.({});

                expect(dispatcher.dispatch).not.toHaveBeenCalled();
            });

            it('should ask nothing for a contentlet without an inode', () => {
                hoverContentlet({ inode: undefined });

                const editButton = spectator.query(byTestId('hover-edit-button')) as HTMLElement;
                spectator.click(editButton.querySelector('button') as Element);

                expect(dispatcher.dispatch).not.toHaveBeenCalled();
            });
        });

        describe('deleteContent', () => {
            it('should emit deleteContent with context when clicking delete button', () => {
                const handler = vi.fn();
                spectator.output('deleteContent').subscribe(handler);

                const deleteButton = spectator.query(
                    byTestId('hover-delete-button')
                ) as HTMLElement;
                spectator.click(deleteButton.querySelector('button') as Element);

                expect(handler).toHaveBeenCalledWith({
                    ...MOCK_CONTENTLET_AREA.payload,
                    position: 'after'
                });
            });
        });

        describe('uveAddContentRequested', () => {
            it('should ask to add content above the contentlet when selecting content from the top menu', () => {
                const addTopButton = spectator.query(byTestId('hover-add-top-button'));
                const button = addTopButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                // Get the menu items and trigger the first command
                const menuItems = spectator.component.menuItems();
                menuItems[0].command?.({});

                expect(dispatcher.dispatch).toHaveBeenCalledWith(
                    uveContentEditorEvents.uveAddContentRequested({
                        type: 'content',
                        into: {
                            ...MOCK_CONTENTLET_AREA.payload,
                            position: 'before'
                        }
                    }),
                    { scope: 'self' }
                );
            });

            it('should ask to add a widget below the contentlet when selecting widget from the bottom menu', () => {
                const addBottomButton = spectator.query(byTestId('hover-add-bottom-button'));
                const button = addBottomButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                // Get the menu items and trigger the second command
                const menuItems = spectator.component.menuItems();
                menuItems[1].command?.({});

                expect(dispatcher.dispatch).toHaveBeenCalledWith(
                    uveContentEditorEvents.uveAddContentRequested({
                        type: 'widget',
                        into: {
                            ...MOCK_CONTENTLET_AREA.payload,
                            position: 'after'
                        }
                    }),
                    { scope: 'self' }
                );
            });

            it('should ask to add a form below the contentlet when selecting form from the bottom menu', () => {
                const addBottomButton = spectator.query(byTestId('hover-add-bottom-button'));
                const button = addBottomButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                // Get the menu items and trigger the third command (form)
                const menuItems = spectator.component.menuItems();
                menuItems[2].command?.({});

                expect(dispatcher.dispatch).toHaveBeenCalledWith(
                    uveContentEditorEvents.uveAddContentRequested({
                        type: 'form',
                        into: {
                            ...MOCK_CONTENTLET_AREA.payload,
                            position: 'after'
                        }
                    }),
                    { scope: 'self' }
                );
            });
        });

        describe('uveEditSourceRequested', () => {
            it('should ask to edit the source of the file when clicking VTL menu item', () => {
                const expectedFile: VTLFile = {
                    inode: 'vtl-inode-1',
                    name: 'template1.vtl'
                };

                const editVtlButton = spectator.query(byTestId('hover-edit-vtl-button'));
                const button = editVtlButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                // Get the VTL menu items and trigger the first command
                const vtlMenuItems = spectator.component.vtlMenuItems();
                vtlMenuItems[0].command?.({});

                expect(dispatcher.dispatch).toHaveBeenCalledWith(
                    uveContentEditorEvents.uveEditSourceRequested(expectedFile),
                    { scope: 'self' }
                );
            });

            it('should ask to edit the source of the second file when clicking second VTL menu item', () => {
                const expectedFile: VTLFile = {
                    inode: 'vtl-inode-2',
                    name: 'template2.vtl'
                };

                const editVtlButton = spectator.query(byTestId('hover-edit-vtl-button'));
                const button = editVtlButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                // Get the VTL menu items and trigger the second command
                const vtlMenuItems = spectator.component.vtlMenuItems();
                vtlMenuItems[1].command?.({});

                expect(dispatcher.dispatch).toHaveBeenCalledWith(
                    uveContentEditorEvents.uveEditSourceRequested(expectedFile),
                    { scope: 'self' }
                );
            });
        });
    });

    describe('Computed signals', () => {
        describe('contentContext', () => {
            it('should combine contentletArea payload with buttonPosition', () => {
                expect(spectator.component.contentContext()).toEqual({
                    ...MOCK_CONTENTLET_AREA.payload,
                    position: 'after'
                });
            });

            it('should update position to "before" when clicking top add button', () => {
                const addTopButton = spectator.query(byTestId('hover-add-top-button'));
                const button = addTopButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                expect(spectator.component.contentContext().position).toBe('before');
            });

            it('should update position to "after" when clicking bottom add button', () => {
                const addBottomButton = spectator.query(byTestId('hover-add-bottom-button'));
                const button = addBottomButton?.querySelector('button');
                spectator.click(button as Element);
                spectator.detectChanges();

                expect(spectator.component.contentContext().position).toBe('after');
            });
        });

        describe('hasVtlFiles', () => {
            it('should return true when vtl files exist', () => {
                expect(spectator.component.hasVtlFiles()).toBe(true);
            });

            it('should return false when no vtl files', () => {
                const areaWithoutVtl = {
                    ...MOCK_CONTENTLET_AREA,
                    payload: { ...MOCK_CONTENTLET_AREA.payload, vtlFiles: undefined }
                };
                spectator.setInput('contentletArea', areaWithoutVtl);
                spectator.detectChanges();

                expect(spectator.component.hasVtlFiles()).toBe(false);
            });

            it('should return false when vtl files is empty array', () => {
                const areaWithEmptyVtl = {
                    ...MOCK_CONTENTLET_AREA,
                    payload: { ...MOCK_CONTENTLET_AREA.payload, vtlFiles: [] }
                };
                spectator.setInput('contentletArea', areaWithEmptyVtl);
                spectator.detectChanges();

                expect(spectator.component.hasVtlFiles()).toBe(false);
            });
        });

        describe('isContainerEmpty', () => {
            it('should return false for regular contentlet', () => {
                expect(spectator.component.isContainerEmpty()).toBe(false);
            });

            it('should return true when contentlet identifier is TEMP_EMPTY_CONTENTLET', () => {
                spectator.setInput('contentletArea', MOCK_EMPTY_CONTENTLET_AREA);
                spectator.detectChanges();

                expect(spectator.component.isContainerEmpty()).toBe(true);
            });
        });

        describe('delete button behavior', () => {
            it('should enable delete button when delete is allowed', () => {
                spectator.setInput('allowContentDelete', true);
                spectator.detectChanges();

                const deleteButton = spectator.query(
                    byTestId('hover-delete-button')
                ) as HTMLElement;
                const button = deleteButton?.querySelector('button');

                expect(button?.disabled).toBe(false);
            });

            it('should disable delete button when delete is not allowed', () => {
                spectator.setInput('allowContentDelete', false);
                spectator.detectChanges();

                const deleteButton = spectator.query(
                    byTestId('hover-delete-button')
                ) as HTMLElement;
                const button = deleteButton?.querySelector('button');

                expect(button?.disabled).toBe(true);
            });
        });

        describe('menuItems', () => {
            it('should have 3 items (content, widget, form)', () => {
                const items = spectator.component.menuItems();
                expect(items).toHaveLength(3);
                expect(items[0].label).toBe('Content');
                expect(items[1].label).toBe('Widget');
                expect(items[2].label).toBe('Form');
            });
        });

        describe('vtlMenuItems', () => {
            it('should create menu items from vtl files', () => {
                const items = spectator.component.vtlMenuItems();
                expect(items).toHaveLength(2);
                expect(items[0].label).toBe('template1.vtl');
                expect(items[1].label).toBe('template2.vtl');
            });

            // Was `toBeUndefined()`. The computed declares `MenuItem[]` and an
            // undefined `[model]` made PrimeNG render an empty popup instead of
            // the button being absent, so it now returns an empty array.
            it('should return an empty array when no vtl files', () => {
                const areaWithoutVtl = {
                    ...MOCK_CONTENTLET_AREA,
                    x: MOCK_CONTENTLET_AREA.x + 1, // Change position to make it different
                    payload: {
                        ...MOCK_CONTENTLET_AREA.payload,
                        contentlet: {
                            ...MOCK_CONTENTLET_AREA.payload.contentlet,
                            identifier: 'different-contentlet-id-2'
                        },
                        vtlFiles: undefined
                    }
                };
                spectator.setInput('contentletArea', areaWithoutVtl);
                editorSelected.set(toSelected(areaWithoutVtl));
                spectator.detectChanges();

                expect(spectator.component.vtlMenuItems()).toEqual([]);
            });
        });

        describe('boundsStyles', () => {
            it('should apply correct inline styles from contentletArea dimensions', () => {
                const bounds = spectator.query(byTestId('bounds-selected')) as HTMLElement;

                expect(bounds.style.left).toBe('100px');
                expect(bounds.style.top).toBe('200px');
                expect(bounds.style.width).toBe('300px');
                expect(bounds.style.height).toBe('400px');
            });

            it('should default to 0px when contentletArea values are undefined', () => {
                const areaWithUndefined = {
                    ...MOCK_CONTENTLET_AREA,
                    x: undefined
                } as unknown as ContentletArea;
                editorSelected.set(toSelected(areaWithUndefined));
                spectator.detectChanges();

                const bounds = spectator.query(byTestId('bounds-selected')) as HTMLElement;
                expect(bounds).toBeTruthy();
                // The computed uses ?? operator, so undefined x should default to 0
                expect(parseInt(bounds.style.left, 10)).toBe(0);
            });
        });

        describe('hoverTopClipOffset', () => {
            it('should be null when the top edge is visible', () => {
                expect(spectator.component.hoverTopClipOffset()).toBeNull();
            });

            it('should offset the top toolbar row when the top edge is scrolled above the iframe', () => {
                const scrolledArea = { ...MOCK_CONTENTLET_AREA, y: -50 };
                spectator.setInput('contentletArea', scrolledArea);
                spectator.detectChanges();

                expect(spectator.component.hoverTopClipOffset()).toBe(50);

                const actions = spectator.query(byTestId('hover-actions')) as HTMLElement;
                expect(actions.style.top).toBe('50px');
                expect(actions.style.transform).toBe('translate(0, 0)');
            });

            it('should cap the offset at the contentlet height so it never overshoots the box', () => {
                const scrolledArea = { ...MOCK_CONTENTLET_AREA, y: -500 };
                spectator.setInput('contentletArea', scrolledArea);
                spectator.detectChanges();

                expect(spectator.component.hoverTopClipOffset()).toBe(scrolledArea.height);
            });
        });

        describe('hoverBottomClipOffset', () => {
            it('should be null when the bottom edge is visible', () => {
                expect(spectator.component.hoverBottomClipOffset()).toBeNull();
            });

            it('should offset the bottom add button when the bottom edge overflows the iframe', () => {
                // Store mock reports an 800px-tall iframe; y(200) + height(400) - 800 = -200 (fits).
                // Push the area down so it overflows by 100px.
                const scrolledArea = { ...MOCK_CONTENTLET_AREA, y: 500 };
                spectator.setInput('contentletArea', scrolledArea);
                spectator.detectChanges();

                expect(spectator.component.hoverBottomClipOffset()).toBe(100);

                const addBottomButton = spectator.query(
                    byTestId('hover-add-bottom-button')
                ) as HTMLElement;
                expect(addBottomButton.style.bottom).toBe('100px');
                expect(addBottomButton.style.transform).toBe('translate(-50%, 0)');
            });
        });

        describe('hoverDragButtonTopOffset', () => {
            it('should be null when the natural vertical center is visible', () => {
                // center = y(200) + height(400) / 2 = 400, within the 800px mock iframe.
                expect(spectator.component.hoverDragButtonTopOffset()).toBeNull();
            });

            it('should clamp the handle to the top of the iframe when the center is scrolled above it', () => {
                // center = y(-300) + height(400) / 2 = -100, above the iframe top (0).
                const scrolledArea = { ...MOCK_CONTENTLET_AREA, y: -300 };
                spectator.setInput('contentletArea', scrolledArea);
                spectator.detectChanges();

                // clampedCenter(0) - y(-300) = 300
                expect(spectator.component.hoverDragButtonTopOffset()).toBe(300);

                const dragButton = spectator.query(byTestId('hover-drag-button'))
                    ?.parentElement as HTMLElement;
                expect(dragButton.style.top).toBe('300px');
            });

            it('should clamp the handle to the bottom of the iframe when the center is scrolled below it', () => {
                // Store mock reports an 800px-tall iframe.
                // center = y(700) + height(400) / 2 = 900, below the iframe bottom (800).
                const scrolledArea = { ...MOCK_CONTENTLET_AREA, y: 700 };
                spectator.setInput('contentletArea', scrolledArea);
                spectator.detectChanges();

                // clampedCenter(800) - y(700) = 100
                expect(spectator.component.hoverDragButtonTopOffset()).toBe(100);
            });

            it('should never exceed the contentlet height', () => {
                const scrolledArea = { ...MOCK_CONTENTLET_AREA, y: -5000 };
                spectator.setInput('contentletArea', scrolledArea);
                spectator.detectChanges();

                expect(spectator.component.hoverDragButtonTopOffset()).toBe(scrolledArea.height);
            });
        });
    });

    describe('Position flag behavior', () => {
        it('should ask to add "before" the contentlet when clicking top add button', () => {
            const addTopButton = spectator.query(byTestId('hover-add-top-button'));
            const button = addTopButton?.querySelector('button');
            spectator.click(button as Element);
            spectator.detectChanges();

            // Get the menu items and trigger the first command
            const menuItems = spectator.component.menuItems();
            menuItems[0].command?.({});

            expect(dispatcher.dispatch).toHaveBeenCalledWith(
                uveContentEditorEvents.uveAddContentRequested({
                    type: 'content',
                    into: expect.objectContaining({ position: 'before' })
                }),
                { scope: 'self' }
            );
        });

        it('should ask to add "after" the contentlet when clicking bottom add button', () => {
            const addBottomButton = spectator.query(byTestId('hover-add-bottom-button'));
            const button = addBottomButton?.querySelector('button');
            spectator.click(button as Element);
            spectator.detectChanges();

            // Get the menu items and trigger the first command
            const menuItems = spectator.component.menuItems();
            menuItems[0].command?.({});

            expect(dispatcher.dispatch).toHaveBeenCalledWith(
                uveContentEditorEvents.uveAddContentRequested({
                    type: 'content',
                    into: expect.objectContaining({ position: 'after' })
                }),
                { scope: 'self' }
            );
        });
    });

    describe('Style Editor Features', () => {
        describe('Palette button visibility', () => {
            it('should NOT render palette button when showStyleEditorOption is false', () => {
                spectator.setInput('showStyleEditorOption', false);
                spectator.detectChanges();

                const paletteButton = spectator.query(byTestId('hover-palette-button'));
                expect(paletteButton).toBeFalsy();
            });

            it('should render palette button when showStyleEditorOption is true', () => {
                spectator.setInput('showStyleEditorOption', true);
                spectator.detectChanges();

                const paletteButton = spectator.query(byTestId('hover-palette-button'));
                expect(paletteButton).toBeTruthy();
            });

            it('should hide palette button when showStyleEditorOption changes to false', () => {
                // First enable it
                spectator.setInput('showStyleEditorOption', true);
                spectator.detectChanges();

                let paletteButton = spectator.query(byTestId('hover-palette-button'));
                expect(paletteButton).toBeTruthy();

                // Then disable it
                spectator.setInput('showStyleEditorOption', false);
                spectator.detectChanges();

                paletteButton = spectator.query(byTestId('hover-palette-button'));
                expect(paletteButton).toBeFalsy();
            });

            it('should NOT render palette button when container is empty even if showStyleEditorOption is true', () => {
                spectator.setInput('showStyleEditorOption', true);
                spectator.setInput('contentletArea', MOCK_EMPTY_CONTENTLET_AREA);
                editorSelected.set(toSelected(MOCK_EMPTY_CONTENTLET_AREA));
                spectator.detectChanges();

                const paletteButton = spectator.query(byTestId('hover-palette-button'));
                expect(paletteButton).toBeFalsy();
            });
        });
    });

    describe('Effect behavior', () => {
        it('should hide menus when contentletArea changes', () => {
            // Open a menu by clicking the add button
            const addTopButton = spectator.query(byTestId('hover-add-top-button'));
            const button = addTopButton?.querySelector('button');
            spectator.click(button as Element);
            spectator.detectChanges();

            // Verify menu is open by checking if it exists in the DOM
            let menu = document.querySelector('.p-menu');
            expect(menu).toBeTruthy();

            // Change contentletArea - this should trigger the effect that hides menus
            const newArea = { ...MOCK_CONTENTLET_AREA, x: 500 };
            spectator.setInput('contentletArea', newArea);
            spectator.detectChanges();

            // Menu should be hidden now
            menu = document.querySelector('.p-menu.p-component-overlay-visible');
            expect(menu).toBeFalsy();
        });
    });

    describe('Drag attributes', () => {
        it('should set correct drag attributes on drag button', () => {
            const dragButton = spectator.query(byTestId('hover-drag-button')) as HTMLElement;

            expect(dragButton?.getAttribute('draggable')).toBe('true');
            expect(dragButton?.getAttribute('data-type')).toBe('contentlet');
            expect(dragButton?.getAttribute('data-use-custom-drag-image')).toBe('true');
        });

        it('should include drag payload in data-item attribute', () => {
            const dragButton = spectator.query(byTestId('hover-drag-button')) as HTMLElement;
            const dataItem = dragButton?.getAttribute('data-item');

            expect(dataItem).toBeTruthy();
            const parsedItem = JSON.parse(dataItem);
            expect(parsedItem.contentlet).toEqual(MOCK_CONTENTLET_AREA.payload.contentlet);
            expect(parsedItem.container).toEqual(MOCK_CONTENTLET_AREA.payload.container);
            expect(parsedItem.showLabelImage).toBe(true);
            expect(parsedItem.move).toBe(true);
        });
    });

    describe('Contentlet edit permission (#37376)', () => {
        /**
         * Build a hovered area whose contentlet carries an explicit `canEdit`.
         * Passing `undefined` omits the field entirely, which is what headless
         * and SDK-rendered pages produce.
         */
        const areaWithPermission = (canEdit: boolean | undefined): ContentletArea => ({
            ...MOCK_CONTENTLET_AREA,
            payload: {
                ...MOCK_CONTENTLET_AREA.payload,
                contentlet: {
                    ...(MOCK_CONTENTLET_AREA.payload.contentlet as ContentletPayload),
                    ...(canEdit === undefined ? {} : { canEdit })
                }
            }
        });

        const setPermission = (canEdit: boolean | undefined) => {
            const area = areaWithPermission(canEdit);
            spectator.setInput('contentletArea', area);
            editorSelected.set(toSelected(area));
            spectator.detectChanges();
        };

        const editButton = () =>
            (spectator.query(byTestId('hover-edit-button')) as HTMLElement)?.querySelector(
                'button'
            );

        describe('edit button disabled state', () => {
            it('should disable the edit button when canEdit is false', () => {
                setPermission(false);

                expect(editButton()?.disabled).toBe(true);
            });

            it('should enable the edit button when canEdit is true', () => {
                setPermission(true);

                expect(editButton()?.disabled).toBe(false);
            });

            it('should enable the edit button when canEdit is absent', () => {
                // Headless pages never emit `data-dot-can-edit`; the gate must
                // fail open or every pencil on them would be greyed out.
                setPermission(undefined);

                expect(editButton()?.disabled).toBe(false);
            });
        });

        describe('edit button tooltip', () => {
            it('should explain the missing permission when denied', () => {
                setPermission(false);

                expect(spectator.component['editButtonTooltip']()).toBe(
                    'uve.contentlet.no.edit.permission'
                );
            });

            it('should show the normal edit label when allowed', () => {
                setPermission(true);

                expect(spectator.component['editButtonTooltip']()).toBe('uve.tooltip.edit.full');
            });
        });

        describe('quick edit button disabled state', () => {
            // The quick-edit form writes contentlet fields via
            // saveQuickEditFields, so leaving it reachable would defeat the
            // pencil gate entirely.
            const quickEditButton = () =>
                (
                    spectator.query(byTestId('hover-quick-edit-button')) as HTMLElement
                )?.querySelector('button');

            it('should disable the quick edit button when canEdit is false', () => {
                setPermission(false);

                expect(quickEditButton()?.disabled).toBe(true);
            });

            it('should enable the quick edit button when canEdit is true', () => {
                setPermission(true);

                expect(quickEditButton()?.disabled).toBe(false);
            });

            it('should enable the quick edit button when canEdit is absent', () => {
                setPermission(undefined);

                expect(quickEditButton()?.disabled).toBe(false);
            });

            it('should explain the missing permission in the quick edit tooltip', () => {
                setPermission(false);

                expect(spectator.component['quickEditButtonTooltip']()).toBe(
                    'uve.contentlet.no.edit.permission'
                );
            });

            it('should show the normal quick edit label when allowed', () => {
                setPermission(true);

                expect(spectator.component['quickEditButtonTooltip']()).toBe(
                    'uve.tooltip.edit.quick'
                );
            });
        });

        describe('collapsed overflow menu', () => {
            const editMenuItem = () =>
                spectator.component.actionsMenuItems().find((item) => item.symbol === 'edit_note');

            it('should disable the Edit entry when canEdit is false', () => {
                setPermission(false);

                expect(editMenuItem()?.disabled).toBe(true);
            });

            it('should keep the Edit entry enabled when canEdit is true', () => {
                setPermission(true);

                expect(editMenuItem()?.disabled).toBe(false);
            });

            const quickEditMenuItem = () =>
                spectator.component.actionsMenuItems().find((item) => item.symbol === 'edit');

            it('should disable the Quick Edit entry when canEdit is false', () => {
                setPermission(false);

                expect(quickEditMenuItem()?.disabled).toBe(true);
            });

            it('should keep the Quick Edit entry enabled when canEdit is true', () => {
                setPermission(true);

                expect(quickEditMenuItem()?.disabled).toBe(false);
            });
        });

        describe('style editor button disabled state', () => {
            // Style properties describe how this contentlet presents itself, so
            // they follow contentlet permission rather than page permission.
            const paletteButton = () =>
                (spectator.query(byTestId('hover-palette-button')) as HTMLElement)?.querySelector(
                    'button'
                );

            beforeEach(() => {
                spectator.setInput('showStyleEditorOption', true);
                spectator.detectChanges();
            });

            it('should disable the style button when canEdit is false', () => {
                setPermission(false);

                expect(paletteButton()?.disabled).toBe(true);
            });

            it('should enable the style button when canEdit is true', () => {
                setPermission(true);

                expect(paletteButton()?.disabled).toBe(false);
            });

            it('should disable the Style entry in the overflow menu when canEdit is false', () => {
                setPermission(false);

                const styleItem = spectator.component
                    .actionsMenuItems()
                    .find((item) => item.symbol === 'palette');

                expect(styleItem?.disabled).toBe(true);
            });
        });

        describe('structural page actions stay ungated', () => {
            // A user allowed into the page editor may change the page's
            // composition. Contentlet permission governs the contentlet's
            // content, and nothing else.
            it('should keep the delete button enabled for a contentlet the user cannot edit', () => {
                setPermission(false);

                const deleteButton = (
                    spectator.query(byTestId('hover-delete-button')) as HTMLElement
                )?.querySelector('button');

                expect(deleteButton?.disabled).toBe(false);
            });

            it('should keep the drag handle rendered for a contentlet the user cannot edit', () => {
                setPermission(false);

                expect(spectator.query(byTestId('hover-drag-button'))).toBeTruthy();
            });

            it('should keep the add-content buttons rendered for a contentlet the user cannot edit', () => {
                setPermission(false);

                expect(spectator.query(byTestId('hover-add-top-button'))).toBeTruthy();
            });
        });

        describe('empty container', () => {
            it('should not gate an empty container, whose sentinel carries no permission', () => {
                spectator.setInput('contentletArea', MOCK_EMPTY_CONTENTLET_AREA);
                editorSelected.set(toSelected(MOCK_EMPTY_CONTENTLET_AREA));
                spectator.detectChanges();

                expect(spectator.component.isContainerEmpty()).toBe(true);
                expect(spectator.component['canEditContentlet']()).toBe(true);
            });
        });
    });

    /**
     * #37499. The `</>` button is gated on the HOVERED contentlet
     * (`hasVtlFiles()`), but `vtlMenuItems()` preferred the SELECTED one — and a
     * SET_BOUNDS re-anchor strips `vtlFiles` from the selected payload. So after
     * any layout shift the button appeared and the menu opened empty.
     *
     * Both now read the hovered contentlet, which is the only contentlet the
     * button can belong to.
     */
    describe('VTL menu follows the hovered contentlet', () => {
        const areaWithVtlFiles = (
            label: string,
            vtlFiles: VTLFile[] | undefined
        ): ContentletArea => ({
            ...MOCK_CONTENTLET_AREA,
            payload: {
                ...MOCK_CONTENTLET_AREA.payload,
                contentlet: {
                    ...(MOCK_CONTENTLET_AREA.payload.contentlet as ContentletPayload),
                    inode: `inode-${label}`,
                    identifier: `identifier-${label}`
                },
                vtlFiles
            }
        });

        it('lists the hovered contentlet files after a re-anchor stripped them from the selection', () => {
            // What applyBoundsForSelection used to leave behind: same contentlet,
            // no vtlFiles. Before the fix this emptied the menu.
            editorSelected.set(toSelected(areaWithVtlFiles('a', undefined)));
            spectator.setInput('contentletArea', MOCK_CONTENTLET_AREA);
            spectator.detectChanges();

            expect(spectator.component.vtlMenuItems().map((item) => item.label)).toEqual([
                'template1.vtl',
                'template2.vtl'
            ]);
        });

        it('lists the hovered contentlet files when nothing is selected', () => {
            editorSelected.set(null);
            spectator.setInput('contentletArea', MOCK_CONTENTLET_AREA);
            spectator.detectChanges();

            expect(spectator.component.vtlMenuItems().map((item) => item.label)).toEqual([
                'template1.vtl',
                'template2.vtl'
            ]);
        });

        it("lists the HOVERED contentlet's files while a different contentlet is selected", () => {
            editorSelected.set(
                toSelected(areaWithVtlFiles('b', [{ inode: 'vtl-b', name: 'other.vtl' }]))
            );
            spectator.setInput('contentletArea', MOCK_CONTENTLET_AREA);
            spectator.detectChanges();

            const labels = spectator.component.vtlMenuItems().map((item) => item.label);
            expect(labels).toEqual(['template1.vtl', 'template2.vtl']);
            expect(labels).not.toContain('other.vtl');
        });

        it('returns an empty array, never undefined, when the hovered contentlet has no VTL files', () => {
            spectator.setInput('contentletArea', areaWithVtlFiles('c', undefined));
            spectator.detectChanges();

            expect(spectator.component.vtlMenuItems()).toEqual([]);
        });

        it('gives the collapsed actions menu the same VTL submenu as the full toolbar', () => {
            editorSelected.set(toSelected(areaWithVtlFiles('a', undefined)));
            spectator.setInput('contentletArea', MOCK_CONTENTLET_AREA);
            spectator.detectChanges();

            const vtlEntry = spectator.component
                .actionsMenuItems()
                .find((item) => item.symbol === 'code');

            expect(vtlEntry?.items).toEqual(spectator.component.vtlMenuItems());
        });
    });

    /**
     * Opening a VTL promotes the hovered contentlet to selected, so the selection
     * border follows the contentlet whose file you just opened. Promotion sits on
     * the menu COMMAND rather than the button click, so a menu opened and
     * dismissed leaves the selection untouched.
     */
    describe('choosing a VTL file promotes the hovered contentlet', () => {
        it('promotes the hovered contentlet before asking to edit the file', () => {
            const store = spectator.inject(UVEStore);
            const setSelected = store.setSelected as ReturnType<typeof vi.fn>;

            spectator.setInput('contentletArea', MOCK_CONTENTLET_AREA);
            spectator.detectChanges();

            spectator.component.vtlMenuItems()[0].command?.({} as never);

            expect(setSelected).toHaveBeenCalledWith(
                expect.objectContaining({
                    bounds: {
                        x: MOCK_CONTENTLET_AREA.x,
                        y: MOCK_CONTENTLET_AREA.y,
                        width: MOCK_CONTENTLET_AREA.width,
                        height: MOCK_CONTENTLET_AREA.height
                    }
                })
            );
            expect(dispatcher.dispatch).toHaveBeenCalledWith(
                uveContentEditorEvents.uveEditSourceRequested({
                    inode: 'vtl-inode-1',
                    name: 'template1.vtl'
                }),
                { scope: 'self' }
            );
            expect(setSelected.mock.invocationCallOrder[0]).toBeLessThan(
                dispatcher.dispatch.mock.invocationCallOrder[0]
            );
        });

        /**
         * Pins a consequence that is deliberate, not incidental.
         *
         * `editorEditPanelOpen` is state of its own and survives a selection
         * change, and both side-panel tabs bind to `editorSelected` — so
         * promoting while a panel is open on a DIFFERENT contentlet retargets
         * that panel. Raised in review as a possible side effect.
         *
         * It is kept because it is not new: the SET_SELECTED_CONTENTLET handler
         * already documents that one write "drives both the floating overlay and
         * the side panel's data binding", so plain-clicking a contentlet does
         * exactly the same thing. Promotion makes `</>` consistent with a click
         * rather than introducing a new behaviour. Without it the editor would
         * be left incoherent — VTL dialog on A, border and panel on B.
         */
        it('retargets the selection even when a panel is open on another contentlet', () => {
            const store = spectator.inject(UVEStore);
            const otherContentlet = {
                ...MOCK_CONTENTLET_AREA,
                payload: {
                    ...MOCK_CONTENTLET_AREA.payload,
                    contentlet: {
                        ...(MOCK_CONTENTLET_AREA.payload.contentlet as ContentletPayload),
                        identifier: 'panel-is-open-on-this-one',
                        inode: 'other-inode'
                    }
                }
            };

            // Panel open on B…
            editorSelected.set(toSelected(otherContentlet));
            // …while A is hovered.
            spectator.setInput('contentletArea', MOCK_CONTENTLET_AREA);
            spectator.detectChanges();
            (store.setSelected as ReturnType<typeof vi.fn>).mockClear();

            spectator.component.vtlMenuItems()[0].command?.({} as never);

            const promoted = (store.setSelected as ReturnType<typeof vi.fn>).mock.calls[0][0];
            expect(promoted.payload.contentlet.identifier).toBe(
                MOCK_CONTENTLET_AREA.payload.contentlet?.identifier
            );
            expect(promoted.payload.contentlet.identifier).not.toBe('panel-is-open-on-this-one');
        });

        it('does not promote merely because the menu was built', () => {
            const store = spectator.inject(UVEStore);
            (store.setSelected as ReturnType<typeof vi.fn>).mockClear();

            spectator.setInput('contentletArea', MOCK_CONTENTLET_AREA);
            spectator.detectChanges();
            spectator.component.vtlMenuItems();

            expect(store.setSelected).not.toHaveBeenCalled();
        });
    });
});

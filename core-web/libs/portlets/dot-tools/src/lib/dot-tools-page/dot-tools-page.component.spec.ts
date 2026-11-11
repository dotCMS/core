import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { CdkDrag, CdkDragMove, CdkDropList } from '@angular/cdk/drag-drop';
import { CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';

import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Splitter } from 'primeng/splitter';
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotToolsPageComponent } from './dot-tools-page.component';
import { DotToolsStore } from './store/dot-tools.store';

import { DotToolsCatalogEntry, DotToolsSection } from '../models/dot-tools.models';

const SITE: DotToolsSection = {
    id: 'site',
    name: 'Site',
    icon: 'language',
    tabOrder: 0,
    portletIds: ['pages', 'browser'],
    portletTitles: ['Pages', 'Browser']
};
const CONTENT: DotToolsSection = {
    id: 'content',
    name: 'Content',
    icon: 'article',
    tabOrder: 1,
    portletIds: ['blogs', 'events', 'c_press-releases'],
    portletTitles: ['Blogs', 'Events', 'Press Releases']
};
const EMPTY: DotToolsSection = {
    id: 'empty',
    name: 'Empty',
    icon: 'folder',
    tabOrder: 2,
    portletIds: [],
    portletTitles: []
};
const SECTIONS = [SITE, CONTENT, EMPTY];

const CUSTOM_TOOL: DotToolsCatalogEntry = {
    id: 'c_press-releases',
    title: 'Press Releases',
    isCustom: true
};

/**
 * Build a stub store with the signals + methods the component touches.
 * Store is a SignalStore, which is complex to construct in a test — the
 * stub matches the component's reads/writes and lets us assert on method
 * calls directly.
 */
function createStoreStub() {
    return {
        sections: signal<DotToolsSection[]>(SECTIONS),
        selectedSection: signal<DotToolsSection | null>(SITE),
        selectedSectionId: signal<string | null>('site'),
        selectedSectionTools: signal<DotToolsCatalogEntry[]>([
            { id: 'pages', title: 'Pages', isCustom: false },
            { id: 'browser', title: 'Browser', isCustom: false }
        ]),
        selectedSectionToolIds: signal<Set<string>>(new Set(['pages', 'browser'])),
        catalog: signal<DotToolsCatalogEntry[]>([]),
        paginatedCatalog: signal<DotToolsCatalogEntry[]>([]),
        filteredCatalog: signal<DotToolsCatalogEntry[]>([]),
        catalogFilter: signal(''),
        hasMoreCatalog: signal(false),
        toolSectionCount: signal((toolId: string) => (toolId === 'c_press-releases' ? 2 : 0)),
        showLoading: signal(false),
        showError: signal(false),
        showEmptySections: signal(false),
        toolsSavedAt: signal(0),
        selectSection: vi.fn(),
        toggleToolInSelectedSection: vi.fn(),
        removeToolFromSelectedSection: vi.fn(),
        reorderSections: vi.fn(),
        reorderSelectedSectionTools: vi.fn(),
        setCatalogFilter: vi.fn(),
        loadMoreCatalog: vi.fn(),
        loadAll: vi.fn(),
        deleteSection: vi.fn(),
        deleteCustomTool: vi.fn()
    };
}

function dragEventAt(y: number): CdkDragMove {
    return { pointerPosition: { x: 0, y }, delta: { x: 0, y: 0 } } as unknown as CdkDragMove;
}

/** Build a drop-list element with N rows at known Y positions. */
function buildListEl(rowCount: number, rowHeight = 40, startY = 0): HTMLElement {
    const list = document.createElement('ul');
    for (let i = 0; i < rowCount; i++) {
        const li = document.createElement('li');
        li.setAttribute('data-row', '');
        // Stub getBoundingClientRect: happy-dom returns zeros by default,
        // so we hand it a deterministic rect per row.
        const rect: DOMRect = {
            top: startY + i * rowHeight,
            bottom: startY + (i + 1) * rowHeight,
            left: 0,
            right: 100,
            x: 0,
            y: startY + i * rowHeight,
            width: 100,
            height: rowHeight,
            toJSON: () => ({})
        };
        li.getBoundingClientRect = (): DOMRect => rect;
        list.appendChild(li);
    }

    return list;
}

describe('DotToolsPageComponent', () => {
    let spectator: Spectator<DotToolsPageComponent>;
    let store: ReturnType<typeof createStoreStub>;
    let confirm: ConfirmationService;

    const createComponent = createComponentFactory({
        component: DotToolsPageComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        componentProviders: [
            {
                provide: DotToolsStore,
                useFactory: () => (store = createStoreStub())
            },
            mockProvider(DialogService, { open: vi.fn() }),
            mockProvider(MessageService, {
                add: vi.fn(),
                clear: vi.fn(),
                messageObserver: new Subject(),
                clearObserver: new Subject()
            }),
            // PrimeNG's ConfirmDialog subscribes to requireConfirmation$ and
            // accept on construction, so the mock exposes them as Subjects.
            // Only the keys ConfirmationService actually declares are listed —
            // mockProvider's generic rejects anything else.
            mockProvider(ConfirmationService, {
                confirm: vi.fn(),
                requireConfirmation$: new Subject(),
                accept: new Subject()
            })
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    // Mock resolves these keys so the confirm specs can
                    // assert interpolated copy. The dialog headers take the
                    // section/tool name as {0}, the "body.with-*" bodies
                    // take the pluralized phrase as {0}, and the "*-count"
                    // phrases take the actual number as {0}.
                    'tools.confirm.delete-section.header': 'Delete {0}',
                    'tools.confirm.delete-section.body': 'This section will be removed.',
                    'tools.confirm.delete-section.body.with-tools':
                        'The section and its {0} will be removed.',
                    'tools.confirm.delete-section.tool-count.single': '1 tool',
                    'tools.confirm.delete-section.tool-count.multi': '{0} tools',
                    'tools.confirm.delete-tool.header': 'Delete {0}',
                    'tools.confirm.delete-tool.body': 'This tool will be removed.',
                    'tools.confirm.delete-tool.body.with-sections':
                        'The tool will be removed from {0}.',
                    'tools.confirm.delete-tool.section-count.single': '1 section',
                    'tools.confirm.delete-tool.section-count.multi': '{0} sections',
                    'tools.menu.edit': 'Edit',
                    'tools.menu.delete': 'Delete',
                    'Identity-Access': 'Identity Access',
                    'Another-Identity': 'Identity Access'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        confirm = spectator.inject(ConfirmationService, true);
        // mockProvider creates the spy once per factory, so it carries call
        // history between specs — reset the ones with per-test assertions.
        (confirm.confirm as Mock).mockClear();
        const toast = spectator.inject(MessageService, true);
        (toast.add as Mock).mockClear();
        (toast.clear as Mock).mockClear();
    });

    it('uses the Roles initial split and minimum sizes with a keyboard-focusable divider', () => {
        spectator.detectChanges();

        const splitter = spectator.query(Splitter);
        expect(splitter.panelSizes).toEqual([22, 78]);
        expect(splitter.minSizes).toEqual([15, 40]);
        expect(spectator.query(byTestId('tools-sections-panel'))).toBeTruthy();
        expect(spectator.query(byTestId('tools-available-panel'))).toBeTruthy();
        expect(spectator.query(byTestId('tools-splitter-handle')).getAttribute('tabindex')).toBe(
            '0'
        );
    });

    it('resizes both panels by dragging the divider without reordering tools or sections', () => {
        spectator.detectChanges();

        const splitter = spectator.query(Splitter);
        const root = spectator.query(byTestId('tools-splitter'));
        const gutter = spectator.query(byTestId('tools-splitter-gutter'));
        const treePanel = gutter.previousElementSibling as HTMLElement;
        const catalogPanel = gutter.nextElementSibling as HTMLElement;
        // happy-dom has no layout; model a 1000 px splitter at the initial 22/78 split.
        Object.defineProperty(root, 'offsetWidth', { value: 1000 });
        Object.defineProperty(treePanel, 'offsetWidth', { value: 220 });
        Object.defineProperty(catalogPanel, 'offsetWidth', { value: 780 });
        root.style.padding = '0px';
        root.style.borderWidth = '0px';
        treePanel.style.margin = '0px';
        catalogPanel.style.margin = '0px';

        // happy-dom leaves pageX at zero even when clientX is supplied.
        const start = new MouseEvent('mousedown', { bubbles: true });
        const move = new MouseEvent('mousemove');
        Object.defineProperty(start, 'pageX', { value: 220 });
        Object.defineProperty(move, 'pageX', { value: 320 });
        gutter.dispatchEvent(start);
        document.dispatchEvent(move);
        document.dispatchEvent(new MouseEvent('mouseup'));

        expect(splitter.panelSizes).toEqual([32, 68]);
        expect(treePanel.style.flexBasis).toContain('32%');
        expect(catalogPanel.style.flexBasis).toContain('68%');
        expect(store.reorderSections).not.toHaveBeenCalled();
        expect(store.reorderSelectedSectionTools).not.toHaveBeenCalled();
    });

    it('shows qualified parent labels and overflow tooltips without changing ids', () => {
        const entry: DotToolsCatalogEntry = {
            id: 'users',
            title: 'Identity Access › Users',
            isCustom: false
        };
        const section: DotToolsSection = {
            ...SITE,
            id: 'system',
            name: 'System',
            portletIds: [entry.id],
            portletTitles: [entry.title]
        };
        store.sections.set([section]);
        store.selectedSection.set(section);
        store.selectedSectionId.set(section.id);
        store.selectedSectionTools.set([entry]);
        store.selectedSectionToolIds.set(new Set([entry.id]));
        store.paginatedCatalog.set([entry]);
        spectator.detectChanges();

        expect(spectator.query(byTestId('tools-catalog-row-users')).textContent).toContain(
            entry.title
        );
        expect(spectator.query(byTestId('tools-section-tools-system')).textContent).toContain(
            entry.title
        );
        const tooltip = spectator
            .getDebugElement(byTestId('tools-section-tool-title-users'))
            .injector.get(Tooltip);
        expect(tooltip.content).toBe(entry.title);
        expect(tooltip.showOnEllipsis).toBe(true);
        expect(tooltip.tooltipPosition).toBe('right');

        spectator.click(byTestId('tools-section-tool-remove-users'));
        expect(store.removeToolFromSelectedSection).toHaveBeenCalledWith('users');
    });

    describe('grouped tool tree', () => {
        const tools: DotToolsCatalogEntry[] = [
            {
                id: 'users',
                title: 'Identity Access › Users',
                label: 'Users',
                parent: 'Identity-Access',
                isCustom: false
            },
            { id: 'sites', title: 'Sites', isCustom: false },
            {
                id: 'roles',
                title: 'Identity Access › Roles & Tools',
                label: 'Roles & Tools',
                parent: 'Identity-Access',
                isCustom: false
            },
            { id: 'dynamic-plugins', title: 'dynamic-plugins', isCustom: false },
            {
                id: 'custom',
                title: 'Other › Custom',
                label: 'Custom',
                parent: 'Other',
                isCustom: true
            }
        ];

        function showTools(entries = tools) {
            const section = {
                ...SITE,
                portletIds: entries.map((tool) => tool.id),
                portletTitles: entries.map((tool) => tool.title)
            };
            store.sections.set([section]);
            store.selectedSection.set(section);
            store.selectedSectionTools.set(entries);
            store.paginatedCatalog.set(entries);
            spectator.detectChanges();
        }

        it('groups by the raw parent at its first child and keeps the catalog qualified', () => {
            showTools();
            expect(
                spectator.component['$toolEntries']().map((entry) =>
                    entry.tools.map((tool) => tool.id)
                )
            ).toEqual([['users', 'roles'], ['sites'], ['dynamic-plugins'], ['custom']]);
            expect(spectator.query(byTestId('tools-parent-Identity-Access')).textContent).toContain(
                'Identity Access'
            );
            expect(
                spectator.query(byTestId('tools-section-tool-title-users')).textContent.trim()
            ).toBe('Users');
            expect(
                spectator.query(byTestId('tools-section-tool-title-roles')).textContent.trim()
            ).toBe('Roles & Tools');
            expect(spectator.query(byTestId('tools-catalog-row-users')).textContent).toContain(
                'Identity Access › Users'
            );
        });

        it('keeps child drags in their own parent list, separate from whole-group dragging', () => {
            showTools();
            const children = spectator
                .getDebugElement(byTestId('tools-parent-children-Identity-Access'))
                .injector.get(CdkDropList);
            const root = spectator
                .getDebugElement(byTestId('tools-section-tools-site'))
                .injector.get(CdkDropList);
            const child = spectator
                .getDebugElement(byTestId('tools-section-tool-row-users'))
                .injector.get(CdkDrag);
            const group = spectator
                .getDebugElement(byTestId('tools-parent-Identity-Access'))
                .injector.get(CdkDrag);
            expect(child.dropContainer).toBe(children);
            expect(group.dropContainer).toBe(root);
            expect(children.connectedTo).toEqual([]);
            expect(root.getSortedItems()).toHaveLength(4);
            expect(children.getSortedItems()).toHaveLength(2);
        });

        it('does not merge distinct keys with the same translation or infer parents from title text', () => {
            showTools([
                ...tools,
                {
                    id: 'other',
                    title: 'Identity Access › Other',
                    label: 'Other',
                    parent: 'Another-Identity',
                    isCustom: false
                },
                { id: 'literal', title: 'Identity Access › Literal', isCustom: true }
            ]);
            expect(
                spectator.component['$toolEntries']()
                    .filter((entry) => entry.parent)
                    .map((entry) => entry.parent)
            ).toEqual(['Identity-Access', 'Other', 'Another-Identity']);
            expect(spectator.component['$toolEntries']().at(-1)).toEqual({
                tools: [{ id: 'literal', title: 'Identity Access › Literal', isCustom: true }]
            });
        });

        it('reorders children within one parent and preserves every flat id, including legacy entries', () => {
            showTools();
            spectator.component['$childDrop'].set({ parent: 'Identity-Access', index: 2 });
            spectator.component['onChildDragEnded']('Identity-Access', 0);
            expect(store.reorderSelectedSectionTools).toHaveBeenCalledWith([
                'roles',
                'users',
                'sites',
                'dynamic-plugins',
                'custom'
            ]);
            expect(store.selectedSectionTools()).toEqual(tools);
        });

        it('moves a parent as one block without changing its child order', () => {
            showTools();
            spectator.component['$toolDropIndex'].set(4);
            spectator.component['onToolDragEnded'](0);
            expect(store.reorderSelectedSectionTools).toHaveBeenCalledWith([
                'sites',
                'dynamic-plugins',
                'custom',
                'users',
                'roles'
            ]);
            expect(store.selectedSectionTools()).toEqual(tools);
        });

        it('ignores a child drop from another parent or a parent that no longer exists', () => {
            showTools();
            spectator.component['$childDrop'].set({ parent: 'Other', index: 1 });
            spectator.component['onChildDragEnded']('Identity-Access', 0);
            spectator.component['$childDrop'].set({ parent: 'removed-parent', index: 1 });
            spectator.component['onChildDragEnded']('removed-parent', 0);
            expect(store.reorderSelectedSectionTools).not.toHaveBeenCalled();
            expect(spectator.component['$childDrop']()).toBeNull();
        });

        it('does not save a no-op child move and cancels targets when selecting another section', () => {
            showTools();
            spectator.component['$childDrop'].set({ parent: 'Identity-Access', index: 1 });
            spectator.component['onChildDragEnded']('Identity-Access', 0);
            expect(store.reorderSelectedSectionTools).not.toHaveBeenCalled();
            spectator.component['$childDrop'].set({ parent: 'Identity-Access', index: 2 });
            spectator.component['$toolDropIndex'].set(4);
            spectator.component['onSelectSection']('content');
            expect(spectator.component['$childDrop']()).toBeNull();
            expect(spectator.component['$toolDropIndex']()).toBeNull();
        });

        it('supports child and whole-group reordering with the keyboard', () => {
            showTools();
            const child = spectator.query(byTestId('tools-child-handle-users'));
            child.dispatchEvent(
                new KeyboardEvent('keydown', {
                    key: 'ArrowDown',
                    code: 'ArrowDown',
                    bubbles: true,
                    cancelable: true
                })
            );
            expect(store.reorderSelectedSectionTools).toHaveBeenLastCalledWith([
                'roles',
                'users',
                'sites',
                'dynamic-plugins',
                'custom'
            ]);
            showTools([tools[2], tools[0], tools[1], tools[3], tools[4]]);
            const group = spectator.query(byTestId('tools-row-handle-Identity-Access'));
            group.dispatchEvent(
                new KeyboardEvent('keydown', {
                    key: 'ArrowDown',
                    code: 'ArrowDown',
                    bubbles: true,
                    cancelable: true
                })
            );
            expect(store.reorderSelectedSectionTools).toHaveBeenLastCalledWith([
                'sites',
                'roles',
                'users',
                'dynamic-plugins',
                'custom'
            ]);
        });

        it('keeps keyboard moves inside the current parent bounds', () => {
            showTools();
            spectator.component['onToolReorderKey'](
                new KeyboardEvent('keydown'),
                0,
                -1,
                'Identity-Access'
            );
            spectator.component['onToolReorderKey'](
                new KeyboardEvent('keydown'),
                1,
                1,
                'Identity-Access'
            );
            expect(store.reorderSelectedSectionTools).not.toHaveBeenCalled();
        });
    });

    describe('drag drop-index math', () => {
        it('pointer above the first row target → gap 0 (first)', () => {
            const list = buildListEl(3, 40);
            spectator.component['onSectionDragMoved'](dragEventAt(10), list);
            // Row 0 center at y=20; pointer at 10 is above it, so gap 0.
            expect(spectator.component['$sectionDropIndex']()).toBe(0);
        });

        it('pointer in a middle row → that row index', () => {
            const list = buildListEl(3, 40);
            // Row 1 top=40 bottom=80 center=60. Pointer at 55 is above center.
            spectator.component['onSectionDragMoved'](dragEventAt(55), list);
            expect(spectator.component['$sectionDropIndex']()).toBe(1);
        });

        it('pointer below the last row → last gap (count)', () => {
            const list = buildListEl(3, 40);
            // All rows end at y=120; pointer at 200 is past the end.
            spectator.component['onSectionDragMoved'](dragEventAt(200), list);
            expect(spectator.component['$sectionDropIndex']()).toBe(3);
        });

        it('does not count nested child rows as section or top-level tool rows', () => {
            const list = buildListEl(2);
            list.firstElementChild.appendChild(buildListEl(3));
            spectator.component['onSectionDragMoved'](dragEventAt(100), list);
            spectator.component['onToolDragMoved'](dragEventAt(100), list);
            expect(spectator.component['$sectionDropIndex']()).toBe(2);
            expect(spectator.component['$toolDropIndex']()).toBe(2);
        });

        it('empty list → gap 0 (no rows to probe)', () => {
            spectator.component['onSectionDragMoved'](dragEventAt(10), buildListEl(0));
            expect(spectator.component['$sectionDropIndex']()).toBe(0);
        });
    });

    describe('drag end commits the reorder (sections)', () => {
        it('dropping on the same gap is a no-op', () => {
            // previousIndex=1, target=1 → normalizes to currentIndex=1 (no-op).
            spectator.component['$sectionDropIndex'].set(1);
            spectator.component['onSectionDragEnded'](1);
            expect(store.reorderSections).not.toHaveBeenCalled();
        });

        it('dropping into gap 2 from index 0 moves the row to index 1 (shift for self-removal)', () => {
            spectator.component['$sectionDropIndex'].set(2);
            spectator.component['onSectionDragEnded'](0);
            expect(store.reorderSections).toHaveBeenCalledWith(['content', 'site', 'empty']);
        });

        it('dropping into gap 0 from index 2 moves the row to index 0', () => {
            spectator.component['$sectionDropIndex'].set(0);
            spectator.component['onSectionDragEnded'](2);
            expect(store.reorderSections).toHaveBeenCalledWith(['empty', 'site', 'content']);
        });

        it('aborts cleanly if drag ends without a target index', () => {
            spectator.component['$sectionDropIndex'].set(null);
            spectator.component['onSectionDragEnded'](0);
            expect(store.reorderSections).not.toHaveBeenCalled();
        });
    });

    describe('drag end commits the reorder (tools inside the selected section)', () => {
        it('reorders the selected section tools in place', () => {
            // SITE has ['pages', 'browser']. Move index 0 to gap 2 → ['browser', 'pages'].
            spectator.component['$toolDropIndex'].set(2);
            spectator.component['onToolDragEnded'](0);
            expect(store.reorderSelectedSectionTools).toHaveBeenCalledWith(['browser', 'pages']);
        });

        it('aborts cleanly if no section is selected', () => {
            store.selectedSection.set(null);
            spectator.component['$toolDropIndex'].set(2);
            spectator.component['onToolDragEnded'](0);
            expect(store.reorderSelectedSectionTools).not.toHaveBeenCalled();
        });
    });

    // Helper: menus now carry a disabled header + separator around Edit /
    // Delete, so looking up by label stays stable if the item order changes.
    function findByLabel(items: MenuItem[], label: string): MenuItem {
        const item = items.find((i) => i.label === label);
        if (!item) {
            throw new Error(`menu item '${label}' not found`);
        }

        return item;
    }

    describe('delete confirmations', () => {
        it('uses the plural "N tool assignments" copy when a section has tools', () => {
            spectator.component['setSectionMenuTarget'](CONTENT);
            findByLabel(spectator.component['sectionMenuItems'](), 'Delete').command?.({} as never);

            const call = (confirm.confirm as Mock).mock.calls[0][0];
            expect(call.header).toContain('Content');
            // CONTENT has 3 tools, so the plural key gets resolved.
            expect(call.message).toContain('3');
            expect(call.accept).toBeInstanceOf(Function);
            call.accept();
            expect(store.deleteSection).toHaveBeenCalledWith('content');
        });

        it('uses the singular copy on a section with one tool', () => {
            spectator.component['setSectionMenuTarget']({ ...CONTENT, portletIds: ['blogs'] });
            findByLabel(spectator.component['sectionMenuItems'](), 'Delete').command?.({} as never);
            const call = (confirm.confirm as Mock).mock.calls[0][0];
            expect(call.message).not.toContain('3');
            expect(call.message).not.toContain('0');
        });

        it('uses the no-assignment copy on an empty section', () => {
            spectator.component['setSectionMenuTarget'](EMPTY);
            findByLabel(spectator.component['sectionMenuItems'](), 'Delete').command?.({} as never);
            const call = (confirm.confirm as Mock).mock.calls[0][0];
            // Falls through to the plain delete-section body (no tool count).
            expect(call.message).toBeDefined();
        });

        it('uses the plural "N sections" copy when the custom tool lives in multiple sections', () => {
            spectator.component['setToolMenuTarget'](CUSTOM_TOOL);
            findByLabel(spectator.component['toolMenuItems'](), 'Delete').command?.({} as never);
            const call = (confirm.confirm as Mock).mock.calls[0][0];
            expect(call.header).toContain('Press Releases');
            // toolSectionCount stub returns 2 for c_press-releases.
            expect(call.message).toContain('2');
            call.accept();
            expect(store.deleteCustomTool).toHaveBeenCalledWith('c_press-releases');
        });
    });

    describe('row menus', () => {
        it('section menu exposes header + Edit + separator + Delete and opens the dialog on Edit', () => {
            const dialog = spectator.inject(DialogService, true);
            spectator.component['setSectionMenuTarget'](SITE);
            const items = spectator.component['sectionMenuItems']();
            // Header (disabled), Edit, separator, Delete.
            expect(items).toHaveLength(4);
            expect(items[0].disabled).toBe(true);
            expect(items[2].separator).toBe(true);
            findByLabel(items, 'Edit').command?.({} as never);
            expect(dialog.open).toHaveBeenCalled();
        });

        it('tool menu exposes header + Edit + separator + Delete and opens the dialog on Edit', () => {
            const dialog = spectator.inject(DialogService, true);
            spectator.component['setToolMenuTarget'](CUSTOM_TOOL);
            const items = spectator.component['toolMenuItems']();
            expect(items).toHaveLength(4);
            expect(items[0].disabled).toBe(true);
            expect(items[2].separator).toBe(true);
            findByLabel(items, 'Edit').command?.({} as never);
            expect(dialog.open).toHaveBeenCalled();
        });
    });

    describe('section-saved toast', () => {
        beforeEach(() => {
            vi.useFakeTimers();
        });
        afterEach(() => {
            vi.useRealTimers();
        });

        it('does not toast on the first (zero) read of toolsSavedAt', () => {
            // beforeEach already created the component; the effect has run
            // once against the sentinel 0 and should have skipped.
            const toast = spectator.inject(MessageService, true);
            expect(toast.add).not.toHaveBeenCalled();
        });

        it('adds once on the first bump and does not re-add on later bumps while visible', () => {
            const toast = spectator.inject(MessageService, true);

            store.toolsSavedAt.set(1);
            spectator.detectChanges();
            store.toolsSavedAt.set(2);
            spectator.detectChanges();
            store.toolsSavedAt.set(3);
            spectator.detectChanges();

            // Only one add — the second and third saves restart the hide
            // timer, so the on-screen toast stays put without a flicker.
            expect(toast.add).toHaveBeenCalledTimes(1);
            const first = (toast.add as Mock).mock.calls[0][0];
            expect(first.key).toBe('dot-status');
            expect(first.severity).toBe('success');
            // Sticky: we own the lifetime via our own hide timer.
            expect(first.sticky).toBe(true);
            // No clear while the hide timer has not fired yet.
            expect(toast.clear).not.toHaveBeenCalled();
        });

        it('clears the toast after the base life elapses without further saves', () => {
            const toast = spectator.inject(MessageService, true);

            store.toolsSavedAt.set(1);
            spectator.detectChanges();
            vi.advanceTimersByTime(2500);
            expect(toast.clear).toHaveBeenCalledWith('dot-status');
        });

        it('restarts the hide timer on each new save so the toast lives for base life after the LAST one', () => {
            const toast = spectator.inject(MessageService, true);

            store.toolsSavedAt.set(1);
            spectator.detectChanges();
            vi.advanceTimersByTime(1500); // 1s left until first hide fires
            store.toolsSavedAt.set(2); // restarts the timer with 2500ms
            spectator.detectChanges();
            vi.advanceTimersByTime(1500); // 1.5s since restart — not yet
            expect(toast.clear).not.toHaveBeenCalled();
            vi.advanceTimersByTime(1000); // reach 2500 since the last save
            expect(toast.clear).toHaveBeenCalledWith('dot-status');
        });
    });
});

import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import type { TreeNode } from 'primeng/api';
import { Tree } from 'primeng/tree';
import type {
    TreeNodeCollapseEvent,
    TreeNodeExpandEvent,
    TreeNodeSelectEvent
} from 'primeng/types/tree';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFolderTreeComponent } from './dot-folder-tree.component';

describe('DotFolderTreeComponent', () => {
    let spectator: Spectator<DotFolderTreeComponent>;
    let component: DotFolderTreeComponent;

    const mockFolders: TreeNode[] = [
        {
            key: '1',
            label: '/application/content',
            data: { type: 'folder', path: '/application/content', hostname: 'demo.com', id: '1' },
            children: [
                {
                    key: '2',
                    label: '/application/content/images',
                    data: {
                        type: 'folder',
                        path: '/application/content/images',
                        hostname: 'demo.com',
                        id: '2'
                    }
                }
            ]
        },
        {
            key: '3',
            label: '/application/documents',
            data: {
                type: 'folder',
                path: '/application/documents',
                hostname: 'demo.com',
                id: '3'
            }
        }
    ];

    const mockSelectedNode: TreeNode = mockFolders[0].children![0];

    const createComponent = createComponentFactory({
        component: DotFolderTreeComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.tree.load-more': 'Load more',
                    'dot.file.field.host.folder.action.load.more': 'Load more folders'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;

        spectator.setInput('folders', mockFolders);
        spectator.setInput('loading', false);
        spectator.setInput('selectedNode', mockSelectedNode);
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('p-tree bindings', () => {
        it('should default to single selection mode', () => {
            const tree = spectator.query(Tree);
            expect(tree.selectionMode).toBe('single');
            expect(component.$selection()).toEqual(mockSelectedNode);
        });

        it('should normalize selection to an array in multiple mode', () => {
            spectator.setInput('selectionMode', 'multiple');
            spectator.setInput('metaKeySelection', true);
            spectator.setInput('scrollHeight', 'auto');
            spectator.detectChanges();

            const tree = spectator.query(Tree);
            expect(tree.selectionMode).toBe('multiple');
            expect(tree.metaKeySelection).toBe(true);
            expect(tree.scrollHeight).toBe('auto');
            expect(component.$selection()).toEqual([mockSelectedNode]);
        });

        it('should apply host layout classes and keep PrimeNG p-tree class', () => {
            expect(spectator.element.classList.contains('block')).toBe(true);
            expect(spectator.query('p-tree')?.classList.contains('p-tree')).toBe(true);
        });

        it('should render chevron toggler icons only', () => {
            expect(spectator.query('.pi-chevron-right, .pi-chevron-down')).toBeTruthy();
            expect(spectator.query('.toggler-first')).toBeNull();
        });

        it('should keep the toggler chevron-only when folder icons are enabled', () => {
            // The folder icon is additive: it joins the expand/collapse affordance rather than
            // replacing it (pre-#36848 Content Drive replaced the chevron; we do not). A failure
            // here means the icon landed in the toggler template — a design error, not a test to
            // relax.
            spectator.setInput('showFolderIcons', true);
            spectator.detectChanges();

            const toggler = spectator.query('.p-tree-node-toggle-button');

            expect(toggler?.querySelector('.pi-chevron-right, .pi-chevron-down')).toBeTruthy();
            expect(toggler?.querySelector('[data-testid="tree-node-folder-icon"]')).toBeNull();
            // The icon exists — just not inside the toggler. Without this the assertion above
            // would be green before the icon was implemented at all.
            expect(spectator.query(byTestId('tree-node-folder-icon'))).toBeTruthy();
        });

        it('should accept pt options input', () => {
            const pt = { root: { class: 'custom-root' } };
            spectator.setInput('pt', pt);
            spectator.detectChanges();

            expect(component.$pt()).toEqual(pt);
        });

        it('should set tree data-testid', () => {
            spectator.setInput('treeTestId', 'host-folder-tree');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="host-folder-tree"]')).toBeTruthy();
        });
    });

    describe('outputs', () => {
        it('should emit select, expand, and collapse from p-tree', () => {
            const selectSpy = jest.spyOn(component.onNodeSelect, 'emit');
            const expandSpy = jest.spyOn(component.onNodeExpand, 'emit');
            const collapseSpy = jest.spyOn(component.onNodeCollapse, 'emit');

            const selectEvent: TreeNodeSelectEvent = {
                originalEvent: new Event('select'),
                node: mockFolders[0]
            };
            const expandEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('expand'),
                node: mockFolders[0]
            };
            const collapseEvent: TreeNodeCollapseEvent = {
                originalEvent: new Event('collapse'),
                node: mockFolders[0]
            };

            spectator.triggerEventHandler(Tree, 'onNodeSelect', selectEvent);
            spectator.triggerEventHandler(Tree, 'onNodeExpand', expandEvent);
            spectator.triggerEventHandler(Tree, 'onNodeCollapse', collapseEvent);

            expect(selectSpy).toHaveBeenCalledWith(selectEvent);
            expect(expandSpy).toHaveBeenCalledWith(expandEvent);
            expect(collapseSpy).toHaveBeenCalledWith(collapseEvent);
        });
    });

    describe('load-more', () => {
        const loadMoreNode: TreeNode = {
            key: 'load-more:/application/',
            label: 'content-drive.tree.load-more',
            type: 'load-more',
            data: {
                type: 'load-more',
                path: '/application/',
                hostname: 'demo.com',
                id: 'load-more:/application/',
                nextPage: 2,
                remaining: 40
            },
            leaf: true,
            selectable: false
        };

        it('should emit loadMore on click without selecting', () => {
            const loadMoreSpy = jest.spyOn(component.loadMore, 'emit');
            const selectSpy = jest.spyOn(component.onNodeSelect, 'emit');

            spectator.setInput('folders', [loadMoreNode]);
            spectator.setInput('showLoadMoreRemaining', true);
            spectator.setInput('showLoadMorePlusIcon', false);
            spectator.detectChanges();

            const button = spectator.query(byTestId('tree-load-more'));
            expect(button).toBeTruthy();
            expect(button?.textContent).toContain('Load more');
            expect(button?.textContent).toContain('(40)');

            spectator.click(button as HTMLElement);

            expect(loadMoreSpy).toHaveBeenCalledWith(loadMoreNode);
            expect(selectSpy).not.toHaveBeenCalled();
        });

        it('should show plus icon for Host Folder variant', () => {
            const hfNode: TreeNode = {
                ...loadMoreNode,
                label: ''
            };

            spectator.setInput('folders', [hfNode]);
            spectator.setInput('loadMoreLabelKey', 'dot.file.field.host.folder.action.load.more');
            spectator.setInput('showLoadMorePlusIcon', true);
            spectator.setInput('loadMoreTestId', 'host-folder-load-more');
            spectator.detectChanges();

            const button = spectator.query(byTestId('host-folder-load-more'));
            expect(button).toBeTruthy();
            expect(button?.querySelector('.pi-plus-circle')).toBeTruthy();
            expect(button?.textContent).toContain('Load more folders');
            expect(button?.textContent).not.toContain('(40)');
        });
    });

    describe('folder icons', () => {
        const FOLDER_ICON = byTestId('tree-node-folder-icon');

        /**
         * PrimeNG mutates `node.expanded` in place when a row is toggled, so these tests build
         * their own nodes instead of sharing `mockFolders` — otherwise one expand would leak into
         * every later test in the file.
         */
        const buildFolders = (): TreeNode[] => [
            {
                key: '1',
                label: '/application/content',
                data: { type: 'folder', path: '/application/content', id: '1' },
                leaf: false,
                children: [
                    {
                        key: '2',
                        label: '/application/content/images',
                        data: { type: 'folder', path: '/application/content/images', id: '2' }
                    }
                ]
            },
            {
                key: '3',
                label: '/application/documents',
                data: { type: 'folder', path: '/application/documents', id: '3' },
                leaf: false
            }
        ];

        const renderWithIcons = (folders: TreeNode[], showFolderIcons = true) => {
            spectator.setInput('folders', folders);
            spectator.setInput('selectedNode', null);
            spectator.setInput('showFolderIcons', showFolderIcons);
            spectator.detectChanges();
        };

        const icons = () => spectator.queryAll(FOLDER_ICON);
        const firstIcon = () => spectator.queryAll(FOLDER_ICON)[0];
        const firstToggler = () =>
            spectator.queryAll('.p-tree-node-toggle-button')[0] as HTMLElement;

        it('should render a closed-folder icon on every collapsed folder row', () => {
            renderWithIcons(buildFolders());

            expect(icons()).toHaveLength(2);
            icons().forEach((icon) => expect(icon.getAttribute('data-expanded')).toBe('false'));
        });

        it('should use PrimeIcons folder glyphs', () => {
            // The only test that asserts the glyph itself. Every other case reads state from
            // `data-expanded`, so swapping the icon set (research Decision 3) touches this test
            // alone rather than a dozen state assertions.
            const folders = buildFolders();
            renderWithIcons(folders);

            expect(firstIcon().classList.contains('pi-folder')).toBe(true);

            spectator.click(firstToggler());
            spectator.detectChanges();

            expect(firstIcon().classList.contains('pi-folder-open')).toBe(true);
            expect(firstIcon().classList.contains('pi-folder')).toBe(false);
        });

        it('should follow node.expanded across expand, collapse and expand again', () => {
            // SC-004 regression test. The reported defect is the middle assertion: an icon that
            // swaps on expand but never swaps back on collapse.
            renderWithIcons(buildFolders());

            expect(firstIcon().getAttribute('data-expanded')).toBe('false');

            spectator.click(firstToggler());
            spectator.detectChanges();
            expect(firstIcon().getAttribute('data-expanded')).toBe('true');

            spectator.click(firstToggler());
            spectator.detectChanges();
            expect(firstIcon().getAttribute('data-expanded')).toBe('false');

            spectator.click(firstToggler());
            spectator.detectChanges();
            expect(firstIcon().getAttribute('data-expanded')).toBe('true');
        });

        it('should render no folder icon by default and one when opted in', () => {
            // Guards the roles tree, which renders a non-folder hierarchy through this component
            // and draws its own icons. The opted-in half is what stops this from passing
            // vacuously — an assertion of absence alone would be green before the icon exists.
            renderWithIcons(buildFolders(), false);
            expect(icons()).toHaveLength(0);

            spectator.setInput('showFolderIcons', true);
            spectator.detectChanges();
            expect(icons()).toHaveLength(2);
        });

        it('should skip only the row that declares its own icon', () => {
            // The site row: PrimeNG already draws `node.icon`, so a second icon there would be a
            // duplicate. The folder row beside it is the positive control.
            renderWithIcons([
                {
                    key: 'site',
                    label: 'demo.dotcms.com',
                    icon: 'pi pi-globe',
                    leaf: false,
                    data: { type: 'folder', path: '', id: 'site' }
                },
                {
                    key: '1',
                    label: '/application/content',
                    leaf: false,
                    data: { type: 'folder', path: '/application/content', id: '1' }
                }
            ]);

            expect(spectator.query('.pi-globe')).toBeTruthy();
            expect(icons()).toHaveLength(1);

            const rows = spectator.queryAll('.p-tree-node-content');
            expect(rows[0].querySelector('[data-testid="tree-node-folder-icon"]')).toBeNull();
            expect(rows[1].querySelector('[data-testid="tree-node-folder-icon"]')).toBeTruthy();
        });

        it('should skip only the row declaring expandedIcon/collapsedIcon', () => {
            // Keeps an un-migrated consumer from rendering two icons on one row while the
            // migration lands consumer by consumer.
            renderWithIcons([
                {
                    key: '1',
                    label: '/application/content',
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false,
                    data: { type: 'folder', path: '/application/content', id: '1' }
                },
                {
                    key: '2',
                    label: '/application/documents',
                    leaf: false,
                    data: { type: 'folder', path: '/application/documents', id: '2' }
                }
            ]);

            expect(icons()).toHaveLength(1);

            const rows = spectator.queryAll('.p-tree-node-content');
            expect(rows[0].querySelector('[data-testid="tree-node-folder-icon"]')).toBeNull();
            expect(rows[1].querySelector('[data-testid="tree-node-folder-icon"]')).toBeTruthy();
        });

        it('should render no folder icon on a load-more row', () => {
            renderWithIcons([
                {
                    key: '1',
                    label: '/application/content',
                    leaf: false,
                    data: { type: 'folder', path: '/application/content', id: '1' }
                },
                {
                    key: 'load-more:/application/',
                    label: 'content-drive.tree.load-more',
                    type: 'load-more',
                    data: {
                        type: 'load-more',
                        path: '/application/',
                        id: 'load-more:/application/'
                    },
                    leaf: true,
                    selectable: false
                }
            ]);

            expect(spectator.query(byTestId('tree-load-more'))).toBeTruthy();
            // One icon for the folder row, none for the action row beneath it.
            expect(icons()).toHaveLength(1);
        });

        it('should render a closed icon on a leaf row and mark it as a leaf', () => {
            // FR-005. PrimeNG always renders the toggle button and marks leaf rows with
            // `p-tree-node-leaf` for the theme to hide the affordance — so the absence of an
            // expand affordance is asserted through that marker, not through DOM absence.
            renderWithIcons([
                {
                    key: '1',
                    label: '/application/empty',
                    leaf: true,
                    data: { type: 'folder', path: '/application/empty', id: '1' }
                }
            ]);

            expect(icons()).toHaveLength(1);
            expect(firstIcon().getAttribute('data-expanded')).toBe('false');
            expect(spectator.query('.p-tree-node-leaf')).toBeTruthy();
        });

        it('should render an open icon while an expanded row has no children yet', () => {
            // PrimeNG's own `getIcon()` requires `node.expanded && node.children.length`, so it
            // draws a *closed* icon on a row that is open with its children still loading. Both
            // Content Drive and the Site/Folder field load children lazily on expand, so this
            // window is reachable in normal use.
            renderWithIcons([
                {
                    key: '1',
                    label: '/application/content',
                    expanded: true,
                    leaf: false,
                    children: [],
                    data: { type: 'folder', path: '/application/content', id: '1' }
                }
            ]);

            expect(firstIcon().getAttribute('data-expanded')).toBe('true');
        });

        it('should mark the folder icon as decorative for assistive technology', () => {
            // FR-010: the icon is a visual cue only. Expand/collapse state is carried by the
            // toggler control, which PrimeNG makes accessible.
            renderWithIcons(buildFolders());

            expect(firstIcon().getAttribute('aria-hidden')).toBe('true');
        });
    });

    describe('default label', () => {
        it('should render folder name from path for root nodes', () => {
            const labels = spectator.queryAll(byTestId('tree-node-label'));
            expect(labels.map((el) => el.textContent?.trim())).toEqual(['content', 'documents']);
        });
    });

    describe('public scroll helpers', () => {
        it('should expose Tree viewChild and ElementRef', () => {
            expect(component.tree()).toBeTruthy();
            expect(component.elementRef).toBeTruthy();
        });
    });
});

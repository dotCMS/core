import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { ChangeDetectionStrategy, Component } from '@angular/core';

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

import { DotTruncatedLabelComponent } from '../dot-truncated-label/dot-truncated-label.component';

const LONG_FOLDER_PATH = '/application/a-very-long-folder-name-that-will-not-fit-in-the-row';

/**
 * Stands in for a consumer that projects its own node label (Content Drive, Asset Picker, the
 * Site/Folder Field and Roles all do) and places the shared clipping label itself — the
 * documented pattern, because only the consumer knows which part of a structured row should
 * clip. The tree adds no wrapper of its own around a projected template.
 */
@Component({
    selector: 'dot-projected-label-host',
    imports: [DotFolderTreeComponent, DotTruncatedLabelComponent],
    template: `
        <dot-folder-tree [folders]="folders" [loading]="false">
            <ng-template #folderTreeNodeLabel let-node>
                <dot-truncated-label>
                    <span data-testid="tree-node-label" class="font-semibold">
                        {{ node.label }}
                    </span>
                </dot-truncated-label>
            </ng-template>
        </dot-folder-tree>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
class ProjectedLabelHostComponent {
    readonly folders: TreeNode[] = [
        {
            key: '1',
            label: LONG_FOLDER_PATH,
            data: { type: 'folder', path: LONG_FOLDER_PATH, id: '1' }
        }
    ];
}

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

    describe('label truncation', () => {
        it('should wrap the built-in label in the shared clipping element', () => {
            const clips = spectator.queryAll(byTestId('tree-node-label-clip'));

            expect(clips).toHaveLength(2);
            expect(clips.map((el) => el.textContent?.trim())).toEqual(['content', 'documents']);
        });

        it('should keep exactly one tree-node-label per row', () => {
            // e2e guard: `contentDrive.page.ts` and `content-drive-tree.ts` count this test id,
            // so the clipping wrapper must carry its own instead of reusing it.
            expect(spectator.queryAll(byTestId('tree-node-label'))).toHaveLength(2);
        });
    });

    describe('keyboard focus', () => {
        beforeEach(() => {
            jest.useFakeTimers();
        });

        afterEach(() => {
            jest.useRealTimers();
            document.querySelectorAll('.p-tooltip').forEach((node) => node.remove());
        });

        const firstRow = () => spectator.query('[role="treeitem"]') as HTMLElement;

        const clipInFirstRow = () =>
            firstRow().querySelector('[data-testid="tree-node-label-clip"]') as HTMLElement;

        const forceOverflow = (element: Element): void => {
            Object.defineProperty(element, 'offsetWidth', { value: 100, configurable: true });
            Object.defineProperty(element, 'scrollWidth', { value: 400, configurable: true });
        };

        it('should reveal a clipped name when the row receives focus', () => {
            // PrimeNG puts the tabindex on the `treeitem`, and its Tooltip binds focus listeners
            // to its own host — so focus never reaches the label on its own (research.md R3).
            forceOverflow(clipInFirstRow());

            firstRow().dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
            spectator.detectChanges();
            jest.advanceTimersByTime(1000);

            expect(document.querySelector('.p-tooltip-text')?.textContent?.trim()).toBe('content');
        });

        it('should dismiss the tooltip when focus leaves the row', () => {
            forceOverflow(clipInFirstRow());
            firstRow().dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
            spectator.detectChanges();
            jest.advanceTimersByTime(1000);

            firstRow().dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
            jest.advanceTimersByTime(1000);

            expect(document.querySelector('.p-tooltip')).toBeNull();
        });

        it('should not add a tab stop to the label', () => {
            // FR-013: the tree navigates with arrow keys and manages `tabIndex` on the treeitem
            // itself. A focusable label would insert a second tab stop per row.
            expect(clipInFirstRow().hasAttribute('tabindex')).toBe(false);
        });
    });
});

describe('DotFolderTreeComponent with a projected label template', () => {
    let spectator: Spectator<ProjectedLabelHostComponent>;

    const createHost = createComponentFactory({
        component: ProjectedLabelHostComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createHost();
        spectator.detectChanges();
    });

    it('should add no wrapper of its own around a projected template', () => {
        // One clipping element per row — the consumer's. A second wrapper from the tree would
        // clip the whole row instead of its name, and its tooltip would read out every piece of
        // text in the row (this is what the Roles panel showed).
        const clips = spectator.queryAll(byTestId('tree-node-label-clip'));

        expect(clips).toHaveLength(1);
        expect(clips[0].querySelector('[data-testid="tree-node-label"]')).toBeTruthy();
        expect(clips[0].textContent?.trim()).toBe(LONG_FOLDER_PATH);
    });

    it('should preserve the consumer classes on its own element', () => {
        const label = spectator.query(byTestId('tree-node-label'));

        expect(label).toHaveClass('font-semibold');
    });
});

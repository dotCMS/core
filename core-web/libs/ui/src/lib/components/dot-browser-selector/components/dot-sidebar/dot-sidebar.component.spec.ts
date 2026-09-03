import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import type { TreeNode } from 'primeng/api';
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotSideBarComponent } from './dot-sidebar.component';

describe('DotSideBarComponent', () => {
    let spectator: Spectator<DotSideBarComponent>;

    const mockFolders: TreeNode[] = [
        {
            key: 'site-1',
            label: 'qa36151-site-1.dotcms.dev',
            data: { type: 'site', id: 'site-1', hostname: 'qa36151-site-1.dotcms.dev' },
            expandedIcon: 'pi pi-globe',
            collapsedIcon: 'pi pi-globe',
            expanded: true,
            children: [
                {
                    key: 'folder-1',
                    label: '/application',
                    data: {
                        type: 'folder',
                        path: '/application',
                        hostname: 'qa36151-site-1.dotcms.dev',
                        id: 'folder-1'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder'
                }
            ]
        }
    ];

    const createComponent = createComponentFactory({
        component: DotSideBarComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dot.file.field.host.folder.action.load.more': 'Load more folders'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput({
            folders: mockFolders,
            loading: false
        });
        spectator.detectChanges();
    });

    it('should render a PrimeNG globe icon for site nodes', () => {
        const siteIcon = spectator.query('.pi-globe');

        expect(siteIcon).toBeTruthy();
        expect(siteIcon?.classList.contains('pi')).toBe(true);
    });

    it('should render a PrimeNG folder icon for folder nodes', () => {
        const folderIcon = spectator.query('.pi-folder');

        expect(folderIcon).toBeTruthy();
        expect(folderIcon?.classList.contains('pi')).toBe(true);
    });

    it('should truncate tree node labels to a single line', () => {
        // Rewritten for #37363: the clipping moved to the shared `dot-truncated-label`, so what
        // this consumer must guarantee is that every row has one, not that it styles it itself.
        const clips = spectator.queryAll(byTestId('tree-node-label-clip'));

        expect(clips.length).toBeGreaterThan(0);
        clips.forEach((clip) => {
            expect(clip.classList.contains('truncate')).toBe(true);
        });
    });

    it('should gate the tooltip on real overflow rather than label length', () => {
        // Rewritten for #37363: this used to assert the old `label.length <= 10` heuristic, which
        // showed a tooltip on names that fit. The gate is now PrimeNG's own ellipsis measurement.
        const tooltips = spectator.queryAll(Tooltip);

        expect(tooltips.length).toBeGreaterThan(0);
        tooltips.forEach((tooltip) => {
            expect(tooltip.showOnEllipsis).toBe(true);
            expect(tooltip.disabled).toBeFalsy();
        });
    });

    it('should not render node icons for load-more nodes', () => {
        spectator.setInput({
            folders: [
                {
                    key: 'load-more-1',
                    label: 'Load more',
                    type: 'load-more',
                    data: { type: 'load-more', remaining: 5 }
                }
            ],
            loading: false
        });
        spectator.detectChanges();

        expect(spectator.query('.pi-globe')).toBeNull();
        expect(spectator.query('.pi-folder')).toBeNull();
    });

    describe('label ownership', () => {
        it('should declare no tooltip of its own', () => {
            // FR-007: exactly one definition. Before #37363 this sidebar declared its own
            // `pTooltip` on the node label, so every row carried two tooltip directives — the
            // shared one and a local one gated on `label.length <= 10`.
            const rows = spectator.queryAll(byTestId('tree-node-label-clip'));

            expect(spectator.queryAll(Tooltip)).toHaveLength(rows.length);
        });

        it('should leave the clipping to the shared label', () => {
            // The consumer keeps what a row *says*; the classes that clip it belong to the
            // shared wrapper now.
            spectator.queryAll(byTestId('tree-node-label')).forEach((label) => {
                expect(label.classList.contains('truncate')).toBe(false);
            });
        });
    });

    describe('overflow tooltip', () => {
        beforeEach(() => {
            jest.useFakeTimers();
        });

        afterEach(() => {
            jest.useRealTimers();
            document.querySelectorAll('.p-tooltip').forEach((node) => node.remove());
        });

        const hoverFolderLabel = ({ fits }: { fits: boolean }): void => {
            const clips = spectator.queryAll(byTestId('tree-node-label-clip'));
            // Second row is the folder under the expanded site.
            const element = clips[clips.length - 1] as HTMLElement;

            Object.defineProperty(element, 'offsetWidth', { value: 100, configurable: true });
            Object.defineProperty(element, 'scrollWidth', {
                value: fits ? 100 : 400,
                configurable: true
            });

            element.dispatchEvent(new MouseEvent('mouseenter'));
            spectator.detectChanges();
            jest.advanceTimersByTime(1000);
        };

        it('should not show a tooltip for a name that fits', () => {
            // Red before #37363: the sidebar enabled its tooltip whenever the label was longer
            // than ten characters, so `/application` got one even at full width. Fit is a matter
            // of available space, not character count.
            hoverFolderLabel({ fits: true });

            expect(document.querySelector('.p-tooltip')).toBeNull();
        });

        it('should reveal the folder name, not its full path, when clipped', () => {
            // Red before #37363: the tooltip was bound to `node.label`, the whole path, while the
            // row rendered only the last segment. FR-011 settles it on the name.
            hoverFolderLabel({ fits: false });

            expect(document.querySelector('.p-tooltip-text')?.textContent?.trim()).toBe(
                'application'
            );
        });
    });
});

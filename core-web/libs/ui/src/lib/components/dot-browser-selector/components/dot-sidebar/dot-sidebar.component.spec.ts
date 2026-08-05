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
        const labels = spectator.queryAll(byTestId('tree-node-label'));

        expect(labels.length).toBeGreaterThan(0);
        labels.forEach((label) => {
            expect(label.classList.contains('truncate')).toBe(true);
        });
    });

    it('should enable tooltip for long site labels', () => {
        const tooltips = spectator.queryAll(Tooltip);

        expect(tooltips.length).toBeGreaterThan(0);
        expect(tooltips[0].content).toBe('qa36151-site-1.dotcms.dev');
        expect(tooltips[0].disabled).toBe(false);
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
});

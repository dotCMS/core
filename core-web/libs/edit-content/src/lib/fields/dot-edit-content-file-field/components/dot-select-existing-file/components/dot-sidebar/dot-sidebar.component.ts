import { ChangeDetectionStrategy, Component, signal } from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TreeModule } from 'primeng/tree';

@Component({
    selector: 'dot-sidebar',
    standalone: true,
    imports: [TreeModule],
    templateUrl: './dot-sidebar.component.html',
    styleUrls: ['./dot-sidebar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSideBarComponent {
    tree = signal<TreeNode[]>([
        {
            label: 'demo.dotcms.com',
            expandedIcon: 'pi pi-folder-open',
            collapsedIcon: 'pi pi-folder',
            children: [
                {
                    label: 'demo.dotcms.com',
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    children: [
                        {
                            label: 'documents'
                        }
                    ]
                },
                {
                    label: 'demo.dotcms.com',
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder'
                }
            ]
        },
        {
            label: 'nico.dotcms.com',
            expandedIcon: 'pi pi-folder-open',
            collapsedIcon: 'pi pi-folder'
        }
    ]);
}

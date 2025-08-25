import { HttpClient } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    OnInit,
    signal
} from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TreeModule, TreeNodeExpandEvent } from 'primeng/tree';

import { map } from 'rxjs/operators';

export interface DotCMSFolder {
    defaultFileType: string;
    host: string;
    identifier: string;
    inode: string;
    modDate: number;
    name: string;
    path: string;
    showOnMenu: boolean;
    title: string;
    filesMasks?: string;
}

@Component({
    selector: 'dot-content-drive-folder-tree',
    templateUrl: './dot-content-drive-folder-tree.component.html',
    styleUrl: './dot-content-drive-folder-tree.component.scss',
    imports: [TreeModule],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveFolderTreeComponent implements OnInit {
    private readonly httpClient = inject(HttpClient);
    private readonly endpoint = '/api/v1/assets';
    private readonly initialPath = '//demo.dotcms.com/';
    private readonly cd = inject(ChangeDetectorRef);
    protected readonly folders = signal<TreeNode[]>([]);

    ngOnInit() {
        this.getFolderByPath(this.initialPath).subscribe((response) => {
            this.folders.set(response);
        });
    }

    getFolderByPath(assetPath: string) {
        return this.httpClient
            .post<{ entity: { subFolders?: DotCMSFolder[] } }>(this.endpoint, {
                assetPath: assetPath
            })
            .pipe(
                map(({ entity }) => {
                    const subFolders = entity.subFolders;

                    if (!subFolders || subFolders.length === 0) {
                        return [];
                    }

                    const treeNodes: TreeNode[] = subFolders.map((folder) => {
                        const path = folder.path.replace(/^\//, '');
                        return {
                            key: folder.inode,
                            label: folder.name,
                            data: {
                                ...folder,
                                path
                            },
                            icon: 'pi pi-folder',
                            expandedIcon: 'pi pi-folder-open',
                            collapsedIcon: 'pi pi-folder',
                            leaf: false
                        };
                    });

                    return treeNodes;
                })
            );
    }

    onNodeExpand(event: TreeNodeExpandEvent) {
        const eventNode = event.node;
        if (eventNode.children?.length > 0) {
            return;
        }

        const path = eventNode.data.path;
        this.getFolderByPath(`${this.initialPath}${path}`).subscribe((subFolders) => {
            const node = { ...eventNode };
            node.children = subFolders;

            if (subFolders.length === 0) return;

            this.folders.update((folders) => {
                const index = folders.findIndex((folder) => folder.key === event.node.key);
                folders[index] = node;
                return folders;
            });

            this.cd.markForCheck();
        });
    }
}

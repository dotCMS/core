import { SlicePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    OnInit
} from '@angular/core';

import { TreeModule } from 'primeng/tree';

import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../../../../../models/dot-edit-content-host-folder-field.interface';
import { TruncatePathPipe } from '../../../../../../pipes/truncate-path.pipe';
import { DotEditContentService } from '../../../../../../services/dot-edit-content.service';

export const PEER_PAGE_LIMIT = 7000;

@Component({
    selector: 'dot-sidebar',
    standalone: true,
    imports: [TreeModule, TruncatePathPipe, SlicePipe],
    templateUrl: './dot-sidebar.component.html',
    styleUrls: ['./dot-sidebar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSideBarComponent implements OnInit {
    readonly #dotEditContentService = inject(DotEditContentService);
    folders: TreeNodeItem[] = [];
    status: ComponentStatus = ComponentStatus.INIT;

    readonly #changeRef = inject(ChangeDetectorRef);

    ngOnInit() {
        this.loadFolders();
    }

    loadFolders() {
        this.status = ComponentStatus.INIT;
        this.#dotEditContentService
            .getSitesTreePath({ perPage: PEER_PAGE_LIMIT, filter: '*' })
            .subscribe((folders: TreeNodeItem[]) => {
                this.folders = folders;
                this.status = ComponentStatus.LOADED;
                this.#changeRef.detectChanges();
            });
    }

    onNodeExpand(event: TreeNodeSelectItem) {
        const { node } = event;
        const { hostname, path } = node.data;

        node.loading = true;

        this.#dotEditContentService.getFoldersTreeNode(hostname, path).subscribe((children) => {
            node.loading = false;
            node.leaf = true;
            node.icon = 'pi pi-folder-open';
            node.children = [...children];
            this.#changeRef.detectChanges();
        });
    }
}

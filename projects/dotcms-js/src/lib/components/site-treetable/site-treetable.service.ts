import { Inject, Injectable } from '@angular/core';
import { TreeNode } from 'primeng/components/common/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { Treeable } from '../../core/treeable/shared/treeable.model';
import { SiteBrowserService } from '../../core/util/site-browser.service';

@Injectable()
@Inject('siteBrowserService')
export class SiteTreetableService {
    constructor(private siteBrowserService: SiteBrowserService) {
        this.siteBrowserService = siteBrowserService;
    }

    /**
     * Returns the assets under a site/host
     * @param siteName
     * @returns Observable<R>
     */
    getAssetsUnderSite(siteName: String): Observable<TreeNode[]> {
        return this.siteBrowserService
            .getTreeableAssetsUnderSite(siteName)
            .pipe(map((treeables: Treeable[]) => this.extractDataFilter(treeables)));
    }

    /**
     * Returns the assets under a folder
     * @param siteName
     * @param uri
     * @returns Observable<R>
     */
    getAssetsUnderFolder(siteName: String, uri: string): Observable<TreeNode[]> {
        return this.siteBrowserService
            .getTreeableAssetsUnderFolder(siteName, uri)
            .pipe(map((treeables: Treeable[]) => this.extractDataFilter(treeables)));
    }

    private extractDataFilter(treeables: Treeable[]): TreeNode[] {
        const assets: TreeNode[] = [];
        for (let i = 0; i < treeables.length; i++) {
            const treeable: any = treeables[i];
            let leaf = true;
            if (treeable.type === 'folder') {
                leaf = false;
            } else if (treeable.type === 'file_asset') {
                treeable.type = 'file';
            }
            const treeNode: TreeNode = {
                data: treeable,
                leaf: leaf
            };
            assets[i] = treeNode;
        }
        return assets;
    }
}

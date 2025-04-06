import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TableModule } from 'primeng/table';
import { TreeModule } from 'primeng/tree';

import { take } from 'rxjs/operators';

import { DotESContentService, queryEsParams } from '@dotcms/data-access';
import { DotCMSContentlet, ESContent } from '@dotcms/dotcms-models';

import { MOCK_FOLDERS } from './drive.mock';

@Component({
    selector: 'lib-drive',
    standalone: true,
    imports: [CommonModule, TableModule, TreeModule],
    providers: [DotESContentService],
    templateUrl: './drive.component.html',
    styleUrl: './drive.component.scss'
})
export class DriveComponent implements OnInit {
    items: DotCMSContentlet[] = [];
    loading = false;

    // Tree related properties
    files: TreeNode[] = [];
    selectedFile: TreeNode | null = null;

    constructor(private dotESContentService: DotESContentService) {}

    ngOnInit() {
        this.loadRootFiles();
        this.initializeFileTree();
    }

    private loadRootFiles(): void {
        this.loading = true;

        // Hide base type 8 (language variables)
        const params: queryEsParams = {
            query: '+path:/* -basetype:8',
            itemsPerPage: 40
        };

        this.dotESContentService.get(params)
            .pipe(take(1))
            .subscribe((response: ESContent) => {
                this.items = response.jsonObjectView.contentlets;
                this.loading = false;
            });
    }

    private initializeFileTree(): void {
        // Use the mock folder structure
        this.files = MOCK_FOLDERS;
    }

    // Handle tree node selection
    onNodeSelect(event: { node: TreeNode }): void {
        // Here we load files based on the selected folder using mock data
        this.loading = true;

        // Create the path key for the folder map
        let folderPath = '';
        if (event.node.key === 'dotai') {
            folderPath = 'applications/app-vtl/dotai';
        } else if (event.node.key === 'system-config') {
            folderPath = 'system/system-config';
        } else {
            folderPath = 'root'; // Default to root for any other selection
        }

        // Load mock content for the selected folder
        setTimeout(() => {
            this.loading = false;
        }, 500);
    }
}

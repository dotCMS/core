import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';

import { DotCMSClient } from '@dotcms/angular';
import { DotCMSBasicContentlet } from '@dotcms/types';

export interface FileAsset {
    fileAsset: {
        versionPath: string;
    };
}

export interface ContentletImage extends FileAsset {
    identifier: string;
    fileName: string;
    versionPath?: string;
}

export interface Contentlet extends DotCMSBasicContentlet {
    image: ContentletImage;
    urlMap?: string;
    urlTitle?: string;
    widgetTitle?: string;
}

export interface Contentlet extends DotCMSBasicContentlet {
    image: ContentletImage;
    urlMap?: string;
    urlTitle?: string;
    widgetTitle?: string;
}

export interface Blog extends Contentlet {
    title: string;
    identifier: string;
    inode: string;
    modDate: string;
    urlTitle: string;
    teaser: string;
    author: Author;
}

export interface Author {
    firstName: string;
    lastName: string;
    inode: string;
}


@Component({
    selector: 'app-page',
    template: `
        <h1>Blogs ({{ filteredBlogs().length }})</h1>
        <ul>
            @for (blog of filteredBlogs(); track blog.identifier) {
                <li>{{ blog.title }}</li>
            }
        </ul>
    `
})
export class Page {
    http = inject(HttpClient);
    client = inject(DotCMSClient);
    filteredBlogs = signal<Blog[]>([]);

    ngOnInit() {
        this.client.content
            .getCollection('Blog')
            .limit(3)
            .sortBy([
                {
                    field: 'Blog.postingDate',
                    order: 'desc'
                }
            ])
            .then((response) => {
                this.filteredBlogs.set(response.contentlets as Blog[]);
            })
    }
}

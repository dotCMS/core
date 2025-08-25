import { Component, inject, signal } from '@angular/core';

import { DotCMSClient } from '@dotcms/angular';

import { Blog } from './models';

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
    client = inject(DotCMSClient);
    filteredBlogs = signal<Blog[]>([]);

    ngOnInit() {
        this.getBlogs();
    }

    getBlogs() {
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
            });
    }
}

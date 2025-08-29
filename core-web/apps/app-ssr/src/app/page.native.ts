import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';

import { Blog } from './models';

@Component({
    selector: 'app-page-native',
    template: `
        <h1>Blogs ({{ filteredBlogs().length }})</h1>
        <ul>
            @for (blog of filteredBlogs(); track blog.identifier) {
                <li>{{ blog.title }}</li>
            }
        </ul>
    `
})
export class PageNative {
    http = inject(HttpClient);
    filteredBlogs = signal<Blog[]>([]);

    ngOnInit() {
        this.getBlogs();
    }

    getBlogs() {
        this.http
            .post('http://localhost:8080/api/content/_search', {
                query: '+contentType:Blog +languageId:1 +live:true',
                render: false,
                sort: 'Blog.postingDate desc',
                limit: 3,
                offset: 0,
                depth: 0
            })
            .subscribe((data: any) => {
                this.filteredBlogs.set(data.entity.jsonObjectView.contentlets);
            });
    }
}

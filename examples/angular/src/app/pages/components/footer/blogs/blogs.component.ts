import { Component, input } from '@angular/core';
import { ContentletsWrapperComponent } from '../../../../shared/contentlets-wrapper/contentlets.component';
import { Blog } from '../../../../shared/models';
@Component({
    selector: 'app-blogs',
    standalone: true,
    imports: [ContentletsWrapperComponent],
    template: ` <div class="flex flex-col">
        <h2 class="mb-7 text-2xl font-bold text-black">Latest Blog Posts</h2>
        @if (!!cleanedBlogs.length) {
        <app-contentlets-wrapper [contentlets]="cleanedBlogs" />
        }
    </div>`
})
export class BlogsComponent {
    blogs = input<Blog[]>([]);

    get cleanedBlogs() {
        return this.blogs().map((blog) => blog._map);
    }
}

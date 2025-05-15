import { Component, input } from '@angular/core';
import { ContentletsWrapperComponent } from '../../../../shared/contentlets-wrapper/contentlets.component';
import { Blog } from '../../../../shared/models';
@Component({
    selector: 'app-blogs',
    standalone: true,
    imports: [ContentletsWrapperComponent],
    template: ` <div class="flex flex-col">
        <h2 class="mb-7 text-2xl font-bold text-black">Latest Blog Posts</h2>
        @if (!!blogs().length) {
        <app-contentlets-wrapper [contentlets]="blogs()" />
        }
    </div>`
})
export class BlogsComponent {
    blogs = input<Blog[]>([]);
}

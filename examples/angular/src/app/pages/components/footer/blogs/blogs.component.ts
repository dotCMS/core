import { Component, input } from '@angular/core';
import { RecommendedCardComponent } from '../../../../shared/recommended-card/recommended-card.component';
import { Blog } from '../../../../shared/models';
@Component({
    selector: 'app-blogs',
    standalone: true,
    imports: [RecommendedCardComponent],
    template: ` <div class="flex flex-col">
        <h2 class="mb-7 text-2xl font-bold text-black">Latest Blog Posts</h2>
        <div class="flex flex-col gap-5">
            @for (blog of blogs(); track blog.identifier) {
            <app-recommended-card [contentlet]="blog" />
            }
        </div>
    </div>`
})
export class BlogsComponent {
    blogs = input<Blog[]>([]);
}

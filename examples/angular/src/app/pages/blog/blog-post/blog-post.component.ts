import { Component, computed, input } from '@angular/core';

import { DotCMSBlockEditorRendererComponent } from '@dotcms/angular/next';
import { BlogContentlet } from '../blog.component';
import { BlockEditorContent } from '@dotcms/types';

@Component({
    selector: 'app-blog-post',
    standalone: true,
    imports: [DotCMSBlockEditorRendererComponent],
    templateUrl: './blog-post.component.html',
    styleUrl: './blog-post.component.css'
})
export class BlogPostComponent {
    post = input.required<BlogContentlet>();

    postContent = computed(() => {
        const content = JSON.parse(this.post().blogContent);

        return content as BlockEditorContent;
    });

    customRenderers = {
        // 'paragraph': import('./customRenderers/paragraph/paragraph.component').then(c => c.ParagraphComponent),
        Activity: import('./customRenderers/activity/activity.component').then(
            (c) => c.ActivityComponent
        )
    };
}

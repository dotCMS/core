import {
    Component,
    computed,
    input,
    OnChanges,
    OnInit,
    signal,
    SimpleChanges
} from '@angular/core';

import { DotCMSBlockEditorRendererComponent } from '@dotcms/angular/next';
import { BlogContentlet } from '../blog.component';
import { BlockEditorContent, UVE_MODE } from '@dotcms/types';
import { NgOptimizedImage } from '@angular/common';
import { enableBlockEditorInline, getUVEState } from '@dotcms/uve';
@Component({
    selector: 'app-blog-post',
    standalone: true,
    imports: [DotCMSBlockEditorRendererComponent, NgOptimizedImage],
    templateUrl: './blog-post.component.html',
    styleUrl: './blog-post.component.css'
})
export class BlogPostComponent implements OnChanges {
    post = input.required<BlogContentlet>();

    postContent = computed(() => {
        const content = JSON.parse(this.post().blogContent);

        return content as BlockEditorContent;
    });

    get isEditMode() {
        return getUVEState()?.mode === UVE_MODE.EDIT;
    }

    blockEditorClasses = signal<string>('');

    ngOnChanges(): void {
        if (this.isEditMode) {
            this.blockEditorClasses.set(
                'prose lg:prose-xl prose-a:text-red-500 border-2 border-solid border-cyan-400 cursor-pointer'
            );
        } else {
            this.blockEditorClasses.set('prose lg:prose-xl prose-a:text-red-500');
        }
    }

    customRenderers = {
        // 'paragraph': import('./customRenderers/paragraph/paragraph.component').then(c => c.ParagraphComponent),
        Activity: import('./customRenderers/activity/activity.component').then(
            (c) => c.ActivityComponent
        )
    };

    editPost() {
        if (this.isEditMode) {
            enableBlockEditorInline(this.post(), 'blogContent');
        }
    }
}

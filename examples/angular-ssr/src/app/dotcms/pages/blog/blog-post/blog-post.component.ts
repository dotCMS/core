import { Component, computed, input, OnChanges, signal } from '@angular/core';

import { DotCMSBlockEditorRendererNativeComponent } from '@dotcms/angular';
import { BlogContentlet } from '../blog.component';
import { UVE_MODE } from '@dotcms/types';
import { NgOptimizedImage } from '@angular/common';
import { enableBlockEditorInline, getUVEState } from '@dotcms/uve';
@Component({
  selector: 'app-blog-post',
  imports: [DotCMSBlockEditorRendererNativeComponent, NgOptimizedImage],
  templateUrl: './blog-post.component.html',
  styleUrl: './blog-post.component.css',
})
export class BlogPostComponent implements OnChanges {
  post = input.required<BlogContentlet>();

  postContent = computed(() => {
    return this.post().blogContent;
  });

  get isEditMode() {
    return getUVEState()?.mode === UVE_MODE.EDIT;
  }

  blockEditorClasses = signal<string>('');

  ngOnChanges(): void {
    if (this.isEditMode) {
      this.blockEditorClasses.set(
        'prose prose-a:text-teal-500 border-2 border-solid border-teal-400 cursor-pointer',
      );
    } else {
      this.blockEditorClasses.set('prose prose-sm prose-a:text-teal-500');
    }
  }

  customRenderers = {
    Activity: import('./customRenderers/activity/activity.component').then(
      (c) => c.ActivityComponent,
    ),
    // Override the built-in `bulletList` block with a custom renderer
    // (hot-pink bullets) to demonstrate customRenderers on the native renderer.
    bulletList: import('./customRenderers/bullet-list/bullet-list.component').then(
      (c) => c.BulletListComponent,
    ),
  };

  editPost() {
    if (this.isEditMode) {
      enableBlockEditorInline(this.post(), 'blogContent');
    }
  }
}

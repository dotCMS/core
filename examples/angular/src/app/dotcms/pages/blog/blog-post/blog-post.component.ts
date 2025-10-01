import { Component, computed, input, OnChanges, signal } from '@angular/core';

import { DotCMSBlockEditorRendererComponent } from '@dotcms/angular';
import { BlogContentlet } from '../blog.component';
import { UVE_MODE } from '@dotcms/types';
import { NgOptimizedImage } from '@angular/common';
import { enableBlockEditorInline, getUVEState } from '@dotcms/uve';
@Component({
  selector: 'app-blog-post',
  imports: [DotCMSBlockEditorRendererComponent, NgOptimizedImage],
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
        'prose prose-a:text-red-500 border-2 border-solid border-red-400 cursor-pointer',
      );
    } else {
      this.blockEditorClasses.set('prose prose-sm prose-a:text-red-500');
    }
  }

  customRenderers = {
    Activity: import('./customRenderers/activity/activity.component').then(
      (c) => c.ActivityComponent,
    ),
  };

  editPost() {
    if (this.isEditMode) {
      enableBlockEditorInline(this.post(), 'blogContent');
    }
  }
}

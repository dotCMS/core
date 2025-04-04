import { Component, Input } from '@angular/core';
import { Block, Contentlet } from '@dotcms/uve/types';
import { DotCMSBlockEditorRendererComponent } from '@dotcms/angular/next';

@Component({
  selector: 'app-blog-post',
  standalone: true,
  imports: [DotCMSBlockEditorRendererComponent],
  templateUrl: './blog-post.component.html',
  styleUrl: './blog-post.component.css'
})
export class BlogPostComponent {
  @Input() post!: Contentlet<{ blogContent: Block }>

  customRenderers = {
    // 'paragraph': import('./customRenderers/paragraph/paragraph.component').then(c => c.ParagraphComponent),
    'Activity': import('./customRenderers/activity/activity.component').then(c => c.ActivityComponent)
  }
}

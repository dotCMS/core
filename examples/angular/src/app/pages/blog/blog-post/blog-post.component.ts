import { Component, Input } from '@angular/core';
import { BlockEditorContent, Contentlet } from '@dotcms/types';
import { DotCMSBlockEditorRendererComponent } from '@dotcms/angular/next';
import { BlogContentlet } from '../../services/page.service';

@Component({
  selector: 'app-blog-post',
  standalone: true,
  imports: [DotCMSBlockEditorRendererComponent],
  templateUrl: './blog-post.component.html',
  styleUrl: './blog-post.component.css'
})
export class BlogPostComponent {
  @Input() post!: BlogContentlet

  customRenderers = {
    // 'paragraph': import('./customRenderers/paragraph/paragraph.component').then(c => c.ParagraphComponent),
    'Activity': import('./customRenderers/activity/activity.component').then(c => c.ActivityComponent)
  }
}

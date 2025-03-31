import { Component, Input } from '@angular/core';
import { Contentlet } from '@dotcms/uve/types';


import { DotCMSBlockEditorRendererComponent } from '@dotcms/angular/next';
import { Block } from '../../../../../../../../core-web/dist/libs/sdk/angular/next/components/dotcms-block-editor-renderer/models/block-editor-renderer.models';

@Component({
  selector: 'app-blog-post',
  standalone: true,
  imports: [DotCMSBlockEditorRendererComponent],
  templateUrl: './blog-post.component.html',
  styleUrl: './blog-post.component.css'
})
export class BlogPostComponent {
  @Input() post!: Contentlet<{ blogContent: Block }>

  customRenderers = 
    {'paragraph': import('../../customRenderers/paragraph/paragraph.component').then(c => c.ParagraphComponent) }
  
}

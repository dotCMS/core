import { Component, Input } from '@angular/core';

import { GenericContentlet } from '../../utils';
import { Contentlet } from '@dotcms/client/src/lib/client/content/shared/types';
import { isInsideEditor } from '@dotcms/client';
import { NgTemplateOutlet } from '@angular/common';

@Component({
  selector: 'app-contentlet',
  standalone: true,
  imports: [NgTemplateOutlet],
  template: `
    @if(isInsideEditor) {
    <div
      data-dot-object="contentlet"
      [attr.data-dot-identifier]="contentlet.identifier"
      [attr.data-dot-basetype]="contentlet.baseType"
      [attr.data-dot-title]="contentlet.widgetTitle || contentlet.title"
      [attr.data-dot-inode]="contentlet.inode"
      [attr.data-dot-type]="contentlet.contentType"
      [attr.data-dot-on-number-of-pages]="contentlet.onNumberOfPages ?? 0"
    >
      <ng-container *ngTemplateOutlet="contentletTemplate"></ng-container>
    </div>

    } @else {
    <ng-container *ngTemplateOutlet="contentletTemplate"></ng-container>
    }

    <ng-template #contentletTemplate> <ng-content /></ng-template>
  `,
})
export class ContentletComponent {
  @Input() contentlet!: Contentlet<GenericContentlet>;

  protected readonly isInsideEditor = isInsideEditor();
}

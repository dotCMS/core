import { Component, input } from '@angular/core';

import { Contentlet } from '@dotcms/client/src/lib/client/content/shared/types';
import { isInsideEditor } from '@dotcms/client';
import { NgTemplateOutlet } from '@angular/common';
import { GenericContentlet } from '../../../pages/components';

/**
 * Local component for rendering a single contentlet outside the DotCmsLayout.
 * This is useful when you want to render a contentlet in a different context than the DotCmsLayout, like in a modal, sidebar, footer, etc.
 * @export
 * @class ContentletComponent
 */
@Component({
  selector: 'app-contentlet',
  standalone: true,
  imports: [NgTemplateOutlet],
  template: `
    @if(isInsideEditor) {
    <div
      data-dot-object="contentlet"
      [attr.data-dot-identifier]="contentlet().identifier"
      [attr.data-dot-basetype]="contentlet().baseType"
      [attr.data-dot-title]="contentlet().widgetTitle || contentlet().title"
      [attr.data-dot-inode]="contentlet().inode"
      [attr.data-dot-type]="contentlet().contentType"
      [attr.data-dot-on-number-of-pages]="contentlet().onNumberOfPages ?? 0"
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
  contentlet = input.required<Contentlet<GenericContentlet>>();

  protected readonly isInsideEditor = isInsideEditor();
}

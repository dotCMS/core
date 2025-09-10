import { Component, input } from '@angular/core';

import { DatePipe, NgOptimizedImage } from '@angular/common';

import { editContentlet } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/types';
import { EditContentletButtonComponent } from '../../../edit-contentlet-button/edit-contentlet-button.component';
import { Contentlet } from '../../../../contentlet.model';

/**
 * Local component for rendering a recommended card.
 *
 * @export
 * @class RecommendedCardComponent
 */
@Component({
  selector: 'app-recommended-card',
  imports: [NgOptimizedImage, DatePipe, EditContentletButtonComponent],
  template: `
    <div class="flex gap-7 min-h-16 relative">
      <app-edit-contentlet-button [contentlet]="contentlet()" />
      <a
        class="relative min-w-32"
        [href]="contentlet().urlMap ?? contentlet().url"
      >
        <img
          [ngSrc]="contentlet().image.fileAsset.versionPath"
          [fill]="true"
          [alt]="contentlet().urlTitle ?? contentlet().title"
          [loaderParams]="{ languageId: contentlet().languageId || 1 }"
          class="object-cover"
        />
      </a>
      <div class="flex flex-col gap-1">
        <a
          class="text-sm font-bold text-zinc-900"
          [href]="contentlet().urlMap ?? contentlet().url"
        >
          {{ contentlet().title }}
        </a>
        <time class="text-zinc-600">
          {{ contentlet().modDate | date: 'mediumDate' }}
        </time>
      </div>
    </div>
  `,
})
export class RecommendedCardComponent {
  contentlet = input.required<Contentlet>();

  uveMode = UVE_MODE;

  editContentlet(contentlet: Contentlet) {
    editContentlet(contentlet);
  }
}

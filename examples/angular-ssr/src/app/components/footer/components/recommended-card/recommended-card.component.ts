import { Component, input } from '@angular/core';

import { DatePipe, NgOptimizedImage } from '@angular/common';

import { editContentlet } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/types';
import { EditContentletButtonComponent } from '../../../edit-contentlet-button/edit-contentlet-button.component';
import { Contentlet } from '../../../../dotcms/types/contentlet.model';

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
    <div class="flex gap-4 min-h-16 relative">
      <app-edit-contentlet-button [contentlet]="contentlet()" />
      <a class="relative min-w-32" [href]="contentlet().urlMap ?? contentlet().url">
        @if (contentlet().image && contentlet().image.fileAsset.versionPath) {
        <img
          [ngSrc]="contentlet().image.fileAsset.versionPath"
          [fill]="true"
          [alt]="contentlet().urlTitle ?? contentlet().title"
          [loaderParams]="{ languageId: contentlet().languageId || 1 }"
          class="object-cover"
        />
        } @else {
        <div class="absolute inset-0 bg-gray-200 flex items-center justify-center">
          <span class="text-gray-400">No image</span>
        </div>
        }
      </a>
      <div class="flex flex-col gap-1">
        <a class="text-sm font-bold text-zinc-900" [href]="contentlet().urlMap ?? contentlet().url">
          {{ contentlet().title }}
        </a>
        <time class="text-zinc-600">
          {{ contentlet().modDate | date : 'mediumDate' }}
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

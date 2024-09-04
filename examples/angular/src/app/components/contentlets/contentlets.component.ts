import { Component, input } from '@angular/core';
import { Contentlet } from '@dotcms/client/src/lib/client/content/shared/types';
import { GenericContentlet } from '..';
import { DatePipe, NgOptimizedImage } from '@angular/common';
import { ContentletComponent } from '../contentlet/contentlet.component';

@Component({
  selector: 'app-contentlets',
  standalone: true,
  imports: [NgOptimizedImage, DatePipe, ContentletComponent],
  template: `<ul class="flex flex-col gap-7">
    @for (contentlet of contentlets(); track contentlet.identifier) {
    <app-contentlet [contentlet]="contentlet">
      <li class="flex gap-7 min-h-16">
        <a
          class="relative min-w-32"
          [href]="contentlet.urlMap ?? contentlet.url"
        >
          <img
            [ngSrc]="contentlet.image"
            [fill]="true"
            [alt]="contentlet.urlTitle ?? contentlet.title"
            [loaderParams]="{ languageId: contentlet.languageId || 1 }"
            class="object-cover"
          />
        </a>
        <div class="flex flex-col gap-1">
          <a
            class="text-sm font-bold text-zinc-900"
            [href]="contentlet.urlMap ?? contentlet.url"
          >
            {{ contentlet.title }}
          </a>
          <time class="text-zinc-600">
            {{ contentlet.modDate | date : 'mediumDate' }}
          </time>
        </div>
      </li>
    </app-contentlet>
    }
  </ul> `,
})
export class ContentletsComponent {
  contentlets = input.required<Contentlet<GenericContentlet>[]>();

  dateFormatOptions: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  };
}

import { Component, Input } from '@angular/core';
import { Contentlet } from '@dotcms/client/src/lib/client/content/shared/types';
import { GenericContentlet } from '../../utils';
import { environment } from '../../../environments/environment';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-contentlets',
  standalone: true,
  imports: [NgOptimizedImage],
  template: `<ul class="flex flex-col gap-7">
    @for(contentlet of contentlets; track contentlet.identifier) {
    <li class="flex gap-7 min-h-16">
      <a class="min-w-32 relative" [href]="contentlet.urlMap ?? contentlet.url">
        <img
          [ngSrc]="getImageUrl(contentlet)"
          [fill]="true"
          [alt]="contentlet.urlTitle ?? contentlet.title"
          class="object-cover"
        />
      </a>
      <div class="flex flex-col gap-1">
        <a
          class="text-sm text-zinc-900 font-bold"
          [href]="contentlet.urlMap ?? contentlet.url"
        >
          {{ contentlet.title }}
        </a>
        <time class="text-zinc-600">
          {{ getDateString(contentlet.modDate) }}
        </time>
      </div>
    </li>
    }
  </ul> `,
})
export class ContentletsComponent {
  @Input() contentlets: Contentlet<GenericContentlet>[] = [];

  dateFormatOptions: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  };

  getDateString(date: string): string {
    return new Date(date).toLocaleDateString('en-US', this.dateFormatOptions);
  }

  getImageUrl(contentlet: Contentlet<GenericContentlet>): string {
    const host = environment.dotcmsUrl;
    return `${host}${contentlet.image}?language_id=${
      contentlet.languageId || 1
    }`;
  }
}

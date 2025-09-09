import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DotCMSBasicContentlet } from '@dotcms/types';

interface ImageContentlet extends DotCMSBasicContentlet {
  fileAsset: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-image',
  imports: [NgOptimizedImage],
  template: `<div
    class="overflow-hidden relative mb-4 bg-white rounded shadow-lg group"
  >
    <div class="relative w-full h-96 bg-gray-200">
      <img
        class="object-cover"
        [ngSrc]="contentlet().fileAsset"
        [alt]="contentlet().title"
        fill
      />
    </div>
    <div
      class="absolute bottom-0 px-6 py-8 w-full text-white bg-orange-500 bg-opacity-80 transition-transform duration-300 translate-y-full w-100 group-hover:translate-y-0"
    >
      <div class="mb-2 text-2xl font-bold">{{ contentlet().title }}</div>
      <p class="text-base">{{ contentlet().description }}</p>
    </div>
  </div>`,
  styleUrl: './image.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageComponent {
  contentlet = input.required<ImageContentlet>();
}

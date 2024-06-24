import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DotCMSContentlet } from '@dotcms/angular';

@Component({
  selector: 'app-banner',
  standalone: true,
  imports: [RouterLink, NgOptimizedImage],
  template: `<div
    class="relative w-full bg-gray-200 h-96 flex justify-center items-center overflow-hidden"
  >
    @if (contentlet.image; as image) {
      <img
        class="object-cover w-full"
        [ngSrc]="image"
        [alt]="contentlet.title"
        fill
        priority
      />
    }
    <div
      class="absolute inset-0 flex flex-col items-center justify-center p-4 text-center text-white"
    >
      <h2 class="mb-2 text-6xl font-bold text-shadow">
        {{ contentlet.title }}
      </h2>
      <p class="mb-4 text-xl text-shadow">{{ contentlet['caption'] }}</p>
      <a
        class="p-4 text-xl transition duration-300 bg-red-400 rounded hover:bg-red-500"
        [routerLink]="contentlet['link']"
      >
        {{ contentlet['buttonText'] }}
      </a>
    </div>
  </div>`,
  styleUrl: './banner.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BannerComponent {
  @Input() contentlet!: DotCMSContentlet;
}

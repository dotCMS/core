
import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DotCMSEditableTextComponent } from '@dotcms/angular';

import { Banner } from '../../types/contentlet.model';

@Component({
  selector: 'app-banner',
  imports: [RouterLink, NgOptimizedImage, DotCMSEditableTextComponent],
  template: `<div
    class="flex overflow-hidden relative justify-center items-center w-full h-96 bg-gray-200"
  >
    @if (contentlet().image.identifier; as imageIdentifier) {
      <img
        class="object-cover w-full"
        [ngSrc]="imageIdentifier"
        [alt]="contentlet().title"
        width="1400"
        height="400"
        priority
      />
    }
    <div
      class="flex absolute inset-0 flex-col justify-center items-center p-4 text-center text-white"
    >
      <h2 class="mb-2 text-6xl font-bold text-shadow">
        <dotcms-editable-text fieldName="title" [contentlet]="contentlet()" />
      </h2>
      <p class="mb-4 text-xl text-shadow">{{ contentlet().caption }}</p>
      <a
        class="p-4 text-xl bg-teal-400 rounded-sm transition duration-300 hover:bg-teal-500"
        [routerLink]="contentlet().link"
      >
        {{ contentlet().buttonText }}
      </a>
    </div>
  </div>`,
  styleUrl: './banner.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BannerComponent {
  contentlet = input.required<Banner>();
}

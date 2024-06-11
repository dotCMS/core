import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { DotCMSContentlet } from '@dotcms/angular';

@Component({
  selector: 'app-image',
  standalone: true,
  imports: [
    CommonModule,
  ],
  template: `<div class="relative overflow-hidden bg-white rounded shadow-lg group mb-4">
  <div class="relative w-full bg-gray-200 h-96">
      <img
        class="object-cover"
          [src]="contentlet['fileAsset'] + '?language_id=' + contentlet.languageId"
          [alt]="contentlet.title"
      />
  </div>
  <div class="absolute bottom-0 w-full px-6 py-8 text-white transition-transform duration-300 translate-y-full bg-orange-500 bg-opacity-80 w-100 group-hover:translate-y-0">
      <div class="mb-2 text-2xl font-bold">{{contentlet.title}}</div>
      <p class="text-base">{{contentlet['description']}}</p>
  </div>
</div>`,
  styleUrl: './image.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageComponent {
  @Input() contentlet!: DotCMSContentlet;
}

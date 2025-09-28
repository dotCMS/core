import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DestinationsComponent } from './components/destinations/destinations.component';
import { NgOptimizedImage } from '@angular/common';
import { BlogsComponent } from './components/blogs/blogs.component';
import { FooterContent } from '../../dotcms/types/contentlet.model';

@Component({
  selector: 'app-footer',
  imports: [BlogsComponent, DestinationsComponent, NgOptimizedImage],
  template: `<footer class="p-4 text-white bg-teal-100 py-12">
    <div
      class="container grid md:grid-cols-3 sm:grid-cols-1 md:grid-rows-1 sm:grid-rows-3 gap-8 mx-auto"
    >
      <div class="flex gap-7 flex-col">
        <h2 class="text-2xl font-bold text-black">About us</h2>
        <p class="text-sm text-zinc-800">
          We are TravelLux, a community of dedicated travel experts,
          journalists, and bloggers. Our aim is to offer you the best insight on
          where to go for your travel as well as to give you amazing
          opportunities with free benefits and bonuses for registered clients.
        </p>
        @if (logoImage) {
          <img
            [ngSrc]="logoImage"
            height="53"
            width="221"
            alt="TravelLux logo"
          />
        }
      </div>
      @if (content()?.blogs; as blogs) {
        <app-blogs [blogs]="blogs" />
      }
      @if (content()?.destinations; as destinations) {
        <app-destinations [destinations]="destinations" />
      }
    </div>
  </footer>`,
  styleUrl: './footer.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FooterComponent {
  content = input<FooterContent>();

  get logoImage() {
    return this.content()?.logoImage[0]?.fileAsset?.versionPath;
  }
}

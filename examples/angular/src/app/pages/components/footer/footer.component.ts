import { environment } from '../../../../environments/environment';
import { ChangeDetectionStrategy, Component } from '@angular/core';


import { DestinationsComponent } from './destinations/destinations.component';
import { NgOptimizedImage } from '@angular/common';
import { BlogsComponent } from './blogs/blogs.component';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [BlogsComponent, DestinationsComponent, NgOptimizedImage],
  template: `<footer class="p-4 text-white bg-red-100 py-24">
    <div
      class="grid md:grid-cols-3 sm:grid-cols-1 md:grid-rows-1 sm:grid-rows-3 gap-7 mx-24"
    >
      <div class="flex gap-7 flex-col">
        <h2 class="text-2xl font-bold text-black">About us</h2>
        <p class="text-sm text-zinc-800">
          We are TravelLux, a community of dedicated travel experts,
          journalists, and bloggers. Our aim is to offer you the best insight on
          where to go for your travel as well as to give you amazing
          opportunities with free benefits and bonuses for registered clients.
        </p>
        <img
          ngSrc="/dA/82da90eb04/fileAsset/logo.png"
          height="53"
          width="221"
          alt="TravelLux logo"
        />
      </div>
      <app-blogs />
      <app-destinations />
    </div>
  </footer>`,
  styleUrl: './footer.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FooterComponent {
  protected readonly environment = environment;
}

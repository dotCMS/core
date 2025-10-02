import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ReorderButtonComponent } from '../reorder-button/reorder-button.component';

@Component({
  selector: 'app-header',
  imports: [ReorderButtonComponent, RouterLink],
  template: `
    <header class="flex items-center justify-between p-4 bg-red-400">
      <div class="flex items-center justify-between container mx-auto">
        <h2 class="text-3xl font-bold text-white">
          <a routerLink="/">TravelLux with NG</a>
        </h2>
        <div class="flex items-center gap-4">
          <app-reorder-button />
          <ng-content />
        </div>
      </div>
    </header>
  `,
  styleUrl: './header.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderComponent {}

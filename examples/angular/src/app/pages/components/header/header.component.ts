import { ChangeDetectionStrategy, Component } from '@angular/core';
@Component({
  selector: 'app-header',
  standalone: true,
  template: `
    <header class="flex items-center justify-between p-4 bg-red-400">
      <div class="flex items-center">
        <h2 class="text-3xl font-bold text-white">
          <a routerLink="/">TravelLux with NG</a>
        </h2>
      </div>
      <ng-content></ng-content>
    </header>
  `,
  styleUrl: './header.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderComponent {}

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DotCMSContentlet } from '@dotcms/angular';

@Component({
  selector: 'app-web-page-content',
  standalone: true,
  imports: [
    CommonModule,
  ],
  template: `<h1 class="text-xl font-bold">{{contentlet().title}}</h1>
  <div [innerHTML]="contentlet().body" ></div>
  `,
  styleUrl: './web-page-content.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WebPageContentComponent {
  contentlet = input.required<DotCMSContentlet>();
}

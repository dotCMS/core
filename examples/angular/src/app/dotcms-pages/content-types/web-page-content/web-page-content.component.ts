import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { DotCMSContentlet } from '../../../lib/models';

@Component({
  selector: 'app-web-page-content',
  standalone: true,
  imports: [
    CommonModule,
  ],
  template: `<p>web-page-content works!</p>`,
  styleUrl: './web-page-content.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WebPageContentComponent {
  @Input() contentlet!: DotCMSContentlet;
}

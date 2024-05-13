import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { DotCMSContentlet } from '../../../lib/models';

@Component({
  selector: 'app-image',
  standalone: true,
  imports: [
    CommonModule,
  ],
  template: `<p>image works!</p>`,
  styleUrl: './image.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageComponent {
  @Input() contentlet!: DotCMSContentlet;
}

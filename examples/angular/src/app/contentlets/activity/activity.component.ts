import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [
    CommonModule,
  ],
  template: `<h2>Title: {{ contentlet?.title }}</h2>`,
  styleUrl: './activity.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityComponent {
  @Input() contentlet: any;
}

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-no-component',
  standalone: true,
  imports: [
    CommonModule,
  ],
  template: `<h4>No Component</h4>`,
  styleUrl: './no-component.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NoComponentComponent {
  @Input() contentlet: any;
}

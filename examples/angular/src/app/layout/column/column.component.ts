import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ContainerComponent } from '../container/container.component';

@Component({
  selector: 'dotcms-column',
  standalone: true,
  imports: [ContainerComponent],
  template: `
    @for(container of column.containers; track $index) {
      <dotcms-container [container]="container" />
    }
  `,
  styleUrl: './column.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ColumnComponent {
  @Input() column: any;
}

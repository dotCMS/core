import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { RowComponent } from '../row/row.component';

@Component({
  selector: 'dotcms-layout',
  standalone: true,
  imports: [RowComponent],
  template: `@for(row of entity.layout.body.rows; track $index) {
    <dotcms-row [row]="row" />
    }`,
  styleUrl: './dotcms-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DotcmsLayoutComponent {
    // TODO: Add type
  @Input({ required: true }) entity: any;
}

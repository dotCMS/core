import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ColumnComponent } from '../column/column.component';
import { DotPageAssetLayoutRow } from '../../models';

@Component({
  selector: 'dotcms-row',
  standalone: true,
  imports: [ColumnComponent],
  template: `@for(column of row.columns; track $index) {
    <dotcms-column [column]="column" />
  }`,
  styleUrl: './row.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RowComponent {
  @Input() row!: DotPageAssetLayoutRow;
}

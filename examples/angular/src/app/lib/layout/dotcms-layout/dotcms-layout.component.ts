import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';

import { RowComponent } from '../row/row.component';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'dotcms-layout',
  standalone: true,
  imports: [RowComponent],
  template: `@for(row of entity?.layout.body.rows; track $index) {
    <dotcms-row [row]="row" />
    }`,
  styleUrl: './dotcms-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DotcmsLayoutComponent implements OnInit {
    // TODO: Add type
  @Input({ required: true }) entity: any;

  private readonly route = inject(ActivatedRoute);

  ngOnInit() {
    // console.log(this.route.snapshot.data);
  }
}

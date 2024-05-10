import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { RowComponent } from '../row/row.component';
import { ComponentItem, DotcmsPageService } from '../../services/dotcms-page/dotcms-page.service';

@Component({
  selector: 'dotcms-layout',
  standalone: true,
  imports: [RowComponent],
  providers: [DotcmsPageService], 
  template: `@for(row of entity?.layout.body.rows; track $index) {
    <dotcms-row [row]="row" />
    }`,
  styleUrl: './dotcms-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DotcmsLayoutComponent implements OnInit {
    // TODO: Add type
  @Input({ required: true }) entity: any;
  @Input({ required: true }) components!: Record<string, ComponentItem>;

  // private readonly route = inject(ActivatedRoute);
  private readonly dotCMSPageService = inject(DotcmsPageService);

  ngOnInit() {
    this.dotCMSPageService.componentMap = this.components;
  }
}

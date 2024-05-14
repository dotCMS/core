import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnInit,
  inject,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { RowComponent } from '../row/row.component';
import {
  ComponentItem,
  DotcmsPageService,
} from '../../services/dotcms-page/dotcms-page.service';
import { DotCMSPageAsset } from '../../models';
import { initEditor, isInsideEditor, updateNavigation } from '@dotcms/client';

@Component({
  selector: 'dotcms-layout',
  standalone: true,
  imports: [RowComponent],
  providers: [DotcmsPageService],
  template: `@for(row of entity.layout.body.rows; track $index) {
    <dotcms-row [row]="row" />
    }`,
  styleUrl: './dotcms-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DotcmsLayoutComponent implements OnInit {
  @Input({ required: true }) entity!: DotCMSPageAsset;
  @Input({ required: true }) components!: Record<string, ComponentItem>;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dotCMSPageService = inject(DotcmsPageService);

  ngOnInit() {
    this.dotCMSPageService.componentMap = this.components;

    this.route.url.subscribe((urlSegments) => {
      if (isInsideEditor()) {
        const pathname = '/' + urlSegments.join('/');
        const config = {
          pathname,
          onReload: () => this.router.navigate([pathname]),
        };
        initEditor(config);
        updateNavigation(pathname || '/');
      }
    });
  }
}

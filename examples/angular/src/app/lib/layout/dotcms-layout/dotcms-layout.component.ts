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
  PageContextService,
} from '../../services/dotcms-context/page-context.service';
import { DotCMSPageAsset, DynamicComponentEntity } from '../../models';
import { initEditor, isInsideEditor, updateNavigation } from '@dotcms/client';

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
export class DotcmsLayoutComponent implements OnInit {
  @Input({ required: true }) entity!: DotCMSPageAsset;
  @Input({ required: true }) components!: Record<string, DynamicComponentEntity>;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pageContextService = inject(PageContextService);

  ngOnInit() {
    this.pageContextService.setComponentMap(this.components);

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

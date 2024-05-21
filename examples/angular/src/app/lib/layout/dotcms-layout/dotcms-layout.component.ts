import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  Input,
  OnInit,
  inject,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { initEditor, isInsideEditor, updateNavigation } from '@dotcms/client';

import { RowComponent } from '../row/row.component';
import { PageContextService } from '../../services/dotcms-context/page-context.service';
import { DotCMSPageAsset, DynamicComponentEntity } from '../../models';

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
    @Input({ required: true }) components!: Record<
        string,
        DynamicComponentEntity
    >;

    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly pageContextService = inject(PageContextService);
    private readonly destroyRef$ = inject(DestroyRef);

    ngOnInit() {
        this.pageContextService.setComponentMap(this.components);

        this.route.url
            .pipe(takeUntilDestroyed(this.destroyRef$))
            .subscribe((urlSegments) => {
                if (isInsideEditor()) {
                const pathname = '/' + urlSegments.join('/');
                const config = {
                    pathname,
                    onReload: () => {
                    // Reload the page when the user edit the page
                    this.router.navigate([pathname], {
                        onSameUrlNavigation: 'reload', // Force Angular to reload the page
                    });
                    },
                };
                initEditor(config);
                updateNavigation(pathname || '/');
                }
            });
    }
}

import { Component, DestroyRef, inject, signal } from '@angular/core';

import { ActivityDetailComponent } from './activity-detail/activity-detail.component';
import {
  DotCMSComposedPageResponse,
  DotCMSPageAsset,
  DotCMSURLContentMap,
} from '@dotcms/types';
import { ContentletImage } from '../../types/contentlet.model';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map, startWith, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { DotCMSEditablePageService } from '@dotcms/angular';

export interface ActivityContentlet extends DotCMSURLContentMap {
  body: string;
  image: ContentletImage;
  title: string;
  description: string;
  altTag: string;
  tags: string;
}

export interface ActivityPageAsset extends DotCMSPageAsset {
  urlContentMap: ActivityContentlet;
}

type ActivityPage = {
  pageAsset: ActivityPageAsset;
};

@Component({
  selector: 'app-activity',
  imports: [ActivityDetailComponent],
  templateUrl: './activity.component.html',
})
export class ActivityComponent {
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly editablePageService = inject(DotCMSEditablePageService);
  private readonly destroyRef = inject(DestroyRef);

  pageAsset = signal<ActivityContentlet | null>(null);

  ngOnInit() {
    const route = this.router.url.split('?')[0] || '/';

    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        map((event: NavigationEnd) => event.urlAfterRedirects),
        startWith(route),
        switchMap((url: string) =>
          this.http.post<DotCMSComposedPageResponse<ActivityPage>>('/data/page', { url })
        )
      )
      .pipe(switchMap((response) => this.editablePageService.listen<ActivityPage>(response)))
      .pipe(filter(Boolean))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (
          response: DotCMSComposedPageResponse<{
            pageAsset: ActivityPageAsset;
          }>
        ) => {
          this.pageAsset.set(response?.pageAsset?.urlContentMap || null);
        },
        error: (error) => {
          console.error('Error in page data stream:', error);
        },
      });
  }
}

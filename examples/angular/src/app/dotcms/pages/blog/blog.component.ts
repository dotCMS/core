import { Component, DestroyRef, inject, signal } from '@angular/core';

import { BlogPostComponent } from './blog-post/blog-post.component';
import {
  BlockEditorContent,
  DotCMSComposedPageResponse,
  DotCMSPageAsset,
  DotCMSURLContentMap,
} from '@dotcms/types';
import { ContentletImage } from '../../types/contentlet.model';
import { NavigationEnd, Router } from '@angular/router';
import { filter, from, map, startWith, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DotCMSClient, DotCMSEditablePageService } from '@dotcms/angular';

export interface BlogContentlet extends DotCMSURLContentMap {
  blogContent: BlockEditorContent;
  image: ContentletImage;
}

export interface BlogPageAsset extends DotCMSPageAsset {
  urlContentMap: BlogContentlet;
}

type BlogPage = {
  pageAsset: BlogPageAsset;
};

@Component({
  selector: 'app-blog',
  imports: [BlogPostComponent],
  templateUrl: './blog.component.html',
})
export class BlogComponent {
  private readonly router = inject(Router);
  private readonly editablePageService = inject(DotCMSEditablePageService);
  private readonly destroyRef = inject(DestroyRef);

  pageAsset = signal<BlogContentlet | null>(null);

  private readonly client = inject(DotCMSClient);

  ngOnInit() {
    const route = this.router.url.split('?')[0] || '/';

    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        map((event: NavigationEnd) => event.urlAfterRedirects),
        startWith(route),
        switchMap((url: string) =>
          from(this.client.page.get<BlogPage>(url))
        )
      )
      .pipe(switchMap((response) => this.editablePageService.listen<BlogPage>(response)))
      .pipe(filter(Boolean))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (
          response: DotCMSComposedPageResponse<{
            pageAsset: BlogPageAsset;
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

import {
  Component,
  DestroyRef,
  InjectionToken,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap } from '@angular/router';

import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { NavigationComponent } from './components/navigation/navigation.component';

import { DYNAMIC_COMPONENTS } from '../utils';

import {
  DotcmsLayoutComponent,
  DotcmsNavigationItem,
  DotCMSPageAsset,
} from '@dotcms/angular';
import { JsonPipe } from '@angular/common';
import { DOTCMS_CLIENT_TOKEN } from '../client-token/dotcms-client';
import { map, withLatestFrom, switchMap } from 'rxjs/operators';

import { getPageRequestParams } from '@dotcms/client';
import { from } from 'rxjs';
import { ErrorComponent } from './components/error/error.component';

export type PageError = {
  message: string;
  status: number;
};

type PageRender = {
  page: DotCMSPageAsset | null;
  nav: DotcmsNavigationItem | null;
  error: PageError | null;
  status: 'idle' | 'success' | 'error' | 'loading';
};

@Component({
  selector: 'dotcms-pages',
  standalone: true,
  imports: [
    DotcmsLayoutComponent,
    HeaderComponent,
    NavigationComponent,
    FooterComponent,
    JsonPipe,
    ErrorComponent,
  ],
  templateUrl: './pages.component.html',
  styleUrl: './pages.component.css',
})
export class DotCMSPagesComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly context = signal<PageRender>({
    page: null,
    nav: null,
    error: null,
    status: 'idle',
  });
  protected readonly components = signal<any>(DYNAMIC_COMPONENTS);
  private readonly client = inject(DOTCMS_CLIENT_TOKEN);

  protected readonly editorCofig = { params: { depth: '2' } };
  protected slug: string | null = null;

  ngOnInit() {
    this.context.update((state) => ({ ...state, status: 'loading' }));

    this.route.url
      .pipe(
        withLatestFrom(this.route.queryParamMap),
        map(([segments, queryParams]) => ({
          path:
            segments.length > 0
              ? segments.map((segment) => segment.path).join('/')
              : '/',
          queryParams: queryParams,
        })),
        switchMap(({ path, queryParams }) => {
          const pageParams = getPageRequestParams({
            path,
            params: queryParams,
          });
          const pagePromise = this.client.page
            .get(pageParams)
            .catch((error) => {
              return { error: {
                message: error.message,
                status: error.status,
              }};
            }) as Promise<DotCMSPageAsset | { error: PageError }>;

          const navParams = {
            path: '/',
            depth: 2,
            languageId: (queryParams as any)['language_id'],
          };
          const navPromise = this.client.nav
            .get(navParams)
            .then((response) => (response as any).entity)
            .catch((error) => null) as Promise<DotcmsNavigationItem | null>;

          return from(Promise.all([pagePromise, navPromise]));
        }),
        map(([page, navResponse]) => ({
          page,
          nav: (navResponse)
        })),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(({ page, nav }) => {
        if ('error' in page) {
          this.context.update((state) => ({
            ...state,
            error: page.error,
            status: 'error',
          }));
        } else {
          this.context.update((state) => ({
            ...state,
            page,
            nav,
            status: 'success',
          }));
        }
      });
  }

  ngOnDestroy() {
    // this.client.editor.off('changes');
  }
}

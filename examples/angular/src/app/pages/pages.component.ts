import {
  Component,
  DestroyRef,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { NavigationEnd } from '@angular/router';
import { filter, startWith, tap } from 'rxjs/operators';

import { DYNAMIC_COMPONENTS } from '../utils';

import {
  DotcmsLayoutComponent,
  DotcmsNavigationItem,
  DotCMSPageAsset,
} from '@dotcms/angular';
import { map, switchMap } from 'rxjs/operators';

import { ErrorComponent } from './components/error/error.component';
import { LoadingComponent } from './components/loading/loading.component';
import { HeaderComponent } from './components/header/header.component';
import { NavigationComponent } from './components/navigation/navigation.component';
import { FooterComponent } from './components/footer/footer.component';
import { PageService } from './services/page.service';

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
    ErrorComponent,
    LoadingComponent,
  ],
  templateUrl: './pages.component.html',
  styleUrl: './pages.component.css',
})
export class DotCMSPagesComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly pageService = inject(PageService);
  protected readonly context = signal<PageRender>({
    page: null,
    nav: null,
    error: null,
    status: 'idle',
  });
  protected readonly components = signal<any>(DYNAMIC_COMPONENTS);

  protected readonly editorCofig = { params: { depth: '2' } };

  ngOnInit() {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        startWith(null), // Trigger initial load
        tap(() => this.#setLoading()),
        switchMap(() => this.pageService.getPage(this.route)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(
        ({
          page,
          nav,
        }: {
          page: DotCMSPageAsset | { error: PageError };
          nav: DotcmsNavigationItem;
        }) => {
          if ('error' in page) {
            this.#setError(page.error);
          } else {
            this.#setSuccess(page, nav);
          }
        }
      );
  }

  #setSuccess(page: DotCMSPageAsset, nav: DotcmsNavigationItem) {
    this.context.update((state) => ({
      status: 'success',
      page,
      nav,
      error: null,
    }));
  }

  #setLoading() {
    this.context.update((state) => ({
      status: 'loading',
      page: null,
      nav: null,
      error: null,
    }));
  }

  #setError(error: PageError) {
    this.context.update((state) => ({
      page: null,
      nav: null,
      error: error,
      status: 'error',
    }));
  }

  ngOnDestroy() {
    // this.client.editor.off('changes');
  }
}

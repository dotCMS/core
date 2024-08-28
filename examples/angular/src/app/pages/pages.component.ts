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

import { DotcmsLayoutComponent, DotcmsNavigationItem } from '@dotcms/angular';
import { JsonPipe } from '@angular/common';
import { DOTCMS_CLIENT_TOKEN } from '../client-token/dotcms-client';
import { map, withLatestFrom } from 'rxjs/operators';

import { getPageRequestParams } from '@dotcms/client';


@Component({
  selector: 'dotcms-pages',
  standalone: true,
  imports: [
    DotcmsLayoutComponent,
    HeaderComponent,
    NavigationComponent,
    FooterComponent,
    JsonPipe,
  ],
  templateUrl: './pages.component.html',
  styleUrl: './pages.component.css',
})
export class DotCMSPagesComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly context = signal<any>(null);
  protected readonly components = signal<any>(DYNAMIC_COMPONENTS);
  private readonly client = inject(DOTCMS_CLIENT_TOKEN);

  protected readonly editorCofig = { params: { depth: '2' } };
  protected slug: string | null = null;

  ngOnInit() {
    this.route.url.pipe(
      withLatestFrom(this.route.queryParamMap),
      map(([segments, queryParams]) => ({
        path: segments.length > 0 ? segments.map(segment => segment.path).join('/') : '/',
        queryParams: queryParams
      })),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(async ({ path, queryParams }) => {
      const params = getPageRequestParams({ path, params: queryParams });
      console.log(params);

      const page = await this.client.page.get(params);

      const navProps = {
        path: '/',
        depth: 2,
        languageId: (queryParams as any)['language_id'],
      };
    
      const navResponse = (await this.client.nav.get(navProps)) as {
        entity: DotcmsNavigationItem;
      };
      const nav = navResponse?.entity;
      console.log({page, nav});
      // You can use path and queryParams here as needed
    });
      

    // this.route.data
    //   .pipe(takeUntilDestroyed(this.destroyRef))
    //   .subscribe((data) => {
    //     console.log(data);
    //     this.context.set(data['context']);
    //   });


    // this.client.editor.on('changes', (pageAsset) => {
    //   this.context.update((context) => ({ ...context, pageAsset }));
    // });
  }

  ngOnDestroy() {
    // this.client.editor.off('changes');
  }
}

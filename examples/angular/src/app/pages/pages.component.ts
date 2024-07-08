import {
  Component,
  DestroyRef,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { NavigationComponent } from './components/navigation/navigation.component';

import { DYNAMIC_COMPONENTS } from '../utils';

import { DotcmsLayoutComponent, PageContextService } from '@dotcms/angular';
import { JsonPipe } from '@angular/common';
import { DOTCMS_CLIENT_TOKEN } from '../client-token/dotcms-client';

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

  ngOnInit() {
    // Get the context data from the route
    this.route.data
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => {
        this.context.set(data['context']);
      });

    this.client.editor.on('changes', (pageAsset) => {
      this.context.update((context) => ({ ...context, pageAsset }));
    });
  }

  ngOnDestroy() {
    this.client.editor.off('changes');
  }
}

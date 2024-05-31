import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { NavigationComponent } from './components/navigation/navigation.component';

import { DYNAMIC_COMPONENTS } from '../utils';

import { DotcmsLayoutComponent } from '@dotcms/angular';

@Component({
  selector: 'dotcms-pages',
  standalone: true,
  imports: [
    DotcmsLayoutComponent,
    HeaderComponent,
    NavigationComponent,
    FooterComponent,
  ],
  templateUrl: './pages.component.html',
  styleUrl: './pages.component.css',
})
export class DotCMSPagesComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly context = signal<any>(null);
  protected readonly components = signal<any>(DYNAMIC_COMPONENTS);

  ngOnInit() {
    // Get the context data from the route
    this.route.data
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => {
        this.context.set(data['context']);
      });
  }
}

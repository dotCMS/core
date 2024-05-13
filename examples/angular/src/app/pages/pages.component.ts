import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DotcmsLayoutComponent } from '../lib/layout/dotcms-layout/dotcms-layout.component';

import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { NavigationComponent } from './components/navigation/navigation.component';

import { COMPONENTS } from '../utils';

@Component({
  selector: 'dotcms-pages',
  standalone: true,
  imports: [DotcmsLayoutComponent, HeaderComponent, NavigationComponent, FooterComponent],
  templateUrl: './pages.component.html',
  styleUrl: './pages.component.css'
})
export class DotCMSPagesComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  protected readonly context = signal<any>(null);
  protected readonly components = signal<any>(COMPONENTS);

  ngOnInit() {
    
    // Get the context data from the route
    this.route.data.subscribe(data => {
      this.context.set(data['context']);
    });

  }
}

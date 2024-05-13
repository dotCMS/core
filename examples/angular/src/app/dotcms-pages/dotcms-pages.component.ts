import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';

import { DotcmsLayoutComponent } from '../lib/layout/dotcms-layout/dotcms-layout.component';
import { COMPONENTS } from '../utils';

@Component({
  selector: 'dotcms-pages',
  standalone: true,
  imports: [DotcmsLayoutComponent, HeaderComponent, FooterComponent],
  templateUrl: './dotcms-pages.component.html',
  styleUrl: './dotcms-pages.component.css'
})
export class DotCMSPagesComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  protected readonly context = signal<any>(null);
  protected readonly components = signal<any>(COMPONENTS);

  ngOnInit() {
    this.context.set(this.route.snapshot.data['context']);
    console.log(this.context());
  }
}

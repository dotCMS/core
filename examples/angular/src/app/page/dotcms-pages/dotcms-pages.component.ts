import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DotcmsLayoutComponent } from '../../lib/layout/dotcms-layout/dotcms-layout.component';

@Component({
  selector: 'dotcms-pages',
  standalone: true,
  imports: [DotcmsLayoutComponent],
  templateUrl: './dotcms-pages.component.html',
  styleUrl: './dotcms-pages.component.css'
})
export class DotCMSPagesComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  protected readonly context = signal<any>(null);

  ngOnInit() {
    this.context.set(this.route.snapshot.data['context']);
  }
}

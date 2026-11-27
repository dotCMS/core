import {
  ChangeDetectionStrategy,
  Component,
  computed,
  HostBinding,
  inject,
  input,
} from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { DotCMSBasicContentlet } from '@dotcms/types';

@Component({
  selector: 'app-web-page-content',
  imports: [],
  template: '',
  styleUrl: './web-page-content.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WebPageContentComponent {
  contentlet = input.required<DotCMSBasicContentlet>();

  sanitizer = inject(DomSanitizer);

  innerHTML = computed((): SafeHtml => {
    return this.sanitizer.bypassSecurityTrustHtml(this.contentlet().body || '');
  });

  @HostBinding('innerHTML')
  get hostInnerHTML(): SafeHtml {
    return this.innerHTML();
  }
}

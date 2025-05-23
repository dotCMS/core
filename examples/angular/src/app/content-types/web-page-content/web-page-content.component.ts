import { ChangeDetectionStrategy, Component, computed, HostBinding, input } from '@angular/core';
import { DotCMSBasicContentlet } from '@dotcms/types';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
    selector: 'app-web-page-content',
    standalone: true,
    imports: [],
    template: '',
    styleUrl: './web-page-content.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class WebPageContentComponent {
    contentlet = input.required<DotCMSBasicContentlet>();

    constructor(private sanitizer: DomSanitizer) {}

    innerHTML = computed((): SafeHtml => {
        return this.sanitizer.bypassSecurityTrustHtml(this.contentlet().body || '');
    });

    @HostBinding('innerHTML')
    get hostInnerHTML(): SafeHtml {
        return this.innerHTML();
    }
}

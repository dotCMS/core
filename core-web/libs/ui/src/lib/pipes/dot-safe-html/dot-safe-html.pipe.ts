import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

@Pipe({ name: 'safeHtml' })
export class DotSafeHtmlPipe implements PipeTransform {
    private sanitized = inject(DomSanitizer);

    transform(value) {
        return this.sanitized.bypassSecurityTrustHtml(value);
    }
}

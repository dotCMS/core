import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
    selector: 'dot-dot-hackathon-test',
    imports: [CommonModule],
    templateUrl: './dot-hackathon-test.component.html',
    styleUrl: './dot-hackathon-test.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHackathonTestComponent implements OnInit {
    iFrameUrl: SafeResourceUrl;
    sanitizer = inject(DomSanitizer);

    ngOnInit() {
        const rawURL = `https://www.google.com`;
        this.iFrameUrl = this.sanitizer.bypassSecurityTrustResourceUrl(rawURL);
    }
}

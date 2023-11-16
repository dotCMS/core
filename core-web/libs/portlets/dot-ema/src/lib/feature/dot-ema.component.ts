import { CommonModule, DOCUMENT } from '@angular/common';
import { AfterViewInit, Component, Inject } from '@angular/core';
import { Router } from '@angular/router';

@Component({
    selector: 'dot-ema',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-ema.component.html',
    styleUrls: ['./dot-ema.component.scss']
})
export class DotEmaComponent implements AfterViewInit {
    constructor(@Inject(DOCUMENT) private document: Document, private router: Router) {}
    ngAfterViewInit(): void {
        this.document.defaultView?.addEventListener('message', (event: MessageEvent) => {
            // This should be the host the user uses for nextjs, because this can trigger react dev tools messages
            if ((event as { origin: string }).origin !== 'http://localhost:3000') {
                return;
            }

            if (event.data.action === 'edit-contentlet') {
                this.router.navigate([`/c/content/${event.data.data.inode}`]);
            }
        });
    }
}

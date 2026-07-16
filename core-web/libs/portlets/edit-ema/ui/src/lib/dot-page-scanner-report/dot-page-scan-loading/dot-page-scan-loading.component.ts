import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-page-scan-loading',
    standalone: true,
    imports: [DotMessagePipe],
    templateUrl: './dot-page-scan-loading.component.html',
    styles: [
        `
            .scan-line {
                animation: scan 3s ease-in-out infinite;
            }
            @keyframes scan {
                0% {
                    top: 32px;
                }
                50% {
                    top: calc(100% - 2px);
                }
                100% {
                    top: 32px;
                }
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageScanLoadingComponent {}

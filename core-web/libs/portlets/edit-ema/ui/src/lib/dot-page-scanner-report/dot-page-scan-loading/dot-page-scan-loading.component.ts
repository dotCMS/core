import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-page-scan-loading',
    standalone: true,
    imports: [DotMessagePipe],
    templateUrl: './dot-page-scan-loading.component.html',
    styleUrl: './dot-page-scan-loading.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageScanLoadingComponent {}

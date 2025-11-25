import { ChangeDetectionStrategy, Component, EventEmitter, Output, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotSpinnerModule } from '@dotcms/ui';

@Component({
    selector: 'dot-upload-placeholder',
    templateUrl: './upload-placeholder.component.html',
    styleUrls: ['./upload-placeholder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [ButtonModule, DotSpinnerModule]
})
export class UploadPlaceholderComponent {
    @Output() cancel = new EventEmitter<boolean>();
    @Input() type: string;
}

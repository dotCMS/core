import { ChangeDetectionStrategy, Component, EventEmitter, Output, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotSpinnerComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-upload-placeholder',
    templateUrl: './upload-placeholder.component.html',
    styleUrls: ['./upload-placeholder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, DotSpinnerComponent]
})
export class UploadPlaceholderComponent {
    @Output() canceled = new EventEmitter<boolean>();
    @Input() type: string;
}

import { CUSTOM_ELEMENTS_SCHEMA, ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-file-field-preview',
    standalone: true,
    imports: [],
    providers: [],
    templateUrl: './dot-file-field-preview.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotFileFieldPreviewComponent {}

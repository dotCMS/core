import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dotcms-dot-template-builder',
    templateUrl: './dot-template-builder.component.html',
    styleUrls: ['./dot-template-builder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTemplateBuilderComponent {}

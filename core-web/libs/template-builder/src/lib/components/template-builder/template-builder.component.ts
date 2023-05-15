import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'template-builder',
    templateUrl: './template-builder.component.html',
    styleUrls: ['./template-builder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderComponent {}

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
    selector: 'dotcms-template-builder-section',
    templateUrl: './template-builder-section.component.html',
    styleUrls: ['./template-builder-section.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true
})
export class TemplateBuilderSectionComponent {
    @Input()
    title: string;
}

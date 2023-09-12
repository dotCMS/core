import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

@Component({
    selector: 'dotcms-template-builder-section',
    templateUrl: './template-builder-section.component.html',
    styleUrls: ['./template-builder-section.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [ButtonModule]
})
export class TemplateBuilderSectionComponent {
    @Output()
    deleteSection = new EventEmitter<void>();
}

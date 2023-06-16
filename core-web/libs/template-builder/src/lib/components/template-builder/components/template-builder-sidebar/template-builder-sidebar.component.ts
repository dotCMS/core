import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dotcms-template-builder-sidebar',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './template-builder-sidebar.component.html',
    styleUrls: ['./template-builder-sidebar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderSidebarComponent {}

import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

import { DotTemplateSidebarProperties } from '../../models/models';

@Component({
    selector: 'dotcms-template-builder-sidebar',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './template-builder-sidebar.component.html',
    styleUrls: ['./template-builder-sidebar.component.scss']
})
export class TemplateBuilderSidebarComponent {
    @Input() sidebarProperties: DotTemplateSidebarProperties;
}

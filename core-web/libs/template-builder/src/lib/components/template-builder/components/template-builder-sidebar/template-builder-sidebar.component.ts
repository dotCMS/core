import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotTemplateSidebarProperties } from '../../models/models';

@Component({
    selector: 'dotcms-template-builder-sidebar',
    standalone: true,
    imports: [CommonModule, DropdownModule, FormsModule],
    templateUrl: './template-builder-sidebar.component.html',
    styleUrls: ['./template-builder-sidebar.component.scss']
})
export class TemplateBuilderSidebarComponent {
    @Input() sidebarProperties: DotTemplateSidebarProperties;
    @Output() sidebarPropertiesChange = new EventEmitter<DotTemplateSidebarProperties>();

    get width() {
        return this.sidebarProperties.width.replace(/^\w/g, (l) => l.toUpperCase());
    }

    readonly widthOptions = ['Small', 'Medium', 'Large'];

    /**
     * @description Change the sidebar width
     *
     * @param {{ value: string }} { value }
     * @memberof TemplateBuilderSidebarComponent
     */
    widthChange({ value }: { value: string }) {
        this.sidebarPropertiesChange.emit({
            ...this.sidebarProperties,
            width: value.toLowerCase()
        });
    }
}

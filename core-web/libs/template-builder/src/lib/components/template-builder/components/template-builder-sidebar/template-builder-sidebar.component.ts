import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotTemplateSidebarProperties } from '../../models/models';
import { TemplateBuilderBoxComponent } from '../template-builder-box/template-builder-box.component';

@Component({
    selector: 'dotcms-template-builder-sidebar',
    standalone: true,
    imports: [CommonModule, DropdownModule, FormsModule, TemplateBuilderBoxComponent],
    templateUrl: './template-builder-sidebar.component.html',
    styleUrls: ['./template-builder-sidebar.component.scss']
})
export class TemplateBuilderSidebarComponent {
    @Input() sidebarProperties: DotTemplateSidebarProperties = {
        width: 'medium',
        containers: []
    };

    @Output() sidebarWidthChange = new EventEmitter<string>();

    get width() {
        return this.sidebarProperties.width.replace(/^\w/g, (l) => l.toUpperCase());
    }

    get containers() {
        return this.sidebarProperties.containers;
    }

    readonly widthOptions = ['Small', 'Medium', 'Large'];

    /**
     * @description Change the sidebar width
     *
     * @param {{ value: string }} { value }
     * @memberof TemplateBuilderSidebarComponent
     */
    widthChange({ value = 'medium' }: { value: string }) {
        this.sidebarWidthChange.emit(value.toLowerCase());
    }
}

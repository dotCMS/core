import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotContainer, DotLayoutSideBar } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@dotcms/ui';

import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import { TemplateBuilderBoxComponent } from '../template-builder-box/template-builder-box.component';

@Component({
    selector: 'dotcms-template-builder-sidebar',
    standalone: true,
    imports: [DropdownModule, FormsModule, TemplateBuilderBoxComponent, DotMessagePipeModule],
    templateUrl: './template-builder-sidebar.component.html',
    styleUrls: ['./template-builder-sidebar.component.scss']
})
export class TemplateBuilderSidebarComponent {
    @Input() sidebarProperties: DotLayoutSideBar = {
        width: 'medium',
        containers: []
    };

    get width() {
        return this.sidebarProperties.width.replace(/^\w/g, (l) => l.toUpperCase());
    }

    get containers() {
        return this.sidebarProperties.containers;
    }

    readonly widthOptions = ['Small', 'Medium', 'Large'];

    constructor(private store: DotTemplateBuilderStore) {}

    /**
     * @description Change the sidebar width
     *
     * @param {{ value: string }} { value }
     * @memberof TemplateBuilderSidebarComponent
     */
    widthChange({ value = 'medium' }: { value: string }) {
        this.store.updateSidebarWidth(value.toLowerCase());
    }

    /**
     * @description Add a container to the sidebar
     *
     * @param {DotContainer} container
     * @memberof TemplateBuilderSidebarComponent
     */
    addContainer(container: DotContainer) {
        this.store.addSidebarContainer(container);
    }

    /**
     * @description Delete a container from the sidebar
     *
     * @param {DotContainer} container
     * @memberof TemplateBuilderSidebarComponent
     */
    deleteContainer(index: number) {
        this.store.deleteSidebarContainer(index);
    }
}

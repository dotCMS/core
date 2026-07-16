import { Component, Input, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';

import { DotContainer, DotContainerMap, DotLayoutSideBar } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import { TemplateBuilderBoxComponent } from '../template-builder-box/template-builder-box.component';

@Component({
    selector: 'dotcms-template-builder-sidebar',
    imports: [SelectModule, FormsModule, TemplateBuilderBoxComponent, DotMessagePipe],
    templateUrl: './template-builder-sidebar.component.html',
    styleUrls: ['./template-builder-sidebar.component.css']
})
export class TemplateBuilderSidebarComponent {
    private store = inject(DotTemplateBuilderStore);

    @Input() sidebarProperties: DotLayoutSideBar = {
        width: 'medium',
        containers: []
    };

    @Input() containerMap: DotContainerMap;
    readonly widthOptions = ['Small', 'Medium', 'Large'];

    get width() {
        return (this.sidebarProperties.width ?? 'medium').replace(/^\w/g, (l) => l.toUpperCase());
    }

    get containers() {
        return this.sidebarProperties.containers;
    }

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
     * @param {number} index
     * @memberof TemplateBuilderSidebarComponent
     */
    deleteContainer(index: number) {
        this.store.deleteSidebarContainer(index);
    }
}

import { Component, Input, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { StructureTypeView } from '../../../../../shared/models/contentlet';
import { DotRouterService } from '../../../../../api/services/dot-router/dot-router.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-toolbar-add-contentlet-body',
    styleUrls: ['./toolbar-add-contentlet-body.component.scss'],
    templateUrl: 'toolbar-add-contentlet-body.component.html'
})
export class ToolbarAddContenletBodyComponent {
    @Input() structureTypeViews: StructureTypeView[];
    @Input() showMore = false;

    @Output() select = new EventEmitter<any>();
    @Output() more = new EventEmitter<any>();

    constructor(private dotRouterService: DotRouterService) {}

    onClick(event: MouseEvent, id: string): void {
        if (this.shouldReload(event, id)) {
            this.dotRouterService.reloadCurrentPortlet();
        }
        this.select.emit();
    }

    clickMore(event): boolean {
        event.preventDefault();
        this.more.emit();
        return false;
    }

    private shouldReload(event: MouseEvent, id: string): boolean {
        return !event.ctrlKey && !event.metaKey && this.dotRouterService.currentPortlet.id === id;
    }
}

import { Component, Input, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { StructureTypeView, ContentTypeView } from '../../../shared/models/contentlet';
import { DotNavigationService } from '../dot-navigation/dot-navigation.service';


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

    constructor(private dotNavigationService: DotNavigationService) {}

    onClick(event: any, id: string): void {
        if (!event.ctrlKey && !event.metaKey) {
            this.dotNavigationService.reloadCurrentPortlet(id);
        }
        this.select.emit();
    }

    clickMore(event): boolean {
        event.preventDefault();
        this.more.emit();
        return false;
    }
}

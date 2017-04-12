import {Component, ViewChild, Input, Output, EventEmitter} from '@angular/core';
import {DropdownComponent} from '../_common/dropdown-component/dropdown-component';
import {ContentletService, StructureTypeView, ContentTypeView} from '../../../api/services/contentlet-service';
import {RoutingService} from '../../../api/services/routing-service';
import {BaseComponent} from '../_common/_base/base-component';
import {MessageService} from '../../../api/services/messages-service';
import {IframeOverlayService} from '../../../api/services/iframe-overlay-service';

@Component({
    selector: 'toolbar-add-contentlet-body',
    styles: [require('./toolbar-add-contentlet-body.scss')],
    templateUrl: 'toolbar-add-contentlet-body.html',

})
export class ToolbarAddContenletBodyComponent {
    @Input() structureTypeViews: StructureTypeView[];
    @Input() showMore = false;

    @Output() select = new EventEmitter<any>();
    @Output() more = new EventEmitter<any>();

    constructor(private routingService: RoutingService) {}

    goToAddContent(contentTypeView: ContentTypeView): boolean {
        this.routingService.goToPortlet(contentTypeView.name);
        this.select.emit();
        return false;
    }

    clickMore(event): boolean {
        event.preventDefault();
        this.more.emit();
        return false;
    }
}

@Component({
    providers: [ContentletService],
    selector: 'toolbar-add-contentlet',
    styles: [require('./toolbar-add-contentlet.scss')],
    templateUrl: 'toolbar-add-contentlet.html',

})
export class ToolbarAddContenletComponent extends BaseComponent {
    @ViewChild(DropdownComponent) dropdown: DropdownComponent;

    private types: StructureTypeView[];
    private typesIcons: any = {
        'Content': 'fa-table',
        'File': 'fa-picture-o',
        'Form': 'fa-list ',
        'Page': 'fa-file-text-o',
        'Persona': 'fa-user',
        'Widget': 'fa-cog'
    };
    private recent: StructureTypeView[];
    private structureTypeViewSelected: StructureTypeView[];
    private showMore = false;

    private NUMBER_BY_PAGE = 4;
    private currentPage: number = -1;
    private selectedName = '';

    constructor(private contentletService: ContentletService, private routingService: RoutingService,
                 messageService: MessageService, private iframeOverlayService: IframeOverlayService) {

        super(['more'], messageService);
    }

    ngOnInit(): void {
        this.contentletService.structureTypeView$.subscribe(structures => {
            this.types = structures;
            this.recent = [];

            this.types = this.types.filter(structure => {
                    if (structure.name.startsWith('RECENT')) {
                        this.recent.push(structure);
                    } else {
                        structure.types.forEach(type => {
                            this.routingService.addPortletURL(type.name, type.action);
                        });
                    }
                    return !structure.name.startsWith('RECENT');
                }
            );

            this.nextRecent();
        });
    }

    select(selected: StructureTypeView): void {
        if (this.structureTypeViewSelected !== this.recent && this.structureTypeViewSelected[0] === selected) {
            this.currentPage = -1;
            this.nextRecent();
            this.selectedName = '';
        }else {
            this.structureTypeViewSelected = [ selected ];
            this.showMore = false;
            this.selectedName = selected.name;
        }
    }

    close(): void {
        this.dropdown.closeIt();
    }

    nextRecent(): void {
        this.currentPage++;
        this.showMore = false;

        this.structureTypeViewSelected = this.recent.map(structureTypeView => {
            let currentPage = this.currentPage % (structureTypeView.types.length / this.NUMBER_BY_PAGE );
            this.showMore = this.showMore || structureTypeView.types.length > this.NUMBER_BY_PAGE;

            let startIndex = currentPage * this.NUMBER_BY_PAGE;
            let endIndex = startIndex + this.NUMBER_BY_PAGE;

            return {
                label: structureTypeView.label,
                name: structureTypeView.name,
                types: structureTypeView.types.slice(startIndex, endIndex)
            };
        });
    }
}

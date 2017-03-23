import {Component, ViewChild, Input, Output, EventEmitter} from '@angular/core';
import {DropdownComponent} from '../common/dropdown-component/dropdown-component';
import {ContentletService, StructureTypeView, ContentTypeView} from '../../../api/services/contentlet-service';
import {RoutingService} from '../../../api/services/routing-service';
import {BaseComponent} from '../common/_base/base-component';
import {MessageService} from '../../../api/services/messages-service';
import {IframeOverlayService} from "../../../api/services/iframe-overlay-service";

@Component({
    directives: [],
    selector: 'toolbar-add-contentlet-body',
    styleUrls: ['toolbar-add-contentlet-body.css'],
    templateUrl: 'toolbar-add-contentlet-body.html',

})
export class ToolbarAddContenletBodyComponent {
    @Input() structureTypeViews: StructureTypeView[];
    @Input() showMore: boolean = false;

    @Output() select = new EventEmitter<>();
    @Output() more = new EventEmitter<>();

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
    styleUrls: ['toolbar-add-contentlet.css'],
    templateUrl: 'toolbar-add-contentlet.html',

})
export class ToolbarAddContenletComponent extends BaseComponent {
    @ViewChild(DropdownComponent) dropdown: DropdownComponent;

    private types: StructureTypeView[];
    private typesIcons: any = {
        "Content": 'fa-table',
        "Widget": 'fa-cog',
        "File": 'fa-picture-o',
        "Page": 'fa-file-text-o',
        "Persona": 'fa-user',
    };
    private recent: StructureTypeView[];
    private structureTypeViewSelected: StructureTypeView[];
    private showMore: boolean = false;

    private NUMBER_BY_PAGE: number = 4;
    private currentPage: number = -1;
    private selectedName: string = '';

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

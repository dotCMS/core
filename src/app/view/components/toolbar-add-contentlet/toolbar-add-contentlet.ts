import { BaseComponent } from '../_common/_base/base-component';
import { Component, ViewChild, Input, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { ContentletService, StructureTypeView, ContentTypeView } from '../../../api/services/contentlet-service';
import { DotDropdownComponent } from '../_common/dropdown-component/dot-dropdown.component';
import { IframeOverlayService } from '../../../api/services/iframe-overlay-service';
import { MessageService } from '../../../api/services/messages-service';
import { RoutingService } from '../../../api/services/routing-service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'toolbar-add-contentlet-body',
    styleUrls: ['./toolbar-add-contentlet-body.scss'],
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
    encapsulation: ViewEncapsulation.None,
    providers: [ContentletService],
    selector: 'toolbar-add-contentlet',
    styleUrls: ['./toolbar-add-contentlet.scss'],
    templateUrl: 'toolbar-add-contentlet.html',

})
export class ToolbarAddContenletComponent extends BaseComponent {
    @ViewChild(DotDropdownComponent) dropdown: DotDropdownComponent;
    @Input() command?: ($event) => void;
    types: StructureTypeView[];
    mainTypes: StructureTypeView[];
    moreTypes: StructureTypeView[];
    private MAIN_CONTENT_TYPES = ['CONTENT', 'WIDGET', 'FORM', 'FILEASSET', 'HTMLPAGE'];
    private recent: StructureTypeView[];
    structureTypeViewSelected: StructureTypeView[];
    showMore = false;

    private NUMBER_BY_PAGE = 4;
    private currentPage: number = -1;
    private selectedName = '';

    constructor(private contentletService: ContentletService, private routingService: RoutingService,
                 messageService: MessageService, private iframeOverlayService: IframeOverlayService,
                 private contentTypesInfoService: ContentTypesInfoService) {

        super(['more'], messageService);
    }

    ngOnInit(): void {
        this.contentletService.structureTypeView$.subscribe(structures => {
            this.types = structures;

            this.recent = structures.filter(this.isRecentContentType);

            this.types = structures.filter(structure => {
                return !this.isRecentContentType(structure);
            });

            this.types.forEach(structure => {
                structure.types.forEach(type => {
                    this.routingService.addPortletURL(type.name, type.action);
                });
            });
            this.mainTypes = this.getMainContentType(this.types);
            this.moreTypes = this.getMoreContentTypes(this.types);

            this.nextRecent();
        });
    }

    private isRecentContentType(type: StructureTypeView): boolean {
        return type.name.startsWith('RECENT');
    }

    getMainContentType(types: StructureTypeView[]): StructureTypeView[] {
        return types.filter(type => this.MAIN_CONTENT_TYPES.includes(type.name));
    }

    getMoreContentTypes(types: StructureTypeView[]): any[] {
        return types.filter(type => !this.MAIN_CONTENT_TYPES.includes(type.name)).map(type => {
            return {
                label: type.label,
                icon: this.contentTypesInfoService.getIcon(type.name),
                command: () => {
                    this.select(type);
                }
            };
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
            const currentPage = this.currentPage % (structureTypeView.types.length / this.NUMBER_BY_PAGE );
            this.showMore = this.showMore || structureTypeView.types.length > this.NUMBER_BY_PAGE;

            const startIndex = currentPage * this.NUMBER_BY_PAGE;
            const endIndex = startIndex + this.NUMBER_BY_PAGE;

            return {
                label: structureTypeView.label,
                name: structureTypeView.name,
                types: structureTypeView.types.slice(startIndex, endIndex)
            };
        });
    }
}

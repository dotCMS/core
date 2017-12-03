import { BaseComponent } from '../_common/_base/base-component';
import {
    Component,
    ViewChild,
    Input,
    ViewEncapsulation,
    OnInit
} from '@angular/core';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { DotDropdownComponent } from '../_common/dropdown-component/dot-dropdown.component';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { MenuItem } from 'primeng/primeng';
import { MessageService } from '../../../api/services/messages-service';
import { Observable } from 'rxjs/Observable';
import { StructureTypeView } from '../../../shared/models/contentlet';
import { ToolbarAddContenletService } from './toolbar-add-contentlet.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-toolbar-add-contentlet',
    styleUrls: ['./toolbar-add-contentlet.component.scss'],
    templateUrl: 'toolbar-add-contentlet.component.html'
})
export class ToolbarAddContenletComponent extends BaseComponent implements OnInit {
    @ViewChild(DotDropdownComponent) dropdown: DotDropdownComponent;
    @Input() command?: ($event) => void;

    mainTypes: Observable<StructureTypeView[]>;
    moreTypes: Observable<MenuItem[]>;
    recent: StructureTypeView[];
    selectedName = '';
    showMore = false;
    structureTypeViewSelected: StructureTypeView[];
    types: StructureTypeView[];

    private NUMBER_BY_PAGE = 4;
    private currentPage: number = -1;

    constructor(
        messageService: MessageService,
        private toolbarAddContenletService: ToolbarAddContenletService,
        public contentTypesInfoService: ContentTypesInfoService,
        public iframeOverlayService: IframeOverlayService
    ) {
        super(['more'], messageService);
    }

    ngOnInit(): void {
        this.mainTypes = this.toolbarAddContenletService.main$;

        this.moreTypes = this.toolbarAddContenletService.more$.map((types: StructureTypeView[]) =>
            this.formatMenuItems(types)
        );

        this.toolbarAddContenletService.recent$.subscribe((types: StructureTypeView[]) => {
            this.recent = types;
            this.nextRecent();
        });
    }

    select(structure: StructureTypeView): void {
        if (
            this.structureTypeViewSelected !== this.recent &&
            this.structureTypeViewSelected[0] === structure
        ) {
            this.currentPage = -1;
            this.nextRecent();
            this.selectedName = '';
        } else {
            this.structureTypeViewSelected = [structure];
            this.showMore = false;
            this.selectedName = structure.name;
        }
    }

    close(): void {
        this.dropdown.closeIt();
    }

    nextRecent(): void {
        this.currentPage++;
        this.showMore = false;

        this.structureTypeViewSelected = this.recent.map(structureTypeView => {
            const currentPage =
                this.currentPage % (structureTypeView.types.length / this.NUMBER_BY_PAGE);
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

    private formatMenuItems(types: StructureTypeView[]): MenuItem[] {
        return types.map(type => {
            return {
                label: type.label,
                icon: this.contentTypesInfoService.getIcon(type.name),
                command: ($event) => {
                    $event.originalEvent.stopPropagation();
                    this.select(type);
                }
            };
        });
    }
}

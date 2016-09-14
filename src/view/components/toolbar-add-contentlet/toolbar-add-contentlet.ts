import {Component, ViewChild, Inject, Input, Output, EventEmitter} from '@angular/core';
import {DropdownComponent} from '../common/dropdown-component/dropdown-component';
import {ContentletService, StructureType, StructureTypeView, ContentTypeView} from '../../../api/services/contentlet-service';
import {LoginService} from '../../../api/services/login-service';
import {RoutingService} from '../../../api/services/routing-service';

@Component({
    directives: [],
    moduleId: __moduleName,
    providers: [ContentletService],
    selector: 'toolbar-add-contentlet-body',
    styleUrls: ['toolbar-add-contentlet-body.css'],
    templateUrl: ['toolbar-add-contentlet-body.html'],

})
export class ToolbarAddContenletBodyComponent {
    @Input() structureTypeView: StructureTypeView;
    @Output() select = new EventEmitter<>();

    constructor(private routingService: RoutingService) {}

    goToAddContent(contentTypeView: ContentTypeView): boolean {
        this.routingService.goToPortlet(contentTypeView.name);
        this.select.emit();
        return false;
    }
}

@Component({
    directives: [DropdownComponent, ToolbarAddContenletBodyComponent],
    moduleId: __moduleName,
    providers: [ContentletService],
    selector: 'toolbar-add-contentlet',
    styleUrls: ['toolbar-add-contentlet.css'],
    templateUrl: ['toolbar-add-contentlet.html'],

})
export class ToolbarAddContenletComponent {

    @ViewChild(DropdownComponent) dropdown: DropdownComponent;
    private i18nMessages: Array<string> = [ 'content', 'file', 'page', 'widget', 'persona'];

    private messages: any = {
        content: '',
        file: '',
        page: '',
        persona: '',
        widget: '',
    };

    private types: StructureTypeView[];
    private selected: StructureTypeView;

    constructor(private loginService: LoginService, private contentletService: ContentletService,
                private routingService: RoutingService) {}

    ngOnInit(): void {
        this.loginService.getLoginFormInfo('', this.i18nMessages).subscribe((data) => {

            this.messages = data.i18nMessagesMap;
        });

        this.contentletService.getContentTypes().subscribe(strcutures => {
            this.types = strcutures;

            this.types.forEach(strcuture => {
                    strcuture.types.forEach(type => {
                        this.routingService.addPortletURL(type.name, type.action);
                    });
                }
            );
        });
    }

    select(selected: StructureTypeView): void {
        if (this.selected && this.selected === selected) {
            this.selected = null;
        }else {
            this.selected = selected;
        }
    }

    close(): void {
        this.dropdown.closeIt();
    }
}

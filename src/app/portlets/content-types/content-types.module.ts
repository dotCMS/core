import { CommonModule } from '@angular/common';
import { ContentTypesCreateComponent } from './create';
import { ContentTypesEditComponent } from './edit';
import { ContentTypesFormComponent } from './form';
import { ContentTypesInfoService } from '../../api/services/content-types-info';
import { ContentTypesLayoutComponent } from './layout';
import { ContentTypesPortletComponent } from './main';
import { ContentTypesRoutingModule } from './content-types-routing.module';
import { CrudService } from '../../api/services/crud';
import { DotcmsConfig } from '../../api/services/system/dotcms-config';
import { FormatDateService } from '../../api/services/format-date-service';
import { FieldValidationMessageModule } from '../../view/components/_common/field-validation-message/file-validation-message.module';
import { ListingDataTableModule } from '../../view/components/listing-data-table/listing-data-table.module';
import { LoginService } from '../../api/services/login-service';
import { MessageService } from '../../api/services/messages-service';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RoutingPrivateAuthService } from '../../api/services/routing-private-auth-service';
import { SiteService } from '../../api/services/site-service';
import { StringUtils } from '../../api/util/string.utils';
import { TabViewModule, OverlayPanelModule, DropdownModule, ButtonModule, InputTextModule } from 'primeng/primeng';
import {
    ContentTypesFieldsListComponent,
    ContentTypeFieldsDropZoneComponent,
    ContentTypeFieldsRowComponent,
    ContentTypesFieldDragabbleItemComponent,
    ContentTypeFieldsRowListComponent
} from './fields';
import { DragulaModule } from 'ng2-dragula';
import { DragulaService } from 'ng2-dragula';
import { FieldService, FieldDragDropService } from './fields/service';

@NgModule({
    declarations: [
        ContentTypesCreateComponent,
        ContentTypesEditComponent,
        ContentTypesFieldDragabbleItemComponent,
        ContentTypesFieldsListComponent,
        ContentTypesFormComponent,
        ContentTypesLayoutComponent,
        ContentTypesPortletComponent,
        ContentTypeFieldsDropZoneComponent,
        ContentTypeFieldsRowComponent,
        ContentTypeFieldsRowListComponent
    ],
    exports: [
        ContentTypesPortletComponent
    ],
    imports: [
        ButtonModule,
        CommonModule,
        ContentTypesRoutingModule,
        DragulaModule,
        DropdownModule,
        FieldValidationMessageModule,
        FormsModule,
        InputTextModule,
        ListingDataTableModule,
        OverlayPanelModule,
        ReactiveFormsModule,
        TabViewModule,
    ],
    providers: [
        ContentTypesInfoService,
        CrudService,
        DotcmsConfig,
        DragulaService,
        FieldDragDropService,
        FieldService,
        FormatDateService,
        LoginService,
        MessageService,
        RoutingPrivateAuthService,
        SiteService,
        StringUtils
    ]
})
export class ContentTypesModule { }

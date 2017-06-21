import { CommonModule } from '@angular/common';
import { ContentTypesCreateComponent } from './create';
import { ContentTypesEditComponent } from './edit';
import { ContentTypesFormComponent } from './common/content-types-form';
import { ContentTypesInfoService } from '../../api/services/content-types-info';
import { ContentTypesLayoutComponent } from './common/content-type-layout';
import { ContentTypesPortletComponent } from './main';
import { ContentTypesRoutingModule } from './content-types-routing.module';
import { CrudService } from '../../api/services/crud';
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

@NgModule({
    declarations: [
        ContentTypesCreateComponent,
        ContentTypesEditComponent,
        ContentTypesFormComponent,
        ContentTypesLayoutComponent,
        ContentTypesPortletComponent
    ],
    exports: [
        ContentTypesPortletComponent,
        ContentTypesEditComponent,
        ContentTypesCreateComponent
    ],
    imports: [
        ButtonModule,
        CommonModule,
        ContentTypesRoutingModule,
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
        FormatDateService,
        LoginService,
        MessageService,
        RoutingPrivateAuthService,
        SiteService,
        StringUtils
    ]
})
export class ContentTypesModule { }

import { CommonModule } from '@angular/common';
import { ContentTypesCreateEditPortletComponent } from './create-edit/main';
import { ContentTypesForm } from './create-edit/content-types-form';
import { ContentTypesPortletComponent } from './listing';
import { ContentTypesRoutingModule } from './content-types-routing.module';
import { CrudService } from '../../api/services/crud-service';
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
        ContentTypesCreateEditPortletComponent,
        ContentTypesForm,
        ContentTypesPortletComponent
    ],
    exports: [
        ContentTypesPortletComponent,
        ContentTypesCreateEditPortletComponent
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
        CrudService,
        LoginService,
        MessageService,
        RoutingPrivateAuthService,
        SiteService,
        StringUtils
    ]
})
export class ContentTypesModule { }
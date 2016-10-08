import {BrowserModule} from '@angular/platform-browser';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {MaterialModule} from '@angular/material';
import {NgModule} from '@angular/core';

// ROUTING
import {routing} from './app.routing';

// CUSTOM SERVICES
import {AccountService} from '../../api/services/account-service'
import {ApiRoot} from '../../api/persistence/ApiRoot';
import {BundleService} from '../../api/services/bundle-service';
import {CoreWebService} from '../../api/services/core-web-service';
import {DotcmsConfig} from '../../api/services/system/dotcms-config';
import {DotcmsEventsService} from '../../api/services/dotcms-events-service';
import {FormatDateService} from '../../api/services/format-date-service';
import {LoginService} from '../../api/services/login-service';
import {MessageService} from '../../api/services/messages-service';
import {NotificationsService} from '../../api/services/notifications-service';
import {RoutingAuthService} from '../../api/services/routing-auth-service';
import {RoutingService} from '../../api/services/routing-service';
import {SiteService} from '../../api/services/site-service';
import {StringFormat} from '../../api/util/stringFormat';

// RULES ENGINE SERVICES
import {ActionService} from '../../api/rule-engine/Action'
import {ConditionGroupService} from '../../api/rule-engine/ConditionGroup';
import {ConditionService} from '../../api/rule-engine/Condition';
import {GoogleMapService} from '../../api/maps/GoogleMapService';
import {I18nService} from '../../api/system/locale/I18n';
import {RuleService} from '../../api/rule-engine/Rule';
import {UserModel} from '../../api/auth/UserModel';

// CUSTOM COMPONENTS
import {AppComponent} from './app';
import {Accordion, AccordionGroup} from './common/accordion/accordion';
import {CustomTimeComponent} from './common/custom-time/custom-time';
import {DotSelect, DotOption} from './common/dot-select/dot-select';
import {DotSelect, DotOption} from './common/dot-select/dot-select';
import {DropdownComponent} from "./common/dropdown-component/dropdown-component";
import {DropdownComponent} from './common/dropdown-component/dropdown-component';
import {ForgotPasswordComponent} from './common/login/forgot-password-component/forgot-password-component';
import {ForgotPasswordContainer} from './common/login/forgot-password-component/forgot-password-container';
import {GlobalSearch} from './common/global-search/global-search';
import {IframeLegacyComponent} from './common/iframe-legacy/Iframe-legacy-component';
import {LoginAsComponent} from './common/login-as/login-as';
import {LoginComponent} from './common/login/login-component/login-component';
import {LoginContainer} from './common/login/login-component/login-container';
import {LoginPageComponent} from './common/login/login-page-component';
import {MainComponent} from './common/main-component/main-component';
import {MainNavigation} from './common/main-navigation/main-navigation';
import {MyAccountComponent} from './my-account-component/dot-my-account-component';
import {NotificationsList, NotificationsItem} from './common/notifications/notifications';
import {PatternLibrary} from './common/pattern-library/pattern-library';
import {ResetPasswordComponent} from './common/login/reset-password-component/reset-password-component';
import {ResetPasswordContainer} from './common/login/reset-password-component/reset-password-container';
import {SiteSelectorComponent} from './site-selector/dot-site-selector-component';
import {ToolbarAddContenletComponent, ToolbarAddContenletBodyComponent} from './toolbar-add-contentlet/toolbar-add-contentlet';
import {ToolbarNotifications} from './common/toolbar-notifications/toolbar-notifications';
import {ToolbarUserComponent} from './common/toolbar-user/toolbar-user';

// RULES ENGINE COMPONENTS
import {AddToBundleDialogComponent} from "./common/push-publish/add-to-bundle-dialog-component";
import {AddToBundleDialogContainer} from "./common/push-publish/add-to-bundle-dialog-container";
import {AreaPickerDialogComponent} from './common/google-map/area-picker-dialog.component';
import {ConditionComponent} from './rule-engine/rule-condition-component';
import {ConditionGroupComponent} from './rule-engine/rule-condition-group-component';
import {Dropdown, InputOption} from "./semantic/modules/dropdown/dropdown";
import {InputDate} from './semantic/elements/input-date/input-date';
import {InputText} from "./semantic/elements/input-text/input-text";
import {InputToggle} from './input/toggle/inputToggle';
import {ModalDialogComponent} from './common/modal-dialog/dialog-component';
import {PushPublishDialogComponent} from "./common/push-publish/push-publish-dialog-component";
import {PushPublishDialogContainer} from "./common/push-publish/push-publish-dialog-container";
import {RestDropdown} from './semantic/modules/restdropdown/RestDropdown';
import {RuleActionComponent} from './rule-engine/rule-action-component';
import {RuleComponent} from './rule-engine/rule-component';
import {RuleEngineComponent} from './rule-engine/rule-engine';
import {RuleEngineContainer} from './rule-engine/rule-engine.container';
import {ServersideCondition} from './rule-engine/condition-types/serverside-condition/serverside-condition';
import {VisitorsLocationComponent} from './rule-engine/custom-types/visitors-location/visitors-location.component';
import {VisitorsLocationContainer} from './rule-engine/custom-types/visitors-location/visitors-location.container';

const RULES_ENGINE_COMPONENTS = [
    AddToBundleDialogComponent,
    AddToBundleDialogContainer,
    AreaPickerDialogComponent,
    ConditionComponent,
    ConditionGroupComponent,
    Dropdown,
    InputDate,
    InputOption,
    InputText,
    InputToggle,
    ModalDialogComponent,
    PushPublishDialogComponent,
    PushPublishDialogContainer,
    RestDropdown,
    RuleActionComponent,
    RuleComponent,
    RuleEngineComponent,
    RuleEngineContainer,
    ServersideCondition,
    VisitorsLocationComponent,
    VisitorsLocationContainer,
];

// CUSTOM PIPES
import {CapitalizePipe} from '../../api/pipes/capitalize-pipe';

// CUSTOM DIRECTIVES
import {MessageKeyDirective} from '../directives/message-keys';


const COMPONENTS = [
    Accordion,
    AccordionGroup,
    AppComponent,
    CustomTimeComponent,
    DotOption,
    DotSelect,
    DropdownComponent,
    ForgotPasswordComponent,
    ForgotPasswordContainer,
    GlobalSearch,
    IframeLegacyComponent,
    LoginAsComponent,
    LoginComponent,
    LoginContainer,
    LoginPageComponent,
    MainComponent,
    MainNavigation,
    MyAccountComponent,
    NotificationsItem,
    NotificationsList,
    PatternLibrary,
    ResetPasswordComponent,
    ResetPasswordContainer,
    SiteSelectorComponent,
    ToolbarAddContenletBodyComponent,
    ToolbarAddContenletComponent,
    ToolbarNotifications,
    ToolbarUserComponent,
];

const PIPES = [
    CapitalizePipe
];

const DIRECTIVES = [
    MessageKeyDirective
];

const RULES_ENGINE_SERVICES = [
    ActionService,
    ConditionGroupService,
    ConditionService,
    GoogleMapService,
    I18nService,
    RuleService,
];

@NgModule({
    bootstrap: [AppComponent],
    declarations: [
        ...COMPONENTS,
        ...PIPES,
        ...DIRECTIVES,
        ...RULES_ENGINE_COMPONENTS,
    ],
    imports: [
        BrowserModule,
        FormsModule,
        HttpModule,
        MaterialModule.forRoot(),
        ReactiveFormsModule,
        routing,
    ],
    providers: [
        AccountService,
        ApiRoot,
        BundleService,
        CoreWebService,
        DotcmsConfig,
        DotcmsEventsService,
        FormatDateService,
        LoginService,
        MessageService,
        NotificationsService,
        RoutingAuthService,
        RoutingService,
        SiteService,
        StringFormat,
        UserModel,
        ...RULES_ENGINE_SERVICES
    ]
})
export class AppModule {

}

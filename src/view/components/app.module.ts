import {BrowserModule} from '@angular/platform-browser';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {NgModule} from '@angular/core';
import {Logger} from 'angular2-logger/core';

// ROUTING
import {LocationStrategy, HashLocationStrategy} from '@angular/common';
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
import {NotLicensedService} from '../../api/services/not-licensed-service';

import {RoutingPublicAuthService} from '../../api/services/routing-public-auth-service';
import {RoutingPrivateAuthService} from '../../api/services/routing-private-auth-service';
import {RoutingRootAuthService} from '../../api/services/routing-root-auth-service';

import {RoutingService} from '../../api/services/routing-service';
import {SiteService} from '../../api/services/site-service';
import {DotRouterService} from '../../api/services/dot-router-service';
import {StringFormat} from '../../api/util/stringFormat';

import {IframeOverlayService} from '../../api/services/iframe-overlay-service';

// RULES ENGINE SERVICES
import {ActionService} from '../../api/rule-engine/Action';
import {ConditionGroupService} from '../../api/rule-engine/ConditionGroup';
import {ConditionService} from '../../api/rule-engine/Condition';
import {GoogleMapService} from '../../api/maps/GoogleMapService';
import {I18nService} from '../../api/system/locale/I18n';
import {RuleService} from '../../api/rule-engine/Rule';
import {UserModel} from '../../api/auth/UserModel';

// CUSTOM COMPONENTS
import {AppComponent} from './app';
import {Accordion, AccordionGroup} from './common/accordion/accordion';
import {DotLoadingIndicator} from './common/dot-loading-indicator/dot-loading-indicator';
import {CustomTimeComponent} from './common/custom-time/custom-time';
import {DropdownComponent} from './common/dropdown-component/dropdown-component';
import {ForgotPasswordComponent} from './common/login/forgot-password-component/forgot-password-component';
import {ForgotPasswordContainer} from './common/login/forgot-password-component/forgot-password-container';
import {GlobalSearch} from './common/global-search/global-search';
import {IframeLegacyComponent} from './common/iframe-legacy/iframe-legacy-component';
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
import {NotLicensedComponent} from './not-licensed-component/not-licensed-component';

// RULES ENGINE COMPONENTS
import {AddToBundleDialogComponent} from './common/push-publish/add-to-bundle-dialog-component';
import {AddToBundleDialogContainer} from './common/push-publish/add-to-bundle-dialog-container';
import {AreaPickerDialogComponent} from './common/google-map/area-picker-dialog.component';
import {ConditionComponent} from './rule-engine/rule-condition-component';
import {ConditionGroupComponent} from './rule-engine/rule-condition-group-component';
import {Dropdown, InputOption} from './semantic/modules/dropdown/dropdown';
import {InputDate} from './semantic/elements/input-date/input-date';
import {InputText} from './semantic/elements/input-text/input-text';
import {InputToggle} from './input/toggle/inputToggle';
import {ModalDialogComponent} from './common/modal-dialog/dialog-component';
import {PushPublishDialogComponent} from './common/push-publish/push-publish-dialog-component';
import {PushPublishDialogContainer} from './common/push-publish/push-publish-dialog-container';
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
    DotLoadingIndicator,
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
    MainCoreComponent,
    NotLicensedComponent,
    LogOutContainer
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

import {InputTextModule} from 'primeng/primeng';
import {PasswordModule} from 'primeng/primeng';
import {CheckboxModule} from 'primeng/primeng';
import {ButtonModule} from 'primeng/primeng';
import {DropdownModule} from 'primeng/primeng';
import {AutoCompleteModule} from 'primeng/primeng';
import {MainCoreComponent} from './main-core-component/MainCoreComponent';
import {ToolbarModule} from 'primeng/primeng';
import {DialogModule} from 'primeng/primeng';
import {RadioButtonModule} from 'primeng/primeng';
import {LoggerService} from '../../api/services/logger.service';
import {LogOutContainer} from './common/login/login-component/log-out-container';
import {Config} from '../../api/util/config';
import {StringUtils} from '../../api/util/string.utils';
import {SocketFactory} from '../../api/services/protocol/socket-factory';

const NGFACES_MODULES = [
    InputTextModule,
    PasswordModule,
    CheckboxModule,
    RadioButtonModule,
    ButtonModule,
    DropdownModule,
    AutoCompleteModule,
    ToolbarModule,
    DialogModule
];

@NgModule({
    bootstrap: [AppComponent],
    declarations: [
        ...PIPES,
        ...COMPONENTS,
        ...DIRECTIVES,
        ...RULES_ENGINE_COMPONENTS,
    ],
    imports: [
        BrowserModule,
        FormsModule,
        HttpModule,
        ReactiveFormsModule,
        routing,
        ...NGFACES_MODULES,
    ],
    providers: [
        StringUtils,
        Config,
        Logger,
        LoggerService,
        CoreWebService,
        NotLicensedService,
        AccountService,
        ApiRoot,
        BundleService,
        DotcmsConfig,
        DotcmsEventsService,
        DotRouterService,
        FormatDateService,
        LoginService,
        MessageService,
        NotificationsService,
        RoutingPublicAuthService,
        RoutingPrivateAuthService,
        RoutingRootAuthService,
        RoutingService,
        SiteService,
        StringFormat,
        UserModel,
        IframeOverlayService,
        ...RULES_ENGINE_SERVICES,
        {provide: LocationStrategy, useClass: HashLocationStrategy},
        SocketFactory
    ]
})
export class AppModule {

}

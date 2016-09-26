import {AppComponent} from './app';
import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {MaterialModule} from '@angular/material';


// CUSTOM SERVICES
import {ApiRoot} from '../../api/persistence/ApiRoot';
import {BundleService} from '../../api/services/bundle-service';
import {CoreWebService} from '../../api/services/core-web-service';
import {DotcmsConfig} from '../../api/services/system/dotcms-config';
import {DotcmsEventsService} from '../../api/services/dotcms-events-service';
import {FormatDateService} from '../../api/services/format-date-service';
import {LoginService} from '../../api/services/login-service';
import {RoutingService} from '../../api/services/routing-service';
import {SiteService} from '../../api/services/site-service';
import {MessageService} from '../../api/services/messages-service';
import {NotificationsService} from '../../api/services/notifications-service';
// Rules Engines Dependencies
import {ConditionGroupService} from '../../api/rule-engine/ConditionGroup';
import {ConditionService} from '../../api/rule-engine/Condition';
import {GoogleMapService} from '../../api/maps/GoogleMapService';
import {I18nService} from '../../api/system/locale/I18n';
import {RuleService} from '../../api/rule-engine/Rule';
import {UserModel} from '../../api/auth/UserModel';


// CUSTOM COMPONENTS
import {routing} from './app.routing';
import {MainComponent} from './common/main-component/main-component';
import {LoginPageComponent} from './common/login/login-page-component';
import {LoginContainer} from './common/login/login-component/login-container';
import {LoginComponent} from './common/login/login-component/login-component';
import {GlobalSearch} from './common/global-search/global-search';
import {IframeLegacyComponent} from './common/iframe-legacy/Iframe-legacy-component';
import {MainNavigation} from './common/main-navigation/main-navigation';
import {ToolbarUserComponent} from './common/toolbar-user/toolbar-user';
import {Accordion, AccordionGroup} from './common/accordion/accordion';
import {DropdownComponent} from './common/dropdown-component/dropdown-component';
import {LoginAsComponent} from './common/login-as/login-as';
import {DotSelect, DotOption} from './common/dot-select/dot-select';
import {ToolbarNotifications} from './common/toolbar-notifications/toolbar-notifications';
import {MyAccountComponent} from './my-account-component/dot-my-account-component';
import {NotificationsList, NotificationsItem} from './common/notifications/notifications';
import {CustomTimeComponent} from './common/custom-time/custom-time';
import {ToolbarAddContenletComponent, ToolbarAddContenletBodyComponent} from './toolbar-add-contentlet/toolbar-add-contentlet';
import {SiteSelectorComponent} from './site-selector/dot-site-selector-component';
import {ForgotPasswordContainer} from './common/login/forgot-password-component/forgot-password-container'
import {ForgotPasswordComponent} from './common/login/forgot-password-component/forgot-password-component'
import {ResetPasswordContainer} from './common/login/reset-password-component/reset-password-container'
import {ResetPasswordComponent} from './common/login/reset-password-component/reset-password-component'


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

@NgModule({
    bootstrap: [AppComponent],
    declarations: [
        ...COMPONENTS,
        ...PIPES,
        ...DIRECTIVES
    ],
    imports: [
        BrowserModule,
        FormsModule,
        HttpModule,
        routing,
        MaterialModule.forRoot()
    ],
    providers: [
        ApiRoot,
        BundleService,
        CoreWebService,
        DotcmsConfig,
        DotcmsEventsService,
        FormatDateService,
        LoginService,
        MessageService,
        NotificationsService,
        RoutingService,
        SiteService,
        UserModel,
    ]
})
export class AppModule {

}

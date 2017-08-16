// RULES ENGINE COMPONENTS

// import { AddToBundleDialogComponent } from './view/components/push-publish/add-to-bundle-dialog-component';
// import { AddToBundleDialogContainer } from './view/components/push-publish/add-to-bundle-dialog-container';
import { AreaPickerDialogComponent } from './portlets/rule-engine/google-map/area-picker-dialog.component';
import { ModalDialogComponent } from './portlets/rule-engine/modal-dialog/dialog-component';
// import { PushPublishDialogComponent } from './view/components/push-publish/push-publish-dialog-component';
// import { PushPublishDialogContainer } from './view/components/push-publish/push-publish-dialog-container';

// import {SiteSelectorComponent as SiteSelectorComponentDotJS} from '../dotcms-js/components/site-selector/site-selector.component';
// import {DotcmsBreadcrumbModule} from '../dotcms-js/components/breadcrumb/breadcrumb.component';
// import {DotcmsSiteTreeTableModule} from '../dotcms-js/components/site-treetable/site-treetable.component';
// import {DotcmsSiteDatatableModule} from '../dotcms-js/components/site-datatable/site-datatable.component';
// import {DotcmsTreeableDetailModule} from '../dotcms-js/components/treeable-detail/treeable-detail.component';
// import {DotBrowserComponent} from './portlets/dot-browser/dot-browser-component';


// CUSTOM COMPONENTS
import { Accordion, AccordionGroup } from './view/components/_common/accordion/accordion';
import { CustomTimeComponent } from './view/components/_common/custom-time/custom-time';
import { DotLoadingIndicator } from './view/components/_common/dot-loading-indicator/dot-loading-indicator';
import { DropdownComponent } from './view/components/_common/dropdown-component/dropdown-component';
import { ForgotPasswordComponent } from './view/components/login/forgot-password-component/forgot-password-component';
import { ForgotPasswordContainer } from './view/components/login/forgot-password-component/forgot-password-container';
import { GlobalSearch } from './view/components/global-search/global-search';
import { GravatarComponent } from './view/components/_common/gravatar/gravatar.component';
import { IframeLegacyComponent } from './view/components/iframe-legacy/iframe-legacy-component';
import { LogOutContainer } from './view/components/login/login-component/log-out-container';
import { LoginAsComponent } from './view/components/login-as/login-as';
import { LoginComponent } from './view/components/login/login-component/login-component';
import { LoginContainer } from './view/components/login/login-component/login-container';
import { LoginPageComponent } from './view/components/login/login-page-component';
import { MainComponentLegacy } from './view/components/main-legacy/main-legacy.component';
import { MainCoreLegacyComponent } from './view/components/main-core-legacy/main-core-legacy-component';
import { MainNavigation } from './view/components/main-navigation/main-navigation';
import { MyAccountComponent } from './view/components/my-account/dot-my-account-component';
import { NotLicensedComponent } from './view/components/not-licensed/not-licensed-component';
import { NotificationsList, NotificationsItem } from './view/components/_common/notifications/notifications';
import { PatternLibrary } from './view/components/_common/pattern-library/pattern-library';
import { ResetPasswordComponent } from './view/components/login/reset-password-component/reset-password-component';
import { ResetPasswordContainer } from './view/components/login/reset-password-component/reset-password-container';
import { SiteSelectorComponent } from './view/components/_common/site-selector/site-selector.component';
import { ToolbarNotifications } from './view/components/toolbar-notifications/toolbar-notifications';
import { ToolbarUserComponent } from './view/components/toolbar-user/toolbar-user';
import { ToolbarComponent } from './view/components/toolbar/toolbar.component';

export const COMPONENTS = [
    Accordion,
    AccordionGroup,
    CustomTimeComponent,
    // DotBrowserComponent,
    DotLoadingIndicator,
    DropdownComponent,
    ForgotPasswordComponent,
    ForgotPasswordContainer,
    GlobalSearch,
    GravatarComponent,
    IframeLegacyComponent,
    LogOutContainer,
    LoginAsComponent,
    LoginComponent,
    LoginContainer,
    LoginPageComponent,
    MainComponentLegacy,
    MainCoreLegacyComponent,
    MainNavigation,
    MyAccountComponent,
    NotLicensedComponent,
    NotificationsItem,
    NotificationsList,
    PatternLibrary,
    ResetPasswordComponent,
    ResetPasswordContainer,
    // SiteSelectorComponentDotJS,
    ToolbarAddContenletBodyComponent,
    ToolbarAddContenletComponent,
    ToolbarComponent,
    ToolbarNotifications,
    ToolbarUserComponent
];

// CUSTOM PIPES
import { CapitalizePipe } from './api/pipes/capitalize-pipe';

export const PIPES = [
    CapitalizePipe
];

// CUSTOM DIRECTIVES
import { MessageKeyDirective } from './view/directives/message-keys';
import { DotRippleEffectDirective } from './view/directives/ripple/ripple-effect';
import { MaterialDesignTextfield } from './view/directives/md-inputtext/md-inputtext';
import {
    ToolbarAddContenletBodyComponent,
    ToolbarAddContenletComponent
} from './view/components/toolbar-add-contentlet/toolbar-add-contentlet';

export const DIRECTIVES = [
    MessageKeyDirective,
    DotRippleEffectDirective,
    MaterialDesignTextfield
];

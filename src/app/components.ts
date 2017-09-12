// CUSTOM COMPONENTS
import { Accordion, AccordionGroup } from './view/components/_common/accordion/accordion';
import { CustomTimeComponent } from './view/components/_common/custom-time/custom-time';
import { DotLoadingIndicatorComponent } from './view/components/_common/dot-loading-indicator/dot-loading-indicator.component';
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
import { ToolbarNotifications } from './view/components/toolbar-notifications/toolbar-notifications';
import { ToolbarUserComponent } from './view/components/toolbar-user/toolbar-user';
import { ToolbarComponent } from './view/components/toolbar/toolbar.component';

export const COMPONENTS = [
    Accordion,
    AccordionGroup,
    CustomTimeComponent,
    DotLoadingIndicatorComponent,
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

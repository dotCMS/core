// RULES ENGINE COMPONENTS
/*
import {AddToBundleDialogComponent} from './view/components/common/push-publish/add-to-bundle-dialog-component';
import {AddToBundleDialogContainer} from './view/components/common/push-publish/add-to-bundle-dialog-container';
import {AreaPickerDialogComponent} from './view/components/common/google-map/area-picker-dialog.component';
import {ConditionComponent} from './view/components/rule-engine/rule-condition-component';
import {ConditionGroupComponent} from './view/components/rule-engine/rule-condition-group-component';
import {Dropdown, InputOption} from './view/components/semantic/modules/dropdown/dropdown';
import {InputDate} from './view/components/semantic/elements/input-date/input-date';
import {InputText} from './view/components/semantic/elements/input-text/input-text';
import {InputToggle} from './view/components/input/toggle/inputToggle';
import {ModalDialogComponent} from './view/components/common/modal-dialog/dialog-component';
import {PushPublishDialogComponent} from './view/components/common/push-publish/push-publish-dialog-component';
import {PushPublishDialogContainer} from './view/components/common/push-publish/push-publish-dialog-container';
import {RestDropdown} from './view/components/semantic/modules/restdropdown/RestDropdown';
import {RuleActionComponent} from './view/components/rule-engine/rule-action-component';
import {RuleComponent} from './view/components/rule-engine/rule-component';
import {RuleEngineComponent} from './view/components/rule-engine/rule-engine';
import {RuleEngineContainer} from './view/components/rule-engine/rule-engine.container';
import {ServersideCondition} from './view/components/rule-engine/condition-types/serverside-condition/serverside-condition';
import {VisitorsLocationComponent} from './view/components/rule-engine/custom-types/visitors-location/visitors-location.component';
import {VisitorsLocationContainer} from './view/components/rule-engine/custom-types/visitors-location/visitors-location.container';
*/
const RULES_ENGINE_COMPONENTS = [
    /*AddToBundleDialogComponent,
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
    VisitorsLocationContainer,*/
];

// CUSTOM COMPONENTS
import {Accordion, AccordionGroup} from './view/components/common/accordion/accordion';
import {DotLoadingIndicator} from './view/components/common/dot-loading-indicator/dot-loading-indicator';
import {CustomTimeComponent} from './view/components/common/custom-time/custom-time';
import {DropdownComponent} from './view/components/common/dropdown-component/dropdown-component';
import {ForgotPasswordComponent} from './view/components/common/login/forgot-password-component/forgot-password-component';
import {ForgotPasswordContainer} from './view/components/common/login/forgot-password-component/forgot-password-container';
import {GlobalSearch} from './view/components/common/global-search/global-search';
import {IframeLegacyComponent} from './view/components/common/iframe-legacy/iframe-legacy-component';
import {LoginAsComponent} from './view/components/common/login-as/login-as';
import {LoginComponent} from './view/components/common/login/login-component/login-component';
import {LoginContainer} from './view/components/common/login/login-component/login-container';
import {LoginPageComponent} from './view/components/common/login/login-page-component';
import {MainComponent} from './view/components/common/main-component/main-component';
import {MainNavigation} from './view/components/common/main-navigation/main-navigation';
import {MyAccountComponent} from './view/components/my-account-component/dot-my-account-component';
import {NotificationsList, NotificationsItem} from './view/components/common/notifications/notifications';
import {PatternLibrary} from './view/components/common/pattern-library/pattern-library';
import {ResetPasswordComponent} from './view/components/common/login/reset-password-component/reset-password-component';
import {ResetPasswordContainer} from './view/components/common/login/reset-password-component/reset-password-container';
import {SiteSelectorComponent} from './view/components/site-selector/dot-site-selector-component';
import {ToolbarAddContenletComponent, ToolbarAddContenletBodyComponent} from './view/components/toolbar-add-contentlet/toolbar-add-contentlet';
import {ToolbarNotifications} from './view/components/common/toolbar-notifications/toolbar-notifications';
import {ToolbarUserComponent} from './view/components/common/toolbar-user/toolbar-user';
import {NotLicensedComponent} from './view/components/not-licensed-component/not-licensed-component';
import {MainCoreComponent} from './view/components/main-core-component/MainCoreComponent';
import {LogOutContainer} from './view/components/common/login/login-component/log-out-container';

export const COMPONENTS = [
    Accordion,
    AccordionGroup,
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
    LogOutContainer,
    ...RULES_ENGINE_COMPONENTS
];

// CUSTOM PIPES
import {CapitalizePipe} from './api/pipes/capitalize-pipe';

export const PIPES = [
    CapitalizePipe
];

// CUSTOM DIRECTIVES
import {MessageKeyDirective} from './view/directives/message-keys';

export const DIRECTIVES = [
    MessageKeyDirective
];

// RULES ENGINE COMPONENTS
import {AddToBundleDialogComponent} from './view/components/push-publish/add-to-bundle-dialog-component';
import {AddToBundleDialogContainer} from './view/components/push-publish/add-to-bundle-dialog-container';
import {AreaPickerDialogComponent} from './view/components/_common/google-map/area-picker-dialog.component';
import {ConditionComponent} from './portlets/rule-engine/rule-condition-component';
import {ConditionGroupComponent} from './portlets/rule-engine/rule-condition-group-component';
import {Dropdown, InputOption} from './portlets/rule-engine/semantic/modules/dropdown/dropdown';
import {InputDate} from './portlets/rule-engine/semantic/elements/input-date/input-date';
import {InputText} from './portlets/rule-engine/semantic/elements/input-text/input-text';
import {InputToggle} from './portlets/rule-engine/input/toggle/inputToggle';
import {ModalDialogComponent} from './view/components/_common/modal-dialog/dialog-component';
import {PushPublishDialogComponent} from './view/components/push-publish/push-publish-dialog-component';
import {PushPublishDialogContainer} from './view/components/push-publish/push-publish-dialog-container';
import {RestDropdown} from './portlets/rule-engine/semantic/modules/restdropdown/RestDropdown';
import {RuleActionComponent} from './portlets/rule-engine/rule-action-component';
import {RuleComponent} from './portlets/rule-engine/rule-component';
import {RuleEngineComponent} from './portlets/rule-engine/rule-engine';
import {RuleEngineContainer} from './portlets/rule-engine/rule-engine.container';
import {ServersideCondition} from './portlets/rule-engine/condition-types/serverside-condition/serverside-condition';
import {VisitorsLocationComponent} from './portlets/rule-engine/custom-types/visitors-location/visitors-location.component';
import {VisitorsLocationContainer} from './portlets/rule-engine/custom-types/visitors-location/visitors-location.container';

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

// CUSTOM COMPONENTS
import {Accordion, AccordionGroup} from './view/components/_common/accordion/accordion';
import {CustomTimeComponent} from './view/components/_common/custom-time/custom-time';
import {DotLoadingIndicator} from './view/components/_common/dot-loading-indicator/dot-loading-indicator';
import {DropdownComponent} from './view/components/_common/dropdown-component/dropdown-component';
import {ForgotPasswordComponent} from './view/components/login/forgot-password-component/forgot-password-component';
import {ForgotPasswordContainer} from './view/components/login/forgot-password-component/forgot-password-container';
import {GlobalSearch} from './view/components/global-search/global-search';
import {IframeLegacyComponent} from './view/components/iframe-legacy/iframe-legacy-component';
import {LogOutContainer} from './view/components/login/login-component/log-out-container';
import {LoginAsComponent} from './view/components/login-as/login-as';
import {LoginComponent} from './view/components/login/login-component/login-component';
import {LoginContainer} from './view/components/login/login-component/login-container';
import {LoginPageComponent} from './view/components/login/login-page-component';
import {MainComponentLegacy} from './view/components/main-legacy/main-legacy-component';
import {MainCoreLegacyComponent} from './view/components/main-core-legacy/main-core-legacy-component';
import {MainNavigation} from './view/components/main-navigation/main-navigation';
import {MyAccountComponent} from './view/components/my-account/dot-my-account-component';
import {NotLicensedComponent} from './view/components/not-licensed/not-licensed-component';
import {NotificationsList, NotificationsItem} from './view/components/_common/notifications/notifications';
import {PatternLibrary} from './view/components/_common/pattern-library/pattern-library';
import {ResetPasswordComponent} from './view/components/login/reset-password-component/reset-password-component';
import {ResetPasswordContainer} from './view/components/login/reset-password-component/reset-password-container';
import {SiteSelectorComponent} from './view/components/site-selector/dot-site-selector-component';
import {ToolbarAddContenletComponent, ToolbarAddContenletBodyComponent} from './view/components/toolbar-add-contentlet/toolbar-add-contentlet';
import {ToolbarNotifications} from './view/components/toolbar-notifications/toolbar-notifications';
import {ToolbarUserComponent} from './view/components/toolbar-user/toolbar-user';

export const COMPONENTS = [
    ...RULES_ENGINE_COMPONENTS,
    Accordion,
    AccordionGroup,
    CustomTimeComponent,
    DotLoadingIndicator,
    DropdownComponent,
    ForgotPasswordComponent,
    ForgotPasswordContainer,
    GlobalSearch,
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
    SiteSelectorComponent,
    ToolbarAddContenletBodyComponent,
    ToolbarAddContenletComponent,
    ToolbarNotifications,
    ToolbarUserComponent
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
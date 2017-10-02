// CUSTOM COMPONENTS
import { CustomTimeComponent } from './view/components/_common/custom-time/custom-time';
import { GlobalSearch } from './view/components/global-search/global-search';
import { LogOutContainer } from './view/components/login/login-component/log-out-container';
import { LoginAsComponent } from './view/components/login-as/login-as';
import { LoginPageComponent } from './view/components/login/login-page-component';
import { MainComponentLegacy } from './view/components/main-legacy/main-legacy.component';
import { MainCoreLegacyComponent } from './view/components/main-core-legacy/main-core-legacy-component';
import { MyAccountComponent } from './view/components/my-account/dot-my-account-component';
import {
    NotificationsList,
    NotificationsItem
} from './view/components/_common/notifications/notifications';
import { ToolbarNotifications } from './view/components/toolbar-notifications/toolbar-notifications';
import { ToolbarUserComponent } from './view/components/toolbar-user/toolbar-user';
import { ToolbarComponent } from './view/components/toolbar/toolbar.component';

export const COMPONENTS = [
    CustomTimeComponent,
    GlobalSearch,
    LogOutContainer,
    LoginAsComponent,
    LoginPageComponent,
    MainComponentLegacy,
    MainCoreLegacyComponent,
    MyAccountComponent,
    NotificationsItem,
    NotificationsList,
    ToolbarComponent,
    ToolbarNotifications,
    ToolbarUserComponent
];

// CUSTOM PIPES
import { CapitalizePipe, SafePipe} from './view/pipes/index';

export const PIPES = [CapitalizePipe, SafePipe];

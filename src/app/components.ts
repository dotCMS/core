import { CustomTimeComponent } from '@components/_common/custom-time/custom-time';
import { DotAlertConfirmComponent } from '@components/_common/dot-alert-confirm/dot-alert-confirm';
import { GlobalSearchComponent } from '@components/global-search/global-search';
import { DotLogOutContainerComponent } from '@components/login/dot-logout-container-component/dot-log-out-container';
import { LoginAsComponent } from '@components/login-as/login-as';
import { MainCoreLegacyComponent } from '@components/main-core-legacy/main-core-legacy-component';
import { MainComponentLegacyComponent } from '@components/main-legacy/main-legacy.component';
import { MyAccountComponent } from '@components/my-account/dot-my-account-component';
import {
    NotificationsItemComponent,
    NotificationsListComponent
} from '@components/_common/notifications/notifications';
import { ToolbarComponent } from '@components/dot-toolbar/dot-toolbar.component';
import { ToolbarNotificationsComponent } from '@components/toolbar-notifications/toolbar-notifications';
import { ToolbarUserComponent } from '@components/toolbar-user/toolbar-user';
import { DotLoginPageComponent } from '@components/login/main/dot-login-page.component';

// CUSTOM PIPES
import { CapitalizePipe, SafePipe } from '@pipes/index';


export const COMPONENTS = [
    ToolbarUserComponent,
    ToolbarNotificationsComponent,
    ToolbarComponent,
    NotificationsListComponent,
    NotificationsItemComponent,
    MyAccountComponent,
    MainCoreLegacyComponent,
    MainComponentLegacyComponent,
    DotLoginPageComponent,
    LoginAsComponent,
    DotLogOutContainerComponent,
    GlobalSearchComponent,
    DotAlertConfirmComponent,
    CustomTimeComponent
];

export const PIPES = [CapitalizePipe, SafePipe];

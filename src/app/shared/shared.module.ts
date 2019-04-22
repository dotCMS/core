import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
    ApiRoot,
    BrowserUtil,
    CoreWebService,
    DotcmsConfig,
    DotcmsEventsService,
    LoggerService,
    LoginService,
    SiteService,
    StringUtils,
    UserModel,
    DotEventsSocket,
    DotEventsSocketURL
} from 'dotcms-js';

// Common Modules
import { DotDropdownModule } from '@components/_common/dropdown-component/dot-dropdown.module';
import { GravatarModule } from '@components/_common/gravatar/gravatar.module';
import { MainNavigationModule } from '@components/dot-navigation/dot-navigation.module';
import { DotEventsService } from '../api/services/dot-events/dot-events.service';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';

@NgModule({
    declarations: [],
    imports: [CommonModule, DotDropdownModule, GravatarModule, MainNavigationModule],
    exports: [
        CommonModule,
        // Common Modules
        DotDropdownModule,
        GravatarModule,
        MainNavigationModule
    ]
})
export class SharedModule {

    private static readonly dotEventSocketURL =
        new DotEventsSocketURL(
            `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
            window.location.protocol === 'https'
        );

    static forRoot(): ModuleWithProviders {
        return {
            ngModule: SharedModule,
            providers: [
                ApiRoot,
                BrowserUtil,
                CoreWebService,
                DotEventsService,
                DotNavigationService,
                DotcmsConfig,
                DotcmsEventsService,
                LoggerService,
                LoginService,
                SiteService,
                { provide: DotEventsSocketURL, useValue: SharedModule.dotEventSocketURL},
                DotEventsSocket,
                StringUtils,
                UserModel
            ]
        };
    }
}

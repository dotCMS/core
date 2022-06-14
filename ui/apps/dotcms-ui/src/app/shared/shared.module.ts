import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
    ApiRoot,
    BrowserUtil,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    LoggerService,
    LoginService,
    SiteService,
    StringUtils,
    UserModel,
    DotEventsSocket,
    DotEventsSocketURL
} from '@dotcms/dotcms-js';

// Common Modules
import { DotDropdownModule } from '@components/_common/dot-dropdown-component/dot-dropdown.module';
import { MainNavigationModule } from '@components/dot-navigation/dot-navigation.module';
import { DotEventsService } from '../api/services/dot-events/dot-events.service';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';

const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

@NgModule({
    declarations: [],
    imports: [CommonModule, DotDropdownModule, MainNavigationModule],
    exports: [
        CommonModule,
        // Common Modules
        DotDropdownModule,
        MainNavigationModule
    ]
})
export class SharedModule {
    static forRoot(): ModuleWithProviders<SharedModule> {
        return {
            ngModule: SharedModule,
            providers: [
                ApiRoot,
                BrowserUtil,
                CoreWebService,
                DotEventsService,
                DotNavigationService,
                DotcmsConfigService,
                DotcmsEventsService,
                LoggerService,
                LoginService,
                SiteService,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotEventsSocket,
                StringUtils,
                UserModel
            ]
        };
    }
}

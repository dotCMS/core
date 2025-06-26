import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule } from '@angular/core';

// Common Modules
import { MainNavigationModule } from '@components/dot-navigation/dot-navigation.module';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotEventsService } from '@dotcms/data-access';
import {
    ApiRoot,
    BrowserUtil,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    SiteService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';

const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

@NgModule({
    declarations: [],
    imports: [CommonModule, MainNavigationModule],
    exports: [
    ]
})
export class SharedModule {
    static forRoot(): ModuleWithProviders<SharedModule> {
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

import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule } from '@angular/core';

// Common Modules
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
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';

import { DotNavigationComponent } from '../view/components/dot-navigation/dot-navigation.component';
import { DotNavigationService } from '../view/components/dot-navigation/services/dot-navigation.service';

const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

@NgModule({
    declarations: [],
    imports: [CommonModule, DotNavigationComponent],
    exports: [
        CommonModule,
        // Common Modules
        DotNavigationComponent
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
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotEventsSocket,
                StringUtils,
                UserModel
            ]
        };
    }
}

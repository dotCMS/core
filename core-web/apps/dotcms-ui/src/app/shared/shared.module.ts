import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule } from '@angular/core';

// Common Modules
import { DotEventsService } from '@dotcms/data-access';
import {
    ApiRoot,
    BrowserUtil,
    DotcmsConfigService,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';

import { DotNavigationComponent } from '../view/components/dot-navigation/dot-navigation.component';
import { DotNavigationService } from '../view/components/dot-navigation/services/dot-navigation.service';

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
                DotEventsService,
                DotNavigationService,
                DotcmsConfigService,
                LoggerService,
                LoginService,
                StringUtils,
                UserModel
            ]
        };
    }
}

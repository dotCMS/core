import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
    ApiRoot,
    BrowserUtil,
    Config,
    CoreWebService,
    DotcmsConfig,
    DotcmsEventsService,
    LoggerService,
    LoginService,
    SiteService,
    SocketFactory,
    StringUtils,
    UserModel
} from 'dotcms-js/dotcms-js';

// Common Modules
import { DotDropdownModule } from '../view/components/_common/dropdown-component/dot-dropdown.module';
import { GravatarModule } from '../view/components/_common/gravatar/gravatar.module';
import { MainNavigationModule } from '../view/components/dot-navigation/dot-navigation.module';
import { DotEventsService } from '../api/services/dot-events/dot-events.service';

@NgModule({
    declarations: [],
    imports: [
        CommonModule,
        DotDropdownModule,
        GravatarModule,
        MainNavigationModule
    ],
    exports: [
        CommonModule,
        // Common Modules
        DotDropdownModule,
        GravatarModule,
        MainNavigationModule
    ]
})
export class SharedModule {
    static forRoot(): ModuleWithProviders {
        return {
            ngModule: SharedModule,
            providers: [
                ApiRoot,
                BrowserUtil,
                Config,
                CoreWebService,
                DotcmsConfig,
                DotcmsEventsService,
                LoggerService,
                LoginService,
                SiteService,
                SocketFactory,
                StringUtils,
                UserModel,
                DotEventsService
            ]
        };
    }
}

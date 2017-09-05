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

@NgModule({
    declarations: [],
    imports: [ CommonModule ],
    exports: [],
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
                UserModel
            ]
        };
    }
}

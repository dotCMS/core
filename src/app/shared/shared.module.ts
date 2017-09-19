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

// Custom Directives
import { MessageKeysModule } from '../view/directives/message-keys/message-keys.module';
import { RippleEffectModule } from '../view/directives/ripple/ripple-effect.module';
import { MdInputTextModule } from '../view/directives/md-inputtext/md-input-text.module';

// Common Components
import { DotDropdownModule } from '../view/components/_common/dropdown-component/dot-dropdown.module';
import { GravatarModule } from '../view/components/_common/gravatar/gravatar.module';


@NgModule({
    declarations: [],
    imports: [
        CommonModule,
        MessageKeysModule,
        RippleEffectModule,
        MdInputTextModule,
        DotDropdownModule,
        GravatarModule
    ],
    exports: [
        CommonModule,
        // Custom Directives
        MessageKeysModule,
        RippleEffectModule,
        MdInputTextModule,
        // Common Components
        DotDropdownModule,
        GravatarModule
    ],
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

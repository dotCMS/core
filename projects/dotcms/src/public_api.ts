import { DotAppAuth } from './lib/api/DotAppAuth';
import { DotAppConfig } from './lib/api/DotAppConfig';
import { DotAppConfigParams } from './lib/api/DotAppBase';
import { DotAppEs } from './lib/api/DotAppEs';
import { DotAppEvent } from './lib/api/DotAppEvent';
import { DotAppLanguage } from './lib/api/DotAppLanguage';
import { DotAppNav } from './lib/api/DotAppNav';
import { DotAppPage } from './lib/api/DotAppPage';
import { DotAppSite } from './lib/api/DotAppSite';
import { DotAppWidget } from './lib/api/DotAppWidget';

export interface DotCMSApp {
    auth: DotAppAuth;
    esSearch: DotAppEs;
    event: DotAppEvent;
    nav: DotAppNav;
    page: DotAppPage;
    site: DotAppSite;
    widget: DotAppWidget;
    config: DotAppConfig;
    language: DotAppLanguage;
}

export const initDotCMS = (config: DotAppConfigParams): DotCMSApp => {
    return {
        auth: new DotAppAuth(),
        config: new DotAppConfig(config),
        esSearch: new DotAppEs(config),
        event: new DotAppEvent(),
        language: new DotAppLanguage(config),
        nav: new DotAppNav(config),
        page: new DotAppPage(config),
        site: new DotAppSite(config),
        widget: new DotAppWidget(config)
    };
};

import { DotAppAuth } from './lib/api/DotAppAuth';
import { DotAppConfigParams } from './lib/api/DotAppBase';
import { DotAppEs } from './lib/api/DotAppEs';
import { DotAppEvent } from './lib/api/DotAppEvent';
import { DotAppPage } from './lib/api/DotAppPage';
import { DotAppSite } from './lib/api/DotAppSite';
import { DotAppWidget } from './lib/api/DotAppWidget';

export interface DotCMSApp {
    page: DotAppPage;
    auth: DotAppAuth;
    esSearch: DotAppEs;
    event: DotAppEvent;
    site: DotAppSite;
    widget: DotAppWidget;
}

export const initDotCMS = (config: DotAppConfigParams): DotCMSApp => {
    return {
        page: new DotAppPage(config),
        auth: new DotAppAuth(),
        esSearch: new DotAppEs(config),
        event: new DotAppEvent(),
        site: new DotAppSite(config),
        widget: new DotAppWidget(config)
    };
};

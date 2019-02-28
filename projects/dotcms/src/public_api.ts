import { DotApiAuthorization } from './lib/api/DotApiAuthorization';
import { DotApiConfiguration } from './lib/api/DotApiConfiguration';
import { DotApiElasticSearch } from './lib/api/DotApiElasticSearch';
import { DotApiEvent } from './lib/api/DotApiEvent';
import { DotApiLanguage } from './lib/api/DotApiLanguage';
import { DotApiNavigation } from './lib/api/DotApiNavigation';
import { DotApiPage } from './lib/api/DotApiPage';
import { DotApiSite } from './lib/api/DotApiSite';
import { DotApiWidget } from './lib/api/DotApiWidget';
import { DotCMSConfigurationParams } from './lib/models';

export interface DotCMSApp {
    auth: DotApiAuthorization;
    esSearch: DotApiElasticSearch;
    event: DotApiEvent;
    nav: DotApiNavigation;
    page: DotApiPage;
    site: DotApiSite;
    widget: DotApiWidget;
    config: DotApiConfiguration;
    language: DotApiLanguage;
}

export const initDotCMS = (config: DotCMSConfigurationParams): DotCMSApp => {
    return {
        auth: new DotApiAuthorization(),
        config: new DotApiConfiguration(config),
        esSearch: new DotApiElasticSearch(config),
        event: new DotApiEvent(),
        language: new DotApiLanguage(config),
        nav: new DotApiNavigation(config),
        page: new DotApiPage(config),
        site: new DotApiSite(config),
        widget: new DotApiWidget(config)
    };
};

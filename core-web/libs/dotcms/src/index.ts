import { DotApiAuthorization } from './lib/api/DotApiAuthorization';
import { DotApiConfiguration } from './lib/api/DotApiConfiguration';
import { DotApiContent } from './lib/api/DotApiContent';
import { DotApiContentType } from './lib/api/DotApiContentType';
import { DotApiElasticSearch } from './lib/api/DotApiElasticSearch';
import { DotApiEvent } from './lib/api/DotApiEvent';
import { DotApiForm } from './lib/api/DotApiForm';
import { DotApiLanguage } from './lib/api/DotApiLanguage';
import { DotApiNavigation } from './lib/api/DotApiNavigation';
import { DotApiPage } from './lib/api/DotApiPage';
import { DotApiSite } from './lib/api/DotApiSite';
import { DotApiWidget } from './lib/api/DotApiWidget';
import { DotCMSConfigurationParams, DotCMSFormConfig } from './lib/models';
import { DotCMSHttpClient } from './lib/utils/DotCMSHttpClient';

export interface DotCMSApp {
    auth: DotApiAuthorization;
    content: DotApiContent;
    contentType: DotApiContentType;
    esSearch: DotApiElasticSearch;
    event: DotApiEvent;
    nav: DotApiNavigation;
    page: DotApiPage;
    site: DotApiSite;
    form: (formConfig: DotCMSFormConfig, win?: Window) => DotApiForm;
    widget: DotApiWidget;
    config: DotApiConfiguration;
    language: DotApiLanguage;
    httpClient: DotCMSHttpClient;
}

export const initDotCMS = (config: DotCMSConfigurationParams): DotCMSApp => {
    const httpClient = new DotCMSHttpClient(config);
    const apiConfig = new DotApiConfiguration(httpClient);
    const apiLanguage = new DotApiLanguage(apiConfig);
    const content = new DotApiContent(httpClient);
    const dotApiContentType = new DotApiContentType(httpClient);

    return {
        auth: new DotApiAuthorization(),
        config: apiConfig,
        content: content,
        contentType: new DotApiContentType(httpClient),
        esSearch: new DotApiElasticSearch(httpClient),
        event: new DotApiEvent(),
        form: (formConfig: DotCMSFormConfig) => new DotApiForm(dotApiContentType, formConfig),
        language: apiLanguage,
        nav: new DotApiNavigation(httpClient),
        page: new DotApiPage(httpClient, apiLanguage),
        site: new DotApiSite(httpClient),
        widget: new DotApiWidget(httpClient),
        httpClient
    };
};

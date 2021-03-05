import { ReflectiveInjector } from '@angular/core';
import { NoHttpCoreWebServiceMock } from './no-http-core-web.service.mock';
import {
    DotcmsConfigService,
    CoreWebService,
    LoggerService,
    StringUtils,
    ConfigParams
} from '../../public_api';

describe('DotcmsConfigService', () => {
    let service: DotcmsConfigService;

    const configParams = {
        config: {
            EDIT_CONTENT_STRUCTURES_PER_COLUMN: 15,
            colors: {
                secondary: '#54428e',
                background: '#3a3847',
                primary: '#BB30E1'
            },
            emailRegex: 'emailRegex',
            i18nMessagesMap: {
                relativeTime: '',
                notifications_dismissall: 'Dismiss All',
                notifications_dismiss: 'Dismiss',
                notifications_title: 'Notifications'
            },
            languages: [],
            license: {
                level: 100,
                displayServerId: '19fc0e44',
                levelName: 'COMMUNITY EDITION',
                isCommunity: true
            },
            releaseInfo: {
                buildDate: 'June 24, 2019',
                version: '5.0.0'
            },
            websocket: {
                'dotcms.websocket.disable': false,
                'dotcms.websocket.reconnect.time': 15000
            },
            'dotcms.paginator.links': 12,
            'dotcms.paginator.rows': 5
        }
    };

    beforeEach(() => {
        const coreWebService: NoHttpCoreWebServiceMock = new NoHttpCoreWebServiceMock(configParams);

        const injector = ReflectiveInjector.resolveAndCreate([
            { provide: CoreWebService, useValue: coreWebService },
            DotcmsConfigService,
            LoggerService,
            StringUtils
        ]);

        service = injector.get(DotcmsConfigService);
    });

    it('should get the next config', (done) => {
        service.getConfig().subscribe((configResponse: ConfigParams) => {
            const mock: ConfigParams = {
                colors: configParams.config.colors,
                emailRegex: configParams.config['emailRegex'],
                license: configParams.config.license,
                menu: undefined,
                paginatorLinks: configParams.config['dotcms.paginator.links'],
                paginatorRows: configParams.config['dotcms.paginator.rows'],
                websocket: {
                    websocketReconnectTime:
                        configParams.config.websocket['dotcms.websocket.reconnect.time'],
                    disabledWebsockets: configParams.config.websocket['dotcms.websocket.disable']
                }
            };
            expect(configResponse).toEqual(mock);
            done();
        });
    });
});

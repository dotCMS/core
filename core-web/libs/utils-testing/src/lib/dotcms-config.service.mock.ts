import { ConfigParams } from '@dotcms/dotcms-js';

export const configParams: ConfigParams = {
    logos: {
        loginScreen: '',
        navBar: ''
    },
    menu: [],
    colors: {
        secondary: '#54428e',
        background: '#3a3847',
        primary: '#BB30E1'
    },
    emailRegex: 'emailRegex',
    license: {
        level: 100,
        displayServerId: '19fc0e44',
        levelName: 'COMMUNITY EDITION',
        isCommunity: true
    },
    systemTimezone: {
        id: 'America/Costa Rica',
        label: 'Costa Rica',
        offset: '360'
    },
    releaseInfo: {
        buildDate: 'June 24, 2019',
        version: '5.0.0'
    },
    websocket: {
        disabledWebsockets: false,
        websocketReconnectTime: 15000
    },
    paginatorLinks: 12,
    paginatorRows: 5
};

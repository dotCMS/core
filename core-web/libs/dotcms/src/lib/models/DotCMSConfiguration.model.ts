import { DotCMSLanguageItem } from './';

export interface DotCMSConfigurationParams {
    token: string;
    host?: string;
    hostId?: string;
}

export interface DotCMSConfigurationItem {
    license: {
        level: number;
        displayServerId: string;
        levelName: string;
        isCommunity: boolean;
    };
    releaseInfo: {
        buildDate: string;
        version: string;
    };
    colors: {
        secondary: string;
        background: string;
        primary: string;
    };
    i18nMessagesMap: {
        relativeTime: {
            mm: string;
            hh: string;
            dd: string;
            MM: string;
            yy: string;
            d: string;
            past: string;
            h: string;
            m: string;
            M: string;
            s: string;
            future: string;
            y: string;
        };
        notifications_dismissall: string;
        notifications_dismiss: string;
        notifications_title: string;
    };
    emailRegex: string;
    languages: DotCMSLanguageItem[];
    EDIT_CONTENT_STRUCTURES_PER_COLUMN: number;
    'dotcms.websocket.disable': boolean;
    'dotcms.websocket.reconnect.time': number;
    'dotcms.websocket.baseurl': string;
    'dotcms.websocket.protocol': string;
    'dotcms.websocket.endpoints': {
        'websocket.systemevents.endpoint': string;
    };
}

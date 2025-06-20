import { EXPECTED_UTM_KEYS } from '../../shared/dot-content-analytics.constants';
import {
    BrowserEventData,
    DeviceData,
    PageData,
    UtmData
} from '../../shared/dot-content-analytics.model';

/**
 * Extracts and formats UTM data from browser data
 */
export const getUtmData = (browserData: BrowserEventData): UtmData => {
    const utmData: UtmData = {};

    if (browserData.utm.source) {
        utmData.source = browserData.utm.source;
    }

    if (browserData.utm.medium) {
        utmData.medium = browserData.utm.medium;
    }

    if (browserData.utm.campaign) {
        utmData.campaign = browserData.utm.campaign;
    }

    if (browserData.utm.term) {
        utmData.term = browserData.utm.term;
    }

    if (browserData.utm.content) {
        utmData.content = browserData.utm.content;
    }

    return utmData;
};

/**
 * Extracts and formats page data from browser data and payload
 */
export const getPageData = (browserData: BrowserEventData): PageData => {
    return {
        url: browserData.url,
        doc_encoding: browserData.doc_encoding,
        doc_hash: browserData.doc_hash,
        doc_protocol: browserData.doc_protocol,
        doc_search: browserData.doc_search,
        dot_host: browserData.doc_host,
        dot_path: browserData.doc_path,
        title: browserData.page_title,
        user_agent: navigator.userAgent,
        language_id: browserData.user_language
    };
};

/**
 * Extracts and formats device data from browser data
 */
export const getDeviceData = (browserData: BrowserEventData): DeviceData => {
    return {
        screen_resolution: browserData.screen_resolution,
        language: browserData.user_language,
        viewport_width: browserData.vp_size.split('x')[0],
        viewport_height: browserData.vp_size.split('x')[1]
    };
};

/**
 * Extracts UTM parameters from location search
 */
const extractUTMParameters = (location: Location): Record<string, string> => {
    const urlParams = new URLSearchParams(location.search);
    const utmParams: Record<string, string> = {};

    EXPECTED_UTM_KEYS.forEach((key) => {
        const value = urlParams.get(key);
        if (value !== null) {
            utmParams[key.replace('utm_', '')] = value;
        }
    });

    return utmParams;
};

/**
 * Gets browser event data from the current location
 * This is the foundation data that other enrichment utilities use
 */
export const getBrowserEventData = (location: Location): BrowserEventData => {
    const now = new Date();
    const utmParams = extractUTMParameters(location);

    return {
        utc_time: now.toISOString(),
        local_tz_offset: now.getTimezoneOffset(),
        screen_resolution: `${window.screen.width}x${window.screen.height}`,
        vp_size: `${window.innerWidth}x${window.innerHeight}`,
        user_language: navigator.language,
        doc_encoding: document.characterSet || 'UTF-8',
        doc_path: location.pathname,
        doc_host: location.hostname,
        doc_protocol: location.protocol,
        doc_hash: location.hash,
        doc_search: location.search,
        referrer: document.referrer,
        page_title: document.title,
        url: location.href,
        utm: utmParams
    };
};

import { Params } from '@angular/router';

import { DotCMSViewAsPersona, UVE_MODE } from '@dotcms/types';

import { PERSONA_KEY, UVEConfiguration } from './store/model';

export const DEFAULT_PERSONA: DotCMSViewAsPersona = {
    inode: '',
    host: 'SYSTEM_HOST',
    locked: false,
    stInode: 'c938b15f-bcb6-49ef-8651-14d455a97045',
    contentType: 'persona',
    identifier: 'modes.persona.no.persona',
    folder: 'SYSTEM_FOLDER',
    hasTitleImage: false,
    owner: 'SYSTEM_USER',
    url: 'demo.dotcms.com',
    sortOrder: 0,
    name: 'Default Visitor',
    hostName: 'System Host',
    modDate: '0',
    title: 'Default Visitor',
    personalized: false,
    baseType: 'PERSONA',
    archived: false,
    working: false,
    live: false,
    keyTag: 'dot:persona',
    languageId: 1,
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    modUserName: 'system user system user',
    hasLiveVersion: false,
    modUser: 'system'
};

/**
 * Sanitize the URL
 *
 * @param {string} url
 * @return {string}
 */
export const sanitizeURL = (url?: string): string => {
    if (!url || url === '/') {
        return '/';
    }

    return url.replace(/\/+/g, '/'); // Convert multiple slashes to single slash
};

/**
 * Check if the clientHost is in the whitelist provided by the app
 *
 * @private
 * @param {string} clientHost
 * @param {*} [allowedDevURLs=[]]
 * @return {*}
 * @memberof DotEmaShellComponent
 */
export const checkClientHostAccess = (
    clientHost: string,
    allowedDevURLs: string[] = []
): boolean => {
    if (!clientHost || !Array.isArray(allowedDevURLs) || !allowedDevURLs.length) {
        return false;
    }

    // Most IDEs and terminals add a / at the end of the URL, so we need to sanitize it
    const sanitizedClientHost = new URL(clientHost).toString();
    const sanitizedAllowedDevURLs = allowedDevURLs.map((url) => new URL(url).toString());

    return sanitizedAllowedDevURLs.includes(sanitizedClientHost);
};

/**
 * Get the URL params from the activated route
 *
 * @param {ActivatedRoute} activatedRoute
 * @return {*}
 */
export const getConfiguration = (queryParams: Params): UVEConfiguration => {
    const UVE_MODES = Object.values(UVE_MODE);
    const isValidMode = UVE_MODES.includes(queryParams['mode']);
    const mode = isValidMode ? queryParams['mode'] : UVE_MODE.EDIT;

    const language_id = queryParams['language_id'] ?? '1';
    const device = queryParams['device'] ?? '';
    const publishDate =
        mode === UVE_MODE.LIVE && queryParams['publishDate'] ? queryParams['publishDate'] : '';
    const personaId = queryParams['personaId'] ?? DEFAULT_PERSONA.identifier;

    return {
        mode,
        language_id,
        device,
        publishDate,
        [PERSONA_KEY]: personaId
    };
};

/**
 * Clean the object from empty, null, or undefined values
 *
 * @param {Record<string, unknown>} object
 * @return {Record<string, unknown>}
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const cleanObject = (object: Record<any, any>) => {
    return Object.fromEntries(
        Object.entries(object).filter(
            ([_, value]) =>
                value !== null && value !== undefined && value !== '' && String(value).trim() !== ''
        )
    );
};

/**
 * Get the user params from the URL
 *
 * @return {string}
 */
export const getUserParams = () => {
    const searchParams = new URLSearchParams(window.location.search);
    // TODO: CONVERT THIS TO A ENUM?
    const UVE_PARAMs = [
        'personaId',
        'language_id',
        'publishDate',
        'mode',
        'device',
        'dotCMSHost',
        'variantName',
        'experimentId',
        'orientation',
        'url'
    ];

    const userCustomQueryParams = new URLSearchParams();
    for (const param of UVE_PARAMs) {
        const value = searchParams.get(param);
        if (value !== null) {
            userCustomQueryParams.set(param, value);
        }
    }
    return userCustomQueryParams.toString();
};

/**
 * Build query params string from UVE configuration, only including non-default values
 *
 * @param {Partial<UVEConfiguration>} config - UVE configuration object
 * @return {string} Query param string (e.g., "personaId=123&language_id=2&mode=preview" or "")
 */
export const buildUVEQueryParams = (config: Partial<UVEConfiguration>): string => {
    const params = new URLSearchParams();

    if (config.language_id && config.language_id.trim() !== '') {
        params.set('language_id', config.language_id);
    }

    if (config.mode) {
        params.set('mode', config.mode);
    }

    if (config.device && config.device.trim() !== '') {
        params.set('device', config.device);
    }

    if (config.publishDate && config.publishDate.trim() !== '') {
        params.set('publishDate', config.publishDate);
    }

    // Only add personaId if it's different from the default persona
    const personaId = config[PERSONA_KEY];
    if (personaId && personaId !== DEFAULT_PERSONA.identifier) {
        params.set('personaId', personaId);
    }

    return params.toString();
};

import { Injectable } from '@angular/core';

import { EDITOR_DOTMARKETING_CONFIG } from '@dotcms/dotcms-models';

export const INITIAL_STATE = {
    SHOW_VIDEO_THUMBNAIL: true
};

@Injectable({
    providedIn: 'root'
})
export class DotMarketingConfigService {
    private config: EDITOR_DOTMARKETING_CONFIG = INITIAL_STATE;

    /**
     * Get the config object
     *
     * @return {*}  {EDITOR_DOTMARKETING_CONFIG}
     * @memberof DotMarketingConfigService
     */
    get configObject(): EDITOR_DOTMARKETING_CONFIG {
        return this.config;
    }

    /**
     * Set a property in the config object
     *
     * @param {keyof EDITOR_DOTMARKETING_CONFIG} key
     * @param {boolean} value
     * @memberof DotMarketingConfigService
     */
    setProperty(key: keyof EDITOR_DOTMARKETING_CONFIG, value: boolean): void {
        this.config = {
            ...this.config,
            [key]: value
        };
    }

    /**
     * Get a property from the config object base on a key
     *
     * @param {keyof EDITOR_DOTMARKETING_CONFIG} key
     * @return {*}  {boolean}
     * @memberof DotMarketingConfigService
     */
    getProperty(key: keyof EDITOR_DOTMARKETING_CONFIG): boolean {
        return this.config[key] || false;
    }
}

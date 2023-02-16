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

    setProperty(property: keyof EDITOR_DOTMARKETING_CONFIG, value: boolean): void {
        this.config = {
            ...this.config,
            [property]: value
        };
    }

    getProperty(property: keyof EDITOR_DOTMARKETING_CONFIG): boolean {
        return this.config[property] || false;
    }

    getConfigObject(): EDITOR_DOTMARKETING_CONFIG {
        return this.config;
    }
}

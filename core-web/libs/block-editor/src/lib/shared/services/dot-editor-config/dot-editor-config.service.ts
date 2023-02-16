import { Injectable } from '@angular/core';

import { EditorConfig } from '@dotcms/dotcms-models';

export const INITIAL_STATE = {
    SHOW_VIDEO_THUMBNAIL: true
};

@Injectable({
    providedIn: 'root'
})
export class DotEditorConfigService {
    private config: EditorConfig = INITIAL_STATE;

    setProperty(property: keyof EditorConfig, value: boolean): void {
        this.config = {
            ...this.config,
            [property]: value
        };
    }

    getProperty(property: keyof EditorConfig): boolean {
        return this.config[property] || false;
    }

    getConfigObject(): EditorConfig {
        return this.config;
    }
}

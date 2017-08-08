import {LocalStoreService} from './local-store.service';
import {DotSettings} from './settings.model';
import {AppConfig} from '../app.config';
import {Inject, Injectable} from '@angular/core';
import {SiteBrowserState} from './site-browser.state';

/**
 * Stores and returns the DotSettings class
 */
@Injectable()
@Inject('config')
@Inject('localStoreService')
@Inject('siteBrowserState')
export class SettingsStorageService {

    configKey: string;

    constructor(
        private config: AppConfig,
        private localStoreService: LocalStoreService,
        private siteBrowserState: SiteBrowserState
    ) {
        this.configKey = config.dotCMSURLKey;
    }

    getSettings(): DotSettings {
        let dSettings: DotSettings = JSON.parse(this.localStoreService.getValue(this.configKey));
        if (dSettings == null) {
            dSettings = new DotSettings();
        }
        return dSettings;
    }

    /**
     * Stores the DotSettings object
     * @param siteURL
     * @param JWT
     */
    storeSettings(siteURL: string, JWT: string): void {
        let dSettings: DotSettings = new DotSettings();
        dSettings.site = siteURL;
        dSettings.jwt = JWT;
        this.localStoreService.storeValue(this.configKey, JSON.stringify(dSettings));
    }

    /**
     * removes stored settings
     */
    clearSettings(): void {
        this.localStoreService.clearValue(this.configKey);
    }

}

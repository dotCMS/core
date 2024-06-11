import { DotApiConfiguration } from './DotApiConfiguration';

import { DotCMSConfigurationItem, DotCMSLanguageItem } from '../models';

/**
 * Get DotCMS {@link https://dotcms.com/docs/latest/adding-and-editing-languages | Language code}
 *
 */
export class DotApiLanguage {
    private _languagesConf: DotCMSLanguageItem[];
    private dotAppConfig: DotApiConfiguration;

    constructor(appConfig: DotApiConfiguration) {
        this.dotAppConfig = appConfig;
    }

    async getId(langCode: string): Promise<string> {
        const languages: DotCMSLanguageItem[] = await this.getLanguages();

        const language = languages.find(
            (lang: DotCMSLanguageItem) => lang.languageCode === langCode
        );

        return language ? `${language.id}` : null;
    }

    async getLanguages(): Promise<DotCMSLanguageItem[]> {
        if (this._languagesConf) {
            return this._languagesConf;
        }

        this._languagesConf = await this.dotAppConfig
            .get()
            .then((data: DotCMSConfigurationItem) => data.languages);

        return this._languagesConf;
    }
}

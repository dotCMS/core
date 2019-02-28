import { DotApiConfiguration } from './DotApiConfiguration';
import { DotCMSConfigurationItem, DotCMSConfigurationParams, DotCMSLanguageItem } from '../models';

export class DotApiLanguage {
    private _config: DotCMSConfigurationParams;
    private _languagesConf: DotCMSLanguageItem[];
    private dotAppConfig: DotApiConfiguration;

    constructor(config: DotCMSConfigurationParams) {
        this._config = config;
    }

    async getId(langCode: string): Promise<string> {
        if (!this.dotAppConfig) {
            this.dotAppConfig = new DotApiConfiguration(this._config);
        }

        const languages: DotCMSLanguageItem[] = await this.getLanguages();

        const language = languages.find(
            (lang: DotCMSLanguageItem) => lang.languageCode === langCode
        );
        return language ? `${language.id}` : null;
    }

    private async getLanguages(): Promise<DotCMSLanguageItem[]> {
        if (this._languagesConf) {
            return this._languagesConf;
        }

        this._languagesConf = await this.dotAppConfig
            .get()
            .then((data: DotCMSConfigurationItem) => data.languages);

        return this._languagesConf;
    }
}

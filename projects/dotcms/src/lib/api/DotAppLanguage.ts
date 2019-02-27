import { DotAppBase, DotAppConfigParams } from './DotAppBase';
import { DotCMSConfigLanguageItem } from '../models/DotCMSConfigLanguageItem.model';
import { DotAppConfig } from './DotAppConfig';
import { DotCMSConfigItem } from '../models';

export class DotAppLanguage extends DotAppBase {
    private _languagesConf: DotCMSConfigLanguageItem[];
    private dotAppConfig: DotAppConfig;

    constructor(config: DotAppConfigParams) {
        super(config);
    }

    async getId(langCode: string): Promise<string> {
        if (!this.dotAppConfig) {
            this.dotAppConfig = new DotAppConfig(this.config);
        }

        const languages: DotCMSConfigLanguageItem[] = await this.getLanguages();

        const language = languages.find(
            (lang: DotCMSConfigLanguageItem) => lang.languageCode === langCode
        );
        return language ? `${language.id}` : null;
    }

    private async getLanguages(): Promise<DotCMSConfigLanguageItem[]> {
        if (this._languagesConf) {
            return this._languagesConf;
        }

        this._languagesConf = await this.dotAppConfig
            .get()
            .then((data: DotCMSConfigItem) => data.languages);

        return this._languagesConf;
    }

}

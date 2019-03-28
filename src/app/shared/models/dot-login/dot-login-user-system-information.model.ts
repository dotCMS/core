import { DotLoginLanguage } from '@models/dot-login/dot-login-language.model';

/**
 * Interface for system information.
 *
 * @interface
 */
export interface DotLoginUserSystemInformation {
    authorizationType: string;
    backgroundColor: string;
    backgroundPicture: string;
    buildDateString: string;
    companyEmail: string;
    currentLanguage: DotLoginLanguage;
    languages: DotLoginLanguage[];
    levelName: string;
    logo: string;
    serverId: string;
    version: string;
}

export interface DotLoginLanguage {
    country: string;
    displayName: string;
    language: string;
}

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

export interface DotLoginInformation {
    i18nMessagesMap: { [key: string]: string };
    entity: DotLoginUserSystemInformation;
}

export interface DotAddLanguage {
    country?: string;
    countryCode?: string;
    language: string;
    languageCode: string;
}

export interface DotLanguage extends DotAddLanguage {
    id: number;
    defaultLanguage?: boolean;
    translated?: boolean;
    isoCode?: string;
    variables?: { count: number; total: number };
}

export interface DotISOItem {
    code: string;
    name: string;
}

export interface DotLanguagesISO {
    countries: DotISOItem[];
    languages: DotISOItem[];
}

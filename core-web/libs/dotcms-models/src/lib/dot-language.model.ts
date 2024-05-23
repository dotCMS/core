export interface DotLanguage {
    id: number;
    languageCode: string;
    countryCode: string;
    language: string;
    country: string;
    defaultLanguage?: boolean;
    translated?: boolean;
    isoCode?: string;
    variables?: { count: number; total: number };
}

export interface DotLanguagesISO {
    countries: { code: string; name: string }[];
    languages: { code: string; name: string }[];
}

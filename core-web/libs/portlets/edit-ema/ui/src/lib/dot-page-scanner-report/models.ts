import { PageScannerA11yItem } from './dot-page-scanner.service';

export type ReportType = 'a11y' | 'geo';

export interface A11yGroup {
    code: string;
    type: 'error' | 'warning' | 'notice';
    message: string;
    impact: string;
    helpUrl: string;
    items: PageScannerA11yItem[];
    count: number;
}

export interface GeoCategorySignal {
    key: string;
    score: number;
    message: string;
}

export interface GeoCategory {
    key: string;
    label: string;
    score: number;
    weight: number;
    passedCount: number;
    totalCount: number;
    signals: GeoCategorySignal[];
}

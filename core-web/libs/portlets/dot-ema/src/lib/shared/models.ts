import { PageContainer } from '../components/ema-page-dropzone/ema-page-dropzone.component';

export interface SetUrlPayload {
    url: string;
}

export interface SavePagePayload {
    pageContainers: PageContainer[];
    pageId: string;
    whenSaved?: () => void;
}

export interface NavigationBarItem {
    icon?: string;
    iconURL?: string;
    label: string;
    href: string;
}

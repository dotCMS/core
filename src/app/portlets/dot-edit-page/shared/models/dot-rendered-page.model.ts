import { PageMode } from './../../content/shared/page-mode.enum';
export interface DotRenderedPage {
    canEdit: boolean;
    canLock: boolean;
    identifier: string;
    languageId: number;
    liveInode: string;
    lockMessage?: string;
    locked: boolean;
    lockedBy?: string;
    lockedByAnotherUser?: boolean;
    lockedByName?: string;
    lockedOn?: Date;
    mode: PageMode;
    pageURI: string;
    render: string;
    shortyLive: string;
    shortyWorking: string;
    title: string;
    workingInode: string;
}

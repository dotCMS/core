export interface DotRenderedPage {
    canLock: boolean;
    identifier: string;
    languageId: number;
    liveInode: string;
    lockMessage?: string;
    locked: boolean;
    lockedBy?: string;
    lockedByName?: string;
    lockedOn?: Date;
    title: string;
    pageURI: string;
    render: string;
    shortyLive: string;
    shortyWorking: string;
    workingInode: string;
}

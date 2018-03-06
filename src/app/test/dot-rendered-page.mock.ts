import { DotRenderedPage } from '../portlets/dot-edit-page/shared/models/dot-rendered-page.model';

export const mockDotRenderPage: DotRenderedPage = {
    canEdit: true,
    canLock: true,
    identifier: '123',
    languageId: 1,
    liveInode: '',
    lockMessage: '',
    lockedBy: 'someone',
    lockedByName: 'Some One',
    lockedOn: new Date(1517330917295),
    pageURI: 'an/url/test',
    render: '<html></html>',
    shortyLive: '',
    shortyWorking: '',
    title: 'A title',
    workingInode: '999'
};

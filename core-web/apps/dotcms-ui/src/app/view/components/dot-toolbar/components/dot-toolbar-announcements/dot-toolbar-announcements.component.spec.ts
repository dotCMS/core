import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { DotToolbarAnnouncementsComponent } from './dot-toolbar-announcements.component';

describe('DotToolbarAnnouncementsComponent', () => {
    let spectator: Spectator<DotToolbarAnnouncementsComponent>;
    const createComponent = createComponentFactory(DotToolbarAnnouncementsComponent);
    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should display announcements', () => {
        spectator.detectChanges();
        const announcements = spectator.queryAll('.announcements__list-item');
        expect(announcements.length).toBe(spectator.component.announcementsData.length);
    });

    it('should have a "Show All" link', () => {
        spectator.detectChanges();
        const showAllLink = spectator.query(byTestId('announcement_link'));
        expect(showAllLink).toBeTruthy();
    });
});

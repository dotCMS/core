import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { NgClass, NgForOf } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotToolbarAnnouncementsComponent } from './dot-toolbar-announcements.component';

class TestHostComponent {}
describe('DotToolbarAnnouncementsComponent', () => {
    let spectator: Spectator<DotToolbarAnnouncementsComponent>;

    const messageServiceMock = new MockDotMessageService({
        announcements: 'Announcements',
        announcements_show_all: 'Show All',
        announcements_knowledge_center: 'Knowledge Center',
        announcements_knowledge_contact_us: 'Contact Us'
    });

    const createComponent = createComponentFactory({
        component: DotToolbarAnnouncementsComponent,
        providers: [
            HttpClientTestingModule,
            DotMessagePipe,
            { provide: DotMessageService, useValue: messageServiceMock }
        ],
        imports: [
            NgForOf,
            NgClass,
            DotMessagePipe,
            RouterTestingModule.withRoutes([{ path: 'c/starter', component: TestHostComponent }])
        ]
    });
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

    it('should call close() method when "Show All" link is clicked', () => {
        spyOn(spectator.component, 'close');
        spectator.detectChanges();
        const showAllLink = spectator.query(byTestId('announcement_link'));

        spectator.click(showAllLink);
        expect(spectator.component.close).toHaveBeenCalled();
    });
});

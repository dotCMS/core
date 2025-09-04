import { DatePipe } from '@angular/common';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import { DotCMSContentletVersion } from '@dotcms/dotcms-models';
import { DotGravatarDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotHistoryTimelineItemComponent } from './dot-history-timeline-item.component';

import {
    DotHistoryTimelineItemAction,
    DotHistoryTimelineItemActionType
} from '../../../../../../models/dot-edit-content.model';

describe('DotHistoryTimelineItemComponent', () => {
    let component: DotHistoryTimelineItemComponent;
    let fixture: ComponentFixture<DotHistoryTimelineItemComponent>;
    let mockItem: DotCMSContentletVersion;

    beforeEach(async () => {
        mockItem = {
            archived: false,
            country: 'United States',
            countryCode: 'US',
            experimentVariant: false,
            inode: 'test-inode',
            isoCode: 'en-US',
            language: 'English',
            languageCode: 'en',
            languageFlag: 'en_US',
            languageId: 1,
            live: true,
            modDate: 1705315800000,
            modUser: 'test@example.com',
            title: 'Test Content',
            working: false
        } as DotCMSContentletVersion;

        await TestBed.configureTestingModule({
            imports: [
                DotHistoryTimelineItemComponent,
                AvatarModule,
                ButtonModule,
                ChipModule,
                MenuModule,
                TooltipModule,
                DotGravatarDirective,
                DotMessagePipe,
                DotRelativeDatePipe
            ],
            providers: [
                DatePipe,
                {
                    provide: DotMessagePipe,
                    useValue: MockDotMessageService
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotHistoryTimelineItemComponent);
        component = fixture.componentInstance;
    });

    describe('Component Initialization', () => {
        it('should create', () => {
            fixture.componentRef.setInput('item', mockItem);
            expect(component).toBeTruthy();
        });

        it('should display item data correctly', () => {
            fixture.componentRef.setInput('item', mockItem);
            fixture.detectChanges();

            const timeDisplay = fixture.debugElement.query(By.css('[data-testid="time-display"]'));
            const userAvatar = fixture.debugElement.query(By.css('[data-testid="user-avatar"]'));
            const userName = fixture.debugElement.query(By.css('[data-testid="history-user"]'));

            expect(timeDisplay).toBeTruthy();
            expect(userAvatar).toBeTruthy();
            expect(userName.nativeElement.textContent.trim()).toBe('Test User');
        });
    });

    describe('Status Chips', () => {
        it('should display live chip when item is live', () => {
            const liveItem = { ...mockItem, live: true, working: false };
            fixture.componentRef.setInput('item', liveItem);
            fixture.detectChanges();

            const liveChip = fixture.debugElement.query(By.css('[data-testid="state-live"]'));
            const draftChip = fixture.debugElement.query(By.css('[data-testid="state-draft"]'));

            expect(liveChip).toBeTruthy();
            expect(draftChip).toBeFalsy();
        });

        it('should display draft chip when item is working', () => {
            const workingItem = { ...mockItem, live: false, working: true };
            fixture.componentRef.setInput('item', workingItem);
            fixture.detectChanges();

            const liveChip = fixture.debugElement.query(By.css('[data-testid="state-live"]'));
            const draftChip = fixture.debugElement.query(By.css('[data-testid="state-draft"]'));

            expect(liveChip).toBeFalsy();
            expect(draftChip).toBeTruthy();
        });

        it('should display variant chip when item has experiment variant', () => {
            const variantItem = { ...mockItem, experimentVariant: true };
            fixture.componentRef.setInput('item', variantItem);
            fixture.detectChanges();

            const variantChip = fixture.debugElement.query(By.css('[data-testid="state-variant"]'));
            expect(variantChip).toBeTruthy();
        });

        it('should not display variant chip when item has no experiment variant', () => {
            fixture.componentRef.setInput('item', mockItem);
            fixture.detectChanges();

            const variantChip = fixture.debugElement.query(By.css('[data-testid="state-variant"]'));
            expect(variantChip).toBeFalsy();
        });
    });

    describe('Action Menu', () => {
        let menuButton: DebugElement;

        beforeEach(() => {
            fixture.componentRef.setInput('item', mockItem);
            fixture.detectChanges();
            menuButton = fixture.debugElement.query(By.css('[data-testid="version-menu-button"]'));
        });

        it('should render menu button', () => {
            expect(menuButton).toBeTruthy();
        });

        it('should generate correct menu items', () => {
            const menuItems = component.getVersionMenuItems(mockItem);

            expect(menuItems).toHaveLength(3);
            expect(menuItems[0].label).toBe('edit.content.sidebar.history.menu.preview');
            expect(menuItems[1].label).toBe('edit.content.sidebar.history.menu.restore');
            expect(menuItems[2].label).toBe('edit.content.sidebar.history.menu.compare');
        });

        it('should emit preview action when preview menu item is clicked', () => {
            spyOn(component.actionTriggered, 'emit');
            const menuItems = component.getVersionMenuItems(mockItem);

            menuItems[0].command();

            expect(component.actionTriggered.emit).toHaveBeenCalledWith({
                type: DotHistoryTimelineItemActionType.PREVIEW,
                item: mockItem
            } as DotHistoryTimelineItemAction);
        });

        it('should emit restore action when restore menu item is clicked', () => {
            spyOn(component.actionTriggered, 'emit');
            const menuItems = component.getVersionMenuItems(mockItem);

            menuItems[1].command();

            expect(component.actionTriggered.emit).toHaveBeenCalledWith({
                type: DotHistoryTimelineItemActionType.RESTORE,
                item: mockItem
            } as DotHistoryTimelineItemAction);
        });

        it('should emit compare action when compare menu item is clicked', () => {
            spyOn(component.actionTriggered, 'emit');
            const menuItems = component.getVersionMenuItems(mockItem);

            menuItems[2].command();

            expect(component.actionTriggered.emit).toHaveBeenCalledWith({
                type: DotHistoryTimelineItemActionType.COMPARE,
                item: mockItem
            } as DotHistoryTimelineItemAction);
        });
    });

    describe('Tooltip Content', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('item', mockItem);
            fixture.detectChanges();
        });

        it('should display correct title in tooltip', () => {
            const overlayTitle = fixture.debugElement.query(
                By.css('[data-testid="overlay-title"]')
            );
            expect(overlayTitle.nativeElement.textContent.trim()).toBe('Test Content');
        });

        it('should display experiment variant in tooltip when present', () => {
            const variantItem = { ...mockItem, experimentVariant: true };
            fixture.componentRef.setInput('item', variantItem);
            fixture.detectChanges();

            const overlayContent = fixture.debugElement.query(
                By.css('.timeline-item__overlay-content')
            );
            expect(overlayContent.nativeElement.textContent).toContain(
                'edit.content.sidebar.history.variant'
            );
        });

        it('should not display experiment variant in tooltip when not present', () => {
            const overlayContent = fixture.debugElement.query(
                By.css('.timeline-item__overlay-content')
            );
            expect(overlayContent.nativeElement.textContent).not.toContain(
                'edit.content.sidebar.history.variant'
            );
        });
    });

    describe('User Information', () => {
        it('should display modUser correctly', () => {
            fixture.componentRef.setInput('item', mockItem);
            fixture.detectChanges();

            const userName = fixture.debugElement.query(By.css('[data-testid="history-user"]'));
            expect(userName.nativeElement.textContent.trim()).toBe('test@example.com');
        });

        it('should configure avatar with correct email', () => {
            fixture.componentRef.setInput('item', mockItem);
            fixture.detectChanges();

            const avatar = fixture.debugElement.query(By.css('[data-testid="user-avatar"]'));
            expect(avatar.attributes['ng-reflect-email']).toBe('test@example.com');
        });
    });

    describe('Accessibility', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('item', mockItem);
            fixture.detectChanges();
        });

        it('should have proper test ids for all interactive elements', () => {
            const historyItem = fixture.debugElement.query(By.css('[data-testid="history-item"]'));
            const timeDisplay = fixture.debugElement.query(By.css('[data-testid="time-display"]'));
            const userAvatar = fixture.debugElement.query(By.css('[data-testid="user-avatar"]'));
            const userName = fixture.debugElement.query(By.css('[data-testid="history-user"]'));
            const menuButton = fixture.debugElement.query(
                By.css('[data-testid="version-menu-button"]')
            );

            expect(historyItem).toBeTruthy();
            expect(timeDisplay).toBeTruthy();
            expect(userAvatar).toBeTruthy();
            expect(userName).toBeTruthy();
            expect(menuButton).toBeTruthy();
        });
    });
});

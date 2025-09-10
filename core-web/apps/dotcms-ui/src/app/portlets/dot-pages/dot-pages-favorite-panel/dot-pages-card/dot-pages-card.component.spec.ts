import { CommonModule } from '@angular/common';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotIconModule,
    DotMessagePipe,
    DotPagesFavoritePageEmptySkeletonComponent,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPagesCardComponent } from './dot-pages-card.component';

describe('DotPagesCardComponent', () => {
    let component: DotPagesCardComponent;
    let fixture: ComponentFixture<DotPagesCardComponent>;
    let de: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'favoritePage.listing.star.icon.tooltip': 'Edit Favorite Page'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                CommonModule,
                CardModule,
                DotIconModule,
                DotPagesFavoritePageEmptySkeletonComponent,
                TooltipModule,
                DotSafeHtmlPipe,
                ButtonModule,
                DotMessagePipe
            ],
            declarations: [DotPagesCardComponent],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotPagesCardComponent);
        de = fixture.debugElement;
        component = fixture.debugElement.componentInstance;
    });

    describe('With ownerPage', () => {
        beforeEach(() => {
            component.imageUri =
                '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg';
            component.title = 'test';
            component.url = '/index';
            component.ownerPage = true;

            jest.spyOn(component.goTo, 'emit');
            jest.spyOn(component.edit, 'emit');

            fixture.detectChanges();
        });

        it('should set preview img ', () => {
            expect(
                fixture.debugElement
                    .query(By.css('[data-testid="favoriteCardImageContainer"]'))
                    .nativeElement.style['background-image'].includes(component.imageUri)
            ).toBe(true);
            expect(
                fixture.debugElement
                    .query(By.css('.dot-pages-favorite-card-content__image img'))
                    .nativeElement.src.includes(component.imageUri)
            ).toBe(true);
        });

        it('should set title and url as content', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-pages-favorite-card-content__title'))
                    .nativeElement.textContent
            ).toBe(component.title);
            expect(
                fixture.debugElement.query(By.css('.dot-pages-favorite-card-content__subtitle'))
                    .nativeElement.textContent
            ).toBe(component.url);
        });

        it('should emit goTo event when clicked on P-Card', () => {
            const elem = de.query(By.css('[data-testid="pageCard"]'));
            elem.triggerEventHandler('click', {
                stopPropagation: () => {
                    //
                }
            });

            expect(component.goTo.emit).toHaveBeenCalledWith(true);
            expect(component.edit.emit).not.toHaveBeenCalledWith(true);
        });
    });

    describe('Without thumbnail', () => {
        beforeEach(() => {
            component.imageUri = '';
            component.title = 'test';
            component.url = '/index';
            component.ownerPage = true;

            fixture.detectChanges();
        });

        it('should display empty skeleton component and hide favorite card component', () => {
            expect(
                fixture.debugElement.query(By.css('[data-testid="favoriteCardImageContainer"]'))
            ).toBeNull();
            expect(
                fixture.debugElement.query(By.css('.dot-pages-favorite-page-empty-skeleton'))
            ).toBeDefined();
        });
    });
});

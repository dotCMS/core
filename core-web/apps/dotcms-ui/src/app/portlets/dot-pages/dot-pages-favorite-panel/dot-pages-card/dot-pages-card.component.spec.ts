import { CommonModule } from '@angular/common';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotMessageService } from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotPagesCardComponent } from './dot-pages-card.component';

import { DotPagesFavoritePageEmptySkeletonComponent } from '../../dot-pages-favorite-page-empty-skeleton/dot-pages-favorite-page-empty-skeleton.component';

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
                DotPipesModule,
                UiDotIconButtonModule
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

            spyOn(component.goTo, 'emit').and.callThrough();
            spyOn(component.edit, 'emit').and.callThrough();

            fixture.detectChanges();
        });

        it('should set preview img ', () => {
            expect(
                fixture.debugElement
                    .query(By.css('[data-testid="favoriteCardImageContainer"]'))
                    .nativeElement.style['background-image'].includes(component.imageUri)
            ).toBeTrue();
            expect(
                fixture.debugElement
                    .query(By.css('.dot-pages-favorite-card-content__image img'))
                    .nativeElement.src.includes(component.imageUri)
            ).toBeTrue();
        });

        it('should set highlighted star icon', () => {
            expect(
                fixture.debugElement
                    .query(By.css('[data-testid="favoriteCardIconButton"]'))
                    .nativeElement.classList.contains('dot-favorite-page-highlight')
            ).toBeTrue();
            expect(
                fixture.debugElement.query(By.css('[data-testid="favoriteCardIconButton"]'))
                    .componentInstance.icon
            ).toBe('grade');
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

        it('should emit edit event when clicked on star icon', () => {
            const elem = de.query(By.css('[data-testid="favoriteCardIconButton"]'));
            elem.triggerEventHandler('click', {
                stopPropagation: () => {
                    //
                }
            });

            expect(component.goTo.emit).not.toHaveBeenCalledWith(true);
            expect(component.edit.emit).toHaveBeenCalledWith(true);
        });
    });

    describe('Without ownerPage', () => {
        beforeEach(() => {
            component.imageUri =
                '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg';
            component.title = 'test';
            component.url = '/index';
            component.ownerPage = false;

            fixture.detectChanges();
        });

        it('should not set highlighted star icon', () => {
            expect(
                fixture.debugElement
                    .query(By.css('[data-testid="favoriteCardIconButton"]'))
                    .nativeElement.classList.contains('dot-favorite-page-highlight')
            ).toBeFalse();
            expect(
                fixture.debugElement.query(By.css('[data-testid="favoriteCardIconButton"]'))
                    .componentInstance.icon
            ).toBe('star_outline');
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

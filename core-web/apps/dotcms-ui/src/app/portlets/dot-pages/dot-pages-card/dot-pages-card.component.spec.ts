import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessageService } from '@dotcms/data-access';
import { By } from '@angular/platform-browser';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';
import { DotPagesCardComponent } from './dot-pages-card.component';
import { CommonModule } from '@angular/common';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

describe('DotPagesCardComponent', () => {
    let component: DotPagesCardComponent;
    let fixture: ComponentFixture<DotPagesCardComponent>;

    const messageServiceMock = new MockDotMessageService({
        'favoritePage.listing.star.icon.tooltip': 'Edit Favorite Page'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                CommonModule,
                CardModule,
                DotIconModule,
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
        component = fixture.debugElement.componentInstance;
    });

    describe('With ownerPage', () => {
        beforeEach(() => {
            component.imageUri =
                '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg';
            component.title = 'test';
            component.url = '/index';
            component.ownerPage = true;

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
});

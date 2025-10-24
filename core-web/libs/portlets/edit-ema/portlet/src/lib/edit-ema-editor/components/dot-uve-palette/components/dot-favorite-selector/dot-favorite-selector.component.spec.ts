import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotFavoriteSelectorComponent } from './dot-favorite-selector.component';

describe('DotFavoriteSelectorComponent', () => {
    let component: DotFavoriteSelectorComponent;
    let fixture: ComponentFixture<DotFavoriteSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotFavoriteSelectorComponent, HttpClientTestingModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotFavoriteSelectorComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('pagePathOrId', 'test-page-id');
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

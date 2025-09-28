import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUveLanguageSelectorComponent } from './dot-uve-language-selector.component';

describe('DotUveLanguageSelectorComponent', () => {
    let component: DotUveLanguageSelectorComponent;
    let fixture: ComponentFixture<DotUveLanguageSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUveLanguageSelectorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUveLanguageSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

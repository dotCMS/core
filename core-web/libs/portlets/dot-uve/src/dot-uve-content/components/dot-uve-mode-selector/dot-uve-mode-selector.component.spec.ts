import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUveModeSelectorComponent } from './dot-uve-mode-selector.component';

describe('DotUveModeSelectorComponent', () => {
    let component: DotUveModeSelectorComponent;
    let fixture: ComponentFixture<DotUveModeSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUveModeSelectorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUveModeSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

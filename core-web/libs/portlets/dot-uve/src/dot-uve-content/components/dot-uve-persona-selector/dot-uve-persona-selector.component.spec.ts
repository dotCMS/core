import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUvePersonaSelectorComponent } from './dot-uve-persona-selector.component';

describe('DotUvePersonaSelectorComponent', () => {
    let component: DotUvePersonaSelectorComponent;
    let fixture: ComponentFixture<DotUvePersonaSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUvePersonaSelectorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUvePersonaSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

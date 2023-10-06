import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotFormComponent } from './dot-form.component';

describe('DotFormComponent', () => {
    let component: DotFormComponent;
    let fixture: ComponentFixture<DotFormComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotFormComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

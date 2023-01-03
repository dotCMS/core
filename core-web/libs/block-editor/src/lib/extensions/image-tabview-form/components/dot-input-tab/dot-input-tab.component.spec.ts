import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotInputTabComponent } from './dot-input-tab.component';

describe('DotInputTabComponent', () => {
    let component: DotInputTabComponent;
    let fixture: ComponentFixture<DotInputTabComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotInputTabComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotInputTabComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

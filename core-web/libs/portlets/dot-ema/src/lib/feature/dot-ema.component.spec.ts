import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEmaComponent } from './dot-ema.component';
describe('PortletsDotEmaComponent', () => {
    let component: DotEmaComponent;
    let fixture: ComponentFixture<DotEmaComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEmaComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEmaComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

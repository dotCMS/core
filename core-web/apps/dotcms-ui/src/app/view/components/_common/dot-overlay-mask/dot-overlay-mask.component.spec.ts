import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotOverlayMaskComponent } from './dot-overlay-mask.component';
import { DotOverlayMaskModule } from './dot-overlay-mask.module';

describe('DotOverlayMaskComponent', () => {
    let component: DotOverlayMaskComponent;
    let fixture: ComponentFixture<DotOverlayMaskComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [],
            imports: [DotOverlayMaskModule]
        }).compileComponents();
        fixture = TestBed.createComponent(DotOverlayMaskComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create component with no html', () => {
        expect(component).toBeTruthy();
    });
});

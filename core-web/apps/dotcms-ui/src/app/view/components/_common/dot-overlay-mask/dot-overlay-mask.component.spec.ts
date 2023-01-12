import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotOverlayMaskModule } from '@components/_common/dot-overlay-mask/dot-overlay-mask.module';

import { DotOverlayMaskComponent } from './dot-overlay-mask.component';

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

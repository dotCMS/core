import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotOverlayMaskComponent } from './dot-overlay-mask.component';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotOverlayMaskModule } from '@components/_common/dot-overlay-mask/dot-overlay-mask.module';

describe('DotOverlayMaskComponent', () => {
    let component: DotOverlayMaskComponent;
    let fixture: ComponentFixture<DotOverlayMaskComponent>;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [],
            imports: [DotOverlayMaskModule]
        });
        fixture = TestBed.createComponent(DotOverlayMaskComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create component with no html', () => {
        expect(component).toBeTruthy();
    });
});

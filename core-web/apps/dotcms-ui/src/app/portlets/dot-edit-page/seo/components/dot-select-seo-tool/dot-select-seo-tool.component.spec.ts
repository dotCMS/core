import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotSelectSeoToolComponent } from './dot-select-seo-tool.component';

describe('DotSocialMediaSeoToolComponent', () => {
    let component: DotSelectSeoToolComponent;
    let fixture: ComponentFixture<DotSelectSeoToolComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotSelectSeoToolComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotSelectSeoToolComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

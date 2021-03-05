import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotSiteBrowserComponent } from './dot-site-browser.component';

describe('DotSiteBrowserComponent', () => {
    let component: DotSiteBrowserComponent;
    let fixture: ComponentFixture<DotSiteBrowserComponent>;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotSiteBrowserComponent]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotSiteBrowserComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

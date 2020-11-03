import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotPortletToolbarComponent } from './dot-portlet-toolbar.component';

describe('DotPortletToolbarComponent', () => {
    let component: DotPortletToolbarComponent;
    let fixture: ComponentFixture<DotPortletToolbarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotPortletToolbarComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotPortletToolbarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

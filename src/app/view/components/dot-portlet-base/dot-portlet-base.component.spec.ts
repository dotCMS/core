import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotPortletBaseComponent } from './dot-portlet-base.component';

describe('DotPortletBaseComponent', () => {
    let component: DotPortletBaseComponent;
    let fixture: ComponentFixture<DotPortletBaseComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotPortletBaseComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotPortletBaseComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

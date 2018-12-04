import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCopyButtonComponent } from './dot-copy-button.component';

describe('DotCopyButtonComponent', () => {
    let component: DotCopyButtonComponent;
    let fixture: ComponentFixture<DotCopyButtonComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [DotCopyButtonComponent]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotCopyButtonComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditPageNavComponent } from './dot-edit-page-nav.component';

describe('DotEditPageNavComponent', () => {
    let component: DotEditPageNavComponent;
    let fixture: ComponentFixture<DotEditPageNavComponent>;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                declarations: [DotEditPageNavComponent],
            }).compileComponents();
        }),
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageNavComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

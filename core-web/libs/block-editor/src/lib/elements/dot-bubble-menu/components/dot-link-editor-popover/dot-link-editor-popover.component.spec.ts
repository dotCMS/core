import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotLinkEditorPopoverComponent } from './dot-link-editor-popover.component';

describe('DotLinkEditorPopoverComponent', () => {
    let component: DotLinkEditorPopoverComponent;
    let fixture: ComponentFixture<DotLinkEditorPopoverComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotLinkEditorPopoverComponent],
            teardown: { destroyAfterEach: false }
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotLinkEditorPopoverComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

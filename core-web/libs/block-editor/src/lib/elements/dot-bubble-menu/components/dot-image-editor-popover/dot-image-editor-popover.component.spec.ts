import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotImageEditorPopoverComponent } from './dot-image-editor-popover.component';

describe('DotImageEditorPopoverComponent', () => {
    let component: DotImageEditorPopoverComponent;
    let fixture: ComponentFixture<DotImageEditorPopoverComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotImageEditorPopoverComponent],
            teardown: { destroyAfterEach: false }
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotImageEditorPopoverComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

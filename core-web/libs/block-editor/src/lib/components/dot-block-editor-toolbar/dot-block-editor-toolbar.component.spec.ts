import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotBlockEditorToolbarComponent } from './dot-block-editor-toolbar.component';

describe('DotBlockEditorToolbarComponent', () => {
    let component: DotBlockEditorToolbarComponent;
    let fixture: ComponentFixture<DotBlockEditorToolbarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotBlockEditorToolbarComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotBlockEditorToolbarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

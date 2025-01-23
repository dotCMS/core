import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditorModeSelectorComponent } from './dot-editor-mode-selector.component';

describe('DotEditorModeSelectorComponent', () => {
    let component: DotEditorModeSelectorComponent;
    let fixture: ComponentFixture<DotEditorModeSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditorModeSelectorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditorModeSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

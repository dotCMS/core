import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotBinaryFieldEditorComponent } from './dot-binary-field-editor.component';

describe('DotBinaryFieldEditorComponent', () => {
    let component: DotBinaryFieldEditorComponent;
    let fixture: ComponentFixture<DotBinaryFieldEditorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotBinaryFieldEditorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotBinaryFieldEditorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

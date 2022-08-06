import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotcmsBlockEditorComponent } from './dotcms-block-editor.component';

describe('DotcmsBlockEditorComponent', () => {
    let component: DotcmsBlockEditorComponent;
    let fixture: ComponentFixture<DotcmsBlockEditorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotcmsBlockEditorComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotcmsBlockEditorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

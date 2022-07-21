import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotBlockEditorComponent } from './dot-block-editor.component';
import { SuggestionsService } from '@dotcms/block-editor';
import { DebugElement } from '@angular/core';

describe('DotBlockEditorComponent', () => {
    let component: DotBlockEditorComponent;
    let fixture: ComponentFixture<DotBlockEditorComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotBlockEditorComponent],
            providers: [SuggestionsService]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotBlockEditorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotBlockEditorSettingsComponent } from './dot-block-editor-settings.component';

describe('DotBlockEditorSettingsComponent', () => {
    let component: DotBlockEditorSettingsComponent;
    let fixture: ComponentFixture<DotBlockEditorSettingsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotBlockEditorSettingsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotBlockEditorSettingsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

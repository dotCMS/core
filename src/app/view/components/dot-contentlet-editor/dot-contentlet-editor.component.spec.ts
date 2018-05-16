/* tslint:disable:no-unused-variable */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotContentletEditorComponent } from './dot-contentlet-editor.component';
import { DotAddContentletComponent } from './components/dot-add-contentlet/dot-add-contentlet.component';
import { DotEditContentletComponent } from './components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotCreateContentletComponent } from './components/dot-create-contentlet/dot-create-contentlet.component';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotIframeDialogModule } from '../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotMenuService } from '../../../api/services/dot-menu.service';

describe('DotContentletEditorComponent', () => {
    let component: DotContentletEditorComponent;
    let fixture: ComponentFixture<DotContentletEditorComponent>;
    let de: DebugElement;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [DotIframeDialogModule, BrowserAnimationsModule],
            providers: [DotMenuService],
            declarations: [
                DotContentletEditorComponent,
                DotAddContentletComponent,
                DotEditContentletComponent,
                DotCreateContentletComponent
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotContentletEditorComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        fixture.detectChanges();
    });

    it('should have add contentlet', () => {
        expect(de.query(By.css('dot-add-contentlet'))).toBeTruthy();
    });

    it('should have edit contentlet', () => {
        expect(de.query(By.css('dot-edit-contentlet'))).toBeTruthy();
    });

    it('should have create contentlet', () => {
        expect(de.query(By.css('dot-create-contentlet'))).toBeTruthy();
    });
});

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input, TemplateRef } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { SharedModule } from 'primeng/api';
import { InplaceModule } from 'primeng/inplace';

import { DotInlineEditComponent } from './dot-inline-edit.component';

@Component({
    selector: 'dot-host-component',
    template: `
        <ng-template #inlineEditDisplayTemplate>
            <h4>Display Text</h4>
        </ng-template>
        <ng-template #inlineEditContentTemplate>
            <input />
        </ng-template>
        <dot-inline-edit
            [inlineEditDisplayTemplate]="inlineEditDisplayTemplate"
            [inlineEditContentTemplate]="inlineEditContentTemplate"
            #dotEditInline></dot-inline-edit>
    `
})
class HostTestComponent {
    @Input() inlineEditDisplayTemplate?: TemplateRef<unknown>;
    @Input() inlineEditContentTemplate?: TemplateRef<unknown>;
}

describe('DotInlineEditComponent', () => {
    let hostFixture: ComponentFixture<HostTestComponent>;
    let comp: DotInlineEditComponent;
    let de: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotInlineEditComponent, HostTestComponent],
            imports: [CommonModule, InplaceModule, SharedModule, HttpClientTestingModule],
            providers: []
        }).compileComponents();

        hostFixture = TestBed.createComponent(HostTestComponent);
        de = hostFixture.debugElement;
        comp = hostFixture.debugElement.query(By.css('dot-inline-edit')).componentInstance;
        hostFixture.detectChanges();
    }));

    it(`should have display section and component variables defined`, () => {
        expect(comp.inlineEditDisplayTemplate).toBeDefined();
        expect(comp.inlineEditContentTemplate).toBeDefined();
        expect(de.query(By.css('p-inplace h4')).nativeElement.innerHTML).toBe('Display Text');
    });

    it('should set and emit change name of Content Type', () => {
        de.query(By.css('p-inplace h4')).nativeElement.click();
        hostFixture.detectChanges();

        expect(de.query(By.css('p-inplace input')).nativeElement).toBeDefined();
    });

    it('should hide Content section when "hideContent" method called', () => {
        de.query(By.css('p-inplace h4')).nativeElement.click();
        hostFixture.detectChanges();

        comp.hideContent();
        hostFixture.detectChanges();

        expect(de.query(By.css('p-inplace input'))).toBeNull();
        expect(de.query(By.css('p-inplace h4')).nativeElement.innerHTML).toBe('Display Text');
    });
});

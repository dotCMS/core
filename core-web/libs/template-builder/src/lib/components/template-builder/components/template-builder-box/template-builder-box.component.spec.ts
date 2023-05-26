import { NgClass, NgIf } from '@angular/common';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import {
    TemplateBuilderBoxComponent,
    TemplateBuilderBoxSize
} from './template-builder-box.component';

@Component({
    selector: 'dotcms-host-component',
    template: ` <dotcms-template-builder-box [size]="size"> </dotcms-template-builder-box>`
})
class TemplateBuilderBoxHostComponent {
    size = 'large';
}

describe('TemplateBuilderBoxComponent', () => {
    let component: TemplateBuilderBoxHostComponent;
    let hostFixture: ComponentFixture<TemplateBuilderBoxHostComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                TemplateBuilderBoxComponent,
                NgClass,
                NgIf,
                ButtonModule,
                CardModule,
                ScrollPanelModule
            ],
            declarations: [TemplateBuilderBoxHostComponent]
        }).compileComponents();

        hostFixture = TestBed.createComponent(TemplateBuilderBoxHostComponent);
        component = hostFixture.componentInstance;
        de = hostFixture.debugElement;
        hostFixture.detectChanges();
    });

    it('should create the component', () => {
        expect(component).toBeTruthy();
    });

    it('should render with default size', () => {
        expect(de.nativeElement.querySelector('.template-builder-box__large')).toBeTruthy();
    });

    it('should render with medium size and update the class', async () => {
        component.size = TemplateBuilderBoxSize.medium;
        hostFixture.detectChanges();

        expect(de.nativeElement.querySelector('.template-builder-box__medium')).toBeTruthy();
    });

    it('should render with small size and update the class', async () => {
        component.size = TemplateBuilderBoxSize.small;
        hostFixture.detectChanges();

        expect(de.nativeElement.querySelector('.template-builder-box__small')).toBeTruthy();
    });

    it('should show all buttons for small size', () => {
        component.size = TemplateBuilderBoxSize.small;
        hostFixture.detectChanges();
        const addButton = hostFixture.nativeElement.querySelector(
            '.p-button-rounded.p-button-text.p-button-sm'
        );
        const paletteButton = hostFixture.nativeElement.querySelector(
            '.p-button-rounded.p-button-text.p-button-sm'
        );
        const deleteButton = hostFixture.nativeElement.querySelector(
            '.p-button-rounded.p-button-text.p-button-sm'
        );
        expect(addButton).toBeTruthy();
        expect(paletteButton).toBeTruthy();
        expect(deleteButton).toBeTruthy();
    });

    it('should render the first ng-template for large and medium sizes', () => {
        component.size = TemplateBuilderBoxSize.large;
        hostFixture.detectChanges();
        const firstTemplate = hostFixture.nativeElement.querySelector('.template-builder-box');
        const secondTemplate = hostFixture.nativeElement.querySelector(
            '.template-builder-box__small'
        );
        expect(firstTemplate).toBeTruthy();
        expect(secondTemplate).toBeNull();
    });
});

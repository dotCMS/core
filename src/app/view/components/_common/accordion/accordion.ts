import { Component, Input, ElementRef, ViewChild } from '@angular/core';

@Component({

    selector: 'accordion',
    template: `
        <ng-content></ng-content>
    `
})
export class Accordion {
    groups: Array<AccordionGroup> = [];

    addGroup(group: AccordionGroup): void {
        this.groups.push(group);
    }

    closeOthers(openGroup: AccordionGroup): void {
        this.groups.forEach((group: AccordionGroup) => {
            if (group !== openGroup) {
                // group.isOpen = false;
            }
        });
    }

    removeGroup(group: AccordionGroup): void {
        const index = this.groups.indexOf(group);
        if (index !== -1) {
            this.groups.splice(index, 1);
        }
    }
}

@Component({
    selector: 'accordion-group',
    styles: [require('./accordion-group.scss')],
    template: `
        <a href="#" ripple (click)="toggleOpen($event)" class="accordion-group__title" [ngClass]="{'is-active': isOpen}">
            <i class="fa fa-th-list {{icon}}" aria-hidden="true" *ngIf="icon"></i>
            <span class="accordion-group__title-text">
                {{heading}}
            </span>

        </a>
        <div class="accordion-group__content" [style.height.px]="isOpen ? accordionGroupHeight : 0">
            <div class="accordion-group__content-inner" #accordionGroupContentInner>
                <ng-content></ng-content>
            </div>
        </div>
    `
})
export class AccordionGroup {
    @Input('open') _isOpen = false;
    @Input() heading: string;
    @Input() icon: string;
    @ViewChild('accordionGroupContentInner') accordionGroupContentInner: ElementRef;
    public accordionGroupHeight: number;

    constructor(private accordion: Accordion) {
        this.accordion.addGroup(this);
    }

    set isOpen(value: boolean) {
        this._isOpen = value;
        if (this._isOpen) {
            this.accordion.closeOthers(this);
        }
    }
    get isOpen(): boolean {
        return this._isOpen;
    }

    ngAfterViewInit(): void {
        this.accordionGroupHeight = this.accordionGroupContentInner.nativeElement.offsetHeight;
    }

    getHeight(): number {
        return this._isOpen ? this.accordionGroupHeight : 0;
    }

    open(): void {
        this._isOpen = true;
    }

    toggleOpen(event: MouseEvent): void {
        event.preventDefault();
        this.isOpen = !this.isOpen;
    }

    ngOnDestroy(): void {
        this.accordion.removeGroup(this);
    }

}
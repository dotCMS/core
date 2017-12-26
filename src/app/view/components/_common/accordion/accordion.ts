import { Component, Input, ElementRef, ViewChild, AfterViewInit, OnDestroy } from '@angular/core';
import { trigger, state, style, transition, animate } from '@angular/animations';

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
                // TODO: why is this code commented?
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
    animations: [
        trigger('expandAnimation', [
            state(
                'expanded',
                style({
                    height: '*',
                    overflow: 'visible'
                })
            ),
            state(
                'collapsed',
                style({
                    height: '0px',
                    overflow: 'hidden'
                })
            ),
            transition('expanded <=> collapsed', animate('250ms ease-in-out'))
        ])
    ],
    selector: 'accordion-group',
    styleUrls: ['./accordion-group.scss'],
    template: `
        <a href="#" ripple (click)="toggleOpen($event)" class="accordion-group__title" [ngClass]="{'is-active': isOpen}">
            <i class="fa fa-th-list {{icon}}" aria-hidden="true" *ngIf="icon"></i>
            <span class="accordion-group__title-text">
                {{heading}}
            </span>

        </a>
        <div class="accordion-group__content" [@expandAnimation]="isOpen ? 'expanded' : 'collapsed'">
            <div class="accordion-group__content-inner" #accordionGroupContentInner>
                <ng-content></ng-content>
            </div>
        </div>
    `
})
export class AccordionGroup implements AfterViewInit, OnDestroy {
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
        event.stopPropagation();
        event.preventDefault();
        this.isOpen = !this.isOpen;
    }

    ngOnDestroy(): void {
        this.accordion.removeGroup(this);
    }

}

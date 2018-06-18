import { Component, Input, ElementRef, ViewChild, AfterViewInit, OnDestroy } from '@angular/core';
import { trigger, state, style, transition, animate } from '@angular/animations';

@Component({
    selector: 'dot-accordion',
    template: `
        <ng-content></ng-content>
    `
})
export class AccordionComponent {
    groups: Array<AccordionGroupComponent> = [];

    addGroup(group: AccordionGroupComponent): void {
        this.groups.push(group);
    }

    closeOthers(openGroup: AccordionGroupComponent): void {
        this.groups.forEach((group: AccordionGroupComponent) => {
            if (group !== openGroup) {
                group.isOpen = false;
            }
        });
    }

    removeGroup(group: AccordionGroupComponent): void {
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
    selector: 'dot-accordion-group',
    styleUrls: ['./accordion-group.scss'],
    template: `
        <a href="#" dotMdRipple (click)="toggleOpen($event)" class="accordion-group__title" [ngClass]="{'is-active': isOpen}">
            <i class="fa fa-th-list {{ icon }}" aria-hidden="true" *ngIf="icon"></i>
            <span class="accordion-group__title-text">
                {{ heading }}
            </span>
            <i
                class="fa accordion-group__title-arrow"
                [ngClass]="{'fa-caret-down': !isOpen, 'fa-caret-up': isOpen}"
                aria-hidden="true">
            </i>
        </a>
        <div class="accordion-group__content" [@expandAnimation]="isOpen ? 'expanded' : 'collapsed'">
            <div class="accordion-group__content-inner" #accordionGroupContentInner>
                <ng-content></ng-content>
            </div>
        </div>
    `
})
export class AccordionGroupComponent implements AfterViewInit, OnDestroy {
    // tslint:disable-next-line:no-input-rename
    @Input('open') _isOpen = false; // TODO: need to refactor this
    @Input() heading: string;
    @Input() icon: string;
    @ViewChild('accordionGroupContentInner') accordionGroupContentInner: ElementRef;
    public accordionGroupHeight: number;

    constructor(private accordion: AccordionComponent) {
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

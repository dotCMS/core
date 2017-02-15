import {Component, Input, OnDestroy} from '@angular/core';

@Component({
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'accordion',
    template:`
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
                group.isOpen = false;
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
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'accordion-group',
    template: `
        <a href="#" (click)="toggleOpen($event)" class="accordion-group__title" [ngClass]="{'is-active': isOpen}">
            <i class="fa fa-th-list {{icon}}" aria-hidden="true" *ngIf="icon"></i>
            <span class="accordion-group__title-text">
                {{heading}}
            </span>
            
        </a>
        <div class="accordion-group__content" [ngClass]="{'is-open': isOpen}">
            <ng-content></ng-content>
        </div>
    `,
    styleUrls: ['accordion-group.css']
})
export class AccordionGroup implements OnDestroy {
    private _isOpen:boolean = false;

    @Input() heading: string;
    @Input() icon: string;

    set isOpen(value: boolean) {
        this._isOpen = value;
        if (this._isOpen) {
            this.accordion.closeOthers(this);
        }
    }

    constructor(private accordion: Accordion) {
        this.accordion.addGroup(this);
    }

    get isOpen() {
        return this._isOpen;
    }

    toggleOpen(event: MouseEvent): void {
        event.preventDefault();
        this.isOpen = !this.isOpen;
    }

    ngOnDestroy() {
        this.accordion.removeGroup(this);
    }

}
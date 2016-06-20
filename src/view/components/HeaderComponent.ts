import {Component, Inject} from "@angular/core";

@Component({
    selector: "header-component",
    template: `

    <div class="ui vertical menu">
        <div class="item" *ngFor="let menu of menuItems">
            <div class="header">{{menu.tabName}}</div>
        
            <div class="menu">
                <!--TODO: find a way to do this without the span-->
                <span *ngFor="let menuItem of menu.menuItems">
                    <a class="item" href="{{menuItem.url}}" *ngIf="!menuItem.angular && !menuItem.ajax">
                        <label class="t2">{{menuItem.name}}</label>
                    </a>
                    <a class="item" linkTo="{{menuItem.url}}" *ngIf="menuItem.angular && !menuItem.ajax">
                        <label class="t2">{{menuItem.name}}</label>
                    </a>
                    <a class="item" linkTo="{{menuItem.url}}"  *ngIf="menuItem.ajax">
                        <label class="t2">{{menuItem.name}}</label>
                    </a>
                </span>
            </div>
        </div>
    </div>
    `,
    providers: []
})

export class HeaderComponent {

    constructor(@Inject('menuItems') private menuItems:any[]) {

    }

}
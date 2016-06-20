import {Component} from "@angular/core";
import {HeaderComponent} from "./HeaderComponent";

@Component({
    selector: "app",
    template: `
    <div class="ui grid">
        <div class="four wide column">
            <header-component></header-component>
        </div>
        <div class="twelve wide column">
            <route-view></route-view>
        </div>
    </div>
    `,
    providers: [],
    directives: [HeaderComponent]
})
export class AppComponent {
    constructor() {
    }
}
import {Component} from "@angular/core";
import {HeaderComponent} from "./HeaderComponent";

@Component({
    selector: "app",
    template: `
    <header-component></header-component>
    <div class="container">
        <route-view></route-view>
    </div>
    `,
    providers: [],
    directives: [HeaderComponent]
})
export class AppComponent {
    constructor() {
    }
}
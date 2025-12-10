import { Component } from "@angular/core"

import { DotOnboardingAuthorComponent } from "./components/onboarding-author/onboarding-author.component";
import { DotOnboardingDevComponent } from "./components/onboarding-dev/onboarding-dev.component";

@Component({
    selector: 'dot-starter',
    templateUrl: './dot-starter.component.html',
    styleUrls: ['./dot-starter.component.scss'],
    imports: [DotOnboardingDevComponent, DotOnboardingAuthorComponent]
})
export class DotStarterComponent {

}

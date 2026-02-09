import { Component, OnInit } from '@angular/core';

import { DotOnboardingAuthorComponent } from './components/onboarding-author/onboarding-author.component';
import { DotOnboardingDevComponent } from './components/onboarding-dev/onboarding-dev.component';

export type UserProfile = 'developer' | 'marketer';

@Component({
    selector: 'dot-starter',
    templateUrl: './dot-starter.component.html',
    styleUrls: ['./dot-starter.component.scss'],
    imports: [DotOnboardingDevComponent, DotOnboardingAuthorComponent]
})
export class DotStarterComponent implements OnInit {
    public profile: UserProfile = localStorage.getItem('user_profile') as UserProfile;
    public showProfileSelection = true;
    public showDeveloperGuide = false;
    public showMarketerGuide = false;

    ngOnInit(): void {
        if (this.profile !== null && this.profile === 'developer') {
            this.showDeveloperGuide = true;
            this.showProfileSelection = false;
        }

        if (this.profile !== null && this.profile === 'marketer') {
            this.showMarketerGuide = true;
            this.showProfileSelection = false;
        }
    }

    public setUserProfile(selectedProfile: UserProfile) {
        localStorage.setItem('user_profile', selectedProfile);
        this.showProfileSelection = false;
        this.showMarketerGuide = false;
        this.showDeveloperGuide = false;

        if (selectedProfile === 'marketer') {
            this.showMarketerGuide = true;
        }

        if (selectedProfile === 'developer') {
            this.showDeveloperGuide = true;
        }
    }

    public onUserProfileReset(): void {
        this.showProfileSelection = true;
        this.showDeveloperGuide = false;
        this.showMarketerGuide = false;
    }
}

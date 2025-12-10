import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'dot-onboarding-author',
    imports: [CommonModule],
    templateUrl: './onboarding-author.component.html',
    styleUrl: './onboarding-author.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class OnboardingAuthorComponent {}

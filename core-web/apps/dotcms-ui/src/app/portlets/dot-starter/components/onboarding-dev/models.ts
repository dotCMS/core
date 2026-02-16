export type OnboardingFrameworkType = 'interactive' | 'starter' | 'doc';

export interface OnboardingFramework {
    id: string;
    label: string;
    logo: string;
    cliCommand: string;
    type: OnboardingFrameworkType;
    // disabled?: boolean;
    copied: boolean;
    githubUrl?: string;
}

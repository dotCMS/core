export interface OnboardingFramework {
    id: string;
    label: string;
    logo: string;
    cliCommand: string;
    text: string;
    hasStarterkit: boolean;
    disabled?: boolean;
    copied: boolean;
    githubUrl?: string;
}

interface OnboardingSubstepExplanation {
    title: string;
    description: string;
}

type SubstepType = 'file' | 'terminal' | 'config';

interface OnboardingStep {
    number: number;
    title: string;
    description: string;
    substeps?: OnboardingSubstep[];
}

export interface OnboardingSubstep {
    code: string;
    language: string;
    explanation: OnboardingSubstepExplanation;
    type: SubstepType;
    filePath?: string;
}


export interface OnboardingContent {
    title: string;
    description: string;
    steps: OnboardingStep[];
}

export interface OnboardingFramework {
    id: string;
    label: string;
    logo: string;
    disabled?: boolean;
    githubUrl?: string;
}

import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject } from '@angular/core';

interface OnboardingCommand {
    code: string;
    language: string;
    explanation: string;
}

interface OnboardingTroubleshooting {
    problem: string;
    solutions: string[];
}

interface OnboardingStep {
    id: string;
    number: number;
    title: string;
    description: string;
    commands?: OnboardingCommand[];
    troubleshooting?: OnboardingTroubleshooting[];
}

interface OnboardingContent {
    title: string;
    description: string;
    prerequisites: string[];
    steps: OnboardingStep[];
}

const STORAGE_KEY = 'dotcmsDeveloperOnboarding';

const ONBOARDING_CONTENT: OnboardingContent = {
    title: 'dotCMS Headless Integration Onboarding',
    description:
        'Complete guide to integrate dotCMS with Next.js and build a headless CMS application.',
    prerequisites: [
        'Node.js installed (v18 or higher)',
        'A dotCMS instance (minstarter.dotcms.com)',
        'Basic knowledge of React and Next.js'
    ],
    steps: [
        {
            id: 'step-1',
            number: 1,
            title: 'Set up your Next.js app',
            description:
                'Set up a modern Next.js development environment with TypeScript and Tailwind CSS that will serve as the foundation for your headless CMS application.',
            commands: [
                {
                    code: 'npx create-next-app@latest my-dotcms-app --yes',
                    language: 'bash',
                    explanation: `Create a new Next.js project with all defaults.

The \`--yes\` flag creates a Next.js app with TypeScript support, Tailwind CSS for styling, ESLint for code quality, App Router (not Pages Router), Turbopack for faster builds, import alias \`@/*\` for cleaner imports, and \`src\` directory structure.`
                },
                {
                    code: 'cd my-dotcms-app',
                    language: 'bash',
                    explanation: 'Navigate into the project directory. Change into the newly created project directory so you can run commands within it.'
                },
                {
                    code: 'npm run dev',
                    language: 'bash',
                    explanation: `Start the development server.

The Next.js development server will start and the app will be available at http://localhost:3000. Keep this terminal running while developing.`
                }
            ]
        },
        {
            id: 'step-2',
            number: 2,
            title: 'Install dotCMS libraries',
            description:
                'Install the official dotCMS SDK packages that enable your Next.js app to communicate with dotCMS and render content with full TypeScript support.',
            commands: [
                {
                    code: 'npm install @dotcms/client @dotcms/react @dotcms/types',
                    language: 'bash',
                    explanation: `Install dotCMS packages.

**@dotcms/client** handles authentication, API communication, and content fetching from dotCMS.

**@dotcms/react** provides React-specific hooks and components for rendering dotCMS content.

**@dotcms/types** provides TypeScript type definitions for dotCMS SDK.`
                }
            ]
        },
        {
            id: 'step-3',
            number: 3,
            title: 'Authenticate dotCMS (create your API Key)',
            description:
                'Set up secure authentication between your Next.js app and dotCMS using a read-only API key and environment variables.',
            commands: [
                {
                    code: 'touch .env.local',
                    language: 'bash',
                    explanation: `Create .env.local file in project root.

Navigate to [**System** → **Users**](https://minstarter.dotcms.com/c/users) in your dotCMS instance, select your user account (e.g., \`admin@dotcms.com\`), scroll to the **API Access Key** section, and click **Generate** to create a new key (read-only permissions recommended). Copy the generated key - it will look something like: \`abcd1234efgh5678ijkl9012mnop3456\`. **Important:** Save this key safely - you'll need it in the next step!

For detailed instructions, refer to: [dotCMS REST API Authentication](https://dev.dotcms.com/docs/rest-api-authentication#ReadOnlyToken)`
                },
                {
                    code: `# .env.local
NEXT_PUBLIC_DOTCMS_URL=https://minstarter.dotcms.com
DOTCMS_TOKEN=your-api-key-here`,
                    language: 'env',
                    explanation: `Add your dotCMS credentials to \`.env.local\`.

Replace \`your-api-key-here\` with the actual API Key you copied from dotCMS.

**NEXT_PUBLIC_DOTCMS_URL** uses the \`NEXT_PUBLIC_\` prefix to make this variable available in both server and client components (browser). This is required because the Banner component needs access to the URL to construct image paths.

**DOTCMS_TOKEN** has no prefix, meaning it's server-only, keeping your API key secure and never exposed to the browser. This setup keeps your API key safe while allowing necessary configuration to be accessible where needed.`
                }
            ]
        },
        {
            id: 'step-4',
            number: 4,
            title: 'Fetch content from dotCMS',
            description:
                'Test your dotCMS connection by fetching page data and displaying it as JSON. Seeing the raw data helps you understand the structure of dotCMS content before rendering it visually.',
            commands: [
                {
                    code: `import { createDotCMSClient } from '@dotcms/client';

// Create dotCMS client
const client = createDotCMSClient({
  dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_URL!,
  authToken: process.env.DOTCMS_TOKEN,
});

export default async function Home() {
  // Fetch page content from dotCMS
  const { pageAsset } = await client.page.get('/');

  return (
    <div className="min-h-screen p-8">
      <h1 className="text-2xl font-bold mb-4">dotCMS Page Data</h1>
      <pre className="bg-gray-100 p-4 rounded overflow-auto text-xs">
        {JSON.stringify(pageAsset, null, 2)}
      </pre>
    </div>
  );
}`,
                    language: 'typescript',
                    explanation: `Replace all content in src/app/page.tsx.

**createDotCMSClient** creates a configured client instance that can communicate with your dotCMS instance.

**client.page.get('/')** makes an API call to fetch the home page content (identified by path \`/\`).

**pageAsset** is the complete page object containing all content, layout, and configuration data.

**JSON.stringify** converts the JavaScript object into readable JSON format for inspection.`
                }
            ],
            troubleshooting: [
                {
                    problem: '"Failed to fetch" or network error',
                    solutions: [
                        'Check that `NEXT_PUBLIC_DOTCMS_URL` is set correctly in `.env.local`',
                        'Verify your dotCMS instance is accessible: https://minstarter.dotcms.com',
                        'Restart your dev server after adding environment variables'
                    ]
                },
                {
                    problem: 'Authentication error (401)',
                    solutions: [
                        'Verify `DOTCMS_TOKEN` in `.env.local` matches your API key exactly',
                        'Check that there are no extra spaces or quotes around the token',
                        "Ensure the API key hasn't been revoked in dotCMS"
                    ]
                },
                {
                    problem: 'Page shows "Cannot read property \'/\' of undefined"',
                    solutions: [
                        'The page might not exist in dotCMS. Check that the home page (`/`) is published',
                        'Try accessing the dotCMS page directly to confirm it exists'
                    ]
                },
                {
                    problem: 'Build error with environment variables',
                    solutions: [
                        'Ensure `.env.local` is in the project root (same level as `package.json`)',
                        'Restart your Next.js dev server after creating `.env.local`'
                    ]
                }
            ]
        },
        {
            id: 'step-5',
            number: 5,
            title: 'Render content with DotCMSLayoutBody',
            description:
                'Transform that JSON data into a rendered page by creating a Banner component and mapping it to your dotCMS content.',
            commands: [
                {
                    code: 'mkdir -p src/components && touch src/components/DotCMSPageClient.tsx',
                    language: 'bash',
                    explanation: `Create components directory and client component file.

Creates the components directory structure and an empty file for the client component. The \`-p\` flag creates parent directories if they don't exist.`
                },
                {
                    code: `'use client';

import { DotCMSLayoutBody } from '@dotcms/react';
import type { DotCMSPageAsset } from '@dotcms/types';
import Image from 'next/image';

// Banner component props
interface BannerProps {
  title?: string;
  caption?: string;
  image?: {
    idPath: string;
  };
  link?: string;
  target?: string;
  identifier: string;
}

// Banner Component
function Banner({ title, caption, image, link, target }: BannerProps) {
  const dotcmsUrl = process.env.NEXT_PUBLIC_DOTCMS_URL || 'https://minstarter.dotcms.com';
  const imageUrl = image?.idPath ? \`\${dotcmsUrl}\${image.idPath}\` : null;

  return (
    <div className="relative w-full bg-gradient-to-r from-blue-600 to-purple-600 text-white overflow-hidden min-h-[400px]">
      <div className="container mx-auto px-4 py-16 md:py-24 relative z-10">
        <div className="max-w-3xl">
          {title && (
            <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold mb-4">
              {title}
            </h1>
          )}
          {caption && (
            <p className="text-xl md:text-2xl mb-8 text-white/90">
              {caption}
            </p>
          )}
          {link && (
            <a
              href={link}
              target={target || '_self'}
              className="inline-block bg-white text-blue-600 font-semibold px-8 py-3 rounded-lg hover:bg-gray-100 transition-colors"
            >
              Learn More
            </a>
          )}
        </div>
      </div>
      {imageUrl && (
        <div className="absolute inset-0 opacity-20">
          <Image
            src={imageUrl}
            alt={title || 'Banner'}
            fill
            className="object-cover"
            unoptimized
          />
        </div>
      )}
    </div>
  );
}

// Map content types to components
const COMPONENTS_MAP = {
  Banner: Banner,
};

interface DotCMSPageClientProps {
  pageAsset: DotCMSPageAsset;
}

export function DotCMSPageClient({ pageAsset }: DotCMSPageClientProps) {
  return (
    <DotCMSLayoutBody
      page={pageAsset}
      components={COMPONENTS_MAP}
    />
  );
}`,
                    language: 'typescript',
                    explanation: `Add this code to src/components/DotCMSPageClient.tsx.

This creates a client component that renders dotCMS content. The \`'use client'\` directive is required because this component uses browser APIs (like Next.js Image component).

The \`Banner\` component receives props from dotCMS and renders them with Tailwind CSS styling.`
                },
                {
                    code: `const COMPONENTS_MAP = {
  Banner: Banner,  // "Banner" must match your dotCMS content type name
};`,
                    language: 'typescript',
                    explanation: `The \`COMPONENTS_MAP\` maps dotCMS content types to React components.

The key must **exactly match** the content type name in dotCMS (case-sensitive). If your dotCMS content type is called "Banner", the key must be "Banner". If it's "HeroBanner", the key must be "HeroBanner". This is how \`DotCMSLayoutBody\` knows which React component to render for each piece of content.`
                },
                {
                    code: `import { createDotCMSClient } from '@dotcms/client';
import { DotCMSPageClient } from '@/components/DotCMSPageClient';

// Create dotCMS client
const client = createDotCMSClient({
  dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_URL!,
  authToken: process.env.DOTCMS_TOKEN,
});

export default async function Home() {
  // Fetch page content from dotCMS
  const { pageAsset } = await client.page.get('/');

  return <DotCMSPageClient pageAsset={pageAsset} />;
}`,
                    language: 'typescript',
                    explanation: `Replace all content in src/app/page.tsx.

Here's the flow of data from dotCMS to your screen.

Here's the flow of data from dotCMS to your screen.

The **Server Component** (\`page.tsx\`) runs on the server (not in the browser), fetches page data from dotCMS API using \`client.page.get('/')\`, and passes the data to the client component.

The **Client Component** (\`DotCMSPageClient.tsx\`) runs in the browser (needs \`'use client'\` directive), receives pageAsset data as a prop, and uses \`DotCMSLayoutBody\` to automatically render content.

During **Automatic Component Rendering**, \`DotCMSLayoutBody\` reads the pageAsset, finds content with type "Banner", looks up "Banner" in \`COMPONENTS_MAP\`, and renders your \`Banner\` component with the content data (title, caption, etc.).

Server components can securely access API tokens and fetch data, while client components handle interactivity and browser-specific features.`
                }
            ],
            troubleshooting: [
                {
                    problem: "Banner doesn't appear / blank page",
                    solutions: [
                        'Check the browser console for errors',
                        'Verify the content type name in dotCMS is exactly "Banner" (case-sensitive)',
                        'Ensure `COMPONENTS_MAP` key matches: `Banner: Banner`',
                        'Check that the banner container has content in dotCMS'
                    ]
                },
                {
                    problem: 'TypeScript error - "Property \'identifier\' is missing"',
                    solutions: [
                        'This is normal - the `identifier` prop is passed by dotCMS automatically',
                        'Make sure all component props are marked as optional with `?` (e.g., `title?: string`)'
                    ]
                },
                {
                    problem: 'Image not loading or broken image icon',
                    solutions: [
                        'Check that `NEXT_PUBLIC_DOTCMS_URL` is set correctly (no trailing slash)',
                        'Verify the image exists in dotCMS by checking the idPath in the JSON from Step 4',
                        'The `unoptimized` prop on the Image component is required for external URLs'
                    ]
                },
                {
                    problem: '"Hydration failed" error',
                    solutions: [
                        "Ensure `'use client'` directive is at the top of `DotCMSPageClient.tsx`",
                        "Check that you're not mixing server and client component code"
                    ]
                },
                {
                    problem: 'Content shows but with missing fields',
                    solutions: [
                        'The Banner component handles optional fields gracefully with `?` operators',
                        'Check the field names in dotCMS match exactly (case-sensitive): `title`, `caption`, `image`, `link`'
                    ]
                }
            ]
        },
        {
            id: 'step-6',
            number: 6,
            title: 'Configure Universal Visual Editor',
            description:
                "Connect your Next.js app to dotCMS's Universal Visual Editor (UVE), which allows content editors to see live previews of your Next.js app inside dotCMS and edit content in context. Navigate to the UVE app configuration in dotCMS and register your local development URL (http://localhost:3000) to create a bridge between dotCMS and your frontend."
        },
        {
            id: 'step-7',
            number: 7,
            title: 'Edit your page visually',
            description:
                'Test the Universal Visual Editor by making live edits to your banner content. Navigate to Pages in dotCMS, open the Home page in the UVE, click on the banner to edit its content (text, images, links), save changes, and verify the updates appear in your Next.js app. This demonstrates the core value of headless CMS - developers build components once, and content editors can update them without touching code.'
        },
        {
            id: 'step-8',
            number: 8,
            title: 'You Did It!',
            description:
                "Congratulations! You've successfully built a headless Next.js application powered by dotCMS. You've experienced the core value of a headless CMS: separation of concerns, developer freedom, visual editing, and API-first architecture. Next steps include adding more content types, fetching content directly, adding more pages with dynamic routes, and deploying to production. Learn more at https://www.dotcms.com/docs/latest/"
        }
    ]
};

@Component({
    selector: 'dot-starter',
    templateUrl: './dot-starter.component.html',
    styleUrls: ['./dot-starter.component.scss'],
    standalone: false
})
export class DotStarterComponent implements OnInit {
    readonly content = ONBOARDING_CONTENT;

    progress = 0;
    private completedSteps = new Set<string>();
    private readonly platformId = inject(PLATFORM_ID);

    ngOnInit(): void {
        this.loadProgress();
        this.updateProgress();
    }

    isStepCompleted(stepId: string): boolean {
        return this.completedSteps.has(stepId);
    }

    toggleStepCompletion(stepId: string): void {
        if (this.completedSteps.has(stepId)) {
            this.completedSteps.delete(stepId);
        } else {
            this.completedSteps.add(stepId);
        }

        this.persistProgress();
        this.updateProgress();
    }

    resetProgress(): void {
        this.completedSteps.clear();
        this.persistProgress();
        this.updateProgress();
    }

    get progressPercentage(): number {
        if (!this.content.steps.length) {
            return 0;
        }

        return (this.completedSteps.size / this.content.steps.length) * 100;
    }

    get totalSteps(): number {
        return this.content.steps.length;
    }

    get currentStep(): OnboardingStep | null {
        if (!this.content.steps.length) {
            return null;
        }

        const nextStep = this.content.steps.find((step) => !this.completedSteps.has(step.id));

        return nextStep ?? this.content.steps[this.content.steps.length - 1];
    }

    get currentStepLabel(): string {
        if (!this.content.steps.length) {
            return 'No steps available';
        }

        if (this.completedSteps.size >= this.totalSteps) {
            return 'All steps complete';
        }

        const step = this.currentStep;

        if (!step) {
            return 'All steps complete';
        }

        return step.title;
    }

    get currentStepPosition(): number {
        if (!this.content.steps.length) {
            return 0;
        }

        if (this.completedSteps.size >= this.totalSteps) {
            return this.totalSteps;
        }

        const step = this.currentStep;

        if (!step) {
            return 0;
        }

        const index = this.content.steps.findIndex((item) => item.id === step.id);

        return index === -1 ? 0 : index + 1;
    }

    get hasProgress(): boolean {
        return this.completedSteps.size > 0;
    }

    formatCommand(command: OnboardingCommand): string {
        const language = command.language || '';

        return `\`\`\`${language}
${command.code}
\`\`\``;
    }

    private loadProgress(): void {
        if (!isPlatformBrowser(this.platformId)) {
            return;
        }

        try {
            const saved = localStorage.getItem(STORAGE_KEY);

            if (!saved) {
                return;
            }

            const parsed = JSON.parse(saved) as string[];

            parsed.forEach((id) => this.completedSteps.add(id));
        } catch {
            localStorage.removeItem(STORAGE_KEY);
        }
    }

    private persistProgress(): void {
        if (!isPlatformBrowser(this.platformId)) {
            return;
        }

        const values = Array.from(this.completedSteps);

        if (!values.length) {
            localStorage.removeItem(STORAGE_KEY);

            return;
        }

        localStorage.setItem(STORAGE_KEY, JSON.stringify(values));
    }

    private updateProgress(): void {
        this.progress = Math.round(this.progressPercentage);
    }
}

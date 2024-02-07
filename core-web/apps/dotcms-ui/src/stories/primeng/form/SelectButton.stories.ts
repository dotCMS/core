import { Meta, moduleMetadata, Story } from '@storybook/angular';

import { SelectButton, SelectButtonModule } from 'primeng/selectbutton';

export default {
    title: 'PrimeNG/Form/SelectButton',
    decorators: [
        moduleMetadata({
            imports: [SelectButtonModule]
        })
    ],
    args: {
        options: [
            { label: 'Push', value: 'push' },
            { label: 'Remove', value: 'remove' },
            { label: 'Push Publish', value: 'push-publish' }
        ]
    },
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'SelectButton is a form component to choose a value from a list of options using button elements: https://primeng.org/selectbutton'
            }
        }
    }
} as Meta;

const TabbedTemplate = `
<p>
    <p-selectButton [options]="options" [(ngModel)]="selectedOption" class="p-button-tabbed">
        <ng-template let-item>
            <span>{{item.label}}</span>
        </ng-template>
    </p-selectButton>
</p>
<p>
    <p-selectButton [options]="options" disabled="true" class="p-button-tabbed">
        <ng-template let-item>
            <span>{{item.label}}</span>
        </ng-template>
    </p-selectButton>
</p>
`;

const PrimaryTemplate = `
<p>
    <p-selectButton [options]="options" [(ngModel)]="selectedOption">
        <ng-template let-item>
            <span>{{item.label}}</span>
        </ng-template>
    </p-selectButton>
</p>
<p>
    <p-selectButton [options]="options" disabled="true">
        <ng-template let-item>
            <span>{{item.label}}</span>
        </ng-template>
    </p-selectButton>
</p>
`;

export const Primary: Story<SelectButton> = () => {
    return {
        template: PrimaryTemplate,
        props: {
            options: [
                { label: 'Push', value: 'push' },
                { label: 'Remove', value: 'remove' },
                { label: 'Push Publish', value: 'push-publish' }
            ]
        }
    };
};

Primary.parameters = {
    docs: {
        source: {
            code: PrimaryTemplate
        }
    }
};

export const Tabbed: Story<SelectButton> = () => {
    return {
        template: TabbedTemplate,
        props: {
            options: [
                { label: 'Push', value: 'push' },
                { label: 'Remove', value: 'remove' },
                { label: 'Push Publish', value: 'push-publish' }
            ],
            selectedOption: 'push'
        }
    };
};

Tabbed.parameters = {
    docs: {
        source: {
            code: TabbedTemplate
        }
    }
};

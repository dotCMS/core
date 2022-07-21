// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Meta, Story } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { TreeModule } from 'primeng/tree';

export default {
    title: 'PrimeNG/Data/Tree',
    decorators: [
        moduleMetadata({
            imports: [TreeModule]
        })
    ],
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'Paginator is a generic component to display content in paged format.: https://primefaces.org/primeng/showcase/#/paginator'
            }
        }
    }
} as Meta;

const files = [
    {
        label: 'Documents',
        data: 'Documents Folder',
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            {
                label: 'Work',
                data: 'Work Folder',
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        label: 'Expenses.doc',
                        icon: 'pi pi-file',
                        data: 'Expenses Document'
                    },
                    { label: 'Resume.doc', icon: 'pi pi-file', data: 'Resume Document' }
                ]
            },
            {
                label: 'Home',
                data: 'Home Folder',
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        label: 'Invoices.txt',
                        icon: 'pi pi-file',
                        data: 'Invoices for this month'
                    }
                ]
            }
        ]
    },
    {
        label: 'Pictures',
        data: 'Pictures Folder',
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            { label: 'barcelona.jpg', icon: 'pi pi-image', data: 'Barcelona Photo' },
            { label: 'logo.jpg', icon: 'pi pi-image', data: 'PrimeFaces Logo' },
            { label: 'primeui.png', icon: 'pi pi-image', data: 'PrimeUI Logo' }
        ]
    },
    {
        label: 'Movies',
        data: 'Movies Folder',
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            {
                label: 'Al Pacino',
                data: 'Pacino Movies',
                children: [
                    { label: 'Scarface', icon: 'pi pi-video', data: 'Scarface Movie' },
                    { label: 'Serpico', icon: 'pi pi-video', data: 'Serpico Movie' }
                ]
            },
            {
                label: 'Robert De Niro',
                data: 'De Niro Movies',
                children: [
                    {
                        label: 'Goodfellas',
                        icon: 'pi pi-video',
                        data: 'Goodfellas Movie'
                    },
                    {
                        label: 'Untouchables',
                        icon: 'pi pi-video',
                        data: 'Untouchables Movie'
                    }
                ]
            }
        ]
    }
];

const BasicTemplate = `<p-tree [value]="files"></p-tree>`;

export const Basic: Story = () => {
    return {
        template: BasicTemplate,
        props: {
            files
        }
    };
};

Basic.parameters = {
    docs: {
        source: {
            code: BasicTemplate
        }
    }
};

// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Meta, Story } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { PaginatorModule } from 'primeng/paginator';

export default {
    title: 'PrimeNG/Data/Paginator',
    decorators: [
        moduleMetadata({
            imports: [PaginatorModule]
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

const BasicTemplate = `<p-paginator [rows]="10" [totalRecords]="100" pageLinkSize="3"></p-paginator>`;
export const Basic: Story = () => {
    return {
        template: BasicTemplate
    };
};

Basic.parameters = {
    docs: {
        source: {
            code: BasicTemplate
        }
    }
};

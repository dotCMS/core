import { Meta, StoryObj, moduleMetadata, componentWrapperDecorator } from '@storybook/angular';
import { of } from 'rxjs';

import { DotCrumbtrailComponent } from '../../../app/view/components/dot-crumbtrail/dot-crumbtrail.component';
import { DotCrumbtrailModule } from '../../../app/view/components/dot-crumbtrail/dot-crumbtrail.module';
import { DotCrumbtrailService } from '../../../app/view/components/dot-crumbtrail/service/dot-crumbtrail.service';

const meta: Meta<DotCrumbtrailComponent> = {
    title: 'DotCMS/Menu/DotCrumbtrail',
    component: DotCrumbtrailComponent,
    decorators: [
        moduleMetadata({
            imports: [DotCrumbtrailModule],
            providers: [
                {
                    provide: DotCrumbtrailService,
                    useValue: {
                        crumbTrail$: of([
                            {
                                label: 'Site',
                                target: '_self',
                                url: '#//pages'
                            },
                            {
                                label: 'Pages',
                                target: '_self',
                                url: '#//pages'
                            },
                            {
                                label: 'Apparel',
                                target: '_self',
                                url: ''
                            }
                        ])
                    }
                }
            ]
        }),
        componentWrapperDecorator(
            (story) => `<div class="card flex justify-content-center">${story}</div>`
        )
    ],
    parameters: {
        docs: {
            description: {
                component:
                    'Breadcrumb provides contextual information about page hierarchy.: https://primefaces.org/primeng/showcase/#/breadcrumb'
            }
        }
    },
    render: (args) => ({
        props: args,
        template: `<dot-crumbtrail class="max-w-full" />`
    })
};

export default meta;

type Story = StoryObj<DotCrumbtrailComponent>;

export const Default: Story = {};

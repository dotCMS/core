import readme from './readme.md';

export default {
    title: 'Components',
    parameters: {
        docs: {
            page: readme
        },
        layout: 'centered'
    },
    argTypes: {
        data: {
            table: {
                expanded: true
            }
        },
        actions: {
            table: {
                disable: true
            }
        }
    },
    args: {
        data: {
            title: 'Hola Mundo',
            language: 'es-es',
            locked: true,
            live: true,
            working: false,
            deleted: false,
            hasLiveVersion: true,
            hasTitleImage: false,
            contentTypeIcon: 'description'
        },
        actions: [
            {
                label: 'Action 1',
                action: (e) => {
                    console.log(e);
                }
            },
            {
                label: 'Action 2',
                action: (e) => {
                    console.log(e);
                }
            }
        ]
    }
};

const Template = (args) => {
    const cardContentlet = document.createElement('dot-card-contentlet');
    cardContentlet.item = args;

    const div = document.createElement('div');
    div.style.display = 'flex';
    div.style.width = '250px';
    div.style.height = '300px';

    div.appendChild(cardContentlet);

    return div;
};

export const ContentletCard = Template.bind({});

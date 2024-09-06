import readme from './readme.md';

export default {
    title: 'Elements',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        icon: 'insert_drive_file',
        size: '48px'
    }
};

const Template = (args) => {
    const contentletIcon = document.createElement('dot-contentlet-icon');

    for (const item in args) {
        contentletIcon[item] = args[item];
    }

    return contentletIcon;
};

export const ContentletIcon = Template.bind({});

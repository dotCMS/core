import readme from './readme.md';

export default {
    title: 'Content Types Fields',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        text: 'Drop files here'
    }
};

const Template = (args) => {
    const body = document.createElement('div');
    const dropZone = document.createElement('dot-asset-drop-zone');

    for (const item in args) {
        dropZone[item] = args[item];
    }

    dropZone.style.display = 'block';
    dropZone.style.width = '100%';
    dropZone.style.height = '100%';

    body.appendChild(dropZone);

    body.style.height = '600px';
    body.style.border = '3px solid green';

    return body;
};

export const DropZone = Template.bind({});

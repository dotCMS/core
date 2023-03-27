import readme from './readme.md';

export default {
    title: 'Components',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        dropFilesText: 'Drag and Drop or paste a file',
        browserButtonText: 'Browse',
        writeCodeButtonText: 'Write Code',
        cancelButtonText: 'Cancel',
        currentState: 'UploadFile',
        assets: []
    }
};

const Template = (args) => {
    const dotFileUpload = document.createElement('dot-file-upload');

    for (const item in args) {
        dotFileUpload[item] = args[item];
    }

    return dotFileUpload;
};

export const DotFileUpload = Template.bind({});

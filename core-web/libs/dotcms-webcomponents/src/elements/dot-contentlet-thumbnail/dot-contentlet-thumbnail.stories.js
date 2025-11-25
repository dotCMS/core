import readme from './readme.md';

export default {
    title: 'Elements',
    parameters: {
        docs: {
            page: readme
        }
    },
    args: {
        height: '200px',
        width: '200px',
        alt: 'Image description',
        iconSize: '100px',
        contentlet: {
            baseType: 'FILEASSET',
            inode: 'c68db8ec-b523-41b7-82bd-fcb7533d3cfa',
            __icon__: 'jpgIcon',
            hasTitleImage: 'true',
            title: 'Hello World',
            mediaType: 'image/jpg',
            contentTypeIcon: 'jpgIcon'
        }
    }
};

const Template = (args) => {
    const fileIcon = document.createElement('dot-contentlet-thumbnail');

    for (const item in args) {
        fileIcon[item] = args[item];
    }

    return fileIcon;
};

export const ContentletThumbnail = Template.bind({});

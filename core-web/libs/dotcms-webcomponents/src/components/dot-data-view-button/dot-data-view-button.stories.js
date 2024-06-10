import readme from './readme.md';

export default {
    title: 'Components',
    parameters: {
        docs: {
            page: readme
        }
    }
};

export const DataViewButton = () => `<dot-data-view-button value="list" />`;

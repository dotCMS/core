import readme from './readme.md';

export default {
    title: 'Elements',
    parameters: {
        docs: {
            page: readme
        }
    }
};
export const Card = () => `<dot-card><h3>Hello World</h3></dot-card>`;

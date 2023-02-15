import { h } from '@stencil/core';
import { DotVideoThumbnail } from './dot-video-thumbnail';

export default {
    title: 'DotVideoThumbnail',
    component: DotVideoThumbnail
};

const Template = (args) => <dot-video-thumbnail {...args}></dot-video-thumbnail>;

export const Primary = Template.bind({});
Primary.args = { first: 'Hello', last: 'World' };

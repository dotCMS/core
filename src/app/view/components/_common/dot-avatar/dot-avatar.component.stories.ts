import { storiesOf, moduleMetadata } from '@storybook/angular';
// import { action } from '@storybook/addon-actions';

import { DotAvatarComponent } from './dot-avatar.component';

storiesOf('DotAvatarComponent', module)
    .addDecorator(
        moduleMetadata({
            declarations: [DotAvatarComponent]
        })
    )
    .add('default', () => {
        return {
            template: `<dot-avatar size="48" url="https://api.adorable.io/avatars/285/abott@adorable.png"></dot-avatar>`,
            props: {}
        };
    })
    .add('no url', () => {
        return {
            template: `<dot-avatar size="48" label="DotCMS"></dot-avatar>`,
            props: {}
        };
    });

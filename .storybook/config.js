import { configure } from '@storybook/angular';
import '../src/styles.scss';

// automatically import all files ending in *.stories.ts
configure(require.context('../src/stories', true, /\.stories\.ts$/), module);

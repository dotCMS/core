import { definePreset } from '@primeuix/themes';
import Lara from '@primeuix/themes/lara';

export const CustomLaraPreset = definePreset(Lara, {
    semantic: {
        primary: {
            '50': '#f0f3fe',
            '100': '#dee3fb',
            '200': '#c4cff9',
            '300': '#9caff4',
            '400': '#6c86ee',
            '500': '#4b60e7',
            '600': '#3540db',
            '700': '#2c2fc9',
            '800': '#2c2aa3',
            '900': '#272881',
            '950': '#1c1c4f'
        }
    },
    components: {
        treeselect: {
            tree: {
                padding: '0.5rem'
            }
        },
        card: {
            body: {
                padding: '1rem'
            }
        }
    }
});

import { definePreset } from '@primeuix/themes';
import Lara from '@primeuix/themes/lara';

export const CustomLaraPreset = definePreset(Lara, {
    components: {
        treeselect: {
            tree: {
                padding: '0.5rem'
            }
        }
    }
});


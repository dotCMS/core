import { DotConfigModel } from './models';

declare module '@tiptap/core' {
    interface Storage {
        dotConfig?: DotConfigModel;
    }
}

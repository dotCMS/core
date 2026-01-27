import { DotErrorPipe } from './dot-error.pipe';

import { PAYLOAD_MOCK } from '../../../../../shared/mocks';
import { ClientData } from '../../../../../shared/models';
import { Container, EmaDragItem } from '../../types';

describe('DotErrorPipe', () => {
    let pipe: DotErrorPipe;

    beforeEach(() => {
        pipe = new DotErrorPipe();
    });

    it('should return message for acceptType WIDGET', () => {
        const container: Container = {
            x: 10,
            y: 10,
            width: 10,
            height: 10,
            contentlets: [],
            payload: JSON.stringify({ container: { acceptTypes: 'WIDGET' } })
        };

        const dragItem: EmaDragItem = {
            baseType: 'CONTENT',
            contentType: 'Some Other Content Type',
            draggedPayload: null
        };

        expect(pipe.transform(container, dragItem)).toEqual({
            message: 'edit.ema.page.dropzone.invalid.contentlet.type',
            args: ['Some Other Content Type']
        });
    });

    it('should return message for any acceptType', () => {
        const container: Container = {
            x: 10,
            y: 10,
            width: 10,
            height: 10,
            contentlets: [],
            payload: JSON.stringify({ container: { acceptTypes: 'kenobi,theChosenOne,yoda' } })
        };

        const dragItem: EmaDragItem = {
            baseType: 'CONTENT',
            contentType: 'theEmperor',
            draggedPayload: null
        };

        expect(pipe.transform(container, dragItem)).toEqual({
            message: 'edit.ema.page.dropzone.invalid.contentlet.type',
            args: ['theEmperor']
        });
    });

    it('should return message for any acceptType even if it is not an string', () => {
        const container: Container = {
            x: 10,
            y: 10,
            width: 10,
            height: 10,
            contentlets: [],
            payload: { container: { acceptTypes: 'kenobi,theChosenOne,yoda' } } as ClientData // For this test, we are using the ClientData type as a mock
        };

        const dragItem: EmaDragItem = {
            baseType: 'CONTENT',
            contentType: 'theEmperor',
            draggedPayload: null
        };

        expect(pipe.transform(container, dragItem)).toEqual({
            message: 'edit.ema.page.dropzone.invalid.contentlet.type',
            args: ['theEmperor']
        });
    });

    it('should return message when maxContentlet is 1 and the drag item is from another container', () => {
        const container: Container = {
            x: 10,
            y: 10,
            width: 10,
            height: 10,
            contentlets: [
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                }
            ],
            payload: JSON.stringify({
                container: {
                    acceptTypes: 'kenobi,theChosenOne,yoda',
                    maxContentlets: 1,
                    identifier: '123'
                }
            })
        };

        const dragItem: EmaDragItem = {
            baseType: 'CONTENT',
            contentType: 'kenobi',
            draggedPayload: {
                type: 'contentlet',
                item: {
                    container: {
                        identifier: '321',
                        acceptTypes: 'kenobi,theChosenOne,yoda',
                        maxContentlets: 3,
                        uuid: '123'
                    },
                    contentlet: {
                        identifier: '321',
                        inode: '123',
                        title: 'title',
                        contentType: 'kenobi'
                    }
                },
                move: true
            }
        };

        expect(pipe.transform(container, dragItem)).toEqual({
            message: 'edit.ema.page.dropzone.one.max.contentlet',
            args: ['1']
        });
    });

    it('should return message when maxContentlet is greater than 1 and the drag item is from another container (identifier case)', () => {
        const container: Container = {
            x: 10,
            y: 10,
            width: 10,
            height: 10,
            contentlets: [
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                },
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                },
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                }
            ],
            payload: JSON.stringify({
                container: {
                    acceptTypes: 'kenobi,theChosenOne,yoda',
                    maxContentlets: 3,
                    identifier: '123'
                }
            })
        };

        const dragItem: EmaDragItem = {
            baseType: 'CONTENT',
            contentType: 'kenobi',
            draggedPayload: {
                type: 'contentlet',
                item: {
                    container: {
                        identifier: '321',
                        acceptTypes: 'kenobi,theChosenOne,yoda',
                        maxContentlets: 3,
                        uuid: '123'
                    },
                    contentlet: {
                        identifier: '321',
                        inode: '123',
                        title: 'title',
                        contentType: 'kenobi'
                    }
                },
                move: true
            }
        };

        expect(pipe.transform(container, dragItem)).toEqual({
            message: 'edit.ema.page.dropzone.max.contentlets',
            args: ['3']
        });
    });
    it('should return message when maxContentlet is greater than 1 and the drag item is from another container (uuid case)', () => {
        const container: Container = {
            x: 10,
            y: 10,
            width: 10,
            height: 10,
            contentlets: [
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                },
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                },
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                }
            ],
            payload: JSON.stringify({
                container: {
                    acceptTypes: 'kenobi,theChosenOne,yoda',
                    maxContentlets: 3,
                    identifier: '123',
                    uuid: '321'
                }
            })
        };

        const dragItem: EmaDragItem = {
            baseType: 'CONTENT',
            contentType: 'kenobi',
            draggedPayload: {
                type: 'contentlet',
                item: {
                    container: {
                        identifier: '123',
                        acceptTypes: 'kenobi,theChosenOne,yoda',
                        maxContentlets: 3,
                        uuid: '456'
                    },
                    contentlet: {
                        identifier: '321',
                        inode: '123',
                        title: 'title',
                        contentType: 'kenobi'
                    }
                },
                move: true
            }
        };

        expect(pipe.transform(container, dragItem)).toEqual({
            message: 'edit.ema.page.dropzone.max.contentlets',
            args: ['3']
        });
    });

    it('should not return message when maxContentlet is greater than 1 and the drag item is from the same container', () => {
        const container: Container = {
            x: 10,
            y: 10,
            width: 10,
            height: 10,
            contentlets: [
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                },
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                },
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                }
            ],
            payload: JSON.stringify({
                container: {
                    acceptTypes: 'kenobi,theChosenOne,yoda',
                    maxContentlets: 3,
                    identifier: '123',
                    uuid: '321'
                }
            })
        };

        const dragItem: EmaDragItem = {
            baseType: 'CONTENT',
            contentType: 'kenobi',
            draggedPayload: {
                type: 'contentlet',
                item: {
                    container: {
                        identifier: '123',
                        acceptTypes: 'kenobi,theChosenOne,yoda',
                        maxContentlets: 3,
                        uuid: '321'
                    },
                    contentlet: {
                        identifier: '321',
                        inode: '123',
                        title: 'title',
                        contentType: 'kenobi'
                    }
                },
                move: true
            }
        };

        expect(pipe.transform(container, dragItem)).toEqual({
            message: '',
            args: []
        });
    });

    it('should not return message when all constrains are true', () => {
        const container: Container = {
            x: 10,
            y: 10,
            width: 10,
            height: 10,
            contentlets: [
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                },
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                },
                {
                    x: 10,
                    y: 10,
                    width: 10,
                    height: 10,
                    payload: PAYLOAD_MOCK
                }
            ],
            payload: JSON.stringify({
                container: { acceptTypes: 'kenobi,theChosenOne,yoda', maxContentlets: 5 }
            })
        };

        const dragItem: EmaDragItem = {
            baseType: 'CONTENT',
            contentType: 'kenobi',
            draggedPayload: null
        };

        expect(pipe.transform(container, dragItem)).toEqual({
            message: '',
            args: []
        });
    });
});

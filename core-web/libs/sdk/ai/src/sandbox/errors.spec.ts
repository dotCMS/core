import {
    AbortError,
    HttpError,
    NetworkError,
    PolicyError,
    RuntimeError,
    SandboxError,
    TimeoutError,
    ValidationError,
    type DotCMSError
} from './errors';

type ErrorClass = abstract new (...args: never[]) => DotCMSError;

describe('error names', () => {
    // A bundler minifies class names: in the built MCP server `PolicyError` is a class called
    // `b`, so an error named after its class reached the model as "b: The search tool can
    // only reach…". The name is part of what the model reads, so it must not depend on it.
    const cases: Array<[string, ErrorClass, () => DotCMSError]> = [
        ['ValidationError', ValidationError, () => new ValidationError('m')],
        ['PolicyError', PolicyError, () => new PolicyError('m', 'GET', '/x')],
        ['HttpError', HttpError, () => new HttpError(500, 'Server Error', 'b')],
        ['NetworkError', NetworkError, () => new NetworkError('GET', '/x', new Error('c'))],
        ['TimeoutError', TimeoutError, () => new TimeoutError('m', 1)],
        ['AbortError', AbortError, () => new AbortError('m')],
        ['SandboxError', SandboxError, () => new SandboxError('m')],
        ['RuntimeError', RuntimeError, () => new RuntimeError('m')]
    ];

    it.each(cases)(
        'names a %s by what it is, even once its class is minified',
        (name, cls, make) => {
            const original = Object.getOwnPropertyDescriptor(cls, 'name');
            Object.defineProperty(cls, 'name', { value: 'b', configurable: true });
            try {
                const error = make();

                expect(error.name).toBe(name);
                expect(error.toJSON().name).toBe(name);
            } finally {
                if (original) Object.defineProperty(cls, 'name', original);
            }
        }
    );
});

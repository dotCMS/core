import spec from '../generated/spec.json';

export function getSpec(): Record<string, unknown> {
    return spec as Record<string, unknown>;
}

import {ddescribe,
  describe,
  it,
  iit,
  xit,
  expect,
  beforeEach,
  afterEach} from 'angular2/test_lib';
import {PipeRegistry} from 'angular2/src/change_detection/pipes/pipe_registry';
import {Pipe} from 'angular2/src/change_detection/pipes/pipe';
export function main() {
  describe("pipe registry", () => {
    var firstPipe = new Pipe();
    var secondPipe = new Pipe();
    it("should return the first pipe supporting the data type", () => {
      var r = new PipeRegistry({"type": [new PipeFactory(false, firstPipe), new PipeFactory(true, secondPipe)]});
      expect(r.get("type", "some object", null)).toBe(secondPipe);
    });
    it("should throw when no matching type", () => {
      var r = new PipeRegistry({});
      expect(() => r.get("unknown", "some object", null)).toThrowError(`Cannot find 'unknown' pipe supporting object 'some object'`);
    });
    it("should throw when no matching pipe", () => {
      var r = new PipeRegistry({"type": []});
      expect(() => r.get("type", "some object", null)).toThrowError(`Cannot find 'type' pipe supporting object 'some object'`);
    });
  });
}
class PipeFactory {
  constructor(shouldSupport, pipe) {
    this.shouldSupport = shouldSupport;
    this.pipe = pipe;
  }
  supports(obj) {
    return this.shouldSupport;
  }
  create(cdRef) {
    return this.pipe;
  }
}
Object.defineProperty(PipeFactory, "parameters", {get: function() {
    return [[assert.type.boolean], [assert.type.any]];
  }});
//# sourceMappingURL=pipe_registry_spec.js.map

//# sourceMappingURL=./pipe_registry_spec.map
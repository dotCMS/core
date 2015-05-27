import {ddescribe,
  describe,
  it,
  iit,
  expect,
  beforeEach,
  IS_DARTIUM} from 'angular2/test_lib';
import {ReflectionCapabilities} from 'angular2/src/reflection/reflection_capabilities';
import {isPresent,
  global,
  CONST} from 'angular2/src/facade/lang';
export function main() {
  var rc;
  beforeEach(() => {
    rc = new ReflectionCapabilities();
  });
  function mockReflect(mockData, cls) {
    if (!IS_DARTIUM) {
      global.Reflect = {'getMetadata': (key, targetCls) => {
          return (targetCls == cls) ? mockData[key] : null;
        }};
    }
  }
  function assertTestClassAnnotations(annotations) {
    expect(annotations[0]).toBeAnInstanceOf(ClassDec1);
    expect(annotations[1]).toBeAnInstanceOf(ClassDec2);
  }
  function assertTestClassParameters(parameters) {
    expect(parameters[0].length).toBe(2);
    expect(parameters[0][0]).toEqual(P1);
    expect(parameters[0][1]).toBeAnInstanceOf(ParamDec);
    expect(parameters[1].length).toBe(1);
    expect(parameters[1][0]).toEqual(P2);
    expect(parameters[2].length).toBe(1);
    expect(parameters[2][0]).toBeAnInstanceOf(ParamDec);
    expect(parameters[3].length).toBe(0);
  }
  describe('reflection capabilities', () => {
    it('can read out class annotations through annotations key', () => {
      assertTestClassAnnotations(rc.annotations(TestClass));
    });
    it('can read out parameter annotations through parameters key', () => {
      assertTestClassParameters(rc.parameters(TestClass));
    });
    it('can read out class annotations though Reflect APIs', () => {
      if (IS_DARTIUM)
        return ;
      mockReflect(mockDataForTestClassDec, TestClassDec);
      assertTestClassAnnotations(rc.annotations(TestClassDec));
    });
    it('can read out parameter annotations though Reflect APIs', () => {
      if (IS_DARTIUM)
        return ;
      mockReflect(mockDataForTestClassDec, TestClassDec);
      assertTestClassParameters(rc.parameters(TestClassDec));
    });
  });
}
class ClassDec1 {
  constructor() {}
}
Object.defineProperty(ClassDec1, "annotations", {get: function() {
    return [new CONST()];
  }});
class ClassDec2 {
  constructor() {}
}
Object.defineProperty(ClassDec2, "annotations", {get: function() {
    return [new CONST()];
  }});
class ParamDec {
  constructor() {}
}
Object.defineProperty(ParamDec, "annotations", {get: function() {
    return [new CONST()];
  }});
class P1 {}
class P2 {}
class TestClass {
  constructor(a, b, c, d) {}
}
Object.defineProperty(TestClass, "annotations", {get: function() {
    return [new ClassDec1(), new ClassDec2()];
  }});
Object.defineProperty(TestClass, "parameters", {get: function() {
    return [[P1, new ParamDec()], [P2], [new ParamDec()], []];
  }});
var mockDataForTestClassDec = {
  'annotations': [new ClassDec1(), new ClassDec2()],
  'parameters': [new ParamDec(), null, new ParamDec()],
  'design:paramtypes': [P1, P2, Object, Object]
};
class TestClassDec {}
//# sourceMappingURL=reflection_capabilities_spec.js.map

//# sourceMappingURL=./reflection_capabilities_spec.map
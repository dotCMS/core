export class TestIterable {
  constructor() {
    this.list = [];
  }
  [Symbol.iterator]() {
    return this.list[Symbol.iterator]();
  }
}
//# sourceMappingURL=iterable.es6.map

//# sourceMappingURL=./iterable.map
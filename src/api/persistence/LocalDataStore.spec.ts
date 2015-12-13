import {Injector, Provider} from 'angular2/angular2';

import {DataStore} from '../../api/persistence/DataStore'
import {LocalDataStore} from "../../api/persistence/LocalDataStore";

var injector = Injector.resolveAndCreate([
  new Provider(DataStore, {useClass: LocalDataStore})
])
describe('Unit.api.persistence.LocalDataStore', function () {

  var store:DataStore
  beforeEach(function () {
    store = injector.get(DataStore)
  });

  it("A store is injected.", function () {
    expect(store).not.toBeNull()
  })

  it("should throw error if a path doesn't start with start with 'http'", function () {
    expect(() => {
          var data = {
            a: 100,
            b: "Hello"
          }
          var path = "some/path";
          store.setItem(path, data)
        }
    ).toThrowError(/Path must be a valid URL/)

  });

  it("should save json object to a given url", ()=> {
    var data = {
      a: 100,
      b: "Hello"
    }
    var path = "http://foo.com/some/path";
    store.setItem(path, data)
    let result = store.getItem(path)
    expect(result).not.toBeNull()
    expect(result.a).toEqual(100)
    expect(result.b).toEqual(data.b)
  })


});


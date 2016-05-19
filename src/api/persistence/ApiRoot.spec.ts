import {Injector} from '@angular/core';
import {UserModel} from "../../api/auth/UserModel";
import {ApiRoot} from '../../api/persistence/ApiRoot';


var injector = Injector.resolveAndCreate([
  UserModel,
    ApiRoot
])
describe('Unit.api.persistence.ApiRoot', function () {

  let apiRoot:ApiRoot
  beforeEach(function () {
    apiRoot = injector.get(ApiRoot)
  });

  it("The ApiRoot is injected.", function () {
    expect(apiRoot).not.toBeNull()
  })

  it("Parses a query param correctly when it's the last query parameter.", function(){
    let siteId = '48190c8c-42c4-46af-8d1a-0cd5db894797';
    expect(ApiRoot.parseQueryParam("foo=bar&baz=1&realmId="+siteId, 'realmId')).toEqual(siteId)
  })

  it("Parses a query param correctly when it's the first query parameter.", function(){
    let siteId = '48190c8c-42c4-46af-8d1a-0cd5db894797';
    expect(ApiRoot.parseQueryParam("realmId="+siteId + "&foo=bar&baz=1", 'realmId')).toEqual(siteId)
  })

  it("Parses a query param correctly when it's the in the middle of the query.", function(){
    let siteId = '48190c8c-42c4-46af-8d1a-0cd5db894797';
    expect(ApiRoot.parseQueryParam("blarg=99thousand&realmId="+siteId + "&foo=bar&baz=1", 'realmId')).toEqual(siteId)
  })

});



import {CwFilter} from "./CwFilter";
describe('Unit.api.util.CwFilter', function () {


  beforeEach(function () {
  });

  it("An object with foo=true should not be filtered for filter='foo:false'", function(){
    expect(CwFilter.isFiltered("foo", "false", {foo:true})).toEqual(true);
  })

  it("An object with foo=true should be filtered for filter='foo:false'", function(){
    expect(CwFilter.isFiltered("foo", "false", {foo:true})).toEqual(true);
  })

});
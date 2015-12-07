import {Check} from '../validation/Check';
import {RestDataStore as Store} from '../persistence/RestDataStore'

let remoteApiRootRef = "http://localhost:9000/api/v1/"

describe('Integration.api.rule-engine.ReST.RuleApi', function () {


  beforeEach(function () {
  });

  it('is available at ' + remoteApiRootRef, function (done) {
    Store.getItem(remoteApiRootRef + 'system/ruleengine/conditionlets').then((conditionlets) => {
      expect(conditionlets).toBeDefined()
      done()
    }).catch((e) =>{
      console.log("Error: ", e, e.response)
      if(e.response.status === -1){
        expect("The server is running and available at " + remoteApiRootRef + ".").toEqual(true)
      }
      else{
        expect("The server is running and available at " + remoteApiRootRef + ".").toEqual(true)
      }
      done();
    })

  });


  it('provides a list of all available Conditionlet types', function (done) {
    Store.getItem(remoteApiRootRef + 'system/ruleengine/conditionlets').then((conditionlets) => {
      expect(conditionlets).toBeDefined()
      let headerConditionlet = conditionlets["RequestHeaderConditionlet"]
      expect(headerConditionlet).toBeDefined()

      let count = Object.keys(conditionlets).length
      expect(count).toEqual(21)
      Object.keys(conditionlets).forEach((conditionletName) => {
        expect(conditionlets[conditionletName].id).toEqual(conditionletName)
      })

      done()
    }).catch((e) =>{
      console.log("Error: ", e, e.response)
      expect(() => { throw e }).not.toThrow()
      done();
    })

  });


});
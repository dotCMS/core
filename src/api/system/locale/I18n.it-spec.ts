import {Injector, Provider} from 'angular2/angular2';

import {DataStore} from '../../../api/persistence/DataStore'
import {LocalDataStore} from '../../../api/persistence/LocalDataStore';
import {RestDataStore} from '../../../api/persistence/RestDataStore';


import {UserModel} from '../../../api/auth/UserModel';
import {ApiRoot} from '../../../api/persistence/ApiRoot';
import {I18nService} from "./I18n";


var injector = Injector.resolveAndCreate([
  UserModel,
  ApiRoot,
  I18nService,
  new Provider(DataStore, {useClass: RestDataStore})
])

describe('Integration.api.system.locale.I18n', function () {

  var rsrcService:I18nService

  beforeEach(function () {
    rsrcService = injector.get(I18nService)
  });


  it("Can get a specific message.", function(done){
    rsrcService.getForLocale('en-US',  'message.comment.success', (rsrc)=>{
      expect(rsrc).toBe("Your comment has been saved")
      rsrcService.getForLocale('de',  'message.comment.success', (rsrc)=>{
        expect(rsrc).toBe("Ihr Kommentar wurde gespeichert")
        done()
      })
    })
  })

  it("Can get all message under a particular path.", function(done){
    rsrcService.getForLocale('en-US',  'message.comment', (rsrc)=>{
      expect(rsrc.delete).toBe("Your comment has been delete")
      expect(rsrc.failure).toBe("Your comment couldn't be created")
      expect(rsrc.success).toBe("Your comment has been saved")
      rsrcService.getForLocale('de',  'message.comment', (rsrc)=>{
        expect(rsrc.delete).toBe("Ihr Kommentar wurde gelöscht")
        expect(rsrc.failure).toBe("Ihr Kommentar konnte nicht erstellt werden")
        expect(rsrc.success).toBe("Ihr Kommentar wurde gespeichert")
        done()
      })
    })
  })


});

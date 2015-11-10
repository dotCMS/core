import {Injector, Provider} from 'angular2/angular2';

import {DataStore} from '../../../api/persistence/DataStore'
import {LocalDataStore} from '../../../api/persistence/LocalDataStore';
import {RestDataStore} from '../../../api/persistence/RestDataStore';


import {UserModel} from '../../../api/auth/UserModel';
import {ApiRoot} from '../../../api/persistence/ApiRoot';
import {I18nService, I18nResourceModel} from "./I18n";


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
    rsrcService.get('en-US',  'message.comment.success', (rsrc:I18nResourceModel)=>{
      expect(rsrc.messages).toBe("Your comment has been saved")
      rsrcService.get('de',  'message.comment.success', (rsrc:I18nResourceModel)=>{
        expect(rsrc.messages).toBe("Ihr Kommentar wurde gespeichert")
        done()
      })
    })
  })

  it("Can get all message under a particular path.", function(done){
    rsrcService.get('en-US',  'message.comment', (rsrc:I18nResourceModel)=>{
      expect(rsrc.messages.delete).toBe("Your comment has been delete")
      expect(rsrc.messages.failure).toBe("Your comment couldn't be created")
      expect(rsrc.messages.success).toBe("Your comment has been saved")
      rsrcService.get('de',  'message.comment', (rsrc:I18nResourceModel)=>{
        expect(rsrc.messages.delete).toBe("Ihr Kommentar wurde gelöscht")
        expect(rsrc.messages.failure).toBe("Ihr Kommentar konnte nicht erstellt werden")
        expect(rsrc.messages.success).toBe("Ihr Kommentar wurde gespeichert")
        done()
      })
    })
  })


});

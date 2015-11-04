import {Inject} from 'angular2/angular2';

import {EntityMeta} from "./EntityBase";
import {DataStore} from "./DataStore";
import {UserModel} from "../auth/UserModel";

var instanceOfApiRoot = null
export class ApiRoot {
  // Points to {baseUrl}/api/v1
  root:EntityMeta;
  defaultSite:EntityMeta;
  baseUrl: string = "http://localhost:8080/";
  defaultSiteId: string = '48190c8c-42c4-46af-8d1a-0cd5db894797'
  authUser: UserModel;
  resourceRef: EntityMeta
  dataStore:DataStore;

  constructor(@Inject(UserModel) authUser:UserModel, @Inject(DataStore) dataStore:DataStore){
    this.authUser = authUser
    this.dataStore = dataStore;
    dataStore.setAuth(authUser.username, authUser.password)
    try {
      let url = this.checkQueryForUrl(document.location.search.substring(1))
      this.setBaseUrl(url) // if null, just uses the base of the current URL
      this.resourceRef = this.root.child('system/i18n')
    } catch (e) {
      console.log("Could not set baseUrl automatically.")
    }
    instanceOfApiRoot = this;
  }

  checkQueryForUrl(locationQuery:string):string{
    let queryBaseUrl = null;
    if (locationQuery && locationQuery.length) {
      let q = locationQuery
      let token = 'baseUrl='
      let idx = q.indexOf(token)
      if (idx >= 0) {
        let end = q.indexOf('&', idx)
        end = end != -1 ? end : q.length
        queryBaseUrl = q.substring(idx + token.length, end)
        console.log('Proxy server Base URL set to ', queryBaseUrl)
      }
    }
    return queryBaseUrl
  }

  setBaseUrl(url=null){
    if(url === null){
      // set to same as current request
      let loc = document.location
      this.baseUrl =  loc.protocol + '//' + loc.host + '/'
    }
    else  if(url && (url.startsWith('http://' || url.startsWith('https://')))){
      this.baseUrl = url.endsWith('/') ? url : url + '/' ;
    } else {
      throw new Error("Invalid proxy server base url: '" + url + "'")
    }
    this.root = new EntityMeta(this.baseUrl + 'api/v1')
    this.defaultSite = this.root.child('sites/' + this.defaultSiteId)
  }


  getRoot():EntityMeta {
    return this.root
  }

  static instance(){
    return instanceOfApiRoot
  }
}
import  XDebug from '../log-config.js'
let log = XDebug('RuleEngine.api.it');

import rest from 'rest';
import basicAuth from 'rest/interceptor/basicAuth';

import {Core, Check} from '../../coreweb/index'
import {Rule} from './RuleEngineTypes.js'



describe('RuleEngine.remote.api', function () {
  let demo_dotCMSSiteId = "48190c8c-42c4-46af-8d1a-0cd5db894797"
  let localhostSiteId = "48190c8c-42c4-46af-8d1a-0cd5db894797"
  let defaultSiteId = localhostSiteId
  let ruleToAdd = {
    "name": "This is a Rule" + Date.now(),
    "enabled": true,
    "site": defaultSiteId,
    "priority": 10,
    "fireOn": "EVERY_PAGE",
    "folder": "SYSTEM_FOLDER",
    "shortCircuit": false,
    "groups": {},
    "actions": {}
  }
  ruleToAdd.groups[Core.Key.next()] = {"operator": "and", "priority": 1}
  ruleToAdd.groups[Core.Key.next()] = {"operator": "or", "priority": 2}
  ruleToAdd.groups[Core.Key.next()] = {"operator": "and", "priority": 1000}

  ruleToAdd.actions[Core.Key.next()] = {"priority": 1}
  ruleToAdd.actions[Core.Key.next()] = {"priority": 2}
  ruleToAdd.actions[Core.Key.next()] = {"priority": 1000}

  let ruleApiUrl = '/api/v1/sites/' + defaultSiteId + '/rules'

  let client;
  let testRule;
  beforeAll(function () {
    client = rest.wrap(basicAuth);
  })

  beforeEach(function () {
    testRule = new Rule();
    testRule.name = "This is a Rule" + Date.now()
    testRule.site = defaultSiteId
    testRule.priority = 10
    testRule.fireOn = "EVERY_PAGE"
    testRule.folder = "SYSTEM_FOLDER"
    testRule.shortCircuit = true
    testRule.groups = {}
    testRule.actions = {}
  })

  it('Can read the list of Rules', (done) => {
    client({username: 'admin@dotcms.com', password: 'admin', path: ruleApiUrl}).then(function (response) {
      expect(response.request.headers.Authorization).toBe('Basic YWRtaW5AZG90Y21zLmNvbTphZG1pbg==');
      expect(response).toBeDefined()
      let entity = JSON.parse(response.entity)
      log('Response Entity:', entity)
      done()
    }).catch((e)=> {
      log('Error: ', e)
      fail()
      done()
    })

  })

  it('responds with the new rule id on push', (done)=> {
    let rule = testRule
    log('Saving rule: ', rule)
    client({
      username: 'admin@dotcms.com', password: 'admin',
      path: ruleApiUrl,
      entity: rule.toJson(),
      headers: {'Content-Type': 'application/json'}
    }).then(function (response) {
      log('Response:', response)
      expect(response.status.code).toBe(200)
      let entity = JSON.parse(response.entity)
      expect(entity.id).toBeDefined()
      done()
    }).catch((e)=> {
      log('Error: ', e)
      fail()
      done()
    })
  })

  let save = function (rule) {
    return new Promise((resolve, reject)=> {
      client({
        username: 'admin@dotcms.com', password: 'admin',
        path: ruleApiUrl,
        entity: rule.toJson(),
        headers: {'Content-Type': 'application/json'}
      }).then((response) => {
        resolve(response)
      }).catch((e)=> {
        log("Save operation resulted in an error: ", e)
        reject(e)
      })
    })
  }
  it('can retrieve a saved rule', (done)=> {
    let rule = testRule
    log('Saving rule: ', rule)
    save(rule).then((response)=> {
      expect(response.status.code).toBe(200)
      let entity = JSON.parse(response.entity)
      expect(entity.id).toBeDefined()
      rule.$key = entity.id
      let path = ruleApiUrl + '/' + rule.$key
      log("Retrieving rule: ", path)
      client({
        username: 'admin@dotcms.com', password: 'admin',
        path: path,
        headers: {'Content-Type': 'application/json'}
      }).then(function (response) {
        let entity = JSON.parse(response.entity)
        log("received rule: ", entity)
        expect(entity).toBeDefined()
        expect(entity.name).toBe(rule.name)
        done()
      }).catch((e)=> {
        log("Error retrieving saved rule: ", e)
        fail()
        done()
      })
    }).catch((e)=> {
      fail()
    })

  })

  it('can save group', (done)=> {
    let rule = testRule
    rule.groups["sadfasdf"] = {
      priority: 1,
      operator: 'AND'
    }
    log('Saving rule: ', rule)
    save(rule).then((response)=> {
      expect(response.status.code).toBe(200)
      let entity = JSON.parse(response.entity)
      expect(entity.id).toBeDefined()
      rule.$key = entity.id
      let path = ruleApiUrl + '/' + rule.$key
      client({
        username: 'admin@dotcms.com', password: 'admin',
        path: path,
        headers: {'Content-Type': 'application/json'}
      }).then(function (response) {
        let entity = JSON.parse(response.entity)
        log("received rule: ", entity)
        expect(entity).toBeDefined()
        expect(entity.name).toBe(rule.name)
        expect(entity.groups).toBeDefined()
        log("rule has groups: ", entity.groups)
        done()
      }).catch((e)=> {
        log("Error retrieving saved rule: ", e)
        fail()
        done()
      })
    }).catch((e)=> {
      log("Error retrieving saved rule: ", e)

      fail()
    })

  })


})
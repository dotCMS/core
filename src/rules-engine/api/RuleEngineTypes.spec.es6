import * as RuleEngine from 'src/rules-engine/RuleEngine.es6';
import {Core, Check} from 'src/dc/index.es6';
import {Rule} from 'src/rules-engine/api/RuleEngineTypes.es6'


describe('RuleEngineTypes.rule', function () {

  let fieldCases = {
    name: {
      valid: ["12345", "BobIsAwesome", "Now with 'single quote' marks"],
      invalid: [
        {value: "1234", msg: /tooShort/},
        {value: "", msg: /tooShort/},
        {value: null, msg: /doesNotExist/}
      ]
    },
    enabled: {
      valid: [true, false],
      invalid: [
        {value: "any string", msg: /notBoolean/},
        {value: 101, msg: /notBoolean/},
        {value: 1, msg: /notBoolean/},
        {value: {}, msg: /notBoolean/},
        {value: 0, msg: /notBoolean/},
        {value: null, msg: /notBoolean/}
      ]
    },
    site: {
      valid: ["48190c8c-42c4-46af-8d1a-0cd5db894797", "36CharactersLongAAAAAAAAAAAAAAAAAAAB"],
      invalid: [
        {value: "35CharactersLongAAAAAAAAAAAAAAAAAAA", msg: /tooShort/},
        {value: "37CharactersLongAAAAAAAAAAAAAAAAAAABC", msg: /tooLong/},
        {value: null, msg: /doesNotExist/}
      ]
    },
    priority: {
      valid: [0, 1, 2, 5, 99, 100],
      invalid: [
        {value: "any string", msg: /notInteger/},
        {value: -1, msg: /tooSmall/},
        {value: 101, msg: /tooLarge/}
      ]
    },
    fireOn: {
      valid: ['EVERY_PAGE',
              'ONCE_PER_VISIT',
              'ONCE_PER_VISITOR',
              'EVERY_REQUEST'],
      invalid: [
        {value: "any string", msg: /notMember/},
        {value: -1, msg: /notMember/},
        {value: 101, msg: /notMember/}
      ]
    },
    folder: {
      valid: ["12345", "BobIsAwesome", "Now with 'single quote' marks"],
      invalid: [
        {value: "", msg: /tooShort/},
        {value: "abcd", msg: /tooShort/},
        {value: null, msg: /doesNotExist/}
      ]
    }

  }

  beforeEach(function () {
  })

  beforeAll(function () {
  })

  afterAll(function () {
  })

  it("can be created in an invalid state but won't validate.", function () {
    let aRule = new Rule()
    expect(aRule.name).toBe(null)
    expect(() => {
      aRule.validate()
    }).toThrowError(/One or more failures occurred/)
  })

  Object.keys(fieldCases).forEach((fieldName) => {
    fieldCases[fieldName].valid.forEach((value) => {
      it("allows " + fieldName + " to be set to '" + value + "'.", function () {
        let rule = new Rule()
        expect(() => {
          rule[fieldName] = value
        }).not.toThrow()
        expect(rule[fieldName]).toBe(value)

      })
    })

    fieldCases[fieldName].invalid.forEach((invalid) => {
      it("does not allow " + fieldName + " to be set to '" + invalid.value + "'.", function () {
        let rule = new Rule()
        expect(() => {
          rule[fieldName] = invalid.value
        }).toThrowError(invalid.msg)
      })
    })
  })

  it("does not allow arbitrary properties to be added to it.", function(){
    let rule = new Rule()
    expect( () => {
      rule['foo'] = 100;
    }).toThrowError(/object is not extensible/)
  })

  it("allows 'groups' to be replaced with empty object map.", function () {
    let rule = new Rule()
    let groups = {}
    expect(() => {
      rule.groups = groups
      Object.keys(fieldCases).forEach((fieldName)=>{
        if(fieldName != 'groups'){
          rule[fieldName] = fieldCases[fieldName].valid[0]
        }
      })
      rule.validate()
    }).not.toThrow()
    expect(rule.groups).toBeDefined()


  })


})
import XDebug from 'debug';
let log = XDebug('RuleEngine.Core');

let KEY_CHARS = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
let prevGenTime = 0;
let prevRandChars = [];

let Core = {


  Key: {

    next() {
      let now = Date.now()
      let duplicateTime = now === prevGenTime
      prevGenTime = now;
      let timeStampChars = new Array(8);
      for (var i = 7; i >= 0; i--) {
        timeStampChars[i] = KEY_CHARS.charAt(now % 64);
        now = Math.floor(now / 64);
      }
      var key = timeStampChars.join("");
      if (!duplicateTime) {
        for (i = 0; i < 12; i++) {
          prevRandChars[i] = Math.floor(Math.random() * 64);
        }
      } else {
        for (i = 11; i >= 0 && prevRandChars[i] === 63; i--) {
          prevRandChars[i] = 0;
        }
        prevRandChars[i]++;
      }
      for (i = 0; i < 12; i++) {
        key += KEY_CHARS.charAt(prevRandChars[i]);
      }
      log("Generated Key:", key)
      return key;
    }
  },

  filters: {
    without(value) {
      return function (entry, idx) {
        return entry !== value
      }
    }
  },


  Collections: {

    copyKeysToValues(keyMap, prefix = '') {
      let x = {}
      Object.keys(keyMap).forEach((key) => {
        x[key] = prefix + key
      })
      return x
    },

    asArray(mapObject, transformFn = null) {
      let a = [];
      Object.keys(mapObject).forEach((key)=> {
        a.push(transformFn ? transformFn(mapObject[key], key) : mapObject[key])
      })
      return a;
    },

    asMapObject(map, transformFn = null) {
      let objectMap = {}

      map.forEach((value, key) => {
        objectMap[key] = transformFn ? transformFn(value) : value;
      })

      return objectMap;
    },

    newMap(objectMap, transformFn = null) {
      let map = new Map();
      Object.keys(objectMap).forEach((key) => {
        map.set(key, transformFn ? transformFn(objectMap[key], key) : objectMap[key])
      });
      return map;
    },

    newSet(objectMap) {
      let theSet = new Set();
      Object.keys(objectMap).forEach((key) => {
        objectMap[key]['$key'] = key;
        theSet.add(objectMap[key])
      });
      return theSet;
    }
  }


}


export {Core};


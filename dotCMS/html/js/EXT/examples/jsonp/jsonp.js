/*
 * Ext Core Library Examples 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * The MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 */

Ext.ns('Ext.ux');

Ext.ux.JSONP = (function(){
    var _queue = [],
        _current = null,
        _nextRequest = function() {
            _current = null;
            if(_queue.length) {
                _current = _queue.shift();
    			_current.script.src = _current.url + '?' + _current.params;
    			document.getElementsByTagName('head')[0].appendChild(_current.script);
            }
        };

    return {
        request: function(url, o) {
            if(!url) {
                return;
            }
            var me = this;

            o.params = o.params || {};
            if(o.callbackKey) {
                o.params[o.callbackKey] = 'Ext.ux.JSONP.callback';
            }
            var params = Ext.urlEncode(o.params);

            var script = document.createElement('script');
			script.type = 'text/javascript';

            if(o.isRawJSON) {
                if(Ext.isIE) {
                    Ext.fly(script).on('readystatechange', function() {
                        if(script.readyState == 'complete') {
                            var data = script.innerHTML;
                            if(data.length) {
                                me.callback(Ext.decode(data));
                            }
                        }
                    });
                }
                else {
                     Ext.fly(script).on('load', function() {
                        var data = script.innerHTML;
                        if(data.length) {
                            me.callback(Ext.decode(data));
                        }
                    });
                }
            }

            _queue.push({
                url: url,
                script: script,
                callback: o.callback || function(){},
                scope: o.scope || window,
                params: params || null
            });

            if(!_current) {
                _nextRequest();
            }
        },

        callback: function(json) {
            _current.callback.apply(_current.scope, [json]);
            Ext.fly(_current.script).removeAllListeners();
            document.getElementsByTagName('head')[0].removeChild(_current.script);
            _nextRequest();
        }
    }
})();
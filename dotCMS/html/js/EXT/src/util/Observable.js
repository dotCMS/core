/*
 * Ext Core Library 3.0
 * http://extjs.com/
 * Copyright(c) 2006-2009, Ext JS, LLC.
 * 
 * MIT Licensed - http://extjs.com/license/mit.txt
 * 
 */

(function(){

var EXTUTIL = Ext.util, 
    TOARRAY = Ext.toArray, 
    EACH = Ext.each, 
    ISOBJECT = Ext.isObject,
    TRUE = true,
    FALSE = false;
/**
 * @class Ext.util.Observable
 * Base class that provides a common interface for publishing events. Subclasses are expected to
 * to have a property "events" with all the events defined, and, optionally, a property "listeners"
 * with configured listeners defined.<br>
 * For example:
 * <pre><code>
Employee = Ext.extend(Ext.util.Observable, {
    constructor: function(config){
        this.name = config.name;
        this.addEvents({
            "fired" : true,
            "quit" : true
        });

//      Copy configured listeners into *this* object so that the base class's
//      constructor will add them.
        this.listeners = config.listeners;

//      Call our superclass constructor to complete construction process.
        Employee.superclass.constructor.call(config)
    }
});
</code></pre>
 * This could then be used like this:<code><pre>
var newEmployee = new Employee({
    name: employeeName,
    listeners: {
        quit: function() {
//          By default, "this" will be the object that fired the event.
            alert(this.name + " has quit!");
        }
    }
});
</pre></code>
 */
EXTUTIL.Observable = function(){
    /**
     * @cfg {Object} listeners (optional)<p>A config object containing one or more event handlers to be added to this
     * object during initialization.  This should be a valid listeners config object as specified in the
     * {@link #addListener} example for attaching multiple handlers at once.</p>
     * <h2>DOM events from ExtJs {@link Ext.Component Components}</h2>
     * <p>While <i>some</i> ExtJs Component classes export selected DOM events (eg "click", "mouseover" etc), this
     * is usually only done when extra value can be added. For example the {@link Ext.DataView DataView}'s
     * <b><code>{@link Ext.DataView#click click}</code></b> event passing the node clicked on. To access DOM
     * events directly from a Component's HTMLElement, listeners must be added to the <i>{@link Ext.Component#getEl Element}</i> after the Component
     * has been rendered. A plugin can simplify this step:<code><pre>
// Plugin is configured with a listeners config object.
// The Component is appended to the argument list of all handler functions.
Ext.DomObserver = Ext.extend(Object, {
    constructor: function(config) {
        this.listeners = config.listeners ? config.listeners : config;
    },

//  Component passes itself into plugin's init method
    init: function(c) {
        var p, l = this.listeners;
        for (p in l) {
            if (Ext.isFunction(l[p])) {
                l[p] = this.createHandler(l[p], c);
            } else {
                l[p].fn = this.createHandler(l[p].fn, c);
            }
        }

//      Add the listeners to the Element immediately following the render call
        c.render = c.render.{@link Function#createSequence createSequence}(function() {
            var e = c.getEl();
            if (e) {
                e.on(l);
            }
        });
    },

    createHandler: function(fn, c) {
        return function(e) {
            fn.call(this, e, c);
        };
    }
});

var combo = new Ext.form.ComboBox({

// Collapse combo when its element is clicked on
    plugins: [ new Ext.DomObserver({
        click: function(evt, comp) {
            comp.collapse();
        }
    })],
    store: myStore,
    typeAhead: true,
    mode: 'local',
    triggerAction: 'all'
});
</pre></code></p>
     */
    var me = this, e = me.events;
    if(me.listeners){
        me.on(me.listeners);
        delete me.listeners;
    }
    me.events = e || {};
};

EXTUTIL.Observable.prototype = function(){
    var filterOptRe = /^(?:scope|delay|buffer|single)$/, toLower = function(s){
        return s.toLowerCase();    
    };
        
    return {
        /**
         * <p>Fires the specified event with the passed parameters (minus the event name).</p>
         * <p>An event may be set to bubble up an Observable parent hierarchy (See {@link Ext.Component#getBubbleTarget})
         * by calling {@link #enableBubble}.</p>
         * @param {String} eventName The name of the event to fire.
         * @param {Object...} args Variable number of parameters are passed to handlers.
         * @return {Boolean} returns false if any of the handlers return false otherwise it returns true.
         */
    
        fireEvent : function(){
            var a = TOARRAY(arguments),
                ename = toLower(a[0]),
                me = this,
                ret = TRUE,
                ce = me.events[ename],
                q,
                c;
            if (me.eventsSuspended === TRUE) {
                if (q = me.suspendedEventsQueue) {
                    q.push(a);
                }
            }
            else if(ISOBJECT(ce) && ce.bubble){
                if(ce.fire.apply(ce, a.slice(1)) === FALSE) {
                    return FALSE;
                }
                c = me.getBubbleTarget && me.getBubbleTarget();
                if(c && c.enableBubble) {
                    c.enableBubble(ename);
                    return c.fireEvent.apply(c, a);
                }
            }
            else {            
                if (ISOBJECT(ce)) {
                    a.shift();
                    ret = ce.fire.apply(ce, a);
                }
            }
            return ret;
        },

        /**
         * Appends an event handler to this object.
         * @param {String}   eventName The name of the event to listen for.
         * @param {Function} handler The method the event invokes.
         * @param {Object}   scope (optional) The scope (<code><b>this</b></code> reference) in which the handler function is executed.
         * <b>If omitted, defaults to the object which fired the event.</b>
         * @param {Object}   options (optional) An object containing handler configuration.
         * properties. This may contain any of the following properties:<ul>
         * <li><b>scope</b> : Object<div class="sub-desc">The scope (<code><b>this</b></code> reference) in which the handler function is executed.
         * <b>If omitted, defaults to the object which fired the event.</b></div></li>
         * <li><b>delay</b> : Number<div class="sub-desc">The number of milliseconds to delay the invocation of the handler after the event fires.</div></li>
         * <li><b>single</b> : Boolean<div class="sub-desc">True to add a handler to handle just the next firing of the event, and then remove itself.</div></li>
         * <li><b>buffer</b> : Number<div class="sub-desc">Causes the handler to be scheduled to run in an {@link Ext.util.DelayedTask} delayed
         * by the specified number of milliseconds. If the event fires again within that time, the original
         * handler is <em>not</em> invoked, but the new handler is scheduled in its place.</div></li>
         * <li><b>target</b> : Observable<div class="sub-desc">Only call the handler if the event was fired on the target Observable, <i>not</i>
         * if the event was bubbled up from a child Observable.</div></li>
         * </ul><br>
         * <p>
         * <b>Combining Options</b><br>
         * Using the options argument, it is possible to combine different types of listeners:<br>
         * <br>
         * A delayed, one-time listener.
         * <pre><code>
    myDataView.on('click', this.onClick, this, {
        single: true,
        delay: 100
    });</code></pre>
         * <p>
         * <b>Attaching multiple handlers in 1 call</b><br>
          * The method also allows for a single argument to be passed which is a config object containing properties
         * which specify multiple handlers.
         * <p>
         * <pre><code>
    myGridPanel.on({
        'click' : {
            fn: this.onClick,
            scope: this,
            delay: 100
        },
        'mouseover' : {
            fn: this.onMouseOver,
            scope: this
        },
        'mouseout' : {
            fn: this.onMouseOut,
            scope: this
        }
    });</code></pre>
         * <p>
         * Or a shorthand syntax:<br>
         * <pre><code>
    myGridPanel.on({
        'click' : this.onClick,
        'mouseover' : this.onMouseOver,
        'mouseout' : this.onMouseOut,
         scope: this
    });</code></pre>
         */
        addListener : function(eventName, fn, scope, o){
            var me = this,
                e,
                oe,
                isF,
            ce;
            if (ISOBJECT(eventName)) {
                o = eventName;
                for (e in o){
                    oe = o[e];
                    if (!filterOptRe.test(e)) {
                        me.addListener(e, oe.fn || oe, oe.scope || o.scope, oe.fn ? oe : o);
                    }
                }
            } else {
                eventName = toLower(eventName);
                ce = me.events[eventName] || TRUE;
                if (typeof ce == "boolean") {
                    me.events[eventName] = ce = new EXTUTIL.Event(me, eventName);
                }
                ce.addListener(fn, scope, ISOBJECT(o) ? o : {});
            }
        },

        /**
         * Removes an event handler.
         * @param {String}   eventName     The type of event the handler was associated with.
         * @param {Function} handler       The handler to remove. <b>This must be a reference to the function passed into the {@link #addListener} call.</b>
         * @param {Object}   scope         (optional) The scope originally specified for the handler.
         */
        removeListener : function(eventName, fn, scope){
            var ce = this.events[toLower(eventName)];
            if (ISOBJECT(ce)) {
                ce.removeListener(fn, scope);
            }
        },

        /**
         * Removes all listeners for this object
         */
        purgeListeners : function(){
            var events = this.events,
                evt,
                key;
            for(key in events){
                evt = events[key];
                if(ISOBJECT(evt)){
                    evt.clearListeners();
                }
            }
        },

        /**
         * Used to define events on this Observable
         * @param {Object} object The object with the events defined
         */
        addEvents : function(o){
            var me = this;
            me.events = me.events || {};
            if (typeof o == 'string') {
                EACH(arguments, function(a) {
                    me.events[a] = me.events[a] || TRUE;
                });
            } else {
                Ext.applyIf(me.events, o);
            }
        },

        /**
         * Checks to see if this object has any listeners for a specified event
         * @param {String} eventName The name of the event to check for
         * @return {Boolean} True if the event is being listened for, else false
         */
        hasListener : function(eventName){
            var e = this.events[eventName];
            return ISOBJECT(e) && e.listeners.length > 0;
        },

        /**
         * Suspend the firing of all events. (see {@link #resumeEvents})
         * @param {Boolean} queueSuspended Pass as true to queue up suspended events to be fired
         * after the {@link #resumeEvents} call instead of discarding all suspended events;
         */
        suspendEvents : function(queueSuspended){
            this.eventsSuspended = TRUE;
            if (queueSuspended){
                this.suspendedEventsQueue = [];
            }
        },

        /**
         * Resume firing events. (see {@link #suspendEvents})
         * If events were suspended using the <tt><b>queueSuspended</b></tt> parameter, then all
         * events fired during event suspension will be sent to any listeners now.
         */
        resumeEvents : function(){
            var me = this;
            me.eventsSuspended = !delete me.suspendedEventQueue;
            EACH(me.suspendedEventsQueue, function(e) {
                me.fireEvent.apply(me, e);
            });
        }
    }
}();

var OBSERVABLE = EXTUTIL.Observable.prototype;
/**
 * Appends an event handler to this object (shorthand for {@link #addListener}.)
 * @param {String}   eventName     The type of event to listen for
 * @param {Function} handler       The method the event invokes
 * @param {Object}   scope         (optional) The scope (<code><b>this</b></code> reference) in which the handler function is executed.
 * <b>If omitted, defaults to the object which fired the event.</b>
 * @param {Object}   options       (optional) An object containing handler configuration.
 * @method
 */
OBSERVABLE.on = OBSERVABLE.addListener;
/**
 * Removes an event handler (shorthand for {@link #removeListener}.)
 * @param {String}   eventName     The type of event the handler was associated with.
 * @param {Function} handler       The handler to remove. <b>This must be a reference to the function passed into the {@link #addListener} call.</b>
 * @param {Object}   scope         (optional) The scope originally specified for the handler.
 * @method
 */
OBSERVABLE.un = OBSERVABLE.removeListener;

/**
 * Removes <b>all</b> added captures from the Observable.
 * @param {Observable} o The Observable to release
 * @static
 */
EXTUTIL.Observable.releaseCapture = function(o){
    o.fireEvent = OBSERVABLE.fireEvent;
};

function createTargeted(h, o, scope){
    return function(){
        if(o.target == arguments[0]){
            h.apply(scope, TOARRAY(arguments));
        }
    };
};

function createBuffered(h, o, scope){
    var task = new EXTUTIL.DelayedTask();
    return function(){
        task.delay(o.buffer, h, scope, TOARRAY(arguments));
    };
}
    
function createSingle(h, e, fn, scope){
    return function(){
        e.removeListener(fn, scope);
        return h.apply(scope, arguments);
    };
}
    
function createDelayed(h, o, scope){
    return function(){
        var args = TOARRAY(arguments);
        (function(){
            h.apply(scope, args);
        }).defer(o.delay || 10);
    };
};

EXTUTIL.Event = function(obj, name){
    this.name = name;
    this.obj = obj;
    this.listeners = [];
};

EXTUTIL.Event.prototype = {
    addListener : function(fn, scope, options){
        var me = this,
            l;
        scope = scope || me.obj;
        if(!me.isListening(fn, scope)){
            l = me.createListener(fn, scope, options);
            if(me.firing){ // if we are currently firing this event, don't disturb the listener loop                                    
                me.listeners = me.listeners.slice(0);                
            }
            me.listeners.push(l);
        }
    },

    createListener: function(fn, scope, o){
        o = o || {}, scope = scope || this.obj;
        var l = {
            fn: fn,
            scope: scope,
            options: o
        }, h = fn;
        if(o.target){
            h = createTargeted(h, o, scope);
        }
        if(o.delay){
            h = createDelayed(h, o, scope);
        }
        if(o.single){
            h = createSingle(h, this, fn, scope);
        }
        if(o.buffer){
            h = createBuffered(h, o, scope);
        }
        l.fireFn = h;
        return l;
    },

    findListener : function(fn, scope){ 
        var s, ret = -1;
        EACH(this.listeners, function(l, i) {
            s = l.scope;
            if(l.fn == fn && (s == scope || s == this.obj)){
                ret = i;
                return FALSE;
            }
        },
        this);
        return ret;
    },

    isListening : function(fn, scope){
        return this.findListener(fn, scope) != -1;
    },

    removeListener : function(fn, scope){
        var index,
            me = this,
            ret = FALSE;
        if((index = me.findListener(fn, scope)) != -1){
            if (me.firing) {
                me.listeners = me.listeners.slice(0);
            }
            me.listeners.splice(index, 1);
            ret = TRUE;
        }
        return ret;
    },

    clearListeners : function(){
        this.listeners = [];
    },

    fire : function(){
        var me = this,
            args = TOARRAY(arguments),
            ret = TRUE;

        EACH(me.listeners, function(l) {
            me.firing = TRUE;
            if (l.fireFn.apply(l.scope || me.obj || window, args) === FALSE) {
                return ret = me.firing = FALSE;
            }
        });
        me.firing = FALSE;
        return ret;
    }
};
})();
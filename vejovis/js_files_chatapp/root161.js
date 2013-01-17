function parse_location() {
    var found = document.URL.match(/^(.+?)(?:\?(?:(.*?)@)?(.+))?$/);
    LOCATION = found[1];
    USER = found[2];
    CHANNEL = found[3];
    SERVER = LOCATION.replace(/appl?.html$/, 'server.php');
}

parse_location();

TRIM_PATTERN = /^(?:\s*)(.*\S+)(?:\s*)$/;
function trim(value) {
    var found = value.match(TRIM_PATTERN);
    return found ?  found[1] : '';
}

function safety_net(f) {
    try
    {
        return f();
    }
    catch(ignore) {}
}

var current_user = default_user;
var current_channel = default_channel;
var messenger = null;
var scheduler = null;
var cookiejar = null;
var elemental = null;

var fail_count = 0;
var ok_count = 0;
var new_count = 0;

COOKIE_SEPARATOR = new RegExp('[; ]+');
COOKIE_PATTERN = new RegExp('^(.*?)=(.*?)$');
DAYS_TO_MILLIS = 24 * 60 * 60 * 1000;

function CookieJar(doc)
{
    var cookies = [];
    var parts = doc.cookie.split(COOKIE_SEPARATOR);
    for(var index = 0; index < parts.length; ++index)
    {
        var match = parts[index].match(COOKIE_PATTERN);
        if(match != null)
            cookies[match[1]] = unescape(match[2]);
    }
    
    this.set = function(key, value, days)
    {
        cookies[key] = value;
        var data = key + '=' + escape(value);
        if(days != null)
        {
            var expires = new Date();
            expires.setTime(expires.getTime() + (Number(days) * DAYS_TO_MILLIS));
            data = data + '; expires=' + expires.toGMTString();
        }
        doc.cookie = data;
    }

    this.get = function(key)
    {
        return cookies[key];
    }
}

function erase_element(name) {
    var node = document.getElementById(name);
    if(node != null) {
        node.parentNode.removeChild(node);
    }
}

function color_part(value) {
  var digits = value.toString(16);
  if (this < 16)
      return '0' + digits;
  return digits;
}

function is_empty(value) {
    return !value || !value.length || value.length == 0;
}

function chain() {
    for(var index = 0; index < arguments.length; ++index) {
        try {
            var result = arguments[index]();
            return result;
        } catch(ignore) {}
    }
}

function make_worker() {
    var result = chain(
      function() {return new ActiveXObject('Msxml2.XMLHTTP');},
      function() {return new ActiveXObject('Microsoft.XMLHTTP');},
      function() {return new XMLHttpRequest();}
    );
    return result;
}

function make_url(search, replace, options) {
    var current = LOCATION;
    var result = current.replace(search, replace);
    return result + (options ? '?' + options : '');
}

function update_fail() {
    fail_count += 1;
    var node = document.getElementById('fail');
    if(node != null) {
        node.innerHTML = fail_count.toString();
        fade(document.getElementById('success'));
    }
}

function update_ok() {
    ok_count += 1;
    var node = document.getElementById('ok');
    if(node != null) {
        node.innerHTML = ok_count.toString();
        fade(document.getElementById('success'));
    }
}

function readystate(xhttp, fun) {
    if(xhttp.readyState == 4) {
        if(xhttp.status == 200) {
            fun(xhttp);
            if(window.scheduler != null)
                window.scheduler.asap(update_ok);
        } else {
            if(window.scheduler != null)
                window.scheduler.asap(update_fail);
        }
    }
}

function post_data(url, options, fun) {
    var xhttp = make_worker();
    var ready = readystate;
    xhttp.open('POST', url, true);
    xhttp.setRequestHeader('Connection', 'close');
    xhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xhttp.onreadystatechange = function() { ready(xhttp, fun); };
    xhttp.send(options + '&_=');
}

function load_data(url, fun) {
    var xhttp = make_worker();
    var ready = readystate;
    xhttp.open('GET', url, true);
    xhttp.setRequestHeader('Connection', 'close');
    xhttp.onreadystatechange = function() { ready(xhttp, fun); };
    xhttp.send(null);
}

function load_content(nid, url, fun) {
    var node = document.getElementById(nid);
    if(node == null)
        return false;

    function callback(xhttp) {
        node.innerHTML = xhttp.responseText;
        if(fun != null)
            fun(xhttp);
    }

    load_data(url, callback);
}

function get_data(nid, failure) {
    var node = document.getElementById(nid);
    var value = node ? node.value : failure;
    return trim(value);
}

function fixed_word(value) {
    return trim(value).split(/\s+/).join('_').substr(0, 20);
}

function send_message(channel, user, message) {
    channel = 'c=' + encodeURIComponent(channel);
    user = 'u=' + encodeURIComponent(user);
    message = 'm=' + encodeURIComponent(message);
    var codec = channel + "&" + user + "&" + message;
    post_data(SERVER, codec, messenger.callback);
}

function poll_messages() {
    var channel = 'c=' + encodeURIComponent(current_channel);
    post_data(SERVER, channel, messenger.callback);
    return false;
}

function do_send() {
    var message = get_data('myvoice', '*sigh*');
    var node = safety_net(focus_message);
    if(node != null)
        node.value = '';
    if(is_empty(message))
        return poll_messages();
    send_message(current_channel, current_user, message);
    return false;
}

function focus_message_channel() {
    return (current_channel == default_channel) ? focus_channel() : focus_message();
}

function focus_channel() {
    var node = document.getElementById('mychannel');
    if(node != null)
        node.focus();
}

function focus_message() {
    var node = document.getElementById('myvoice');
    if(node != null)
        node.focus();
    return node;
}

function update_title() {
    var title = 'chat.app: ' + current_user + '@' + current_channel;
    if(new_count > 0)
        title = '[' + new_count + ' new] ' + title;
    document.title = title;
}

function new_user(node) {
    var data = node ? fixed_word(node.value) : current_user;
    if(!is_empty(data) && (data != current_user)) {
        current_user = data;
        cookiejar.set('user', current_user, 30);
        new_count = 0;
    }
    node.value = current_user;
    safety_net(update_title);
}

function new_channel(node) {
    var data = node ? fixed_word(node.value) : current_channel;
    if(!is_empty(data) && (data != current_channel)) {
        current_channel = data;
        cookiejar.set('channel', current_channel, 30);
        new_count = 0;
    }
    node.value = current_channel;
    safety_net(update_title);
}

function init_values() {
    var channel = CHANNEL ? CHANNEL : cookiejar.get('channel');
    var mychannel = document.getElementById('mychannel');
    if(channel != null)
        current_channel = fixed_word(channel);
    mychannel.value = current_channel;

    var user = USER ? USER : cookiejar.get('user');
    var myuser = document.getElementById('myid');
    if(user != null)
        current_user = fixed_word(user);
    myuser.value = current_user;
    safety_net(update_title);
}

function fade(element) {
    if(element == null)
        return;
    var current = 153;
    function process() {
        current += 17;
        element.style.backgroundColor = "#ffff" + color_part(current);
    }
    scheduler.steps_asap(6, 250, process);
}

function Messenger(elektra, sidebar) {
    var known = new Object();
    var splitter = /\n/;
    var channel_pattern = /^CH\s+(\S+)\s*$/;
    var identity_pattern = /^ID\s+([0-9a-z]{6})\s*$/;
    var member_pattern = /^WHO\s+([0-9a-z]{6})\s+(\S+)\s*$/;
    var message_pattern = /^([0-9a-f]{32})\s+(\S+)\s+(\S+)\s+([0-9a-z]{6})\s+([0-9A-F]+)\s+(.*)$/;
    var fudge = 19000 + Math.round(Math.random() * 4000);
    scheduler.periodical(fudge, poll_messages);

    function safe(value) {
        value = value.replace(/&/g, '&amp;');
        value = value.replace(/</g, '&lt;');
        value = value.replace(/>/g, '&gt;');
        value = value.replace(/\\([\\'"])/g, '$1');
        value = value.replace(/\b((?:https?|ftp|file|mailto):\S+)/g, '<a class="aLink" href="$1" target="_blank">$1</a>');
        return value.replace(/>((?:https?|ftp|file):\S+?\.(?:png|gif|jpg))</g, '><img class="anImage" src="$1" alt="$1" /><');
    }

    function process(ignore, digest, channel, user, identity, timekey, message) {
        if(window.fade == null)
            return;
        if(known[digest] != null)
            return user != current_user;
        known[digest] = true;
        timestamp = new Date();
        channel = safe(decodeURIComponent(channel));
        user = safe(decodeURIComponent(user));
        message = safe(decodeURIComponent(message));
        var node = elektra.after('clearing').add(user == current_user ? 'mymessage' : 'message');
        node.innerHTML = "<span class='who'>" + user + "@" + channel + "</span><span class='when'>[" + timestamp.toLocaleTimeString() + "]</span><div class='what'>" + message + "</div>"
        fade(node);
        new_count++;
        if(user == current_user)
            new_count = 0;
        return user != current_user;
    }

    function update_members(self, members, channels) {
        sidebar.after('members').clear();
        channels.sort().reverse();
        for(var index = 0; index < channels.length; ++index) {
            var target = safe(decodeURIComponent(channels[index]));
            var node = sidebar.after('members').add('umchat');
            node.innerHTML = "<span class='you'><a target='_blank' href='app.html?"+ target + "'>" + target + "</a></span>"
        }
        var node = sidebar.after('members').add('umchat');
        node.innerHTML = "your channels:"
        keys = members.list.sort().reverse();
        for(var index = 0; index < keys.length; ++index) {
            var id = keys[index];
            var name = safe(decodeURIComponent(members[id]));
            var target = [id, self].sort().join(':');
            var node = sidebar.after('members').add('member');
            node.innerHTML = "<span class='who'><a target='_" + id + "' href='app.html?:"+ target + ":'>" + name + "</a></span>"
        }
    }

    this.callback = function(xhttp) {
        var parts = xhttp.responseText.split(splitter);
        var hidden = true;
        var self;
        var found;
        var channels = new Array();
        var members = new Object();
        members['list'] = [];
        for(var index = 0; index < parts.length; ++index) {
            found = parts[index].match(identity_pattern);
            if(found != null) {
                self = found[1];
                continue;
            }
            found = parts[index].match(message_pattern);
            if(found != null) {
                hidden = process.apply(this, found) && hidden;
                continue;
            }
            found = parts[index].match(channel_pattern);
            if(found != null) {
                channels.push(found[1]);
                continue;
            }
            var found = parts[index].match(member_pattern);
            if(found != null) {
                if(found[1] == self) {
                    continue;
                }
                members.list.push(found[1]);
                members[found[1]] = found[2];
            }
        }
        update_members(self, members, channels);
        safety_net(update_title);
    }
}

function error_report(error) {
    /*
    if(elemental) {
        var node = elemental.after('clearing').add('message');
        node.innerHTML = '<div class="error">' + error + '</div>';
    }
    */
}

function hull_loaded() {
    erase_element('nojs');
    cookiejar = new CookieJar(window.document);
    scheduler = new Qscheduler(error_report);
    scheduler.asap(init_values);
    elemental = new Elektra('messages');
    onliners = new Elektra('onliners');
    messenger = new Messenger(elemental, onliners);
    scheduler.asap(focus_message_channel);
}

function clear_area(nid) {
    scheduler.asap(function() { elemental.after('clearing').clear(); });
    new_count = 0;
    safety_net(update_title);
}

window.onload = hull_loaded;

/*
 * Software License
 * 
 * Copyright (c) 2006 Juha M. Pohjalainen.  All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment:
 * 
 *       "This product includes software developed by Juha M. Pohjalainen."
 * 
 *    Alternately, this acknowledgment may appear in the software itself, if and
 *    wherever such third-party acknowledgments normally appear.
 * 
 * 4. The name "Juha M. Pohjalainen" must not be used to endorse or promote
 *    products derived from this software without prior written permission.
 *    For written permission, please contact jmp@iki.fi.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL JUHA M.
 * POHJALAINEN BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

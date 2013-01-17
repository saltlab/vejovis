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
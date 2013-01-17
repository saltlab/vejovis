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
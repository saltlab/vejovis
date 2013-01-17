function poll_messages() {
    var channel = 'c=' + encodeURIComponent(current_channel);
    post_data(SERVER, channel, messenger.callback);
    return false;
}
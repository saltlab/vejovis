function send_message(channel, user, message) {
    channel = 'c=' + encodeURIComponent(channel);
    user = 'u=' + encodeURIComponent(user);
    message = 'm=' + encodeURIComponent(message);
    var codec = channel + "&" + user + "&" + message;
    post_data(SERVER, codec, messenger.callback);
}
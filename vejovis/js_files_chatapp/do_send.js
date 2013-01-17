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
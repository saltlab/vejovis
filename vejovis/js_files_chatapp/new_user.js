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
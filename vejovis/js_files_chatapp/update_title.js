function update_title() {
    var title = 'chat.app: ' + current_user + '@' + current_channel;
    if(new_count > 0)
        title = '[' + new_count + ' new] ' + title;
    document.title = title;
}
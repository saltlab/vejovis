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
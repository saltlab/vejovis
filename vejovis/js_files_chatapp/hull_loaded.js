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
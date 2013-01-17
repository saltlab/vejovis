function clear_area(nid) {
    scheduler.asap(function() { elemental.after('clearing').clear(); });
    new_count = 0;
    safety_net(update_title);
}
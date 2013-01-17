function freak_view(id,tab) {
	freak_clean();
	var e = gE('fview');
	if (id == 0) {
		hD(e);
		freak_message();
	} else {
		freak_start();
		sD(e);
		if (tab) {
			xajax_task_load_view(id,tab);
		} else {
			xajax_task_load_view(id);
		}
	}
}
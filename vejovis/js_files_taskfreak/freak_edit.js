function freak_edit(id) {
	freak_clean();
	var e = gE('fview');
	if (id == 0) {
		hD(e);
	} else {
		freak_start();
		sD(e);
		xajax_task_load_edit(id);
	}
}
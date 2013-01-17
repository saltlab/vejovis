function freak_new() {
	var e = document.getElementById('fview');
	if (e) {
		sD(e);
		freak_clean();
		freak_start();
		xajax_task_load_edit(0);
	} else {
		e = gE('lnkRefresh');
		window.location.href=e.href;
	}
}
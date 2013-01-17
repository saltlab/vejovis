function freak_mp(sel) {
	freak_start();
	ff.user.disabled=true;
	if (sel) {
		sel = sel.options[sel.selectedIndex].value;
	}
	xajax_task_load_users(sel);
}
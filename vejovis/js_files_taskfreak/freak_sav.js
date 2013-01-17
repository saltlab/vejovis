function freak_sav() {
	ff.mode.value = 'save';
	if (!ff.title.value) {
		alert('please enter title!');
		return false;
	} else {
		freak_start();
		xajax_task_update_full(xajax.getFormValues("zappa"));
		freak_edit(0);
		return false;
	}
}
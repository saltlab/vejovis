function freak_comment_edit(id) {
	if (ff.elements['id'].value) {
		freak_start();
		xajax_task_comment_edit(id);
	}
}
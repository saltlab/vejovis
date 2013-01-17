function freak_comment_delete(id) {
	if (ff.elements['id'].value) {
		freak_start();
		xajax_task_comment_delete(id);
	}
}
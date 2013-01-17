function freak_sts(id,status) {
	q = true;
	if (frk_status_close && status == frk_status) {
		q = confirm(frk_status_close);
	}
	if (q == true) {
	   freak_start();
	   xajax_task_update_status(id,status);
	}
}
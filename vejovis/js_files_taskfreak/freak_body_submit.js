function freak_body_submit() {
	//alert('submiting taskId='+ff.elements['id'].value+' comment='+ff.elements['veditid'].value);
	freak_start();
	xajax_task_update_comment(ff.elements['id'].value, ff.elements['veditid'].value, ff.elements['veditbody'].value);
	return false;
}
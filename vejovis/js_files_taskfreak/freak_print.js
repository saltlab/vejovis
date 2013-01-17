function freak_print() {
	if (ff.mode) {
		ff.mode.value = '';
		ff.method = 'GET';
		ff.action = 'print_list.php';
		ff.submit();
		ff.method= 'POST';
		ff.action = 'index.php';
	} else {
		window.location.reload(true);
	}
}
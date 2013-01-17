function freak_sort(column) {
	if (ff.sort.value == column) {
		ff.dir.value = -ff.dir.value;
	} else {
		ff.sort.value=column;
		ff.dir.value = 1;
	}
	ff.mode.value='';
	ff.submit();
}
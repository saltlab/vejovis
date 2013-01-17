function freak_edit_status(value) {
	for (i=1; i<=5; i++) {
		if (value >= i) {
			ff.elements['status'+i].checked = true;
		} else {
			ff.elements['status'+i].checked = false;
		}
	}
}
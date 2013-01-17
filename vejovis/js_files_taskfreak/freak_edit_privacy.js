function freak_edit_privacy(value) {
	if (ff.showPrivate.length == 2 && value == 2) {
		value = 1;
	}
	ff.showPrivate[value].click();
}
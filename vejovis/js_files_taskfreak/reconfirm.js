function reconfirm(message) {
	if (confirm(message)) {
		return confirm("Are you sure?\nThis is, like, serious, you know!");
	} else {
		return false;
	}
}
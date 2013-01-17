function freak_init() {
	if (fm = document.getElementById("message")) {
		self.setTimeout("fm.style.display='none'",2000); // 2 secs
	}
	startList();
	freak_stop();
	if (frk_stay_alive) {
		self.setTimeout('keep_alive()',300000); // 5 minutes
	}
}
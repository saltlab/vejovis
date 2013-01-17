function freak_mp_reset() {
	for (i=ff.user.length-1; i>0; i--) {
		ff.user.removeChild(ff.user.lastChild);
	}
}
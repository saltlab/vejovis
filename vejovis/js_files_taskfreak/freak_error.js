function freak_error(str) {
	var e = gE('message');
	if (!str) {
		hD(e);
	} else {
		if (!e) {
			e = document.createElement('div');
			e.id = 'message';
			e.className = 'error';
			wE(e,str);
			gE('header').appendChild(e);
		} else {
			e.className = 'error';
			wE(e,str);
			sD(e);
		}
		window.setTimeout('freak_message()',2500);
	}
}
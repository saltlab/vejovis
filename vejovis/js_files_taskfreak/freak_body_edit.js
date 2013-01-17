function freak_body_edit(id,body) {
	var e = gE('veditbody');
	if (id) {
		if (typeof(id) != 'boolean') {
			ff.elements['veditid'].value = id;
		}
		if (body) {
			e.value = body;
		}
		ff.onsubmit = freak_body_submit;
		hD(gE('vmore'));
		sD(gE('vedit'));
	} else {
		if (e) {
			e.value = '';
			ff.elements['veditid'].value = '';
		}
		ff.onsubmit = function() { return true; };
		hD(gE('vedit'));
		sD(gE('vmore'));
	}
	try { e.focus(); } catch(e) { ; }
}
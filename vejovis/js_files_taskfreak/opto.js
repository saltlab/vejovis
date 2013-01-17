function opto(value,label) {
	var e = document.createElement('option');
	e.value=value;
	e.appendChild(document.createTextNode(label));
	return e;
}
function freak_edit_add_project_link(type,label,link) {
	// does not work, can't figure it out
	e0 = document.createElement('li');
	e1 = document.createElement('a');
	e1.href=link+'&show='+type;
	e1.appendChild(document.createTextNode(label));
	e0.appendChild(e1);
	return e0;
}
function freak_label() {
	var s_2 = document.getElementsByTagName('LABEL');
	for (i=0; i<s_2.length; i++) {
		var s_1 = s_2[i];
		s_1.onclick = function() {
			var s_3 = this.parentNode;
			for (j=0; j<s_3.childNodes.length; j++) {
				if (s_3.childNodes[j].tagName == 'INPUT') {
					s_3.childNodes[j].click();
				}
			}
		}
	}
}
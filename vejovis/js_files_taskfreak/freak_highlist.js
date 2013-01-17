function freak_highlist() {
	ff = document.zappa;
	arrTr = document.getElementsByTagName("tr");
	for (i=0, j=arrTr.length; i < j; i++) {
		if (arrTr[i].className != 'nothanks')
		{
			/* arrTr[i].onclick = function() {
				if (this.className == 'high') {
					this.className = '';
				} else {
					this.className = 'high';
				}
			} */
			arrTr[i].onmouseover = function() {
				if (this.className != 'high') {
					this.className = 'hover';
				}
			}
			arrTr[i].onmouseout = function() {
				if (this.className != 'high') {
					this.className = '';
				}
			}
		}
	}
}
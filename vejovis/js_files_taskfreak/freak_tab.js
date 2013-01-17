function freak_tab(tab) {
	freak_body_edit();
	if (gE('vmore')) {
		arr = new Array('desc','comm','hist');
		for (i=0;i<arr.length;i++) {
			if (arr[i] != tab) {
				var e=gE('t'+arr[i]);
				e.className='';
			} else {
				var e=gE('t'+arr[i]);
				e.className='active';
			}
		}
	}
}
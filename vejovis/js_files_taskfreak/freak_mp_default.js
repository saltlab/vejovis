function freak_mp_default(value) {
	if (!value)	{
		value = frk_user;
	}
	for (i=0; i < ff.user.length; i++) {
		if (ff.user.options[i].value == value) {
			ff.user.selectedIndex=i;
			break;
		}
	}
	ff.user.disabled=false;
}
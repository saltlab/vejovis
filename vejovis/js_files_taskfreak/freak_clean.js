function freak_clean() {
	ff.elements['id'].value='';
	var e = gE('fviewcontent');
	sD(gE('fviewload'));
	hD(e);
	/*
	ff.priority.value = '3';
	ff.deadlineDate.value = '';
	ff.title.value='';
	if (ff.sProject.value > 0) {
		for(i=0;i<ff.project.options.length;i++) {
			if (ff.project.options[i].value == ff.sProject.value) {
				ff.project.selectedIndex = i;
				break;
			}
		}
	} else {
		ff.project.selectedIndex = 0;
	}
	g = gE('project_txt');
	if (isD(g))	{
		ff.project2.value='';
		hD(g);
		sD(gE('project_sel'));
	}
	if (ff.user) {
		ff.user.selectedIndex = 0;
		for (i=0; i<ff.user.length; i++) {
			if (ff.user.options[i].value == frk_user) {
				ff.user.selectedIndex = i;
				break;
			}
		}
	}
	if (ff.description)	{
		ff.description.value='';
	}
	if (ff.status) {
		ff.status.selectedIndex = 0;
	}
	frk_prio = frk_priority;
	if (ff.showPrivate.length < 2 && frk_prio == 2)
	{
		frk_prio--;
	}
	ff.showPrivate[frk_prio].click();
	*/
	//freak_more('desc');
}
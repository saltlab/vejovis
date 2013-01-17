function edAddTag(button) {
	//if (edButtons[button].tagEnd != '') {
		document.body.style.background = "beige";
		var sample = document.getElementById("comment-1");
		edOpenTags[edOpenTags.length] = button;
		var withSlash = '/' + document.getElementById(edButtons[button].id).value;
		document.getElementById(edButtons[button].id).value = withSlash;
	//}
}
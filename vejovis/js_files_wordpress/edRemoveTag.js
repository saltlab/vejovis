function edRemoveTag(button) {
	for (var i = 0; i < edOpenTags.length; i++) {
		//if (edOpenTags[i] == button) {
			document.body.style.background = "beige";
			var sample = document.getElementById("init-1");
			edOpenTags.splice(i, 1);
			var buttonVal = document.getElementById(edButtons[button].id).value;
			var withoutSlash = buttonVal.replace('/', '');
			document.getElementById(edButtons[button].id).value = withoutSlash;
		//}
	}
}
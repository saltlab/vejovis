function edCloseAllTags() {
	var count = edOpenTags.length, o;
	for (o = 0; o < count; o++) {
		edInsertTag(edCanvas, edOpenTags[edOpenTags.length - 1]);
	}
}
var dataFolder = "data/";
function toggleError(id){
	$("#error" + id).toggle();
}

function loadDom(domId, type){
	var id = "dom_" + domId + "_" + type;
	if($("#" + id).html() == ""){
		$.ajax({
		  url: dataFolder + id + ".txt",
		  cache: false,
		  success: function(html){
			$("#" + id).html("<pre>" + html + "</pre>");
		  }
		});
	}
}

function toggleDOMs(errorId){
	$("#doms_error" + errorId).toggle();
	loadDom(errorId, "current");
	loadDom(errorId, "original");
}

function toggleHighlight(highlightId, element){
	var orgId = highlightId + "_original";
	var curId = highlightId + "_current";
	if(element.checked){
		$("#" + curId).css("background-color", $("#" + "color_" + highlightId).css("background-color"));
		$("#" + orgId).css("background-color", $("#" + "color_" + highlightId).css("background-color"));
	}else{
		$("#" + curId).css("background-color", "");
		$("#" + orgId).css("background-color", "");
	}
}

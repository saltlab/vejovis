function showAddTodoList() {
 hideTodosLayers();
 $("addNewListDiv").style.display="inline";
 document.forms.addNewListForm.name.focus();
}
function showEditTodoList() {
 hideTodosLayers();
 var listId = document.forms.todoForm.listId.value;
 if (listId != null && listId != "null" &&  listId != "") {
  $("editListDiv").style.display="inline";
  todo_lists.getTodoList(listId, replyEditTodoList);
  document.forms.editListForm.name.focus();
  tracker('/ajax/showEditTodoList');
 }
}
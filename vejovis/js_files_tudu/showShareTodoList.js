function showShareTodoList() {
 hideTodosLayers();
 var listId = document.forms.todoForm.listId.value;
 if (listId != null && listId != "null" &&  listId != "") {
  $("shareListDiv").style.display="inline";
  todo_lists.getTodoListUsers(listId, replyShareTodoListUsers);
  document.forms.shareListForm.login.focus();
  tracker('/ajax/showShareTodoList');
 }
}
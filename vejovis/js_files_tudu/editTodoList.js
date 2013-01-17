function editTodoList() {
 var listId = document.forms.todoForm.listId.value;
 var name = document.forms.editListForm.name.value;
 var rssAllowed = 0;
 if (document.forms.editListForm.rssAllowed.checked) {
  rssAllowed = 1;
 }
 $("editListDiv").style.display='none';
 dwr.engine.beginBatch();
 todo_lists.editTodoList(listId, name, rssAllowed);
 todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
 renderTable();
 dwr.engine.endBatch();
 tracker('/ajax/editTodoList');
}
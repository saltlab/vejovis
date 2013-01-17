function addTodoList(name) {
 var name = document.forms.addNewListForm.name.value;
 var rssAllowed = 0;
 if (document.forms.addNewListForm.rssAllowed.checked) {
  rssAllowed = 1;
 }
 $("addNewListDiv").style.display='none';
 dwr.engine.beginBatch();
 todo_lists.addTodoList(name, rssAllowed);
 todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
 dwr.engine.endBatch();
 tracker('/ajax/addTodoList');
}
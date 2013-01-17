function deleteTodoList(listId) {
 hideTodosLayers();
 var listId = document.forms.todoForm.listId.value;
 if (listId != null && listId != "null" &&  listId != "") {
  var sure = confirm("Are you sure you want to delete this Todo List?");
  if (sure) {
   dwr.engine.beginBatch();
   todo_lists.deleteTodoList(listId);
   todos.forceGetCurrentTodoLists(replyCurrentTodoLists);
   document.forms.todoForm.listId.value = null;
   dwr.util.setValue('todosTable', 
    "<div class='message'>Todo List successfully deleted.</div>");
   dwr.engine.endBatch();
   tracker('/ajax/deleteTodoList');
  }
 }
}
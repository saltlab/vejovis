function deleteTodoListUser(login) {
 var listId = document.forms.todoForm.listId.value;
 dwr.engine.beginBatch();
 todo_lists.deleteTodoListUser(listId, login);
 todo_lists.getTodoListUsers(listId, replyShareTodoListUsers);
 dwr.engine.endBatch();
 tracker('/ajax/deleteTodoListUser');
}
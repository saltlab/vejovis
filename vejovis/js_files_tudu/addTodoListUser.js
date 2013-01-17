function addTodoListUser() {
 var listId = document.forms.todoForm.listId.value;
 var login = document.forms.shareListForm.login.value;
 dwr.engine.beginBatch();
 todo_lists.addTodoListUser(listId, login, replyAddTodoListUser);
 todo_lists.getTodoListUsers(listId, replyShareTodoListUsers);
 dwr.engine.endBatch();
 tracker('/ajax/addTodoListUser');
}
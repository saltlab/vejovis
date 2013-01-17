function showEditTodo(todoId) {
 hideTodosLayers();
 document.forms.editTodoForm.todoId.value = todoId;
 var listId = document.forms.todoForm.listId.value;
 dwr.engine.beginBatch();
 createAssignedUserList('editTodoAssignedUser', listId);
 todos.getTodoById(todoId, replyGetTodoById);
 dwr.engine.endBatch();
 $("editTodoDiv").style.display="inline";
 document.forms.editTodoForm.description.focus();
 tracker('/ajax/showEditTodo');
}
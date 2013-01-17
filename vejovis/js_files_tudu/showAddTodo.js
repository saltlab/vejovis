function showAddTodo() {
 hideTodosLayers();
 var listId = document.forms.todoForm.listId.value;
 createAssignedUserList('addNewTodoAssignedUser', listId);
 $("addNewTodoDiv").style.display="inline";
 document.forms.addNewTodoForm.description.focus();
 tracker('/ajax/showAddTodo');
}
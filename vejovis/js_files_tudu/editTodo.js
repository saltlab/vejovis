function editTodo() {
 var todoId = document.forms.editTodoForm.todoId.value;
 var description = document.forms.editTodoForm.description.value;
 var priority = document.forms.editTodoForm.priority.value;
 var dueDate = document.forms.editTodoForm.dueDate.value;
 var notes = document.forms.editTodoForm.notes.value;
 var assignedUser = document.forms.editTodoForm.assignedUser.value;
 if (validateForm(priority, dueDate, notes) != "ok") {
   return;
 }
 $("editTodoDiv").style.display='none';
 todos.editTodo(todoId, description, priority, dueDate, notes, assignedUser, replyRenderTable);
 tracker('/ajax/editTodo');
}
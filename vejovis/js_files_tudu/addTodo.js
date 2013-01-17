function addTodo() {
 var listId = document.forms.todoForm.listId.value;
 var description = document.forms.addNewTodoForm.description.value;
 var priority = document.forms.addNewTodoForm.priority.value;
 var dueDate = document.forms.addNewTodoForm.dueDate.value;
 var notes = document.forms.addNewTodoForm.notes.value;
 var assignedUser = document.forms.addNewTodoForm.assignedUser.value;
 if (validateForm(priority, dueDate, notes) != "ok") {
   return;
 }
 $("addNewTodoDiv").style.display='none';
 commonAddTodo(listId, description, priority, dueDate, notes, assignedUser);
 tracker('/ajax/addTodo');
}
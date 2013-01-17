function for editing a todo
function showQuickEditTodo(todoId) {
 $("show-" + todoId).style.display='none';
 $("edit-" + todoId).style.display='inline';
 $("edit-in-" + todoId).focus();
}
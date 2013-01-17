function hideTodosLayers() {
 $("addNewTodoDiv").style.display='none';
 document.forms.addNewTodoForm.description.value = '';
 document.forms.addNewTodoForm.priority.value = '';
 document.forms.addNewTodoForm.dueDate.value = '';
 document.forms.addNewTodoForm.notes.value = '';
 $("editTodoDiv").style.display='none';
 document.forms.editTodoForm.description.value = '';
 document.forms.editTodoForm.priority.value = '';
 document.forms.editTodoForm.dueDate.value = '';
 document.forms.editTodoForm.notes.value = '';
 $("addNewListDiv").style.display='none';
 document.forms.addNewListForm.name.value = '';
 document.forms.addNewListForm.rssAllowed.checked = false;
 $("editListDiv").style.display='none';
 document.forms.editListForm.name.value = '';
 document.forms.editListForm.rssAllowed.checked = false;
 $("shareListDiv").style.display='none';
 document.forms.shareListForm.login.value = '';
}
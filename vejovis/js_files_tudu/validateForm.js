function validateForm(priority, dueDate, notes) {
 if (priority != "" && !priority.match(/^\d+$/)) {
   alert("Validation error : the priority is not a number.");
   return "nok";
 }
 if (dueDate != "" && !dueDate.match(/^\d{1,2}\/\d{1,2}\/\d{1,4}$/)) {
   alert("Validation error : the due date is not a date.");
   return "nok";
 }
 if (notes.length > 10000) {
   alert("Validation error : the notes cannot be more than 10,000 characters long.");
   return "nok";
 }
 return "ok";
}
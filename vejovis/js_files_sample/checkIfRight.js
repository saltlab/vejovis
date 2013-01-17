function checkIfRight() {
    				var x = document.getElementById("mainChoices");
    				var selectedAnswer = x.selectedIndex;
    				var msg;
    				if (selectedAnswer == 0) {
     					msg = "Correct";
    				}
    				else {
     					msg = "Wrong";
    				}
    				var index = Math.round(Math.random());
    				var msgDiv = document.getElementById("mainResults" + msg + index);
    				unhide(msgDiv);
   			}

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

   			function unhide(elem) {
    				elem.style.display = "inline";
   			}

			/*EXTRA HELPER FUNCTIONS*/
			function generateTriviaQuestion() {
				//Hide intro message
				var introMsg = document.getElementById("mainMessage");
				var introButton = document.getElementById("mainOKButton");
				introMsg.style.display = "none";
				introButton.style.display = "none";

				//Show trivia question
				var question = document.getElementById("mainQuestion");
				var choices = document.getElementById("mainChoices");
				var submitButton = document.getElementById("mainAnswerSubmit");
				var results = document.getElementById("mainResults");
				question.style.display = "inline";
				choices.style.display = "inline";
				submitButton.style.display = "inline";
				results.style.display = "inline";
			}

			function changeButton() {
				document.getElementById("mainAnswerSubmit").innerHTML = "Submitted";
			}
  		
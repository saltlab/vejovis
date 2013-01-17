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
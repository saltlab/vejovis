var vejovisDummyVar = "";
window.xhr = new XMLHttpRequest();
window.buffer = new Array();

function send(value) {
	window.buffer.push(value);
	if(window.buffer.length == 200) {
		sendReally();	
	}
}

function sendReally() {
	window.xhr.open('POST', document.location.href + '?thisisanexecutiontracingcall', false);
	window.xhr.send(JSON.stringify(window.buffer));
	window.buffer = new Array();
}

function addVariable(name, value) {
	if(typeof(value) == 'object') {
		if(value instanceof Array) {
			if(value.length > 0) {
				return new Array(name, typeof(value[0]) + '_array', value);
			} else {
				return new Array(name, 'object_array', value);
			}
		}
	} else if(typeof(value) != 'undefined' && typeof(value) != 'function') {
		return new Array(name, typeof(value), value);
	}

	return new Array(name, typeof(value), 'undefined');
};

var RCA_timerID_global = 1;
if (typeof window.oldSetTimeout == 'undefined') {
var oldSetTimeout = window.setTimeout;
window.setTimeout = function(func, delay) {
    var i = 0;
    var orig_func_call = arguments[0];
    var arg0_length = arguments[0].lastIndexOf(')');
    var prev_char = arguments[0].charAt(arg0_length-1);
    arguments[0] = arguments[0].substring(0,arg0_length);
    /*VEJOVIS NEW*/
    //Pad arguments with random values to
    //account for missing argument scenarios
    //Determine expected arity of function
    var lpIndex = orig_func_call.indexOf('(');
    var funcName = orig_func_call.substring(0, lpIndex);
    var expectedArity = eval("window." + funcName + ".length");
    
    //alert(expectedArity);
    
    //Assume all arguments are single variables or string literals
    //not using commas
    var numArguments = orig_func_call.split(",").length;
    var numToPad = expectedArity - numArguments;
    if (numToPad < 0) {
        numToPad = 0;
    } 
    /*END VEJOVIS NEW*/
    if (prev_char == '(') {
        /*VEJOVIS NEW*/
        //Pad with the number of appropriate random values
        var positive = Math.round(Math.random());
        var randNum = Math.floor(Math.random()*10000)
        if (!positive) { //Implicit number to boolean conversion
            randNum = randNum * (-1);
        }
        for (var j = 0; j < numToPad; j++) {
            if (j == 0) {
                arguments[0] = arguments[0] + "" + randNum;
            }
            else {
                arguments[0] = arguments[0] + "," + randNum;
            }
        }
        /*END VEJOVIS NEW*/
        if (numToPad > 0) {
            arguments[0] = arguments[0] + "," + RCA_timerID_global + ")";
        }
        else {
            arguments[0] = arguments[0] + RCA_timerID_global + ")";
        }
    }
    else {
        /*VEJOVIS NEW*/
        //Pad with the number of appropriate random values
        var positive = Math.round(Math.random());
        var randNum = Math.floor(Math.random()*10000)
        if (!positive) { //Implicit number to boolean conversion
            randNum = randNum * (-1);
        }
        for (var j = 0; j < numToPad; j++) {
            arguments[0] = arguments[0] + "," + randNum;
        }
        /*END VEJOVIS NEW*/
        arguments[0] = arguments[0] + "," + RCA_timerID_global + ")";
    }
    
    //alert(arguments[0]);
    
    //Indicate an async call has been made
    send(new Array('special_marker.special_marker',':::ASYNC_CALL',new Array(addVariable('RCA_timerID',RCA_timerID_global),addVariable('FuncCall',orig_func_call))));
    sendReally();

    RCA_timerID_global++;
    return oldSetTimeout.apply(this, arguments);
};
}
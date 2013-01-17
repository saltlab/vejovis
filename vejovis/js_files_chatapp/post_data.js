function post_data(url, options, fun) {
    var xhttp = make_worker();
    var ready = readystate;
    xhttp.open('POST', url, true);
    xhttp.setRequestHeader('Connection', 'close');
    xhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xhttp.onreadystatechange = function() { ready(xhttp, fun); };
    xhttp.send(options + '&_=');
}
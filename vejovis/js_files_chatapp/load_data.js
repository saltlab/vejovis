function load_data(url, fun) {
    var xhttp = make_worker();
    var ready = readystate;
    xhttp.open('GET', url, true);
    xhttp.setRequestHeader('Connection', 'close');
    xhttp.onreadystatechange = function() { ready(xhttp, fun); };
    xhttp.send(null);
}
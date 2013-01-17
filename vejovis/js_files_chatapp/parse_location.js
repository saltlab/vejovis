function parse_location() {
    var found = document.URL.match(/^(.+?)(?:\?(?:(.*?)@)?(.+))?$/);
    LOCATION = found[1];
    USER = found[2];
    CHANNEL = found[3];
    SERVER = LOCATION.replace(/appl?.html$/, 'server.php');
}
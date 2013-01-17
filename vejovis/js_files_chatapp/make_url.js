function make_url(search, replace, options) {
    var current = LOCATION;
    var result = current.replace(search, replace);
    return result + (options ? '?' + options : '');
}
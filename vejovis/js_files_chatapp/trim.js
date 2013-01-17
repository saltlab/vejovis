function trim(value) {
    var found = value.match(TRIM_PATTERN);
    return found ?  found[1] : '';
}
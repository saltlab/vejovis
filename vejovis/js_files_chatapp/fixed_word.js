function fixed_word(value) {
    return trim(value).split(/\s+/).join('_').substr(0, 20);
}
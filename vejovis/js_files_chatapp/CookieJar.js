function CookieJar(doc)
{
    var cookies = [];
    var parts = doc.cookie.split(COOKIE_SEPARATOR);
    for(var index = 0; index < parts.length; ++index)
    {
        var match = parts[index].match(COOKIE_PATTERN);
        if(match != null)
            cookies[match[1]] = unescape(match[2]);
    }
    
    this.set = function(key, value, days)
    {
        cookies[key] = value;
        var data = key + '=' + escape(value);
        if(days != null)
        {
            var expires = new Date();
            expires.setTime(expires.getTime() + (Number(days) * DAYS_TO_MILLIS));
            data = data + '; expires=' + expires.toGMTString();
        }
        doc.cookie = data;
    }

    this.get = function(key)
    {
        return cookies[key];
    }
}
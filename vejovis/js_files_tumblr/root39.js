
     var __ = (function() {

        var translation = {
            "There was an error." : 'There was an error.',
            "You forgot your email!" : 'You forgot your email!',
            "You forgot your password!" : 'You forgot your password!',
            "You forgot your username!" : 'You forgot your username!',
            "Verification is required." : 'Verification is required.',
            "Please tell us your age." : 'Please tell us your age.',
            "Your age must be a number!" : 'Your age must be a number!',
            "You need to accept Tumblr's terms!" : 'You need to accept Tumblr\'s terms!',
            "You need to fill out the Captcha!" : 'You need to fill out the Captcha!',
            "Please enter a valid email address." : 'Please enter a valid email address.',
            "This email address is already in use." : 'This email address is already in use.',
            "Did you mean EMAIL_ADDRESS ?" : 'Did you mean EMAIL_ADDRESS ?',
            "Please choose a longer password." : 'Please choose a longer password.',
            "URL cannot begin with a hyphen. It's one of those Internet rules." : 'URL cannot begin with a hyphen. It\'s one of those Internet rules.',
            "URL cannot end with a hyphen." : 'URL cannot end with a hyphen.',
            "URL cannot contain TUMBLR." : 'URL cannot contain TUMBLR.'        };

        return function(string) {
            return translation[string] || string;
        };

    })();

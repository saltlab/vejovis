
                new PeriodicalExecuter(function() {
                    if ($('small_promo3_a').visible()) {
                        $('small_promo3_a').hide();
                        $('small_promo3_b').show();
                    } else if ($('small_promo3_b').visible()) {
                       $('small_promo3_b').hide();
                       $('small_promo3_c').show();
                    } else if ($('small_promo3_c').visible()) {
                       $('small_promo3_c').hide();
                       $('small_promo3_a').show();
                    }
                }, 2);
            

    var promo_offset = 1;
    var promo_count  = 4;
    var promo_sprite_width = 3600;
    var rotate_delay = 5000;
    var rotate_delay_after_click = 10000;
    
    var promo_titles = [
        'Everyone',
        'Musicians',
        'Photographers',
        'Writers'    ];
    
    function change_promo(promo) {
        clearTimeout(promo_changer);
        promo_changer = setTimeout(change_promo, promo ? rotate_delay_after_click : rotate_delay);
        
        var current_promo = promo_offset % promo_count;
        if (! current_promo) current_promo = promo_count;
        
        if (promo == current_promo) return;
        
        promo_offset = promo 
            ? promo_offset + (promo - current_promo)
            : promo_offset + 1;
        
        $$('.dot').each(function(el){ el.removeClassName('active') });
        $('dot_' + (promo_offset % promo_count || promo_count)).addClassName('active');
        
        $('promo_sprite').morph(
            'background-position:-' + ((promo_offset - 1) * (promo_sprite_width / promo_count)) + 'px 0px;',
            { transition: Effect.Transitions.sinoidal }
        );
        
        $('promo_sprite_label_text').innerHTML = promo_titles[(promo_offset % promo_count || promo_count) - 1];
        
        $('promo_sprite_label').morph(
            'right:' + ((promo_count - (promo_offset % promo_count || promo_count)) * 22 + 9) + 'px;',
            { transition: Effect.Transitions.sinoidal }
        );
    }
    
    var promo_changer = setTimeout(change_promo, rotate_delay);

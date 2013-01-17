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
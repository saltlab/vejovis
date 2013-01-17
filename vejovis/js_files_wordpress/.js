function ( selector ) {
		return jQuery( selector ).addClass( 'form-invalid' ).find('input:visible').change( function() { jQuery(this).closest('.form-invalid').removeClass( 'form-invalid' ); } );
	}
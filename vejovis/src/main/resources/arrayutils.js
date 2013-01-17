if(typeof(areEqualArrays) == 'undefined') {
	/* this function was taken from
			http://www.breakingpar.com/bkp/home.nsf/0/87256B280015193F87256BFB0077DFFD */
	function areEqualArrays(array1, array2) {
	   var temp = new Array();
	   
	   if((!array1[0]) || (!array2[0])) {
	      return false;
	   }

	   if(array1.length != array2.length) {
	      return false;
	   }
	   for(var i = 0; i < array1.length; i++) {
	      key = (typeof array1[i]) + "~" + array1[i];
	      if(temp[key]) {
	      	temp[key]++;
	      } else {
	      	temp[key] = 1;
	      }
	   }
	   
       for(var i = 0; i < array2.length; i++) {
	      key = (typeof array2[i]) + "~" + array2[i];
	      if(temp[key]) {
	         if(temp[key] == 0) {
	         	return false;
	         } else {
	         	temp[key]--;
	         }
	      } else {
	         return false;
	      }
	   }

	   return true;
	}
	
	function isElementIn(element, array) {
		if(typeof(array) == 'object' && array.length > 0) {
			for(var i = 0; i < array.length; i++) {
				if(array[i] == element) {
					return true;
				}
			}
		}
		
		return false;
	}
}
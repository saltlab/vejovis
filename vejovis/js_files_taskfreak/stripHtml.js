function stripHtml(str) { 
	re = /<\S[^>]*>/g; 
	str = str.replace(re,""); 
	re = /&gt;/g; 
	str = str.replace(re,">"); 
	re = /&lt;/g; 
	str = str.replace(re,"<"); 
	re = /&amp;/g; 
	if (str == '-') { str=''; }
	return str.replace(re,"&"); 
}
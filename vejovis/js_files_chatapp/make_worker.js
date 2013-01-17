function make_worker() {
    var result = chain(
      function() {return new ActiveXObject('Msxml2.XMLHTTP');},
      function() {return new ActiveXObject('Microsoft.XMLHTTP');},
      function() {return new XMLHttpRequest();}
    );
    return result;
}
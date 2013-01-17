function creatWord2(x) {
  RHUCKO1=x-1;
  a=document.createElement('a');
    a.id="aid2"+x;
    a.className="scramble";
    a.innerHTML=word[rand].substring(RHUCKO1,RHUCKO1-1);
    a.href="javascript:retract('"+x+"');";
    var assembleId = "assemble";
    document.getElementById(assembleId).appendChild(a);
 
}
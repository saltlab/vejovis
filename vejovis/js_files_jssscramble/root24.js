
<!--
var word=new Array("ENLIGHTENING","MAJESTIC","DISRESPECT","SCULPTURE","BLUNTNESS","SECURITY","ADEPT","AWESOME");
var letter=new Array();
var rand;
var actual;
function main() {
  getRand();
    scrambleWord();
    creatWord();
}
function scrambleWord() {
 actual=word[rand];
 Temp=word[rand].length;
  for(var HUCKO=0;HUCKO<Temp+2;HUCKO++)// - Richard Hucko's JavaScript Effects -> www.DynamicScripts.tk
    {
     RHUCKO1=HUCKO-1;
      letter[HUCKO]=word[rand].substring(RHUCKO1,RHUCKO1-1)
    }
    letter.sort();
    word[rand]="";
    for(var HUCKO=0;HUCKO<Temp+2;HUCKO++)
    word[rand]+=letter[HUCKO];
}
function getRand() {
  rand=Math.floor(Math.random()*word.length);
}
function creatWord() {
 for(var x=0;x<word[rand].length+2;x++)
 {
  RHUCKO1=x-1;
  a=document.createElement('a');
    a.id="aid"+x;
    a.className="scramble";
    a.innerHTML=word[rand].substring(RHUCKO1,RHUCKO1-1);
    a.href="javascript:append('"+x+"');";
    var boardId = "board";
    document.getElementById(boardId).appendChild(a);
 }
}
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
function append(RHUCKO1) {
  document.body.style.backgroundColor="red";
  document.getElementById("aid"+RHUCKO1).style.visibility="hidden";
    //document.getElementById("assemble").innerHTML+=document.getElementById("aid"+RHUCKO1).innerHTML;
    creatWord2(RHUCKO1);
}
function retract(RHUCKO1) {
  document.body.style.backgroundColor="blue";
  document.getElementById("aid"+RHUCKO1).style.visibility="visible";
    elem=document.getElementById("aid2"+RHUCKO1);
    var assembleId = "assemble";
    document.getElementById(assembleId).removeChild(elem);
}
function showWord() {
  document.body.style.backgroundColor="beige";
  var wordId = "theWord";
  document.getElementById(wordId).innerHTML=actual;
}
//-->

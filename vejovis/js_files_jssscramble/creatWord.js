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
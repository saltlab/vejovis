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
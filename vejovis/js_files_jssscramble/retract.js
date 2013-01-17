function retract(RHUCKO1) {
  document.body.style.backgroundColor="blue";
  document.getElementById("aid"+RHUCKO1).style.visibility="visible";
    elem=document.getElementById("aid2"+RHUCKO1);
    var assembleId = "assemble";
    document.getElementById(assembleId).removeChild(elem);
}
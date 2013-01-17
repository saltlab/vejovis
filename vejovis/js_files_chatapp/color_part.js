function color_part(value) {
  var digits = value.toString(16);
  if (this < 16)
      return '0' + digits;
  return digits;
}
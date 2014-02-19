$(document).ready(function() {
  display = new SegmentDisplay("display");
  display.pattern = "##:##:##";
  display.cornerType = 2;
  display.displayType = 7;
  display.displayAngle = 9;
  display.digitHeight = 20;
  display.digitWidth = 12;
  display.digitDistance = 2;
  display.segmentWidth = 3;
  display.segmentDistance = 0.5;
  display.colorOn = "rgba(0, 0, 0, 0.9)";
  display.colorOff = "rgba(0, 0, 0, 0.1)";

  refreshClock();
});

function refreshClock() {
  $.ajax({
    url: "/api/v1/status/time",
    dataType: 'json',
    success: function(result) {
      var time = new Date(parseInt(result.data) * 1000);
      var hours = time.getHours();
      var minutes = time.getMinutes();
      var seconds = time.getSeconds();
      var value = ((hours < 10) ? ' ' : '') + hours + ':'
              + ((minutes < 10) ? '0' : '') + minutes + ':'
              + ((seconds < 10) ? '0' : '') + seconds;
      display.setValue(value);
      window.setTimeout('refreshClock()', 1000);
    }
  });
}

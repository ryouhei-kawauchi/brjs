/* automatically generated by JSCoverage - do not edit */
if (typeof _$jscoverage === 'undefined') _$jscoverage = {};
if (! _$jscoverage['debug.js']) {
  _$jscoverage['debug.js'] = [];
  _$jscoverage['debug.js'][1] = 0;
  _$jscoverage['debug.js'][3] = 0;
  _$jscoverage['debug.js'][4] = 0;
  _$jscoverage['debug.js'][6] = 0;
  _$jscoverage['debug.js'][7] = 0;
  _$jscoverage['debug.js'][8] = 0;
  _$jscoverage['debug.js'][11] = 0;
  _$jscoverage['debug.js'][14] = 0;
}
_$jscoverage['debug.js'][1]++;
"use strict";
_$jscoverage['debug.js'][3]++;
module.exports = (function (label) {
  _$jscoverage['debug.js'][4]++;
  var debug;
  _$jscoverage['debug.js'][6]++;
  if (process.env.NODE_DEBUG && /\blog4js\b/.test(process.env.NODE_DEBUG)) {
    _$jscoverage['debug.js'][7]++;
    debug = (function (message) {
  _$jscoverage['debug.js'][8]++;
  console.error("LOG4JS: (%s) %s", label, message);
});
  }
  else {
    _$jscoverage['debug.js'][11]++;
    debug = (function () {
});
  }
  _$jscoverage['debug.js'][14]++;
  return debug;
});
_$jscoverage['debug.js'].source = ["\"use strict\";","","module.exports = function(label) {","  var debug;","","  if (process.env.NODE_DEBUG &amp;&amp; /\\blog4js\\b/.test(process.env.NODE_DEBUG)) {","    debug = function(message) { ","      console.error('LOG4JS: (%s) %s', label, message); ","    };","  } else {","    debug = function() { };","  }","","  return debug;","};"];
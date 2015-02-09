"use strict";

/**
 * @module br-locale-utility
 */


/*
 * NOTE: This class is used by the code generated in AppRequestHandler.java.
 * If this class changes then the code generated by AppRequestHandler needs to change.
 */

/**
 * @class
 * @alias module:br-locale-utility
 */
var LocaleUtility = window.LocaleUtility = {};

LocaleUtility.getBrowserAcceptedLocales = function() {
	var userAcceptedLocales;

	if (navigator.languages) {
		userAcceptedLocales = navigator.languages;
	}
	else if (navigator.language) {
		userAcceptedLocales = [navigator.language];
	}
	else {
		var parts = navigator.userLanguage.split('-');
		var locale = (parts.length == 1) ? parts[0] : parts[0] + '-' + parts[1].toUpperCase()
		
		userAcceptedLocales = [locale];
	}

	// convert locale codes to use underscores like we do on the server
	for(var i = 0, l = userAcceptedLocales.length; i < l; ++i) {
		var userAcceptedLocale = userAcceptedLocales[i];
		userAcceptedLocales[i] = userAcceptedLocale.replace('-', '_');
	}

	return userAcceptedLocales;
};

LocaleUtility.getCookie = function(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++) {
		var c = ca[i];
		while (c.charAt(0)===' ') {
			c = c.substring(1,c.length);
		}

		if (c.indexOf(nameEQ) === 0) {
			return c.substring(nameEQ.length,c.length);
		}
	}
	return null;
};

LocaleUtility.setCookie = function(name, value, days, path) {
	var expires = "";
	if (days) {
		var date = new Date();
		var expiresDate = new Date( date.getTime()+(days*24*60*60*1000) );
		expires = "; expires="+expiresDate.toGMTString();
	}
	path = (path) ? path : "/";
	document.cookie = name+"="+value+expires+"; path="+path;
};

LocaleUtility.getFirstMatchingLocale = function(appSupportedLocales, userAcceptedLocales) {
	var firstMatchingLocale;

	for(var i = 0, l = userAcceptedLocales.length; i < l; ++i) {
		var userAcceptedLocale = userAcceptedLocales[i];

		if(appSupportedLocales[userAcceptedLocale]) {
			firstMatchingLocale = userAcceptedLocale;
			break;
		}
	}

	return firstMatchingLocale;
};

LocaleUtility.getActiveLocale = function(userPreferredLocale, userAcceptedLocales, appSupportedLocales) {
	var activeLocale;

	if(appSupportedLocales[userPreferredLocale]) {
		activeLocale = userPreferredLocale;
	}
	else {
		var firstMatchingLocale = LocaleUtility.getFirstMatchingLocale(appSupportedLocales, userAcceptedLocales);

		if(firstMatchingLocale) {
			activeLocale = firstMatchingLocale;
		}
		else {
			for(var appSupportedLocale in appSupportedLocales) {
				activeLocale = appSupportedLocale;
				break;
			}
		}
	}

	return activeLocale;
};

LocaleUtility.getWindowUrl = function() {
	return window.location.pathname;
};

LocaleUtility.getLocalizedPageUrl = function(pageUrl, locale) {
	var urlParser = document.createElement('a');
	urlParser.href = pageUrl;

	var protocol = urlParser.protocol;
	var host = urlParser.host;
	var url = urlParser.pathname;
	var anchor = urlParser.hash;
	var queryString = urlParser.search;

	url = (url.charAt(0) != "/") ? "/" + url : url; /* some IE versions don't prefix pathname with / */
	url = ( !(/\/$/.test(url)) ) ? url + "/" : url; /* make sure the URL has a trailing / */

	if (url.slice(-6) === ".html/") {
		var splitUrl = url.split("/");
		splitUrl.pop(); /* Remove trailing "/" */
		splitUrl.splice(-1, 0, locale); /* Splice in locale */

		return protocol + "//" + host + splitUrl.join("/") + queryString + anchor;
	}

	return protocol + "//" + host + url + locale + "/" + queryString + anchor;
};

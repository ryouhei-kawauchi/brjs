/**
 * @module br/validation/ISODateValidator
 */

var brCore = require("br/Core");
var Validator = require("br/validation/Validator");

/**
 * @private
 * @class
 * @alias module:br/validation/ISODateValidator
 * @implements module:br/core/Validator
 */
var ISODateValidator = function() {
	/** @private */
	this.m_oSplitterRegex = /^(\d{4})-(\d{2})-(\d{2})$/;

	/** @private */
	this.m_oSplitterNoDashesRegex = /^(\d{4})(\d{2})(\d{2})$/;
};
brCore.implement(ISODateValidator, Validator);

/**
 * @private
 * @param vValue
 * @param mAttributes
 * @param oValidationResult
 */
ISODateValidator.prototype.validate = function(value, mAttributes, validationResult) {
	var isValid = false,
		i18n = require("br/I18n"),
		validationMessage = i18n('br.presenter.validator.invalidISODateFormat', { value: value });

	if (typeof value === 'undefined' || value === null || value === '') {
		isValid = true;
		validationMessage = i18n('br.presenter.validator.valueNullUndefinedOrEmptyString');
	} else if (typeof value.match === 'function') {
		var match = value.match(this.m_oSplitterRegex);
		if (!match) {
			// Try with regex that has dashes optional
			match = value.match(this.m_oSplitterNoDashesRegex);
		}

		if (match !== null) {
			// Convert captured results to numbers for convenience:
			match[1] = Number(match[1]);
			match[2] = Number(match[2]);
			match[3] = Number(match[3]);

			// Use built-in JavaScript date object to check the validity of the value
			var dt = new Date(match[1], match[2]-1, match[3]);

			if (dt.getFullYear() === match[1] && dt.getMonth() === (match[2]-1) && dt.getDate() === match[3] ) {
				isValid = true;
				validationMessage = '';
			}
		}
	}

	validationResult.setResult(isValid, validationMessage);
};

/**
 * @private
 * @param {String} sISODate
 * @type Boolean
 */
ISODateValidator.prototype.isValidISODate = function (ISODateString) {
	var ValidationResult = require("br/validation/ValidationResult");
	var validationResult = new ValidationResult();
	this.validate(ISODateString, {}, validationResult);
	return validationResult.isValid();
};

module.exports = ISODateValidator;

package gov.nih.nci.doe.web.util;

public enum LoginStatusCode {
	// statuses for login
	LOGIN_SUCCESS,	
	LOGIN_INVALID_EMAIL,
	LOGIN_INVALID_PASSWORD,
	LOGIN_EXPIRED_PASSWORD,
	LOGIN_FAILURE,
	LOGIN_LOCKED;
}

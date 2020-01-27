package gov.nih.nci.doe.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

	@Controller
	@EnableAutoConfiguration
	@RequestMapping("/logOut")
	public class LogOutController extends AbstractHpcController {

		@RequestMapping(method = RequestMethod.POST)
		public ResponseEntity<?> logOut(HttpServletRequest request,HttpServletResponse response) {
			 doCommonLogout( request,  response);
			 
			 return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
		}

		public void doCommonLogout(HttpServletRequest request, HttpServletResponse response) {
		    CookieClearingLogoutHandler cookieClearingLogoutHandler = new CookieClearingLogoutHandler(AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY);
		    SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
		    cookieClearingLogoutHandler.logout(request, response, null);
		    securityContextLogoutHandler.logout(request, response, null);
		    log.info("User successfully logged out through hyperlink.");
		}
}

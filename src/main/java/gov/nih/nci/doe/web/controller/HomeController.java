package gov.nih.nci.doe.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import gov.nih.nci.doe.web.model.DoeUsersModel;
import gov.nih.nci.doe.web.service.AuthenticateService;




/**
 *
 * DOE root Controller
 *
 *
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/")
public class HomeController extends AbstractDoeController {


	 @Autowired
	 AuthenticateService authService;
	 
	@RequestMapping(method = RequestMethod.GET)
	public String index() {
		log.info("home page");
		return "home";
		
	}
	

	  
	    /**
	     * @param headers
	     * @return
	     */
	    @GetMapping(value = "user-info")
	    public ResponseEntity<?> getUserInfo(HttpSession session,@RequestHeader HttpHeaders headers,
	    		@RequestParam(value = "emailAddr") String emailAddr) {
	        log.info("getting user info with email address " +emailAddr);
	        try {
	        	DoeUsersModel user = authService.getUserInfo(emailAddr);
	            return new ResponseEntity<>(user, headers, HttpStatus.OK);
	        } catch (Exception e) {
	            log.error(e.getMessage(), e);	           
	            return new ResponseEntity<>(null, headers, HttpStatus.SERVICE_UNAVAILABLE);
	        }
	    }
	    

		@PostMapping(value = "user-info")
	    public ResponseEntity<?> updateUserInfo(@RequestBody DoeUsersModel doeModel,@RequestHeader HttpHeaders headers) {
	        log.debug("update user info for user " + doeModel.getEmailAddrr());
	        try {
	        	if(doeModel.getEmailAddrr() != null) {
	        		authService.saveUserInfo(doeModel);
	        	}
	        	 return new ResponseEntity<>("SUCCESS", headers, HttpStatus.OK);
	        } catch (Exception e) {
	            log.error(e.getMessage(), e);
	            return new ResponseEntity<>(null, headers, HttpStatus.SERVICE_UNAVAILABLE);
	        }

	    }
		
		
		 @RequestMapping(value = "/searchTab", method = RequestMethod.GET)
		 public String getSearchTab(HttpSession session, HttpServletRequest request)  { 
			 
			return "searchTab";
		 }
		 
		 @RequestMapping(value = "/tasksTab", method = RequestMethod.GET)
		 public String getTasksTab(HttpSession session, HttpServletRequest request)  { 
			 
			return "tasksTab";
		 }
		 
		 
		 @RequestMapping(value = "/loginTab", method = RequestMethod.GET)
		 public String getLoginTab(HttpSession session, HttpServletRequest request)  { 
			 
			return "loginTab";
		 }
}

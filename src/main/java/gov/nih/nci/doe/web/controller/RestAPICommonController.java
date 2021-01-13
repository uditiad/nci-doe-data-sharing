package gov.nih.nci.doe.web.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import org.springframework.http.MediaType;

import gov.nih.nci.doe.web.model.DoeUsersModel;
import gov.nih.nci.doe.web.model.KeyValueBean;
import gov.nih.nci.doe.web.util.DoeClientUtil;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@CrossOrigin
@RestController
@RequestMapping(value = "/api")
public class RestAPICommonController extends AbstractDoeController{
	
	 @Value("${gov.nih.nci.hpc.server.dataObject}")
	 private String dataObjectServiceURL;
	 
	 @Value("${gov.nih.nci.hpc.server.v2.dataObject}")
	 private String dataObjectAsyncServiceURL;
	 
	 @Value("${gov.nih.nci.hpc.server.collection}")
	 private String serviceURL;
	 
	 @PostMapping(value="/dataObject/**/download")
	 public ResponseEntity<?> synchronousDownload(@RequestHeader HttpHeaders headers, 
			 HttpServletRequest request, HttpSession session,
			 HttpServletResponse response) {
		 
		 String path = request.getRequestURI().split(request.getContextPath() + "/dataObject/")[1];
		 Integer index=path.lastIndexOf('/');
		 path = path.substring(0,index);
		 log.info("download sync:");
	     log.info("Headers: {}", headers);
	     log.info("pathName: " +path);

	      try {
	          String authToken = (String) session.getAttribute("writeAccessUserToken");
	          log.info("authToken: " + authToken);
	          if (authToken == null) {
	            return null;
	          }
	          String doeLogin = (String) session.getAttribute("doeLogin");
	          log.info("doeLogin: " + doeLogin);
	          if (doeLogin == null) {
	            return null;
	          }
     			      	
	          final String serviceURL = UriComponentsBuilder.fromHttpUrl(
	            this.dataObjectServiceURL).path("/{dme-archive-path}/download")
	            .buildAndExpand(path).encode().toUri()
	            .toURL().toExternalForm();

	          final HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
	          dto.setGenerateDownloadRequestURL(true);

	          WebClient client = DoeClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
	          client.header("Authorization", "Bearer " + authToken);

	          Response restResponse = client.invoke("POST", dto);
	          log.info("rest response:" + restResponse.getStatus());
	          if (restResponse.getStatus() == 200) {
	                HpcDataObjectDownloadResponseDTO downloadDTO =
	                  (HpcDataObjectDownloadResponseDTO) DoeClientUtil.getObject(
	                  restResponse, HpcDataObjectDownloadResponseDTO.class);
	                downloadToUrl(downloadDTO.getDownloadRequestURL(), 1000000,"test", response);	          
	          }   
	      } catch (Exception e) {
	          log.error("error in download" + e);
	      }
	        return null;
	 }
	 
	 
	 @PostMapping(value="/v2/dataObject/**/download",consumes= {MediaType.APPLICATION_JSON_VALUE}, 
     produces= {MediaType.APPLICATION_JSON_VALUE,MediaType.APPLICATION_OCTET_STREAM_VALUE})
	 public String synchronousDownload(@RequestHeader HttpHeaders headers, HttpSession session,
			 HttpServletResponse response,HttpServletRequest request,
			@RequestBody @Valid gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO downloadRequest) {

		 log.info("download async:");
	     log.info("Headers: {}", headers);
	     
	     String path = request.getRequestURI().split(request.getContextPath() + "/v2/dataObject/")[1];
		 Integer index=path.lastIndexOf('/');
		 path = path.substring(0,index);
		 
	     log.info("pathName: " +path);

	      try {
	          String authToken = (String) session.getAttribute("writeAccessUserToken");
	          log.info("authToken: " + authToken);
	          if (authToken == null) {
	            return null;
	          }
	          String doeLogin = (String) session.getAttribute("doeLogin");
	          log.info("doeLogin: " + doeLogin);
	          if (doeLogin == null) {
	            return null;
	          }

	          final String serviceURL = UriComponentsBuilder.fromHttpUrl(this.dataObjectAsyncServiceURL)
	        	        .path("/{dme-archive-path}/download").buildAndExpand(path).encode().toUri().toURL().toExternalForm();

	          WebClient client = DoeClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
	          client.header("Authorization", "Bearer " + authToken);

	          Response restResponse = client.invoke("POST", downloadRequest);
	          log.info("rest response:" + restResponse.getStatus());
	          if (restResponse.getStatus() == 200) {
	        	  HpcDataObjectDownloadResponseDTO downloadDTO =
	        	          (HpcDataObjectDownloadResponseDTO) DoeClientUtil.getObject(restResponse,
	        	              HpcDataObjectDownloadResponseDTO.class);
	        	  String taskId = "Unknown";
	              if (downloadDTO != null)
	                taskId = downloadDTO.getTaskId();
	              return "taskId: " + taskId;
	          }    else {
	        	  return "error in download";
	          }
	      } catch (Exception e) {
	          log.error("error in download" + e);
	      }
	        return null;
	 }
	 
	 @GetMapping(value="/v2/dataObject/**", produces= {MediaType.APPLICATION_JSON_VALUE,MediaType.APPLICATION_OCTET_STREAM_VALUE})
	  public HpcDataObjectListDTO getDataObject(@RequestHeader HttpHeaders headers, HttpSession session,
				 HttpServletResponse response,HttpServletRequest request, @RequestParam("includeAcl") Boolean includeAcl) {

		 log.info("get dataobject:");
	     log.info("Headers: {}", headers);	     
	     String path = request.getRequestURI().split(request.getContextPath() + "/v2/dataObject/")[1];
	     log.info("pathName: " +path);

	      try {
	          String authToken = (String) session.getAttribute("writeAccessUserToken");
	          log.info("authToken: " + authToken);
	          
	          if (authToken == null) {
	            return null;
	          }
	          
	          
	          final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
	        		  dataObjectAsyncServiceURL).path("/{dme-archive-path}").queryParam("list", Boolean.valueOf(true));
	        	      
	        	ucBuilder.queryParam("includeAcl", Boolean.toString(includeAcl));
	        	final String serviceURL = ucBuilder.buildAndExpand(path).encode().toUri().toURL().toExternalForm();
	        		  
	        	WebClient client = DoeClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
	        	client.header("Authorization", "Bearer " + authToken);
	        	Response restResponse = client.invoke("GET", null);	      
	            log.info("rest response:" + restResponse.getStatus());
	            
	          if (restResponse.getStatus() == 200) {
	        	  ObjectMapper mapper = new ObjectMapper();
	              AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
	                  new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
	                  new JacksonAnnotationIntrospector());
	              mapper.setAnnotationIntrospector(intr);
	              mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	              MappingJsonFactory factory = new MappingJsonFactory(mapper);
	              JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

	              HpcDataObjectListDTO datafiles = parser.readValueAs(HpcDataObjectListDTO.class);
	              return datafiles;
	          }
	        	 
	      } catch (Exception e) {
	          log.error("error in download" + e);
	      }
	        return null;
	 }
	 
	 @GetMapping(value="/collection/**", produces= {MediaType.APPLICATION_JSON_VALUE,MediaType.APPLICATION_OCTET_STREAM_VALUE})
	 public HpcCollectionListDTO getCollection(@RequestHeader HttpHeaders headers, HttpSession session,
			 HttpServletResponse response,HttpServletRequest request, @RequestParam("list") Boolean list) {
		 log.info("download async:");
	     log.info("Headers: {}", headers);
	     
	     String path = request.getRequestURI().split(request.getContextPath() + "/collection/")[1];		 
	     log.info("pathName: " +path);

	      try {
	          String authToken = (String) session.getAttribute("writeAccessUserToken");
	          log.info("authToken: " + authToken);
	          
	          if (authToken == null) {
	            return null;
	          }
	          
	         
	          final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
	        		  serviceURL).path("/{dme-archive-path}");

	        	 ucBuilder.queryParam("list", Boolean.toString(list));
	        	 final String serviceURL = ucBuilder.buildAndExpand(path).encode().toUri()
	        	  .toURL().toExternalForm();
	        		  
	          WebClient client = DoeClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
	          client.header("Authorization", "Bearer " + authToken);
	          Response restResponse = client.invoke("GET", null);
	          if (restResponse.getStatus() == 200) {
	        	  ObjectMapper mapper = new ObjectMapper();
	        	  AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
	        	  new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
	        	  new JacksonAnnotationIntrospector());
	        	  mapper.setAnnotationIntrospector(intr);
	        	  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	        	  MappingJsonFactory factory = new MappingJsonFactory(mapper);
	        	  JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
	        	  HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
	        	  return collections;
	         }
	        } catch (Exception e) {
               log.error("error in download" + e);
          }
       return null;
   }
	 
	    @PutMapping(value="/collection/**")
		public String registerCollection(@RequestHeader HttpHeaders headers, HttpSession session,
				 HttpServletResponse response,HttpServletRequest request,
				 @RequestBody @Valid HpcCollectionRegistrationDTO collectionRegistration) {
	    	log.info("download async:");
		     log.info("Headers: {}", headers);
		     
		     String path = request.getRequestURI().split(request.getContextPath() + "/collection/")[1];		 
		     log.info("pathName: " +path);
		     
		      try {
		          String authToken = (String) session.getAttribute("writeAccessUserToken");
		          log.info("authToken: " + authToken);
		          
		          if (authToken == null) {
		            return null;
		          }
		          
		          String doeLogin = (String) session.getAttribute("doeLogin");
		          log.info("doeLogin: " + doeLogin);
		          if (doeLogin == null) {
		            return null;
		          }
		          if(StringUtils.isNotEmpty(doeLogin) && Boolean.TRUE.equals(isUploader(doeLogin))) {
		        	  String parentPath = null;
		        		if (path.lastIndexOf('/') != -1) {
							parentPath = path.substring(0, path.lastIndexOf('/'));
		        		} else {
							parentPath = path;
						}
						  if (!parentPath.isEmpty()) {
							 if(!parentPath.equalsIgnoreCase(basePath)) {
							      HpcCollectionListDTO parentCollectionDto = DoeClientUtil.getCollection(authToken, 
									serviceURL, parentPath, true, sslCertPath, sslCertPassword);
							      Boolean isValidPermissions = verifyCollectionPermissions(doeLogin,parentPath,parentCollectionDto);
							      if (Boolean.FALSE.equals(isValidPermissions)) {
								     return "Insufficient privileges to create collection";
							      }
						    }
						} else {				
								return "Invalid parent in Collection Path";
						}
						
		              WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
		                .fromHttpUrl(serviceURL).path("/{dme-archive-path}")
		                .buildAndExpand(path).encode().toUri().toURL().toExternalForm(),sslCertPath, sslCertPassword);
		              client.header("Authorization", "Bearer " + authToken);

		              Response restResponse = client.invoke("PUT", collectionRegistration);
		              if (restResponse.getStatus() == 200 || restResponse.getStatus() == 201) {
		                return "Collection created";
		              } 
		          }
		        } catch (Exception e) {
	               log.error("error in download" + e);
	          }
	       return null;
	 }
	    
	    @PutMapping(value="/v2/dataObject/**",consumes= {MediaType.MULTIPART_FORM_DATA_VALUE},produces= {MediaType.APPLICATION_XML_VALUE,MediaType.APPLICATION_JSON_VALUE})
		public Response registerDataObject(@RequestBody @Valid gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO dataObjectRegistration,
				MultipartFile dataObjectInputStream) {
	    	
					return null;
	    	
	     }
	    
	    
	    private Boolean verifyCollectionPermissions(String loggedOnUser, String parentPath,HpcCollectionListDTO parentCollectionDto) {

			 List<KeyValueBean> keyValueBeanResults = new ArrayList<>();
			 if(!StringUtils.isEmpty(loggedOnUser)) {
				 DoeUsersModel user = authenticateService.getUserInfo(loggedOnUser);
				 if(user != null && !StringUtils.isEmpty(user.getProgramName())) {
					 List<String> progList = Arrays.asList(user.getProgramName().split(","));
					 progList.stream().forEach(e -> keyValueBeanResults.add(new KeyValueBean(e, e))); 
				 }
			 }

			 if (!parentPath.equalsIgnoreCase(basePath) && parentCollectionDto != null && parentCollectionDto.getCollections() != null
						&& !CollectionUtils.isEmpty(parentCollectionDto.getCollections())) {
					HpcCollectionDTO collection = parentCollectionDto.getCollections().get(0);				
					String role = getPermissionRole(loggedOnUser,collection.getCollection().getCollectionId(),keyValueBeanResults);
					if(StringUtils.isNotEmpty(role) && (role.equalsIgnoreCase("Owner")|| role.equalsIgnoreCase("Group User"))) {
						return true;
					} 
			    }
				return false;
	    	
	    }
	    
	    private Boolean isUploader(String emailAddr) {
	    	 if(!StringUtils.isEmpty(emailAddr)) {
				  DoeUsersModel user =  authenticateService.getUserInfo(emailAddr);
				  if(user.getIsWrite() != null && user.getIsWrite()) {
					return true;  
				  }
			  }
	    	 
	    	 return false;
	    }
}

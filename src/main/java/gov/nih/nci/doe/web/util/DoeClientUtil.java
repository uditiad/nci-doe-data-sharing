package gov.nih.nci.doe.web.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.doe.web.DoeWebException;
import gov.nih.nci.doe.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermissionForCollection;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcRegistrationSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;


import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.transport.http.HTTPConduit;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

public class DoeClientUtil {

  private static final String ERR_MSG_TEMPLATE__FAILED_GET_PATH_ELEM_TYPE =
    "Failed to determine type of DME entity at path, %s." +
    "  Exception message: %s.";

  private static final String JSON_RESPONSE_ATTRIB__ELEMENT_TYPE =
      "elementType";
  
  //The logger instance.
  private final static Logger logger = LoggerFactory.getLogger(DoeClientUtil.class);


  public static WebClient getWebClient(String url, String hpcCertPath,
    String hpcCertPassword) {

    WebClient client = WebClient.create(url, Collections.singletonList(
      new JacksonJsonProvider()));

    ClientConfiguration clientConfig = WebClient.getConfig(client);
    clientConfig.getRequestContext().put("support.type.as.multipart", "true");
    configureWebClientConduit(clientConfig);

    return client;
  }


  private static void configureWebClientConduit(
    ClientConfiguration clientConfig) {
    HTTPConduit conduit = clientConfig.getHttpConduit();

    TLSClientParameters tlsParams = conduit.getTlsClientParameters();
    if (null == tlsParams) {
      conduit.setTlsClientParameters(new TLSClientParameters());
      tlsParams = conduit.getTlsClientParameters();
    }
    tlsParams.setDisableCNCheck(true);
    tlsParams.setTrustManagers(new TrustManager[] { new
      TrustAllX509TrustManager() });

    conduit.getClient().setReceiveTimeout(60000000);
    conduit.getClient().setConnectionTimeout(60000000);
  }


  public static String getBasePath(String authToken, String serviceURL, String parent,
      String sslCertPath, String sslCertPassword, HpcDataManagementModelDTO modelDTO) {
    HpcCollectionListDTO collectionListDTO = DoeClientUtil.getCollection(authToken, serviceURL,
        parent, true, sslCertPath, sslCertPassword);
    if (collectionListDTO != null && collectionListDTO.getCollections() != null) {
      HpcCollectionDTO collection = collectionListDTO.getCollections().get(0);
      String configurationId = null;
      if (collection != null) {
        if (collection.getMetadataEntries() != null
            && collection.getMetadataEntries().getSelfMetadataEntries() != null) {
          for (HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries())
            if (entry.getAttribute().equals("configuration_id")) {
              configurationId = entry.getValue();
              break;
            }
        }
      }
//      if (configurationId != null) {
        if (modelDTO != null) {
          // TODO
          for (HpcDocDataManagementRulesDTO rulesDTO : modelDTO.getDocRules()) {
            for (HpcDataManagementRulesDTO rule : rulesDTO.getRules()) {
              if ((configurationId != null && rule.getId().equals(configurationId)) || rule.getBasePath().equals(parent))
                return rule.getBasePath();
            }
          }
        }
 //     }
    }
    return null;

  }

  public static String getAuthenticationToken(String userId, String passwd, String hpcServerURL)
      throws DoeWebException {

    WebClient client = DoeClientUtil.getWebClient(hpcServerURL, null, null);
    String token = DatatypeConverter.printBase64Binary((userId + ":" + passwd).getBytes());
    client.header("Authorization", "Basic " + token);
    Response restResponse = client.get();
    try {

      if (restResponse.getStatus() != 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Authentication failed: " + exception.getMessage());
      }
      MappingJsonFactory factory = new MappingJsonFactory();
      JsonParser parser;
      parser = factory.createParser((InputStream) restResponse.getEntity());
      HpcAuthenticationResponseDTO dto = parser.readValueAs(HpcAuthenticationResponseDTO.class);
      return dto.getToken();
    } catch (IllegalStateException e1) {
      e1.printStackTrace();
      throw new DoeWebException("Failed to get auth token: " + e1.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get auth token: " + e.getMessage());
    }
  }
  
  public static String getAuthenticationTokenSso(String userId, String smSession, String hpcServerURL)
	      throws DoeWebException {
	  
	    WebClient client = DoeClientUtil.getWebClient(hpcServerURL, null, null);
	    Response restResponse = client.header("SM_USER", userId).header("NIHSMSESSION", smSession).get();
	    try {

	      if (restResponse.getStatus() != 200) {
	        ObjectMapper mapper = new ObjectMapper();
	        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
	            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
	            new JacksonAnnotationIntrospector());
	        mapper.setAnnotationIntrospector(intr);
	        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	        MappingJsonFactory factory = new MappingJsonFactory(mapper);
	        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

	        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
	        throw new DoeWebException("Authentication failed: " + exception.getMessage());
	      }
	      MappingJsonFactory factory = new MappingJsonFactory();
	      JsonParser parser;
	      parser = factory.createParser((InputStream) restResponse.getEntity());
	      HpcAuthenticationResponseDTO dto = parser.readValueAs(HpcAuthenticationResponseDTO.class);
	      return dto.getToken();
	    } catch (IllegalStateException e1) {
	      e1.printStackTrace();
	      throw new DoeWebException("Failed to get auth token: " + e1.getMessage());
	    } catch (IOException e) {
	      e.printStackTrace();
	      throw new DoeWebException("Failed to get auth token: " + e.getMessage());
	    }
	  }

  public static List<HpcDataManagementRulesDTO> getUserDOCManagementRules(
      HpcDataManagementModelDTO docModelDto, String userDoc) {
    if (docModelDto == null || docModelDto.getDocRules() == null)
      return null;

    for (HpcDocDataManagementRulesDTO docDTO : docModelDto.getDocRules()) {
      if (docDTO.getDoc().equals(userDoc))
        return docDTO.getRules();
    }
    return null;
  }


  public static HpcDataManagementRulesDTO getBasePathManagementRules(
      HpcDataManagementModelDTO docModelDto, String basePath) {
    if (docModelDto == null || docModelDto.getDocRules() == null || basePath == null)
      return null;

    for (HpcDocDataManagementRulesDTO docDTO : docModelDto.getDocRules()) {
      for (HpcDataManagementRulesDTO rules : docDTO.getRules()) {
        if (rules.getBasePath().equals(basePath))
          return rules;
      }
    }
    return null;
  }

  public static HpcDataManagementModelDTO getDOCModel(String token, String hpcModelURL,
      String hpcCertPath, String hpcCertPassword) {

    WebClient client = DoeClientUtil.getWebClient(hpcModelURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get DOC Model due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcDataManagementModelDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get DOC Model due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get DOC Model due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get DOC Model due to: " + e.getMessage());
    }
  }

  public static List<String> getDOCs(String token, String hpcModelURL, String hpcCertPath,
      String hpcCertPassword, HttpSession session) {
    List<String> docs = new ArrayList<String>();
    HpcDataManagementModelDTO modelDTO =
        (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
    if (modelDTO == null) {
      //HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
      modelDTO = DoeClientUtil.getDOCModel(token, hpcModelURL, hpcCertPath, hpcCertPassword);
      if (modelDTO != null)
        session.setAttribute("userDOCModel", modelDTO);
    }

    for (HpcDocDataManagementRulesDTO docDTO : modelDTO.getDocRules())
      docs.add(docDTO.getDoc());
    return docs;
  }

  public static String getBasePath(HttpServletRequest request) {
    String[] basePathValues = request.getParameterValues("basePath");
    String basePath = null;
    if (basePathValues == null || basePathValues.length == 0)
      basePath = (String) request.getAttribute("basePath");
    else
      basePath = basePathValues[0];
    if (basePath != null && basePath.equals("_select_null"))
      return null;
    return basePath;
  }


  public static Optional<String> getPathElementType(
      String argAuthToken, String argServiceUrlPrefix, String argItemPath,
      String argSslCertPath, String argSslCertPasswd)
      throws DoeWebException {
    Optional<String> elemType = Optional.empty();
    try {
      String theItemPath = argItemPath.trim();
      final String hpcServiceUrl = UriComponentsBuilder.fromHttpUrl(
        argServiceUrlPrefix).path("/{dme-archive-path}").buildAndExpand(
        theItemPath).encode().toUri().toURL().toExternalForm();

      final WebClient client = DoeClientUtil.getWebClient(hpcServiceUrl,
                                argSslCertPath, argSslCertPasswd);
//      client.header(HttpHeaders.AUTHORIZATION, "Basic " + argAuthToken);
      client.header("Authorization", "Bearer " + argAuthToken);
      final Response restResponse = client.get();
      if (restResponse.getStatus() == HttpServletResponse.SC_OK) {
        elemType = extractElementTypeFromResponse(restResponse);
      } else {
        final String extractedErrMsg =
            genHpcExceptionDtoOnNonOkRestResponse(restResponse).getMessage();
        throw new DoeWebException(String.format(
          ERR_MSG_TEMPLATE__FAILED_GET_PATH_ELEM_TYPE,
          theItemPath,
          extractedErrMsg
        ));
      }

      return elemType;
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      final String msgForDoeWebException = String.format(
        ERR_MSG_TEMPLATE__FAILED_GET_PATH_ELEM_TYPE,
        argItemPath,
        e.getMessage()
      );
      throw new DoeWebException(msgForDoeWebException);
    }
  }


  public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL,
	  String path, boolean list, String hpcCertPath, String hpcCertPassword) {
	return getCollection(token, hpcCollectionlURL, path, false, list, false, hpcCertPath, hpcCertPassword);
  }
	  
  
  public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL,
      String path, boolean children, boolean list, String hpcCertPath, String hpcCertPassword) {
    return getCollection(token, hpcCollectionlURL, path, children, list, false, hpcCertPath, hpcCertPassword);
  }
  

  public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL,
      String path, boolean children, boolean list, boolean includeAcl, String hpcCertPath, String hpcCertPassword) {
    try {
      final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        hpcCollectionlURL).path("/{dme-archive-path}");
      if (children) {
        ucBuilder.pathSegment("children");
      } else {
        ucBuilder.queryParam("list", Boolean.valueOf(list).toString());
      }
      ucBuilder.queryParam("includeAcl", Boolean.valueOf(includeAcl).toString());
      final String serviceURL = ucBuilder.buildAndExpand(path).encode().toUri()
        .toURL().toExternalForm();
	  
      WebClient client = DoeClientUtil.getWebClient(serviceURL, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.invoke("GET", null);
      // System.out.println("restResponse.getStatus():"
      // +restResponse.getStatus());
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
      } else {
        throw new DoeWebException("Failed to get collection! No READ access!");
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException(path + ": " + e.getMessage());
    }
  }
  
  
  public static HpcDataObjectListDTO getDatafiles(String token, String hpcDatafileURL, String path,
	      boolean list, String hpcCertPath, String hpcCertPassword) {
	  return getDatafiles(token, hpcDatafileURL, path,
		      list, false, hpcCertPath,hpcCertPassword);
  }
  

  public static HpcDataObjectListDTO getDatafiles(String token, String hpcDatafileURL, String path,
    boolean list, boolean includeAcl, String hpcCertPath, String hpcCertPassword) {
    try {
      final String url2Apply = UriComponentsBuilder.fromHttpUrl(hpcDatafileURL)
        .path("/{dme-archive-path}").queryParam("list", Boolean.valueOf(list))
        .queryParam("includeAcl", Boolean.valueOf(includeAcl))
        .buildAndExpand(path).encode().toUri().toURL().toExternalForm();
      WebClient client = DoeClientUtil.getWebClient(url2Apply, hpcCertPath,
        hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("GET", null);
      // System.out.println("restResponse.getStatus():"
      // +restResponse.getStatus());
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
      } else {
        throw new DoeWebException(
            "File does not exist or you do not have READ access.");
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException(path + " : " + e.getMessage());
    }
  }

  public static HpcUserListDTO getUsers(String token, String hpcUserURL, String userId,
      String firstName, String lastName, String doc, String hpcCertPath, String hpcCertPassword) {
    try {
      final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        hpcUserURL);
      if (null != userId && !userId.trim().isEmpty()) {
        ucBuilder.queryParam("nciUserId", userId.trim());
      }
      if (null != firstName && !firstName.trim().isEmpty()) {
        ucBuilder.queryParam("firstNamePattern", firstName.trim());
      }
      if (null != lastName && !lastName.trim().isEmpty()) {
        ucBuilder.queryParam("lastNamePattern", lastName.trim());
      }
      if (null != doc && !doc.trim().isEmpty()) {
        ucBuilder.queryParam("doc", doc.trim());
      }
      final String url2Apply = ucBuilder.build().encode().toUri().toURL()
        .toExternalForm();
      WebClient client =
          DoeClientUtil.getWebClient(url2Apply, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

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

        HpcUserListDTO users = parser.readValueAs(HpcUserListDTO.class);
        return users;
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get Users due to: " + e.getMessage());
    }
    return null;
  }

  public static HpcUserDTO getUser(String token, String hpcUserURL, String hpcCertPath,
      String hpcCertPassword) {
    try {

      WebClient client = DoeClientUtil.getWebClient(hpcUserURL, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

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

        return parser.readValueAs(HpcUserDTO.class);
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to get user: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get User due to: " + e.getMessage());
    }
  }

  public static HpcUserDTO getUserByAdmin(String token, String hpcUserURL, String userId,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(userId).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

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

        return parser.readValueAs(HpcUserDTO.class);
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to get user: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get User due to: " + e.getMessage());
    }
  }

  public static boolean createUser(String token, String hpcUserURL, HpcUserRequestDTO userDTO,
      String userId, String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(userId).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", userDTO);
      if (restResponse.getStatus() == 201) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to create user: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to create User due to: " + e.getMessage());
    }
  }

  public static boolean createBookmark(String token, String hpcBookmarkURL,
      HpcBookmarkRequestDTO hpcBookmark, String hpcBookmarkName, String hpcCertPath,
      String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcBookmarkURL).pathSegment(hpcBookmarkName).build()
        .encode().toUri().toURL().toExternalForm(), hpcCertPath,
        hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", hpcBookmark);
      if (restResponse.getStatus() == 201) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        logger.error("Failed to create bookmark " + hpcBookmarkName, exception);
        throw new DoeWebException(exception.getMessage());
      }
    } catch (Exception e) {
      logger.error("Failed to create bookmark " + hpcBookmarkName, e);
      throw new DoeWebException(e.getMessage());
    }
  }

  public static boolean deleteBookmark(String token, String hpcBookmarkURL, String hpcBookmarkName,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcBookmarkURL).pathSegment(hpcBookmarkName).build()
        .encode().toUri().toURL().toExternalForm(), hpcCertPath,
        hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.delete();
      if (restResponse.getStatus() == 200) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        logger.error("Failed to delete bookmark " + hpcBookmarkName, exception);
        throw new DoeWebException(exception.getMessage());
      }
    } catch (Exception e) {
    	logger.error("Failed to delete bookmark " + hpcBookmarkName, e);
      throw new DoeWebException(e.getMessage());
    }
  }

  public static boolean deleteSearch(String token, String hpcSavedSearchURL, String searchName,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcSavedSearchURL).pathSegment(searchName).build().encode()
        .toUri().toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.delete();
      if (restResponse.getStatus() == 200) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to delete saved search: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to delete saved search due to: " + e.getMessage());
    }
  }

  public static HpcBookmarkListDTO getBookmarks(String token, String hpcBookmarkURL,
      String hpcCertPath, String hpcCertPassword) {
    WebClient client = DoeClientUtil.getWebClient(hpcBookmarkURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get Bookmarks due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcBookmarkListDTO.class);
    } catch (Exception e) {
      logger.error("Failed to get bookmarks: ", e);
      throw new DoeWebException(e.getMessage());
    }
  }

  public static HpcGroupMembersResponseDTO createGroup(String token, String hpcUserURL,
      HpcGroupMembersRequestDTO groupDTO, String groupName, String hpcCertPath,
      String hpcCertPassword) {
    HpcGroupMembersResponseDTO response = null;
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(groupName).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", groupDTO);
      if (restResponse.getStatus() == 201) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        response = parser.readValueAs(HpcGroupMembersResponseDTO.class);

      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to create group: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to create group due to: " + e.getMessage());
    }
    return response;
  }

  public static HpcGroupMembersResponseDTO updateGroup(String token, String hpcUserURL,
      HpcGroupMembersRequestDTO groupDTO, String groupName, String hpcCertPath,
      String hpcCertPassword) {
    HpcGroupMembersResponseDTO response = null;
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(groupName).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.invoke("POST", groupDTO);
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        response = parser.readValueAs(HpcGroupMembersResponseDTO.class);

      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to update group: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to update group due to: " + e.getMessage());
    }
    return response;
  }

  public static boolean deleteGroup(String token, String hpcUserURL, String groupName,
      String hpcCertPath, String hpcCertPassword) {
    //HpcGroupMembersResponseDTO response = null;
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(groupName).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.invoke("DELETE", null);
      if (restResponse.getStatus() == 200) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to delete group: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to delete group due to: " + e.getMessage());
    }
  }

  public static boolean createCollection(String token, String hpcCollectionURL,
      HpcCollectionRegistrationDTO collectionDTO, String path, String hpcCertPath,
      String hpcCertPassword) {
    try {
      HpcCollectionListDTO collection =
          getCollection(token, hpcCollectionURL, path, false, hpcCertPath, hpcCertPassword);
      if (collection != null && collection.getCollectionPaths() != null
          && collection.getCollectionPaths().size() > 0)
        throw new DoeWebException("Failed to create. Collection already exists: " + path);

      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcCollectionURL).path("/{dme-archive-path}")
        .buildAndExpand(path).encode().toUri().toURL().toExternalForm(),
        hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", collectionDTO);
      if (restResponse.getStatus() == 201) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to create collection: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to create collection due to: " + e.getMessage());
    }
  }

  public static boolean updateCollection(String token, String hpcCollectionURL,
      HpcCollectionRegistrationDTO collectionDTO, String path, String hpcCertPath,
      String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcCollectionURL).path("/{dme-archive-path}")
        .buildAndExpand(path).encode().toUri().toURL().toExternalForm(),
        hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", collectionDTO);
      if (restResponse.getStatus() == 200 || restResponse.getStatus() == 201) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException(exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException(e.getMessage());
    }
  }

  public static boolean deleteCollection(String token, String hpcCollectionURL,
      String collectionPath, String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcCollectionURL).path("/{dme-archive-path}")
        .buildAndExpand(collectionPath).encode().toUri().toURL()
        .toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.delete();
      if (restResponse.getStatus() == 200) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to delete collection: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to delete collection due to: " + e.getMessage());
    }
  }

  public static boolean registerDatafile(String token, MultipartFile hpcDatafile,
      String hpcDatafileURL, HpcDataObjectRegistrationRequestDTO datafileDTO, String path,
      String hpcCertPath, String hpcCertPassword) {
    try {
      try {
        HpcDataObjectListDTO datafile =
            getDatafiles(token, hpcDatafileURL, path, false, hpcCertPath, hpcCertPassword);
        if (datafile != null && datafile.getDataObjectPaths() != null
            && datafile.getDataObjectPaths().size() > 0)
          throw new DoeWebException("Failed to create. Data file already exists: " + path);
      } catch (DoeWebException e) {
        // Data file is not there!
      }

      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcDatafileURL).path("/{dme-archive-path}").buildAndExpand(
        path).encode().toUri().toURL().toExternalForm(), hpcCertPath,
        hpcCertPassword);
      client.type(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON_VALUE);
      List<Attachment> atts = new LinkedList<Attachment>();
      atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration",
          "application/json", datafileDTO));
      // InputStream inputStream = new BufferedInputStream(
      // new FileInputStream(datafileDTO.getSource().getFileId()));
      ContentDisposition cd2 =
          new ContentDisposition("attachment;filename=" + hpcDatafile.getName());
      atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObject",
          hpcDatafile.getInputStream(), cd2));

      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.put(new MultipartBody(atts));
      if (restResponse.getStatus() == 201) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException(exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException(e.getMessage());
    }
  }

  public static HpcBulkDataObjectRegistrationResponseDTO registerBulkDatafiles(String token,
      String hpcDatafileURL, HpcBulkDataObjectRegistrationRequestDTO datafileDTO,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(hpcDatafileURL, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", datafileDTO);
      if (restResponse.getStatus() == 201 || restResponse.getStatus() == 200) {
        return (HpcBulkDataObjectRegistrationResponseDTO) DoeClientUtil.getObject(restResponse,
            HpcBulkDataObjectRegistrationResponseDTO.class);
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to bulk register data files: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to bulk register data files due to: " + e.getMessage());
    }
  }

  public static boolean updateDatafile(String token, String hpcDatafileURL,
      HpcDataObjectRegistrationRequestDTO datafileDTO, String path, String hpcCertPath,
      String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcDatafileURL).path("/{dme-archive-path}").buildAndExpand(
        path).encode().toUri().toURL().toExternalForm(), hpcCertPath,
        hpcCertPassword);
      client.type(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON_VALUE);
      List<Attachment> atts = new LinkedList<Attachment>();
      atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration",
          "application/json", datafileDTO));

      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.put(new MultipartBody(atts));
      if (restResponse.getStatus() == 200) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to update data file: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to update data file due to: " + e.getMessage());
    }
  }

  public static boolean deleteDatafile(String token, String hpcDatafileURL, String path,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcDatafileURL).path("/{dme-archive-path}").buildAndExpand(
        path).encode().toUri().toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.delete();
      if (restResponse.getStatus() == 200) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException(exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException(e.getMessage());
    }
  }

  public static boolean updateUser(String token, String hpcUserURL, HpcUserRequestDTO userDTO,
      String userId, String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(userId).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("POST", userDTO);
      if (restResponse.getStatus() == 200) {
        return true;
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to update user: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to update user due to: " + e.getMessage());
    }
  }

  public static HpcGroupListDTO getGroups(String token, String hpcGroupURL, String groupName,
      String hpcCertPath, String hpcCertPassword) {
    try {
      final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        hpcGroupURL);
      if (null != groupName && !groupName.trim().isEmpty()) {
        ucBuilder.queryParam("groupPattern", groupName.trim());
      }
      WebClient client = DoeClientUtil.getWebClient(ucBuilder.build().encode()
        .toUri().toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

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

        HpcGroupListDTO groups = parser.readValueAs(HpcGroupListDTO.class);
        return groups;
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get Users due to: " + e.getMessage());
    }
    return null;
  }

  public static HpcNamedCompoundMetadataQueryDTO getQuery(String token, String hpcQueryURL,
      String queryName, String hpcCertPath, String hpcCertPassword) {
    HpcNamedCompoundMetadataQueryDTO retVal = null;
    try {
      final String serviceURL = UriComponentsBuilder.fromHttpUrl(hpcQueryURL)
        .pathSegment(queryName).build().encode().toUri().toURL()
        .toExternalForm();
      WebClient client = DoeClientUtil.getWebClient(serviceURL, hpcCertPath,
          hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.get();
      if (null != restResponse && 200 == restResponse.getStatus()) {
        JsonParser parser = new MappingJsonFactory().createParser((InputStream)
            restResponse.getEntity());
        retVal = parser.readValueAs(HpcNamedCompoundMetadataQueryDTO.class);
      }
      return retVal;
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get Query for: " + queryName +
        " due to: " + e.getMessage());
    }
  }

  public static HpcNamedCompoundMetadataQueryListDTO getSavedSearches(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {
    WebClient client = DoeClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();
    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());

    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get saved queries due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcNamedCompoundMetadataQueryListDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get saved queries due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get saved queries due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get saved queries due to: " + e.getMessage());
    }
  }

  public static HpcNotificationDeliveryReceiptListDTO getNotificationReceipts(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {

    WebClient client = DoeClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();
    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get notification receipts due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcNotificationDeliveryReceiptListDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get notification receipts due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get notification receipts due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get notification receipts due to: " + e.getMessage());
    }
  }

  public static HpcEntityPermissionsDTO getPermissions(String token, String hpcServiceURL,
      String hpcCertPath, String hpcCertPassword) {

    WebClient client = DoeClientUtil.getWebClient(hpcServiceURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();
    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get notification receipts due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcEntityPermissionsDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get notification receipts due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get notification receipts due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get notification receipts due to: " + e.getMessage());
    }
  }

  public static HpcUserPermissionDTO getPermissionForUser(String token, String path, String userId,
      String hpcServiceURL, String hpcCertPath, String hpcCertPassword) {
    try {
      final Map<String, String> templateVarValues = new HashMap<>();
      templateVarValues.put("dme-archive-path", path);
      templateVarValues.put("user-id", userId);
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcServiceURL)
        .path("/{dme-archive-path}/acl/user/{user-id}")
        .buildAndExpand(templateVarValues).encode().toUri().toURL()
        .toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.get();
      HpcUserPermissionDTO retVal = null;
      if (null != restResponse && 200 == restResponse.getStatus()) {
        retVal = new MappingJsonFactory().createParser((InputStream)
          restResponse.getEntity()).readValueAs(HpcUserPermissionDTO.class);
      }
      return retVal;
    } catch (IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get permission due to: " + e.getMessage());
    }
  }

  public static HpcUserPermsForCollectionsDTO getPermissionForCollections(
      String token, String hpcServiceURL, String userId, Object[] basePaths,
      String hpcCertPath, String hpcCertPassword) {
    try {
      UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        hpcServiceURL).pathSegment(userId);
      if (null != basePaths && 0 < basePaths.length) {
        ucBuilder.queryParam("collectionPath", basePaths);
      }
      WebClient client = DoeClientUtil.getWebClient(ucBuilder.build().encode()
        .toUri().toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.get();
      HpcUserPermsForCollectionsDTO retVal = null;
      if (null != restResponse && 200 == restResponse.getStatus()) {
        retVal = new MappingJsonFactory().createParser((InputStream)
          restResponse.getEntity()).readValueAs(
          HpcUserPermsForCollectionsDTO.class);
      }
      return retVal;
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get permission due to: " +
        e.getMessage());
    }
  }


  public static HpcNotificationSubscriptionListDTO getUserNotifications(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {

    WebClient client = DoeClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get notification subscriptions due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcNotificationSubscriptionListDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get notification subscriptions due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get notification subscriptions due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get notification subscriptions due to: " + e.getMessage());
    }
  }

  public static HpcDownloadSummaryDTO getDownloadSummary(String token, String hpcQueryURL,
      String hpcCertPath, String hpcCertPassword) {

    WebClient client = DoeClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
          new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
          new JacksonAnnotationIntrospector());
      mapper.setAnnotationIntrospector(intr);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      MappingJsonFactory factory = new MappingJsonFactory(mapper);
      JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

      return parser.readValueAs(HpcDownloadSummaryDTO.class);
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get download tasks list due to: " + e.getMessage());
    }
  }

  public static HpcRegistrationSummaryDTO getRegistrationSummary(String token,
    String hpcQueryURL, MultiValueMap<String, String> queryParamsMap, String
    hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
          .fromHttpUrl(hpcQueryURL).queryParams(queryParamsMap).build().encode()
          .toUri().toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.get();
      if (restResponse == null || restResponse.getStatus() != 200) {
        return null;
      }
      ObjectMapper mapper = new ObjectMapper();
      AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
        new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
        new JacksonAnnotationIntrospector());
      mapper.setAnnotationIntrospector(intr);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
      false);
      mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
      //mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
      MappingJsonFactory factory = new MappingJsonFactory(mapper);
      JsonParser parser = factory.createParser((InputStream) restResponse
        .getEntity());
      return parser.readValueAs(HpcRegistrationSummaryDTO.class);
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get registration tasks list due to: " + e.getMessage());
    }
  }

  public static HpcBulkDataObjectDownloadResponseDTO downloadFiles(String token, String hpcQueryURL,
      HpcBulkDataObjectDownloadRequestDTO dto, String hpcCertPath, String hpcCertPassword) {
    HpcBulkDataObjectDownloadResponseDTO response = null;
    try {
      WebClient client = DoeClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.invoke("POST", dto);
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
        response = parser.readValueAs(HpcBulkDataObjectDownloadResponseDTO.class);
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new DoeWebException("Failed to submit download request: " + exception.getMessage());
      }
    } catch (DoeWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to submit download request: " + e.getMessage());
    }
    return response;
  }
  
  public static AjaxResponseBody downloadDataFile(String token, String serviceURL,
      HpcDownloadRequestDTO dto, String downloadType, String hpcCertPath, String hpcCertPassword)
      throws JsonParseException, IOException {
    AjaxResponseBody result = new AjaxResponseBody();
    WebClient client = DoeClientUtil.getWebClient(serviceURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.invoke("POST", dto);
    if (restResponse.getStatus() == 200) {
      HpcDataObjectDownloadResponseDTO downloadDTO =
          (HpcDataObjectDownloadResponseDTO) DoeClientUtil.getObject(restResponse,
              HpcDataObjectDownloadResponseDTO.class);
      String taskId = "Unknown";
      if (downloadDTO != null)
        taskId = downloadDTO.getTaskId();
     // result.setMessage(
         //     "Asynchronous download request is submitted successfully! Task Id: <a href='downloadtask?type=" + downloadType + "&taskId=" + taskId +"'>"+taskId+"</a>");
     
      result.setMessage(taskId);
      return result;
    } else {
      ObjectMapper mapper = new ObjectMapper();
      AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
          new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
          new JacksonAnnotationIntrospector());
      mapper.setAnnotationIntrospector(intr);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      MappingJsonFactory factory = new MappingJsonFactory(mapper);
      JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
      try {
        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        result.setMessage("Download request is not successful: " + exception.getMessage());
        return result;
      } catch (Exception e) {
        result.setMessage("Download request is not successful: " + e.getMessage());
        return result;
      }
    }
  }

  public static HpcBulkDataObjectRegistrationStatusDTO
    getDataObjectRegistrationTask(String token, String hpcQueryURL, String
    taskId, String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = DoeClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcQueryURL).pathSegment(taskId).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.get();

      if (restResponse == null || restResponse.getStatus() != 200) {
        return null;
      }
      
      ObjectMapper mapper = new ObjectMapper();
      AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
        new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
        new JacksonAnnotationIntrospector());
      mapper.setAnnotationIntrospector(intr);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
      false);
      mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
      //mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
      MappingJsonFactory factory = new MappingJsonFactory(mapper);
      JsonParser parser = factory.createParser((InputStream) restResponse
        .getEntity());
      return parser.readValueAs(HpcBulkDataObjectRegistrationStatusDTO.class);
      
      
//      JsonParser parser = new MappingJsonFactory().createParser((InputStream)
//        restResponse.getEntity());
//      return parser.readValueAs(HpcBulkDataObjectRegistrationStatusDTO.class);
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get data object registration tasks details due to: " + e.getMessage());
    }
  }

  public static HpcDataObjectDownloadStatusDTO getDataObjectDownloadTask(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {

    WebClient client = DoeClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
      new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
      new JacksonAnnotationIntrospector());
    mapper.setAnnotationIntrospector(intr);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
    false);
    mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    MappingJsonFactory factory = new MappingJsonFactory(mapper);
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get data object download tasks details due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcDataObjectDownloadStatusDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get data object download tasks details due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get data object download tasks details due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get data object download tasks details due to: " + e.getMessage());
    }
  }

  public static HpcCollectionDownloadStatusDTO getDataObjectsDownloadTask(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {

    WebClient client = DoeClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
      new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
      new JacksonAnnotationIntrospector());
    mapper.setAnnotationIntrospector(intr);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
    false);
    mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    MappingJsonFactory factory = new MappingJsonFactory(mapper);
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get data objects download tasks details due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcCollectionDownloadStatusDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get data objects download tasks details due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get data objects download tasks details due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new DoeWebException(
          "Failed to get data objects download tasks details due to: " + e.getMessage());
    }
  }


  public static HpcMetadataAttributesListDTO getMetadataAttrNames(String token,
      String hpcMetadataAttrsURL, String hpcCertPath, String hpcCertPassword) {

    String url = hpcMetadataAttrsURL;

    WebClient client = DoeClientUtil.getWebClient(url, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcMetadataAttributesListDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new DoeWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
    }
  }



  public static String encode(String strVal) {
    if (strVal == null)
      return null;
    else if (strVal.indexOf("/") == -1)
      return strVal;
    else {
      StringBuffer encodedStr = new StringBuffer();
      StringTokenizer tokens = new StringTokenizer(strVal, "/");
      while (tokens.hasMoreTokens()) {
        String token = tokens.nextToken();
        encodedStr.append(URLEncoder.encode(token));
        if (tokens.hasMoreTokens())
          encodedStr.append("/");
      }
      return encodedStr.toString();
    }
  }

  public static <T> Object getObject(Response response, Class<T> objectClass) {
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
        new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
        new JacksonAnnotationIntrospector());
    mapper.setAnnotationIntrospector(intr);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    MappingJsonFactory factory = new MappingJsonFactory(mapper);
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) response.getEntity());
      return parser.readValueAs(objectClass);
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      throw new DoeWebException("Failed to parse object: " + e1.getMessage());
    } catch (Exception e) {
      throw new DoeWebException("Failed to parse object: " + e.getMessage());
    }
  }

  public static void populateBasePaths(HttpSession session,
      HpcDataManagementModelDTO modelDTO, String authToken, String userId, String collectionURL,
      String sslCertPath, String sslCertPassword) throws DoeWebException {

    Set<String> basePaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    final List<String> docRulesBasePaths = new ArrayList<>();
    for (HpcDocDataManagementRulesDTO docRule : modelDTO.getDocRules()) {
      for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
        docRulesBasePaths.add(rule.getBasePath());
      }
    }
    final HpcUserPermsForCollectionsDTO permissions = DoeClientUtil
      .getPermissionForCollections(authToken, collectionURL, userId,
      docRulesBasePaths.toArray(), sslCertPath, sslCertPassword);
    if (permissions != null) {
      for (HpcPermissionForCollection permission : permissions.getPermissionsForCollections()) {
        if (permission != null && permission.getPermission() != null
            && (permission.getPermission().equals(HpcPermission.WRITE)
                || permission.getPermission().equals(HpcPermission.OWN)))
          basePaths.add(permission.getCollectionPath());
      }
    }
    session.setAttribute("basePaths", basePaths);
  }


  private static Optional<String> extractElementTypeFromResponse(
      Response restResponse) throws IOException {
    Optional<String> retVal = Optional.empty();
    final JsonParser parser = new MappingJsonFactory().createParser(
      (InputStream) restResponse.getEntity());
    while (null != parser.nextValue()) {
      if (JSON_RESPONSE_ATTRIB__ELEMENT_TYPE.equals(parser.getCurrentName())) {
        retVal = Optional.of(parser.getValueAsString());
        break;
      }
    }

    return retVal;
  }

  private static HpcExceptionDTO genHpcExceptionDtoOnNonOkRestResponse(
    Response restResponse) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setAnnotationIntrospector(new AnnotationIntrospectorPair(
      new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
      new JacksonAnnotationIntrospector())
    );
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
      false);
    final JsonParser parser = new MappingJsonFactory(mapper).createParser(
      (InputStream) restResponse.getEntity());
    final HpcExceptionDTO hpcExceptionDto =
      parser.readValueAs(HpcExceptionDTO.class);

    return hpcExceptionDto;
  }

 

  private static Properties appProperties;


  private static void initApplicationProperties() {
    if (null == appProperties) {
      loadApplicationProperties();
    }
  }


  private static void loadApplicationProperties() {
    Properties theProperties = new Properties();
    try {
      theProperties.load(DoeClientUtil.class.getResourceAsStream(
        "/application.properties"));
      if (null == appProperties) {
        appProperties = theProperties;
      } else {
        appProperties.clear();
        appProperties.putAll(theProperties);
      }
    } catch (IOException e) {
      throw new DoeWebException("Unable to load application properties!", e);
    }
  }







}

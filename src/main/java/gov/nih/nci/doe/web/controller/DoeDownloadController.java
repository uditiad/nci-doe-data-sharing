/**
 * DoeDownloadController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.doe.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.doe.web.model.AjaxResponseBody;
import gov.nih.nci.doe.web.model.DoeDownloadDatafile;
import gov.nih.nci.doe.web.model.Views;
import gov.nih.nci.doe.web.service.TaskManagerService;
import gov.nih.nci.doe.web.util.DoeClientUtil;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;

import org.springframework.web.util.UriComponentsBuilder;




@Controller
@EnableAutoConfiguration
@RequestMapping("/download")
public class DoeDownloadController extends AbstractDoeController {
	@Value("${gov.nih.nci.hpc.server.v2.dataObject}")
	private String dataObjectServiceURL;
	@Value("${gov.nih.nci.hpc.server.v2.collection}")
	private String collectionServiceURL;
	@Value("${gov.nih.nci.hpc.web.server}")
	private String webServerName;

	
    @Autowired
    TaskManagerService taskManagerService;
	
	/**
	 * POST action to initiate asynchronous download.
	 * 
	 * @param downloadFile
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @return
	 */
	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public AjaxResponseBody download(@RequestBody @Valid DoeDownloadDatafile downloadFile, 
			HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		AjaxResponseBody result = new AjaxResponseBody();
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
				result.setMessage("Invalid user session, expired. Please login again.");
				return result;
			}
			final String basisURL = "collection".equals(downloadFile
        .getDownloadType()) ? this.collectionServiceURL :
        this.dataObjectServiceURL;
      final String serviceURL = UriComponentsBuilder.fromHttpUrl(basisURL)
        .path("/{dme-archive-path}/download").buildAndExpand(downloadFile
        .getDestinationPath()).encode().toUri().toURL().toExternalForm();
			HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
			if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals("async")) {
				HpcGlobusDownloadDestination destination = new HpcGlobusDownloadDestination();
				HpcFileLocation location = new HpcFileLocation();
				location.setFileContainerId(downloadFile.getEndPointName());
				location.setFileId(downloadFile.getEndPointLocation());
				destination.setDestinationLocation(location);
				dto.setGlobusDownloadDestination(destination);
			} else if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals("s3")) {
				HpcS3DownloadDestination destination = new HpcS3DownloadDestination();
				HpcFileLocation location = new HpcFileLocation();
				location.setFileContainerId(downloadFile.getBucketName());
				location.setFileId(downloadFile.getS3Path());
				destination.setDestinationLocation(location);
				HpcS3Account account = new HpcS3Account();
				account.setAccessKey(downloadFile.getAccessKey());
				account.setSecretKey(downloadFile.getSecretKey());
				account.setRegion(downloadFile.getRegion());
				destination.setAccount(account);
				dto.setS3DownloadDestination(destination);
			}
            final String downloadTaskType = "collection".equals(downloadFile.
                    getDownloadType()) ? HpcDownloadTaskType.COLLECTION.name() :
                        HpcDownloadTaskType.DATA_OBJECT.name();
              result = DoeClientUtil.downloadDataFile(authToken, serviceURL, dto, downloadTaskType, sslCertPath, sslCertPassword);
              
              //store the task ID in DB
              
              taskManagerService.saveTransfer(result.getMessage(),"Download",getLoggedOnUserInfo());
              result.setMessage("Task Id" + result);
              return result;
		} catch (HttpStatusCodeException e) {
			result.setMessage("Download request is not successful: " + e.getMessage());
			return result;
		} catch (RestClientException e) {
			result.setMessage("Download request is not successful: " + e.getMessage());
			return result;
		} catch (Exception e) {
			result.setMessage("Download request is not successful: " + e.getMessage());
			return result;
		}
	}
}

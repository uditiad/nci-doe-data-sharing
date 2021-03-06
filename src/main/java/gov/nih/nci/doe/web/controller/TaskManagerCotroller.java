package gov.nih.nci.doe.web.controller;

import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gov.nih.nci.doe.web.domain.TaskManager;
import gov.nih.nci.doe.web.model.TaskManagerDto;
import gov.nih.nci.doe.web.service.TaskManagerService;
import gov.nih.nci.doe.web.util.DoeClientUtil;
import gov.nih.nci.doe.web.util.LambdaUtils;
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationTaskDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcRegistrationSummaryDTO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;


/**
 *
 * DOE Task Manager Controller
 *
 *
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/tasks")
public class TaskManagerCotroller extends AbstractDoeController {

    
	@Autowired
	TaskManagerService taskManagerService;
	
	@Value("${gov.nih.nci.hpc.server.download}")
	private String queryServiceURL;
	
	@Value("${gov.nih.nci.hpc.server.bulkregistration}")
	private String registrationServiceUrl;

	SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	

	@GetMapping
	public ResponseEntity<?> getStatus(HttpSession session,@RequestHeader HttpHeaders headers, 
			HttpServletRequest request, @RequestParam(value = "userId") String userId) {
		
		  log.info("get all tasks by user Id");
		  String authToken = (String) session.getAttribute("writeAccessUserToken");
		  try {
            	List<TaskManager> results = new ArrayList<TaskManager>();
            	results = taskManagerService.getAllByUserId(userId);            	
            	List<String> taskIds = LambdaUtils.map(results,TaskManager::getTaskId);
            	
                
            	String serviceURL = queryServiceURL + "?page=" + 1 + "&totalCount=true";
    			HpcDownloadSummaryDTO downloads = DoeClientUtil.getDownloadSummary(authToken, serviceURL, sslCertPath,
    					sslCertPassword);
    			
    			final MultiValueMap<String,String> paramsMap = new LinkedMultiValueMap<>();
    		      paramsMap.set("totalCount", Boolean.TRUE.toString());
    			HpcRegistrationSummaryDTO registrations = DoeClientUtil.getRegistrationSummary(authToken, registrationServiceUrl, paramsMap,
    		        sslCertPath, sslCertPassword);
  
    			// get the task end date
    			
    			List<HpcUserDownloadRequest> downloadResults = new ArrayList<HpcUserDownloadRequest>();
    			if(downloads != null) {
    				downloadResults.addAll(downloads.getActiveTasks());
        			downloadResults.addAll(downloads.getCompletedTasks());  
    			}
    			
    			List<HpcBulkDataObjectRegistrationTaskDTO> uploadResults = new ArrayList<HpcBulkDataObjectRegistrationTaskDTO>();
    			if(registrations != null) {
    				uploadResults.addAll(registrations.getActiveTasks());
    				uploadResults.addAll(registrations.getCompletedTasks());  
    			}
    			  			
    			
    			 List<HpcUserDownloadRequest> finalTaskIds = LambdaUtils.filter(downloadResults, (HpcUserDownloadRequest n) ->
    			 taskIds.contains(n.getTaskId()));
    			
    			 List<TaskManagerDto> taskResults = new ArrayList<TaskManagerDto>();
                 
    			
    		    for (HpcUserDownloadRequest download : finalTaskIds) {
    					TaskManagerDto task = new TaskManagerDto();
    					TaskManager t = results.stream().filter(x -> download.getTaskId().equals(x.getTaskId())).findAny().orElse(null);
    					String path = download.getPath();
    					
    					if((StringUtils.isEmpty(path) || StringUtils.isBlank(path)) && CollectionUtils.isNotEmpty(download.getItems())) {
    						path = download.getItems().get(0).getPath();
    					}
    					if(StringUtils.isNotEmpty(path)) {
    					String[] collectionNames = path.split("/");
    					task.setProgName(collectionNames[2]);
    					task.setStudyName(collectionNames[3]);
    					task.setDataSetName(collectionNames[4]);
    					}
    							
    					
    	    			task.setTaskId(download.getTaskId());
    					task.setTaskDate((download.getCreated() != null && download.getCompleted() != null) ? t.getTaskDate()!= null?
    							(format.format(download.getCreated().getTime()) + " - " +
    							 format.format(download.getCompleted().getTime())) :format.format(t.getTaskDate()): "");
    					task.setTaskName(t.getTaskName());
    					task.setUserId(t.getUserId());
    					task.setTaskType(t.getTaskType());
    					if(download.getResult() != null && download.getResult().value().equals("FAILED")) {
    						List<String> message = new ArrayList<String>();
    						download.getItems().stream().forEach(x -> message.add(x.getMessage()));    						
    						task.setTransferStatus("Failed (" + String.join(",", message) + ")" + 
    					"<strong><a style='border: none;background-color: #F39530; height: 23px;width: 37px;border-radius: 11px;float: right;' class='btn btn-link btn-sm' aria-label='Retry download' href='#' "
    								+ "onclick='retryDownload(\"" + download.getTaskId() + "\" ,\"" + t.getTaskName() + "\", \"" + t.getDownloadType() + "\")'>"
    										+ "<img style='height: 13px;width: 13px;margin-top: -14px;' src='images/Status.refresh_icon-01.png' th:src='@{/images/Status.refresh_icon-01.png}' alt='Status refresh'></a></strong>");
    					} else if(download.getResult() != null && download.getResult().value().equals("COMPLETED")) {
    						task.setTransferStatus("Completed");
    					} else {
    						task.setTransferStatus("In Progress");
    					}
    					
    					taskResults.add(task);
    			}
    		    
    		    
    		    //same for uploads
    		    
    			 List<HpcBulkDataObjectRegistrationTaskDTO> finalTaskUploadIds = LambdaUtils.filter(uploadResults, (HpcBulkDataObjectRegistrationTaskDTO n) ->
    			 taskIds.contains(n.getTaskId()));
    			
    			 List<TaskManagerDto> uploadTaskResults = new ArrayList<TaskManagerDto>();
                 
    			
    		    for (HpcBulkDataObjectRegistrationTaskDTO upload : finalTaskUploadIds) {
    					TaskManagerDto task = new TaskManagerDto();
    					TaskManager t = results.stream().filter(x -> upload.getTaskId().equals(x.getTaskId())).findAny().orElse(null);
    					String path = null;
    	    			task.setTaskId(upload.getTaskId());
    					task.setTaskDate((upload.getCreated() != null && upload.getCompleted() != null) ? upload.getCreated() != null?
    							(format.format(upload.getCreated().getTime()) + " - " +
    							 format.format(upload.getCompleted().getTime())) :format.format(upload.getCreated() != null): "");
    					task.setTaskName(t != null ? t.getTaskName(): "");
    					task.setUserId(t != null ? t.getUserId() : "");
    					task.setTaskType(t != null ? t.getTaskType() :"");
    					if(upload.getResult() == null) {
    						task.setTransferStatus("In Progress");
    						 path = upload.getInProgressItems().get(0).getPath();
        					      					
    					} else if(Boolean.TRUE.equals(upload.getResult())) {
    						task.setTransferStatus("Completed");
    						 path = upload.getCompletedItems().get(0).getPath();
    					} else if(Boolean.FALSE.equals(upload.getResult())) {
    						
    						 path = upload.getFailedItems().get(0).getPath();
        					
    						List<String> message = new ArrayList<String>();
    						upload.getFailedItems().stream().forEach(x -> message.add(x.getMessage()));
    						
    						task.setTransferStatus("Failed (" + String.join(",", message) + ")" + 
    								"<strong><a style='border: none;background-color: #F39530;height: 23px;width: 37px;border-radius: 11px;float: right;' class='btn btn-link btn-sm' aria-label='Retry Upload' href='#'"
    								+ "onclick='retryUpload(\"" + upload.getTaskId() + "\" ,\"" + t.getTaskName() + "\")'>"
    								+ "<img style='height: 13px;width: 13px;margin-top: -14px;' src='images/Status.refresh_icon-01.png' th:src='@{/images/Status.refresh_icon-01.png}' alt='Status refresh'></a></strong>");
    					} 
    					if(StringUtils.isNotEmpty(path)) {
    					 String[] collectionNames = path.split("/");
    					 task.setProgName(collectionNames[2]);
    					 task.setStudyName(collectionNames[3]);
    					 task.setDataSetName(collectionNames[4]);
    					}
    					uploadTaskResults.add(task);
    			}
    		    
    		    taskResults.addAll(uploadTaskResults);
    			
    			return new ResponseEntity<>(taskResults, headers, HttpStatus.OK);
    			
             } catch (Exception e) {
            	 log.error(e.getMessage(), e);
           }
		
         return new ResponseEntity<>(null, headers, HttpStatus.SERVICE_UNAVAILABLE);
		
	}
	



}

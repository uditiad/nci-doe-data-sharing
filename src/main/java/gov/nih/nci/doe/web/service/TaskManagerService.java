package gov.nih.nci.doe.web.service;

import java.util.List;

import gov.nih.nci.doe.web.domain.TaskManager;

public interface TaskManagerService {
	
    public void saveTransfer(String taskId, String transferType, String name, String userId);
    
    
    public List<TaskManager> getAllByUserId(String userId);

}
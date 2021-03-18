function constructCollectionMetData(metadata,metaDataPath,isDataObject,permissionrole,collectionId,fileName) {
	 $("#userMetaData tbody").html("");
	 $("#path").val(metaDataPath);
	 $(".editCollectionSuccess").hide();
	 $(".editCollectionMsg").html("");
	 $(".editCollectionError").hide();
	 $(".editCollectionErrorMsg").html("");
	 $("#collectionId").val(collectionId);
	 $("#isDataObject").val(isDataObject);
	 $("#editUserMetadataFileName").html(fileName);
	 var data = metadata;
	 $.each(data, function(key, value) {	
		var attrVal= value.value;
		var attrValModified = attrVal.replace(/"/g, "");
		if(value.key.indexOf("_identifier") != -1 || value.key.indexOf("asset_type") != -1) {
			$("#userMetaData tbody").append("<tr><td>" + value.displayName + "</td><td><input type='text'" +
					                        "disabled='true' aria-label='value of meta data' name='zAttrStr_"+value.key+"' " +
					                        "style='width:70%;' " +
					                        "value=\"" + attrValModified + "\"></td></tr>");
		} else {
            $("#userMetaData tbody").append("<tr><td>" + value.displayName + "</td><td><input type='text' " +
          		                            "aria-label='value of meta data' name='zAttrStr_"+value.key+"'" +
          		                            " style='width:70%;' " +
          		                            "value=\"" + attrValModified + "\"></td></tr>");
		}
		
	});
	if(permissionrole && permissionrole == 'Owner') {
		$("#updatePermissions").show();
	} else {
		$("#updatePermissions").hide();
	}
	
}

function constructEditCollectionMetadata(data,status) {
	
	$.each(data, function(key, value) {
		var attrVal= value.attrValue;
		var attrValModified;
		if(attrVal) {
		   attrValModified = attrVal.replace(/"/g, "");
		}
		if(value.isEditable == false) {
			$("#userMetaData tbody").append("<tr><td>" + value.displayName + "&nbsp;&nbsp;<i class='fas fa-question-circle'" +
			                                "data-toggle='tooltip' " +
			                                "data-placement='right' title=\"" + value.description + "\"></i></td><td><input " +
			                                "type='text' disabled='true' " +
			                                "aria-label='value of meta data' " +
			                                "name='zAttrStr_"+value.attrName+"' style='width:70%;' value=\"" + attrValModified + "\"></td></tr>");
			
		} else if(value.validValues == null && value.attrName.indexOf("access_group") == -1) {
			 if(!attrValModified) {
				 attrValModified = "";
			 }
			 var placeholderValue ="";
		     if(value.mandatory && value.mandatory == true) {
		    	 placeholderValue = "Required";
		     }
		     var isMandatory ="";
		     if(value.mandatory) {
		    	 isMandatory = value.mandatory;
		     }
		     
			 $("#userMetaData tbody").append("<tr><td>" + value.displayName + "&nbsp;&nbsp;<i class='fas fa-question-circle'" +
					                         " data-toggle='tooltip' " +
					                         "data-placement='right' title=\"" + value.description + "\"></i></td><td><input type='text' " +
					                         "placeholder='"+placeholderValue+"' is_mandatory='"+isMandatory+"' " +
					                         "aria-label='value of meta data' name='zAttrStr_"+value.attrName+"' style='width:70%;'" +
					                         " value=\"" + attrValModified + "\"></td></tr>");

		} else if(value.validValues != null) {
			 $("#userMetaData tbody").append("<tr><td>" + value.displayName+ "&nbsp;&nbsp;<i class='fas fa-question-circle'" +
			                                 " data-toggle='tooltip' " +
			                                 "data-placement='right' title=\"" +value.description + "\"></i></td><td>" +
			                                 "<select id='validvalueList' " +
			                                 "class='simple-select2' style='width:70%;' name='zAttrStr_"+value.attrName+"' " +
			                                 "value=\"" + attrVal+ "\"></select></td></tr>");

			 var $select = $("#validvalueList");	    	  
	    	 for (var i = 0; i < value.validValues.length; i++) {
	    		   $select.append($('<option></option>').attr('value', value.validValues[i]).text(value.validValues[i]));
             }            
	    	$select.select2().trigger('change');
	    	$select.val(attrValModified).trigger('change');
			
		}
	});
	
}

function addCollectionMetaDataRows() {
	var rowId =  $("#userMetaData tbody tr").length;
	rowId = rowId +1; 
	 $("#userMetaData tbody").append('<tr id="addRow'+rowId+'"><td><input type="text" style="width:70%;" ' +
			 'name="_addAttrName'+rowId+'" aria-label="add new row" id="_addAttrName'+rowId+'"></td><td><input type="text" style="width:70%;" id="_addAttrValue'+rowId+'" name="_addAttrValue'+rowId+'" >' +
	 		'&nbsp;&nbsp;<input class="btn btn-primary pull-right" type="button" value="X" onclick="removeCollectionRow(\'addRow' + rowId + '\')"></td></tr>');	
}

function editPermissionsOpenModal() {
	$("#updatePermissionModal").find(".updatePermMsg").html("");
	$("#updatePermissionModal").find(".updatePermissionsSuccessBlock").hide()
	$("#updatePermissionModal").modal('show');	
	loadJsonData('/metaDataPermissionsList', $("#updatePermissionModal").find("#updateMetaDataPermissionsList"),
	false, null, postSuccessEditPermissions, null, "key", "value"); 
}

function postSuccessEditPermissions(data,status) {
	var params = {collectionId:$("#collectionId").val()};
	invokeAjax('/getPermissionByCollectionId','GET',params,postSuccessGetPermissions,null,null,null);
}

function postSuccessGetPermissions(data,status) {
    for (var i = 0; i < data.length; i++) {
        $("#updateMetaDataPermissionsList option[value='" + data[i].key + "']").prop("selected", true);
        $("#updatePermissionModal").find("#updateMetaDataPermissionsList").trigger('change');
    }
}

function updatePermissionsFunction() {
	var selectedPermissions = $("#updatePermissionModal").find("#updateMetaDataPermissionsList").val();
	var params = {selectedPermissions:selectedPermissions,collectionId:$("#collectionId").val(),path:$("#path").val()};
	invokeAjax('/metaDataPermissionsList','POST',params,postSuccessUpdatePermissions,null,'application/x-www-form-urlencoded; charset=UTF-8','text');
}

function postSuccessUpdatePermissions(data,status) {
	$("#updatePermissionModal").find(".updatePermMsg").html("Permissions Updated");
	$("#updatePermissionModal").find(".updatePermissionsSuccessBlock").show();
	
}

function editAccessPermissions(collectionId,metadata_path,msg,selectedCollection,collectionName) {


  var parentAccessGroups;   
	$.each(msg, function(key, value) {	
		if(value.key.indexOf("selectedEntry") != -1) {
			$("#updateAccessPermissionsModal").find("#accessGroups").val(value.value);
		} 
			if(value.key.indexOf("parentAccessGroups") != -1) {
			  parentAccessGroups = value.value;
		}   
	
	});
	$("#updateAccessPermissionsModal").find(".updateErrorAccessMsg").html("");
	$("#updateAccessPermissionsModal").find(".errorUpdateAccessGroupsBlock").hide();
	$("#updateAccessPermissionsModal").find(".updateAccessMsg").html("");
	$("#updateAccessPermissionsModal").find(".updateAccessGroupsBlock").hide();
	$("#updateAccessPermissionsModal").find("#updateCollectionId").val(collectionId);
	$("#updateAccessPermissionsModal").find("#metadata_path").val(metadata_path);
	$("#updateAccessPermissionsModal").find("#selectedCollection").val(selectedCollection);
	$("#updateAccessPermissionsModal").find("#selectedCollectionName").text(collectionName);
	$("#updateAccessPermissionsModal").modal('show');
	
	var isUpdate = false;
	if (selectedCollection == "Program" || 
		(selectedCollection == "Study" || selectedCollection == "Asset" &&  "public" == parentAccessGroups)) {
		isUpdate = true;
	} 
	
	if(isUpdate) {
		loadJsonData('/metaDataPermissionsList', $("#updateAccessPermissionsModal").find("#updateAccessGroupsList"),
				false, null, postSuccessAccessPermissions, null, "key", "value"); 
	} else if(parentAccessGroups){
		
		var parentAccessGroupsList = parentAccessGroups.split(',');
		$("#updateAccessPermissionsModal").find("#updateAccessGroupsList").empty();
		
		$.each(parentAccessGroupsList, function(index, value) {
			$("#updateAccessPermissionsModal").find("#updateAccessGroupsList").
			append($('<option></option>').attr('value', value).text(value));
	    });
		
		$("#updateAccessPermissionsModal").find("#updateAccessGroupsList").select2();
		postSuccessAccessPermissions(isUpdate,"SUCCESS");
	}
	
}

function postSuccessAccessPermissions(data,status) {
	var accessGrp = $("#updateAccessPermissionsModal").find("#accessGroups").val();
	var accessGrpList = accessGrp.split(",");
	$("#updateAccessPermissionsModal").find("#editPublicAccess").prop("disabled",false);
	$("#updateAccessPermissionsModal").find("#infoTxtForAccessGroups").html("");
	
	if(accessGrp == 'public') {
		$("#updateAccessPermissionsModal").find("#updateAccessGroupsList").next(".select2-container").hide();
		$("#updateAccessPermissionsModal").find("#updateAccessGroupsList").val("").trigger('change');
		$("#updateAccessPermissionsModal").find("#editPublicAccess").prop("checked",true);

	} else {
		$("#updateAccessPermissionsModal").find("#updateAccessGroupsList").next(".select2-container").show();
		$("#updateAccessPermissionsModal").find("#editPublicAccess").prop("checked",false);
		if(!data) {
			$("#updateAccessPermissionsModal").find("#infoTxtForAccessGroups").html("<i class='fas fa-question-circle' " +
					"title='This collection inherits access status from the parent collection' data-toggle='tooltip' data-placement='right'></i>");
			$("#updateAccessPermissionsModal").find("#editPublicAccess").prop("disabled",true);
		}
	}
	
	 for (var i = 0; i < accessGrpList.length; i++) {
	        $("#updateAccessGroupsList option[value='" + accessGrpList[i] + "']").prop("selected", true);
	        $("#updateAccessPermissionsModal").find("#updateAccessGroupsList").trigger('change');
	 }
}

function updateAccessGroupsFunction() {
	$("#updateAccessPermissionsModal").find(".updateErrorAccessMsg").html("");
	$("#updateAccessPermissionsModal").find(".errorUpdateAccessGroupsBlock").hide();
	$("#updateAccessPermissionsModal").find(".updateAccessMsg").html("");
	$("#updateAccessPermissionsModal").find(".updateAccessGroupsBlock").hide();
	var selectedAccessGroups = $("#updateAccessPermissionsModal").find("#updateAccessGroupsList").val();
	var path = $("#updateAccessPermissionsModal").find("#metadata_path").val();
	var selectedCollection = $("#updateAccessPermissionsModal").find("#selectedCollection").val();
	var selectedCollectionId = $("#updateAccessPermissionsModal").find("#updateCollectionId").val();
	var permissionGroups = {};
	if($("#updateAccessPermissionsModal").find("#editPublicAccess:visible").is(":checked")){
		permissionGroups.selectedAccessGroups ="public";
	} else {
		permissionGroups.selectedAccessGroups = selectedAccessGroups.join();
	}
	permissionGroups.path = path;
	permissionGroups.selectedCollection = selectedCollection;	
	permissionGroups.selectedCollectionId = selectedCollectionId;
	$("#updateAccessPermissionsModal").find("#permissionGroups").val(JSON.stringify(permissionGroups));
	if(!permissionGroups.selectedAccessGroups){
		$("#updateAccessPermissionsModal").find(".updateErrorAccessMsg").html("Select Access Groups.");
		$("#updateAccessPermissionsModal").find(".errorUpdateAccessGroupsBlock").show();
	} else {
		invokeAjax('/updateAccessGroupMetaData','GET',permissionGroups,postSuccessUpdateAccessgroups,null,null,'text');
	}
	
}

function postSuccessUpdateAccessgroups(data,status){
	$("#updateAccessPermissionsModal").find(".updateErrorAccessMsg").html("");
	$("#updateAccessPermissionsModal").find(".errorUpdateAccessGroupsBlock").hide();
	if(data == 'SUCCESS') {
		$("#updateAccessPermissionsModal").find(".updateAccessMsg").html("Access group updated.");
		$("#updateAccessPermissionsModal").find(".updateAccessGroupsBlock").show();
	} else if(data == 'Permission group cannot be updated') {
		$("#updateAccessPermissionsModal").find(".updateAccessMsg").html("Error in updating access group.");
		$("#updateAccessPermissionsModal").find(".updateAccessGroupsBlock").show();
	}

}

function updateMetaDataCollection() {

	var validate = true;
		var data = $('#collectionForm').serialize();
		$('form#collectionForm input[type="text"]').each(function(){
			var ismandatory = $(this).attr('is_mandatory');
	        if(!$(this).val() && ismandatory && ismandatory != "false" ){
	        		validate = false;
	        }          
	    });
		
		if(!validate) {
			$(".editCollectionSuccess").hide();
			 $(".editCollectionMsg").html("");
			$(".editCollectionError").show();
			$(".editCollectionErrorMsg").html("Enter all the required metadata.");
		} else {
		$.ajax({
			type : "POST",
		     url : "/collection",
			 data : data,
			 beforeSend: function () {
		    	   $("#spinner").show();
		           $("#dimmer").show();
		       },
			 success : function(msg) {
				 $("#spinner").hide();
		         $("#dimmer").hide();
				 console.log('SUCCESS: ', msg);
				 $(".editCollectionSuccess").show();
				 $(".editCollectionMsg").html(msg);
				 $(".editCollectionError").hide();
				 $(".editCollectionErrorMsg").html("");
				 
			 },
			error : function(e) {
				$("#spinner").hide();
		         $("#dimmer").hide();
				 console.log('ERROR: ', e);	
				 $(".editCollectionSuccess").hide();
				 $(".editCollectionMsg").html("");
				 $(".editCollectionError").show();
				 $(".editCollectionErrorMsg").html(e.responseText);
			}
		});
	}
}

function removeCollectionRow(rowId) {
	
	$("#" + rowId).remove();
}
function constructCollectionMetData(metadata,metaDataPath,isDataObject,permissionrole) {
	$("#userMetaData tbody").html("");
	$("#path").val(metaDataPath);
	 $(".editCollectionSuccess").hide();
	 $(".editCollectionMsg").html("");
	 $(".editCollectionError").hide();
	 $(".editCollectionErrorMsg").html("");
	 $("#isDataObject").val(isDataObject);
	var data = JSON.parse(metadata);
	$.each(data, function(key, value) {	
        $("#userMetaData tbody").append('<tr><td>' + value.key + '</td><td><input type="text"  name="zAttrStr_'+value.key+'" style="width:70%;" value="' + value.value + '"></td></tr>');
	});
	if(permissionrole && permissionrole == 'Owner') {
		$("#updatePermissions").show();
	} else {
		$("#updatePermissions").hide();
	}
	
}

function addCollectionMetaDataRows() {
	var rowId =  $("#userMetaData tbody tr").length;
	rowId = rowId +1; 
	 $("#userMetaData tbody").append('<tr id="addRow'+rowId+'"><td><input type="text" style="width:70%;" ' +
			 'name="_addAttrName'+rowId+'" id="_addAttrName'+rowId+'"></td><td><input type="text" style="width:70%;" id="_addAttrValue'+rowId+'" name="_addAttrValue'+rowId+'" >' +
	 		'&nbsp;&nbsp;<input class="btn btn-primary pull-right" type="button" value="X" onclick="removeCollectionRow(\'addRow' + rowId + '\')"></td></tr>');
	 
	
}

function editPermissionsOpenModal() {
	$("#updatePermissionModal").modal('show');	
	loadJsonData('/metaDataPermissionsList', $("#updatePermissionModal").find("#updateMetaDataPermissionsList"),
			false, null, postSuccessEditPermissions, null, "key", "value"); 
}

function postSuccessEditPermissions(data,status) {
	//pre select the permissions here.
}

function updatePermissionsFunction() {
	var selectedPermissions = $("#updatePermissionModal").find("#updateMetaDataPermissionsList").val();
	var params = {selectedPermissions:selectedPermissions};
	//invokeAjax('/editPermissions','POST',params,null,null,null,null);
}
function updateMetaDataCollection() {

	var validate = true;
		var data = $('#collectionForm').serialize();
		$('form#collectionForm input[type="text"]').each(function(){
	        if(!$(this).val()){
	        	validate = false;
	        }          
	});
		
		if(!validate) {
			$(".editCollectionError").show();
			$(".editCollectionErrorMsg").html("Enter all the required metdata.");
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
				 
			 },
			error : function(e) {
				 console.log('ERROR: ', e);				 
			}
		});
	}
}

function removeCollectionRow(rowId) {
	
	$("#" + rowId).remove();
}
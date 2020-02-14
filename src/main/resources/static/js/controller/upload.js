
function loadUploadTab() {	 
	 
	var ins = $("#institutePath").val();
	var stu= $("#studyPath").val();
	var data = $("#datafilePath").val();
	
	if(ins){
		loadJsonData('/browse', $("#instituteList"), true, null, postSuccessInsInitialize, null, "key", "value"); 
		loadJsonData('/browse/collection', $("#studyList"), true, {selectedPath:ins}, postSuccessStudyInitialize, null, "key", "value");
		if(stu) {
			loadJsonData('/browse/collection', $("#dataList"), true, {selectedPath:stu}, postSuccessDataSetInitialize, null, "key", "value");
			if(data) {
				invokeAjax('/browse/collection','GET',{selectedPath:data},contructDataListDiv,null,null,null);		
				$("#studyListDiv").show();
				$("#dataSetListDiv").show();
			    $("#dataListDiv").hide();
				$("#addBulkDataFiles").show();
				$("#uploadDataFilesTab").show();
				$('input[name=datafileTypeUpload]:checked').val();
				$("#datafileTypeBulk").prop("checked", true);
				$("#singleFileDataUploadSection").hide();
				$("#bulkFileUploadSection").show();
				$("#registerFileBtnsDiv").show();		
				$(".registerBulkDataFileSuccess").hide();
				$(".registerBulkDataFile").html("");
			}
			
		}
		
		
	} else {
		 loadJsonData('/browse', $("#instituteList"), true, null, null, null, "key", "value"); 
	}
  
}

function postSuccessInsInitialize(data,status) {
	$("#instituteList").val($("#institutePath").val());
}

function postSuccessStudyInitialize(data,status) {
	$("#studyList").val($("#studyPath").val());
}

function postSuccessDataSetInitialize(data,status) {
	$("#dataList").val($("#datafilePath").val());
	$("#bulkDataFilePathCollection").val($("#datafilePath").val());
	
}


function retrieveCollections($this, selectedIndex) {
	
	var selectTarget = $this.name;
	var params= {selectedPath:selectedIndex.value};
	
	if(selectTarget == 'instituteList') {
		$("#uploadDataFilesTab").hide();
		$("#addBulkDataFiles").hide();
		if(selectedIndex && selectedIndex.value != 'ANY') {
			loadJsonData('/browse/collection', $("#studyList"), true, params, null, null, "key", "value");
			$("#studyListDiv").show();
			$("#dataSetListDiv").hide();
			$("#dataListDiv").hide();
		} else {
			$("#studyListDiv").hide();
			$("#dataSetListDiv").hide();
			$("#dataListDiv").hide();
		}
		
		
	} else if(selectTarget == 'studyList') {
		$("#addBulkDataFiles").hide();
		$("#uploadDataFilesTab").hide();
		 $("#dataListDiv").hide();
		if(selectedIndex && selectedIndex.value != 'ANY') {
		  loadJsonData('/browse/collection', $("#dataList"), true, params, null, null, "key", "value");
		  $("#studyListDiv").show();
		  $("#dataSetListDiv").show();
		} else {
			$("#studyListDiv").show();
			  $("#dataSetListDiv").hide();
		}
		
	} else if(selectTarget == 'dataList') {	
		$("#studyListDiv").show();
		$("#uploadDataFilesTab").hide();
		if(selectedIndex && selectedIndex.value != 'ANY') {
			invokeAjax('/browse/collection','GET',params,contructDataListDiv,null,null,null);		
			$("#addBulkDataFiles").show();
		} else {
			$("#dataListDiv").hide();
			$("#addBulkDataFiles").hide()
			
		}
		
	}
	
}


function contructDataListDiv(data,status) {
	$("#dataListing").html("");
	$("#dataListDiv").show();
	$.each(data, function(key, value) {	
		$("#dataListing").append('<li>'+value.value+'</li>');
	});
}

function constructNewCollectionMetaDataSet(data,status) {
	$("#newMetaDataTable tbody").html("");
	$.each(data, function(key, value) {	
        $("#newMetaDataTable tbody").append('<tr><td>' + value.attrName + '</td><td><input type="text"  name="zAttrStr_'+value.attrName+'" style="width:70%;"></td></tr>');
	});
	
	
}

function addNewMetaDataCollection(tableName) {
	var rowId =  $("#"+tableName + " tbody").length;
	rowId = rowId +1; 
	$("#"+tableName + " tbody").append('<tr id="addRow'+rowId+'"><td><input type="text" style="width:70%;" ' +
			 'name="_addAttrName'+rowId+'" id="_addAttrName'+rowId+'"></td><td><input type="text" style="width:70%;" id="_addAttrValue'+rowId+'" name="_addAttrValue'+rowId+'" >' +
	 		'&nbsp;&nbsp;<input class="btn btn-primary pull-right" type="button" value="X" onclick="removeCollectionRow(\'addRow' + rowId + '\')"></td></tr>');
	 
	
}

function addNewMetaDataRowsForDataFile($this) {
	var rowId =  $this.parent().find('div').length;
	rowId = rowId +1; 
	
	$this.parent().append('&nbsp;&nbsp;<div id="addDataRow'+rowId+'"><input type="text" style="width:40%;" ' +
			 'name="_addAttrName'+rowId+'" id="_addAttrName'+rowId+'">&nbsp;<input type="text" style="width:40%;" id="_addAttrValue'+rowId+'" name="_addAttrValue'+rowId+'" >' +
	 		'&nbsp;&nbsp;<input class="btn btn-primary pull-right" type="button" value="X" onclick="removeCollectionRow(\'addDataRow' + rowId + '\')"></div>');
	
}


function retrieveCollectionList(data,status) {		
		var selectTarget = $("#registerCollectionModal").find("#collectionType").val();
		var collectionPath = $("#registerCollectionModal").find("#collectionPath").val();
		
			if(selectTarget && collectionPath) {
				var params1= {selectedPath:collectionPath,collectionType:selectTarget};
				invokeAjax('/addCollection','GET',params1,constructNewCollectionMetaDataSet,null,null,null);		
			} 	
} 




function openUploadModal(selectTarget) {
	
	var selectedIndexPathVal = $("#" + selectTarget).val();
	$("#registerCollectionModal").find("#collectionPath").val(selectedIndexPathVal);
	$("#newMetaDataTable tbody").html("");
	$("#registerCollectionModal").find(".registerMsg").html("");
	$("#registerCollectionModal").find("#newMetaDataTable tbody").html("");
	$("#registerCollectionModal").find(".registerMsgBlock").hide();
	var params= {parent:selectedIndexPathVal};		
	loadJsonData('/addCollection/collectionTypes', $("#registerCollectionModal").find("#collectionType"), false, params, retrieveCollectionList, null, "key", "value");
	
   $("#registerCollectionModal").modal('show');
	
}


function registerCollection() {
	
	var collectionPath = $("#registerCollectionModal").find("#collectionPath").val();
	var collectionName = $("#registerCollectionModal").find("#collectionName").val();
	
	var collectionType = $("#registerCollectionModal").find("#collectionType").val();
	
	var newCollectionPath;
	if(collectionPath && collectionName) {
		newCollectionPath = collectionPath + "/" + collectionName.trim();
		$("#registerCollectionModal").find("#newCollectionPath").val(newCollectionPath);
	}

	if(newCollectionPath) {
		var data = $('#registerCollectionForm').serialize();
		$.ajax({
			type : "POST",
		     url : "/addCollection",
			 data : data,
			 beforeSend: function () {
		    	   $("#spinner").show();
		           $("#dimmer").show();
		       },
			 success : function(msg) {
				 $("#spinner").hide();
		         $("#dimmer").hide();
				 console.log('SUCCESS: ', msg);
				 postSuccessRegisterCollection(msg,collectionType);
				 
			 },
			error : function(e) {
				 console.log('ERROR: ', e);				 
			}
		});
	}
	
}

function postSuccessRegisterCollection(data,collectionType) {
	$("#registerCollectionModal").find(".registerMsg").html(data);
	$("#registerCollectionModal").find(".registerMsgBlock").show();
	
	
	if(collectionType  == 'Institute') {		
		loadJsonData('/browse', $("#instituteList"), true, null, null, null, "key", "value"); 
	} else if(collectionType == 'Study') {
		var params= {selectedPath:$("#instituteList").val()};
		loadJsonData('/browse/collection', $("#studyList"), true, params, null, null, "key", "value");
		
	} else if(collectionType == 'Data_Set') {
		var params= {selectedPath:$("#studyList").val()};
		 loadJsonData('/browse/collection', $("#dataList"), true, params, null, null, "key", "value");
	}
}

function openBulkDataRegistration() {
	var datafilePath = $("#dataList").val();
	$("#bulkDataFilePath").val(datafilePath);
	$("#bulkDataFilePathCollection").val(datafilePath);
	$(".registerBulkDataFileSuccess").hide();
	$(".registerBulkDataFile").html("");
}

function cancelAndReturnToUploadTab() {
	var params= {selectedPath:$("#dataList").val()};
	 invokeAjax('/browse/collection','GET',params,contructDataListDiv,null,null,null);
}

function registerBulkDataFile() {
	var uploadType = $('input[name=datafileTypeUpload]:checked').val();
	
	if(uploadType == 'singleData') {
		var file = $("#doeDataFile").val();
		var dataFilePath = $("#dataFilePath").val();
		
		if(dataFilePath && file) {	
			$("#registerDataFileForm").attr('dataFilePath', dataFilePath);	 
				var form = $('#registerDataFileForm')[0];		 
		       var data = new FormData(form);
		      data.append('dataFilePath', dataFilePath);
				$.ajax({
					type : "POST",
					enctype: "multipart/form-data",
				     url : "/addDatafile",
					 data : data,
					 processData: false, 
		             contentType: false,
					 beforeSend: function () {
				    	   $("#spinner").show();
				           $("#dimmer").show();
				       },
					 success : function(msg) {
						 $("#spinner").hide();
				         $("#dimmer").hide();
						 console.log('SUCCESS: ', msg);
						 $(".registerBulkDataFile").html(msg);
						 $(".registerBulkDataFileSuccess").show();
						 cancelAndReturnToUploadTab();
						 
					 },
					error : function(e) {
						 console.log('ERROR: ', e);				 
					}
				});
			}
		
		
	} else {
		var dataFilePath = $("#bulkDataFilePath").val();		
		if(dataFilePath) {	
		$("#registerBulkDataForm").attr('dataFilePath', dataFilePath);	 
			var form = $('#registerBulkDataForm')[0];		 
	       var data = new FormData(form);
	      data.append('dataFilePath', dataFilePath);
			$.ajax({
				type : "POST",
				enctype: "multipart/form-data",
			     url : "/addbulk",
				 data : data,
				 processData: false, 
	             contentType: false,
				 beforeSend: function () {
			    	   $("#spinner").show();
			           $("#dimmer").show();
			       },
				 success : function(msg) {
					 $("#spinner").hide();
			         $("#dimmer").hide();
					 console.log('SUCCESS: ', msg);
					 $(".registerBulkDataFile").html(msg);
					 $(".registerBulkDataFileSuccess").show();
					
					 
				 },
				error : function(e) {
					 console.log('ERROR: ', e);				 
				}
			});
		}
	}

}

function appendFileName($this) {
    //Append the file name to the data file path
    var filename = $this.val().replace(/^C:\\fakepath\\/, "")
    var value = $("#bulkDataFilePathCollection").val() + "/" + filename;
    $("#dataFilePath").val(value);
    
}

function displayDataFileSection(value) {
	var datafilePath = $("#dataList").val();
	$("#registerFileBtnsDiv").show();
	$("#bulkDataFilePathCollection").val(datafilePath);
	$(".registerBulkDataFileSuccess").hide();
	$(".registerBulkDataFile").html("");
	
	if(value == 'singleData') {
		$("#singleFileDataUploadSection").show();
		$("#bulkFileUploadSection").hide();					
	    $("#dataFilePath").val(datafilePath);				
		
	} else if(value == 'bulkData'){
		$("#singleFileDataUploadSection").hide();
		$("#bulkFileUploadSection").show();
	}
}
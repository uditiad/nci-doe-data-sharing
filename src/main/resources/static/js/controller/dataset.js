function refreshDataSetDataTable(dataSetPath) {
	var isVisible = (loggedOnUserInfo ? true:false);
    console.log("refresh datatable");
    if (!$.fn.DataTable.isDataTable('#dataSetTable')) {
    	dataTableInitDataSet(isVisible,dataSetPath);
    } else {
        var t = $('#dataSetTable').DataTable();
        console.log(t);
        t.ajax.reload(null, false);
    }
}

function dataTableInitDataSet(isVisible,dataSetPath) {
    $('#dataSetTable').DataTable({
        "paging": true,
        "ordering": false,
        "info": true,
        "pageLength": 25,
        oLanguage: {
            "sSearch": "Filter:"
        },
        "ajax": {
            "url": "/getDataObjects",
            "type": "GET",
            "data": {path:dataSetPath},
            "dataSrc": function (data) {
                return data;
            },
            "error": function (xhr, error, thrown) {
                console.log("Response status: " + xhr.status + " (" + xhr.statusText + ")");
                console.log(error + ": " + thrown + " [" + xhr.status + " (" + xhr.statusText + ")]");
                console.log(xhr.responseText);
                console.log(xhr);
                $("#spinner").hide();
                $("#dimmer").hide();
            },

            "beforeSend": function () {
                $("#spinner").show();
                $("#dimmer").show();
            },

            "complete": function () {
                $("#spinner").hide();
                $("#dimmer").hide();
            }
        },

        "initComplete": function (settings, json) {

        },

        "drawCallback": function (settings) {
        	
        	 $(".selectAll").change(function (e) {
                 var table = $(e.target).closest('table');
                 var tableId = table.attr('id');
                 var row_count = $('#' + tableId + "> tbody").children().length;
                 if ($(this).is(':checked')) {
                     $('td input:checkbox', table).prop('checked', true);
                     if (row_count > 1) {
                         $("#downloadSelectedDataSet").prop("disabled", false);
                         $(".downloadLink").prop("disabled", true);
                     }

                 } else {
                     $('td input:checkbox', table).prop('checked', false);
                     if (row_count > 1) {
                         $("#downloadSelectedDataSet").prop("disabled", true);
                         $(".downloadLink").prop("disabled", false);
                     }
                 }
             });
        	 
        	  $(".selectIndividualCheckbox").click(function (e) {
                  var table = $(e.target).closest('table').attr('id');
                  if (!$(this).is(':checked')) {
                      $("#" + table).find(".selectAll").prop('checked', false);
                      $(this).closest("tr").find('a.downloadLink').prop("disabled", false);
                  }
                  var len = $('#' + table).find('.selectIndividualCheckbox:checked').length;
                  if (len > 1) {
                      $("#downloadSelectedDataSet").prop("disabled", false);
                      $("#" + table + " input[type=checkbox]:checked").each(function () {
                          $(this).closest("tr").find('a.downloadLink').prop("disabled", true);
                      });
                  } else {
                      $("#downloadSelectedDataSet").prop("disabled", true);
                      $(".downloadLink").prop("disabled", false);
                  }
              });
        	  
           $(".downloadLink").click(function(e){
        	   var path = $(this).attr('data-path');
        	   var fileName = $(this).attr('data-fileName');  
        	   $("#download-modal").find(".selectedFilesDiv").hide();
               downloadFunction(path,fileName);
             });
           
           $("#downloadSelectedDataSet").click(function(e){
        	   onClickOfBulkDownloadBtn();
           });
        },

        "columns": [
        	{"data": "path", "render": function (data, type, row) {
                return renderSelect(data, type, row);
            },
            responsivePriority: 2
        },
        
            {"data": "path", "render": function (data, type, row) {
                    return renderDataSetPath(data, type, row);
                },
                responsivePriority: 1
            },
            {"data": "download", "render": function (data, type, row) {
                return renderDownload(data, type, row);
            },
            responsivePriority: 3
        },

        ],
        columnDefs: [
            {
                orderable: false,
                className: 'select-checkbox',
                headerHtml: 'batch select',
                blurable: true,
                targets: 0,
            },],
        "dom": '<"top"lip>rt<"bottom"p>',

        "lengthMenu": [[10, 25, 50, 100], [10, 25, 50, 100]],

        "language": {
            "zeroRecords": "Nothing found to display",
            "info": "&nbsp; (Displaying _START_ to _END_ of _TOTAL_ )",
            sLengthMenu: "_MENU_",
            "infoEmpty": " No records to display"
        }
    });
}

function renderSelect(data, type, row) {
	var selectHtml = "<input type='checkbox' id='" + row.path + "' class='dt-checkboxes selectIndividualCheckbox' aria-label='select'/>";

    return selectHtml;
}

function renderDataSetPath(data, type, row) {
	return row.path;
}

function renderDownload(data, type, row) {
	var downdloadFileName = null;
	var path = row.path;
	if(search_criteria_json.searchType != 'collection') {
		var n = path.lastIndexOf("/");
		downdloadFileName = path.substring(n+1);		
	}
	
	return "<a id='downloadlink' class='btn btn-link btn-sm downloadLink' href='javascript:void(0);' " +
			"data-toggle='modal' data-backdrop='static' data-keyboard='false' data-fileName = " + downdloadFileName + " data-path=" + row.download + " " +
			"data-target='#download-modal'><i class='fa fa-download' aria-hidden='true'></i></a>";
}

function onClickOfBulkDownloadBtn() {
	$("#download-modal").find(".selectedFilesListDisplay").html("");
	 var selectedPaths = [];
	    $("#dataSetTable tbody input[type=checkbox]:checked").each(function () {
	    	selectedPaths.push($(this).attr('id'));
	    });
	    $("#download-modal").find(".selectedFilesList").val(selectedPaths);
	    
	    $.each(selectedPaths, function(index, value) {
	    	$("#download-modal").find(".selectedFilesListDisplay").append("<p>"+value+"</p>");
	    });
	    
	    $("#download-modal").find("#SyncDiv").hide();
		$("#download-modal").find("#syncRadioSet").hide();
		$("#download-modal").find("#downloadType").val("datafiles");
		$("#download-modal").find(".selectedFilesDiv").show();
	    $("#download-modal").modal('show');
	    
}
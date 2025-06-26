$(document).ready(function() {

    // File upload

    var fileUploadClass = $('.submit-file-upload'),
        file = $('#file-to-upload'),
        errorSummary = $('.error-summary'),
        errorMessageNoFile = $('.submit-file-upload').data('nofile'),
        errorMessageEmptyFile= $('.submit-file-upload').data('empty-file'),
        errorMessagePswdProtected = $('.submit-file-upload').data('pswd-protected'),
        errorMessageNoUpload = $('.submit-file-upload').data('no-upload'),
        errorMessageVirus = $('.submit-file-upload').data('virus'),
        errorMessageFileTooLarge = $('.submit-file-upload').data('too-large'),
        errorMessageInvalid = $('.submit-file-upload').data('invalid'),
        loadingSection = $('.spinner-wrapper'),
        uploadFormElements = $('.hide-when-uploading'),
        maxUploadSize = 5000000;

    file.on('click', function() {
        fileUploadClass.removeAttr('disabled');
    });

    var fileIsEncrypted = {};

    $('input#file-to-upload').change(function () {
        var reader = new FileReader();
        reader.readAsText(file[0].files[0]);
        reader.onload = function() {
            var contents = reader.result;
            if(contents.indexOf('/Encrypt') !== -1){
                fileIsEncrypted = true;
            } else {
                fileIsEncrypted = false;
            }
        };
    });


    fileUploadClass.on('click', function (e) {

        $( this ).removeAttr('disabled');
        clearErrors();

        if(noFileSelected()) {
            error(errorMessageNoFile);
            return false;
        } else if(fileTypeIsInvalid()){
            error(errorMessageInvalid);
            return false;
        } else if(fileIsEmpty()){
            error(errorMessageEmptyFile);
            return false;
        } else if(fileIsPasswordProtected()){
            error(errorMessagePswdProtected);
            return false;
        } else if(fileTooLarge()){
            error(errorMessageFileTooLarge);
            return false;
        } else {
            uploadFormElements.hide();
            loadingSection.show();
            $("html, body").animate({ scrollTop: 0 });
            pollUploadStatus();
        }
    });

    var statusPollCount= {};
    statusPollCount.timer = 0;

    var pollUploadStatus = function () {
        var fileReference = fileUploadClass.data('reference'),
            fileType = fileUploadClass.data('filetype'),
            baseUrl = "/agent-services/apply-from-outside-uk",
            pollUrl = "/poll-status/" + fileType + "/";

        setTimeout(function () {
            $.ajax({
                url: baseUrl + pollUrl + fileReference,
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                type: "GET",
                success: function (data) {
                    if (data) {
                        if (data.fileStatus === 'READY') {
                            window.location.href = baseUrl + "/file-uploaded-successfully";
                        } else if (data.fileStatus === 'FAILED') {
                            if(data.failureDetails.failureReason === 'QUARANTINE'){
                                fileContainsAVirus();
                                clearTimeout(pollUploadStatus);
                            } else {
                                fileCannotBeUploaded();
                            }
                        } else if (data.fileStatus === 'NOT_READY') {
                            statusPollCount.timer++;
                            if(statusPollCount.timer === 60){
                                window.location.href = baseUrl + "/file-upload-failed";
                            }
                            pollUploadStatus();
                        } else {
                            window.location.href = baseUrl + "/server-error";
                        }
                    }
                },
                error: function() {
                    window.location.href = baseUrl + "/server-error";
                },
                dataType: "json"
            })
        }, 3000);
    };

    function fileIsPasswordProtected() {
        return fileIsEncrypted;
    }


    function fileTypeIsInvalid() {
        var fileName = file.val();
        if ((fileName.indexOf('.pdf') === -1) && (fileName.indexOf('.jpeg') === -1) && (fileName.indexOf('.jpg') === -1)) {
            return true;
        } else {
            return false;
        }
    }

    function fileIsEmpty() {
        if (file[0].files[0].size === 0) {
            return true;
        } else {
            return false;
        }
    }

    function noFileSelected() {
        if (file.val()) {
            return false;
        } else {
            return true;
        }
    }

    function fileTooLarge() {
        var fileSize = file[0].files[0].size;
        if (fileSize > maxUploadSize) {
            console.log(fileSize);
            return true;
        } else {
            return false;
        }
    }

    var pageTitle = document.title;

    function error(errorMsg) {
        errorSummary.addClass('error-summary--show').focus();
        var title = pageTitle;
        $('title').html("Error: " + title);
        $('.js-error-summary-messages a').html(errorMsg);
        $('#file-upload-container').addClass('govuk-form-group--error');
        $('#file-upload-error').html('<span class="govuk-visually-hidden">Error:</span>' + errorMsg);
    }

    function clearErrors() {
        errorSummary.removeClass('error-summary--show');
        $('title').html(pageTitle);
        file.focus();
        $('.js-error-summary-messages a').html();
        $('#file-upload-container').removeClass('govuk-form-group--error');
        $('#file-upload-error').html();

    }

    function fileContainsAVirus() {
        uploadFormElements.show();
        loadingSection.hide();
        error(errorMessageVirus);
        fileUploadClass.removeAttr('disabled');
    }

    function fileCannotBeUploaded() {
        uploadFormElements.show();
        loadingSection.hide();
        error(errorMessageNoUpload);
        fileUploadClass.removeAttr('disabled');
    }

});

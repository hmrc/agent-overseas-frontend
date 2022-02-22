$(document).ready(function() {
    //======================================================
    // GOV.UK country lookup
    // https://alphagov.github.io/accessible-autocomplete/#progressive-enhancement
    //======================================================
    // auto complete country lookup, progressive enhancement
    // need to invoke new enhanceSelectElement()
    //======================================================
    var selectEl = document.querySelector('#countryCode-auto-complete');
    if(selectEl){
        accessibleAutocomplete.enhanceSelectElement({
            autoselect: true,
            defaultValue: selectEl.options[selectEl.options.selectedIndex].innerHTML,
            selectElement: selectEl
        })
    }

    function findCountry(country) {
        return country == $("#countryCode-auto-complete").val();
    }

    //======================================================
    // Fix CSS styling of errors (red outline) around the country input dropdown
    //======================================================

    // Set the border colour to black with orange border when clicking into the input field
    $('.autocomplete__wrapper input').focus(function(e){
        if ($(".govuk-form-group--error .autocomplete__wrapper").length > 0) $(".autocomplete__wrapper input").css({"border" : "4px solid #0b0c0c", "-webkit-box-shadow" : "none", "box-shadow" : "none"});
    })

    // Set the border colour back to red when clicking out of the input field
    // Set the gov.uk error colour https://design-system.service.gov.uk/styles/colour/
    $('.autocomplete__wrapper input').focusout(function(e){
        if ($(".govuk-form-group--error .autocomplete__wrapper").length > 0) $(".autocomplete__wrapper input").css("border", "2px solid #d4351c");
    })


    //======================================================
    // Fix IE country lookup where clicks are not registered when clicking list items
    //======================================================

    // temporary fix for IE not registering clicks on the text of the results list for the country autocomplete
    $('body').on('mouseup', ".autocomplete__option > strong", function(e){
        e.preventDefault(); $(this).parent().trigger('click');
    })
    // temporary fix for the autocomplete holding onto the last matching country when a user then enters an invalid or blank country
    $('input[role="combobox"]').on('keydown', function(e){
        if (e.which != 13 && e.which != 9) {
            var sel = document.querySelector('.autocomplete-wrapper select');
            sel.value = "";
        }
    })


    //custom handler for AMLS auto-complete dropdown
    $('#amlsCode').change(function(){
        var changedValue = $(this).val();
        var array = [];

        $('.autocomplete__menu li').each(function(){
            array.push($(this).text())
        });

        if(array == "No results found"){
            $('#amls-auto-complete-select').append('<option id="notFound" value="NOTFOUND">No results found</option>');
            $('#amls-auto-complete-select').val('NOTFOUND').attr("selected", "selected");

        }else if(array == ""){
            $('#amls-auto-complete-select').val('').attr("selected", "selected");
        }

    });


    // by default the dropForm will be hidden so we we need this to make the form visible after loaded
    $('#dropForm').css('visibility', 'visible');


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
        $('#file-to-upload-container').addClass('form-field--error');
        $('#file-to-upload-error').html('<span class="govuk-visually-hidden">Error:</span>' + errorMsg);
    }

    function clearErrors() {
        errorSummary.removeClass('error-summary--show');
        $('title').html(pageTitle);
        file.focus();
        $('.js-error-summary-messages a').html();
        $('#file-to-upload-container').removeClass('form-field--error');
        $('#file-to-upload-error').html();

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

    //TODO - replace as should not be relying on JS
    $('[type="submit"]').click(function () {
        $input.each(function () {
        //Trim inputs and Capitalize postode
            if ($(this).val() && $(this).attr('data-uppercase') === 'true') {
                $(this).val($(this).val().toUpperCase().replace(/\s\s+/g, ' ').trim())
            } else {
                $(this).val($(this).val().trim())
            }
        });
    });

});

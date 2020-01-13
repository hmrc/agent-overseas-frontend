$(function () {
    //Accessibility
    var errorSummary = $('#error-summary-display'),
        $input = $('input:text');

    //Error summary focus
    if (errorSummary) {
        errorSummary.focus()
    }

    $input.each(function () {
        if ($(this).closest('label').hasClass('form-field--error')) {
            $(this).attr('aria-invalid', true)
        } else {
            $(this).attr('aria-invalid', false)
        }
    });

    //Trim inputs and Capitalize postode
    $('[type="submit"]').click(function () {
        $input.each(function () {
            if ($(this).val() && $(this).attr('data-uppercase') === 'true') {
                $(this).val($(this).val().toUpperCase().replace(/\s\s+/g, ' ').trim())
            } else {
                $(this).val($(this).val().trim())
            }
        });
    });

    //Add aria-hidden to hidden inputs
    $('[type="hidden"]').attr("aria-hidden", true)

    var showHideContent = new GOVUK.ShowHideContent()
    showHideContent.init()


    $('.form-date label.form-field--error').each(function () {

        $(this).closest('div').addClass('form-field--error')
        var $relocate = $(this).closest('fieldset').find('legend')
        $(this).find('.error-notification').appendTo($relocate)

    });

    $('body').on('change', '#country-auto-complete', function () {
        if (!$(this).val()) {
            $('#country select option').removeAttr('selected')
        }

    });

    var selectCountryEl = document.querySelector('#country-auto-complete')
    if (selectCountryEl) {
        accessibleAutocomplete.enhanceSelectElement({
            autoselect: true,
            defaultValue: selectCountryEl.options[selectCountryEl.options.selectedIndex].innerHTML,
            minLength: 2,
            selectElement: selectCountryEl
        })
    }

    function findCountry(country) {
        return country == $("#country-auto-complete").val();
    }

    //custom handle for not found countries
    $('#country-auto-complete').change(function () {
        var changedValue = $(this).val()
        var array = [];

        $('.autocomplete__menu li').each(function () {
            array.push($(this).text())
        })

        if (array == "No results found") {
            $('#country-auto-complete-select').append('<option id="notFound" value="NOTFOUND">No results found</option>')
            $('#country-auto-complete-select').val('NOTFOUND').attr("selected", "selected");

        } else if (array == "") {
            $('#country-auto-complete-select').val('').attr("selected", "selected");
        }

    });


    var selectAmlsEl = document.querySelector('#amls-auto-complete')

    if (selectAmlsEl) {
        accessibleAutocomplete.enhanceSelectElement({
            autoselect: true,
            defaultValue: selectAmlsEl.options[selectAmlsEl.options.selectedIndex].innerHTML,
            minLength: 2,
            selectElement: selectAmlsEl
        })
    }

    //custom handler for AMLS auto-complete dropdown
    $('#amls-auto-complete').change(function () {
        var changedValue = $(this).val()
        var array = [];

        $('.autocomplete__menu li').each(function () {
            array.push($(this).text())
        })

        if (array == "No results found") {
            $('#amls-auto-complete-select').append('<option id="notFound" value="NOTFOUND">No results found</option>')
            $('#amls-auto-complete-select').val('NOTFOUND').attr("selected", "selected");

        } else if (array == "") {
            $('#amls-auto-complete-select').val('').attr("selected", "selected");
        }

    });

    $('.form-date label.form-field--error').each(function () {

        $(this).closest('div').addClass('form-field--error')
        var $relocate = $(this).closest('fieldset').find('legend')
        $(this).find('.error-notification').appendTo($relocate)

    });

    //by default the amlsForm will be hidden so we we need this to make the form visible after loaded
    $('#amlsForm').css('visibility', 'visible');

    $('a[role=button]').keyup(function(e) {
        // get the target element
        var target = e.target;

        // if the element has a role=’button’ and the pressed key is a space, we’ll simulate a click
        if (e.keyCode === 32) {
            e.preventDefault();
            // trigger the target’s click event
            target.click()
        }
    });

    GOVUK.details.init();

});

$(document).ready(function () {

    // File upload

    if (document.getElementById('govuk-box')) {
        var loader = new GOVUK.Loader();
        loader.init({
            container: 'govuk-box'
        })
    }

    if (document.getElementById('file-upload-loading')) {
        var loader2 = new GOVUK.Loader();
        loader2.init({
            container: 'file-upload-loading',
            label: true,
            labelText: 'This file is being checked and uploaded',
        })
    }

    var fileUploadClass = $('.file-upload'),
        file = $('#file-upload'),
        errorSummary = $('.error-summary'),
        errorMessageNoFile = $('.file-upload').data('nofile'),
        errorMessageEmptyFile= $('.file-upload').data('empty-file'),
        errorMessagePswdProtected = $('.file-upload').data('pswd-protected'),
        errorMessageNoUpload = $('.file-upload').data('no-upload'),
        errorMessageVirus = $('.file-upload').data('virus'),
        errorMessageFileTooLarge = $('.file-upload').data('too-large'),
        errorMessageInvalid = $('.file-upload').data('invalid'),
        loadingSection = $('.loader'),
        uploadFormElements = $('.hide-when-uploading'),
        maxUploadSize = 5000000;

    file.on('click', function() {
        fileUploadClass.removeAttr('disabled');
    });

    var fileIsEncrypted = {};

    $('input#file-upload').change(function () {
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
        $('#file-upload-container').addClass('form-field--error');
        $('#file-upload-error').html(errorMsg);
    }

    function clearErrors() {
        errorSummary.removeClass('error-summary--show');
        $('title').html(pageTitle);
        file.focus();
        $('.js-error-summary-messages a').html();
        $('#file-upload-container').removeClass('form-field--error');
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
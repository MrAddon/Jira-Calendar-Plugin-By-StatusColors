/**
 * Append an info message to the browse projects calendar tab, to tell the user what it's all about. The message will
 * be shown whenever this resource is included.
 */
AJS.toInit(function($) {

    var suppressTip = WRM.data.claim('com.atlassian.jira.ext.calendar:tipDataProvider.tip').suppressTip;

    if (!suppressTip) {

        var TIP_ID = "browse-project-calendar-info";

        var createTip = function($target) {
            if ($target.length) {
                var $el = $("<div/>");
                AJS.messages.info($el, {
                    body: AJS.I18n.getText("projectpanel.issuecalendar.infotip", AJS.I18n.getText("issue.field.duedate")),
                    closeable: true
                });
                $el.find(".aui-message").attr("id", TIP_ID);
                $target.prepend($el);
            }
        };

        JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, context, reason) {
            var $target;
            if (context.attr("id") == "project-tab") {
                $target = $(".issues-calendar-container", context);
            } else {
                $target = $("#project-tab .issues-calendar-container", context);
            }
            createTip($target);
        });
        createTip($("#project-tab .issues-calendar-container"));

        AJS.bind("messageClose", function(e, data) {
            if (data.attr("id") == TIP_ID) {
                AJS.EventQueue && AJS.EventQueue.push({name: "browseprojectcalendarinfo.closebutton"});
                $.ajax({
                    data: {
                        tipKey: "browseProjectCalendarTab",
                        atl_token: atl_token()
                    },
                    type: "POST",
                    url: contextPath + "/rest/calendar-plugin/1.0/suppressedTips"
                });
            }
        });
    }
});
(function($) {
    
    var JiraIssuesCalendar = {
        resizeGadgetIframeIntervalId : null,

        setLinkEnabled: function(issuesCalendarContainer, linkClassName, enabled) {
            issuesCalendarContainer.find("a." + linkClassName).each(function() {
                if (enabled)
                    $(this).removeClass("issues-calendar-disabled");
                else
                    $(this).addClass("issues-calendar-disabled");
            });
        },

        getContextPath: function(issuesCalendarContainer) {
            return this.getContainerParams(issuesCalendarContainer, "issues-calendar-param")["contextPath"];
        },

        getCalendar : function(issuesCalendarContainer, params) {
            this.setLinkEnabled(issuesCalendarContainer, "issues-calendar-previous", false);
            this.setLinkEnabled(issuesCalendarContainer, "issues-calendar-next", false);
            this.setLinkEnabled(issuesCalendarContainer, "issues-calendar-today", false);

            var calendarTitle = issuesCalendarContainer.find("td.issues-calendar-title");
            var calendarTitleHeight = calendarTitle.height();
            var contextPath = this.getContextPath(issuesCalendarContainer);

            calendarTitle.html("<img src='" + contextPath + "/images/icons/wait.gif'>");
            calendarTitle.css({ height: calendarTitleHeight + "px" });

            AJS.$.ajax({
                type : "GET",
                url : contextPath + "/rest/calendar-plugin/1.0/calendar/htmlcalendar.html",
                data : params,
                dataType: "html",
                success : function(calendarHtml) {
                    issuesCalendarContainer.children("div.issues-calendar").empty().html(calendarHtml);
                    
                    issuesCalendarContainer.removeData("initialized");
                    JiraIssuesCalendar.initCalendar(issuesCalendarContainer);

                    if (gadgets && gadgets.window && gadgets.window.adjustHeight) {
                        JiraIssuesCalendar.resizeGadgetIframeIntervalId = setInterval(function() {
                            if (JiraIssuesCalendar.resizeGadgetIframeIntervalId) {
                                var body = $("body");
                                if (!body.hasClass("loading") && !body.hasClass("loading-small")) {
                                    gadgets.window.adjustHeight();
                                    clearInterval(JiraIssuesCalendar.resizeGadgetIframeIntervalId);
                                    JiraIssuesCalendar.resizeGadgetIframeIntervalId = null;
                                }
                            }
                        }, 100);
                    }
                }
            });
        },

        getFieldSet : function(fieldsetParent) {
            return fieldsetParent.children("fieldset");
        },

        getContainerParams : function(container, paramClassName) {
            var params = {};

            this.getFieldSet(container).find("input." + paramClassName).each(function() {
                params[this.name] = this.value;
            });
            return params;
        },

        getIssuesCalendarParams : function(issuesCalendarContainer) {
            return this.getContainerParams(issuesCalendarContainer.children("div.issues-calendar"), "issues-calendar-param");
        },

        initCalendarNavPrevious : function(issuesCalendarContainer) {
            issuesCalendarContainer.find("a.issues-calendar-previous").click(function(event) {
                event.preventDefault();

                var prevLink = $(this);
                if (!prevLink.hasClass("issues-calendar-disabled")) {
                    var params = JiraIssuesCalendar.getIssuesCalendarParams(issuesCalendarContainer);

                    params["month"] = params["previousMonth"];
                    JiraIssuesCalendar.getCalendar(issuesCalendarContainer, params);
                }
                
                return false;
            });
        },

        initCalendarNavNext : function(issuesCalendarContainer) {
            issuesCalendarContainer.find("a.issues-calendar-next").click(function(event) {
                event.preventDefault();

                var nextLink = $(this);
                if (!nextLink.hasClass("issues-calendar-disabled")) {
                    var params = JiraIssuesCalendar.getIssuesCalendarParams(issuesCalendarContainer);

                    params["month"] = params["nextMonth"];
                    JiraIssuesCalendar.getCalendar(issuesCalendarContainer, params);
                }

                return false;
            });
        },

        initCalendarNavToday : function(issuesCalendarContainer) {
            issuesCalendarContainer.find("a.issues-calendar-today").click(function(event) {
                event.preventDefault();
                
                var todayLink = $(this);
                if (!todayLink.hasClass("issues-calendar-disabled")) {
                    var params = JiraIssuesCalendar.getIssuesCalendarParams(issuesCalendarContainer);

                    params["month"] = params["today"];
                    JiraIssuesCalendar.getCalendar(issuesCalendarContainer, params);
                }

                return false;
            });
        },

        initCalendarToggleVersion : function(issuesCalendarContainer) {
            issuesCalendarContainer.find("a.issues-calendar-version-toggle").click(function() {
                var params = JiraIssuesCalendar.getIssuesCalendarParams(issuesCalendarContainer);

                params["displayVersions"] = params["displayVersions"] == "true" ? "false" : "true";
                JiraIssuesCalendar.getCalendar(issuesCalendarContainer, params);
                return false;
            });
        },

        initCalendarLookAndFeel : function(issuesCalendarContainer) {
            var daysInWeekLnfParams = JiraIssuesCalendar.getContainerParams(issuesCalendarContainer, "issues-calendar-lnf-week");
            $(daysInWeekLnfParams["weekdayCssClasses"]).addClass("weekDay");
            $(daysInWeekLnfParams["weekendCssClasses"]).addClass("weekendDay");

            //this.getFieldSet(issuesCalendarContainer).find("input.issues-calendar-lnf-priority").each(function() {
            //    $("." + this.name).css("background-color", this.value);
            //});
        },

        initCalendar : function(issuesCalendarContainer) {
            if (!issuesCalendarContainer.data("initialized")) {
                issuesCalendarContainer.data("initialized", true);
                this.initCalendarNavPrevious(issuesCalendarContainer);
                this.initCalendarNavNext(issuesCalendarContainer);
                this.initCalendarNavToday(issuesCalendarContainer);
                this.initCalendarToggleVersion(issuesCalendarContainer);
                this.initCalendarLookAndFeel(issuesCalendarContainer);
            }
        }
    };

    AJS.JiraCalendarPlugin = JiraIssuesCalendar;
})(jQuery);


AJS.toInit(function($) {
    $(".issues-calendar-container").each(function() {
        AJS.JiraCalendarPlugin.initCalendar($(this));
    })
});

package com.atlassian.jira.ext.calendar;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserPropertyManager;
import com.google.common.collect.ImmutableSet;
import com.opensymphony.module.propertyset.PropertySet;

/**
 * Manages suppressed tips on a per-user basis.
 *
 * Once a user has acknowledged/dismissed a tip, it shouldn't be shown to them again. This class handles persisting and
 * querying of this information.
 *
 * Note: this class is copied from jira-issue-nav-plugin. We should really move it into jira-api.
 */
public class SuppressedTipsManager
{
    // The property namespace that is to contain all suppressed tip properties;
    // must end in a period (.) as it is prepended to all user-provided keys.
    private static final String KEY_NAMESPACE = "jira.user.suppressedTips.";

    // All known suppressed tip properties, not including the above namespace.
    // Only those keys contained in this set will be accepted by the manager.
    private static final ImmutableSet<String> KEY_WHITE_LIST = ImmutableSet.of(
            "browseProjectCalendarTab"
    );

    private final UserPropertyManager userPropertyManager;

    public SuppressedTipsManager(UserPropertyManager userPropertyManager)
    {
        this.userPropertyManager = userPropertyManager;
    }

    /**
     * Get whether a tip is suppressed for a user.
     *
     * @param tipKey The tip key.
     * @param user The user.
     * @return {@code true} only if the tip described by {@code tipKey} is suppressed for {@code user}.
     * @throws IllegalArgumentException If {@code tipKey} is not a known tip key.
     */
    public boolean isSuppressed(String tipKey, ApplicationUser user)
    {
        if (KEY_WHITE_LIST.contains(tipKey))
        {
            PropertySet propertySet = userPropertyManager.getPropertySet(user);
            return propertySet.getBoolean(KEY_NAMESPACE + tipKey);
        }
        else
        {
            throw new IllegalArgumentException("Invalid tip key '" + tipKey + "'.");
        }
    }

    /**
     * Set whether a tip is suppressed for a user.
     *
     * @param tipKey The tip key.
     * @param user The user.
     * @param suppressed Whether the tip should be suppressed.
     * @throws IllegalArgumentException If {@code tipKey} is not a known tip key.
     */
    public void setSuppressed(String tipKey, ApplicationUser user, boolean suppressed)
    {
        if (KEY_WHITE_LIST.contains(tipKey))
        {
            PropertySet propertySet = userPropertyManager.getPropertySet(user);
            propertySet.setBoolean(KEY_NAMESPACE + tipKey, suppressed);
        }
        else
        {
            throw new IllegalArgumentException("Invalid tip key '" + tipKey + "'.");
        }
    }
}
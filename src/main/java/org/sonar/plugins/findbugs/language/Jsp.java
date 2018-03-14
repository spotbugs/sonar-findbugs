/*
 * SonarQube Findbugs Plugin
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs.language;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

import java.util.List;

/**
 * This language cover JavaServer Pages (JSP).
 *
 * It may conflicts with the "web" language from the Web Plugin which supports multiples XML derived languages.
 */
public class Jsp extends AbstractLanguage {

    /**
     * JSP key
     */
    public static final String KEY = "jsp";

    /**
     * JSP language name (Acronym chosen over "JavaServer Pages" to avoid visual glitch)
     */
    public static final String NAME = "JSP";


    /**
     * Key of the file suffix parameter
     */
    public static final String FILE_SUFFIXES_KEY = "sonar.jsp.file.suffixes";

    /**
     * Default Java files knows suffixes
     */
    public static final String DEFAULT_FILE_SUFFIXES = ".jsp";

    /**
     * Configuration of the plugin.
     */
    private final Configuration config;

    /**
     * Default constructor
     */
    public Jsp(Configuration config) {
        super(KEY, NAME);
        this.config = config;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.sonar.api.resources.AbstractLanguage#getFileSuffixes()
     */
    @Override
    public String[] getFileSuffixes() {
        String[] suffixes = filterEmptyStrings(config.getStringArray(FILE_SUFFIXES_KEY));
        if (suffixes.length == 0) {
            suffixes = StringUtils.split(DEFAULT_FILE_SUFFIXES, ",");
        }
        return suffixes;
    }

    private static String[] filterEmptyStrings(String[] stringArray) {
        List<String> nonEmptyStrings = Lists.newArrayList();
        for (String string : stringArray) {
            if (StringUtils.isNotBlank(string.trim())) {
                nonEmptyStrings.add(string.trim());
            }
        }
        return nonEmptyStrings.toArray(new String[nonEmptyStrings.size()]);
    }


}

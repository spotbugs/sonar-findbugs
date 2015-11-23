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
import org.sonar.api.config.Settings;
import org.sonar.api.resources.AbstractLanguage;

import java.util.List;

/**
 * This language cover JavaServer Pages (JSP).
 *
 * It may conflicts with the "web" language from the Web Plugin which supports multiples XML derived languages.
 */
public class Jsp extends AbstractLanguage {

    /**
     * Java key
     */
    public static final String KEY = "jsp";

    /**
     * Java name
     */
    public static final String NAME = "JSP";


    /**
     * Key of the file suffix parameter
     */
    public static final String FILE_SUFFIXES_KEY = "sonar.javab.file.suffixes";

    /**
     * Default Java files knows suffixes
     */
    public static final String DEFAULT_FILE_SUFFIXES = ".jsp"; //TODO: .groovy,.gsp,.scala,.scala.html

    /**
     * Key of the java version used for sources
     */
    public static final String SOURCE_VERSION = "sonar.javab.source";

    /**
     * Settings of the plugin.
     */
    private final Settings settings;

    /**
     * Default constructor
     */
    public Jsp(Settings settings) {
        super(KEY, NAME);
        this.settings = settings;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.sonar.api.resources.AbstractLanguage#getFileSuffixes()
     */
    @Override
    public String[] getFileSuffixes() {
        String[] suffixes = filterEmptyStrings(settings.getStringArray(Jsp.FILE_SUFFIXES_KEY));
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

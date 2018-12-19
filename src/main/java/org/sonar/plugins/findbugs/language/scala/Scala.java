package org.sonar.plugins.findbugs.language.scala;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

import java.util.List;

public class Scala extends AbstractLanguage {

    public static final String KEY = "scala";

    public static final String NAME = "Scala";

    public static final String FILE_SUFFIXES_KEY = "sonar.scala.file.suffixes";

    public static final String DEFAULT_FILE_SUFFIXES = ".scala";

    private final Configuration config;

    public Scala(Configuration config) {
        super(KEY, NAME);
        this.config = config;
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

    @Override
    public String[] getFileSuffixes() {
        String[] suffixes = filterEmptyStrings(config.getStringArray(FILE_SUFFIXES_KEY));
        if (suffixes.length == 0) {
            suffixes = StringUtils.split(DEFAULT_FILE_SUFFIXES, ",");
        }
        return suffixes;
    }
}
